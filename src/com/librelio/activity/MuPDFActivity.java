package com.librelio.activity;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import com.artifex.mupdf.LinkInfo;
import com.artifex.mupdf.MediaHolder;
import com.artifex.mupdf.MuPDFCore;
import com.artifex.mupdf.MuPDFPageAdapter;
import com.artifex.mupdf.MuPDFPageView;
import com.artifex.mupdf.OutlineItem;
import com.artifex.mupdf.PDFPreviewPagerAdapter;
import com.artifex.mupdf.domain.OutlineActivityData;
import com.artifex.mupdf.domain.SearchTaskResult;
import com.artifex.mupdf.view.DocumentReaderView;
import com.artifex.mupdf.view.ReaderView;
import com.librelio.base.BaseActivity;
import com.librelio.lib.utils.PDFParser;
import com.librelio.model.Magazine;
import com.librelio.storage.MagazineManager;
import com.librelio.task.TinySafeAsyncTask;
import com.librelio.view.HorizontalListView;
import com.librelio.view.ProgressDialogX;
import com.niveales.wind.R;

//TODO: remove preffix mXXXX from all properties this class
public class MuPDFActivity extends BaseActivity{
	private static final String TAG = "MuPDFActivity";

	private static final int SEARCH_PROGRESS_DELAY = 200;
	private static final int WAIT_DIALOG = 0;
	private static final String FILE_NAME = "FileName";

	private MuPDFCore core;
	private String fileName;
	private int mOrientation;

	private int          mPageSliderRes;
	private boolean      buttonsVisible;
	private boolean      mTopBarIsSearch;

	private WeakReference<SearchTask> searchTask;
	private ProgressDialog dialog;

	private AlertDialog.Builder alertBuilder;
	private ReaderView   docView;
	private View         buttonsView;
	private EditText     mPasswordView;
	private TextView     mFilenameView;
//	private SeekBar      mPageSlider;
//	private TextView     mPageNumberView;
	private ImageButton  mSearchButton;
	private ImageButton  mCancelButton;
	private ImageButton  mOutlineButton;
	private ViewSwitcher mTopBarSwitcher;
// XXX	private ImageButton  mLinkButton;
	private ImageButton  mSearchBack;
	private ImageButton  mSearchFwd;
	private EditText     mSearchText;
	//private SearchTaskResult mSearchTaskResult;
	private final Handler mHandler = new Handler();
	private FrameLayout mPreviewBarHolder;
	private HorizontalListView mPreview;
	private MuPDFPageAdapter mDocViewAdapter;
	private SparseArray<LinkInfo[]> linkOfDocument;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		alertBuilder = new AlertDialog.Builder(this);
	
		core = getMuPdfCore(savedInstanceState);
	
		if (core == null) {
			return;
		}
	
		mOrientation = getResources().getConfiguration().orientation;

		if(mOrientation == Configuration.ORIENTATION_LANDSCAPE) {
			core.setDisplayPages(2);
		} else {
			core.setDisplayPages(1);
		}

		createUI(savedInstanceState);
	}
	
	private void requestPassword(final Bundle savedInstanceState) {
		mPasswordView = new EditText(this);
		mPasswordView.setInputType(EditorInfo.TYPE_TEXT_VARIATION_PASSWORD);
		mPasswordView.setTransformationMethod(new PasswordTransformationMethod());

		AlertDialog alert = alertBuilder.create();
		alert.setTitle(R.string.enter_password);
		alert.setView(mPasswordView);
		alert.setButton(AlertDialog.BUTTON_POSITIVE, getResources().getString(R.string.ok),
				new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				if (core.authenticatePassword(mPasswordView.getText().toString())) {
					createUI(savedInstanceState);
				} else {
					requestPassword(savedInstanceState);
				}
			}
		});
		alert.setButton(AlertDialog.BUTTON_NEGATIVE, getResources().getString(R.string.cancel),
				new DialogInterface.OnClickListener() {

			public void onClick(DialogInterface dialog, int which) {
				finish();
			}
		});
		alert.show();
	}

	private MuPDFCore getMuPdfCore(Bundle savedInstanceState) {
		MuPDFCore core = null;
		if (core == null) {
			core = (MuPDFCore)getLastNonConfigurationInstance();

			if (savedInstanceState != null && savedInstanceState.containsKey(FILE_NAME)) {
				fileName = savedInstanceState.getString(FILE_NAME);
			}
		}
		if (core == null) {
			Intent intent = getIntent();
			if (Intent.ACTION_VIEW.equals(intent.getAction())) {
				Uri uri = intent.getData();
				if (uri.toString().startsWith("content://media/external/file")) {
					// Handle view requests from the Transformer Prime's file manager
					// Hopefully other file managers will use this same scheme, if not
					// using explicit paths.
					Cursor cursor = getContentResolver().query(uri, new String[]{"_data"}, null, null, null);
					if (cursor.moveToFirst()) {
						uri = Uri.parse(cursor.getString(0));
					}
				}

				core = openFile(Uri.decode(uri.getEncodedPath()));
				SearchTaskResult.recycle();
			}
			if (core != null && core.needsPassword()) {
				requestPassword(savedInstanceState);
				return null;
			}
		}
		if (core == null) {
			AlertDialog alert = alertBuilder.create();
			
			alert.setTitle(R.string.open_failed);
			alert.setButton(AlertDialog.BUTTON_POSITIVE, getString(R.string.dismiss),
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							finish();
						}
					});
			alert.show();
			return null;
		}
		return core;
	}

	private void createUI(Bundle savedInstanceState) {
		if (core == null)
			return;
		// Now create the UI.
		// First create the document view making use of the ReaderView's internal
		// gesture recognition
		docView = new DocumentReaderView(this, linkOfDocument) {

			@Override
			protected void onMoveToChild(View view, int i) {
				Log.d(TAG,"onMoveToChild id = "+i);

				if (core == null){
					return;
				} 
				MuPDFPageView pageView = (MuPDFPageView) docView.getDisplayedView();
				if(pageView!=null){
					pageView.cleanRunningLinkList();
				}
				new ActivateAutoLinks().safeExecute(i);
				super.onMoveToChild(view, i);
			}

			@Override
			public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
				if (!isShowButtonsDisabled()) {
					hideButtons();
				}
				return super.onScroll(e1, e2, distanceX, distanceY);
			}

			@Override
			protected void onContextMenuClick() {
				if (!buttonsVisible) {
					showButtons();
				} else {
					hideButtons();
				}
			}

			@Override
			protected void onBuy(String path) {
				MuPDFActivity.this.onBuy(path);
			}


		};
		mDocViewAdapter = new MuPDFPageAdapter(this, core);
		docView.setAdapter(mDocViewAdapter);

		// Make the buttons overlay, and store all its
		// controls in variables
		makeButtonsView();

		// Set up the page slider
		int smax = Math.max(core.countPages()-1,1);
		mPageSliderRes = ((10 + smax - 1)/smax) * 2;

		// Set the file-name text
		mFilenameView.setText(fileName);

		// Activate the seekbar
//		mPageSlider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
//			public void onStopTrackingTouch(SeekBar seekBar) {
//				mDocView.setDisplayedViewIndex((seekBar.getProgress()+mPageSliderRes/2)/mPageSliderRes);
//			}
//
//			public void onStartTrackingTouch(SeekBar seekBar) {}
//
//			public void onProgressChanged(SeekBar seekBar, int progress,
//					boolean fromUser) {
//				updatePageNumView((progress+mPageSliderRes/2)/mPageSliderRes);
//			}
//		});

		// Activate the search-preparing button
		mSearchButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				searchModeOn();
			}
		});

		mCancelButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				searchModeOff();
			}
		});

		// Search invoking buttons are disabled while there is no text specified
		mSearchBack.setEnabled(false);
		mSearchFwd.setEnabled(false);

		// React to interaction with the text widget
		mSearchText.addTextChangedListener(new TextWatcher() {

			public void afterTextChanged(Editable s) {
				boolean haveText = s.toString().length() > 0;
				mSearchBack.setEnabled(haveText);
				mSearchFwd.setEnabled(haveText);

				// Remove any previous search results
				if (SearchTaskResult.get() != null && !mSearchText.getText().toString().equals(SearchTaskResult.get().txt)) {
					SearchTaskResult.recycle();
					docView.resetupChildren();
				}
			}
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {}
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {}
		});

		//React to Done button on keyboard
		mSearchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_DONE)
					search(1);
				return false;
			}
		});

		mSearchText.setOnKeyListener(new View.OnKeyListener() {
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER)
					search(1);
				return false;
			}
		});

		// Activate search invoking buttons
		mSearchBack.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				search(-1);
			}
		});
		mSearchFwd.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				search(1);
			}
		});

		if (core.hasOutline()) {
			mOutlineButton.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					OutlineItem outline[] = core.getOutline();
					if (outline != null) {
						OutlineActivityData.get().items = outline;
						Intent intent = new Intent(MuPDFActivity.this, OutlineActivity.class);
						startActivityForResult(intent, 0);
					}
				}
			});
		} else {
			mOutlineButton.setVisibility(View.GONE);
		}

		// Reenstate last state if it was recorded
		SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
		int orientation = prefs.getInt("orientation", mOrientation);
		int pageNum = prefs.getInt("page"+fileName, 0);
		if(orientation == mOrientation)
			docView.setDisplayedViewIndex(pageNum);
		else {
			if(orientation == Configuration.ORIENTATION_PORTRAIT) {
				docView.setDisplayedViewIndex((pageNum + 1) / 2);
			} else {
				docView.setDisplayedViewIndex((pageNum == 0) ? 0 : pageNum * 2 - 1);
			}
		}

		if (savedInstanceState == null || !savedInstanceState.getBoolean("ButtonsHidden", false)) {
			showButtons();
		}

		if(savedInstanceState != null && savedInstanceState.getBoolean("SearchMode", false)) {
			searchModeOn();
		}

		// Stick the document view and the buttons overlay into a parent view
		RelativeLayout layout = new RelativeLayout(this);
		layout.addView(docView);
		layout.addView(buttonsView);
//		layout.setBackgroundResource(R.drawable.tiled_background);
		//layout.setBackgroundResource(R.color.canvas);
		layout.setBackgroundColor(Color.BLACK);
		setContentView(layout);
	}

	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode >= 0)
			docView.setDisplayedViewIndex(resultCode);
		super.onActivityResult(requestCode, resultCode, data);
	}

	public Object onRetainNonConfigurationInstance() {
		MuPDFCore mycore = core;
		core = null;
		return mycore;
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		if (fileName != null && docView != null) {
			outState.putString("FileName", fileName);

			// Store current page in the prefs against the file name,
			// so that we can pick it up each time the file is loaded
			// Other info is needed only for screen-orientation change,
			// so it can go in the bundle
			SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
			SharedPreferences.Editor edit = prefs.edit();
			edit.putInt("page"+fileName, docView.getDisplayedViewIndex());
			edit.putInt("orientation", mOrientation);
			edit.commit();
		}

		if (!buttonsVisible)
			outState.putBoolean("ButtonsHidden", true);

		if (mTopBarIsSearch)
			outState.putBoolean("SearchMode", true);
	}

	@Override
	protected void onPause() {
		super.onPause();

		killSearch();

		if (fileName != null && docView != null) {
			SharedPreferences prefs = getPreferences(Context.MODE_PRIVATE);
			SharedPreferences.Editor edit = prefs.edit();
			edit.putInt("page"+fileName, docView.getDisplayedViewIndex());
			edit.putInt("orientation", mOrientation);
			edit.commit();
		}
	}
	
	@Override
	public void onDestroy() {
		if (core != null) {
			core.onDestroy();
		}
		core = null;

		super.onDestroy();
	}

	void showButtons() {
		if (core == null) {
			return;
		}
		if (!buttonsVisible) {
			buttonsVisible = true;
			// Update page number text and slider
			int index = docView.getDisplayedViewIndex();
			updatePageNumView(index);
//			mPageSlider.setMax((core.countPages()-1)*mPageSliderRes);
//			mPageSlider.setProgress(index*mPageSliderRes);
			if (mTopBarIsSearch) {
				mSearchText.requestFocus();
				showKeyboard();
			}

			Animation anim = new TranslateAnimation(0, 0, -mTopBarSwitcher.getHeight(), 0);
			anim.setDuration(200);
			anim.setAnimationListener(new Animation.AnimationListener() {
				public void onAnimationStart(Animation animation) {
					mTopBarSwitcher.setVisibility(View.VISIBLE);
				}
				public void onAnimationRepeat(Animation animation) {}
				public void onAnimationEnd(Animation animation) {}
			});
			mTopBarSwitcher.startAnimation(anim);
			// Update listView position
			mPreview.setSelection(docView.getDisplayedViewIndex());
			anim = new TranslateAnimation(0, 0, mPreviewBarHolder.getHeight(), 0);
			anim.setDuration(200);
			anim.setAnimationListener(new Animation.AnimationListener() {
				public void onAnimationStart(Animation animation) {
					mPreviewBarHolder.setVisibility(View.VISIBLE);
				}
				public void onAnimationRepeat(Animation animation) {}
				public void onAnimationEnd(Animation animation) {
				}
			});
			mPreviewBarHolder.startAnimation(anim);
		}
	}

	void hideButtons() {
		if (buttonsVisible) {
			buttonsVisible = false;
			hideKeyboard();

			Animation anim = new TranslateAnimation(0, 0, 0, -mTopBarSwitcher.getHeight());
			anim.setDuration(200);
			anim.setAnimationListener(new Animation.AnimationListener() {
				public void onAnimationStart(Animation animation) {}
				public void onAnimationRepeat(Animation animation) {}
				public void onAnimationEnd(Animation animation) {
					mTopBarSwitcher.setVisibility(View.INVISIBLE);
				}
			});
			mTopBarSwitcher.startAnimation(anim);
			
			anim = new TranslateAnimation(0, 0, 0, this.mPreviewBarHolder.getHeight());
			anim.setDuration(200);
			anim.setAnimationListener(new Animation.AnimationListener() {
				public void onAnimationStart(Animation animation) {
					mPreviewBarHolder.setVisibility(View.INVISIBLE);
				}
				public void onAnimationRepeat(Animation animation) {}
				public void onAnimationEnd(Animation animation) {
				}
			});
			mPreviewBarHolder.startAnimation(anim);
		}
	}

	void searchModeOn() {
		if (!mTopBarIsSearch) {
			mTopBarIsSearch = true;
			//Focus on EditTextWidget
			mSearchText.requestFocus();
			showKeyboard();
			mTopBarSwitcher.showNext();
		}
	}

	void searchModeOff() {
		if (mTopBarIsSearch) {
			mTopBarIsSearch = false;
			hideKeyboard();
			mTopBarSwitcher.showPrevious();
			SearchTaskResult.recycle();
			// Make the ReaderView act on the change to mSearchTaskResult
			// via overridden onChildSetup method.
			docView.resetupChildren();
		}
	}

	void updatePageNumView(int index) {
		if (core == null)
			return;
//		mPageNumberView.setText(String.format("%d/%d", index+1, core.countPages()));
	}

	void makeButtonsView() {
		buttonsView = getLayoutInflater().inflate(R.layout.buttons,null);
		mFilenameView = (TextView)buttonsView.findViewById(R.id.docNameText);
//		mPageSlider = (SeekBar)mButtonsView.findViewById(R.id.pageSlider);
		mPreviewBarHolder = (FrameLayout) buttonsView.findViewById(R.id.PreviewBarHolder);
		mPreview = new HorizontalListView(this);
		FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-1, -1);
		mPreview.setLayoutParams(lp);
		mPreview.setAdapter(new PDFPreviewPagerAdapter(this, core));
		mPreview.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> pArg0, View pArg1,
					int position, long id) {
				// TODO Auto-generated method stub
				hideButtons();
				docView.setDisplayedViewIndex((int)id);
			}

		});
		mPreviewBarHolder.addView(mPreview);
//		Gallery mGallery = (Gallery) mButtonsView.findViewById(R.id.PreviewGallery);
//		mGallery.setAdapter(new PDFPreviewPagerAdapter(this, core));

//		mPageNumberView = (TextView)mButtonsView.findViewById(R.id.pageNumber);
		mSearchButton = (ImageButton)buttonsView.findViewById(R.id.searchButton);
		mCancelButton = (ImageButton)buttonsView.findViewById(R.id.cancel);
		mOutlineButton = (ImageButton)buttonsView.findViewById(R.id.outlineButton);
		mTopBarSwitcher = (ViewSwitcher)buttonsView.findViewById(R.id.switcher);
		mSearchBack = (ImageButton)buttonsView.findViewById(R.id.searchBack);
		mSearchFwd = (ImageButton)buttonsView.findViewById(R.id.searchForward);
		mSearchText = (EditText)buttonsView.findViewById(R.id.searchText);
// XXX		mLinkButton = (ImageButton)mButtonsView.findViewById(R.id.linkButton);
		mTopBarSwitcher.setVisibility(View.INVISIBLE);
//		mPageNumberView.setVisibility(View.INVISIBLE);
//		mPageSlider.setVisibility(View.INVISIBLE);
		mPreviewBarHolder.setVisibility(View.INVISIBLE);
	}

	void showKeyboard() {
		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imm != null)
			imm.showSoftInput(mSearchText, 0);
	}

	void hideKeyboard() {
		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		if (imm != null)
			imm.hideSoftInputFromWindow(mSearchText.getWindowToken(), 0);
	}

	void killSearch() {
		if (searchTask != null && null != searchTask.get()) {
			searchTask.get().cancel(true);
			searchTask = null;
		}
	}

	void search(int direction) {
		hideKeyboard();
		if (core == null)
			return;
		killSearch();

		final int increment = direction;
		final int startIndex = SearchTaskResult.get() == null ? docView.getDisplayedViewIndex() : SearchTaskResult.get().pageNumber + increment;

		SearchTask st = new SearchTask(this, increment, startIndex);
		st.safeExecute();
		searchTask = new WeakReference<MuPDFActivity.SearchTask>(st);
	}

	@Override
	public boolean onSearchRequested() {
		if (buttonsVisible && mTopBarIsSearch) {
			hideButtons();
		} else {
			showButtons();
			searchModeOn();
		}
		return super.onSearchRequested();
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (buttonsVisible && !mTopBarIsSearch) {
			hideButtons();
		} else {
			showButtons();
			searchModeOff();
		}
		return super.onPrepareOptionsMenu(menu);
	}

	private MuPDFCore openFile(String path) {
		int lastSlashPos = path.lastIndexOf('/');
		fileName = new String(lastSlashPos == -1
					? path
					: path.substring(lastSlashPos+1));
		Log.d(TAG, "Trying to open " + path);
		PDFParser linkGetter = new PDFParser(path);
		linkOfDocument = linkGetter.getLinkInfo();
		Log.d(TAG,"link size = "+linkOfDocument.size());
		for(int i=0;i<linkOfDocument.size();i++){
			Log.d(TAG,"--- i = "+i);
			if(linkOfDocument.get(i)!=null){
				for(int j=0;j<linkOfDocument.get(i).length;j++){
					String link = linkOfDocument.get(i)[j].uri;
					Log.d(TAG,"link[" + j + "] = "+link);
					String local = "http://localhost";
					if(link.startsWith(local)){
						Log.d(TAG,"   link: "+link);
					}
				}
			}
		}
		try {
			core = new MuPDFCore(path);
			// New file: drop the old outline data
			OutlineActivityData.set(null);
		} catch (Exception e) {
			Log.e(TAG, "get core failed", e);
			return null;
		}
		return core;
	}



	private void onBuy(String path) {
		Log.d(TAG, "onBuy event path = " + path);
		MagazineManager magazineManager = new MagazineManager(getContext());
		Magazine magazine = magazineManager.findByFileName(path);
		if (null != magazine) {
			Intent intent = new Intent(getContext(), BillingActivity.class);
			intent
				.putExtra(DownloadActivity.FILE_NAME_KEY, magazine.getFileName())
				.putExtra(DownloadActivity.TITLE_KEY, magazine.getTitle())
				.putExtra(DownloadActivity.SUBTITLE_KEY, magazine.getSubtitle());
			getContext().startActivity(intent);
		}
	}

	private Context getContext() {
		return this;
	}

	private class ActivateAutoLinks extends TinySafeAsyncTask<Integer, Void, ArrayList<LinkInfo>> {

		@Override
		protected ArrayList<LinkInfo> doInBackground(Integer... params) {
			int page = params[0].intValue();
			Log.d(TAG, "Page = " + page);
			if (null != core) {
				LinkInfo[] links = core.getPageLinks(page);
				if(null == links){
					return null;
				}
				ArrayList<LinkInfo> autoLinks = new ArrayList<LinkInfo>();
				for (LinkInfo link : links) {
					Log.d(TAG, "activateAutoLinks link: " + link.uri);
					if (null == link.uri) {
						continue;
					}
					if (link.isMediaURI()) {
						if (link.isAutoPlay()) {
							autoLinks.add(link);
						}
					}
				}
				return autoLinks;
			}
			return null;
		}

		@Override
		protected void onPostExecute(final ArrayList<LinkInfo> autoLinks) {
			if (isCancelled() || autoLinks == null) {
				return;
			}
			docView.post(new Runnable() {
				public void run() {
					for(LinkInfo link : autoLinks){
						MuPDFPageView pageView = (MuPDFPageView) docView.getDisplayedView();
						if (pageView != null && null != core) {
							String basePath = core.getFileDirectory();
							MediaHolder mediaHolder = new MediaHolder(getContext(), link, basePath);
							pageView.addMediaHolder(mediaHolder, link.uri);
							pageView.addView(mediaHolder);
							mediaHolder.setVisibility(View.VISIBLE);
							mediaHolder.requestLayout();
						}
					}
				}
			});
		}
	}

	private class SearchTask extends TinySafeAsyncTask<Void, Integer, SearchTaskResult> {
		private final int increment; 
		private final int startIndex;
		private final ProgressDialogX progressDialog;
		
		public SearchTask(Context context, int increment, int startIndex) {
			this.increment = increment;
			this.startIndex = startIndex;
			progressDialog = new ProgressDialogX(context);
			progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			progressDialog.setTitle(getString(R.string.searching_));
			progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
				public void onCancel(DialogInterface dialog) {
					killSearch();
				}
			});
			progressDialog.setMax(core.countPages());

		}

		@Override
		protected SearchTaskResult doInBackground(Void... params) {
			int index = startIndex;

			while (0 <= index && index < core.countPages() && !isCancelled()) {
				publishProgress(index);
				RectF searchHits[] = core.searchPage(index, mSearchText.getText().toString());

				if (searchHits != null && searchHits.length > 0) {
					return SearchTaskResult.init(mSearchText.getText().toString(), index, searchHits);
				}

				index += increment;
			}
			return null;
		}

		@Override
		protected void onPostExecute(SearchTaskResult result) {
			if (isCancelled()) {
				return;
			}
			progressDialog.cancel();
			if (result != null) {
				// Ask the ReaderView to move to the resulting page
				docView.setDisplayedViewIndex(result.pageNumber);
			    SearchTaskResult.recycle();
				// Make the ReaderView act on the change to mSearchTaskResult
				// via overridden onChildSetup method.
			    docView.resetupChildren();
			} else {
				alertBuilder.setTitle(SearchTaskResult.get() == null ? R.string.text_not_found : R.string.no_further_occurences_found);
				AlertDialog alert = alertBuilder.create();
				alert.setButton(AlertDialog.BUTTON_POSITIVE, "Dismiss",
						(DialogInterface.OnClickListener)null);
				alert.show();
			}
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
			progressDialog.cancel();
		}

		@Override
		protected void onProgressUpdate(Integer... values) {
			super.onProgressUpdate(values);
			progressDialog.setProgress(values[0].intValue());
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			mHandler.postDelayed(new Runnable() {
				public void run() {
					if (!progressDialog.isCancelled())
					{
						progressDialog.show();
						progressDialog.setProgress(startIndex);
					}
				}
			}, SEARCH_PROGRESS_DELAY);
		}
	}

}

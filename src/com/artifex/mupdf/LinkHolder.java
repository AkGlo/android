/**
 * 
 */
package com.artifex.mupdf;

import com.librelio.lib.LibrelioApplication;
import com.librelio.lib.ui.SlideShowActivity;
import com.librelio.lib.utils.SlideshowAdapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.FrameLayout;
import android.widget.Gallery;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.FrameLayout.LayoutParams;

/**
 * @author Dmitry Valetin
 *
 */
public class LinkHolder extends FrameLayout {

	private LinkInfo mLinkInfo;
	private Gallery mGallery;
	private WebView mWebVew;
	private float scale = 1.0f;
	private int mAutoplayDelay;
	private Handler mAutoplayHandler;
	
	/**
	 * @param pContext
	 */
	public LinkHolder(Context pContext, LinkInfo link) {
		super(pContext);
		mLinkInfo = link;
		String uriString = link.uri;
		if(uriString == null)
			return;
		if(Uri.parse(uriString).getQueryParameter("warect") != null && Uri.parse(uriString).getQueryParameter("warect").equals("full")) {
			return;
		}
		
		boolean autoPlay = Uri.parse(uriString).getQueryParameter("waplay") != null && Uri.parse(uriString).getQueryParameter("waplay").equals("auto");

		
		if(uriString.startsWith("http://localhost/")) {
			// local resource
			final String path = Uri.parse(uriString).getPath();
			if (path.endsWith("jpg") || path.endsWith("png")
					|| path.endsWith("bmp")) {
				
				mAutoplayDelay = 800;
				if(Uri.parse(uriString).getQueryParameter("wadelay") != null) {
					mAutoplayDelay = Integer.valueOf(Uri.parse(uriString).getQueryParameter("wadelay"));
				}
				
				int bgColor = Color.BLACK;
				if(Uri.parse(uriString).getQueryParameter("wabgcolor") != null) {
					bgColor = Uri.parse(uriString).getQueryParameter("wabgcolor").equals("white") ?
							Color.WHITE : Color.BLACK;
				}
				FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
						LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
				lp.gravity = Gravity.CENTER;
				mGallery = new Gallery(getContext());

				mGallery.setAdapter(new SlideshowAdapter(getContext(),
						LibrelioApplication.appDirectory + "/wind_355/"
								+ Uri.parse(uriString).getPath()));
				mGallery.setLayoutParams(lp);
				mGallery.setBackgroundColor(bgColor);

				mGallery.setOnItemClickListener(new OnItemClickListener() {

					@Override
					public void onItemClick(AdapterView<?> pParent, View pView,
							int pPosition, long pId) {
						setVisibility(View.GONE);
						Intent intent = new Intent(getContext(), SlideShowActivity.class);
						intent.putExtra("path", path);
						intent.putExtra("uri", mLinkInfo.uri);
						getContext().startActivity(intent);
					}
				});
				if(autoPlay) {
					mAutoplayHandler = new Handler();
					mAutoplayHandler.postDelayed(new Runnable() {

						@Override
						public void run() {
							int item = mGallery.getSelectedItemPosition() + 1;
							if(item >= mGallery.getCount()) {
								item = 0;
							}
							mGallery.setSelection(item);
							mAutoplayHandler.postDelayed(this, mAutoplayDelay);
						}}, mAutoplayDelay);
				} else {
					setVisibility(View.GONE);
				}
				addView(mGallery);
//				requestLayout();
			}
			if (path.endsWith("mp4")) {
				
			}
		}
		
		
	}

	
	public void hitLinkUri(String uri) {
		if(mLinkInfo.uri.equals(uri)) {
			// TODO: start playing link
			this.setVisibility(View.VISIBLE);
		}
	}
	
	public LinkInfo getLinkInfo() {
		return mLinkInfo;
	}
}

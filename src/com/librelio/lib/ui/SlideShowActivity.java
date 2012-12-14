/**
 * 
 */
package com.librelio.lib.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Gallery;

import com.artifex.mupdf.SimpleGallery;
import com.librelio.lib.LibrelioApplication;
import com.librelio.lib.adapter.SlideshowAdapter;
import com.niveales.wind.R;

/**
 * @author Dmitry Valetin
 * 
 */
public class SlideShowActivity extends Activity {
	private SimpleGallery mSlideshowGallery;

	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.sideshow_activity_layout);
		mSlideshowGallery = (SimpleGallery) findViewById(R.id.SlideshowGallery);

		String path = getIntent().getExtras().getString("path");

		SlideshowAdapter adapter = new SlideshowAdapter(this, 
				((LibrelioApplication)getApplication()).APP_DIRECTORY+"/wind_355/"+path);
//		SlideshowAdapter adapter = new SlideshowAdapter(this, 
//				((LibrelioApplication)getApplication()).appDirectory+"/wind_355/PWAVIETNAM_4.jpg");

		mSlideshowGallery.setAdapter(adapter);
		mSlideshowGallery.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> pParent, View pView,
					int pPosition, long pId) {
				// TODO Auto-generated method stub
				SlideShowActivity.this.finish();
			}
		});
	}
}

package com.librelio.fragment;
/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.librelio.activity.HTMLViewerActivity;

/**
 * A fragment that displays a WebView.
 * <p/>
 * The WebView is automically paused or resumed when the Fragment is paused or resumed.
 */
public class WebViewFragment extends Fragment {
    private WebView mWebView;
    private boolean mIsWebViewAvailable;
    private HTMLViewerActivity.OnWebViewClickListener onWebViewClickListener;

    public WebViewFragment() {
    }

    public static WebViewFragment newInstance(String url) {
        WebViewFragment f = new WebViewFragment();
        Bundle bundle = new Bundle();
        bundle.putString("url", url);
        f.setArguments(bundle);
        return f;
    }

    /**
     * Called to instantiate the view. Creates and returns the WebView.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (mWebView != null) {
            mWebView.destroy();
        }
        mWebView = new WebView(getActivity());
        mIsWebViewAvailable = true;
        mWebView.setWebViewClient(new InnerWebViewClient()); // forces it to open in app

        mWebView.loadUrl(getArguments().getString("url"));
        WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        mWebView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        mWebView.setBackgroundColor(Color.BLACK);
        mWebView.setOnTouchListener(new View.OnTouchListener() {
            float oldX = 0, newX = 0, sens = 5;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        oldX = event.getX();
                        break;

                    case MotionEvent.ACTION_UP:
                        newX = event.getX();
                        if (Math.abs(oldX - newX) < sens) {
                            if (onWebViewClickListener != null) {
                                onWebViewClickListener.onWebViewClick();
                            }
                            return true;
                        }
                        oldX = 0;
                        newX = 0;
                        break;
                }

                return false;
            }
        });
        return mWebView;
    }

    /**
     * Called when the fragment is visible to the user and actively running. Resumes the WebView.
     */
    @Override
    public void onPause() {
        super.onPause();
        mWebView.onPause();
    }

    /**
     * Called when the fragment is no longer resumed. Pauses the WebView.
     */
    @Override
    public void onResume() {
        mWebView.onResume();
        super.onResume();
    }

    /**
     * Called when the WebView has been detached from the fragment.
     * The WebView is no longer available after this time.
     */
    @Override
    public void onDestroyView() {
        mIsWebViewAvailable = false;
        super.onDestroyView();
    }

    /**
     * Called when the fragment is no longer in use. Destroys the internal state of the WebView.
     */
    @Override
    public void onDestroy() {
        if (mWebView != null) {
            mWebView.destroy();
            mWebView = null;
        }
        super.onDestroy();
    }

    /**
     * Gets the WebView.
     */
    public WebView getWebView() {
        return mIsWebViewAvailable ? mWebView : null;
    }

    public void setOnWebViewClickListener(HTMLViewerActivity.OnWebViewClickListener onWebViewClickListener) {
        this.onWebViewClickListener = onWebViewClickListener;
    }

    /* To ensure links open within the application */
    private class InnerWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }
    }
}
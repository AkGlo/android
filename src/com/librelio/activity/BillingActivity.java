/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package com.librelio.activity;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpParams;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.android.vending.billing.IInAppBillingService;
import com.librelio.LibrelioApplication;
import com.librelio.base.BaseActivity;
import com.niveales.wind.R;

/**
 * The class for purchases via Google Play
 * 
 * @author Nikolay Moskvin <moskvin@netcook.org>
 * 
 */
public class BillingActivity extends BaseActivity {
	private static final String TAG = "BillingActivity";
	private static final int CALLBACK_CODE = 101;
	private static final int BILLING_RESPONSE_RESULT_OK = 0;	
	private static final int BILLING_RESPONSE_RESULT_USER_CANCELED = 1;
	private static final int BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE = 3;
	private static final int BILLING_RESPONSE_RESULT_ITEM_UNAVAILABLE = 5;
	private static final int BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED = 7;
	private static final int CONNECTION_ALERT = 1;
	private static final int SERVER_ALERT = 2;

	private String fileName;
	private String title;
	private String subtitle;
	private String productId;
	private String productPrice;
	private String productTitle;
	private String dataResponse;
	private String signatureResponse;

	private Button buy;
	private Button cancel;
	private Button subsYear;
	private Button subsMonthly;

	private IInAppBillingService billingService;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.wait_bar);
		if(!isNetworkConnected()){
			showDialog(CONNECTION_ALERT);
		} else {
			bindService(
					new Intent(
							"com.android.vending.billing.InAppBillingService.BIND"), 
							mServiceConn, 
							Context.BIND_AUTO_CREATE);
			fileName = getIntent().getExtras().getString(DownloadActivity.FILE_NAME_KEY);
			title = getIntent().getExtras().getString(DownloadActivity.TITLE_KEY);
			subtitle = getIntent().getExtras().getString(DownloadActivity.SUBTITLE_KEY);
			int finId = fileName.indexOf("/");
			productId = fileName.substring(0,finId);
		}
	}

	private void initViews() {
		setContentView(R.layout.billing_activity);
		buy = (Button)findViewById(R.id.billing_buy_button);
		subsMonthly = (Button)findViewById(R.id.billing_subs_monthly);
		subsYear = (Button)findViewById(R.id.billing_subs_year);
		cancel = (Button)findViewById(R.id.billing_cancel_button);
		//
		cancel.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});
		//
		if(productPrice == null){
			buy.setVisibility(View.GONE);
		} else {
			buy.setText(productTitle + ": "+ productPrice);
			buy.setOnClickListener(getBuyOnClick());
		}
		//
		String abonnement = getResources().getString(R.string.abonnement_wind);
		String year = getResources().getString(R.string.year);
		String month = getResources().getString(R.string.month);
		if(LibrelioApplication.isEnableYearlySubs(getContext())){
			subsYear.setText("   " + abonnement + " 1 " + year + "   ");
			subsYear.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(MainMagazineActivity.REQUEST_SUBS);
					sendBroadcast(intent);
					finish();
				}
			});
		} else {
			subsYear.setVisibility(View.GONE);
		}
		if(LibrelioApplication.isEnableMonthlySubs(getContext())){
			subsMonthly.setText("   " + abonnement + " 1 " + month + "   ");
			subsMonthly.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Intent intent = new Intent(MainMagazineActivity.REQUEST_SUBS);
					sendBroadcast(intent);
					finish();
				}
			});
		} else {
			subsMonthly.setVisibility(View.GONE);
		}
	}

	private ServiceConnection mServiceConn = new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName name) {
			Log.d(TAG, "onServiceDisconnected");
			billingService = null;
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			
			Log.d(TAG, "onServiceConnected");
			billingService = IInAppBillingService.Stub.asInterface(service);
			new AsyncTask<String, String, Bundle>() {
				Bundle ownedItems = null;
				//int responseCons = -1;
				@Override
				protected Bundle doInBackground(String... params) {
					Bundle skuDetails = null;
					try {
						ArrayList<String> skuList = new ArrayList<String>();
						skuList.add(productId);
						Bundle querySkus = new Bundle();
						querySkus.putStringArrayList("ITEM_ID_LIST", skuList);
						//String purchaseToken = "inapp:"+getPackageName()+":android.test.purchased";
						//responseCons = billingService.consumePurchase(3, getPackageName(),purchaseToken);
						skuDetails = billingService.getSkuDetails(3, getPackageName(), "inapp", querySkus);
						ownedItems = billingService.getPurchases(3, getPackageName(), "inapp", null);
					} catch (RemoteException e) {
						Log.d(TAG, "InAppBillingService failed", e);
						return null;
					}
					return skuDetails;
				}
				@Override
				protected void onPostExecute(Bundle skuDetails) {
					//Log.d(TAG,"responseCons"+responseCons);
					//If item was purchase then download begin without open billing activity 
					int getPurchaseResponse = ownedItems.getInt("RESPONSE_CODE");
					if(getPurchaseResponse == 0){
						ArrayList<String> ownedSkus = ownedItems.getStringArrayList("INAPP_PURCHASE_ITEM_LIST");
						for(String s : ownedSkus){
							Log.d(TAG,"owned: "+s);
						}
						if(ownedSkus.contains(productId)){
							new DownloadFromTempURLTask().execute();
				            finish();
				            return;
						}
					}
					//
					int response = skuDetails.getInt("RESPONSE_CODE");
					if (response == 0) {
						ArrayList<String> responseList = skuDetails
								.getStringArrayList("DETAILS_LIST");
						for (String thisResponse : responseList) {
							JSONObject object = null;
							String sku = "";
							String price = "";
							try {
								object = new JSONObject(thisResponse);
								sku = object.getString("productId");
								price = object.getString("price");
								productTitle = object.getString("title");
							} catch (JSONException e) {
								Log.e(TAG, "getSKU details failed", e);
							}
							if (sku.equals(productId)) {
								productPrice = price;
							}
						}
					}
					initViews();
					super.onPostExecute(skuDetails);
				}
				
			}.execute();
	   }
	};

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {	
		Log.d(TAG,requestCode+" "+resultCode);
		Log.d(TAG,"data = "+data.getExtras().getString("INAPP_PURCHASE_DATA"));
		Log.d(TAG,"signature = "+data.getExtras().getString("INAPP_DATA_SIGNATURE"));
		
	   if (requestCode == CALLBACK_CODE) {    	
	      String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");
	        
	      if (resultCode == RESULT_OK&purchaseData!=null) {
	         try {
	        	 Log.d(TAG,"Succes!!!");
	            JSONObject jo = new JSONObject(purchaseData);
	            String sku = jo.getString("productId");
	            dataResponse = data.getExtras().getString("INAPP_PURCHASE_DATA");
	            signatureResponse = data.getExtras().getString("INAPP_DATA_SIGNATURE");
	            Log.d(TAG,"You have bought the " + sku + ". Excellent choice,adventurer!");
	            new DownloadFromTempURLTask().execute();
	          }
	          catch (JSONException e) {
	             Log.d(TAG,"Failed to parse purchase data.");
	             e.printStackTrace();
	          }
	      } 
	   } 
	   finish();
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case CONNECTION_ALERT:{
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			String message = getResources().getString(R.string.connection_failed);
			builder.setMessage(message).setPositiveButton(R.string.ok, new android.content.DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					finish();
				}
			});
			return builder.create();
		}
		case SERVER_ALERT:{
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			String message = getResources().getString(R.string.server_error);
			builder.setMessage(message).setPositiveButton(R.string.ok, new android.content.DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					//finish();
				}
			});
			return builder.create();
		}
		}
		return super.onCreateDialog(id);
	}

	@Override
	protected void onDestroy() {
		if (isNetworkConnected()) {
			unbindService(mServiceConn);
		}
		super.onDestroy();
	}

	private OnClickListener getBuyOnClick(){
		return new OnClickListener() {
			@Override
			public void onClick(View v) {
				new PurchaseTask().execute();
			}
		};
	}

	private boolean isNetworkConnected() {
		return LibrelioApplication.thereIsConnection(this);
	}

	private Context getContext(){
		return this;
	}

	private String buildQuery() {
		StringBuilder query = new StringBuilder(LibrelioApplication.getServerUrl());
		query.append("?product_id=")
			.append(productId)
			.append("&data=")
			.append(dataResponse)
			.append("&signature=")
			.append(signatureResponse)
			.append("&urlstring=")
			.append(LibrelioApplication.getClientName(getContext()))
			.append("/")
			.append(LibrelioApplication.getMagazineName(getContext()))
			.append("/")
			.append(fileName);
		return query.toString();
	}

	private class DownloadFromTempURLTask extends AsyncTask<Void, Void, Void>{
		HttpResponse res = null;
		@Override
		protected Void doInBackground(Void... params) {
			
			HttpClient httpclient = new DefaultHttpClient();
			String query = buildQuery();
			Log.d(TAG,"query = " + query);
			HttpGet httpget = new HttpGet(query);
			HttpParams params1 = httpclient.getParams();
			HttpClientParams.setRedirecting(params1, false);
			try {
				res = httpclient.execute(httpget);
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return null;
		}
		protected void onPostExecute(Void result) {
			String tempURL = null;
			for(Header h : res.getAllHeaders()){
				if(h.getName().equalsIgnoreCase("location")){
					tempURL = h.getValue();
				}
				Log.d(TAG, "res- name:" + h.getName() + "  val:" + h.getValue());
			}
			if(tempURL == null){
				Toast.makeText(getContext(), "Download failed", Toast.LENGTH_SHORT).show();
				finish();
				return;
			}
			Intent intent = new Intent(getContext(), DownloadActivity.class);
			intent.putExtra(DownloadActivity.FILE_NAME_KEY, fileName);
			intent.putExtra(DownloadActivity.SUBTITLE_KEY, subtitle);
			intent.putExtra(DownloadActivity.TITLE_KEY, title);
			intent.putExtra(DownloadActivity.IS_TEMP_KEY, true);
			intent.putExtra(DownloadActivity.IS_SAMPLE_KEY, false);
			intent.putExtra(DownloadActivity.TEMP_URL_KEY, tempURL);
			startActivity(intent);
		};
	}

	private class PurchaseTask extends AsyncTask<String, String, String>{
		Bundle buyIntentBundle = null;
		@Override
		protected String doInBackground(String... params) {
			try {
				buyIntentBundle = billingService.getBuyIntent(3, getPackageName(),
						   productId, "inapp", null);
			} catch (RemoteException e1) {
				Log.e(TAG,"Problem with getBuyIntent",e1);
			}
			return null;
		}
		protected void onPostExecute(String result) {
			int response = buyIntentBundle.getInt("RESPONSE_CODE");
			Log.d(TAG,"response = "+response);
			switch (response) {
			case BILLING_RESPONSE_RESULT_USER_CANCELED:
			case BILLING_RESPONSE_RESULT_BILLING_UNAVAILABLE:
			case BILLING_RESPONSE_RESULT_ITEM_UNAVAILABLE:
				Toast.makeText(getContext(), "Error!", Toast.LENGTH_LONG).show();
				return;
			}
			//
			if(response == BILLING_RESPONSE_RESULT_ITEM_ALREADY_OWNED){
				new DownloadFromTempURLTask().execute();
				return;
			} else if(response == BILLING_RESPONSE_RESULT_OK){
				PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");
				Log.d(TAG,"pendingIntent = "+pendingIntent);
				if(pendingIntent==null){
					Toast.makeText(getContext(), "Error!", Toast.LENGTH_LONG).show();
					return;
				}
				try {
					startIntentSenderForResult(pendingIntent.getIntentSender(),
							CALLBACK_CODE, new Intent(), Integer.valueOf(0), Integer.valueOf(0),
							Integer.valueOf(0));
				} catch (SendIntentException e) {
					Log.e(TAG,"Problem with startIntentSenderForResult",e);
				}
			}
			super.onPostExecute(result);
		}
	}
}

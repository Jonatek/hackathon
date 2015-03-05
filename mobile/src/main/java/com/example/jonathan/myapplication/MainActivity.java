package com.example.jonathan.myapplication;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.SyncStateContract;
import android.util.Log;

import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * Created by Jonathan on 05/03/2015.
 */
public class MainActivity extends Activity {
    public static final String LOG_TAG = "MainActivity";

    private static String mIP;
    public String ip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new GetSecurityTokenAsyncTask().execute();
    }

    private class GetSecurityTokenAsyncTask extends AsyncTask<Void, Void, String> {
        final String TAG = "GetSecurityToken";

        @Override
        protected String doInBackground(Void... params) {
            Log.d(TAG, TAG);

            // Create a new HttpClient and Post Header
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost("https://dev.bouyguestelecom.fr/security/token");

            try {
                // Add your data
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
                nameValuePairs.add(new BasicNameValuePair("appId", getString(R.string.appId)));
                nameValuePairs.add(new BasicNameValuePair("appSecret", getString(R.string.appSecret)));
                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                // Execute HTTP Post Request
                HttpResponse response = httpClient.execute(httpPost);
                StatusLine statusLine = response.getStatusLine();
                int statusCode = statusLine.getStatusCode();
                Log.d(TAG, "statusCode: " + statusCode);
                if (statusCode == 204) {
                    Header headerToken = response.getFirstHeader("x-token");
                    Log.d(TAG, "token: " + headerToken.getValue());
                    return headerToken.getValue();
                } else {
                    Log.e(TAG, "Failed to get token");
                    return null;
                }
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            if (result != null) {
                new GetSessionIdAsyncTask(getIP(), result, new SessionIdCallback() {
                    @Override
                    public void onResult(boolean result) {
                        if (result) {
                            // DO IT
                            class getEPG extends AsyncTask<Void, Void, Void> {
                                final String TAG = "getEPG";

                                @Override
                                protected Void doInBackground(Void... params) {
                                    Log.d(TAG, "doInBackgrousendKeyPressnd");
                                    StringBuilder builder = new StringBuilder();
                                    HttpClient client = new DefaultHttpClient();
                                    HttpGet httpGet = new HttpGet("http://openbbox.flex.bouyguesbox.fr:81/v0/media/epg/live");
                                    // HttpGet httpGet = new HttpGet(Constants.FLEX + Constants.EPG_LIVE);

                                    try {
                                        HttpResponse response = client.execute(httpGet);
                                        StatusLine statusLine = response.getStatusLine();
                                        int statusCode = statusLine.getStatusCode();
                                        if (statusCode == 200) {
                                            HttpEntity entity = response.getEntity();
                                            InputStream content = entity.getContent();
                                            BufferedReader reader = new BufferedReader(new InputStreamReader(content));
                                            String line;
                                            while ((line = reader.readLine()) != null) {
                                                builder.append(line);
                                            }
                                        } else {
                                            Log.e(TAG, "Failed to download file");
                                        }
                                        //Log.d(TAG, String.valueOf(builder));
                                    } catch (ClientProtocolException e) {
                                        e.printStackTrace();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    try {
                                        final JSONArray channelsJSONArray = new JSONArray(builder.toString());
                                        
                                        /*DataMap dataMap;
                                        PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(Constants.EPG_DATA);
                                        for (int i = 0; i < channelsJSONArray.length(); i++) {
                                            Log.d(TAG, "i=" + i);
                                            final JSONObject channelJSONObject = channelsJSONArray.getJSONObject(i);
                                            final JSONObject programInfo = channelJSONObject.getJSONObject("programInfo");
                                            dataMap = new DataMap();
                                            dataMap.putInt(Constants.EPG_ID, channelJSONObject.getInt("epgChannelNumber"));
                                            Log.d(TAG, programInfo.getString("shortTitle"));
                                            dataMap.putString(Constants.EPG_NAME, programInfo.getString("shortTitle"));
                                            if (programInfo.has("longSummary"))
                                                dataMap.putString(Constants.EPG_DESC, programInfo.getString("longSummary"));
                                            else if (programInfo.has("shortSummary"))
                                                dataMap.putString(Constants.EPG_DESC, programInfo.getString("shortSummary"));
                                            else dataMap.putString(Constants.EPG_DESC, "");
                                            dataMap.putString(Constants.EPG_STARTTIME, channelJSONObject.getString("startTime"));
                                            dataMap.putString(Constants.EPG_ENDTIME, channelJSONObject.getString("endTime"));
                                            if (channelJSONObject.has("media"))
                                                dataMap.putAsset(Constants.EPG_IMAGE, getAssetFromURL(channelJSONObject.getJSONArray("media").getJSONObject(0).getString("url")));
                                            else {
                                                dataMap.putAsset(Constants.EPG_IMAGE, null);
                                            }
                                            Log.d(TAG, "epgChannelNumber: " + channelJSONObject.getInt("epgChannelNumber"));
                                            dataMap.putInt(SyncStateContract.Constants.EPG_CHANNEL_NUMBER, channelJSONObject.getInt("epgChannelNumber"));
                                            putDataMapRequest.getDataMap().putDataMap(String.valueOf(i), dataMap);
                                        }
                                        putDataMapRequest.getDataMap().putDouble(SyncStateContract.Constants.EPG_TIMESTAMP, (System.currentTimeMillis())); // Pour que les données changent effectivement
                                        putDataMapRequest.getDataMap().putInt(SyncStateContract.Constants.EPG_COUNT, channelsJSONArray.length());
                                        PutDataRequest request = putDataMapRequest.asPutDataRequest();
                                        PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi
                                                .putDataItem(mGoogleApiClient, request);
                                        pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                                            @Override
                                            public void onResult(DataApi.DataItemResult dataItemResult) {
                                                Log.d(TAG, "onResult: " + dataItemResult.getStatus().toString());
                                            }
                                        });
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    } */
                                    return null;
                                }

                                public Asset getAssetFromURL(String src) {
                                    try {
                                        java.net.URL url = new java.net.URL("http://195.36.152.209" + src);
                                        HttpURLConnection connection = (HttpURLConnection) url
                                                .openConnection();
                                        connection.setDoInput(true);
                                        connection.connect();
                                        InputStream input = connection.getInputStream();
                                        return Asset.createFromBytes(InputStreamToByteArray(input));
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                        return null;
                                    }
                                }

                                private byte[] InputStreamToByteArray(InputStream is) throws IOException {
                                    ByteArrayOutputStream buffer = new ByteArrayOutputStream();

                                    int nRead;
                                    byte[] data = new byte[16384];

                                    while ((nRead = is.read(data, 0, data.length)) != -1) {
                                        buffer.write(data, 0, nRead);
                                    }

                                    buffer.flush();

                                    return buffer.toByteArray();
                                }
                            }
                        } else {
                            Log.d(TAG, "ERROR sessionId");
                        }
                    }
                }).execute();
            } else {
                Log.d(TAG, "ERROR get token");
            }
        }
    }

    public interface SessionIdCallback {
        public void onResult(boolean result);
    }

    private class GetSessionIdAsyncTask extends AsyncTask<Void, Void, Boolean> {
        final String TAG = "GetSessionId";
        private String ip;
        private String token;

        private SessionIdCallback mListener;

        public GetSessionIdAsyncTask(String ip, String token, SessionIdCallback mListener) {
            this.token = token;
            this.ip = ip;
            this.mListener = mListener;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // Create a new HttpClient and Post Header
            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost("http://" + ip + ":8080/api.bbox.lan/v0/security/sessionId");

            try {
                JSONObject jsonObjectToken = new JSONObject();
                try {
                    jsonObjectToken.put("token", token);
                    httpPost.setEntity(new ByteArrayEntity(jsonObjectToken.toString().getBytes("UTF8")));

                    // Execute HTTP Post Request
                    HttpResponse response = httpClient.execute(httpPost);
                    StatusLine statusLine = response.getStatusLine();
                    int statusCode = statusLine.getStatusCode();
                    Log.d(TAG, "statusCode: " + statusCode);
                    if (statusCode == 204) {
                        Header headerSessionId = response.getFirstHeader("x-sessionid");
                        ip = String.valueOf(headerSessionId);
                        Log.d(TAG, "headerSessionId: " + headerSessionId.getValue());
                        Log.d(TAG, "Test: " + ip);
                        return true;
                    } else {
                        Log.e(TAG, "Failed to get sessionId");
                        return false;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (mListener != null)
                mListener.onResult(result);
        }
    }

    public static String getIP() {
        try {
            for(Enumeration en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf =(NetworkInterface)  en.nextElement();
                for(Enumeration enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = (InetAddress) enumIpAddr.nextElement();
                    if(!inetAddress.isLoopbackAddress()&&inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
            return "";
        }

        return "";
    }
}

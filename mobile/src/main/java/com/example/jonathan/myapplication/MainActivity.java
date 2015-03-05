package com.example.jonathan.myapplication;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.example.jonathan.myapplication.R;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import fr.bouyguestelecom.tv.openapi.secondscreen.application.ApplicationsManager;
import fr.bouyguestelecom.tv.openapi.secondscreen.authenticate.IAuthCallback;
import fr.bouyguestelecom.tv.openapi.secondscreen.bbox.Bbox;
import fr.bouyguestelecom.tv.openapi.secondscreen.notification.NotificationManager;
import fr.bouyguestelecom.tv.openapi.secondscreen.notification.NotificationType;
import fr.bouyguestelecom.tv.openapi.secondscreen.notification.WebSocket;

/**
 * Created by Jonathan on 05/03/2015.
 */
public class MainActivity extends Activity {
    public static final String LOG_TAG = "MainActivity";

    private static String mIP;

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
                new GetSessionIdAsyncTask(getIP(getBaseContext()), result, new SessionIdCallback() {
                    @Override
                    public void onResult(boolean result) {
                        if (result) {
                            // DO IT
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
                    // adb shell am startservice -a "fr.bouyguestelecom.bboxapi.StartService" --user 0
                    HttpResponse response = httpClient.execute(httpPost);
                    StatusLine statusLine = response.getStatusLine();
                    int statusCode = statusLine.getStatusCode();
                    Log.d(TAG, "statusCode: " + statusCode);
                    if (statusCode == 204) {
                        Header headerSessionId = response.getFirstHeader("x-sessionid");
                        Log.d(TAG, "headerSessionId: " + headerSessionId.getValue());
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

    public static String getIP(Context context) {
        if (mIP != null) return mIP;
        SharedPreferences preference = PreferenceManager.getDefaultSharedPreferences(context);
        return preference.getString("bboxIP", null);
    }
}

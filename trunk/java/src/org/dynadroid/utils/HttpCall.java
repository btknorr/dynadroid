package org.dynadroid.utils;

import android.os.AsyncTask;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.dynadroid.Application;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: brianknorr
 * Date: Aug 27, 2010
 * Time: 7:02:38 PM
 * To change this template use File | Settings | File Templates.
 */
public class HttpCall {
    private static HttpClient httpClient;
    private static HashMap<String, String> headers = new HashMap();
    public static long DEFAULT_URL_CACHE_DURATION = 1000 * 60 * 60; //one hour

    public String result, url, data;
    public boolean lookInCache = true;
    public boolean offline = false;
    public long urlCacheDuration = DEFAULT_URL_CACHE_DURATION;

    public HttpCall() {

    }

    public HttpCall(String url) {
        this.url = url;
    }

    public HttpCall url(String url) {
        this.url = url;
        return this;
    }

    public HttpCall data(String data) {
        this.data = data;
        return this;
    }

    public HttpCall addHeader(String name, String value) {
        headers.put(name, value);
        return this;
    }

    public HttpCall urlCacheDuration(long durationInMillis) {
        this.urlCacheDuration = durationInMillis;
        return this;
    }

    public HttpCall lookInCache(boolean lookInCache) {
        this.lookInCache = lookInCache;
        return this;
    }

    public HttpCall get(final HttpDelegate httpDelegate) {
        final HttpCall me = this;
        new AsyncTask() {
            protected Object doInBackground(Object... objects) {
                doGet(me);
                return httpDelegate;
            }

            protected void onPostExecute(Object o) {
                //System.out.println("!!!!!!onPostExecute httpCall.result = " + me.result);
                ((HttpDelegate) o).completed(me);
            }
        }.execute(null);
        return this;
    }

    public HttpCall post(final HttpDelegate httpDelegate) {
        final HttpCall me = this;
        new AsyncTask() {
            protected Object doInBackground(Object... objects) {
                doPost(me);
                return httpDelegate;
            }

            protected void onPostExecute(Object o) {
                ((HttpDelegate) o).completed(me);
            }
        }.execute(null);
        return this;
    }

    public HttpCall post(String data, final HttpDelegate httpDelegate) {
        return data(data).post(httpDelegate);
    }

    protected static synchronized HttpClient httpClient() {
        if (httpClient == null) {
            httpClient = new DefaultHttpClient();
        }
        return httpClient;
    }

    protected static synchronized void doGet(HttpCall httpCall) {
        try {
            String result = httpCall.lookInCache ? UrlCache.get(httpCall.url) : null;
            if (result == null) {
                //System.out.println("!!!!!loading from network " + httpCall.url);
                if (!Application.isOnline()) {
                    httpCall.offline = true;
                } else {
                    HttpGet get = new HttpGet(httpCall.url);
                    for (String name : headers.keySet()) {
                        get.addHeader(name, headers.get(name));
                    }
                    HttpResponse response = httpClient().execute(get);
                    result = processEntity(response.getEntity());
                    //System.out.println("********Result" + result);
                    UrlCache.set(httpCall.url, result, httpCall.urlCacheDuration);
                    httpCall.offline = false;
                }
            } else {
                //System.out.println("!!!!!loading from CACHE " + httpCall.url);
            }
            httpCall.result = result;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected static synchronized void doPost(HttpCall httpCall) {
        try {
            if (!Application.isOnline()) {
                httpCall.offline = true;
            } else {
                HttpPost httpPost = new HttpPost(httpCall.url);
                for (String name : headers.keySet()) {
                    httpPost.addHeader(name, headers.get(name));
                }
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                nameValuePairs.add(new BasicNameValuePair("error", httpCall.data));
                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                HttpResponse response = httpClient().execute(httpPost);
                String result = processEntity(response.getEntity());
                httpCall.result = result;
                httpCall.offline = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static synchronized String processEntity(HttpEntity entity) throws IllegalStateException, IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(entity.getContent()));
        String line, result = "";
        while ((line = br.readLine()) != null) {
            result += line;
        }
        return result;
    }
}
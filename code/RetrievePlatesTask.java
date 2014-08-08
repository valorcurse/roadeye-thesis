package com.cgi.roadeye.android;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Vector;

/**
 * Created by valorcurse on 18-7-14.
 */
public class RetrievePlatesTask implements Runnable {
    String url = "http://valorcur.se/plates/data.js";
    Vector<String> platesInformation;

    public RetrievePlatesTask(Vector<String> platesInformation) {
        this.platesInformation = platesInformation;
    }

    @Override
    public void run() {
        try {
            JSONObject jsonObj = new JSONObject(getPlatesFromWebsite());

            JSONArray platesInfo = jsonObj.getJSONArray("plates");

            for (int i = 0; i < platesInfo.length(); i++) {
                JSONObject plateInfo = platesInfo.getJSONObject(i);
                String plateString = plateInfo.get("info").toString();

                // If plate info is not already stored
                if (platesInformation.indexOf(plateString) == -1)
                    platesInformation.add(plateString);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }


    }

    private String getPlatesFromWebsite() {

        HttpClient httpclient = new DefaultHttpClient();
        HttpResponse response = null;

        try {
            response = httpclient.execute(new HttpGet(url));
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (response == null) return "";

        StatusLine statusLine = response.getStatusLine();

        if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            try {
                response.getEntity().writeTo(out);
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return out.toString();

        } else {

            //Closes the connection.
            try {
                response.getEntity().getContent().close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                throw new IOException(statusLine.getReasonPhrase());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return "";
    }
}

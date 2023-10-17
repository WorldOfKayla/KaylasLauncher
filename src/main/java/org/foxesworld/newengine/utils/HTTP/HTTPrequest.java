package org.foxesworld.newengine.utils.HTTP;

import com.google.gson.Gson;
import org.foxesworld.newengine.APP;
import org.foxesworld.newengine.AppFrame;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

public class HTTPrequest {

    private String requestMethod;
    private AppFrame appFrame;

    public HTTPrequest(AppFrame appFrame, String requestMethod){
        this.appFrame = appFrame;
        appFrame.getLOGGER().debug("HTTP "+requestMethod+" init");
        this.requestMethod = requestMethod;
    }

    public String send(String URL, Map<String, String> parameters) {
        HttpURLConnection httpURLConnection = null;
        try {
            java.net.URL url = new URL(URL);
            this.appFrame.getLOGGER().debug(url);
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestMethod(this.requestMethod);
            this.setRequestProperties(httpURLConnection, "RequestProperties.json");
            httpURLConnection.setUseCaches(false);
            httpURLConnection.setDoInput(true);
            httpURLConnection.setDoOutput(true);
            httpURLConnection.connect();

            try (OutputStream os = httpURLConnection.getOutputStream()) {
                byte[] postDataBytes = this.formParams(parameters).toString().getBytes("UTF-8");
                os.write(postDataBytes);
            }

            InputStream is = httpURLConnection.getInputStream();
            StringBuilder response;
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            response = new StringBuilder();
            String line;
            while ((line = rd.readLine()) != null) {
                response.append(line);
            }

            String str = response.toString();
            System.out.println(str);
            return str;
        } catch (Exception e) {
            return null;
        } finally {
            if (httpURLConnection != null)
                httpURLConnection.disconnect();
        }
    }

    private StringBuilder getBoundary(int length){
        StringBuilder boundary = new StringBuilder();
        for(int k = 0; k < length; k++){
            boundary.append(Long.toString(new Random().nextLong(), 3));
        }
        return boundary;
    }

    private StringBuilder formParams(Map<String, String> parameters){
        StringBuilder postData = new StringBuilder();
        for (Map.Entry<String, String> param : parameters.entrySet()) {
            if (postData.length() != 0) {
                postData.append('&');
            }
            postData.append(param.getKey());
            postData.append('=');
            postData.append(param.getValue());
        }
        return postData;
    }

    private void setRequestProperties(HttpURLConnection httpURLConnection, String propertyPath) {
        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(propertyPath);
        if (inputStream != null) {
            InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
            RequestProperties[] requestProperties = new Gson().fromJson(reader, RequestProperties[].class);
            for (RequestProperties requestHeader : requestProperties) {
                String value = requestHeader.getPropertyValue;
                if (value.contains("{$boundary}")) {
                    value = value.replace("{$boundary}", this.getBoundary(3));
                }
                httpURLConnection.setRequestProperty(requestHeader.propertyKey, value);
            }
        }
    }

    private  void requestProperties(HttpURLConnection httpURLConnection){
        Map<String, List<String>> requestProperties = httpURLConnection.getRequestProperties();

        for (Map.Entry<String, List<String>> entry : requestProperties.entrySet()) {
            String headerName = entry.getKey();
            List<String> headerValues = entry.getValue();
            System.out.println(headerName + ": " + headerValues);
        }
    }
}

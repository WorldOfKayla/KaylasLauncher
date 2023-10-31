package org.foxesworld.engine.utils.HTTP;

import org.foxesworld.engine.Engine;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class HTTPrequest {

    private String requestMethod;
    private Engine engine;

    public HTTPrequest(Engine engine, String requestMethod) {
        this.engine = engine;
        engine.getLOGGER().debug("HTTP " + requestMethod + " init");
        this.requestMethod = requestMethod;
    }

    public String send(Map<String, String> parameters) {
        HttpURLConnection httpURLConnection = null;
        try {
            java.net.URL url = new URL(engine.getEngineData().bindUrl);
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestMethod(this.requestMethod);
            this.setRequestProperties(httpURLConnection, engine.getEngineData().requestProperties);
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
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            response = new StringBuilder();
            String line;
            while ((line = rd.readLine()) != null) {
                response.append(line);
            }

            return response.toString();
        } catch (Exception e) {
            return null;
        } finally {
            if (httpURLConnection != null)
                httpURLConnection.disconnect();
        }
    }

    private StringBuilder getBoundary(int length) {
        StringBuilder boundary = new StringBuilder();
        for (int k = 0; k < length; k++) {
            boundary.append(Long.toString(new Random().nextLong(), 3));
        }
        return boundary;
    }

    private StringBuilder formParams(Map<String, String> parameters) {
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

    private void setRequestProperties(HttpURLConnection httpURLConnection, List<RequestProperty> properties) {
        for(RequestProperty requestProperty: properties){
            String value = requestProperty.propertyValue;
            if (value.contains("{$boundary}")) {
                value = value.replace("{$boundary}", this.getBoundary(3));
            }
            httpURLConnection.setRequestProperty(requestProperty.propertyKey, value);
            engine.getLOGGER().debug("Adding request header " + requestProperty.propertyKey);
        }
    }
}

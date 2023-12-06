package org.foxesworld.engine.utils.HTTP;

import org.foxesworld.engine.Engine;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class HTTPrequest {

    private final String requestMethod;
    private final Engine engine;
    private HttpURLConnection httpURLConnection = null;

    public HTTPrequest(Engine engine, String requestMethod) {
        this.engine = engine;
        engine.getLOGGER().debug("HTTP " + requestMethod + " init");
        this.requestMethod = requestMethod;
    }

    public String send(String queryUrl, Map<String, String> parameters) {
        try {
            URL url = new URL(queryUrl);
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setRequestMethod(this.requestMethod);
            this.setRequestProperties(httpURLConnection, engine.getEngineData().getRequestProperties());
            httpURLConnection.setUseCaches(false);
            httpURLConnection.setDoInput(true);
            httpURLConnection.setDoOutput(true);
            httpURLConnection.connect();

            try (OutputStream os = httpURLConnection.getOutputStream()) {
                byte[] postDataBytes = this.formParams(parameters).toString().getBytes(StandardCharsets.UTF_8);
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

    private StringBuilder getBoundary(int length, int radix) {
        StringBuilder boundary = new StringBuilder();
        for (int k = 0; k < length; k++) {
            boundary.append(Long.toString(new Random().nextLong(), radix));
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

    public void setRequestProperties(HttpURLConnection httpURLConnection, List<RequestProperty> properties) {
        for(RequestProperty requestProperty: properties){
            String value = requestProperty.propertyValue;
            if (value.contains("{$boundary}")) {
                value = value.replace("{$boundary}", this.getBoundary(3, 3));
            }
            if(!httpURLConnection.getRequestProperties().containsKey(requestProperty.propertyKey)) {
                httpURLConnection.setRequestProperty(requestProperty.propertyKey, value);
                //engine.getLOGGER().debug("Adding request header " + requestProperty.propertyKey);
            }

        }
    }

    public HttpURLConnection getHttpURLConnection() {
        return httpURLConnection;
    }
}

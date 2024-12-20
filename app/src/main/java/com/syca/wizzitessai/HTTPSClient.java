package com.syca.wizzitessai;

import okhttp3.*;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;

public class HTTPSClient {

    public static HTTPResponse httpsJSONRequest(String url, Map<String, Object> params, Map<String, String> headers) {
        // Initialize the HTTPResponse
        HTTPResponse httpResponse = new HTTPResponse(false, null, -1, null, null);

        // If no headers are passed, use an empty map
        if (headers == null) {
            headers = Map.of();
        }

        // Create a new thread for the HTTP request
        Map<String, String> finalHeaders = headers;
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Construct the JSON object from params
                    JSONObject json = new JSONObject();
                    for (Map.Entry<String, Object> entry : params.entrySet()) {
                        json.put(entry.getKey(), entry.getValue());
                    }

                    // Create MediaType for JSON
                    MediaType JSON = MediaType.parse("application/json; charset=utf-8");
                    String jsonString = json.toString();

                    // Convert headers Map to Headers
                    Headers.Builder headerBuilder = new Headers.Builder();
                    for (Map.Entry<String, String> entry : finalHeaders.entrySet()) {
                        headerBuilder.add(entry.getKey(), entry.getValue());
                    }
                    Headers okhttp3Headers = headerBuilder.build();

                    // Initialize OkHttpClient
                    OkHttpClient client = new OkHttpClient.Builder().build();

                    // Create request with JSON body
                    RequestBody body = RequestBody.create(jsonString, JSON);
                    Request request = new Request.Builder()
                            .url(url)
                            .headers(okhttp3Headers)
                            .post(body)
                            .build();

                    // Execute the request
                    Response res = client.newCall(request).execute();

                    // Parse response
                    String bodyString = res.body() != null ? res.body().string() : null;
                    if (res.code() != 200) {
                        httpResponse.setHasException(true);
                    }
                    httpResponse.setStatusCode(res.code());
                    httpResponse.setHeaders(res.headers().toMultimap());
                    httpResponse.setContent(bodyString);
                } catch (Exception ex) {
                    // Handle exceptions
                    httpResponse.setHasException(true);
                    httpResponse.setException(ex);
                }
            }
        });

        t.start(); // Start the thread

        try {
            t.join(); // Wait for the thread to finish
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return httpResponse;
    }
}

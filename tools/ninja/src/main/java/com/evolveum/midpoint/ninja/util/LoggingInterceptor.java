package com.evolveum.midpoint.ninja.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by Viliam Repan (lazyman).
 */
public class LoggingInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingInterceptor.class);

    private AtomicLong counter = new AtomicLong(0);

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {

        long number = counter.incrementAndGet();

        logRequest(number, request, body);

        ClientHttpResponse clientHttpResponse = execution.execute(request, body);

        traceResponse(number, clientHttpResponse);

        return clientHttpResponse;
    }

    private void logRequest(long number, HttpRequest request, byte[] body) throws IOException {
        StringBuilder builder = new StringBuilder();

        builder.append("ID: ").append(number).append('\n');
        builder.append("Request: ").append(request.getMethod()).append(' ').append(request.getURI()).append('\n');
        builder.append("Headers: ").append(request.getHeaders()).append('\n');
        builder.append("Payload: ").append(getRequestBody(body));

        LOG.debug("Outbound message\n---------------------\n{}\n---------------------", builder.toString());
    }

    private String getRequestBody(byte[] body) {
        if (body == null || body.length == 0) {
            return "";
        }

        return new String(body, StandardCharsets.UTF_8);
    }

    private void traceResponse(long number, ClientHttpResponse response) throws IOException {
        String body = getBodyString(response);

        StringBuilder builder = new StringBuilder();
        builder.append("ID: ").append(number).append('\n');
        builder.append("Status: ").append(response.getStatusCode()).append(' ').append(response.getStatusText()).append('\n');
        builder.append("Headers: ").append(response.getHeaders()).append('\n');
        builder.append("Payload: ").append(body);

        LOG.debug("Inbound message\n---------------------\n{}\n---------------------", builder.toString());
    }

    private String getBodyString(ClientHttpResponse response) {
        try {
            if (response != null && response.getBody() != null) {
                StringBuilder builder = new StringBuilder();

                BufferedReader reader = new BufferedReader(new InputStreamReader(response.getBody(), StandardCharsets.UTF_8));
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                    builder.append('\n');
                }
                return builder.toString();
            } else {
                return "";
            }
        } catch (IOException e) {
            return "";
        }
    }
}

package com.evolveum.midpoint.ninja.util;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.Base64;

/**
 * Created by Viliam Repan (lazyman).
 */
public class BasicAuthenticationInterceptor implements ClientHttpRequestInterceptor {

    private String username;
    private String password;

    public BasicAuthenticationInterceptor(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest httpRequest, byte[] bytes, ClientHttpRequestExecution clientHttpRequestExecution) throws IOException {
        HttpHeaders headers = httpRequest.getHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, encodeCredentialsForBasicAuth(username, password));

        return clientHttpRequestExecution.execute(httpRequest, bytes);
    }

    public static String encodeCredentialsForBasicAuth(String username, String password) {
        username = username != null ? username : "";
        password = password != null ? password : "";

        return "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
    }
}

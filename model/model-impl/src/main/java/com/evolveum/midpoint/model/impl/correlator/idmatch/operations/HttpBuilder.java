package com.evolveum.midpoint.model.impl.correlator.idmatch.operations;

import com.evolveum.midpoint.model.impl.correlator.idmatch.operations.auth.AuthenticationProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.IOException;

public class HttpBuilder {
    AuthenticationProvider authenticationProvider;
    CloseableHttpClient httpClient;

    public HttpBuilder(AuthenticationProvider authenticationProvider) {
        this.authenticationProvider = authenticationProvider;
    }

    public HttpClient httpClient() {

         httpClient = HttpClientBuilder.create()
                .setDefaultCredentialsProvider(authenticationProvider.provider())
                .build();

        return httpClient;

    }

    public void clientClose(){
        try {
            httpClient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

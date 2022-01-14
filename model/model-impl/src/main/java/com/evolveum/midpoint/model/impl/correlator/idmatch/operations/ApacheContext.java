package com.evolveum.midpoint.model.impl.correlator.idmatch.operations;


import com.evolveum.midpoint.model.impl.correlator.idmatch.operations.auth.AuthenticationProvider;

import java.io.IOException;


public class ApacheContext {

    public ApacheApiRequest apacheApiRequest;

    public ApacheContext(ApacheApiRequest apacheApiRequest) {
        this.apacheApiRequest = apacheApiRequest;
    }

    public void executeRequest(AuthenticationProvider authenticationProvider, String urlPrefix, String jsonString, String urlSuffix){
        try {
            apacheApiRequest.doRequest(urlPrefix,jsonString,urlSuffix);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

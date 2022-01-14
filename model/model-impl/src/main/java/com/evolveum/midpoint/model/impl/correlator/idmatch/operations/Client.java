package com.evolveum.midpoint.model.impl.correlator.idmatch.operations;

import com.evolveum.midpoint.model.impl.correlator.idmatch.constants.Channel;
import com.evolveum.midpoint.model.impl.correlator.idmatch.constants.MatchStatus;
import com.evolveum.midpoint.model.impl.correlator.idmatch.data.ListResponse;
import com.evolveum.midpoint.model.impl.correlator.idmatch.operations.auth.AuthenticationProvider;

import java.io.IOException;

public class Client {


    private final ApacheApiRequest apachePutRequest;
    private final ApacheApiRequest apachePostRequest;
    private final ApacheApiRequest apacheGetRequest;
    private final ApacheApiRequest apacheDeleteRequest;

    private final HttpClientSuper httpClientSuper;

    String urlPrefix;
    String username;
    String password;

    AuthenticationProvider authenticationProvider;

    ListResponse listResponse;

    String NO_RESPONSE_MESSAGES = "NO RESPONSE MESSAGES";

    public Client(String urlPrefix, String username, String password) {
        this.urlPrefix = urlPrefix;
        this.username = username;
        this.password = password;
        this.authenticationProvider = new AuthenticationProvider(username, password);

        apachePutRequest = new ApachePutRequest(this.authenticationProvider);
        apachePostRequest = new ApachePostRequest(this.authenticationProvider);
        apacheGetRequest = new ApacheGetRequest(this.authenticationProvider);
        apacheDeleteRequest = new ApacheDeleteRequest(this.authenticationProvider);
        httpClientSuper = new HttpClientSuper(this.authenticationProvider);
    }


    public void peoplePut(String sorLabel, String sorId, String object) {

        StringBuilder urlSuffix = new StringBuilder(sorLabel + "/" + sorId);
        StringBuilder url = new StringBuilder(urlPrefix + Channel.URL_PREFIX_MAIN_OPERATIONS.getUrl());


        try {
            apachePutRequest.doRequest(url.toString(), urlSuffix.toString(), object);
            printResponses(apachePutRequest);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }




    public void listPeople(String sorLabel) {

        StringBuilder url = new StringBuilder(urlPrefix + Channel.URL_PREFIX_MAIN_OPERATIONS.getUrl());
        try {
            apacheGetRequest.doRequest(url.toString(), sorLabel, "");
            printResponses(apacheGetRequest);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void peopleById(String sorLabel, String sorId) {

        StringBuilder url = new StringBuilder(urlPrefix + Channel.URL_PREFIX_MAIN_OPERATIONS.getUrl());
        StringBuilder urlSuffix = new StringBuilder(sorLabel + "/" + sorId);

        try {
            apacheGetRequest.doRequest(url.toString(), urlSuffix.toString(), "");
            printResponses(apacheGetRequest);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void listMatchRequest(MatchStatus matchStatus) {

        StringBuilder urlSuffix = new StringBuilder(matchStatus.getUrl());

        try {
            apacheGetRequest.doRequest(urlPrefix, urlSuffix.toString(), "");
            printResponses(apacheGetRequest);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public void getMatchRequest(String id) {

        StringBuilder url = new StringBuilder(urlPrefix + Channel.URL_PREFIX_GET_MATCH_REQUEST_MATCH_ID.getUrl());

        try {
            apacheGetRequest.doRequest(url.toString(), id, "");
            printResponses(apacheGetRequest);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void searchMatchRequestsByReferenceId(String refId) {
        StringBuilder url = new StringBuilder(urlPrefix + Channel.URL_PREFIX_GET_MATCH_REQUEST_REFERENCE_ID.getUrl());

        try {
            apacheGetRequest.doRequest(url.toString(), refId, "");
            printResponses(apacheGetRequest);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void peoplePost(String sorLabel, String sorId, String object) {

        StringBuilder urlSuffix = new StringBuilder(sorLabel + "/" + sorId);
        StringBuilder url = new StringBuilder(urlPrefix + Channel.URL_PREFIX_MAIN_OPERATIONS.getUrl());
        try {
            apachePostRequest.doRequest(url.toString(), urlSuffix.toString(), object);
            printResponses(apachePostRequest);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void deletePeople(String sorLabel, String sorId) {

        StringBuilder urlSuffix = new StringBuilder(sorLabel + "/" + sorId);
        StringBuilder url = new StringBuilder(urlPrefix + Channel.URL_PREFIX_MAIN_OPERATIONS.getUrl());
        try {
            apacheDeleteRequest.doRequest(url.toString(), urlSuffix.toString(), "");
            printResponses(apacheDeleteRequest);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void printResponses(ApacheApiRequest apacheApiRequest) {
        if (!apacheApiRequest.listResponse().isEmpty()) {
            listResponse = apacheApiRequest.listResponse().get(0);
            System.out.printf("%s\n %s \n %s \n %n", "Response code: " + listResponse.getResponseCode(), "Message: " + listResponse.getMessage(), "Entity: " + listResponse.getEntity());
        } else System.out.println(NO_RESPONSE_MESSAGES);
    }


    public void close() {
        httpClientSuper.clientClose();
    }







}

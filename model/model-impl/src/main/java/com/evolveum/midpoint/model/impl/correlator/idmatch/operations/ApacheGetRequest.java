package com.evolveum.midpoint.model.impl.correlator.idmatch.operations;

import com.evolveum.midpoint.model.impl.correlator.idmatch.data.ListResponse;
import com.evolveum.midpoint.model.impl.correlator.idmatch.operations.auth.AuthenticationProvider;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class ApacheGetRequest extends HttpClientSuper implements ApacheApiRequest {


    List<ListResponse> httpResponse = new ArrayList<>();

    public ApacheGetRequest(AuthenticationProvider authenticationProvider) {
        super(authenticationProvider);
    }


    @Override
    public void doRequest(String urlPrefix, String urlSuffix, String jsonString) throws IOException {

        HttpGet request = new HttpGet(urlPrefix + urlSuffix);

        ResponseHandler<List<ListResponse>> responseHandler = new ApacheResponseHandler();
        setHttpResponse(httpClient().execute(request, responseHandler));

    }


    @Override
    public List<ListResponse> listResponse() {
        return httpResponse;
    }


    public void setHttpResponse(List<ListResponse> httpResponse) {
        this.httpResponse = httpResponse;
    }

}

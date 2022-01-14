package com.evolveum.midpoint.model.impl.correlator.idmatch.operations;

import com.evolveum.midpoint.model.impl.correlator.idmatch.data.ListResponse;
import com.evolveum.midpoint.model.impl.correlator.idmatch.operations.auth.AuthenticationProvider;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class ApachePutRequest extends HttpClientSuper implements ApacheApiRequest {


    List<ListResponse> httpResponse = new ArrayList<>();

    public ApachePutRequest(AuthenticationProvider authenticationProvider) {
        super(authenticationProvider);
    }


    @Override
    public void doRequest(String channel, String urlSuffix, String jsonString) throws IOException {

        HttpPut request = new HttpPut(channel + urlSuffix);
        request.addHeader("content-type", "application/json");
        request.setEntity(new StringEntity(jsonString));

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

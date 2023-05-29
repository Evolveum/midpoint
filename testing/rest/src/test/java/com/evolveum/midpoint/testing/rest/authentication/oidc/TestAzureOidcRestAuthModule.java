/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.rest.authentication.oidc;

import java.io.File;
import java.util.Set;

import com.microsoft.aad.msal4j.IAuthenticationResult;
import com.microsoft.aad.msal4j.PublicClientApplication;
import com.microsoft.aad.msal4j.UserNamePasswordParameters;
import jakarta.ws.rs.core.MediaType;
import org.apache.cxf.jaxrs.client.WebClient;
import org.springframework.beans.factory.annotation.Autowired;

import com.evolveum.midpoint.common.rest.MidpointAbstractProvider;
import com.evolveum.midpoint.common.rest.MidpointJsonProvider;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;

public class TestAzureOidcRestAuthModule extends TestAbstractOidcRestModule {

    private static final String AUTH_SERVER_URL_KEY= "authServerUrl";
    private static final String EMAIL_SUFFIX_KEY= "emailSuffix";
    private static final String SERVER_PREFIX = "azure";

    private static final String OPAQUE_TOKEN_CLIENT_ID_KEY = "opaqueToken." + CLIENT_ID_KEY;

    public static final File ADMIN_USER =
            new File(BASE_AUTH_REPO_DIR, "user-admin-azure.xml");

    @Override
    public void initSystem() throws Exception {
        super.initSystem();
        addObject(ADMIN_USER);
    }

    @Autowired
    protected MidpointJsonProvider jsonProvider;

    private PublicClientApplication pca;
    private PublicClientApplication pcaForOpaqueToken;


    @Override
    protected String getAcceptHeader() {
        return MediaType.APPLICATION_JSON;
    }

    @Override
    protected String getContentType() {
        return MediaType.APPLICATION_JSON;
    }

    @Override
    protected MidpointAbstractProvider getProvider() {
        return jsonProvider;
    }

    @Override
    public void initSystem(Task initTask, OperationResult result) throws Exception {
        super.initSystem(initTask, result);


        this.pca = PublicClientApplication.builder(getProperty(CLIENT_ID_KEY))
                .authority(getProperty(AUTH_SERVER_URL_KEY))
                .build();
        this.pcaForOpaqueToken = PublicClientApplication.builder(getProperty(OPAQUE_TOKEN_CLIENT_ID_KEY))
                .authority(getProperty(AUTH_SERVER_URL_KEY))
                .build();
    }

    @Override
    protected String getServerPrefix() {
        return SERVER_PREFIX;
    }

    private WebClient prepareClient(PublicClientApplication pca, String... scopes) {
        UserNamePasswordParameters parameters =
                UserNamePasswordParameters
                .builder(Set.of(scopes),
                        USER_ADMINISTRATOR_USERNAME + "@" + getProperty(EMAIL_SUFFIX_KEY),
                        getUserPasssword().toCharArray())
                .build();
        IAuthenticationResult result = pca.acquireToken(parameters).join();

        WebClient client = prepareClient("bearer", result.accessToken());
        client.path("/users/" + SystemObjectsType.USER_ADMINISTRATOR.value());
        return client;
    }

    @Override
    protected WebClient prepareClient() {
        return prepareClient(pca, "api://" + getProperty(CLIENT_ID_KEY) + "/rest-user/ReadUser");
    }

    @Override
    protected WebClient prepareClientForOpaqueToken() {
        return prepareClient(pcaForOpaqueToken, "user.read", "openid", "profile");
    }

    @Override
    protected String getNameOfUsernameClaim() {
        return "name";
    }
}

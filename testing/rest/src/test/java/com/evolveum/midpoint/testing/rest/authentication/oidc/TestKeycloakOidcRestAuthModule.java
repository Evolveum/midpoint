/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.rest.authentication.oidc;

import com.evolveum.midpoint.common.rest.MidpointAbstractProvider;
import com.evolveum.midpoint.common.rest.MidpointJsonProvider;
import com.evolveum.midpoint.repo.cache.global.GlobalQueryCache;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.poi.util.ReplacingInputStream;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.representations.AccessTokenResponse;
import org.springframework.beans.factory.annotation.Autowired;

import jakarta.ws.rs.core.MediaType;
import java.io.File;
import java.io.FileInputStream;

public class TestKeycloakOidcRestAuthModule extends TestAbstractOidcRestModule {

    private static final Trace LOGGER = TraceManager.getTrace(GlobalQueryCache.class);

    private static final File KEYCLOAK_CONFIGURATION = new File(BASE_AUTHENTICATION_DIR, "keycloak.json");

    private static final String AUTH_SERVER_URL_KEY= "authServerUrl";
    private static final String SERVER_PREFIX = "keycloak";

    @Autowired
    protected MidpointJsonProvider jsonProvider;

    private AuthzClient authzClient;

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
        ReplacingInputStream is1 = new ReplacingInputStream(
                new FileInputStream(KEYCLOAK_CONFIGURATION),
                createTag(AUTH_SERVER_URL_KEY),
                getProperty(AUTH_SERVER_URL_KEY));

        ReplacingInputStream is2 = new ReplacingInputStream(
                is1,
                createTag(CLIENT_ID_KEY),
                getProperty(CLIENT_ID_KEY));

        ReplacingInputStream is3 = new ReplacingInputStream(
                is2,
                createTag(CLIENT_SECRET_KEY),
                getProperty(CLIENT_SECRET_KEY));

        authzClient = AuthzClient.create(is3);
    }

    @Override
    protected String getServerPrefix() {
        return SERVER_PREFIX;
    }

    private AuthzClient getAuthzClient() {
        return authzClient;
    }

    @Override
    protected WebClient prepareClient() {
        AccessTokenResponse result = getAuthzClient().obtainAccessToken(
                USER_ADMINISTRATOR_USERNAME,
                getUserPasssword());

        WebClient client = prepareClient(result.getTokenType(), result.getToken());
        client.path("/users/" + SystemObjectsType.USER_ADMINISTRATOR.value());
        return client;
    }
}

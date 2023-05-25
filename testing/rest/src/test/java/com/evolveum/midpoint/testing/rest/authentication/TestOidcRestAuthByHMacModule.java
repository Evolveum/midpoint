/*
 * Copyright (c) 2016-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.rest.authentication;

import com.evolveum.midpoint.common.rest.MidpointAbstractProvider;
import com.evolveum.midpoint.common.rest.MidpointJsonProvider;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.testing.rest.RestServiceInitializer;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.apache.cxf.jaxrs.client.WebClient;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.representations.AccessTokenResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.File;
import java.io.FileInputStream;

import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

public class TestOidcRestAuthByHMacModule extends TestAbstractOidcRestModule {

    private static final File KEYCLOAK_HMAC_CONFIGURATION = new File(BASE_AUTHENTICATION_DIR, "keycloak-hmac.json");

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
        authzClient = AuthzClient.create(new FileInputStream(KEYCLOAK_HMAC_CONFIGURATION));
    }

    public AuthzClient getAuthzClient() {
        return authzClient;
    }

    protected void assertForAuthByPublicKey(Response response) {
        assertUnsuccess(response);
    }

    @Override
    protected void assertForAuthByHMac(Response response) {
        assertSuccess(response);
    }
}

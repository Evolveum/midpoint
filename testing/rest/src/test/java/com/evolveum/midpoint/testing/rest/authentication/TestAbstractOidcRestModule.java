/*
 * Copyright (c) 2016-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.rest.authentication;

import com.evolveum.midpoint.common.rest.MidpointAbstractProvider;
import com.evolveum.midpoint.common.rest.MidpointJsonProvider;
import com.evolveum.midpoint.model.common.SystemObjectCache;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.cache.RepositoryCache;
import com.evolveum.midpoint.repo.cache.global.GlobalObjectCache;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.testing.rest.RestServiceInitializer;

import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityPolicyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.apache.cxf.jaxrs.client.WebClient;
import org.keycloak.authorization.client.AuthzClient;
import org.keycloak.representations.AccessTokenResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterTest;
import org.testng.annotations.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;

import static org.testng.AssertJUnit.assertNotNull;

public abstract class TestAbstractOidcRestModule extends TestAbstractAuthentication {

    public static final String USER_ADMINISTRATOR_USERNAME = "administrator";
    public static final String USER_ADMINISTRATOR_PASSWORD = "secret";

    public static final File SECURITY_POLICY_ISSUER_URI = new File(BASE_REPO_DIR, "security-policy-issuer-uri.xml");
    public static final File SECURITY_POLICY_JWS_URI = new File(BASE_REPO_DIR, "security-policy-jws-uri.xml");
    public static final File SECURITY_POLICY_JWS_URI_WRONG_ALG = new File(BASE_REPO_DIR, "security-policy-jws-uri-wrong-alg.xml");
    public static final File SECURITY_POLICY_PUBLIC_KEY = new File(BASE_REPO_DIR, "security-policy-public-key.xml");
    public static final File SECURITY_POLICY_PUBLIC_KEY_WRONG_ALG = new File(BASE_REPO_DIR, "security-policy-public-key-wrong-alg.xml");
    public static final File SECURITY_POLICY_SYMMETRIC_KEY = new File(BASE_REPO_DIR, "security-policy-symmetric-key.xml");
    public static final File SECURITY_POLICY_SYMMETRIC_KEY_WRONG_KEY = new File(BASE_REPO_DIR, "security-policy-symmetric-key-wrong-alg.xml");

    public abstract AuthzClient getAuthzClient();

    @Test
    public void oidcAuthByIssuerUriTest() throws Exception {
        replaceSecurityPolicy(SECURITY_POLICY_ISSUER_URI);

        WebClient client = prepareClient();

        when();
        Response response = client.get();

        then();
        assertForAuthByPublicKey(response);
    }

    @Test
    public void oidcAuthByJWSUriTest() throws Exception {
        replaceSecurityPolicy(SECURITY_POLICY_JWS_URI);

        WebClient client = prepareClient();

        when();
        Response response = client.get();

        then();
        assertForAuthByPublicKey(response);
    }

    @Test
    public void oidcAuthByJWSUriWithWrongAlgTest() throws Exception {
        replaceSecurityPolicy(SECURITY_POLICY_JWS_URI_WRONG_ALG);

        WebClient client = prepareClient();

        when();
        Response response = client.get();

        then();
        assertUnsuccess(response);
    }

    @Test
    public void oidcAuthByPublicKeyTest() throws Exception {
        replaceSecurityPolicy(SECURITY_POLICY_PUBLIC_KEY);

        WebClient client = prepareClient();

        when();
        Response response = client.get();

        then();
        assertForAuthByPublicKey(response);
    }

    @Test
    public void oidcAuthByPublicKeyWithWrongAlgTest() throws Exception {
        replaceSecurityPolicy(SECURITY_POLICY_PUBLIC_KEY_WRONG_ALG);

        WebClient client = prepareClient();

        when();
        Response response = client.get();

        then();
        assertUnsuccess(response);
    }

    @Test
    public void oidcAuthBySymmetricKey() throws Exception {
        replaceSecurityPolicy(SECURITY_POLICY_SYMMETRIC_KEY);

        WebClient client = prepareClient();

        when();
        Response response = client.get();

        then();
        assertForAuthByHMac(response);
    }

    @Test
    public void oidcAuthBySymmetricKeyWithWrongAlgTest() throws Exception {
        replaceSecurityPolicy(SECURITY_POLICY_SYMMETRIC_KEY_WRONG_KEY);

        WebClient client = prepareClient();

        when();
        Response response = client.get();

        then();
        assertUnsuccess(response);
    }

    protected abstract void assertForAuthByPublicKey(Response response);
    protected abstract void assertForAuthByHMac(Response response);

    private WebClient prepareClient() {
        AccessTokenResponse result = getAuthzClient().obtainAccessToken(USER_ADMINISTRATOR_USERNAME, USER_ADMINISTRATOR_PASSWORD);

        WebClient client = prepareClient(result.getTokenType(), result.getToken());
        client.path("/users/" + SystemObjectsType.USER_ADMINISTRATOR.value());
        return client;
    }

    protected void createAuthorizationHeader(WebClient client, String username, String password) {
        if (username != null) {
            String authorizationHeader = username + " " + password;
            client.header("Authorization", authorizationHeader);
        }
    }

    protected void assertSuccess(Response response) {
        assertStatus(response, 200);
        UserType userType = response.readEntity(UserType.class);
        assertNotNull("Returned entity in body must not be null.", userType);
        logger.info("Returned entity: {}", userType.asPrismObject().debugDump());

        displayDumpable("Audit", getDummyAuditService());
        getDummyAuditService().assertRecords(2);
        getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
    }

    protected void assertUnsuccess(Response response) {
        assertStatus(response, 401);

        displayDumpable("Audit", getDummyAuditService());
        getDummyAuditService().assertRecords(1);
        getDummyAuditService().assertFailedLogin(SchemaConstants.CHANNEL_REST_URI);
    }
}

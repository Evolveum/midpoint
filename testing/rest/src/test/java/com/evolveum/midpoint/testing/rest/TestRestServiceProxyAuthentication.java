/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.rest;

import static org.testng.AssertJUnit.assertNotNull;

import java.io.File;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.common.rest.MidpointAbstractProvider;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author skublik
 */
public class TestRestServiceProxyAuthentication extends RestServiceInitializer {

    // REST and end user authorization
    public static final File USER_EGOIST_FILE = new File(BASE_REPO_DIR, "user-egoist.xml");
    public static final String USER_EGOIST_OID = "b6f3e3c8-d48b-11e4-8d88-001e8c717e5b";
    public static final String USER_EGOIST_USERNAME = "egoist";
    public static final String USER_EGOIST_PASSWORD = "OnlyMypassw0rd";

    // REST and full authorization but not switchable
    public static final File USER_HEAD_FILE = new File(BASE_REPO_DIR, "user-head.xml");
    public static final String USER_HEAD_OID = "c7f3e3c8-d48b-11e4-8d88-001e8c717e5b";
    public static final String USER_HEAD_USERNAME = "head";
    public static final String USER_HEAD_PASSWORD = "HeadPassw0rd";

    public static final File ROLE_PROXY_FILE = new File(BASE_REPO_DIR, "role-proxy.xml");

    // REST and end user authorization
    public static final File USER_PROXY_FILE = new File(BASE_REPO_DIR, "user-proxy.xml");
    public static final String USER_PROXY_OID = "d8f3e3c8-d48b-11e4-8d88-001e8c717e5b";
    public static final String USER_PROXY_USERNAME = "proxy";
    public static final String USER_PROXY_PASSWORD = "ProxyPassw0rd";

    @Override
    public void initSystem(Task initTask, OperationResult result) throws Exception {
        super.initSystem(initTask, result);

        addObject(ROLE_PROXY_FILE, initTask, result);
        addObject(USER_EGOIST_FILE, initTask, result);
        addObject(USER_HEAD_FILE, initTask, result);
        addObject(USER_PROXY_FILE, initTask, result);

        InternalMonitor.reset();
    }

    @Test
    public void test001getUserSelfBySomebody() {
        WebClient client = prepareClient(USER_SOMEBODY_OID);
        client.path("/self/");

        getDummyAuditService().clear();

        when();
        Response response = client.get();

        then();
        assertStatus(response, 200);
        UserType userType = response.readEntity(UserType.class);
        AssertJUnit.assertNotNull("Returned entity in body must not be null.", userType);
        logger.info("Returned entity: {}", userType.asPrismObject().debugDump());

        displayDumpable("Audit", getDummyAuditService());
        getDummyAuditService().assertRecords(2);
        getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
    }

    @Test
    public void test002getUserSelfByEgoist() {
        WebClient client = prepareClient(USER_EGOIST_OID);
        client.path("/self/");

        getDummyAuditService().clear();

        when();
        Response response = client.get();

        then();
        assertStatus(response, 200);
        UserType userType = response.readEntity(UserType.class);
        assertNotNull("Returned entity in body must not be null.", userType);
        logger.info("Returned entity: {}", userType.asPrismObject().debugDump());

        displayDumpable("Audit", getDummyAuditService());
        getDummyAuditService().assertRecords(2);
        getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
    }

    /**
     * egoist doesn't have authorization to read other object. ot has only end user role,
     * so he is allowed to performed defined actions on his own.
     */
    @Test
    public void test003getUserAdministratorByEgoist() {
        WebClient client = prepareClient(USER_EGOIST_OID);
        client.path("/users/" + SystemObjectsType.USER_ADMINISTRATOR.value());

        getDummyAuditService().clear();

        when();
        Response response = client.get();

        then();
        assertStatus(response, 403);

        displayDumpable("Audit", getDummyAuditService());
        getDummyAuditService().assertRecords(2);
        getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
    }

    /**
     * user head is a super user and has also rest authorization so he can perform any action
     */
    @Test
    public void test004getUserSelfByHead() {
        WebClient client = prepareClient(null);
        client.path("/self");

        getDummyAuditService().clear();

        when();
        Response response = client.get();

        then();
        assertStatus(response, 200);
        UserType userType = response.readEntity(UserType.class);
        assertNotNull("Returned entity in body must not be null.", userType);
        logger.info("Returned entity: {}", userType.asPrismObject().debugDump());

        displayDumpable("Audit", getDummyAuditService());
        getDummyAuditService().assertRecords(2);
        getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
    }

    /**
     * even though head is a superuser, it is not allowed for service application to switch to this user,
     * therefore head is not allowed to read user administrator using inpersonation
     */
    @Test
    public void test005getUserSelfByProxyHead() {
        WebClient client = prepareClient(USER_HEAD_OID);
        client.path("/self");

        getDummyAuditService().clear();

        when();
        Response response = client.get();

        then();
        assertStatus(response, 403);

        displayDumpable("Audit", getDummyAuditService());
        getDummyAuditService().assertRecords(2);
        getDummyAuditService().assertFailedProxyLogin(SchemaConstants.CHANNEL_REST_URI);
    }

    @Override
    protected String getAcceptHeader() {
        return MediaType.APPLICATION_XML;
    }

    @Override
    protected String getContentType() {
        return MediaType.APPLICATION_XML;
    }

    @Override
    protected MidpointAbstractProvider getProvider() {
        return xmlProvider;
    }

    private WebClient prepareClient(String proxyUserOid) {
        WebClient client = prepareClient(USER_PROXY_USERNAME, USER_PROXY_PASSWORD);
        if (StringUtils.isNotBlank(proxyUserOid)) {
            client.header("Switch-To-Principal", proxyUserOid);
        }
        return client;
    }
}

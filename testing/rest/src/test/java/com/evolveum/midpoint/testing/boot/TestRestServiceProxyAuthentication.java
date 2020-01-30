/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.boot;

import com.evolveum.midpoint.common.rest.MidpointAbstractProvider;
import com.evolveum.midpoint.gui.test.TestMidPointSpringApplication;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;

/**
 * @author skublik
 */
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
@SpringBootTest(classes = TestMidPointSpringApplication.class, webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
public class TestRestServiceProxyAuthentication extends RestServiceInitializer {

    private static final transient Trace LOGGER = TraceManager.getTrace(TestRestServiceProxyAuthentication.class);

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
        final String TEST_NAME = "test001getUserSelfBySomebody";

        WebClient client = prepareClient(USER_SOMEBODY_OID);
        client.path("/self/");

        getDummyAuditService().clear();

        TestUtil.displayWhen(TEST_NAME);
        Response response = client.get();

        TestUtil.displayThen(TEST_NAME);
        assertStatus(response, 200);
        UserType userType = response.readEntity(UserType.class);
        AssertJUnit.assertNotNull("Returned entity in body must not be null.", userType);
        LOGGER.info("Returned entity: {}", userType.asPrismObject().debugDump());

        IntegrationTestTools.display("Audit", getDummyAuditService());
        getDummyAuditService().assertRecords(2);
        getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
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
        WebClient client = prepareClient("proxy", "proxyPassword");
        if (StringUtils.isNotBlank(proxyUserOid)){
            client.header("Switch-To-Principal", proxyUserOid);
        }
        return client;
    }
}

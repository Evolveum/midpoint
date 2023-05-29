/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.rest;

import com.evolveum.midpoint.common.rest.MidpointAbstractProvider;
import com.evolveum.midpoint.common.rest.MidpointJsonProvider;
import com.evolveum.midpoint.common.rest.MidpointXmlProvider;
import com.evolveum.midpoint.common.rest.MidpointYamlProvider;
import com.evolveum.midpoint.gui.test.TestMidPointSpringApplication;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyAuditService;
import com.evolveum.midpoint.web.AbstractGuiIntegrationTest;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.local.LocalConduit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import jakarta.ws.rs.core.Response;
import java.io.File;
import java.util.Collections;

import static org.testng.AssertJUnit.assertEquals;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@ActiveProfiles("test")
@SpringBootTest(classes = TestMidPointSpringApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = "server.port=18088")
public abstract class AbstractRestServiceInitializer extends AbstractGuiIntegrationTest {

    protected static final File BASE_REPO_DIR = new File("src/test/resources/repo/");

    public static final File USER_ADMINISTRATOR_FILE = new File(BASE_REPO_DIR, "user-administrator.xml");
    public static final String USER_ADMINISTRATOR_USERNAME = "administrator";
    public static final String USER_ADMINISTRATOR_PASSWORD = "5ecr3t";

    // No authorization
    public static final File USER_NOBODY_FILE = new File(BASE_REPO_DIR, "user-nobody.xml");
    public static final String USER_NOBODY_OID = "ffb9729c-d48b-11e4-9720-001e8c717e5b";
    public static final String USER_NOBODY_USERNAME = "nobody";
    public static final String USER_NOBODY_PASSWORD = "nopassword";

    // REST authorization only
    public static final File USER_CYCLOPS_FILE = new File(BASE_REPO_DIR, "user-cyclops.xml");
    public static final String USER_CYCLOPS_OID = "6020bb52-d48e-11e4-9eaf-001e8c717e5b";
    public static final String USER_CYCLOPS_USERNAME = "cyclops";
    public static final String USER_CYCLOPS_PASSWORD = "cyclopassword";

    // REST and reader authorization
    public static final File USER_SOMEBODY_FILE = new File(BASE_REPO_DIR, "user-somebody.xml");
    public static final String USER_SOMEBODY_OID = "a5f3e3c8-d48b-11e4-8d88-001e8c717e5b";
    public static final String USER_SOMEBODY_USERNAME = "somebody";
    public static final String USER_SOMEBODY_PASSWORD = "somepassword";

    // other
    public static final File USER_JACK_FILE = new File(BASE_REPO_DIR, "user-jack.xml");
    public static final String USER_JACK_OID = "229487cb-59b6-490b-879d-7a6d925dd08c";

    public static final File ROLE_SUPERUSER_FILE = new File(BASE_REPO_DIR, "role-superuser.xml");
    public static final File ROLE_ENDUSER_FILE = new File(BASE_REPO_DIR, "role-enduser.xml");
    public static final File ROLE_REST_FILE = new File(BASE_REPO_DIR, "role-rest.xml");
    public static final File ROLE_READER_FILE = new File(BASE_REPO_DIR, "role-reader.xml");

    public static final File SYSTEM_CONFIGURATION_FILE = new File(BASE_REPO_DIR, "system-configuration.xml");

    public static final File VALUE_POLICY_GENERAL = new File(BASE_REPO_DIR, "value-policy-general.xml");
    public static final File VALUE_POLICY_NUMERIC = new File(BASE_REPO_DIR, "value-policy-numeric.xml");
    public static final File VALUE_POLICY_SIMPLE = new File(BASE_REPO_DIR, "value-policy-simple.xml");
    public static final File VALUE_POLICY_SECURITY_ANSWER = new File(BASE_REPO_DIR, "value-policy-security-answer.xml");
    public static final File SECURITY_POLICY = new File(BASE_REPO_DIR, "security-policy.xml");
    public static final File SECURITY_POLICY_NO_HISTORY = new File(BASE_REPO_DIR, "security-policy-no-history.xml");

    @Autowired
    private ProvisioningService provisioning;

    @Autowired
    protected MidpointXmlProvider xmlProvider;

    @Autowired
    protected MidpointJsonProvider jsonProvider;

    @Autowired
    protected MidpointYamlProvider yamlProvider;

    protected abstract String getAcceptHeader();
    protected abstract String getContentType();
    protected abstract MidpointAbstractProvider getProvider();

    protected String ENDPOINT_ADDRESS = "http://localhost:"
            + TestMidPointSpringApplication.DEFAULT_PORT
            + System.getProperty("mp.test.rest.context.path", "/ws/rest");

    protected WebClient prepareClient(String username, String password) {
        WebClient client = WebClient.create(ENDPOINT_ADDRESS, Collections.singletonList(getProvider()));
        ClientConfiguration clientConfig = WebClient.getConfig(client);

        clientConfig.getRequestContext().put(LocalConduit.DIRECT_DISPATCH, Boolean.TRUE);

        client.accept(getAcceptHeader());
        client.type(getContentType());

        createAuthorizationHeader(client, username, password);
        return client;
    }

    protected void createAuthorizationHeader(WebClient client, String username, String password) {
        if (username != null) {
            String authorizationHeader = "Basic " + org.apache.cxf.common.util.Base64Utility
                    .encode((username + ":" + (password == null ? "" : password)).getBytes());
            client.header("Authorization", authorizationHeader);
        }
    }

    protected void assertStatus(Response response, int expStatus) {
        assertEquals("Expected " + expStatus + " but got " + response.getStatus(), expStatus,
                response.getStatus());
    }

    public PrismContext getPrismContext() {
        return prismContext;
    }

    public ModelService getModelService() {
        return modelService;
    }

    public RepositoryService getRepositoryService() {
        return repositoryService;
    }

    public ProvisioningService getProvisioning() {
        return provisioning;
    }

    public DummyAuditService getDummyAuditService() {
        return dummyAuditService;
    }
}

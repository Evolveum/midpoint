/*
 * Copyright (c) 2016-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.rest.authentication;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationBehavioralDataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.apache.cxf.jaxrs.client.WebClient;
import org.testng.annotations.Test;

import javax.ws.rs.core.Response;
import java.io.File;
import java.util.Optional;

import static org.testng.AssertJUnit.assertEquals;

public class TestOptionForSkipUpdatingAuthFocusBehavior extends TestAbstractAuthentication {

    public static final File SECURITY_POLICY_DEFAULT = new File(BASE_REPO_DIR, "security-policy-default.xml");
    public static final File SECURITY_POLICY_ENABLED = new File(BASE_REPO_DIR, "security-policy-enabled.xml");
    public static final File SECURITY_POLICY_DISABLED = new File(BASE_REPO_DIR, "security-policy-disabled.xml");
    public static final File SECURITY_POLICY_ONLY_UNSUCCESSFUL = new File(BASE_REPO_DIR, "security-policy-unsuccessful.xml");

    @Test
    public void test001DefaultOption() throws Exception {
        runEnabledOptionTest(SECURITY_POLICY_DEFAULT);
    }

    @Test
    public void test002EnabledOption() throws Exception {
        runEnabledOptionTest(SECURITY_POLICY_ENABLED);
    }

    @Test
    public void test003DisabledOption() throws Exception {
        replaceSecurityPolicy(SECURITY_POLICY_DISABLED);

        WebClient client = prepareUnsuccessfulClient();

        when();
        Response response = client.get();

        then();
        assertStatus(response, 401);
        assertNumberOfFailedLogin(getUser(USER_ADMINISTRATOR_OID), 0);
    }

    @Test
    public void test004OnlyUnsuccessfulOption() throws Exception {
        replaceSecurityPolicy(SECURITY_POLICY_ONLY_UNSUCCESSFUL);

        WebClient clientUnsuccessful = prepareUnsuccessfulClient();
        WebClient clientSuccessful = prepareSuccessfulClient();

        when();
        Response responseUnsuccessful = clientUnsuccessful.get();
        PrismObject<UserType> adminAfterFail = getUser(USER_ADMINISTRATOR_OID);
        Response responseSuccessful1 = clientSuccessful.get();
        PrismObject<UserType> adminAfterSuccess1 = getUser(USER_ADMINISTRATOR_OID);
        Response responseSuccessful2 = clientSuccessful.get();
        PrismObject<UserType> adminAfterSuccess2 = getUser(USER_ADMINISTRATOR_OID);


        then();
        assertStatus(responseUnsuccessful, 401);
        assertNumberOfFailedLogin(adminAfterFail, 1);
        assertStatus(responseSuccessful1, 200);
        assertNumberOfFailedLogin(adminAfterSuccess1, 0);
        assertStatus(responseSuccessful2, 200);
        assertNumberOfFailedLogin(adminAfterSuccess2, 0);
        assertEquals("Event type of previous and last success login aren't same.",
                adminAfterSuccess1.asObjectable().getCredentials().getPassword().beginLastSuccessfulLogin(),
                adminAfterSuccess2.asObjectable().getCredentials().getPassword().beginLastSuccessfulLogin());
    }

    private void runEnabledOptionTest(File securityPolicy) throws Exception {
        replaceSecurityPolicy(securityPolicy);

        WebClient clientUnsuccessful = prepareUnsuccessfulClient();
        WebClient clientSuccessful = prepareSuccessfulClient();

        when();
        Response responseUnsuccessful = clientUnsuccessful.get();
        PrismObject<UserType> adminAfterFail = getUser(USER_ADMINISTRATOR_OID);
        Response responseSuccessful = clientSuccessful.get();
        PrismObject<UserType> adminAfterSuccess = getUser(USER_ADMINISTRATOR_OID);


        then();
        assertStatus(responseUnsuccessful, 401);
        assertNumberOfFailedLogin(adminAfterFail, 1);
        assertStatus(responseSuccessful, 200);
        assertNumberOfFailedLogin(adminAfterSuccess, 0);
    }

    private WebClient prepareSuccessfulClient() {
        WebClient client = prepareClient(USER_ADMINISTRATOR_USERNAME, USER_ADMINISTRATOR_PASSWORD);
        client.path("/users/" + SystemObjectsType.USER_ADMINISTRATOR.value());
        return client;
    }

    private WebClient prepareUnsuccessfulClient() {
        WebClient client = prepareClient(USER_ADMINISTRATOR_USERNAME, "wrong");
        client.path("/users/" + SystemObjectsType.USER_ADMINISTRATOR.value());
        return client;
    }

    private void assertNumberOfFailedLogin(PrismObject<UserType> user, int expectedFails){
        int actualFails = 0;
        Optional<AuthenticationBehavioralDataType> authentication = user.asObjectable().getBehavior().getAuthentication().stream()
                .filter(auth -> auth.getSequenceIdentifier().equals("rest")).findFirst();
        if (authentication.isPresent()) {
            actualFails = authentication.get().getFailedLogins();
        }
        assertEquals("Expected failed login " + expectedFails + " but got " + actualFails, expectedFails, actualFails);
    }

}

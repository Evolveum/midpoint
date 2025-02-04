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

import jakarta.ws.rs.core.Response;
import org.apache.cxf.jaxrs.client.WebClient;
import org.assertj.core.api.Assertions;
import org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator;
import org.springframework.aop.framework.autoproxy.BeanNameAutoProxyCreator;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Map;
import java.util.Optional;

import static org.testng.AssertJUnit.assertEquals;

public class TestBasicAuthentication extends TestAbstractAuthentication {

    public static final File SECURITY_POLICY_DEFAULT = new File(BASE_AUTH_REPO_DIR, "security-policy-default.xml");
    public static final File SECURITY_POLICY_ENABLED = new File(BASE_AUTH_REPO_DIR, "security-policy-enabled.xml");
    public static final File SECURITY_POLICY_DISABLED = new File(BASE_AUTH_REPO_DIR, "security-policy-disabled.xml");
    public static final File SECURITY_POLICY_ONLY_UNSUCCESSFUL = new File(BASE_AUTH_REPO_DIR, "security-policy-unsuccessful.xml");

    @Autowired
    protected BeanNameAutoProxyCreator beanNameAutoProxyCreator;

    @Test
    public void test001DefaultOptionForSkipUpdatingAuthFocusBehavior() throws Exception {
        runEnabledOptionTest(SECURITY_POLICY_DEFAULT);
    }

    @Test
    public void test002EnabledOptionForSkipUpdatingAuthFocusBehavior() throws Exception {
        runEnabledOptionTest(SECURITY_POLICY_ENABLED);
    }

    @Test
    public void test003DisabledOptionForSkipUpdatingAuthFocusBehavior() throws Exception {
        replaceSecurityPolicy(SECURITY_POLICY_DISABLED);

        WebClient client = prepareUnsuccessfulClient();

        when();
        Response response = client.get();

        then();
        assertStatus(response, 401);
        assertNumberOfFailedLogin(getUser(USER_ADMINISTRATOR_OID), 0);
    }

    @Test
    public void test004OnlyUnsuccessfulOptionForSkipUpdatingAuthFocusBehavior() throws Exception {
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

    @Test
    public void test005MemoryLeakBecauseOfAuthFilters() throws Exception {
        replaceSecurityPolicy(SECURITY_POLICY_DEFAULT);

        when();
        for (int i = 0; i < 100; i++){
            Response responseSuccessful = prepareSuccessfulClient().get();
            assertStatus(responseSuccessful, 200);
        }

        then();
        beanNameAutoProxyCreator.isExposeProxy();
        var field = AbstractAutoProxyCreator.class.getDeclaredField("advisedBeans");
        field.setAccessible(true);

        // can fail if increase number of beans in application, then please increase count
        int count = 1500;
        Assertions.assertThat(
                        ((Map)field.get(beanNameAutoProxyCreator)).size())
                .as("Size of advisedBeans field in beanNameAutoProxyCreator")
                .withFailMessage("Field contains more than " + count + " advisedBeans, possible memory leak")
                .isLessThan(count);
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

    private WebClient prepareUnsuccessfulClient() {
        WebClient client = prepareClient(USER_ADMINISTRATOR_USERNAME, "wrong");
        client.path("/users/" + SystemObjectsType.USER_ADMINISTRATOR.value());
        return client;
    }

    protected WebClient prepareSuccessfulClient() {
        WebClient client = prepareClient(USER_ADMINISTRATOR_USERNAME, USER_ADMINISTRATOR_PASSWORD);
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

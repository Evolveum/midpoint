/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest;

import static org.testng.AssertJUnit.assertEquals;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ExternalDataType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestSecretProviders extends AbstractInitializedModelIntegrationTest {

    @Autowired
    private Protector protector;

    /**
     * Test that loads a secret from a properties file using propertiesFile secret provider.
     *
     * @throws Exception
     */
    @Test
    public void test100ProtectedStringInResourceConfiguration() throws Exception {
        DummyResource orange = getDummyResource(RESOURCE_DUMMY_ORANGE_NAME);
        assertEquals("Wrong guarded useless string", "whatever", orange.getUselessGuardedString());
    }

    /*
     * Test that loads a secret from an environment variables secret provider.
     */
    @Test
    public void test110ProtectedStringInUser() throws Exception {
        final String ENV_PREFIX = "MP_";
        final String ENV_VAR_NAME = "USER_PASSWORD";
        final String ENV_VAR_VALUE = "qwe123";

        System.setProperty(ENV_PREFIX + ENV_VAR_NAME, ENV_VAR_VALUE);

        ProtectedStringType ps = createProtectedString("env-provider", ENV_VAR_NAME);

        UserType user = new UserType()
                .name("secret.provider.user")
                .beginCredentials()
                .beginPassword()
                .value(ps)
                .<CredentialsType>end()
                .end();

        String userOid = addObject(user.asPrismObject());

        PrismObject<UserType> repoUser = getUser(userOid);
        ProtectedStringType protectedString = repoUser.asObjectable().getCredentials().getPassword().getValue();

        String value = protector.decryptString(protectedString);
        AssertJUnit.assertEquals("Wrong password", ENV_VAR_VALUE, value);

    }

    @Test
    public void test120TestResolvingSecrets() throws Exception {

        ProtectedStringType nonExistingProvider = createProtectedString("non-existing-provider", "MP_USER_PASSWORD");
        try {
            protector.decryptString(nonExistingProvider);
            AssertJUnit.fail("Expected encryption exception");
        } catch (EncryptionException ex) {
            AssertJUnit.assertEquals(
                    "Wrong message",
                    "No secrets provider with identifier non-existing-provider found", ex.getMessage());
        }

        ProtectedStringType nonExistingKey = createProtectedString("env-provider", "NON_EXISTING_KEY");
        try {
            protector.decryptString(nonExistingKey);
            AssertJUnit.fail("Expected encryption exception");
        } catch (EncryptionException ex) {
            AssertJUnit.assertEquals(
                    "Wrong message",
                    "No secret with key NON_EXISTING_KEY found in provider env-provider", ex.getMessage());
        }
    }

    private ProtectedStringType createProtectedString(String provider, String key) {
        ProtectedStringType ps = new ProtectedStringType();
        ExternalDataType ed = new ExternalDataType();
        ed.setProvider(provider);
        ed.setKey(key);
        ps.setExternalData(ed);

        return ps;
    }

    // TODO test cacheable secrets provider delegate
}

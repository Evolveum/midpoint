/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest;

import static org.testng.AssertJUnit.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import com.evolveum.midpoint.prism.crypto.ProtectedData;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.common.secrets.CacheableSecretsProviderDelegate;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.crypto.SecretsProvider;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CustomSecretsProviderType;
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
     */
    @Test
    public void test100ProtectedStringInResourceConfiguration() {
        DummyResource orange = getDummyResource(RESOURCE_DUMMY_ORANGE_NAME);
        assertEquals("Wrong guarded useless string", "whatever", orange.getUselessGuardedString());
    }

    /*
     * Test that loads a secret from an environment variables secret provider.
     */
    @Test
    public void test110ProtectedStringInUser() throws Exception {
        final String ENV_VAR_NAME = "MP_USER_PASSWORD";
        final String ENV_VAR_VALUE = "qwe123";

        System.setProperty(ENV_VAR_NAME, ENV_VAR_VALUE);

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
    public void test120TestResolvingSecrets() {
        ProtectedStringType nonExistingProvider = createProtectedString("non-existing-provider", "MP_USER_PASSWORD");
        try {
            protector.decryptString(nonExistingProvider);
            AssertJUnit.fail("Expected encryption exception");
        } catch (EncryptionException ex) {
            AssertJUnit.assertEquals(
                    "Wrong message",
                    "No secrets provider with identifier non-existing-provider found", ex.getMessage());
        }

        final String nonExisting = "MP_NON_EXISTING_KEY";
        ProtectedStringType nonExistingKey = createProtectedString("env-provider", nonExisting);
        try {
            protector.decryptString(nonExistingKey);
            AssertJUnit.fail("Expected encryption exception");
        } catch (EncryptionException ex) {
            AssertJUnit.assertEquals(
                    "Wrong message",
                    "No secret with key " + nonExisting + " found in provider env-provider", ex.getMessage());
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

    @Test
    public void test130CacheableSecretsProvider() throws Exception {
        final CustomSecretsProviderType custom = new CustomSecretsProviderType();
        custom.setIdentifier("fake");
        custom.setCache(XmlTypeConverter.createDuration("PT10S"));
        custom.setClassName("com.example.FakeSecretsProvider");

        final String value = "example";

        final AtomicInteger counter = new AtomicInteger(0);

        SecretsProvider<CustomSecretsProviderType> provider = new SecretsProvider<>() {

            @Override
            public @NotNull String getIdentifier() {
                return custom.getIdentifier();
            }

            @Override
            public CustomSecretsProviderType getConfiguration() {
                return custom;
            }

            @Override
            public String getSecretString(@NotNull String key) throws EncryptionException {
                counter.incrementAndGet();

                return value;
            }
        };

        CacheableSecretsProviderDelegate<CustomSecretsProviderType> delegate =
                new CacheableSecretsProviderDelegate<>(provider, custom.getCache());

        final String key = "key";

        // first attempt
        AssertJUnit.assertEquals(value, delegate.getSecretString(key));
        AssertJUnit.assertEquals(1, counter.get());

        // second attempt should be cached
        AssertJUnit.assertEquals(value, delegate.getSecretString(key));
        AssertJUnit.assertEquals(1, counter.get());

        Clock.get().overrideOffset(20000L);

        // third attempt should not be cached, because the cache has expired
        AssertJUnit.assertEquals(value, delegate.getSecretString(key));
        AssertJUnit.assertEquals(2, counter.get());

        // fourth attempt should be cached
        AssertJUnit.assertEquals(value, delegate.getSecretString(key));
        AssertJUnit.assertEquals(2, counter.get());

        Clock.get().resetOverride();
    }

    /**
     * Decrypts first as if it's byte[] via {@link Protector#decrypt(ProtectedData)},
     * cache and then try to use {@link Protector#decryptString(ProtectedData)}
     */
    @Test
    public void test140TestMismatchedAccessToProtectedString() throws Exception {
        ProtectedStringType ps = createProtectedString("file-provider","file-secret");

        // real secret value not yet cached
        protector.decrypt(ps); // <-- we're working with it as it's byte[] - decrypt()

        // secret value already cached, now we try get it as string
        ProtectedStringType ps1 = createProtectedString("file-provider","file-secret");
        protector.decryptString(ps1);   // <-- we're thinking about String here - decryptString()
    }
}

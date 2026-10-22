/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.intest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicInteger;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.common.secrets.CacheableSecretsProviderDelegate;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.*;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ExternalDataType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestSecretProviders extends AbstractInitializedModelIntegrationTest {

    private static final File SYSTEM_CONFIGURATION_CUSTOM_SECRETS_PROVIDER_FILE =
            new File(COMMON_DIR, "system-configuration-custom-secrets-provider.xml");

    private static final String CUSTOM_PROVIDER_IDENTIFIER = "custom-provider";

    /** Secret file served by file-provider (see common/system-configuration.xml). */
    private static final File FILE_SECRET_FILE = new File("src/test/resources/file-secret-provider/file-secret");

    @Autowired
    private Protector protector;

    /**
     * Test that loads a secret from a properties file using propertiesFile secret provider.
     */
    @Test
    public void test100ProtectedStringInResourceConfiguration() {
        DummyResource orange = getDummyResource(RESOURCE_DUMMY_ORANGE_NAME);
        assertThat(orange.getUselessGuardedString()).as("guarded useless string").isEqualTo("whatever");
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
        assertThat(value).as("password").isEqualTo(ENV_VAR_VALUE);

    }

    @Test
    public void test120TestResolvingSecrets() {
        ProtectedStringType nonExistingProvider = createProtectedString("non-existing-provider", "MP_USER_PASSWORD");
        assertThatThrownBy(() -> protector.decryptString(nonExistingProvider))
                .isInstanceOf(EncryptionException.class)
                .hasMessage("No secrets provider with identifier non-existing-provider found");

        final String nonExisting = "MP_NON_EXISTING_KEY";
        ProtectedStringType nonExistingKey = createProtectedString("env-provider", nonExisting);
        assertThatThrownBy(() -> protector.decryptString(nonExistingKey))
                .isInstanceOf(EncryptionException.class)
                .hasMessage("No secret with key " + nonExisting + " found in provider env-provider");
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
        assertThat(delegate.getSecretString(key)).isEqualTo(value);
        assertThat(counter.get()).isEqualTo(1);

        // second attempt should be cached
        assertThat(delegate.getSecretString(key)).isEqualTo(value);
        assertThat(counter.get()).isEqualTo(1);

        Clock.get().overrideOffset(20000L);

        // third attempt should not be cached, because the cache has expired
        assertThat(delegate.getSecretString(key)).isEqualTo(value);
        assertThat(counter.get()).isEqualTo(2);

        // fourth attempt should be cached
        assertThat(delegate.getSecretString(key)).isEqualTo(value);
        assertThat(counter.get()).isEqualTo(2);

        Clock.get().resetOverride();
    }

    /**
     * Decrypts first as if it's byte[] via {@link Protector#decrypt(ProtectedData)},
     * cache and then try to use {@link Protector#decryptString(ProtectedData)}
     */
    @Test
    public void test140TestMismatchedAccessToProtectedString() throws Exception {
        ProtectedStringType ps = createProtectedString("file-provider", "file-secret");

        // real secret value not yet cached
        protector.decrypt(ps); // <-- we're working with it as it's byte[] - decrypt()

        // secret value already cached, now we try get it as string
        ProtectedStringType ps1 = createProtectedString("file-provider", "file-secret");
        protector.decryptString(ps1);   // <-- we're thinking about String here - decryptString()
    }

    /**
     * Custom provider implementation has a protected string in its own configuration.
     * The protected string points (via external data) to another provider (file-provider).
     * Custom provider has to be able to decrypt it during initialization and use the clear value.
     */
    @Test
    public void test150CustomProviderWithProtectedConfiguration() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("custom provider configuration with protected string resolved via file-provider");
        SystemConfigurationType template = prismContext.parserFor(SYSTEM_CONFIGURATION_CUSTOM_SECRETS_PROVIDER_FILE)
                .parseRealValue(SystemConfigurationType.class);
        CustomSecretsProviderType custom = template.getSecretsProviders().getCustom().get(0).clone();
        assertThat(custom.getIdentifier()).as("provider identifier").isEqualTo(CUSTOM_PROVIDER_IDENTIFIER);

        when("custom provider is added to system configuration");
        modifySystemObjectInRepo(
                SystemConfigurationType.class,
                SystemObjectsType.SYSTEM_CONFIGURATION.value(),
                prismContext.deltaFor(SystemConfigurationType.class)
                        .item(SystemConfigurationType.F_SECRETS_PROVIDERS, SecretsProvidersType.F_CUSTOM)
                        .add(custom)
                        .asItemDeltas(),
                result);

        then("custom provider is registered in protector");
        SecretsProvider<?> provider = findSecretsProvider(CUSTOM_PROVIDER_IDENTIFIER);
        assertThat(provider)
                .as("custom provider")
                .isNotNull();
        assertThat(provider.getDependencies())
                .as("dependencies")
                .containsExactly("file-provider");

        and("custom provider has decrypted protected string from its configuration");
        ProtectedStringType valueForEvaluation = createProtectedString(CUSTOM_PROVIDER_IDENTIFIER, MyCustomSecretsProvider.KEY_CONNECTION_TOKEN);
        String expected = Files.readString(FILE_SECRET_FILE.toPath());
        assertThat(protector.decryptString(valueForEvaluation))
                .as("decrypted configuration value")
                .isEqualTo(expected);

        and("unknown key is reported as missing secret");
        ProtectedStringType unknown = createProtectedString(CUSTOM_PROVIDER_IDENTIFIER, "unknown");
        assertThatThrownBy(() -> protector.decryptString(unknown))
                .isInstanceOf(EncryptionException.class)
                .hasMessage("No secret with key unknown found in provider " + CUSTOM_PROVIDER_IDENTIFIER);

        when("custom provider is removed from system configuration");
        PrismObject<SystemConfigurationType> systemConfiguration = getSystemConfiguration().asPrismObject();
        CustomSecretsProviderType stored = systemConfiguration.asObjectable().getSecretsProviders().getCustom().stream()
                .filter(c -> CUSTOM_PROVIDER_IDENTIFIER.equals(c.getIdentifier()))
                .findFirst()
                .orElseThrow();
        modifySystemObjectInRepo(
                SystemConfigurationType.class,
                SystemObjectsType.SYSTEM_CONFIGURATION.value(),
                prismContext.deltaFor(SystemConfigurationType.class)
                        .item(SystemConfigurationType.F_SECRETS_PROVIDERS, SecretsProvidersType.F_CUSTOM)
                        .delete(stored.clone())
                        .asItemDeltas(),
                result);

        then("custom provider is no longer available");
        assertThat(findSecretsProvider(CUSTOM_PROVIDER_IDENTIFIER))
                .as("custom provider after removal")
                .isNull();
    }

    private SecretsProvider<?> findSecretsProvider(String identifier) {
        return ((SecretsResolver) protector).getSecretsProviders().stream()
                .filter(p -> identifier.equals(p.getIdentifier()))
                .findFirst()
                .orElse(null);
    }
}

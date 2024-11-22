/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.dummy;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.*;
import static com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification.ACCOUNT_DEFAULT;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.schema.util.RawRepoShadow;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsStorageTypeType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.testng.annotations.Test;

import com.evolveum.midpoint.provisioning.impl.opendj.TestOpenDj;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyTestResource;
import com.evolveum.midpoint.test.TestObject;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Tests all variants (here called scenarios) of password caching in dummy resource:
 *
 * - password readable: yes, existence only, no
 * - caching: on, legacy, off
 * - policy: encrypted, hashed, none
 *
 * To avoid creating lots of testing methods, this class is organized around {@link #SCENARIOS}. Each test method
 * executes all of these. It brings some complexities e.g. when debugging failing tests, but I believe it's still less
 * hassle than having 18 test methods for each test case.
 *
 * Related tests:
 *
 * - `test520` and later in {@link TestOpenDj} and its subclasses
 * - `AbstractPasswordTest` in `model-intest`
 */
public class TestDummyPasswordCaching extends AbstractDummyTest {

    private static final File TEST_DIR = new File(TEST_DIR_DUMMY, "dummy-password-caching");

    private static final String RESOURCE_TEMPLATE_FILE_NAME = "resource-dummy-template.xml";

    /** OID of the dummy connector. Determined in {@link #initSystem(Task, OperationResult)} */
    private String connectorOid;

    /**
     * The policy that provides hashed versions of the passwords. Encrypted versions do not need specific policy,
     * because they are applied by default (for non-legacy caching).
     */
    private static final TestObject<?> SECURITY_POLICY_HASHING = TestObject.file(
            TEST_DIR, "security-policy-hashing.xml", "8481f7bd-cdb2-4f16-9a11-27b51a1eec18");

    private static final List<Scenario> SCENARIOS = List.of(
            new Scenario(Readability.FULL, Caching.ON, Storage.ENCRYPTING),
            new Scenario(Readability.FULL, Caching.ON, Storage.HASHING),
            new Scenario(Readability.FULL, Caching.LEGACY, Storage.ENCRYPTING),
            new Scenario(Readability.FULL, Caching.LEGACY, Storage.HASHING),
            new Scenario(Readability.FULL, Caching.OFF, Storage.ENCRYPTING),
            new Scenario(Readability.FULL, Caching.OFF, Storage.HASHING),
            new Scenario(Readability.EXISTENCE, Caching.ON, Storage.ENCRYPTING),
            new Scenario(Readability.EXISTENCE, Caching.ON, Storage.HASHING),
            new Scenario(Readability.EXISTENCE, Caching.LEGACY, Storage.ENCRYPTING),
            new Scenario(Readability.EXISTENCE, Caching.LEGACY, Storage.HASHING),
            new Scenario(Readability.EXISTENCE, Caching.OFF, Storage.ENCRYPTING),
            new Scenario(Readability.EXISTENCE, Caching.OFF, Storage.HASHING),
            new Scenario(Readability.NONE, Caching.ON, Storage.ENCRYPTING),
            new Scenario(Readability.NONE, Caching.ON, Storage.HASHING),
            new Scenario(Readability.NONE, Caching.LEGACY, Storage.ENCRYPTING),
            new Scenario(Readability.NONE, Caching.LEGACY, Storage.HASHING),
            new Scenario(Readability.NONE, Caching.OFF, Storage.ENCRYPTING),
            new Scenario(Readability.NONE, Caching.OFF, Storage.HASHING));

    /** Test resources for individual scenarios. */
    private final Map<Scenario, DummyTestResource> resourceMap = new HashMap<>();

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        repoAdd(SECURITY_POLICY_HASHING, initResult);

        connectorOid = determineConnectorOid(initResult);
        for (var scenario : SCENARIOS) {
            getOrCreateResource(scenario);
        }
    }

    private String determineConnectorOid(OperationResult result) throws SchemaException {
        return findConnectorByType(IntegrationTestTools.DUMMY_CONNECTOR_TYPE, result)
                .getOid();
    }

    /**
     * Testing password caching for discovered accounts.
     *
     * An account (with a password) is created on the resource. We check the cached password after discovering it.
     */
    @Test
    public void test100DiscoverAccount() throws Exception {
        var task = getTestTask();
        var result = task.getResult();
        var password = "secret";

        for (var scenario : SCENARIOS) {
            given("scenario " + scenario);
            var resource = getOrCreateResource(scenario);
            var accountName = getTestNameShort() + "-" + scenario;

            when("discovering account");
            createAccount(resource, accountName, password);
            var provisioningShadow = discoverShadow(resource, accountName, task, result);

            then("password is correct in fetched and cached shadow");
            var repoShadow = getShadowRepo(provisioningShadow.getOid());
            displayDumpable("shadow (provisioning)", provisioningShadow);
            displayDumpable("shadow (repo)", repoShadow);

            assertAccountPasswordAfterRead(scenario, provisioningShadow, password);
            assertRepoPasswordAfterDiscovery(scenario, repoShadow.getBean(), password);
        }
    }

    /**
     * Testing password caching for discovered accounts when there's no password.
     *
     * An account (without a password) is created on the resource. We check there's no cached password after discovering it.
     */
    @Test
    public void test110DiscoverAccountWithoutPassword() throws Exception {
        for (var scenario : SCENARIOS) {
            given("scenario " + scenario);

            var task = getTestTask();
            var result = task.getResult();
            var resource = getOrCreateResource(scenario);
            var accountName = getTestNameShort() + "-" + scenario;

            when("discovering account");
            createAccount(resource, accountName, null);
            var provisioningShadow = discoverShadow(resource, accountName, task, result);

            then("no password in fetched and cached shadow");
            var repoShadow = getShadowRepo(provisioningShadow.getOid());
            assertNoShadowPassword(provisioningShadow);
            assertNoShadowPassword(repoShadow.getPrismObject());
        }
    }

    /**
     * Testing password caching for fetched accounts (with existing shadows).
     *
     * An account is created without a password, discovered, then modified on the resource to have a password.
     * Fetched again - and checked.
     */
    @Test
    public void test120FetchAccount() throws Exception {
        var task = getTestTask();
        var result = task.getResult();
        var password1 = "secret";
        var password2 = "Secret";

        for (var scenario : SCENARIOS) {
            given("scenario " + scenario);
            var resource = getOrCreateResource(scenario);
            var accountName = getTestNameShort() + "-" + scenario;

            and("an account created, discovered, and then modified to have a password");
            createAccount(resource, accountName, null);
            var oid = discoverShadow(resource, accountName, task, result)
                    .getOid();
            setAccountPassword(resource, accountName, password1);
            var repoShadow0 = getShadowRepo(oid);

            when("account is fetched");
            var provisioningShadow1 = fetchShadow(oid, task, result);

            then("password is correct in fetched and cached shadow");
            var repoShadow1 = getShadowRepo(provisioningShadow1.getOid());
            displayDumpable("shadow (provisioning)", provisioningShadow1);
            displayDumpable("shadow (repo)", repoShadow1);

            assertAccountPasswordAfterRead(scenario, provisioningShadow1, password1);
            assertRepoPasswordAfterGet(scenario, repoShadow1.getBean(), password1, repoShadow0);

            when("password is modified on the resource (again) and fetched (again)");
            setAccountPassword(resource, accountName, password2);

            var provisioningShadow2 = fetchShadow(oid, task, result);

            then("password is correct in fetched and cached shadow");
            var repoShadow2 = getShadowRepo(provisioningShadow2.getOid());
            displayDumpable("shadow (provisioning)", provisioningShadow2);
            displayDumpable("shadow (repo)", repoShadow2);

            assertAccountPasswordAfterRead(scenario, provisioningShadow2, password2);
            assertRepoPasswordAfterGet(scenario, repoShadow2.getBean(), password2, repoShadow1);
        }
    }

    /**
     * Testing password caching for fetched accounts when the security policy is changed.
     *
     * Account is discovered under the original security policy. Then the policy is changed and the account is fetched again.
     * Cached data should be updated to cover the new policy.
     */
    @Test
    public void test130FetchAccountUnderChangedPolicy() throws Exception {
        var task = getTestTask();
        var result = task.getResult();
        var password = "secret";

        for (var scenario : SCENARIOS) {
            given("scenario " + scenario);
            var resource = getOrCreateResource(scenario);
            var accountName = getTestNameShort() + "-" + scenario;

            and("an account created and discovered");
            createAccount(resource, accountName, password);
            var oid = discoverShadow(resource, accountName, task, result)
                    .getOid();

            var repoShadow0 = getShadowRepo(oid);
            assertRepoPasswordAfterDiscovery(scenario, repoShadow0.getBean(), password);

            when("security policy is changed and the account is fetched");
            var modifiedScenario = scenario.withNextStorage();
            try {
                updateResourceWithScenario(resource, modifiedScenario, task, result);
                fetchShadow(oid, task, result);

                then("password is correct in the cached shadow");
                var repoShadow1 = getShadowRepo(oid);
                displayDumpable("shadow (repo)", repoShadow1);
                assertRepoPasswordAfterGet(modifiedScenario, repoShadow1.getBean(), password, repoShadow0);
            } finally {
                updateResourceWithScenario(resource, scenario, task, result);
            }
        }
    }

    /**
     * Testing password caching for accounts that are created and modified by midPoint (with a password).
     *
     * We test modification with clear, encrypted, and hashed values.
     * (The hashed values can sneak in when using `asIs` expression is used on focus hashed value. Eventually, model
     * should take care of that, but it's not the case now.)
     *
     * Finally, we delete the password value.
     */
    @Test
    public void test200CreateAndModifyAccount() throws Exception {
        var task = getTestTask();
        var result = task.getResult();
        var password1 = "secret";
        var password2 = "SecretEncrypted";
        var password3 = "SecretClear";
        var password4 = "Nonsense";

        for (var scenario : SCENARIOS) {
            given("scenario " + scenario);
            var resource = getOrCreateResource(scenario);
            var accountName = getTestNameShort() + "-" + scenario;

            when("creating account");
            var shadowToCreate = Resource.of(resource.get())
                    .shadow(ACCOUNT_DEFAULT)
                    .withSimpleAttribute(ICFS_NAME, accountName)
                    .withPassword(password1)
                    .asPrismObject();
            var oid = provisioningService.addObject(shadowToCreate, null, null, task, result);

            then("password is correct in cached shadow");
            var repoShadow1 = getShadowRepo(oid);
            displayDumpable("shadow (repo)", repoShadow1);
            assertRepoPasswordAfterMidPointAdd(scenario, repoShadow1.getBean(), password1);

            when("modifying account password via midPoint (encrypted)");
            setAccountPasswordByMidPoint(resource, oid, encrypted(password2), task, result);

            then("password is correct in cached shadow");
            var repoShadow2 = getShadowRepo(oid);
            displayDumpable("shadow (repo)", repoShadow2);
            assertRepoPasswordAfterMidPointModify(scenario, repoShadow2.getBean(), password2);

            when("modifying account password via midPoint (cleartext)");
            setAccountPasswordByMidPoint(resource, oid, clear(password3), task, result);

            then("password is correct in cached shadow");
            var repoShadow3 = getShadowRepo(oid);
            displayDumpable("shadow (repo)", repoShadow3);
            assertRepoPasswordAfterMidPointModify(scenario, repoShadow3.getBean(), password3);

            when("modifying account password via midPoint (hashed)");
            setAccountPasswordByMidPoint(resource, oid, hashed(password4), task, result);

            then("password is correct (untouched) in cached shadow");
            var repoShadow4 = getShadowRepo(oid);
            displayDumpable("shadow (repo)", repoShadow4);
            assertRepoPasswordAfterMidPointModify(scenario, repoShadow3.getBean(), password3); // hashed password => no change
        }
    }

    // TODO add more "transitional" tests; like those in TestOpenDj.

    private @Nullable ProtectedStringType encrypted(String password) throws EncryptionException {
        var ps = clear(password);
        protector.encrypt(ps);
        return ps;
    }

    private @Nullable ProtectedStringType hashed(String password) throws EncryptionException, SchemaException {
        var ps = clear(password);
        protector.hash(ps);
        return ps;
    }

    private @Nullable ProtectedStringType clear(String password) {
        return new ProtectedStringType().clearValue(password);
    }

    /** This is the same for discovery (search) and fetch (get). */
    private void assertAccountPasswordAfterRead(Scenario scenario, ShadowType provisioningShadow, String password)
            throws Exception {
        switch (scenario.readability) {
            case FULL -> assertEncryptedShadowPassword(provisioningShadow, password);
            case EXISTENCE -> assertIncompleteShadowPassword(provisioningShadow);
            case NONE -> assertNoShadowPassword(provisioningShadow);
        }
    }

    /** Assuming the shadow was not there before. */
    private void assertRepoPasswordAfterDiscovery(Scenario scenario, ShadowType repoShadow, String password) throws Exception {
        if (scenario.readability == Readability.NONE || scenario.caching == Caching.OFF) {
            assertNoShadowPassword(repoShadow);
        } else if (scenario.readability == Readability.EXISTENCE) {
            assertIncompleteShadowPassword(repoShadow); // for both caching ON and LEGACY, we store only the "incomplete" flag
        } else if (scenario.caching == Caching.LEGACY) {
            // We cache only hashed values in legacy mode
            assertHashedShadowPassword(repoShadow, password);
        } else {
            assert scenario.readability == Readability.FULL;
            assert scenario.caching == Caching.ON;
            assertShadowPassword(repoShadow, password, scenario.storage.getStorageType());
        }
    }

    /** Assuming the shadow was already there, with the value of `repoShadowBefore`. */
    private void assertRepoPasswordAfterGet(
            Scenario scenario, ShadowType repoShadow, String password, RawRepoShadow repoShadowBefore) throws Exception {
        if (scenario.readability == Readability.NONE) {
            assertNoShadowPassword(repoShadow);
        } else if (scenario.caching == Caching.OFF) {
            assertNoShadowPassword(repoShadow);
        } else if (scenario.caching == Caching.LEGACY) {
            // "Get" should not change anything in the legacy mode!
            var passwordValueAfter = ShadowUtil.getPasswordValueProperty(repoShadow);
            var passwordValueBefore = ShadowUtil.getPasswordValueProperty(repoShadowBefore.getBean());
            assertThat(passwordValueAfter)
                    .as("password after (legacy caching)")
                    .isEqualTo(passwordValueBefore);
        } else if (scenario.readability == Readability.EXISTENCE) {
            assertIncompleteShadowPassword(repoShadow); // FIXME if needed
        } else {
            assert scenario.readability == Readability.FULL;
            assert scenario.caching == Caching.ON;
            assertShadowPassword(repoShadow, password, scenario.storage.getStorageType());
        }
    }

    private void assertRepoPasswordAfterMidPointAdd(Scenario scenario, @NotNull ShadowType repoShadow, String password)
            throws Exception {
        switch (scenario.caching) {
            case OFF -> assertNoShadowPassword(repoShadow);
            case LEGACY -> assertHashedShadowPassword(repoShadow, password); // This is the only mode for legacy
            case ON -> assertShadowPassword(repoShadow, password, scenario.storage.getStorageType());
        }
    }

    private void assertRepoPasswordAfterMidPointModify(Scenario scenario, @NotNull ShadowType repoShadow, String password)
            throws Exception {
        assertRepoPasswordAfterMidPointAdd(scenario, repoShadow, password); // should be the same
    }

    private DummyTestResource getOrCreateResource(Scenario scenario) throws Exception {
        var existing = resourceMap.get(scenario);
        if (existing != null) {
            return existing;
        }

        var oid = UUID.randomUUID().toString();
        var object = createResourceDefinition(oid, scenario.instanceId(), scenario.readability, scenario.storage, scenario.caching);
        DummyTestResource resource = DummyTestResource.fromTestObject(object, scenario.instanceId(), null);
        resource.initAndTest(this, getTestTask(), getTestOperationResult());
        resourceMap.put(scenario, resource);
        return resource;
    }

    private TestObject<ObjectType> createResourceDefinition(
            String oid, String instanceId, Readability readability, Storage storage, Caching caching)
            throws IOException {
        return TestObject.templateFile(TEST_DIR, RESOURCE_TEMPLATE_FILE_NAME, oid, Map.of(
                "#OID#", oid,
                "#CONNECTOR_OID#", connectorOid,
                "#INSTANCE_ID#", instanceId,
                "#READABILITY#", readability.getConfigText(),
                "#SECURITY_POLICY_REF_ELEMENT#", storage.getConfigText(),
                "#CACHING_STRATEGY_ELEMENT#", caching.getConfigText(),
                "#LEGACY_CACHING_STRATEGY_ELEMENT#", caching.getLegacyConfigText()));
    }

    /** Replaces the resource definition (ResourceType) with the specified scenario. */
    private void updateResourceWithScenario(DummyTestResource resource, Scenario scenario, Task task, OperationResult result)
            throws Exception {
        var object =
                createResourceDefinition(resource.oid, resource.name, scenario.readability, scenario.storage, scenario.caching);
        repositoryService.addObject(object.get(), RepoAddOptions.createOverwrite(), result);
        testResourceAssertSuccess(resource.oid, task, result);
    }

    private void createAccount(DummyTestResource resource, String name, String password) throws Exception {
        resource.controller
                .addAccount(name)
                .setPassword(password);
    }

    private void setAccountPassword(DummyTestResource resource, String name, String password) throws Exception {
        resource.controller.getDummyResource()
                .getAccountByName(name)
                .setPassword(password);
    }

    private void setAccountPasswordByMidPoint(
            DummyTestResource resource, String shadowOid, @Nullable ProtectedStringType value, Task task, OperationResult result)
            throws Exception {
        provisioningService.modifyObject(
                ShadowType.class, shadowOid,
                Resource.of(resource.get())
                        .deltaFor(RI_ACCOUNT_OBJECT_CLASS)
                        .item(PATH_PASSWORD_VALUE)
                        .replace(value)
                        .asItemDeltas(),
                null, null, task, result);
    }

    private @NotNull ShadowType discoverShadow(DummyTestResource resource, String accountName, Task task, OperationResult result)
            throws CommonException {
        var shadows = provisioningService.searchShadows(
                Resource.of(resource.get())
                        .queryFor(RI_ACCOUNT_OBJECT_CLASS)
                        .and().item(ICFS_NAME_PATH).eq(accountName)
                        .build(),
                null, task, result);
        return MiscUtil.extractSingletonRequired(shadows).getBean();
    }

    private @NotNull ShadowType fetchShadow(String oid, Task task, OperationResult result) throws CommonException {
        return provisioningService
                .getObject(ShadowType.class, oid, null, task, result)
                .asObjectable();
    }

    private enum Readability {
        FULL("readable"), EXISTENCE("incomplete"), NONE("unreadable");

        private final String configText;

        Readability(String configText) {
            this.configText = configText;
        }

        String getConfigText() {
            return configText;
        }
    }

    private enum Caching {
        ON("<cachingStrategy>passive</cachingStrategy>", ""),
        LEGACY("<cachingStrategy>none</cachingStrategy>", "<cachingStrategy>passive</cachingStrategy>"),
        OFF("<cachingStrategy>none</cachingStrategy>", "");

        private final String configText, legacyConfigText;

        Caching(String configText, String legacyConfigText) {
            this.configText = configText;
            this.legacyConfigText = legacyConfigText;
        }

        String getConfigText() {
            return configText;
        }

        String getLegacyConfigText() {
            return legacyConfigText;
        }
    }

    private enum Storage {
        ENCRYPTING(CredentialsStorageTypeType.ENCRYPTION, ""),
        HASHING(CredentialsStorageTypeType.HASHING, "<securityPolicyRef oid='" + SECURITY_POLICY_HASHING.oid + "'/>");

        private final CredentialsStorageTypeType storageType;
        private final String configText;

        Storage(CredentialsStorageTypeType storageType, String configText) {
            this.storageType = storageType;
            this.configText = configText;
        }

        CredentialsStorageTypeType getStorageType() {
            return storageType;
        }

        String getConfigText() {
            return configText;
        }
    }

    private record Scenario(
            @NotNull Readability readability,
            @NotNull Caching caching,
            @NotNull Storage storage) {

        @Override
        public String toString() {
            return "R-" + readability + "-C-" + caching + "-S-" + storage;
        }

        String instanceId() {
            return toString();
        }

        /** Returns the same scenario but with the storage type changed to the next one. */
        Scenario withNextStorage() {
            return new Scenario(readability, caching, storage == Storage.ENCRYPTING ? Storage.HASHING : Storage.ENCRYPTING);
        }
    }
}

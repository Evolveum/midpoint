/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.impl.dummy;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.ICFS_NAME;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.RI_ACCOUNT_OBJECT_CLASS;
import static com.evolveum.midpoint.test.DummyResourceContoller.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType.ACCOUNT;

import java.io.File;
import java.util.Collection;
import javax.xml.namespace.QName;

import org.identityconnectors.framework.common.objects.AttributeDelta;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.ConnectorOperationHook;
import com.evolveum.icf.dummy.resource.DummyObject;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.Resource;
import com.evolveum.midpoint.schema.util.ShadowBuilder;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * Tests the treatment of volatile attributes at the level of provisioning.
 *
 * Each account has a volatility type (determining how the account is treated by the resource) and
 * an account type (determining the configuration that midPoint applies for its handling).
 *
 * Volatility types:
 *
 * - type 0: no volatility
 * - type 1: `water` attribute is provided at account creation time, if it's not specified explicitly
 * - type 2: `water` attribute is changed when it's modified (X -> "_"+X); the same new value is stored into the `gossip` as well.
 * - type 3: `water` attribute is changed (X -> "_"+X) on _any_ modification of the resource object.
 *
 * Account types:
 *
 * - type 0: no volatility specific configuration
 * - simple type 1: standard configuration for treating type 1 volatility (expects any attributes may be generated)
 * - simple type 2: `water` is considered to be a "volatility trigger", i.e., its modification can trigger modification of anything in the account
 *
 * Representation:
 *
 * - `ship` encodes the account type
 * - `location` encodes the volatility type
 *
 * Other notes:
 *
 * . This test uses its own shadow caching configuration. It does need to be run with different default caching configs.
 * See {@link #shouldSkipWholeClass()}.
 */
public class TestDummyVolatility extends AbstractDummyTest {

    private static final File TEST_DIR = new File(TEST_DIR_DUMMY, "dummy-volatility");

    private static final File RESOURCE_DUMMY_FILE = new File(TEST_DIR, "resource-dummy.xml");

    private static final VolatilityType VOLATILITY_TYPE_0 = new VolatilityType0();
    private static final VolatilityType VOLATILITY_TYPE_1 = new VolatilityType1();
    private static final VolatilityType VOLATILITY_TYPE_2 = new VolatilityType2();
    private static final VolatilityType VOLATILITY_TYPE_3 = new VolatilityType3();

    private static final AccountType NO_VOLATILITY_CONFIG = new AccountType("no-volatility-config");
    private static final AccountType SIMPLE_TYPE_1_CONFIG = new AccountType("simple-type-1-config");
    private static final AccountType SIMPLE_TYPE_2_CONFIG = new AccountType("simple-type-2-config");
    private static final AccountType SIMPLE_TYPE_3_CONFIG = new AccountType("simple-type-3-config");

    @Override
    protected boolean shouldSkipWholeClass() {
        // No need to waste time running this class under different caching overrides, as it uses its own (explicit) caching.
        return !InternalsConfig.getShadowCachingDefault().isStandardForTests();
    }

    @Override
    protected File getResourceDummyFile() {
        return RESOURCE_DUMMY_FILE;
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        initAndReloadDummyResource(initTask, initResult);

        dummyResource.registerHook(VOLATILITY_TYPE_0.hook());
        dummyResource.registerHook(VOLATILITY_TYPE_1.hook());
        dummyResource.registerHook(VOLATILITY_TYPE_2.hook());
        dummyResource.registerHook(VOLATILITY_TYPE_3.hook());
    }

    /** Adds a non-volatile account under standard config. Nothing special here. */
    @Test
    public void test100AddingType0ShadowWithType0Config() throws CommonException {
        executeAccountCreationTest(VOLATILITY_TYPE_0, NO_VOLATILITY_CONFIG, false, false);
    }

    /** Adds a volatile (type 1) account under standard config. MidPoint does not see the computed value until fetched. */
    @Test
    public void test110AddingType1ShadowWithType0Config() throws CommonException {
        executeAccountCreationTest(VOLATILITY_TYPE_1, NO_VOLATILITY_CONFIG, false, true);
    }

    /** Adds a non-volatile account under simple type-1-aware config. No harm, just extra care. */
    @Test
    public void test120AddingType0ShadowWithSimpleType1Config() throws CommonException {
        executeAccountCreationTest(VOLATILITY_TYPE_0, SIMPLE_TYPE_1_CONFIG, false, false);
    }

    /** Adds a volatile (type 1) account under simple type-1-aware config. MidPoint sees the computed value from the start. */
    @Test
    public void test130AddingType1ShadowWithSimpleType1Config() throws CommonException {
        executeAccountCreationTest(VOLATILITY_TYPE_1, SIMPLE_TYPE_1_CONFIG, true, true);
    }

    private void executeAccountCreationTest(
            VolatilityType volatilityType, AccountType accountType,
            boolean waterExpectedAfterCreation, boolean waterExpectedAfterFetch)
            throws CommonException {
        var task = getTestTask();
        var result = task.getResult();
        var accountName = getTestNameShort();

        when("shadow is created via provisioning module");
        var shadowToCreate = createShadow(volatilityType, accountType, accountName).asPrismObject();
        var oid = provisioningService.addObject(shadowToCreate, null, null, task, result);

        then("volatile attributes are correctly cached");
        assertRepoShadowNew(oid)
                .display()
                .assertCachedOrigValues(ICFS_NAME, accountName)
                .assertCachedOrigValues(DUMMY_ACCOUNT_ATTRIBUTE_SHIP_QNAME, accountType.identifier())
                .assertCachedOrigValues(DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_QNAME, volatilityType.identifier())
                .assertCachedOrigValues(DUMMY_ACCOUNT_ATTRIBUTE_WATER_QNAME, waterExpectedAfterCreation ? accountName : null);

        when("shadow is fetched");
        provisioningService.getShadow(oid, null, task, result);

        then("volatile attributes are correctly cached");
        assertRepoShadowNew(oid)
                .display()
                .assertCachedOrigValues(ICFS_NAME, accountName)
                .assertCachedOrigValues(DUMMY_ACCOUNT_ATTRIBUTE_SHIP_QNAME, accountType.identifier())
                .assertCachedOrigValues(DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_QNAME, volatilityType.identifier())
                .assertCachedOrigValues(DUMMY_ACCOUNT_ATTRIBUTE_WATER_QNAME, waterExpectedAfterFetch ? accountName : null);
    }

    /** Modifies a volatile (type 2) account under "no volatility" config. MidPoint will see the wrong values. */
    @Test
    public void test200ModifyingType2ShadowWithType0Config() throws CommonException {
        executeAccountType2ModificationTest(VOLATILITY_TYPE_2, NO_VOLATILITY_CONFIG, false);
    }

    /** Modifies a volatile (type 2) account under mismatched (type 1) config. MidPoint will see the wrong values. */
    @Test
    public void test210ModifyingType2ShadowWithType1Config() throws CommonException {
        executeAccountType2ModificationTest(VOLATILITY_TYPE_2, SIMPLE_TYPE_1_CONFIG, false);
    }

    /** Modifies a volatile (type 2) account under type-2 aware config. MidPoint will see the correct values. */
    @Test
    public void test220ModifyingType2ShadowWithType2Config() throws CommonException {
        executeAccountType2ModificationTest(VOLATILITY_TYPE_2, SIMPLE_TYPE_2_CONFIG, true);
    }

    @SuppressWarnings("SameParameterValue")
    private void executeAccountType2ModificationTest(
            VolatilityType volatilityType, AccountType accountType, boolean expectedExtraValues)
            throws CommonException {
        var task = getTestTask();
        var result = task.getResult();
        var accountName = getTestNameShort();
        var oldWater = "water";
        var newWater = "WATER";

        given("shadow that is created via provisioning module");
        var shadowToCreate =
                createShadow(volatilityType, accountType, accountName)
                        .withSimpleAttribute(DUMMY_ACCOUNT_ATTRIBUTE_WATER_QNAME, oldWater)
                        .asPrismObject();
        var oid = provisioningService.addObject(shadowToCreate, null, null, task, result);

        when("the 'water' attribute is modified");
        provisioningService.modifyObject(
                ShadowType.class,
                oid,
                Resource.of(resource)
                        .deltaFor(RI_ACCOUNT_OBJECT_CLASS)
                        .item(ShadowType.F_ATTRIBUTES, DUMMY_ACCOUNT_ATTRIBUTE_WATER_QNAME)
                        .replace(newWater)
                        .asItemDeltas(),
                null, null, task, result);

        then("volatile attributes are correctly cached");
        assertRepoShadowNew(oid)
                .display()
                .assertCachedOrigValues(ICFS_NAME, accountName)
                .assertCachedOrigValues(DUMMY_ACCOUNT_ATTRIBUTE_SHIP_QNAME, accountType.identifier())
                .assertCachedOrigValues(DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_QNAME, volatilityType.identifier())
                .assertCachedOrigValues(DUMMY_ACCOUNT_ATTRIBUTE_WATER_QNAME, expectedExtraValues ? "_" + newWater : newWater)
                .assertCachedOrigValues(DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_QNAME, expectedExtraValues ? "_" + newWater : null);
    }

    /** Modifies a volatile (type 3) account under "no volatility" config. MidPoint will see the wrong values. */
    @Test
    public void test300ModifyingType3ShadowWithType0Config() throws CommonException {
        executeAccountType3ModificationTest(VOLATILITY_TYPE_3, NO_VOLATILITY_CONFIG, false);
    }

    /** Modifies a volatile (type 3) account under mismatched (type 1) config. MidPoint will see the wrong values. */
    @Test
    public void test310ModifyingType3ShadowWithType1Config() throws CommonException {
        executeAccountType3ModificationTest(VOLATILITY_TYPE_3, SIMPLE_TYPE_1_CONFIG, false);
    }

    /** Modifies a volatile (type 3) account under mismatched (type 2) config. MidPoint will see the wrong values. */
    @Test
    public void test320ModifyingType2ShadowWithType2Config() throws CommonException {
        executeAccountType3ModificationTest(VOLATILITY_TYPE_3, SIMPLE_TYPE_2_CONFIG, false);
    }

    /** Modifies a volatile (type 3) account under type-3 aware config. MidPoint will see the correct values. */
    @Test
    public void test330ModifyingType2ShadowWithType2Config() throws CommonException {
        executeAccountType3ModificationTest(VOLATILITY_TYPE_3, SIMPLE_TYPE_3_CONFIG, true);
    }

    @SuppressWarnings("SameParameterValue")
    private void executeAccountType3ModificationTest(
            VolatilityType volatilityType, AccountType accountType, boolean expectedExtraValues)
            throws CommonException {
        var task = getTestTask();
        var result = task.getResult();
        var accountName = getTestNameShort();
        var water = "water";

        given("shadow that is created via provisioning module");
        var shadowToCreate =
                createShadow(volatilityType, accountType, accountName)
                        .withSimpleAttribute(DUMMY_ACCOUNT_ATTRIBUTE_WATER_QNAME, water)
                        .asPrismObject();
        var oid = provisioningService.addObject(shadowToCreate, null, null, task, result);

        when("an unrelated attribute is modified");
        provisioningService.modifyObject(
                ShadowType.class,
                oid,
                Resource.of(resource)
                        .deltaFor(RI_ACCOUNT_OBJECT_CLASS)
                        .item(ShadowType.F_ATTRIBUTES, DUMMY_ACCOUNT_ATTRIBUTE_LOOT_QNAME)
                        .replace(100L)
                        .asItemDeltas(),
                null, null, task, result);

        then("volatile attributes are correctly cached");
        assertRepoShadowNew(oid)
                .display()
                .assertCachedOrigValues(ICFS_NAME, accountName)
                .assertCachedOrigValues(DUMMY_ACCOUNT_ATTRIBUTE_SHIP_QNAME, accountType.identifier())
                .assertCachedOrigValues(DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_QNAME, volatilityType.identifier())
                .assertCachedOrigValues(DUMMY_ACCOUNT_ATTRIBUTE_WATER_QNAME, expectedExtraValues ? "_" + water : water);
    }

    private ShadowBuilder createShadow(VolatilityType volatilityType, AccountType accountType, String name)
            throws SchemaException, ConfigurationException {
        return Resource.of(resource)
                .shadow(ResourceObjectTypeIdentification.of(ACCOUNT, accountType.identifier()))
                .withSimpleAttribute(ICFS_NAME, name)
                .withSimpleAttribute(DUMMY_ACCOUNT_ATTRIBUTE_SHIP_QNAME, accountType.identifier)
                .withSimpleAttribute(DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_QNAME, volatilityType.identifier());
    }

    protected @NotNull Collection<? extends QName> getCachedAccountAttributes() throws SchemaException, ConfigurationException {
        return getAccountObjectClassDefinition().getAttributeNames();
    }

    private record AccountType(String identifier) {
    }

    private static abstract class VolatilityType {
        abstract String identifier();
        abstract ConnectorOperationHook hook();
        boolean matches(DummyObject object) {
            return identifier().equals(object.getAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_LOCATION_NAME, String.class));
        }
    }

    /** Baseline: no volatility. */
    private static class VolatilityType0 extends VolatilityType {

        String identifier() {
            return "type0";
        }

        ConnectorOperationHook hook() {
            return new VolatilityHook();
        }
    }

    /** "Type 1" volatility provides a default value for `water` attribute when object is created. */
    private static class VolatilityType1 extends VolatilityType {

        String identifier() {
            return "type1";
        }

        ConnectorOperationHook hook() {
            return new VolatilityHook() {
                @Override
                public void afterCreateOperation(DummyObject object) {
                    try {
                        if (matches(object)
                                && isEmpty(object.getAttributeValues(DUMMY_ACCOUNT_ATTRIBUTE_WATER_NAME, String.class))) {
                            object.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_WATER_NAME, object.getName());
                        }
                    } catch (Exception e) {
                        throw SystemException.unexpected(e);
                    }
                }
            };
        }
    }

    /** "Type 2" volatility changes the `water` attribute wherever it's modified. */
    private static class VolatilityType2 extends VolatilityType {

        String identifier() {
            return "type2";
        }

        ConnectorOperationHook hook() {
            return new VolatilityHook() {
                @Override
                public void afterModifyOperation(DummyObject object, Collection<?> modifications) {
                    try {
                        if (!matches(object)) {
                            return;
                        }
                        var modificationOpt = modifications.stream()
                                .filter(mod -> DUMMY_ACCOUNT_ATTRIBUTE_WATER_NAME.equals(((AttributeDelta) mod).getName()))
                                .findFirst();
                        if (modificationOpt.isEmpty()) {
                            return;
                        }
                        var valueFromClient = object.getAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_WATER_NAME, String.class);
                        var valueToInject = "_" + valueFromClient;
                        object.replaceAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_WATER_NAME, valueToInject);
                        if (!emptyIfNull(object.getAttributeValues(DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, String.class))
                                .contains(valueToInject)) {
                            object.addAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_GOSSIP_NAME, valueToInject);
                        }
                    } catch (Exception e) {
                        throw SystemException.unexpected(e);
                    }
                }
            };
        }
    }

    /** "Type 3" volatility changes the `water` attribute wherever any attribute modified. */
    private static class VolatilityType3 extends VolatilityType {

        String identifier() {
            return "type3";
        }

        ConnectorOperationHook hook() {
            return new VolatilityHook() {
                @Override
                public void afterModifyOperation(DummyObject object, Collection<?> modifications) {
                    try {
                        if (!matches(object)) {
                            return;
                        }
                        var currentValue = object.getAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_WATER_NAME, String.class);
                        object.replaceAttributeValue(DUMMY_ACCOUNT_ATTRIBUTE_WATER_NAME, "_" + currentValue);
                    } catch (Exception e) {
                        throw SystemException.unexpected(e);
                    }
                }
            };
        }
    }

    private static class VolatilityHook implements ConnectorOperationHook {

        @Override
        public void afterCreateOperation(DummyObject object) {
        }

        @Override
        public void afterModifyOperation(DummyObject object, Collection<?> modifications) {
        }
    }
}

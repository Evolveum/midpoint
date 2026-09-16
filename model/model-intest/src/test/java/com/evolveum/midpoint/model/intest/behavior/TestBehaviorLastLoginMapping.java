/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.intest.behavior;

import java.io.File;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.assertj.core.api.Assertions;
import org.opends.server.types.Entry;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.intest.AbstractEmptyModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.ldap.OpenDJController;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowBehaviorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Tests for mapping of `behavior/lastLoginTimestamp` between resource and focus (issue 11882).
 *
 * The LDAP attribute `midpointLastLogin` (added to OpenDJ schema at runtime) plays the role of
 * AD `lastLogonTimestamp`: with `lastLoginDateAttribute` set, the connector consumes it and it
 * surfaces only as shadow `behavior/lastLoginTimestamp`.
 *
 * Inbound works either by unsetting `lastLoginDateAttribute` (plain attribute mapping, as in 4.8)
 * or via a mapping with explicit source `$shadow/behavior/lastLoginTimestamp`. Outbound is not
 * supported - see the disabled {@link #test400OutboundToBehavior()}.
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestBehaviorLastLoginMapping extends AbstractEmptyModelIntegrationTest {

    private static final File TEST_DIR = new File("src/test/resources/behavior");

    /** The `midpointLastLogin` attribute (generalized time) that plays the role of AD `lastLogonTimestamp`. */
    private static final File SCHEMA_LDIF_FILE = new File(TEST_DIR, "schema.ldif");
    private static final File ENTRIES_LDIF_FILE = new File(TEST_DIR, "entries.ldif");

    private static final File RESOURCE_OPENDJ_WORKAROUND_FILE = new File(TEST_DIR, "resource-opendj-behavior-workaround.xml");
    private static final String RESOURCE_OPENDJ_WORKAROUND_OID = "10000000-0000-0000-0000-000011882002";

    private static final File RESOURCE_OPENDJ_INBOUND_FILE = new File(TEST_DIR, "resource-opendj-behavior-inbound.xml");
    private static final String RESOURCE_OPENDJ_INBOUND_OID = "10000000-0000-0000-0000-000011882003";

    private static final File RESOURCE_OPENDJ_OUTBOUND_FILE = new File(TEST_DIR, "resource-opendj-behavior-outbound.xml");
    private static final String RESOURCE_OPENDJ_OUTBOUND_OID = "10000000-0000-0000-0000-000011882004";

    private static final QName OBJECT_CLASS_INET_ORG_PERSON = new QName(MidPointConstants.NS_RI, "inetOrgPerson");
    private static final ItemName ATTR_MIDPOINT_LAST_LOGIN = ItemName.from(MidPointConstants.NS_RI, "midpointLastLogin");

    private static final String NS_PIRACY = "http://midpoint.evolveum.com/xml/ns/samples/piracy";
    private static final ItemName EXT_FUNERAL_TIMESTAMP = ItemName.from(NS_PIRACY, "funeralTimestamp");
    private static final ItemPath PATH_EXT_FUNERAL_TIMESTAMP = ItemPath.create(UserType.F_EXTENSION, EXT_FUNERAL_TIMESTAMP);

    // Last login values must match entries.ldif
    private static final String ACCOUNT_JACK_UID = "jack";
    private static final String ACCOUNT_JACK_DN = "uid=jack,ou=People,dc=example,dc=com";
    private static final XMLGregorianCalendar JACK_LAST_LOGIN = XmlTypeConverter.createXMLGregorianCalendar("2025-08-01T12:00:00Z");

    private static final String ACCOUNT_WILL_UID = "will";
    private static final XMLGregorianCalendar WILL_LAST_LOGIN = XmlTypeConverter.createXMLGregorianCalendar("2025-06-15T08:30:00Z");

    private static final String UPDATED_LAST_LOGIN_LDAP = "20250910070000Z";
    private static final XMLGregorianCalendar UPDATED_LAST_LOGIN = XmlTypeConverter.createXMLGregorianCalendar("2025-09-10T07:00:00Z");

    private static final XMLGregorianCalendar NEW_LAST_LOGIN = XmlTypeConverter.createXMLGregorianCalendar("2025-12-24T18:00:00Z");
    private static final String NEW_LAST_LOGIN_LDAP = "20251224180000Z";

    @Override
    protected PrismObject<UserType> getDefaultActor() {
        return userAdministrator;
    }

    @Override
    protected void startResources() throws Exception {
        openDJController.startCleanServer();
    }

    @AfterClass
    public static void stopResources() {
        openDJController.stop();
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        openDJController.executeLdifChanges(SCHEMA_LDIF_FILE);
        openDJController.addEntriesFromLdifFile(ENTRIES_LDIF_FILE);

        importObjectFromFile(RESOURCE_OPENDJ_WORKAROUND_FILE, initResult);
        importObjectFromFile(RESOURCE_OPENDJ_INBOUND_FILE, initResult);
        importObjectFromFile(RESOURCE_OPENDJ_OUTBOUND_FILE, initResult);
    }

    @Test
    public void test010ResourcesSanity() throws Exception {
        Task task = getTestTask();

        for (String oid : new String[] {
                RESOURCE_OPENDJ_WORKAROUND_OID,
                RESOURCE_OPENDJ_INBOUND_OID,
                RESOURCE_OPENDJ_OUTBOUND_OID }) {
            OperationResult testResult = modelService.testResource(oid, task, task.getResult());
            TestUtil.assertSuccess("Test connection for " + oid, testResult);
        }
    }

    /**
     * Connector consumes `midpointLastLogin`, midPoint puts the value into shadow
     * `behavior/lastLoginTimestamp`. The attribute itself is not present in the shadow.
     * This is the baseline demonstrating the AD-like situation.
     */
    @Test
    public void test100NativeBehaviorFetch() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when("accounts are imported from the 'inbound' resource");
        importAccountsAndWait(RESOURCE_OPENDJ_INBOUND_OID, task, result);

        then("user jack exists and his shadow has behavior/lastLoginTimestamp, but no ri:midpointLastLogin attribute");
        PrismObject<UserType> jack = findUserByUsername(ACCOUNT_JACK_UID);
        Assertions.assertThat(jack).as("user jack").isNotNull();

        String shadowOid = getLiveLinkRefOid(jack, RESOURCE_OPENDJ_INBOUND_OID);
        PrismObject<ShadowType> shadow = getShadowModel(shadowOid);

        ShadowBehaviorType behavior = shadow.asObjectable().getBehavior();
        Assertions.assertThat(behavior).as("shadow behavior").isNotNull();
        Assertions.assertThat(behavior.getLastLoginTimestamp()).as("shadow lastLoginTimestamp").isNotNull();
        Assertions.assertThat(behavior.getLastLoginTimestamp().toGregorianCalendar().getTimeInMillis())
                .as("shadow lastLoginTimestamp millis")
                .isEqualTo(JACK_LAST_LOGIN.toGregorianCalendar().getTimeInMillis());

        Object attributeValue = ShadowUtil.getAttributeValue(shadow, ATTR_MIDPOINT_LAST_LOGIN);
        Assertions.assertThat(attributeValue).as("ri:midpointLastLogin attribute value").isNull();
    }

    /**
     * Workaround for issue 11882: `lastLoginDateAttribute` connector property is not set,
     * so `midpointLastLogin` is a plain attribute and the classic inbound mapping works.
     */
    @Test
    public void test200InboundWorkaround() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when("accounts are imported from the 'workaround' resource");
        importAccountsAndWait(RESOURCE_OPENDJ_WORKAROUND_OID, task, result);

        then("user will exists and has the last login value in the extension property");
        PrismObject<UserType> will = findUserByUsername(ACCOUNT_WILL_UID);
        Assertions.assertThat(will).as("user will").isNotNull();
        assertFuneralTimestamp(will, WILL_LAST_LOGIN);

        and("his shadow has the plain ri:midpointLastLogin attribute and no behavior data");
        String shadowOid = getLiveLinkRefOid(will, RESOURCE_OPENDJ_WORKAROUND_OID);
        PrismObject<ShadowType> shadow = getShadowModel(shadowOid);

        Object attributeValue = ShadowUtil.getAttributeValue(shadow, ATTR_MIDPOINT_LAST_LOGIN);
        Assertions.assertThat(attributeValue).as("ri:midpointLastLogin attribute value").isNotNull();

        ShadowBehaviorType behavior = shadow.asObjectable().getBehavior();
        Assertions.assertThat(behavior).as("shadow behavior").isNull();
    }

    /**
     * Inbound mapping with explicit source `$shadow/behavior/lastLoginTimestamp` brings
     * the value to the user extension property. This works even though there is no
     * first-class support for behavior mappings - the source is resolved against the shadow.
     */
    @Test
    public void test300InboundFromBehavior() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        when("accounts are imported from the 'inbound' resource (mapping sourced from behavior/lastLoginTimestamp)");
        importAccountsAndWait(RESOURCE_OPENDJ_INBOUND_OID, task, result);

        then("the last login value is propagated to the user extension property");
        PrismObject<UserType> jack = findUserByUsername(ACCOUNT_JACK_UID);
        assertFuneralTimestamp(jack, JACK_LAST_LOGIN);
    }

    /**
     * Updated last login value on the resource is propagated to the extension property
     * on re-import, i.e. the behavior-sourced inbound reacts to value changes, not only
     * to the initial import.
     */
    @Test
    public void test310InboundFromBehaviorUpdate() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("jack's last login changed on the resource");
        openDJController.modifyReplace(ACCOUNT_JACK_DN, "midpointLastLogin", UPDATED_LAST_LOGIN_LDAP);

        when("accounts are re-imported from the 'inbound' resource");
        importAccountsAndWait(RESOURCE_OPENDJ_INBOUND_OID, task, result);

        then("the extension property carries the updated value");
        PrismObject<UserType> jack = findUserByUsername(ACCOUNT_JACK_UID);
        assertFuneralTimestamp(jack, UPDATED_LAST_LOGIN);
    }

    /**
     * Outbound mapping targeting `$shadow/behavior/lastLoginTimestamp` should push the value
     * to the resource (connector translates it to the `lastLoginDateAttribute` LDAP attribute).
     *
     * EXPECTED TO FAIL (issue 11882): the lens does not evaluate outbound mappings for behavior,
     * and provisioning has no MODIFY path for behavior items.
     */
    @Test(enabled = false) // Enable when outbound mapping for behavior is implemented (issue 11882)
    public void test400OutboundToBehavior() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        given("user jack is linked to the 'outbound' resource");
        importAccountsAndWait(RESOURCE_OPENDJ_OUTBOUND_OID, task, result);
        PrismObject<UserType> jack = findUserByUsername(ACCOUNT_JACK_UID);
        Assertions.assertThat(jack).as("user jack").isNotNull();
        getLiveLinkRefOid(jack, RESOURCE_OPENDJ_OUTBOUND_OID); // asserts the link exists

        when("funeralTimestamp is set on the user");
        modifyUserReplace(jack.getOid(), PATH_EXT_FUNERAL_TIMESTAMP, task, result, NEW_LAST_LOGIN);

        then("the value is written to the LDAP attribute");
        Entry entry = openDJController.fetchEntry(ACCOUNT_JACK_DN);
        Assertions.assertThat(entry).as("jack LDAP entry").isNotNull();
        OpenDJController.assertAttribute(entry, "midpointLastLogin", NEW_LAST_LOGIN_LDAP);
    }

    private void importAccountsAndWait(String resourceOid, Task task, OperationResult result) throws Exception {
        loginAdministrator();
        modelService.importFromResource(resourceOid, OBJECT_CLASS_INET_ORG_PERSON, task, result);
        waitForTaskFinish(task, 40000);
    }

    private XMLGregorianCalendar getFuneralTimestamp(PrismObject<UserType> user) {
        return user.getPropertyRealValue(PATH_EXT_FUNERAL_TIMESTAMP, XMLGregorianCalendar.class);
    }

    private void assertFuneralTimestamp(PrismObject<UserType> user, XMLGregorianCalendar expected) {
        XMLGregorianCalendar actual = getFuneralTimestamp(user);
        Assertions.assertThat(actual).as("funeralTimestamp of " + user.getName()).isNotNull();
        Assertions.assertThat(actual.toGregorianCalendar().getTimeInMillis())
                .as("funeralTimestamp millis of " + user.getName())
                .isEqualTo(expected.toGregorianCalendar().getTimeInMillis());
    }
}

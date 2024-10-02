/*
 * Copyright (C) 2015-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.conntest.ad;

import com.evolveum.midpoint.model.test.CommonInitialObjects;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.testing.conntest.AbstractLdapTest;
import com.evolveum.midpoint.testing.conntest.UserLdapConnectionConfig;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidDnException;
import org.apache.directory.api.ldap.model.name.Ava;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.testng.annotations.Listeners;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.evolveum.midpoint.schema.util.task.ActivityStateUtil.getRootSyncTokenRealValueRequired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

/**
 *  *
 * @author Radovan Semancik
 */
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public abstract class AbstractAdLdapTest extends AbstractLdapTest
        implements AdTestMixin {

    @Override
    public String getStartSystemCommand() {
        return null;
    }

    @Override
    public String getStopSystemCommand() {
        return null;
    }

    @Override
    protected boolean useSsl() {
        return true;
    }

    @Override
    protected int getSearchSizeLimit() {
        return -1;
    }

    @Override
    public String getPrimaryIdentifierAttributeName() {
        return "objectGUID";
    }

    @Override
    protected String getPeopleLdapSuffix() {
        return "CN=Users," + getLdapSuffix();
    }

    @Override
    protected String getGroupsLdapSuffix() {
        return "CN=Users," + getLdapSuffix();
    }

    @Override
    protected int getLdapServerPort() {
        return 636;
    }

    @Override
    protected String getLdapAccountObjectClass() {
        return "user";
    }

    @Override
    protected String getLdapGroupObjectClass() {
        return "group";
    }

    @Override
    protected String getLdapGroupMemberAttribute() {
        return "member";
    }

    @Override
    protected boolean isGroupMemberMandatory() {
        return false;
    }

    @Override
    protected String getCreateTimeStampAttributeName() { return "whenCreated"; }

    protected String getObjectCategoryPerson() {
        return "CN=Person,CN=Schema,CN=Configuration," + getLdapSuffix();
    }

    protected String getObjectCategoryGroup() {
        return "CN=Group,CN=Schema,CN=Configuration," + getLdapSuffix();
    }

    protected boolean hasExchange() { return false; };

    /**
     * Returns true if this test does not really care about all the details.
     */
    protected boolean isVagueTest() {
        return false;
    }

    protected String getLdapConnectorClassName() {
        return AD_CONNECTOR_TYPE;
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        binaryAttributeDetector.addBinaryAttribute(ATTRIBUTE_OBJECT_GUID_NAME);
        binaryAttributeDetector.addBinaryAttribute(ATTRIBUTE_UNICODE_PWD_NAME);

        // TODO push this to upper classes after RC3
        CommonInitialObjects.addMarks(this, initTask, initResult);
    }


    protected void assertStepSyncToken(String syncTaskOid, int step, long tsStart, long tsEnd)
            throws ObjectNotFoundException, SchemaException {
        OperationResult result = createOperationResult("assertStepSyncToken");
        Task task = taskManager.getTaskPlain(syncTaskOid, result);
        String tokenRealValue =
                (String) getRootSyncTokenRealValueRequired(task.getRawTaskObjectClonedIfNecessary().asObjectable());
        assertThat(StringUtils.isBlank(tokenRealValue))
                .as("Empty sync token value")
                .isTrue();
        assertSuccess(result);
    }

    @Override
    protected void assertAccountShadow(PrismObject<ShadowType> shadow, String dn) throws SchemaException, ConfigurationException {
        super.assertAccountShadow(shadow, dn);
        ShadowSimpleAttribute<String> primaryIdAttr = ShadowUtil.getSimpleAttribute(shadow, getPrimaryIdentifierAttributeQName());
        assertNotNull("No primary identifier (" + getPrimaryIdentifierAttributeQName() + " in " + shadow, primaryIdAttr);
        String primaryId = primaryIdAttr.getRealValue();
        assertTrue("Unexpected chars in primary ID: '" + primaryId + "'", primaryId.matches("[a-z0-9\\-]+"));

        ShadowSimpleAttribute<String> objectSidAttr = ShadowUtil.getSimpleAttribute(shadow, new QName(MidPointConstants.NS_RI, ATTRIBUTE_OBJECT_SID_NAME));
        assertNotNull("No SID in " + shadow, objectSidAttr);
        display("SID of " + dn + ": " + objectSidAttr);
    }

    protected void assertSid(PrismObject<ShadowType> shadow, String expectedSid) {
        ShadowSimpleAttribute<String> objectSidAttr = ShadowUtil.getSimpleAttribute(shadow, new QName(MidPointConstants.NS_RI, ATTRIBUTE_OBJECT_SID_NAME));
        assertNotNull("No SID in " + shadow, objectSidAttr);
        display("SID of " + shadow + ": " + objectSidAttr);
        assertEquals("Wrong SID in " + shadow, expectedSid, objectSidAttr.getRealValue());
    }

    protected void assertObjectCategory(PrismObject<ShadowType> shadow, String expectedObjectCategory) {
        ShadowSimpleAttribute<String> objectCategoryAttr = ShadowUtil.getSimpleAttribute(shadow, new QName(MidPointConstants.NS_RI, ATTRIBUTE_OBJECT_CATEGORY_NAME));
        assertNotNull("No objectCategory in " + shadow, objectCategoryAttr);
        display("objectCategory of " + shadow + ": " + objectCategoryAttr);
        assertEquals("Wrong objectCategory in " + shadow, expectedObjectCategory, objectCategoryAttr.getRealValue());
    }

    @Override
    protected Entry assertLdapAccount(String samAccountName, String cn) throws LdapException, IOException, CursorException {
        Entry entry = searchLdapAccount("(cn=" + cn + ")");
        assertAttribute(entry, "cn", cn);
        assertAttribute(entry, ATTRIBUTE_SAM_ACCOUNT_NAME_NAME, samAccountName);
        return entry;
    }

    @Override
    protected void assertNoLdapAccount(String uid) {
        throw new UnsupportedOperationException("Boom! Cannot do this here. This is bloody AD! We need full name!");
    }

    protected void assertNoLdapAccount(String uid, String cn) throws LdapException, IOException, CursorException {
        assertNoLdapAccount(null, uid, cn);
    }

    protected void assertNoLdapAccount(UserLdapConnectionConfig config, String uid, String cn) throws LdapException, IOException, CursorException {
        LdapNetworkConnection connection = ldapConnect(config);
        List<Entry> entriesCn = ldapSearch(config, connection, "(cn=" + cn + ")");
        List<Entry> entriesSamAccountName = ldapSearch(config, connection, "(sAMAccountName=" + uid + ")");
        ldapDisconnect(connection);

        assertEquals("Unexpected number of entries for cn=" + cn + ": " + entriesCn, 0, entriesCn.size());
        assertEquals("Unexpected number of entries for sAMAccountName=" + uid + ": " + entriesSamAccountName, 0, entriesSamAccountName.size());
    }

    @Override
    protected String toAccountDn(String username) {
        throw new UnsupportedOperationException("Boom! Cannot do this here. This is bloody AD! We need full name!");
    }

    @Override
    protected String toAccountDn(String username, String fullName) {
        return ("CN=" + fullName + "," + getPeopleLdapSuffix());
    }

    @Override
    protected Rdn toAccountRdn(String username, String fullName) {
        try {
            return new Rdn(new Ava("CN", fullName));
        } catch (LdapInvalidDnException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    protected void assertLdapPassword(String uid, String fullName, String password) throws LdapException, IOException, CursorException {
        assertLdapPassword(null, uid, fullName, password);
    }

    protected void assertLdapPassword(UserLdapConnectionConfig config, String uid, String fullName, String password) throws LdapException, IOException, CursorException {
        Entry entry = getLdapAccountByCn(config, fullName);
        assertLdapPassword(config, entry, password);
    }

    protected void assertLdapPassword(String uid, String password) {
        throw new UnsupportedOperationException("Boom! Cannot do this here. This is bloody AD! We need full name!");
    }

    protected ObjectQuery createSamAccountNameQuery(String samAccountName) throws SchemaException {
        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass());
        ObjectQueryUtil.filterAnd(query.getFilter(), createAttributeFilter(ATTRIBUTE_SAM_ACCOUNT_NAME_NAME, samAccountName));
        return query;
    }

    @Override
    protected Entry createAccountEntry(String uid, String cn, String givenName, String sn) throws LdapException {
        byte[] password = encodePassword("Secret.123");
        Entry entry = new DefaultEntry(toAccountDn(uid, cn),
                "objectclass", getLdapAccountObjectClass(),
                ATTRIBUTE_SAM_ACCOUNT_NAME_NAME, uid,
                "cn", cn,
                "givenName", givenName,
                "sn", sn,
                ATTRIBUTE_USER_ACCOUNT_CONTROL_NAME, "512",
                ATTRIBUTE_UNICODE_PWD_NAME, password);
        return entry;
    }

    private byte[] encodePassword(String password) {
        String quotedPassword = "\"" + password + "\"";
        return quotedPassword.getBytes(StandardCharsets.UTF_16LE);
    }

    public <T> void assertAttribute(PrismObject<ShadowType> shadow, String attrName, T... expectedValues) {
        assertAttribute(shadow, new QName(getResourceNamespace(), attrName), expectedValues);
    }

}

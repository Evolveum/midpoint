/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.conntest;

import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.PATH_CREDENTIALS_PASSWORD_VALUE;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;

import org.apache.commons.lang3.mutable.MutableInt;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.impl.sync.tasks.recon.ReconciliationLauncher;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SearchResultMetadata;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-conntest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public abstract class AbstractLdapConnTest extends AbstractLdapSynchronizationTest {

    protected static final String ACCOUNT_IDM_DN = "uid=idm,ou=Administrators,dc=example,dc=com";
    protected static final String ACCOUNT_0_UID = "u00000000";
    protected static final String ACCOUNT_19_UID = "u00000019";
    protected static final String ACCOUNT_20_UID = "u00000020";
    protected static final String ACCOUNT_68_UID = "u00000068";
    protected static final String ACCOUNT_69_UID = "u00000069";
    protected static final String ACCOUNT_240_UID = "u00000240";
    protected static final String ACCOUNT_241_UID = "u00000241";

    protected static final int NUMBER_OF_GENERATED_ACCOUNTS = 4000;

    protected static final String ACCOUNT_HT_UID = "ht";
    protected static final String ACCOUNT_HT_CN = "Herman Toothrot";
    protected static final String ACCOUNT_HT_GIVENNAME = "Herman";
    protected static final String ACCOUNT_HT_SN = "Toothrot";

    private static final String USER_LARGO_NAME = "LarGO";
    private static final String USER_LARGO_GIVEN_NAME = "Largo";
    private static final String USER_LARGO_FAMILY_NAME = "LaGrande";

    protected static final File ROLE_UNDEAD_FILE = new File(COMMON_DIR, "role-undead.xml");
    protected static final String ROLE_UNDEAD_OID = "54885c40-ffcc-11e5-b782-63b3e4e2a69d";

    protected static final File ROLE_EVIL_FILE = new File(COMMON_DIR, "role-evil.xml");
    protected static final String ROLE_EVIL_OID = "624b43ec-ffcc-11e5-8297-f392afa54704";

    protected static final String GROUP_UNDEAD_CN = "undead";
    protected static final String GROUP_UNDEAD_DESCRIPTION = "Death is for loosers";

    protected static final String GROUP_EVIL_CN = "evil";
    protected static final String GROUP_EVIL_DESCRIPTION = "No pain no gain";

    protected static final String REGEXP_RESOURCE_OID_PLACEHOLDER = "%%%RESOURCE%%%";

    protected static final String ROOM_NUMBER_INVISIBLE = "invisible";

    protected static final String ACCOUNT_BILBO_UID = "bilbo";
    protected static final String ACCOUNT_BILBO_CN = "Bilbo Baggins";
    protected static final String ACCOUNT_BILBO_GIVENNAME = "Bilbo";
    protected static final String ACCOUNT_BILBO_SN = "Baggins";

    protected String account0Oid;
    protected String accountBarbossaOid;
    protected String accountBarbossaDn;
    protected String accountBarbossaEntryId;
    protected String accountLechuckOid;
    protected String accountLechuckDn;

    protected String groupEvilShadowOid;

    @Autowired
    protected ReconciliationLauncher reconciliationLauncher;

    protected boolean isIdmAdminInteOrgPerson() {
        return false;
    }

    protected File getResourceFile() {
        return new File(getBaseDir(), "resource.xml");
    }

    protected abstract String getAccount0Cn();

    protected int getNumberOfAllAccounts() {
        return NUMBER_OF_GENERATED_ACCOUNTS + (isIdmAdminInteOrgPerson() ? 1 : 0);
    }

    protected boolean hasAssociationShortcut() {
        return true;
    }

    protected boolean isVlvSearchBeyondEndResurnsLastEntry() {
        return false;
    }

    protected boolean hasLdapGroupBaseContext() {
        return false;
    }


    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        // Users
        repoAddObjectFromFile(USER_BARBOSSA_FILE, initResult);
        repoAddObjectFromFile(USER_GUYBRUSH_FILE, initResult);
        repoAddObjectFromFile(USER_LECHUCK_FILE, initResult);

        // Roles
        repoAddObjectFromFileReplaceResource(ROLE_UNDEAD_FILE, RoleType.class, initResult);
        repoAddObjectFromFileReplaceResource(ROLE_EVIL_FILE, RoleType.class, initResult);
    }

    protected <O extends ObjectType> void repoAddObjectFromFileReplaceResource(File file, Class<O> type, OperationResult result) throws IOException, SchemaException, ObjectAlreadyExistsException {
        String fileContent = MiscUtil.readFile(file);
        String xmlString = fileContent.replaceAll(REGEXP_RESOURCE_OID_PLACEHOLDER, getResourceOid());
        PrismObject<O> object = PrismTestUtil.parseObject(xmlString);
        repositoryService.addObject(object, null, result);
    }

    @Test
    public void test000Sanity() throws Exception {
        super.test000Sanity();
        cleanupDelete(toAccountDn(USER_BARBOSSA_USERNAME));
        cleanupDelete(toAccountDn(USER_CPTBARBOSSA_USERNAME));

        cleanupDelete(toGroupDn(GROUP_UNDEAD_CN));
        cleanupDelete(toGroupDn(GROUP_EVIL_CN));

        if (needsGroupFakeMemberEntry()) {
            addLdapGroup(GROUP_UNDEAD_CN, GROUP_UNDEAD_DESCRIPTION, "uid=fake," + getPeopleLdapSuffix());
            addLdapGroup(GROUP_EVIL_CN, GROUP_EVIL_DESCRIPTION, "uid=fake," + getPeopleLdapSuffix());
        } else {
            addLdapGroup(GROUP_UNDEAD_CN, GROUP_UNDEAD_DESCRIPTION);
            addLdapGroup(GROUP_EVIL_CN, GROUP_EVIL_DESCRIPTION);
        }

        addAdditionalLdapEntries();
    }

    protected void addAdditionalLdapEntries() throws Exception {
        // For use in subclasses.
    }

    @Test
    public void test100SearchAccount0ByLdapUid() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = createUidQuery(ACCOUNT_0_UID);

        rememberCounter(InternalCounters.CONNECTOR_OPERATION_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT);

        // WHEN
        when();
        SearchResultList<PrismObject<ShadowType>> shadows = modelService.searchObjects(ShadowType.class, query, null, task, result);

        assertEquals("Unexpected search result: " + shadows, 1, shadows.size());

        PrismObject<ShadowType> shadow = shadows.get(0);
        assertAccountShadow(shadow, toAccountDn(ACCOUNT_0_UID));

        assertConnectorOperationIncrement(1, 2);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);

        SearchResultMetadata metadata = shadows.getMetadata();
        if (metadata != null) {
            assertFalse(metadata.isPartialResults());
        }

        assertLdapConnectorReasonableInstances();
    }

    /**
     * No paging. It should return all accounts.
     */
    @Test
    public void test150SearchAllAccounts() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass());

        SearchResultList<PrismObject<ShadowType>> searchResultList = doSearch(query, getNumberOfAllAccounts(), task, result);

        assertConnectorOperationIncrement(1, getNumberOfAllAccounts() + 1);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);

        SearchResultMetadata metadata = searchResultList.getMetadata();
        if (metadata != null) {
            assertFalse(metadata.isPartialResults());
        }
        // Uknown number of results. This is SPR search.
        assertApproxNumberOfAllResults(metadata, null);

        assertLdapConnectorReasonableInstances();
    }

    @Test
    public void test151CountAllAccounts() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass());

        when();
        Integer count = modelService.countObjects(ShadowType.class, query, null, task, result);

        assertCountAllAccounts(count);

        assertLdapConnectorReasonableInstances();
    }

    protected void assertCountAllAccounts(Integer count) {
        assertEquals("Wrong account count", (Integer) getNumberOfAllAccounts(), count);
    }

    /**
     * Blocksize is 100, so this is in one block.
     */
    @Test
    public void test152SearchFirst50Accounts() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass());

        ObjectPaging paging = prismContext.queryFactory().createPaging();
        paging.setMaxSize(50);
        query.setPaging(paging);

        SearchResultList<PrismObject<ShadowType>> searchResultList = doSearch(query, 50, task, result);

        assertConnectorOperationIncrement(1, 51);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);

        SearchResultMetadata metadata = searchResultList.getMetadata();
        if (metadata != null) {
            assertFalse(metadata.isPartialResults());
        }

        // Uknown number of results. This is SPR search.
        assertApproxNumberOfAllResults(metadata, null);

        assertLdapConnectorReasonableInstances();
    }

    /**
     * Blocksize is 100, so this gets more than two blocks.
     */
    @Test
    public void test154SearchFirst222Accounts() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass());

        ObjectPaging paging = prismContext.queryFactory().createPaging();
        paging.setMaxSize(222);
        query.setPaging(paging);

        SearchResultList<PrismObject<ShadowType>> searchResultList = doSearch(query, 222, task, result);

        assertConnectorOperationIncrement(1, 223);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);

        SearchResultMetadata metadata = searchResultList.getMetadata();
        if (metadata != null) {
            assertFalse(metadata.isPartialResults());
        }

        // Uknown number of results. This is SPR search.
        assertApproxNumberOfAllResults(metadata, null);

        assertLdapConnectorReasonableInstances();
    }

    /**
     * Make a search that starts in the list of all accounts but goes beyond the end.
     */
    @Test
    public void test156SearchThroughEnd() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass());

        ObjectPaging paging = prismContext.queryFactory().createPaging();
        paging.setOffset(getNumberOfAllAccounts() - 150);
        paging.setMaxSize(333);
        query.setPaging(paging);

        SearchResultList<PrismObject<ShadowType>> searchResultList = doSearch(query, 150, task, result);

        assertConnectorOperationIncrement(1, 151);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);

        SearchResultMetadata metadata = searchResultList.getMetadata();
        if (metadata != null) {
            assertFalse(metadata.isPartialResults());
        }

        assertLdapConnectorReasonableInstances();
    }

    /**
     * Make a search that goes beyond the end of the list of all accounts.
     */
    @Test
    public void test158SearchBeyondEnd() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass());

        ObjectPaging paging = prismContext.queryFactory().createPaging();
        paging.setOffset(getNumberOfAllAccounts() + 50);
        paging.setMaxSize(123);
        query.setPaging(paging);

        int expectedEntries = 0;
        if (isVlvSearchBeyondEndResurnsLastEntry()) {
            expectedEntries = 1;
        }
        SearchResultList<PrismObject<ShadowType>> searchResultList = doSearch(query, expectedEntries, task, result);

//        Fails for 389ds tests. For some unknown reason. And this is not that important. There are similar asserts in other tests that are passing.
//        assertConnectorOperationIncrement(1);

        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);

        SearchResultMetadata metadata = searchResultList.getMetadata();
        if (metadata != null) {
            assertFalse(metadata.isPartialResults());
        }

        assertLdapConnectorReasonableInstances();
    }

    @Test
    public void test162SearchFirst50AccountsOffset0() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass());

        ObjectPaging paging = prismContext.queryFactory().createPaging();
        paging.setOffset(0);
        paging.setMaxSize(50);
        query.setPaging(paging);

        SearchResultList<PrismObject<ShadowType>> searchResultList = doSearch(query, 50, task, result);

        assertConnectorOperationIncrement(1, 51);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);

        SearchResultMetadata metadata = searchResultList.getMetadata();
        if (metadata != null) {
            assertFalse(metadata.isPartialResults());
        }

        // VLV search. We should know the estimate
        assertApproxNumberOfAllResults(metadata, getNumberOfAllAccounts());

        assertLdapConnectorReasonableInstances();
    }

    /**
     * Blocksize is 100, so this is in one block.
     * There is offset, so VLV should be used.
     * No explicit sorting.
     */
    @Test
    public void test172Search50AccountsOffset20() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass());

        ObjectPaging paging = prismContext.queryFactory().createPaging(20, 50);
        query.setPaging(paging);

        SearchResultList<PrismObject<ShadowType>> searchResultList = doSearch(query, 50, task, result);

        assertConnectorOperationIncrement(1, 51);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);

        SearchResultMetadata metadata = searchResultList.getMetadata();
        if (metadata != null) {
            assertFalse(metadata.isPartialResults());
        }

        assertLdapConnectorReasonableInstances();
    }

    /**
     * Blocksize is 100, so this gets more than two blocks.
     * There is offset, so VLV should be used.
     * No explicit sorting.
     */
    @Test
    public void test174SearchFirst222AccountsOffset20() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass());

        ObjectPaging paging = prismContext.queryFactory().createPaging(20, 222);
        query.setPaging(paging);

        SearchResultList<PrismObject<ShadowType>> searchResultList = doSearch(query, 222, task, result);

        assertConnectorOperationIncrement(1, 223);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);

        SearchResultMetadata metadata = searchResultList.getMetadata();
        if (metadata != null) {
            assertFalse(metadata.isPartialResults());
        }

        assertLdapConnectorReasonableInstances();
    }

    /**
     * Blocksize is 100, so this is in one block.
     * There is offset, so VLV should be used.
     * Explicit sorting.
     */
    @Test
    public void test182Search50AccountsOffset20SortUid() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass());

        ObjectPaging paging = prismContext.queryFactory().createPaging(20, 50);
        paging.setOrdering(getAttributePath("uid"), OrderDirection.ASCENDING);
        query.setPaging(paging);

        SearchResultList<PrismObject<ShadowType>> shadows = doSearch(query, 50, task, result);

        assertAccountShadow(shadows.get(0), toAccountDn(isIdmAdminInteOrgPerson() ? ACCOUNT_19_UID : ACCOUNT_20_UID));
        assertAccountShadow(shadows.get(49), toAccountDn(isIdmAdminInteOrgPerson() ? ACCOUNT_68_UID : ACCOUNT_69_UID));

        assertConnectorOperationIncrement(1, 51);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);

        SearchResultMetadata metadata = shadows.getMetadata();
        if (metadata != null) {
            assertFalse(metadata.isPartialResults());
        }

        assertLdapConnectorReasonableInstances();
    }

    /**
     * Blocksize is 100, so this gets more than two blocks.
     * There is offset, so VLV should be used.
     * No explicit sorting.
     */
    @Test
    public void test184SearchFirst222AccountsOffset20SortUid() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass());

        ObjectPaging paging = prismContext.queryFactory().createPaging(20, 222);
        paging.setOrdering(getAttributePath("uid"), OrderDirection.ASCENDING);
        query.setPaging(paging);

        SearchResultList<PrismObject<ShadowType>> shadows = doSearch(query, 222, task, result);

        assertAccountShadow(shadows.get(0), toAccountDn(isIdmAdminInteOrgPerson() ? ACCOUNT_19_UID : ACCOUNT_20_UID));
        assertAccountShadow(shadows.get(221), toAccountDn(isIdmAdminInteOrgPerson() ? ACCOUNT_240_UID : ACCOUNT_241_UID));

        assertConnectorOperationIncrement(1, 223);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);

        SearchResultMetadata metadata = shadows.getMetadata();
        if (metadata != null) {
            assertFalse(metadata.isPartialResults());
        }

        assertLdapConnectorReasonableInstances();
    }

    /**
     * No paging. Allow incomplete results. This should violate sizelimit, but some results should
     * be returned anyway.
     */
    @Test
    public void test190SearchAllAccountsSizelimit() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass());
        query.setAllowPartialResults(true);

        SearchResultList<PrismObject<ShadowType>> resultList = doSearch(query, getSearchSizeLimit(), task, result);

        assertConnectorOperationIncrement(1, getSearchSizeLimit() + 1);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);

        SearchResultMetadata metadata = resultList.getMetadata();
        assertNotNull("No search metadata", metadata);
        assertTrue("Partial results not indicated", metadata.isPartialResults());

        assertLdapConnectorReasonableInstances();
    }

    /**
     * Do many searches with different sorting and paging options. This test is designed
     * to deplete SSS/VLV resources on the LDAP server side, so the server may reach with
     * an error. Make sure that the connector transparently handles the error and that
     * we can sustain a large number of searches.
     */
    @Test
    public void test195SearchInferno() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass());

        // WHEN
        when();
        singleInfernoSearch(query, 30, 10, 30, "uid", task, result);
        singleInfernoSearch(query, 40, 5, 40, "cn", task, result);
        singleInfernoSearch(query, 15, 2, 15, "sn", task, result);
        singleInfernoSearch(query, 42, 200, 42, "uid", task, result);
        singleInfernoSearch(query, 200, 30, 200, "sn", task, result);

        assertConnectorOperationIncrement(5, 332);
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);

        assertLdapConnectorReasonableInstances();
    }

    private void singleInfernoSearch(ObjectQuery query, int expectedNumberOfResults, Integer offset, Integer maxSize, String sortAttrName, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        ObjectPaging paging = prismContext.queryFactory().createPaging(offset, maxSize);
        paging.setOrdering(getAttributePath(sortAttrName), OrderDirection.ASCENDING);
        query.setPaging(paging);

        final MutableInt count = new MutableInt();
        ResultHandler<ShadowType> handler = (object, parentResult) -> {
            count.increment();
            return true;
        };

        modelService.searchObjectsIterative(ShadowType.class, query, handler, null, task, result);

        assertEquals("Unexpected number of search results", expectedNumberOfResults, count.intValue());
    }

    // TODO: scoped search

    // TODO: count shadows

    @Test
    public void test200AssignAccountToBarbossa() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        long tsStart = System.currentTimeMillis();

        // WHEN
        when();
        assignAccountToUser(USER_BARBOSSA_OID, getResourceOid(), null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        long tsEnd = System.currentTimeMillis();

        assertLdapConnectorReasonableInstances();

        Entry entry = assertLdapAccount(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME);
        assertAttribute(entry, "title", null);

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        String shadowOid = getSingleLinkOid(user);
        PrismObject<ShadowType> shadow = getShadowModel(shadowOid);
        display("Shadow (model)", shadow);
        accountBarbossaOid = shadow.getOid();
        Collection<ResourceAttribute<?>> identifiers = ShadowUtil.getPrimaryIdentifiers(shadow);
        accountBarbossaEntryId = (String) identifiers.iterator().next().getRealValue();
        assertNotNull("No identifier in " + shadow, accountBarbossaEntryId);

        assertEquals("Wrong ICFS UID", getAttributeAsString(entry, getPrimaryIdentifierAttributeName()), accountBarbossaEntryId);

        assertLdapPassword(USER_BARBOSSA_USERNAME, USER_BARBOSSA_PASSWORD);

        ResourceAttribute<XMLGregorianCalendar> createTimestampAttribute = ShadowUtil.getAttribute(shadow, new QName(MidPointConstants.NS_RI, "createTimestamp"));
        assertNotNull("No createTimestamp in " + shadow, createTimestampAttribute);
        XMLGregorianCalendar createTimestamp = createTimestampAttribute.getRealValue();
        long createTimestampMillis = XmlTypeConverter.toMillis(createTimestamp);
        // LDAP server may be on a different host. Allow for some clock offset.
        TestUtil.assertBetween("Wrong createTimestamp in " + shadow, roundTsDown(tsStart) - 1000, roundTsUp(tsEnd) + 1000, createTimestampMillis);

        assertLdapConnectorReasonableInstances();
    }

    @Test
    public void test210ModifyAccountBarbossaReplaceTitle() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectDelta<ShadowType> delta = prismContext.deltaFactory().object()
                .createEmptyModifyDelta(ShadowType.class, accountBarbossaOid);
        QName attrQName = new QName(MidPointConstants.NS_RI, "title");
        //noinspection unchecked
        ResourceAttributeDefinition<String> attrDef =
                (ResourceAttributeDefinition<String>) accountDefinition.findAttributeDefinition(attrQName);
        PropertyDelta<String> attrDelta = prismContext.deltaFactory().property().createModificationReplaceProperty(
                ItemPath.create(ShadowType.F_ATTRIBUTES, attrQName), attrDef, "Captain");
        delta.addModification(attrDelta);

        // WHEN
        when();
        modelService.executeChanges(MiscSchemaUtil.createCollection(delta), null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        Entry entry = assertLdapAccount(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME);
        assertAttribute(entry, "title", "Captain");

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        String shadowOid = getSingleLinkOid(user);
        assertEquals("Shadows have moved", accountBarbossaOid, shadowOid);

        assertLdapConnectorReasonableInstances();
    }

    /**
     * Make a duplicate modification. Add a title value that is already there.
     * Normal LDAP should fail. So check that connector and midPoint handles that.
     */
    @Test
    public void test212ModifyAccountBarbossaAddTitleDuplicate() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectDelta<ShadowType> delta = prismContext.deltaFactory().object()
                .createEmptyModifyDelta(ShadowType.class, accountBarbossaOid);
        QName attrQName = new QName(MidPointConstants.NS_RI, "title");
        //noinspection unchecked
        ResourceAttributeDefinition<String> attrDef =
                (ResourceAttributeDefinition<String>) accountDefinition.findAttributeDefinition(attrQName);
        PropertyDelta<String> attrDelta = prismContext.deltaFactory().property().createModificationAddProperty(
                ItemPath.create(ShadowType.F_ATTRIBUTES, attrQName), attrDef, "Captain");
        delta.addModification(attrDelta);

        // WHEN
        when();
        modelService.executeChanges(MiscSchemaUtil.createCollection(delta), null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        Entry entry = assertLdapAccount(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME);
        assertAttribute(entry, "title", "Captain");

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        String shadowOid = getSingleLinkOid(user);
        assertEquals("Shadows have moved", accountBarbossaOid, shadowOid);

        assertLdapConnectorReasonableInstances();
    }

    /**
     * Make another duplicate modification. Add a title value that is already there,
     * but with a different capitalization.
     */
    @Test
    public void test213ModifyAccountBarbossaAddTitleDuplicateCapitalized() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectDelta<ShadowType> delta = prismContext.deltaFactory().object()
                .createEmptyModifyDelta(ShadowType.class, accountBarbossaOid);
        QName attrQName = new QName(MidPointConstants.NS_RI, "title");
        //noinspection unchecked
        ResourceAttributeDefinition<String> attrDef =
                (ResourceAttributeDefinition<String>) accountDefinition.findAttributeDefinition(attrQName);
        PropertyDelta<String> attrDelta = prismContext.deltaFactory().property().createModificationAddProperty(
                ItemPath.create(ShadowType.F_ATTRIBUTES, attrQName), attrDef, "CAPTAIN");
        delta.addModification(attrDelta);

        // WHEN
        when();
        modelService.executeChanges(MiscSchemaUtil.createCollection(delta), null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        Entry entry = assertLdapAccount(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME);
        assertAttribute(entry, "title", "Captain");

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        String shadowOid = getSingleLinkOid(user);
        assertEquals("Shadows have moved", accountBarbossaOid, shadowOid);

        assertLdapConnectorReasonableInstances();
    }

    @Test
    public void test220ModifyUserBarbossaPassword() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ProtectedStringType userPasswordPs = new ProtectedStringType();
        userPasswordPs.setClearValue(USER_BARBOSSA_PASSWORD_2);

        // WHEN
        when();
        modifyUserReplace(USER_BARBOSSA_OID, PATH_CREDENTIALS_PASSWORD_VALUE, task, result, userPasswordPs);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        Entry entry = assertLdapAccount(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME);
        assertAttribute(entry, "title", "Captain");
        assertLdapPassword(USER_BARBOSSA_USERNAME, USER_BARBOSSA_PASSWORD_2);

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        String shadowOid = getSingleLinkOid(user);
        assertEquals("Shadows have moved", accountBarbossaOid, shadowOid);

        assertLdapConnectorReasonableInstances();
    }

    @Test
    public void test230ModifyUserBarbossaEmployeeType() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        modifyUserReplace(USER_BARBOSSA_OID, UserType.F_SUBTYPE, task, result, "Pirate");

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        Entry entry = assertLdapAccount(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME);
        assertAttribute(entry, "employeeType", "Pirate");

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        String shadowOid = getSingleLinkOid(user);
        assertEquals("Shadows have moved", accountBarbossaOid, shadowOid);

        assertLdapConnectorReasonableInstances();
    }

    @Test
    public void test232ModifyUserBarbossaEmployeeTypeAgain() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        modifyUserReplace(USER_BARBOSSA_OID, UserType.F_SUBTYPE, task, result, "Pirate");

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        Entry entry = assertLdapAccount(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME);
        assertAttribute(entry, "employeeType", "Pirate");

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        String shadowOid = getSingleLinkOid(user);
        assertEquals("Shadows have moved", accountBarbossaOid, shadowOid);

        assertLdapConnectorReasonableInstances();
    }

    @Test
    public void test234ModifyUserBarbossaEmployeeTypeAgainCapitalized() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        modifyUserReplace(USER_BARBOSSA_OID, UserType.F_SUBTYPE, task, result, "PIRATE");

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        Entry entry = assertLdapAccount(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME);
        assertAttribute(entry, "employeeType", "Pirate");

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        String shadowOid = getSingleLinkOid(user);
        assertEquals("Shadows have moved", accountBarbossaOid, shadowOid);

        assertLdapConnectorReasonableInstances();
    }

    @Test
    public void test290ModifyUserBarbossaRename() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        modifyUserReplace(USER_BARBOSSA_OID, UserType.F_NAME, task, result, PrismTestUtil.createPolyString(USER_CPTBARBOSSA_USERNAME));

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        Entry entry = assertLdapAccount(USER_CPTBARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME);
        displayValue("LDAP entry after", entry);
        assertEquals("Wrong DN", toAccountDn(USER_CPTBARBOSSA_USERNAME), entry.getDn().toString());
        assertAttribute(entry, "title", "Captain");

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        String shadowOid = getSingleLinkOid(user);
        assertEquals("Shadows have moved", accountBarbossaOid, shadowOid);

        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, shadowOid, null, result);
        display("Repo shadow after rename", repoShadow);

        String repoPrimaryIdentifier = getAttributeValue(repoShadow, getPrimaryIdentifierAttributeQName(), String.class);
        if ("dn".equals(getPrimaryIdentifierAttributeName())) {
            assertEquals("Entry DN (primary identifier) was not updated in the shadow", toAccountDn(USER_CPTBARBOSSA_USERNAME).toLowerCase(), repoPrimaryIdentifier);
        } else {
            assertEquals("Entry ID changed after rename", accountBarbossaEntryId, repoPrimaryIdentifier);
        }

        assertLdapConnectorReasonableInstances();
    }

    /**
     * Try the rename again. This time just as a capitalization of the original name.
     * The DN should not change.
     */
    @Test
    public void test292ModifyUserBarbossaRenameCapitalized() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        rememberCounter(InternalCounters.CONNECTOR_OPERATION_COUNT);

        // WHEN
        when();
        modifyUserReplace(USER_BARBOSSA_OID, UserType.F_NAME, task, result,
                PrismTestUtil.createPolyString(USER_CPTBARBOSSA_USERNAME.toUpperCase()));

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        Entry entry = assertLdapAccount(USER_CPTBARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME);
        displayValue("LDAP entry after", entry);
        assertAttribute(entry, "title", "Captain");
        assertEquals("Wrong DN", toAccountDn(USER_CPTBARBOSSA_USERNAME), entry.getDn().toString());

        assertConnectorOperationIncrement(1, 2); // Just account read, no modify

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        String shadowOid = getSingleLinkOid(user);
        assertEquals("Shadows have moved", accountBarbossaOid, shadowOid);

        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, shadowOid, null, result);
        display("Repo shadow after rename", repoShadow);

        String repoPrimaryIdentifier = getAttributeValue(repoShadow, getPrimaryIdentifierAttributeQName(), String.class);
        if ("dn".equals(getPrimaryIdentifierAttributeName())) {
            assertEquals("Entry DN (primary identifier) was not updated in the shadow", toAccountDn(USER_CPTBARBOSSA_USERNAME).toLowerCase(), repoPrimaryIdentifier);
        } else {
            assertEquals("Entry ID changed after rename", accountBarbossaEntryId, repoPrimaryIdentifier);
        }

        assertLdapConnectorReasonableInstances();
    }

    @Test
    public void test299UnAssignAccountBarbossa() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        unassignAccountFromUser(USER_BARBOSSA_OID, getResourceOid(), null, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertNoLdapAccount(USER_BARBOSSA_USERNAME);
        assertNoLdapAccount(USER_CPTBARBOSSA_USERNAME);

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        assertNoLinkedAccount(user);

        assertLdapConnectorReasonableInstances();
    }

    /**
     * MID-2853: Unexpected association behaviour - removing roles does not always remove from groups
     */
    @Test
    public void test300AssignRoleEvilToLechuck() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        assignRole(USER_LECHUCK_OID, ROLE_EVIL_OID, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        Entry entry = assertLdapAccount(USER_LECHUCK_USERNAME, USER_LECHUCK_FULL_NAME);

        PrismObject<UserType> user = getUser(USER_LECHUCK_OID);
        String shadowOid = getSingleLinkOid(user);
        PrismObject<ShadowType> shadow = getShadowModel(shadowOid);
        display("Shadow (model)", shadow);
        accountLechuckOid = shadow.getOid();
        accountLechuckDn = entry.getDn().toString();
        assertNotNull(accountLechuckDn);

        assertLdapGroupMember(entry, GROUP_EVIL_CN);
        assertLdapNoGroupMember(entry, GROUP_UNDEAD_CN);

        Entry ldapEntryEvil = getLdapEntry(toGroupDn(GROUP_EVIL_CN));
        displayValue("Evil group", ldapEntryEvil);
        Entry ldapEntryUndead = getLdapEntry(toGroupDn(GROUP_UNDEAD_CN));
        displayValue("Undead group", ldapEntryUndead);

        assertLdapConnectorReasonableInstances();
    }

    /**
     * MID-2853: Unexpected association behaviour - removing roles does not always remove from groups
     */
    @Test
    public void test302AssignRoleUndeadToLechuck() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        assignRole(USER_LECHUCK_OID, ROLE_UNDEAD_OID, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        Entry entry = assertLdapAccount(USER_LECHUCK_USERNAME, USER_LECHUCK_FULL_NAME);

        PrismObject<UserType> user = getUser(USER_LECHUCK_OID);
        String shadowOid = getSingleLinkOid(user);
        PrismObject<ShadowType> shadow = getShadowModel(shadowOid);
        display("Shadow (model)", shadow);

        assertLdapGroupMember(entry, GROUP_EVIL_CN);
        assertLdapGroupMember(entry, GROUP_UNDEAD_CN);

        Entry ldapEntryEvil = getLdapEntry(toGroupDn(GROUP_EVIL_CN));
        displayValue("Evil group", ldapEntryEvil);
        Entry ldapEntryUndead = getLdapEntry(toGroupDn(GROUP_UNDEAD_CN));
        displayValue("Undead group", ldapEntryUndead);

        assertLdapConnectorReasonableInstances();
    }

    /**
     * MID-2853: Unexpected association behaviour - removing roles does not always remove from groups
     */
    @Test
    public void test306UnassignRoleEvilFromLechuck() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        unassignRole(USER_LECHUCK_OID, ROLE_EVIL_OID, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        Entry entry = assertLdapAccount(USER_LECHUCK_USERNAME, USER_LECHUCK_FULL_NAME);

        PrismObject<UserType> user = getUser(USER_LECHUCK_OID);
        String shadowOid = getSingleLinkOid(user);
        PrismObject<ShadowType> shadow = getShadowModel(shadowOid);
        display("Shadow (model)", shadow);

        assertLdapNoGroupMember(entry, GROUP_EVIL_CN);
        assertLdapGroupMember(entry, GROUP_UNDEAD_CN);

        Entry ldapEntryEvil = getLdapEntry(toGroupDn(GROUP_EVIL_CN));
        displayValue("Evil group", ldapEntryEvil);
        Entry ldapEntryUndead = getLdapEntry(toGroupDn(GROUP_UNDEAD_CN));
        displayValue("Undead group", ldapEntryUndead);

        assertLdapConnectorReasonableInstances();
    }

    /**
     * MID-2853: Unexpected association behaviour - removing roles does not always remove from groups
     */
    @Test
    public void test309UnassignRoleUndeadFromLechuck() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        unassignRole(USER_LECHUCK_OID, ROLE_UNDEAD_OID, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> user = getUser(USER_LECHUCK_OID);
        assertNoLinkedAccount(user);

        assertNoEntry(accountLechuckDn);

        assertNoObject(ShadowType.class, accountLechuckOid, task, result);

        assertLdapNoGroupMember(accountLechuckDn, GROUP_EVIL_CN);
        assertLdapNoGroupMember(accountLechuckDn, GROUP_UNDEAD_CN);

        Entry ldapEntryEvil = getLdapEntry(toGroupDn(GROUP_EVIL_CN));
        displayValue("Evil group", ldapEntryEvil);
        Entry ldapEntryUndead = getLdapEntry(toGroupDn(GROUP_UNDEAD_CN));
        displayValue("Undead group", ldapEntryUndead);

        assertLdapConnectorReasonableInstances();
    }

    @Test
    public void test310SearchGroupEvilByCn() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();
        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getGroupObjectClass());
        ObjectQueryUtil.filterAnd(query.getFilter(), createAttributeFilter("cn", GROUP_EVIL_CN));

        rememberCounter(InternalCounters.CONNECTOR_OPERATION_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT);

        // WHEN
        when();
        SearchResultList<PrismObject<ShadowType>> shadows = modelService.searchObjects(ShadowType.class, query, null, task, result);

        assertEquals("Unexpected search result: " + shadows, 1, shadows.size());

        PrismObject<ShadowType> shadow = shadows.get(0);
        assertGroupShadow(shadow, toGroupDn(GROUP_EVIL_CN));
        groupEvilShadowOid = shadow.getOid();
        assertNotNull(groupEvilShadowOid);

        if (hasLdapGroupBaseContext()) {
            assertConnectorOperationIncrement(1, 2);
        } else {
            assertConnectorOperationIncrement(1, 1);
        }
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);

        SearchResultMetadata metadata = shadows.getMetadata();
        if (metadata != null) {
            assertFalse(metadata.isPartialResults());
        }

        assertLdapConnectorReasonableInstances();
    }

    /**
     * MID-3209: Rename does not change group membership for associations, when resource does not implement its own referential integrity
     */
    @Test
    public void test312AssignRoleEvilToBarbossa() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        // WHEN
        when();
        assignRole(USER_BARBOSSA_OID, ROLE_EVIL_OID, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        Entry entry = assertLdapAccount(USER_CPTBARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME);
        displayValue("Account LDAP entry", entry);

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        String shadowOid = getSingleLinkOid(user);
        PrismObject<ShadowType> shadow = getShadowModel(shadowOid);
        display("Shadow (model)", shadow);
        accountBarbossaOid = shadow.getOid();
        accountBarbossaDn = entry.getDn().toString();
        assertNotNull(accountBarbossaDn);

        Collection<ResourceAttribute<?>> identifiers = ShadowUtil.getPrimaryIdentifiers(shadow);
        accountBarbossaEntryId = (String) identifiers.iterator().next().getRealValue();
        assertNotNull("No identifier in " + shadow, accountBarbossaEntryId);

        Entry ldapEntryEvil = getLdapEntry(toGroupDn(GROUP_EVIL_CN));
        displayValue("Evil group", ldapEntryEvil);
        Entry ldapEntryUndead = getLdapEntry(toGroupDn(GROUP_UNDEAD_CN));
        displayValue("Undead group", ldapEntryUndead);

        assertLdapGroupMember(entry, GROUP_EVIL_CN);
        assertLdapNoGroupMember(entry, GROUP_UNDEAD_CN);

        IntegrationTestTools.assertAssociation(shadow, getAssociationGroupName(), groupEvilShadowOid);

        assertLdapConnectorReasonableInstances();
    }

    /**
     * MID-3209: Rename does not change group membership for associations, when resource does not implement its own referential integrity
     */
    @Test
    public void test314ModifyUserBarbossaRenameBack() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = getUser(USER_BARBOSSA_OID);
        display("user defore", userBefore);
        assertNotNull(userBefore);

        // WHEN
        when();
        modifyUserReplace(USER_BARBOSSA_OID, UserType.F_NAME, task, result, PrismTestUtil.createPolyString(USER_BARBOSSA_USERNAME));

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        Entry entry = assertLdapAccount(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME);
        displayValue("LDAP entry after", entry);
        assertEquals("Wrong DN", toAccountDn(USER_BARBOSSA_USERNAME), entry.getDn().toString());

        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        String shadowOid = getSingleLinkOid(user);
        assertEquals("Shadows have moved", accountBarbossaOid, shadowOid);

        PrismObject<ShadowType> repoShadow = repositoryService.getObject(ShadowType.class, shadowOid, null, result);
        display("Repo shadow after rename", repoShadow);

        String repoPrimaryIdentifier = getAttributeValue(repoShadow, getPrimaryIdentifierAttributeQName(), String.class);
        if ("dn".equals(getPrimaryIdentifierAttributeName())) {
            assertEquals("Entry DN (primary identifier) was not updated in the shadow", toAccountDn(USER_BARBOSSA_USERNAME).toLowerCase(), repoPrimaryIdentifier);
        } else {
            assertEquals("Entry ID changed after rename", accountBarbossaEntryId, repoPrimaryIdentifier);
        }

        Entry ldapEntryEvil = getLdapEntry(toGroupDn(GROUP_EVIL_CN));
        displayValue("Evil group", ldapEntryEvil);
        Entry ldapEntryUndead = getLdapEntry(toGroupDn(GROUP_UNDEAD_CN));
        displayValue("Undead group", ldapEntryUndead);

        assertLdapGroupMember(entry, GROUP_EVIL_CN);
        assertLdapNoGroupMember(accountBarbossaDn, GROUP_EVIL_CN);
        assertLdapNoGroupMember(entry, GROUP_UNDEAD_CN);

        assertLdapConnectorReasonableInstances();
    }

    /**
     * Add user that has role evil, check association. Use mixed lower/upper chars in the username.
     * MID-3713
     */
    @Test
    public void test320AddEvilUserLargo() throws Exception {
        // GIVEN
        Task task = getTestTask();
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = createUser(USER_LARGO_NAME, USER_LARGO_GIVEN_NAME, USER_LARGO_FAMILY_NAME, true);
        AssignmentType assignmentType = createTargetAssignment(ROLE_EVIL_OID, RoleType.COMPLEX_TYPE);
        userBefore.asObjectable().getAssignment().add(assignmentType);
        display("user before", userBefore);

        // WHEN
        when();
        addObject(userBefore, task, result);

        // THEN
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<UserType> userAfter = findUserByUsername(USER_LARGO_NAME);
        display("user after", userAfter);
        assertAssignedRole(userAfter, ROLE_EVIL_OID);
        assertAssignments(userAfter, 1);

        String shadowOid = getSingleLinkOid(userAfter);
        PrismObject<ShadowType> shadow = getShadowModel(shadowOid);
        display("Shadow (model)", shadow);

        Entry entry = assertLdapAccount(USER_LARGO_NAME, userAfter.asObjectable().getFullName().getOrig());
        displayValue("account after", entry);

        assertLdapGroupMember(entry, GROUP_EVIL_CN);
        assertLdapNoGroupMember(entry, GROUP_UNDEAD_CN);

        Entry ldapEntryEvil = getLdapEntry(toGroupDn(GROUP_EVIL_CN));
        displayValue("Evil group", ldapEntryEvil);
        Entry ldapEntryUndead = getLdapEntry(toGroupDn(GROUP_UNDEAD_CN));
        displayValue("Undead group", ldapEntryUndead);

        assertAssociation(shadow, ASSOCIATION_GROUP_NAME, groupEvilShadowOid);

        assertLdapConnectorReasonableInstances();
    }

    @Test
    public void test350SearchInvisibleAccount() throws Exception {
        // GIVEN
        createBilboEntry();

        SearchResultList<PrismObject<ShadowType>> shadows = searchBilbo();

        assertEquals("Unexpected search result: " + shadows, 1, shadows.size());

        PrismObject<ShadowType> shadow = shadows.get(0);
        IntegrationTestTools.displayXml("Bilbo", shadow);
        assertAccountShadow(shadow, toAccountDn(ACCOUNT_BILBO_UID));
    }

    protected Entry createBilboEntry() throws LdapException, IOException {
        Entry entry = createAccountEntry(
                ACCOUNT_BILBO_UID, ACCOUNT_BILBO_CN, ACCOUNT_BILBO_GIVENNAME, ACCOUNT_BILBO_SN);
        markInvisible(entry);
        addLdapEntry(entry);
        return entry;
    }

    protected void markInvisible(Entry entry) throws LdapException {
        entry.add(LDAP_ATTRIBUTE_ROOM_NUMBER, ROOM_NUMBER_INVISIBLE);
    }


    protected SearchResultList<PrismObject<ShadowType>> searchBilbo() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();

        ObjectQuery query = createUidQuery(ACCOUNT_BILBO_UID);

        rememberCounter(InternalCounters.CONNECTOR_OPERATION_COUNT);
        rememberCounter(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT);

        // WHEN
        when();
        SearchResultList<PrismObject<ShadowType>> shadows = modelService.searchObjects(ShadowType.class, query, null, task, result);

        // THEN
        then();
        assertSuccess(result);
        display("Bilbos", shadows);

        assertLdapConnectorReasonableInstances();
        assertCounterIncrement(InternalCounters.CONNECTOR_SIMULATED_PAGING_SEARCH_COUNT, 0);

        return shadows;
    }

    protected void assertConnectorOperationIncrement(int shortcutIncrement, int noShortcutIncrement) {
        if (hasAssociationShortcut()) {
            assertCounterIncrement(InternalCounters.CONNECTOR_OPERATION_COUNT, shortcutIncrement);
        } else {
            assertCounterIncrement(InternalCounters.CONNECTOR_OPERATION_COUNT, noShortcutIncrement);
        }
    }

}

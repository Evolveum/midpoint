/*
 * Copyright (c) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql;

import static java.util.Collections.singleton;
import static org.testng.AssertJUnit.*;

import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.RI_ACCOUNT_OBJECT_CLASS;

import java.io.File;
import java.util.*;
import javax.xml.namespace.QName;

import jakarta.persistence.EntityManager;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.repo.api.RepoModifyOptions;
import com.evolveum.midpoint.repo.sql.data.common.RShadow;
import com.evolveum.midpoint.repo.sql.data.common.RUser;
import com.evolveum.midpoint.repo.sql.data.common.any.RAssignmentExtension;
import com.evolveum.midpoint.repo.sql.data.common.any.RExtItem;
import com.evolveum.midpoint.repo.sql.data.common.container.RAssignment;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RetrieveOption;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Comprehensive test for extension and attribute values processing.
 * Introduced as part of providing "index-only" extension values (MID-5558)
 * and related refactoring of ObjectDeltaUpdater.
 */
@ContextConfiguration(locations = { "../../../../../ctx-test.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ExtensionTest extends BaseSQLRepoTest {

    private static final File TEST_DIR = new File("src/test/resources/extension");
    private static final File USER_RUMCAJS_FILE = new File(TEST_DIR, "user-rumcajs.xml");
    private static final File USER_MANKA_FILE = new File(TEST_DIR, "user-manka.xml");

    private PrismObject<UserType> expectedUser;
    private String userOid;

    private RExtItem itemHidden1;
    private RExtItem itemHidden2;
    private RExtItem itemHidden3;
    private RExtItem itemVisible;
    private RExtItem itemWeapon;
    private RExtItem itemShipName;

    private PrismObject<ShadowType> expectedShadow;
    private String shadowOid;

    private ResourceAttributeDefinition<String> attrGroupNameDefinition;
    private ResourceAttributeDefinition<String> attrMemberDefinition;
    private ResourceAttributeDefinition<String> attrManagerDefinition;

    private RExtItem itemGroupName;
    private RExtItem itemMember;
    private RExtItem itemManager;

    private PrismObjectDefinition<ShadowType> shadowDefinition;
    private ResourceAttributeContainerDefinition shadowAttributesDefinition;

    @Override
    public void initSystem() throws Exception {
        itemHidden1 = createOrFindExtensionItemDefinition(UserType.class, EXT_HIDDEN1);
        itemHidden2 = createOrFindExtensionItemDefinition(UserType.class, EXT_HIDDEN2);
        itemHidden3 = createOrFindExtensionItemDefinition(UserType.class, EXT_HIDDEN3);
        itemVisible = createOrFindExtensionItemDefinition(UserType.class, EXT_VISIBLE);
        itemWeapon = createOrFindExtensionItemDefinition(UserType.class, EXT_WEAPON);
        itemShipName = createOrFindExtensionItemDefinition(UserType.class, EXT_SHIP_NAME);

        sqlRepositoryService.sqlConfiguration().setEnableNoFetchExtensionValuesInsertion(true);
        sqlRepositoryService.sqlConfiguration().setEnableNoFetchExtensionValuesDeletion(true);
        sqlRepositoryService.sqlConfiguration().setEnableIndexOnlyItems(true);

        createShadowDefinition();
    }

    private void createShadowDefinition() {
        ResourceObjectClassDefinitionImpl ctd = ResourceObjectClassDefinitionImpl.raw(RI_ACCOUNT_OBJECT_CLASS);
        attrGroupNameDefinition = ctd.createAttributeDefinition(
                ATTR_GROUP_NAME, DOMUtil.XSD_STRING,
                def -> def.setMaxOccurs(1));
        attrMemberDefinition = ctd.createAttributeDefinition(
                ATTR_MEMBER, DOMUtil.XSD_STRING,
                def -> {
                    def.setMaxOccurs(-1);
                    def.setIndexOnly(true);
                });
        attrManagerDefinition = ctd.createAttributeDefinition(
                ATTR_MANAGER, DOMUtil.XSD_STRING,
                def -> def.setMaxOccurs(-1));

        shadowAttributesDefinition = new ResourceAttributeContainerDefinitionImpl(ShadowType.F_ATTRIBUTES, ctd);
        shadowDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ShadowType.class)
                .cloneWithReplacedDefinition(ShadowType.F_ATTRIBUTES, shadowAttributesDefinition);
        itemGroupName = extItemDictionary.createOrFindItemDefinition(attrGroupNameDefinition, false);
        itemMember = extItemDictionary.createOrFindItemDefinition(attrMemberDefinition, false);
        itemManager = extItemDictionary.createOrFindItemDefinition(attrManagerDefinition, false);
    }

    boolean isNoFetchInsertion() {
        return true;    // this is the default in production
    }

    boolean isNoFetchDeletion() {
        return false;       // this is the default in production
    }

    int getExtraSafeInsertionSelects(int insertions) {
        return 0;
    }

    private RepoModifyOptions getOptions() {
        RepoModifyOptions options = new RepoModifyOptions();
        options.setUseNoFetchExtensionValuesInsertion(isNoFetchInsertion());
        options.setUseNoFetchExtensionValuesDeletion(isNoFetchDeletion());
        return options;
    }

    private boolean checkCounts() {
        return false;
        // Hibernate 6 generates one queries less
        //return isUsingH2();
    }

    private void assertCounts(int queries, int executions) {
        if (checkCounts()) {
            assertEquals("Wrong # of queries", queries, queryListener.getQueryCount());
            assertEquals("Wrong # of executions", executions, queryListener.getExecutionCount());
        }
    }

    @Test
    public void test010AddUser() throws Exception {
        OperationResult result = new OperationResult(ExtensionTest.class.getName() + ".test010AddUser");

        PrismObject<UserType> user = PrismTestUtil.parseObject(USER_RUMCAJS_FILE);
        expectedUser = PrismTestUtil.parseObject(USER_RUMCAJS_FILE);

        queryListener.start();
        userOid = repositoryService.addObject(user, null, result);
        queryListener.dumpAndStop();

        try (EntityManager em = factory.createEntityManager()) {
            RUser u = em.find(RUser.class, userOid);
            assertExtension(u, itemHidden1, "h1.1", "h1.2", "h1.3");
            assertExtension(u, itemHidden2);
            assertExtension(u, itemHidden3, "h3.1");
            assertExtension(u, itemVisible, "v1", "v2", "v3");
            assertExtension(u, itemWeapon);
        }

        assertGetObject(result);
        assertSearch(EXT_HIDDEN1, "h1.1", 1, result);
        assertSearch(EXT_HIDDEN1, "h1.2", 1, result);
        assertSearch(EXT_HIDDEN1, "h1.3", 1, result);
        assertSearch(EXT_HIDDEN1, "h1.4", 0, result);
        assertSearch(EXT_HIDDEN2, "h2.1", 0, result);
        assertSearch(EXT_HIDDEN3, "h3.1", 1, result);
        assertSearch(EXT_HIDDEN3, "h1.3", 0, result);

        /*
 [1] select count(*) from m_user where oid=?
 [1] insert into m_object (createChannel, createTimestamp, creatorRef_relation, creatorRef_targetOid, creatorRef_type, fullObject, lifecycleState, modifierRef_relation, modifierRef_targetOid, modifierRef_type, modifyChannel, modifyTimestamp, name_norm, name_orig, objectTypeClass, tenantRef_relation, tenantRef_targetOid, tenantRef_type, version, oid) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
 [1] insert into m_focus (administrativeStatus, archiveTimestamp, disableReason, disableTimestamp, effectiveStatus, enableTimestamp, validFrom, validTo, validityChangeTimestamp, validityStatus, costCenter, emailAddress, hasPhoto, locale, locality_norm, locality_orig, preferredLanguage, telephoneNumber, timezone, oid) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
 [1] insert into m_user (additionalName_norm, additionalName_orig, employeeNumber, familyName_norm, familyName_orig, fullName_norm, fullName_orig, givenName_norm, givenName_orig, honorificPrefix_norm, honorificPrefix_orig, honorificSuffix_norm, honorificSuffix_orig, name_norm, name_orig, nickName_norm, nickName_orig, title_norm, title_orig, oid) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
 [7] insert into m_object_ext_string (item_id, owner_oid, ownerType, stringValue) values (?, ?, ?, ?)
         */
        assertCounts(5, 11);
    }

    @Test
    public void test020AddVisibleUserExtensionValue() throws Exception {
        OperationResult result = new OperationResult(ExtensionTest.class.getName() + ".test020AddVisibleUserExtensionValue");

        ObjectDelta<UserType> delta = getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_EXTENSION, EXT_VISIBLE)
                .add("v4", "v5")
                .asObjectDelta("");
        delta.applyTo(expectedUser);

        queryListener.start();
        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), getOptions(), result);
        queryListener.dumpAndStop();

        try (EntityManager em = factory.createEntityManager()) {
            RUser u = em.find(RUser.class, userOid);
            assertExtension(u, itemHidden1, "h1.1", "h1.2", "h1.3");
            assertExtension(u, itemHidden2);
            assertExtension(u, itemHidden3, "h3.1");
            assertExtension(u, itemVisible, "v1", "v2", "v3", "v4", "v5");
            assertExtension(u, itemWeapon);
        }

        assertGetObject(result);
        assertSearch(EXT_HIDDEN1, "h1.1", 1, result);
        assertSearch(EXT_HIDDEN1, "h1.2", 1, result);
        assertSearch(EXT_HIDDEN1, "h1.3", 1, result);
        assertSearch(EXT_HIDDEN1, "h1.4", 0, result);
        assertSearch(EXT_HIDDEN2, "h2.1", 0, result);
        assertSearch(EXT_HIDDEN3, "h3.1", 1, result);
        assertSearch(EXT_HIDDEN3, "h1.3", 0, result);
        assertSearch(EXT_VISIBLE, "v1", 1, result);
        assertSearch(EXT_VISIBLE, "v2", 1, result);
        assertSearch(EXT_VISIBLE, "v3", 1, result);
        assertSearch(EXT_VISIBLE, "v4", 1, result);
        assertSearch(EXT_VISIBLE, "v5", 1, result);
        assertSearch(EXT_VISIBLE, "v6", 0, result);

        /*
 [1] select oid from m_object where oid = ? for update
 [1] select ruser0_.oid as oid1_27_, ruser0_2_.createChannel as createCh2_27_, ruser0_2_.createTimestamp as createTi3_27_, ruser0_2_.creatorRef_relation as creatorR4_27_, ruser0_2_.creatorRef_targetOid as creatorR5_27_, ruser0_2_.creatorRef_type as creatorR6_27_, ruser0_2_.fullObject as fullObje7_27_, ruser0_2_.lifecycleState as lifecycl8_27_, ruser0_2_.modifierRef_relation as modifier9_27_, ruser0_2_.modifierRef_targetOid as modifie10_27_, ruser0_2_.modifierRef_type as modifie11_27_, ruser0_2_.modifyChannel as modifyC12_27_, ruser0_2_.modifyTimestamp as modifyT13_27_, ruser0_2_.name_norm as name_no14_27_, ruser0_2_.name_orig as name_or15_27_, ruser0_2_.objectTypeClass as objectT16_27_, ruser0_2_.tenantRef_relation as tenantR17_27_, ruser0_2_.tenantRef_targetOid as tenantR18_27_, ruser0_2_.tenantRef_type as tenantR19_27_, ruser0_2_.version as version20_27_, ruser0_1_.administrativeStatus as administ1_53_, ruser0_1_.archiveTimestamp as archiveT2_53_, ruser0_1_.disableReason as disableR3_53_, ruser0_1_.disableTimestamp as disableT4_53_, ruser0_1_.effectiveStatus as effectiv5_53_, ruser0_1_.enableTimestamp as enableTi6_53_, ruser0_1_.validFrom as validFro7_53_, ruser0_1_.validTo as validTo8_53_, ruser0_1_.validityChangeTimestamp as validity9_53_, ruser0_1_.validityStatus as validit10_53_, ruser0_1_.costCenter as costCen11_53_, ruser0_1_.emailAddress as emailAd12_53_, ruser0_1_.hasPhoto as hasPhot13_53_, ruser0_1_.locale as locale14_53_, ruser0_1_.locality_norm as localit15_53_, ruser0_1_.locality_orig as localit16_53_, ruser0_1_.preferredLanguage as preferr17_53_, ruser0_1_.telephoneNumber as telepho18_53_, ruser0_1_.timezone as timezon19_53_, ruser0_.additionalName_norm as addition1_73_, ruser0_.additionalName_orig as addition2_73_, ruser0_.employeeNumber as employee3_73_, ruser0_.familyName_norm as familyNa4_73_, ruser0_.familyName_orig as familyNa5_73_, ruser0_.fullName_norm as fullName6_73_, ruser0_.fullName_orig as fullName7_73_, ruser0_.givenName_norm as givenNam8_73_, ruser0_.givenName_orig as givenNam9_73_, ruser0_.honorificPrefix_norm as honorif10_73_, ruser0_.honorificPrefix_orig as honorif11_73_, ruser0_.honorificSuffix_norm as honorif12_73_, ruser0_.honorificSuffix_orig as honorif13_73_, ruser0_.name_norm as name_no14_73_, ruser0_.name_orig as name_or15_73_, ruser0_.nickName_norm as nickNam16_73_, ruser0_.nickName_orig as nickNam17_73_, ruser0_.title_norm as title_n18_73_, ruser0_.title_orig as title_o19_73_ from m_user ruser0_ inner join m_focus ruser0_1_ on ruser0_.oid=ruser0_1_.oid inner join m_object ruser0_2_ on ruser0_.oid=ruser0_2_.oid where ruser0_.oid=?
 [2] insert into m_object_ext_string (item_id, owner_oid, ownerType, stringValue) values (?, ?, ?, ?)
 [1] update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?

        safe insertions:

 [1] select oid from m_object where oid = ? for update
 [1] select ruser0_.oid as oid1_27_, ruser0_2_.createChannel as createCh2_27_, ruser0_2_.createTimestamp as createTi3_27_, ruser0_2_.creatorRef_relation as creatorR4_27_, ruser0_2_.creatorRef_targetOid as creatorR5_27_, ruser0_2_.creatorRef_type as creatorR6_27_, ruser0_2_.fullObject as fullObje7_27_, ruser0_2_.lifecycleState as lifecycl8_27_, ruser0_2_.modifierRef_relation as modifier9_27_, ruser0_2_.modifierRef_targetOid as modifie10_27_, ruser0_2_.modifierRef_type as modifie11_27_, ruser0_2_.modifyChannel as modifyC12_27_, ruser0_2_.modifyTimestamp as modifyT13_27_, ruser0_2_.name_norm as name_no14_27_, ruser0_2_.name_orig as name_or15_27_, ruser0_2_.objectTypeClass as objectT16_27_, ruser0_2_.tenantRef_relation as tenantR17_27_, ruser0_2_.tenantRef_targetOid as tenantR18_27_, ruser0_2_.tenantRef_type as tenantR19_27_, ruser0_2_.version as version20_27_, ruser0_1_.administrativeStatus as administ1_53_, ruser0_1_.archiveTimestamp as archiveT2_53_, ruser0_1_.disableReason as disableR3_53_, ruser0_1_.disableTimestamp as disableT4_53_, ruser0_1_.effectiveStatus as effectiv5_53_, ruser0_1_.enableTimestamp as enableTi6_53_, ruser0_1_.validFrom as validFro7_53_, ruser0_1_.validTo as validTo8_53_, ruser0_1_.validityChangeTimestamp as validity9_53_, ruser0_1_.validityStatus as validit10_53_, ruser0_1_.costCenter as costCen11_53_, ruser0_1_.emailAddress as emailAd12_53_, ruser0_1_.hasPhoto as hasPhot13_53_, ruser0_1_.locale as locale14_53_, ruser0_1_.locality_norm as localit15_53_, ruser0_1_.locality_orig as localit16_53_, ruser0_1_.preferredLanguage as preferr17_53_, ruser0_1_.telephoneNumber as telepho18_53_, ruser0_1_.timezone as timezon19_53_, ruser0_.additionalName_norm as addition1_73_, ruser0_.additionalName_orig as addition2_73_, ruser0_.employeeNumber as employee3_73_, ruser0_.familyName_norm as familyNa4_73_, ruser0_.familyName_orig as familyNa5_73_, ruser0_.fullName_norm as fullName6_73_, ruser0_.fullName_orig as fullName7_73_, ruser0_.givenName_norm as givenNam8_73_, ruser0_.givenName_orig as givenNam9_73_, ruser0_.honorificPrefix_norm as honorif10_73_, ruser0_.honorificPrefix_orig as honorif11_73_, ruser0_.honorificSuffix_norm as honorif12_73_, ruser0_.honorificSuffix_orig as honorif13_73_, ruser0_.name_norm as name_no14_73_, ruser0_.name_orig as name_or15_73_, ruser0_.nickName_norm as nickNam16_73_, ruser0_.nickName_orig as nickNam17_73_, ruser0_.title_norm as title_n18_73_, ruser0_.title_orig as title_o19_73_ from m_user ruser0_ inner join m_focus ruser0_1_ on ruser0_.oid=ruser0_1_.oid inner join m_object ruser0_2_ on ruser0_.oid=ruser0_2_.oid where ruser0_.oid=?
 [1] select roextstrin0_.item_id as item_id1_33_0_, roextstrin0_.owner_oid as owner_oi2_33_0_, roextstrin0_.ownerType as ownerTyp3_33_0_, roextstrin0_.stringValue as stringVa4_33_0_ from m_object_ext_string roextstrin0_ where roextstrin0_.item_id=? and roextstrin0_.owner_oid=? and roextstrin0_.ownerType=? and roextstrin0_.stringValue=?
 [1] select roextstrin0_.item_id as item_id1_33_0_, roextstrin0_.owner_oid as owner_oi2_33_0_, roextstrin0_.ownerType as ownerTyp3_33_0_, roextstrin0_.stringValue as stringVa4_33_0_ from m_object_ext_string roextstrin0_ where roextstrin0_.item_id=? and roextstrin0_.owner_oid=? and roextstrin0_.ownerType=? and roextstrin0_.stringValue=?
 [2] insert into m_object_ext_string (item_id, owner_oid, ownerType, stringValue) values (?, ?, ?, ?)
 [1] update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
         */
        assertCounts(4 + getExtraSafeInsertionSelects(2), 5 + getExtraSafeInsertionSelects(2));
    }

    @Test
    public void test025AddVisibleUserExtensionValueDuplicate() throws Exception {
        OperationResult result = new OperationResult(ExtensionTest.class.getName() + ".test025AddVisibleUserExtensionValueDuplicate");

        ObjectDelta<UserType> delta = getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_EXTENSION, EXT_VISIBLE)
                .add("v4")
                .asObjectDelta("");
        delta.applyTo(expectedUser);

        queryListener.start();
        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), getOptions(), result);
        queryListener.dumpAndStop();

        try (EntityManager em = factory.createEntityManager()) {
            RUser u = em.find(RUser.class, userOid);
            assertExtension(u, itemHidden1, "h1.1", "h1.2", "h1.3");
            assertExtension(u, itemHidden2);
            assertExtension(u, itemHidden3, "h3.1");
            assertExtension(u, itemVisible, "v1", "v2", "v3", "v4", "v5");
            assertExtension(u, itemWeapon);
        }

        assertGetObject(result);
        assertSearch(EXT_HIDDEN1, "h1.1", 1, result);
        assertSearch(EXT_HIDDEN1, "h1.2", 1, result);
        assertSearch(EXT_HIDDEN1, "h1.3", 1, result);
        assertSearch(EXT_HIDDEN1, "h1.4", 0, result);
        assertSearch(EXT_HIDDEN2, "h2.1", 0, result);
        assertSearch(EXT_HIDDEN3, "h3.1", 1, result);
        assertSearch(EXT_HIDDEN3, "h1.3", 0, result);
        assertSearch(EXT_VISIBLE, "v1", 1, result);
        assertSearch(EXT_VISIBLE, "v2", 1, result);
        assertSearch(EXT_VISIBLE, "v3", 1, result);
        assertSearch(EXT_VISIBLE, "v4", 1, result);
        assertSearch(EXT_VISIBLE, "v5", 1, result);
        assertSearch(EXT_VISIBLE, "v6", 0, result);

        /*
 [1] select oid from m_object where oid = ? for update
 [1] select ruser0_.oid as oid1_27_, ruser0_2_.createChannel as createCh2_27_, ruser0_2_.createTimestamp as createTi3_27_, ruser0_2_.creatorRef_relation as creatorR4_27_, ruser0_2_.creatorRef_targetOid as creatorR5_27_, ruser0_2_.creatorRef_type as creatorR6_27_, ruser0_2_.fullObject as fullObje7_27_, ruser0_2_.lifecycleState as lifecycl8_27_, ruser0_2_.modifierRef_relation as modifier9_27_, ruser0_2_.modifierRef_targetOid as modifie10_27_, ruser0_2_.modifierRef_type as modifie11_27_, ruser0_2_.modifyChannel as modifyC12_27_, ruser0_2_.modifyTimestamp as modifyT13_27_, ruser0_2_.name_norm as name_no14_27_, ruser0_2_.name_orig as name_or15_27_, ruser0_2_.objectTypeClass as objectT16_27_, ruser0_2_.tenantRef_relation as tenantR17_27_, ruser0_2_.tenantRef_targetOid as tenantR18_27_, ruser0_2_.tenantRef_type as tenantR19_27_, ruser0_2_.version as version20_27_, ruser0_1_.administrativeStatus as administ1_53_, ruser0_1_.archiveTimestamp as archiveT2_53_, ruser0_1_.disableReason as disableR3_53_, ruser0_1_.disableTimestamp as disableT4_53_, ruser0_1_.effectiveStatus as effectiv5_53_, ruser0_1_.enableTimestamp as enableTi6_53_, ruser0_1_.validFrom as validFro7_53_, ruser0_1_.validTo as validTo8_53_, ruser0_1_.validityChangeTimestamp as validity9_53_, ruser0_1_.validityStatus as validit10_53_, ruser0_1_.costCenter as costCen11_53_, ruser0_1_.emailAddress as emailAd12_53_, ruser0_1_.hasPhoto as hasPhot13_53_, ruser0_1_.locale as locale14_53_, ruser0_1_.locality_norm as localit15_53_, ruser0_1_.locality_orig as localit16_53_, ruser0_1_.preferredLanguage as preferr17_53_, ruser0_1_.telephoneNumber as telepho18_53_, ruser0_1_.timezone as timezon19_53_, ruser0_.additionalName_norm as addition1_73_, ruser0_.additionalName_orig as addition2_73_, ruser0_.employeeNumber as employee3_73_, ruser0_.familyName_norm as familyNa4_73_, ruser0_.familyName_orig as familyNa5_73_, ruser0_.fullName_norm as fullName6_73_, ruser0_.fullName_orig as fullName7_73_, ruser0_.givenName_norm as givenNam8_73_, ruser0_.givenName_orig as givenNam9_73_, ruser0_.honorificPrefix_norm as honorif10_73_, ruser0_.honorificPrefix_orig as honorif11_73_, ruser0_.honorificSuffix_norm as honorif12_73_, ruser0_.honorificSuffix_orig as honorif13_73_, ruser0_.name_norm as name_no14_73_, ruser0_.name_orig as name_or15_73_, ruser0_.nickName_norm as nickNam16_73_, ruser0_.nickName_orig as nickNam17_73_, ruser0_.title_norm as title_n18_73_, ruser0_.title_orig as title_o19_73_ from m_user ruser0_ inner join m_focus ruser0_1_ on ruser0_.oid=ruser0_1_.oid inner join m_object ruser0_2_ on ruser0_.oid=ruser0_2_.oid where ruser0_.oid=?
 [1] update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
         */
        assertCounts(3, 3);
    }

    @Test
    public void test026ReplaceVisibleExtensionValues() throws Exception {
        OperationResult result = new OperationResult("test026ReplaceVisibleExtensionValues");

        ObjectDelta<UserType> delta = getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_EXTENSION, EXT_WEAPON)
                .replace("weapon1", "weapon2")
                .asObjectDelta("");
        delta.applyTo(expectedUser);

        queryListener.start();
        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), getOptions(), result);
        queryListener.dumpAndStop();

        try (EntityManager em = factory.createEntityManager()) {
            RUser u = em.find(RUser.class, userOid);
            assertExtension(u, itemHidden1, "h1.1", "h1.2", "h1.3");
            assertExtension(u, itemHidden2);
            assertExtension(u, itemHidden3, "h3.1");
            assertExtension(u, itemVisible, "v1", "v2", "v3", "v4", "v5");
            assertExtension(u, itemWeapon, "weapon1", "weapon2");
        }

        assertGetObject(result);

        /*
 [1] select oid from m_object where oid = ? for update
 [1] select ruser0_.oid as oid1_27_, ruser0_2_.createChannel as createCh2_27_, ruser0_2_.createTimestamp as createTi3_27_, ruser0_2_.creatorRef_relation as creatorR4_27_, ruser0_2_.creatorRef_targetOid as creatorR5_27_, ruser0_2_.creatorRef_type as creatorR6_27_, ruser0_2_.fullObject as fullObje7_27_, ruser0_2_.lifecycleState as lifecycl8_27_, ruser0_2_.modifierRef_relation as modifier9_27_, ruser0_2_.modifierRef_targetOid as modifie10_27_, ruser0_2_.modifierRef_type as modifie11_27_, ruser0_2_.modifyChannel as modifyC12_27_, ruser0_2_.modifyTimestamp as modifyT13_27_, ruser0_2_.name_norm as name_no14_27_, ruser0_2_.name_orig as name_or15_27_, ruser0_2_.objectTypeClass as objectT16_27_, ruser0_2_.tenantRef_relation as tenantR17_27_, ruser0_2_.tenantRef_targetOid as tenantR18_27_, ruser0_2_.tenantRef_type as tenantR19_27_, ruser0_2_.version as version20_27_, ruser0_1_.administrativeStatus as administ1_53_, ruser0_1_.archiveTimestamp as archiveT2_53_, ruser0_1_.disableReason as disableR3_53_, ruser0_1_.disableTimestamp as disableT4_53_, ruser0_1_.effectiveStatus as effectiv5_53_, ruser0_1_.enableTimestamp as enableTi6_53_, ruser0_1_.validFrom as validFro7_53_, ruser0_1_.validTo as validTo8_53_, ruser0_1_.validityChangeTimestamp as validity9_53_, ruser0_1_.validityStatus as validit10_53_, ruser0_1_.costCenter as costCen11_53_, ruser0_1_.emailAddress as emailAd12_53_, ruser0_1_.hasPhoto as hasPhot13_53_, ruser0_1_.locale as locale14_53_, ruser0_1_.locality_norm as localit15_53_, ruser0_1_.locality_orig as localit16_53_, ruser0_1_.preferredLanguage as preferr17_53_, ruser0_1_.telephoneNumber as telepho18_53_, ruser0_1_.timezone as timezon19_53_, ruser0_.additionalName_norm as addition1_73_, ruser0_.additionalName_orig as addition2_73_, ruser0_.employeeNumber as employee3_73_, ruser0_.familyName_norm as familyNa4_73_, ruser0_.familyName_orig as familyNa5_73_, ruser0_.fullName_norm as fullName6_73_, ruser0_.fullName_orig as fullName7_73_, ruser0_.givenName_norm as givenNam8_73_, ruser0_.givenName_orig as givenNam9_73_, ruser0_.honorificPrefix_norm as honorif10_73_, ruser0_.honorificPrefix_orig as honorif11_73_, ruser0_.honorificSuffix_norm as honorif12_73_, ruser0_.honorificSuffix_orig as honorif13_73_, ruser0_.name_norm as name_no14_73_, ruser0_.name_orig as name_or15_73_, ruser0_.nickName_norm as nickNam16_73_, ruser0_.nickName_orig as nickNam17_73_, ruser0_.title_norm as title_n18_73_, ruser0_.title_orig as title_o19_73_ from m_user ruser0_ inner join m_focus ruser0_1_ on ruser0_.oid=ruser0_1_.oid inner join m_object ruser0_2_ on ruser0_.oid=ruser0_2_.oid where ruser0_.oid=?
 [1] select strings0_.owner_oid as owner_oi2_33_0_, strings0_.item_id as item_id1_33_0_, strings0_.ownerType as ownerTyp3_33_0_, strings0_.stringValue as stringVa4_33_0_, strings0_.item_id as item_id1_33_1_, strings0_.owner_oid as owner_oi2_33_1_, strings0_.ownerType as ownerTyp3_33_1_, strings0_.stringValue as stringVa4_33_1_ from m_object_ext_string strings0_ where strings0_.owner_oid=?
 [1] select roextstrin0_.item_id as item_id1_33_0_, roextstrin0_.owner_oid as owner_oi2_33_0_, roextstrin0_.ownerType as ownerTyp3_33_0_, roextstrin0_.stringValue as stringVa4_33_0_ from m_object_ext_string roextstrin0_ where roextstrin0_.item_id=? and roextstrin0_.owner_oid=? and roextstrin0_.ownerType=? and roextstrin0_.stringValue=?
 [1] select roextstrin0_.item_id as item_id1_33_0_, roextstrin0_.owner_oid as owner_oi2_33_0_, roextstrin0_.ownerType as ownerTyp3_33_0_, roextstrin0_.stringValue as stringVa4_33_0_ from m_object_ext_string roextstrin0_ where roextstrin0_.item_id=? and roextstrin0_.owner_oid=? and roextstrin0_.ownerType=? and roextstrin0_.stringValue=?
 [2] insert into m_object_ext_string (item_id, owner_oid, ownerType, stringValue) values (?, ?, ?, ?)
 [1] update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?

         No deletions here, because there were no previous values of ext:weapon.
         */
        assertCounts(7, 8);
    }

    @Test
    public void test027DeleteVisibleExtensionValues() throws Exception {
        OperationResult result = new OperationResult("test027DeleteVisibleExtensionValues");

        ObjectDelta<UserType> delta = getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_EXTENSION, EXT_WEAPON)
                .delete("weapon1", "weapon2")
                .asObjectDelta("");
        delta.applyTo(expectedUser);

        queryListener.start();
        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), getOptions(), result);
        queryListener.dumpAndStop();

        try (EntityManager em = factory.createEntityManager()) {
            RUser u = em.find(RUser.class, userOid);
            assertExtension(u, itemHidden1, "h1.1", "h1.2", "h1.3");
            assertExtension(u, itemHidden2);
            assertExtension(u, itemHidden3, "h3.1");
            assertExtension(u, itemVisible, "v1", "v2", "v3", "v4", "v5");
            assertExtension(u, itemWeapon);
        }

        assertGetObject(result);
        /*
         Default: (note that we select all strings here)

 [1] select oid from m_object where oid = ? for update
 [1] select ruser0_.oid as oid1_27_, ruser0_2_.createChannel as createCh2_27_, ruser0_2_.createTimestamp as createTi3_27_, ruser0_2_.creatorRef_relation as creatorR4_27_, ruser0_2_.creatorRef_targetOid as creatorR5_27_, ruser0_2_.creatorRef_type as creatorR6_27_, ruser0_2_.fullObject as fullObje7_27_, ruser0_2_.lifecycleState as lifecycl8_27_, ruser0_2_.modifierRef_relation as modifier9_27_, ruser0_2_.modifierRef_targetOid as modifie10_27_, ruser0_2_.modifierRef_type as modifie11_27_, ruser0_2_.modifyChannel as modifyC12_27_, ruser0_2_.modifyTimestamp as modifyT13_27_, ruser0_2_.name_norm as name_no14_27_, ruser0_2_.name_orig as name_or15_27_, ruser0_2_.objectTypeClass as objectT16_27_, ruser0_2_.tenantRef_relation as tenantR17_27_, ruser0_2_.tenantRef_targetOid as tenantR18_27_, ruser0_2_.tenantRef_type as tenantR19_27_, ruser0_2_.version as version20_27_, ruser0_1_.administrativeStatus as administ1_53_, ruser0_1_.archiveTimestamp as archiveT2_53_, ruser0_1_.disableReason as disableR3_53_, ruser0_1_.disableTimestamp as disableT4_53_, ruser0_1_.effectiveStatus as effectiv5_53_, ruser0_1_.enableTimestamp as enableTi6_53_, ruser0_1_.validFrom as validFro7_53_, ruser0_1_.validTo as validTo8_53_, ruser0_1_.validityChangeTimestamp as validity9_53_, ruser0_1_.validityStatus as validit10_53_, ruser0_1_.costCenter as costCen11_53_, ruser0_1_.emailAddress as emailAd12_53_, ruser0_1_.hasPhoto as hasPhot13_53_, ruser0_1_.locale as locale14_53_, ruser0_1_.locality_norm as localit15_53_, ruser0_1_.locality_orig as localit16_53_, ruser0_1_.preferredLanguage as preferr17_53_, ruser0_1_.telephoneNumber as telepho18_53_, ruser0_1_.timezone as timezon19_53_, ruser0_.additionalName_norm as addition1_73_, ruser0_.additionalName_orig as addition2_73_, ruser0_.employeeNumber as employee3_73_, ruser0_.familyName_norm as familyNa4_73_, ruser0_.familyName_orig as familyNa5_73_, ruser0_.fullName_norm as fullName6_73_, ruser0_.fullName_orig as fullName7_73_, ruser0_.givenName_norm as givenNam8_73_, ruser0_.givenName_orig as givenNam9_73_, ruser0_.honorificPrefix_norm as honorif10_73_, ruser0_.honorificPrefix_orig as honorif11_73_, ruser0_.honorificSuffix_norm as honorif12_73_, ruser0_.honorificSuffix_orig as honorif13_73_, ruser0_.name_norm as name_no14_73_, ruser0_.name_orig as name_or15_73_, ruser0_.nickName_norm as nickNam16_73_, ruser0_.nickName_orig as nickNam17_73_, ruser0_.title_norm as title_n18_73_, ruser0_.title_orig as title_o19_73_ from m_user ruser0_ inner join m_focus ruser0_1_ on ruser0_.oid=ruser0_1_.oid inner join m_object ruser0_2_ on ruser0_.oid=ruser0_2_.oid where ruser0_.oid=?
 [1] select strings0_.owner_oid as owner_oi2_33_0_, strings0_.item_id as item_id1_33_0_, strings0_.ownerType as ownerTyp3_33_0_, strings0_.stringValue as stringVa4_33_0_, strings0_.item_id as item_id1_33_1_, strings0_.owner_oid as owner_oi2_33_1_, strings0_.ownerType as ownerTyp3_33_1_, strings0_.stringValue as stringVa4_33_1_ from m_object_ext_string strings0_ where strings0_.owner_oid=?
 [1] update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
 [2] delete from m_object_ext_string where item_id=? and owner_oid=? and ownerType=? and stringValue=?

         No-fetch deletion:

 [1] select oid from m_object where oid = ? for update
 [1] select ruser0_.oid as oid1_27_, ruser0_2_.createChannel as createCh2_27_, ruser0_2_.createTimestamp as createTi3_27_, ruser0_2_.creatorRef_relation as creatorR4_27_, ruser0_2_.creatorRef_targetOid as creatorR5_27_, ruser0_2_.creatorRef_type as creatorR6_27_, ruser0_2_.fullObject as fullObje7_27_, ruser0_2_.lifecycleState as lifecycl8_27_, ruser0_2_.modifierRef_relation as modifier9_27_, ruser0_2_.modifierRef_targetOid as modifie10_27_, ruser0_2_.modifierRef_type as modifie11_27_, ruser0_2_.modifyChannel as modifyC12_27_, ruser0_2_.modifyTimestamp as modifyT13_27_, ruser0_2_.name_norm as name_no14_27_, ruser0_2_.name_orig as name_or15_27_, ruser0_2_.objectTypeClass as objectT16_27_, ruser0_2_.tenantRef_relation as tenantR17_27_, ruser0_2_.tenantRef_targetOid as tenantR18_27_, ruser0_2_.tenantRef_type as tenantR19_27_, ruser0_2_.version as version20_27_, ruser0_1_.administrativeStatus as administ1_53_, ruser0_1_.archiveTimestamp as archiveT2_53_, ruser0_1_.disableReason as disableR3_53_, ruser0_1_.disableTimestamp as disableT4_53_, ruser0_1_.effectiveStatus as effectiv5_53_, ruser0_1_.enableTimestamp as enableTi6_53_, ruser0_1_.validFrom as validFro7_53_, ruser0_1_.validTo as validTo8_53_, ruser0_1_.validityChangeTimestamp as validity9_53_, ruser0_1_.validityStatus as validit10_53_, ruser0_1_.costCenter as costCen11_53_, ruser0_1_.emailAddress as emailAd12_53_, ruser0_1_.hasPhoto as hasPhot13_53_, ruser0_1_.locale as locale14_53_, ruser0_1_.locality_norm as localit15_53_, ruser0_1_.locality_orig as localit16_53_, ruser0_1_.preferredLanguage as preferr17_53_, ruser0_1_.telephoneNumber as telepho18_53_, ruser0_1_.timezone as timezon19_53_, ruser0_.additionalName_norm as addition1_73_, ruser0_.additionalName_orig as addition2_73_, ruser0_.employeeNumber as employee3_73_, ruser0_.familyName_norm as familyNa4_73_, ruser0_.familyName_orig as familyNa5_73_, ruser0_.fullName_norm as fullName6_73_, ruser0_.fullName_orig as fullName7_73_, ruser0_.givenName_norm as givenNam8_73_, ruser0_.givenName_orig as givenNam9_73_, ruser0_.honorificPrefix_norm as honorif10_73_, ruser0_.honorificPrefix_orig as honorif11_73_, ruser0_.honorificSuffix_norm as honorif12_73_, ruser0_.honorificSuffix_orig as honorif13_73_, ruser0_.name_norm as name_no14_73_, ruser0_.name_orig as name_or15_73_, ruser0_.nickName_norm as nickNam16_73_, ruser0_.nickName_orig as nickNam17_73_, ruser0_.title_norm as title_n18_73_, ruser0_.title_orig as title_o19_73_ from m_user ruser0_ inner join m_focus ruser0_1_ on ruser0_.oid=ruser0_1_.oid inner join m_object ruser0_2_ on ruser0_.oid=ruser0_2_.oid where ruser0_.oid=?
 [1] delete from m_object_ext_string where owner_oid=? and ownerType=? and item_id=? and stringValue=?
 [1] delete from m_object_ext_string where owner_oid=? and ownerType=? and item_id=? and stringValue=?
 [1] update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?

         */
        if (isNoFetchDeletion()) {
            assertCounts(5, 5);
        } else {
            assertCounts(5, 6);
        }
    }

    /**
     * We load extension values (e.g. invoking REPLACE operation) and then try to delete an extension value.
     */
    @Test
    public void test028DeleteAlreadyLoadedVisibleExtensionValue() throws Exception {
        OperationResult result = new OperationResult("test028DeleteAlreadyLoadedVisibleExtensionValue");

        ObjectDelta<UserType> delta = getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_EXTENSION, EXT_WEAPON)
                .replace("w1")
                .item(UserType.F_EXTENSION, EXT_VISIBLE)
                .delete("v4", "v5")
                .asObjectDelta("");
        delta.applyTo(expectedUser);

        queryListener.start();
        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), getOptions(), result);
        queryListener.dumpAndStop();

        try (EntityManager em = factory.createEntityManager()) {
            RUser u = em.find(RUser.class, userOid);
            assertExtension(u, itemHidden1, "h1.1", "h1.2", "h1.3");
            assertExtension(u, itemHidden2);
            assertExtension(u, itemHidden3, "h3.1");
            assertExtension(u, itemVisible, "v1", "v2", "v3");
            assertExtension(u, itemWeapon, "w1");
        }

        assertGetObject(result);

        /*
 [1] select oid from m_object where oid = ? for update
 [1] select ruser0_.oid as oid1_27_, ruser0_2_.createChannel as createCh2_27_, ruser0_2_.createTimestamp as createTi3_27_, ruser0_2_.creatorRef_relation as creatorR4_27_, ruser0_2_.creatorRef_targetOid as creatorR5_27_, ruser0_2_.creatorRef_type as creatorR6_27_, ruser0_2_.fullObject as fullObje7_27_, ruser0_2_.lifecycleState as lifecycl8_27_, ruser0_2_.modifierRef_relation as modifier9_27_, ruser0_2_.modifierRef_targetOid as modifie10_27_, ruser0_2_.modifierRef_type as modifie11_27_, ruser0_2_.modifyChannel as modifyC12_27_, ruser0_2_.modifyTimestamp as modifyT13_27_, ruser0_2_.name_norm as name_no14_27_, ruser0_2_.name_orig as name_or15_27_, ruser0_2_.objectTypeClass as objectT16_27_, ruser0_2_.tenantRef_relation as tenantR17_27_, ruser0_2_.tenantRef_targetOid as tenantR18_27_, ruser0_2_.tenantRef_type as tenantR19_27_, ruser0_2_.version as version20_27_, ruser0_1_.administrativeStatus as administ1_53_, ruser0_1_.archiveTimestamp as archiveT2_53_, ruser0_1_.disableReason as disableR3_53_, ruser0_1_.disableTimestamp as disableT4_53_, ruser0_1_.effectiveStatus as effectiv5_53_, ruser0_1_.enableTimestamp as enableTi6_53_, ruser0_1_.validFrom as validFro7_53_, ruser0_1_.validTo as validTo8_53_, ruser0_1_.validityChangeTimestamp as validity9_53_, ruser0_1_.validityStatus as validit10_53_, ruser0_1_.costCenter as costCen11_53_, ruser0_1_.emailAddress as emailAd12_53_, ruser0_1_.hasPhoto as hasPhot13_53_, ruser0_1_.locale as locale14_53_, ruser0_1_.locality_norm as localit15_53_, ruser0_1_.locality_orig as localit16_53_, ruser0_1_.preferredLanguage as preferr17_53_, ruser0_1_.telephoneNumber as telepho18_53_, ruser0_1_.timezone as timezon19_53_, ruser0_.additionalName_norm as addition1_73_, ruser0_.additionalName_orig as addition2_73_, ruser0_.employeeNumber as employee3_73_, ruser0_.familyName_norm as familyNa4_73_, ruser0_.familyName_orig as familyNa5_73_, ruser0_.fullName_norm as fullName6_73_, ruser0_.fullName_orig as fullName7_73_, ruser0_.givenName_norm as givenNam8_73_, ruser0_.givenName_orig as givenNam9_73_, ruser0_.honorificPrefix_norm as honorif10_73_, ruser0_.honorificPrefix_orig as honorif11_73_, ruser0_.honorificSuffix_norm as honorif12_73_, ruser0_.honorificSuffix_orig as honorif13_73_, ruser0_.name_norm as name_no14_73_, ruser0_.name_orig as name_or15_73_, ruser0_.nickName_norm as nickNam16_73_, ruser0_.nickName_orig as nickNam17_73_, ruser0_.title_norm as title_n18_73_, ruser0_.title_orig as title_o19_73_ from m_user ruser0_ inner join m_focus ruser0_1_ on ruser0_.oid=ruser0_1_.oid inner join m_object ruser0_2_ on ruser0_.oid=ruser0_2_.oid where ruser0_.oid=?
 [1] select strings0_.owner_oid as owner_oi2_33_0_, strings0_.item_id as item_id1_33_0_, strings0_.ownerType as ownerTyp3_33_0_, strings0_.stringValue as stringVa4_33_0_, strings0_.item_id as item_id1_33_1_, strings0_.owner_oid as owner_oi2_33_1_, strings0_.ownerType as ownerTyp3_33_1_, strings0_.stringValue as stringVa4_33_1_ from m_object_ext_string strings0_ where strings0_.owner_oid=?
 [1] select roextstrin0_.item_id as item_id1_33_0_, roextstrin0_.owner_oid as owner_oi2_33_0_, roextstrin0_.ownerType as ownerTyp3_33_0_, roextstrin0_.stringValue as stringVa4_33_0_ from m_object_ext_string roextstrin0_ where roextstrin0_.item_id=? and roextstrin0_.owner_oid=? and roextstrin0_.ownerType=? and roextstrin0_.stringValue=?
 [1] insert into m_object_ext_string (item_id, owner_oid, ownerType, stringValue) values (?, ?, ?, ?)
 [1] update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
 [2] delete from m_object_ext_string where item_id=? and owner_oid=? and ownerType=? and stringValue=?

        Actually, 4th SELECT probably could be eliminated by letting Hibernate take care of the addition. But let's ignore this for a moment.
         */
        assertCounts(7, 8);
    }

    @Test
    public void test029ReplaceNonIndexedExtensionProperty() throws Exception {
        OperationResult result = new OperationResult("test029ReplaceNonIndexedExtensionProperty");

        ObjectDelta<UserType> delta = getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_EXTENSION, EXT_LOOT)
                .replace(34)
                .asObjectDelta("");
        delta.applyTo(expectedUser);

        queryListener.start();
        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), getOptions(), result);
        queryListener.dumpAndStop();

        RExtItem extItemDef = extItemDictionary.findItemByDefinition(delta.getModifications().iterator().next().getDefinition());
        assertNull("ext item definition for loot exists", extItemDef);

        assertGetObject(result);
        /*
 [1] select oid from m_object where oid = ? for update
 [1] select ruser0_.oid as oid1_27_, ruser0_2_.createChannel as createCh2_27_, ruser0_2_.createTimestamp as createTi3_27_, ruser0_2_.creatorRef_relation as creatorR4_27_, ruser0_2_.creatorRef_targetOid as creatorR5_27_, ruser0_2_.creatorRef_type as creatorR6_27_, ruser0_2_.fullObject as fullObje7_27_, ruser0_2_.lifecycleState as lifecycl8_27_, ruser0_2_.modifierRef_relation as modifier9_27_, ruser0_2_.modifierRef_targetOid as modifie10_27_, ruser0_2_.modifierRef_type as modifie11_27_, ruser0_2_.modifyChannel as modifyC12_27_, ruser0_2_.modifyTimestamp as modifyT13_27_, ruser0_2_.name_norm as name_no14_27_, ruser0_2_.name_orig as name_or15_27_, ruser0_2_.objectTypeClass as objectT16_27_, ruser0_2_.tenantRef_relation as tenantR17_27_, ruser0_2_.tenantRef_targetOid as tenantR18_27_, ruser0_2_.tenantRef_type as tenantR19_27_, ruser0_2_.version as version20_27_, ruser0_1_.administrativeStatus as administ1_53_, ruser0_1_.archiveTimestamp as archiveT2_53_, ruser0_1_.disableReason as disableR3_53_, ruser0_1_.disableTimestamp as disableT4_53_, ruser0_1_.effectiveStatus as effectiv5_53_, ruser0_1_.enableTimestamp as enableTi6_53_, ruser0_1_.validFrom as validFro7_53_, ruser0_1_.validTo as validTo8_53_, ruser0_1_.validityChangeTimestamp as validity9_53_, ruser0_1_.validityStatus as validit10_53_, ruser0_1_.costCenter as costCen11_53_, ruser0_1_.emailAddress as emailAd12_53_, ruser0_1_.hasPhoto as hasPhot13_53_, ruser0_1_.locale as locale14_53_, ruser0_1_.locality_norm as localit15_53_, ruser0_1_.locality_orig as localit16_53_, ruser0_1_.preferredLanguage as preferr17_53_, ruser0_1_.telephoneNumber as telepho18_53_, ruser0_1_.timezone as timezon19_53_, ruser0_.additionalName_norm as addition1_73_, ruser0_.additionalName_orig as addition2_73_, ruser0_.employeeNumber as employee3_73_, ruser0_.familyName_norm as familyNa4_73_, ruser0_.familyName_orig as familyNa5_73_, ruser0_.fullName_norm as fullName6_73_, ruser0_.fullName_orig as fullName7_73_, ruser0_.givenName_norm as givenNam8_73_, ruser0_.givenName_orig as givenNam9_73_, ruser0_.honorificPrefix_norm as honorif10_73_, ruser0_.honorificPrefix_orig as honorif11_73_, ruser0_.honorificSuffix_norm as honorif12_73_, ruser0_.honorificSuffix_orig as honorif13_73_, ruser0_.name_norm as name_no14_73_, ruser0_.name_orig as name_or15_73_, ruser0_.nickName_norm as nickNam16_73_, ruser0_.nickName_orig as nickNam17_73_, ruser0_.title_norm as title_n18_73_, ruser0_.title_orig as title_o19_73_ from m_user ruser0_ inner join m_focus ruser0_1_ on ruser0_.oid=ruser0_1_.oid inner join m_object ruser0_2_ on ruser0_.oid=ruser0_2_.oid where ruser0_.oid=?
 [1] update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
         */
        assertCounts(3, 3);
    }

    @Test
    public void test030AddHiddenUserExtensionValues() throws Exception {
        OperationResult result = new OperationResult(ExtensionTest.class.getName() + ".test030AddHiddenUserExtensionValues");

        ObjectDelta<UserType> delta = getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_EXTENSION, EXT_HIDDEN1).add("h1.4")
                .item(UserType.F_EXTENSION, EXT_HIDDEN2).add("h2.1", "h2.2")
                .asObjectDelta("");
        delta.applyTo(expectedUser);

        queryListener.start();
        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), getOptions(), result);
        queryListener.dumpAndStop();

        try (EntityManager em = factory.createEntityManager()) {
            RUser u = em.find(RUser.class, userOid);
            assertExtension(u, itemHidden1, "h1.1", "h1.2", "h1.3", "h1.4");
            assertExtension(u, itemHidden2, "h2.1", "h2.2");
            assertExtension(u, itemHidden3, "h3.1");
            assertExtension(u, itemVisible, "v1", "v2", "v3");
            assertExtension(u, itemWeapon, "w1");
        }
        assertGetObject(result);
        assertSearch(EXT_HIDDEN1, "h1.1", 1, result);
        assertSearch(EXT_HIDDEN1, "h1.2", 1, result);
        assertSearch(EXT_HIDDEN1, "h1.3", 1, result);
        assertSearch(EXT_HIDDEN1, "h1.4", 1, result);
        assertSearch(EXT_HIDDEN1, "h1.5", 0, result);
        assertSearch(EXT_HIDDEN2, "h2.1", 1, result);
        assertSearch(EXT_HIDDEN2, "h2.2", 1, result);
        assertSearch(EXT_HIDDEN3, "h3.1", 1, result);
        assertSearch(EXT_HIDDEN3, "h1.3", 0, result);

        /*
 [1] select oid from m_object where oid = ? for update
 [1] select ruser0_.oid as oid1_27_, ruser0_2_.createChannel as createCh2_27_, ruser0_2_.createTimestamp as createTi3_27_, ruser0_2_.creatorRef_relation as creatorR4_27_, ruser0_2_.creatorRef_targetOid as creatorR5_27_, ruser0_2_.creatorRef_type as creatorR6_27_, ruser0_2_.fullObject as fullObje7_27_, ruser0_2_.lifecycleState as lifecycl8_27_, ruser0_2_.modifierRef_relation as modifier9_27_, ruser0_2_.modifierRef_targetOid as modifie10_27_, ruser0_2_.modifierRef_type as modifie11_27_, ruser0_2_.modifyChannel as modifyC12_27_, ruser0_2_.modifyTimestamp as modifyT13_27_, ruser0_2_.name_norm as name_no14_27_, ruser0_2_.name_orig as name_or15_27_, ruser0_2_.objectTypeClass as objectT16_27_, ruser0_2_.tenantRef_relation as tenantR17_27_, ruser0_2_.tenantRef_targetOid as tenantR18_27_, ruser0_2_.tenantRef_type as tenantR19_27_, ruser0_2_.version as version20_27_, ruser0_1_.administrativeStatus as administ1_53_, ruser0_1_.archiveTimestamp as archiveT2_53_, ruser0_1_.disableReason as disableR3_53_, ruser0_1_.disableTimestamp as disableT4_53_, ruser0_1_.effectiveStatus as effectiv5_53_, ruser0_1_.enableTimestamp as enableTi6_53_, ruser0_1_.validFrom as validFro7_53_, ruser0_1_.validTo as validTo8_53_, ruser0_1_.validityChangeTimestamp as validity9_53_, ruser0_1_.validityStatus as validit10_53_, ruser0_1_.costCenter as costCen11_53_, ruser0_1_.emailAddress as emailAd12_53_, ruser0_1_.hasPhoto as hasPhot13_53_, ruser0_1_.locale as locale14_53_, ruser0_1_.locality_norm as localit15_53_, ruser0_1_.locality_orig as localit16_53_, ruser0_1_.preferredLanguage as preferr17_53_, ruser0_1_.telephoneNumber as telepho18_53_, ruser0_1_.timezone as timezon19_53_, ruser0_.additionalName_norm as addition1_73_, ruser0_.additionalName_orig as addition2_73_, ruser0_.employeeNumber as employee3_73_, ruser0_.familyName_norm as familyNa4_73_, ruser0_.familyName_orig as familyNa5_73_, ruser0_.fullName_norm as fullName6_73_, ruser0_.fullName_orig as fullName7_73_, ruser0_.givenName_norm as givenNam8_73_, ruser0_.givenName_orig as givenNam9_73_, ruser0_.honorificPrefix_norm as honorif10_73_, ruser0_.honorificPrefix_orig as honorif11_73_, ruser0_.honorificSuffix_norm as honorif12_73_, ruser0_.honorificSuffix_orig as honorif13_73_, ruser0_.name_norm as name_no14_73_, ruser0_.name_orig as name_or15_73_, ruser0_.nickName_norm as nickNam16_73_, ruser0_.nickName_orig as nickNam17_73_, ruser0_.title_norm as title_n18_73_, ruser0_.title_orig as title_o19_73_ from m_user ruser0_ inner join m_focus ruser0_1_ on ruser0_.oid=ruser0_1_.oid inner join m_object ruser0_2_ on ruser0_.oid=ruser0_2_.oid where ruser0_.oid=?
 [3] insert into m_object_ext_string (item_id, owner_oid, ownerType, stringValue) values (?, ?, ?, ?)
 [1] update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
         */
        assertCounts(4 + getExtraSafeInsertionSelects(3), 6 + getExtraSafeInsertionSelects(3));
    }

    @Test
    public void test032AddHiddenUserExtensionValuesDuplicate() throws Exception {
        OperationResult result = new OperationResult(ExtensionTest.class.getName() + ".test032AddHiddenUserExtensionValuesDuplicate");

        ObjectDelta<UserType> delta = getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_EXTENSION, EXT_HIDDEN1).add("h1.4")
                .item(UserType.F_EXTENSION, EXT_HIDDEN2).add("h2.1", "h2.2")
                .asObjectDelta("");
        delta.applyTo(expectedUser);

        queryListener.start();
        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), getOptions(), result);
        queryListener.dumpAndStop();

        try (EntityManager em = factory.createEntityManager()) {
            RUser u = em.find(RUser.class, userOid);
            assertExtension(u, itemHidden1, "h1.1", "h1.2", "h1.3", "h1.4");
            assertExtension(u, itemHidden2, "h2.1", "h2.2");
            assertExtension(u, itemHidden3, "h3.1");
            assertExtension(u, itemVisible, "v1", "v2", "v3");
            assertExtension(u, itemWeapon, "w1");
        }

        assertGetObject(result);
        assertSearch(EXT_HIDDEN1, "h1.1", 1, result);
        assertSearch(EXT_HIDDEN1, "h1.2", 1, result);
        assertSearch(EXT_HIDDEN1, "h1.3", 1, result);
        assertSearch(EXT_HIDDEN1, "h1.4", 1, result);
        assertSearch(EXT_HIDDEN1, "h1.5", 0, result);
        assertSearch(EXT_HIDDEN2, "h2.1", 1, result);
        assertSearch(EXT_HIDDEN2, "h2.2", 1, result);
        assertSearch(EXT_HIDDEN3, "h3.1", 1, result);
        assertSearch(EXT_HIDDEN3, "h1.3", 0, result);

        /*
 [1] select oid from m_object where oid = ? for update
 [1] select ruser0_.oid as oid1_27_, ruser0_2_.createChannel as createCh2_27_, ruser0_2_.createTimestamp as createTi3_27_, ruser0_2_.creatorRef_relation as creatorR4_27_, ruser0_2_.creatorRef_targetOid as creatorR5_27_, ruser0_2_.creatorRef_type as creatorR6_27_, ruser0_2_.fullObject as fullObje7_27_, ruser0_2_.lifecycleState as lifecycl8_27_, ruser0_2_.modifierRef_relation as modifier9_27_, ruser0_2_.modifierRef_targetOid as modifie10_27_, ruser0_2_.modifierRef_type as modifie11_27_, ruser0_2_.modifyChannel as modifyC12_27_, ruser0_2_.modifyTimestamp as modifyT13_27_, ruser0_2_.name_norm as name_no14_27_, ruser0_2_.name_orig as name_or15_27_, ruser0_2_.objectTypeClass as objectT16_27_, ruser0_2_.tenantRef_relation as tenantR17_27_, ruser0_2_.tenantRef_targetOid as tenantR18_27_, ruser0_2_.tenantRef_type as tenantR19_27_, ruser0_2_.version as version20_27_, ruser0_1_.administrativeStatus as administ1_53_, ruser0_1_.archiveTimestamp as archiveT2_53_, ruser0_1_.disableReason as disableR3_53_, ruser0_1_.disableTimestamp as disableT4_53_, ruser0_1_.effectiveStatus as effectiv5_53_, ruser0_1_.enableTimestamp as enableTi6_53_, ruser0_1_.validFrom as validFro7_53_, ruser0_1_.validTo as validTo8_53_, ruser0_1_.validityChangeTimestamp as validity9_53_, ruser0_1_.validityStatus as validit10_53_, ruser0_1_.costCenter as costCen11_53_, ruser0_1_.emailAddress as emailAd12_53_, ruser0_1_.hasPhoto as hasPhot13_53_, ruser0_1_.locale as locale14_53_, ruser0_1_.locality_norm as localit15_53_, ruser0_1_.locality_orig as localit16_53_, ruser0_1_.preferredLanguage as preferr17_53_, ruser0_1_.telephoneNumber as telepho18_53_, ruser0_1_.timezone as timezon19_53_, ruser0_.additionalName_norm as addition1_73_, ruser0_.additionalName_orig as addition2_73_, ruser0_.employeeNumber as employee3_73_, ruser0_.familyName_norm as familyNa4_73_, ruser0_.familyName_orig as familyNa5_73_, ruser0_.fullName_norm as fullName6_73_, ruser0_.fullName_orig as fullName7_73_, ruser0_.givenName_norm as givenNam8_73_, ruser0_.givenName_orig as givenNam9_73_, ruser0_.honorificPrefix_norm as honorif10_73_, ruser0_.honorificPrefix_orig as honorif11_73_, ruser0_.honorificSuffix_norm as honorif12_73_, ruser0_.honorificSuffix_orig as honorif13_73_, ruser0_.name_norm as name_no14_73_, ruser0_.name_orig as name_or15_73_, ruser0_.nickName_norm as nickNam16_73_, ruser0_.nickName_orig as nickNam17_73_, ruser0_.title_norm as title_n18_73_, ruser0_.title_orig as title_o19_73_ from m_user ruser0_ inner join m_focus ruser0_1_ on ruser0_.oid=ruser0_1_.oid inner join m_object ruser0_2_ on ruser0_.oid=ruser0_2_.oid where ruser0_.oid=?
 [3] insert into m_object_ext_string (item_id, owner_oid, ownerType, stringValue) values (?, ?, ?, ?)
         <<< ConstraintViolationException here -> restarting the operation in safer way >>>
 [1] select oid from m_object where oid = ? for update
 [1] select ruser0_.oid as oid1_27_, ruser0_2_.createChannel as createCh2_27_, ruser0_2_.createTimestamp as createTi3_27_, ruser0_2_.creatorRef_relation as creatorR4_27_, ruser0_2_.creatorRef_targetOid as creatorR5_27_, ruser0_2_.creatorRef_type as creatorR6_27_, ruser0_2_.fullObject as fullObje7_27_, ruser0_2_.lifecycleState as lifecycl8_27_, ruser0_2_.modifierRef_relation as modifier9_27_, ruser0_2_.modifierRef_targetOid as modifie10_27_, ruser0_2_.modifierRef_type as modifie11_27_, ruser0_2_.modifyChannel as modifyC12_27_, ruser0_2_.modifyTimestamp as modifyT13_27_, ruser0_2_.name_norm as name_no14_27_, ruser0_2_.name_orig as name_or15_27_, ruser0_2_.objectTypeClass as objectT16_27_, ruser0_2_.tenantRef_relation as tenantR17_27_, ruser0_2_.tenantRef_targetOid as tenantR18_27_, ruser0_2_.tenantRef_type as tenantR19_27_, ruser0_2_.version as version20_27_, ruser0_1_.administrativeStatus as administ1_53_, ruser0_1_.archiveTimestamp as archiveT2_53_, ruser0_1_.disableReason as disableR3_53_, ruser0_1_.disableTimestamp as disableT4_53_, ruser0_1_.effectiveStatus as effectiv5_53_, ruser0_1_.enableTimestamp as enableTi6_53_, ruser0_1_.validFrom as validFro7_53_, ruser0_1_.validTo as validTo8_53_, ruser0_1_.validityChangeTimestamp as validity9_53_, ruser0_1_.validityStatus as validit10_53_, ruser0_1_.costCenter as costCen11_53_, ruser0_1_.emailAddress as emailAd12_53_, ruser0_1_.hasPhoto as hasPhot13_53_, ruser0_1_.locale as locale14_53_, ruser0_1_.locality_norm as localit15_53_, ruser0_1_.locality_orig as localit16_53_, ruser0_1_.preferredLanguage as preferr17_53_, ruser0_1_.telephoneNumber as telepho18_53_, ruser0_1_.timezone as timezon19_53_, ruser0_.additionalName_norm as addition1_73_, ruser0_.additionalName_orig as addition2_73_, ruser0_.employeeNumber as employee3_73_, ruser0_.familyName_norm as familyNa4_73_, ruser0_.familyName_orig as familyNa5_73_, ruser0_.fullName_norm as fullName6_73_, ruser0_.fullName_orig as fullName7_73_, ruser0_.givenName_norm as givenNam8_73_, ruser0_.givenName_orig as givenNam9_73_, ruser0_.honorificPrefix_norm as honorif10_73_, ruser0_.honorificPrefix_orig as honorif11_73_, ruser0_.honorificSuffix_norm as honorif12_73_, ruser0_.honorificSuffix_orig as honorif13_73_, ruser0_.name_norm as name_no14_73_, ruser0_.name_orig as name_or15_73_, ruser0_.nickName_norm as nickNam16_73_, ruser0_.nickName_orig as nickNam17_73_, ruser0_.title_norm as title_n18_73_, ruser0_.title_orig as title_o19_73_ from m_user ruser0_ inner join m_focus ruser0_1_ on ruser0_.oid=ruser0_1_.oid inner join m_object ruser0_2_ on ruser0_.oid=ruser0_2_.oid where ruser0_.oid=?
 [1] select roextstrin0_.item_id as item_id1_33_0_, roextstrin0_.owner_oid as owner_oi2_33_0_, roextstrin0_.ownerType as ownerTyp3_33_0_, roextstrin0_.stringValue as stringVa4_33_0_ from m_object_ext_string roextstrin0_ where roextstrin0_.item_id=? and roextstrin0_.owner_oid=? and roextstrin0_.ownerType=? and roextstrin0_.stringValue=?
 [1] select roextstrin0_.item_id as item_id1_33_0_, roextstrin0_.owner_oid as owner_oi2_33_0_, roextstrin0_.ownerType as ownerTyp3_33_0_, roextstrin0_.stringValue as stringVa4_33_0_ from m_object_ext_string roextstrin0_ where roextstrin0_.item_id=? and roextstrin0_.owner_oid=? and roextstrin0_.ownerType=? and roextstrin0_.stringValue=?
 [1] select roextstrin0_.item_id as item_id1_33_0_, roextstrin0_.owner_oid as owner_oi2_33_0_, roextstrin0_.ownerType as ownerTyp3_33_0_, roextstrin0_.stringValue as stringVa4_33_0_ from m_object_ext_string roextstrin0_ where roextstrin0_.item_id=? and roextstrin0_.owner_oid=? and roextstrin0_.ownerType=? and roextstrin0_.stringValue=?
 [1] update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?

        Safe insertions (no need of restarting the operation):

 [1] select oid from m_object where oid = ? for update
 [1] select ruser0_.oid as oid1_27_, ruser0_2_.createChannel as createCh2_27_, ruser0_2_.createTimestamp as createTi3_27_, ruser0_2_.creatorRef_relation as creatorR4_27_, ruser0_2_.creatorRef_targetOid as creatorR5_27_, ruser0_2_.creatorRef_type as creatorR6_27_, ruser0_2_.fullObject as fullObje7_27_, ruser0_2_.lifecycleState as lifecycl8_27_, ruser0_2_.modifierRef_relation as modifier9_27_, ruser0_2_.modifierRef_targetOid as modifie10_27_, ruser0_2_.modifierRef_type as modifie11_27_, ruser0_2_.modifyChannel as modifyC12_27_, ruser0_2_.modifyTimestamp as modifyT13_27_, ruser0_2_.name_norm as name_no14_27_, ruser0_2_.name_orig as name_or15_27_, ruser0_2_.objectTypeClass as objectT16_27_, ruser0_2_.tenantRef_relation as tenantR17_27_, ruser0_2_.tenantRef_targetOid as tenantR18_27_, ruser0_2_.tenantRef_type as tenantR19_27_, ruser0_2_.version as version20_27_, ruser0_1_.administrativeStatus as administ1_53_, ruser0_1_.archiveTimestamp as archiveT2_53_, ruser0_1_.disableReason as disableR3_53_, ruser0_1_.disableTimestamp as disableT4_53_, ruser0_1_.effectiveStatus as effectiv5_53_, ruser0_1_.enableTimestamp as enableTi6_53_, ruser0_1_.validFrom as validFro7_53_, ruser0_1_.validTo as validTo8_53_, ruser0_1_.validityChangeTimestamp as validity9_53_, ruser0_1_.validityStatus as validit10_53_, ruser0_1_.costCenter as costCen11_53_, ruser0_1_.emailAddress as emailAd12_53_, ruser0_1_.hasPhoto as hasPhot13_53_, ruser0_1_.locale as locale14_53_, ruser0_1_.locality_norm as localit15_53_, ruser0_1_.locality_orig as localit16_53_, ruser0_1_.preferredLanguage as preferr17_53_, ruser0_1_.telephoneNumber as telepho18_53_, ruser0_1_.timezone as timezon19_53_, ruser0_.additionalName_norm as addition1_73_, ruser0_.additionalName_orig as addition2_73_, ruser0_.employeeNumber as employee3_73_, ruser0_.familyName_norm as familyNa4_73_, ruser0_.familyName_orig as familyNa5_73_, ruser0_.fullName_norm as fullName6_73_, ruser0_.fullName_orig as fullName7_73_, ruser0_.givenName_norm as givenNam8_73_, ruser0_.givenName_orig as givenNam9_73_, ruser0_.honorificPrefix_norm as honorif10_73_, ruser0_.honorificPrefix_orig as honorif11_73_, ruser0_.honorificSuffix_norm as honorif12_73_, ruser0_.honorificSuffix_orig as honorif13_73_, ruser0_.name_norm as name_no14_73_, ruser0_.name_orig as name_or15_73_, ruser0_.nickName_norm as nickNam16_73_, ruser0_.nickName_orig as nickNam17_73_, ruser0_.title_norm as title_n18_73_, ruser0_.title_orig as title_o19_73_ from m_user ruser0_ inner join m_focus ruser0_1_ on ruser0_.oid=ruser0_1_.oid inner join m_object ruser0_2_ on ruser0_.oid=ruser0_2_.oid where ruser0_.oid=?
 [1] select roextstrin0_.item_id as item_id1_33_0_, roextstrin0_.owner_oid as owner_oi2_33_0_, roextstrin0_.ownerType as ownerTyp3_33_0_, roextstrin0_.stringValue as stringVa4_33_0_ from m_object_ext_string roextstrin0_ where roextstrin0_.item_id=? and roextstrin0_.owner_oid=? and roextstrin0_.ownerType=? and roextstrin0_.stringValue=?
 [1] select roextstrin0_.item_id as item_id1_33_0_, roextstrin0_.owner_oid as owner_oi2_33_0_, roextstrin0_.ownerType as ownerTyp3_33_0_, roextstrin0_.stringValue as stringVa4_33_0_ from m_object_ext_string roextstrin0_ where roextstrin0_.item_id=? and roextstrin0_.owner_oid=? and roextstrin0_.ownerType=? and roextstrin0_.stringValue=?
 [1] select roextstrin0_.item_id as item_id1_33_0_, roextstrin0_.owner_oid as owner_oi2_33_0_, roextstrin0_.ownerType as ownerTyp3_33_0_, roextstrin0_.stringValue as stringVa4_33_0_ from m_object_ext_string roextstrin0_ where roextstrin0_.item_id=? and roextstrin0_.owner_oid=? and roextstrin0_.ownerType=? and roextstrin0_.stringValue=?
 [1] update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
         */
        if (!isNoFetchInsertion()) {
            assertCounts(6, 6);
        } else {
            assertCounts(9, 11);
        }
    }

    @Test
    public void test035DeleteHiddenUserExtensionValues() throws Exception {
        OperationResult result = new OperationResult(ExtensionTest.class.getName() + ".test035DeleteHiddenUserExtensionValues");

        ObjectDelta<UserType> delta = getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_EXTENSION, EXT_HIDDEN1).delete("h1.1", "h1.2")
                .asObjectDelta("");
        delta.applyTo(expectedUser);

        queryListener.start();
        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), getOptions(), result);
        queryListener.dumpAndStop();

        try (EntityManager em = factory.createEntityManager()) {
            RUser u = em.find(RUser.class, userOid);
            assertExtension(u, itemHidden1, "h1.3", "h1.4");
            assertExtension(u, itemHidden2, "h2.1", "h2.2");
            assertExtension(u, itemHidden3, "h3.1");
            assertExtension(u, itemVisible, "v1", "v2", "v3");
            assertExtension(u, itemWeapon, "w1");
        }

        assertGetObject(result);
        assertSearch(EXT_HIDDEN1, "h1.1", 0, result);
        assertSearch(EXT_HIDDEN1, "h1.2", 0, result);
        assertSearch(EXT_HIDDEN1, "h1.3", 1, result);
        assertSearch(EXT_HIDDEN1, "h1.4", 1, result);
        assertSearch(EXT_HIDDEN1, "h1.5", 0, result);
        assertSearch(EXT_HIDDEN2, "h2.1", 1, result);
        assertSearch(EXT_HIDDEN2, "h2.2", 1, result);
        assertSearch(EXT_HIDDEN3, "h3.1", 1, result);
        assertSearch(EXT_HIDDEN3, "h1.3", 0, result);

        /*
        fetch-deletion:

 [1] select oid from m_object where oid = ? for update
 [1] select ruser0_.oid as oid1_27_, ruser0_2_.createChannel as createCh2_27_, ruser0_2_.createTimestamp as createTi3_27_, ruser0_2_.creatorRef_relation as creatorR4_27_, ruser0_2_.creatorRef_targetOid as creatorR5_27_, ruser0_2_.creatorRef_type as creatorR6_27_, ruser0_2_.fullObject as fullObje7_27_, ruser0_2_.lifecycleState as lifecycl8_27_, ruser0_2_.modifierRef_relation as modifier9_27_, ruser0_2_.modifierRef_targetOid as modifie10_27_, ruser0_2_.modifierRef_type as modifie11_27_, ruser0_2_.modifyChannel as modifyC12_27_, ruser0_2_.modifyTimestamp as modifyT13_27_, ruser0_2_.name_norm as name_no14_27_, ruser0_2_.name_orig as name_or15_27_, ruser0_2_.objectTypeClass as objectT16_27_, ruser0_2_.tenantRef_relation as tenantR17_27_, ruser0_2_.tenantRef_targetOid as tenantR18_27_, ruser0_2_.tenantRef_type as tenantR19_27_, ruser0_2_.version as version20_27_, ruser0_1_.administrativeStatus as administ1_53_, ruser0_1_.archiveTimestamp as archiveT2_53_, ruser0_1_.disableReason as disableR3_53_, ruser0_1_.disableTimestamp as disableT4_53_, ruser0_1_.effectiveStatus as effectiv5_53_, ruser0_1_.enableTimestamp as enableTi6_53_, ruser0_1_.validFrom as validFro7_53_, ruser0_1_.validTo as validTo8_53_, ruser0_1_.validityChangeTimestamp as validity9_53_, ruser0_1_.validityStatus as validit10_53_, ruser0_1_.costCenter as costCen11_53_, ruser0_1_.emailAddress as emailAd12_53_, ruser0_1_.hasPhoto as hasPhot13_53_, ruser0_1_.locale as locale14_53_, ruser0_1_.locality_norm as localit15_53_, ruser0_1_.locality_orig as localit16_53_, ruser0_1_.preferredLanguage as preferr17_53_, ruser0_1_.telephoneNumber as telepho18_53_, ruser0_1_.timezone as timezon19_53_, ruser0_.additionalName_norm as addition1_73_, ruser0_.additionalName_orig as addition2_73_, ruser0_.employeeNumber as employee3_73_, ruser0_.familyName_norm as familyNa4_73_, ruser0_.familyName_orig as familyNa5_73_, ruser0_.fullName_norm as fullName6_73_, ruser0_.fullName_orig as fullName7_73_, ruser0_.givenName_norm as givenNam8_73_, ruser0_.givenName_orig as givenNam9_73_, ruser0_.honorificPrefix_norm as honorif10_73_, ruser0_.honorificPrefix_orig as honorif11_73_, ruser0_.honorificSuffix_norm as honorif12_73_, ruser0_.honorificSuffix_orig as honorif13_73_, ruser0_.name_norm as name_no14_73_, ruser0_.name_orig as name_or15_73_, ruser0_.nickName_norm as nickNam16_73_, ruser0_.nickName_orig as nickNam17_73_, ruser0_.title_norm as title_n18_73_, ruser0_.title_orig as title_o19_73_ from m_user ruser0_ inner join m_focus ruser0_1_ on ruser0_.oid=ruser0_1_.oid inner join m_object ruser0_2_ on ruser0_.oid=ruser0_2_.oid where ruser0_.oid=?
 [1] select strings0_.owner_oid as owner_oi2_33_0_, strings0_.item_id as item_id1_33_0_, strings0_.ownerType as ownerTyp3_33_0_, strings0_.stringValue as stringVa4_33_0_, strings0_.item_id as item_id1_33_1_, strings0_.owner_oid as owner_oi2_33_1_, strings0_.ownerType as ownerTyp3_33_1_, strings0_.stringValue as stringVa4_33_1_ from m_object_ext_string strings0_ where strings0_.owner_oid=?
 [1] update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
 [2] delete from m_object_ext_string where item_id=? and owner_oid=? and ownerType=? and stringValue=?

        no-fetch deletion:

 [1] select oid from m_object where oid = ? for update
 [1] select ruser0_.oid as oid1_27_, ruser0_2_.createChannel as createCh2_27_, ruser0_2_.createTimestamp as createTi3_27_, ruser0_2_.creatorRef_relation as creatorR4_27_, ruser0_2_.creatorRef_targetOid as creatorR5_27_, ruser0_2_.creatorRef_type as creatorR6_27_, ruser0_2_.fullObject as fullObje7_27_, ruser0_2_.lifecycleState as lifecycl8_27_, ruser0_2_.modifierRef_relation as modifier9_27_, ruser0_2_.modifierRef_targetOid as modifie10_27_, ruser0_2_.modifierRef_type as modifie11_27_, ruser0_2_.modifyChannel as modifyC12_27_, ruser0_2_.modifyTimestamp as modifyT13_27_, ruser0_2_.name_norm as name_no14_27_, ruser0_2_.name_orig as name_or15_27_, ruser0_2_.objectTypeClass as objectT16_27_, ruser0_2_.tenantRef_relation as tenantR17_27_, ruser0_2_.tenantRef_targetOid as tenantR18_27_, ruser0_2_.tenantRef_type as tenantR19_27_, ruser0_2_.version as version20_27_, ruser0_1_.administrativeStatus as administ1_53_, ruser0_1_.archiveTimestamp as archiveT2_53_, ruser0_1_.disableReason as disableR3_53_, ruser0_1_.disableTimestamp as disableT4_53_, ruser0_1_.effectiveStatus as effectiv5_53_, ruser0_1_.enableTimestamp as enableTi6_53_, ruser0_1_.validFrom as validFro7_53_, ruser0_1_.validTo as validTo8_53_, ruser0_1_.validityChangeTimestamp as validity9_53_, ruser0_1_.validityStatus as validit10_53_, ruser0_1_.costCenter as costCen11_53_, ruser0_1_.emailAddress as emailAd12_53_, ruser0_1_.hasPhoto as hasPhot13_53_, ruser0_1_.locale as locale14_53_, ruser0_1_.locality_norm as localit15_53_, ruser0_1_.locality_orig as localit16_53_, ruser0_1_.preferredLanguage as preferr17_53_, ruser0_1_.telephoneNumber as telepho18_53_, ruser0_1_.timezone as timezon19_53_, ruser0_.additionalName_norm as addition1_73_, ruser0_.additionalName_orig as addition2_73_, ruser0_.employeeNumber as employee3_73_, ruser0_.familyName_norm as familyNa4_73_, ruser0_.familyName_orig as familyNa5_73_, ruser0_.fullName_norm as fullName6_73_, ruser0_.fullName_orig as fullName7_73_, ruser0_.givenName_norm as givenNam8_73_, ruser0_.givenName_orig as givenNam9_73_, ruser0_.honorificPrefix_norm as honorif10_73_, ruser0_.honorificPrefix_orig as honorif11_73_, ruser0_.honorificSuffix_norm as honorif12_73_, ruser0_.honorificSuffix_orig as honorif13_73_, ruser0_.name_norm as name_no14_73_, ruser0_.name_orig as name_or15_73_, ruser0_.nickName_norm as nickNam16_73_, ruser0_.nickName_orig as nickNam17_73_, ruser0_.title_norm as title_n18_73_, ruser0_.title_orig as title_o19_73_ from m_user ruser0_ inner join m_focus ruser0_1_ on ruser0_.oid=ruser0_1_.oid inner join m_object ruser0_2_ on ruser0_.oid=ruser0_2_.oid where ruser0_.oid=?
 [1] delete from m_object_ext_string where owner_oid=? and ownerType=? and item_id=? and stringValue=?
 [1] delete from m_object_ext_string where owner_oid=? and ownerType=? and item_id=? and stringValue=?
 [1] update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
         */
        if (isNoFetchDeletion()) {
            assertCounts(5, 5);
        } else {
            assertCounts(5, 6);
        }
    }

    @Test
    public void test036DeleteHiddenUserExtensionValuesDuplicate() throws Exception {
        OperationResult result = new OperationResult(ExtensionTest.class.getName() + ".test036DeleteHiddenUserExtensionValuesDuplicate");

        ObjectDelta<UserType> delta = getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_EXTENSION, EXT_HIDDEN1).delete("h1.1", "h1.2")
                .asObjectDelta("");
        delta.applyTo(expectedUser);

        queryListener.start();
        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), getOptions(), result);
        queryListener.dumpAndStop();

        try (EntityManager em = factory.createEntityManager()) {
            RUser u = em.find(RUser.class, userOid);
            assertExtension(u, itemHidden1, "h1.3", "h1.4");
            assertExtension(u, itemHidden2, "h2.1", "h2.2");
            assertExtension(u, itemHidden3, "h3.1");
            assertExtension(u, itemVisible, "v1", "v2", "v3");
            assertExtension(u, itemWeapon, "w1");
        }

        assertGetObject(result);
        assertSearch(EXT_HIDDEN1, "h1.1", 0, result);
        assertSearch(EXT_HIDDEN1, "h1.2", 0, result);
        assertSearch(EXT_HIDDEN1, "h1.3", 1, result);
        assertSearch(EXT_HIDDEN1, "h1.4", 1, result);
        assertSearch(EXT_HIDDEN1, "h1.5", 0, result);
        assertSearch(EXT_HIDDEN2, "h2.1", 1, result);
        assertSearch(EXT_HIDDEN2, "h2.2", 1, result);
        assertSearch(EXT_HIDDEN3, "h3.1", 1, result);
        assertSearch(EXT_HIDDEN3, "h1.3", 0, result);

        /*
        fetch-deletion:

 [1] select oid from m_object where oid = ? for update
 [1] select ruser0_.oid as oid1_27_, ruser0_2_.createChannel as createCh2_27_, ruser0_2_.createTimestamp as createTi3_27_, ruser0_2_.creatorRef_relation as creatorR4_27_, ruser0_2_.creatorRef_targetOid as creatorR5_27_, ruser0_2_.creatorRef_type as creatorR6_27_, ruser0_2_.fullObject as fullObje7_27_, ruser0_2_.lifecycleState as lifecycl8_27_, ruser0_2_.modifierRef_relation as modifier9_27_, ruser0_2_.modifierRef_targetOid as modifie10_27_, ruser0_2_.modifierRef_type as modifie11_27_, ruser0_2_.modifyChannel as modifyC12_27_, ruser0_2_.modifyTimestamp as modifyT13_27_, ruser0_2_.name_norm as name_no14_27_, ruser0_2_.name_orig as name_or15_27_, ruser0_2_.objectTypeClass as objectT16_27_, ruser0_2_.tenantRef_relation as tenantR17_27_, ruser0_2_.tenantRef_targetOid as tenantR18_27_, ruser0_2_.tenantRef_type as tenantR19_27_, ruser0_2_.version as version20_27_, ruser0_1_.administrativeStatus as administ1_53_, ruser0_1_.archiveTimestamp as archiveT2_53_, ruser0_1_.disableReason as disableR3_53_, ruser0_1_.disableTimestamp as disableT4_53_, ruser0_1_.effectiveStatus as effectiv5_53_, ruser0_1_.enableTimestamp as enableTi6_53_, ruser0_1_.validFrom as validFro7_53_, ruser0_1_.validTo as validTo8_53_, ruser0_1_.validityChangeTimestamp as validity9_53_, ruser0_1_.validityStatus as validit10_53_, ruser0_1_.costCenter as costCen11_53_, ruser0_1_.emailAddress as emailAd12_53_, ruser0_1_.hasPhoto as hasPhot13_53_, ruser0_1_.locale as locale14_53_, ruser0_1_.locality_norm as localit15_53_, ruser0_1_.locality_orig as localit16_53_, ruser0_1_.preferredLanguage as preferr17_53_, ruser0_1_.telephoneNumber as telepho18_53_, ruser0_1_.timezone as timezon19_53_, ruser0_.additionalName_norm as addition1_73_, ruser0_.additionalName_orig as addition2_73_, ruser0_.employeeNumber as employee3_73_, ruser0_.familyName_norm as familyNa4_73_, ruser0_.familyName_orig as familyNa5_73_, ruser0_.fullName_norm as fullName6_73_, ruser0_.fullName_orig as fullName7_73_, ruser0_.givenName_norm as givenNam8_73_, ruser0_.givenName_orig as givenNam9_73_, ruser0_.honorificPrefix_norm as honorif10_73_, ruser0_.honorificPrefix_orig as honorif11_73_, ruser0_.honorificSuffix_norm as honorif12_73_, ruser0_.honorificSuffix_orig as honorif13_73_, ruser0_.name_norm as name_no14_73_, ruser0_.name_orig as name_or15_73_, ruser0_.nickName_norm as nickNam16_73_, ruser0_.nickName_orig as nickNam17_73_, ruser0_.title_norm as title_n18_73_, ruser0_.title_orig as title_o19_73_ from m_user ruser0_ inner join m_focus ruser0_1_ on ruser0_.oid=ruser0_1_.oid inner join m_object ruser0_2_ on ruser0_.oid=ruser0_2_.oid where ruser0_.oid=?
 [1] select strings0_.owner_oid as owner_oi2_33_0_, strings0_.item_id as item_id1_33_0_, strings0_.ownerType as ownerTyp3_33_0_, strings0_.stringValue as stringVa4_33_0_, strings0_.item_id as item_id1_33_1_, strings0_.owner_oid as owner_oi2_33_1_, strings0_.ownerType as ownerTyp3_33_1_, strings0_.stringValue as stringVa4_33_1_ from m_object_ext_string strings0_ where strings0_.owner_oid=?
 [1] update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?

        no-fetch-deletion:

 [1] select oid from m_object where oid = ? for update
 [1] select ruser0_.oid as oid1_27_, ruser0_2_.createChannel as createCh2_27_, ruser0_2_.createTimestamp as createTi3_27_, ruser0_2_.creatorRef_relation as creatorR4_27_, ruser0_2_.creatorRef_targetOid as creatorR5_27_, ruser0_2_.creatorRef_type as creatorR6_27_, ruser0_2_.fullObject as fullObje7_27_, ruser0_2_.lifecycleState as lifecycl8_27_, ruser0_2_.modifierRef_relation as modifier9_27_, ruser0_2_.modifierRef_targetOid as modifie10_27_, ruser0_2_.modifierRef_type as modifie11_27_, ruser0_2_.modifyChannel as modifyC12_27_, ruser0_2_.modifyTimestamp as modifyT13_27_, ruser0_2_.name_norm as name_no14_27_, ruser0_2_.name_orig as name_or15_27_, ruser0_2_.objectTypeClass as objectT16_27_, ruser0_2_.tenantRef_relation as tenantR17_27_, ruser0_2_.tenantRef_targetOid as tenantR18_27_, ruser0_2_.tenantRef_type as tenantR19_27_, ruser0_2_.version as version20_27_, ruser0_1_.administrativeStatus as administ1_53_, ruser0_1_.archiveTimestamp as archiveT2_53_, ruser0_1_.disableReason as disableR3_53_, ruser0_1_.disableTimestamp as disableT4_53_, ruser0_1_.effectiveStatus as effectiv5_53_, ruser0_1_.enableTimestamp as enableTi6_53_, ruser0_1_.validFrom as validFro7_53_, ruser0_1_.validTo as validTo8_53_, ruser0_1_.validityChangeTimestamp as validity9_53_, ruser0_1_.validityStatus as validit10_53_, ruser0_1_.costCenter as costCen11_53_, ruser0_1_.emailAddress as emailAd12_53_, ruser0_1_.hasPhoto as hasPhot13_53_, ruser0_1_.locale as locale14_53_, ruser0_1_.locality_norm as localit15_53_, ruser0_1_.locality_orig as localit16_53_, ruser0_1_.preferredLanguage as preferr17_53_, ruser0_1_.telephoneNumber as telepho18_53_, ruser0_1_.timezone as timezon19_53_, ruser0_.additionalName_norm as addition1_73_, ruser0_.additionalName_orig as addition2_73_, ruser0_.employeeNumber as employee3_73_, ruser0_.familyName_norm as familyNa4_73_, ruser0_.familyName_orig as familyNa5_73_, ruser0_.fullName_norm as fullName6_73_, ruser0_.fullName_orig as fullName7_73_, ruser0_.givenName_norm as givenNam8_73_, ruser0_.givenName_orig as givenNam9_73_, ruser0_.honorificPrefix_norm as honorif10_73_, ruser0_.honorificPrefix_orig as honorif11_73_, ruser0_.honorificSuffix_norm as honorif12_73_, ruser0_.honorificSuffix_orig as honorif13_73_, ruser0_.name_norm as name_no14_73_, ruser0_.name_orig as name_or15_73_, ruser0_.nickName_norm as nickNam16_73_, ruser0_.nickName_orig as nickNam17_73_, ruser0_.title_norm as title_n18_73_, ruser0_.title_orig as title_o19_73_ from m_user ruser0_ inner join m_focus ruser0_1_ on ruser0_.oid=ruser0_1_.oid inner join m_object ruser0_2_ on ruser0_.oid=ruser0_2_.oid where ruser0_.oid=?
 [1] delete from m_object_ext_string where owner_oid=? and ownerType=? and item_id=? and stringValue=?
 [1] delete from m_object_ext_string where owner_oid=? and ownerType=? and item_id=? and stringValue=?
 [1] update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
         */
        if (isNoFetchDeletion()) {
            assertCounts(5, 5);
        } else {
            assertCounts(4, 4);
        }
    }

    @Test
    public void test040ReplaceHiddenExtensionValues() throws Exception {
        OperationResult result = new OperationResult(ExtensionTest.class.getName() + ".test040ReplaceHiddenExtensionValues");

        ObjectDelta<UserType> delta = getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_EXTENSION, EXT_HIDDEN1).replace("h1.2", "h1.5")
                .item(UserType.F_EXTENSION, EXT_HIDDEN2).replace()
                .item(UserType.F_EXTENSION, EXT_HIDDEN3).replace()
                .asObjectDelta("");
        delta.applyTo(expectedUser);

        queryListener.start();
        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), getOptions(), result);
        queryListener.dumpAndStop();

        try (EntityManager em = factory.createEntityManager()) {
            RUser u = em.find(RUser.class, userOid);
            assertExtension(u, itemHidden1, "h1.2", "h1.5");
            assertExtension(u, itemHidden2);
            assertExtension(u, itemHidden3);
            assertExtension(u, itemVisible, "v1", "v2", "v3");
            assertExtension(u, itemWeapon, "w1");
        }

        assertGetObject(result);
        assertSearch(EXT_HIDDEN1, "h1.1", 0, result);
        assertSearch(EXT_HIDDEN1, "h1.2", 1, result);
        assertSearch(EXT_HIDDEN1, "h1.3", 0, result);
        assertSearch(EXT_HIDDEN1, "h1.4", 0, result);
        assertSearch(EXT_HIDDEN1, "h1.5", 1, result);
        assertSearch(EXT_HIDDEN2, "h2.1", 0, result);
        assertSearch(EXT_HIDDEN2, "h2.2", 0, result);
        assertSearch(EXT_HIDDEN3, "h3.1", 0, result);
        assertSearch(EXT_HIDDEN3, "h1.3", 0, result);

        /*
 [1] select oid from m_object where oid = ? for update
 [1] select ruser0_.oid as oid1_27_, ruser0_2_.createChannel as createCh2_27_, ruser0_2_.createTimestamp as createTi3_27_, ruser0_2_.creatorRef_relation as creatorR4_27_, ruser0_2_.creatorRef_targetOid as creatorR5_27_, ruser0_2_.creatorRef_type as creatorR6_27_, ruser0_2_.fullObject as fullObje7_27_, ruser0_2_.lifecycleState as lifecycl8_27_, ruser0_2_.modifierRef_relation as modifier9_27_, ruser0_2_.modifierRef_targetOid as modifie10_27_, ruser0_2_.modifierRef_type as modifie11_27_, ruser0_2_.modifyChannel as modifyC12_27_, ruser0_2_.modifyTimestamp as modifyT13_27_, ruser0_2_.name_norm as name_no14_27_, ruser0_2_.name_orig as name_or15_27_, ruser0_2_.objectTypeClass as objectT16_27_, ruser0_2_.tenantRef_relation as tenantR17_27_, ruser0_2_.tenantRef_targetOid as tenantR18_27_, ruser0_2_.tenantRef_type as tenantR19_27_, ruser0_2_.version as version20_27_, ruser0_1_.administrativeStatus as administ1_53_, ruser0_1_.archiveTimestamp as archiveT2_53_, ruser0_1_.disableReason as disableR3_53_, ruser0_1_.disableTimestamp as disableT4_53_, ruser0_1_.effectiveStatus as effectiv5_53_, ruser0_1_.enableTimestamp as enableTi6_53_, ruser0_1_.validFrom as validFro7_53_, ruser0_1_.validTo as validTo8_53_, ruser0_1_.validityChangeTimestamp as validity9_53_, ruser0_1_.validityStatus as validit10_53_, ruser0_1_.costCenter as costCen11_53_, ruser0_1_.emailAddress as emailAd12_53_, ruser0_1_.hasPhoto as hasPhot13_53_, ruser0_1_.locale as locale14_53_, ruser0_1_.locality_norm as localit15_53_, ruser0_1_.locality_orig as localit16_53_, ruser0_1_.preferredLanguage as preferr17_53_, ruser0_1_.telephoneNumber as telepho18_53_, ruser0_1_.timezone as timezon19_53_, ruser0_.additionalName_norm as addition1_73_, ruser0_.additionalName_orig as addition2_73_, ruser0_.employeeNumber as employee3_73_, ruser0_.familyName_norm as familyNa4_73_, ruser0_.familyName_orig as familyNa5_73_, ruser0_.fullName_norm as fullName6_73_, ruser0_.fullName_orig as fullName7_73_, ruser0_.givenName_norm as givenNam8_73_, ruser0_.givenName_orig as givenNam9_73_, ruser0_.honorificPrefix_norm as honorif10_73_, ruser0_.honorificPrefix_orig as honorif11_73_, ruser0_.honorificSuffix_norm as honorif12_73_, ruser0_.honorificSuffix_orig as honorif13_73_, ruser0_.name_norm as name_no14_73_, ruser0_.name_orig as name_or15_73_, ruser0_.nickName_norm as nickNam16_73_, ruser0_.nickName_orig as nickNam17_73_, ruser0_.title_norm as title_n18_73_, ruser0_.title_orig as title_o19_73_ from m_user ruser0_ inner join m_focus ruser0_1_ on ruser0_.oid=ruser0_1_.oid inner join m_object ruser0_2_ on ruser0_.oid=ruser0_2_.oid where ruser0_.oid=?
 [1] select strings0_.owner_oid as owner_oi2_33_0_, strings0_.item_id as item_id1_33_0_, strings0_.ownerType as ownerTyp3_33_0_, strings0_.stringValue as stringVa4_33_0_, strings0_.item_id as item_id1_33_1_, strings0_.owner_oid as owner_oi2_33_1_, strings0_.ownerType as ownerTyp3_33_1_, strings0_.stringValue as stringVa4_33_1_ from m_object_ext_string strings0_ where strings0_.owner_oid=?
 [1] select roextstrin_.item_id, roextstrin_.owner_oid, roextstrin_.ownerType, roextstrin_.stringValue from m_object_ext_string roextstrin_ where roextstrin_.item_id=? and roextstrin_.owner_oid=? and roextstrin_.ownerType=? and roextstrin_.stringValue=?
 [1] select roextstrin_.item_id, roextstrin_.owner_oid, roextstrin_.ownerType, roextstrin_.stringValue from m_object_ext_string roextstrin_ where roextstrin_.item_id=? and roextstrin_.owner_oid=? and roextstrin_.ownerType=? and roextstrin_.stringValue=?
 [2] insert into m_object_ext_string (item_id, owner_oid, ownerType, stringValue) values (?, ?, ?, ?)
 [1] update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
 [5] delete from m_object_ext_string where item_id=? and owner_oid=? and ownerType=? and stringValue=?
         */
        assertCounts(8, 13);
    }

    @Test // MID-5573
    public void test045ReplaceVisibleExtensionValuesToNull() throws Exception {
        OperationResult result = new OperationResult(ExtensionTest.class.getName() + ".test045ReplaceVisibleExtensionValuesToNull");

        ObjectDelta<UserType> delta = getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_EXTENSION, EXT_VISIBLE).replace()
                .asObjectDelta("");
        delta.applyTo(expectedUser);

        queryListener.start();
        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), getOptions(), result);
        queryListener.dumpAndStop();

        try (EntityManager em = factory.createEntityManager()) {
            RUser u = em.find(RUser.class, userOid);
            assertExtension(u, itemHidden1, "h1.2", "h1.5");
            assertExtension(u, itemHidden2);
            assertExtension(u, itemHidden3);
            assertExtension(u, itemVisible);
            assertExtension(u, itemWeapon, "w1");
        }

        assertGetObject(result);
        assertSearch(EXT_VISIBLE, "v0", 0, result);
        assertSearch(EXT_VISIBLE, "v1", 0, result);
        assertSearch(EXT_VISIBLE, "v2", 0, result);
        assertSearch(EXT_VISIBLE, "v3", 0, result);

        /*
 [1] select oid from m_object where oid = ? for update
 [1] select ruser0_.oid as oid1_27_, ruser0_2_.createChannel as createCh2_27_, ruser0_2_.createTimestamp as createTi3_27_, ruser0_2_.creatorRef_relation as creatorR4_27_, ruser0_2_.creatorRef_targetOid as creatorR5_27_, ruser0_2_.creatorRef_type as creatorR6_27_, ruser0_2_.fullObject as fullObje7_27_, ruser0_2_.lifecycleState as lifecycl8_27_, ruser0_2_.modifierRef_relation as modifier9_27_, ruser0_2_.modifierRef_targetOid as modifie10_27_, ruser0_2_.modifierRef_type as modifie11_27_, ruser0_2_.modifyChannel as modifyC12_27_, ruser0_2_.modifyTimestamp as modifyT13_27_, ruser0_2_.name_norm as name_no14_27_, ruser0_2_.name_orig as name_or15_27_, ruser0_2_.objectTypeClass as objectT16_27_, ruser0_2_.tenantRef_relation as tenantR17_27_, ruser0_2_.tenantRef_targetOid as tenantR18_27_, ruser0_2_.tenantRef_type as tenantR19_27_, ruser0_2_.version as version20_27_, ruser0_1_.administrativeStatus as administ1_53_, ruser0_1_.archiveTimestamp as archiveT2_53_, ruser0_1_.disableReason as disableR3_53_, ruser0_1_.disableTimestamp as disableT4_53_, ruser0_1_.effectiveStatus as effectiv5_53_, ruser0_1_.enableTimestamp as enableTi6_53_, ruser0_1_.validFrom as validFro7_53_, ruser0_1_.validTo as validTo8_53_, ruser0_1_.validityChangeTimestamp as validity9_53_, ruser0_1_.validityStatus as validit10_53_, ruser0_1_.costCenter as costCen11_53_, ruser0_1_.emailAddress as emailAd12_53_, ruser0_1_.hasPhoto as hasPhot13_53_, ruser0_1_.locale as locale14_53_, ruser0_1_.locality_norm as localit15_53_, ruser0_1_.locality_orig as localit16_53_, ruser0_1_.preferredLanguage as preferr17_53_, ruser0_1_.telephoneNumber as telepho18_53_, ruser0_1_.timezone as timezon19_53_, ruser0_.additionalName_norm as addition1_73_, ruser0_.additionalName_orig as addition2_73_, ruser0_.employeeNumber as employee3_73_, ruser0_.familyName_norm as familyNa4_73_, ruser0_.familyName_orig as familyNa5_73_, ruser0_.fullName_norm as fullName6_73_, ruser0_.fullName_orig as fullName7_73_, ruser0_.givenName_norm as givenNam8_73_, ruser0_.givenName_orig as givenNam9_73_, ruser0_.honorificPrefix_norm as honorif10_73_, ruser0_.honorificPrefix_orig as honorif11_73_, ruser0_.honorificSuffix_norm as honorif12_73_, ruser0_.honorificSuffix_orig as honorif13_73_, ruser0_.name_norm as name_no14_73_, ruser0_.name_orig as name_or15_73_, ruser0_.nickName_norm as nickNam16_73_, ruser0_.nickName_orig as nickNam17_73_, ruser0_.title_norm as title_n18_73_, ruser0_.title_orig as title_o19_73_ from m_user ruser0_ inner join m_focus ruser0_1_ on ruser0_.oid=ruser0_1_.oid inner join m_object ruser0_2_ on ruser0_.oid=ruser0_2_.oid where ruser0_.oid=?
 [1] select strings0_.owner_oid as owner_oi2_33_0_, strings0_.item_id as item_id1_33_0_, strings0_.ownerType as ownerTyp3_33_0_, strings0_.stringValue as stringVa4_33_0_, strings0_.item_id as item_id1_33_1_, strings0_.owner_oid as owner_oi2_33_1_, strings0_.ownerType as ownerTyp3_33_1_, strings0_.stringValue as stringVa4_33_1_ from m_object_ext_string strings0_ where strings0_.owner_oid=?
 [1] update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
 [3] delete from m_object_ext_string where item_id=? and owner_oid=? and ownerType=? and stringValue=?
         */
        assertCounts(5, 7);
    }

    @Test
    public void test050DeleteWholeExtension() throws Exception {
        OperationResult result = new OperationResult(ExtensionTest.class.getName() + ".test050DeleteWholeExtension");

        PrismContainerValue<?> existingExtension = expectedUser.getExtension().getValue().clone();
        existingExtension.removeProperty(EXT_HIDDEN1);      // there are two values of this kind there

        ObjectDelta<UserType> delta = getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_EXTENSION)
                .delete(existingExtension)
                .asObjectDelta("");
        expectedUser.asObjectable().setExtension(null);     // we cannot apply the delta here as expectedUser has hidden1 values in the extension

        queryListener.start();
        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), getOptions(), result);
        queryListener.dumpAndStop();

        try (EntityManager em = factory.createEntityManager()) {
            RUser u = em.find(RUser.class, userOid);
            assertExtension(u, itemHidden1);
            assertExtension(u, itemHidden2);
            assertExtension(u, itemHidden3);
            assertExtension(u, itemVisible);
            assertExtension(u, itemWeapon);
        }

        assertGetObject(result);

        /*
 [1] select oid from m_object where oid = ? for update
 [1] select ruser0_.oid as oid1_27_, ruser0_2_.createChannel as createCh2_27_, ruser0_2_.createTimestamp as createTi3_27_, ruser0_2_.creatorRef_relation as creatorR4_27_, ruser0_2_.creatorRef_targetOid as creatorR5_27_, ruser0_2_.creatorRef_type as creatorR6_27_, ruser0_2_.fullObject as fullObje7_27_, ruser0_2_.lifecycleState as lifecycl8_27_, ruser0_2_.modifierRef_relation as modifier9_27_, ruser0_2_.modifierRef_targetOid as modifie10_27_, ruser0_2_.modifierRef_type as modifie11_27_, ruser0_2_.modifyChannel as modifyC12_27_, ruser0_2_.modifyTimestamp as modifyT13_27_, ruser0_2_.name_norm as name_no14_27_, ruser0_2_.name_orig as name_or15_27_, ruser0_2_.objectTypeClass as objectT16_27_, ruser0_2_.tenantRef_relation as tenantR17_27_, ruser0_2_.tenantRef_targetOid as tenantR18_27_, ruser0_2_.tenantRef_type as tenantR19_27_, ruser0_2_.version as version20_27_, ruser0_1_.administrativeStatus as administ1_53_, ruser0_1_.archiveTimestamp as archiveT2_53_, ruser0_1_.disableReason as disableR3_53_, ruser0_1_.disableTimestamp as disableT4_53_, ruser0_1_.effectiveStatus as effectiv5_53_, ruser0_1_.enableTimestamp as enableTi6_53_, ruser0_1_.validFrom as validFro7_53_, ruser0_1_.validTo as validTo8_53_, ruser0_1_.validityChangeTimestamp as validity9_53_, ruser0_1_.validityStatus as validit10_53_, ruser0_1_.costCenter as costCen11_53_, ruser0_1_.emailAddress as emailAd12_53_, ruser0_1_.hasPhoto as hasPhot13_53_, ruser0_1_.locale as locale14_53_, ruser0_1_.locality_norm as localit15_53_, ruser0_1_.locality_orig as localit16_53_, ruser0_1_.preferredLanguage as preferr17_53_, ruser0_1_.telephoneNumber as telepho18_53_, ruser0_1_.timezone as timezon19_53_, ruser0_.additionalName_norm as addition1_73_, ruser0_.additionalName_orig as addition2_73_, ruser0_.employeeNumber as employee3_73_, ruser0_.familyName_norm as familyNa4_73_, ruser0_.familyName_orig as familyNa5_73_, ruser0_.fullName_norm as fullName6_73_, ruser0_.fullName_orig as fullName7_73_, ruser0_.givenName_norm as givenNam8_73_, ruser0_.givenName_orig as givenNam9_73_, ruser0_.honorificPrefix_norm as honorif10_73_, ruser0_.honorificPrefix_orig as honorif11_73_, ruser0_.honorificSuffix_norm as honorif12_73_, ruser0_.honorificSuffix_orig as honorif13_73_, ruser0_.name_norm as name_no14_73_, ruser0_.name_orig as name_or15_73_, ruser0_.nickName_norm as nickNam16_73_, ruser0_.nickName_orig as nickNam17_73_, ruser0_.title_norm as title_n18_73_, ruser0_.title_orig as title_o19_73_ from m_user ruser0_ inner join m_focus ruser0_1_ on ruser0_.oid=ruser0_1_.oid inner join m_object ruser0_2_ on ruser0_.oid=ruser0_2_.oid where ruser0_.oid=?
 [1] select booleans0_.owner_oid as owner_oi2_28_0_, booleans0_.item_id as item_id1_28_0_, booleans0_.ownerType as ownerTyp3_28_0_, booleans0_.booleanValue as booleanV4_28_0_, booleans0_.item_id as item_id1_28_1_, booleans0_.owner_oid as owner_oi2_28_1_, booleans0_.ownerType as ownerTyp3_28_1_, booleans0_.booleanValue as booleanV4_28_1_ from m_object_ext_boolean booleans0_ where booleans0_.owner_oid=?
 [1] select dates0_.owner_oid as owner_oi2_29_0_, dates0_.item_id as item_id1_29_0_, dates0_.ownerType as ownerTyp3_29_0_, dates0_.dateValue as dateValu4_29_0_, dates0_.item_id as item_id1_29_1_, dates0_.owner_oid as owner_oi2_29_1_, dates0_.ownerType as ownerTyp3_29_1_, dates0_.dateValue as dateValu4_29_1_ from m_object_ext_date dates0_ where dates0_.owner_oid=?
 [1] select longs0_.owner_oid as owner_oi2_30_0_, longs0_.item_id as item_id1_30_0_, longs0_.ownerType as ownerTyp3_30_0_, longs0_.longValue as longValu4_30_0_, longs0_.item_id as item_id1_30_1_, longs0_.owner_oid as owner_oi2_30_1_, longs0_.ownerType as ownerTyp3_30_1_, longs0_.longValue as longValu4_30_1_ from m_object_ext_long longs0_ where longs0_.owner_oid=?
 [1] select polys0_.owner_oid as owner_oi2_31_0_, polys0_.item_id as item_id1_31_0_, polys0_.ownerType as ownerTyp3_31_0_, polys0_.orig as orig4_31_0_, polys0_.item_id as item_id1_31_1_, polys0_.owner_oid as owner_oi2_31_1_, polys0_.ownerType as ownerTyp3_31_1_, polys0_.orig as orig4_31_1_, polys0_.norm as norm5_31_1_ from m_object_ext_poly polys0_ where polys0_.owner_oid=?
 [1] select references0_.owner_oid as owner_oi2_32_0_, references0_.item_id as item_id1_32_0_, references0_.ownerType as ownerTyp3_32_0_, references0_.targetoid as targetoi4_32_0_, references0_.item_id as item_id1_32_1_, references0_.owner_oid as owner_oi2_32_1_, references0_.ownerType as ownerTyp3_32_1_, references0_.targetoid as targetoi4_32_1_, references0_.relation as relation5_32_1_, references0_.targetType as targetTy6_32_1_ from m_object_ext_reference references0_ where references0_.owner_oid=?
 [1] select strings0_.owner_oid as owner_oi2_33_0_, strings0_.item_id as item_id1_33_0_, strings0_.ownerType as ownerTyp3_33_0_, strings0_.stringValue as stringVa4_33_0_, strings0_.item_id as item_id1_33_1_, strings0_.owner_oid as owner_oi2_33_1_, strings0_.ownerType as ownerTyp3_33_1_, strings0_.stringValue as stringVa4_33_1_ from m_object_ext_string strings0_ where strings0_.owner_oid=?
 [1] update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
 [3] delete from m_object_ext_string where item_id=? and owner_oid=? and ownerType=? and stringValue=?
         */
        assertCounts(10, 12);
    }

    @Test
    public void test055AddWholeExtension() throws Exception {
        OperationResult result = new OperationResult(ExtensionTest.class.getName() + ".test055AddWholeExtension");

        PrismContainerValue<?> extValue = expectedUser.getDefinition().getExtensionDefinition().instantiate().getValue();
        extValue.findOrCreateProperty(EXT_HIDDEN1).addRealValues("H1:1", "H1:2");
        extValue.findOrCreateProperty(EXT_HIDDEN3).addRealValues("H3:1");
        extValue.findOrCreateProperty(EXT_VISIBLE).addRealValues("V1");
        extValue.findOrCreateProperty(EXT_VISIBLE).addRealValues("V2");

        ObjectDelta<UserType> delta = getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_EXTENSION)
                .add(extValue.clone())
                .asObjectDelta("");
        delta.applyTo(expectedUser);

        queryListener.start();
        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), getOptions(), result);
        queryListener.dumpAndStop();

        try (EntityManager em = factory.createEntityManager()) {
            RUser u = em.find(RUser.class, userOid);
            assertExtension(u, itemHidden1, "H1:1", "H1:2");
            assertExtension(u, itemHidden2);
            assertExtension(u, itemHidden3, "H3:1");
            assertExtension(u, itemVisible, "V1", "V2");
            assertExtension(u, itemWeapon);
        }

        assertGetObject(result);

        /*
 [1] select oid from m_object where oid = ? for update
 [1] select ruser0_.oid as oid1_27_, ruser0_2_.createChannel as createCh2_27_, ruser0_2_.createTimestamp as createTi3_27_, ruser0_2_.creatorRef_relation as creatorR4_27_, ruser0_2_.creatorRef_targetOid as creatorR5_27_, ruser0_2_.creatorRef_type as creatorR6_27_, ruser0_2_.fullObject as fullObje7_27_, ruser0_2_.lifecycleState as lifecycl8_27_, ruser0_2_.modifierRef_relation as modifier9_27_, ruser0_2_.modifierRef_targetOid as modifie10_27_, ruser0_2_.modifierRef_type as modifie11_27_, ruser0_2_.modifyChannel as modifyC12_27_, ruser0_2_.modifyTimestamp as modifyT13_27_, ruser0_2_.name_norm as name_no14_27_, ruser0_2_.name_orig as name_or15_27_, ruser0_2_.objectTypeClass as objectT16_27_, ruser0_2_.tenantRef_relation as tenantR17_27_, ruser0_2_.tenantRef_targetOid as tenantR18_27_, ruser0_2_.tenantRef_type as tenantR19_27_, ruser0_2_.version as version20_27_, ruser0_1_.administrativeStatus as administ1_53_, ruser0_1_.archiveTimestamp as archiveT2_53_, ruser0_1_.disableReason as disableR3_53_, ruser0_1_.disableTimestamp as disableT4_53_, ruser0_1_.effectiveStatus as effectiv5_53_, ruser0_1_.enableTimestamp as enableTi6_53_, ruser0_1_.validFrom as validFro7_53_, ruser0_1_.validTo as validTo8_53_, ruser0_1_.validityChangeTimestamp as validity9_53_, ruser0_1_.validityStatus as validit10_53_, ruser0_1_.costCenter as costCen11_53_, ruser0_1_.emailAddress as emailAd12_53_, ruser0_1_.hasPhoto as hasPhot13_53_, ruser0_1_.locale as locale14_53_, ruser0_1_.locality_norm as localit15_53_, ruser0_1_.locality_orig as localit16_53_, ruser0_1_.preferredLanguage as preferr17_53_, ruser0_1_.telephoneNumber as telepho18_53_, ruser0_1_.timezone as timezon19_53_, ruser0_.additionalName_norm as addition1_73_, ruser0_.additionalName_orig as addition2_73_, ruser0_.employeeNumber as employee3_73_, ruser0_.familyName_norm as familyNa4_73_, ruser0_.familyName_orig as familyNa5_73_, ruser0_.fullName_norm as fullName6_73_, ruser0_.fullName_orig as fullName7_73_, ruser0_.givenName_norm as givenNam8_73_, ruser0_.givenName_orig as givenNam9_73_, ruser0_.honorificPrefix_norm as honorif10_73_, ruser0_.honorificPrefix_orig as honorif11_73_, ruser0_.honorificSuffix_norm as honorif12_73_, ruser0_.honorificSuffix_orig as honorif13_73_, ruser0_.name_norm as name_no14_73_, ruser0_.name_orig as name_or15_73_, ruser0_.nickName_norm as nickNam16_73_, ruser0_.nickName_orig as nickNam17_73_, ruser0_.title_norm as title_n18_73_, ruser0_.title_orig as title_o19_73_ from m_user ruser0_ inner join m_focus ruser0_1_ on ruser0_.oid=ruser0_1_.oid inner join m_object ruser0_2_ on ruser0_.oid=ruser0_2_.oid where ruser0_.oid=?
 [1] select booleans0_.owner_oid as owner_oi2_28_0_, booleans0_.item_id as item_id1_28_0_, booleans0_.ownerType as ownerTyp3_28_0_, booleans0_.booleanValue as booleanV4_28_0_, booleans0_.item_id as item_id1_28_1_, booleans0_.owner_oid as owner_oi2_28_1_, booleans0_.ownerType as ownerTyp3_28_1_, booleans0_.booleanValue as booleanV4_28_1_ from m_object_ext_boolean booleans0_ where booleans0_.owner_oid=?
 [1] select dates0_.owner_oid as owner_oi2_29_0_, dates0_.item_id as item_id1_29_0_, dates0_.ownerType as ownerTyp3_29_0_, dates0_.dateValue as dateValu4_29_0_, dates0_.item_id as item_id1_29_1_, dates0_.owner_oid as owner_oi2_29_1_, dates0_.ownerType as ownerTyp3_29_1_, dates0_.dateValue as dateValu4_29_1_ from m_object_ext_date dates0_ where dates0_.owner_oid=?
 [1] select longs0_.owner_oid as owner_oi2_30_0_, longs0_.item_id as item_id1_30_0_, longs0_.ownerType as ownerTyp3_30_0_, longs0_.longValue as longValu4_30_0_, longs0_.item_id as item_id1_30_1_, longs0_.owner_oid as owner_oi2_30_1_, longs0_.ownerType as ownerTyp3_30_1_, longs0_.longValue as longValu4_30_1_ from m_object_ext_long longs0_ where longs0_.owner_oid=?
 [1] select polys0_.owner_oid as owner_oi2_31_0_, polys0_.item_id as item_id1_31_0_, polys0_.ownerType as ownerTyp3_31_0_, polys0_.orig as orig4_31_0_, polys0_.item_id as item_id1_31_1_, polys0_.owner_oid as owner_oi2_31_1_, polys0_.ownerType as ownerTyp3_31_1_, polys0_.orig as orig4_31_1_, polys0_.norm as norm5_31_1_ from m_object_ext_poly polys0_ where polys0_.owner_oid=?
 [1] select references0_.owner_oid as owner_oi2_32_0_, references0_.item_id as item_id1_32_0_, references0_.ownerType as ownerTyp3_32_0_, references0_.targetoid as targetoi4_32_0_, references0_.item_id as item_id1_32_1_, references0_.owner_oid as owner_oi2_32_1_, references0_.ownerType as ownerTyp3_32_1_, references0_.targetoid as targetoi4_32_1_, references0_.relation as relation5_32_1_, references0_.targetType as targetTy6_32_1_ from m_object_ext_reference references0_ where references0_.owner_oid=?
 [1] select strings0_.owner_oid as owner_oi2_33_0_, strings0_.item_id as item_id1_33_0_, strings0_.ownerType as ownerTyp3_33_0_, strings0_.stringValue as stringVa4_33_0_, strings0_.item_id as item_id1_33_1_, strings0_.owner_oid as owner_oi2_33_1_, strings0_.ownerType as ownerTyp3_33_1_, strings0_.stringValue as stringVa4_33_1_ from m_object_ext_string strings0_ where strings0_.owner_oid=?
 [5] insert into m_object_ext_string (item_id, owner_oid, ownerType, stringValue) values (?, ?, ?, ?)
 [1] update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
         */
        // todo Can we eliminate deletion selects? Only if we know there're no index-only entries + that extension did not exist before the modify operation.
        assertCounts(10 + getExtraSafeInsertionSelects(5), 14 + getExtraSafeInsertionSelects(5));
    }

    /**
     * This is really tricky. We try to add another value to single-valued extension container.
     */
    @Test
    public void test058AddWholeExtensionDifferentValue() throws Exception {
        OperationResult result = new OperationResult(ExtensionTest.class.getName() + ".test055AddWholeExtension");

        PrismContainerValue<?> extValue = expectedUser.getDefinition().getExtensionDefinition().instantiate().getValue();
        extValue.findOrCreateProperty(EXT_HIDDEN1).addRealValues("H1:100");
        extValue.findOrCreateProperty(EXT_VISIBLE).addRealValues("V3");

        ObjectDelta<UserType> delta = getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_EXTENSION)
                .add(extValue.clone())
                .asObjectDelta("");
        delta.applyTo(expectedUser);

        queryListener.start();
        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), getOptions(), result);
        queryListener.dumpAndStop();

        try (EntityManager em = factory.createEntityManager()) {
            RUser u = em.find(RUser.class, userOid);
            assertExtension(u, itemHidden1, "H1:100");
            assertExtension(u, itemHidden2);
            assertExtension(u, itemHidden3);
            assertExtension(u, itemVisible, "V3");
            assertExtension(u, itemWeapon);
        }

        assertGetObject(result);

        /*
 [1] select oid from m_object where oid = ? for update
 [1] select ruser0_.oid as oid1_27_, ruser0_2_.createChannel as createCh2_27_, ruser0_2_.createTimestamp as createTi3_27_, ruser0_2_.creatorRef_relation as creatorR4_27_, ruser0_2_.creatorRef_targetOid as creatorR5_27_, ruser0_2_.creatorRef_type as creatorR6_27_, ruser0_2_.fullObject as fullObje7_27_, ruser0_2_.lifecycleState as lifecycl8_27_, ruser0_2_.modifierRef_relation as modifier9_27_, ruser0_2_.modifierRef_targetOid as modifie10_27_, ruser0_2_.modifierRef_type as modifie11_27_, ruser0_2_.modifyChannel as modifyC12_27_, ruser0_2_.modifyTimestamp as modifyT13_27_, ruser0_2_.name_norm as name_no14_27_, ruser0_2_.name_orig as name_or15_27_, ruser0_2_.objectTypeClass as objectT16_27_, ruser0_2_.tenantRef_relation as tenantR17_27_, ruser0_2_.tenantRef_targetOid as tenantR18_27_, ruser0_2_.tenantRef_type as tenantR19_27_, ruser0_2_.version as version20_27_, ruser0_1_.administrativeStatus as administ1_53_, ruser0_1_.archiveTimestamp as archiveT2_53_, ruser0_1_.disableReason as disableR3_53_, ruser0_1_.disableTimestamp as disableT4_53_, ruser0_1_.effectiveStatus as effectiv5_53_, ruser0_1_.enableTimestamp as enableTi6_53_, ruser0_1_.validFrom as validFro7_53_, ruser0_1_.validTo as validTo8_53_, ruser0_1_.validityChangeTimestamp as validity9_53_, ruser0_1_.validityStatus as validit10_53_, ruser0_1_.costCenter as costCen11_53_, ruser0_1_.emailAddress as emailAd12_53_, ruser0_1_.hasPhoto as hasPhot13_53_, ruser0_1_.locale as locale14_53_, ruser0_1_.locality_norm as localit15_53_, ruser0_1_.locality_orig as localit16_53_, ruser0_1_.preferredLanguage as preferr17_53_, ruser0_1_.telephoneNumber as telepho18_53_, ruser0_1_.timezone as timezon19_53_, ruser0_.additionalName_norm as addition1_73_, ruser0_.additionalName_orig as addition2_73_, ruser0_.employeeNumber as employee3_73_, ruser0_.familyName_norm as familyNa4_73_, ruser0_.familyName_orig as familyNa5_73_, ruser0_.fullName_norm as fullName6_73_, ruser0_.fullName_orig as fullName7_73_, ruser0_.givenName_norm as givenNam8_73_, ruser0_.givenName_orig as givenNam9_73_, ruser0_.honorificPrefix_norm as honorif10_73_, ruser0_.honorificPrefix_orig as honorif11_73_, ruser0_.honorificSuffix_norm as honorif12_73_, ruser0_.honorificSuffix_orig as honorif13_73_, ruser0_.name_norm as name_no14_73_, ruser0_.name_orig as name_or15_73_, ruser0_.nickName_norm as nickNam16_73_, ruser0_.nickName_orig as nickNam17_73_, ruser0_.title_norm as title_n18_73_, ruser0_.title_orig as title_o19_73_ from m_user ruser0_ inner join m_focus ruser0_1_ on ruser0_.oid=ruser0_1_.oid inner join m_object ruser0_2_ on ruser0_.oid=ruser0_2_.oid where ruser0_.oid=?
 [1] select booleans0_.owner_oid as owner_oi2_28_0_, booleans0_.item_id as item_id1_28_0_, booleans0_.ownerType as ownerTyp3_28_0_, booleans0_.booleanValue as booleanV4_28_0_, booleans0_.item_id as item_id1_28_1_, booleans0_.owner_oid as owner_oi2_28_1_, booleans0_.ownerType as ownerTyp3_28_1_, booleans0_.booleanValue as booleanV4_28_1_ from m_object_ext_boolean booleans0_ where booleans0_.owner_oid=?
 [1] select dates0_.owner_oid as owner_oi2_29_0_, dates0_.item_id as item_id1_29_0_, dates0_.ownerType as ownerTyp3_29_0_, dates0_.dateValue as dateValu4_29_0_, dates0_.item_id as item_id1_29_1_, dates0_.owner_oid as owner_oi2_29_1_, dates0_.ownerType as ownerTyp3_29_1_, dates0_.dateValue as dateValu4_29_1_ from m_object_ext_date dates0_ where dates0_.owner_oid=?
 [1] select longs0_.owner_oid as owner_oi2_30_0_, longs0_.item_id as item_id1_30_0_, longs0_.ownerType as ownerTyp3_30_0_, longs0_.longValue as longValu4_30_0_, longs0_.item_id as item_id1_30_1_, longs0_.owner_oid as owner_oi2_30_1_, longs0_.ownerType as ownerTyp3_30_1_, longs0_.longValue as longValu4_30_1_ from m_object_ext_long longs0_ where longs0_.owner_oid=?
 [1] select polys0_.owner_oid as owner_oi2_31_0_, polys0_.item_id as item_id1_31_0_, polys0_.ownerType as ownerTyp3_31_0_, polys0_.orig as orig4_31_0_, polys0_.item_id as item_id1_31_1_, polys0_.owner_oid as owner_oi2_31_1_, polys0_.ownerType as ownerTyp3_31_1_, polys0_.orig as orig4_31_1_, polys0_.norm as norm5_31_1_ from m_object_ext_poly polys0_ where polys0_.owner_oid=?
 [1] select references0_.owner_oid as owner_oi2_32_0_, references0_.item_id as item_id1_32_0_, references0_.ownerType as ownerTyp3_32_0_, references0_.targetoid as targetoi4_32_0_, references0_.item_id as item_id1_32_1_, references0_.owner_oid as owner_oi2_32_1_, references0_.ownerType as ownerTyp3_32_1_, references0_.targetoid as targetoi4_32_1_, references0_.relation as relation5_32_1_, references0_.targetType as targetTy6_32_1_ from m_object_ext_reference references0_ where references0_.owner_oid=?
 [1] select strings0_.owner_oid as owner_oi2_33_0_, strings0_.item_id as item_id1_33_0_, strings0_.ownerType as ownerTyp3_33_0_, strings0_.stringValue as stringVa4_33_0_, strings0_.item_id as item_id1_33_1_, strings0_.owner_oid as owner_oi2_33_1_, strings0_.ownerType as ownerTyp3_33_1_, strings0_.stringValue as stringVa4_33_1_ from m_object_ext_string strings0_ where strings0_.owner_oid=?
 [2] insert into m_object_ext_string (item_id, owner_oid, ownerType, stringValue) values (?, ?, ?, ?)
 [1] update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
 [5] delete from m_object_ext_string where item_id=? and owner_oid=? and ownerType=? and stringValue=?
         */
        assertCounts(11 + getExtraSafeInsertionSelects(2), 16 + getExtraSafeInsertionSelects(2));
    }

    @Test
    public void test060ReplaceWholeExtension() throws Exception {
        OperationResult result = new OperationResult(ExtensionTest.class.getName() + ".test060ReplaceWholeExtension");

        PrismContainerValue<?> extValue = expectedUser.getDefinition().getExtensionDefinition().instantiate().getValue();
        extValue.findOrCreateProperty(EXT_HIDDEN1).addRealValues("H1:2", "H1:3");
        extValue.findOrCreateProperty(EXT_HIDDEN2).addRealValues("H2:1");

        ObjectDelta<UserType> delta = getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_EXTENSION)
                .replace(extValue.clone())
                .asObjectDelta("");
        delta.applyTo(expectedUser);

        queryListener.start();
        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), getOptions(), result);
        queryListener.dumpAndStop();

        try (EntityManager em = factory.createEntityManager()) {
            RUser u = em.find(RUser.class, userOid);
            assertExtension(u, itemHidden1, "H1:2", "H1:3");
            assertExtension(u, itemHidden2, "H2:1");
            assertExtension(u, itemHidden3);
            assertExtension(u, itemVisible);
            assertExtension(u, itemWeapon);
        }

        // Disabled because of null/empty extension dichotomy: getObject returns empty extension while we expect null one.
        // This is really not interesting.
        //assertGetObject(result);

        /*
 [1] select oid from m_object where oid = ? for update
 [1] select ruser0_.oid as oid1_27_, ruser0_2_.createChannel as createCh2_27_, ruser0_2_.createTimestamp as createTi3_27_, ruser0_2_.creatorRef_relation as creatorR4_27_, ruser0_2_.creatorRef_targetOid as creatorR5_27_, ruser0_2_.creatorRef_type as creatorR6_27_, ruser0_2_.fullObject as fullObje7_27_, ruser0_2_.lifecycleState as lifecycl8_27_, ruser0_2_.modifierRef_relation as modifier9_27_, ruser0_2_.modifierRef_targetOid as modifie10_27_, ruser0_2_.modifierRef_type as modifie11_27_, ruser0_2_.modifyChannel as modifyC12_27_, ruser0_2_.modifyTimestamp as modifyT13_27_, ruser0_2_.name_norm as name_no14_27_, ruser0_2_.name_orig as name_or15_27_, ruser0_2_.objectTypeClass as objectT16_27_, ruser0_2_.tenantRef_relation as tenantR17_27_, ruser0_2_.tenantRef_targetOid as tenantR18_27_, ruser0_2_.tenantRef_type as tenantR19_27_, ruser0_2_.version as version20_27_, ruser0_1_.administrativeStatus as administ1_53_, ruser0_1_.archiveTimestamp as archiveT2_53_, ruser0_1_.disableReason as disableR3_53_, ruser0_1_.disableTimestamp as disableT4_53_, ruser0_1_.effectiveStatus as effectiv5_53_, ruser0_1_.enableTimestamp as enableTi6_53_, ruser0_1_.validFrom as validFro7_53_, ruser0_1_.validTo as validTo8_53_, ruser0_1_.validityChangeTimestamp as validity9_53_, ruser0_1_.validityStatus as validit10_53_, ruser0_1_.costCenter as costCen11_53_, ruser0_1_.emailAddress as emailAd12_53_, ruser0_1_.hasPhoto as hasPhot13_53_, ruser0_1_.locale as locale14_53_, ruser0_1_.locality_norm as localit15_53_, ruser0_1_.locality_orig as localit16_53_, ruser0_1_.preferredLanguage as preferr17_53_, ruser0_1_.telephoneNumber as telepho18_53_, ruser0_1_.timezone as timezon19_53_, ruser0_.additionalName_norm as addition1_73_, ruser0_.additionalName_orig as addition2_73_, ruser0_.employeeNumber as employee3_73_, ruser0_.familyName_norm as familyNa4_73_, ruser0_.familyName_orig as familyNa5_73_, ruser0_.fullName_norm as fullName6_73_, ruser0_.fullName_orig as fullName7_73_, ruser0_.givenName_norm as givenNam8_73_, ruser0_.givenName_orig as givenNam9_73_, ruser0_.honorificPrefix_norm as honorif10_73_, ruser0_.honorificPrefix_orig as honorif11_73_, ruser0_.honorificSuffix_norm as honorif12_73_, ruser0_.honorificSuffix_orig as honorif13_73_, ruser0_.name_norm as name_no14_73_, ruser0_.name_orig as name_or15_73_, ruser0_.nickName_norm as nickNam16_73_, ruser0_.nickName_orig as nickNam17_73_, ruser0_.title_norm as title_n18_73_, ruser0_.title_orig as title_o19_73_ from m_user ruser0_ inner join m_focus ruser0_1_ on ruser0_.oid=ruser0_1_.oid inner join m_object ruser0_2_ on ruser0_.oid=ruser0_2_.oid where ruser0_.oid=?
 [1] select booleans0_.owner_oid as owner_oi2_28_0_, booleans0_.item_id as item_id1_28_0_, booleans0_.ownerType as ownerTyp3_28_0_, booleans0_.booleanValue as booleanV4_28_0_, booleans0_.item_id as item_id1_28_1_, booleans0_.owner_oid as owner_oi2_28_1_, booleans0_.ownerType as ownerTyp3_28_1_, booleans0_.booleanValue as booleanV4_28_1_ from m_object_ext_boolean booleans0_ where booleans0_.owner_oid=?
 [1] select dates0_.owner_oid as owner_oi2_29_0_, dates0_.item_id as item_id1_29_0_, dates0_.ownerType as ownerTyp3_29_0_, dates0_.dateValue as dateValu4_29_0_, dates0_.item_id as item_id1_29_1_, dates0_.owner_oid as owner_oi2_29_1_, dates0_.ownerType as ownerTyp3_29_1_, dates0_.dateValue as dateValu4_29_1_ from m_object_ext_date dates0_ where dates0_.owner_oid=?
 [1] select longs0_.owner_oid as owner_oi2_30_0_, longs0_.item_id as item_id1_30_0_, longs0_.ownerType as ownerTyp3_30_0_, longs0_.longValue as longValu4_30_0_, longs0_.item_id as item_id1_30_1_, longs0_.owner_oid as owner_oi2_30_1_, longs0_.ownerType as ownerTyp3_30_1_, longs0_.longValue as longValu4_30_1_ from m_object_ext_long longs0_ where longs0_.owner_oid=?
 [1] select polys0_.owner_oid as owner_oi2_31_0_, polys0_.item_id as item_id1_31_0_, polys0_.ownerType as ownerTyp3_31_0_, polys0_.orig as orig4_31_0_, polys0_.item_id as item_id1_31_1_, polys0_.owner_oid as owner_oi2_31_1_, polys0_.ownerType as ownerTyp3_31_1_, polys0_.orig as orig4_31_1_, polys0_.norm as norm5_31_1_ from m_object_ext_poly polys0_ where polys0_.owner_oid=?
 [1] select references0_.owner_oid as owner_oi2_32_0_, references0_.item_id as item_id1_32_0_, references0_.ownerType as ownerTyp3_32_0_, references0_.targetoid as targetoi4_32_0_, references0_.item_id as item_id1_32_1_, references0_.owner_oid as owner_oi2_32_1_, references0_.ownerType as ownerTyp3_32_1_, references0_.targetoid as targetoi4_32_1_, references0_.relation as relation5_32_1_, references0_.targetType as targetTy6_32_1_ from m_object_ext_reference references0_ where references0_.owner_oid=?
 [1] select strings0_.owner_oid as owner_oi2_33_0_, strings0_.item_id as item_id1_33_0_, strings0_.ownerType as ownerTyp3_33_0_, strings0_.stringValue as stringVa4_33_0_, strings0_.item_id as item_id1_33_1_, strings0_.owner_oid as owner_oi2_33_1_, strings0_.ownerType as ownerTyp3_33_1_, strings0_.stringValue as stringVa4_33_1_ from m_object_ext_string strings0_ where strings0_.owner_oid=?
 [3] insert into m_object_ext_string (item_id, owner_oid, ownerType, stringValue) values (?, ?, ?, ?)
 [1] update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
 [2] delete from m_object_ext_string where item_id=? and owner_oid=? and ownerType=? and stringValue=?
         */
        assertCounts(11 + getExtraSafeInsertionSelects(3), 14 + getExtraSafeInsertionSelects(3));
    }

    @Test
    public void test065ReplaceWholeExtensionToNull() throws Exception {
        OperationResult result = new OperationResult(ExtensionTest.class.getName() + ".test065ReplaceWholeExtensionToNull");

        ObjectDelta<UserType> delta = getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_EXTENSION)
                .replace()
                .asObjectDelta("");
        delta.applyTo(expectedUser);

        queryListener.start();
        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), getOptions(), result);
        queryListener.dumpAndStop();

        try (EntityManager em = factory.createEntityManager()) {
            RUser u = em.find(RUser.class, userOid);
            assertExtension(u, itemHidden1);
            assertExtension(u, itemHidden2);
            assertExtension(u, itemHidden3);
            assertExtension(u, itemVisible);
            assertExtension(u, itemWeapon);
        }

        assertGetObject(result);

        /*
 [1] select oid from m_object where oid = ? for update
 [1] select ruser0_.oid as oid1_27_, ruser0_2_.createChannel as createCh2_27_, ruser0_2_.createTimestamp as createTi3_27_, ruser0_2_.creatorRef_relation as creatorR4_27_, ruser0_2_.creatorRef_targetOid as creatorR5_27_, ruser0_2_.creatorRef_type as creatorR6_27_, ruser0_2_.fullObject as fullObje7_27_, ruser0_2_.lifecycleState as lifecycl8_27_, ruser0_2_.modifierRef_relation as modifier9_27_, ruser0_2_.modifierRef_targetOid as modifie10_27_, ruser0_2_.modifierRef_type as modifie11_27_, ruser0_2_.modifyChannel as modifyC12_27_, ruser0_2_.modifyTimestamp as modifyT13_27_, ruser0_2_.name_norm as name_no14_27_, ruser0_2_.name_orig as name_or15_27_, ruser0_2_.objectTypeClass as objectT16_27_, ruser0_2_.tenantRef_relation as tenantR17_27_, ruser0_2_.tenantRef_targetOid as tenantR18_27_, ruser0_2_.tenantRef_type as tenantR19_27_, ruser0_2_.version as version20_27_, ruser0_1_.administrativeStatus as administ1_53_, ruser0_1_.archiveTimestamp as archiveT2_53_, ruser0_1_.disableReason as disableR3_53_, ruser0_1_.disableTimestamp as disableT4_53_, ruser0_1_.effectiveStatus as effectiv5_53_, ruser0_1_.enableTimestamp as enableTi6_53_, ruser0_1_.validFrom as validFro7_53_, ruser0_1_.validTo as validTo8_53_, ruser0_1_.validityChangeTimestamp as validity9_53_, ruser0_1_.validityStatus as validit10_53_, ruser0_1_.costCenter as costCen11_53_, ruser0_1_.emailAddress as emailAd12_53_, ruser0_1_.hasPhoto as hasPhot13_53_, ruser0_1_.locale as locale14_53_, ruser0_1_.locality_norm as localit15_53_, ruser0_1_.locality_orig as localit16_53_, ruser0_1_.preferredLanguage as preferr17_53_, ruser0_1_.telephoneNumber as telepho18_53_, ruser0_1_.timezone as timezon19_53_, ruser0_.additionalName_norm as addition1_73_, ruser0_.additionalName_orig as addition2_73_, ruser0_.employeeNumber as employee3_73_, ruser0_.familyName_norm as familyNa4_73_, ruser0_.familyName_orig as familyNa5_73_, ruser0_.fullName_norm as fullName6_73_, ruser0_.fullName_orig as fullName7_73_, ruser0_.givenName_norm as givenNam8_73_, ruser0_.givenName_orig as givenNam9_73_, ruser0_.honorificPrefix_norm as honorif10_73_, ruser0_.honorificPrefix_orig as honorif11_73_, ruser0_.honorificSuffix_norm as honorif12_73_, ruser0_.honorificSuffix_orig as honorif13_73_, ruser0_.name_norm as name_no14_73_, ruser0_.name_orig as name_or15_73_, ruser0_.nickName_norm as nickNam16_73_, ruser0_.nickName_orig as nickNam17_73_, ruser0_.title_norm as title_n18_73_, ruser0_.title_orig as title_o19_73_ from m_user ruser0_ inner join m_focus ruser0_1_ on ruser0_.oid=ruser0_1_.oid inner join m_object ruser0_2_ on ruser0_.oid=ruser0_2_.oid where ruser0_.oid=?
 [1] select booleans0_.owner_oid as owner_oi2_28_0_, booleans0_.item_id as item_id1_28_0_, booleans0_.ownerType as ownerTyp3_28_0_, booleans0_.booleanValue as booleanV4_28_0_, booleans0_.item_id as item_id1_28_1_, booleans0_.owner_oid as owner_oi2_28_1_, booleans0_.ownerType as ownerTyp3_28_1_, booleans0_.booleanValue as booleanV4_28_1_ from m_object_ext_boolean booleans0_ where booleans0_.owner_oid=?
 [1] select dates0_.owner_oid as owner_oi2_29_0_, dates0_.item_id as item_id1_29_0_, dates0_.ownerType as ownerTyp3_29_0_, dates0_.dateValue as dateValu4_29_0_, dates0_.item_id as item_id1_29_1_, dates0_.owner_oid as owner_oi2_29_1_, dates0_.ownerType as ownerTyp3_29_1_, dates0_.dateValue as dateValu4_29_1_ from m_object_ext_date dates0_ where dates0_.owner_oid=?
 [1] select longs0_.owner_oid as owner_oi2_30_0_, longs0_.item_id as item_id1_30_0_, longs0_.ownerType as ownerTyp3_30_0_, longs0_.longValue as longValu4_30_0_, longs0_.item_id as item_id1_30_1_, longs0_.owner_oid as owner_oi2_30_1_, longs0_.ownerType as ownerTyp3_30_1_, longs0_.longValue as longValu4_30_1_ from m_object_ext_long longs0_ where longs0_.owner_oid=?
 [1] select polys0_.owner_oid as owner_oi2_31_0_, polys0_.item_id as item_id1_31_0_, polys0_.ownerType as ownerTyp3_31_0_, polys0_.orig as orig4_31_0_, polys0_.item_id as item_id1_31_1_, polys0_.owner_oid as owner_oi2_31_1_, polys0_.ownerType as ownerTyp3_31_1_, polys0_.orig as orig4_31_1_, polys0_.norm as norm5_31_1_ from m_object_ext_poly polys0_ where polys0_.owner_oid=?
 [1] select references0_.owner_oid as owner_oi2_32_0_, references0_.item_id as item_id1_32_0_, references0_.ownerType as ownerTyp3_32_0_, references0_.targetoid as targetoi4_32_0_, references0_.item_id as item_id1_32_1_, references0_.owner_oid as owner_oi2_32_1_, references0_.ownerType as ownerTyp3_32_1_, references0_.targetoid as targetoi4_32_1_, references0_.relation as relation5_32_1_, references0_.targetType as targetTy6_32_1_ from m_object_ext_reference references0_ where references0_.owner_oid=?
 [1] select strings0_.owner_oid as owner_oi2_33_0_, strings0_.item_id as item_id1_33_0_, strings0_.ownerType as ownerTyp3_33_0_, strings0_.stringValue as stringVa4_33_0_, strings0_.item_id as item_id1_33_1_, strings0_.owner_oid as owner_oi2_33_1_, strings0_.ownerType as ownerTyp3_33_1_, strings0_.stringValue as stringVa4_33_1_ from m_object_ext_string strings0_ where strings0_.owner_oid=?
 [1] update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
 [3] delete from m_object_ext_string where item_id=? and owner_oid=? and ownerType=? and stringValue=?
         */
        assertCounts(10, 12);
    }

    private void assertGetObject(OperationResult result) throws SchemaException, ObjectNotFoundException {
        assertGetObjectNoInclude(userOid, expectedUser, result);
        assertGetObjectInclude(userOid, null, expectedUser, result);
        assertGetObjectInclude(userOid, singleton(EXT_HIDDEN1), expectedUser, result);
        assertGetObjectInclude(userOid, Arrays.asList(EXT_HIDDEN1, EXT_HIDDEN2), expectedUser, result);
        assertGetObjectInclude(userOid, Arrays.asList(EXT_HIDDEN1, EXT_HIDDEN2, EXT_HIDDEN3), expectedUser, result);
    }

    @Test
    public void test110AddUserWithAssignment() throws Exception {
        OperationResult result = new OperationResult(ExtensionTest.class.getName() + ".test110AddUserWithAssignment");

        PrismObject<UserType> user = PrismTestUtil.parseObject(USER_MANKA_FILE);
        expectedUser = PrismTestUtil.parseObject(USER_MANKA_FILE);

        queryListener.start();
        userOid = repositoryService.addObject(user, null, result);
        queryListener.dumpAndStop();

        try (EntityManager em = factory.createEntityManager()) {
            RUser u = em.find(RUser.class, userOid);
            assertExtension(u, itemHidden1, "h1.1", "h1.2");
            assertExtension(u, itemHidden2);
            assertExtension(u, itemHidden3);
            assertExtension(u, itemVisible, "v1", "v2", "v3");
            assertExtension(u, itemWeapon);
            RAssignment a = u.getAssignments().iterator().next();
            assertExtension(a, itemWeapon, "w1", "w2", "w3");
            assertExtension(a, itemShipName, "none");
        }

        assertGetObject(result);

        /*
 [1] select count(*) from m_user where oid=?
 [1] insert into m_object (createChannel, createTimestamp, creatorRef_relation, creatorRef_targetOid, creatorRef_type, fullObject, lifecycleState, modifierRef_relation, modifierRef_targetOid, modifierRef_type, modifyChannel, modifyTimestamp, name_norm, name_orig, objectTypeClass, tenantRef_relation, tenantRef_targetOid, tenantRef_type, version, oid) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
 [1] insert into m_focus (administrativeStatus, archiveTimestamp, disableReason, disableTimestamp, effectiveStatus, enableTimestamp, validFrom, validTo, validityChangeTimestamp, validityStatus, costCenter, emailAddress, hasPhoto, locale, locality_norm, locality_orig, preferredLanguage, telephoneNumber, timezone, oid) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
 [1] insert into m_user (additionalName_norm, additionalName_orig, employeeNumber, familyName_norm, familyName_orig, fullName_norm, fullName_orig, givenName_norm, givenName_orig, honorificPrefix_norm, honorificPrefix_orig, honorificSuffix_norm, honorificSuffix_orig, name_norm, name_orig, nickName_norm, nickName_orig, title_norm, title_orig, oid) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
 [1] insert into m_assignment (administrativeStatus, archiveTimestamp, disableReason, disableTimestamp, effectiveStatus, enableTimestamp, validFrom, validTo, validityChangeTimestamp, validityStatus, assignmentOwner, createChannel, createTimestamp, creatorRef_relation, creatorRef_targetOid, creatorRef_type, extId, extOid, lifecycleState, modifierRef_relation, modifierRef_targetOid, modifierRef_type, modifyChannel, modifyTimestamp, orderValue, orgRef_relation, orgRef_targetOid, orgRef_type, resourceRef_relation, resourceRef_targetOid, resourceRef_type, targetRef_relation, targetRef_targetOid, targetRef_type, tenantRef_relation, tenantRef_targetOid, tenantRef_type, id, owner_oid) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
 [1] insert into m_assignment_extension (owner_id, owner_owner_oid) values (?, ?)
 [4] insert into m_assignment_ext_string (item_id, anyContainer_owner_id, anyContainer_owner_owner_oid, stringValue) values (?, ?, ?, ?)
 [5] insert into m_object_ext_string (item_id, owner_oid, ownerType, stringValue) values (?, ?, ?, ?)
 [1] update m_assignment set administrativeStatus=?, archiveTimestamp=?, disableReason=?, disableTimestamp=?, effectiveStatus=?, enableTimestamp=?, validFrom=?, validTo=?, validityChangeTimestamp=?, validityStatus=?, assignmentOwner=?, createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, extId=?, extOid=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, orderValue=?, orgRef_relation=?, orgRef_targetOid=?, orgRef_type=?, resourceRef_relation=?, resourceRef_targetOid=?, resourceRef_type=?, targetRef_relation=?, targetRef_targetOid=?, targetRef_type=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=? where id=? and owner_oid=?
         */
        assertCounts(9, 16);
    }

    @Test
    public void test120AddAssignmentExtensionValue() throws Exception {
        OperationResult result = new OperationResult(ExtensionTest.class.getName() + ".test120AddAssignmentExtensionValue");

        ObjectDelta<UserType> delta = getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT, 1, AssignmentType.F_EXTENSION, EXT_WEAPON)
                .add("w4", "w5")
                .asObjectDelta("");
        delta.applyTo(expectedUser);

        queryListener.start();
        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), getOptions(), result);
        queryListener.dumpAndStop();

        try (EntityManager em = factory.createEntityManager()) {
            RUser u = em.find(RUser.class, userOid);
            assertExtension(u, itemHidden1, "h1.1", "h1.2");
            assertExtension(u, itemHidden2);
            assertExtension(u, itemHidden3);
            assertExtension(u, itemVisible, "v1", "v2", "v3");
            assertExtension(u, itemWeapon);
            RAssignment a = u.getAssignments().iterator().next();
            assertExtension(a, itemWeapon, "w1", "w2", "w3", "w4", "w5");
            assertExtension(a, itemShipName, "none");
        }

        assertGetObject(result);

        /*
 [1] select oid from m_object where oid = ? for update
 [1] select ruser0_.oid as oid1_27_, ruser0_2_.createChannel as createCh2_27_, ruser0_2_.createTimestamp as createTi3_27_, ruser0_2_.creatorRef_relation as creatorR4_27_, ruser0_2_.creatorRef_targetOid as creatorR5_27_, ruser0_2_.creatorRef_type as creatorR6_27_, ruser0_2_.fullObject as fullObje7_27_, ruser0_2_.lifecycleState as lifecycl8_27_, ruser0_2_.modifierRef_relation as modifier9_27_, ruser0_2_.modifierRef_targetOid as modifie10_27_, ruser0_2_.modifierRef_type as modifie11_27_, ruser0_2_.modifyChannel as modifyC12_27_, ruser0_2_.modifyTimestamp as modifyT13_27_, ruser0_2_.name_norm as name_no14_27_, ruser0_2_.name_orig as name_or15_27_, ruser0_2_.objectTypeClass as objectT16_27_, ruser0_2_.tenantRef_relation as tenantR17_27_, ruser0_2_.tenantRef_targetOid as tenantR18_27_, ruser0_2_.tenantRef_type as tenantR19_27_, ruser0_2_.version as version20_27_, ruser0_1_.administrativeStatus as administ1_53_, ruser0_1_.archiveTimestamp as archiveT2_53_, ruser0_1_.disableReason as disableR3_53_, ruser0_1_.disableTimestamp as disableT4_53_, ruser0_1_.effectiveStatus as effectiv5_53_, ruser0_1_.enableTimestamp as enableTi6_53_, ruser0_1_.validFrom as validFro7_53_, ruser0_1_.validTo as validTo8_53_, ruser0_1_.validityChangeTimestamp as validity9_53_, ruser0_1_.validityStatus as validit10_53_, ruser0_1_.costCenter as costCen11_53_, ruser0_1_.emailAddress as emailAd12_53_, ruser0_1_.hasPhoto as hasPhot13_53_, ruser0_1_.locale as locale14_53_, ruser0_1_.locality_norm as localit15_53_, ruser0_1_.locality_orig as localit16_53_, ruser0_1_.preferredLanguage as preferr17_53_, ruser0_1_.telephoneNumber as telepho18_53_, ruser0_1_.timezone as timezon19_53_, ruser0_.additionalName_norm as addition1_73_, ruser0_.additionalName_orig as addition2_73_, ruser0_.employeeNumber as employee3_73_, ruser0_.familyName_norm as familyNa4_73_, ruser0_.familyName_orig as familyNa5_73_, ruser0_.fullName_norm as fullName6_73_, ruser0_.fullName_orig as fullName7_73_, ruser0_.givenName_norm as givenNam8_73_, ruser0_.givenName_orig as givenNam9_73_, ruser0_.honorificPrefix_norm as honorif10_73_, ruser0_.honorificPrefix_orig as honorif11_73_, ruser0_.honorificSuffix_norm as honorif12_73_, ruser0_.honorificSuffix_orig as honorif13_73_, ruser0_.name_norm as name_no14_73_, ruser0_.name_orig as name_or15_73_, ruser0_.nickName_norm as nickNam16_73_, ruser0_.nickName_orig as nickNam17_73_, ruser0_.title_norm as title_n18_73_, ruser0_.title_orig as title_o19_73_ from m_user ruser0_ inner join m_focus ruser0_1_ on ruser0_.oid=ruser0_1_.oid inner join m_object ruser0_2_ on ruser0_.oid=ruser0_2_.oid where ruser0_.oid=?
 [1] select assignment0_.owner_oid as owner_oi2_5_0_, assignment0_.id as id1_5_0_, assignment0_.id as id1_5_1_, assignment0_.owner_oid as owner_oi2_5_1_, assignment0_.administrativeStatus as administ3_5_1_, assignment0_.archiveTimestamp as archiveT4_5_1_, assignment0_.disableReason as disableR5_5_1_, assignment0_.disableTimestamp as disableT6_5_1_, assignment0_.effectiveStatus as effectiv7_5_1_, assignment0_.enableTimestamp as enableTi8_5_1_, assignment0_.validFrom as validFro9_5_1_, assignment0_.validTo as validTo10_5_1_, assignment0_.validityChangeTimestamp as validit11_5_1_, assignment0_.validityStatus as validit12_5_1_, assignment0_.assignmentOwner as assignm13_5_1_, assignment0_.createChannel as createC14_5_1_, assignment0_.createTimestamp as createT15_5_1_, assignment0_.creatorRef_relation as creator16_5_1_, assignment0_.creatorRef_targetOid as creator17_5_1_, assignment0_.creatorRef_type as creator18_5_1_, assignment0_.extId as extId38_5_1_, assignment0_.extOid as extOid39_5_1_, assignment0_.lifecycleState as lifecyc19_5_1_, assignment0_.modifierRef_relation as modifie20_5_1_, assignment0_.modifierRef_targetOid as modifie21_5_1_, assignment0_.modifierRef_type as modifie22_5_1_, assignment0_.modifyChannel as modifyC23_5_1_, assignment0_.modifyTimestamp as modifyT24_5_1_, assignment0_.orderValue as orderVa25_5_1_, assignment0_.orgRef_relation as orgRef_26_5_1_, assignment0_.orgRef_targetOid as orgRef_27_5_1_, assignment0_.orgRef_type as orgRef_28_5_1_, assignment0_.resourceRef_relation as resourc29_5_1_, assignment0_.resourceRef_targetOid as resourc30_5_1_, assignment0_.resourceRef_type as resourc31_5_1_, assignment0_.targetRef_relation as targetR32_5_1_, assignment0_.targetRef_targetOid as targetR33_5_1_, assignment0_.targetRef_type as targetR34_5_1_, assignment0_.tenantRef_relation as tenantR35_5_1_, assignment0_.tenantRef_targetOid as tenantR36_5_1_, assignment0_.tenantRef_type as tenantR37_5_1_, rassignmen1_.owner_id as owner_id1_12_2_, rassignmen1_.owner_owner_oid as owner_ow2_12_2_ from m_assignment assignment0_ left outer join m_assignment_extension rassignmen1_ on assignment0_.extId=rassignmen1_.owner_id and assignment0_.extOid=rassignmen1_.owner_owner_oid where assignment0_.owner_oid=?
 [1] select strings0_.anyContainer_owner_id as anyConta2_11_0_, strings0_.anyContainer_owner_owner_oid as anyConta3_11_0_, strings0_.item_id as item_id1_11_0_, strings0_.stringValue as stringVa4_11_0_, strings0_.item_id as item_id1_11_1_, strings0_.anyContainer_owner_id as anyConta2_11_1_, strings0_.anyContainer_owner_owner_oid as anyConta3_11_1_, strings0_.stringValue as stringVa4_11_1_ from m_assignment_ext_string strings0_ where strings0_.anyContainer_owner_id=? and strings0_.anyContainer_owner_owner_oid=?
 [2] insert into m_assignment_ext_string (item_id, anyContainer_owner_id, anyContainer_owner_owner_oid, stringValue) values (?, ?, ?, ?)
 [1] update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
         */
        // TODO eliminate "all strings" selection (why is it there at all?)
        assertCounts(6 + getExtraSafeInsertionSelects(2), 7 + getExtraSafeInsertionSelects(2));
    }

    @Test
    public void test125AddAssignmentExtensionValueDuplicate() throws Exception {
        OperationResult result = new OperationResult(ExtensionTest.class.getName() + ".test125AddAssignmentExtensionValueDuplicate");

        ObjectDelta<UserType> delta = getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT, 1, AssignmentType.F_EXTENSION, EXT_WEAPON)
                .add("w4")
                .asObjectDelta("");
        delta.applyTo(expectedUser);

        queryListener.start();
        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), getOptions(), result);
        queryListener.dumpAndStop();

        try (EntityManager em = factory.createEntityManager()) {
            RUser u = em.find(RUser.class, userOid);
            assertExtension(u, itemHidden1, "h1.1", "h1.2");
            assertExtension(u, itemHidden2);
            assertExtension(u, itemHidden3);
            assertExtension(u, itemVisible, "v1", "v2", "v3");
            assertExtension(u, itemWeapon);
            RAssignment a = u.getAssignments().iterator().next();
            assertExtension(a, itemWeapon, "w1", "w2", "w3", "w4", "w5");
            assertExtension(a, itemShipName, "none");
        }

        assertGetObject(result);

        /*
 [1] select oid from m_object where oid = ? for update
 [1] select ruser0_.oid as oid1_27_, ruser0_2_.createChannel as createCh2_27_, ruser0_2_.createTimestamp as createTi3_27_, ruser0_2_.creatorRef_relation as creatorR4_27_, ruser0_2_.creatorRef_targetOid as creatorR5_27_, ruser0_2_.creatorRef_type as creatorR6_27_, ruser0_2_.fullObject as fullObje7_27_, ruser0_2_.lifecycleState as lifecycl8_27_, ruser0_2_.modifierRef_relation as modifier9_27_, ruser0_2_.modifierRef_targetOid as modifie10_27_, ruser0_2_.modifierRef_type as modifie11_27_, ruser0_2_.modifyChannel as modifyC12_27_, ruser0_2_.modifyTimestamp as modifyT13_27_, ruser0_2_.name_norm as name_no14_27_, ruser0_2_.name_orig as name_or15_27_, ruser0_2_.objectTypeClass as objectT16_27_, ruser0_2_.tenantRef_relation as tenantR17_27_, ruser0_2_.tenantRef_targetOid as tenantR18_27_, ruser0_2_.tenantRef_type as tenantR19_27_, ruser0_2_.version as version20_27_, ruser0_1_.administrativeStatus as administ1_53_, ruser0_1_.archiveTimestamp as archiveT2_53_, ruser0_1_.disableReason as disableR3_53_, ruser0_1_.disableTimestamp as disableT4_53_, ruser0_1_.effectiveStatus as effectiv5_53_, ruser0_1_.enableTimestamp as enableTi6_53_, ruser0_1_.validFrom as validFro7_53_, ruser0_1_.validTo as validTo8_53_, ruser0_1_.validityChangeTimestamp as validity9_53_, ruser0_1_.validityStatus as validit10_53_, ruser0_1_.costCenter as costCen11_53_, ruser0_1_.emailAddress as emailAd12_53_, ruser0_1_.hasPhoto as hasPhot13_53_, ruser0_1_.locale as locale14_53_, ruser0_1_.locality_norm as localit15_53_, ruser0_1_.locality_orig as localit16_53_, ruser0_1_.preferredLanguage as preferr17_53_, ruser0_1_.telephoneNumber as telepho18_53_, ruser0_1_.timezone as timezon19_53_, ruser0_.additionalName_norm as addition1_73_, ruser0_.additionalName_orig as addition2_73_, ruser0_.employeeNumber as employee3_73_, ruser0_.familyName_norm as familyNa4_73_, ruser0_.familyName_orig as familyNa5_73_, ruser0_.fullName_norm as fullName6_73_, ruser0_.fullName_orig as fullName7_73_, ruser0_.givenName_norm as givenNam8_73_, ruser0_.givenName_orig as givenNam9_73_, ruser0_.honorificPrefix_norm as honorif10_73_, ruser0_.honorificPrefix_orig as honorif11_73_, ruser0_.honorificSuffix_norm as honorif12_73_, ruser0_.honorificSuffix_orig as honorif13_73_, ruser0_.name_norm as name_no14_73_, ruser0_.name_orig as name_or15_73_, ruser0_.nickName_norm as nickNam16_73_, ruser0_.nickName_orig as nickNam17_73_, ruser0_.title_norm as title_n18_73_, ruser0_.title_orig as title_o19_73_ from m_user ruser0_ inner join m_focus ruser0_1_ on ruser0_.oid=ruser0_1_.oid inner join m_object ruser0_2_ on ruser0_.oid=ruser0_2_.oid where ruser0_.oid=?
 [1] update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
         */
        assertCounts(3, 3);
    }

    @Test
    public void test126ReplaceAssignmentExtensionValues() throws Exception {
        OperationResult result = new OperationResult("test126ReplaceAssignmentExtensionValues");

        ObjectDelta<UserType> delta = getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT, 1, AssignmentType.F_EXTENSION, EXT_WEAPON)
                .replace("w2", "w9")
                .asObjectDelta("");
        delta.applyTo(expectedUser);

        queryListener.start();
        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), getOptions(), result);
        queryListener.dumpAndStop();

        try (EntityManager em = factory.createEntityManager()) {
            RUser u = em.find(RUser.class, userOid);
            assertExtension(u, itemHidden1, "h1.1", "h1.2");
            assertExtension(u, itemHidden2);
            assertExtension(u, itemHidden3);
            assertExtension(u, itemVisible, "v1", "v2", "v3");
            assertExtension(u, itemWeapon);
            RAssignment a = u.getAssignments().iterator().next();
            assertExtension(a, itemWeapon, "w2", "w9");
            assertExtension(a, itemShipName, "none");
        }

        assertGetObject(result);

        /*
 [1] select oid from m_object where oid = ? for update
 [1] select ruser0_.oid as oid1_27_, ruser0_2_.createChannel as createCh2_27_, ruser0_2_.createTimestamp as createTi3_27_, ruser0_2_.creatorRef_relation as creatorR4_27_, ruser0_2_.creatorRef_targetOid as creatorR5_27_, ruser0_2_.creatorRef_type as creatorR6_27_, ruser0_2_.fullObject as fullObje7_27_, ruser0_2_.lifecycleState as lifecycl8_27_, ruser0_2_.modifierRef_relation as modifier9_27_, ruser0_2_.modifierRef_targetOid as modifie10_27_, ruser0_2_.modifierRef_type as modifie11_27_, ruser0_2_.modifyChannel as modifyC12_27_, ruser0_2_.modifyTimestamp as modifyT13_27_, ruser0_2_.name_norm as name_no14_27_, ruser0_2_.name_orig as name_or15_27_, ruser0_2_.objectTypeClass as objectT16_27_, ruser0_2_.tenantRef_relation as tenantR17_27_, ruser0_2_.tenantRef_targetOid as tenantR18_27_, ruser0_2_.tenantRef_type as tenantR19_27_, ruser0_2_.version as version20_27_, ruser0_1_.administrativeStatus as administ1_53_, ruser0_1_.archiveTimestamp as archiveT2_53_, ruser0_1_.disableReason as disableR3_53_, ruser0_1_.disableTimestamp as disableT4_53_, ruser0_1_.effectiveStatus as effectiv5_53_, ruser0_1_.enableTimestamp as enableTi6_53_, ruser0_1_.validFrom as validFro7_53_, ruser0_1_.validTo as validTo8_53_, ruser0_1_.validityChangeTimestamp as validity9_53_, ruser0_1_.validityStatus as validit10_53_, ruser0_1_.costCenter as costCen11_53_, ruser0_1_.emailAddress as emailAd12_53_, ruser0_1_.hasPhoto as hasPhot13_53_, ruser0_1_.locale as locale14_53_, ruser0_1_.locality_norm as localit15_53_, ruser0_1_.locality_orig as localit16_53_, ruser0_1_.preferredLanguage as preferr17_53_, ruser0_1_.telephoneNumber as telepho18_53_, ruser0_1_.timezone as timezon19_53_, ruser0_.additionalName_norm as addition1_73_, ruser0_.additionalName_orig as addition2_73_, ruser0_.employeeNumber as employee3_73_, ruser0_.familyName_norm as familyNa4_73_, ruser0_.familyName_orig as familyNa5_73_, ruser0_.fullName_norm as fullName6_73_, ruser0_.fullName_orig as fullName7_73_, ruser0_.givenName_norm as givenNam8_73_, ruser0_.givenName_orig as givenNam9_73_, ruser0_.honorificPrefix_norm as honorif10_73_, ruser0_.honorificPrefix_orig as honorif11_73_, ruser0_.honorificSuffix_norm as honorif12_73_, ruser0_.honorificSuffix_orig as honorif13_73_, ruser0_.name_norm as name_no14_73_, ruser0_.name_orig as name_or15_73_, ruser0_.nickName_norm as nickNam16_73_, ruser0_.nickName_orig as nickNam17_73_, ruser0_.title_norm as title_n18_73_, ruser0_.title_orig as title_o19_73_ from m_user ruser0_ inner join m_focus ruser0_1_ on ruser0_.oid=ruser0_1_.oid inner join m_object ruser0_2_ on ruser0_.oid=ruser0_2_.oid where ruser0_.oid=?
 [1] select assignment0_.owner_oid as owner_oi2_5_0_, assignment0_.id as id1_5_0_, assignment0_.id as id1_5_1_, assignment0_.owner_oid as owner_oi2_5_1_, assignment0_.administrativeStatus as administ3_5_1_, assignment0_.archiveTimestamp as archiveT4_5_1_, assignment0_.disableReason as disableR5_5_1_, assignment0_.disableTimestamp as disableT6_5_1_, assignment0_.effectiveStatus as effectiv7_5_1_, assignment0_.enableTimestamp as enableTi8_5_1_, assignment0_.validFrom as validFro9_5_1_, assignment0_.validTo as validTo10_5_1_, assignment0_.validityChangeTimestamp as validit11_5_1_, assignment0_.validityStatus as validit12_5_1_, assignment0_.assignmentOwner as assignm13_5_1_, assignment0_.createChannel as createC14_5_1_, assignment0_.createTimestamp as createT15_5_1_, assignment0_.creatorRef_relation as creator16_5_1_, assignment0_.creatorRef_targetOid as creator17_5_1_, assignment0_.creatorRef_type as creator18_5_1_, assignment0_.extId as extId38_5_1_, assignment0_.extOid as extOid39_5_1_, assignment0_.lifecycleState as lifecyc19_5_1_, assignment0_.modifierRef_relation as modifie20_5_1_, assignment0_.modifierRef_targetOid as modifie21_5_1_, assignment0_.modifierRef_type as modifie22_5_1_, assignment0_.modifyChannel as modifyC23_5_1_, assignment0_.modifyTimestamp as modifyT24_5_1_, assignment0_.orderValue as orderVa25_5_1_, assignment0_.orgRef_relation as orgRef_26_5_1_, assignment0_.orgRef_targetOid as orgRef_27_5_1_, assignment0_.orgRef_type as orgRef_28_5_1_, assignment0_.resourceRef_relation as resourc29_5_1_, assignment0_.resourceRef_targetOid as resourc30_5_1_, assignment0_.resourceRef_type as resourc31_5_1_, assignment0_.targetRef_relation as targetR32_5_1_, assignment0_.targetRef_targetOid as targetR33_5_1_, assignment0_.targetRef_type as targetR34_5_1_, assignment0_.tenantRef_relation as tenantR35_5_1_, assignment0_.tenantRef_targetOid as tenantR36_5_1_, assignment0_.tenantRef_type as tenantR37_5_1_, rassignmen1_.owner_id as owner_id1_12_2_, rassignmen1_.owner_owner_oid as owner_ow2_12_2_ from m_assignment assignment0_ left outer join m_assignment_extension rassignmen1_ on assignment0_.extId=rassignmen1_.owner_id and assignment0_.extOid=rassignmen1_.owner_owner_oid where assignment0_.owner_oid=?
 [1] select strings0_.anyContainer_owner_id as anyConta2_11_0_, strings0_.anyContainer_owner_owner_oid as anyConta3_11_0_, strings0_.item_id as item_id1_11_0_, strings0_.stringValue as stringVa4_11_0_, strings0_.item_id as item_id1_11_1_, strings0_.anyContainer_owner_id as anyConta2_11_1_, strings0_.anyContainer_owner_owner_oid as anyConta3_11_1_, strings0_.stringValue as stringVa4_11_1_ from m_assignment_ext_string strings0_ where strings0_.anyContainer_owner_id=? and strings0_.anyContainer_owner_owner_oid=?
 [1] select raextstrin_.item_id, raextstrin_.anyContainer_owner_id, raextstrin_.anyContainer_owner_owner_oid, raextstrin_.stringValue from m_assignment_ext_string raextstrin_ where raextstrin_.item_id=? and raextstrin_.anyContainer_owner_id=? and raextstrin_.anyContainer_owner_owner_oid=? and raextstrin_.stringValue=?
 [1] insert into m_assignment_ext_string (item_id, anyContainer_owner_id, anyContainer_owner_owner_oid, stringValue) values (?, ?, ?, ?)
 [1] update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
 [4] delete from m_assignment_ext_string where item_id=? and anyContainer_owner_id=? and anyContainer_owner_owner_oid=? and stringValue=?
         */
        assertCounts(8, 11);
    }

    @Test
    public void test127DeleteAssignmentExtensionValues() throws Exception {
        OperationResult result = new OperationResult("test127DeleteAssignmentExtensionValues");

        ObjectDelta<UserType> delta = getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT, 1, AssignmentType.F_EXTENSION, EXT_WEAPON)
                .delete("w2")
                .asObjectDelta("");
        delta.applyTo(expectedUser);

        queryListener.start();
        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), getOptions(), result);
        queryListener.dumpAndStop();

        try (EntityManager em = factory.createEntityManager()) {
            RUser u = em.find(RUser.class, userOid);
            assertExtension(u, itemHidden1, "h1.1", "h1.2");
            assertExtension(u, itemHidden2);
            assertExtension(u, itemHidden3);
            assertExtension(u, itemVisible, "v1", "v2", "v3");
            assertExtension(u, itemWeapon);
            RAssignment a = u.getAssignments().iterator().next();
            assertExtension(a, itemWeapon, "w9");
            assertExtension(a, itemShipName, "none");
        }

        assertGetObject(result);

        /*
 [1] select oid from m_object where oid = ? for update
 [1] select ruser0_.oid as oid1_27_, ruser0_2_.createChannel as createCh2_27_, ruser0_2_.createTimestamp as createTi3_27_, ruser0_2_.creatorRef_relation as creatorR4_27_, ruser0_2_.creatorRef_targetOid as creatorR5_27_, ruser0_2_.creatorRef_type as creatorR6_27_, ruser0_2_.fullObject as fullObje7_27_, ruser0_2_.lifecycleState as lifecycl8_27_, ruser0_2_.modifierRef_relation as modifier9_27_, ruser0_2_.modifierRef_targetOid as modifie10_27_, ruser0_2_.modifierRef_type as modifie11_27_, ruser0_2_.modifyChannel as modifyC12_27_, ruser0_2_.modifyTimestamp as modifyT13_27_, ruser0_2_.name_norm as name_no14_27_, ruser0_2_.name_orig as name_or15_27_, ruser0_2_.objectTypeClass as objectT16_27_, ruser0_2_.tenantRef_relation as tenantR17_27_, ruser0_2_.tenantRef_targetOid as tenantR18_27_, ruser0_2_.tenantRef_type as tenantR19_27_, ruser0_2_.version as version20_27_, ruser0_1_.administrativeStatus as administ1_53_, ruser0_1_.archiveTimestamp as archiveT2_53_, ruser0_1_.disableReason as disableR3_53_, ruser0_1_.disableTimestamp as disableT4_53_, ruser0_1_.effectiveStatus as effectiv5_53_, ruser0_1_.enableTimestamp as enableTi6_53_, ruser0_1_.validFrom as validFro7_53_, ruser0_1_.validTo as validTo8_53_, ruser0_1_.validityChangeTimestamp as validity9_53_, ruser0_1_.validityStatus as validit10_53_, ruser0_1_.costCenter as costCen11_53_, ruser0_1_.emailAddress as emailAd12_53_, ruser0_1_.hasPhoto as hasPhot13_53_, ruser0_1_.locale as locale14_53_, ruser0_1_.locality_norm as localit15_53_, ruser0_1_.locality_orig as localit16_53_, ruser0_1_.preferredLanguage as preferr17_53_, ruser0_1_.telephoneNumber as telepho18_53_, ruser0_1_.timezone as timezon19_53_, ruser0_.additionalName_norm as addition1_73_, ruser0_.additionalName_orig as addition2_73_, ruser0_.employeeNumber as employee3_73_, ruser0_.familyName_norm as familyNa4_73_, ruser0_.familyName_orig as familyNa5_73_, ruser0_.fullName_norm as fullName6_73_, ruser0_.fullName_orig as fullName7_73_, ruser0_.givenName_norm as givenNam8_73_, ruser0_.givenName_orig as givenNam9_73_, ruser0_.honorificPrefix_norm as honorif10_73_, ruser0_.honorificPrefix_orig as honorif11_73_, ruser0_.honorificSuffix_norm as honorif12_73_, ruser0_.honorificSuffix_orig as honorif13_73_, ruser0_.name_norm as name_no14_73_, ruser0_.name_orig as name_or15_73_, ruser0_.nickName_norm as nickNam16_73_, ruser0_.nickName_orig as nickNam17_73_, ruser0_.title_norm as title_n18_73_, ruser0_.title_orig as title_o19_73_ from m_user ruser0_ inner join m_focus ruser0_1_ on ruser0_.oid=ruser0_1_.oid inner join m_object ruser0_2_ on ruser0_.oid=ruser0_2_.oid where ruser0_.oid=?
 [1] select assignment0_.owner_oid as owner_oi2_5_0_, assignment0_.id as id1_5_0_, assignment0_.id as id1_5_1_, assignment0_.owner_oid as owner_oi2_5_1_, assignment0_.administrativeStatus as administ3_5_1_, assignment0_.archiveTimestamp as archiveT4_5_1_, assignment0_.disableReason as disableR5_5_1_, assignment0_.disableTimestamp as disableT6_5_1_, assignment0_.effectiveStatus as effectiv7_5_1_, assignment0_.enableTimestamp as enableTi8_5_1_, assignment0_.validFrom as validFro9_5_1_, assignment0_.validTo as validTo10_5_1_, assignment0_.validityChangeTimestamp as validit11_5_1_, assignment0_.validityStatus as validit12_5_1_, assignment0_.assignmentOwner as assignm13_5_1_, assignment0_.createChannel as createC14_5_1_, assignment0_.createTimestamp as createT15_5_1_, assignment0_.creatorRef_relation as creator16_5_1_, assignment0_.creatorRef_targetOid as creator17_5_1_, assignment0_.creatorRef_type as creator18_5_1_, assignment0_.extId as extId38_5_1_, assignment0_.extOid as extOid39_5_1_, assignment0_.lifecycleState as lifecyc19_5_1_, assignment0_.modifierRef_relation as modifie20_5_1_, assignment0_.modifierRef_targetOid as modifie21_5_1_, assignment0_.modifierRef_type as modifie22_5_1_, assignment0_.modifyChannel as modifyC23_5_1_, assignment0_.modifyTimestamp as modifyT24_5_1_, assignment0_.orderValue as orderVa25_5_1_, assignment0_.orgRef_relation as orgRef_26_5_1_, assignment0_.orgRef_targetOid as orgRef_27_5_1_, assignment0_.orgRef_type as orgRef_28_5_1_, assignment0_.resourceRef_relation as resourc29_5_1_, assignment0_.resourceRef_targetOid as resourc30_5_1_, assignment0_.resourceRef_type as resourc31_5_1_, assignment0_.targetRef_relation as targetR32_5_1_, assignment0_.targetRef_targetOid as targetR33_5_1_, assignment0_.targetRef_type as targetR34_5_1_, assignment0_.tenantRef_relation as tenantR35_5_1_, assignment0_.tenantRef_targetOid as tenantR36_5_1_, assignment0_.tenantRef_type as tenantR37_5_1_, rassignmen1_.owner_id as owner_id1_12_2_, rassignmen1_.owner_owner_oid as owner_ow2_12_2_ from m_assignment assignment0_ left outer join m_assignment_extension rassignmen1_ on assignment0_.extId=rassignmen1_.owner_id and assignment0_.extOid=rassignmen1_.owner_owner_oid where assignment0_.owner_oid=?
 [1] select strings0_.anyContainer_owner_id as anyConta2_11_0_, strings0_.anyContainer_owner_owner_oid as anyConta3_11_0_, strings0_.item_id as item_id1_11_0_, strings0_.stringValue as stringVa4_11_0_, strings0_.item_id as item_id1_11_1_, strings0_.anyContainer_owner_id as anyConta2_11_1_, strings0_.anyContainer_owner_owner_oid as anyConta3_11_1_, strings0_.stringValue as stringVa4_11_1_ from m_assignment_ext_string strings0_ where strings0_.anyContainer_owner_id=? and strings0_.anyContainer_owner_owner_oid=?
 [1] update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
 [1] delete from m_assignment_ext_string where item_id=? and anyContainer_owner_id=? and anyContainer_owner_owner_oid=? and stringValue=?
         */
        assertCounts(6, 6);
    }

    @Test
    public void test129ReplaceNonIndexedExtensionProperty() throws Exception {
        OperationResult result = new OperationResult("test129ReplaceNonIndexedExtensionProperty");

        ObjectDelta<UserType> delta = getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT, 1, AssignmentType.F_EXTENSION, EXT_LOOT)
                .replace(34)
                .asObjectDelta("");
        delta.applyTo(expectedUser);

        queryListener.start();
        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), getOptions(), result);
        queryListener.dumpAndStop();

        RExtItem extItemDef = extItemDictionary.findItemByDefinition(delta.getModifications().iterator().next().getDefinition());
        assertNull("ext item definition for loot exists", extItemDef);

        assertGetObject(result);

        /*
 [1] select oid from m_object where oid = ? for update
 [1] select ruser0_.oid as oid1_27_, ruser0_2_.createChannel as createCh2_27_, ruser0_2_.createTimestamp as createTi3_27_, ruser0_2_.creatorRef_relation as creatorR4_27_, ruser0_2_.creatorRef_targetOid as creatorR5_27_, ruser0_2_.creatorRef_type as creatorR6_27_, ruser0_2_.fullObject as fullObje7_27_, ruser0_2_.lifecycleState as lifecycl8_27_, ruser0_2_.modifierRef_relation as modifier9_27_, ruser0_2_.modifierRef_targetOid as modifie10_27_, ruser0_2_.modifierRef_type as modifie11_27_, ruser0_2_.modifyChannel as modifyC12_27_, ruser0_2_.modifyTimestamp as modifyT13_27_, ruser0_2_.name_norm as name_no14_27_, ruser0_2_.name_orig as name_or15_27_, ruser0_2_.objectTypeClass as objectT16_27_, ruser0_2_.tenantRef_relation as tenantR17_27_, ruser0_2_.tenantRef_targetOid as tenantR18_27_, ruser0_2_.tenantRef_type as tenantR19_27_, ruser0_2_.version as version20_27_, ruser0_1_.administrativeStatus as administ1_53_, ruser0_1_.archiveTimestamp as archiveT2_53_, ruser0_1_.disableReason as disableR3_53_, ruser0_1_.disableTimestamp as disableT4_53_, ruser0_1_.effectiveStatus as effectiv5_53_, ruser0_1_.enableTimestamp as enableTi6_53_, ruser0_1_.validFrom as validFro7_53_, ruser0_1_.validTo as validTo8_53_, ruser0_1_.validityChangeTimestamp as validity9_53_, ruser0_1_.validityStatus as validit10_53_, ruser0_1_.costCenter as costCen11_53_, ruser0_1_.emailAddress as emailAd12_53_, ruser0_1_.hasPhoto as hasPhot13_53_, ruser0_1_.locale as locale14_53_, ruser0_1_.locality_norm as localit15_53_, ruser0_1_.locality_orig as localit16_53_, ruser0_1_.preferredLanguage as preferr17_53_, ruser0_1_.telephoneNumber as telepho18_53_, ruser0_1_.timezone as timezon19_53_, ruser0_.additionalName_norm as addition1_73_, ruser0_.additionalName_orig as addition2_73_, ruser0_.employeeNumber as employee3_73_, ruser0_.familyName_norm as familyNa4_73_, ruser0_.familyName_orig as familyNa5_73_, ruser0_.fullName_norm as fullName6_73_, ruser0_.fullName_orig as fullName7_73_, ruser0_.givenName_norm as givenNam8_73_, ruser0_.givenName_orig as givenNam9_73_, ruser0_.honorificPrefix_norm as honorif10_73_, ruser0_.honorificPrefix_orig as honorif11_73_, ruser0_.honorificSuffix_norm as honorif12_73_, ruser0_.honorificSuffix_orig as honorif13_73_, ruser0_.name_norm as name_no14_73_, ruser0_.name_orig as name_or15_73_, ruser0_.nickName_norm as nickNam16_73_, ruser0_.nickName_orig as nickNam17_73_, ruser0_.title_norm as title_n18_73_, ruser0_.title_orig as title_o19_73_ from m_user ruser0_ inner join m_focus ruser0_1_ on ruser0_.oid=ruser0_1_.oid inner join m_object ruser0_2_ on ruser0_.oid=ruser0_2_.oid where ruser0_.oid=?
 [1] select assignment0_.owner_oid as owner_oi2_5_0_, assignment0_.id as id1_5_0_, assignment0_.id as id1_5_1_, assignment0_.owner_oid as owner_oi2_5_1_, assignment0_.administrativeStatus as administ3_5_1_, assignment0_.archiveTimestamp as archiveT4_5_1_, assignment0_.disableReason as disableR5_5_1_, assignment0_.disableTimestamp as disableT6_5_1_, assignment0_.effectiveStatus as effectiv7_5_1_, assignment0_.enableTimestamp as enableTi8_5_1_, assignment0_.validFrom as validFro9_5_1_, assignment0_.validTo as validTo10_5_1_, assignment0_.validityChangeTimestamp as validit11_5_1_, assignment0_.validityStatus as validit12_5_1_, assignment0_.assignmentOwner as assignm13_5_1_, assignment0_.createChannel as createC14_5_1_, assignment0_.createTimestamp as createT15_5_1_, assignment0_.creatorRef_relation as creator16_5_1_, assignment0_.creatorRef_targetOid as creator17_5_1_, assignment0_.creatorRef_type as creator18_5_1_, assignment0_.extId as extId38_5_1_, assignment0_.extOid as extOid39_5_1_, assignment0_.lifecycleState as lifecyc19_5_1_, assignment0_.modifierRef_relation as modifie20_5_1_, assignment0_.modifierRef_targetOid as modifie21_5_1_, assignment0_.modifierRef_type as modifie22_5_1_, assignment0_.modifyChannel as modifyC23_5_1_, assignment0_.modifyTimestamp as modifyT24_5_1_, assignment0_.orderValue as orderVa25_5_1_, assignment0_.orgRef_relation as orgRef_26_5_1_, assignment0_.orgRef_targetOid as orgRef_27_5_1_, assignment0_.orgRef_type as orgRef_28_5_1_, assignment0_.resourceRef_relation as resourc29_5_1_, assignment0_.resourceRef_targetOid as resourc30_5_1_, assignment0_.resourceRef_type as resourc31_5_1_, assignment0_.targetRef_relation as targetR32_5_1_, assignment0_.targetRef_targetOid as targetR33_5_1_, assignment0_.targetRef_type as targetR34_5_1_, assignment0_.tenantRef_relation as tenantR35_5_1_, assignment0_.tenantRef_targetOid as tenantR36_5_1_, assignment0_.tenantRef_type as tenantR37_5_1_, rassignmen1_.owner_id as owner_id1_12_2_, rassignmen1_.owner_owner_oid as owner_ow2_12_2_ from m_assignment assignment0_ left outer join m_assignment_extension rassignmen1_ on assignment0_.extId=rassignmen1_.owner_id and assignment0_.extOid=rassignmen1_.owner_owner_oid where assignment0_.owner_oid=?
 [1] update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
         */
        assertCounts(4, 4);
    }

    // No tests for hidden (index-only) assignment extension values yet.

    @Test
    public void test145ReplaceAssignmentValuesToNull() throws Exception {
        OperationResult result = new OperationResult(ExtensionTest.class.getName() + ".test145ReplaceAssignmentValuesToNull");

        ObjectDelta<UserType> delta = getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT, 1, AssignmentType.F_EXTENSION, EXT_WEAPON)
                .replace()
                .asObjectDelta("");
        delta.applyTo(expectedUser);

        queryListener.start();
        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), getOptions(), result);
        queryListener.dumpAndStop();

        try (EntityManager em = factory.createEntityManager()) {
            RUser u = em.find(RUser.class, userOid);
            assertExtension(u, itemHidden1, "h1.1", "h1.2");
            assertExtension(u, itemHidden2);
            assertExtension(u, itemHidden3);
            assertExtension(u, itemVisible, "v1", "v2", "v3");
            assertExtension(u, itemWeapon);
            RAssignment a = u.getAssignments().iterator().next();
            assertExtension(a, itemWeapon);
            assertExtension(a, itemShipName, "none");
        }

        assertGetObject(result);

        /*
 [1] select oid from m_object where oid = ? for update
 [1] select ruser0_.oid as oid1_27_, ruser0_2_.createChannel as createCh2_27_, ruser0_2_.createTimestamp as createTi3_27_, ruser0_2_.creatorRef_relation as creatorR4_27_, ruser0_2_.creatorRef_targetOid as creatorR5_27_, ruser0_2_.creatorRef_type as creatorR6_27_, ruser0_2_.fullObject as fullObje7_27_, ruser0_2_.lifecycleState as lifecycl8_27_, ruser0_2_.modifierRef_relation as modifier9_27_, ruser0_2_.modifierRef_targetOid as modifie10_27_, ruser0_2_.modifierRef_type as modifie11_27_, ruser0_2_.modifyChannel as modifyC12_27_, ruser0_2_.modifyTimestamp as modifyT13_27_, ruser0_2_.name_norm as name_no14_27_, ruser0_2_.name_orig as name_or15_27_, ruser0_2_.objectTypeClass as objectT16_27_, ruser0_2_.tenantRef_relation as tenantR17_27_, ruser0_2_.tenantRef_targetOid as tenantR18_27_, ruser0_2_.tenantRef_type as tenantR19_27_, ruser0_2_.version as version20_27_, ruser0_1_.administrativeStatus as administ1_53_, ruser0_1_.archiveTimestamp as archiveT2_53_, ruser0_1_.disableReason as disableR3_53_, ruser0_1_.disableTimestamp as disableT4_53_, ruser0_1_.effectiveStatus as effectiv5_53_, ruser0_1_.enableTimestamp as enableTi6_53_, ruser0_1_.validFrom as validFro7_53_, ruser0_1_.validTo as validTo8_53_, ruser0_1_.validityChangeTimestamp as validity9_53_, ruser0_1_.validityStatus as validit10_53_, ruser0_1_.costCenter as costCen11_53_, ruser0_1_.emailAddress as emailAd12_53_, ruser0_1_.hasPhoto as hasPhot13_53_, ruser0_1_.locale as locale14_53_, ruser0_1_.locality_norm as localit15_53_, ruser0_1_.locality_orig as localit16_53_, ruser0_1_.preferredLanguage as preferr17_53_, ruser0_1_.telephoneNumber as telepho18_53_, ruser0_1_.timezone as timezon19_53_, ruser0_.additionalName_norm as addition1_73_, ruser0_.additionalName_orig as addition2_73_, ruser0_.employeeNumber as employee3_73_, ruser0_.familyName_norm as familyNa4_73_, ruser0_.familyName_orig as familyNa5_73_, ruser0_.fullName_norm as fullName6_73_, ruser0_.fullName_orig as fullName7_73_, ruser0_.givenName_norm as givenNam8_73_, ruser0_.givenName_orig as givenNam9_73_, ruser0_.honorificPrefix_norm as honorif10_73_, ruser0_.honorificPrefix_orig as honorif11_73_, ruser0_.honorificSuffix_norm as honorif12_73_, ruser0_.honorificSuffix_orig as honorif13_73_, ruser0_.name_norm as name_no14_73_, ruser0_.name_orig as name_or15_73_, ruser0_.nickName_norm as nickNam16_73_, ruser0_.nickName_orig as nickNam17_73_, ruser0_.title_norm as title_n18_73_, ruser0_.title_orig as title_o19_73_ from m_user ruser0_ inner join m_focus ruser0_1_ on ruser0_.oid=ruser0_1_.oid inner join m_object ruser0_2_ on ruser0_.oid=ruser0_2_.oid where ruser0_.oid=?
 [1] select assignment0_.owner_oid as owner_oi2_5_0_, assignment0_.id as id1_5_0_, assignment0_.id as id1_5_1_, assignment0_.owner_oid as owner_oi2_5_1_, assignment0_.administrativeStatus as administ3_5_1_, assignment0_.archiveTimestamp as archiveT4_5_1_, assignment0_.disableReason as disableR5_5_1_, assignment0_.disableTimestamp as disableT6_5_1_, assignment0_.effectiveStatus as effectiv7_5_1_, assignment0_.enableTimestamp as enableTi8_5_1_, assignment0_.validFrom as validFro9_5_1_, assignment0_.validTo as validTo10_5_1_, assignment0_.validityChangeTimestamp as validit11_5_1_, assignment0_.validityStatus as validit12_5_1_, assignment0_.assignmentOwner as assignm13_5_1_, assignment0_.createChannel as createC14_5_1_, assignment0_.createTimestamp as createT15_5_1_, assignment0_.creatorRef_relation as creator16_5_1_, assignment0_.creatorRef_targetOid as creator17_5_1_, assignment0_.creatorRef_type as creator18_5_1_, assignment0_.extId as extId38_5_1_, assignment0_.extOid as extOid39_5_1_, assignment0_.lifecycleState as lifecyc19_5_1_, assignment0_.modifierRef_relation as modifie20_5_1_, assignment0_.modifierRef_targetOid as modifie21_5_1_, assignment0_.modifierRef_type as modifie22_5_1_, assignment0_.modifyChannel as modifyC23_5_1_, assignment0_.modifyTimestamp as modifyT24_5_1_, assignment0_.orderValue as orderVa25_5_1_, assignment0_.orgRef_relation as orgRef_26_5_1_, assignment0_.orgRef_targetOid as orgRef_27_5_1_, assignment0_.orgRef_type as orgRef_28_5_1_, assignment0_.resourceRef_relation as resourc29_5_1_, assignment0_.resourceRef_targetOid as resourc30_5_1_, assignment0_.resourceRef_type as resourc31_5_1_, assignment0_.targetRef_relation as targetR32_5_1_, assignment0_.targetRef_targetOid as targetR33_5_1_, assignment0_.targetRef_type as targetR34_5_1_, assignment0_.tenantRef_relation as tenantR35_5_1_, assignment0_.tenantRef_targetOid as tenantR36_5_1_, assignment0_.tenantRef_type as tenantR37_5_1_, rassignmen1_.owner_id as owner_id1_12_2_, rassignmen1_.owner_owner_oid as owner_ow2_12_2_ from m_assignment assignment0_ left outer join m_assignment_extension rassignmen1_ on assignment0_.extId=rassignmen1_.owner_id and assignment0_.extOid=rassignmen1_.owner_owner_oid where assignment0_.owner_oid=?
 [1] select strings0_.anyContainer_owner_id as anyConta2_11_0_, strings0_.anyContainer_owner_owner_oid as anyConta3_11_0_, strings0_.item_id as item_id1_11_0_, strings0_.stringValue as stringVa4_11_0_, strings0_.item_id as item_id1_11_1_, strings0_.anyContainer_owner_id as anyConta2_11_1_, strings0_.anyContainer_owner_owner_oid as anyConta3_11_1_, strings0_.stringValue as stringVa4_11_1_ from m_assignment_ext_string strings0_ where strings0_.anyContainer_owner_id=? and strings0_.anyContainer_owner_owner_oid=?
 [1] update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
 [1] delete from m_assignment_ext_string where item_id=? and anyContainer_owner_id=? and anyContainer_owner_owner_oid=? and stringValue=?
         */
        assertCounts(6, 6);
    }

    @Test
    public void test150DeleteWholeAssignmentExtension() throws Exception {
        OperationResult result = new OperationResult(ExtensionTest.class.getName() + ".test150DeleteWholeAssignmentExtension");

        PrismContainerValue<?> existingExtension =
                expectedUser.findContainer(ItemPath.create(UserType.F_ASSIGNMENT, 1, AssignmentType.F_EXTENSION)).getValue();

        ObjectDelta<UserType> delta = getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT, 1, AssignmentType.F_EXTENSION)
                .delete(existingExtension.clone())
                .asObjectDelta("");
        delta.applyTo(expectedUser);

        queryListener.start();
        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), getOptions(), result);
        queryListener.dumpAndStop();

        try (EntityManager em = factory.createEntityManager()) {
            RUser u = em.find(RUser.class, userOid);
            assertExtension(u, itemHidden1, "h1.1", "h1.2");
            assertExtension(u, itemHidden2);
            assertExtension(u, itemHidden3);
            assertExtension(u, itemVisible, "v1", "v2", "v3");
            assertExtension(u, itemWeapon);
            RAssignment a = u.getAssignments().iterator().next();
            // should here be RAssignmentExtension at all?
            assertExtension(a, itemWeapon);
            assertExtension(a, itemShipName);
        }

        assertGetObject(result);

        /*
 [1] select oid from m_object where oid = ? for update
 [1] select ruser0_.oid as oid1_27_, ruser0_2_.createChannel as createCh2_27_, ruser0_2_.createTimestamp as createTi3_27_, ruser0_2_.creatorRef_relation as creatorR4_27_, ruser0_2_.creatorRef_targetOid as creatorR5_27_, ruser0_2_.creatorRef_type as creatorR6_27_, ruser0_2_.fullObject as fullObje7_27_, ruser0_2_.lifecycleState as lifecycl8_27_, ruser0_2_.modifierRef_relation as modifier9_27_, ruser0_2_.modifierRef_targetOid as modifie10_27_, ruser0_2_.modifierRef_type as modifie11_27_, ruser0_2_.modifyChannel as modifyC12_27_, ruser0_2_.modifyTimestamp as modifyT13_27_, ruser0_2_.name_norm as name_no14_27_, ruser0_2_.name_orig as name_or15_27_, ruser0_2_.objectTypeClass as objectT16_27_, ruser0_2_.tenantRef_relation as tenantR17_27_, ruser0_2_.tenantRef_targetOid as tenantR18_27_, ruser0_2_.tenantRef_type as tenantR19_27_, ruser0_2_.version as version20_27_, ruser0_1_.administrativeStatus as administ1_53_, ruser0_1_.archiveTimestamp as archiveT2_53_, ruser0_1_.disableReason as disableR3_53_, ruser0_1_.disableTimestamp as disableT4_53_, ruser0_1_.effectiveStatus as effectiv5_53_, ruser0_1_.enableTimestamp as enableTi6_53_, ruser0_1_.validFrom as validFro7_53_, ruser0_1_.validTo as validTo8_53_, ruser0_1_.validityChangeTimestamp as validity9_53_, ruser0_1_.validityStatus as validit10_53_, ruser0_1_.costCenter as costCen11_53_, ruser0_1_.emailAddress as emailAd12_53_, ruser0_1_.hasPhoto as hasPhot13_53_, ruser0_1_.locale as locale14_53_, ruser0_1_.locality_norm as localit15_53_, ruser0_1_.locality_orig as localit16_53_, ruser0_1_.preferredLanguage as preferr17_53_, ruser0_1_.telephoneNumber as telepho18_53_, ruser0_1_.timezone as timezon19_53_, ruser0_.additionalName_norm as addition1_73_, ruser0_.additionalName_orig as addition2_73_, ruser0_.employeeNumber as employee3_73_, ruser0_.familyName_norm as familyNa4_73_, ruser0_.familyName_orig as familyNa5_73_, ruser0_.fullName_norm as fullName6_73_, ruser0_.fullName_orig as fullName7_73_, ruser0_.givenName_norm as givenNam8_73_, ruser0_.givenName_orig as givenNam9_73_, ruser0_.honorificPrefix_norm as honorif10_73_, ruser0_.honorificPrefix_orig as honorif11_73_, ruser0_.honorificSuffix_norm as honorif12_73_, ruser0_.honorificSuffix_orig as honorif13_73_, ruser0_.name_norm as name_no14_73_, ruser0_.name_orig as name_or15_73_, ruser0_.nickName_norm as nickNam16_73_, ruser0_.nickName_orig as nickNam17_73_, ruser0_.title_norm as title_n18_73_, ruser0_.title_orig as title_o19_73_ from m_user ruser0_ inner join m_focus ruser0_1_ on ruser0_.oid=ruser0_1_.oid inner join m_object ruser0_2_ on ruser0_.oid=ruser0_2_.oid where ruser0_.oid=?
 [1] select assignment0_.owner_oid as owner_oi2_5_0_, assignment0_.id as id1_5_0_, assignment0_.id as id1_5_1_, assignment0_.owner_oid as owner_oi2_5_1_, assignment0_.administrativeStatus as administ3_5_1_, assignment0_.archiveTimestamp as archiveT4_5_1_, assignment0_.disableReason as disableR5_5_1_, assignment0_.disableTimestamp as disableT6_5_1_, assignment0_.effectiveStatus as effectiv7_5_1_, assignment0_.enableTimestamp as enableTi8_5_1_, assignment0_.validFrom as validFro9_5_1_, assignment0_.validTo as validTo10_5_1_, assignment0_.validityChangeTimestamp as validit11_5_1_, assignment0_.validityStatus as validit12_5_1_, assignment0_.assignmentOwner as assignm13_5_1_, assignment0_.createChannel as createC14_5_1_, assignment0_.createTimestamp as createT15_5_1_, assignment0_.creatorRef_relation as creator16_5_1_, assignment0_.creatorRef_targetOid as creator17_5_1_, assignment0_.creatorRef_type as creator18_5_1_, assignment0_.extId as extId38_5_1_, assignment0_.extOid as extOid39_5_1_, assignment0_.lifecycleState as lifecyc19_5_1_, assignment0_.modifierRef_relation as modifie20_5_1_, assignment0_.modifierRef_targetOid as modifie21_5_1_, assignment0_.modifierRef_type as modifie22_5_1_, assignment0_.modifyChannel as modifyC23_5_1_, assignment0_.modifyTimestamp as modifyT24_5_1_, assignment0_.orderValue as orderVa25_5_1_, assignment0_.orgRef_relation as orgRef_26_5_1_, assignment0_.orgRef_targetOid as orgRef_27_5_1_, assignment0_.orgRef_type as orgRef_28_5_1_, assignment0_.resourceRef_relation as resourc29_5_1_, assignment0_.resourceRef_targetOid as resourc30_5_1_, assignment0_.resourceRef_type as resourc31_5_1_, assignment0_.targetRef_relation as targetR32_5_1_, assignment0_.targetRef_targetOid as targetR33_5_1_, assignment0_.targetRef_type as targetR34_5_1_, assignment0_.tenantRef_relation as tenantR35_5_1_, assignment0_.tenantRef_targetOid as tenantR36_5_1_, assignment0_.tenantRef_type as tenantR37_5_1_, rassignmen1_.owner_id as owner_id1_12_2_, rassignmen1_.owner_owner_oid as owner_ow2_12_2_ from m_assignment assignment0_ left outer join m_assignment_extension rassignmen1_ on assignment0_.extId=rassignmen1_.owner_id and assignment0_.extOid=rassignmen1_.owner_owner_oid where assignment0_.owner_oid=?
 [1] select booleans0_.anyContainer_owner_id as anyConta2_6_0_, booleans0_.anyContainer_owner_owner_oid as anyConta3_6_0_, booleans0_.item_id as item_id1_6_0_, booleans0_.booleanValue as booleanV4_6_0_, booleans0_.item_id as item_id1_6_1_, booleans0_.anyContainer_owner_id as anyConta2_6_1_, booleans0_.anyContainer_owner_owner_oid as anyConta3_6_1_, booleans0_.booleanValue as booleanV4_6_1_ from m_assignment_ext_boolean booleans0_ where booleans0_.anyContainer_owner_id=? and booleans0_.anyContainer_owner_owner_oid=?
 [1] select dates0_.anyContainer_owner_id as anyConta2_7_0_, dates0_.anyContainer_owner_owner_oid as anyConta3_7_0_, dates0_.item_id as item_id1_7_0_, dates0_.dateValue as dateValu4_7_0_, dates0_.item_id as item_id1_7_1_, dates0_.anyContainer_owner_id as anyConta2_7_1_, dates0_.anyContainer_owner_owner_oid as anyConta3_7_1_, dates0_.dateValue as dateValu4_7_1_ from m_assignment_ext_date dates0_ where dates0_.anyContainer_owner_id=? and dates0_.anyContainer_owner_owner_oid=?
 [1] select longs0_.anyContainer_owner_id as anyConta2_8_0_, longs0_.anyContainer_owner_owner_oid as anyConta3_8_0_, longs0_.item_id as item_id1_8_0_, longs0_.longValue as longValu4_8_0_, longs0_.item_id as item_id1_8_1_, longs0_.anyContainer_owner_id as anyConta2_8_1_, longs0_.anyContainer_owner_owner_oid as anyConta3_8_1_, longs0_.longValue as longValu4_8_1_ from m_assignment_ext_long longs0_ where longs0_.anyContainer_owner_id=? and longs0_.anyContainer_owner_owner_oid=?
 [1] select polys0_.anyContainer_owner_id as anyConta2_9_0_, polys0_.anyContainer_owner_owner_oid as anyConta3_9_0_, polys0_.item_id as item_id1_9_0_, polys0_.orig as orig4_9_0_, polys0_.item_id as item_id1_9_1_, polys0_.anyContainer_owner_id as anyConta2_9_1_, polys0_.anyContainer_owner_owner_oid as anyConta3_9_1_, polys0_.orig as orig4_9_1_, polys0_.norm as norm5_9_1_ from m_assignment_ext_poly polys0_ where polys0_.anyContainer_owner_id=? and polys0_.anyContainer_owner_owner_oid=?
 [1] select references0_.anyContainer_owner_id as anyConta2_10_0_, references0_.anyContainer_owner_owner_oid as anyConta3_10_0_, references0_.item_id as item_id1_10_0_, references0_.targetoid as targetoi4_10_0_, references0_.item_id as item_id1_10_1_, references0_.anyContainer_owner_id as anyConta2_10_1_, references0_.anyContainer_owner_owner_oid as anyConta3_10_1_, references0_.targetoid as targetoi4_10_1_, references0_.relation as relation5_10_1_, references0_.targetType as targetTy6_10_1_ from m_assignment_ext_reference references0_ where references0_.anyContainer_owner_id=? and references0_.anyContainer_owner_owner_oid=?
 [1] select strings0_.anyContainer_owner_id as anyConta2_11_0_, strings0_.anyContainer_owner_owner_oid as anyConta3_11_0_, strings0_.item_id as item_id1_11_0_, strings0_.stringValue as stringVa4_11_0_, strings0_.item_id as item_id1_11_1_, strings0_.anyContainer_owner_id as anyConta2_11_1_, strings0_.anyContainer_owner_owner_oid as anyConta3_11_1_, strings0_.stringValue as stringVa4_11_1_ from m_assignment_ext_string strings0_ where strings0_.anyContainer_owner_id=? and strings0_.anyContainer_owner_owner_oid=?
 [1] update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
 [1] delete from m_assignment_ext_string where item_id=? and anyContainer_owner_id=? and anyContainer_owner_owner_oid=? and stringValue=?
         */
        assertCounts(11, 11);
    }

    @Test
    public void test155AddWholeAssignmentExtension() throws Exception {
        OperationResult result = new OperationResult(ExtensionTest.class.getName() + ".test155AddWholeAssignmentExtension");

        PrismContainerValue<?> extValue = expectedUser.getDefinition()
                .findContainerDefinition(UserType.F_ASSIGNMENT)
                .findContainerDefinition(AssignmentType.F_EXTENSION)
                .instantiate().getValue();
        extValue.findOrCreateProperty(EXT_WEAPON).addRealValues("W1", "W2");

        ObjectDelta<UserType> delta = getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT, 1, AssignmentType.F_EXTENSION)
                .add(extValue.clone())
                .asObjectDelta("");
        delta.applyTo(expectedUser);

        queryListener.start();
        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), getOptions(), result);
        queryListener.dumpAndStop();

        try (EntityManager em = factory.createEntityManager()) {
            RUser u = em.find(RUser.class, userOid);
            assertExtension(u, itemHidden1, "h1.1", "h1.2");
            assertExtension(u, itemHidden2);
            assertExtension(u, itemHidden3);
            assertExtension(u, itemVisible, "v1", "v2", "v3");
            assertExtension(u, itemWeapon);
            RAssignment a = u.getAssignments().iterator().next();
            assertExtension(a, itemWeapon, "W1", "W2");
            assertExtension(a, itemShipName);
        }

        assertGetObject(result);

        /*
 [1] select oid from m_object where oid = ? for update
 [1] select ruser0_.oid as oid1_27_, ruser0_2_.createChannel as createCh2_27_, ruser0_2_.createTimestamp as createTi3_27_, ruser0_2_.creatorRef_relation as creatorR4_27_, ruser0_2_.creatorRef_targetOid as creatorR5_27_, ruser0_2_.creatorRef_type as creatorR6_27_, ruser0_2_.fullObject as fullObje7_27_, ruser0_2_.lifecycleState as lifecycl8_27_, ruser0_2_.modifierRef_relation as modifier9_27_, ruser0_2_.modifierRef_targetOid as modifie10_27_, ruser0_2_.modifierRef_type as modifie11_27_, ruser0_2_.modifyChannel as modifyC12_27_, ruser0_2_.modifyTimestamp as modifyT13_27_, ruser0_2_.name_norm as name_no14_27_, ruser0_2_.name_orig as name_or15_27_, ruser0_2_.objectTypeClass as objectT16_27_, ruser0_2_.tenantRef_relation as tenantR17_27_, ruser0_2_.tenantRef_targetOid as tenantR18_27_, ruser0_2_.tenantRef_type as tenantR19_27_, ruser0_2_.version as version20_27_, ruser0_1_.administrativeStatus as administ1_53_, ruser0_1_.archiveTimestamp as archiveT2_53_, ruser0_1_.disableReason as disableR3_53_, ruser0_1_.disableTimestamp as disableT4_53_, ruser0_1_.effectiveStatus as effectiv5_53_, ruser0_1_.enableTimestamp as enableTi6_53_, ruser0_1_.validFrom as validFro7_53_, ruser0_1_.validTo as validTo8_53_, ruser0_1_.validityChangeTimestamp as validity9_53_, ruser0_1_.validityStatus as validit10_53_, ruser0_1_.costCenter as costCen11_53_, ruser0_1_.emailAddress as emailAd12_53_, ruser0_1_.hasPhoto as hasPhot13_53_, ruser0_1_.locale as locale14_53_, ruser0_1_.locality_norm as localit15_53_, ruser0_1_.locality_orig as localit16_53_, ruser0_1_.preferredLanguage as preferr17_53_, ruser0_1_.telephoneNumber as telepho18_53_, ruser0_1_.timezone as timezon19_53_, ruser0_.additionalName_norm as addition1_73_, ruser0_.additionalName_orig as addition2_73_, ruser0_.employeeNumber as employee3_73_, ruser0_.familyName_norm as familyNa4_73_, ruser0_.familyName_orig as familyNa5_73_, ruser0_.fullName_norm as fullName6_73_, ruser0_.fullName_orig as fullName7_73_, ruser0_.givenName_norm as givenNam8_73_, ruser0_.givenName_orig as givenNam9_73_, ruser0_.honorificPrefix_norm as honorif10_73_, ruser0_.honorificPrefix_orig as honorif11_73_, ruser0_.honorificSuffix_norm as honorif12_73_, ruser0_.honorificSuffix_orig as honorif13_73_, ruser0_.name_norm as name_no14_73_, ruser0_.name_orig as name_or15_73_, ruser0_.nickName_norm as nickNam16_73_, ruser0_.nickName_orig as nickNam17_73_, ruser0_.title_norm as title_n18_73_, ruser0_.title_orig as title_o19_73_ from m_user ruser0_ inner join m_focus ruser0_1_ on ruser0_.oid=ruser0_1_.oid inner join m_object ruser0_2_ on ruser0_.oid=ruser0_2_.oid where ruser0_.oid=?
 [1] select assignment0_.owner_oid as owner_oi2_5_0_, assignment0_.id as id1_5_0_, assignment0_.id as id1_5_1_, assignment0_.owner_oid as owner_oi2_5_1_, assignment0_.administrativeStatus as administ3_5_1_, assignment0_.archiveTimestamp as archiveT4_5_1_, assignment0_.disableReason as disableR5_5_1_, assignment0_.disableTimestamp as disableT6_5_1_, assignment0_.effectiveStatus as effectiv7_5_1_, assignment0_.enableTimestamp as enableTi8_5_1_, assignment0_.validFrom as validFro9_5_1_, assignment0_.validTo as validTo10_5_1_, assignment0_.validityChangeTimestamp as validit11_5_1_, assignment0_.validityStatus as validit12_5_1_, assignment0_.assignmentOwner as assignm13_5_1_, assignment0_.createChannel as createC14_5_1_, assignment0_.createTimestamp as createT15_5_1_, assignment0_.creatorRef_relation as creator16_5_1_, assignment0_.creatorRef_targetOid as creator17_5_1_, assignment0_.creatorRef_type as creator18_5_1_, assignment0_.extId as extId38_5_1_, assignment0_.extOid as extOid39_5_1_, assignment0_.lifecycleState as lifecyc19_5_1_, assignment0_.modifierRef_relation as modifie20_5_1_, assignment0_.modifierRef_targetOid as modifie21_5_1_, assignment0_.modifierRef_type as modifie22_5_1_, assignment0_.modifyChannel as modifyC23_5_1_, assignment0_.modifyTimestamp as modifyT24_5_1_, assignment0_.orderValue as orderVa25_5_1_, assignment0_.orgRef_relation as orgRef_26_5_1_, assignment0_.orgRef_targetOid as orgRef_27_5_1_, assignment0_.orgRef_type as orgRef_28_5_1_, assignment0_.resourceRef_relation as resourc29_5_1_, assignment0_.resourceRef_targetOid as resourc30_5_1_, assignment0_.resourceRef_type as resourc31_5_1_, assignment0_.targetRef_relation as targetR32_5_1_, assignment0_.targetRef_targetOid as targetR33_5_1_, assignment0_.targetRef_type as targetR34_5_1_, assignment0_.tenantRef_relation as tenantR35_5_1_, assignment0_.tenantRef_targetOid as tenantR36_5_1_, assignment0_.tenantRef_type as tenantR37_5_1_, rassignmen1_.owner_id as owner_id1_12_2_, rassignmen1_.owner_owner_oid as owner_ow2_12_2_ from m_assignment assignment0_ left outer join m_assignment_extension rassignmen1_ on assignment0_.extId=rassignmen1_.owner_id and assignment0_.extOid=rassignmen1_.owner_owner_oid where assignment0_.owner_oid=?
 [1] select booleans0_.anyContainer_owner_id as anyConta2_6_0_, booleans0_.anyContainer_owner_owner_oid as anyConta3_6_0_, booleans0_.item_id as item_id1_6_0_, booleans0_.booleanValue as booleanV4_6_0_, booleans0_.item_id as item_id1_6_1_, booleans0_.anyContainer_owner_id as anyConta2_6_1_, booleans0_.anyContainer_owner_owner_oid as anyConta3_6_1_, booleans0_.booleanValue as booleanV4_6_1_ from m_assignment_ext_boolean booleans0_ where booleans0_.anyContainer_owner_id=? and booleans0_.anyContainer_owner_owner_oid=?
 [1] select dates0_.anyContainer_owner_id as anyConta2_7_0_, dates0_.anyContainer_owner_owner_oid as anyConta3_7_0_, dates0_.item_id as item_id1_7_0_, dates0_.dateValue as dateValu4_7_0_, dates0_.item_id as item_id1_7_1_, dates0_.anyContainer_owner_id as anyConta2_7_1_, dates0_.anyContainer_owner_owner_oid as anyConta3_7_1_, dates0_.dateValue as dateValu4_7_1_ from m_assignment_ext_date dates0_ where dates0_.anyContainer_owner_id=? and dates0_.anyContainer_owner_owner_oid=?
 [1] select longs0_.anyContainer_owner_id as anyConta2_8_0_, longs0_.anyContainer_owner_owner_oid as anyConta3_8_0_, longs0_.item_id as item_id1_8_0_, longs0_.longValue as longValu4_8_0_, longs0_.item_id as item_id1_8_1_, longs0_.anyContainer_owner_id as anyConta2_8_1_, longs0_.anyContainer_owner_owner_oid as anyConta3_8_1_, longs0_.longValue as longValu4_8_1_ from m_assignment_ext_long longs0_ where longs0_.anyContainer_owner_id=? and longs0_.anyContainer_owner_owner_oid=?
 [1] select polys0_.anyContainer_owner_id as anyConta2_9_0_, polys0_.anyContainer_owner_owner_oid as anyConta3_9_0_, polys0_.item_id as item_id1_9_0_, polys0_.orig as orig4_9_0_, polys0_.item_id as item_id1_9_1_, polys0_.anyContainer_owner_id as anyConta2_9_1_, polys0_.anyContainer_owner_owner_oid as anyConta3_9_1_, polys0_.orig as orig4_9_1_, polys0_.norm as norm5_9_1_ from m_assignment_ext_poly polys0_ where polys0_.anyContainer_owner_id=? and polys0_.anyContainer_owner_owner_oid=?
 [1] select references0_.anyContainer_owner_id as anyConta2_10_0_, references0_.anyContainer_owner_owner_oid as anyConta3_10_0_, references0_.item_id as item_id1_10_0_, references0_.targetoid as targetoi4_10_0_, references0_.item_id as item_id1_10_1_, references0_.anyContainer_owner_id as anyConta2_10_1_, references0_.anyContainer_owner_owner_oid as anyConta3_10_1_, references0_.targetoid as targetoi4_10_1_, references0_.relation as relation5_10_1_, references0_.targetType as targetTy6_10_1_ from m_assignment_ext_reference references0_ where references0_.anyContainer_owner_id=? and references0_.anyContainer_owner_owner_oid=?
 [1] select strings0_.anyContainer_owner_id as anyConta2_11_0_, strings0_.anyContainer_owner_owner_oid as anyConta3_11_0_, strings0_.item_id as item_id1_11_0_, strings0_.stringValue as stringVa4_11_0_, strings0_.item_id as item_id1_11_1_, strings0_.anyContainer_owner_id as anyConta2_11_1_, strings0_.anyContainer_owner_owner_oid as anyConta3_11_1_, strings0_.stringValue as stringVa4_11_1_ from m_assignment_ext_string strings0_ where strings0_.anyContainer_owner_id=? and strings0_.anyContainer_owner_owner_oid=?
 [2] insert into m_assignment_ext_string (item_id, anyContainer_owner_id, anyContainer_owner_owner_oid, stringValue) values (?, ?, ?, ?)
 [1] update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
         */

        // todo Can we eliminate deletion selects? Only if we know that extension did not exist before the modify operation.
        assertCounts(11 + getExtraSafeInsertionSelects(2), 12 + getExtraSafeInsertionSelects(2));
    }

    /**
     * This is really tricky. We try to add another value to single-valued extension container.
     */
    @Test
    public void test158AddWholeAssignmentExtensionDifferentValue() throws Exception {
        OperationResult result = new OperationResult(ExtensionTest.class.getName() + ".test158AddWholeAssignmentExtensionDifferentValue");

        PrismContainerValue<?> extValue = expectedUser.getDefinition()
                .findContainerDefinition(UserType.F_ASSIGNMENT)
                .findContainerDefinition(AssignmentType.F_EXTENSION)
                .instantiate().getValue();
        extValue.findOrCreateProperty(EXT_WEAPON).addRealValues("W3");
        extValue.findOrCreateProperty(EXT_SHIP_NAME).addRealValues("?");

        ObjectDelta<UserType> delta = getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT, 1, AssignmentType.F_EXTENSION)
                .add(extValue.clone())
                .asObjectDelta("");
        delta.applyTo(expectedUser);

        queryListener.start();
        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), getOptions(), result);
        queryListener.dumpAndStop();

        try (EntityManager em = factory.createEntityManager()) {
            RUser u = em.find(RUser.class, userOid);
            assertExtension(u, itemHidden1, "h1.1", "h1.2");
            assertExtension(u, itemHidden2);
            assertExtension(u, itemHidden3);
            assertExtension(u, itemVisible, "v1", "v2", "v3");
            assertExtension(u, itemWeapon);
            RAssignment a = u.getAssignments().iterator().next();
            assertExtension(a, itemWeapon, "W3");
            assertExtension(a, itemShipName, "?");
        }

        assertGetObject(result);

        /*
 [1] select oid from m_object where oid = ? for update
 [1] select ruser0_.oid as oid1_27_, ruser0_2_.createChannel as createCh2_27_, ruser0_2_.createTimestamp as createTi3_27_, ruser0_2_.creatorRef_relation as creatorR4_27_, ruser0_2_.creatorRef_targetOid as creatorR5_27_, ruser0_2_.creatorRef_type as creatorR6_27_, ruser0_2_.fullObject as fullObje7_27_, ruser0_2_.lifecycleState as lifecycl8_27_, ruser0_2_.modifierRef_relation as modifier9_27_, ruser0_2_.modifierRef_targetOid as modifie10_27_, ruser0_2_.modifierRef_type as modifie11_27_, ruser0_2_.modifyChannel as modifyC12_27_, ruser0_2_.modifyTimestamp as modifyT13_27_, ruser0_2_.name_norm as name_no14_27_, ruser0_2_.name_orig as name_or15_27_, ruser0_2_.objectTypeClass as objectT16_27_, ruser0_2_.tenantRef_relation as tenantR17_27_, ruser0_2_.tenantRef_targetOid as tenantR18_27_, ruser0_2_.tenantRef_type as tenantR19_27_, ruser0_2_.version as version20_27_, ruser0_1_.administrativeStatus as administ1_53_, ruser0_1_.archiveTimestamp as archiveT2_53_, ruser0_1_.disableReason as disableR3_53_, ruser0_1_.disableTimestamp as disableT4_53_, ruser0_1_.effectiveStatus as effectiv5_53_, ruser0_1_.enableTimestamp as enableTi6_53_, ruser0_1_.validFrom as validFro7_53_, ruser0_1_.validTo as validTo8_53_, ruser0_1_.validityChangeTimestamp as validity9_53_, ruser0_1_.validityStatus as validit10_53_, ruser0_1_.costCenter as costCen11_53_, ruser0_1_.emailAddress as emailAd12_53_, ruser0_1_.hasPhoto as hasPhot13_53_, ruser0_1_.locale as locale14_53_, ruser0_1_.locality_norm as localit15_53_, ruser0_1_.locality_orig as localit16_53_, ruser0_1_.preferredLanguage as preferr17_53_, ruser0_1_.telephoneNumber as telepho18_53_, ruser0_1_.timezone as timezon19_53_, ruser0_.additionalName_norm as addition1_73_, ruser0_.additionalName_orig as addition2_73_, ruser0_.employeeNumber as employee3_73_, ruser0_.familyName_norm as familyNa4_73_, ruser0_.familyName_orig as familyNa5_73_, ruser0_.fullName_norm as fullName6_73_, ruser0_.fullName_orig as fullName7_73_, ruser0_.givenName_norm as givenNam8_73_, ruser0_.givenName_orig as givenNam9_73_, ruser0_.honorificPrefix_norm as honorif10_73_, ruser0_.honorificPrefix_orig as honorif11_73_, ruser0_.honorificSuffix_norm as honorif12_73_, ruser0_.honorificSuffix_orig as honorif13_73_, ruser0_.name_norm as name_no14_73_, ruser0_.name_orig as name_or15_73_, ruser0_.nickName_norm as nickNam16_73_, ruser0_.nickName_orig as nickNam17_73_, ruser0_.title_norm as title_n18_73_, ruser0_.title_orig as title_o19_73_ from m_user ruser0_ inner join m_focus ruser0_1_ on ruser0_.oid=ruser0_1_.oid inner join m_object ruser0_2_ on ruser0_.oid=ruser0_2_.oid where ruser0_.oid=?
 [1] select assignment0_.owner_oid as owner_oi2_5_0_, assignment0_.id as id1_5_0_, assignment0_.id as id1_5_1_, assignment0_.owner_oid as owner_oi2_5_1_, assignment0_.administrativeStatus as administ3_5_1_, assignment0_.archiveTimestamp as archiveT4_5_1_, assignment0_.disableReason as disableR5_5_1_, assignment0_.disableTimestamp as disableT6_5_1_, assignment0_.effectiveStatus as effectiv7_5_1_, assignment0_.enableTimestamp as enableTi8_5_1_, assignment0_.validFrom as validFro9_5_1_, assignment0_.validTo as validTo10_5_1_, assignment0_.validityChangeTimestamp as validit11_5_1_, assignment0_.validityStatus as validit12_5_1_, assignment0_.assignmentOwner as assignm13_5_1_, assignment0_.createChannel as createC14_5_1_, assignment0_.createTimestamp as createT15_5_1_, assignment0_.creatorRef_relation as creator16_5_1_, assignment0_.creatorRef_targetOid as creator17_5_1_, assignment0_.creatorRef_type as creator18_5_1_, assignment0_.extId as extId38_5_1_, assignment0_.extOid as extOid39_5_1_, assignment0_.lifecycleState as lifecyc19_5_1_, assignment0_.modifierRef_relation as modifie20_5_1_, assignment0_.modifierRef_targetOid as modifie21_5_1_, assignment0_.modifierRef_type as modifie22_5_1_, assignment0_.modifyChannel as modifyC23_5_1_, assignment0_.modifyTimestamp as modifyT24_5_1_, assignment0_.orderValue as orderVa25_5_1_, assignment0_.orgRef_relation as orgRef_26_5_1_, assignment0_.orgRef_targetOid as orgRef_27_5_1_, assignment0_.orgRef_type as orgRef_28_5_1_, assignment0_.resourceRef_relation as resourc29_5_1_, assignment0_.resourceRef_targetOid as resourc30_5_1_, assignment0_.resourceRef_type as resourc31_5_1_, assignment0_.targetRef_relation as targetR32_5_1_, assignment0_.targetRef_targetOid as targetR33_5_1_, assignment0_.targetRef_type as targetR34_5_1_, assignment0_.tenantRef_relation as tenantR35_5_1_, assignment0_.tenantRef_targetOid as tenantR36_5_1_, assignment0_.tenantRef_type as tenantR37_5_1_, rassignmen1_.owner_id as owner_id1_12_2_, rassignmen1_.owner_owner_oid as owner_ow2_12_2_ from m_assignment assignment0_ left outer join m_assignment_extension rassignmen1_ on assignment0_.extId=rassignmen1_.owner_id and assignment0_.extOid=rassignmen1_.owner_owner_oid where assignment0_.owner_oid=?
 [1] select booleans0_.anyContainer_owner_id as anyConta2_6_0_, booleans0_.anyContainer_owner_owner_oid as anyConta3_6_0_, booleans0_.item_id as item_id1_6_0_, booleans0_.booleanValue as booleanV4_6_0_, booleans0_.item_id as item_id1_6_1_, booleans0_.anyContainer_owner_id as anyConta2_6_1_, booleans0_.anyContainer_owner_owner_oid as anyConta3_6_1_, booleans0_.booleanValue as booleanV4_6_1_ from m_assignment_ext_boolean booleans0_ where booleans0_.anyContainer_owner_id=? and booleans0_.anyContainer_owner_owner_oid=?
 [1] select dates0_.anyContainer_owner_id as anyConta2_7_0_, dates0_.anyContainer_owner_owner_oid as anyConta3_7_0_, dates0_.item_id as item_id1_7_0_, dates0_.dateValue as dateValu4_7_0_, dates0_.item_id as item_id1_7_1_, dates0_.anyContainer_owner_id as anyConta2_7_1_, dates0_.anyContainer_owner_owner_oid as anyConta3_7_1_, dates0_.dateValue as dateValu4_7_1_ from m_assignment_ext_date dates0_ where dates0_.anyContainer_owner_id=? and dates0_.anyContainer_owner_owner_oid=?
 [1] select longs0_.anyContainer_owner_id as anyConta2_8_0_, longs0_.anyContainer_owner_owner_oid as anyConta3_8_0_, longs0_.item_id as item_id1_8_0_, longs0_.longValue as longValu4_8_0_, longs0_.item_id as item_id1_8_1_, longs0_.anyContainer_owner_id as anyConta2_8_1_, longs0_.anyContainer_owner_owner_oid as anyConta3_8_1_, longs0_.longValue as longValu4_8_1_ from m_assignment_ext_long longs0_ where longs0_.anyContainer_owner_id=? and longs0_.anyContainer_owner_owner_oid=?
 [1] select polys0_.anyContainer_owner_id as anyConta2_9_0_, polys0_.anyContainer_owner_owner_oid as anyConta3_9_0_, polys0_.item_id as item_id1_9_0_, polys0_.orig as orig4_9_0_, polys0_.item_id as item_id1_9_1_, polys0_.anyContainer_owner_id as anyConta2_9_1_, polys0_.anyContainer_owner_owner_oid as anyConta3_9_1_, polys0_.orig as orig4_9_1_, polys0_.norm as norm5_9_1_ from m_assignment_ext_poly polys0_ where polys0_.anyContainer_owner_id=? and polys0_.anyContainer_owner_owner_oid=?
 [1] select references0_.anyContainer_owner_id as anyConta2_10_0_, references0_.anyContainer_owner_owner_oid as anyConta3_10_0_, references0_.item_id as item_id1_10_0_, references0_.targetoid as targetoi4_10_0_, references0_.item_id as item_id1_10_1_, references0_.anyContainer_owner_id as anyConta2_10_1_, references0_.anyContainer_owner_owner_oid as anyConta3_10_1_, references0_.targetoid as targetoi4_10_1_, references0_.relation as relation5_10_1_, references0_.targetType as targetTy6_10_1_ from m_assignment_ext_reference references0_ where references0_.anyContainer_owner_id=? and references0_.anyContainer_owner_owner_oid=?
 [1] select strings0_.anyContainer_owner_id as anyConta2_11_0_, strings0_.anyContainer_owner_owner_oid as anyConta3_11_0_, strings0_.item_id as item_id1_11_0_, strings0_.stringValue as stringVa4_11_0_, strings0_.item_id as item_id1_11_1_, strings0_.anyContainer_owner_id as anyConta2_11_1_, strings0_.anyContainer_owner_owner_oid as anyConta3_11_1_, strings0_.stringValue as stringVa4_11_1_ from m_assignment_ext_string strings0_ where strings0_.anyContainer_owner_id=? and strings0_.anyContainer_owner_owner_oid=?
 [2] insert into m_assignment_ext_string (item_id, anyContainer_owner_id, anyContainer_owner_owner_oid, stringValue) values (?, ?, ?, ?)
 [1] update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
 [2] delete from m_assignment_ext_string where item_id=? and anyContainer_owner_id=? and anyContainer_owner_owner_oid=? and stringValue=?
         */
        assertCounts(12 + getExtraSafeInsertionSelects(2), 14 + getExtraSafeInsertionSelects(2));
    }

    @Test
    public void test160ReplaceWholeAssignmentExtension() throws Exception {
        OperationResult result = new OperationResult(ExtensionTest.class.getName() + ".test055AddWholeExtension");

        PrismContainerValue<?> extValue = expectedUser.getDefinition()
                .findContainerDefinition(UserType.F_ASSIGNMENT)
                .findContainerDefinition(AssignmentType.F_EXTENSION)
                .instantiate().getValue();
        extValue.findOrCreateProperty(EXT_WEAPON).addRealValues("W4", "W5");
        extValue.findOrCreateProperty(EXT_SHIP_NAME).addRealValues("ship");

        ObjectDelta<UserType> delta = getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT, 1, AssignmentType.F_EXTENSION)
                .replace(extValue.clone())
                .asObjectDelta("");
        delta.applyTo(expectedUser);

        queryListener.start();
        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), getOptions(), result);
        queryListener.dumpAndStop();

        try (EntityManager em = factory.createEntityManager()) {
            RUser u = em.find(RUser.class, userOid);
            assertExtension(u, itemHidden1, "h1.1", "h1.2");
            assertExtension(u, itemHidden2);
            assertExtension(u, itemHidden3);
            assertExtension(u, itemVisible, "v1", "v2", "v3");
            assertExtension(u, itemWeapon);
            RAssignment a = u.getAssignments().iterator().next();
            assertExtension(a, itemWeapon, "W4", "W5");
            assertExtension(a, itemShipName, "ship");
        }

        assertGetObject(result);

        /*
 [1] select oid from m_object where oid = ? for update
 [1] select ruser0_.oid as oid1_27_, ruser0_2_.createChannel as createCh2_27_, ruser0_2_.createTimestamp as createTi3_27_, ruser0_2_.creatorRef_relation as creatorR4_27_, ruser0_2_.creatorRef_targetOid as creatorR5_27_, ruser0_2_.creatorRef_type as creatorR6_27_, ruser0_2_.fullObject as fullObje7_27_, ruser0_2_.lifecycleState as lifecycl8_27_, ruser0_2_.modifierRef_relation as modifier9_27_, ruser0_2_.modifierRef_targetOid as modifie10_27_, ruser0_2_.modifierRef_type as modifie11_27_, ruser0_2_.modifyChannel as modifyC12_27_, ruser0_2_.modifyTimestamp as modifyT13_27_, ruser0_2_.name_norm as name_no14_27_, ruser0_2_.name_orig as name_or15_27_, ruser0_2_.objectTypeClass as objectT16_27_, ruser0_2_.tenantRef_relation as tenantR17_27_, ruser0_2_.tenantRef_targetOid as tenantR18_27_, ruser0_2_.tenantRef_type as tenantR19_27_, ruser0_2_.version as version20_27_, ruser0_1_.administrativeStatus as administ1_53_, ruser0_1_.archiveTimestamp as archiveT2_53_, ruser0_1_.disableReason as disableR3_53_, ruser0_1_.disableTimestamp as disableT4_53_, ruser0_1_.effectiveStatus as effectiv5_53_, ruser0_1_.enableTimestamp as enableTi6_53_, ruser0_1_.validFrom as validFro7_53_, ruser0_1_.validTo as validTo8_53_, ruser0_1_.validityChangeTimestamp as validity9_53_, ruser0_1_.validityStatus as validit10_53_, ruser0_1_.costCenter as costCen11_53_, ruser0_1_.emailAddress as emailAd12_53_, ruser0_1_.hasPhoto as hasPhot13_53_, ruser0_1_.locale as locale14_53_, ruser0_1_.locality_norm as localit15_53_, ruser0_1_.locality_orig as localit16_53_, ruser0_1_.preferredLanguage as preferr17_53_, ruser0_1_.telephoneNumber as telepho18_53_, ruser0_1_.timezone as timezon19_53_, ruser0_.additionalName_norm as addition1_73_, ruser0_.additionalName_orig as addition2_73_, ruser0_.employeeNumber as employee3_73_, ruser0_.familyName_norm as familyNa4_73_, ruser0_.familyName_orig as familyNa5_73_, ruser0_.fullName_norm as fullName6_73_, ruser0_.fullName_orig as fullName7_73_, ruser0_.givenName_norm as givenNam8_73_, ruser0_.givenName_orig as givenNam9_73_, ruser0_.honorificPrefix_norm as honorif10_73_, ruser0_.honorificPrefix_orig as honorif11_73_, ruser0_.honorificSuffix_norm as honorif12_73_, ruser0_.honorificSuffix_orig as honorif13_73_, ruser0_.name_norm as name_no14_73_, ruser0_.name_orig as name_or15_73_, ruser0_.nickName_norm as nickNam16_73_, ruser0_.nickName_orig as nickNam17_73_, ruser0_.title_norm as title_n18_73_, ruser0_.title_orig as title_o19_73_ from m_user ruser0_ inner join m_focus ruser0_1_ on ruser0_.oid=ruser0_1_.oid inner join m_object ruser0_2_ on ruser0_.oid=ruser0_2_.oid where ruser0_.oid=?
 [1] select assignment0_.owner_oid as owner_oi2_5_0_, assignment0_.id as id1_5_0_, assignment0_.id as id1_5_1_, assignment0_.owner_oid as owner_oi2_5_1_, assignment0_.administrativeStatus as administ3_5_1_, assignment0_.archiveTimestamp as archiveT4_5_1_, assignment0_.disableReason as disableR5_5_1_, assignment0_.disableTimestamp as disableT6_5_1_, assignment0_.effectiveStatus as effectiv7_5_1_, assignment0_.enableTimestamp as enableTi8_5_1_, assignment0_.validFrom as validFro9_5_1_, assignment0_.validTo as validTo10_5_1_, assignment0_.validityChangeTimestamp as validit11_5_1_, assignment0_.validityStatus as validit12_5_1_, assignment0_.assignmentOwner as assignm13_5_1_, assignment0_.createChannel as createC14_5_1_, assignment0_.createTimestamp as createT15_5_1_, assignment0_.creatorRef_relation as creator16_5_1_, assignment0_.creatorRef_targetOid as creator17_5_1_, assignment0_.creatorRef_type as creator18_5_1_, assignment0_.extId as extId38_5_1_, assignment0_.extOid as extOid39_5_1_, assignment0_.lifecycleState as lifecyc19_5_1_, assignment0_.modifierRef_relation as modifie20_5_1_, assignment0_.modifierRef_targetOid as modifie21_5_1_, assignment0_.modifierRef_type as modifie22_5_1_, assignment0_.modifyChannel as modifyC23_5_1_, assignment0_.modifyTimestamp as modifyT24_5_1_, assignment0_.orderValue as orderVa25_5_1_, assignment0_.orgRef_relation as orgRef_26_5_1_, assignment0_.orgRef_targetOid as orgRef_27_5_1_, assignment0_.orgRef_type as orgRef_28_5_1_, assignment0_.resourceRef_relation as resourc29_5_1_, assignment0_.resourceRef_targetOid as resourc30_5_1_, assignment0_.resourceRef_type as resourc31_5_1_, assignment0_.targetRef_relation as targetR32_5_1_, assignment0_.targetRef_targetOid as targetR33_5_1_, assignment0_.targetRef_type as targetR34_5_1_, assignment0_.tenantRef_relation as tenantR35_5_1_, assignment0_.tenantRef_targetOid as tenantR36_5_1_, assignment0_.tenantRef_type as tenantR37_5_1_, rassignmen1_.owner_id as owner_id1_12_2_, rassignmen1_.owner_owner_oid as owner_ow2_12_2_ from m_assignment assignment0_ left outer join m_assignment_extension rassignmen1_ on assignment0_.extId=rassignmen1_.owner_id and assignment0_.extOid=rassignmen1_.owner_owner_oid where assignment0_.owner_oid=?
 [1] select booleans0_.anyContainer_owner_id as anyConta2_6_0_, booleans0_.anyContainer_owner_owner_oid as anyConta3_6_0_, booleans0_.item_id as item_id1_6_0_, booleans0_.booleanValue as booleanV4_6_0_, booleans0_.item_id as item_id1_6_1_, booleans0_.anyContainer_owner_id as anyConta2_6_1_, booleans0_.anyContainer_owner_owner_oid as anyConta3_6_1_, booleans0_.booleanValue as booleanV4_6_1_ from m_assignment_ext_boolean booleans0_ where booleans0_.anyContainer_owner_id=? and booleans0_.anyContainer_owner_owner_oid=?
 [1] select dates0_.anyContainer_owner_id as anyConta2_7_0_, dates0_.anyContainer_owner_owner_oid as anyConta3_7_0_, dates0_.item_id as item_id1_7_0_, dates0_.dateValue as dateValu4_7_0_, dates0_.item_id as item_id1_7_1_, dates0_.anyContainer_owner_id as anyConta2_7_1_, dates0_.anyContainer_owner_owner_oid as anyConta3_7_1_, dates0_.dateValue as dateValu4_7_1_ from m_assignment_ext_date dates0_ where dates0_.anyContainer_owner_id=? and dates0_.anyContainer_owner_owner_oid=?
 [1] select longs0_.anyContainer_owner_id as anyConta2_8_0_, longs0_.anyContainer_owner_owner_oid as anyConta3_8_0_, longs0_.item_id as item_id1_8_0_, longs0_.longValue as longValu4_8_0_, longs0_.item_id as item_id1_8_1_, longs0_.anyContainer_owner_id as anyConta2_8_1_, longs0_.anyContainer_owner_owner_oid as anyConta3_8_1_, longs0_.longValue as longValu4_8_1_ from m_assignment_ext_long longs0_ where longs0_.anyContainer_owner_id=? and longs0_.anyContainer_owner_owner_oid=?
 [1] select polys0_.anyContainer_owner_id as anyConta2_9_0_, polys0_.anyContainer_owner_owner_oid as anyConta3_9_0_, polys0_.item_id as item_id1_9_0_, polys0_.orig as orig4_9_0_, polys0_.item_id as item_id1_9_1_, polys0_.anyContainer_owner_id as anyConta2_9_1_, polys0_.anyContainer_owner_owner_oid as anyConta3_9_1_, polys0_.orig as orig4_9_1_, polys0_.norm as norm5_9_1_ from m_assignment_ext_poly polys0_ where polys0_.anyContainer_owner_id=? and polys0_.anyContainer_owner_owner_oid=?
 [1] select references0_.anyContainer_owner_id as anyConta2_10_0_, references0_.anyContainer_owner_owner_oid as anyConta3_10_0_, references0_.item_id as item_id1_10_0_, references0_.targetoid as targetoi4_10_0_, references0_.item_id as item_id1_10_1_, references0_.anyContainer_owner_id as anyConta2_10_1_, references0_.anyContainer_owner_owner_oid as anyConta3_10_1_, references0_.targetoid as targetoi4_10_1_, references0_.relation as relation5_10_1_, references0_.targetType as targetTy6_10_1_ from m_assignment_ext_reference references0_ where references0_.anyContainer_owner_id=? and references0_.anyContainer_owner_owner_oid=?
 [1] select strings0_.anyContainer_owner_id as anyConta2_11_0_, strings0_.anyContainer_owner_owner_oid as anyConta3_11_0_, strings0_.item_id as item_id1_11_0_, strings0_.stringValue as stringVa4_11_0_, strings0_.item_id as item_id1_11_1_, strings0_.anyContainer_owner_id as anyConta2_11_1_, strings0_.anyContainer_owner_owner_oid as anyConta3_11_1_, strings0_.stringValue as stringVa4_11_1_ from m_assignment_ext_string strings0_ where strings0_.anyContainer_owner_id=? and strings0_.anyContainer_owner_owner_oid=?
 [3] insert into m_assignment_ext_string (item_id, anyContainer_owner_id, anyContainer_owner_owner_oid, stringValue) values (?, ?, ?, ?)
 [1] update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
 [2] delete from m_assignment_ext_string where item_id=? and anyContainer_owner_id=? and anyContainer_owner_owner_oid=? and stringValue=?
         */
        assertCounts(12 + getExtraSafeInsertionSelects(3), 15 + getExtraSafeInsertionSelects(3));
    }

    @Test
    public void test165ReplaceWholeAssignmentExtensionToNull() throws Exception {
        OperationResult result = new OperationResult(ExtensionTest.class.getName() + ".test165ReplaceWholeAssignmentExtensionToNull");

        ObjectDelta<UserType> delta = getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT, 1, AssignmentType.F_EXTENSION)
                .replace()
                .asObjectDelta("");
        delta.applyTo(expectedUser);

        queryListener.start();
        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), getOptions(), result);
        queryListener.dumpAndStop();

        try (EntityManager em = factory.createEntityManager()) {
            RUser u = em.find(RUser.class, userOid);
            assertExtension(u, itemHidden1, "h1.1", "h1.2");
            assertExtension(u, itemHidden2);
            assertExtension(u, itemHidden3);
            assertExtension(u, itemVisible, "v1", "v2", "v3");
            assertExtension(u, itemWeapon);
            RAssignment a = u.getAssignments().iterator().next();
            assertExtension(a, itemWeapon);
            assertExtension(a, itemShipName);
        }

        assertGetObject(result);

        /*
 [1] select oid from m_object where oid = ? for update
 [1] select ruser0_.oid as oid1_27_, ruser0_2_.createChannel as createCh2_27_, ruser0_2_.createTimestamp as createTi3_27_, ruser0_2_.creatorRef_relation as creatorR4_27_, ruser0_2_.creatorRef_targetOid as creatorR5_27_, ruser0_2_.creatorRef_type as creatorR6_27_, ruser0_2_.fullObject as fullObje7_27_, ruser0_2_.lifecycleState as lifecycl8_27_, ruser0_2_.modifierRef_relation as modifier9_27_, ruser0_2_.modifierRef_targetOid as modifie10_27_, ruser0_2_.modifierRef_type as modifie11_27_, ruser0_2_.modifyChannel as modifyC12_27_, ruser0_2_.modifyTimestamp as modifyT13_27_, ruser0_2_.name_norm as name_no14_27_, ruser0_2_.name_orig as name_or15_27_, ruser0_2_.objectTypeClass as objectT16_27_, ruser0_2_.tenantRef_relation as tenantR17_27_, ruser0_2_.tenantRef_targetOid as tenantR18_27_, ruser0_2_.tenantRef_type as tenantR19_27_, ruser0_2_.version as version20_27_, ruser0_1_.administrativeStatus as administ1_53_, ruser0_1_.archiveTimestamp as archiveT2_53_, ruser0_1_.disableReason as disableR3_53_, ruser0_1_.disableTimestamp as disableT4_53_, ruser0_1_.effectiveStatus as effectiv5_53_, ruser0_1_.enableTimestamp as enableTi6_53_, ruser0_1_.validFrom as validFro7_53_, ruser0_1_.validTo as validTo8_53_, ruser0_1_.validityChangeTimestamp as validity9_53_, ruser0_1_.validityStatus as validit10_53_, ruser0_1_.costCenter as costCen11_53_, ruser0_1_.emailAddress as emailAd12_53_, ruser0_1_.hasPhoto as hasPhot13_53_, ruser0_1_.locale as locale14_53_, ruser0_1_.locality_norm as localit15_53_, ruser0_1_.locality_orig as localit16_53_, ruser0_1_.preferredLanguage as preferr17_53_, ruser0_1_.telephoneNumber as telepho18_53_, ruser0_1_.timezone as timezon19_53_, ruser0_.additionalName_norm as addition1_73_, ruser0_.additionalName_orig as addition2_73_, ruser0_.employeeNumber as employee3_73_, ruser0_.familyName_norm as familyNa4_73_, ruser0_.familyName_orig as familyNa5_73_, ruser0_.fullName_norm as fullName6_73_, ruser0_.fullName_orig as fullName7_73_, ruser0_.givenName_norm as givenNam8_73_, ruser0_.givenName_orig as givenNam9_73_, ruser0_.honorificPrefix_norm as honorif10_73_, ruser0_.honorificPrefix_orig as honorif11_73_, ruser0_.honorificSuffix_norm as honorif12_73_, ruser0_.honorificSuffix_orig as honorif13_73_, ruser0_.name_norm as name_no14_73_, ruser0_.name_orig as name_or15_73_, ruser0_.nickName_norm as nickNam16_73_, ruser0_.nickName_orig as nickNam17_73_, ruser0_.title_norm as title_n18_73_, ruser0_.title_orig as title_o19_73_ from m_user ruser0_ inner join m_focus ruser0_1_ on ruser0_.oid=ruser0_1_.oid inner join m_object ruser0_2_ on ruser0_.oid=ruser0_2_.oid where ruser0_.oid=?
 [1] select assignment0_.owner_oid as owner_oi2_5_0_, assignment0_.id as id1_5_0_, assignment0_.id as id1_5_1_, assignment0_.owner_oid as owner_oi2_5_1_, assignment0_.administrativeStatus as administ3_5_1_, assignment0_.archiveTimestamp as archiveT4_5_1_, assignment0_.disableReason as disableR5_5_1_, assignment0_.disableTimestamp as disableT6_5_1_, assignment0_.effectiveStatus as effectiv7_5_1_, assignment0_.enableTimestamp as enableTi8_5_1_, assignment0_.validFrom as validFro9_5_1_, assignment0_.validTo as validTo10_5_1_, assignment0_.validityChangeTimestamp as validit11_5_1_, assignment0_.validityStatus as validit12_5_1_, assignment0_.assignmentOwner as assignm13_5_1_, assignment0_.createChannel as createC14_5_1_, assignment0_.createTimestamp as createT15_5_1_, assignment0_.creatorRef_relation as creator16_5_1_, assignment0_.creatorRef_targetOid as creator17_5_1_, assignment0_.creatorRef_type as creator18_5_1_, assignment0_.extId as extId38_5_1_, assignment0_.extOid as extOid39_5_1_, assignment0_.lifecycleState as lifecyc19_5_1_, assignment0_.modifierRef_relation as modifie20_5_1_, assignment0_.modifierRef_targetOid as modifie21_5_1_, assignment0_.modifierRef_type as modifie22_5_1_, assignment0_.modifyChannel as modifyC23_5_1_, assignment0_.modifyTimestamp as modifyT24_5_1_, assignment0_.orderValue as orderVa25_5_1_, assignment0_.orgRef_relation as orgRef_26_5_1_, assignment0_.orgRef_targetOid as orgRef_27_5_1_, assignment0_.orgRef_type as orgRef_28_5_1_, assignment0_.resourceRef_relation as resourc29_5_1_, assignment0_.resourceRef_targetOid as resourc30_5_1_, assignment0_.resourceRef_type as resourc31_5_1_, assignment0_.targetRef_relation as targetR32_5_1_, assignment0_.targetRef_targetOid as targetR33_5_1_, assignment0_.targetRef_type as targetR34_5_1_, assignment0_.tenantRef_relation as tenantR35_5_1_, assignment0_.tenantRef_targetOid as tenantR36_5_1_, assignment0_.tenantRef_type as tenantR37_5_1_, rassignmen1_.owner_id as owner_id1_12_2_, rassignmen1_.owner_owner_oid as owner_ow2_12_2_ from m_assignment assignment0_ left outer join m_assignment_extension rassignmen1_ on assignment0_.extId=rassignmen1_.owner_id and assignment0_.extOid=rassignmen1_.owner_owner_oid where assignment0_.owner_oid=?
 [1] select booleans0_.anyContainer_owner_id as anyConta2_6_0_, booleans0_.anyContainer_owner_owner_oid as anyConta3_6_0_, booleans0_.item_id as item_id1_6_0_, booleans0_.booleanValue as booleanV4_6_0_, booleans0_.item_id as item_id1_6_1_, booleans0_.anyContainer_owner_id as anyConta2_6_1_, booleans0_.anyContainer_owner_owner_oid as anyConta3_6_1_, booleans0_.booleanValue as booleanV4_6_1_ from m_assignment_ext_boolean booleans0_ where booleans0_.anyContainer_owner_id=? and booleans0_.anyContainer_owner_owner_oid=?
 [1] select dates0_.anyContainer_owner_id as anyConta2_7_0_, dates0_.anyContainer_owner_owner_oid as anyConta3_7_0_, dates0_.item_id as item_id1_7_0_, dates0_.dateValue as dateValu4_7_0_, dates0_.item_id as item_id1_7_1_, dates0_.anyContainer_owner_id as anyConta2_7_1_, dates0_.anyContainer_owner_owner_oid as anyConta3_7_1_, dates0_.dateValue as dateValu4_7_1_ from m_assignment_ext_date dates0_ where dates0_.anyContainer_owner_id=? and dates0_.anyContainer_owner_owner_oid=?
 [1] select longs0_.anyContainer_owner_id as anyConta2_8_0_, longs0_.anyContainer_owner_owner_oid as anyConta3_8_0_, longs0_.item_id as item_id1_8_0_, longs0_.longValue as longValu4_8_0_, longs0_.item_id as item_id1_8_1_, longs0_.anyContainer_owner_id as anyConta2_8_1_, longs0_.anyContainer_owner_owner_oid as anyConta3_8_1_, longs0_.longValue as longValu4_8_1_ from m_assignment_ext_long longs0_ where longs0_.anyContainer_owner_id=? and longs0_.anyContainer_owner_owner_oid=?
 [1] select polys0_.anyContainer_owner_id as anyConta2_9_0_, polys0_.anyContainer_owner_owner_oid as anyConta3_9_0_, polys0_.item_id as item_id1_9_0_, polys0_.orig as orig4_9_0_, polys0_.item_id as item_id1_9_1_, polys0_.anyContainer_owner_id as anyConta2_9_1_, polys0_.anyContainer_owner_owner_oid as anyConta3_9_1_, polys0_.orig as orig4_9_1_, polys0_.norm as norm5_9_1_ from m_assignment_ext_poly polys0_ where polys0_.anyContainer_owner_id=? and polys0_.anyContainer_owner_owner_oid=?
 [1] select references0_.anyContainer_owner_id as anyConta2_10_0_, references0_.anyContainer_owner_owner_oid as anyConta3_10_0_, references0_.item_id as item_id1_10_0_, references0_.targetoid as targetoi4_10_0_, references0_.item_id as item_id1_10_1_, references0_.anyContainer_owner_id as anyConta2_10_1_, references0_.anyContainer_owner_owner_oid as anyConta3_10_1_, references0_.targetoid as targetoi4_10_1_, references0_.relation as relation5_10_1_, references0_.targetType as targetTy6_10_1_ from m_assignment_ext_reference references0_ where references0_.anyContainer_owner_id=? and references0_.anyContainer_owner_owner_oid=?
 [1] select strings0_.anyContainer_owner_id as anyConta2_11_0_, strings0_.anyContainer_owner_owner_oid as anyConta3_11_0_, strings0_.item_id as item_id1_11_0_, strings0_.stringValue as stringVa4_11_0_, strings0_.item_id as item_id1_11_1_, strings0_.anyContainer_owner_id as anyConta2_11_1_, strings0_.anyContainer_owner_owner_oid as anyConta3_11_1_, strings0_.stringValue as stringVa4_11_1_ from m_assignment_ext_string strings0_ where strings0_.anyContainer_owner_id=? and strings0_.anyContainer_owner_owner_oid=?
 [1] update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
 [3] delete from m_assignment_ext_string where item_id=? and anyContainer_owner_id=? and anyContainer_owner_owner_oid=? and stringValue=?
         */
        assertCounts(11, 13);
    }

    @Test
    public void test170AddAssignments() throws Exception {
        OperationResult result = new OperationResult(ExtensionTest.class.getName() + ".test170AddAssignments");

        AssignmentType newAssignment = new AssignmentType(prismContext)
                .id(777L)
                .targetRef("999999", OrgType.COMPLEX_TYPE);
        PrismContainerValue<?> newExtValue = newAssignment.asPrismContainerValue()
                .findOrCreateContainer(AssignmentType.F_EXTENSION)
                .getValue();
        newExtValue.findOrCreateProperty(EXT_WEAPON).addRealValues("W2.1", "W2.2");
        newExtValue.findOrCreateProperty(EXT_SHIP_NAME).addRealValues("ship2");

        AssignmentType newAssignment2 = new AssignmentType(prismContext)
                .id(888L)
                .targetRef("999999", OrgType.COMPLEX_TYPE);
        PrismContainerValue<?> newExtValue2 = newAssignment2.asPrismContainerValue()
                .findOrCreateContainer(AssignmentType.F_EXTENSION)
                .getValue();
        newExtValue2.findOrCreateProperty(EXT_WEAPON).addRealValues("W3.1", "W3.2");
        newExtValue2.findOrCreateProperty(EXT_SHIP_NAME).addRealValues("ship3");

        ObjectDelta<UserType> delta = getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT)
                .add(newAssignment, newAssignment2)
                .asObjectDelta("");
        delta.applyTo(expectedUser);

        queryListener.start();
        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), getOptions(), result);
        queryListener.dumpAndStop();

        try (EntityManager em = factory.createEntityManager()) {
            RUser u = em.find(RUser.class, userOid);
            assertExtension(u, itemHidden1, "h1.1", "h1.2");
            assertExtension(u, itemHidden2);
            assertExtension(u, itemHidden3);
            assertExtension(u, itemVisible, "v1", "v2", "v3");
            assertExtension(u, itemWeapon);
            RAssignment a1 = find(u.getAssignments(), 1);
            RAssignment a2 = find(u.getAssignments(), 777);
            RAssignment a3 = find(u.getAssignments(), 888);
            assertExtension(a1, itemWeapon);
            assertExtension(a1, itemShipName);
            assertExtension(a2, itemWeapon, "W2.1", "W2.2");
            assertExtension(a2, itemShipName, "ship2");
            assertExtension(a3, itemWeapon, "W3.1", "W3.2");
            assertExtension(a3, itemShipName, "ship3");
        }

        assertGetObject(result);

        /*
 [1] select oid from m_object where oid = ? for update
 [1] select ruser0_.oid as oid1_27_, ruser0_2_.createChannel as createCh2_27_, ruser0_2_.createTimestamp as createTi3_27_, ruser0_2_.creatorRef_relation as creatorR4_27_, ruser0_2_.creatorRef_targetOid as creatorR5_27_, ruser0_2_.creatorRef_type as creatorR6_27_, ruser0_2_.fullObject as fullObje7_27_, ruser0_2_.lifecycleState as lifecycl8_27_, ruser0_2_.modifierRef_relation as modifier9_27_, ruser0_2_.modifierRef_targetOid as modifie10_27_, ruser0_2_.modifierRef_type as modifie11_27_, ruser0_2_.modifyChannel as modifyC12_27_, ruser0_2_.modifyTimestamp as modifyT13_27_, ruser0_2_.name_norm as name_no14_27_, ruser0_2_.name_orig as name_or15_27_, ruser0_2_.objectTypeClass as objectT16_27_, ruser0_2_.tenantRef_relation as tenantR17_27_, ruser0_2_.tenantRef_targetOid as tenantR18_27_, ruser0_2_.tenantRef_type as tenantR19_27_, ruser0_2_.version as version20_27_, ruser0_1_.administrativeStatus as administ1_53_, ruser0_1_.archiveTimestamp as archiveT2_53_, ruser0_1_.disableReason as disableR3_53_, ruser0_1_.disableTimestamp as disableT4_53_, ruser0_1_.effectiveStatus as effectiv5_53_, ruser0_1_.enableTimestamp as enableTi6_53_, ruser0_1_.validFrom as validFro7_53_, ruser0_1_.validTo as validTo8_53_, ruser0_1_.validityChangeTimestamp as validity9_53_, ruser0_1_.validityStatus as validit10_53_, ruser0_1_.costCenter as costCen11_53_, ruser0_1_.emailAddress as emailAd12_53_, ruser0_1_.hasPhoto as hasPhot13_53_, ruser0_1_.locale as locale14_53_, ruser0_1_.locality_norm as localit15_53_, ruser0_1_.locality_orig as localit16_53_, ruser0_1_.preferredLanguage as preferr17_53_, ruser0_1_.telephoneNumber as telepho18_53_, ruser0_1_.timezone as timezon19_53_, ruser0_.additionalName_norm as addition1_73_, ruser0_.additionalName_orig as addition2_73_, ruser0_.employeeNumber as employee3_73_, ruser0_.familyName_norm as familyNa4_73_, ruser0_.familyName_orig as familyNa5_73_, ruser0_.fullName_norm as fullName6_73_, ruser0_.fullName_orig as fullName7_73_, ruser0_.givenName_norm as givenNam8_73_, ruser0_.givenName_orig as givenNam9_73_, ruser0_.honorificPrefix_norm as honorif10_73_, ruser0_.honorificPrefix_orig as honorif11_73_, ruser0_.honorificSuffix_norm as honorif12_73_, ruser0_.honorificSuffix_orig as honorif13_73_, ruser0_.name_norm as name_no14_73_, ruser0_.name_orig as name_or15_73_, ruser0_.nickName_norm as nickNam16_73_, ruser0_.nickName_orig as nickNam17_73_, ruser0_.title_norm as title_n18_73_, ruser0_.title_orig as title_o19_73_ from m_user ruser0_ inner join m_focus ruser0_1_ on ruser0_.oid=ruser0_1_.oid inner join m_object ruser0_2_ on ruser0_.oid=ruser0_2_.oid where ruser0_.oid=?
 [1] select assignment0_.owner_oid as owner_oi2_5_0_, assignment0_.id as id1_5_0_, assignment0_.id as id1_5_1_, assignment0_.owner_oid as owner_oi2_5_1_, assignment0_.administrativeStatus as administ3_5_1_, assignment0_.archiveTimestamp as archiveT4_5_1_, assignment0_.disableReason as disableR5_5_1_, assignment0_.disableTimestamp as disableT6_5_1_, assignment0_.effectiveStatus as effectiv7_5_1_, assignment0_.enableTimestamp as enableTi8_5_1_, assignment0_.validFrom as validFro9_5_1_, assignment0_.validTo as validTo10_5_1_, assignment0_.validityChangeTimestamp as validit11_5_1_, assignment0_.validityStatus as validit12_5_1_, assignment0_.assignmentOwner as assignm13_5_1_, assignment0_.createChannel as createC14_5_1_, assignment0_.createTimestamp as createT15_5_1_, assignment0_.creatorRef_relation as creator16_5_1_, assignment0_.creatorRef_targetOid as creator17_5_1_, assignment0_.creatorRef_type as creator18_5_1_, assignment0_.extId as extId38_5_1_, assignment0_.extOid as extOid39_5_1_, assignment0_.lifecycleState as lifecyc19_5_1_, assignment0_.modifierRef_relation as modifie20_5_1_, assignment0_.modifierRef_targetOid as modifie21_5_1_, assignment0_.modifierRef_type as modifie22_5_1_, assignment0_.modifyChannel as modifyC23_5_1_, assignment0_.modifyTimestamp as modifyT24_5_1_, assignment0_.orderValue as orderVa25_5_1_, assignment0_.orgRef_relation as orgRef_26_5_1_, assignment0_.orgRef_targetOid as orgRef_27_5_1_, assignment0_.orgRef_type as orgRef_28_5_1_, assignment0_.resourceRef_relation as resourc29_5_1_, assignment0_.resourceRef_targetOid as resourc30_5_1_, assignment0_.resourceRef_type as resourc31_5_1_, assignment0_.targetRef_relation as targetR32_5_1_, assignment0_.targetRef_targetOid as targetR33_5_1_, assignment0_.targetRef_type as targetR34_5_1_, assignment0_.tenantRef_relation as tenantR35_5_1_, assignment0_.tenantRef_targetOid as tenantR36_5_1_, assignment0_.tenantRef_type as tenantR37_5_1_, rassignmen1_.owner_id as owner_id1_12_2_, rassignmen1_.owner_owner_oid as owner_ow2_12_2_ from m_assignment assignment0_ left outer join m_assignment_extension rassignmen1_ on assignment0_.extId=rassignmen1_.owner_id and assignment0_.extOid=rassignmen1_.owner_owner_oid where assignment0_.owner_oid=?
 [1] select rassignmen_.owner_id, rassignmen_.owner_owner_oid from m_assignment_extension rassignmen_ where rassignmen_.owner_id=? and rassignmen_.owner_owner_oid=?
 [1] select raextstrin_.item_id, raextstrin_.anyContainer_owner_id, raextstrin_.anyContainer_owner_owner_oid, raextstrin_.stringValue from m_assignment_ext_string raextstrin_ where raextstrin_.item_id=? and raextstrin_.anyContainer_owner_id=? and raextstrin_.anyContainer_owner_owner_oid=? and raextstrin_.stringValue=?
 [1] select raextstrin_.item_id, raextstrin_.anyContainer_owner_id, raextstrin_.anyContainer_owner_owner_oid, raextstrin_.stringValue from m_assignment_ext_string raextstrin_ where raextstrin_.item_id=? and raextstrin_.anyContainer_owner_id=? and raextstrin_.anyContainer_owner_owner_oid=? and raextstrin_.stringValue=?
 [1] select raextstrin_.item_id, raextstrin_.anyContainer_owner_id, raextstrin_.anyContainer_owner_owner_oid, raextstrin_.stringValue from m_assignment_ext_string raextstrin_ where raextstrin_.item_id=? and raextstrin_.anyContainer_owner_id=? and raextstrin_.anyContainer_owner_owner_oid=? and raextstrin_.stringValue=?
 [1] select rassignmen_.owner_id, rassignmen_.owner_owner_oid from m_assignment_extension rassignmen_ where rassignmen_.owner_id=? and rassignmen_.owner_owner_oid=?
 [1] select raextstrin_.item_id, raextstrin_.anyContainer_owner_id, raextstrin_.anyContainer_owner_owner_oid, raextstrin_.stringValue from m_assignment_ext_string raextstrin_ where raextstrin_.item_id=? and raextstrin_.anyContainer_owner_id=? and raextstrin_.anyContainer_owner_owner_oid=? and raextstrin_.stringValue=?
 [1] select raextstrin_.item_id, raextstrin_.anyContainer_owner_id, raextstrin_.anyContainer_owner_owner_oid, raextstrin_.stringValue from m_assignment_ext_string raextstrin_ where raextstrin_.item_id=? and raextstrin_.anyContainer_owner_id=? and raextstrin_.anyContainer_owner_owner_oid=? and raextstrin_.stringValue=?
 [1] select raextstrin_.item_id, raextstrin_.anyContainer_owner_id, raextstrin_.anyContainer_owner_owner_oid, raextstrin_.stringValue from m_assignment_ext_string raextstrin_ where raextstrin_.item_id=? and raextstrin_.anyContainer_owner_id=? and raextstrin_.anyContainer_owner_owner_oid=? and raextstrin_.stringValue=?
 [1] insert into m_assignment (administrativeStatus, archiveTimestamp, disableReason, disableTimestamp, effectiveStatus, enableTimestamp, validFrom, validTo, validityChangeTimestamp, validityStatus, assignmentOwner, createChannel, createTimestamp, creatorRef_relation, creatorRef_targetOid, creatorRef_type, extId, extOid, lifecycleState, modifierRef_relation, modifierRef_targetOid, modifierRef_type, modifyChannel, modifyTimestamp, orderValue, orgRef_relation, orgRef_targetOid, orgRef_type, resourceRef_relation, resourceRef_targetOid, resourceRef_type, targetRef_relation, targetRef_targetOid, targetRef_type, tenantRef_relation, tenantRef_targetOid, tenantRef_type, id, owner_oid) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
 [1] insert into m_assignment_extension (owner_id, owner_owner_oid) values (?, ?)
 [3] insert into m_assignment_ext_string (item_id, anyContainer_owner_id, anyContainer_owner_owner_oid, stringValue) values (?, ?, ?, ?)
 [1] insert into m_assignment (administrativeStatus, archiveTimestamp, disableReason, disableTimestamp, effectiveStatus, enableTimestamp, validFrom, validTo, validityChangeTimestamp, validityStatus, assignmentOwner, createChannel, createTimestamp, creatorRef_relation, creatorRef_targetOid, creatorRef_type, extId, extOid, lifecycleState, modifierRef_relation, modifierRef_targetOid, modifierRef_type, modifyChannel, modifyTimestamp, orderValue, orgRef_relation, orgRef_targetOid, orgRef_type, resourceRef_relation, resourceRef_targetOid, resourceRef_type, targetRef_relation, targetRef_targetOid, targetRef_type, tenantRef_relation, tenantRef_targetOid, tenantRef_type, id, owner_oid) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
 [1] insert into m_assignment_extension (owner_id, owner_owner_oid) values (?, ?)
 [3] insert into m_assignment_ext_string (item_id, anyContainer_owner_id, anyContainer_owner_owner_oid, stringValue) values (?, ?, ?, ?)
 [1] update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
 [2] update m_assignment set administrativeStatus=?, archiveTimestamp=?, disableReason=?, disableTimestamp=?, effectiveStatus=?, enableTimestamp=?, validFrom=?, validTo=?, validityChangeTimestamp=?, validityStatus=?, assignmentOwner=?, createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, extId=?, extOid=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, orderValue=?, orgRef_relation=?, orgRef_targetOid=?, orgRef_type=?, resourceRef_relation=?, resourceRef_targetOid=?, resourceRef_type=?, targetRef_relation=?, targetRef_targetOid=?, targetRef_type=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=? where id=? and owner_oid=?
         */
        // TODO why there are specific SELECTs for assignment extension values? But let's keep that for now.
        assertCounts(19, 24);
    }

    @Test
    public void test173DeleteAssignment() throws Exception {
        OperationResult result = new OperationResult(ExtensionTest.class.getName() + ".test173DeleteAssignment");

        ObjectDelta<UserType> delta = getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT)
                .delete(new AssignmentType(prismContext).id(777L))
                .asObjectDelta("");
        delta.applyTo(expectedUser);

        queryListener.start();
        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), getOptions(), result);
        queryListener.dumpAndStop();

        try (EntityManager em = factory.createEntityManager()) {
            RUser u = em.find(RUser.class, userOid);
            assertExtension(u, itemHidden1, "h1.1", "h1.2");
            assertExtension(u, itemHidden2);
            assertExtension(u, itemHidden3);
            assertExtension(u, itemVisible, "v1", "v2", "v3");
            assertExtension(u, itemWeapon);
            assertEquals("Wrong # of assignments", 2, u.getAssignments().size());
            RAssignment a1 = find(u.getAssignments(), 1);
            RAssignment a3 = find(u.getAssignments(), 888);
            assertExtension(a1, itemWeapon);
            assertExtension(a1, itemShipName);
            assertExtension(a3, itemWeapon, "W3.1", "W3.2");
            assertExtension(a3, itemShipName, "ship3");
        }

        assertGetObject(result);

        /*
 [1] select oid from m_object where oid = ? for update
 [1] select ruser0_.oid as oid1_27_, ruser0_2_.createChannel as createCh2_27_, ruser0_2_.createTimestamp as createTi3_27_, ruser0_2_.creatorRef_relation as creatorR4_27_, ruser0_2_.creatorRef_targetOid as creatorR5_27_, ruser0_2_.creatorRef_type as creatorR6_27_, ruser0_2_.fullObject as fullObje7_27_, ruser0_2_.lifecycleState as lifecycl8_27_, ruser0_2_.modifierRef_relation as modifier9_27_, ruser0_2_.modifierRef_targetOid as modifie10_27_, ruser0_2_.modifierRef_type as modifie11_27_, ruser0_2_.modifyChannel as modifyC12_27_, ruser0_2_.modifyTimestamp as modifyT13_27_, ruser0_2_.name_norm as name_no14_27_, ruser0_2_.name_orig as name_or15_27_, ruser0_2_.objectTypeClass as objectT16_27_, ruser0_2_.tenantRef_relation as tenantR17_27_, ruser0_2_.tenantRef_targetOid as tenantR18_27_, ruser0_2_.tenantRef_type as tenantR19_27_, ruser0_2_.version as version20_27_, ruser0_1_.administrativeStatus as administ1_53_, ruser0_1_.archiveTimestamp as archiveT2_53_, ruser0_1_.disableReason as disableR3_53_, ruser0_1_.disableTimestamp as disableT4_53_, ruser0_1_.effectiveStatus as effectiv5_53_, ruser0_1_.enableTimestamp as enableTi6_53_, ruser0_1_.validFrom as validFro7_53_, ruser0_1_.validTo as validTo8_53_, ruser0_1_.validityChangeTimestamp as validity9_53_, ruser0_1_.validityStatus as validit10_53_, ruser0_1_.costCenter as costCen11_53_, ruser0_1_.emailAddress as emailAd12_53_, ruser0_1_.hasPhoto as hasPhot13_53_, ruser0_1_.locale as locale14_53_, ruser0_1_.locality_norm as localit15_53_, ruser0_1_.locality_orig as localit16_53_, ruser0_1_.preferredLanguage as preferr17_53_, ruser0_1_.telephoneNumber as telepho18_53_, ruser0_1_.timezone as timezon19_53_, ruser0_.additionalName_norm as addition1_73_, ruser0_.additionalName_orig as addition2_73_, ruser0_.employeeNumber as employee3_73_, ruser0_.familyName_norm as familyNa4_73_, ruser0_.familyName_orig as familyNa5_73_, ruser0_.fullName_norm as fullName6_73_, ruser0_.fullName_orig as fullName7_73_, ruser0_.givenName_norm as givenNam8_73_, ruser0_.givenName_orig as givenNam9_73_, ruser0_.honorificPrefix_norm as honorif10_73_, ruser0_.honorificPrefix_orig as honorif11_73_, ruser0_.honorificSuffix_norm as honorif12_73_, ruser0_.honorificSuffix_orig as honorif13_73_, ruser0_.name_norm as name_no14_73_, ruser0_.name_orig as name_or15_73_, ruser0_.nickName_norm as nickNam16_73_, ruser0_.nickName_orig as nickNam17_73_, ruser0_.title_norm as title_n18_73_, ruser0_.title_orig as title_o19_73_ from m_user ruser0_ inner join m_focus ruser0_1_ on ruser0_.oid=ruser0_1_.oid inner join m_object ruser0_2_ on ruser0_.oid=ruser0_2_.oid where ruser0_.oid=?
 [1] select assignment0_.owner_oid as owner_oi2_5_0_, assignment0_.id as id1_5_0_, assignment0_.id as id1_5_1_, assignment0_.owner_oid as owner_oi2_5_1_, assignment0_.administrativeStatus as administ3_5_1_, assignment0_.archiveTimestamp as archiveT4_5_1_, assignment0_.disableReason as disableR5_5_1_, assignment0_.disableTimestamp as disableT6_5_1_, assignment0_.effectiveStatus as effectiv7_5_1_, assignment0_.enableTimestamp as enableTi8_5_1_, assignment0_.validFrom as validFro9_5_1_, assignment0_.validTo as validTo10_5_1_, assignment0_.validityChangeTimestamp as validit11_5_1_, assignment0_.validityStatus as validit12_5_1_, assignment0_.assignmentOwner as assignm13_5_1_, assignment0_.createChannel as createC14_5_1_, assignment0_.createTimestamp as createT15_5_1_, assignment0_.creatorRef_relation as creator16_5_1_, assignment0_.creatorRef_targetOid as creator17_5_1_, assignment0_.creatorRef_type as creator18_5_1_, assignment0_.extId as extId38_5_1_, assignment0_.extOid as extOid39_5_1_, assignment0_.lifecycleState as lifecyc19_5_1_, assignment0_.modifierRef_relation as modifie20_5_1_, assignment0_.modifierRef_targetOid as modifie21_5_1_, assignment0_.modifierRef_type as modifie22_5_1_, assignment0_.modifyChannel as modifyC23_5_1_, assignment0_.modifyTimestamp as modifyT24_5_1_, assignment0_.orderValue as orderVa25_5_1_, assignment0_.orgRef_relation as orgRef_26_5_1_, assignment0_.orgRef_targetOid as orgRef_27_5_1_, assignment0_.orgRef_type as orgRef_28_5_1_, assignment0_.resourceRef_relation as resourc29_5_1_, assignment0_.resourceRef_targetOid as resourc30_5_1_, assignment0_.resourceRef_type as resourc31_5_1_, assignment0_.targetRef_relation as targetR32_5_1_, assignment0_.targetRef_targetOid as targetR33_5_1_, assignment0_.targetRef_type as targetR34_5_1_, assignment0_.tenantRef_relation as tenantR35_5_1_, assignment0_.tenantRef_targetOid as tenantR36_5_1_, assignment0_.tenantRef_type as tenantR37_5_1_, rassignmen1_.owner_id as owner_id1_12_2_, rassignmen1_.owner_owner_oid as owner_ow2_12_2_ from m_assignment assignment0_ left outer join m_assignment_extension rassignmen1_ on assignment0_.extId=rassignmen1_.owner_id and assignment0_.extOid=rassignmen1_.owner_owner_oid where assignment0_.owner_oid=?
 [1] select createappr0_.owner_id as owner_id1_14_0_, createappr0_.owner_owner_oid as owner_ow2_14_0_, createappr0_.reference_type as referenc3_14_0_, createappr0_.relation as relation4_14_0_, createappr0_.targetOid as targetOi5_14_0_, createappr0_.owner_id as owner_id1_14_1_, createappr0_.owner_owner_oid as owner_ow2_14_1_, createappr0_.reference_type as referenc3_14_1_, createappr0_.relation as relation4_14_1_, createappr0_.targetOid as targetOi5_14_1_, createappr0_.targetType as targetTy6_14_1_ from m_assignment_reference createappr0_ where ( createappr0_.reference_type= 0) and createappr0_.owner_id=? and createappr0_.owner_owner_oid=?
 [1] select modifyappr0_.owner_id as owner_id1_14_0_, modifyappr0_.owner_owner_oid as owner_ow2_14_0_, modifyappr0_.reference_type as referenc3_14_0_, modifyappr0_.relation as relation4_14_0_, modifyappr0_.targetOid as targetOi5_14_0_, modifyappr0_.owner_id as owner_id1_14_1_, modifyappr0_.owner_owner_oid as owner_ow2_14_1_, modifyappr0_.reference_type as referenc3_14_1_, modifyappr0_.relation as relation4_14_1_, modifyappr0_.targetOid as targetOi5_14_1_, modifyappr0_.targetType as targetTy6_14_1_ from m_assignment_reference modifyappr0_ where ( modifyappr0_.reference_type= 1) and modifyappr0_.owner_id=? and modifyappr0_.owner_owner_oid=?
 [1] select booleans0_.anyContainer_owner_id as anyConta2_6_0_, booleans0_.anyContainer_owner_owner_oid as anyConta3_6_0_, booleans0_.item_id as item_id1_6_0_, booleans0_.booleanValue as booleanV4_6_0_, booleans0_.item_id as item_id1_6_1_, booleans0_.anyContainer_owner_id as anyConta2_6_1_, booleans0_.anyContainer_owner_owner_oid as anyConta3_6_1_, booleans0_.booleanValue as booleanV4_6_1_ from m_assignment_ext_boolean booleans0_ where booleans0_.anyContainer_owner_id=? and booleans0_.anyContainer_owner_owner_oid=?
 [1] select dates0_.anyContainer_owner_id as anyConta2_7_0_, dates0_.anyContainer_owner_owner_oid as anyConta3_7_0_, dates0_.item_id as item_id1_7_0_, dates0_.dateValue as dateValu4_7_0_, dates0_.item_id as item_id1_7_1_, dates0_.anyContainer_owner_id as anyConta2_7_1_, dates0_.anyContainer_owner_owner_oid as anyConta3_7_1_, dates0_.dateValue as dateValu4_7_1_ from m_assignment_ext_date dates0_ where dates0_.anyContainer_owner_id=? and dates0_.anyContainer_owner_owner_oid=?
 [1] select longs0_.anyContainer_owner_id as anyConta2_8_0_, longs0_.anyContainer_owner_owner_oid as anyConta3_8_0_, longs0_.item_id as item_id1_8_0_, longs0_.longValue as longValu4_8_0_, longs0_.item_id as item_id1_8_1_, longs0_.anyContainer_owner_id as anyConta2_8_1_, longs0_.anyContainer_owner_owner_oid as anyConta3_8_1_, longs0_.longValue as longValu4_8_1_ from m_assignment_ext_long longs0_ where longs0_.anyContainer_owner_id=? and longs0_.anyContainer_owner_owner_oid=?
 [1] select polys0_.anyContainer_owner_id as anyConta2_9_0_, polys0_.anyContainer_owner_owner_oid as anyConta3_9_0_, polys0_.item_id as item_id1_9_0_, polys0_.orig as orig4_9_0_, polys0_.item_id as item_id1_9_1_, polys0_.anyContainer_owner_id as anyConta2_9_1_, polys0_.anyContainer_owner_owner_oid as anyConta3_9_1_, polys0_.orig as orig4_9_1_, polys0_.norm as norm5_9_1_ from m_assignment_ext_poly polys0_ where polys0_.anyContainer_owner_id=? and polys0_.anyContainer_owner_owner_oid=?
 [1] select references0_.anyContainer_owner_id as anyConta2_10_0_, references0_.anyContainer_owner_owner_oid as anyConta3_10_0_, references0_.item_id as item_id1_10_0_, references0_.targetoid as targetoi4_10_0_, references0_.item_id as item_id1_10_1_, references0_.anyContainer_owner_id as anyConta2_10_1_, references0_.anyContainer_owner_owner_oid as anyConta3_10_1_, references0_.targetoid as targetoi4_10_1_, references0_.relation as relation5_10_1_, references0_.targetType as targetTy6_10_1_ from m_assignment_ext_reference references0_ where references0_.anyContainer_owner_id=? and references0_.anyContainer_owner_owner_oid=?
 [1] select strings0_.anyContainer_owner_id as anyConta2_11_0_, strings0_.anyContainer_owner_owner_oid as anyConta3_11_0_, strings0_.item_id as item_id1_11_0_, strings0_.stringValue as stringVa4_11_0_, strings0_.item_id as item_id1_11_1_, strings0_.anyContainer_owner_id as anyConta2_11_1_, strings0_.anyContainer_owner_owner_oid as anyConta3_11_1_, strings0_.stringValue as stringVa4_11_1_ from m_assignment_ext_string strings0_ where strings0_.anyContainer_owner_id=? and strings0_.anyContainer_owner_owner_oid=?
 [1] update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
 [1] delete from m_assignment_policy_situation where assignment_id=? and assignment_oid=?
 [1] delete from m_assignment where id=? and owner_oid=?
 [3] delete from m_assignment_ext_string where item_id=? and anyContainer_owner_id=? and anyContainer_owner_owner_oid=? and stringValue=?
 [1] delete from m_assignment_extension where owner_id=? and owner_owner_oid=?
          */
        assertCounts(16, 18);
    }

    @Test
    public void test175ReplaceAssignments() throws Exception {
        OperationResult result = new OperationResult(ExtensionTest.class.getName() + ".test175ReplaceAssignments");

        AssignmentType newAssignment = new AssignmentType(prismContext)
                .id(999L)
                .targetRef("999999aaaaa", OrgType.COMPLEX_TYPE);
        PrismContainerValue<?> newExtValue = newAssignment.asPrismContainerValue()
                .findOrCreateContainer(AssignmentType.F_EXTENSION)
                .getValue();
        newExtValue.findOrCreateProperty(EXT_WEAPON).addRealValues("W4.1", "W4.2");
        newExtValue.findOrCreateProperty(EXT_SHIP_NAME).addRealValues("ship4");

        ObjectDelta<UserType> delta = getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT)
                .replace(newAssignment)
                .asObjectDelta("");
        delta.applyTo(expectedUser);

        queryListener.start();
        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), getOptions(), result);
        queryListener.dumpAndStop();

        try (EntityManager em = factory.createEntityManager()) {
            RUser u = em.find(RUser.class, userOid);
            assertExtension(u, itemHidden1, "h1.1", "h1.2");
            assertExtension(u, itemHidden2);
            assertExtension(u, itemHidden3);
            assertExtension(u, itemVisible, "v1", "v2", "v3");
            assertExtension(u, itemWeapon);
            assertEquals("Wrong # of assignments", 1, u.getAssignments().size());
            RAssignment a = find(u.getAssignments(), 999);
            assertExtension(a, itemWeapon, "W4.1", "W4.2");
            assertExtension(a, itemShipName, "ship4");
        }

        assertGetObject(result);

        /*
 [1] select oid from m_object where oid = ? for update
 [1] select ruser0_.oid as oid1_27_, ruser0_2_.createChannel as createCh2_27_, ruser0_2_.createTimestamp as createTi3_27_, ruser0_2_.creatorRef_relation as creatorR4_27_, ruser0_2_.creatorRef_targetOid as creatorR5_27_, ruser0_2_.creatorRef_type as creatorR6_27_, ruser0_2_.fullObject as fullObje7_27_, ruser0_2_.lifecycleState as lifecycl8_27_, ruser0_2_.modifierRef_relation as modifier9_27_, ruser0_2_.modifierRef_targetOid as modifie10_27_, ruser0_2_.modifierRef_type as modifie11_27_, ruser0_2_.modifyChannel as modifyC12_27_, ruser0_2_.modifyTimestamp as modifyT13_27_, ruser0_2_.name_norm as name_no14_27_, ruser0_2_.name_orig as name_or15_27_, ruser0_2_.objectTypeClass as objectT16_27_, ruser0_2_.tenantRef_relation as tenantR17_27_, ruser0_2_.tenantRef_targetOid as tenantR18_27_, ruser0_2_.tenantRef_type as tenantR19_27_, ruser0_2_.version as version20_27_, ruser0_1_.administrativeStatus as administ1_53_, ruser0_1_.archiveTimestamp as archiveT2_53_, ruser0_1_.disableReason as disableR3_53_, ruser0_1_.disableTimestamp as disableT4_53_, ruser0_1_.effectiveStatus as effectiv5_53_, ruser0_1_.enableTimestamp as enableTi6_53_, ruser0_1_.validFrom as validFro7_53_, ruser0_1_.validTo as validTo8_53_, ruser0_1_.validityChangeTimestamp as validity9_53_, ruser0_1_.validityStatus as validit10_53_, ruser0_1_.costCenter as costCen11_53_, ruser0_1_.emailAddress as emailAd12_53_, ruser0_1_.hasPhoto as hasPhot13_53_, ruser0_1_.locale as locale14_53_, ruser0_1_.locality_norm as localit15_53_, ruser0_1_.locality_orig as localit16_53_, ruser0_1_.preferredLanguage as preferr17_53_, ruser0_1_.telephoneNumber as telepho18_53_, ruser0_1_.timezone as timezon19_53_, ruser0_.additionalName_norm as addition1_73_, ruser0_.additionalName_orig as addition2_73_, ruser0_.employeeNumber as employee3_73_, ruser0_.familyName_norm as familyNa4_73_, ruser0_.familyName_orig as familyNa5_73_, ruser0_.fullName_norm as fullName6_73_, ruser0_.fullName_orig as fullName7_73_, ruser0_.givenName_norm as givenNam8_73_, ruser0_.givenName_orig as givenNam9_73_, ruser0_.honorificPrefix_norm as honorif10_73_, ruser0_.honorificPrefix_orig as honorif11_73_, ruser0_.honorificSuffix_norm as honorif12_73_, ruser0_.honorificSuffix_orig as honorif13_73_, ruser0_.name_norm as name_no14_73_, ruser0_.name_orig as name_or15_73_, ruser0_.nickName_norm as nickNam16_73_, ruser0_.nickName_orig as nickNam17_73_, ruser0_.title_norm as title_n18_73_, ruser0_.title_orig as title_o19_73_ from m_user ruser0_ inner join m_focus ruser0_1_ on ruser0_.oid=ruser0_1_.oid inner join m_object ruser0_2_ on ruser0_.oid=ruser0_2_.oid where ruser0_.oid=?
 [1] select assignment0_.owner_oid as owner_oi2_5_0_, assignment0_.id as id1_5_0_, assignment0_.id as id1_5_1_, assignment0_.owner_oid as owner_oi2_5_1_, assignment0_.administrativeStatus as administ3_5_1_, assignment0_.archiveTimestamp as archiveT4_5_1_, assignment0_.disableReason as disableR5_5_1_, assignment0_.disableTimestamp as disableT6_5_1_, assignment0_.effectiveStatus as effectiv7_5_1_, assignment0_.enableTimestamp as enableTi8_5_1_, assignment0_.validFrom as validFro9_5_1_, assignment0_.validTo as validTo10_5_1_, assignment0_.validityChangeTimestamp as validit11_5_1_, assignment0_.validityStatus as validit12_5_1_, assignment0_.assignmentOwner as assignm13_5_1_, assignment0_.createChannel as createC14_5_1_, assignment0_.createTimestamp as createT15_5_1_, assignment0_.creatorRef_relation as creator16_5_1_, assignment0_.creatorRef_targetOid as creator17_5_1_, assignment0_.creatorRef_type as creator18_5_1_, assignment0_.extId as extId38_5_1_, assignment0_.extOid as extOid39_5_1_, assignment0_.lifecycleState as lifecyc19_5_1_, assignment0_.modifierRef_relation as modifie20_5_1_, assignment0_.modifierRef_targetOid as modifie21_5_1_, assignment0_.modifierRef_type as modifie22_5_1_, assignment0_.modifyChannel as modifyC23_5_1_, assignment0_.modifyTimestamp as modifyT24_5_1_, assignment0_.orderValue as orderVa25_5_1_, assignment0_.orgRef_relation as orgRef_26_5_1_, assignment0_.orgRef_targetOid as orgRef_27_5_1_, assignment0_.orgRef_type as orgRef_28_5_1_, assignment0_.resourceRef_relation as resourc29_5_1_, assignment0_.resourceRef_targetOid as resourc30_5_1_, assignment0_.resourceRef_type as resourc31_5_1_, assignment0_.targetRef_relation as targetR32_5_1_, assignment0_.targetRef_targetOid as targetR33_5_1_, assignment0_.targetRef_type as targetR34_5_1_, assignment0_.tenantRef_relation as tenantR35_5_1_, assignment0_.tenantRef_targetOid as tenantR36_5_1_, assignment0_.tenantRef_type as tenantR37_5_1_, rassignmen1_.owner_id as owner_id1_12_2_, rassignmen1_.owner_owner_oid as owner_ow2_12_2_ from m_assignment assignment0_ left outer join m_assignment_extension rassignmen1_ on assignment0_.extId=rassignmen1_.owner_id and assignment0_.extOid=rassignmen1_.owner_owner_oid where assignment0_.owner_oid=?
 [1] select dates0_.anyContainer_owner_id as anyConta2_7_0_, dates0_.anyContainer_owner_owner_oid as anyConta3_7_0_, dates0_.item_id as item_id1_7_0_, dates0_.dateValue as dateValu4_7_0_, dates0_.item_id as item_id1_7_1_, dates0_.anyContainer_owner_id as anyConta2_7_1_, dates0_.anyContainer_owner_owner_oid as anyConta3_7_1_, dates0_.dateValue as dateValu4_7_1_ from m_assignment_ext_date dates0_ where dates0_.anyContainer_owner_id=? and dates0_.anyContainer_owner_owner_oid=?
 [1] select dates0_.anyContainer_owner_id as anyConta2_7_0_, dates0_.anyContainer_owner_owner_oid as anyConta3_7_0_, dates0_.item_id as item_id1_7_0_, dates0_.dateValue as dateValu4_7_0_, dates0_.item_id as item_id1_7_1_, dates0_.anyContainer_owner_id as anyConta2_7_1_, dates0_.anyContainer_owner_owner_oid as anyConta3_7_1_, dates0_.dateValue as dateValu4_7_1_ from m_assignment_ext_date dates0_ where dates0_.anyContainer_owner_id=? and dates0_.anyContainer_owner_owner_oid=?
 [1] select longs0_.anyContainer_owner_id as anyConta2_8_0_, longs0_.anyContainer_owner_owner_oid as anyConta3_8_0_, longs0_.item_id as item_id1_8_0_, longs0_.longValue as longValu4_8_0_, longs0_.item_id as item_id1_8_1_, longs0_.anyContainer_owner_id as anyConta2_8_1_, longs0_.anyContainer_owner_owner_oid as anyConta3_8_1_, longs0_.longValue as longValu4_8_1_ from m_assignment_ext_long longs0_ where longs0_.anyContainer_owner_id=? and longs0_.anyContainer_owner_owner_oid=?
 [1] select longs0_.anyContainer_owner_id as anyConta2_8_0_, longs0_.anyContainer_owner_owner_oid as anyConta3_8_0_, longs0_.item_id as item_id1_8_0_, longs0_.longValue as longValu4_8_0_, longs0_.item_id as item_id1_8_1_, longs0_.anyContainer_owner_id as anyConta2_8_1_, longs0_.anyContainer_owner_owner_oid as anyConta3_8_1_, longs0_.longValue as longValu4_8_1_ from m_assignment_ext_long longs0_ where longs0_.anyContainer_owner_id=? and longs0_.anyContainer_owner_owner_oid=?
 [1] select polys0_.anyContainer_owner_id as anyConta2_9_0_, polys0_.anyContainer_owner_owner_oid as anyConta3_9_0_, polys0_.item_id as item_id1_9_0_, polys0_.orig as orig4_9_0_, polys0_.item_id as item_id1_9_1_, polys0_.anyContainer_owner_id as anyConta2_9_1_, polys0_.anyContainer_owner_owner_oid as anyConta3_9_1_, polys0_.orig as orig4_9_1_, polys0_.norm as norm5_9_1_ from m_assignment_ext_poly polys0_ where polys0_.anyContainer_owner_id=? and polys0_.anyContainer_owner_owner_oid=?
 [1] select polys0_.anyContainer_owner_id as anyConta2_9_0_, polys0_.anyContainer_owner_owner_oid as anyConta3_9_0_, polys0_.item_id as item_id1_9_0_, polys0_.orig as orig4_9_0_, polys0_.item_id as item_id1_9_1_, polys0_.anyContainer_owner_id as anyConta2_9_1_, polys0_.anyContainer_owner_owner_oid as anyConta3_9_1_, polys0_.orig as orig4_9_1_, polys0_.norm as norm5_9_1_ from m_assignment_ext_poly polys0_ where polys0_.anyContainer_owner_id=? and polys0_.anyContainer_owner_owner_oid=?
 [1] select references0_.anyContainer_owner_id as anyConta2_10_0_, references0_.anyContainer_owner_owner_oid as anyConta3_10_0_, references0_.item_id as item_id1_10_0_, references0_.targetoid as targetoi4_10_0_, references0_.item_id as item_id1_10_1_, references0_.anyContainer_owner_id as anyConta2_10_1_, references0_.anyContainer_owner_owner_oid as anyConta3_10_1_, references0_.targetoid as targetoi4_10_1_, references0_.relation as relation5_10_1_, references0_.targetType as targetTy6_10_1_ from m_assignment_ext_reference references0_ where references0_.anyContainer_owner_id=? and references0_.anyContainer_owner_owner_oid=?
 [1] select references0_.anyContainer_owner_id as anyConta2_10_0_, references0_.anyContainer_owner_owner_oid as anyConta3_10_0_, references0_.item_id as item_id1_10_0_, references0_.targetoid as targetoi4_10_0_, references0_.item_id as item_id1_10_1_, references0_.anyContainer_owner_id as anyConta2_10_1_, references0_.anyContainer_owner_owner_oid as anyConta3_10_1_, references0_.targetoid as targetoi4_10_1_, references0_.relation as relation5_10_1_, references0_.targetType as targetTy6_10_1_ from m_assignment_ext_reference references0_ where references0_.anyContainer_owner_id=? and references0_.anyContainer_owner_owner_oid=?
 [1] select strings0_.anyContainer_owner_id as anyConta2_11_0_, strings0_.anyContainer_owner_owner_oid as anyConta3_11_0_, strings0_.item_id as item_id1_11_0_, strings0_.stringValue as stringVa4_11_0_, strings0_.item_id as item_id1_11_1_, strings0_.anyContainer_owner_id as anyConta2_11_1_, strings0_.anyContainer_owner_owner_oid as anyConta3_11_1_, strings0_.stringValue as stringVa4_11_1_ from m_assignment_ext_string strings0_ where strings0_.anyContainer_owner_id=? and strings0_.anyContainer_owner_owner_oid=?
 [1] select strings0_.anyContainer_owner_id as anyConta2_11_0_, strings0_.anyContainer_owner_owner_oid as anyConta3_11_0_, strings0_.item_id as item_id1_11_0_, strings0_.stringValue as stringVa4_11_0_, strings0_.item_id as item_id1_11_1_, strings0_.anyContainer_owner_id as anyConta2_11_1_, strings0_.anyContainer_owner_owner_oid as anyConta3_11_1_, strings0_.stringValue as stringVa4_11_1_ from m_assignment_ext_string strings0_ where strings0_.anyContainer_owner_id=? and strings0_.anyContainer_owner_owner_oid=?
 [1] select rassignmen_.owner_id, rassignmen_.owner_owner_oid from m_assignment_extension rassignmen_ where rassignmen_.owner_id=? and rassignmen_.owner_owner_oid=?
 [1] select raextstrin_.item_id, raextstrin_.anyContainer_owner_id, raextstrin_.anyContainer_owner_owner_oid, raextstrin_.stringValue from m_assignment_ext_string raextstrin_ where raextstrin_.item_id=? and raextstrin_.anyContainer_owner_id=? and raextstrin_.anyContainer_owner_owner_oid=? and raextstrin_.stringValue=?
 [1] select raextstrin_.item_id, raextstrin_.anyContainer_owner_id, raextstrin_.anyContainer_owner_owner_oid, raextstrin_.stringValue from m_assignment_ext_string raextstrin_ where raextstrin_.item_id=? and raextstrin_.anyContainer_owner_id=? and raextstrin_.anyContainer_owner_owner_oid=? and raextstrin_.stringValue=?
 [1] select raextstrin_.item_id, raextstrin_.anyContainer_owner_id, raextstrin_.anyContainer_owner_owner_oid, raextstrin_.stringValue from m_assignment_ext_string raextstrin_ where raextstrin_.item_id=? and raextstrin_.anyContainer_owner_id=? and raextstrin_.anyContainer_owner_owner_oid=? and raextstrin_.stringValue=?
 [1] select createappr0_.owner_id as owner_id1_14_0_, createappr0_.owner_owner_oid as owner_ow2_14_0_, createappr0_.reference_type as referenc3_14_0_, createappr0_.relation as relation4_14_0_, createappr0_.targetOid as targetOi5_14_0_, createappr0_.owner_id as owner_id1_14_1_, createappr0_.owner_owner_oid as owner_ow2_14_1_, createappr0_.reference_type as referenc3_14_1_, createappr0_.relation as relation4_14_1_, createappr0_.targetOid as targetOi5_14_1_, createappr0_.targetType as targetTy6_14_1_ from m_assignment_reference createappr0_ where ( createappr0_.reference_type= 0) and createappr0_.owner_id=? and createappr0_.owner_owner_oid=?
 [1] select modifyappr0_.owner_id as owner_id1_14_0_, modifyappr0_.owner_owner_oid as owner_ow2_14_0_, modifyappr0_.reference_type as referenc3_14_0_, modifyappr0_.relation as relation4_14_0_, modifyappr0_.targetOid as targetOi5_14_0_, modifyappr0_.owner_id as owner_id1_14_1_, modifyappr0_.owner_owner_oid as owner_ow2_14_1_, modifyappr0_.reference_type as referenc3_14_1_, modifyappr0_.relation as relation4_14_1_, modifyappr0_.targetOid as targetOi5_14_1_, modifyappr0_.targetType as targetTy6_14_1_ from m_assignment_reference modifyappr0_ where ( modifyappr0_.reference_type= 1) and modifyappr0_.owner_id=? and modifyappr0_.owner_owner_oid=?
 [1] select booleans0_.anyContainer_owner_id as anyConta2_6_0_, booleans0_.anyContainer_owner_owner_oid as anyConta3_6_0_, booleans0_.item_id as item_id1_6_0_, booleans0_.booleanValue as booleanV4_6_0_, booleans0_.item_id as item_id1_6_1_, booleans0_.anyContainer_owner_id as anyConta2_6_1_, booleans0_.anyContainer_owner_owner_oid as anyConta3_6_1_, booleans0_.booleanValue as booleanV4_6_1_ from m_assignment_ext_boolean booleans0_ where booleans0_.anyContainer_owner_id=? and booleans0_.anyContainer_owner_owner_oid=?
 [1] select createappr0_.owner_id as owner_id1_14_0_, createappr0_.owner_owner_oid as owner_ow2_14_0_, createappr0_.reference_type as referenc3_14_0_, createappr0_.relation as relation4_14_0_, createappr0_.targetOid as targetOi5_14_0_, createappr0_.owner_id as owner_id1_14_1_, createappr0_.owner_owner_oid as owner_ow2_14_1_, createappr0_.reference_type as referenc3_14_1_, createappr0_.relation as relation4_14_1_, createappr0_.targetOid as targetOi5_14_1_, createappr0_.targetType as targetTy6_14_1_ from m_assignment_reference createappr0_ where ( createappr0_.reference_type= 0) and createappr0_.owner_id=? and createappr0_.owner_owner_oid=?
 [1] select modifyappr0_.owner_id as owner_id1_14_0_, modifyappr0_.owner_owner_oid as owner_ow2_14_0_, modifyappr0_.reference_type as referenc3_14_0_, modifyappr0_.relation as relation4_14_0_, modifyappr0_.targetOid as targetOi5_14_0_, modifyappr0_.owner_id as owner_id1_14_1_, modifyappr0_.owner_owner_oid as owner_ow2_14_1_, modifyappr0_.reference_type as referenc3_14_1_, modifyappr0_.relation as relation4_14_1_, modifyappr0_.targetOid as targetOi5_14_1_, modifyappr0_.targetType as targetTy6_14_1_ from m_assignment_reference modifyappr0_ where ( modifyappr0_.reference_type= 1) and modifyappr0_.owner_id=? and modifyappr0_.owner_owner_oid=?
 [1] select booleans0_.anyContainer_owner_id as anyConta2_6_0_, booleans0_.anyContainer_owner_owner_oid as anyConta3_6_0_, booleans0_.item_id as item_id1_6_0_, booleans0_.booleanValue as booleanV4_6_0_, booleans0_.item_id as item_id1_6_1_, booleans0_.anyContainer_owner_id as anyConta2_6_1_, booleans0_.anyContainer_owner_owner_oid as anyConta3_6_1_, booleans0_.booleanValue as booleanV4_6_1_ from m_assignment_ext_boolean booleans0_ where booleans0_.anyContainer_owner_id=? and booleans0_.anyContainer_owner_owner_oid=?
 [1] insert into m_assignment (administrativeStatus, archiveTimestamp, disableReason, disableTimestamp, effectiveStatus, enableTimestamp, validFrom, validTo, validityChangeTimestamp, validityStatus, assignmentOwner, createChannel, createTimestamp, creatorRef_relation, creatorRef_targetOid, creatorRef_type, extId, extOid, lifecycleState, modifierRef_relation, modifierRef_targetOid, modifierRef_type, modifyChannel, modifyTimestamp, orderValue, orgRef_relation, orgRef_targetOid, orgRef_type, resourceRef_relation, resourceRef_targetOid, resourceRef_type, targetRef_relation, targetRef_targetOid, targetRef_type, tenantRef_relation, tenantRef_targetOid, tenantRef_type, id, owner_oid) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
 [1] insert into m_assignment_extension (owner_id, owner_owner_oid) values (?, ?)
 [3] insert into m_assignment_ext_string (item_id, anyContainer_owner_id, anyContainer_owner_owner_oid, stringValue) values (?, ?, ?, ?)
 [1] update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
 [1] update m_assignment set administrativeStatus=?, archiveTimestamp=?, disableReason=?, disableTimestamp=?, effectiveStatus=?, enableTimestamp=?, validFrom=?, validTo=?, validityChangeTimestamp=?, validityStatus=?, assignmentOwner=?, createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, extId=?, extOid=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, orderValue=?, orgRef_relation=?, orgRef_targetOid=?, orgRef_type=?, resourceRef_relation=?, resourceRef_targetOid=?, resourceRef_type=?, targetRef_relation=?, targetRef_targetOid=?, targetRef_type=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=? where id=? and owner_oid=?
 [2] delete from m_assignment_policy_situation where assignment_id=? and assignment_oid=?
 [1] delete from m_assignment where id=? and owner_oid=?
 [3] delete from m_assignment_ext_string where item_id=? and anyContainer_owner_id=? and anyContainer_owner_owner_oid=? and stringValue=?
 [1] delete from m_assignment_extension where owner_id=? and owner_owner_oid=?
 [1] delete from m_assignment where id=? and owner_oid=?
 [1] delete from m_assignment_extension where owner_id=? and owner_owner_oid=?
         */
        // TODO clean up this mess
        assertCounts(34, 39);
    }

    private RAssignment find(Set<RAssignment> assignments, int id) {
        RAssignment rv = assignments.stream()
                .filter(a -> a.getId() == id)
                .findFirst().orElse(null);
        assertNotNull("No assignment #" + id, rv);
        return rv;
    }

    private void assertGetObjectNoInclude(String oid, PrismObject<UserType> expected, OperationResult result) throws SchemaException,
            ObjectNotFoundException {
        checkObject(oid, expected, false, null, result);
    }

    // toInclude == null means "ALL"
    private void assertGetObjectInclude(String oid, Collection<ItemName> toInclude, PrismObject<UserType> expected, OperationResult result) throws SchemaException,
            ObjectNotFoundException {
        checkObject(oid, expected, true, toInclude, result);
    }

    private void checkObject(String oid, PrismObject<UserType> expected, boolean include, Collection<ItemName> toInclude, OperationResult result) throws ObjectNotFoundException, SchemaException {
        PrismObject<UserType> expectedAdapted;
        if (include && toInclude == null) {
            expectedAdapted = expected;
        } else {
            expectedAdapted = expected.clone();
            PrismContainer<?> extension = expectedAdapted.getExtension();
            if (extension != null) {
                if (!include || !toInclude.contains(EXT_HIDDEN1)) {
                    extension.getValue().removeProperty(EXT_HIDDEN1);
                }
                if (!include || !toInclude.contains(EXT_HIDDEN2)) {
                    extension.getValue().removeProperty(EXT_HIDDEN2);
                }
                if (!include || !toInclude.contains(EXT_HIDDEN3)) {
                    extension.getValue().removeProperty(EXT_HIDDEN3);
                }
                if (extension.isEmpty()) {
                    expectedAdapted.getValue().removeContainer(UserType.F_EXTENSION);
                }
            }
        }

        Collection<SelectorOptions<GetOperationOptions>> options;
        if (include) {
            if (toInclude == null) {
                options = Collections.singletonList(
                        SelectorOptions.create(GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE)));
            } else {
                ItemPath[] paths = toInclude.stream()
                        .map(name -> ItemPath.create(UserType.F_EXTENSION, name)).toArray(ItemPath[]::new);
                options = schemaService.getOperationOptionsBuilder().items((Object[]) paths).retrieve().build();
            }
        } else {
            options = null;
        }
        PrismObject<UserType> real = repositoryService.getObject(UserType.class, oid, options, result);
        ObjectDelta<UserType> delta = expectedAdapted.diff(real);
        if (verbose) {
            System.out.println("Expected object = \n" + expectedAdapted.debugDump());
            System.out.println("Real object in repo = \n" + real.debugDump());
            System.out.println("Difference = \n" + delta.debugDump());
        }
        if (!delta.isEmpty()) {
            fail("Objects are not equal with include=" + include + ", toInclude=" + toInclude + ":\n*** Expected:\n" +
                    expectedAdapted.debugDump() + "\n*** Got:\n" + real.debugDump() + "\n*** Delta:\n" + delta.debugDump());
        }
    }

    // This is ad-hoc test moved here from ObjectDeltaUpdaterTest
    @Test
    public void test199ReplaceAssignmentExtension() throws Exception {
        OperationResult result = new OperationResult("test199ReplaceAssignmentExtension");

        String file = FOLDER_BASE + "/modify/user-with-assignment-extension.xml";
        PrismObject<UserType> user = prismContext.parseObject(new File(file));
        user.setOid(null);
        repositoryService.addObject(user, null, result);
        String userOid = user.getOid();

        QName SHIP_NAME_QNAME = new QName("http://example.com/p", "shipName");
        PrismPropertyDefinition<String> def1 = prismContext.definitionFactory().createPropertyDefinition(SHIP_NAME_QNAME, DOMUtil.XSD_STRING);
        ExtensionType extension = new ExtensionType(prismContext);
        PrismProperty<String> loot = def1.instantiate();
        loot.setRealValue("otherString");
        extension.asPrismContainerValue().add(loot);

        List<ItemDelta<?, ?>> deltas = prismContext.deltaFor(UserType.class)
                .item(ItemPath.create(UserType.F_ASSIGNMENT, 1, AssignmentType.F_EXTENSION))
                .replace(extension)
                .asItemDeltas();

        repositoryService.modifyObject(UserType.class, userOid, deltas, getOptions(), result);

        ObjectQuery query = prismContext.queryFor(UserType.class)
                .item(ItemPath.create(UserType.F_ASSIGNMENT, AssignmentType.F_EXTENSION, SHIP_NAME_QNAME), def1).eq("otherString")
                .build();
        List list = repositoryService.searchObjects(UserType.class, query, null, result);
        assertEquals("Wrong # of query1 results", 1, list.size());

        EntityManager em = open();
        RUser ruser = (RUser) em.createQuery("from RUser where oid = :o").setParameter("o", user.getOid()).getSingleResult();
        RAssignmentExtension ext = ruser.getAssignments().iterator().next().getExtension();
        assertEquals(1, ext.getStrings().size());
        close(em);

        // delete
        deltas = prismContext.deltaFor(UserType.class)
                .item(ItemPath.create(UserType.F_ASSIGNMENT, 1, AssignmentType.F_EXTENSION))
                .delete(extension.asPrismContainerValue().clone())
                .asItemDeltas();
        repositoryService.modifyObject(UserType.class, userOid, deltas, getOptions(), result);

        em = open();
        ruser = (RUser) em.createQuery("from RUser where oid = :o").setParameter("o", user.getOid()).getSingleResult();
        ext = ruser.getAssignments().iterator().next().getExtension();
        assertEquals(0, ext.getStrings().size());
        close(em);

        // add
        deltas = prismContext.deltaFor(UserType.class)
                .item(ItemPath.create(UserType.F_ASSIGNMENT, 1, AssignmentType.F_EXTENSION))
                .add(extension.asPrismContainerValue().clone())
                .asItemDeltas();
        repositoryService.modifyObject(UserType.class, userOid, deltas, getOptions(), result);

        em = open();
        ruser = (RUser) em.createQuery("from RUser where oid = :o").setParameter("o", user.getOid()).getSingleResult();
        ext = ruser.getAssignments().iterator().next().getExtension();
        assertEquals(1, ext.getStrings().size());
        close(em);
    }

    @Test
    public void test410AddShadow() throws Exception {
        OperationResult result = new OperationResult(ExtensionTest.class.getName() + ".test410AddShadow");

        expectedShadow = shadowDefinition.instantiate();
        expectedShadow.asObjectable().name("alumni");
        PrismContainer<?> attributesContainer = shadowAttributesDefinition.instantiate();
        expectedShadow.getValue().add(attributesContainer);
        PrismContainerValue<?> attributes = attributesContainer.getValue();
        ResourceAttribute<String> name = attrGroupNameDefinition.instantiate();
        name.setRealValue("alumni");
        attributes.add(name);
        ResourceAttribute<String> member = attrMemberDefinition.instantiate();
        member.addRealValues("banderson", "kwhite");
        attributes.add(member);
        ResourceAttribute<String> manager = attrManagerDefinition.instantiate();
        manager.addRealValues("jack", "jim");
        attributes.add(manager);

        queryListener.start();
        shadowOid = repositoryService.addObject(expectedShadow.clone(), null, result);
        queryListener.dumpAndStop();

        try (EntityManager em = factory.createEntityManager()) {
            RShadow s = em.find(RShadow.class, shadowOid);
            assertExtension(s, itemGroupName, "alumni");
            assertExtension(s, itemMember, "banderson", "kwhite");
            assertExtension(s, itemManager, "jack", "jim");
        }

        assertGetShadow(result);

        /*
 [1] insert into m_object (createChannel, createTimestamp, creatorRef_relation, creatorRef_targetOid, creatorRef_type, fullObject, lifecycleState, modifierRef_relation, modifierRef_targetOid, modifierRef_type, modifyChannel, modifyTimestamp, name_norm, name_orig, objectTypeClass, tenantRef_relation, tenantRef_targetOid, tenantRef_type, version, oid) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
 [1] insert into m_shadow (attemptNumber, dead, exist, failedOperationType, fullSynchronizationTimestamp, intent, kind, name_norm, name_orig, objectClass, pendingOperationCount, primaryIdentifierValue, resourceRef_relation, resourceRef_targetOid, resourceRef_type, status, synchronizationSituation, synchronizationTimestamp, oid) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
 [5] insert into m_object_ext_string (item_id, owner_oid, ownerType, stringValue) values (?, ?, ?, ?)
         */
        assertCounts(3, 7);
    }

    @Test
    public void test420AddVisibleAttributeValues() throws Exception {
        OperationResult result = new OperationResult(ExtensionTest.class.getName() + ".test420AddVisibleAttributeValues");

        ObjectDelta<ShadowType> delta = getPrismContext().deltaFor(ShadowType.class)
                .item(ItemPath.create(ShadowType.F_ATTRIBUTES, ATTR_MANAGER), attrManagerDefinition)
                .add("alice", "bob")
                .asObjectDelta("");
        delta.applyTo(expectedShadow);

        queryListener.start();
        repositoryService.modifyObject(ShadowType.class, shadowOid, delta.getModifications(), getOptions(), result);
        queryListener.dumpAndStop();

        try (EntityManager em = factory.createEntityManager()) {
            RShadow s = em.find(RShadow.class, shadowOid);
            assertExtension(s, itemGroupName, "alumni");
            assertExtension(s, itemMember, "banderson", "kwhite");
            assertExtension(s, itemManager, "jack", "jim", "alice", "bob");
        }

        assertGetShadow(result);

        /*
 [1] select oid from m_object where oid = ? for update
 [1] select rshadow0_.oid as oid1_27_, rshadow0_1_.createChannel as createCh2_27_, rshadow0_1_.createTimestamp as createTi3_27_, rshadow0_1_.creatorRef_relation as creatorR4_27_, rshadow0_1_.creatorRef_targetOid as creatorR5_27_, rshadow0_1_.creatorRef_type as creatorR6_27_, rshadow0_1_.fullObject as fullObje7_27_, rshadow0_1_.lifecycleState as lifecycl8_27_, rshadow0_1_.modifierRef_relation as modifier9_27_, rshadow0_1_.modifierRef_targetOid as modifie10_27_, rshadow0_1_.modifierRef_type as modifie11_27_, rshadow0_1_.modifyChannel as modifyC12_27_, rshadow0_1_.modifyTimestamp as modifyT13_27_, rshadow0_1_.name_norm as name_no14_27_, rshadow0_1_.name_orig as name_or15_27_, rshadow0_1_.objectTypeClass as objectT16_27_, rshadow0_1_.tenantRef_relation as tenantR17_27_, rshadow0_1_.tenantRef_targetOid as tenantR18_27_, rshadow0_1_.tenantRef_type as tenantR19_27_, rshadow0_1_.version as version20_27_, rshadow0_.attemptNumber as attemptN1_41_, rshadow0_.dead as dead2_41_, rshadow0_.exist as exist3_41_, rshadow0_.failedOperationType as failedOp4_41_, rshadow0_.fullSynchronizationTimestamp as fullSync5_41_, rshadow0_.intent as intent6_41_, rshadow0_.kind as kind7_41_, rshadow0_.name_norm as name_nor8_41_, rshadow0_.name_orig as name_ori9_41_, rshadow0_.objectClass as objectC10_41_, rshadow0_.pendingOperationCount as pending11_41_, rshadow0_.primaryIdentifierValue as primary12_41_, rshadow0_.resourceRef_relation as resourc13_41_, rshadow0_.resourceRef_targetOid as resourc14_41_, rshadow0_.resourceRef_type as resourc15_41_, rshadow0_.status as status16_41_, rshadow0_.synchronizationSituation as synchro17_41_, rshadow0_.synchronizationTimestamp as synchro18_41_ from m_shadow rshadow0_ inner join m_object rshadow0_1_ on rshadow0_.oid=rshadow0_1_.oid where rshadow0_.oid=?
 [2] insert into m_object_ext_string (item_id, owner_oid, ownerType, stringValue) values (?, ?, ?, ?)
 [1] update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?

        safe insertions:

 [1] select oid from m_object where oid = ? for update
 [1] select rshadow0_.oid as oid1_27_, rshadow0_1_.createChannel as createCh2_27_, rshadow0_1_.createTimestamp as createTi3_27_, rshadow0_1_.creatorRef_relation as creatorR4_27_, rshadow0_1_.creatorRef_targetOid as creatorR5_27_, rshadow0_1_.creatorRef_type as creatorR6_27_, rshadow0_1_.fullObject as fullObje7_27_, rshadow0_1_.lifecycleState as lifecycl8_27_, rshadow0_1_.modifierRef_relation as modifier9_27_, rshadow0_1_.modifierRef_targetOid as modifie10_27_, rshadow0_1_.modifierRef_type as modifie11_27_, rshadow0_1_.modifyChannel as modifyC12_27_, rshadow0_1_.modifyTimestamp as modifyT13_27_, rshadow0_1_.name_norm as name_no14_27_, rshadow0_1_.name_orig as name_or15_27_, rshadow0_1_.objectTypeClass as objectT16_27_, rshadow0_1_.tenantRef_relation as tenantR17_27_, rshadow0_1_.tenantRef_targetOid as tenantR18_27_, rshadow0_1_.tenantRef_type as tenantR19_27_, rshadow0_1_.version as version20_27_, rshadow0_.attemptNumber as attemptN1_41_, rshadow0_.dead as dead2_41_, rshadow0_.exist as exist3_41_, rshadow0_.failedOperationType as failedOp4_41_, rshadow0_.fullSynchronizationTimestamp as fullSync5_41_, rshadow0_.intent as intent6_41_, rshadow0_.kind as kind7_41_, rshadow0_.name_norm as name_nor8_41_, rshadow0_.name_orig as name_ori9_41_, rshadow0_.objectClass as objectC10_41_, rshadow0_.pendingOperationCount as pending11_41_, rshadow0_.primaryIdentifierValue as primary12_41_, rshadow0_.resourceRef_relation as resourc13_41_, rshadow0_.resourceRef_targetOid as resourc14_41_, rshadow0_.resourceRef_type as resourc15_41_, rshadow0_.status as status16_41_, rshadow0_.synchronizationSituation as synchro17_41_, rshadow0_.synchronizationTimestamp as synchro18_41_ from m_shadow rshadow0_ inner join m_object rshadow0_1_ on rshadow0_.oid=rshadow0_1_.oid where rshadow0_.oid=?
 [1] select roextstrin0_.item_id as item_id1_33_0_, roextstrin0_.owner_oid as owner_oi2_33_0_, roextstrin0_.ownerType as ownerTyp3_33_0_, roextstrin0_.stringValue as stringVa4_33_0_ from m_object_ext_string roextstrin0_ where roextstrin0_.item_id=? and roextstrin0_.owner_oid=? and roextstrin0_.ownerType=? and roextstrin0_.stringValue=?
 [1] select roextstrin0_.item_id as item_id1_33_0_, roextstrin0_.owner_oid as owner_oi2_33_0_, roextstrin0_.ownerType as ownerTyp3_33_0_, roextstrin0_.stringValue as stringVa4_33_0_ from m_object_ext_string roextstrin0_ where roextstrin0_.item_id=? and roextstrin0_.owner_oid=? and roextstrin0_.ownerType=? and roextstrin0_.stringValue=?
 [2] insert into m_object_ext_string (item_id, owner_oid, ownerType, stringValue) values (?, ?, ?, ?)
 [1] update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
         */
        assertCounts(4 + getExtraSafeInsertionSelects(2), 5 + getExtraSafeInsertionSelects(2));
    }

    @Test
    public void test425AddVisibleAttributeValuesDuplicate() throws Exception {
        OperationResult result = new OperationResult(ExtensionTest.class.getName() + ".test425AddVisibleAttributeValuesDuplicate");

        ObjectDelta<ShadowType> delta = getPrismContext().deltaFor(ShadowType.class)
                .item(ItemPath.create(ShadowType.F_ATTRIBUTES, ATTR_MANAGER), attrManagerDefinition)
                .add("alice", "bob")
                .asObjectDelta("");
        delta.applyTo(expectedShadow);

        queryListener.start();
        repositoryService.modifyObject(ShadowType.class, shadowOid, delta.getModifications(), getOptions(), result);
        queryListener.dumpAndStop();

        try (EntityManager em = factory.createEntityManager()) {
            RShadow s = em.find(RShadow.class, shadowOid);
            assertExtension(s, itemGroupName, "alumni");
            assertExtension(s, itemMember, "banderson", "kwhite");
            assertExtension(s, itemManager, "jack", "jim", "alice", "bob");
        }

        assertGetShadow(result);

        /*
 [1] select oid from m_object where oid = ? for update
 [1] select rshadow0_.oid as oid1_27_, rshadow0_1_.createChannel as createCh2_27_, rshadow0_1_.createTimestamp as createTi3_27_, rshadow0_1_.creatorRef_relation as creatorR4_27_, rshadow0_1_.creatorRef_targetOid as creatorR5_27_, rshadow0_1_.creatorRef_type as creatorR6_27_, rshadow0_1_.fullObject as fullObje7_27_, rshadow0_1_.lifecycleState as lifecycl8_27_, rshadow0_1_.modifierRef_relation as modifier9_27_, rshadow0_1_.modifierRef_targetOid as modifie10_27_, rshadow0_1_.modifierRef_type as modifie11_27_, rshadow0_1_.modifyChannel as modifyC12_27_, rshadow0_1_.modifyTimestamp as modifyT13_27_, rshadow0_1_.name_norm as name_no14_27_, rshadow0_1_.name_orig as name_or15_27_, rshadow0_1_.objectTypeClass as objectT16_27_, rshadow0_1_.tenantRef_relation as tenantR17_27_, rshadow0_1_.tenantRef_targetOid as tenantR18_27_, rshadow0_1_.tenantRef_type as tenantR19_27_, rshadow0_1_.version as version20_27_, rshadow0_.attemptNumber as attemptN1_41_, rshadow0_.dead as dead2_41_, rshadow0_.exist as exist3_41_, rshadow0_.failedOperationType as failedOp4_41_, rshadow0_.fullSynchronizationTimestamp as fullSync5_41_, rshadow0_.intent as intent6_41_, rshadow0_.kind as kind7_41_, rshadow0_.name_norm as name_nor8_41_, rshadow0_.name_orig as name_ori9_41_, rshadow0_.objectClass as objectC10_41_, rshadow0_.pendingOperationCount as pending11_41_, rshadow0_.primaryIdentifierValue as primary12_41_, rshadow0_.resourceRef_relation as resourc13_41_, rshadow0_.resourceRef_targetOid as resourc14_41_, rshadow0_.resourceRef_type as resourc15_41_, rshadow0_.status as status16_41_, rshadow0_.synchronizationSituation as synchro17_41_, rshadow0_.synchronizationTimestamp as synchro18_41_ from m_shadow rshadow0_ inner join m_object rshadow0_1_ on rshadow0_.oid=rshadow0_1_.oid where rshadow0_.oid=?
 [1] update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
         */
        assertCounts(3, 3);
    }

    @Test
    public void test426ReplaceVisibleAttributeValues() throws Exception {
        OperationResult result = new OperationResult(ExtensionTest.class.getName() + ".test426ReplaceVisibleAttributeValues");

        ObjectDelta<ShadowType> delta = getPrismContext().deltaFor(ShadowType.class)
                .item(ItemPath.create(ShadowType.F_ATTRIBUTES, ATTR_MANAGER), attrManagerDefinition)
                .replace("bob", "chuck")
                .asObjectDelta("");
        delta.applyTo(expectedShadow);

        queryListener.start();
        repositoryService.modifyObject(ShadowType.class, shadowOid, delta.getModifications(), getOptions(), result);
        queryListener.dumpAndStop();

        try (EntityManager em = factory.createEntityManager()) {
            RShadow s = em.find(RShadow.class, shadowOid);
            assertExtension(s, itemGroupName, "alumni");
            assertExtension(s, itemMember, "banderson", "kwhite");
            assertExtension(s, itemManager, "bob", "chuck");
        }

        assertGetShadow(result);

        /*
 [1] select oid from m_object where oid = ? for update
 [1] select rshadow0_.oid as oid1_27_, rshadow0_1_.createChannel as createCh2_27_, rshadow0_1_.createTimestamp as createTi3_27_, rshadow0_1_.creatorRef_relation as creatorR4_27_, rshadow0_1_.creatorRef_targetOid as creatorR5_27_, rshadow0_1_.creatorRef_type as creatorR6_27_, rshadow0_1_.fullObject as fullObje7_27_, rshadow0_1_.lifecycleState as lifecycl8_27_, rshadow0_1_.modifierRef_relation as modifier9_27_, rshadow0_1_.modifierRef_targetOid as modifie10_27_, rshadow0_1_.modifierRef_type as modifie11_27_, rshadow0_1_.modifyChannel as modifyC12_27_, rshadow0_1_.modifyTimestamp as modifyT13_27_, rshadow0_1_.name_norm as name_no14_27_, rshadow0_1_.name_orig as name_or15_27_, rshadow0_1_.objectTypeClass as objectT16_27_, rshadow0_1_.tenantRef_relation as tenantR17_27_, rshadow0_1_.tenantRef_targetOid as tenantR18_27_, rshadow0_1_.tenantRef_type as tenantR19_27_, rshadow0_1_.version as version20_27_, rshadow0_.attemptNumber as attemptN1_41_, rshadow0_.dead as dead2_41_, rshadow0_.exist as exist3_41_, rshadow0_.failedOperationType as failedOp4_41_, rshadow0_.fullSynchronizationTimestamp as fullSync5_41_, rshadow0_.intent as intent6_41_, rshadow0_.kind as kind7_41_, rshadow0_.name_norm as name_nor8_41_, rshadow0_.name_orig as name_ori9_41_, rshadow0_.objectClass as objectC10_41_, rshadow0_.pendingOperationCount as pending11_41_, rshadow0_.primaryIdentifierValue as primary12_41_, rshadow0_.resourceRef_relation as resourc13_41_, rshadow0_.resourceRef_targetOid as resourc14_41_, rshadow0_.resourceRef_type as resourc15_41_, rshadow0_.status as status16_41_, rshadow0_.synchronizationSituation as synchro17_41_, rshadow0_.synchronizationTimestamp as synchro18_41_ from m_shadow rshadow0_ inner join m_object rshadow0_1_ on rshadow0_.oid=rshadow0_1_.oid where rshadow0_.oid=?
 [1] select strings0_.owner_oid as owner_oi2_33_0_, strings0_.item_id as item_id1_33_0_, strings0_.ownerType as ownerTyp3_33_0_, strings0_.stringValue as stringVa4_33_0_, strings0_.item_id as item_id1_33_1_, strings0_.owner_oid as owner_oi2_33_1_, strings0_.ownerType as ownerTyp3_33_1_, strings0_.stringValue as stringVa4_33_1_ from m_object_ext_string strings0_ where strings0_.owner_oid=?
 [1] select roextstrin0_.item_id as item_id1_33_0_, roextstrin0_.owner_oid as owner_oi2_33_0_, roextstrin0_.ownerType as ownerTyp3_33_0_, roextstrin0_.stringValue as stringVa4_33_0_ from m_object_ext_string roextstrin0_ where roextstrin0_.item_id=? and roextstrin0_.owner_oid=? and roextstrin0_.ownerType=? and roextstrin0_.stringValue=?
 [1] insert into m_object_ext_string (item_id, owner_oid, ownerType, stringValue) values (?, ?, ?, ?)
 [1] update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
 [3] delete from m_object_ext_string where item_id=? and owner_oid=? and ownerType=? and stringValue=?
         */
        assertCounts(7, 9);
    }

    @Test
    public void test427DeleteVisibleAttributeValues() throws Exception {
        OperationResult result = new OperationResult(ExtensionTest.class.getName() + ".test427DeleteVisibleAttributeValues");

        ObjectDelta<ShadowType> delta = getPrismContext().deltaFor(ShadowType.class)
                .item(ItemPath.create(ShadowType.F_ATTRIBUTES, ATTR_GROUP_NAME), attrGroupNameDefinition)
                .delete("alumni")
                .asObjectDelta("");
        delta.applyTo(expectedShadow);

        queryListener.start();
        repositoryService.modifyObject(ShadowType.class, shadowOid, delta.getModifications(), getOptions(), result);
        queryListener.dumpAndStop();

        try (EntityManager em = factory.createEntityManager()) {
            RShadow s = em.find(RShadow.class, shadowOid);
            assertExtension(s, itemGroupName);
            assertExtension(s, itemMember, "banderson", "kwhite");
            assertExtension(s, itemManager, "bob", "chuck");
        }

        assertGetShadow(result);

        /*

 [1] select oid from m_object where oid = ? for update
 [1] select rshadow0_.oid as oid1_27_, rshadow0_1_.createChannel as createCh2_27_, rshadow0_1_.createTimestamp as createTi3_27_, rshadow0_1_.creatorRef_relation as creatorR4_27_, rshadow0_1_.creatorRef_targetOid as creatorR5_27_, rshadow0_1_.creatorRef_type as creatorR6_27_, rshadow0_1_.fullObject as fullObje7_27_, rshadow0_1_.lifecycleState as lifecycl8_27_, rshadow0_1_.modifierRef_relation as modifier9_27_, rshadow0_1_.modifierRef_targetOid as modifie10_27_, rshadow0_1_.modifierRef_type as modifie11_27_, rshadow0_1_.modifyChannel as modifyC12_27_, rshadow0_1_.modifyTimestamp as modifyT13_27_, rshadow0_1_.name_norm as name_no14_27_, rshadow0_1_.name_orig as name_or15_27_, rshadow0_1_.objectTypeClass as objectT16_27_, rshadow0_1_.tenantRef_relation as tenantR17_27_, rshadow0_1_.tenantRef_targetOid as tenantR18_27_, rshadow0_1_.tenantRef_type as tenantR19_27_, rshadow0_1_.version as version20_27_, rshadow0_.attemptNumber as attemptN1_41_, rshadow0_.dead as dead2_41_, rshadow0_.exist as exist3_41_, rshadow0_.failedOperationType as failedOp4_41_, rshadow0_.fullSynchronizationTimestamp as fullSync5_41_, rshadow0_.intent as intent6_41_, rshadow0_.kind as kind7_41_, rshadow0_.name_norm as name_nor8_41_, rshadow0_.name_orig as name_ori9_41_, rshadow0_.objectClass as objectC10_41_, rshadow0_.pendingOperationCount as pending11_41_, rshadow0_.primaryIdentifierValue as primary12_41_, rshadow0_.resourceRef_relation as resourc13_41_, rshadow0_.resourceRef_targetOid as resourc14_41_, rshadow0_.resourceRef_type as resourc15_41_, rshadow0_.status as status16_41_, rshadow0_.synchronizationSituation as synchro17_41_, rshadow0_.synchronizationTimestamp as synchro18_41_ from m_shadow rshadow0_ inner join m_object rshadow0_1_ on rshadow0_.oid=rshadow0_1_.oid where rshadow0_.oid=?
 [1] select strings0_.owner_oid as owner_oi2_33_0_, strings0_.item_id as item_id1_33_0_, strings0_.ownerType as ownerTyp3_33_0_, strings0_.stringValue as stringVa4_33_0_, strings0_.item_id as item_id1_33_1_, strings0_.owner_oid as owner_oi2_33_1_, strings0_.ownerType as ownerTyp3_33_1_, strings0_.stringValue as stringVa4_33_1_ from m_object_ext_string strings0_ where strings0_.owner_oid=?
 [1] update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
 [1] delete from m_object_ext_string where item_id=? and owner_oid=? and ownerType=? and stringValue=?

        no fetch deletion:

 [1] select oid from m_object where oid = ? for update
 [1] select rshadow0_.oid as oid1_27_, rshadow0_1_.createChannel as createCh2_27_, rshadow0_1_.createTimestamp as createTi3_27_, rshadow0_1_.creatorRef_relation as creatorR4_27_, rshadow0_1_.creatorRef_targetOid as creatorR5_27_, rshadow0_1_.creatorRef_type as creatorR6_27_, rshadow0_1_.fullObject as fullObje7_27_, rshadow0_1_.lifecycleState as lifecycl8_27_, rshadow0_1_.modifierRef_relation as modifier9_27_, rshadow0_1_.modifierRef_targetOid as modifie10_27_, rshadow0_1_.modifierRef_type as modifie11_27_, rshadow0_1_.modifyChannel as modifyC12_27_, rshadow0_1_.modifyTimestamp as modifyT13_27_, rshadow0_1_.name_norm as name_no14_27_, rshadow0_1_.name_orig as name_or15_27_, rshadow0_1_.objectTypeClass as objectT16_27_, rshadow0_1_.tenantRef_relation as tenantR17_27_, rshadow0_1_.tenantRef_targetOid as tenantR18_27_, rshadow0_1_.tenantRef_type as tenantR19_27_, rshadow0_1_.version as version20_27_, rshadow0_.attemptNumber as attemptN1_41_, rshadow0_.dead as dead2_41_, rshadow0_.exist as exist3_41_, rshadow0_.failedOperationType as failedOp4_41_, rshadow0_.fullSynchronizationTimestamp as fullSync5_41_, rshadow0_.intent as intent6_41_, rshadow0_.kind as kind7_41_, rshadow0_.name_norm as name_nor8_41_, rshadow0_.name_orig as name_ori9_41_, rshadow0_.objectClass as objectC10_41_, rshadow0_.pendingOperationCount as pending11_41_, rshadow0_.primaryIdentifierValue as primary12_41_, rshadow0_.resourceRef_relation as resourc13_41_, rshadow0_.resourceRef_targetOid as resourc14_41_, rshadow0_.resourceRef_type as resourc15_41_, rshadow0_.status as status16_41_, rshadow0_.synchronizationSituation as synchro17_41_, rshadow0_.synchronizationTimestamp as synchro18_41_ from m_shadow rshadow0_ inner join m_object rshadow0_1_ on rshadow0_.oid=rshadow0_1_.oid where rshadow0_.oid=?
 [1] delete from m_object_ext_string where owner_oid=? and ownerType=? and item_id=? and stringValue=?
 [1] update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
         */
        if (isNoFetchDeletion()) {
            assertCounts(4, 4);
        } else {
            assertCounts(5, 5);
        }
    }

    /**
     * We load extension values (e.g. invoking REPLACE operation) and then try to delete an extension value.
     */
    @Test
    public void test428DeleteAlreadyLoadedVisibleAttributeValue() throws Exception {
        OperationResult result = new OperationResult(ExtensionTest.class.getName() + ".test428DeleteAlreadyLoadedVisibleAttributeValue");

        ObjectDelta<ShadowType> delta = getPrismContext().deltaFor(ShadowType.class)
                .item(ItemPath.create(ShadowType.F_ATTRIBUTES, ATTR_GROUP_NAME), attrGroupNameDefinition)
                .replace("ALUMNI")
                .item(ItemPath.create(ShadowType.F_ATTRIBUTES, ATTR_MANAGER), attrManagerDefinition)
                .delete("bob")
                .asObjectDelta("");
        delta.applyTo(expectedShadow);

        queryListener.start();
        repositoryService.modifyObject(ShadowType.class, shadowOid, delta.getModifications(), getOptions(), result);
        queryListener.dumpAndStop();

        try (EntityManager em = factory.createEntityManager()) {
            RShadow s = em.find(RShadow.class, shadowOid);
            assertExtension(s, itemGroupName, "ALUMNI");
            assertExtension(s, itemMember, "banderson", "kwhite");
            assertExtension(s, itemManager, "chuck");
        }

        assertGetShadow(result);

        /*
 [1] select oid from m_object where oid = ? for update
 [1] select rshadow0_.oid as oid1_27_, rshadow0_1_.createChannel as createCh2_27_, rshadow0_1_.createTimestamp as createTi3_27_, rshadow0_1_.creatorRef_relation as creatorR4_27_, rshadow0_1_.creatorRef_targetOid as creatorR5_27_, rshadow0_1_.creatorRef_type as creatorR6_27_, rshadow0_1_.fullObject as fullObje7_27_, rshadow0_1_.lifecycleState as lifecycl8_27_, rshadow0_1_.modifierRef_relation as modifier9_27_, rshadow0_1_.modifierRef_targetOid as modifie10_27_, rshadow0_1_.modifierRef_type as modifie11_27_, rshadow0_1_.modifyChannel as modifyC12_27_, rshadow0_1_.modifyTimestamp as modifyT13_27_, rshadow0_1_.name_norm as name_no14_27_, rshadow0_1_.name_orig as name_or15_27_, rshadow0_1_.objectTypeClass as objectT16_27_, rshadow0_1_.tenantRef_relation as tenantR17_27_, rshadow0_1_.tenantRef_targetOid as tenantR18_27_, rshadow0_1_.tenantRef_type as tenantR19_27_, rshadow0_1_.version as version20_27_, rshadow0_.attemptNumber as attemptN1_41_, rshadow0_.dead as dead2_41_, rshadow0_.exist as exist3_41_, rshadow0_.failedOperationType as failedOp4_41_, rshadow0_.fullSynchronizationTimestamp as fullSync5_41_, rshadow0_.intent as intent6_41_, rshadow0_.kind as kind7_41_, rshadow0_.name_norm as name_nor8_41_, rshadow0_.name_orig as name_ori9_41_, rshadow0_.objectClass as objectC10_41_, rshadow0_.pendingOperationCount as pending11_41_, rshadow0_.primaryIdentifierValue as primary12_41_, rshadow0_.resourceRef_relation as resourc13_41_, rshadow0_.resourceRef_targetOid as resourc14_41_, rshadow0_.resourceRef_type as resourc15_41_, rshadow0_.status as status16_41_, rshadow0_.synchronizationSituation as synchro17_41_, rshadow0_.synchronizationTimestamp as synchro18_41_ from m_shadow rshadow0_ inner join m_object rshadow0_1_ on rshadow0_.oid=rshadow0_1_.oid where rshadow0_.oid=?
 [1] select strings0_.owner_oid as owner_oi2_33_0_, strings0_.item_id as item_id1_33_0_, strings0_.ownerType as ownerTyp3_33_0_, strings0_.stringValue as stringVa4_33_0_, strings0_.item_id as item_id1_33_1_, strings0_.owner_oid as owner_oi2_33_1_, strings0_.ownerType as ownerTyp3_33_1_, strings0_.stringValue as stringVa4_33_1_ from m_object_ext_string strings0_ where strings0_.owner_oid=?
 [1] select roextstrin0_.item_id as item_id1_33_0_, roextstrin0_.owner_oid as owner_oi2_33_0_, roextstrin0_.ownerType as ownerTyp3_33_0_, roextstrin0_.stringValue as stringVa4_33_0_ from m_object_ext_string roextstrin0_ where roextstrin0_.item_id=? and roextstrin0_.owner_oid=? and roextstrin0_.ownerType=? and roextstrin0_.stringValue=?
 [1] insert into m_object_ext_string (item_id, owner_oid, ownerType, stringValue) values (?, ?, ?, ?)
 [1] update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
 [1] delete from m_object_ext_string where item_id=? and owner_oid=? and ownerType=? and stringValue=?
         */
        assertCounts(7, 7);
    }

    // skipping replace non-indexed attribute value test (x29)

    @Test
    public void test430AddHiddenAttributeValues() throws Exception {
        OperationResult result = new OperationResult(ExtensionTest.class.getName() + ".test430AddHiddenAttributeValues");

        ObjectDelta<ShadowType> delta = getPrismContext().deltaFor(ShadowType.class)
                .item(ItemPath.create(ShadowType.F_ATTRIBUTES, ATTR_MEMBER), attrMemberDefinition)
                .add("tbrown", "jsmith")
                .asObjectDelta("");
        delta.applyTo(expectedShadow);

        queryListener.start();
        repositoryService.modifyObject(ShadowType.class, shadowOid, delta.getModifications(), getOptions(), result);
        queryListener.dumpAndStop();

        try (EntityManager em = factory.createEntityManager()) {
            RShadow s = em.find(RShadow.class, shadowOid);
            assertExtension(s, itemGroupName, "ALUMNI");
            assertExtension(s, itemMember, "banderson", "kwhite", "tbrown", "jsmith");
            assertExtension(s, itemManager, "chuck");
        }

        assertGetObject(result);

        /*
 [1] select oid from m_object where oid = ? for update
 [1] select rshadow0_.oid as oid1_27_, rshadow0_1_.createChannel as createCh2_27_, rshadow0_1_.createTimestamp as createTi3_27_, rshadow0_1_.creatorRef_relation as creatorR4_27_, rshadow0_1_.creatorRef_targetOid as creatorR5_27_, rshadow0_1_.creatorRef_type as creatorR6_27_, rshadow0_1_.fullObject as fullObje7_27_, rshadow0_1_.lifecycleState as lifecycl8_27_, rshadow0_1_.modifierRef_relation as modifier9_27_, rshadow0_1_.modifierRef_targetOid as modifie10_27_, rshadow0_1_.modifierRef_type as modifie11_27_, rshadow0_1_.modifyChannel as modifyC12_27_, rshadow0_1_.modifyTimestamp as modifyT13_27_, rshadow0_1_.name_norm as name_no14_27_, rshadow0_1_.name_orig as name_or15_27_, rshadow0_1_.objectTypeClass as objectT16_27_, rshadow0_1_.tenantRef_relation as tenantR17_27_, rshadow0_1_.tenantRef_targetOid as tenantR18_27_, rshadow0_1_.tenantRef_type as tenantR19_27_, rshadow0_1_.version as version20_27_, rshadow0_.attemptNumber as attemptN1_41_, rshadow0_.dead as dead2_41_, rshadow0_.exist as exist3_41_, rshadow0_.failedOperationType as failedOp4_41_, rshadow0_.fullSynchronizationTimestamp as fullSync5_41_, rshadow0_.intent as intent6_41_, rshadow0_.kind as kind7_41_, rshadow0_.name_norm as name_nor8_41_, rshadow0_.name_orig as name_ori9_41_, rshadow0_.objectClass as objectC10_41_, rshadow0_.pendingOperationCount as pending11_41_, rshadow0_.primaryIdentifierValue as primary12_41_, rshadow0_.resourceRef_relation as resourc13_41_, rshadow0_.resourceRef_targetOid as resourc14_41_, rshadow0_.resourceRef_type as resourc15_41_, rshadow0_.status as status16_41_, rshadow0_.synchronizationSituation as synchro17_41_, rshadow0_.synchronizationTimestamp as synchro18_41_ from m_shadow rshadow0_ inner join m_object rshadow0_1_ on rshadow0_.oid=rshadow0_1_.oid where rshadow0_.oid=?
 [2] insert into m_object_ext_string (item_id, owner_oid, ownerType, stringValue) values (?, ?, ?, ?)
 [1] update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?

        safe insertions:

 [1] select oid from m_object where oid = ? for update
 [1] select rshadow0_.oid as oid1_27_, rshadow0_1_.createChannel as createCh2_27_, rshadow0_1_.createTimestamp as createTi3_27_, rshadow0_1_.creatorRef_relation as creatorR4_27_, rshadow0_1_.creatorRef_targetOid as creatorR5_27_, rshadow0_1_.creatorRef_type as creatorR6_27_, rshadow0_1_.fullObject as fullObje7_27_, rshadow0_1_.lifecycleState as lifecycl8_27_, rshadow0_1_.modifierRef_relation as modifier9_27_, rshadow0_1_.modifierRef_targetOid as modifie10_27_, rshadow0_1_.modifierRef_type as modifie11_27_, rshadow0_1_.modifyChannel as modifyC12_27_, rshadow0_1_.modifyTimestamp as modifyT13_27_, rshadow0_1_.name_norm as name_no14_27_, rshadow0_1_.name_orig as name_or15_27_, rshadow0_1_.objectTypeClass as objectT16_27_, rshadow0_1_.tenantRef_relation as tenantR17_27_, rshadow0_1_.tenantRef_targetOid as tenantR18_27_, rshadow0_1_.tenantRef_type as tenantR19_27_, rshadow0_1_.version as version20_27_, rshadow0_.attemptNumber as attemptN1_41_, rshadow0_.dead as dead2_41_, rshadow0_.exist as exist3_41_, rshadow0_.failedOperationType as failedOp4_41_, rshadow0_.fullSynchronizationTimestamp as fullSync5_41_, rshadow0_.intent as intent6_41_, rshadow0_.kind as kind7_41_, rshadow0_.name_norm as name_nor8_41_, rshadow0_.name_orig as name_ori9_41_, rshadow0_.objectClass as objectC10_41_, rshadow0_.pendingOperationCount as pending11_41_, rshadow0_.primaryIdentifierValue as primary12_41_, rshadow0_.resourceRef_relation as resourc13_41_, rshadow0_.resourceRef_targetOid as resourc14_41_, rshadow0_.resourceRef_type as resourc15_41_, rshadow0_.status as status16_41_, rshadow0_.synchronizationSituation as synchro17_41_, rshadow0_.synchronizationTimestamp as synchro18_41_ from m_shadow rshadow0_ inner join m_object rshadow0_1_ on rshadow0_.oid=rshadow0_1_.oid where rshadow0_.oid=?
 [1] select roextstrin0_.item_id as item_id1_33_0_, roextstrin0_.owner_oid as owner_oi2_33_0_, roextstrin0_.ownerType as ownerTyp3_33_0_, roextstrin0_.stringValue as stringVa4_33_0_ from m_object_ext_string roextstrin0_ where roextstrin0_.item_id=? and roextstrin0_.owner_oid=? and roextstrin0_.ownerType=? and roextstrin0_.stringValue=?
 [1] select roextstrin0_.item_id as item_id1_33_0_, roextstrin0_.owner_oid as owner_oi2_33_0_, roextstrin0_.ownerType as ownerTyp3_33_0_, roextstrin0_.stringValue as stringVa4_33_0_ from m_object_ext_string roextstrin0_ where roextstrin0_.item_id=? and roextstrin0_.owner_oid=? and roextstrin0_.ownerType=? and roextstrin0_.stringValue=?
 [2] insert into m_object_ext_string (item_id, owner_oid, ownerType, stringValue) values (?, ?, ?, ?)
 [1] update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
         */
        assertCounts(4 + getExtraSafeInsertionSelects(2), 5 + getExtraSafeInsertionSelects(2));
    }

    @Test
    public void test432AddHiddenAttributeValuesDuplicate() throws Exception {
        OperationResult result = new OperationResult(ExtensionTest.class.getName() + ".test432AddHiddenAttributeValuesDuplicate");

        ObjectDelta<ShadowType> delta = getPrismContext().deltaFor(ShadowType.class)
                .item(ItemPath.create(ShadowType.F_ATTRIBUTES, ATTR_MEMBER), attrMemberDefinition)
                .add("tbrown", "jsmith")
                .asObjectDelta("");
        delta.applyTo(expectedShadow);

        queryListener.start();
        repositoryService.modifyObject(ShadowType.class, shadowOid, delta.getModifications(), getOptions(), result);
        queryListener.dumpAndStop();

        try (EntityManager em = factory.createEntityManager()) {
            RShadow s = em.find(RShadow.class, shadowOid);
            assertExtension(s, itemGroupName, "ALUMNI");
            assertExtension(s, itemMember, "banderson", "kwhite", "tbrown", "jsmith");
            assertExtension(s, itemManager, "chuck");
        }

        assertGetObject(result);

        /*
 [1] select oid from m_object where oid = ? for update
 [1] select rshadow0_.oid as oid1_27_, rshadow0_1_.createChannel as createCh2_27_, rshadow0_1_.createTimestamp as createTi3_27_, rshadow0_1_.creatorRef_relation as creatorR4_27_, rshadow0_1_.creatorRef_targetOid as creatorR5_27_, rshadow0_1_.creatorRef_type as creatorR6_27_, rshadow0_1_.fullObject as fullObje7_27_, rshadow0_1_.lifecycleState as lifecycl8_27_, rshadow0_1_.modifierRef_relation as modifier9_27_, rshadow0_1_.modifierRef_targetOid as modifie10_27_, rshadow0_1_.modifierRef_type as modifie11_27_, rshadow0_1_.modifyChannel as modifyC12_27_, rshadow0_1_.modifyTimestamp as modifyT13_27_, rshadow0_1_.name_norm as name_no14_27_, rshadow0_1_.name_orig as name_or15_27_, rshadow0_1_.objectTypeClass as objectT16_27_, rshadow0_1_.tenantRef_relation as tenantR17_27_, rshadow0_1_.tenantRef_targetOid as tenantR18_27_, rshadow0_1_.tenantRef_type as tenantR19_27_, rshadow0_1_.version as version20_27_, rshadow0_.attemptNumber as attemptN1_41_, rshadow0_.dead as dead2_41_, rshadow0_.exist as exist3_41_, rshadow0_.failedOperationType as failedOp4_41_, rshadow0_.fullSynchronizationTimestamp as fullSync5_41_, rshadow0_.intent as intent6_41_, rshadow0_.kind as kind7_41_, rshadow0_.name_norm as name_nor8_41_, rshadow0_.name_orig as name_ori9_41_, rshadow0_.objectClass as objectC10_41_, rshadow0_.pendingOperationCount as pending11_41_, rshadow0_.primaryIdentifierValue as primary12_41_, rshadow0_.resourceRef_relation as resourc13_41_, rshadow0_.resourceRef_targetOid as resourc14_41_, rshadow0_.resourceRef_type as resourc15_41_, rshadow0_.status as status16_41_, rshadow0_.synchronizationSituation as synchro17_41_, rshadow0_.synchronizationTimestamp as synchro18_41_ from m_shadow rshadow0_ inner join m_object rshadow0_1_ on rshadow0_.oid=rshadow0_1_.oid where rshadow0_.oid=?
 [2] insert into m_object_ext_string (item_id, owner_oid, ownerType, stringValue) values (?, ?, ?, ?)
         <<< ConstraintViolationException here -> restarting the operation in safer way >>>
 [1] select oid from m_object where oid = ? for update
 [1] select rshadow0_.oid as oid1_27_, rshadow0_1_.createChannel as createCh2_27_, rshadow0_1_.createTimestamp as createTi3_27_, rshadow0_1_.creatorRef_relation as creatorR4_27_, rshadow0_1_.creatorRef_targetOid as creatorR5_27_, rshadow0_1_.creatorRef_type as creatorR6_27_, rshadow0_1_.fullObject as fullObje7_27_, rshadow0_1_.lifecycleState as lifecycl8_27_, rshadow0_1_.modifierRef_relation as modifier9_27_, rshadow0_1_.modifierRef_targetOid as modifie10_27_, rshadow0_1_.modifierRef_type as modifie11_27_, rshadow0_1_.modifyChannel as modifyC12_27_, rshadow0_1_.modifyTimestamp as modifyT13_27_, rshadow0_1_.name_norm as name_no14_27_, rshadow0_1_.name_orig as name_or15_27_, rshadow0_1_.objectTypeClass as objectT16_27_, rshadow0_1_.tenantRef_relation as tenantR17_27_, rshadow0_1_.tenantRef_targetOid as tenantR18_27_, rshadow0_1_.tenantRef_type as tenantR19_27_, rshadow0_1_.version as version20_27_, rshadow0_.attemptNumber as attemptN1_41_, rshadow0_.dead as dead2_41_, rshadow0_.exist as exist3_41_, rshadow0_.failedOperationType as failedOp4_41_, rshadow0_.fullSynchronizationTimestamp as fullSync5_41_, rshadow0_.intent as intent6_41_, rshadow0_.kind as kind7_41_, rshadow0_.name_norm as name_nor8_41_, rshadow0_.name_orig as name_ori9_41_, rshadow0_.objectClass as objectC10_41_, rshadow0_.pendingOperationCount as pending11_41_, rshadow0_.primaryIdentifierValue as primary12_41_, rshadow0_.resourceRef_relation as resourc13_41_, rshadow0_.resourceRef_targetOid as resourc14_41_, rshadow0_.resourceRef_type as resourc15_41_, rshadow0_.status as status16_41_, rshadow0_.synchronizationSituation as synchro17_41_, rshadow0_.synchronizationTimestamp as synchro18_41_ from m_shadow rshadow0_ inner join m_object rshadow0_1_ on rshadow0_.oid=rshadow0_1_.oid where rshadow0_.oid=?
 [1] select roextstrin0_.item_id as item_id1_33_0_, roextstrin0_.owner_oid as owner_oi2_33_0_, roextstrin0_.ownerType as ownerTyp3_33_0_, roextstrin0_.stringValue as stringVa4_33_0_ from m_object_ext_string roextstrin0_ where roextstrin0_.item_id=? and roextstrin0_.owner_oid=? and roextstrin0_.ownerType=? and roextstrin0_.stringValue=?
 [1] select roextstrin0_.item_id as item_id1_33_0_, roextstrin0_.owner_oid as owner_oi2_33_0_, roextstrin0_.ownerType as ownerTyp3_33_0_, roextstrin0_.stringValue as stringVa4_33_0_ from m_object_ext_string roextstrin0_ where roextstrin0_.item_id=? and roextstrin0_.owner_oid=? and roextstrin0_.ownerType=? and roextstrin0_.stringValue=?
 [1] update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?

        safe insertions:

 [1] select oid from m_object where oid = ? for update
 [1] select rshadow0_.oid as oid1_27_, rshadow0_1_.createChannel as createCh2_27_, rshadow0_1_.createTimestamp as createTi3_27_, rshadow0_1_.creatorRef_relation as creatorR4_27_, rshadow0_1_.creatorRef_targetOid as creatorR5_27_, rshadow0_1_.creatorRef_type as creatorR6_27_, rshadow0_1_.fullObject as fullObje7_27_, rshadow0_1_.lifecycleState as lifecycl8_27_, rshadow0_1_.modifierRef_relation as modifier9_27_, rshadow0_1_.modifierRef_targetOid as modifie10_27_, rshadow0_1_.modifierRef_type as modifie11_27_, rshadow0_1_.modifyChannel as modifyC12_27_, rshadow0_1_.modifyTimestamp as modifyT13_27_, rshadow0_1_.name_norm as name_no14_27_, rshadow0_1_.name_orig as name_or15_27_, rshadow0_1_.objectTypeClass as objectT16_27_, rshadow0_1_.tenantRef_relation as tenantR17_27_, rshadow0_1_.tenantRef_targetOid as tenantR18_27_, rshadow0_1_.tenantRef_type as tenantR19_27_, rshadow0_1_.version as version20_27_, rshadow0_.attemptNumber as attemptN1_41_, rshadow0_.dead as dead2_41_, rshadow0_.exist as exist3_41_, rshadow0_.failedOperationType as failedOp4_41_, rshadow0_.fullSynchronizationTimestamp as fullSync5_41_, rshadow0_.intent as intent6_41_, rshadow0_.kind as kind7_41_, rshadow0_.name_norm as name_nor8_41_, rshadow0_.name_orig as name_ori9_41_, rshadow0_.objectClass as objectC10_41_, rshadow0_.pendingOperationCount as pending11_41_, rshadow0_.primaryIdentifierValue as primary12_41_, rshadow0_.resourceRef_relation as resourc13_41_, rshadow0_.resourceRef_targetOid as resourc14_41_, rshadow0_.resourceRef_type as resourc15_41_, rshadow0_.status as status16_41_, rshadow0_.synchronizationSituation as synchro17_41_, rshadow0_.synchronizationTimestamp as synchro18_41_ from m_shadow rshadow0_ inner join m_object rshadow0_1_ on rshadow0_.oid=rshadow0_1_.oid where rshadow0_.oid=?
 [1] select roextstrin0_.item_id as item_id1_33_0_, roextstrin0_.owner_oid as owner_oi2_33_0_, roextstrin0_.ownerType as ownerTyp3_33_0_, roextstrin0_.stringValue as stringVa4_33_0_ from m_object_ext_string roextstrin0_ where roextstrin0_.item_id=? and roextstrin0_.owner_oid=? and roextstrin0_.ownerType=? and roextstrin0_.stringValue=?
 [1] select roextstrin0_.item_id as item_id1_33_0_, roextstrin0_.owner_oid as owner_oi2_33_0_, roextstrin0_.ownerType as ownerTyp3_33_0_, roextstrin0_.stringValue as stringVa4_33_0_ from m_object_ext_string roextstrin0_ where roextstrin0_.item_id=? and roextstrin0_.owner_oid=? and roextstrin0_.ownerType=? and roextstrin0_.stringValue=?
 [1] update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
         */
        if (isNoFetchInsertion()) {
            assertCounts(8, 9);
        } else {
            assertCounts(5, 5);
        }
    }

    @Test
    public void test435DeleteHiddenAttributeValues() throws Exception {
        OperationResult result = new OperationResult(ExtensionTest.class.getName() + ".test435DeleteHiddenAttributeValues");

        ObjectDelta<ShadowType> delta = getPrismContext().deltaFor(ShadowType.class)
                .item(ItemPath.create(ShadowType.F_ATTRIBUTES, ATTR_MEMBER), attrMemberDefinition)
                .delete("tbrown", "jsmith")
                .asObjectDelta("");
        delta.applyTo(expectedShadow);

        queryListener.start();
        repositoryService.modifyObject(ShadowType.class, shadowOid, delta.getModifications(), getOptions(), result);
        queryListener.dumpAndStop();

        try (EntityManager em = factory.createEntityManager()) {
            RShadow s = em.find(RShadow.class, shadowOid);
            assertExtension(s, itemGroupName, "ALUMNI");
            assertExtension(s, itemMember, "banderson", "kwhite");
            assertExtension(s, itemManager, "chuck");
        }

        assertGetObject(result);

        /*
 [1] select oid from m_object where oid = ? for update
 [1] select rshadow0_.oid as oid1_27_, rshadow0_1_.createChannel as createCh2_27_, rshadow0_1_.createTimestamp as createTi3_27_, rshadow0_1_.creatorRef_relation as creatorR4_27_, rshadow0_1_.creatorRef_targetOid as creatorR5_27_, rshadow0_1_.creatorRef_type as creatorR6_27_, rshadow0_1_.fullObject as fullObje7_27_, rshadow0_1_.lifecycleState as lifecycl8_27_, rshadow0_1_.modifierRef_relation as modifier9_27_, rshadow0_1_.modifierRef_targetOid as modifie10_27_, rshadow0_1_.modifierRef_type as modifie11_27_, rshadow0_1_.modifyChannel as modifyC12_27_, rshadow0_1_.modifyTimestamp as modifyT13_27_, rshadow0_1_.name_norm as name_no14_27_, rshadow0_1_.name_orig as name_or15_27_, rshadow0_1_.objectTypeClass as objectT16_27_, rshadow0_1_.tenantRef_relation as tenantR17_27_, rshadow0_1_.tenantRef_targetOid as tenantR18_27_, rshadow0_1_.tenantRef_type as tenantR19_27_, rshadow0_1_.version as version20_27_, rshadow0_.attemptNumber as attemptN1_41_, rshadow0_.dead as dead2_41_, rshadow0_.exist as exist3_41_, rshadow0_.failedOperationType as failedOp4_41_, rshadow0_.fullSynchronizationTimestamp as fullSync5_41_, rshadow0_.intent as intent6_41_, rshadow0_.kind as kind7_41_, rshadow0_.name_norm as name_nor8_41_, rshadow0_.name_orig as name_ori9_41_, rshadow0_.objectClass as objectC10_41_, rshadow0_.pendingOperationCount as pending11_41_, rshadow0_.primaryIdentifierValue as primary12_41_, rshadow0_.resourceRef_relation as resourc13_41_, rshadow0_.resourceRef_targetOid as resourc14_41_, rshadow0_.resourceRef_type as resourc15_41_, rshadow0_.status as status16_41_, rshadow0_.synchronizationSituation as synchro17_41_, rshadow0_.synchronizationTimestamp as synchro18_41_ from m_shadow rshadow0_ inner join m_object rshadow0_1_ on rshadow0_.oid=rshadow0_1_.oid where rshadow0_.oid=?
 [1] select strings0_.owner_oid as owner_oi2_33_0_, strings0_.item_id as item_id1_33_0_, strings0_.ownerType as ownerTyp3_33_0_, strings0_.stringValue as stringVa4_33_0_, strings0_.item_id as item_id1_33_1_, strings0_.owner_oid as owner_oi2_33_1_, strings0_.ownerType as ownerTyp3_33_1_, strings0_.stringValue as stringVa4_33_1_ from m_object_ext_string strings0_ where strings0_.owner_oid=?
 [1] update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
 [2] delete from m_object_ext_string where item_id=? and owner_oid=? and ownerType=? and stringValue=?

        no fetch delete

 [1] select oid from m_object where oid = ? for update
 [1] select rshadow0_.oid as oid1_27_, rshadow0_1_.createChannel as createCh2_27_, rshadow0_1_.createTimestamp as createTi3_27_, rshadow0_1_.creatorRef_relation as creatorR4_27_, rshadow0_1_.creatorRef_targetOid as creatorR5_27_, rshadow0_1_.creatorRef_type as creatorR6_27_, rshadow0_1_.fullObject as fullObje7_27_, rshadow0_1_.lifecycleState as lifecycl8_27_, rshadow0_1_.modifierRef_relation as modifier9_27_, rshadow0_1_.modifierRef_targetOid as modifie10_27_, rshadow0_1_.modifierRef_type as modifie11_27_, rshadow0_1_.modifyChannel as modifyC12_27_, rshadow0_1_.modifyTimestamp as modifyT13_27_, rshadow0_1_.name_norm as name_no14_27_, rshadow0_1_.name_orig as name_or15_27_, rshadow0_1_.objectTypeClass as objectT16_27_, rshadow0_1_.tenantRef_relation as tenantR17_27_, rshadow0_1_.tenantRef_targetOid as tenantR18_27_, rshadow0_1_.tenantRef_type as tenantR19_27_, rshadow0_1_.version as version20_27_, rshadow0_.attemptNumber as attemptN1_41_, rshadow0_.dead as dead2_41_, rshadow0_.exist as exist3_41_, rshadow0_.failedOperationType as failedOp4_41_, rshadow0_.fullSynchronizationTimestamp as fullSync5_41_, rshadow0_.intent as intent6_41_, rshadow0_.kind as kind7_41_, rshadow0_.name_norm as name_nor8_41_, rshadow0_.name_orig as name_ori9_41_, rshadow0_.objectClass as objectC10_41_, rshadow0_.pendingOperationCount as pending11_41_, rshadow0_.primaryIdentifierValue as primary12_41_, rshadow0_.resourceRef_relation as resourc13_41_, rshadow0_.resourceRef_targetOid as resourc14_41_, rshadow0_.resourceRef_type as resourc15_41_, rshadow0_.status as status16_41_, rshadow0_.synchronizationSituation as synchro17_41_, rshadow0_.synchronizationTimestamp as synchro18_41_ from m_shadow rshadow0_ inner join m_object rshadow0_1_ on rshadow0_.oid=rshadow0_1_.oid where rshadow0_.oid=?
 [1] delete from m_object_ext_string where owner_oid=? and ownerType=? and item_id=? and stringValue=?
 [1] delete from m_object_ext_string where owner_oid=? and ownerType=? and item_id=? and stringValue=?
 [1] update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?

         */
        if (isNoFetchDeletion()) {
            assertCounts(5, 5);
        } else {
            assertCounts(5, 6);
        }
    }

    @Test
    public void test436DeleteHiddenAttributeValuesDuplicate() throws Exception {
        OperationResult result = new OperationResult(ExtensionTest.class.getName() + ".test436DeleteHiddenAttributeValuesDuplicate");

        ObjectDelta<ShadowType> delta = getPrismContext().deltaFor(ShadowType.class)
                .item(ItemPath.create(ShadowType.F_ATTRIBUTES, ATTR_MEMBER), attrMemberDefinition)
                .delete("tbrown", "jsmith")
                .asObjectDelta("");
        delta.applyTo(expectedShadow);

        queryListener.start();
        repositoryService.modifyObject(ShadowType.class, shadowOid, delta.getModifications(), getOptions(), result);
        queryListener.dumpAndStop();

        try (EntityManager em = factory.createEntityManager()) {
            RShadow s = em.find(RShadow.class, shadowOid);
            assertExtension(s, itemGroupName, "ALUMNI");
            assertExtension(s, itemMember, "banderson", "kwhite");
            assertExtension(s, itemManager, "chuck");
        }

        assertGetObject(result);

        /*
 [1] select oid from m_object where oid = ? for update
 [1] select rshadow0_.oid as oid1_27_, rshadow0_1_.createChannel as createCh2_27_, rshadow0_1_.createTimestamp as createTi3_27_, rshadow0_1_.creatorRef_relation as creatorR4_27_, rshadow0_1_.creatorRef_targetOid as creatorR5_27_, rshadow0_1_.creatorRef_type as creatorR6_27_, rshadow0_1_.fullObject as fullObje7_27_, rshadow0_1_.lifecycleState as lifecycl8_27_, rshadow0_1_.modifierRef_relation as modifier9_27_, rshadow0_1_.modifierRef_targetOid as modifie10_27_, rshadow0_1_.modifierRef_type as modifie11_27_, rshadow0_1_.modifyChannel as modifyC12_27_, rshadow0_1_.modifyTimestamp as modifyT13_27_, rshadow0_1_.name_norm as name_no14_27_, rshadow0_1_.name_orig as name_or15_27_, rshadow0_1_.objectTypeClass as objectT16_27_, rshadow0_1_.tenantRef_relation as tenantR17_27_, rshadow0_1_.tenantRef_targetOid as tenantR18_27_, rshadow0_1_.tenantRef_type as tenantR19_27_, rshadow0_1_.version as version20_27_, rshadow0_.attemptNumber as attemptN1_41_, rshadow0_.dead as dead2_41_, rshadow0_.exist as exist3_41_, rshadow0_.failedOperationType as failedOp4_41_, rshadow0_.fullSynchronizationTimestamp as fullSync5_41_, rshadow0_.intent as intent6_41_, rshadow0_.kind as kind7_41_, rshadow0_.name_norm as name_nor8_41_, rshadow0_.name_orig as name_ori9_41_, rshadow0_.objectClass as objectC10_41_, rshadow0_.pendingOperationCount as pending11_41_, rshadow0_.primaryIdentifierValue as primary12_41_, rshadow0_.resourceRef_relation as resourc13_41_, rshadow0_.resourceRef_targetOid as resourc14_41_, rshadow0_.resourceRef_type as resourc15_41_, rshadow0_.status as status16_41_, rshadow0_.synchronizationSituation as synchro17_41_, rshadow0_.synchronizationTimestamp as synchro18_41_ from m_shadow rshadow0_ inner join m_object rshadow0_1_ on rshadow0_.oid=rshadow0_1_.oid where rshadow0_.oid=?
 [1] select strings0_.owner_oid as owner_oi2_33_0_, strings0_.item_id as item_id1_33_0_, strings0_.ownerType as ownerTyp3_33_0_, strings0_.stringValue as stringVa4_33_0_, strings0_.item_id as item_id1_33_1_, strings0_.owner_oid as owner_oi2_33_1_, strings0_.ownerType as ownerTyp3_33_1_, strings0_.stringValue as stringVa4_33_1_ from m_object_ext_string strings0_ where strings0_.owner_oid=?
 [1] update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?

        no fetch delete

 [1] select oid from m_object where oid = ? for update
 [1] select rshadow0_.oid as oid1_27_, rshadow0_1_.createChannel as createCh2_27_, rshadow0_1_.createTimestamp as createTi3_27_, rshadow0_1_.creatorRef_relation as creatorR4_27_, rshadow0_1_.creatorRef_targetOid as creatorR5_27_, rshadow0_1_.creatorRef_type as creatorR6_27_, rshadow0_1_.fullObject as fullObje7_27_, rshadow0_1_.lifecycleState as lifecycl8_27_, rshadow0_1_.modifierRef_relation as modifier9_27_, rshadow0_1_.modifierRef_targetOid as modifie10_27_, rshadow0_1_.modifierRef_type as modifie11_27_, rshadow0_1_.modifyChannel as modifyC12_27_, rshadow0_1_.modifyTimestamp as modifyT13_27_, rshadow0_1_.name_norm as name_no14_27_, rshadow0_1_.name_orig as name_or15_27_, rshadow0_1_.objectTypeClass as objectT16_27_, rshadow0_1_.tenantRef_relation as tenantR17_27_, rshadow0_1_.tenantRef_targetOid as tenantR18_27_, rshadow0_1_.tenantRef_type as tenantR19_27_, rshadow0_1_.version as version20_27_, rshadow0_.attemptNumber as attemptN1_41_, rshadow0_.dead as dead2_41_, rshadow0_.exist as exist3_41_, rshadow0_.failedOperationType as failedOp4_41_, rshadow0_.fullSynchronizationTimestamp as fullSync5_41_, rshadow0_.intent as intent6_41_, rshadow0_.kind as kind7_41_, rshadow0_.name_norm as name_nor8_41_, rshadow0_.name_orig as name_ori9_41_, rshadow0_.objectClass as objectC10_41_, rshadow0_.pendingOperationCount as pending11_41_, rshadow0_.primaryIdentifierValue as primary12_41_, rshadow0_.resourceRef_relation as resourc13_41_, rshadow0_.resourceRef_targetOid as resourc14_41_, rshadow0_.resourceRef_type as resourc15_41_, rshadow0_.status as status16_41_, rshadow0_.synchronizationSituation as synchro17_41_, rshadow0_.synchronizationTimestamp as synchro18_41_ from m_shadow rshadow0_ inner join m_object rshadow0_1_ on rshadow0_.oid=rshadow0_1_.oid where rshadow0_.oid=?
 [1] delete from m_object_ext_string where owner_oid=? and ownerType=? and item_id=? and stringValue=?
 [1] delete from m_object_ext_string where owner_oid=? and ownerType=? and item_id=? and stringValue=?
 [1] update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
         */
        if (isNoFetchDeletion()) {
            assertCounts(5, 5);
        } else {
            assertCounts(4, 4);
        }
    }

    @Test
    public void test440ReplaceHiddenAttributeValues() throws Exception {
        OperationResult result = new OperationResult(ExtensionTest.class.getName() + ".test440ReplaceHiddenAttributeValues");

        ObjectDelta<ShadowType> delta = getPrismContext().deltaFor(ShadowType.class)
                .item(ItemPath.create(ShadowType.F_ATTRIBUTES, ATTR_MEMBER), attrMemberDefinition)
                .replace("alice", "bob")
                .asObjectDelta("");
        delta.applyTo(expectedShadow);

        queryListener.start();
        repositoryService.modifyObject(ShadowType.class, shadowOid, delta.getModifications(), getOptions(), result);
        queryListener.dumpAndStop();

        try (EntityManager em = factory.createEntityManager()) {
            RShadow s = em.find(RShadow.class, shadowOid);
            assertExtension(s, itemGroupName, "ALUMNI");
            assertExtension(s, itemMember, "alice", "bob");
            assertExtension(s, itemManager, "chuck");
        }

        assertGetObject(result);

        /*
         safe insertions (can be improved)

 [1] select oid from m_object where oid = ? for update
 [1] select rshadow0_.oid as oid1_27_, rshadow0_1_.createChannel as createCh2_27_, rshadow0_1_.createTimestamp as createTi3_27_, rshadow0_1_.creatorRef_relation as creatorR4_27_, rshadow0_1_.creatorRef_targetOid as creatorR5_27_, rshadow0_1_.creatorRef_type as creatorR6_27_, rshadow0_1_.fullObject as fullObje7_27_, rshadow0_1_.lifecycleState as lifecycl8_27_, rshadow0_1_.modifierRef_relation as modifier9_27_, rshadow0_1_.modifierRef_targetOid as modifie10_27_, rshadow0_1_.modifierRef_type as modifie11_27_, rshadow0_1_.modifyChannel as modifyC12_27_, rshadow0_1_.modifyTimestamp as modifyT13_27_, rshadow0_1_.name_norm as name_no14_27_, rshadow0_1_.name_orig as name_or15_27_, rshadow0_1_.objectTypeClass as objectT16_27_, rshadow0_1_.tenantRef_relation as tenantR17_27_, rshadow0_1_.tenantRef_targetOid as tenantR18_27_, rshadow0_1_.tenantRef_type as tenantR19_27_, rshadow0_1_.version as version20_27_, rshadow0_.attemptNumber as attemptN1_41_, rshadow0_.dead as dead2_41_, rshadow0_.exist as exist3_41_, rshadow0_.failedOperationType as failedOp4_41_, rshadow0_.fullSynchronizationTimestamp as fullSync5_41_, rshadow0_.intent as intent6_41_, rshadow0_.kind as kind7_41_, rshadow0_.name_norm as name_nor8_41_, rshadow0_.name_orig as name_ori9_41_, rshadow0_.objectClass as objectC10_41_, rshadow0_.pendingOperationCount as pending11_41_, rshadow0_.primaryIdentifierValue as primary12_41_, rshadow0_.resourceRef_relation as resourc13_41_, rshadow0_.resourceRef_targetOid as resourc14_41_, rshadow0_.resourceRef_type as resourc15_41_, rshadow0_.status as status16_41_, rshadow0_.synchronizationSituation as synchro17_41_, rshadow0_.synchronizationTimestamp as synchro18_41_ from m_shadow rshadow0_ inner join m_object rshadow0_1_ on rshadow0_.oid=rshadow0_1_.oid where rshadow0_.oid=?
 [1] select strings0_.owner_oid as owner_oi2_33_0_, strings0_.item_id as item_id1_33_0_, strings0_.ownerType as ownerTyp3_33_0_, strings0_.stringValue as stringVa4_33_0_, strings0_.item_id as item_id1_33_1_, strings0_.owner_oid as owner_oi2_33_1_, strings0_.ownerType as ownerTyp3_33_1_, strings0_.stringValue as stringVa4_33_1_ from m_object_ext_string strings0_ where strings0_.owner_oid=?
 [1] select roextstrin0_.item_id as item_id1_33_0_, roextstrin0_.owner_oid as owner_oi2_33_0_, roextstrin0_.ownerType as ownerTyp3_33_0_, roextstrin0_.stringValue as stringVa4_33_0_ from m_object_ext_string roextstrin0_ where roextstrin0_.item_id=? and roextstrin0_.owner_oid=? and roextstrin0_.ownerType=? and roextstrin0_.stringValue=?
 [1] select roextstrin0_.item_id as item_id1_33_0_, roextstrin0_.owner_oid as owner_oi2_33_0_, roextstrin0_.ownerType as ownerTyp3_33_0_, roextstrin0_.stringValue as stringVa4_33_0_ from m_object_ext_string roextstrin0_ where roextstrin0_.item_id=? and roextstrin0_.owner_oid=? and roextstrin0_.ownerType=? and roextstrin0_.stringValue=?
 [2] insert into m_object_ext_string (item_id, owner_oid, ownerType, stringValue) values (?, ?, ?, ?)
 [1] update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
 [2] delete from m_object_ext_string where item_id=? and owner_oid=? and ownerType=? and stringValue=?
         */
        assertCounts(8, 10);
    }

    @Test
    public void test450DeleteWholeContainer() throws Exception {
        OperationResult result = new OperationResult(ExtensionTest.class.getName() + ".test450DeleteWholeContainer");

        PrismContainerValue<?> existingAttributes = expectedShadow.findContainer(ShadowType.F_ATTRIBUTES).getValue().clone();
        existingAttributes.removeProperty(ATTR_MEMBER);      // there are two values of this kind there

        ObjectDelta<ShadowType> delta = getPrismContext().deltaFor(ShadowType.class)
                .item(ShadowType.F_ATTRIBUTES)
                .delete(existingAttributes)
                .asObjectDelta("");
        expectedShadow.asObjectable().setAttributes(null);

        queryListener.start();
        repositoryService.modifyObject(ShadowType.class, shadowOid, delta.getModifications(), getOptions(), result);
        queryListener.dumpAndStop();

        try (EntityManager em = factory.createEntityManager()) {
            RShadow s = em.find(RShadow.class, shadowOid);
            assertExtension(s, itemGroupName);
            assertExtension(s, itemMember);
            assertExtension(s, itemManager);
        }

        assertGetObject(result);

        /*
 [1] select oid from m_object where oid = ? for update
 [1] select rshadow0_.oid as oid1_27_, rshadow0_1_.createChannel as createCh2_27_, rshadow0_1_.createTimestamp as createTi3_27_, rshadow0_1_.creatorRef_relation as creatorR4_27_, rshadow0_1_.creatorRef_targetOid as creatorR5_27_, rshadow0_1_.creatorRef_type as creatorR6_27_, rshadow0_1_.fullObject as fullObje7_27_, rshadow0_1_.lifecycleState as lifecycl8_27_, rshadow0_1_.modifierRef_relation as modifier9_27_, rshadow0_1_.modifierRef_targetOid as modifie10_27_, rshadow0_1_.modifierRef_type as modifie11_27_, rshadow0_1_.modifyChannel as modifyC12_27_, rshadow0_1_.modifyTimestamp as modifyT13_27_, rshadow0_1_.name_norm as name_no14_27_, rshadow0_1_.name_orig as name_or15_27_, rshadow0_1_.objectTypeClass as objectT16_27_, rshadow0_1_.tenantRef_relation as tenantR17_27_, rshadow0_1_.tenantRef_targetOid as tenantR18_27_, rshadow0_1_.tenantRef_type as tenantR19_27_, rshadow0_1_.version as version20_27_, rshadow0_.attemptNumber as attemptN1_41_, rshadow0_.dead as dead2_41_, rshadow0_.exist as exist3_41_, rshadow0_.failedOperationType as failedOp4_41_, rshadow0_.fullSynchronizationTimestamp as fullSync5_41_, rshadow0_.intent as intent6_41_, rshadow0_.kind as kind7_41_, rshadow0_.name_norm as name_nor8_41_, rshadow0_.name_orig as name_ori9_41_, rshadow0_.objectClass as objectC10_41_, rshadow0_.pendingOperationCount as pending11_41_, rshadow0_.primaryIdentifierValue as primary12_41_, rshadow0_.resourceRef_relation as resourc13_41_, rshadow0_.resourceRef_targetOid as resourc14_41_, rshadow0_.resourceRef_type as resourc15_41_, rshadow0_.status as status16_41_, rshadow0_.synchronizationSituation as synchro17_41_, rshadow0_.synchronizationTimestamp as synchro18_41_ from m_shadow rshadow0_ inner join m_object rshadow0_1_ on rshadow0_.oid=rshadow0_1_.oid where rshadow0_.oid=?
 [1] select booleans0_.owner_oid as owner_oi2_28_0_, booleans0_.item_id as item_id1_28_0_, booleans0_.ownerType as ownerTyp3_28_0_, booleans0_.booleanValue as booleanV4_28_0_, booleans0_.item_id as item_id1_28_1_, booleans0_.owner_oid as owner_oi2_28_1_, booleans0_.ownerType as ownerTyp3_28_1_, booleans0_.booleanValue as booleanV4_28_1_ from m_object_ext_boolean booleans0_ where booleans0_.owner_oid=?
 [1] select dates0_.owner_oid as owner_oi2_29_0_, dates0_.item_id as item_id1_29_0_, dates0_.ownerType as ownerTyp3_29_0_, dates0_.dateValue as dateValu4_29_0_, dates0_.item_id as item_id1_29_1_, dates0_.owner_oid as owner_oi2_29_1_, dates0_.ownerType as ownerTyp3_29_1_, dates0_.dateValue as dateValu4_29_1_ from m_object_ext_date dates0_ where dates0_.owner_oid=?
 [1] select longs0_.owner_oid as owner_oi2_30_0_, longs0_.item_id as item_id1_30_0_, longs0_.ownerType as ownerTyp3_30_0_, longs0_.longValue as longValu4_30_0_, longs0_.item_id as item_id1_30_1_, longs0_.owner_oid as owner_oi2_30_1_, longs0_.ownerType as ownerTyp3_30_1_, longs0_.longValue as longValu4_30_1_ from m_object_ext_long longs0_ where longs0_.owner_oid=?
 [1] select polys0_.owner_oid as owner_oi2_31_0_, polys0_.item_id as item_id1_31_0_, polys0_.ownerType as ownerTyp3_31_0_, polys0_.orig as orig4_31_0_, polys0_.item_id as item_id1_31_1_, polys0_.owner_oid as owner_oi2_31_1_, polys0_.ownerType as ownerTyp3_31_1_, polys0_.orig as orig4_31_1_, polys0_.norm as norm5_31_1_ from m_object_ext_poly polys0_ where polys0_.owner_oid=?
 [1] select references0_.owner_oid as owner_oi2_32_0_, references0_.item_id as item_id1_32_0_, references0_.ownerType as ownerTyp3_32_0_, references0_.targetoid as targetoi4_32_0_, references0_.item_id as item_id1_32_1_, references0_.owner_oid as owner_oi2_32_1_, references0_.ownerType as ownerTyp3_32_1_, references0_.targetoid as targetoi4_32_1_, references0_.relation as relation5_32_1_, references0_.targetType as targetTy6_32_1_ from m_object_ext_reference references0_ where references0_.owner_oid=?
 [1] select strings0_.owner_oid as owner_oi2_33_0_, strings0_.item_id as item_id1_33_0_, strings0_.ownerType as ownerTyp3_33_0_, strings0_.stringValue as stringVa4_33_0_, strings0_.item_id as item_id1_33_1_, strings0_.owner_oid as owner_oi2_33_1_, strings0_.ownerType as ownerTyp3_33_1_, strings0_.stringValue as stringVa4_33_1_ from m_object_ext_string strings0_ where strings0_.owner_oid=?
 [1] update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
 [4] delete from m_object_ext_string where item_id=? and owner_oid=? and ownerType=? and stringValue=?
         */
        assertCounts(10, 13);
    }

    @Test
    public void test455AddWholeContainer() throws Exception {
        OperationResult result = new OperationResult(ExtensionTest.class.getName() + ".test455AddWholeContainer");

        expectedShadow.getValue().removeContainer(ShadowType.F_ATTRIBUTES); // just for sure
        PrismContainer<?> attributesContainer = shadowAttributesDefinition.instantiate();
        PrismContainerValue<?> attributes = attributesContainer.getValue();
        ResourceAttribute<String> name = attrGroupNameDefinition.instantiate();
        name.setRealValue("alumni");
        attributes.add(name);
        ResourceAttribute<String> member = attrMemberDefinition.instantiate();
        member.addRealValues("banderson", "kwhite");
        attributes.add(member);
        ResourceAttribute<String> manager = attrManagerDefinition.instantiate();
        manager.addRealValues("jack", "jim");
        attributes.add(manager);

        ObjectDelta<ShadowType> delta = getPrismContext().deltaFor(ShadowType.class)
                .item(ShadowType.F_ATTRIBUTES)
                .add(attributes.clone())
                .asObjectDelta("");
        delta.applyTo(expectedShadow);

        queryListener.start();
        repositoryService.modifyObject(ShadowType.class, shadowOid, delta.getModifications(), getOptions(), result);
        queryListener.dumpAndStop();

        try (EntityManager em = factory.createEntityManager()) {
            RShadow s = em.find(RShadow.class, shadowOid);
            assertExtension(s, itemGroupName, "alumni");
            assertExtension(s, itemMember, "banderson", "kwhite");
            assertExtension(s, itemManager, "jack", "jim");
        }

        assertGetObject(result);

        /*
 [1] select oid from m_object where oid = ? for update
 [1] select rshadow0_.oid as oid1_27_, rshadow0_1_.createChannel as createCh2_27_, rshadow0_1_.createTimestamp as createTi3_27_, rshadow0_1_.creatorRef_relation as creatorR4_27_, rshadow0_1_.creatorRef_targetOid as creatorR5_27_, rshadow0_1_.creatorRef_type as creatorR6_27_, rshadow0_1_.fullObject as fullObje7_27_, rshadow0_1_.lifecycleState as lifecycl8_27_, rshadow0_1_.modifierRef_relation as modifier9_27_, rshadow0_1_.modifierRef_targetOid as modifie10_27_, rshadow0_1_.modifierRef_type as modifie11_27_, rshadow0_1_.modifyChannel as modifyC12_27_, rshadow0_1_.modifyTimestamp as modifyT13_27_, rshadow0_1_.name_norm as name_no14_27_, rshadow0_1_.name_orig as name_or15_27_, rshadow0_1_.objectTypeClass as objectT16_27_, rshadow0_1_.tenantRef_relation as tenantR17_27_, rshadow0_1_.tenantRef_targetOid as tenantR18_27_, rshadow0_1_.tenantRef_type as tenantR19_27_, rshadow0_1_.version as version20_27_, rshadow0_.attemptNumber as attemptN1_41_, rshadow0_.dead as dead2_41_, rshadow0_.exist as exist3_41_, rshadow0_.failedOperationType as failedOp4_41_, rshadow0_.fullSynchronizationTimestamp as fullSync5_41_, rshadow0_.intent as intent6_41_, rshadow0_.kind as kind7_41_, rshadow0_.name_norm as name_nor8_41_, rshadow0_.name_orig as name_ori9_41_, rshadow0_.objectClass as objectC10_41_, rshadow0_.pendingOperationCount as pending11_41_, rshadow0_.primaryIdentifierValue as primary12_41_, rshadow0_.resourceRef_relation as resourc13_41_, rshadow0_.resourceRef_targetOid as resourc14_41_, rshadow0_.resourceRef_type as resourc15_41_, rshadow0_.status as status16_41_, rshadow0_.synchronizationSituation as synchro17_41_, rshadow0_.synchronizationTimestamp as synchro18_41_ from m_shadow rshadow0_ inner join m_object rshadow0_1_ on rshadow0_.oid=rshadow0_1_.oid where rshadow0_.oid=?
 [1] select booleans0_.owner_oid as owner_oi2_28_0_, booleans0_.item_id as item_id1_28_0_, booleans0_.ownerType as ownerTyp3_28_0_, booleans0_.booleanValue as booleanV4_28_0_, booleans0_.item_id as item_id1_28_1_, booleans0_.owner_oid as owner_oi2_28_1_, booleans0_.ownerType as ownerTyp3_28_1_, booleans0_.booleanValue as booleanV4_28_1_ from m_object_ext_boolean booleans0_ where booleans0_.owner_oid=?
 [1] select dates0_.owner_oid as owner_oi2_29_0_, dates0_.item_id as item_id1_29_0_, dates0_.ownerType as ownerTyp3_29_0_, dates0_.dateValue as dateValu4_29_0_, dates0_.item_id as item_id1_29_1_, dates0_.owner_oid as owner_oi2_29_1_, dates0_.ownerType as ownerTyp3_29_1_, dates0_.dateValue as dateValu4_29_1_ from m_object_ext_date dates0_ where dates0_.owner_oid=?
 [1] select longs0_.owner_oid as owner_oi2_30_0_, longs0_.item_id as item_id1_30_0_, longs0_.ownerType as ownerTyp3_30_0_, longs0_.longValue as longValu4_30_0_, longs0_.item_id as item_id1_30_1_, longs0_.owner_oid as owner_oi2_30_1_, longs0_.ownerType as ownerTyp3_30_1_, longs0_.longValue as longValu4_30_1_ from m_object_ext_long longs0_ where longs0_.owner_oid=?
 [1] select polys0_.owner_oid as owner_oi2_31_0_, polys0_.item_id as item_id1_31_0_, polys0_.ownerType as ownerTyp3_31_0_, polys0_.orig as orig4_31_0_, polys0_.item_id as item_id1_31_1_, polys0_.owner_oid as owner_oi2_31_1_, polys0_.ownerType as ownerTyp3_31_1_, polys0_.orig as orig4_31_1_, polys0_.norm as norm5_31_1_ from m_object_ext_poly polys0_ where polys0_.owner_oid=?
 [1] select references0_.owner_oid as owner_oi2_32_0_, references0_.item_id as item_id1_32_0_, references0_.ownerType as ownerTyp3_32_0_, references0_.targetoid as targetoi4_32_0_, references0_.item_id as item_id1_32_1_, references0_.owner_oid as owner_oi2_32_1_, references0_.ownerType as ownerTyp3_32_1_, references0_.targetoid as targetoi4_32_1_, references0_.relation as relation5_32_1_, references0_.targetType as targetTy6_32_1_ from m_object_ext_reference references0_ where references0_.owner_oid=?
 [1] select strings0_.owner_oid as owner_oi2_33_0_, strings0_.item_id as item_id1_33_0_, strings0_.ownerType as ownerTyp3_33_0_, strings0_.stringValue as stringVa4_33_0_, strings0_.item_id as item_id1_33_1_, strings0_.owner_oid as owner_oi2_33_1_, strings0_.ownerType as ownerTyp3_33_1_, strings0_.stringValue as stringVa4_33_1_ from m_object_ext_string strings0_ where strings0_.owner_oid=?
 [5] insert into m_object_ext_string (item_id, owner_oid, ownerType, stringValue) values (?, ?, ?, ?)
 [1] update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?

        safe insertions (can be improved)

 [1] select oid from m_object where oid = ? for update
 [1] select rshadow0_.oid as oid1_27_, rshadow0_1_.createChannel as createCh2_27_, rshadow0_1_.createTimestamp as createTi3_27_, rshadow0_1_.creatorRef_relation as creatorR4_27_, rshadow0_1_.creatorRef_targetOid as creatorR5_27_, rshadow0_1_.creatorRef_type as creatorR6_27_, rshadow0_1_.fullObject as fullObje7_27_, rshadow0_1_.lifecycleState as lifecycl8_27_, rshadow0_1_.modifierRef_relation as modifier9_27_, rshadow0_1_.modifierRef_targetOid as modifie10_27_, rshadow0_1_.modifierRef_type as modifie11_27_, rshadow0_1_.modifyChannel as modifyC12_27_, rshadow0_1_.modifyTimestamp as modifyT13_27_, rshadow0_1_.name_norm as name_no14_27_, rshadow0_1_.name_orig as name_or15_27_, rshadow0_1_.objectTypeClass as objectT16_27_, rshadow0_1_.tenantRef_relation as tenantR17_27_, rshadow0_1_.tenantRef_targetOid as tenantR18_27_, rshadow0_1_.tenantRef_type as tenantR19_27_, rshadow0_1_.version as version20_27_, rshadow0_.attemptNumber as attemptN1_41_, rshadow0_.dead as dead2_41_, rshadow0_.exist as exist3_41_, rshadow0_.failedOperationType as failedOp4_41_, rshadow0_.fullSynchronizationTimestamp as fullSync5_41_, rshadow0_.intent as intent6_41_, rshadow0_.kind as kind7_41_, rshadow0_.name_norm as name_nor8_41_, rshadow0_.name_orig as name_ori9_41_, rshadow0_.objectClass as objectC10_41_, rshadow0_.pendingOperationCount as pending11_41_, rshadow0_.primaryIdentifierValue as primary12_41_, rshadow0_.resourceRef_relation as resourc13_41_, rshadow0_.resourceRef_targetOid as resourc14_41_, rshadow0_.resourceRef_type as resourc15_41_, rshadow0_.status as status16_41_, rshadow0_.synchronizationSituation as synchro17_41_, rshadow0_.synchronizationTimestamp as synchro18_41_ from m_shadow rshadow0_ inner join m_object rshadow0_1_ on rshadow0_.oid=rshadow0_1_.oid where rshadow0_.oid=?
 [1] select booleans0_.owner_oid as owner_oi2_28_0_, booleans0_.item_id as item_id1_28_0_, booleans0_.ownerType as ownerTyp3_28_0_, booleans0_.booleanValue as booleanV4_28_0_, booleans0_.item_id as item_id1_28_1_, booleans0_.owner_oid as owner_oi2_28_1_, booleans0_.ownerType as ownerTyp3_28_1_, booleans0_.booleanValue as booleanV4_28_1_ from m_object_ext_boolean booleans0_ where booleans0_.owner_oid=?
 [1] select dates0_.owner_oid as owner_oi2_29_0_, dates0_.item_id as item_id1_29_0_, dates0_.ownerType as ownerTyp3_29_0_, dates0_.dateValue as dateValu4_29_0_, dates0_.item_id as item_id1_29_1_, dates0_.owner_oid as owner_oi2_29_1_, dates0_.ownerType as ownerTyp3_29_1_, dates0_.dateValue as dateValu4_29_1_ from m_object_ext_date dates0_ where dates0_.owner_oid=?
 [1] select longs0_.owner_oid as owner_oi2_30_0_, longs0_.item_id as item_id1_30_0_, longs0_.ownerType as ownerTyp3_30_0_, longs0_.longValue as longValu4_30_0_, longs0_.item_id as item_id1_30_1_, longs0_.owner_oid as owner_oi2_30_1_, longs0_.ownerType as ownerTyp3_30_1_, longs0_.longValue as longValu4_30_1_ from m_object_ext_long longs0_ where longs0_.owner_oid=?
 [1] select polys0_.owner_oid as owner_oi2_31_0_, polys0_.item_id as item_id1_31_0_, polys0_.ownerType as ownerTyp3_31_0_, polys0_.orig as orig4_31_0_, polys0_.item_id as item_id1_31_1_, polys0_.owner_oid as owner_oi2_31_1_, polys0_.ownerType as ownerTyp3_31_1_, polys0_.orig as orig4_31_1_, polys0_.norm as norm5_31_1_ from m_object_ext_poly polys0_ where polys0_.owner_oid=?
 [1] select references0_.owner_oid as owner_oi2_32_0_, references0_.item_id as item_id1_32_0_, references0_.ownerType as ownerTyp3_32_0_, references0_.targetoid as targetoi4_32_0_, references0_.item_id as item_id1_32_1_, references0_.owner_oid as owner_oi2_32_1_, references0_.ownerType as ownerTyp3_32_1_, references0_.targetoid as targetoi4_32_1_, references0_.relation as relation5_32_1_, references0_.targetType as targetTy6_32_1_ from m_object_ext_reference references0_ where references0_.owner_oid=?
 [1] select strings0_.owner_oid as owner_oi2_33_0_, strings0_.item_id as item_id1_33_0_, strings0_.ownerType as ownerTyp3_33_0_, strings0_.stringValue as stringVa4_33_0_, strings0_.item_id as item_id1_33_1_, strings0_.owner_oid as owner_oi2_33_1_, strings0_.ownerType as ownerTyp3_33_1_, strings0_.stringValue as stringVa4_33_1_ from m_object_ext_string strings0_ where strings0_.owner_oid=?
 [1] select roextstrin0_.item_id as item_id1_33_0_, roextstrin0_.owner_oid as owner_oi2_33_0_, roextstrin0_.ownerType as ownerTyp3_33_0_, roextstrin0_.stringValue as stringVa4_33_0_ from m_object_ext_string roextstrin0_ where roextstrin0_.item_id=? and roextstrin0_.owner_oid=? and roextstrin0_.ownerType=? and roextstrin0_.stringValue=?
 [1] select roextstrin0_.item_id as item_id1_33_0_, roextstrin0_.owner_oid as owner_oi2_33_0_, roextstrin0_.ownerType as ownerTyp3_33_0_, roextstrin0_.stringValue as stringVa4_33_0_ from m_object_ext_string roextstrin0_ where roextstrin0_.item_id=? and roextstrin0_.owner_oid=? and roextstrin0_.ownerType=? and roextstrin0_.stringValue=?
 [1] select roextstrin0_.item_id as item_id1_33_0_, roextstrin0_.owner_oid as owner_oi2_33_0_, roextstrin0_.ownerType as ownerTyp3_33_0_, roextstrin0_.stringValue as stringVa4_33_0_ from m_object_ext_string roextstrin0_ where roextstrin0_.item_id=? and roextstrin0_.owner_oid=? and roextstrin0_.ownerType=? and roextstrin0_.stringValue=?
 [1] select roextstrin0_.item_id as item_id1_33_0_, roextstrin0_.owner_oid as owner_oi2_33_0_, roextstrin0_.ownerType as ownerTyp3_33_0_, roextstrin0_.stringValue as stringVa4_33_0_ from m_object_ext_string roextstrin0_ where roextstrin0_.item_id=? and roextstrin0_.owner_oid=? and roextstrin0_.ownerType=? and roextstrin0_.stringValue=?
 [1] select roextstrin0_.item_id as item_id1_33_0_, roextstrin0_.owner_oid as owner_oi2_33_0_, roextstrin0_.ownerType as ownerTyp3_33_0_, roextstrin0_.stringValue as stringVa4_33_0_ from m_object_ext_string roextstrin0_ where roextstrin0_.item_id=? and roextstrin0_.owner_oid=? and roextstrin0_.ownerType=? and roextstrin0_.stringValue=?
 [5] insert into m_object_ext_string (item_id, owner_oid, ownerType, stringValue) values (?, ?, ?, ?)
 [1] update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
         */
        assertCounts(10 + getExtraSafeInsertionSelects(5), 14 + getExtraSafeInsertionSelects(5));
    }

    /**
     * This is really tricky. We try to add another value to single-valued attributes container.
     */
    @Test
    public void test458AddWholeContainerDifferentValue() throws Exception {
        OperationResult result = new OperationResult(ExtensionTest.class.getName() + ".test458AddWholeContainerDifferentValue");

        PrismContainer<?> attributesContainer = shadowAttributesDefinition.instantiate();
        PrismContainerValue<?> attributes = attributesContainer.getValue();
        ResourceAttribute<String> name = attrGroupNameDefinition.instantiate();
        name.setRealValue("alumni2");
        attributes.add(name);
        ResourceAttribute<String> member = attrMemberDefinition.instantiate();
        member.addRealValues("banderson2", "kwhite2");
        attributes.add(member);
        ResourceAttribute<String> manager = attrManagerDefinition.instantiate();
        manager.addRealValues("jack2", "jim2");
        attributes.add(manager);

        ObjectDelta<ShadowType> delta = getPrismContext().deltaFor(ShadowType.class)
                .item(ShadowType.F_ATTRIBUTES)
                .add(attributes.clone())
                .asObjectDelta("");
        delta.applyTo(expectedShadow);

        queryListener.start();
        repositoryService.modifyObject(ShadowType.class, shadowOid, delta.getModifications(), getOptions(), result);
        queryListener.dumpAndStop();

        try (EntityManager em = factory.createEntityManager()) {
            RShadow s = em.find(RShadow.class, shadowOid);
            assertExtension(s, itemGroupName, "alumni2");
            assertExtension(s, itemMember, "banderson2", "kwhite2");
            assertExtension(s, itemManager, "jack2", "jim2");
        }

        assertGetObject(result);

        /*
 [1] select oid from m_object where oid = ? for update
 [1] select rshadow0_.oid as oid1_27_, rshadow0_1_.createChannel as createCh2_27_, rshadow0_1_.createTimestamp as createTi3_27_, rshadow0_1_.creatorRef_relation as creatorR4_27_, rshadow0_1_.creatorRef_targetOid as creatorR5_27_, rshadow0_1_.creatorRef_type as creatorR6_27_, rshadow0_1_.fullObject as fullObje7_27_, rshadow0_1_.lifecycleState as lifecycl8_27_, rshadow0_1_.modifierRef_relation as modifier9_27_, rshadow0_1_.modifierRef_targetOid as modifie10_27_, rshadow0_1_.modifierRef_type as modifie11_27_, rshadow0_1_.modifyChannel as modifyC12_27_, rshadow0_1_.modifyTimestamp as modifyT13_27_, rshadow0_1_.name_norm as name_no14_27_, rshadow0_1_.name_orig as name_or15_27_, rshadow0_1_.objectTypeClass as objectT16_27_, rshadow0_1_.tenantRef_relation as tenantR17_27_, rshadow0_1_.tenantRef_targetOid as tenantR18_27_, rshadow0_1_.tenantRef_type as tenantR19_27_, rshadow0_1_.version as version20_27_, rshadow0_.attemptNumber as attemptN1_41_, rshadow0_.dead as dead2_41_, rshadow0_.exist as exist3_41_, rshadow0_.failedOperationType as failedOp4_41_, rshadow0_.fullSynchronizationTimestamp as fullSync5_41_, rshadow0_.intent as intent6_41_, rshadow0_.kind as kind7_41_, rshadow0_.name_norm as name_nor8_41_, rshadow0_.name_orig as name_ori9_41_, rshadow0_.objectClass as objectC10_41_, rshadow0_.pendingOperationCount as pending11_41_, rshadow0_.primaryIdentifierValue as primary12_41_, rshadow0_.resourceRef_relation as resourc13_41_, rshadow0_.resourceRef_targetOid as resourc14_41_, rshadow0_.resourceRef_type as resourc15_41_, rshadow0_.status as status16_41_, rshadow0_.synchronizationSituation as synchro17_41_, rshadow0_.synchronizationTimestamp as synchro18_41_ from m_shadow rshadow0_ inner join m_object rshadow0_1_ on rshadow0_.oid=rshadow0_1_.oid where rshadow0_.oid=?
 [1] select booleans0_.owner_oid as owner_oi2_28_0_, booleans0_.item_id as item_id1_28_0_, booleans0_.ownerType as ownerTyp3_28_0_, booleans0_.booleanValue as booleanV4_28_0_, booleans0_.item_id as item_id1_28_1_, booleans0_.owner_oid as owner_oi2_28_1_, booleans0_.ownerType as ownerTyp3_28_1_, booleans0_.booleanValue as booleanV4_28_1_ from m_object_ext_boolean booleans0_ where booleans0_.owner_oid=?
 [1] select dates0_.owner_oid as owner_oi2_29_0_, dates0_.item_id as item_id1_29_0_, dates0_.ownerType as ownerTyp3_29_0_, dates0_.dateValue as dateValu4_29_0_, dates0_.item_id as item_id1_29_1_, dates0_.owner_oid as owner_oi2_29_1_, dates0_.ownerType as ownerTyp3_29_1_, dates0_.dateValue as dateValu4_29_1_ from m_object_ext_date dates0_ where dates0_.owner_oid=?
 [1] select longs0_.owner_oid as owner_oi2_30_0_, longs0_.item_id as item_id1_30_0_, longs0_.ownerType as ownerTyp3_30_0_, longs0_.longValue as longValu4_30_0_, longs0_.item_id as item_id1_30_1_, longs0_.owner_oid as owner_oi2_30_1_, longs0_.ownerType as ownerTyp3_30_1_, longs0_.longValue as longValu4_30_1_ from m_object_ext_long longs0_ where longs0_.owner_oid=?
 [1] select polys0_.owner_oid as owner_oi2_31_0_, polys0_.item_id as item_id1_31_0_, polys0_.ownerType as ownerTyp3_31_0_, polys0_.orig as orig4_31_0_, polys0_.item_id as item_id1_31_1_, polys0_.owner_oid as owner_oi2_31_1_, polys0_.ownerType as ownerTyp3_31_1_, polys0_.orig as orig4_31_1_, polys0_.norm as norm5_31_1_ from m_object_ext_poly polys0_ where polys0_.owner_oid=?
 [1] select references0_.owner_oid as owner_oi2_32_0_, references0_.item_id as item_id1_32_0_, references0_.ownerType as ownerTyp3_32_0_, references0_.targetoid as targetoi4_32_0_, references0_.item_id as item_id1_32_1_, references0_.owner_oid as owner_oi2_32_1_, references0_.ownerType as ownerTyp3_32_1_, references0_.targetoid as targetoi4_32_1_, references0_.relation as relation5_32_1_, references0_.targetType as targetTy6_32_1_ from m_object_ext_reference references0_ where references0_.owner_oid=?
 [1] select strings0_.owner_oid as owner_oi2_33_0_, strings0_.item_id as item_id1_33_0_, strings0_.ownerType as ownerTyp3_33_0_, strings0_.stringValue as stringVa4_33_0_, strings0_.item_id as item_id1_33_1_, strings0_.owner_oid as owner_oi2_33_1_, strings0_.ownerType as ownerTyp3_33_1_, strings0_.stringValue as stringVa4_33_1_ from m_object_ext_string strings0_ where strings0_.owner_oid=?
 [5] insert into m_object_ext_string (item_id, owner_oid, ownerType, stringValue) values (?, ?, ?, ?)
 [1] update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
 [5] delete from m_object_ext_string where item_id=? and owner_oid=? and ownerType=? and stringValue=?

        safe insertions (can be improved)

 [1] select oid from m_object where oid = ? for update
 [1] select rshadow0_.oid as oid1_27_, rshadow0_1_.createChannel as createCh2_27_, rshadow0_1_.createTimestamp as createTi3_27_, rshadow0_1_.creatorRef_relation as creatorR4_27_, rshadow0_1_.creatorRef_targetOid as creatorR5_27_, rshadow0_1_.creatorRef_type as creatorR6_27_, rshadow0_1_.fullObject as fullObje7_27_, rshadow0_1_.lifecycleState as lifecycl8_27_, rshadow0_1_.modifierRef_relation as modifier9_27_, rshadow0_1_.modifierRef_targetOid as modifie10_27_, rshadow0_1_.modifierRef_type as modifie11_27_, rshadow0_1_.modifyChannel as modifyC12_27_, rshadow0_1_.modifyTimestamp as modifyT13_27_, rshadow0_1_.name_norm as name_no14_27_, rshadow0_1_.name_orig as name_or15_27_, rshadow0_1_.objectTypeClass as objectT16_27_, rshadow0_1_.tenantRef_relation as tenantR17_27_, rshadow0_1_.tenantRef_targetOid as tenantR18_27_, rshadow0_1_.tenantRef_type as tenantR19_27_, rshadow0_1_.version as version20_27_, rshadow0_.attemptNumber as attemptN1_41_, rshadow0_.dead as dead2_41_, rshadow0_.exist as exist3_41_, rshadow0_.failedOperationType as failedOp4_41_, rshadow0_.fullSynchronizationTimestamp as fullSync5_41_, rshadow0_.intent as intent6_41_, rshadow0_.kind as kind7_41_, rshadow0_.name_norm as name_nor8_41_, rshadow0_.name_orig as name_ori9_41_, rshadow0_.objectClass as objectC10_41_, rshadow0_.pendingOperationCount as pending11_41_, rshadow0_.primaryIdentifierValue as primary12_41_, rshadow0_.resourceRef_relation as resourc13_41_, rshadow0_.resourceRef_targetOid as resourc14_41_, rshadow0_.resourceRef_type as resourc15_41_, rshadow0_.status as status16_41_, rshadow0_.synchronizationSituation as synchro17_41_, rshadow0_.synchronizationTimestamp as synchro18_41_ from m_shadow rshadow0_ inner join m_object rshadow0_1_ on rshadow0_.oid=rshadow0_1_.oid where rshadow0_.oid=?
 [1] select booleans0_.owner_oid as owner_oi2_28_0_, booleans0_.item_id as item_id1_28_0_, booleans0_.ownerType as ownerTyp3_28_0_, booleans0_.booleanValue as booleanV4_28_0_, booleans0_.item_id as item_id1_28_1_, booleans0_.owner_oid as owner_oi2_28_1_, booleans0_.ownerType as ownerTyp3_28_1_, booleans0_.booleanValue as booleanV4_28_1_ from m_object_ext_boolean booleans0_ where booleans0_.owner_oid=?
 [1] select dates0_.owner_oid as owner_oi2_29_0_, dates0_.item_id as item_id1_29_0_, dates0_.ownerType as ownerTyp3_29_0_, dates0_.dateValue as dateValu4_29_0_, dates0_.item_id as item_id1_29_1_, dates0_.owner_oid as owner_oi2_29_1_, dates0_.ownerType as ownerTyp3_29_1_, dates0_.dateValue as dateValu4_29_1_ from m_object_ext_date dates0_ where dates0_.owner_oid=?
 [1] select longs0_.owner_oid as owner_oi2_30_0_, longs0_.item_id as item_id1_30_0_, longs0_.ownerType as ownerTyp3_30_0_, longs0_.longValue as longValu4_30_0_, longs0_.item_id as item_id1_30_1_, longs0_.owner_oid as owner_oi2_30_1_, longs0_.ownerType as ownerTyp3_30_1_, longs0_.longValue as longValu4_30_1_ from m_object_ext_long longs0_ where longs0_.owner_oid=?
 [1] select polys0_.owner_oid as owner_oi2_31_0_, polys0_.item_id as item_id1_31_0_, polys0_.ownerType as ownerTyp3_31_0_, polys0_.orig as orig4_31_0_, polys0_.item_id as item_id1_31_1_, polys0_.owner_oid as owner_oi2_31_1_, polys0_.ownerType as ownerTyp3_31_1_, polys0_.orig as orig4_31_1_, polys0_.norm as norm5_31_1_ from m_object_ext_poly polys0_ where polys0_.owner_oid=?
 [1] select references0_.owner_oid as owner_oi2_32_0_, references0_.item_id as item_id1_32_0_, references0_.ownerType as ownerTyp3_32_0_, references0_.targetoid as targetoi4_32_0_, references0_.item_id as item_id1_32_1_, references0_.owner_oid as owner_oi2_32_1_, references0_.ownerType as ownerTyp3_32_1_, references0_.targetoid as targetoi4_32_1_, references0_.relation as relation5_32_1_, references0_.targetType as targetTy6_32_1_ from m_object_ext_reference references0_ where references0_.owner_oid=?
 [1] select strings0_.owner_oid as owner_oi2_33_0_, strings0_.item_id as item_id1_33_0_, strings0_.ownerType as ownerTyp3_33_0_, strings0_.stringValue as stringVa4_33_0_, strings0_.item_id as item_id1_33_1_, strings0_.owner_oid as owner_oi2_33_1_, strings0_.ownerType as ownerTyp3_33_1_, strings0_.stringValue as stringVa4_33_1_ from m_object_ext_string strings0_ where strings0_.owner_oid=?
 [1] select roextstrin0_.item_id as item_id1_33_0_, roextstrin0_.owner_oid as owner_oi2_33_0_, roextstrin0_.ownerType as ownerTyp3_33_0_, roextstrin0_.stringValue as stringVa4_33_0_ from m_object_ext_string roextstrin0_ where roextstrin0_.item_id=? and roextstrin0_.owner_oid=? and roextstrin0_.ownerType=? and roextstrin0_.stringValue=?
 [1] select roextstrin0_.item_id as item_id1_33_0_, roextstrin0_.owner_oid as owner_oi2_33_0_, roextstrin0_.ownerType as ownerTyp3_33_0_, roextstrin0_.stringValue as stringVa4_33_0_ from m_object_ext_string roextstrin0_ where roextstrin0_.item_id=? and roextstrin0_.owner_oid=? and roextstrin0_.ownerType=? and roextstrin0_.stringValue=?
 [1] select roextstrin0_.item_id as item_id1_33_0_, roextstrin0_.owner_oid as owner_oi2_33_0_, roextstrin0_.ownerType as ownerTyp3_33_0_, roextstrin0_.stringValue as stringVa4_33_0_ from m_object_ext_string roextstrin0_ where roextstrin0_.item_id=? and roextstrin0_.owner_oid=? and roextstrin0_.ownerType=? and roextstrin0_.stringValue=?
 [1] select roextstrin0_.item_id as item_id1_33_0_, roextstrin0_.owner_oid as owner_oi2_33_0_, roextstrin0_.ownerType as ownerTyp3_33_0_, roextstrin0_.stringValue as stringVa4_33_0_ from m_object_ext_string roextstrin0_ where roextstrin0_.item_id=? and roextstrin0_.owner_oid=? and roextstrin0_.ownerType=? and roextstrin0_.stringValue=?
 [1] select roextstrin0_.item_id as item_id1_33_0_, roextstrin0_.owner_oid as owner_oi2_33_0_, roextstrin0_.ownerType as ownerTyp3_33_0_, roextstrin0_.stringValue as stringVa4_33_0_ from m_object_ext_string roextstrin0_ where roextstrin0_.item_id=? and roextstrin0_.owner_oid=? and roextstrin0_.ownerType=? and roextstrin0_.stringValue=?
 [5] insert into m_object_ext_string (item_id, owner_oid, ownerType, stringValue) values (?, ?, ?, ?)
 [1] update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
 [5] delete from m_object_ext_string where item_id=? and owner_oid=? and ownerType=? and stringValue=?
         */
        assertCounts(11 + getExtraSafeInsertionSelects(5), 19 + getExtraSafeInsertionSelects(5));
    }

    @Test
    public void test460ReplaceWholeContainer() throws Exception {
        OperationResult result = new OperationResult(ExtensionTest.class.getName() + ".test460ReplaceWholeContainer");

        PrismContainer<?> attributesContainer = shadowAttributesDefinition.instantiate();
        PrismContainerValue<?> attributes = attributesContainer.getValue();
        ResourceAttribute<String> name = attrGroupNameDefinition.instantiate();
        name.setRealValue("alumni3");
        attributes.add(name);
        ResourceAttribute<String> member = attrMemberDefinition.instantiate();
        member.addRealValues("banderson3", "kwhite3");
        attributes.add(member);
        ResourceAttribute<String> manager = attrManagerDefinition.instantiate();
        manager.addRealValues("jack3", "jim3");
        attributes.add(manager);

        ObjectDelta<ShadowType> delta = getPrismContext().deltaFor(ShadowType.class)
                .item(ShadowType.F_ATTRIBUTES)
                .replace(attributes.clone())
                .asObjectDelta("");
        delta.applyTo(expectedShadow);

        queryListener.start();
        repositoryService.modifyObject(ShadowType.class, shadowOid, delta.getModifications(), getOptions(), result);
        queryListener.dumpAndStop();

        try (EntityManager em = factory.createEntityManager()) {
            RShadow s = em.find(RShadow.class, shadowOid);
            assertExtension(s, itemGroupName, "alumni3");
            assertExtension(s, itemMember, "banderson3", "kwhite3");
            assertExtension(s, itemManager, "jack3", "jim3");
        }

        assertGetObject(result);

        /*
 [1] select oid from m_object where oid = ? for update
 [1] select rshadow0_.oid as oid1_27_, rshadow0_1_.createChannel as createCh2_27_, rshadow0_1_.createTimestamp as createTi3_27_, rshadow0_1_.creatorRef_relation as creatorR4_27_, rshadow0_1_.creatorRef_targetOid as creatorR5_27_, rshadow0_1_.creatorRef_type as creatorR6_27_, rshadow0_1_.fullObject as fullObje7_27_, rshadow0_1_.lifecycleState as lifecycl8_27_, rshadow0_1_.modifierRef_relation as modifier9_27_, rshadow0_1_.modifierRef_targetOid as modifie10_27_, rshadow0_1_.modifierRef_type as modifie11_27_, rshadow0_1_.modifyChannel as modifyC12_27_, rshadow0_1_.modifyTimestamp as modifyT13_27_, rshadow0_1_.name_norm as name_no14_27_, rshadow0_1_.name_orig as name_or15_27_, rshadow0_1_.objectTypeClass as objectT16_27_, rshadow0_1_.tenantRef_relation as tenantR17_27_, rshadow0_1_.tenantRef_targetOid as tenantR18_27_, rshadow0_1_.tenantRef_type as tenantR19_27_, rshadow0_1_.version as version20_27_, rshadow0_.attemptNumber as attemptN1_41_, rshadow0_.dead as dead2_41_, rshadow0_.exist as exist3_41_, rshadow0_.failedOperationType as failedOp4_41_, rshadow0_.fullSynchronizationTimestamp as fullSync5_41_, rshadow0_.intent as intent6_41_, rshadow0_.kind as kind7_41_, rshadow0_.name_norm as name_nor8_41_, rshadow0_.name_orig as name_ori9_41_, rshadow0_.objectClass as objectC10_41_, rshadow0_.pendingOperationCount as pending11_41_, rshadow0_.primaryIdentifierValue as primary12_41_, rshadow0_.resourceRef_relation as resourc13_41_, rshadow0_.resourceRef_targetOid as resourc14_41_, rshadow0_.resourceRef_type as resourc15_41_, rshadow0_.status as status16_41_, rshadow0_.synchronizationSituation as synchro17_41_, rshadow0_.synchronizationTimestamp as synchro18_41_ from m_shadow rshadow0_ inner join m_object rshadow0_1_ on rshadow0_.oid=rshadow0_1_.oid where rshadow0_.oid=?
 [1] select booleans0_.owner_oid as owner_oi2_28_0_, booleans0_.item_id as item_id1_28_0_, booleans0_.ownerType as ownerTyp3_28_0_, booleans0_.booleanValue as booleanV4_28_0_, booleans0_.item_id as item_id1_28_1_, booleans0_.owner_oid as owner_oi2_28_1_, booleans0_.ownerType as ownerTyp3_28_1_, booleans0_.booleanValue as booleanV4_28_1_ from m_object_ext_boolean booleans0_ where booleans0_.owner_oid=?
 [1] select dates0_.owner_oid as owner_oi2_29_0_, dates0_.item_id as item_id1_29_0_, dates0_.ownerType as ownerTyp3_29_0_, dates0_.dateValue as dateValu4_29_0_, dates0_.item_id as item_id1_29_1_, dates0_.owner_oid as owner_oi2_29_1_, dates0_.ownerType as ownerTyp3_29_1_, dates0_.dateValue as dateValu4_29_1_ from m_object_ext_date dates0_ where dates0_.owner_oid=?
 [1] select longs0_.owner_oid as owner_oi2_30_0_, longs0_.item_id as item_id1_30_0_, longs0_.ownerType as ownerTyp3_30_0_, longs0_.longValue as longValu4_30_0_, longs0_.item_id as item_id1_30_1_, longs0_.owner_oid as owner_oi2_30_1_, longs0_.ownerType as ownerTyp3_30_1_, longs0_.longValue as longValu4_30_1_ from m_object_ext_long longs0_ where longs0_.owner_oid=?
 [1] select polys0_.owner_oid as owner_oi2_31_0_, polys0_.item_id as item_id1_31_0_, polys0_.ownerType as ownerTyp3_31_0_, polys0_.orig as orig4_31_0_, polys0_.item_id as item_id1_31_1_, polys0_.owner_oid as owner_oi2_31_1_, polys0_.ownerType as ownerTyp3_31_1_, polys0_.orig as orig4_31_1_, polys0_.norm as norm5_31_1_ from m_object_ext_poly polys0_ where polys0_.owner_oid=?
 [1] select references0_.owner_oid as owner_oi2_32_0_, references0_.item_id as item_id1_32_0_, references0_.ownerType as ownerTyp3_32_0_, references0_.targetoid as targetoi4_32_0_, references0_.item_id as item_id1_32_1_, references0_.owner_oid as owner_oi2_32_1_, references0_.ownerType as ownerTyp3_32_1_, references0_.targetoid as targetoi4_32_1_, references0_.relation as relation5_32_1_, references0_.targetType as targetTy6_32_1_ from m_object_ext_reference references0_ where references0_.owner_oid=?
 [1] select strings0_.owner_oid as owner_oi2_33_0_, strings0_.item_id as item_id1_33_0_, strings0_.ownerType as ownerTyp3_33_0_, strings0_.stringValue as stringVa4_33_0_, strings0_.item_id as item_id1_33_1_, strings0_.owner_oid as owner_oi2_33_1_, strings0_.ownerType as ownerTyp3_33_1_, strings0_.stringValue as stringVa4_33_1_ from m_object_ext_string strings0_ where strings0_.owner_oid=?
 [5] insert into m_object_ext_string (item_id, owner_oid, ownerType, stringValue) values (?, ?, ?, ?)
 [1] update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
 [5] delete from m_object_ext_string where item_id=? and owner_oid=? and ownerType=? and stringValue=?

        safe insertions (can be improved)

 [1] select oid from m_object where oid = ? for update
 [1] select rshadow0_.oid as oid1_27_, rshadow0_1_.createChannel as createCh2_27_, rshadow0_1_.createTimestamp as createTi3_27_, rshadow0_1_.creatorRef_relation as creatorR4_27_, rshadow0_1_.creatorRef_targetOid as creatorR5_27_, rshadow0_1_.creatorRef_type as creatorR6_27_, rshadow0_1_.fullObject as fullObje7_27_, rshadow0_1_.lifecycleState as lifecycl8_27_, rshadow0_1_.modifierRef_relation as modifier9_27_, rshadow0_1_.modifierRef_targetOid as modifie10_27_, rshadow0_1_.modifierRef_type as modifie11_27_, rshadow0_1_.modifyChannel as modifyC12_27_, rshadow0_1_.modifyTimestamp as modifyT13_27_, rshadow0_1_.name_norm as name_no14_27_, rshadow0_1_.name_orig as name_or15_27_, rshadow0_1_.objectTypeClass as objectT16_27_, rshadow0_1_.tenantRef_relation as tenantR17_27_, rshadow0_1_.tenantRef_targetOid as tenantR18_27_, rshadow0_1_.tenantRef_type as tenantR19_27_, rshadow0_1_.version as version20_27_, rshadow0_.attemptNumber as attemptN1_41_, rshadow0_.dead as dead2_41_, rshadow0_.exist as exist3_41_, rshadow0_.failedOperationType as failedOp4_41_, rshadow0_.fullSynchronizationTimestamp as fullSync5_41_, rshadow0_.intent as intent6_41_, rshadow0_.kind as kind7_41_, rshadow0_.name_norm as name_nor8_41_, rshadow0_.name_orig as name_ori9_41_, rshadow0_.objectClass as objectC10_41_, rshadow0_.pendingOperationCount as pending11_41_, rshadow0_.primaryIdentifierValue as primary12_41_, rshadow0_.resourceRef_relation as resourc13_41_, rshadow0_.resourceRef_targetOid as resourc14_41_, rshadow0_.resourceRef_type as resourc15_41_, rshadow0_.status as status16_41_, rshadow0_.synchronizationSituation as synchro17_41_, rshadow0_.synchronizationTimestamp as synchro18_41_ from m_shadow rshadow0_ inner join m_object rshadow0_1_ on rshadow0_.oid=rshadow0_1_.oid where rshadow0_.oid=?
 [1] select booleans0_.owner_oid as owner_oi2_28_0_, booleans0_.item_id as item_id1_28_0_, booleans0_.ownerType as ownerTyp3_28_0_, booleans0_.booleanValue as booleanV4_28_0_, booleans0_.item_id as item_id1_28_1_, booleans0_.owner_oid as owner_oi2_28_1_, booleans0_.ownerType as ownerTyp3_28_1_, booleans0_.booleanValue as booleanV4_28_1_ from m_object_ext_boolean booleans0_ where booleans0_.owner_oid=?
 [1] select dates0_.owner_oid as owner_oi2_29_0_, dates0_.item_id as item_id1_29_0_, dates0_.ownerType as ownerTyp3_29_0_, dates0_.dateValue as dateValu4_29_0_, dates0_.item_id as item_id1_29_1_, dates0_.owner_oid as owner_oi2_29_1_, dates0_.ownerType as ownerTyp3_29_1_, dates0_.dateValue as dateValu4_29_1_ from m_object_ext_date dates0_ where dates0_.owner_oid=?
 [1] select longs0_.owner_oid as owner_oi2_30_0_, longs0_.item_id as item_id1_30_0_, longs0_.ownerType as ownerTyp3_30_0_, longs0_.longValue as longValu4_30_0_, longs0_.item_id as item_id1_30_1_, longs0_.owner_oid as owner_oi2_30_1_, longs0_.ownerType as ownerTyp3_30_1_, longs0_.longValue as longValu4_30_1_ from m_object_ext_long longs0_ where longs0_.owner_oid=?
 [1] select polys0_.owner_oid as owner_oi2_31_0_, polys0_.item_id as item_id1_31_0_, polys0_.ownerType as ownerTyp3_31_0_, polys0_.orig as orig4_31_0_, polys0_.item_id as item_id1_31_1_, polys0_.owner_oid as owner_oi2_31_1_, polys0_.ownerType as ownerTyp3_31_1_, polys0_.orig as orig4_31_1_, polys0_.norm as norm5_31_1_ from m_object_ext_poly polys0_ where polys0_.owner_oid=?
 [1] select references0_.owner_oid as owner_oi2_32_0_, references0_.item_id as item_id1_32_0_, references0_.ownerType as ownerTyp3_32_0_, references0_.targetoid as targetoi4_32_0_, references0_.item_id as item_id1_32_1_, references0_.owner_oid as owner_oi2_32_1_, references0_.ownerType as ownerTyp3_32_1_, references0_.targetoid as targetoi4_32_1_, references0_.relation as relation5_32_1_, references0_.targetType as targetTy6_32_1_ from m_object_ext_reference references0_ where references0_.owner_oid=?
 [1] select strings0_.owner_oid as owner_oi2_33_0_, strings0_.item_id as item_id1_33_0_, strings0_.ownerType as ownerTyp3_33_0_, strings0_.stringValue as stringVa4_33_0_, strings0_.item_id as item_id1_33_1_, strings0_.owner_oid as owner_oi2_33_1_, strings0_.ownerType as ownerTyp3_33_1_, strings0_.stringValue as stringVa4_33_1_ from m_object_ext_string strings0_ where strings0_.owner_oid=?
 [1] select roextstrin0_.item_id as item_id1_33_0_, roextstrin0_.owner_oid as owner_oi2_33_0_, roextstrin0_.ownerType as ownerTyp3_33_0_, roextstrin0_.stringValue as stringVa4_33_0_ from m_object_ext_string roextstrin0_ where roextstrin0_.item_id=? and roextstrin0_.owner_oid=? and roextstrin0_.ownerType=? and roextstrin0_.stringValue=?
 [1] select roextstrin0_.item_id as item_id1_33_0_, roextstrin0_.owner_oid as owner_oi2_33_0_, roextstrin0_.ownerType as ownerTyp3_33_0_, roextstrin0_.stringValue as stringVa4_33_0_ from m_object_ext_string roextstrin0_ where roextstrin0_.item_id=? and roextstrin0_.owner_oid=? and roextstrin0_.ownerType=? and roextstrin0_.stringValue=?
 [1] select roextstrin0_.item_id as item_id1_33_0_, roextstrin0_.owner_oid as owner_oi2_33_0_, roextstrin0_.ownerType as ownerTyp3_33_0_, roextstrin0_.stringValue as stringVa4_33_0_ from m_object_ext_string roextstrin0_ where roextstrin0_.item_id=? and roextstrin0_.owner_oid=? and roextstrin0_.ownerType=? and roextstrin0_.stringValue=?
 [1] select roextstrin0_.item_id as item_id1_33_0_, roextstrin0_.owner_oid as owner_oi2_33_0_, roextstrin0_.ownerType as ownerTyp3_33_0_, roextstrin0_.stringValue as stringVa4_33_0_ from m_object_ext_string roextstrin0_ where roextstrin0_.item_id=? and roextstrin0_.owner_oid=? and roextstrin0_.ownerType=? and roextstrin0_.stringValue=?
 [1] select roextstrin0_.item_id as item_id1_33_0_, roextstrin0_.owner_oid as owner_oi2_33_0_, roextstrin0_.ownerType as ownerTyp3_33_0_, roextstrin0_.stringValue as stringVa4_33_0_ from m_object_ext_string roextstrin0_ where roextstrin0_.item_id=? and roextstrin0_.owner_oid=? and roextstrin0_.ownerType=? and roextstrin0_.stringValue=?
 [5] insert into m_object_ext_string (item_id, owner_oid, ownerType, stringValue) values (?, ?, ?, ?)
 [1] update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
 [5] delete from m_object_ext_string where item_id=? and owner_oid=? and ownerType=? and stringValue=?
         */
        assertCounts(11 + getExtraSafeInsertionSelects(5), 19 + getExtraSafeInsertionSelects(5));
    }

    @Test
    public void test465ReplaceWholeContainerToNull() throws Exception {
        OperationResult result = new OperationResult(ExtensionTest.class.getName() + ".test465ReplaceWholeContainerToNull");

        ObjectDelta<ShadowType> delta = getPrismContext().deltaFor(ShadowType.class)
                .item(ShadowType.F_ATTRIBUTES)
                .replace()
                .asObjectDelta("");
        delta.applyTo(expectedShadow);

        queryListener.start();
        repositoryService.modifyObject(ShadowType.class, shadowOid, delta.getModifications(), getOptions(), result);
        queryListener.dumpAndStop();

        try (EntityManager em = factory.createEntityManager()) {
            RShadow s = em.find(RShadow.class, shadowOid);
            assertExtension(s, itemGroupName);
            assertExtension(s, itemMember);
            assertExtension(s, itemManager);
        }

        assertGetObject(result);

        /*
 [1] select oid from m_object where oid = ? for update
 [1] select rshadow0_.oid as oid1_27_, rshadow0_1_.createChannel as createCh2_27_, rshadow0_1_.createTimestamp as createTi3_27_, rshadow0_1_.creatorRef_relation as creatorR4_27_, rshadow0_1_.creatorRef_targetOid as creatorR5_27_, rshadow0_1_.creatorRef_type as creatorR6_27_, rshadow0_1_.fullObject as fullObje7_27_, rshadow0_1_.lifecycleState as lifecycl8_27_, rshadow0_1_.modifierRef_relation as modifier9_27_, rshadow0_1_.modifierRef_targetOid as modifie10_27_, rshadow0_1_.modifierRef_type as modifie11_27_, rshadow0_1_.modifyChannel as modifyC12_27_, rshadow0_1_.modifyTimestamp as modifyT13_27_, rshadow0_1_.name_norm as name_no14_27_, rshadow0_1_.name_orig as name_or15_27_, rshadow0_1_.objectTypeClass as objectT16_27_, rshadow0_1_.tenantRef_relation as tenantR17_27_, rshadow0_1_.tenantRef_targetOid as tenantR18_27_, rshadow0_1_.tenantRef_type as tenantR19_27_, rshadow0_1_.version as version20_27_, rshadow0_.attemptNumber as attemptN1_41_, rshadow0_.dead as dead2_41_, rshadow0_.exist as exist3_41_, rshadow0_.failedOperationType as failedOp4_41_, rshadow0_.fullSynchronizationTimestamp as fullSync5_41_, rshadow0_.intent as intent6_41_, rshadow0_.kind as kind7_41_, rshadow0_.name_norm as name_nor8_41_, rshadow0_.name_orig as name_ori9_41_, rshadow0_.objectClass as objectC10_41_, rshadow0_.pendingOperationCount as pending11_41_, rshadow0_.primaryIdentifierValue as primary12_41_, rshadow0_.resourceRef_relation as resourc13_41_, rshadow0_.resourceRef_targetOid as resourc14_41_, rshadow0_.resourceRef_type as resourc15_41_, rshadow0_.status as status16_41_, rshadow0_.synchronizationSituation as synchro17_41_, rshadow0_.synchronizationTimestamp as synchro18_41_ from m_shadow rshadow0_ inner join m_object rshadow0_1_ on rshadow0_.oid=rshadow0_1_.oid where rshadow0_.oid=?
 [1] select booleans0_.owner_oid as owner_oi2_28_0_, booleans0_.item_id as item_id1_28_0_, booleans0_.ownerType as ownerTyp3_28_0_, booleans0_.booleanValue as booleanV4_28_0_, booleans0_.item_id as item_id1_28_1_, booleans0_.owner_oid as owner_oi2_28_1_, booleans0_.ownerType as ownerTyp3_28_1_, booleans0_.booleanValue as booleanV4_28_1_ from m_object_ext_boolean booleans0_ where booleans0_.owner_oid=?
 [1] select dates0_.owner_oid as owner_oi2_29_0_, dates0_.item_id as item_id1_29_0_, dates0_.ownerType as ownerTyp3_29_0_, dates0_.dateValue as dateValu4_29_0_, dates0_.item_id as item_id1_29_1_, dates0_.owner_oid as owner_oi2_29_1_, dates0_.ownerType as ownerTyp3_29_1_, dates0_.dateValue as dateValu4_29_1_ from m_object_ext_date dates0_ where dates0_.owner_oid=?
 [1] select longs0_.owner_oid as owner_oi2_30_0_, longs0_.item_id as item_id1_30_0_, longs0_.ownerType as ownerTyp3_30_0_, longs0_.longValue as longValu4_30_0_, longs0_.item_id as item_id1_30_1_, longs0_.owner_oid as owner_oi2_30_1_, longs0_.ownerType as ownerTyp3_30_1_, longs0_.longValue as longValu4_30_1_ from m_object_ext_long longs0_ where longs0_.owner_oid=?
 [1] select polys0_.owner_oid as owner_oi2_31_0_, polys0_.item_id as item_id1_31_0_, polys0_.ownerType as ownerTyp3_31_0_, polys0_.orig as orig4_31_0_, polys0_.item_id as item_id1_31_1_, polys0_.owner_oid as owner_oi2_31_1_, polys0_.ownerType as ownerTyp3_31_1_, polys0_.orig as orig4_31_1_, polys0_.norm as norm5_31_1_ from m_object_ext_poly polys0_ where polys0_.owner_oid=?
 [1] select references0_.owner_oid as owner_oi2_32_0_, references0_.item_id as item_id1_32_0_, references0_.ownerType as ownerTyp3_32_0_, references0_.targetoid as targetoi4_32_0_, references0_.item_id as item_id1_32_1_, references0_.owner_oid as owner_oi2_32_1_, references0_.ownerType as ownerTyp3_32_1_, references0_.targetoid as targetoi4_32_1_, references0_.relation as relation5_32_1_, references0_.targetType as targetTy6_32_1_ from m_object_ext_reference references0_ where references0_.owner_oid=?
 [1] select strings0_.owner_oid as owner_oi2_33_0_, strings0_.item_id as item_id1_33_0_, strings0_.ownerType as ownerTyp3_33_0_, strings0_.stringValue as stringVa4_33_0_, strings0_.item_id as item_id1_33_1_, strings0_.owner_oid as owner_oi2_33_1_, strings0_.ownerType as ownerTyp3_33_1_, strings0_.stringValue as stringVa4_33_1_ from m_object_ext_string strings0_ where strings0_.owner_oid=?
 [1] update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
 [5] delete from m_object_ext_string where item_id=? and owner_oid=? and ownerType=? and stringValue=?
         */
        assertCounts(10, 14);
    }

    private void assertGetShadow(OperationResult result) throws SchemaException, ObjectNotFoundException {
        assertGetShadowNoInclude(shadowOid, expectedShadow, result);
        assertGetShadowInclude(shadowOid, null, expectedShadow, result);
        // This is not yet supported:
        //assertGetShadowInclude(shadowOid, singleton(ATTR_MEMBER), expectedShadow, result);
    }

    private void assertGetShadowNoInclude(String oid, PrismObject<ShadowType> expected, OperationResult result) throws SchemaException,
            ObjectNotFoundException {
        checkShadow(oid, expected, true, false, null, result);
        checkShadow(oid, expected, false, false, null, result);
    }

    // toInclude == null means "ALL"
    private void assertGetShadowInclude(String oid, Collection<ItemName> toInclude, PrismObject<ShadowType> expected, OperationResult result) throws SchemaException,
            ObjectNotFoundException {
        checkShadow(oid, expected, true, true, toInclude, result);
        checkShadow(oid, expected, false, true, toInclude, result);
    }

    private void checkShadow(String oid, PrismObject<ShadowType> expected, boolean raw, boolean include, Collection<ItemName> toInclude, OperationResult result) throws ObjectNotFoundException, SchemaException {
        PrismObject<ShadowType> expectedAdapted;
        if (include && toInclude == null) {
            expectedAdapted = expected;
        } else {
            expectedAdapted = expected.clone();
            PrismContainer<?> attributes = expectedAdapted.findContainer(ShadowType.F_ATTRIBUTES);
            if (attributes != null) {
                if (!include || !toInclude.contains(ATTR_MEMBER)) {
                    attributes.getValue().removeProperty(ATTR_MEMBER);
                }
                if (attributes.isEmpty()) {
                    expectedAdapted.getValue().removeContainer(ShadowType.F_ATTRIBUTES);
                }
            }
        }

        Collection<SelectorOptions<GetOperationOptions>> options;
        if (include) {
            if (toInclude == null) {
                options = schemaService.getOperationOptionsBuilder().retrieve().raw(raw).build();
            } else {
                ItemPath[] paths = toInclude.stream()
                        .map(name -> ItemPath.create(ShadowType.F_ATTRIBUTES, name)).toArray(ItemPath[]::new);
                options = schemaService.getOperationOptionsBuilder().items((Object[]) paths).retrieve().raw(raw).build();
            }
        } else {
            options = schemaService.getOperationOptionsBuilder().raw(raw).build();
        }
        PrismObject<ShadowType> real = repositoryService.getObject(ShadowType.class, oid, options, result);
        ObjectDelta<ShadowType> delta = expectedAdapted.diff(real);
        if (verbose) {
            System.out.println("Expected object = \n" + expectedAdapted.debugDump());
            System.out.println("Real object in repo = \n" + real.debugDump());
            System.out.println("Difference = \n" + delta.debugDump());
        }
        if (!delta.isEmpty()) {
            fail("Objects are not equal with include=" + include + ", toInclude=" + toInclude + ":\n*** Expected:\n" +
                    expectedAdapted.debugDump() + "\n*** Got:\n" + real.debugDump() + "\n*** Delta:\n" + delta.debugDump());
        }
    }
}

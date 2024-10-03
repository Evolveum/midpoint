/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sql;

import static org.testng.AssertJUnit.assertEquals;

import static com.evolveum.midpoint.prism.util.PrismTestUtil.getPrismContext;

import java.io.File;
import java.util.*;

import jakarta.persistence.EntityManager;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.Assert;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.repo.sql.data.common.RFocusPhoto;
import com.evolveum.midpoint.repo.sql.data.common.RUser;
import com.evolveum.midpoint.repo.sql.data.common.any.RExtItem;
import com.evolveum.midpoint.repo.sql.data.common.container.RAssignment;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RActivation;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.data.common.enums.RActivationStatus;
import com.evolveum.midpoint.repo.sql.testing.TestStatementInspector;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * Note: Tests related to extension and attributes were moved into ExtensionTest.
 */
@ContextConfiguration(locations = { "../../../../../ctx-test.xml" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ObjectDeltaUpdaterTest extends BaseSQLRepoTest {

    private static final File DATA_FOLDER = new File("./src/test/resources/update");

    private static final String FILE_USER = "user.xml";

    private String userOid;

    private RExtItem itemVisibleSingle;

    @AfterMethod
    public void afterMethod() {
        TestStatementInspector.clear();
    }

    @BeforeClass
    public void beforeClass() throws Exception {
        OperationResult result = new OperationResult("setup");

        itemVisibleSingle = createOrFindExtensionItemDefinition(UserType.class, EXT_VISIBLE_SINGLE);

        PrismObject<UserType> user = prismContext.parseObject(new File(DATA_FOLDER, FILE_USER));

        userOid = repositoryService.addObject(user, new RepoAddOptions(), result);
        AssertJUnit.assertNotNull(userOid);

        result.computeStatusIfUnknown();
        AssertJUnit.assertTrue(result.isSuccess());
    }

    @Test
    public void test100ReplaceNameAndOthers() throws Exception {
        OperationResult result = new OperationResult("test100ReplaceNameAndOthers");

        ObjectDelta<UserType> delta = prismContext.deltaFactory().object().createEmptyModifyDelta(UserType.class, userOid);
        delta.addModificationReplaceProperty(UserType.F_NAME, new PolyString("ášdf", "asdf"));
        delta.addModificationReplaceProperty(UserType.F_GIVEN_NAME, new PolyString("ášdf", "asdf"));
        delta.addModificationReplaceProperty(UserType.F_LOCALE, "en-US");
        delta.addModificationReplaceProperty(
                ItemPath.create(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS), ActivationStatusType.DISABLED);

        TestStatementInspector.start();
        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), result);
        TestStatementInspector.dump();

        /*
        This looks quite well: Maybe the first two could be merged together.
         - select oid from m_object where oid = ? for update
         - select ruser0_.oid as oid1_27_, ruser0_2_.createChannel as createCh2_27_, ruser0_2_.createTimestamp as createTi3_27_, ruser0_2_.creatorRef_relation as creatorR4_27_, ruser0_2_.creatorRef_targetOid as creatorR5_27_, ruser0_2_.creatorRef_type as creatorR6_27_, ruser0_2_.fullObject as fullObje7_27_, ruser0_2_.lifecycleState as lifecycl8_27_, ruser0_2_.modifierRef_relation as modifier9_27_, ruser0_2_.modifierRef_targetOid as modifie10_27_, ruser0_2_.modifierRef_type as modifie11_27_, ruser0_2_.modifyChannel as modifyC12_27_, ruser0_2_.modifyTimestamp as modifyT13_27_, ruser0_2_.name_norm as name_no14_27_, ruser0_2_.name_orig as name_or15_27_, ruser0_2_.objectTypeClass as objectT16_27_, ruser0_2_.tenantRef_relation as tenantR17_27_, ruser0_2_.tenantRef_targetOid as tenantR18_27_, ruser0_2_.tenantRef_type as tenantR19_27_, ruser0_2_.version as version20_27_, ruser0_1_.administrativeStatus as administ1_53_, ruser0_1_.archiveTimestamp as archiveT2_53_, ruser0_1_.disableReason as disableR3_53_, ruser0_1_.disableTimestamp as disableT4_53_, ruser0_1_.effectiveStatus as effectiv5_53_, ruser0_1_.enableTimestamp as enableTi6_53_, ruser0_1_.validFrom as validFro7_53_, ruser0_1_.validTo as validTo8_53_, ruser0_1_.validityChangeTimestamp as validity9_53_, ruser0_1_.validityStatus as validit10_53_, ruser0_1_.costCenter as costCen11_53_, ruser0_1_.emailAddress as emailAd12_53_, ruser0_1_.hasPhoto as hasPhot13_53_, ruser0_1_.locale as locale14_53_, ruser0_1_.locality_norm as localit15_53_, ruser0_1_.locality_orig as localit16_53_, ruser0_1_.preferredLanguage as preferr17_53_, ruser0_1_.telephoneNumber as telepho18_53_, ruser0_1_.timezone as timezon19_53_, ruser0_.additionalName_norm as addition1_73_, ruser0_.additionalName_orig as addition2_73_, ruser0_.employeeNumber as employee3_73_, ruser0_.familyName_norm as familyNa4_73_, ruser0_.familyName_orig as familyNa5_73_, ruser0_.fullName_norm as fullName6_73_, ruser0_.fullName_orig as fullName7_73_, ruser0_.givenName_norm as givenNam8_73_, ruser0_.givenName_orig as givenNam9_73_, ruser0_.honorificPrefix_norm as honorif10_73_, ruser0_.honorificPrefix_orig as honorif11_73_, ruser0_.honorificSuffix_norm as honorif12_73_, ruser0_.honorificSuffix_orig as honorif13_73_, ruser0_.name_norm as name_no14_73_, ruser0_.name_orig as name_or15_73_, ruser0_.nickName_norm as nickNam16_73_, ruser0_.nickName_orig as nickNam17_73_, ruser0_.title_norm as title_n18_73_, ruser0_.title_orig as title_o19_73_ from m_user ruser0_ inner join m_focus ruser0_1_ on ruser0_.oid=ruser0_1_.oid inner join m_object ruser0_2_ on ruser0_.oid=ruser0_2_.oid where ruser0_.oid=?
         - update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
         - update m_focus set administrativeStatus=?, archiveTimestamp=?, disableReason=?, disableTimestamp=?, effectiveStatus=?, enableTimestamp=?, validFrom=?, validTo=?, validityChangeTimestamp=?, validityStatus=?, costCenter=?, emailAddress=?, hasPhoto=?, locale=?, locality_norm=?, locality_orig=?, preferredLanguage=?, telephoneNumber=?, timezone=? where oid=?
         - update m_user set additionalName_norm=?, additionalName_orig=?, employeeNumber=?, familyName_norm=?, familyName_orig=?, fullName_norm=?, fullName_orig=?, givenName_norm=?, givenName_orig=?, honorificPrefix_norm=?, honorificPrefix_orig=?, honorificSuffix_norm=?, honorificSuffix_orig=?, name_norm=?, name_orig=?, nickName_norm=?, nickName_orig=?, title_norm=?, title_orig=? where oid=?
         */

        if (isUsingH2()) {
            AssertJUnit.assertEquals(5, TestStatementInspector.getQueryCount());
        }

        try (EntityManager em = factory.createEntityManager()) {
            RUser u = em.find(RUser.class, userOid);
            AssertJUnit.assertEquals(new RPolyString("ášdf", "asdf"), u.getName());
            AssertJUnit.assertEquals(new RPolyString("ášdf", "asdf"), u.getNameCopy());
            AssertJUnit.assertEquals(new RPolyString("ášdf", "asdf"), u.getGivenName());
            AssertJUnit.assertEquals(u.getLocale(), "en-US");
            AssertJUnit.assertEquals(RActivationStatus.DISABLED, u.getActivation().getAdministrativeStatus());
        }
    }

    @Test
    public void test110DeleteActivation() throws Exception {
        OperationResult result = new OperationResult("test110DeleteActivation");

        ObjectDelta<UserType> delta = prismContext.deltaFactory().object().createEmptyModifyDelta(UserType.class, userOid);
        ActivationType activation = new ActivationType().administrativeStatus(ActivationStatusType.DISABLED);
        delta.addModificationDeleteContainer(UserType.F_ACTIVATION, activation.asPrismContainerValue());

        TestStatementInspector.start();
        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), result);
        TestStatementInspector.dump();

        /* (What is being changed: version in M_OBJECT and activation-related columns in M_FOCUS.)

         - select oid from m_object where oid = ? for update
         - select ruser0_.oid as oid1_27_, ruser0_2_.createChannel as createCh2_27_, ruser0_2_.createTimestamp as createTi3_27_, ruser0_2_.creatorRef_relation as creatorR4_27_, ruser0_2_.creatorRef_targetOid as creatorR5_27_, ruser0_2_.creatorRef_type as creatorR6_27_, ruser0_2_.fullObject as fullObje7_27_, ruser0_2_.lifecycleState as lifecycl8_27_, ruser0_2_.modifierRef_relation as modifier9_27_, ruser0_2_.modifierRef_targetOid as modifie10_27_, ruser0_2_.modifierRef_type as modifie11_27_, ruser0_2_.modifyChannel as modifyC12_27_, ruser0_2_.modifyTimestamp as modifyT13_27_, ruser0_2_.name_norm as name_no14_27_, ruser0_2_.name_orig as name_or15_27_, ruser0_2_.objectTypeClass as objectT16_27_, ruser0_2_.tenantRef_relation as tenantR17_27_, ruser0_2_.tenantRef_targetOid as tenantR18_27_, ruser0_2_.tenantRef_type as tenantR19_27_, ruser0_2_.version as version20_27_, ruser0_1_.administrativeStatus as administ1_53_, ruser0_1_.archiveTimestamp as archiveT2_53_, ruser0_1_.disableReason as disableR3_53_, ruser0_1_.disableTimestamp as disableT4_53_, ruser0_1_.effectiveStatus as effectiv5_53_, ruser0_1_.enableTimestamp as enableTi6_53_, ruser0_1_.validFrom as validFro7_53_, ruser0_1_.validTo as validTo8_53_, ruser0_1_.validityChangeTimestamp as validity9_53_, ruser0_1_.validityStatus as validit10_53_, ruser0_1_.costCenter as costCen11_53_, ruser0_1_.emailAddress as emailAd12_53_, ruser0_1_.hasPhoto as hasPhot13_53_, ruser0_1_.locale as locale14_53_, ruser0_1_.locality_norm as localit15_53_, ruser0_1_.locality_orig as localit16_53_, ruser0_1_.preferredLanguage as preferr17_53_, ruser0_1_.telephoneNumber as telepho18_53_, ruser0_1_.timezone as timezon19_53_, ruser0_.additionalName_norm as addition1_73_, ruser0_.additionalName_orig as addition2_73_, ruser0_.employeeNumber as employee3_73_, ruser0_.familyName_norm as familyNa4_73_, ruser0_.familyName_orig as familyNa5_73_, ruser0_.fullName_norm as fullName6_73_, ruser0_.fullName_orig as fullName7_73_, ruser0_.givenName_norm as givenNam8_73_, ruser0_.givenName_orig as givenNam9_73_, ruser0_.honorificPrefix_norm as honorif10_73_, ruser0_.honorificPrefix_orig as honorif11_73_, ruser0_.honorificSuffix_norm as honorif12_73_, ruser0_.honorificSuffix_orig as honorif13_73_, ruser0_.name_norm as name_no14_73_, ruser0_.name_orig as name_or15_73_, ruser0_.nickName_norm as nickNam16_73_, ruser0_.nickName_orig as nickNam17_73_, ruser0_.title_norm as title_n18_73_, ruser0_.title_orig as title_o19_73_ from m_user ruser0_ inner join m_focus ruser0_1_ on ruser0_.oid=ruser0_1_.oid inner join m_object ruser0_2_ on ruser0_.oid=ruser0_2_.oid where ruser0_.oid=?
         - update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
         - update m_focus set administrativeStatus=?, archiveTimestamp=?, disableReason=?, disableTimestamp=?, effectiveStatus=?, enableTimestamp=?, validFrom=?, validTo=?, validityChangeTimestamp=?, validityStatus=?, costCenter=?, emailAddress=?, hasPhoto=?, locale=?, locality_norm=?, locality_orig=?, preferredLanguage=?, telephoneNumber=?, timezone=? where oid=?
         */

        if (isUsingH2()) {
            AssertJUnit.assertEquals(4, TestStatementInspector.getQueryCount());
        }

        try (EntityManager em = factory.createEntityManager()) {
            RUser u = em.find(RUser.class, userOid);
            AssertJUnit.assertNull(u.getActivation());
        }
    }

    @Test
    public void test120AddActivation() throws Exception {
        OperationResult result = new OperationResult("test120AddActivation");

        List<ItemDelta<?, ?>> modifications = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ACTIVATION)
                .add(new ActivationType(prismContext).administrativeStatus(ActivationStatusType.ARCHIVED))
                .asItemDeltas();

        TestStatementInspector.start();
        repositoryService.modifyObject(UserType.class, userOid, modifications, result);
        TestStatementInspector.dump();

        /*
         - select oid from m_object where oid = ? for update
         - select ruser0_.oid as oid1_27_, ruser0_2_.createChannel as createCh2_27_, ruser0_2_.createTimestamp as createTi3_27_, ruser0_2_.creatorRef_relation as creatorR4_27_, ruser0_2_.creatorRef_targetOid as creatorR5_27_, ruser0_2_.creatorRef_type as creatorR6_27_, ruser0_2_.fullObject as fullObje7_27_, ruser0_2_.lifecycleState as lifecycl8_27_, ruser0_2_.modifierRef_relation as modifier9_27_, ruser0_2_.modifierRef_targetOid as modifie10_27_, ruser0_2_.modifierRef_type as modifie11_27_, ruser0_2_.modifyChannel as modifyC12_27_, ruser0_2_.modifyTimestamp as modifyT13_27_, ruser0_2_.name_norm as name_no14_27_, ruser0_2_.name_orig as name_or15_27_, ruser0_2_.objectTypeClass as objectT16_27_, ruser0_2_.tenantRef_relation as tenantR17_27_, ruser0_2_.tenantRef_targetOid as tenantR18_27_, ruser0_2_.tenantRef_type as tenantR19_27_, ruser0_2_.version as version20_27_, ruser0_1_.administrativeStatus as administ1_53_, ruser0_1_.archiveTimestamp as archiveT2_53_, ruser0_1_.disableReason as disableR3_53_, ruser0_1_.disableTimestamp as disableT4_53_, ruser0_1_.effectiveStatus as effectiv5_53_, ruser0_1_.enableTimestamp as enableTi6_53_, ruser0_1_.validFrom as validFro7_53_, ruser0_1_.validTo as validTo8_53_, ruser0_1_.validityChangeTimestamp as validity9_53_, ruser0_1_.validityStatus as validit10_53_, ruser0_1_.costCenter as costCen11_53_, ruser0_1_.emailAddress as emailAd12_53_, ruser0_1_.hasPhoto as hasPhot13_53_, ruser0_1_.locale as locale14_53_, ruser0_1_.locality_norm as localit15_53_, ruser0_1_.locality_orig as localit16_53_, ruser0_1_.preferredLanguage as preferr17_53_, ruser0_1_.telephoneNumber as telepho18_53_, ruser0_1_.timezone as timezon19_53_, ruser0_.additionalName_norm as addition1_73_, ruser0_.additionalName_orig as addition2_73_, ruser0_.employeeNumber as employee3_73_, ruser0_.familyName_norm as familyNa4_73_, ruser0_.familyName_orig as familyNa5_73_, ruser0_.fullName_norm as fullName6_73_, ruser0_.fullName_orig as fullName7_73_, ruser0_.givenName_norm as givenNam8_73_, ruser0_.givenName_orig as givenNam9_73_, ruser0_.honorificPrefix_norm as honorif10_73_, ruser0_.honorificPrefix_orig as honorif11_73_, ruser0_.honorificSuffix_norm as honorif12_73_, ruser0_.honorificSuffix_orig as honorif13_73_, ruser0_.name_norm as name_no14_73_, ruser0_.name_orig as name_or15_73_, ruser0_.nickName_norm as nickNam16_73_, ruser0_.nickName_orig as nickNam17_73_, ruser0_.title_norm as title_n18_73_, ruser0_.title_orig as title_o19_73_ from m_user ruser0_ inner join m_focus ruser0_1_ on ruser0_.oid=ruser0_1_.oid inner join m_object ruser0_2_ on ruser0_.oid=ruser0_2_.oid where ruser0_.oid=?
         - update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
         - update m_focus set administrativeStatus=?, archiveTimestamp=?, disableReason=?, disableTimestamp=?, effectiveStatus=?, enableTimestamp=?, validFrom=?, validTo=?, validityChangeTimestamp=?, validityStatus=?, costCenter=?, emailAddress=?, hasPhoto=?, locale=?, locality_norm=?, locality_orig=?, preferredLanguage=?, telephoneNumber=?, timezone=? where oid=?
         */
        if (isUsingH2()) {
            AssertJUnit.assertEquals(4, TestStatementInspector.getQueryCount());
        }

        try (EntityManager em = factory.createEntityManager()) {
            RUser u = em.find(RUser.class, userOid);
            AssertJUnit.assertEquals(RActivationStatus.ARCHIVED, u.getActivation().getAdministrativeStatus());
        }
    }

    @Test
    public void test130AddDeleteAssignment() throws Exception {
        OperationResult result = new OperationResult("test130AddDeleteAssignment");

        List<ItemDelta<?, ?>> modifications = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT)
                .delete(new AssignmentType(prismContext).id(1L))
                .add(new AssignmentType(prismContext)
                        .description("asdf")
                        .targetRef("444", OrgType.COMPLEX_TYPE)
                        .beginMetadata()
                        .createChannel("zzz")
                        .modifyApproverRef("555", UserType.COMPLEX_TYPE)
                        .<AssignmentType>end())
                .asItemDeltas();

        TestStatementInspector.start();
        repositoryService.modifyObject(UserType.class, userOid, modifications, result);
        TestStatementInspector.dump();

        /*
         - select oid from m_object where oid = ? for update
         - select ruser0_.oid as oid1_27_, ruser0_2_.createChannel as createCh2_27_, ruser0_2_.createTimestamp as createTi3_27_, ruser0_2_.creatorRef_relation as creatorR4_27_, ruser0_2_.creatorRef_targetOid as creatorR5_27_, ruser0_2_.creatorRef_type as creatorR6_27_, ruser0_2_.fullObject as fullObje7_27_, ruser0_2_.lifecycleState as lifecycl8_27_, ruser0_2_.modifierRef_relation as modifier9_27_, ruser0_2_.modifierRef_targetOid as modifie10_27_, ruser0_2_.modifierRef_type as modifie11_27_, ruser0_2_.modifyChannel as modifyC12_27_, ruser0_2_.modifyTimestamp as modifyT13_27_, ruser0_2_.name_norm as name_no14_27_, ruser0_2_.name_orig as name_or15_27_, ruser0_2_.objectTypeClass as objectT16_27_, ruser0_2_.tenantRef_relation as tenantR17_27_, ruser0_2_.tenantRef_targetOid as tenantR18_27_, ruser0_2_.tenantRef_type as tenantR19_27_, ruser0_2_.version as version20_27_, ruser0_1_.administrativeStatus as administ1_53_, ruser0_1_.archiveTimestamp as archiveT2_53_, ruser0_1_.disableReason as disableR3_53_, ruser0_1_.disableTimestamp as disableT4_53_, ruser0_1_.effectiveStatus as effectiv5_53_, ruser0_1_.enableTimestamp as enableTi6_53_, ruser0_1_.validFrom as validFro7_53_, ruser0_1_.validTo as validTo8_53_, ruser0_1_.validityChangeTimestamp as validity9_53_, ruser0_1_.validityStatus as validit10_53_, ruser0_1_.costCenter as costCen11_53_, ruser0_1_.emailAddress as emailAd12_53_, ruser0_1_.hasPhoto as hasPhot13_53_, ruser0_1_.locale as locale14_53_, ruser0_1_.locality_norm as localit15_53_, ruser0_1_.locality_orig as localit16_53_, ruser0_1_.preferredLanguage as preferr17_53_, ruser0_1_.telephoneNumber as telepho18_53_, ruser0_1_.timezone as timezon19_53_, ruser0_.additionalName_norm as addition1_73_, ruser0_.additionalName_orig as addition2_73_, ruser0_.employeeNumber as employee3_73_, ruser0_.familyName_norm as familyNa4_73_, ruser0_.familyName_orig as familyNa5_73_, ruser0_.fullName_norm as fullName6_73_, ruser0_.fullName_orig as fullName7_73_, ruser0_.givenName_norm as givenNam8_73_, ruser0_.givenName_orig as givenNam9_73_, ruser0_.honorificPrefix_norm as honorif10_73_, ruser0_.honorificPrefix_orig as honorif11_73_, ruser0_.honorificSuffix_norm as honorif12_73_, ruser0_.honorificSuffix_orig as honorif13_73_, ruser0_.name_norm as name_no14_73_, ruser0_.name_orig as name_or15_73_, ruser0_.nickName_norm as nickNam16_73_, ruser0_.nickName_orig as nickNam17_73_, ruser0_.title_norm as title_n18_73_, ruser0_.title_orig as title_o19_73_ from m_user ruser0_ inner join m_focus ruser0_1_ on ruser0_.oid=ruser0_1_.oid inner join m_object ruser0_2_ on ruser0_.oid=ruser0_2_.oid where ruser0_.oid=?
         - select assignment0_.owner_oid as owner_oi2_5_0_, assignment0_.id as id1_5_0_, assignment0_.id as id1_5_1_, assignment0_.owner_oid as owner_oi2_5_1_, assignment0_.administrativeStatus as administ3_5_1_, assignment0_.archiveTimestamp as archiveT4_5_1_, assignment0_.disableReason as disableR5_5_1_, assignment0_.disableTimestamp as disableT6_5_1_, assignment0_.effectiveStatus as effectiv7_5_1_, assignment0_.enableTimestamp as enableTi8_5_1_, assignment0_.validFrom as validFro9_5_1_, assignment0_.validTo as validTo10_5_1_, assignment0_.validityChangeTimestamp as validit11_5_1_, assignment0_.validityStatus as validit12_5_1_, assignment0_.assignmentOwner as assignm13_5_1_, assignment0_.createChannel as createC14_5_1_, assignment0_.createTimestamp as createT15_5_1_, assignment0_.creatorRef_relation as creator16_5_1_, assignment0_.creatorRef_targetOid as creator17_5_1_, assignment0_.creatorRef_type as creator18_5_1_, assignment0_.extId as extId38_5_1_, assignment0_.extOid as extOid39_5_1_, assignment0_.lifecycleState as lifecyc19_5_1_, assignment0_.modifierRef_relation as modifie20_5_1_, assignment0_.modifierRef_targetOid as modifie21_5_1_, assignment0_.modifierRef_type as modifie22_5_1_, assignment0_.modifyChannel as modifyC23_5_1_, assignment0_.modifyTimestamp as modifyT24_5_1_, assignment0_.orderValue as orderVa25_5_1_, assignment0_.orgRef_relation as orgRef_26_5_1_, assignment0_.orgRef_targetOid as orgRef_27_5_1_, assignment0_.orgRef_type as orgRef_28_5_1_, assignment0_.resourceRef_relation as resourc29_5_1_, assignment0_.resourceRef_targetOid as resourc30_5_1_, assignment0_.resourceRef_type as resourc31_5_1_, assignment0_.targetRef_relation as targetR32_5_1_, assignment0_.targetRef_targetOid as targetR33_5_1_, assignment0_.targetRef_type as targetR34_5_1_, assignment0_.tenantRef_relation as tenantR35_5_1_, assignment0_.tenantRef_targetOid as tenantR36_5_1_, assignment0_.tenantRef_type as tenantR37_5_1_, rassignmen1_.owner_id as owner_id1_12_2_, rassignmen1_.owner_owner_oid as owner_ow2_12_2_, rassignmen1_.booleansCount as booleans3_12_2_, rassignmen1_.datesCount as datesCou4_12_2_, rassignmen1_.longsCount as longsCou5_12_2_, rassignmen1_.polysCount as polysCou6_12_2_, rassignmen1_.referencesCount as referenc7_12_2_, rassignmen1_.stringsCount as stringsC8_12_2_ from m_assignment assignment0_ left outer join m_assignment_extension rassignmen1_ on assignment0_.extId=rassignmen1_.owner_id and assignment0_.extOid=rassignmen1_.owner_owner_oid where assignment0_.owner_oid=?
         - select createappr0_.owner_id as owner_id1_14_0_, createappr0_.owner_owner_oid as owner_ow2_14_0_, createappr0_.reference_type as referenc3_14_0_, createappr0_.relation as relation4_14_0_, createappr0_.targetOid as targetOi5_14_0_, createappr0_.owner_id as owner_id1_14_1_, createappr0_.owner_owner_oid as owner_ow2_14_1_, createappr0_.reference_type as referenc3_14_1_, createappr0_.relation as relation4_14_1_, createappr0_.targetOid as targetOi5_14_1_, createappr0_.targetType as targetTy6_14_1_ from m_assignment_reference createappr0_ where ( createappr0_.reference_type= 0) and createappr0_.owner_id=? and createappr0_.owner_owner_oid=?
         - select modifyappr0_.owner_id as owner_id1_14_0_, modifyappr0_.owner_owner_oid as owner_ow2_14_0_, modifyappr0_.reference_type as referenc3_14_0_, modifyappr0_.relation as relation4_14_0_, modifyappr0_.targetOid as targetOi5_14_0_, modifyappr0_.owner_id as owner_id1_14_1_, modifyappr0_.owner_owner_oid as owner_ow2_14_1_, modifyappr0_.reference_type as referenc3_14_1_, modifyappr0_.relation as relation4_14_1_, modifyappr0_.targetOid as targetOi5_14_1_, modifyappr0_.targetType as targetTy6_14_1_ from m_assignment_reference modifyappr0_ where ( modifyappr0_.reference_type= 1) and modifyappr0_.owner_id=? and modifyappr0_.owner_owner_oid=?
         - insert into m_assignment (administrativeStatus, archiveTimestamp, disableReason, disableTimestamp, effectiveStatus, enableTimestamp, validFrom, validTo, validityChangeTimestamp, validityStatus, assignmentOwner, createChannel, createTimestamp, creatorRef_relation, creatorRef_targetOid, creatorRef_type, extId, extOid, lifecycleState, modifierRef_relation, modifierRef_targetOid, modifierRef_type, modifyChannel, modifyTimestamp, orderValue, orgRef_relation, orgRef_targetOid, orgRef_type, resourceRef_relation, resourceRef_targetOid, resourceRef_type, targetRef_relation, targetRef_targetOid, targetRef_type, tenantRef_relation, tenantRef_targetOid, tenantRef_type, id, owner_oid) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
         - insert into m_assignment_reference (targetType, owner_id, owner_owner_oid, reference_type, relation, targetOid) values (?, ?, ?, ?, ?, ?)
         - update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
         - delete from m_assignment_policy_situation where assignment_id=? and assignment_oid=?
         - delete from m_assignment where id=? and owner_oid=?
         */

        // todo this should be only 8 queries, these two aren't expected ... that's because hibernate handles collections too eagerly
        // select createappr0_.owner_id as owner_id1_14_0_, createappr0_.owner_owner_oid as owner_ow2_14_0_, createappr0_.reference_type as referenc3_14_0_, createappr0_.relation as relation4_14_0_, createappr0_.targetOid as targetOi5_14_0_, createappr0_.owner_id as owner_id1_14_1_, createappr0_.owner_owner_oid as owner_ow2_14_1_, createappr0_.reference_type as referenc3_14_1_, createappr0_.relation as relation4_14_1_, createappr0_.targetOid as targetOi5_14_1_, createappr0_.targetType as targetTy6_14_1_ from m_assignment_reference createappr0_ where ( createappr0_.reference_type= 0) and createappr0_.owner_id=? and createappr0_.owner_owner_oid=?
        // select modifyappr0_.owner_id as owner_id1_14_0_, modifyappr0_.owner_owner_oid as owner_ow2_14_0_, modifyappr0_.reference_type as referenc3_14_0_, modifyappr0_.relation as relation4_14_0_, modifyappr0_.targetOid as targetOi5_14_0_, modifyappr0_.owner_id as owner_id1_14_1_, modifyappr0_.owner_owner_oid as owner_ow2_14_1_, modifyappr0_.reference_type as referenc3_14_1_, modifyappr0_.relation as relation4_14_1_, modifyappr0_.targetOid as targetOi5_14_1_, modifyappr0_.targetType as targetTy6_14_1_ from m_assignment_reference modifyappr0_ where ( modifyappr0_.reference_type= 1) and modifyappr0_.owner_id=? and modifyappr0_.owner_owner_oid=?
        if (isUsingH2()) {
            AssertJUnit.assertEquals(10, TestStatementInspector.getQueryCount());
        }

        try (EntityManager em = factory.createEntityManager()) {
            RUser u = em.find(RUser.class, userOid);

            Set<RAssignment> assignments = u.getAssignments();
            AssertJUnit.assertEquals(1, assignments.size());

            RAssignment a = assignments.iterator().next();
            AssertJUnit.assertEquals("zzz", a.getCreateChannel());

            ObjectReferenceType targetRef = a.getTargetRef().toJAXB(prismContext);
            AssertJUnit.assertEquals(createRef(OrgType.COMPLEX_TYPE, "444", SchemaConstants.ORG_DEFAULT), targetRef);

            //noinspection unchecked
            assertReferences((Collection) a.getModifyApproverRef(), createRepoRef(UserType.COMPLEX_TYPE, "555"));
        }
    }

    @Test
    public void test140AddActivationToAssignmentOldApi() throws Exception {
        OperationResult result = new OperationResult("test140AddActivationToAssignmentOldApi");

        ObjectDelta<UserType> delta = prismContext.deltaFactory().object().createEmptyModifyDelta(UserType.class, userOid);

        ActivationType activation = new ActivationType();
        activation.setAdministrativeStatus(ActivationStatusType.ENABLED);
        delta.addModificationAddContainer(
                ItemPath.create(UserType.F_ASSIGNMENT, 2, AssignmentType.F_ACTIVATION), activation.asPrismContainerValue());

        TestStatementInspector.start();
        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), result);
        TestStatementInspector.dump();

        /*
         - select oid from m_object where oid = ? for update
         - select ruser0_.oid as oid1_27_, ruser0_2_.createChannel as createCh2_27_, ruser0_2_.createTimestamp as createTi3_27_, ruser0_2_.creatorRef_relation as creatorR4_27_, ruser0_2_.creatorRef_targetOid as creatorR5_27_, ruser0_2_.creatorRef_type as creatorR6_27_, ruser0_2_.fullObject as fullObje7_27_, ruser0_2_.lifecycleState as lifecycl8_27_, ruser0_2_.modifierRef_relation as modifier9_27_, ruser0_2_.modifierRef_targetOid as modifie10_27_, ruser0_2_.modifierRef_type as modifie11_27_, ruser0_2_.modifyChannel as modifyC12_27_, ruser0_2_.modifyTimestamp as modifyT13_27_, ruser0_2_.name_norm as name_no14_27_, ruser0_2_.name_orig as name_or15_27_, ruser0_2_.objectTypeClass as objectT16_27_, ruser0_2_.tenantRef_relation as tenantR17_27_, ruser0_2_.tenantRef_targetOid as tenantR18_27_, ruser0_2_.tenantRef_type as tenantR19_27_, ruser0_2_.version as version20_27_, ruser0_1_.administrativeStatus as administ1_53_, ruser0_1_.archiveTimestamp as archiveT2_53_, ruser0_1_.disableReason as disableR3_53_, ruser0_1_.disableTimestamp as disableT4_53_, ruser0_1_.effectiveStatus as effectiv5_53_, ruser0_1_.enableTimestamp as enableTi6_53_, ruser0_1_.validFrom as validFro7_53_, ruser0_1_.validTo as validTo8_53_, ruser0_1_.validityChangeTimestamp as validity9_53_, ruser0_1_.validityStatus as validit10_53_, ruser0_1_.costCenter as costCen11_53_, ruser0_1_.emailAddress as emailAd12_53_, ruser0_1_.hasPhoto as hasPhot13_53_, ruser0_1_.locale as locale14_53_, ruser0_1_.locality_norm as localit15_53_, ruser0_1_.locality_orig as localit16_53_, ruser0_1_.preferredLanguage as preferr17_53_, ruser0_1_.telephoneNumber as telepho18_53_, ruser0_1_.timezone as timezon19_53_, ruser0_.additionalName_norm as addition1_73_, ruser0_.additionalName_orig as addition2_73_, ruser0_.employeeNumber as employee3_73_, ruser0_.familyName_norm as familyNa4_73_, ruser0_.familyName_orig as familyNa5_73_, ruser0_.fullName_norm as fullName6_73_, ruser0_.fullName_orig as fullName7_73_, ruser0_.givenName_norm as givenNam8_73_, ruser0_.givenName_orig as givenNam9_73_, ruser0_.honorificPrefix_norm as honorif10_73_, ruser0_.honorificPrefix_orig as honorif11_73_, ruser0_.honorificSuffix_norm as honorif12_73_, ruser0_.honorificSuffix_orig as honorif13_73_, ruser0_.name_norm as name_no14_73_, ruser0_.name_orig as name_or15_73_, ruser0_.nickName_norm as nickNam16_73_, ruser0_.nickName_orig as nickNam17_73_, ruser0_.title_norm as title_n18_73_, ruser0_.title_orig as title_o19_73_ from m_user ruser0_ inner join m_focus ruser0_1_ on ruser0_.oid=ruser0_1_.oid inner join m_object ruser0_2_ on ruser0_.oid=ruser0_2_.oid where ruser0_.oid=?
         - select assignment0_.owner_oid as owner_oi2_5_0_, assignment0_.id as id1_5_0_, assignment0_.id as id1_5_1_, assignment0_.owner_oid as owner_oi2_5_1_, assignment0_.administrativeStatus as administ3_5_1_, assignment0_.archiveTimestamp as archiveT4_5_1_, assignment0_.disableReason as disableR5_5_1_, assignment0_.disableTimestamp as disableT6_5_1_, assignment0_.effectiveStatus as effectiv7_5_1_, assignment0_.enableTimestamp as enableTi8_5_1_, assignment0_.validFrom as validFro9_5_1_, assignment0_.validTo as validTo10_5_1_, assignment0_.validityChangeTimestamp as validit11_5_1_, assignment0_.validityStatus as validit12_5_1_, assignment0_.assignmentOwner as assignm13_5_1_, assignment0_.createChannel as createC14_5_1_, assignment0_.createTimestamp as createT15_5_1_, assignment0_.creatorRef_relation as creator16_5_1_, assignment0_.creatorRef_targetOid as creator17_5_1_, assignment0_.creatorRef_type as creator18_5_1_, assignment0_.extId as extId38_5_1_, assignment0_.extOid as extOid39_5_1_, assignment0_.lifecycleState as lifecyc19_5_1_, assignment0_.modifierRef_relation as modifie20_5_1_, assignment0_.modifierRef_targetOid as modifie21_5_1_, assignment0_.modifierRef_type as modifie22_5_1_, assignment0_.modifyChannel as modifyC23_5_1_, assignment0_.modifyTimestamp as modifyT24_5_1_, assignment0_.orderValue as orderVa25_5_1_, assignment0_.orgRef_relation as orgRef_26_5_1_, assignment0_.orgRef_targetOid as orgRef_27_5_1_, assignment0_.orgRef_type as orgRef_28_5_1_, assignment0_.resourceRef_relation as resourc29_5_1_, assignment0_.resourceRef_targetOid as resourc30_5_1_, assignment0_.resourceRef_type as resourc31_5_1_, assignment0_.targetRef_relation as targetR32_5_1_, assignment0_.targetRef_targetOid as targetR33_5_1_, assignment0_.targetRef_type as targetR34_5_1_, assignment0_.tenantRef_relation as tenantR35_5_1_, assignment0_.tenantRef_targetOid as tenantR36_5_1_, assignment0_.tenantRef_type as tenantR37_5_1_, rassignmen1_.owner_id as owner_id1_12_2_, rassignmen1_.owner_owner_oid as owner_ow2_12_2_, rassignmen1_.booleansCount as booleans3_12_2_, rassignmen1_.datesCount as datesCou4_12_2_, rassignmen1_.longsCount as longsCou5_12_2_, rassignmen1_.polysCount as polysCou6_12_2_, rassignmen1_.referencesCount as referenc7_12_2_, rassignmen1_.stringsCount as stringsC8_12_2_ from m_assignment assignment0_ left outer join m_assignment_extension rassignmen1_ on assignment0_.extId=rassignmen1_.owner_id and assignment0_.extOid=rassignmen1_.owner_owner_oid where assignment0_.owner_oid=?
         - update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
         - update m_assignment set administrativeStatus=?, archiveTimestamp=?, disableReason=?, disableTimestamp=?, effectiveStatus=?, enableTimestamp=?, validFrom=?, validTo=?, validityChangeTimestamp=?, validityStatus=?, assignmentOwner=?, createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, extId=?, extOid=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, orderValue=?, orgRef_relation=?, orgRef_targetOid=?, orgRef_type=?, resourceRef_relation=?, resourceRef_targetOid=?, resourceRef_type=?, targetRef_relation=?, targetRef_targetOid=?, targetRef_type=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=? where id=? and owner_oid=?
         */

        if (isUsingH2()) {
            AssertJUnit.assertEquals(5, TestStatementInspector.getQueryCount());
        }

        try (EntityManager em = factory.createEntityManager()) {
            RUser u = em.find(RUser.class, userOid);

            Set<RAssignment> assignments = u.getAssignments();
            AssertJUnit.assertEquals(1, assignments.size());

            RAssignment a = assignments.iterator().next();
            RActivation act = a.getActivation();
            AssertJUnit.assertNotNull(act);
            AssertJUnit.assertEquals(RActivationStatus.ENABLED, act.getAdministrativeStatus());
        }
    }

    @Test
    public void test147DeleteAssignmentActivation() throws Exception {
        OperationResult result = new OperationResult("test145AddActivationToAssignment");

        List<ItemDelta<?, ?>> modifications = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT, 2, AssignmentType.F_ACTIVATION)
                .delete(new ActivationType(prismContext).administrativeStatus(ActivationStatusType.ENABLED))
                .asItemDeltas();

        TestStatementInspector.start();
        repositoryService.modifyObject(UserType.class, userOid, modifications, result);
        TestStatementInspector.dump();

        /*
         - select oid from m_object where oid = ? for update
         - select ruser0_.oid as oid1_27_, ruser0_2_.createChannel as createCh2_27_, ruser0_2_.createTimestamp as createTi3_27_, ruser0_2_.creatorRef_relation as creatorR4_27_, ruser0_2_.creatorRef_targetOid as creatorR5_27_, ruser0_2_.creatorRef_type as creatorR6_27_, ruser0_2_.fullObject as fullObje7_27_, ruser0_2_.lifecycleState as lifecycl8_27_, ruser0_2_.modifierRef_relation as modifier9_27_, ruser0_2_.modifierRef_targetOid as modifie10_27_, ruser0_2_.modifierRef_type as modifie11_27_, ruser0_2_.modifyChannel as modifyC12_27_, ruser0_2_.modifyTimestamp as modifyT13_27_, ruser0_2_.name_norm as name_no14_27_, ruser0_2_.name_orig as name_or15_27_, ruser0_2_.objectTypeClass as objectT16_27_, ruser0_2_.tenantRef_relation as tenantR17_27_, ruser0_2_.tenantRef_targetOid as tenantR18_27_, ruser0_2_.tenantRef_type as tenantR19_27_, ruser0_2_.version as version20_27_, ruser0_1_.administrativeStatus as administ1_53_, ruser0_1_.archiveTimestamp as archiveT2_53_, ruser0_1_.disableReason as disableR3_53_, ruser0_1_.disableTimestamp as disableT4_53_, ruser0_1_.effectiveStatus as effectiv5_53_, ruser0_1_.enableTimestamp as enableTi6_53_, ruser0_1_.validFrom as validFro7_53_, ruser0_1_.validTo as validTo8_53_, ruser0_1_.validityChangeTimestamp as validity9_53_, ruser0_1_.validityStatus as validit10_53_, ruser0_1_.costCenter as costCen11_53_, ruser0_1_.emailAddress as emailAd12_53_, ruser0_1_.hasPhoto as hasPhot13_53_, ruser0_1_.locale as locale14_53_, ruser0_1_.locality_norm as localit15_53_, ruser0_1_.locality_orig as localit16_53_, ruser0_1_.preferredLanguage as preferr17_53_, ruser0_1_.telephoneNumber as telepho18_53_, ruser0_1_.timezone as timezon19_53_, ruser0_.additionalName_norm as addition1_73_, ruser0_.additionalName_orig as addition2_73_, ruser0_.employeeNumber as employee3_73_, ruser0_.familyName_norm as familyNa4_73_, ruser0_.familyName_orig as familyNa5_73_, ruser0_.fullName_norm as fullName6_73_, ruser0_.fullName_orig as fullName7_73_, ruser0_.givenName_norm as givenNam8_73_, ruser0_.givenName_orig as givenNam9_73_, ruser0_.honorificPrefix_norm as honorif10_73_, ruser0_.honorificPrefix_orig as honorif11_73_, ruser0_.honorificSuffix_norm as honorif12_73_, ruser0_.honorificSuffix_orig as honorif13_73_, ruser0_.name_norm as name_no14_73_, ruser0_.name_orig as name_or15_73_, ruser0_.nickName_norm as nickNam16_73_, ruser0_.nickName_orig as nickNam17_73_, ruser0_.title_norm as title_n18_73_, ruser0_.title_orig as title_o19_73_ from m_user ruser0_ inner join m_focus ruser0_1_ on ruser0_.oid=ruser0_1_.oid inner join m_object ruser0_2_ on ruser0_.oid=ruser0_2_.oid where ruser0_.oid=?
         - select assignment0_.owner_oid as owner_oi2_5_0_, assignment0_.id as id1_5_0_, assignment0_.id as id1_5_1_, assignment0_.owner_oid as owner_oi2_5_1_, assignment0_.administrativeStatus as administ3_5_1_, assignment0_.archiveTimestamp as archiveT4_5_1_, assignment0_.disableReason as disableR5_5_1_, assignment0_.disableTimestamp as disableT6_5_1_, assignment0_.effectiveStatus as effectiv7_5_1_, assignment0_.enableTimestamp as enableTi8_5_1_, assignment0_.validFrom as validFro9_5_1_, assignment0_.validTo as validTo10_5_1_, assignment0_.validityChangeTimestamp as validit11_5_1_, assignment0_.validityStatus as validit12_5_1_, assignment0_.assignmentOwner as assignm13_5_1_, assignment0_.createChannel as createC14_5_1_, assignment0_.createTimestamp as createT15_5_1_, assignment0_.creatorRef_relation as creator16_5_1_, assignment0_.creatorRef_targetOid as creator17_5_1_, assignment0_.creatorRef_type as creator18_5_1_, assignment0_.extId as extId38_5_1_, assignment0_.extOid as extOid39_5_1_, assignment0_.lifecycleState as lifecyc19_5_1_, assignment0_.modifierRef_relation as modifie20_5_1_, assignment0_.modifierRef_targetOid as modifie21_5_1_, assignment0_.modifierRef_type as modifie22_5_1_, assignment0_.modifyChannel as modifyC23_5_1_, assignment0_.modifyTimestamp as modifyT24_5_1_, assignment0_.orderValue as orderVa25_5_1_, assignment0_.orgRef_relation as orgRef_26_5_1_, assignment0_.orgRef_targetOid as orgRef_27_5_1_, assignment0_.orgRef_type as orgRef_28_5_1_, assignment0_.resourceRef_relation as resourc29_5_1_, assignment0_.resourceRef_targetOid as resourc30_5_1_, assignment0_.resourceRef_type as resourc31_5_1_, assignment0_.targetRef_relation as targetR32_5_1_, assignment0_.targetRef_targetOid as targetR33_5_1_, assignment0_.targetRef_type as targetR34_5_1_, assignment0_.tenantRef_relation as tenantR35_5_1_, assignment0_.tenantRef_targetOid as tenantR36_5_1_, assignment0_.tenantRef_type as tenantR37_5_1_, rassignmen1_.owner_id as owner_id1_12_2_, rassignmen1_.owner_owner_oid as owner_ow2_12_2_, rassignmen1_.booleansCount as booleans3_12_2_, rassignmen1_.datesCount as datesCou4_12_2_, rassignmen1_.longsCount as longsCou5_12_2_, rassignmen1_.polysCount as polysCou6_12_2_, rassignmen1_.referencesCount as referenc7_12_2_, rassignmen1_.stringsCount as stringsC8_12_2_ from m_assignment assignment0_ left outer join m_assignment_extension rassignmen1_ on assignment0_.extId=rassignmen1_.owner_id and assignment0_.extOid=rassignmen1_.owner_owner_oid where assignment0_.owner_oid=?
         - update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
         - update m_assignment set administrativeStatus=?, archiveTimestamp=?, disableReason=?, disableTimestamp=?, effectiveStatus=?, enableTimestamp=?, validFrom=?, validTo=?, validityChangeTimestamp=?, validityStatus=?, assignmentOwner=?, createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, extId=?, extOid=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, orderValue=?, orgRef_relation=?, orgRef_targetOid=?, orgRef_type=?, resourceRef_relation=?, resourceRef_targetOid=?, resourceRef_type=?, targetRef_relation=?, targetRef_targetOid=?, targetRef_type=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=? where id=? and owner_oid=?
         */

        if (isUsingH2()) {
            AssertJUnit.assertEquals(5, TestStatementInspector.getQueryCount());
        }

        try (EntityManager em = factory.createEntityManager()) {
            RUser u = em.find(RUser.class, userOid);
            Set<RAssignment> assignments = u.getAssignments();
            AssertJUnit.assertEquals(1, assignments.size());
            AssertJUnit.assertNull(assignments.iterator().next().getActivation());
        }
    }

    @Test
    public void test148AddAssignmentActivation() throws Exception {
        OperationResult result = new OperationResult("test148AddAssignmentActivation");

        List<ItemDelta<?, ?>> modifications = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT, 2, AssignmentType.F_ACTIVATION)
                .add(new ActivationType(prismContext).administrativeStatus(ActivationStatusType.ARCHIVED))
                .asItemDeltas();

        TestStatementInspector.start();
        repositoryService.modifyObject(UserType.class, userOid, modifications, result);
        TestStatementInspector.dump();

        /*
         - select oid from m_object where oid = ? for update
         - select ruser0_.oid as oid1_27_, ruser0_2_.createChannel as createCh2_27_, ruser0_2_.createTimestamp as createTi3_27_, ruser0_2_.creatorRef_relation as creatorR4_27_, ruser0_2_.creatorRef_targetOid as creatorR5_27_, ruser0_2_.creatorRef_type as creatorR6_27_, ruser0_2_.fullObject as fullObje7_27_, ruser0_2_.lifecycleState as lifecycl8_27_, ruser0_2_.modifierRef_relation as modifier9_27_, ruser0_2_.modifierRef_targetOid as modifie10_27_, ruser0_2_.modifierRef_type as modifie11_27_, ruser0_2_.modifyChannel as modifyC12_27_, ruser0_2_.modifyTimestamp as modifyT13_27_, ruser0_2_.name_norm as name_no14_27_, ruser0_2_.name_orig as name_or15_27_, ruser0_2_.objectTypeClass as objectT16_27_, ruser0_2_.tenantRef_relation as tenantR17_27_, ruser0_2_.tenantRef_targetOid as tenantR18_27_, ruser0_2_.tenantRef_type as tenantR19_27_, ruser0_2_.version as version20_27_, ruser0_1_.administrativeStatus as administ1_53_, ruser0_1_.archiveTimestamp as archiveT2_53_, ruser0_1_.disableReason as disableR3_53_, ruser0_1_.disableTimestamp as disableT4_53_, ruser0_1_.effectiveStatus as effectiv5_53_, ruser0_1_.enableTimestamp as enableTi6_53_, ruser0_1_.validFrom as validFro7_53_, ruser0_1_.validTo as validTo8_53_, ruser0_1_.validityChangeTimestamp as validity9_53_, ruser0_1_.validityStatus as validit10_53_, ruser0_1_.costCenter as costCen11_53_, ruser0_1_.emailAddress as emailAd12_53_, ruser0_1_.hasPhoto as hasPhot13_53_, ruser0_1_.locale as locale14_53_, ruser0_1_.locality_norm as localit15_53_, ruser0_1_.locality_orig as localit16_53_, ruser0_1_.preferredLanguage as preferr17_53_, ruser0_1_.telephoneNumber as telepho18_53_, ruser0_1_.timezone as timezon19_53_, ruser0_.additionalName_norm as addition1_73_, ruser0_.additionalName_orig as addition2_73_, ruser0_.employeeNumber as employee3_73_, ruser0_.familyName_norm as familyNa4_73_, ruser0_.familyName_orig as familyNa5_73_, ruser0_.fullName_norm as fullName6_73_, ruser0_.fullName_orig as fullName7_73_, ruser0_.givenName_norm as givenNam8_73_, ruser0_.givenName_orig as givenNam9_73_, ruser0_.honorificPrefix_norm as honorif10_73_, ruser0_.honorificPrefix_orig as honorif11_73_, ruser0_.honorificSuffix_norm as honorif12_73_, ruser0_.honorificSuffix_orig as honorif13_73_, ruser0_.name_norm as name_no14_73_, ruser0_.name_orig as name_or15_73_, ruser0_.nickName_norm as nickNam16_73_, ruser0_.nickName_orig as nickNam17_73_, ruser0_.title_norm as title_n18_73_, ruser0_.title_orig as title_o19_73_ from m_user ruser0_ inner join m_focus ruser0_1_ on ruser0_.oid=ruser0_1_.oid inner join m_object ruser0_2_ on ruser0_.oid=ruser0_2_.oid where ruser0_.oid=?
         - select assignment0_.owner_oid as owner_oi2_5_0_, assignment0_.id as id1_5_0_, assignment0_.id as id1_5_1_, assignment0_.owner_oid as owner_oi2_5_1_, assignment0_.administrativeStatus as administ3_5_1_, assignment0_.archiveTimestamp as archiveT4_5_1_, assignment0_.disableReason as disableR5_5_1_, assignment0_.disableTimestamp as disableT6_5_1_, assignment0_.effectiveStatus as effectiv7_5_1_, assignment0_.enableTimestamp as enableTi8_5_1_, assignment0_.validFrom as validFro9_5_1_, assignment0_.validTo as validTo10_5_1_, assignment0_.validityChangeTimestamp as validit11_5_1_, assignment0_.validityStatus as validit12_5_1_, assignment0_.assignmentOwner as assignm13_5_1_, assignment0_.createChannel as createC14_5_1_, assignment0_.createTimestamp as createT15_5_1_, assignment0_.creatorRef_relation as creator16_5_1_, assignment0_.creatorRef_targetOid as creator17_5_1_, assignment0_.creatorRef_type as creator18_5_1_, assignment0_.extId as extId38_5_1_, assignment0_.extOid as extOid39_5_1_, assignment0_.lifecycleState as lifecyc19_5_1_, assignment0_.modifierRef_relation as modifie20_5_1_, assignment0_.modifierRef_targetOid as modifie21_5_1_, assignment0_.modifierRef_type as modifie22_5_1_, assignment0_.modifyChannel as modifyC23_5_1_, assignment0_.modifyTimestamp as modifyT24_5_1_, assignment0_.orderValue as orderVa25_5_1_, assignment0_.orgRef_relation as orgRef_26_5_1_, assignment0_.orgRef_targetOid as orgRef_27_5_1_, assignment0_.orgRef_type as orgRef_28_5_1_, assignment0_.resourceRef_relation as resourc29_5_1_, assignment0_.resourceRef_targetOid as resourc30_5_1_, assignment0_.resourceRef_type as resourc31_5_1_, assignment0_.targetRef_relation as targetR32_5_1_, assignment0_.targetRef_targetOid as targetR33_5_1_, assignment0_.targetRef_type as targetR34_5_1_, assignment0_.tenantRef_relation as tenantR35_5_1_, assignment0_.tenantRef_targetOid as tenantR36_5_1_, assignment0_.tenantRef_type as tenantR37_5_1_, rassignmen1_.owner_id as owner_id1_12_2_, rassignmen1_.owner_owner_oid as owner_ow2_12_2_, rassignmen1_.booleansCount as booleans3_12_2_, rassignmen1_.datesCount as datesCou4_12_2_, rassignmen1_.longsCount as longsCou5_12_2_, rassignmen1_.polysCount as polysCou6_12_2_, rassignmen1_.referencesCount as referenc7_12_2_, rassignmen1_.stringsCount as stringsC8_12_2_ from m_assignment assignment0_ left outer join m_assignment_extension rassignmen1_ on assignment0_.extId=rassignmen1_.owner_id and assignment0_.extOid=rassignmen1_.owner_owner_oid where assignment0_.owner_oid=?
         - update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
         - update m_assignment set administrativeStatus=?, archiveTimestamp=?, disableReason=?, disableTimestamp=?, effectiveStatus=?, enableTimestamp=?, validFrom=?, validTo=?, validityChangeTimestamp=?, validityStatus=?, assignmentOwner=?, createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, extId=?, extOid=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, orderValue=?, orgRef_relation=?, orgRef_targetOid=?, orgRef_type=?, resourceRef_relation=?, resourceRef_targetOid=?, resourceRef_type=?, targetRef_relation=?, targetRef_targetOid=?, targetRef_type=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=? where id=? and owner_oid=?
         */

        if (isUsingH2()) {
            AssertJUnit.assertEquals(5, TestStatementInspector.getQueryCount());
        }

        try (EntityManager em = factory.createEntityManager()) {
            RUser u = em.find(RUser.class, userOid);
            Set<RAssignment> assignments = u.getAssignments();
            AssertJUnit.assertEquals(1, assignments.size());
            RActivation act = assignments.iterator().next().getActivation();
            AssertJUnit.assertNotNull(act);
            AssertJUnit.assertEquals(RActivationStatus.ARCHIVED, act.getAdministrativeStatus());
        }
    }

    @Test
    public void test149ReplaceAssignmentActivationWithNull() throws Exception {
        OperationResult result = new OperationResult("test149ReplaceAssignmentActivationWithNull");

        List<ItemDelta<?, ?>> modifications = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT, 2, AssignmentType.F_ACTIVATION)
                .replace()
                .asItemDeltas();

        TestStatementInspector.start();
        repositoryService.modifyObject(UserType.class, userOid, modifications, result);
        TestStatementInspector.dump();

        /*
         - select oid from m_object where oid = ? for update
         - select ruser0_.oid as oid1_27_, ruser0_2_.createChannel as createCh2_27_, ruser0_2_.createTimestamp as createTi3_27_, ruser0_2_.creatorRef_relation as creatorR4_27_, ruser0_2_.creatorRef_targetOid as creatorR5_27_, ruser0_2_.creatorRef_type as creatorR6_27_, ruser0_2_.fullObject as fullObje7_27_, ruser0_2_.lifecycleState as lifecycl8_27_, ruser0_2_.modifierRef_relation as modifier9_27_, ruser0_2_.modifierRef_targetOid as modifie10_27_, ruser0_2_.modifierRef_type as modifie11_27_, ruser0_2_.modifyChannel as modifyC12_27_, ruser0_2_.modifyTimestamp as modifyT13_27_, ruser0_2_.name_norm as name_no14_27_, ruser0_2_.name_orig as name_or15_27_, ruser0_2_.objectTypeClass as objectT16_27_, ruser0_2_.tenantRef_relation as tenantR17_27_, ruser0_2_.tenantRef_targetOid as tenantR18_27_, ruser0_2_.tenantRef_type as tenantR19_27_, ruser0_2_.version as version20_27_, ruser0_1_.administrativeStatus as administ1_53_, ruser0_1_.archiveTimestamp as archiveT2_53_, ruser0_1_.disableReason as disableR3_53_, ruser0_1_.disableTimestamp as disableT4_53_, ruser0_1_.effectiveStatus as effectiv5_53_, ruser0_1_.enableTimestamp as enableTi6_53_, ruser0_1_.validFrom as validFro7_53_, ruser0_1_.validTo as validTo8_53_, ruser0_1_.validityChangeTimestamp as validity9_53_, ruser0_1_.validityStatus as validit10_53_, ruser0_1_.costCenter as costCen11_53_, ruser0_1_.emailAddress as emailAd12_53_, ruser0_1_.hasPhoto as hasPhot13_53_, ruser0_1_.locale as locale14_53_, ruser0_1_.locality_norm as localit15_53_, ruser0_1_.locality_orig as localit16_53_, ruser0_1_.preferredLanguage as preferr17_53_, ruser0_1_.telephoneNumber as telepho18_53_, ruser0_1_.timezone as timezon19_53_, ruser0_.additionalName_norm as addition1_73_, ruser0_.additionalName_orig as addition2_73_, ruser0_.employeeNumber as employee3_73_, ruser0_.familyName_norm as familyNa4_73_, ruser0_.familyName_orig as familyNa5_73_, ruser0_.fullName_norm as fullName6_73_, ruser0_.fullName_orig as fullName7_73_, ruser0_.givenName_norm as givenNam8_73_, ruser0_.givenName_orig as givenNam9_73_, ruser0_.honorificPrefix_norm as honorif10_73_, ruser0_.honorificPrefix_orig as honorif11_73_, ruser0_.honorificSuffix_norm as honorif12_73_, ruser0_.honorificSuffix_orig as honorif13_73_, ruser0_.name_norm as name_no14_73_, ruser0_.name_orig as name_or15_73_, ruser0_.nickName_norm as nickNam16_73_, ruser0_.nickName_orig as nickNam17_73_, ruser0_.title_norm as title_n18_73_, ruser0_.title_orig as title_o19_73_ from m_user ruser0_ inner join m_focus ruser0_1_ on ruser0_.oid=ruser0_1_.oid inner join m_object ruser0_2_ on ruser0_.oid=ruser0_2_.oid where ruser0_.oid=?
         - select assignment0_.owner_oid as owner_oi2_5_0_, assignment0_.id as id1_5_0_, assignment0_.id as id1_5_1_, assignment0_.owner_oid as owner_oi2_5_1_, assignment0_.administrativeStatus as administ3_5_1_, assignment0_.archiveTimestamp as archiveT4_5_1_, assignment0_.disableReason as disableR5_5_1_, assignment0_.disableTimestamp as disableT6_5_1_, assignment0_.effectiveStatus as effectiv7_5_1_, assignment0_.enableTimestamp as enableTi8_5_1_, assignment0_.validFrom as validFro9_5_1_, assignment0_.validTo as validTo10_5_1_, assignment0_.validityChangeTimestamp as validit11_5_1_, assignment0_.validityStatus as validit12_5_1_, assignment0_.assignmentOwner as assignm13_5_1_, assignment0_.createChannel as createC14_5_1_, assignment0_.createTimestamp as createT15_5_1_, assignment0_.creatorRef_relation as creator16_5_1_, assignment0_.creatorRef_targetOid as creator17_5_1_, assignment0_.creatorRef_type as creator18_5_1_, assignment0_.extId as extId38_5_1_, assignment0_.extOid as extOid39_5_1_, assignment0_.lifecycleState as lifecyc19_5_1_, assignment0_.modifierRef_relation as modifie20_5_1_, assignment0_.modifierRef_targetOid as modifie21_5_1_, assignment0_.modifierRef_type as modifie22_5_1_, assignment0_.modifyChannel as modifyC23_5_1_, assignment0_.modifyTimestamp as modifyT24_5_1_, assignment0_.orderValue as orderVa25_5_1_, assignment0_.orgRef_relation as orgRef_26_5_1_, assignment0_.orgRef_targetOid as orgRef_27_5_1_, assignment0_.orgRef_type as orgRef_28_5_1_, assignment0_.resourceRef_relation as resourc29_5_1_, assignment0_.resourceRef_targetOid as resourc30_5_1_, assignment0_.resourceRef_type as resourc31_5_1_, assignment0_.targetRef_relation as targetR32_5_1_, assignment0_.targetRef_targetOid as targetR33_5_1_, assignment0_.targetRef_type as targetR34_5_1_, assignment0_.tenantRef_relation as tenantR35_5_1_, assignment0_.tenantRef_targetOid as tenantR36_5_1_, assignment0_.tenantRef_type as tenantR37_5_1_, rassignmen1_.owner_id as owner_id1_12_2_, rassignmen1_.owner_owner_oid as owner_ow2_12_2_, rassignmen1_.booleansCount as booleans3_12_2_, rassignmen1_.datesCount as datesCou4_12_2_, rassignmen1_.longsCount as longsCou5_12_2_, rassignmen1_.polysCount as polysCou6_12_2_, rassignmen1_.referencesCount as referenc7_12_2_, rassignmen1_.stringsCount as stringsC8_12_2_ from m_assignment assignment0_ left outer join m_assignment_extension rassignmen1_ on assignment0_.extId=rassignmen1_.owner_id and assignment0_.extOid=rassignmen1_.owner_owner_oid where assignment0_.owner_oid=?
         - update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
         - update m_assignment set administrativeStatus=?, archiveTimestamp=?, disableReason=?, disableTimestamp=?, effectiveStatus=?, enableTimestamp=?, validFrom=?, validTo=?, validityChangeTimestamp=?, validityStatus=?, assignmentOwner=?, createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, extId=?, extOid=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, orderValue=?, orgRef_relation=?, orgRef_targetOid=?, orgRef_type=?, resourceRef_relation=?, resourceRef_targetOid=?, resourceRef_type=?, targetRef_relation=?, targetRef_targetOid=?, targetRef_type=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=? where id=? and owner_oid=?
         */

        if (isUsingH2()) {
            AssertJUnit.assertEquals(5, TestStatementInspector.getQueryCount());
        }

        try (EntityManager em = factory.createEntityManager()) {
            RUser u = em.find(RUser.class, userOid);
            Set<RAssignment> assignments = u.getAssignments();
            AssertJUnit.assertEquals(1, assignments.size());
            AssertJUnit.assertNull(assignments.iterator().next().getActivation());
        }
    }

    @Test
    public void test150AddDeleteLinkRef() throws Exception {
        OperationResult result = new OperationResult("test150AddDeleteLinkRef");

        List<ItemDelta<?, ?>> modifications = prismContext.deltaFor(UserType.class)
                .item(UserType.F_LINK_REF)
                .delete(createRef(ShadowType.COMPLEX_TYPE, "456"))
                .add(createRef(ShadowType.COMPLEX_TYPE, "789"))
                .asItemDeltas();

        TestStatementInspector.start();
        repositoryService.modifyObject(UserType.class, userOid, modifications, result);
        TestStatementInspector.dump();

        /*
         - select oid from m_object where oid = ? for update
         - select ruser0_.oid as oid1_27_, ruser0_2_.createChannel as createCh2_27_, ruser0_2_.createTimestamp as createTi3_27_, ruser0_2_.creatorRef_relation as creatorR4_27_, ruser0_2_.creatorRef_targetOid as creatorR5_27_, ruser0_2_.creatorRef_type as creatorR6_27_, ruser0_2_.fullObject as fullObje7_27_, ruser0_2_.lifecycleState as lifecycl8_27_, ruser0_2_.modifierRef_relation as modifier9_27_, ruser0_2_.modifierRef_targetOid as modifie10_27_, ruser0_2_.modifierRef_type as modifie11_27_, ruser0_2_.modifyChannel as modifyC12_27_, ruser0_2_.modifyTimestamp as modifyT13_27_, ruser0_2_.name_norm as name_no14_27_, ruser0_2_.name_orig as name_or15_27_, ruser0_2_.objectTypeClass as objectT16_27_, ruser0_2_.tenantRef_relation as tenantR17_27_, ruser0_2_.tenantRef_targetOid as tenantR18_27_, ruser0_2_.tenantRef_type as tenantR19_27_, ruser0_2_.version as version20_27_, ruser0_1_.administrativeStatus as administ1_53_, ruser0_1_.archiveTimestamp as archiveT2_53_, ruser0_1_.disableReason as disableR3_53_, ruser0_1_.disableTimestamp as disableT4_53_, ruser0_1_.effectiveStatus as effectiv5_53_, ruser0_1_.enableTimestamp as enableTi6_53_, ruser0_1_.validFrom as validFro7_53_, ruser0_1_.validTo as validTo8_53_, ruser0_1_.validityChangeTimestamp as validity9_53_, ruser0_1_.validityStatus as validit10_53_, ruser0_1_.costCenter as costCen11_53_, ruser0_1_.emailAddress as emailAd12_53_, ruser0_1_.hasPhoto as hasPhot13_53_, ruser0_1_.locale as locale14_53_, ruser0_1_.locality_norm as localit15_53_, ruser0_1_.locality_orig as localit16_53_, ruser0_1_.preferredLanguage as preferr17_53_, ruser0_1_.telephoneNumber as telepho18_53_, ruser0_1_.timezone as timezon19_53_, ruser0_.additionalName_norm as addition1_73_, ruser0_.additionalName_orig as addition2_73_, ruser0_.employeeNumber as employee3_73_, ruser0_.familyName_norm as familyNa4_73_, ruser0_.familyName_orig as familyNa5_73_, ruser0_.fullName_norm as fullName6_73_, ruser0_.fullName_orig as fullName7_73_, ruser0_.givenName_norm as givenNam8_73_, ruser0_.givenName_orig as givenNam9_73_, ruser0_.honorificPrefix_norm as honorif10_73_, ruser0_.honorificPrefix_orig as honorif11_73_, ruser0_.honorificSuffix_norm as honorif12_73_, ruser0_.honorificSuffix_orig as honorif13_73_, ruser0_.name_norm as name_no14_73_, ruser0_.name_orig as name_or15_73_, ruser0_.nickName_norm as nickNam16_73_, ruser0_.nickName_orig as nickNam17_73_, ruser0_.title_norm as title_n18_73_, ruser0_.title_orig as title_o19_73_ from m_user ruser0_ inner join m_focus ruser0_1_ on ruser0_.oid=ruser0_1_.oid inner join m_object ruser0_2_ on ruser0_.oid=ruser0_2_.oid where ruser0_.oid=?
         - select linkref0_.owner_oid as owner_oi1_39_0_, linkref0_.reference_type as referenc2_39_0_, linkref0_.relation as relation3_39_0_, linkref0_.targetOid as targetOi4_39_0_, linkref0_.owner_oid as owner_oi1_39_1_, linkref0_.reference_type as referenc2_39_1_, linkref0_.relation as relation3_39_1_, linkref0_.targetOid as targetOi4_39_1_, linkref0_.targetType as targetTy5_39_1_ from m_reference linkref0_ where ( linkref0_.reference_type= 1) and linkref0_.owner_oid=?
         - insert into m_reference (targetType, owner_oid, reference_type, relation, targetOid) values (?, ?, ?, ?, ?)
         - update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
         - delete from m_reference where owner_oid=? and reference_type=? and relation=? and targetOid=?
         */
        if (isUsingH2()) {
            AssertJUnit.assertEquals(6, TestStatementInspector.getQueryCount());
        }

        try (EntityManager em = factory.createEntityManager()) {
            RUser u = em.find(RUser.class, userOid);

            //noinspection unchecked
            assertReferences((Collection) u.getLinkRef(),
                    createRepoRef(ShadowType.COMPLEX_TYPE, "123"),
                    createRepoRef(ShadowType.COMPLEX_TYPE, "789"));
        }
    }

    @Test
    public void test160AddDeleteParentRef() throws Exception {
        OperationResult result = new OperationResult("test160AddDeleteParentRef");

        ObjectDelta<UserType> delta = prismContext.deltaFactory().object().createEmptyModifyDelta(UserType.class, userOid);
        ObjectReferenceType parentOrgRef = createRef(OrgType.COMPLEX_TYPE, "456");
        delta.addModificationDeleteReference(UserType.F_PARENT_ORG_REF, parentOrgRef.asReferenceValue());

        parentOrgRef = createRef(OrgType.COMPLEX_TYPE, "789");
        delta.addModificationAddReference(UserType.F_PARENT_ORG_REF, parentOrgRef.asReferenceValue());

        TestStatementInspector.start();
        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), result);
        TestStatementInspector.dump();

        /*
         - select oid from m_object where oid = ? for update
         - select ruser0_.oid as oid1_27_, ruser0_2_.createChannel as createCh2_27_, ruser0_2_.createTimestamp as createTi3_27_, ruser0_2_.creatorRef_relation as creatorR4_27_, ruser0_2_.creatorRef_targetOid as creatorR5_27_, ruser0_2_.creatorRef_type as creatorR6_27_, ruser0_2_.fullObject as fullObje7_27_, ruser0_2_.lifecycleState as lifecycl8_27_, ruser0_2_.modifierRef_relation as modifier9_27_, ruser0_2_.modifierRef_targetOid as modifie10_27_, ruser0_2_.modifierRef_type as modifie11_27_, ruser0_2_.modifyChannel as modifyC12_27_, ruser0_2_.modifyTimestamp as modifyT13_27_, ruser0_2_.name_norm as name_no14_27_, ruser0_2_.name_orig as name_or15_27_, ruser0_2_.objectTypeClass as objectT16_27_, ruser0_2_.tenantRef_relation as tenantR17_27_, ruser0_2_.tenantRef_targetOid as tenantR18_27_, ruser0_2_.tenantRef_type as tenantR19_27_, ruser0_2_.version as version20_27_, ruser0_1_.administrativeStatus as administ1_53_, ruser0_1_.archiveTimestamp as archiveT2_53_, ruser0_1_.disableReason as disableR3_53_, ruser0_1_.disableTimestamp as disableT4_53_, ruser0_1_.effectiveStatus as effectiv5_53_, ruser0_1_.enableTimestamp as enableTi6_53_, ruser0_1_.validFrom as validFro7_53_, ruser0_1_.validTo as validTo8_53_, ruser0_1_.validityChangeTimestamp as validity9_53_, ruser0_1_.validityStatus as validit10_53_, ruser0_1_.costCenter as costCen11_53_, ruser0_1_.emailAddress as emailAd12_53_, ruser0_1_.hasPhoto as hasPhot13_53_, ruser0_1_.locale as locale14_53_, ruser0_1_.locality_norm as localit15_53_, ruser0_1_.locality_orig as localit16_53_, ruser0_1_.preferredLanguage as preferr17_53_, ruser0_1_.telephoneNumber as telepho18_53_, ruser0_1_.timezone as timezon19_53_, ruser0_.additionalName_norm as addition1_73_, ruser0_.additionalName_orig as addition2_73_, ruser0_.employeeNumber as employee3_73_, ruser0_.familyName_norm as familyNa4_73_, ruser0_.familyName_orig as familyNa5_73_, ruser0_.fullName_norm as fullName6_73_, ruser0_.fullName_orig as fullName7_73_, ruser0_.givenName_norm as givenNam8_73_, ruser0_.givenName_orig as givenNam9_73_, ruser0_.honorificPrefix_norm as honorif10_73_, ruser0_.honorificPrefix_orig as honorif11_73_, ruser0_.honorificSuffix_norm as honorif12_73_, ruser0_.honorificSuffix_orig as honorif13_73_, ruser0_.name_norm as name_no14_73_, ruser0_.name_orig as name_or15_73_, ruser0_.nickName_norm as nickNam16_73_, ruser0_.nickName_orig as nickNam17_73_, ruser0_.title_norm as title_n18_73_, ruser0_.title_orig as title_o19_73_ from m_user ruser0_ inner join m_focus ruser0_1_ on ruser0_.oid=ruser0_1_.oid inner join m_object ruser0_2_ on ruser0_.oid=ruser0_2_.oid where ruser0_.oid=?
         - select parentorgr0_.owner_oid as owner_oi1_39_0_, parentorgr0_.reference_type as referenc2_39_0_, parentorgr0_.relation as relation3_39_0_, parentorgr0_.targetOid as targetOi4_39_0_, parentorgr0_.owner_oid as owner_oi1_39_1_, parentorgr0_.reference_type as referenc2_39_1_, parentorgr0_.relation as relation3_39_1_, parentorgr0_.targetOid as targetOi4_39_1_, parentorgr0_.targetType as targetTy5_39_1_ from m_reference parentorgr0_ where ( parentorgr0_.reference_type= 0) and parentorgr0_.owner_oid=?
         - insert into m_reference (targetType, owner_oid, reference_type, relation, targetOid) values (?, ?, ?, ?, ?)
         - update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
         - delete from m_reference where owner_oid=? and reference_type=? and relation=? and targetOid=?
         */

        if (isUsingH2()) {
            AssertJUnit.assertEquals(6, TestStatementInspector.getQueryCount());
        }

        try (EntityManager em = factory.createEntityManager()) {
            RUser u = em.find(RUser.class, userOid);

            //noinspection unchecked
            assertReferences((Collection) u.getParentOrgRef(),
                    createRepoRef(OrgType.COMPLEX_TYPE, "123"),
                    createRepoRef(OrgType.COMPLEX_TYPE, "789"));
        }
    }

    @Test
    public void test170ModifySubtypeAndMetadataCreateChannel() throws Exception {
        OperationResult result = new OperationResult("test170ModifySubtypeAndMetadataCreateChannel");

        List<ItemDelta<?, ?>> modifications = prismContext.deltaFor(UserType.class)
                .item(UserType.F_SUBTYPE)
                .add("one", "two")
                .item(UserType.F_METADATA, MetadataType.F_CREATE_CHANNEL)
                .replace("asdf")
                .asItemDeltas();

        TestStatementInspector.start();
        repositoryService.modifyObject(UserType.class, userOid, modifications, result);
        TestStatementInspector.dump();

        /*
         - select oid from m_object where oid = ? for update
         - select ruser0_.oid as oid1_27_, ruser0_2_.createChannel as createCh2_27_, ruser0_2_.createTimestamp as createTi3_27_, ruser0_2_.creatorRef_relation as creatorR4_27_, ruser0_2_.creatorRef_targetOid as creatorR5_27_, ruser0_2_.creatorRef_type as creatorR6_27_, ruser0_2_.fullObject as fullObje7_27_, ruser0_2_.lifecycleState as lifecycl8_27_, ruser0_2_.modifierRef_relation as modifier9_27_, ruser0_2_.modifierRef_targetOid as modifie10_27_, ruser0_2_.modifierRef_type as modifie11_27_, ruser0_2_.modifyChannel as modifyC12_27_, ruser0_2_.modifyTimestamp as modifyT13_27_, ruser0_2_.name_norm as name_no14_27_, ruser0_2_.name_orig as name_or15_27_, ruser0_2_.objectTypeClass as objectT16_27_, ruser0_2_.tenantRef_relation as tenantR17_27_, ruser0_2_.tenantRef_targetOid as tenantR18_27_, ruser0_2_.tenantRef_type as tenantR19_27_, ruser0_2_.version as version20_27_, ruser0_1_.administrativeStatus as administ1_53_, ruser0_1_.archiveTimestamp as archiveT2_53_, ruser0_1_.disableReason as disableR3_53_, ruser0_1_.disableTimestamp as disableT4_53_, ruser0_1_.effectiveStatus as effectiv5_53_, ruser0_1_.enableTimestamp as enableTi6_53_, ruser0_1_.validFrom as validFro7_53_, ruser0_1_.validTo as validTo8_53_, ruser0_1_.validityChangeTimestamp as validity9_53_, ruser0_1_.validityStatus as validit10_53_, ruser0_1_.costCenter as costCen11_53_, ruser0_1_.emailAddress as emailAd12_53_, ruser0_1_.hasPhoto as hasPhot13_53_, ruser0_1_.locale as locale14_53_, ruser0_1_.locality_norm as localit15_53_, ruser0_1_.locality_orig as localit16_53_, ruser0_1_.preferredLanguage as preferr17_53_, ruser0_1_.telephoneNumber as telepho18_53_, ruser0_1_.timezone as timezon19_53_, ruser0_.additionalName_norm as addition1_73_, ruser0_.additionalName_orig as addition2_73_, ruser0_.employeeNumber as employee3_73_, ruser0_.familyName_norm as familyNa4_73_, ruser0_.familyName_orig as familyNa5_73_, ruser0_.fullName_norm as fullName6_73_, ruser0_.fullName_orig as fullName7_73_, ruser0_.givenName_norm as givenNam8_73_, ruser0_.givenName_orig as givenNam9_73_, ruser0_.honorificPrefix_norm as honorif10_73_, ruser0_.honorificPrefix_orig as honorif11_73_, ruser0_.honorificSuffix_norm as honorif12_73_, ruser0_.honorificSuffix_orig as honorif13_73_, ruser0_.name_norm as name_no14_73_, ruser0_.name_orig as name_or15_73_, ruser0_.nickName_norm as nickNam16_73_, ruser0_.nickName_orig as nickNam17_73_, ruser0_.title_norm as title_n18_73_, ruser0_.title_orig as title_o19_73_ from m_user ruser0_ inner join m_focus ruser0_1_ on ruser0_.oid=ruser0_1_.oid inner join m_object ruser0_2_ on ruser0_.oid=ruser0_2_.oid where ruser0_.oid=?
         - select ... subtype
         - update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
         - insert into m_user_subtype (user_oid, subtype) values (?, ?)
         */

        if (isUsingH2()) {
            AssertJUnit.assertEquals(5, TestStatementInspector.getQueryCount());
        }

        try (EntityManager em = factory.createEntityManager()) {
            RUser u = em.find(RUser.class, userOid);
            AssertJUnit.assertEquals("asdf", u.getCreateChannel());
            AssertJUnit.assertEquals(u.getSubtype(), new HashSet<>(Arrays.asList("one", "two")));
        }
    }

    @Test
    public void test180ReplaceSomeMetadataItems() throws Exception {
        OperationResult result = new OperationResult("test180ReplaceSomeMetadataItems");

        List<ItemDelta<?, ?>> modifications = prismContext.deltaFor(UserType.class)
                .item(UserType.F_METADATA, MetadataType.F_CREATE_APPROVER_REF)
                .replace(createRef(UserType.COMPLEX_TYPE, "111"))
                .item(UserType.F_METADATA, MetadataType.F_CREATE_CHANNEL)
                .replace("zxcv")
                .asItemDeltas();

        TestStatementInspector.start();
        repositoryService.modifyObject(UserType.class, userOid, modifications, result);
        TestStatementInspector.dump();

        /*
         - select oid from m_object where oid = ? for update
         - select ruser0_.oid as oid1_27_, ruser0_2_.createChannel as createCh2_27_, ruser0_2_.createTimestamp as createTi3_27_, ruser0_2_.creatorRef_relation as creatorR4_27_, ruser0_2_.creatorRef_targetOid as creatorR5_27_, ruser0_2_.creatorRef_type as creatorR6_27_, ruser0_2_.fullObject as fullObje7_27_, ruser0_2_.lifecycleState as lifecycl8_27_, ruser0_2_.modifierRef_relation as modifier9_27_, ruser0_2_.modifierRef_targetOid as modifie10_27_, ruser0_2_.modifierRef_type as modifie11_27_, ruser0_2_.modifyChannel as modifyC12_27_, ruser0_2_.modifyTimestamp as modifyT13_27_, ruser0_2_.name_norm as name_no14_27_, ruser0_2_.name_orig as name_or15_27_, ruser0_2_.objectTypeClass as objectT16_27_, ruser0_2_.tenantRef_relation as tenantR17_27_, ruser0_2_.tenantRef_targetOid as tenantR18_27_, ruser0_2_.tenantRef_type as tenantR19_27_, ruser0_2_.version as version20_27_, ruser0_1_.administrativeStatus as administ1_53_, ruser0_1_.archiveTimestamp as archiveT2_53_, ruser0_1_.disableReason as disableR3_53_, ruser0_1_.disableTimestamp as disableT4_53_, ruser0_1_.effectiveStatus as effectiv5_53_, ruser0_1_.enableTimestamp as enableTi6_53_, ruser0_1_.validFrom as validFro7_53_, ruser0_1_.validTo as validTo8_53_, ruser0_1_.validityChangeTimestamp as validity9_53_, ruser0_1_.validityStatus as validit10_53_, ruser0_1_.costCenter as costCen11_53_, ruser0_1_.emailAddress as emailAd12_53_, ruser0_1_.hasPhoto as hasPhot13_53_, ruser0_1_.locale as locale14_53_, ruser0_1_.locality_norm as localit15_53_, ruser0_1_.locality_orig as localit16_53_, ruser0_1_.preferredLanguage as preferr17_53_, ruser0_1_.telephoneNumber as telepho18_53_, ruser0_1_.timezone as timezon19_53_, ruser0_.additionalName_norm as addition1_73_, ruser0_.additionalName_orig as addition2_73_, ruser0_.employeeNumber as employee3_73_, ruser0_.familyName_norm as familyNa4_73_, ruser0_.familyName_orig as familyNa5_73_, ruser0_.fullName_norm as fullName6_73_, ruser0_.fullName_orig as fullName7_73_, ruser0_.givenName_norm as givenNam8_73_, ruser0_.givenName_orig as givenNam9_73_, ruser0_.honorificPrefix_norm as honorif10_73_, ruser0_.honorificPrefix_orig as honorif11_73_, ruser0_.honorificSuffix_norm as honorif12_73_, ruser0_.honorificSuffix_orig as honorif13_73_, ruser0_.name_norm as name_no14_73_, ruser0_.name_orig as name_or15_73_, ruser0_.nickName_norm as nickNam16_73_, ruser0_.nickName_orig as nickNam17_73_, ruser0_.title_norm as title_n18_73_, ruser0_.title_orig as title_o19_73_ from m_user ruser0_ inner join m_focus ruser0_1_ on ruser0_.oid=ruser0_1_.oid inner join m_object ruser0_2_ on ruser0_.oid=ruser0_2_.oid where ruser0_.oid=?
         - select createappr0_.owner_oid as owner_oi1_39_0_, createappr0_.reference_type as referenc2_39_0_, createappr0_.relation as relation3_39_0_, createappr0_.targetOid as targetOi4_39_0_, createappr0_.owner_oid as owner_oi1_39_1_, createappr0_.reference_type as referenc2_39_1_, createappr0_.relation as relation3_39_1_, createappr0_.targetOid as targetOi4_39_1_, createappr0_.targetType as targetTy5_39_1_ from m_reference createappr0_ where ( createappr0_.reference_type= 5) and createappr0_.owner_oid=?
         - insert into m_reference (targetType, owner_oid, reference_type, relation, targetOid) values (?, ?, ?, ?, ?)
         - update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
         */

        if (isUsingH2()) {
            AssertJUnit.assertEquals(5, TestStatementInspector.getQueryCount());
        }

        try (EntityManager em = factory.createEntityManager()) {
            RUser u = em.find(RUser.class, userOid);
            AssertJUnit.assertEquals("zxcv", u.getCreateChannel());
            AssertJUnit.assertEquals(1, u.getCreateApproverRef().size());

            //noinspection unchecked
            assertReferences((Collection) u.getCreateApproverRef(),
                    createRepoRef(UserType.COMPLEX_TYPE, "111"));
        }
    }

    @Test
    public void test185AddModifyApproverRef() throws Exception {
        OperationResult result = new OperationResult("test185AddModifyApproverRef");

        List<ItemDelta<?, ?>> modifications = prismContext.deltaFor(UserType.class)
                .item(UserType.F_METADATA, MetadataType.F_MODIFY_APPROVER_REF)
                .add(createRef(UserType.COMPLEX_TYPE, "654"))
                .asItemDeltas();

        TestStatementInspector.start();
        repositoryService.modifyObject(UserType.class, userOid, modifications, result);
        TestStatementInspector.dump();

        /*
         - select oid from m_object where oid = ? for update
         - select ruser0_.oid as oid1_27_, ruser0_2_.createChannel as createCh2_27_, ruser0_2_.createTimestamp as createTi3_27_, ruser0_2_.creatorRef_relation as creatorR4_27_, ruser0_2_.creatorRef_targetOid as creatorR5_27_, ruser0_2_.creatorRef_type as creatorR6_27_, ruser0_2_.fullObject as fullObje7_27_, ruser0_2_.lifecycleState as lifecycl8_27_, ruser0_2_.modifierRef_relation as modifier9_27_, ruser0_2_.modifierRef_targetOid as modifie10_27_, ruser0_2_.modifierRef_type as modifie11_27_, ruser0_2_.modifyChannel as modifyC12_27_, ruser0_2_.modifyTimestamp as modifyT13_27_, ruser0_2_.name_norm as name_no14_27_, ruser0_2_.name_orig as name_or15_27_, ruser0_2_.objectTypeClass as objectT16_27_, ruser0_2_.tenantRef_relation as tenantR17_27_, ruser0_2_.tenantRef_targetOid as tenantR18_27_, ruser0_2_.tenantRef_type as tenantR19_27_, ruser0_2_.version as version20_27_, ruser0_1_.administrativeStatus as administ1_53_, ruser0_1_.archiveTimestamp as archiveT2_53_, ruser0_1_.disableReason as disableR3_53_, ruser0_1_.disableTimestamp as disableT4_53_, ruser0_1_.effectiveStatus as effectiv5_53_, ruser0_1_.enableTimestamp as enableTi6_53_, ruser0_1_.validFrom as validFro7_53_, ruser0_1_.validTo as validTo8_53_, ruser0_1_.validityChangeTimestamp as validity9_53_, ruser0_1_.validityStatus as validit10_53_, ruser0_1_.costCenter as costCen11_53_, ruser0_1_.emailAddress as emailAd12_53_, ruser0_1_.hasPhoto as hasPhot13_53_, ruser0_1_.locale as locale14_53_, ruser0_1_.locality_norm as localit15_53_, ruser0_1_.locality_orig as localit16_53_, ruser0_1_.preferredLanguage as preferr17_53_, ruser0_1_.telephoneNumber as telepho18_53_, ruser0_1_.timezone as timezon19_53_, ruser0_.additionalName_norm as addition1_73_, ruser0_.additionalName_orig as addition2_73_, ruser0_.employeeNumber as employee3_73_, ruser0_.familyName_norm as familyNa4_73_, ruser0_.familyName_orig as familyNa5_73_, ruser0_.fullName_norm as fullName6_73_, ruser0_.fullName_orig as fullName7_73_, ruser0_.givenName_norm as givenNam8_73_, ruser0_.givenName_orig as givenNam9_73_, ruser0_.honorificPrefix_norm as honorif10_73_, ruser0_.honorificPrefix_orig as honorif11_73_, ruser0_.honorificSuffix_norm as honorif12_73_, ruser0_.honorificSuffix_orig as honorif13_73_, ruser0_.name_norm as name_no14_73_, ruser0_.name_orig as name_or15_73_, ruser0_.nickName_norm as nickNam16_73_, ruser0_.nickName_orig as nickNam17_73_, ruser0_.title_norm as title_n18_73_, ruser0_.title_orig as title_o19_73_ from m_user ruser0_ inner join m_focus ruser0_1_ on ruser0_.oid=ruser0_1_.oid inner join m_object ruser0_2_ on ruser0_.oid=ruser0_2_.oid where ruser0_.oid=?
         - select modifyappr0_.owner_oid as owner_oi1_39_0_, modifyappr0_.reference_type as referenc2_39_0_, modifyappr0_.relation as relation3_39_0_, modifyappr0_.targetOid as targetOi4_39_0_, modifyappr0_.owner_oid as owner_oi1_39_1_, modifyappr0_.reference_type as referenc2_39_1_, modifyappr0_.relation as relation3_39_1_, modifyappr0_.targetOid as targetOi4_39_1_, modifyappr0_.targetType as targetTy5_39_1_ from m_reference modifyappr0_ where ( modifyappr0_.reference_type= 6) and modifyappr0_.owner_oid=?
         - insert into m_reference (targetType, owner_oid, reference_type, relation, targetOid) values (?, ?, ?, ?, ?)
         - update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
         */

        if (isUsingH2()) {
            AssertJUnit.assertEquals(5, TestStatementInspector.getQueryCount());
        }

        try (EntityManager em = factory.createEntityManager()) {
            RUser u = em.find(RUser.class, userOid);
            AssertJUnit.assertEquals(1, u.getModifyApproverRef().size());

            //noinspection unchecked
            assertReferences((Collection) u.getModifyApproverRef(),
                    createRepoRef(UserType.COMPLEX_TYPE, "654"));
        }
    }

    @Test
    public void test187DeleteModifyApproverRef() throws Exception {
        OperationResult result = new OperationResult("test185AddModifyApproverRef");

        List<ItemDelta<?, ?>> modifications = prismContext.deltaFor(UserType.class)
                .item(UserType.F_METADATA, MetadataType.F_MODIFY_APPROVER_REF)
                .delete(createRef(UserType.COMPLEX_TYPE, "654"))
                .asItemDeltas();

        TestStatementInspector.start();
        repositoryService.modifyObject(UserType.class, userOid, modifications, result);
        TestStatementInspector.dump();

        /*
         - select oid from m_object where oid = ? for update
         - select ruser0_.oid as oid1_27_, ruser0_2_.createChannel as createCh2_27_, ruser0_2_.createTimestamp as createTi3_27_, ruser0_2_.creatorRef_relation as creatorR4_27_, ruser0_2_.creatorRef_targetOid as creatorR5_27_, ruser0_2_.creatorRef_type as creatorR6_27_, ruser0_2_.fullObject as fullObje7_27_, ruser0_2_.lifecycleState as lifecycl8_27_, ruser0_2_.modifierRef_relation as modifier9_27_, ruser0_2_.modifierRef_targetOid as modifie10_27_, ruser0_2_.modifierRef_type as modifie11_27_, ruser0_2_.modifyChannel as modifyC12_27_, ruser0_2_.modifyTimestamp as modifyT13_27_, ruser0_2_.name_norm as name_no14_27_, ruser0_2_.name_orig as name_or15_27_, ruser0_2_.objectTypeClass as objectT16_27_, ruser0_2_.tenantRef_relation as tenantR17_27_, ruser0_2_.tenantRef_targetOid as tenantR18_27_, ruser0_2_.tenantRef_type as tenantR19_27_, ruser0_2_.version as version20_27_, ruser0_1_.administrativeStatus as administ1_53_, ruser0_1_.archiveTimestamp as archiveT2_53_, ruser0_1_.disableReason as disableR3_53_, ruser0_1_.disableTimestamp as disableT4_53_, ruser0_1_.effectiveStatus as effectiv5_53_, ruser0_1_.enableTimestamp as enableTi6_53_, ruser0_1_.validFrom as validFro7_53_, ruser0_1_.validTo as validTo8_53_, ruser0_1_.validityChangeTimestamp as validity9_53_, ruser0_1_.validityStatus as validit10_53_, ruser0_1_.costCenter as costCen11_53_, ruser0_1_.emailAddress as emailAd12_53_, ruser0_1_.hasPhoto as hasPhot13_53_, ruser0_1_.locale as locale14_53_, ruser0_1_.locality_norm as localit15_53_, ruser0_1_.locality_orig as localit16_53_, ruser0_1_.preferredLanguage as preferr17_53_, ruser0_1_.telephoneNumber as telepho18_53_, ruser0_1_.timezone as timezon19_53_, ruser0_.additionalName_norm as addition1_73_, ruser0_.additionalName_orig as addition2_73_, ruser0_.employeeNumber as employee3_73_, ruser0_.familyName_norm as familyNa4_73_, ruser0_.familyName_orig as familyNa5_73_, ruser0_.fullName_norm as fullName6_73_, ruser0_.fullName_orig as fullName7_73_, ruser0_.givenName_norm as givenNam8_73_, ruser0_.givenName_orig as givenNam9_73_, ruser0_.honorificPrefix_norm as honorif10_73_, ruser0_.honorificPrefix_orig as honorif11_73_, ruser0_.honorificSuffix_norm as honorif12_73_, ruser0_.honorificSuffix_orig as honorif13_73_, ruser0_.name_norm as name_no14_73_, ruser0_.name_orig as name_or15_73_, ruser0_.nickName_norm as nickNam16_73_, ruser0_.nickName_orig as nickNam17_73_, ruser0_.title_norm as title_n18_73_, ruser0_.title_orig as title_o19_73_ from m_user ruser0_ inner join m_focus ruser0_1_ on ruser0_.oid=ruser0_1_.oid inner join m_object ruser0_2_ on ruser0_.oid=ruser0_2_.oid where ruser0_.oid=?
         - select modifyappr0_.owner_oid as owner_oi1_39_0_, modifyappr0_.reference_type as referenc2_39_0_, modifyappr0_.relation as relation3_39_0_, modifyappr0_.targetOid as targetOi4_39_0_, modifyappr0_.owner_oid as owner_oi1_39_1_, modifyappr0_.reference_type as referenc2_39_1_, modifyappr0_.relation as relation3_39_1_, modifyappr0_.targetOid as targetOi4_39_1_, modifyappr0_.targetType as targetTy5_39_1_ from m_reference modifyappr0_ where ( modifyappr0_.reference_type= 6) and modifyappr0_.owner_oid=?
         - update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
         - delete from m_reference where owner_oid=? and reference_type=? and relation=? and targetOid=?
         */

        if (isUsingH2()) {
            AssertJUnit.assertEquals(5, TestStatementInspector.getQueryCount());
        }

        try (EntityManager em = factory.createEntityManager()) {
            RUser u = em.find(RUser.class, userOid);
            AssertJUnit.assertEquals(0, u.getModifyApproverRef().size());
        }
    }

    @Test
    public void test190ReplaceWholeMetadata() throws Exception {
        OperationResult result = new OperationResult("test190ReplaceWholeMetadata");

        List<ItemDelta<?, ?>> modifications = prismContext.deltaFor(UserType.class)
                .item(UserType.F_METADATA)
                .replace(new MetadataType(prismContext)
                        .createChannel("ch1")
                        .modifierRef(createRef(UserType.COMPLEX_TYPE, "44"))
                        .createApproverRef(createRef(UserType.COMPLEX_TYPE, "55"))
                        .createApproverRef(createRef(UserType.COMPLEX_TYPE, "77"))
                        .modifyApproverRef(createRef(UserType.COMPLEX_TYPE, "55"))
                        .modifyApproverRef(createRef(UserType.COMPLEX_TYPE, "66"))
                )
                .asItemDeltas();

        TestStatementInspector.start();
        repositoryService.modifyObject(UserType.class, userOid, modifications, result);
        TestStatementInspector.dump();

        /*
         - select oid from m_object where oid = ? for update
         - select ruser0_.oid as oid1_27_, ruser0_2_.createChannel as createCh2_27_, ruser0_2_.createTimestamp as createTi3_27_, ruser0_2_.creatorRef_relation as creatorR4_27_, ruser0_2_.creatorRef_targetOid as creatorR5_27_, ruser0_2_.creatorRef_type as creatorR6_27_, ruser0_2_.fullObject as fullObje7_27_, ruser0_2_.lifecycleState as lifecycl8_27_, ruser0_2_.modifierRef_relation as modifier9_27_, ruser0_2_.modifierRef_targetOid as modifie10_27_, ruser0_2_.modifierRef_type as modifie11_27_, ruser0_2_.modifyChannel as modifyC12_27_, ruser0_2_.modifyTimestamp as modifyT13_27_, ruser0_2_.name_norm as name_no14_27_, ruser0_2_.name_orig as name_or15_27_, ruser0_2_.objectTypeClass as objectT16_27_, ruser0_2_.tenantRef_relation as tenantR17_27_, ruser0_2_.tenantRef_targetOid as tenantR18_27_, ruser0_2_.tenantRef_type as tenantR19_27_, ruser0_2_.version as version20_27_, ruser0_1_.administrativeStatus as administ1_53_, ruser0_1_.archiveTimestamp as archiveT2_53_, ruser0_1_.disableReason as disableR3_53_, ruser0_1_.disableTimestamp as disableT4_53_, ruser0_1_.effectiveStatus as effectiv5_53_, ruser0_1_.enableTimestamp as enableTi6_53_, ruser0_1_.validFrom as validFro7_53_, ruser0_1_.validTo as validTo8_53_, ruser0_1_.validityChangeTimestamp as validity9_53_, ruser0_1_.validityStatus as validit10_53_, ruser0_1_.costCenter as costCen11_53_, ruser0_1_.emailAddress as emailAd12_53_, ruser0_1_.hasPhoto as hasPhot13_53_, ruser0_1_.locale as locale14_53_, ruser0_1_.locality_norm as localit15_53_, ruser0_1_.locality_orig as localit16_53_, ruser0_1_.preferredLanguage as preferr17_53_, ruser0_1_.telephoneNumber as telepho18_53_, ruser0_1_.timezone as timezon19_53_, ruser0_.additionalName_norm as addition1_73_, ruser0_.additionalName_orig as addition2_73_, ruser0_.employeeNumber as employee3_73_, ruser0_.familyName_norm as familyNa4_73_, ruser0_.familyName_orig as familyNa5_73_, ruser0_.fullName_norm as fullName6_73_, ruser0_.fullName_orig as fullName7_73_, ruser0_.givenName_norm as givenNam8_73_, ruser0_.givenName_orig as givenNam9_73_, ruser0_.honorificPrefix_norm as honorif10_73_, ruser0_.honorificPrefix_orig as honorif11_73_, ruser0_.honorificSuffix_norm as honorif12_73_, ruser0_.honorificSuffix_orig as honorif13_73_, ruser0_.name_norm as name_no14_73_, ruser0_.name_orig as name_or15_73_, ruser0_.nickName_norm as nickNam16_73_, ruser0_.nickName_orig as nickNam17_73_, ruser0_.title_norm as title_n18_73_, ruser0_.title_orig as title_o19_73_ from m_user ruser0_ inner join m_focus ruser0_1_ on ruser0_.oid=ruser0_1_.oid inner join m_object ruser0_2_ on ruser0_.oid=ruser0_2_.oid where ruser0_.oid=?
         - select createappr0_.owner_oid as owner_oi1_39_0_, createappr0_.reference_type as referenc2_39_0_, createappr0_.relation as relation3_39_0_, createappr0_.targetOid as targetOi4_39_0_, createappr0_.owner_oid as owner_oi1_39_1_, createappr0_.reference_type as referenc2_39_1_, createappr0_.relation as relation3_39_1_, createappr0_.targetOid as targetOi4_39_1_, createappr0_.targetType as targetTy5_39_1_ from m_reference createappr0_ where ( createappr0_.reference_type= 5) and createappr0_.owner_oid=?
         - select modifyappr0_.owner_oid as owner_oi1_39_0_, modifyappr0_.reference_type as referenc2_39_0_, modifyappr0_.relation as relation3_39_0_, modifyappr0_.targetOid as targetOi4_39_0_, modifyappr0_.owner_oid as owner_oi1_39_1_, modifyappr0_.reference_type as referenc2_39_1_, modifyappr0_.relation as relation3_39_1_, modifyappr0_.targetOid as targetOi4_39_1_, modifyappr0_.targetType as targetTy5_39_1_ from m_reference modifyappr0_ where ( modifyappr0_.reference_type= 6) and modifyappr0_.owner_oid=?
         - select robjectref_.owner_oid, robjectref_.reference_type, robjectref_.relation, robjectref_.targetOid, robjectref_.targetType as targetTy5_39_ from m_reference robjectref_ where robjectref_.owner_oid=? and robjectref_.reference_type=? and robjectref_.relation=? and robjectref_.targetOid=?
         - select robjectref_.owner_oid, robjectref_.reference_type, robjectref_.relation, robjectref_.targetOid, robjectref_.targetType as targetTy5_39_ from m_reference robjectref_ where robjectref_.owner_oid=? and robjectref_.reference_type=? and robjectref_.relation=? and robjectref_.targetOid=?
         - select robjectref_.owner_oid, robjectref_.reference_type, robjectref_.relation, robjectref_.targetOid, robjectref_.targetType as targetTy5_39_ from m_reference robjectref_ where robjectref_.owner_oid=? and robjectref_.reference_type=? and robjectref_.relation=? and robjectref_.targetOid=?
         - select robjectref_.owner_oid, robjectref_.reference_type, robjectref_.relation, robjectref_.targetOid, robjectref_.targetType as targetTy5_39_ from m_reference robjectref_ where robjectref_.owner_oid=? and robjectref_.reference_type=? and robjectref_.relation=? and robjectref_.targetOid=?
         - insert into m_reference (targetType, owner_oid, reference_type, relation, targetOid) values (?, ?, ?, ?, ?)
         - update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
         - delete from m_reference where owner_oid=? and reference_type=? and relation=? and targetOid=?
         */

        if (isUsingH2()) {
            AssertJUnit.assertEquals(7, TestStatementInspector.getQueryCount());
        }

        try (EntityManager em = factory.createEntityManager()) {
            RUser u = em.find(RUser.class, userOid);
            AssertJUnit.assertEquals("ch1", u.getCreateChannel());
            AssertJUnit.assertEquals(2, u.getModifyApproverRef().size());
            AssertJUnit.assertEquals(2, u.getCreateApproverRef().size());

            assertEquals(u.getModifierRef(),
                    createEmbeddedRepoRef(UserType.COMPLEX_TYPE, "44"));
            //noinspection unchecked
            assertReferences((Collection) u.getCreateApproverRef(),
                    createRepoRef(UserType.COMPLEX_TYPE, "55"),
                    createRepoRef(UserType.COMPLEX_TYPE, "77"));
            //noinspection unchecked
            assertReferences((Collection) u.getModifyApproverRef(),
                    createRepoRef(UserType.COMPLEX_TYPE, "55"),
                    createRepoRef(UserType.COMPLEX_TYPE, "66"));
        }
    }

    @Test
    public void test192DeleteWholeMetadata() throws Exception {
        OperationResult result = new OperationResult("test192DeleteWholeMetadata");

        List<ItemDelta<?, ?>> modifications = prismContext.deltaFor(UserType.class)
                .item(UserType.F_METADATA)
                .delete(new MetadataType(prismContext)
                        .createChannel("ch1")
                        .modifierRef(createRef(UserType.COMPLEX_TYPE, "44"))
                        .createApproverRef(createRef(UserType.COMPLEX_TYPE, "55"))
                        .createApproverRef(createRef(UserType.COMPLEX_TYPE, "77"))
                        .modifyApproverRef(createRef(UserType.COMPLEX_TYPE, "55"))
                        .modifyApproverRef(createRef(UserType.COMPLEX_TYPE, "66"))
                )
                .asItemDeltas();

        TestStatementInspector.start();
        repositoryService.modifyObject(UserType.class, userOid, modifications, result);
        TestStatementInspector.dump();

        /*
         - select oid from m_object where oid = ? for update
         - select ruser0_.oid as oid1_27_, ruser0_2_.createChannel as createCh2_27_, ruser0_2_.createTimestamp as createTi3_27_, ruser0_2_.creatorRef_relation as creatorR4_27_, ruser0_2_.creatorRef_targetOid as creatorR5_27_, ruser0_2_.creatorRef_type as creatorR6_27_, ruser0_2_.fullObject as fullObje7_27_, ruser0_2_.lifecycleState as lifecycl8_27_, ruser0_2_.modifierRef_relation as modifier9_27_, ruser0_2_.modifierRef_targetOid as modifie10_27_, ruser0_2_.modifierRef_type as modifie11_27_, ruser0_2_.modifyChannel as modifyC12_27_, ruser0_2_.modifyTimestamp as modifyT13_27_, ruser0_2_.name_norm as name_no14_27_, ruser0_2_.name_orig as name_or15_27_, ruser0_2_.objectTypeClass as objectT16_27_, ruser0_2_.tenantRef_relation as tenantR17_27_, ruser0_2_.tenantRef_targetOid as tenantR18_27_, ruser0_2_.tenantRef_type as tenantR19_27_, ruser0_2_.version as version20_27_, ruser0_1_.administrativeStatus as administ1_53_, ruser0_1_.archiveTimestamp as archiveT2_53_, ruser0_1_.disableReason as disableR3_53_, ruser0_1_.disableTimestamp as disableT4_53_, ruser0_1_.effectiveStatus as effectiv5_53_, ruser0_1_.enableTimestamp as enableTi6_53_, ruser0_1_.validFrom as validFro7_53_, ruser0_1_.validTo as validTo8_53_, ruser0_1_.validityChangeTimestamp as validity9_53_, ruser0_1_.validityStatus as validit10_53_, ruser0_1_.costCenter as costCen11_53_, ruser0_1_.emailAddress as emailAd12_53_, ruser0_1_.hasPhoto as hasPhot13_53_, ruser0_1_.locale as locale14_53_, ruser0_1_.locality_norm as localit15_53_, ruser0_1_.locality_orig as localit16_53_, ruser0_1_.preferredLanguage as preferr17_53_, ruser0_1_.telephoneNumber as telepho18_53_, ruser0_1_.timezone as timezon19_53_, ruser0_.additionalName_norm as addition1_73_, ruser0_.additionalName_orig as addition2_73_, ruser0_.employeeNumber as employee3_73_, ruser0_.familyName_norm as familyNa4_73_, ruser0_.familyName_orig as familyNa5_73_, ruser0_.fullName_norm as fullName6_73_, ruser0_.fullName_orig as fullName7_73_, ruser0_.givenName_norm as givenNam8_73_, ruser0_.givenName_orig as givenNam9_73_, ruser0_.honorificPrefix_norm as honorif10_73_, ruser0_.honorificPrefix_orig as honorif11_73_, ruser0_.honorificSuffix_norm as honorif12_73_, ruser0_.honorificSuffix_orig as honorif13_73_, ruser0_.name_norm as name_no14_73_, ruser0_.name_orig as name_or15_73_, ruser0_.nickName_norm as nickNam16_73_, ruser0_.nickName_orig as nickNam17_73_, ruser0_.title_norm as title_n18_73_, ruser0_.title_orig as title_o19_73_ from m_user ruser0_ inner join m_focus ruser0_1_ on ruser0_.oid=ruser0_1_.oid inner join m_object ruser0_2_ on ruser0_.oid=ruser0_2_.oid where ruser0_.oid=?
         - select createappr0_.owner_oid as owner_oi1_39_0_, createappr0_.reference_type as referenc2_39_0_, createappr0_.relation as relation3_39_0_, createappr0_.targetOid as targetOi4_39_0_, createappr0_.owner_oid as owner_oi1_39_1_, createappr0_.reference_type as referenc2_39_1_, createappr0_.relation as relation3_39_1_, createappr0_.targetOid as targetOi4_39_1_, createappr0_.targetType as targetTy5_39_1_ from m_reference createappr0_ where ( createappr0_.reference_type= 5) and createappr0_.owner_oid=?
         - select modifyappr0_.owner_oid as owner_oi1_39_0_, modifyappr0_.reference_type as referenc2_39_0_, modifyappr0_.relation as relation3_39_0_, modifyappr0_.targetOid as targetOi4_39_0_, modifyappr0_.owner_oid as owner_oi1_39_1_, modifyappr0_.reference_type as referenc2_39_1_, modifyappr0_.relation as relation3_39_1_, modifyappr0_.targetOid as targetOi4_39_1_, modifyappr0_.targetType as targetTy5_39_1_ from m_reference modifyappr0_ where ( modifyappr0_.reference_type= 6) and modifyappr0_.owner_oid=?
         - update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
         - delete from m_reference where owner_oid=? and reference_type=? and relation=? and targetOid=?
         */

        if (isUsingH2()) {
            AssertJUnit.assertEquals(6, TestStatementInspector.getQueryCount());
        }

        try (EntityManager em = factory.createEntityManager()) {
            RUser u = em.find(RUser.class, userOid);
            AssertJUnit.assertNull(u.getCreateChannel());
            AssertJUnit.assertNull(u.getModifierRef());
            AssertJUnit.assertEquals(0, u.getModifyApproverRef().size());
            AssertJUnit.assertEquals(0, u.getCreateApproverRef().size());
        }
    }

    @Test
    public void test194AddWholeMetadata() throws Exception {
        OperationResult result = new OperationResult("test194AddWholeMetadata");

        List<ItemDelta<?, ?>> modifications = prismContext.deltaFor(UserType.class)
                .item(UserType.F_METADATA)
                .add(new MetadataType(prismContext)
                        .createChannel("ch1")
                        .modifierRef(createRef(UserType.COMPLEX_TYPE, "44"))
                        .createApproverRef(createRef(UserType.COMPLEX_TYPE, "55"))
                        .createApproverRef(createRef(UserType.COMPLEX_TYPE, "77"))
                        .modifyApproverRef(createRef(UserType.COMPLEX_TYPE, "55"))
                        .modifyApproverRef(createRef(UserType.COMPLEX_TYPE, "66"))
                )
                .asItemDeltas();

        TestStatementInspector.start();
        repositoryService.modifyObject(UserType.class, userOid, modifications, result);
        TestStatementInspector.dump();

        /*
         - select oid from m_object where oid = ? for update
         - select ruser0_.oid as oid1_27_, ruser0_2_.createChannel as createCh2_27_, ruser0_2_.createTimestamp as createTi3_27_, ruser0_2_.creatorRef_relation as creatorR4_27_, ruser0_2_.creatorRef_targetOid as creatorR5_27_, ruser0_2_.creatorRef_type as creatorR6_27_, ruser0_2_.fullObject as fullObje7_27_, ruser0_2_.lifecycleState as lifecycl8_27_, ruser0_2_.modifierRef_relation as modifier9_27_, ruser0_2_.modifierRef_targetOid as modifie10_27_, ruser0_2_.modifierRef_type as modifie11_27_, ruser0_2_.modifyChannel as modifyC12_27_, ruser0_2_.modifyTimestamp as modifyT13_27_, ruser0_2_.name_norm as name_no14_27_, ruser0_2_.name_orig as name_or15_27_, ruser0_2_.objectTypeClass as objectT16_27_, ruser0_2_.tenantRef_relation as tenantR17_27_, ruser0_2_.tenantRef_targetOid as tenantR18_27_, ruser0_2_.tenantRef_type as tenantR19_27_, ruser0_2_.version as version20_27_, ruser0_1_.administrativeStatus as administ1_53_, ruser0_1_.archiveTimestamp as archiveT2_53_, ruser0_1_.disableReason as disableR3_53_, ruser0_1_.disableTimestamp as disableT4_53_, ruser0_1_.effectiveStatus as effectiv5_53_, ruser0_1_.enableTimestamp as enableTi6_53_, ruser0_1_.validFrom as validFro7_53_, ruser0_1_.validTo as validTo8_53_, ruser0_1_.validityChangeTimestamp as validity9_53_, ruser0_1_.validityStatus as validit10_53_, ruser0_1_.costCenter as costCen11_53_, ruser0_1_.emailAddress as emailAd12_53_, ruser0_1_.hasPhoto as hasPhot13_53_, ruser0_1_.locale as locale14_53_, ruser0_1_.locality_norm as localit15_53_, ruser0_1_.locality_orig as localit16_53_, ruser0_1_.preferredLanguage as preferr17_53_, ruser0_1_.telephoneNumber as telepho18_53_, ruser0_1_.timezone as timezon19_53_, ruser0_.additionalName_norm as addition1_73_, ruser0_.additionalName_orig as addition2_73_, ruser0_.employeeNumber as employee3_73_, ruser0_.familyName_norm as familyNa4_73_, ruser0_.familyName_orig as familyNa5_73_, ruser0_.fullName_norm as fullName6_73_, ruser0_.fullName_orig as fullName7_73_, ruser0_.givenName_norm as givenNam8_73_, ruser0_.givenName_orig as givenNam9_73_, ruser0_.honorificPrefix_norm as honorif10_73_, ruser0_.honorificPrefix_orig as honorif11_73_, ruser0_.honorificSuffix_norm as honorif12_73_, ruser0_.honorificSuffix_orig as honorif13_73_, ruser0_.name_norm as name_no14_73_, ruser0_.name_orig as name_or15_73_, ruser0_.nickName_norm as nickNam16_73_, ruser0_.nickName_orig as nickNam17_73_, ruser0_.title_norm as title_n18_73_, ruser0_.title_orig as title_o19_73_ from m_user ruser0_ inner join m_focus ruser0_1_ on ruser0_.oid=ruser0_1_.oid inner join m_object ruser0_2_ on ruser0_.oid=ruser0_2_.oid where ruser0_.oid=?
         - select createappr0_.owner_oid as owner_oi1_39_0_, createappr0_.reference_type as referenc2_39_0_, createappr0_.relation as relation3_39_0_, createappr0_.targetOid as targetOi4_39_0_, createappr0_.owner_oid as owner_oi1_39_1_, createappr0_.reference_type as referenc2_39_1_, createappr0_.relation as relation3_39_1_, createappr0_.targetOid as targetOi4_39_1_, createappr0_.targetType as targetTy5_39_1_ from m_reference createappr0_ where ( createappr0_.reference_type= 5) and createappr0_.owner_oid=?
         - select modifyappr0_.owner_oid as owner_oi1_39_0_, modifyappr0_.reference_type as referenc2_39_0_, modifyappr0_.relation as relation3_39_0_, modifyappr0_.targetOid as targetOi4_39_0_, modifyappr0_.owner_oid as owner_oi1_39_1_, modifyappr0_.reference_type as referenc2_39_1_, modifyappr0_.relation as relation3_39_1_, modifyappr0_.targetOid as targetOi4_39_1_, modifyappr0_.targetType as targetTy5_39_1_ from m_reference modifyappr0_ where ( modifyappr0_.reference_type= 6) and modifyappr0_.owner_oid=?
         - select robjectref_.owner_oid, robjectref_.reference_type, robjectref_.relation, robjectref_.targetOid, robjectref_.targetType as targetTy5_39_ from m_reference robjectref_ where robjectref_.owner_oid=? and robjectref_.reference_type=? and robjectref_.relation=? and robjectref_.targetOid=?
         - select robjectref_.owner_oid, robjectref_.reference_type, robjectref_.relation, robjectref_.targetOid, robjectref_.targetType as targetTy5_39_ from m_reference robjectref_ where robjectref_.owner_oid=? and robjectref_.reference_type=? and robjectref_.relation=? and robjectref_.targetOid=?
         - select robjectref_.owner_oid, robjectref_.reference_type, robjectref_.relation, robjectref_.targetOid, robjectref_.targetType as targetTy5_39_ from m_reference robjectref_ where robjectref_.owner_oid=? and robjectref_.reference_type=? and robjectref_.relation=? and robjectref_.targetOid=?
         - select robjectref_.owner_oid, robjectref_.reference_type, robjectref_.relation, robjectref_.targetOid, robjectref_.targetType as targetTy5_39_ from m_reference robjectref_ where robjectref_.owner_oid=? and robjectref_.reference_type=? and robjectref_.relation=? and robjectref_.targetOid=?
         - insert into m_reference (targetType, owner_oid, reference_type, relation, targetOid) values (?, ?, ?, ?, ?)
         - update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
         */

        if (isUsingH2()) {
            AssertJUnit.assertEquals(6, TestStatementInspector.getQueryCount());
        }

        try (EntityManager em = factory.createEntityManager()) {
            RUser u = em.find(RUser.class, userOid);
            AssertJUnit.assertEquals("ch1", u.getCreateChannel());
            AssertJUnit.assertEquals(2, u.getModifyApproverRef().size());
            AssertJUnit.assertEquals(2, u.getCreateApproverRef().size());

            assertEquals(u.getModifierRef(),
                    createEmbeddedRepoRef(UserType.COMPLEX_TYPE, "44"));
            //noinspection unchecked
            assertReferences((Collection) u.getCreateApproverRef(),
                    createRepoRef(UserType.COMPLEX_TYPE, "55"),
                    createRepoRef(UserType.COMPLEX_TYPE, "77"));
            //noinspection unchecked
            assertReferences((Collection) u.getModifyApproverRef(),
                    createRepoRef(UserType.COMPLEX_TYPE, "55"),
                    createRepoRef(UserType.COMPLEX_TYPE, "66"));
        }
    }

    @Test
    public void test200ReplaceAssignmentMetadataCreateChannel() throws Exception {
        OperationResult result = new OperationResult("test200ReplaceAssignmentMetadataCreateChannel");

        List<ItemDelta<?, ?>> modifications = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT, 2, AssignmentType.F_METADATA, MetadataType.F_CREATE_CHANNEL)
                .replace("asdf2")
                .asItemDeltas();

        TestStatementInspector.start();
        repositoryService.modifyObject(UserType.class, userOid, modifications, result);
        TestStatementInspector.dump();

        /*
         - select oid from m_object where oid = ? for update
         - select ruser0_.oid as oid1_27_, ruser0_2_.createChannel as createCh2_27_, ruser0_2_.createTimestamp as createTi3_27_, ruser0_2_.creatorRef_relation as creatorR4_27_, ruser0_2_.creatorRef_targetOid as creatorR5_27_, ruser0_2_.creatorRef_type as creatorR6_27_, ruser0_2_.fullObject as fullObje7_27_, ruser0_2_.lifecycleState as lifecycl8_27_, ruser0_2_.modifierRef_relation as modifier9_27_, ruser0_2_.modifierRef_targetOid as modifie10_27_, ruser0_2_.modifierRef_type as modifie11_27_, ruser0_2_.modifyChannel as modifyC12_27_, ruser0_2_.modifyTimestamp as modifyT13_27_, ruser0_2_.name_norm as name_no14_27_, ruser0_2_.name_orig as name_or15_27_, ruser0_2_.objectTypeClass as objectT16_27_, ruser0_2_.tenantRef_relation as tenantR17_27_, ruser0_2_.tenantRef_targetOid as tenantR18_27_, ruser0_2_.tenantRef_type as tenantR19_27_, ruser0_2_.version as version20_27_, ruser0_1_.administrativeStatus as administ1_53_, ruser0_1_.archiveTimestamp as archiveT2_53_, ruser0_1_.disableReason as disableR3_53_, ruser0_1_.disableTimestamp as disableT4_53_, ruser0_1_.effectiveStatus as effectiv5_53_, ruser0_1_.enableTimestamp as enableTi6_53_, ruser0_1_.validFrom as validFro7_53_, ruser0_1_.validTo as validTo8_53_, ruser0_1_.validityChangeTimestamp as validity9_53_, ruser0_1_.validityStatus as validit10_53_, ruser0_1_.costCenter as costCen11_53_, ruser0_1_.emailAddress as emailAd12_53_, ruser0_1_.hasPhoto as hasPhot13_53_, ruser0_1_.locale as locale14_53_, ruser0_1_.locality_norm as localit15_53_, ruser0_1_.locality_orig as localit16_53_, ruser0_1_.preferredLanguage as preferr17_53_, ruser0_1_.telephoneNumber as telepho18_53_, ruser0_1_.timezone as timezon19_53_, ruser0_.additionalName_norm as addition1_73_, ruser0_.additionalName_orig as addition2_73_, ruser0_.employeeNumber as employee3_73_, ruser0_.familyName_norm as familyNa4_73_, ruser0_.familyName_orig as familyNa5_73_, ruser0_.fullName_norm as fullName6_73_, ruser0_.fullName_orig as fullName7_73_, ruser0_.givenName_norm as givenNam8_73_, ruser0_.givenName_orig as givenNam9_73_, ruser0_.honorificPrefix_norm as honorif10_73_, ruser0_.honorificPrefix_orig as honorif11_73_, ruser0_.honorificSuffix_norm as honorif12_73_, ruser0_.honorificSuffix_orig as honorif13_73_, ruser0_.name_norm as name_no14_73_, ruser0_.name_orig as name_or15_73_, ruser0_.nickName_norm as nickNam16_73_, ruser0_.nickName_orig as nickNam17_73_, ruser0_.title_norm as title_n18_73_, ruser0_.title_orig as title_o19_73_ from m_user ruser0_ inner join m_focus ruser0_1_ on ruser0_.oid=ruser0_1_.oid inner join m_object ruser0_2_ on ruser0_.oid=ruser0_2_.oid where ruser0_.oid=?
         - select assignment0_.owner_oid as owner_oi2_5_0_, assignment0_.id as id1_5_0_, assignment0_.id as id1_5_1_, assignment0_.owner_oid as owner_oi2_5_1_, assignment0_.administrativeStatus as administ3_5_1_, assignment0_.archiveTimestamp as archiveT4_5_1_, assignment0_.disableReason as disableR5_5_1_, assignment0_.disableTimestamp as disableT6_5_1_, assignment0_.effectiveStatus as effectiv7_5_1_, assignment0_.enableTimestamp as enableTi8_5_1_, assignment0_.validFrom as validFro9_5_1_, assignment0_.validTo as validTo10_5_1_, assignment0_.validityChangeTimestamp as validit11_5_1_, assignment0_.validityStatus as validit12_5_1_, assignment0_.assignmentOwner as assignm13_5_1_, assignment0_.createChannel as createC14_5_1_, assignment0_.createTimestamp as createT15_5_1_, assignment0_.creatorRef_relation as creator16_5_1_, assignment0_.creatorRef_targetOid as creator17_5_1_, assignment0_.creatorRef_type as creator18_5_1_, assignment0_.extId as extId38_5_1_, assignment0_.extOid as extOid39_5_1_, assignment0_.lifecycleState as lifecyc19_5_1_, assignment0_.modifierRef_relation as modifie20_5_1_, assignment0_.modifierRef_targetOid as modifie21_5_1_, assignment0_.modifierRef_type as modifie22_5_1_, assignment0_.modifyChannel as modifyC23_5_1_, assignment0_.modifyTimestamp as modifyT24_5_1_, assignment0_.orderValue as orderVa25_5_1_, assignment0_.orgRef_relation as orgRef_26_5_1_, assignment0_.orgRef_targetOid as orgRef_27_5_1_, assignment0_.orgRef_type as orgRef_28_5_1_, assignment0_.resourceRef_relation as resourc29_5_1_, assignment0_.resourceRef_targetOid as resourc30_5_1_, assignment0_.resourceRef_type as resourc31_5_1_, assignment0_.targetRef_relation as targetR32_5_1_, assignment0_.targetRef_targetOid as targetR33_5_1_, assignment0_.targetRef_type as targetR34_5_1_, assignment0_.tenantRef_relation as tenantR35_5_1_, assignment0_.tenantRef_targetOid as tenantR36_5_1_, assignment0_.tenantRef_type as tenantR37_5_1_, rassignmen1_.owner_id as owner_id1_12_2_, rassignmen1_.owner_owner_oid as owner_ow2_12_2_, rassignmen1_.booleansCount as booleans3_12_2_, rassignmen1_.datesCount as datesCou4_12_2_, rassignmen1_.longsCount as longsCou5_12_2_, rassignmen1_.polysCount as polysCou6_12_2_, rassignmen1_.referencesCount as referenc7_12_2_, rassignmen1_.stringsCount as stringsC8_12_2_ from m_assignment assignment0_ left outer join m_assignment_extension rassignmen1_ on assignment0_.extId=rassignmen1_.owner_id and assignment0_.extOid=rassignmen1_.owner_owner_oid where assignment0_.owner_oid=?
         - update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
         - update m_assignment set administrativeStatus=?, archiveTimestamp=?, disableReason=?, disableTimestamp=?, effectiveStatus=?, enableTimestamp=?, validFrom=?, validTo=?, validityChangeTimestamp=?, validityStatus=?, assignmentOwner=?, createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, extId=?, extOid=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, orderValue=?, orgRef_relation=?, orgRef_targetOid=?, orgRef_type=?, resourceRef_relation=?, resourceRef_targetOid=?, resourceRef_type=?, targetRef_relation=?, targetRef_targetOid=?, targetRef_type=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=? where id=? and owner_oid=?
         */

        if (isUsingH2()) {
            AssertJUnit.assertEquals(5, TestStatementInspector.getQueryCount());
        }

        try (EntityManager em = factory.createEntityManager()) {
            RUser u = em.find(RUser.class, userOid);
            AssertJUnit.assertEquals("asdf2", u.getAssignments().iterator().next().getCreateChannel());
        }
    }

    @Test
    public void test210ReplaceSomeAssignmentMetadataItems() throws Exception {
        OperationResult result = new OperationResult("test210ReplaceSomeAssignmentMetadataItems");

        List<ItemDelta<?, ?>> modifications = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT, 2, AssignmentType.F_METADATA, MetadataType.F_CREATE_APPROVER_REF)
                .replace(createRef(UserType.COMPLEX_TYPE, "1112"))
                .item(UserType.F_ASSIGNMENT, 2, AssignmentType.F_METADATA, MetadataType.F_CREATE_CHANNEL)
                .replace("zxcv2")
                .asItemDeltas();

        TestStatementInspector.start();
        repositoryService.modifyObject(UserType.class, userOid, modifications, result);
        TestStatementInspector.dump();

        /*
         - select oid from m_object where oid = ? for update
         - select ruser0_.oid as oid1_27_, ruser0_2_.createChannel as createCh2_27_, ruser0_2_.createTimestamp as createTi3_27_, ruser0_2_.creatorRef_relation as creatorR4_27_, ruser0_2_.creatorRef_targetOid as creatorR5_27_, ruser0_2_.creatorRef_type as creatorR6_27_, ruser0_2_.fullObject as fullObje7_27_, ruser0_2_.lifecycleState as lifecycl8_27_, ruser0_2_.modifierRef_relation as modifier9_27_, ruser0_2_.modifierRef_targetOid as modifie10_27_, ruser0_2_.modifierRef_type as modifie11_27_, ruser0_2_.modifyChannel as modifyC12_27_, ruser0_2_.modifyTimestamp as modifyT13_27_, ruser0_2_.name_norm as name_no14_27_, ruser0_2_.name_orig as name_or15_27_, ruser0_2_.objectTypeClass as objectT16_27_, ruser0_2_.tenantRef_relation as tenantR17_27_, ruser0_2_.tenantRef_targetOid as tenantR18_27_, ruser0_2_.tenantRef_type as tenantR19_27_, ruser0_2_.version as version20_27_, ruser0_1_.administrativeStatus as administ1_53_, ruser0_1_.archiveTimestamp as archiveT2_53_, ruser0_1_.disableReason as disableR3_53_, ruser0_1_.disableTimestamp as disableT4_53_, ruser0_1_.effectiveStatus as effectiv5_53_, ruser0_1_.enableTimestamp as enableTi6_53_, ruser0_1_.validFrom as validFro7_53_, ruser0_1_.validTo as validTo8_53_, ruser0_1_.validityChangeTimestamp as validity9_53_, ruser0_1_.validityStatus as validit10_53_, ruser0_1_.costCenter as costCen11_53_, ruser0_1_.emailAddress as emailAd12_53_, ruser0_1_.hasPhoto as hasPhot13_53_, ruser0_1_.locale as locale14_53_, ruser0_1_.locality_norm as localit15_53_, ruser0_1_.locality_orig as localit16_53_, ruser0_1_.preferredLanguage as preferr17_53_, ruser0_1_.telephoneNumber as telepho18_53_, ruser0_1_.timezone as timezon19_53_, ruser0_.additionalName_norm as addition1_73_, ruser0_.additionalName_orig as addition2_73_, ruser0_.employeeNumber as employee3_73_, ruser0_.familyName_norm as familyNa4_73_, ruser0_.familyName_orig as familyNa5_73_, ruser0_.fullName_norm as fullName6_73_, ruser0_.fullName_orig as fullName7_73_, ruser0_.givenName_norm as givenNam8_73_, ruser0_.givenName_orig as givenNam9_73_, ruser0_.honorificPrefix_norm as honorif10_73_, ruser0_.honorificPrefix_orig as honorif11_73_, ruser0_.honorificSuffix_norm as honorif12_73_, ruser0_.honorificSuffix_orig as honorif13_73_, ruser0_.name_norm as name_no14_73_, ruser0_.name_orig as name_or15_73_, ruser0_.nickName_norm as nickNam16_73_, ruser0_.nickName_orig as nickNam17_73_, ruser0_.title_norm as title_n18_73_, ruser0_.title_orig as title_o19_73_ from m_user ruser0_ inner join m_focus ruser0_1_ on ruser0_.oid=ruser0_1_.oid inner join m_object ruser0_2_ on ruser0_.oid=ruser0_2_.oid where ruser0_.oid=?
         - select assignment0_.owner_oid as owner_oi2_5_0_, assignment0_.id as id1_5_0_, assignment0_.id as id1_5_1_, assignment0_.owner_oid as owner_oi2_5_1_, assignment0_.administrativeStatus as administ3_5_1_, assignment0_.archiveTimestamp as archiveT4_5_1_, assignment0_.disableReason as disableR5_5_1_, assignment0_.disableTimestamp as disableT6_5_1_, assignment0_.effectiveStatus as effectiv7_5_1_, assignment0_.enableTimestamp as enableTi8_5_1_, assignment0_.validFrom as validFro9_5_1_, assignment0_.validTo as validTo10_5_1_, assignment0_.validityChangeTimestamp as validit11_5_1_, assignment0_.validityStatus as validit12_5_1_, assignment0_.assignmentOwner as assignm13_5_1_, assignment0_.createChannel as createC14_5_1_, assignment0_.createTimestamp as createT15_5_1_, assignment0_.creatorRef_relation as creator16_5_1_, assignment0_.creatorRef_targetOid as creator17_5_1_, assignment0_.creatorRef_type as creator18_5_1_, assignment0_.extId as extId38_5_1_, assignment0_.extOid as extOid39_5_1_, assignment0_.lifecycleState as lifecyc19_5_1_, assignment0_.modifierRef_relation as modifie20_5_1_, assignment0_.modifierRef_targetOid as modifie21_5_1_, assignment0_.modifierRef_type as modifie22_5_1_, assignment0_.modifyChannel as modifyC23_5_1_, assignment0_.modifyTimestamp as modifyT24_5_1_, assignment0_.orderValue as orderVa25_5_1_, assignment0_.orgRef_relation as orgRef_26_5_1_, assignment0_.orgRef_targetOid as orgRef_27_5_1_, assignment0_.orgRef_type as orgRef_28_5_1_, assignment0_.resourceRef_relation as resourc29_5_1_, assignment0_.resourceRef_targetOid as resourc30_5_1_, assignment0_.resourceRef_type as resourc31_5_1_, assignment0_.targetRef_relation as targetR32_5_1_, assignment0_.targetRef_targetOid as targetR33_5_1_, assignment0_.targetRef_type as targetR34_5_1_, assignment0_.tenantRef_relation as tenantR35_5_1_, assignment0_.tenantRef_targetOid as tenantR36_5_1_, assignment0_.tenantRef_type as tenantR37_5_1_, rassignmen1_.owner_id as owner_id1_12_2_, rassignmen1_.owner_owner_oid as owner_ow2_12_2_, rassignmen1_.booleansCount as booleans3_12_2_, rassignmen1_.datesCount as datesCou4_12_2_, rassignmen1_.longsCount as longsCou5_12_2_, rassignmen1_.polysCount as polysCou6_12_2_, rassignmen1_.referencesCount as referenc7_12_2_, rassignmen1_.stringsCount as stringsC8_12_2_ from m_assignment assignment0_ left outer join m_assignment_extension rassignmen1_ on assignment0_.extId=rassignmen1_.owner_id and assignment0_.extOid=rassignmen1_.owner_owner_oid where assignment0_.owner_oid=?
         - select createappr0_.owner_id as owner_id1_14_0_, createappr0_.owner_owner_oid as owner_ow2_14_0_, createappr0_.reference_type as referenc3_14_0_, createappr0_.relation as relation4_14_0_, createappr0_.targetOid as targetOi5_14_0_, createappr0_.owner_id as owner_id1_14_1_, createappr0_.owner_owner_oid as owner_ow2_14_1_, createappr0_.reference_type as referenc3_14_1_, createappr0_.relation as relation4_14_1_, createappr0_.targetOid as targetOi5_14_1_, createappr0_.targetType as targetTy6_14_1_ from m_assignment_reference createappr0_ where ( createappr0_.reference_type= 0) and createappr0_.owner_id=? and createappr0_.owner_owner_oid=?
         - insert into m_assignment_reference (targetType, owner_id, owner_owner_oid, reference_type, relation, targetOid) values (?, ?, ?, ?, ?, ?)
         - update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
         - update m_assignment set administrativeStatus=?, archiveTimestamp=?, disableReason=?, disableTimestamp=?, effectiveStatus=?, enableTimestamp=?, validFrom=?, validTo=?, validityChangeTimestamp=?, validityStatus=?, assignmentOwner=?, createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, extId=?, extOid=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, orderValue=?, orgRef_relation=?, orgRef_targetOid=?, orgRef_type=?, resourceRef_relation=?, resourceRef_targetOid=?, resourceRef_type=?, targetRef_relation=?, targetRef_targetOid=?, targetRef_type=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=? where id=? and owner_oid=?
         */
        if (isUsingH2()) {
            AssertJUnit.assertEquals(7, TestStatementInspector.getQueryCount());
        }

        try (EntityManager em = factory.createEntityManager()) {
            RAssignment a = em.find(RUser.class, userOid).getAssignments().iterator().next();
            AssertJUnit.assertEquals("zxcv2", a.getCreateChannel());
            AssertJUnit.assertEquals(1, a.getCreateApproverRef().size());

            //noinspection unchecked
            assertReferences((Collection) a.getCreateApproverRef(),
                    createRepoRef(UserType.COMPLEX_TYPE, "1112"));
        }
    }

    @Test
    public void test220AddAssignmentModifyApproverRef() throws Exception {
        OperationResult result = new OperationResult("test220AddAssignmentModifyApproverRef");

        List<ItemDelta<?, ?>> modifications = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT, 2, AssignmentType.F_METADATA, MetadataType.F_MODIFY_APPROVER_REF)
                .add(createRef(UserType.COMPLEX_TYPE, "6542"))
                .asItemDeltas();

        TestStatementInspector.start();
        repositoryService.modifyObject(UserType.class, userOid, modifications, result);
        TestStatementInspector.dump();

        /*
         - select oid from m_object where oid = ? for update
         - select ruser0_.oid as oid1_27_, ruser0_2_.createChannel as createCh2_27_, ruser0_2_.createTimestamp as createTi3_27_, ruser0_2_.creatorRef_relation as creatorR4_27_, ruser0_2_.creatorRef_targetOid as creatorR5_27_, ruser0_2_.creatorRef_type as creatorR6_27_, ruser0_2_.fullObject as fullObje7_27_, ruser0_2_.lifecycleState as lifecycl8_27_, ruser0_2_.modifierRef_relation as modifier9_27_, ruser0_2_.modifierRef_targetOid as modifie10_27_, ruser0_2_.modifierRef_type as modifie11_27_, ruser0_2_.modifyChannel as modifyC12_27_, ruser0_2_.modifyTimestamp as modifyT13_27_, ruser0_2_.name_norm as name_no14_27_, ruser0_2_.name_orig as name_or15_27_, ruser0_2_.objectTypeClass as objectT16_27_, ruser0_2_.tenantRef_relation as tenantR17_27_, ruser0_2_.tenantRef_targetOid as tenantR18_27_, ruser0_2_.tenantRef_type as tenantR19_27_, ruser0_2_.version as version20_27_, ruser0_1_.administrativeStatus as administ1_53_, ruser0_1_.archiveTimestamp as archiveT2_53_, ruser0_1_.disableReason as disableR3_53_, ruser0_1_.disableTimestamp as disableT4_53_, ruser0_1_.effectiveStatus as effectiv5_53_, ruser0_1_.enableTimestamp as enableTi6_53_, ruser0_1_.validFrom as validFro7_53_, ruser0_1_.validTo as validTo8_53_, ruser0_1_.validityChangeTimestamp as validity9_53_, ruser0_1_.validityStatus as validit10_53_, ruser0_1_.costCenter as costCen11_53_, ruser0_1_.emailAddress as emailAd12_53_, ruser0_1_.hasPhoto as hasPhot13_53_, ruser0_1_.locale as locale14_53_, ruser0_1_.locality_norm as localit15_53_, ruser0_1_.locality_orig as localit16_53_, ruser0_1_.preferredLanguage as preferr17_53_, ruser0_1_.telephoneNumber as telepho18_53_, ruser0_1_.timezone as timezon19_53_, ruser0_.additionalName_norm as addition1_73_, ruser0_.additionalName_orig as addition2_73_, ruser0_.employeeNumber as employee3_73_, ruser0_.familyName_norm as familyNa4_73_, ruser0_.familyName_orig as familyNa5_73_, ruser0_.fullName_norm as fullName6_73_, ruser0_.fullName_orig as fullName7_73_, ruser0_.givenName_norm as givenNam8_73_, ruser0_.givenName_orig as givenNam9_73_, ruser0_.honorificPrefix_norm as honorif10_73_, ruser0_.honorificPrefix_orig as honorif11_73_, ruser0_.honorificSuffix_norm as honorif12_73_, ruser0_.honorificSuffix_orig as honorif13_73_, ruser0_.name_norm as name_no14_73_, ruser0_.name_orig as name_or15_73_, ruser0_.nickName_norm as nickNam16_73_, ruser0_.nickName_orig as nickNam17_73_, ruser0_.title_norm as title_n18_73_, ruser0_.title_orig as title_o19_73_ from m_user ruser0_ inner join m_focus ruser0_1_ on ruser0_.oid=ruser0_1_.oid inner join m_object ruser0_2_ on ruser0_.oid=ruser0_2_.oid where ruser0_.oid=?
         - select assignment0_.owner_oid as owner_oi2_5_0_, assignment0_.id as id1_5_0_, assignment0_.id as id1_5_1_, assignment0_.owner_oid as owner_oi2_5_1_, assignment0_.administrativeStatus as administ3_5_1_, assignment0_.archiveTimestamp as archiveT4_5_1_, assignment0_.disableReason as disableR5_5_1_, assignment0_.disableTimestamp as disableT6_5_1_, assignment0_.effectiveStatus as effectiv7_5_1_, assignment0_.enableTimestamp as enableTi8_5_1_, assignment0_.validFrom as validFro9_5_1_, assignment0_.validTo as validTo10_5_1_, assignment0_.validityChangeTimestamp as validit11_5_1_, assignment0_.validityStatus as validit12_5_1_, assignment0_.assignmentOwner as assignm13_5_1_, assignment0_.createChannel as createC14_5_1_, assignment0_.createTimestamp as createT15_5_1_, assignment0_.creatorRef_relation as creator16_5_1_, assignment0_.creatorRef_targetOid as creator17_5_1_, assignment0_.creatorRef_type as creator18_5_1_, assignment0_.extId as extId38_5_1_, assignment0_.extOid as extOid39_5_1_, assignment0_.lifecycleState as lifecyc19_5_1_, assignment0_.modifierRef_relation as modifie20_5_1_, assignment0_.modifierRef_targetOid as modifie21_5_1_, assignment0_.modifierRef_type as modifie22_5_1_, assignment0_.modifyChannel as modifyC23_5_1_, assignment0_.modifyTimestamp as modifyT24_5_1_, assignment0_.orderValue as orderVa25_5_1_, assignment0_.orgRef_relation as orgRef_26_5_1_, assignment0_.orgRef_targetOid as orgRef_27_5_1_, assignment0_.orgRef_type as orgRef_28_5_1_, assignment0_.resourceRef_relation as resourc29_5_1_, assignment0_.resourceRef_targetOid as resourc30_5_1_, assignment0_.resourceRef_type as resourc31_5_1_, assignment0_.targetRef_relation as targetR32_5_1_, assignment0_.targetRef_targetOid as targetR33_5_1_, assignment0_.targetRef_type as targetR34_5_1_, assignment0_.tenantRef_relation as tenantR35_5_1_, assignment0_.tenantRef_targetOid as tenantR36_5_1_, assignment0_.tenantRef_type as tenantR37_5_1_, rassignmen1_.owner_id as owner_id1_12_2_, rassignmen1_.owner_owner_oid as owner_ow2_12_2_, rassignmen1_.booleansCount as booleans3_12_2_, rassignmen1_.datesCount as datesCou4_12_2_, rassignmen1_.longsCount as longsCou5_12_2_, rassignmen1_.polysCount as polysCou6_12_2_, rassignmen1_.referencesCount as referenc7_12_2_, rassignmen1_.stringsCount as stringsC8_12_2_ from m_assignment assignment0_ left outer join m_assignment_extension rassignmen1_ on assignment0_.extId=rassignmen1_.owner_id and assignment0_.extOid=rassignmen1_.owner_owner_oid where assignment0_.owner_oid=?
         - select modifyappr0_.owner_id as owner_id1_14_0_, modifyappr0_.owner_owner_oid as owner_ow2_14_0_, modifyappr0_.reference_type as referenc3_14_0_, modifyappr0_.relation as relation4_14_0_, modifyappr0_.targetOid as targetOi5_14_0_, modifyappr0_.owner_id as owner_id1_14_1_, modifyappr0_.owner_owner_oid as owner_ow2_14_1_, modifyappr0_.reference_type as referenc3_14_1_, modifyappr0_.relation as relation4_14_1_, modifyappr0_.targetOid as targetOi5_14_1_, modifyappr0_.targetType as targetTy6_14_1_ from m_assignment_reference modifyappr0_ where ( modifyappr0_.reference_type= 1) and modifyappr0_.owner_id=? and modifyappr0_.owner_owner_oid=?
         - insert into m_assignment_reference (targetType, owner_id, owner_owner_oid, reference_type, relation, targetOid) values (?, ?, ?, ?, ?, ?)
         - update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
         */
        if (isUsingH2()) {
            AssertJUnit.assertEquals(6, TestStatementInspector.getQueryCount());
        }

        try (EntityManager em = factory.createEntityManager()) {
            RAssignment a = em.find(RUser.class, userOid).getAssignments().iterator().next();
            AssertJUnit.assertEquals(2, a.getModifyApproverRef().size());

            //noinspection unchecked
            assertReferences((Collection) a.getModifyApproverRef(),
                    createRepoRef(UserType.COMPLEX_TYPE, "555"),
                    createRepoRef(UserType.COMPLEX_TYPE, "6542"));
        }
    }

    @Test
    public void test222DeleteAssignmentModifyApproverRef() throws Exception {
        OperationResult result = new OperationResult("test222DeleteAssignmentModifyApproverRef");

        List<ItemDelta<?, ?>> modifications = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT, 2, AssignmentType.F_METADATA, MetadataType.F_MODIFY_APPROVER_REF)
                .delete(createRef(UserType.COMPLEX_TYPE, "6542"))
                .asItemDeltas();

        TestStatementInspector.start();
        repositoryService.modifyObject(UserType.class, userOid, modifications, result);
        TestStatementInspector.dump();

        /*
         - select oid from m_object where oid = ? for update
         - select ruser0_.oid as oid1_27_, ruser0_2_.createChannel as createCh2_27_, ruser0_2_.createTimestamp as createTi3_27_, ruser0_2_.creatorRef_relation as creatorR4_27_, ruser0_2_.creatorRef_targetOid as creatorR5_27_, ruser0_2_.creatorRef_type as creatorR6_27_, ruser0_2_.fullObject as fullObje7_27_, ruser0_2_.lifecycleState as lifecycl8_27_, ruser0_2_.modifierRef_relation as modifier9_27_, ruser0_2_.modifierRef_targetOid as modifie10_27_, ruser0_2_.modifierRef_type as modifie11_27_, ruser0_2_.modifyChannel as modifyC12_27_, ruser0_2_.modifyTimestamp as modifyT13_27_, ruser0_2_.name_norm as name_no14_27_, ruser0_2_.name_orig as name_or15_27_, ruser0_2_.objectTypeClass as objectT16_27_, ruser0_2_.tenantRef_relation as tenantR17_27_, ruser0_2_.tenantRef_targetOid as tenantR18_27_, ruser0_2_.tenantRef_type as tenantR19_27_, ruser0_2_.version as version20_27_, ruser0_1_.administrativeStatus as administ1_53_, ruser0_1_.archiveTimestamp as archiveT2_53_, ruser0_1_.disableReason as disableR3_53_, ruser0_1_.disableTimestamp as disableT4_53_, ruser0_1_.effectiveStatus as effectiv5_53_, ruser0_1_.enableTimestamp as enableTi6_53_, ruser0_1_.validFrom as validFro7_53_, ruser0_1_.validTo as validTo8_53_, ruser0_1_.validityChangeTimestamp as validity9_53_, ruser0_1_.validityStatus as validit10_53_, ruser0_1_.costCenter as costCen11_53_, ruser0_1_.emailAddress as emailAd12_53_, ruser0_1_.hasPhoto as hasPhot13_53_, ruser0_1_.locale as locale14_53_, ruser0_1_.locality_norm as localit15_53_, ruser0_1_.locality_orig as localit16_53_, ruser0_1_.preferredLanguage as preferr17_53_, ruser0_1_.telephoneNumber as telepho18_53_, ruser0_1_.timezone as timezon19_53_, ruser0_.additionalName_norm as addition1_73_, ruser0_.additionalName_orig as addition2_73_, ruser0_.employeeNumber as employee3_73_, ruser0_.familyName_norm as familyNa4_73_, ruser0_.familyName_orig as familyNa5_73_, ruser0_.fullName_norm as fullName6_73_, ruser0_.fullName_orig as fullName7_73_, ruser0_.givenName_norm as givenNam8_73_, ruser0_.givenName_orig as givenNam9_73_, ruser0_.honorificPrefix_norm as honorif10_73_, ruser0_.honorificPrefix_orig as honorif11_73_, ruser0_.honorificSuffix_norm as honorif12_73_, ruser0_.honorificSuffix_orig as honorif13_73_, ruser0_.name_norm as name_no14_73_, ruser0_.name_orig as name_or15_73_, ruser0_.nickName_norm as nickNam16_73_, ruser0_.nickName_orig as nickNam17_73_, ruser0_.title_norm as title_n18_73_, ruser0_.title_orig as title_o19_73_ from m_user ruser0_ inner join m_focus ruser0_1_ on ruser0_.oid=ruser0_1_.oid inner join m_object ruser0_2_ on ruser0_.oid=ruser0_2_.oid where ruser0_.oid=?
         - select assignment0_.owner_oid as owner_oi2_5_0_, assignment0_.id as id1_5_0_, assignment0_.id as id1_5_1_, assignment0_.owner_oid as owner_oi2_5_1_, assignment0_.administrativeStatus as administ3_5_1_, assignment0_.archiveTimestamp as archiveT4_5_1_, assignment0_.disableReason as disableR5_5_1_, assignment0_.disableTimestamp as disableT6_5_1_, assignment0_.effectiveStatus as effectiv7_5_1_, assignment0_.enableTimestamp as enableTi8_5_1_, assignment0_.validFrom as validFro9_5_1_, assignment0_.validTo as validTo10_5_1_, assignment0_.validityChangeTimestamp as validit11_5_1_, assignment0_.validityStatus as validit12_5_1_, assignment0_.assignmentOwner as assignm13_5_1_, assignment0_.createChannel as createC14_5_1_, assignment0_.createTimestamp as createT15_5_1_, assignment0_.creatorRef_relation as creator16_5_1_, assignment0_.creatorRef_targetOid as creator17_5_1_, assignment0_.creatorRef_type as creator18_5_1_, assignment0_.extId as extId38_5_1_, assignment0_.extOid as extOid39_5_1_, assignment0_.lifecycleState as lifecyc19_5_1_, assignment0_.modifierRef_relation as modifie20_5_1_, assignment0_.modifierRef_targetOid as modifie21_5_1_, assignment0_.modifierRef_type as modifie22_5_1_, assignment0_.modifyChannel as modifyC23_5_1_, assignment0_.modifyTimestamp as modifyT24_5_1_, assignment0_.orderValue as orderVa25_5_1_, assignment0_.orgRef_relation as orgRef_26_5_1_, assignment0_.orgRef_targetOid as orgRef_27_5_1_, assignment0_.orgRef_type as orgRef_28_5_1_, assignment0_.resourceRef_relation as resourc29_5_1_, assignment0_.resourceRef_targetOid as resourc30_5_1_, assignment0_.resourceRef_type as resourc31_5_1_, assignment0_.targetRef_relation as targetR32_5_1_, assignment0_.targetRef_targetOid as targetR33_5_1_, assignment0_.targetRef_type as targetR34_5_1_, assignment0_.tenantRef_relation as tenantR35_5_1_, assignment0_.tenantRef_targetOid as tenantR36_5_1_, assignment0_.tenantRef_type as tenantR37_5_1_, rassignmen1_.owner_id as owner_id1_12_2_, rassignmen1_.owner_owner_oid as owner_ow2_12_2_, rassignmen1_.booleansCount as booleans3_12_2_, rassignmen1_.datesCount as datesCou4_12_2_, rassignmen1_.longsCount as longsCou5_12_2_, rassignmen1_.polysCount as polysCou6_12_2_, rassignmen1_.referencesCount as referenc7_12_2_, rassignmen1_.stringsCount as stringsC8_12_2_ from m_assignment assignment0_ left outer join m_assignment_extension rassignmen1_ on assignment0_.extId=rassignmen1_.owner_id and assignment0_.extOid=rassignmen1_.owner_owner_oid where assignment0_.owner_oid=?
         - select modifyappr0_.owner_id as owner_id1_14_0_, modifyappr0_.owner_owner_oid as owner_ow2_14_0_, modifyappr0_.reference_type as referenc3_14_0_, modifyappr0_.relation as relation4_14_0_, modifyappr0_.targetOid as targetOi5_14_0_, modifyappr0_.owner_id as owner_id1_14_1_, modifyappr0_.owner_owner_oid as owner_ow2_14_1_, modifyappr0_.reference_type as referenc3_14_1_, modifyappr0_.relation as relation4_14_1_, modifyappr0_.targetOid as targetOi5_14_1_, modifyappr0_.targetType as targetTy6_14_1_ from m_assignment_reference modifyappr0_ where ( modifyappr0_.reference_type= 1) and modifyappr0_.owner_id=? and modifyappr0_.owner_owner_oid=?
         - update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
         - delete from m_assignment_reference where owner_id=? and owner_owner_oid=? and reference_type=? and relation=? and targetOid=?
         */
        if (isUsingH2()) {
            AssertJUnit.assertEquals(6, TestStatementInspector.getQueryCount());
        }

        try (EntityManager em = factory.createEntityManager()) {
            RAssignment a = em.find(RUser.class, userOid).getAssignments().iterator().next();
            AssertJUnit.assertEquals(1, a.getModifyApproverRef().size());

            //noinspection unchecked
            assertReferences((Collection) a.getModifyApproverRef(),
                    createRepoRef(UserType.COMPLEX_TYPE, "555"));
        }
    }

    @Test
    public void test230ReplaceWholeAssignmentMetadata() throws Exception {
        OperationResult result = new OperationResult("test230ReplaceWholeAssignmentMetadata");

        List<ItemDelta<?, ?>> modifications = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT, 2, AssignmentType.F_METADATA)
                .replace(new MetadataType(prismContext)
                        .createChannel("ch1.2")
                        .modifierRef(createRef(UserType.COMPLEX_TYPE, "44.2"))
                        .createApproverRef(createRef(UserType.COMPLEX_TYPE, "55.2"))
                        .createApproverRef(createRef(UserType.COMPLEX_TYPE, "77.2"))
                        .modifyApproverRef(createRef(UserType.COMPLEX_TYPE, "55.2"))
                        .modifyApproverRef(createRef(UserType.COMPLEX_TYPE, "66.2"))
                )
                .asItemDeltas();

        TestStatementInspector.start();
        repositoryService.modifyObject(UserType.class, userOid, modifications, result);
        TestStatementInspector.dump();

        /*
         - select oid from m_object where oid = ? for update
         - select ruser0_.oid as oid1_27_, ruser0_2_.createChannel as createCh2_27_, ruser0_2_.createTimestamp as createTi3_27_, ruser0_2_.creatorRef_relation as creatorR4_27_, ruser0_2_.creatorRef_targetOid as creatorR5_27_, ruser0_2_.creatorRef_type as creatorR6_27_, ruser0_2_.fullObject as fullObje7_27_, ruser0_2_.lifecycleState as lifecycl8_27_, ruser0_2_.modifierRef_relation as modifier9_27_, ruser0_2_.modifierRef_targetOid as modifie10_27_, ruser0_2_.modifierRef_type as modifie11_27_, ruser0_2_.modifyChannel as modifyC12_27_, ruser0_2_.modifyTimestamp as modifyT13_27_, ruser0_2_.name_norm as name_no14_27_, ruser0_2_.name_orig as name_or15_27_, ruser0_2_.objectTypeClass as objectT16_27_, ruser0_2_.tenantRef_relation as tenantR17_27_, ruser0_2_.tenantRef_targetOid as tenantR18_27_, ruser0_2_.tenantRef_type as tenantR19_27_, ruser0_2_.version as version20_27_, ruser0_1_.administrativeStatus as administ1_53_, ruser0_1_.archiveTimestamp as archiveT2_53_, ruser0_1_.disableReason as disableR3_53_, ruser0_1_.disableTimestamp as disableT4_53_, ruser0_1_.effectiveStatus as effectiv5_53_, ruser0_1_.enableTimestamp as enableTi6_53_, ruser0_1_.validFrom as validFro7_53_, ruser0_1_.validTo as validTo8_53_, ruser0_1_.validityChangeTimestamp as validity9_53_, ruser0_1_.validityStatus as validit10_53_, ruser0_1_.costCenter as costCen11_53_, ruser0_1_.emailAddress as emailAd12_53_, ruser0_1_.hasPhoto as hasPhot13_53_, ruser0_1_.locale as locale14_53_, ruser0_1_.locality_norm as localit15_53_, ruser0_1_.locality_orig as localit16_53_, ruser0_1_.preferredLanguage as preferr17_53_, ruser0_1_.telephoneNumber as telepho18_53_, ruser0_1_.timezone as timezon19_53_, ruser0_.additionalName_norm as addition1_73_, ruser0_.additionalName_orig as addition2_73_, ruser0_.employeeNumber as employee3_73_, ruser0_.familyName_norm as familyNa4_73_, ruser0_.familyName_orig as familyNa5_73_, ruser0_.fullName_norm as fullName6_73_, ruser0_.fullName_orig as fullName7_73_, ruser0_.givenName_norm as givenNam8_73_, ruser0_.givenName_orig as givenNam9_73_, ruser0_.honorificPrefix_norm as honorif10_73_, ruser0_.honorificPrefix_orig as honorif11_73_, ruser0_.honorificSuffix_norm as honorif12_73_, ruser0_.honorificSuffix_orig as honorif13_73_, ruser0_.name_norm as name_no14_73_, ruser0_.name_orig as name_or15_73_, ruser0_.nickName_norm as nickNam16_73_, ruser0_.nickName_orig as nickNam17_73_, ruser0_.title_norm as title_n18_73_, ruser0_.title_orig as title_o19_73_ from m_user ruser0_ inner join m_focus ruser0_1_ on ruser0_.oid=ruser0_1_.oid inner join m_object ruser0_2_ on ruser0_.oid=ruser0_2_.oid where ruser0_.oid=?
         - select assignment0_.owner_oid as owner_oi2_5_0_, assignment0_.id as id1_5_0_, assignment0_.id as id1_5_1_, assignment0_.owner_oid as owner_oi2_5_1_, assignment0_.administrativeStatus as administ3_5_1_, assignment0_.archiveTimestamp as archiveT4_5_1_, assignment0_.disableReason as disableR5_5_1_, assignment0_.disableTimestamp as disableT6_5_1_, assignment0_.effectiveStatus as effectiv7_5_1_, assignment0_.enableTimestamp as enableTi8_5_1_, assignment0_.validFrom as validFro9_5_1_, assignment0_.validTo as validTo10_5_1_, assignment0_.validityChangeTimestamp as validit11_5_1_, assignment0_.validityStatus as validit12_5_1_, assignment0_.assignmentOwner as assignm13_5_1_, assignment0_.createChannel as createC14_5_1_, assignment0_.createTimestamp as createT15_5_1_, assignment0_.creatorRef_relation as creator16_5_1_, assignment0_.creatorRef_targetOid as creator17_5_1_, assignment0_.creatorRef_type as creator18_5_1_, assignment0_.extId as extId38_5_1_, assignment0_.extOid as extOid39_5_1_, assignment0_.lifecycleState as lifecyc19_5_1_, assignment0_.modifierRef_relation as modifie20_5_1_, assignment0_.modifierRef_targetOid as modifie21_5_1_, assignment0_.modifierRef_type as modifie22_5_1_, assignment0_.modifyChannel as modifyC23_5_1_, assignment0_.modifyTimestamp as modifyT24_5_1_, assignment0_.orderValue as orderVa25_5_1_, assignment0_.orgRef_relation as orgRef_26_5_1_, assignment0_.orgRef_targetOid as orgRef_27_5_1_, assignment0_.orgRef_type as orgRef_28_5_1_, assignment0_.resourceRef_relation as resourc29_5_1_, assignment0_.resourceRef_targetOid as resourc30_5_1_, assignment0_.resourceRef_type as resourc31_5_1_, assignment0_.targetRef_relation as targetR32_5_1_, assignment0_.targetRef_targetOid as targetR33_5_1_, assignment0_.targetRef_type as targetR34_5_1_, assignment0_.tenantRef_relation as tenantR35_5_1_, assignment0_.tenantRef_targetOid as tenantR36_5_1_, assignment0_.tenantRef_type as tenantR37_5_1_, rassignmen1_.owner_id as owner_id1_12_2_, rassignmen1_.owner_owner_oid as owner_ow2_12_2_, rassignmen1_.booleansCount as booleans3_12_2_, rassignmen1_.datesCount as datesCou4_12_2_, rassignmen1_.longsCount as longsCou5_12_2_, rassignmen1_.polysCount as polysCou6_12_2_, rassignmen1_.referencesCount as referenc7_12_2_, rassignmen1_.stringsCount as stringsC8_12_2_ from m_assignment assignment0_ left outer join m_assignment_extension rassignmen1_ on assignment0_.extId=rassignmen1_.owner_id and assignment0_.extOid=rassignmen1_.owner_owner_oid where assignment0_.owner_oid=?
         - select createappr0_.owner_id as owner_id1_14_0_, createappr0_.owner_owner_oid as owner_ow2_14_0_, createappr0_.reference_type as referenc3_14_0_, createappr0_.relation as relation4_14_0_, createappr0_.targetOid as targetOi5_14_0_, createappr0_.owner_id as owner_id1_14_1_, createappr0_.owner_owner_oid as owner_ow2_14_1_, createappr0_.reference_type as referenc3_14_1_, createappr0_.relation as relation4_14_1_, createappr0_.targetOid as targetOi5_14_1_, createappr0_.targetType as targetTy6_14_1_ from m_assignment_reference createappr0_ where ( createappr0_.reference_type= 0) and createappr0_.owner_id=? and createappr0_.owner_owner_oid=?
         - select modifyappr0_.owner_id as owner_id1_14_0_, modifyappr0_.owner_owner_oid as owner_ow2_14_0_, modifyappr0_.reference_type as referenc3_14_0_, modifyappr0_.relation as relation4_14_0_, modifyappr0_.targetOid as targetOi5_14_0_, modifyappr0_.owner_id as owner_id1_14_1_, modifyappr0_.owner_owner_oid as owner_ow2_14_1_, modifyappr0_.reference_type as referenc3_14_1_, modifyappr0_.relation as relation4_14_1_, modifyappr0_.targetOid as targetOi5_14_1_, modifyappr0_.targetType as targetTy6_14_1_ from m_assignment_reference modifyappr0_ where ( modifyappr0_.reference_type= 1) and modifyappr0_.owner_id=? and modifyappr0_.owner_owner_oid=?
         - select rassignmen_.owner_id, rassignmen_.owner_owner_oid, rassignmen_.reference_type, rassignmen_.relation, rassignmen_.targetOid, rassignmen_.targetType as targetTy6_14_ from m_assignment_reference rassignmen_ where rassignmen_.owner_id=? and rassignmen_.owner_owner_oid=? and rassignmen_.reference_type=? and rassignmen_.relation=? and rassignmen_.targetOid=?
         - select rassignmen_.owner_id, rassignmen_.owner_owner_oid, rassignmen_.reference_type, rassignmen_.relation, rassignmen_.targetOid, rassignmen_.targetType as targetTy6_14_ from m_assignment_reference rassignmen_ where rassignmen_.owner_id=? and rassignmen_.owner_owner_oid=? and rassignmen_.reference_type=? and rassignmen_.relation=? and rassignmen_.targetOid=?
         - select rassignmen_.owner_id, rassignmen_.owner_owner_oid, rassignmen_.reference_type, rassignmen_.relation, rassignmen_.targetOid, rassignmen_.targetType as targetTy6_14_ from m_assignment_reference rassignmen_ where rassignmen_.owner_id=? and rassignmen_.owner_owner_oid=? and rassignmen_.reference_type=? and rassignmen_.relation=? and rassignmen_.targetOid=?
         - select rassignmen_.owner_id, rassignmen_.owner_owner_oid, rassignmen_.reference_type, rassignmen_.relation, rassignmen_.targetOid, rassignmen_.targetType as targetTy6_14_ from m_assignment_reference rassignmen_ where rassignmen_.owner_id=? and rassignmen_.owner_owner_oid=? and rassignmen_.reference_type=? and rassignmen_.relation=? and rassignmen_.targetOid=?
         - insert into m_assignment_reference (targetType, owner_id, owner_owner_oid, reference_type, relation, targetOid) values (?, ?, ?, ?, ?, ?)
         - update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
         - update m_assignment set administrativeStatus=?, archiveTimestamp=?, disableReason=?, disableTimestamp=?, effectiveStatus=?, enableTimestamp=?, validFrom=?, validTo=?, validityChangeTimestamp=?, validityStatus=?, assignmentOwner=?, createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, extId=?, extOid=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, orderValue=?, orgRef_relation=?, orgRef_targetOid=?, orgRef_type=?, resourceRef_relation=?, resourceRef_targetOid=?, resourceRef_type=?, targetRef_relation=?, targetRef_targetOid=?, targetRef_type=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=? where id=? and owner_oid=?
         - delete from m_assignment_reference where owner_id=? and owner_owner_oid=? and reference_type=? and relation=? and targetOid=?
         */

        if (isUsingH2()) {
            AssertJUnit.assertEquals(9, TestStatementInspector.getQueryCount());
        }

        try (EntityManager em = factory.createEntityManager()) {
            RAssignment u = em.find(RUser.class, userOid).getAssignments().iterator().next();
            AssertJUnit.assertEquals("ch1.2", u.getCreateChannel());
            AssertJUnit.assertEquals(2, u.getModifyApproverRef().size());
            AssertJUnit.assertEquals(2, u.getCreateApproverRef().size());

            assertEquals(u.getModifierRef(),
                    createEmbeddedRepoRef(UserType.COMPLEX_TYPE, "44.2"));
            //noinspection unchecked
            assertReferences((Collection) u.getCreateApproverRef(),
                    createRepoRef(UserType.COMPLEX_TYPE, "55.2"),
                    createRepoRef(UserType.COMPLEX_TYPE, "77.2"));
            //noinspection unchecked
            assertReferences((Collection) u.getModifyApproverRef(),
                    createRepoRef(UserType.COMPLEX_TYPE, "55.2"),
                    createRepoRef(UserType.COMPLEX_TYPE, "66.2"));
        }
    }

    @Test
    public void test232DeleteWholeAssignmentMetadata() throws Exception {
        OperationResult result = new OperationResult("test232DeleteWholeAssignmentMetadata");

        List<ItemDelta<?, ?>> modifications = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT, 2, AssignmentType.F_METADATA)
                .delete(new MetadataType(prismContext)
                        .createChannel("ch1.2")
                        .modifierRef(createRef(UserType.COMPLEX_TYPE, "44.2"))
                        .createApproverRef(createRef(UserType.COMPLEX_TYPE, "55.2"))
                        .createApproverRef(createRef(UserType.COMPLEX_TYPE, "77.2"))
                        .modifyApproverRef(createRef(UserType.COMPLEX_TYPE, "55.2"))
                        .modifyApproverRef(createRef(UserType.COMPLEX_TYPE, "66.2"))
                )
                .asItemDeltas();

        TestStatementInspector.start();
        repositoryService.modifyObject(UserType.class, userOid, modifications, result);
        TestStatementInspector.dump();

        /*
         - select oid from m_object where oid = ? for update
         - select ruser0_.oid as oid1_27_, ruser0_2_.createChannel as createCh2_27_, ruser0_2_.createTimestamp as createTi3_27_, ruser0_2_.creatorRef_relation as creatorR4_27_, ruser0_2_.creatorRef_targetOid as creatorR5_27_, ruser0_2_.creatorRef_type as creatorR6_27_, ruser0_2_.fullObject as fullObje7_27_, ruser0_2_.lifecycleState as lifecycl8_27_, ruser0_2_.modifierRef_relation as modifier9_27_, ruser0_2_.modifierRef_targetOid as modifie10_27_, ruser0_2_.modifierRef_type as modifie11_27_, ruser0_2_.modifyChannel as modifyC12_27_, ruser0_2_.modifyTimestamp as modifyT13_27_, ruser0_2_.name_norm as name_no14_27_, ruser0_2_.name_orig as name_or15_27_, ruser0_2_.objectTypeClass as objectT16_27_, ruser0_2_.tenantRef_relation as tenantR17_27_, ruser0_2_.tenantRef_targetOid as tenantR18_27_, ruser0_2_.tenantRef_type as tenantR19_27_, ruser0_2_.version as version20_27_, ruser0_1_.administrativeStatus as administ1_53_, ruser0_1_.archiveTimestamp as archiveT2_53_, ruser0_1_.disableReason as disableR3_53_, ruser0_1_.disableTimestamp as disableT4_53_, ruser0_1_.effectiveStatus as effectiv5_53_, ruser0_1_.enableTimestamp as enableTi6_53_, ruser0_1_.validFrom as validFro7_53_, ruser0_1_.validTo as validTo8_53_, ruser0_1_.validityChangeTimestamp as validity9_53_, ruser0_1_.validityStatus as validit10_53_, ruser0_1_.costCenter as costCen11_53_, ruser0_1_.emailAddress as emailAd12_53_, ruser0_1_.hasPhoto as hasPhot13_53_, ruser0_1_.locale as locale14_53_, ruser0_1_.locality_norm as localit15_53_, ruser0_1_.locality_orig as localit16_53_, ruser0_1_.preferredLanguage as preferr17_53_, ruser0_1_.telephoneNumber as telepho18_53_, ruser0_1_.timezone as timezon19_53_, ruser0_.additionalName_norm as addition1_73_, ruser0_.additionalName_orig as addition2_73_, ruser0_.employeeNumber as employee3_73_, ruser0_.familyName_norm as familyNa4_73_, ruser0_.familyName_orig as familyNa5_73_, ruser0_.fullName_norm as fullName6_73_, ruser0_.fullName_orig as fullName7_73_, ruser0_.givenName_norm as givenNam8_73_, ruser0_.givenName_orig as givenNam9_73_, ruser0_.honorificPrefix_norm as honorif10_73_, ruser0_.honorificPrefix_orig as honorif11_73_, ruser0_.honorificSuffix_norm as honorif12_73_, ruser0_.honorificSuffix_orig as honorif13_73_, ruser0_.name_norm as name_no14_73_, ruser0_.name_orig as name_or15_73_, ruser0_.nickName_norm as nickNam16_73_, ruser0_.nickName_orig as nickNam17_73_, ruser0_.title_norm as title_n18_73_, ruser0_.title_orig as title_o19_73_ from m_user ruser0_ inner join m_focus ruser0_1_ on ruser0_.oid=ruser0_1_.oid inner join m_object ruser0_2_ on ruser0_.oid=ruser0_2_.oid where ruser0_.oid=?
         - select assignment0_.owner_oid as owner_oi2_5_0_, assignment0_.id as id1_5_0_, assignment0_.id as id1_5_1_, assignment0_.owner_oid as owner_oi2_5_1_, assignment0_.administrativeStatus as administ3_5_1_, assignment0_.archiveTimestamp as archiveT4_5_1_, assignment0_.disableReason as disableR5_5_1_, assignment0_.disableTimestamp as disableT6_5_1_, assignment0_.effectiveStatus as effectiv7_5_1_, assignment0_.enableTimestamp as enableTi8_5_1_, assignment0_.validFrom as validFro9_5_1_, assignment0_.validTo as validTo10_5_1_, assignment0_.validityChangeTimestamp as validit11_5_1_, assignment0_.validityStatus as validit12_5_1_, assignment0_.assignmentOwner as assignm13_5_1_, assignment0_.createChannel as createC14_5_1_, assignment0_.createTimestamp as createT15_5_1_, assignment0_.creatorRef_relation as creator16_5_1_, assignment0_.creatorRef_targetOid as creator17_5_1_, assignment0_.creatorRef_type as creator18_5_1_, assignment0_.extId as extId38_5_1_, assignment0_.extOid as extOid39_5_1_, assignment0_.lifecycleState as lifecyc19_5_1_, assignment0_.modifierRef_relation as modifie20_5_1_, assignment0_.modifierRef_targetOid as modifie21_5_1_, assignment0_.modifierRef_type as modifie22_5_1_, assignment0_.modifyChannel as modifyC23_5_1_, assignment0_.modifyTimestamp as modifyT24_5_1_, assignment0_.orderValue as orderVa25_5_1_, assignment0_.orgRef_relation as orgRef_26_5_1_, assignment0_.orgRef_targetOid as orgRef_27_5_1_, assignment0_.orgRef_type as orgRef_28_5_1_, assignment0_.resourceRef_relation as resourc29_5_1_, assignment0_.resourceRef_targetOid as resourc30_5_1_, assignment0_.resourceRef_type as resourc31_5_1_, assignment0_.targetRef_relation as targetR32_5_1_, assignment0_.targetRef_targetOid as targetR33_5_1_, assignment0_.targetRef_type as targetR34_5_1_, assignment0_.tenantRef_relation as tenantR35_5_1_, assignment0_.tenantRef_targetOid as tenantR36_5_1_, assignment0_.tenantRef_type as tenantR37_5_1_, rassignmen1_.owner_id as owner_id1_12_2_, rassignmen1_.owner_owner_oid as owner_ow2_12_2_, rassignmen1_.booleansCount as booleans3_12_2_, rassignmen1_.datesCount as datesCou4_12_2_, rassignmen1_.longsCount as longsCou5_12_2_, rassignmen1_.polysCount as polysCou6_12_2_, rassignmen1_.referencesCount as referenc7_12_2_, rassignmen1_.stringsCount as stringsC8_12_2_ from m_assignment assignment0_ left outer join m_assignment_extension rassignmen1_ on assignment0_.extId=rassignmen1_.owner_id and assignment0_.extOid=rassignmen1_.owner_owner_oid where assignment0_.owner_oid=?
         - select createappr0_.owner_id as owner_id1_14_0_, createappr0_.owner_owner_oid as owner_ow2_14_0_, createappr0_.reference_type as referenc3_14_0_, createappr0_.relation as relation4_14_0_, createappr0_.targetOid as targetOi5_14_0_, createappr0_.owner_id as owner_id1_14_1_, createappr0_.owner_owner_oid as owner_ow2_14_1_, createappr0_.reference_type as referenc3_14_1_, createappr0_.relation as relation4_14_1_, createappr0_.targetOid as targetOi5_14_1_, createappr0_.targetType as targetTy6_14_1_ from m_assignment_reference createappr0_ where ( createappr0_.reference_type= 0) and createappr0_.owner_id=? and createappr0_.owner_owner_oid=?
         - select modifyappr0_.owner_id as owner_id1_14_0_, modifyappr0_.owner_owner_oid as owner_ow2_14_0_, modifyappr0_.reference_type as referenc3_14_0_, modifyappr0_.relation as relation4_14_0_, modifyappr0_.targetOid as targetOi5_14_0_, modifyappr0_.owner_id as owner_id1_14_1_, modifyappr0_.owner_owner_oid as owner_ow2_14_1_, modifyappr0_.reference_type as referenc3_14_1_, modifyappr0_.relation as relation4_14_1_, modifyappr0_.targetOid as targetOi5_14_1_, modifyappr0_.targetType as targetTy6_14_1_ from m_assignment_reference modifyappr0_ where ( modifyappr0_.reference_type= 1) and modifyappr0_.owner_id=? and modifyappr0_.owner_owner_oid=?
         - update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
         - update m_assignment set administrativeStatus=?, archiveTimestamp=?, disableReason=?, disableTimestamp=?, effectiveStatus=?, enableTimestamp=?, validFrom=?, validTo=?, validityChangeTimestamp=?, validityStatus=?, assignmentOwner=?, createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, extId=?, extOid=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, orderValue=?, orgRef_relation=?, orgRef_targetOid=?, orgRef_type=?, resourceRef_relation=?, resourceRef_targetOid=?, resourceRef_type=?, targetRef_relation=?, targetRef_targetOid=?, targetRef_type=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=? where id=? and owner_oid=?
         - delete from m_assignment_reference where owner_id=? and owner_owner_oid=? and reference_type=? and relation=? and targetOid=?
         */

        if (isUsingH2()) {
            AssertJUnit.assertEquals(8, TestStatementInspector.getQueryCount());
        }

        try (EntityManager em = factory.createEntityManager()) {
            RAssignment a = em.find(RUser.class, userOid).getAssignments().iterator().next();
            AssertJUnit.assertNull(a.getCreateChannel());
            AssertJUnit.assertNull(a.getModifierRef());
            AssertJUnit.assertEquals(0, a.getModifyApproverRef().size());
            AssertJUnit.assertEquals(0, a.getCreateApproverRef().size());
        }
    }

    @Test
    public void test235AddWholeAssignmentMetadata() throws Exception {
        OperationResult result = new OperationResult("test235AddWholeAssignmentMetadata");

        List<ItemDelta<?, ?>> modifications = prismContext.deltaFor(UserType.class)
                .item(UserType.F_ASSIGNMENT, 2, AssignmentType.F_METADATA)
                .add(new MetadataType(prismContext)
                        .createChannel("ch1.3")
                        .modifierRef(createRef(UserType.COMPLEX_TYPE, "44.3"))
                        .createApproverRef(createRef(UserType.COMPLEX_TYPE, "55.3"))
                        .createApproverRef(createRef(UserType.COMPLEX_TYPE, "77.3"))
                        .modifyApproverRef(createRef(UserType.COMPLEX_TYPE, "55.3"))
                        .modifyApproverRef(createRef(UserType.COMPLEX_TYPE, "66.3"))
                )
                .asItemDeltas();

        TestStatementInspector.start();
        repositoryService.modifyObject(UserType.class, userOid, modifications, result);
        TestStatementInspector.dump();

        /*
         - select oid from m_object where oid = ? for update
         - select ruser0_.oid as oid1_27_, ruser0_2_.createChannel as createCh2_27_, ruser0_2_.createTimestamp as createTi3_27_, ruser0_2_.creatorRef_relation as creatorR4_27_, ruser0_2_.creatorRef_targetOid as creatorR5_27_, ruser0_2_.creatorRef_type as creatorR6_27_, ruser0_2_.fullObject as fullObje7_27_, ruser0_2_.lifecycleState as lifecycl8_27_, ruser0_2_.modifierRef_relation as modifier9_27_, ruser0_2_.modifierRef_targetOid as modifie10_27_, ruser0_2_.modifierRef_type as modifie11_27_, ruser0_2_.modifyChannel as modifyC12_27_, ruser0_2_.modifyTimestamp as modifyT13_27_, ruser0_2_.name_norm as name_no14_27_, ruser0_2_.name_orig as name_or15_27_, ruser0_2_.objectTypeClass as objectT16_27_, ruser0_2_.tenantRef_relation as tenantR17_27_, ruser0_2_.tenantRef_targetOid as tenantR18_27_, ruser0_2_.tenantRef_type as tenantR19_27_, ruser0_2_.version as version20_27_, ruser0_1_.administrativeStatus as administ1_53_, ruser0_1_.archiveTimestamp as archiveT2_53_, ruser0_1_.disableReason as disableR3_53_, ruser0_1_.disableTimestamp as disableT4_53_, ruser0_1_.effectiveStatus as effectiv5_53_, ruser0_1_.enableTimestamp as enableTi6_53_, ruser0_1_.validFrom as validFro7_53_, ruser0_1_.validTo as validTo8_53_, ruser0_1_.validityChangeTimestamp as validity9_53_, ruser0_1_.validityStatus as validit10_53_, ruser0_1_.costCenter as costCen11_53_, ruser0_1_.emailAddress as emailAd12_53_, ruser0_1_.hasPhoto as hasPhot13_53_, ruser0_1_.locale as locale14_53_, ruser0_1_.locality_norm as localit15_53_, ruser0_1_.locality_orig as localit16_53_, ruser0_1_.preferredLanguage as preferr17_53_, ruser0_1_.telephoneNumber as telepho18_53_, ruser0_1_.timezone as timezon19_53_, ruser0_.additionalName_norm as addition1_73_, ruser0_.additionalName_orig as addition2_73_, ruser0_.employeeNumber as employee3_73_, ruser0_.familyName_norm as familyNa4_73_, ruser0_.familyName_orig as familyNa5_73_, ruser0_.fullName_norm as fullName6_73_, ruser0_.fullName_orig as fullName7_73_, ruser0_.givenName_norm as givenNam8_73_, ruser0_.givenName_orig as givenNam9_73_, ruser0_.honorificPrefix_norm as honorif10_73_, ruser0_.honorificPrefix_orig as honorif11_73_, ruser0_.honorificSuffix_norm as honorif12_73_, ruser0_.honorificSuffix_orig as honorif13_73_, ruser0_.name_norm as name_no14_73_, ruser0_.name_orig as name_or15_73_, ruser0_.nickName_norm as nickNam16_73_, ruser0_.nickName_orig as nickNam17_73_, ruser0_.title_norm as title_n18_73_, ruser0_.title_orig as title_o19_73_ from m_user ruser0_ inner join m_focus ruser0_1_ on ruser0_.oid=ruser0_1_.oid inner join m_object ruser0_2_ on ruser0_.oid=ruser0_2_.oid where ruser0_.oid=?
         - select assignment0_.owner_oid as owner_oi2_5_0_, assignment0_.id as id1_5_0_, assignment0_.id as id1_5_1_, assignment0_.owner_oid as owner_oi2_5_1_, assignment0_.administrativeStatus as administ3_5_1_, assignment0_.archiveTimestamp as archiveT4_5_1_, assignment0_.disableReason as disableR5_5_1_, assignment0_.disableTimestamp as disableT6_5_1_, assignment0_.effectiveStatus as effectiv7_5_1_, assignment0_.enableTimestamp as enableTi8_5_1_, assignment0_.validFrom as validFro9_5_1_, assignment0_.validTo as validTo10_5_1_, assignment0_.validityChangeTimestamp as validit11_5_1_, assignment0_.validityStatus as validit12_5_1_, assignment0_.assignmentOwner as assignm13_5_1_, assignment0_.createChannel as createC14_5_1_, assignment0_.createTimestamp as createT15_5_1_, assignment0_.creatorRef_relation as creator16_5_1_, assignment0_.creatorRef_targetOid as creator17_5_1_, assignment0_.creatorRef_type as creator18_5_1_, assignment0_.extId as extId38_5_1_, assignment0_.extOid as extOid39_5_1_, assignment0_.lifecycleState as lifecyc19_5_1_, assignment0_.modifierRef_relation as modifie20_5_1_, assignment0_.modifierRef_targetOid as modifie21_5_1_, assignment0_.modifierRef_type as modifie22_5_1_, assignment0_.modifyChannel as modifyC23_5_1_, assignment0_.modifyTimestamp as modifyT24_5_1_, assignment0_.orderValue as orderVa25_5_1_, assignment0_.orgRef_relation as orgRef_26_5_1_, assignment0_.orgRef_targetOid as orgRef_27_5_1_, assignment0_.orgRef_type as orgRef_28_5_1_, assignment0_.resourceRef_relation as resourc29_5_1_, assignment0_.resourceRef_targetOid as resourc30_5_1_, assignment0_.resourceRef_type as resourc31_5_1_, assignment0_.targetRef_relation as targetR32_5_1_, assignment0_.targetRef_targetOid as targetR33_5_1_, assignment0_.targetRef_type as targetR34_5_1_, assignment0_.tenantRef_relation as tenantR35_5_1_, assignment0_.tenantRef_targetOid as tenantR36_5_1_, assignment0_.tenantRef_type as tenantR37_5_1_, rassignmen1_.owner_id as owner_id1_12_2_, rassignmen1_.owner_owner_oid as owner_ow2_12_2_, rassignmen1_.booleansCount as booleans3_12_2_, rassignmen1_.datesCount as datesCou4_12_2_, rassignmen1_.longsCount as longsCou5_12_2_, rassignmen1_.polysCount as polysCou6_12_2_, rassignmen1_.referencesCount as referenc7_12_2_, rassignmen1_.stringsCount as stringsC8_12_2_ from m_assignment assignment0_ left outer join m_assignment_extension rassignmen1_ on assignment0_.extId=rassignmen1_.owner_id and assignment0_.extOid=rassignmen1_.owner_owner_oid where assignment0_.owner_oid=?
         - select createappr0_.owner_id as owner_id1_14_0_, createappr0_.owner_owner_oid as owner_ow2_14_0_, createappr0_.reference_type as referenc3_14_0_, createappr0_.relation as relation4_14_0_, createappr0_.targetOid as targetOi5_14_0_, createappr0_.owner_id as owner_id1_14_1_, createappr0_.owner_owner_oid as owner_ow2_14_1_, createappr0_.reference_type as referenc3_14_1_, createappr0_.relation as relation4_14_1_, createappr0_.targetOid as targetOi5_14_1_, createappr0_.targetType as targetTy6_14_1_ from m_assignment_reference createappr0_ where ( createappr0_.reference_type= 0) and createappr0_.owner_id=? and createappr0_.owner_owner_oid=?
         - select modifyappr0_.owner_id as owner_id1_14_0_, modifyappr0_.owner_owner_oid as owner_ow2_14_0_, modifyappr0_.reference_type as referenc3_14_0_, modifyappr0_.relation as relation4_14_0_, modifyappr0_.targetOid as targetOi5_14_0_, modifyappr0_.owner_id as owner_id1_14_1_, modifyappr0_.owner_owner_oid as owner_ow2_14_1_, modifyappr0_.reference_type as referenc3_14_1_, modifyappr0_.relation as relation4_14_1_, modifyappr0_.targetOid as targetOi5_14_1_, modifyappr0_.targetType as targetTy6_14_1_ from m_assignment_reference modifyappr0_ where ( modifyappr0_.reference_type= 1) and modifyappr0_.owner_id=? and modifyappr0_.owner_owner_oid=?
         - select rassignmen_.owner_id, rassignmen_.owner_owner_oid, rassignmen_.reference_type, rassignmen_.relation, rassignmen_.targetOid, rassignmen_.targetType as targetTy6_14_ from m_assignment_reference rassignmen_ where rassignmen_.owner_id=? and rassignmen_.owner_owner_oid=? and rassignmen_.reference_type=? and rassignmen_.relation=? and rassignmen_.targetOid=?
         - select rassignmen_.owner_id, rassignmen_.owner_owner_oid, rassignmen_.reference_type, rassignmen_.relation, rassignmen_.targetOid, rassignmen_.targetType as targetTy6_14_ from m_assignment_reference rassignmen_ where rassignmen_.owner_id=? and rassignmen_.owner_owner_oid=? and rassignmen_.reference_type=? and rassignmen_.relation=? and rassignmen_.targetOid=?
         - select rassignmen_.owner_id, rassignmen_.owner_owner_oid, rassignmen_.reference_type, rassignmen_.relation, rassignmen_.targetOid, rassignmen_.targetType as targetTy6_14_ from m_assignment_reference rassignmen_ where rassignmen_.owner_id=? and rassignmen_.owner_owner_oid=? and rassignmen_.reference_type=? and rassignmen_.relation=? and rassignmen_.targetOid=?
         - select rassignmen_.owner_id, rassignmen_.owner_owner_oid, rassignmen_.reference_type, rassignmen_.relation, rassignmen_.targetOid, rassignmen_.targetType as targetTy6_14_ from m_assignment_reference rassignmen_ where rassignmen_.owner_id=? and rassignmen_.owner_owner_oid=? and rassignmen_.reference_type=? and rassignmen_.relation=? and rassignmen_.targetOid=?
         - insert into m_assignment_reference (targetType, owner_id, owner_owner_oid, reference_type, relation, targetOid) values (?, ?, ?, ?, ?, ?)
         - update m_object set createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, fullObject=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, name_norm=?, name_orig=?, objectTypeClass=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=?, version=? where oid=?
         - update m_assignment set administrativeStatus=?, archiveTimestamp=?, disableReason=?, disableTimestamp=?, effectiveStatus=?, enableTimestamp=?, validFrom=?, validTo=?, validityChangeTimestamp=?, validityStatus=?, assignmentOwner=?, createChannel=?, createTimestamp=?, creatorRef_relation=?, creatorRef_targetOid=?, creatorRef_type=?, extId=?, extOid=?, lifecycleState=?, modifierRef_relation=?, modifierRef_targetOid=?, modifierRef_type=?, modifyChannel=?, modifyTimestamp=?, orderValue=?, orgRef_relation=?, orgRef_targetOid=?, orgRef_type=?, resourceRef_relation=?, resourceRef_targetOid=?, resourceRef_type=?, targetRef_relation=?, targetRef_targetOid=?, targetRef_type=?, tenantRef_relation=?, tenantRef_targetOid=?, tenantRef_type=? where id=? and owner_oid=?
         */
        if (isUsingH2()) {
            AssertJUnit.assertEquals(8, TestStatementInspector.getQueryCount());
        }

        try (EntityManager em = factory.createEntityManager()) {
            RAssignment a = em.find(RUser.class, userOid).getAssignments().iterator().next();
            AssertJUnit.assertEquals("ch1.3", a.getCreateChannel());
            AssertJUnit.assertEquals(2, a.getModifyApproverRef().size());
            AssertJUnit.assertEquals(2, a.getCreateApproverRef().size());

            assertEquals(a.getModifierRef(),
                    createEmbeddedRepoRef(UserType.COMPLEX_TYPE, "44.3"));
            //noinspection unchecked
            assertReferences((Collection) a.getCreateApproverRef(),
                    createRepoRef(UserType.COMPLEX_TYPE, "55.3"),
                    createRepoRef(UserType.COMPLEX_TYPE, "77.3"));
            //noinspection unchecked
            assertReferences((Collection) a.getModifyApproverRef(),
                    createRepoRef(UserType.COMPLEX_TYPE, "55.3"),
                    createRepoRef(UserType.COMPLEX_TYPE, "66.3"));
        }
    }

//    @Test
//    public void test250ModifyShadow() throws Exception {
//        // todo implement
//        //account-delta.xml
//        [
//        attributes/ship
//            REPLACE: Flying Dutchman
//        attributes/title
//            DELETE: Very Nice Pirate
//        cachingMetadata
//            REPLACE: CachingMetadataType(retrievalTimestamp:2018-02-09T18:30:10.423+01:00)
//        ]
//    }

    @Test
    public void test270ModifyOperationExecution() throws Exception {
        OperationResult result = new OperationResult("test270ModifyOperationExecution");

        String file = FOLDER_BASE + "/modify/user-with-assignment-extension.xml";
        PrismObject<UserType> user = prismContext.parseObject(new File(file));

        UserType userType = user.asObjectable();
        userType.setName(new PolyStringType("test270modifyOperationExecution"));
        OperationExecutionType oe = createOperationExecution("repo1");
        userType.getOperationExecution().add(oe);

        user.setOid(null);
        repositoryService.addObject(user, null, result);
        String userOid = user.getOid();

        assertOperationExecutionSize(userOid, 1);

        oe = createOperationExecution("repo2");
        Collection<ItemDelta<?, ?>> deltas = prismContext.deltaFor(UserType.class)
                .item(UserType.F_OPERATION_EXECUTION)
                .add(oe.asPrismContainerValue().clone())
                .asItemDeltas();
        repositoryService.modifyObject(UserType.class, userOid, deltas, result);

        assertOperationExecutionSize(userOid, 2);
    }

    private OperationExecutionType createOperationExecution(String channel) {
        OperationExecutionType oe = new OperationExecutionType();
        oe.setChannel(channel);
        oe.setStatus(OperationResultStatusType.SUCCESS);
        oe.setTimestamp(XmlTypeConverter.createXMLGregorianCalendar(new Date()));
        ObjectReferenceType ref = new ObjectReferenceType();
        ref.setOid(SystemObjectsType.USER_ADMINISTRATOR.value());
        ref.setType(UserType.COMPLEX_TYPE);
        oe.setInitiatorRef(ref);

        return oe;
    }

    private void assertOperationExecutionSize(String oid, int expectedSize) {

        try (EntityManager em = open()) {
            RUser rUser = (RUser) em.createQuery("from RUser where oid = :o")
                    .setParameter("o", oid)
                    .getSingleResult();

            AssertJUnit.assertEquals(expectedSize, rUser.getOperationExecutions().size());
        }
    }

    // More photo-related tests should go to UserPhotoTest

    @Test
    public void test280AddPhoto() throws Exception {
        OperationResult result = new OperationResult("test280AddPhoto");

        ObjectDelta<UserType> delta = prismContext.deltaFactory().object().createEmptyModifyDelta(UserType.class, userOid);
        delta.addModificationAddProperty(UserType.F_JPEG_PHOTO, new byte[] { 1, 2, 3 });

        TestStatementInspector.start();
        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), result);
        TestStatementInspector.dump();

        if (isUsingH2()) {
            AssertJUnit.assertEquals(6, TestStatementInspector.getQueryCount());
        }

        logger.info("test280AddPhoto check");
        try (EntityManager em = factory.createEntityManager()) {
            RUser u = em.find(RUser.class, userOid);

            Set<RFocusPhoto> p = u.getJpegPhoto();
            AssertJUnit.assertEquals(1, p.size());

            RFocusPhoto photo = p.iterator().next();
            Assert.assertEquals(photo.getPhoto(), new byte[] { 1, 2, 3 });
        }
    }

    @Test
    public void test290ReplacePhoto() throws Exception {
        OperationResult result = new OperationResult("test290ReplacePhoto");

        ObjectDelta<UserType> delta = prismContext.deltaFactory().object().createEmptyModifyDelta(UserType.class, userOid);
        delta.addModificationReplaceProperty(UserType.F_JPEG_PHOTO, new byte[] { 4, 5, 6 });

        TestStatementInspector.start();
        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), result);
        TestStatementInspector.dump();

        if (isUsingH2()) {
            AssertJUnit.assertEquals(6, TestStatementInspector.getQueryCount());
        }

        logger.info("test290ReplacePhoto check");
        try (EntityManager em = factory.createEntityManager()) {
            RUser u = em.find(RUser.class, userOid);

            Set<RFocusPhoto> p = u.getJpegPhoto();
            AssertJUnit.assertEquals(1, p.size());

            RFocusPhoto photo = p.iterator().next();
            Assert.assertEquals(photo.getPhoto(), new byte[] { 4, 5, 6 });
        }
    }

    @Test
    public void test300DeletePhoto() throws Exception {
        OperationResult result = new OperationResult("test300DeletePhoto");

        ObjectDelta<UserType> delta = prismContext.deltaFactory().object().createEmptyModifyDelta(UserType.class, userOid);
        delta.addModificationDeleteProperty(UserType.F_JPEG_PHOTO, new byte[] { 4, 5, 6 });

        TestStatementInspector.start();
        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), result);
        TestStatementInspector.dump();

        if (isUsingH2()) {
            AssertJUnit.assertEquals(5, TestStatementInspector.getQueryCount());
        }

        logger.info("test300DeletePhoto check");
        try (EntityManager em = factory.createEntityManager()) {
            RUser u = em.find(RUser.class, userOid);

            Set<RFocusPhoto> p = u.getJpegPhoto();
            AssertJUnit.assertEquals(0, p.size());
        }
    }

    // MID-5906
    @Test
    public void test400AddExtensionItem() throws Exception {
        OperationResult result = new OperationResult("test400AddExtensionItem");

        ObjectDelta<UserType> delta = getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_EXTENSION, EXT_VISIBLE_SINGLE).add("v1")
                .asObjectDelta(userOid);

        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), null, result);

        try (EntityManager em = factory.createEntityManager()) {
            RUser u = em.find(RUser.class, userOid);
            assertExtension(u, itemVisibleSingle, "v1");
        }

        assertSearch(EXT_VISIBLE_SINGLE, "v1", 1, result);
        assertSearch(EXT_VISIBLE_SINGLE, "v2", 0, result);
    }

    // MID-5906
    @Test
    public void test410ReplaceExtensionItem() throws Exception {
        OperationResult result = new OperationResult("test410ReplaceExtensionItem");

        ObjectDelta<UserType> delta = getPrismContext().deltaFor(UserType.class)
                .item(UserType.F_EXTENSION, EXT_VISIBLE_SINGLE).replace()
                .asObjectDelta(userOid);

        repositoryService.modifyObject(UserType.class, userOid, delta.getModifications(), null, result);

        try (EntityManager em = factory.createEntityManager()) {
            RUser u = em.find(RUser.class, userOid);
            assertExtension(u, itemVisibleSingle);
        }

        assertSearch(EXT_VISIBLE_SINGLE, "v1", 0, result);
        assertSearch(EXT_VISIBLE_SINGLE, "v2", 0, result);
    }
}

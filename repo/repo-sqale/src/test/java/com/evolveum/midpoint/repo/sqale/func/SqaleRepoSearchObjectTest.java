/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.func;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.UUID;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.sqale.SqaleRepoBaseTest;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.QFocus;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.MObjectType;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QAssignmentHolder;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.repo.sqlbase.perfmon.SqlPerformanceMonitorImpl;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;

public class SqaleRepoSearchObjectTest extends SqaleRepoBaseTest {

    // org structure
    private String org1Oid; // one root
    private String org11Oid;
    private String org111Oid;
    private String org112Oid;
    private String org12Oid;
    private String org2Oid; // second root
    private String org21Oid;
    private String orgXOid; // under two orgs

    private String user1Oid; // user without org
    private String user2Oid; // different user, this one is in org
    private String user3Oid; // another user in org
    private String user4Oid; // another user in org
    private String task1Oid; // task has more attribute type variability
    private String shadow1Oid; // ditto
    private String service1Oid; // object with integer attribute
    private String case1Oid; // Closed case, two work items
    private String case2Oid;

    // other info used in queries
    private String creatorOid = UUID.randomUUID().toString();
    private String modifierOid = UUID.randomUUID().toString();
    private QName relation1 = QName.valueOf("{https://random.org/ns}rel-1");
    private QName relation2 = QName.valueOf("{https://random.org/ns}rel-2");

    @BeforeClass
    public void initObjects() throws Exception {
        OperationResult result = createOperationResult();

        // org structure
        org1Oid = repositoryService.addObject(
                new OrgType(prismContext).name("org-1").asPrismObject(),
                null, result);
        org11Oid = repositoryService.addObject(
                new OrgType(prismContext).name("org-1-1")
                        .parentOrgRef(org1Oid, OrgType.COMPLEX_TYPE)
                        .asPrismObject(),
                null, result);
        org111Oid = repositoryService.addObject(
                new OrgType(prismContext).name("org-1-1-1")
                        .parentOrgRef(org11Oid, OrgType.COMPLEX_TYPE, relation2)
                        .asPrismObject(),
                null, result);
        org112Oid = repositoryService.addObject(
                new OrgType(prismContext).name("org-1-1-2")
                        .parentOrgRef(org11Oid, OrgType.COMPLEX_TYPE, relation1)
                        .asPrismObject(),
                null, result);
        org12Oid = repositoryService.addObject(
                new OrgType(prismContext).name("org-1-2")
                        .parentOrgRef(org1Oid, OrgType.COMPLEX_TYPE)
                        .asPrismObject(),
                null, result);
        org2Oid = repositoryService.addObject(
                new OrgType(prismContext).name("org-2").asPrismObject(),
                null, result);
        org21Oid = repositoryService.addObject(
                new OrgType(prismContext).name("org-2-1")
                        .costCenter("5")
                        .parentOrgRef(org2Oid, OrgType.COMPLEX_TYPE)
                        .asPrismObject(),
                null, result);
        orgXOid = repositoryService.addObject(
                new OrgType(prismContext).name("org-X")
                        .parentOrgRef(org12Oid, OrgType.COMPLEX_TYPE)
                        .parentOrgRef(org21Oid, OrgType.COMPLEX_TYPE, SchemaConstants.ORG_MANAGER)
                        .asPrismObject(),
                null, result);

        // other objects
        user1Oid = repositoryService.addObject(
                new UserType(prismContext).name("user-1")
                        .metadata(new MetadataType()
                                .creatorRef(creatorOid, UserType.COMPLEX_TYPE, relation1)
                                .createChannel("create-channel")
                                .createTimestamp(MiscUtil.asXMLGregorianCalendar(1L))
                                .modifierRef(modifierOid, UserType.COMPLEX_TYPE, relation2)
                                .modifyChannel("modify-channel")
                                .modifyTimestamp(MiscUtil.asXMLGregorianCalendar(2L)))
                        .asPrismObject(),
                null, result);
        user2Oid = repositoryService.addObject(
                new UserType(prismContext).name("user-2")
                        .parentOrgRef(orgXOid, OrgType.COMPLEX_TYPE)
                        .parentOrgRef(org11Oid, OrgType.COMPLEX_TYPE, relation1)
                        .asPrismObject(),
                null, result);
        user3Oid = repositoryService.addObject(
                new UserType(prismContext).name("user-3")
                        .costCenter("50")
                        .parentOrgRef(orgXOid, OrgType.COMPLEX_TYPE)
                        .parentOrgRef(org21Oid, OrgType.COMPLEX_TYPE, relation1)
                        .asPrismObject(),
                null, result);
        user4Oid = repositoryService.addObject(
                new UserType(prismContext).name("user-4")
                        .costCenter("51")
                        .parentOrgRef(org111Oid, OrgType.COMPLEX_TYPE)
                        .asPrismObject(),
                null, result);
        task1Oid = repositoryService.addObject(
                new TaskType(prismContext).name("task-1").asPrismObject(),
                null, result);
        shadow1Oid = repositoryService.addObject(
                new ShadowType(prismContext).name("shadow-1").asPrismObject(),
                null, result);
        service1Oid = repositoryService.addObject(
                new ServiceType(prismContext).name("service-1").asPrismObject(),
                null, result);
        case1Oid = repositoryService.addObject(
                new CaseType(prismContext).name("case-1")
                        .state("closed")
                        .closeTimestamp(MiscUtil.asXMLGregorianCalendar(321L))
                        .workItem(new CaseWorkItemType(prismContext)
                                .id(41L)
                                .createTimestamp(MiscUtil.asXMLGregorianCalendar(10000L))
                                .closeTimestamp(MiscUtil.asXMLGregorianCalendar(10100L))
                                .deadline(MiscUtil.asXMLGregorianCalendar(10200L))
                                .originalAssigneeRef(user3Oid, UserType.COMPLEX_TYPE)
                                .performerRef(user3Oid, UserType.COMPLEX_TYPE)
                                .stageNumber(1)
                                .output(new AbstractWorkItemOutputType(prismContext).outcome("OUTCOME one")))
                        .workItem(new CaseWorkItemType(prismContext)
                                .id(42L)
                                .createTimestamp(MiscUtil.asXMLGregorianCalendar(20000L))
                                .closeTimestamp(MiscUtil.asXMLGregorianCalendar(20100L))
                                .deadline(MiscUtil.asXMLGregorianCalendar(20200L))
                                .originalAssigneeRef(user1Oid, UserType.COMPLEX_TYPE)
                                .performerRef(user1Oid, UserType.COMPLEX_TYPE)
                                .stageNumber(2)
                                .output(new AbstractWorkItemOutputType(prismContext).outcome("OUTCOME two")))
                        .asPrismObject(),
                null, result);

        assertThatOperationResult(result).isSuccess();
    }

    // region simple filters
    @Test
    public void test100SearchUserByName() throws Exception {
        when("searching all objects with query without any conditions and paging");
        OperationResult operationResult = createOperationResult();
        SearchResultList<ObjectType> result = searchObjects(ObjectType.class,
                prismContext.queryFor(ObjectType.class)
                        .build(),
                operationResult);

        then("all objects are returned");
        assertThat(result).hasSize((int) count(QObject.CLASS));

        and("operation result is success");
        assertThatOperationResult(operationResult).isSuccess();
        OperationResult subresult = operationResult.getLastSubresult();
        assertThat(subresult).isNotNull();
        assertThat(subresult.getOperation()).isEqualTo("SqaleRepositoryService.searchObjects");
        assertThat(subresult.getStatus()).isEqualTo(OperationResultStatus.SUCCESS);
    }

    @Test
    public void test110SearchUserByName() throws Exception {
        when("searching user with query without any conditions and paging");
        OperationResult operationResult = createOperationResult();
        SearchResultList<UserType> result = searchObjects(UserType.class,
                prismContext.queryFor(UserType.class)
                        .item(UserType.F_NAME).eq(PolyString.fromOrig("user-1"))
                        .build(),
                operationResult);

        then("user with the matching name is returned");
        assertThatOperationResult(operationResult).isSuccess();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOid()).isEqualTo(user1Oid);
    }

    @Test
    public void test180SearchCaseWorkitemByOutcome() throws Exception {
        searchCaseWorkitemByOutcome("OUTCOME one", case1Oid);
        searchCaseWorkitemByOutcome("OUTCOME two", case1Oid);
        searchCaseWorkitemByOutcome("OUTCOME nonexist", null);
    }

    private void searchCaseWorkitemByOutcome(String wiOutcome, String expectedOid) throws Exception {
        when("searching case with query for workitem/output/outcome " + wiOutcome);
        OperationResult operationResult = createOperationResult();
        SearchResultList<CaseType> result = searchObjects(CaseType.class,
                prismContext.queryFor(CaseType.class)
                        .item(ItemPath.create(CaseType.F_WORK_ITEM, CaseWorkItemType.F_OUTPUT, AbstractWorkItemOutputType.F_OUTCOME)).eq(wiOutcome)
                        .build(),
                operationResult);

        then("case with the matching workitem outcome is returned");
        assertThatOperationResult(operationResult).isSuccess();
        if (expectedOid == null) {
            assertThat(result).hasSize(0);
        } else {
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getOid()).isEqualTo(expectedOid);
        }
    }
    // endregion

    // region org filter
    @Test
    public void test200QueryForRootOrganizations() throws SchemaException {
        when("searching orgs with is-root filter");
        OperationResult operationResult = createOperationResult();
        SearchResultList<OrgType> result = searchObjects(OrgType.class,
                prismContext.queryFor(OrgType.class)
                        .isRoot()
                        .build(),
                operationResult);

        then("only organizations without any parents are returned");
        assertThatOperationResult(operationResult).isSuccess();
        assertThat(result).hasSize(2)
                .extracting(o -> o.getOid())
                .containsExactlyInAnyOrder(org1Oid, org2Oid);
    }

    @Test
    public void test201QueryForRootOrganizationsWithWrongType() throws SchemaException {
        // Currently this is "undefined", this does not work in old repo, in new repo it
        // checks parent-org refs (not closure). Prism does not complain either.
        // First we should fix it on Prism level first, then add type check to OrgFilterProcessor.
        when("searching user with is-root filter");
        OperationResult operationResult = createOperationResult();
        SearchResultList<UserType> result = searchObjects(UserType.class,
                prismContext.queryFor(UserType.class)
                        .isRoot()
                        .build(),
                operationResult);

        then("only users without any organizations are returned");
        assertThatOperationResult(operationResult).isSuccess();
        assertThat(result).hasSize(1)
                .extracting(o -> o.getOid())
                .containsExactlyInAnyOrder(user1Oid);
    }

    @Test
    public void test210QueryForDirectChildrenOrgs() throws SchemaException {
        when("searching orgs just under another org");
        OperationResult operationResult = createOperationResult();
        SearchResultList<OrgType> result = searchObjects(OrgType.class,
                prismContext.queryFor(OrgType.class)
                        .isDirectChildOf(org1Oid)
                        .build(),
                operationResult);

        then("only orgs with direct parent-org ref to another org are returned");
        assertThatOperationResult(operationResult).isSuccess();
        assertThat(result).hasSize(2)
                .extracting(o -> o.getOid())
                .containsExactlyInAnyOrder(org11Oid, org12Oid);
    }

    @Test
    public void test211QueryForDirectChildrenOfAnyType() throws SchemaException {
        when("searching objects just under an org");
        OperationResult operationResult = createOperationResult();
        SearchResultList<ObjectType> result = searchObjects(ObjectType.class,
                prismContext.queryFor(ObjectType.class)
                        .isDirectChildOf(org11Oid)
                        .build(),
                operationResult);

        then("only objects (of any type) with direct parent-org ref to another org are returned");
        assertThatOperationResult(operationResult).isSuccess();
        assertThat(result).hasSize(3)
                .extracting(o -> o.getOid())
                .containsExactlyInAnyOrder(org111Oid, org112Oid, user2Oid);
    }

    @Test
    public void test212QueryForDirectChildrenOfAnyTypeWithRelation() throws SchemaException {
        when("searching objects just under an org with specific relation");
        OperationResult operationResult = createOperationResult();
        SearchResultList<ObjectType> result = searchObjects(ObjectType.class,
                prismContext.queryFor(ObjectType.class)
                        .isDirectChildOf(prismContext.itemFactory()
                                .createReferenceValue(org11Oid).relation(relation1))
                        .build(),
                operationResult);

        then("only objects with direct parent-org ref with specified relation are returned");
        assertThatOperationResult(operationResult).isSuccess();
        assertThat(result).hasSize(2)
                .extracting(o -> o.getOid())
                .containsExactlyInAnyOrder(org112Oid, user2Oid);
    }

    @Test
    public void test215QueryForChildrenOfAnyType() throws SchemaException {
        when("searching objects anywhere under an org");
        OperationResult operationResult = createOperationResult();
        SearchResultList<ObjectType> result = searchObjects(ObjectType.class,
                prismContext.queryFor(ObjectType.class)
                        .isChildOf(org2Oid)
                        .build(),
                operationResult);

        then("all objects under the specified organization are returned");
        assertThatOperationResult(operationResult).isSuccess();
        assertThat(result).hasSize(4)
                .extracting(o -> o.getOid())
                .containsExactlyInAnyOrder(org21Oid, orgXOid, user2Oid, user3Oid);
    }

    @Test
    public void test216QueryForChildrenOfAnyTypeWithRelation() throws SchemaException {
        when("searching objects anywhere under an org with specific relation");
        OperationResult operationResult = createOperationResult();
        SearchResultList<ObjectType> result = searchObjects(ObjectType.class,
                prismContext.queryFor(ObjectType.class)
                        .isChildOf(prismContext.itemFactory()
                                .createReferenceValue(org2Oid).relation(relation1))
                        .build(),
                operationResult);

        then("all objects under the specified organization with specified relation are returned");
        assertThatOperationResult(operationResult).isSuccess();
        assertThat(result).hasSize(1)
                .extracting(o -> o.getOid())
                .containsExactlyInAnyOrder(user3Oid);
        // user-2 has another parent link with relation1, but not under org-2
    }

    @Test
    public void test220QueryForParents() throws SchemaException {
        when("searching parents of an org");
        OperationResult operationResult = createOperationResult();
        SearchResultList<OrgType> result = searchObjects(OrgType.class,
                prismContext.queryFor(OrgType.class)
                        .isParentOf(org112Oid)
                        .build(),
                operationResult);

        then("all ancestors of the specified organization are returned");
        assertThatOperationResult(operationResult).isSuccess();
        assertThat(result).hasSize(2)
                .extracting(o -> o.getOid())
                .containsExactlyInAnyOrder(org11Oid, org1Oid);
    }

    @Test
    public void test221QueryForParentsWithOrgSupertype() throws SchemaException {
        when("searching parents of an org using more abstract type");
        OperationResult operationResult = createOperationResult();
        SearchResultList<ObjectType> result = searchObjects(ObjectType.class,
                prismContext.queryFor(ObjectType.class)
                        .isParentOf(org112Oid)
                        .build(),
                operationResult);

        then("returns orgs but as supertype  instances");
        assertThatOperationResult(operationResult).isSuccess();
        assertThat(result).hasSize(2)
                .extracting(o -> o.getOid())
                .containsExactlyInAnyOrder(org11Oid, org1Oid);
    }

    @Test
    public void test222QueryForParentsUsingOtherType() throws SchemaException {
        when("searching parents of an org using unrelated type");
        OperationResult operationResult = createOperationResult();
        SearchResultList<UserType> result = searchObjects(UserType.class,
                prismContext.queryFor(UserType.class)
                        .isParentOf(org112Oid)
                        .build(),
                operationResult);

        then("returns nothing (officially the behaviour is undefined)");
        assertThatOperationResult(operationResult).isSuccess();
        assertThat(result).isEmpty();
    }

    @Test
    public void test223QueryForParentIgnoresRelation() throws SchemaException {
        when("searching parents of an org with specified relation");
        OperationResult operationResult = createOperationResult();
        SearchResultList<OrgType> result = searchObjects(OrgType.class,
                prismContext.queryFor(OrgType.class)
                        .isParentOf(prismContext.itemFactory()
                                .createReferenceValue(org112Oid).relation(relation2))
                        .build(),
                operationResult);

        then("all ancestors are returned, ignoring provided relation");
        assertThatOperationResult(operationResult).isSuccess();
        assertThat(result).hasSize(2)
                .extracting(o -> o.getOid())
                .containsExactlyInAnyOrder(org11Oid, org1Oid);
    }

    @Test
    public void test230QueryForChildrenOfAnyTypeWithAnotherCondition() throws SchemaException {
        when("searching objects anywhere under an org");
        OperationResult operationResult = createOperationResult();
        SearchResultList<FocusType> result = searchObjects(FocusType.class,
                prismContext.queryFor(FocusType.class)
                        .isChildOf(org2Oid)
                        .and().item(FocusType.F_COST_CENTER).startsWith("5")
                        .build(),
                operationResult);

        then("all objects under the specified organization matching other conditions are returned");
        assertThatOperationResult(operationResult).isSuccess();
        assertThat(result).hasSize(2)
                .extracting(o -> o.getOid())
                .containsExactlyInAnyOrder(org21Oid, user3Oid);
    }

    // TODO child/parent tests
    // endregion

    // region other filters: inOid, type, any
    @Test
    public void test300QueryForObjectsWithInOid() throws SchemaException {
        when("searching objects by list of OIDs");
        OperationResult operationResult = createOperationResult();
        SearchResultList<ObjectType> result = searchObjects(ObjectType.class,
                prismContext.queryFor(ObjectType.class)
                        .id(user1Oid, task1Oid, org2Oid)
                        .build(),
                operationResult);

        then("all objects with specified OIDs are returned");
        assertThatOperationResult(operationResult).isSuccess();
        assertThat(result).hasSize(3)
                .extracting(o -> o.getOid())
                .containsExactlyInAnyOrder(user1Oid, task1Oid, org2Oid);
    }

    @Test
    public void test301QueryForFocusWithInOid() throws SchemaException {
        when("searching focus objects by list of OIDs");
        OperationResult operationResult = createOperationResult();
        SearchResultList<FocusType> result = searchObjects(FocusType.class,
                prismContext.queryFor(FocusType.class)
                        .id(user1Oid, task1Oid, org2Oid) // task will not match, it's not a focus
                        .build(),
                operationResult);

        then("all focus objects with specified OIDs are returned");
        assertThatOperationResult(operationResult).isSuccess();
        assertThat(result).hasSize(2)
                .extracting(o -> o.getOid())
                .containsExactlyInAnyOrder(user1Oid, org2Oid);
    }

    /*
    // TODO TYPE tests - unclear how to implement TYPE filter at the moment
    @Test
    public void test310QueryWithTypeFilter() throws SchemaException {
        when("query includes type filter");
        OperationResult operationResult = createOperationResult();
        SearchResultList<ObjectType> result = searchObjects(ObjectType.class,
                prismContext.queryFor(ObjectType.class)
                        .type(FocusType.class)
                        .block()
                        .id(user1Oid, task1Oid, org2Oid) // task will not match, it's not a focus
                        .and()
                        .item(FocusType.F_COST_CENTER).eq("5")
                        .endBlock()
                        .build(),
                operationResult);

        then("search is narrowed only to objects of specific type");
        assertThatOperationResult(operationResult).isSuccess();
        assertThat(result).hasSize(2)
                .extracting(o -> o.getOid())
                .containsExactlyInAnyOrder(user1Oid, org2Oid);
    }
    */

/* TODO EXISTS tests
1. @Count property => pendingOperationCount > 0; see: ClassDefinitionParser#parseMethod() + getJaxbName()
EXISTS(pendingOperation, null)

2. multi-value container stored in table:
EXISTS(operationExecution, AND(REF: taskRef, PRV(oid=task-oid-2, targetType=null); EQUAL: status, PPV(OperationResultStatusType:SUCCESS)))

3. cointainer query (AccessCertificationWorkItemType) with EXISTS to the parent container (AccessCertificationCaseType)
  matching on parent's ownerID (OID of AccessCertificationCampaignType) + its own CID (AccessCertificationCaseType)
EXISTS({http://prism.evolveum.com/xml/ns/public/types-3}parent, AND(IN OID (for owner): e8c07a7a-1b11-11e8-9b32-1715a2e8273b, IN OID: 1))

parent vs ..?

4. part of AND in container query, used on sub-container (workItem) with nested AND condition
CertificationTest.test730CurrentUnansweredCases >>> Q{
AND(
  EQUAL: stageNumber, {http://prism.evolveum.com/xml/ns/public/types-3}parent/stageNumber;
  EQUAL: ../state, PPV(AccessCertificationCampaignStateType:IN_REVIEW_STAGE);
  EXISTS(workItem,
    AND(
      EQUAL: closeTimestamp, ;
      EQUAL: output/outcome, )))
, null paging}

5. EXISTS with .. (another way how to say parent)
EXISTS(..,
  OR(
    IN OID: c0c010c0-d34d-b33f-f00d-111111111111; ,
    REF: ownerRef,PRV(oid=c0c010c0-d34d-b33f-f00d-111111111111, targetType={.../common/common-3}UserType)))

6. nothing new, nested in OR, EXISTS(workItem) with AND

OR(
  IN OID: af69e388-88bd-43f9-9259-73676124c196; ,
  EXISTS(workItem,
    AND(
      EQUAL: closeTimestamp,,
      REF: assigneeRef, PRV(oid=af69e388-88bd-43f9-9259-73676124c196, targetType=null), PRV(oid=c0c010c0-d34d-b33f-f00d-111111111111, targetType=null))))

OR(
  NONE,
  LESS-OR-EQUAL: activation/validFrom,PPV(XMLGregorianCalendarImpl:2021-05-21T15:44:41.955+02:00),
  LESS-OR-EQUAL: activation/validTo,PPV(XMLGregorianCalendarImpl:2021-05-21T15:44:41.955+02:00),
  EXISTS(assignment,
    OR(
      LESS-OR-EQUAL: activation/validFrom,PPV(XMLGregorianCalendarImpl:2021-05-21T15:44:41.955+02:00),
      LESS-OR-EQUAL: activation/validTo,PPV(XMLGregorianCalendarImpl:2021-05-21T15:44:41.955+02:00))))

7. typical case EXISTS(assignment, ...)

8. AND(2x EXISTS with multiple ref values)
main >>> Q{
AND(
  EXISTS(assignment,
    REF: targetRef,
      PRV(oid=4076afcc-4075-4f18-b596-edb71dbf72a9, targetType={.../common/common-3}OrgType, targetName=A-newOrg, relation={.../common/org-3}default),
      PRV(oid=4076afcc-4075-4f18-b596-edb71dbf72a9, targetType={.../common/common-3}OrgType, targetName=A-newOrg, relation={.../common/org-3}manager),
      PRV(oid=4076afcc-4075-4f18-b596-edb71dbf72a9, targetType={.../common/common-3}OrgType, targetName=A-newOrg, relation={.../common/org-3}approver),
      PRV(oid=4076afcc-4075-4f18-b596-edb71dbf72a9, targetType={.../common/common-3}OrgType, targetName=A-newOrg, relation={.../common/org-3}owner));
  EXISTS(assignment,
    REF: targetRef,
      PRV(oid=4076afcc-4075-4f18-b596-edb71dbf72a9, targetType={.../common/common-3}OrgType, targetName=A-newOrg, relation={.../common/org-3}default),
      PRV(oid=4076afcc-4075-4f18-b596-edb71dbf72a9, targetType={.../common/common-3}OrgType, targetName=A-newOrg, relation={.../common/org-3}manager),
      PRV(oid=4076afcc-4075-4f18-b596-edb71dbf72a9, targetType={.../common/common-3}OrgType, targetName=A-newOrg, relation={.../common/org-3}approver),
      PRV(oid=4076afcc-4075-4f18-b596-edb71dbf72a9, targetType={.../common/common-3}OrgType, targetName=A-newOrg, relation={.../common/org-3}owner)))
, null paging}
*/
    // endregion

    // region special cases
    @Test
    public void test900SearchByWholeContainerIsNotPossible() {
        expect("creating query with embedded container equality fails");
        assertThatThrownBy(
                () -> prismContext.queryFor(UserType.class)
                        .item(UserType.F_METADATA).eq(new MetadataType())
                        .build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageStartingWith("Unsupported item definition: PCD:");

        // even if query was possible this would fail in the actual repo search, which is expected
    }

    @Test
    public void test920SearchObjectTypeFindsAllObjects() throws SchemaException {
        OperationResult operationResult = createOperationResult();

        given("query without any filter");
        ObjectQuery query = prismContext.queryFor(ObjectType.class).build();

        when("search is called with ObjectType");
        SearchResultList<ObjectType> result =
                searchObjects(ObjectType.class, query, operationResult);

        then("all repository objects are returned");
        assertThat(result).hasSize((int) count(QObject.CLASS));
    }

    @Test
    public void test921SearchAssignmentHolderTypeFindsAllObjectsExceptShadows()
            throws SchemaException {
        OperationResult operationResult = createOperationResult();

        given("query without any filter");
        ObjectQuery query = prismContext.queryFor(ObjectType.class).build();

        when("search is called with AssignmentHolderType");
        SearchResultList<AssignmentHolderType> result =
                searchObjects(AssignmentHolderType.class, query, operationResult);

        then("all repository objects except shadows are returned");
        QObject<MObject> o = aliasFor(QObject.CLASS);
        assertThat(result).hasSize((int) count(o, o.objectType.ne(MObjectType.SHADOW)));
        assertThat(result).hasSize((int) count(QAssignmentHolder.CLASS));
        // without additional objects the test would be meaningless
        assertThat(result).hasSizeLessThan((int) count(o));
    }

    @Test
    public void test921SearchFocusTypeFindsOnlyFocusObjects()
            throws SchemaException {
        OperationResult operationResult = createOperationResult();

        given("query without any filter");
        ObjectQuery query = prismContext.queryFor(ObjectType.class).build();

        when("search is called with FocusType");
        SearchResultList<FocusType> result =
                searchObjects(FocusType.class, query, operationResult);

        then("only focus objects from repository are returned");
        assertThat(result).hasSize((int) count(QFocus.CLASS));
        // without additional objects the test would be meaningless
        assertThat(result).hasSizeLessThan((int) count(QObject.CLASS));
    }

    @Test
    public void test950SearchOperationUpdatesPerformanceMonitor() throws SchemaException {
        OperationResult operationResult = createOperationResult();

        given("cleared performance information");
        SqlPerformanceMonitorImpl pm = repositoryService.getPerformanceMonitor();
        pm.clearGlobalPerformanceInformation();
        assertThat(pm.getGlobalPerformanceInformation().getAllData()).isEmpty();

        when("search is called on the repository");
        searchObjects(FocusType.class,
                prismContext.queryFor(FocusType.class).build(),
                operationResult);

        then("performance monitor is updated");
        assertThatOperationResult(operationResult).isSuccess();
        assertSingleOperationRecorded(pm, RepositoryService.OP_SEARCH_OBJECTS);
    }

    @Test(enabled = false)
    public void test960SearchByAxiomQueryLanguage() throws SchemaException {
        OperationResult operationResult = createOperationResult();
        SearchResultList<FocusType> focusTypes = searchObjects(FocusType.class,
                ". type UserType and employeeNumber startsWith \"5\"",
                operationResult);
        System.out.println("focusTypes = " + focusTypes);
        // even if query was possible this would fail in the actual repo search, which is expected
    }
    // endregion

    // support methods

    /** Search objects using Axiom query language. */
    @SafeVarargs
    @NotNull
    private <T extends ObjectType> SearchResultList<T> searchObjects(
            @NotNull Class<T> type,
            String query,
            OperationResult operationResult,
            SelectorOptions<GetOperationOptions>... selectorOptions)
            throws SchemaException {
        ObjectFilter objectFilter = prismContext.createQueryParser().parseQuery(type, query);
        ObjectQuery objectQuery = prismContext.queryFactory().createQuery(objectFilter);
        return searchObjects(type, objectQuery, operationResult, selectorOptions);
    }

    /** Search objects using {@link ObjectQuery}. */
    @SafeVarargs
    @NotNull
    private <T extends ObjectType> SearchResultList<T> searchObjects(
            @NotNull Class<T> type,
            ObjectQuery query,
            OperationResult operationResult,
            SelectorOptions<GetOperationOptions>... selectorOptions)
            throws SchemaException {
        QueryType queryType = prismContext.getQueryConverter().createQueryType(query);
        String serializedQuery = prismContext.xmlSerializer().serializeAnyData(
                queryType, SchemaConstants.MODEL_EXTENSION_OBJECT_QUERY);
        display("QUERY: " + serializedQuery);

        // sanity check if it's re-parsable
        assertThat(prismContext.parserFor(serializedQuery).parseRealValue(QueryType.class))
                .isNotNull();
        return repositoryService.searchObjects(
                type,
                query,
                Arrays.asList(selectorOptions),
                operationResult)
                .map(p -> p.asObjectable());
    }
}

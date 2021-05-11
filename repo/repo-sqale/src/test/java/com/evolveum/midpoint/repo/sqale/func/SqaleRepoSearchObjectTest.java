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

import com.querydsl.sql.SQLQuery;
import org.jetbrains.annotations.NotNull;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.sqale.SqaleRepoBaseTest;
import com.evolveum.midpoint.repo.sqale.qmodel.focus.QUser;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.repo.sqale.qmodel.org.QOrgClosure;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QObjectReference;
import com.evolveum.midpoint.repo.sqale.qmodel.ref.QObjectReferenceMapping;
import com.evolveum.midpoint.repo.sqlbase.JdbcSession;
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

    private String user1Oid; // typical object
    private String task1Oid; // task has more attribute type variability
    private String shadow1Oid; // ditto
    private String service1Oid; // object with integer attribute
    private String org1Oid;
    private String org11Oid;
    private String org111Oid;
    private String org112Oid;
    private String org2Oid;
    private String org21Oid;
    private String orgXOid; // under two orgs

    // other info used in queries
    private String creatorOid = UUID.randomUUID().toString();
    private String modifierOid = UUID.randomUUID().toString();
    private QName relation1 = QName.valueOf("{https://random.org/ns}rel-1");
    private QName relation2 = QName.valueOf("{https://random.org/ns}rel-2");

    @BeforeClass
    public void initObjects() throws Exception {
        OperationResult result = createOperationResult();

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
        task1Oid = repositoryService.addObject(
                new TaskType(prismContext).name("task-1").asPrismObject(),
                null, result);
        shadow1Oid = repositoryService.addObject(
                new ShadowType(prismContext).name("shadow-1").asPrismObject(),
                null, result);
        service1Oid = repositoryService.addObject(
                new ServiceType(prismContext).name("service-1").asPrismObject(),
                null, result);
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
                        .parentOrgRef(org11Oid, OrgType.COMPLEX_TYPE)
                        .asPrismObject(),
                null, result);
        org112Oid = repositoryService.addObject(
                new OrgType(prismContext).name("org-1-1-2")
                        .parentOrgRef(org11Oid, OrgType.COMPLEX_TYPE)
                        .asPrismObject(),
                null, result);
        org2Oid = repositoryService.addObject(
                new OrgType(prismContext).name("org-2").asPrismObject(),
                null, result);
        org21Oid = repositoryService.addObject(
                new OrgType(prismContext).name("org-2-1")
                        .parentOrgRef(org2Oid, OrgType.COMPLEX_TYPE)
                        .asPrismObject(),
                null, result);
        orgXOid = repositoryService.addObject(
                new OrgType(prismContext).name("org-X")
                        .parentOrgRef(org11Oid, OrgType.COMPLEX_TYPE)
                        .parentOrgRef(org21Oid, OrgType.COMPLEX_TYPE, SchemaConstants.ORG_MANAGER)
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
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOid()).isEqualTo(user1Oid);

        and("operation result is success");
        assertThatOperationResult(operationResult).isSuccess();
    }
    // endregion

    // region org filter
    @Test
    public void testQuerydslCteSimple() {
        try (JdbcSession jdbcSession = sqlRepoContext.newJdbcSession().startReadOnlyTransaction()) {
            // CTE query just for the organization closure
            QOrgClosure orgc = new QOrgClosure();
            QObjectReference<?> ref = QObjectReferenceMapping.getForParentOrg().defaultAlias();
            QObjectReference<?> par = QObjectReferenceMapping.getForParentOrg().defaultAlias();
            //noinspection unchecked
            SQLQuery<?> query = sqlRepoContext.newQuery(jdbcSession.connection())
                    .withRecursive(orgc, orgc.parent, orgc.child)
                    .as(new SQLQuery<>().union(
                            // non-recursive term: initial select
                            new SQLQuery<>()
//                                    .select(ref.ownerOid, ref.ownerOid) // use this to include identity loops
                                    .select(ref.targetOid, ref.ownerOid)
                                    .from(ref)
                                    // add where here if possible for much faster results (often not possible)
                                    .where(),
                            // recursive term: each time add the parents for what we have gathered in orgc until now
                            new SQLQuery<>().select(par.targetOid, orgc.child)
                                    .from(par, orgc)
                                    .where(par.ownerOid.eq(orgc.parent))))
                    .select(orgc.parent, orgc.child)
                    .from(orgc)
                    .where(orgc.child.eq(UUID.randomUUID()));
            System.out.println("query = " + query);
            Object o = query.fetchFirst();
            System.out.println("o = " + o);
        }
    }

    @Test
    public void testQuerydslCteForUser() {
        try (JdbcSession jdbcSession = sqlRepoContext.newJdbcSession().startReadOnlyTransaction()) {
            // CTE for user with condition that is child of org X
            UUID orgXOid = UUID.randomUUID();

            QUser u = aliasFor(QUser.class);
            QOrgClosure orgc = new QOrgClosure(); // vanilla new, this has no mapping
            QObjectReference<?> ref = QObjectReferenceMapping.getForParentOrg().defaultAlias();
            QObjectReference<?> par = QObjectReferenceMapping.getForParentOrg().defaultAlias();
            //noinspection unchecked
            SQLQuery<?> query = sqlRepoContext.newQuery(jdbcSession.connection())
                    .withRecursive(orgc, orgc.parent, orgc.child)
                    .as(new SQLQuery<>().union(
                            // non-recursive term: initial select
                            new SQLQuery<>()
//                                    .select(ref.ownerOid, ref.ownerOid) // use this to include identity loops
                                    .select(ref.targetOid, ref.ownerOid)
                                    .from(ref)
                                    // add where here if possible for much faster results (often not possible)
                                    .where(),
                            // recursive term: each time add the parents for what we have gathered in orgc until now
                            new SQLQuery<>().select(par.targetOid, orgc.child)
                                    .from(par, orgc)
                                    .where(par.ownerOid.eq(orgc.parent))))
                    .select(u)
                    .from(u)
                    .where(u.honorificPrefixNorm.startsWith("x")
                            // query filter condition
                            .and(new SQLQuery<>()
                                    .from(orgc)
                                    .where(orgc.child.eq(u.oid)
                                            .and(orgc.parent.eq(orgXOid)))
                                    .exists()));

            System.out.println("query = " + query);
            Object o = query.fetchFirst();
            // just exec check, hardly returns something for random UUID
            System.out.println("o = " + o);
        }
    }
    // TODO
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
    // endregion

    // support methods
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

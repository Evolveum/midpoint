/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.repo.sql.data.common.ROrgClosure;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.testng.AssertJUnit.assertEquals;

@ContextConfiguration(locations = {"../../../../../ctx-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class OrgStructTest extends BaseSQLRepoTest {

    private static final File TEST_DIR = new File("src/test/resources/orgstruct");

    private static final String ORG_STRUCT_OBJECTS = TEST_DIR + "/org-monkey-island.xml";
    private static final String ORG_STRUCT_OBJECTS_INCORRECT = TEST_DIR + "/org-monkey-island-incorrect.xml";

    private static final String MODIFY_ORG_ADD_REF_FILENAME = TEST_DIR + "/modify-orgStruct-add-orgref.xml";
    private static final String MODIFY_ORG_INCORRECT_ADD_REF_FILENAME = TEST_DIR + "/modify-orgStruct-incorrect-add-orgref.xml";

    private static final String MODIFY_ORG_DELETE_REF_FILENAME = TEST_DIR + "/modify-orgStruct-delete-ref.xml";
    private static final String MODIFY_ORG_INCORRECT_DELETE_REF_FILENAME = TEST_DIR + "/modify-orgStruct-incorrect-delete-ref.xml";

    private static final String MODIFY_ORG_ADD_USER_FILENAME = TEST_DIR + "/modify-orgStruct-add-user.xml";

    private static final String ORG_F001_OID = "00000000-8888-6666-0000-100000000001";
    private static final String ORG_F002_OID = "00000000-8888-6666-0000-100000000002";
    private static final String ORG_F003_OID = "00000000-8888-6666-0000-100000000003";
    private static final String ORG_F004_OID = "00000000-8888-6666-0000-100000000004";
    private static final String ORG_F005_OID = "00000000-8888-6666-0000-100000000005";
    private static final String ORG_F006_OID = "00000000-8888-6666-0000-100000000006";
    private static final String ORG_F007_OID = "00000000-8888-6666-0000-100000000007";
    private static final String ORG_F008_OID = "00000000-8888-6666-0000-100000000008";
    private static final String ORG_F009_OID = "00000000-8888-6666-0000-100000000009";
    private static final String ORG_F010_OID = "00000000-8888-6666-0000-100000000010";
    private static final String ORG_F012_OID = "00000000-8888-6666-0000-100000000012";

    private static final String ORG_PROJECT_ROOT_OID = "00000000-8888-6666-0000-200000000000";

    private static final String MODIFY_ORG_ADD_REF_OID = "00000000-8888-6666-0000-100000000005";
    private static final String MODIFY_ORG_INCORRECT_ADD_REF_OID = "00000000-8888-6666-0000-100000000006";

    private static final String MODIFY_ORG_DELETE_REF_OID = "00000000-8888-6666-0000-100000000006";
    private static final String MODIFY_ORG_INCORRECT_DELETE_REF_OID = "00000000-8888-6666-0000-100000000006";

    private static final String DELETE_ORG_OID = "00000000-8888-6666-0000-100000000006";

    private static final String SEARCH_ORG_OID_UNBOUNDED_DEPTH = "00000000-8888-6666-0000-100000000001";
    private static final String SEARCH_ORG_OID_DEPTH1 = "00000000-8888-6666-0000-100000000001";

    private static final String MODIFY_USER_DELETE_REF_OID = "00000000-8888-6666-0000-100000000002";

    private static final Trace LOGGER = TraceManager.getTrace(OrgStructTest.class);

    String ELAINE_OID;
    private static final String ELAINE_NAME = "elaine";
    private static final String ELAINE_NAME1 = "elaine1";

    @Test
    public void test001addOrgStructObjects() throws Exception {
        OperationResult opResult = new OperationResult("test001addOrgStructObjects");
        List<PrismObject<? extends Objectable>> orgStruct = prismContext.parserFor(new File(ORG_STRUCT_OBJECTS)).parseObjects();

        for (PrismObject<? extends Objectable> o : orgStruct) {
            repositoryService.addObject((PrismObject<ObjectType>) o, null, opResult);
        }
        opResult.computeStatusIfUnknown();
        AssertJUnit.assertTrue(opResult.isSuccess());

        List<PrismObject<OrgType>> orgTypes = repositoryService.searchObjects(OrgType.class, new ObjectQuery(), null, opResult);
        AssertJUnit.assertNotNull(orgTypes);
        AssertJUnit.assertEquals(9, orgTypes.size());

        OrgType pRoot = repositoryService.getObject(OrgType.class, ORG_PROJECT_ROOT_OID, null, opResult).asObjectable();
        AssertJUnit.assertEquals("PRoot", pRoot.getName().getOrig());

        PrismObjectDefinition<UserType> userObjectDef = prismContext.getSchemaRegistry()
                .findObjectDefinitionByCompileTimeClass(UserType.class);

        ObjectQuery query = QueryBuilder.queryFor(UserType.class, prismContext)
                .item(UserType.F_NAME).eq(ELAINE_NAME)
                .build();
        List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, null, opResult);

        AssertJUnit.assertEquals(1, users.size());
        ELAINE_OID = users.get(0).getOid();

        testMonkeySubordinate();

        Session session = open();
        try {
            Query qCount = session.createQuery("select count(*) from ROrgClosure");
            assertCount(qCount, 19);

            // check descendants for F0001 org unit
            qCount = session.createQuery("select count(*) from ROrgClosure where ancestorOid = :ancestorOid");
            qCount.setParameter("ancestorOid", ORG_F001_OID);
            assertCount(qCount, 6);

            qCount = session.createQuery("select count(*) from ROrgClosure where ancestorOid = :ancestorOid and descendantOid = :descendantOid");
            qCount.setParameter("ancestorOid", ORG_F001_OID);
            qCount.setParameter("descendantOid", ORG_F006_OID);
            assertCount(qCount, 1);
        } finally {
            close(session);
        }
    }

    private void assertCount(Query query, int count) {
        Number number = (Number) query.uniqueResult();
        AssertJUnit.assertNotNull(number);
        AssertJUnit.assertEquals(count, number.intValue());
    }

    /**
     * Tests for repo.matchObject() method
     */
    private void testMonkeySubordinate() throws SchemaException, ObjectNotFoundException {
        assertSubordinate(false, ORG_F003_OID, ORG_F001_OID);
        assertSubordinate(true, ORG_F003_OID, ORG_F003_OID);
        assertSubordinate(true, ORG_F003_OID, ORG_F005_OID);
        assertSubordinate(false, ORG_F003_OID, ORG_F002_OID);
        assertSubordinate(false, ORG_F003_OID, ORG_F004_OID);

        assertSubordinate(true, ORG_F001_OID, ORG_F001_OID);
        assertSubordinate(true, ORG_F001_OID, ORG_F003_OID);
        assertSubordinate(true, ORG_F001_OID, ORG_F005_OID);
        assertSubordinate(true, ORG_F001_OID, ORG_F002_OID);
        assertSubordinate(true, ORG_F001_OID, ORG_F004_OID);

        assertSubordinate(false, ORG_F005_OID, ORG_F001_OID);
        assertSubordinate(false, ORG_F005_OID, ORG_F003_OID);
        assertSubordinate(true, ORG_F005_OID, ORG_F005_OID);
        assertSubordinate(false, ORG_F005_OID, ORG_F002_OID);
        assertSubordinate(false, ORG_F005_OID, ORG_F004_OID);

        assertSubordinate(true, ORG_F003_OID, ORG_F005_OID, ORG_F006_OID);
        assertSubordinate(true, ORG_F003_OID, ORG_F002_OID, ORG_F006_OID);
        assertSubordinate(true, ORG_F003_OID, ORG_F004_OID, ORG_F005_OID);
        assertSubordinate(true, ORG_F003_OID, ORG_F005_OID, ORG_F004_OID);
        assertSubordinate(true, ORG_F003_OID, ORG_F005_OID, ORG_F001_OID);
        assertSubordinate(false, ORG_F003_OID, ORG_F001_OID, ORG_F004_OID);
        assertSubordinate(false, ORG_F003_OID, ORG_F002_OID, ORG_F004_OID);
        assertSubordinate(false, ORG_F003_OID, ORG_F001_OID, ORG_F002_OID, ORG_F004_OID);
        assertSubordinate(true, ORG_F003_OID, ORG_F001_OID, ORG_F005_OID, ORG_F004_OID);
        assertSubordinate(true, ORG_F003_OID, ORG_F001_OID, ORG_F003_OID, ORG_F004_OID);
        assertSubordinate(true, ORG_F003_OID, ORG_F001_OID, ORG_F005_OID, ORG_F006_OID);

        assertSubordinate(true, ORG_F001_OID, ORG_F005_OID, ORG_F006_OID);
        assertSubordinate(true, ORG_F001_OID, ORG_F002_OID, ORG_F006_OID);
        assertSubordinate(true, ORG_F001_OID, ORG_F005_OID, ORG_F004_OID);
        assertSubordinate(true, ORG_F001_OID, ORG_F005_OID, ORG_F001_OID);
        assertSubordinate(true, ORG_F001_OID, ORG_F001_OID, ORG_F004_OID);
        assertSubordinate(true, ORG_F001_OID, ORG_F002_OID, ORG_F004_OID);
        assertSubordinate(true, ORG_F001_OID, ORG_F001_OID, ORG_F002_OID, ORG_F004_OID);
        assertSubordinate(true, ORG_F001_OID, ORG_F001_OID, ORG_F005_OID, ORG_F004_OID);
        assertSubordinate(true, ORG_F001_OID, ORG_F001_OID, ORG_F003_OID, ORG_F004_OID);
        assertSubordinate(true, ORG_F001_OID, ORG_F001_OID, ORG_F005_OID, ORG_F006_OID);

        assertSubordinate(true, ORG_F006_OID, ORG_F005_OID, ORG_F006_OID);
        assertSubordinate(true, ORG_F006_OID, ORG_F002_OID, ORG_F006_OID);
        assertSubordinate(false, ORG_F006_OID, ORG_F005_OID, ORG_F004_OID);
        assertSubordinate(false, ORG_F006_OID, ORG_F005_OID, ORG_F001_OID);
        assertSubordinate(false, ORG_F006_OID, ORG_F001_OID, ORG_F004_OID);
        assertSubordinate(false, ORG_F006_OID, ORG_F002_OID, ORG_F004_OID);
        assertSubordinate(false, ORG_F006_OID, ORG_F001_OID, ORG_F002_OID, ORG_F004_OID);
        assertSubordinate(false, ORG_F006_OID, ORG_F001_OID, ORG_F005_OID, ORG_F004_OID);
        assertSubordinate(false, ORG_F006_OID, ORG_F001_OID, ORG_F003_OID, ORG_F004_OID);
        assertSubordinate(true, ORG_F006_OID, ORG_F001_OID, ORG_F005_OID, ORG_F006_OID);
    }

    private void assertSubordinate(boolean expected, String upperOrgOid, String... lowerObjectOids) throws SchemaException {
        Collection<String> lowerObjectOidCol = Arrays.asList(lowerObjectOids);
        LOGGER.debug("=======> {}: {}", upperOrgOid, lowerObjectOidCol);
        boolean actual = repositoryService.isAnySubordinate(upperOrgOid, lowerObjectOidCol);
        if (expected != actual) {
            LOGGER.error("=======X {}: {}; expected={}, actual={}", new Object[]{upperOrgOid, lowerObjectOidCol, expected, actual});
            assertEquals("Wrong subordinate match: " + upperOrgOid + " to " + lowerObjectOidCol, expected, actual);
        } else {
            LOGGER.debug("=======O {}: {}; got={}", new Object[]{upperOrgOid, lowerObjectOidCol, expected});
        }
    }

    @Test
    public void test001addOrgStructObjectsIncorrect() throws Exception {
        OperationResult opResult = new OperationResult("test001addOrgStructObjectsIncorrect");

        List<PrismObject<? extends Objectable>> orgStructIncorrect = prismContext.parserFor(
                new File(ORG_STRUCT_OBJECTS_INCORRECT)).parseObjects();

        for (PrismObject<? extends Objectable> o : orgStructIncorrect) {
            repositoryService.addObject((PrismObject<ObjectType>) o, null, opResult);
        }
        opResult.computeStatusIfUnknown();
        AssertJUnit.assertTrue(opResult.isSuccess());

        Session session = open();
        try {
            LOGGER.info("==============CLOSURE TABLE==========");
            // descendants of F007 - F007<0>, F009<1>, F008<2>, F0010<2>
            Criteria criteria = session.createCriteria(ROrgClosure.class)
                    .createCriteria("ancestor", "anc")
                    .setFetchMode("ancestor", FetchMode.JOIN)
                    .add(Restrictions.eq("anc.oid", ORG_F007_OID));

            List<ROrgClosure> orgClosure = criteria.list();
            for (ROrgClosure c : orgClosure) {
                LOGGER.info("{}", c.getDescendant());
            }
            AssertJUnit.assertEquals(4, orgClosure.size());

            criteria = session.createCriteria(ROrgClosure.class)
                    .createCriteria("ancestor", "anc")
                    .setFetchMode("ancestor", FetchMode.JOIN)
                    .add(Restrictions.eq("anc.oid", ORG_F009_OID));

            orgClosure = criteria.list();
            AssertJUnit.assertEquals(3, orgClosure.size());

            ObjectQuery query = QueryBuilder.queryFor(UserType.class, prismContext)
                    .item(UserType.F_NAME).eq(ELAINE_NAME1)
                    .build();
            List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, null, opResult);

            AssertJUnit.assertNotNull(users);
            AssertJUnit.assertEquals(1, users.size());
            UserType elaine1 = users.get(0).asObjectable();
            LOGGER.info("--->elaine1<----");

            AssertJUnit.assertEquals("Expected name elaine1, but got " + elaine1.getName().getOrig(), "elaine1", elaine1.getName().getOrig());
            AssertJUnit.assertEquals("Expected elaine has one org ref, but got " + elaine1.getParentOrgRef().size(), 2, elaine1.getParentOrgRef().size());
            AssertJUnit.assertEquals("Parent org ref oid not equal.", "00000000-8888-6666-0000-100000000011", elaine1.getParentOrgRef().get(0).getOid());
        } finally {
            close(session);
        }
    }

    @Test
    public void test002modifyOrgStructAddRef() throws Exception {
        OperationResult opResult = new OperationResult("test002modifyOrgStructAddRef");
        // test modification of org ref in another org type..

        ObjectModificationType modification = PrismTestUtil.parseAtomicValue(new File(MODIFY_ORG_ADD_REF_FILENAME),
                ObjectModificationType.COMPLEX_TYPE);
        ObjectDelta<OrgType> delta = DeltaConvertor.createObjectDelta(modification, OrgType.class, prismContext);

        Query query;
        List<ROrgClosure> orgClosure;

        Session session = open();
        try {
            orgClosure = getOrgClosureByDescendant(MODIFY_ORG_ADD_REF_OID, session);

            LOGGER.info("before modify");
            for (ROrgClosure c : orgClosure) {
                LOGGER.info("{}\t{}", new Object[]{c.getAncestor().getOid(), c.getDescendant().getOid()});
            }
            AssertJUnit.assertEquals(3, orgClosure.size());
        } finally {
            close(session);
        }

        repositoryService.modifyObject(OrgType.class, MODIFY_ORG_ADD_REF_OID, delta.getModifications(), opResult);

        session = open();
        try {
            orgClosure = getOrgClosureByDescendant(MODIFY_ORG_ADD_REF_OID, session);

            LOGGER.info("after modify");
            for (ROrgClosure c : orgClosure) {
                LOGGER.info("{}\t{}", new Object[]{c.getAncestor().getOid(), c.getDescendant().getOid()});
            }
            AssertJUnit.assertEquals(4, orgClosure.size());

            List<String> ancestors = new ArrayList<String>();
            ancestors.add(MODIFY_ORG_ADD_REF_OID);
            ancestors.add(ORG_F003_OID);
            ancestors.add(ORG_F001_OID);
            ancestors.add(ORG_F002_OID);

            for (String ancestorOid : ancestors) {
                orgClosure = getOrgClosure(ancestorOid, MODIFY_ORG_ADD_REF_OID, session);
                LOGGER.info("=> A: {}, D: {}", orgClosure.get(0).getAncestor(), orgClosure.get(0).getDescendant());

                AssertJUnit.assertEquals(1, orgClosure.size());
                AssertJUnit.assertEquals(ancestorOid, orgClosure.get(0).getAncestor().getOid());
                AssertJUnit.assertEquals(MODIFY_ORG_ADD_REF_OID, orgClosure.get(0).getDescendant().getOid());
            }
        } finally {
            close(session);
        }
    }

    private List<ROrgClosure> getOrgClosure(String ancestorOid, String descendantOid, Session session) {
        Query query = session.createQuery("from ROrgClosure where ancestorOid=:aOid and descendantOid=:dOid");
        query.setString("aOid", ancestorOid);
        query.setString("dOid", descendantOid);
        return query.list();
    }

    private List<ROrgClosure> getOrgClosureByDescendant(String descendantOid, Session session) {
        Query query = session.createQuery("from ROrgClosure where descendantOid=:oid");
        query.setString("oid", descendantOid);
        return query.list();
    }

    @Test
    public void test002modifyOrgStructAddRefIncorrect() throws Exception {
        OperationResult opResult = new OperationResult("test002modifyOrgStructAddRefIncorrect");
        // test modification of org ref in another org type..

        ObjectModificationType modification = PrismTestUtil.parseAtomicValue(new File(MODIFY_ORG_INCORRECT_ADD_REF_FILENAME),
                ObjectModificationType.COMPLEX_TYPE);
        ObjectDelta<OrgType> delta = DeltaConvertor.createObjectDelta(modification, OrgType.class, prismContext);

        repositoryService.modifyObject(OrgType.class, MODIFY_ORG_INCORRECT_ADD_REF_OID, delta.getModifications(), opResult);

        Session session = open();

        try {
            List<ROrgClosure> orgClosure = getOrgClosureByDescendant(MODIFY_ORG_INCORRECT_ADD_REF_OID, session);

            LOGGER.info("after modify incorrect - closure");
            for (ROrgClosure c : orgClosure) {
                LOGGER.info("{}\t{}", new Object[]{c.getAncestor().getOid(), c.getDescendant().getOid()});
            }
            AssertJUnit.assertEquals(5, orgClosure.size());

            List<String> ancestors = new ArrayList<String>();
            ancestors.add(MODIFY_ORG_INCORRECT_ADD_REF_OID);
            ancestors.add(ORG_F001_OID);
            ancestors.add(ORG_F002_OID);

            for (String ancestorOid : ancestors) {
                orgClosure = getOrgClosure(ancestorOid, MODIFY_ORG_INCORRECT_ADD_REF_OID, session);

                AssertJUnit.assertEquals(1, orgClosure.size());

                AssertJUnit.assertEquals(ancestorOid, orgClosure.get(0).getAncestor().getOid());
                AssertJUnit.assertEquals(MODIFY_ORG_INCORRECT_ADD_REF_OID, orgClosure.get(0).getDescendant().getOid());
            }

        } finally {
            close(session);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test003modifyOrgStructDeleteRef() throws Exception {
        // test modification of org ref - delete org ref
        OperationResult opResult = new OperationResult("test003modifyOrgStructDeleteRef");

        ObjectModificationType modification = PrismTestUtil.parseAtomicValue(new File(MODIFY_ORG_DELETE_REF_FILENAME),
                ObjectModificationType.COMPLEX_TYPE);

        ObjectDelta<OrgType> delta = DeltaConvertor.createObjectDelta(modification, OrgType.class, prismContext);

        Session session = open();
        try {
            LOGGER.info("==>before modify - delete<==");
            List<ROrgClosure> orgClosure = getOrgClosure(ORG_F003_OID, MODIFY_ORG_DELETE_REF_OID, session);
            AssertJUnit.assertEquals(1, orgClosure.size());

            session.getTransaction().commit();

            repositoryService.modifyObject(OrgType.class, MODIFY_ORG_DELETE_REF_OID, delta.getModifications(), opResult);

            session.clear();
            session.beginTransaction();

            LOGGER.info("==>after modify - delete<==");
            orgClosure = getOrgClosure(ORG_F003_OID, MODIFY_ORG_DELETE_REF_OID, session);
            AssertJUnit.assertEquals(0, orgClosure.size());
        } finally {
            close(session);
        }
    }

    @Test
    public void test003modifyOrgStructDeleteRefIncorrect() throws Exception {
        // test modification of org ref - delete org ref
        OperationResult opResult = new OperationResult("test003modifyOrgStructDeleteRefIncorrect");
        ObjectModificationType modification = PrismTestUtil.parseAtomicValue(new File(MODIFY_ORG_INCORRECT_DELETE_REF_FILENAME),
                ObjectModificationType.COMPLEX_TYPE);

        ObjectDelta<OrgType> delta = DeltaConvertor.createObjectDelta(modification, OrgType.class, prismContext);

        Session session = open();
        try {
            LOGGER.info("==>before modify - delete<==");
            List<ROrgClosure> orgClosure = getOrgClosure(ORG_F012_OID, MODIFY_ORG_INCORRECT_DELETE_REF_OID, session);
            AssertJUnit.assertEquals(0, orgClosure.size());

            session.getTransaction().commit();

            repositoryService.modifyObject(OrgType.class, MODIFY_ORG_INCORRECT_DELETE_REF_OID, delta.getModifications(), opResult);

            session.clear();
            session.beginTransaction();

            LOGGER.info("==>after modify - delete<==");
            orgClosure = getOrgClosure(ORG_F012_OID, MODIFY_ORG_INCORRECT_DELETE_REF_OID, session);
            AssertJUnit.assertEquals(0, orgClosure.size());
        } finally {
            close(session);
        }
    }

    @Test(enabled = false)          // users are not in the closure any more
    public void test004modifyOrgStructAddUser() throws Exception {
        Session session = open();
        try {
            OperationResult opResult = new OperationResult("test004modifyOrgStructAddUser");

            //test modification of org ref in another org type..
            ObjectModificationType modification = PrismTestUtil.parseAtomicValue(new File(MODIFY_ORG_ADD_USER_FILENAME),
                    ObjectModificationType.COMPLEX_TYPE);

            ObjectDelta<UserType> delta = DeltaConvertor.createObjectDelta(modification, UserType.class, prismContext);

            repositoryService.modifyObject(UserType.class, ELAINE_OID, delta.getModifications(), opResult);

            LOGGER.info("==>after modify - add user to org<==");
            List<ROrgClosure> orgClosure = getOrgClosureByDescendant(ELAINE_OID, session);
            AssertJUnit.assertEquals(7, orgClosure.size());

            LOGGER.info("==============CLOSURE TABLE==========");

            for (ROrgClosure o : orgClosure) {
                LOGGER.info("=> A: {}, D: {}", o.getAncestor(), o.getDescendant());
            }
        } finally {
            close(session);
        }
    }

    @Test
    public void test005deleteOrg() throws Exception {
        OperationResult opResult = new OperationResult("test005deleteOrg");

        repositoryService.deleteObject(OrgType.class, DELETE_ORG_OID, opResult);

        Session session = open();
        try {
            Query sqlOrgClosure = session.createQuery("select count(*) from ROrgClosure where descendantOid=:oid or ancestorOid=:oid");
            sqlOrgClosure.setParameter("oid", DELETE_ORG_OID);

            Number number = (Number) sqlOrgClosure.uniqueResult();
            AssertJUnit.assertEquals(0, (number != null ? number.intValue() : 0));
        } finally {
            close(session);
        }
    }

    @Test
    public void test006searchOrgStructUserUnbounded() throws Exception {
        OperationResult parentResult = new OperationResult("test006searchOrgStructUserUnbounded");

        ObjectQuery objectQuery = QueryBuilder.queryFor(ObjectType.class, prismContext)
                .isChildOf(SEARCH_ORG_OID_UNBOUNDED_DEPTH)
                .asc(ObjectType.F_NAME)
                .build();

        List<PrismObject<ObjectType>> orgClosure = repositoryService.searchObjects(ObjectType.class, objectQuery, null, parentResult);

        for (PrismObject<ObjectType> u : orgClosure) {
            LOGGER.info("USER000 ======> {}", ObjectTypeUtil.toShortString(u.asObjectable()));
        }

        AssertJUnit.assertEquals(7, orgClosure.size());
    }

    @Test
    public void test007searchOrgStructOrgDepth() throws Exception {
        OperationResult parentResult = new OperationResult("test007searchOrgStructOrgDepth");

        Session session = open();
        try {
            List<ROrgClosure> orgClosure = getOrgClosure(SEARCH_ORG_OID_DEPTH1, SEARCH_ORG_OID_DEPTH1, session);

            LOGGER.info("==============CLOSURE TABLE==========");
            for (ROrgClosure o : orgClosure) {
                LOGGER.info("=> A: {}, D: {}", o.getAncestor(), o.getDescendant());
            }
            AssertJUnit.assertEquals(1, orgClosure.size());
            session.getTransaction().commit();
            session.close();

            ObjectQuery objectQuery = QueryBuilder.queryFor(ObjectType.class, prismContext)
                    .isDirectChildOf(SEARCH_ORG_OID_DEPTH1)
                    .asc(ObjectType.F_NAME)
                    .build();
            List<PrismObject<ObjectType>> sOrgClosure = repositoryService.searchObjects(ObjectType.class, objectQuery, null, parentResult);

            for (PrismObject<ObjectType> u : sOrgClosure) {
                LOGGER.info("USER000 ======> {}", ObjectTypeUtil.toShortString(u.asObjectable()));
            }
            AssertJUnit.assertEquals(4, sOrgClosure.size());
        } finally {
            if (session.isOpen()) {
                close(session);
            }
        }
    }

    @Test
    public void test008searchRootOrg() throws Exception {
        OperationResult parentResult = new OperationResult("test008searchRootOrg");

        ObjectQuery qSearch = QueryBuilder.queryFor(ObjectType.class, prismContext)
                .isRoot()
                .asc(ObjectType.F_NAME)
                .build();
        List<PrismObject<OrgType>> rootOrgs = repositoryService.searchObjects(OrgType.class, qSearch, null, parentResult);

        for (PrismObject<OrgType> ro : rootOrgs) {
            LOGGER.info("ROOT  ========= {}", ObjectTypeUtil.toShortString(ro.asObjectable()));
        }
        AssertJUnit.assertEquals(5, rootOrgs.size());
    }

    @Test
    public void test009modifyOrgStructRemoveUser() throws Exception {
        OperationResult opResult = new OperationResult("test009modifyOrgStructRemoveUser");

        PrismReferenceValue prv = new PrismReferenceValue(MODIFY_USER_DELETE_REF_OID);
        prv.setTargetType(OrgType.COMPLEX_TYPE);
        ObjectDelta<UserType> delta = ObjectDelta.createModificationDeleteReference(UserType.class, ELAINE_OID, UserType.F_PARENT_ORG_REF, prismContext, prv);

        repositoryService.modifyObject(UserType.class, ELAINE_OID, delta.getModifications(), opResult);

        UserType userElaine = repositoryService.getObject(UserType.class, ELAINE_OID, null, opResult).asObjectable();
        LOGGER.trace("elaine's og refs");
        for (ObjectReferenceType ort : userElaine.getParentOrgRef()) {
            LOGGER.trace("{}", ort);
            if (ort.getOid().equals(MODIFY_USER_DELETE_REF_OID)) {
                AssertJUnit.fail("expected that elain does not have reference on the org with oid:" + MODIFY_USER_DELETE_REF_OID);
            }
        }
    }

    @Test
    public void test011OrgFilter() throws Exception {
    	final String TEST_NAME = "test011OrgFilter";
    	TestUtil.displayTestTile(TEST_NAME);
        OperationResult opResult = new OperationResult(TEST_NAME);

        ObjectQuery query = QueryBuilder.queryFor(ObjectType.class, prismContext)
                .isDirectChildOf(ORG_F001_OID)
                .asc(ObjectType.F_NAME)
                .build();

        // WHEN
        List<PrismObject<ObjectType>> orgClosure = repositoryService.searchObjects(ObjectType.class, query, null, opResult);

        // THEN
        AssertJUnit.assertEquals(4, orgClosure.size());
    }
    
    @Test
    public void test100ParentOrgRefFilterNullRelation() throws Exception {
    	final String TEST_NAME = "test100ParentOrgRefFilterNullRelation";
    	TestUtil.displayTestTile(TEST_NAME);
        OperationResult opResult = new OperationResult(TEST_NAME);

        ObjectQuery query = QueryBuilder.queryFor(ObjectType.class, prismContext)
                .item(ObjectType.F_PARENT_ORG_REF).ref(new PrismReferenceValue(ORG_F001_OID))
                .build();

        // WHEN
        List<PrismObject<ObjectType>> orgs = repositoryService.searchObjects(ObjectType.class, query, null, opResult);

        // THEN
        PrismAsserts.assertOids(orgs, ORG_F002_OID, ORG_F003_OID, ORG_F004_OID, ELAINE_OID);
    }
    
    @Test
    public void test101ParentOrgRefFilterManagerRelation() throws Exception {
    	final String TEST_NAME = "test101ParentOrgRefFilterManagerRelation";
    	TestUtil.displayTestTile(TEST_NAME);
        OperationResult opResult = new OperationResult(TEST_NAME);

        PrismReferenceValue refVal = new PrismReferenceValue(ORG_F001_OID);
        refVal.setRelation(SchemaConstants.ORG_MANAGER);
        ObjectQuery query = QueryBuilder.queryFor(ObjectType.class, prismContext)
                .item(ObjectType.F_PARENT_ORG_REF).ref(refVal)
                .build();

        // WHEN
        List<PrismObject<ObjectType>> orgs = repositoryService.searchObjects(ObjectType.class, query, null, opResult);

        // THEN
        PrismAsserts.assertOids(orgs, ELAINE_OID);
    }
    
    @Test
    public void test102ParentOrgRefFilterAnyRelation() throws Exception {
    	final String TEST_NAME = "test102ParentOrgRefFilterAnyRelation";
    	TestUtil.displayTestTile(TEST_NAME);
        OperationResult opResult = new OperationResult(TEST_NAME);

        PrismReferenceValue refVal = new PrismReferenceValue(ORG_F001_OID);
        refVal.setRelation(PrismConstants.Q_ANY);
        ObjectQuery query = QueryBuilder.queryFor(ObjectType.class, prismContext)
                .item(ObjectType.F_PARENT_ORG_REF).ref(refVal)
                .build();

        // WHEN
        List<PrismObject<ObjectType>> orgs = repositoryService.searchObjects(ObjectType.class, query, null, opResult);

        // THEN
        PrismAsserts.assertOids(orgs, ORG_F002_OID, ORG_F003_OID, ORG_F004_OID, ELAINE_OID, ELAINE_OID);
    }

}

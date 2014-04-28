/*
 * Copyright (c) 2010-2013 Evolveum
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

import static org.testng.AssertJUnit.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.xml.namespace.QName;

import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualsFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrFilter;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.prism.query.OrgFilter;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.repo.sql.data.common.RObjectReference;
import com.evolveum.midpoint.repo.sql.data.common.ROrgClosure;
import com.evolveum.midpoint.repo.sql.data.common.ROrgIncorrect;
import com.evolveum.midpoint.repo.sql.data.common.RUser;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.data.common.other.RObjectType;
import com.evolveum.midpoint.repo.sql.data.common.type.RParentOrgRef;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

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

    @SuppressWarnings({"unchecked", "unused"})
    @Test
    public void test001addOrgStructObjects() throws Exception {
        OperationResult opResult = new OperationResult("test001addOrgStructObjects");
        List<PrismObject<? extends Objectable>> orgStruct = prismContext.parseObjects(
                new File(ORG_STRUCT_OBJECTS));  

        for (PrismObject<? extends Objectable> o : orgStruct) {
            repositoryService.addObject((PrismObject<ObjectType>) o, null, opResult);
        }

        List<PrismObject<OrgType>> orgTypes = repositoryService.searchObjects(OrgType.class, new ObjectQuery(), null, opResult);
        AssertJUnit.assertNotNull(orgTypes);
        AssertJUnit.assertEquals(9, orgTypes.size());

        OrgType orgF001 = repositoryService.getObject(OrgType.class, ORG_F001_OID, null, opResult).asObjectable();
        AssertJUnit.assertNotNull(orgF001);
        AssertJUnit.assertEquals("F0001", orgF001.getName().getOrig());

        OrgType orgF003 = repositoryService.getObject(OrgType.class, ORG_F003_OID, null, opResult).asObjectable();
        OrgType orgF004 = repositoryService.getObject(OrgType.class, ORG_F004_OID, null, opResult).asObjectable();
        OrgType orgF006 = repositoryService.getObject(OrgType.class, ORG_F006_OID, null, opResult).asObjectable();

        AssertJUnit.assertNotNull(orgF006);
        AssertJUnit.assertEquals("F0006", orgF006.getName().getOrig());

        ObjectReferenceType oRefType;
        boolean isEqual003 = false;
        boolean isEqual004 = false;
        for (int i = 0; i < orgF006.getParentOrgRef().size(); i++) {
            oRefType = orgF006.getParentOrgRef().get(i);
            isEqual003 = isEqual003 || oRefType.getOid().equals(orgF003.getOid());
            isEqual004 = isEqual004 || oRefType.getOid().equals(orgF004.getOid());
        }

        AssertJUnit.assertEquals(true, isEqual003);
        AssertJUnit.assertEquals(true, isEqual004);

        OrgType pRoot = repositoryService.getObject(OrgType.class, ORG_PROJECT_ROOT_OID, null, opResult).asObjectable();
        AssertJUnit.assertEquals("PRoot", pRoot.getName().getOrig());

//		QueryType query = QueryUtil.createNameQuery(ELAINE_NAME);
        ObjectQuery query = new ObjectQuery();
        PrismObjectDefinition<UserType> userObjectDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
        query.setFilter(EqualsFilter.createEqual(UserType.F_NAME, userObjectDef.findPropertyDefinition(UserType.F_NAME), null, ELAINE_NAME));

        List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, null, opResult);

        AssertJUnit.assertNotNull(users);
        AssertJUnit.assertEquals(1, users.size());
        UserType elaine = users.get(0).asObjectable();

        AssertJUnit.assertEquals("Expected name elaine, but got " + elaine.getName().getOrig(), "elaine", elaine.getName().getOrig());
        AssertJUnit.assertEquals("Expected elaine has one org ref, but got " + elaine.getParentOrgRef().size(), 2, elaine.getParentOrgRef().size());
        AssertJUnit.assertEquals("Parent org ref oid not equal.", "00000000-8888-6666-0000-100000000001", elaine.getParentOrgRef().get(0).getOid());

        ELAINE_OID = elaine.getOid();

        testMonkeySubordinate();

        LOGGER.info("==>after add<==");
        Session session = getFactory().openSession();
        session.beginTransaction();
        Query qCount = session.createQuery("select count(*) from ROrgClosure");
        long count = (Long) qCount.uniqueResult();
        AssertJUnit.assertEquals(54L, count);

        // check descendants for F0001 org unit
        qCount = session.createQuery("select count(*) from ROrgClosure where ancestorOid = :ancestorOid");
        qCount.setParameter("ancestorOid", ORG_F001_OID);
        count = (Long) qCount.uniqueResult();
        AssertJUnit.assertEquals(13L, count);

        qCount = session.createQuery("select count(*) from ROrgClosure where ancestorOid = :ancestorOid and descendantOid = :descendantOid");
        qCount.setParameter("ancestorOid", ORG_F001_OID);
        qCount.setParameter("descendantOid", ORG_F006_OID);
        count = (Long) qCount.uniqueResult();
        AssertJUnit.assertEquals(1, count);

        session.getTransaction().commit();
        session.close();
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
			assertEquals("Wrong subordinate match: "+upperOrgOid+" to "+lowerObjectOidCol, expected, actual);
		} else {
			LOGGER.debug("=======O {}: {}; got={}", new Object[]{upperOrgOid, lowerObjectOidCol, expected});
		}
	}

	private ObjectFilter createOrgNameFilter(String name) throws SchemaException {
		return EqualsFilter.createEqual(OrgType.F_NAME, OrgType.class, prismContext, PrismTestUtil.createPolyString(name));
	}
    
    private ObjectFilter createOrgDisplayNameFilter(String name) throws SchemaException {
		return EqualsFilter.createEqual(OrgType.F_DISPLAY_NAME, OrgType.class, prismContext, PrismTestUtil.createPolyString(name));
	}

	private <O extends ObjectType> void assertMatch(PrismObject<O> obj, ObjectFilter filter, boolean expected) throws SchemaException {
    	ObjectQuery query = new ObjectQuery();
		query.setFilter(filter);
		boolean actual = repositoryService.matchObject(obj, query);
		assertEquals("Wrong match for "+obj+" and "+filter, expected, actual);
    }

	@SuppressWarnings({"unchecked"})
    @Test
    public void test001addOrgStructObjectsIncorrect() throws Exception {

    	LOGGER.info("===[ addIncorrectOrgStruct ]===");
    	
         OperationResult opResult = new OperationResult("===[ addIncorrectOrgStruct ]===");       
                  
        List<PrismObject<? extends Objectable>> orgStructIncorrect = prismContext.parseObjects(
        	 new File(ORG_STRUCT_OBJECTS_INCORRECT));
            
        for (PrismObject<? extends Objectable> o : orgStructIncorrect) {  	 
         repositoryService.addObject((PrismObject<ObjectType>) o, null, opResult);
        }

        OrgType orgF008 = repositoryService.getObject(OrgType.class, ORG_F008_OID, null, opResult).asObjectable();
        OrgType orgF009 = repositoryService.getObject(OrgType.class, ORG_F009_OID, null, opResult).asObjectable();
        AssertJUnit.assertEquals(orgF008.getParentOrgRef().get(0).getOid(), orgF009.getOid());

        OrgType orgF010 = repositoryService.getObject(OrgType.class, ORG_F010_OID, null, opResult).asObjectable();

        ObjectReferenceType oRefType = new ObjectReferenceType();
        boolean isEqual009 = false;
        boolean isEqual012 = false;
        for (int i = 0; i < orgF010.getParentOrgRef().size(); i++) {
            oRefType = orgF010.getParentOrgRef().get(i);
            isEqual009 = isEqual009 || oRefType.getOid().equals(orgF009.getOid());
            isEqual012 = isEqual012 || oRefType.getOid().equals(ORG_F012_OID);
        }

        AssertJUnit.assertEquals(true, isEqual009);
        AssertJUnit.assertEquals(true, isEqual012);

        LOGGER.info("==>after add<==");
        Session session = getFactory().openSession();
        session.beginTransaction();

        LOGGER.info("==============CLOSURE TABLE==========");
        List<ROrgClosure> orgClosure = session.createQuery("from ROrgClosure").list();
        // descendants of F007 - F007<0>, F009<1>, F008<2>, F0010<2>
        Criteria criteria = session.createCriteria(ROrgClosure.class)
                .createCriteria("ancestor", "anc")
                .setFetchMode("ancestor", FetchMode.JOIN)
                .add(Restrictions.eq("anc.oid", ORG_F007_OID));

        orgClosure = criteria.list();
        AssertJUnit.assertEquals(5, orgClosure.size());

        criteria = session.createCriteria(ROrgClosure.class)
                .add(Restrictions.eq("depth", 2))
                .createCriteria("ancestor", "anc")
                .setFetchMode("ancestor", FetchMode.JOIN)
                .add(Restrictions.eq("anc.oid", ORG_F007_OID));


        orgClosure = criteria.list();
        AssertJUnit.assertEquals(3, orgClosure.size());

        LOGGER.info("==============ORG INCORRECT TABLE==========");
        List<ROrgIncorrect> orgIncorrect = session.createQuery("from ROrgIncorrect").list();
        AssertJUnit.assertEquals(1, orgIncorrect.size());
        AssertJUnit.assertEquals(ORG_F012_OID, orgIncorrect.get(0).getAncestorOid());


        ObjectQuery query = new ObjectQuery();
        PrismObjectDefinition<UserType> userObjectDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
        query.setFilter(EqualsFilter.createEqual(UserType.F_NAME, userObjectDef.findPropertyDefinition(UserType.F_NAME), null, ELAINE_NAME1));

        List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, null, opResult);

        AssertJUnit.assertNotNull(users);
        AssertJUnit.assertEquals(1, users.size());
        UserType elaine1 = users.get(0).asObjectable();
        LOGGER.info("--->elaine1<----");
//        LOGGER.info(prismContext.silentMarshalObject(elaine1, LOGGER));
        AssertJUnit.assertEquals("Expected name elaine, but got " + elaine1.getName().getOrig(), "elaine1", elaine1.getName().getOrig());
        AssertJUnit.assertEquals("Expected elaine has one org ref, but got " + elaine1.getParentOrgRef().size(), 2, elaine1.getParentOrgRef().size());
        AssertJUnit.assertEquals("Parent org ref oid not equal.", "00000000-8888-6666-0000-100000000011", elaine1.getParentOrgRef().get(0).getOid());

        session.getTransaction().commit();
        session.close();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test002modifyOrgStructAddRef() throws Exception {
        LOGGER.info("===[ modifyOrgStruct ]===");
        OperationResult opResult = new OperationResult("===[ modifyOrgStruct ]===");
        // test modification of org ref in another org type..

        ObjectModificationType modification = PrismTestUtil.parseAtomicValue(new File(MODIFY_ORG_ADD_REF_FILENAME),
                ObjectModificationType.COMPLEX_TYPE);
        ObjectDelta<OrgType> delta = DeltaConvertor.createObjectDelta(modification, OrgType.class, prismContext);

        Session session = getFactory().openSession();
        session.beginTransaction();

        Criteria criteria = session.createCriteria(ROrgClosure.class)
                .createCriteria("descendant", "desc")
                .setFetchMode("descendant", FetchMode.JOIN)
                .add(Restrictions.eq("desc.oid", MODIFY_ORG_ADD_REF_OID));

        List<ROrgClosure> orgClosure = criteria.list();

        LOGGER.info("before modify");
        for (ROrgClosure c : orgClosure) {
            LOGGER.info("{}\t{}", new Object[]{c.getAncestor().getOid(), c.getDescendant().getOid()});
        }
        AssertJUnit.assertEquals(3, orgClosure.size());
        session.getTransaction().commit();

        repositoryService.modifyObject(OrgType.class, MODIFY_ORG_ADD_REF_OID, delta.getModifications(), opResult);

        session.clear();
        session.beginTransaction();
        criteria = session.createCriteria(ROrgClosure.class)
                .createCriteria("descendant", "desc")
                .setFetchMode("descendant", FetchMode.JOIN)
                .add(Restrictions.eq("desc.oid", MODIFY_ORG_ADD_REF_OID));

        orgClosure = criteria.list();

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
            criteria = session.createCriteria(ROrgClosure.class);
            criteria.createCriteria("ancestor", "anc").setFetchMode("ancestor", FetchMode.JOIN)
                    .add(Restrictions.eq("anc.oid", ancestorOid));
            criteria.createCriteria("descendant", "desc")
                    .setFetchMode("descendant", FetchMode.JOIN).add(Restrictions.eq("desc.oid", MODIFY_ORG_ADD_REF_OID));

            orgClosure = criteria.list();

            LOGGER.info("=> A: {}, D: {}", new Object[]{orgClosure.get(0).getAncestor().toJAXB(prismContext, null),
                    orgClosure.get(0).getDescendant().toJAXB(prismContext, null)});

            AssertJUnit.assertEquals(1, orgClosure.size());
            AssertJUnit.assertEquals(ancestorOid, orgClosure.get(0)
                    .getAncestor().getOid());
            AssertJUnit.assertEquals(MODIFY_ORG_ADD_REF_OID, orgClosure.get(0)
                    .getDescendant().getOid());
        }
        session.getTransaction().commit();
        session.close();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test002modifyOrgStructAddRefIncorrect() throws Exception {
        LOGGER.info("===[ modifyOrgStruct ]===");
        OperationResult opResult = new OperationResult("===[ modifyOrgStructIncorrect ]===");
        // test modification of org ref in another org type..

        ObjectModificationType modification = PrismTestUtil.parseAtomicValue(new File(MODIFY_ORG_INCORRECT_ADD_REF_FILENAME),
                ObjectModificationType.COMPLEX_TYPE);
        ObjectDelta<OrgType> delta = DeltaConvertor.createObjectDelta(modification, OrgType.class, prismContext);

        repositoryService.modifyObject(OrgType.class, MODIFY_ORG_INCORRECT_ADD_REF_OID, delta.getModifications(), opResult);

        Session session = getFactory().openSession();
        session.beginTransaction();

        Criteria criteria = session.createCriteria(ROrgClosure.class)
                .createCriteria("descendant", "desc")
                .setFetchMode("descendant", FetchMode.JOIN)
                .add(Restrictions.eq("desc.oid", MODIFY_ORG_INCORRECT_ADD_REF_OID));

        List<ROrgClosure> orgClosure = criteria.list();

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
            criteria = session.createCriteria(ROrgClosure.class);
            criteria.createCriteria("ancestor", "anc").setFetchMode("ancestor", FetchMode.JOIN)
                    .add(Restrictions.eq("anc.oid", ancestorOid));
            criteria.createCriteria("descendant", "desc")
                    .setFetchMode("descendant", FetchMode.JOIN)
                    .add(Restrictions.eq("desc.oid", MODIFY_ORG_INCORRECT_ADD_REF_OID));

            orgClosure = criteria.list();

            AssertJUnit.assertEquals(1, orgClosure.size());

            AssertJUnit.assertEquals(ancestorOid, orgClosure.get(0).getAncestor().getOid());
            AssertJUnit.assertEquals(MODIFY_ORG_INCORRECT_ADD_REF_OID, orgClosure.get(0).getDescendant().getOid());
        }


        criteria = session.createCriteria(ROrgIncorrect.class)
                .add(Restrictions.eq("descendantOid", MODIFY_ORG_INCORRECT_ADD_REF_OID));

        List<ROrgIncorrect> orgIncorrect = criteria.list();

        AssertJUnit.assertEquals(1, orgIncorrect.size());
        AssertJUnit.assertEquals(ORG_F012_OID, orgIncorrect.get(0).getAncestorOid());

        session.getTransaction().commit();
        session.close();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test003modifyOrgStructDeleteRef() throws Exception {

        // test modification of org ref - delete org ref
        LOGGER.info("===[ modify delete org ref ]===");
        OperationResult opResult = new OperationResult("===[ modify delete org ref ]===");
        
        ObjectModificationType modification = PrismTestUtil.parseAtomicValue(new File(MODIFY_ORG_DELETE_REF_FILENAME),
                ObjectModificationType.COMPLEX_TYPE);

        ObjectDelta<OrgType> delta = DeltaConvertor.createObjectDelta(modification, OrgType.class, prismContext);

        Session session = getFactory().openSession();
        session.beginTransaction();

        LOGGER.info("==>before modify - delete<==");

        Query qDelete = session.createQuery("from ROrgClosure as o where " +
                "o.descendantOid = :descendantOid and o.ancestorOid = :ancestorOid and o.depth = :depth");
        qDelete.setParameter("descendantOid", MODIFY_ORG_DELETE_REF_OID);
        qDelete.setParameter("ancestorOid", ORG_F003_OID);
        qDelete.setParameter("depth", 1);

        List<ROrgClosure> orgClosure = qDelete.list();

        AssertJUnit.assertEquals(1, orgClosure.size());

        session.getTransaction().commit();

        repositoryService.modifyObject(OrgType.class, MODIFY_ORG_DELETE_REF_OID, delta.getModifications(), opResult);

        session.clear();
        session.beginTransaction();

        LOGGER.info("==>after modify - delete<==");

        qDelete = session.createQuery("from ROrgClosure as o where " +
                "o.descendantOid = :descendantOid  and o.ancestorOid = :ancestorOid and o.depth = :depth");
        qDelete.setParameter("descendantOid", MODIFY_ORG_DELETE_REF_OID);
        qDelete.setParameter("ancestorOid", ORG_F003_OID);
        qDelete.setParameter("depth", 1);

        orgClosure = qDelete.list();

        AssertJUnit.assertEquals(0, orgClosure.size());

        session.getTransaction().commit();
        session.close();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test003modifyOrgStructDeleteRefIncorrect() throws Exception {

        // test modification of org ref - delete org ref
        LOGGER.info("===[ modify delete org ref ]===");
        OperationResult opResult = new OperationResult("===[ modify delete org ref ]===");
        ObjectModificationType modification = PrismTestUtil.parseAtomicValue(new File(MODIFY_ORG_INCORRECT_DELETE_REF_FILENAME),
                ObjectModificationType.COMPLEX_TYPE);

        ObjectDelta<OrgType> delta = DeltaConvertor.createObjectDelta(modification, OrgType.class, prismContext);

        Session session = getFactory().openSession();
        session.beginTransaction();

        LOGGER.info("==>before modify - delete<==");

        Query qDelete = session.createQuery("from ROrgClosure as o where " +
                "o.descendantOid = :descendantOid  and o.ancestorOid = :ancestorOid");
        qDelete.setParameter("descendantOid", MODIFY_ORG_INCORRECT_DELETE_REF_OID);
        qDelete.setParameter("ancestorOid", ORG_F012_OID);

        List<ROrgClosure> orgClosure = qDelete.list();

        AssertJUnit.assertEquals(0, orgClosure.size());

        session.getTransaction().commit();

        repositoryService.modifyObject(OrgType.class, MODIFY_ORG_INCORRECT_DELETE_REF_OID, delta.getModifications(), opResult);

        session.clear();
        session.beginTransaction();

        LOGGER.info("==>after modify - delete<==");

        qDelete = session.createQuery("from ROrgClosure as o where " +
                "o.descendantOid = :descendantOid and o.ancestorOid = :ancestorOid");
        qDelete.setParameter("descendantOid", MODIFY_ORG_INCORRECT_DELETE_REF_OID);
        qDelete.setParameter("ancestorOid", ORG_F012_OID);

        orgClosure = qDelete.list();

        AssertJUnit.assertEquals(0, orgClosure.size());

        session.getTransaction().commit();
        session.close();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test004modifyOrgStructAddUser() throws Exception {
        Session session = getFactory().openSession();

        OperationResult opResult = new OperationResult("test004modifyOrgStructAddUser");

        //test modification of org ref in another org type..
        ObjectModificationType modification = PrismTestUtil.parseAtomicValue(new File(MODIFY_ORG_ADD_USER_FILENAME),
                ObjectModificationType.COMPLEX_TYPE);

        ObjectDelta<UserType> delta = DeltaConvertor.createObjectDelta(modification, UserType.class, prismContext);

        repositoryService.modifyObject(UserType.class, ELAINE_OID, delta.getModifications(), opResult);

        session.beginTransaction();

        LOGGER.info("==>after modify - add user to org<==");

        Query sqlOrgClosure = session.createQuery("from ROrgClosure as o where o.descendantOid = :descendantOid");
        sqlOrgClosure.setParameter("descendantOid", ELAINE_OID);
        List<ROrgClosure> orgClosure = sqlOrgClosure.list();

        AssertJUnit.assertEquals(7, orgClosure.size());

        LOGGER.info("==============CLOSURE TABLE==========");

        for (ROrgClosure o : orgClosure) {
            LOGGER.info("=> A: {}, D: {}", new Object[]{o.getAncestor().toJAXB(prismContext, null),
                    o.getDescendant().toJAXB(prismContext, null)});

        }
        session.getTransaction().commit();
        session.close();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test005deleteOrg() throws Exception {
        OperationResult opResult = new OperationResult("test005deleteOrg");

        repositoryService.deleteObject(OrgType.class, DELETE_ORG_OID, opResult);

        Session session = getFactory().openSession();
        session.beginTransaction();

        Query sqlOrgClosure = session.createQuery("select count(*) from ROrgClosure where descendantOid=:oid or ancestorOid=:oid");
        sqlOrgClosure.setParameter("oid", DELETE_ORG_OID);

        Number number = (Number) sqlOrgClosure.uniqueResult();
        AssertJUnit.assertEquals(0, (number != null ? number.intValue() : 0));

        session.getTransaction().commit();
        session.close();
    }

    @Test
    public void test006searchOrgStructUserUnbounded() throws Exception {
        OperationResult parentResult = new OperationResult("test006searchOrgStructUserUnbounded");

        ObjectQuery objectQuery = ObjectQuery.createObjectQuery(OrgFilter.createOrg(SEARCH_ORG_OID_UNBOUNDED_DEPTH));
        objectQuery.setPaging(ObjectPaging.createPaging(null, null, ObjectType.F_NAME, OrderDirection.ASCENDING));

        List<PrismObject<ObjectType>> orgClosure = repositoryService.searchObjects(ObjectType.class, objectQuery, null, parentResult);

        for (PrismObject<ObjectType> u : orgClosure) {
            LOGGER.info("USER000 ======> {}", ObjectTypeUtil.toShortString(u.asObjectable()));
        }

        AssertJUnit.assertEquals(7, orgClosure.size());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test007searchOrgStructOrgDepth() throws Exception {
        OperationResult parentResult = new OperationResult("test007searchOrgStructOrgDepth");

        Session session = getFactory().openSession();
        session.beginTransaction();

        Query sqlSearch = session.createQuery("from ROrgClosure where depthValue=1 and (ancestorOid=:oid or descendantOid=:oid)");
        sqlSearch.setParameter("oid", SEARCH_ORG_OID_DEPTH1);

        List<ROrgClosure> orgClosure = sqlSearch.list();

        LOGGER.info("==============CLOSURE TABLE==========");
        for (ROrgClosure o : orgClosure) {
            LOGGER.info("=> A: {}, D: {}", new Object[]{o.getAncestor().toJAXB(prismContext, null),
                    o.getDescendant().toJAXB(prismContext, null)});
        }
        AssertJUnit.assertEquals(4, orgClosure.size());
        session.getTransaction().commit();
        session.close();

        ObjectQuery objectQuery = ObjectQuery.createObjectQuery(OrgFilter.createOrg(SEARCH_ORG_OID_DEPTH1, OrgFilter.Scope.ONE_LEVEL));
        objectQuery.setPaging(ObjectPaging.createPaging(null, null, ObjectType.F_NAME, OrderDirection.ASCENDING));

        List<PrismObject<ObjectType>> sOrgClosure = repositoryService.searchObjects(ObjectType.class, objectQuery, null, parentResult);

        for (PrismObject<ObjectType> u : sOrgClosure) {
            LOGGER.info("USER000 ======> {}", ObjectTypeUtil.toShortString(u.asObjectable()));
        }
        AssertJUnit.assertEquals(4, sOrgClosure.size());
    }

    @Test
    public void test008searchRootOrg() throws Exception {
        OperationResult parentResult = new OperationResult("test008searchRootOrg");

        ObjectQuery qSearch = ObjectQuery.createObjectQuery(OrgFilter.createRootOrg());
        qSearch.setPaging(ObjectPaging.createPaging(null, null, ObjectType.F_NAME, OrderDirection.ASCENDING));

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
        OperationResult opResult = new OperationResult("test011OrgFilter");

        ObjectQuery query = new ObjectQuery();
        PrismReferenceValue baseOrgRef = new PrismReferenceValue(ORG_F001_OID);
        ObjectFilter filter = OrgFilter.createOrg(baseOrgRef, OrgFilter.Scope.ONE_LEVEL);
        ObjectPaging paging = ObjectPaging.createEmptyPaging();
        paging.setOrderBy(ObjectType.F_NAME);
        query.setFilter(filter);
        query.setPaging(paging);

        List<PrismObject<ObjectType>> orgClosure = repositoryService.searchObjects(ObjectType.class, query, null, opResult);

        AssertJUnit.assertEquals(4, orgClosure.size());
    }

    private RUser getUser(String oid, int count, RUser other) {
        Session session = getFactory().openSession();
        session.beginTransaction();
        RUser user = (RUser) session.get(RUser.class, oid);
        AssertJUnit.assertEquals(count, user.getParentOrgRef().size());

        session.getTransaction().commit();
        session.close();

        return user;
    }

    private RObjectReference createRef(RUser user, String targetOid, String namespace, String localpart) {
        RParentOrgRef ref = new RParentOrgRef();
        ref.setOwner(user);
        ref.setTargetOid(targetOid);
        ref.setType(RObjectType.ORG);

        QName q = null;
        if (namespace != null && localpart != null) {
            q = new QName(namespace, localpart);
        }
        ref.setRelation(RUtil.qnameToString(q));

        return ref;
    }
}

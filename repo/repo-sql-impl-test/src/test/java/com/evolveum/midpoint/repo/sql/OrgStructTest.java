/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.EqualsFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrgFilter;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.repo.sql.data.common.RContainerType;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.RObjectReference;
import com.evolveum.midpoint.repo.sql.data.common.ROrg;
import com.evolveum.midpoint.repo.sql.data.common.ROrgClosure;
import com.evolveum.midpoint.repo.sql.data.common.RUser;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.data.common.id.RContainerId;
import com.evolveum.midpoint.repo.sql.data.common.type.RParentOrgRef;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projection;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.hibernate.sql.JoinType;
import org.hibernate.type.IntegerType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ContextConfiguration(locations = {"../../../../../ctx-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class OrgStructTest extends BaseSQLRepoTest {

    private static final File TEST_DIR = new File("src/test/resources/orgstruct");

    private static final String ORG_STRUCT_OBJECTS = TEST_DIR + "/org-monkey-island.xml";
    private static final String ORG_F001_OID = "00000000-8888-6666-0000-100000000001";
    private static final String ORG_PROJECT_ROOT_OID = "00000000-8888-6666-0000-200000000000";

    private static final String MODIFY_ORG_ADD_REF_OID = "00000000-8888-6666-0000-100000000005";
    private static final String MODIFY_ORG_ADD_REF_FILENAME = TEST_DIR + "/modify-orgStruct-add-orgref.xml";
    private static final String MODIFY_DELETE_REF_OID = "00000000-8888-6666-0000-100000000006";
    private static final String MODIFY_DELETE_REF_FILENAME = TEST_DIR + "/modify-orgStruct-delete-ref.xml";
    private static final String MODIFY_ORG_ADD_USER_FILENAME = TEST_DIR + "/modify-orgStruct-add-user.xml";

    private static final String QUERY_ORG_STRUCT_USER_UNBOUNDED = TEST_DIR + "/query-org-struct-user-unbounded.xml";
    private static final String QUERY_ORG_STRUCT_ORG_DEPTH = TEST_DIR + "/query-org-struct-org-depth.xml";

    private static final Trace LOGGER = TraceManager.getTrace(OrgStructTest.class);

    String ELAINE_OID;
    private static final String ELAINE_NAME = "elaine";

    @SuppressWarnings("unchecked")
    @Test
    public void test001addOrgStructObjects() throws Exception {

        LOGGER.info("===[ addOrgStruct ]===");

        List<PrismObject<? extends Objectable>> orgStruct = prismContext.getPrismDomProcessor().parseObjects(
                new File(ORG_STRUCT_OBJECTS));

        OperationResult opResult = new OperationResult("===[ addOrgStruct ]===");

        for (PrismObject<? extends Objectable> o : orgStruct) {
            repositoryService.addObject((PrismObject<ObjectType>) o, opResult);
        }

        List<PrismObject<OrgType>> orgTypes = repositoryService.searchObjects(OrgType.class, new ObjectQuery(), opResult);
        AssertJUnit.assertNotNull(orgTypes);
        AssertJUnit.assertEquals(9, orgTypes.size());

        OrgType orgF001 = repositoryService.getObject(OrgType.class, ORG_F001_OID, opResult).asObjectable();
        AssertJUnit.assertNotNull(orgF001);
        AssertJUnit.assertEquals("F0001", orgF001.getName().getOrig());
        AssertJUnit.assertEquals("The office of the most respectful Governor.", orgF001.getDescription());

//		PrismAsserts.assertEqualsPolyString("Governor Office", "Governor Office", orgF001.getDisplayName());
        AssertJUnit.assertEquals("0001", orgF001.getIdentifier());
        AssertJUnit.assertEquals(1, orgF001.getOrgType().size());
        AssertJUnit.assertEquals("functional", orgF001.getOrgType().get(0));
        AssertJUnit.assertEquals("CC0", orgF001.getCostCenter());
//		PrismAsserts.assertEqualsPolyString("The Governor's Mansion", "The Governor's Mansion", orgF001.getLocality());

        OrgType pRoot = repositoryService.getObject(OrgType.class, ORG_PROJECT_ROOT_OID, opResult).asObjectable();
        AssertJUnit.assertEquals("PRoot", pRoot.getName().getOrig());
        AssertJUnit.assertEquals("Project organizational structure root", pRoot.getDescription());
//		PrismAsserts.assertEqualsPolyString("Projects", "Projects", pRoot.getDisplayName());
        AssertJUnit.assertEquals(1, pRoot.getOrgType().size());
        AssertJUnit.assertEquals("project", pRoot.getOrgType().get(0));

//		QueryType query = QueryUtil.createNameQuery(ELAINE_NAME);
        ObjectQuery query = new ObjectQuery();
        PrismObjectDefinition userObjectDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
        query.setFilter(EqualsFilter.createEqual(null, userObjectDef, UserType.F_NAME, ELAINE_NAME));

        List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, opResult);

        AssertJUnit.assertNotNull(users);
        AssertJUnit.assertEquals(1, users.size());
        UserType elaine = users.get(0).asObjectable();
        LOGGER.info("--->elaine<----");
        LOGGER.info(prismContext.silentMarshalObject(elaine, LOGGER));
        AssertJUnit.assertEquals("Expected name elaine, but got " + elaine.getName().getOrig(), "elaine", elaine.getName().getOrig());
        AssertJUnit.assertEquals("Expected elaine has one org ref, but got " + elaine.getParentOrgRef().size(), 2, elaine.getParentOrgRef().size());
        AssertJUnit.assertEquals("Parent org ref oid not equal.", "00000000-8888-6666-0000-100000000001", elaine.getParentOrgRef().get(0).getOid());
//		AssertJUnit.assertEquals("Elaine is not manager, but: "
//				+ elaine.getParentOrgRef().get(0).getRelation().getLocalPart(), "manager", elaine.getParentOrgRef()
//				.get(0).getRelation().getLocalPart());
//		PrismAsserts.assertEqualsPolyString("Elaine Marley", "Elaine Marley", elaine.getFullName());
//		PrismAsserts.assertEqualsPolyString("Marley", "Marley", elaine.getFamilyName());
//		PrismAsserts.assertEqualsPolyString("Elaine", "Elaine", elaine.getGivenName());
//		PrismAsserts.assertEqualsPolyString("Governor", "Governor", elaine.getTitle());
        ELAINE_OID = elaine.getOid();

        LOGGER.info("==>after add<==");
        Session session = getFactory().openSession();
        session.beginTransaction();
        List<ROrgClosure> results = session.createQuery("from ROrgClosure").list();
        LOGGER.info("==============CLOSURE TABLE==========");
        AssertJUnit.assertEquals(65, results.size());
        for (ROrgClosure o : results) {
            LOGGER.info("=> A: {}, D: {}, depth: {}", new Object[]{o.getAncestor().toJAXB(prismContext),
                    o.getDescendant().toJAXB(prismContext), o.getDepth()});
        }

        // check descendants for F0001 org unit
        String ancestorOid = "00000000-8888-6666-0000-100000000001";
        String descendantOid = "00000000-8888-6666-0000-100000000006";
        Criteria criteria = session.createCriteria(ROrgClosure.class).createCriteria("ancestor", "anc")
                .setFetchMode("ancestor", FetchMode.JOIN).add(Restrictions.eq("anc.oid", ancestorOid));

        results = criteria.list();
        AssertJUnit.assertEquals(21, results.size());

        criteria = session.createCriteria(ROrgClosure.class).add(Restrictions.eq("depth", 2));
        Criteria ancCriteria = criteria.createCriteria("ancestor", "anc").setFetchMode("ancestor", FetchMode.JOIN)
                .add(Restrictions.eq("anc.oid", ancestorOid));
        Criteria descCriteria = criteria.createCriteria("descendant", "desc")
                .setFetchMode("descendant", FetchMode.JOIN).add(Restrictions.eq("desc.oid", descendantOid));

        results = criteria.list();
        AssertJUnit.assertEquals(2, results.size());

        session.getTransaction().commit();
        session.close();
    }

    @Test
    public void test002modifyOrgStructAdd() throws Exception {
        LOGGER.info("===[ modifyOrgStruct ]===");
        OperationResult opResult = new OperationResult("===[ modifyOrgStruct ]===");
        // test modification of org ref in another org type..

        ObjectModificationType modification = prismContext.getPrismJaxbProcessor().unmarshalObject(new File(MODIFY_ORG_ADD_REF_FILENAME),
                ObjectModificationType.class);
        ObjectDelta delta = DeltaConvertor.createObjectDelta(modification, OrgType.class, prismContext);

        Session session = getFactory().openSession();
        session.beginTransaction();
        String descendantOid = "00000000-8888-6666-0000-100000000005";
        Criteria criteria = session.createCriteria(ROrgClosure.class).createCriteria("descendant", "desc")
                .setFetchMode("descendant", FetchMode.JOIN).add(Restrictions.eq("desc.oid", descendantOid));

        List<ROrgClosure> results = criteria.list();

        LOGGER.info("before modify");
        for (ROrgClosure c : results) {
            LOGGER.info("{}\t{}\t{}", new Object[]{c.getAncestor().getOid(), c.getDescendant().getOid(), c.getDepth()});
        }
        AssertJUnit.assertEquals(3, results.size());
        session.getTransaction().commit();

        repositoryService.modifyObject(OrgType.class, MODIFY_ORG_ADD_REF_OID, delta.getModifications(), opResult);

        session.clear();
        session.beginTransaction();
        criteria = session.createCriteria(ROrgClosure.class).createCriteria("descendant", "desc")
                .setFetchMode("descendant", FetchMode.JOIN).add(Restrictions.eq("desc.oid", descendantOid));

        results = criteria.list();
        LOGGER.info("after modify");
        for (ROrgClosure c : results) {
            LOGGER.info("{}\t{}\t{}", new Object[]{c.getAncestor().getOid(), c.getDescendant().getOid(), c.getDepth()});
        }
        AssertJUnit.assertEquals(5, results.size());

        List<String> ancestors = new ArrayList<String>();
        ancestors.add("00000000-8888-6666-0000-100000000005");
        ancestors.add("00000000-8888-6666-0000-100000000003");
        ancestors.add("00000000-8888-6666-0000-100000000001");
        ancestors.add("00000000-8888-6666-0000-100000000002");

        for (String ancestorOid : ancestors) {

            criteria = session.createCriteria(ROrgClosure.class);
            Criteria ancCriteria = criteria.createCriteria("ancestor", "anc").setFetchMode("ancestor", FetchMode.JOIN)
                    .add(Restrictions.eq("anc.oid", ancestorOid));
            Criteria descCriteria = criteria.createCriteria("descendant", "desc")
                    .setFetchMode("descendant", FetchMode.JOIN).add(Restrictions.eq("desc.oid", descendantOid));

            results = criteria.list();
//			LOGGER.info("==============CLOSURE TABLE FOR EACH==========");
//			
//			for (ROrgClosure o : results) {
//				LOGGER.info("=> A: {}, D: {}, depth: {}", new Object[] { o.getAncestor().toJAXB(prismContext),
//						o.getDescendant().toJAXB(prismContext), o.getDepth() });
//
//			}
            if (ancestorOid.equals("00000000-8888-6666-0000-100000000001")) {
                AssertJUnit.assertEquals(2, results.size());
            } else {
                AssertJUnit.assertEquals(1, results.size());
                AssertJUnit.assertEquals(ancestorOid, results.get(0).getAncestor().getOid());
                AssertJUnit.assertEquals(descendantOid, results.get(0).getDescendant().getOid());
                int depth = -1;
                if (ancestorOid.equals("00000000-8888-6666-0000-100000000005")) {
                    depth = 0;
                } else if (ancestorOid.equals("00000000-8888-6666-0000-100000000003")) {
                    depth = 1;
                } else if (ancestorOid.equals("00000000-8888-6666-0000-100000000002")) {
                    depth = 1;
                }
                AssertJUnit.assertEquals(depth, results.get(0).getDepth());
            }
        }
        session.getTransaction().commit();
        session.close();
    }

    @Test
    public void test003modifyOrgStructDeleteRef() throws Exception {
        // test modification of org ref - delete org ref
        LOGGER.info("===[ modify delete org ref ]===");
        OperationResult opResult = new OperationResult("===[ modify delete org ref ]===");
        ObjectModificationType modification = prismContext.getPrismJaxbProcessor().unmarshalObject(new File(MODIFY_DELETE_REF_FILENAME),
                ObjectModificationType.class);
        ObjectDelta delta = DeltaConvertor.createObjectDelta(modification, OrgType.class, prismContext);

        repositoryService.modifyObject(OrgType.class, MODIFY_DELETE_REF_OID, delta.getModifications(), opResult);

        Session session = getFactory().openSession();
        session.beginTransaction();
        LOGGER.info("==>after modify - delete<==");
        List<ROrgClosure> results = session.createQuery("from ROrgClosure").list();
        AssertJUnit.assertEquals(53, results.size());
        LOGGER.info("==============CLOSURE TABLE==========");
        for (ROrgClosure o : results) {
            LOGGER.info("=> A: {}, D: {}, depth: {}", new Object[]{o.getAncestor().toJAXB(prismContext),
                    o.getDescendant().toJAXB(prismContext), o.getDepth()});

        }
        session.getTransaction().commit();
        session.close();
    }

    @Test
    public void test004modifyOrgStructAddUser() throws Exception {
        LOGGER.info("===[ modify add user to orgStruct ]===");
        OperationResult opResult = new OperationResult("===[ modify add user to orgStruct ]===");
        // test modification of org ref in another org type..
        ObjectModificationType modification = prismContext.getPrismJaxbProcessor().unmarshalObject(new File(MODIFY_ORG_ADD_USER_FILENAME),
                ObjectModificationType.class);
        ObjectDelta delta = DeltaConvertor.createObjectDelta(modification, UserType.class, prismContext);

        repositoryService.modifyObject(UserType.class, ELAINE_OID, delta.getModifications(), opResult);

        Session session = getFactory().openSession();
        session.beginTransaction();
        LOGGER.info("==>after modify - add user to org<==");
        List<ROrgClosure> results = session.createQuery("from ROrgClosure").list();
        AssertJUnit.assertEquals(56, results.size());
        LOGGER.info("==============CLOSURE TABLE==========");
        for (ROrgClosure o : results) {
            LOGGER.info("=> A: {}, D: {}, depth: {}", new Object[]{o.getAncestor().toJAXB(prismContext),
                    o.getDescendant().toJAXB(prismContext), o.getDepth()});

        }
        session.getTransaction().commit();
        session.close();
    }

    @Test
    public void test005deleteOrg() throws Exception {
        LOGGER.info("===[ deleteOrgStruct ]===");
        String orgOidToDelete = "00000000-8888-6666-0000-100000000002";
        OperationResult opResult = new OperationResult("===[ deleteOrgStruct ]===");
        repositoryService.deleteObject(OrgType.class, orgOidToDelete, opResult);
                                       
        Session session = getFactory().openSession();
        session.beginTransaction();
        LOGGER.info("==>after delete<==");
        List<ROrgClosure> results = session.createQuery("from ROrgClosure").list();
        AssertJUnit.assertEquals(50, results.size());
        LOGGER.info("==============CLOSURE TABLE==========");
        for (ROrgClosure o : results) {
            LOGGER.info("=> A: {}, D: {}, depth: {}", new Object[]{o.getAncestor().toJAXB(prismContext),
                    o.getDescendant().toJAXB(prismContext), o.getDepth()});

        }
        session.getTransaction().commit();
        session.close();
    }

    @Test
    public void test006searchOrgStructUserUnbounded() throws Exception {
        LOGGER.info("===[ SEARCH QUERY ]===");
        OperationResult parentResult = new OperationResult("search objects - org struct");
//        Session session = getFactory().openSession();
//        session.beginTransaction();

//        List<ROrgClosure> results = session.createQuery("from ROrgClosure").list();
//        LOGGER.info("==============CLOSURE TABLE==========");
//        for (ROrgClosure o : results) {
//            LOGGER.info("=> A: {}, D: {}, depth: {}", new Object[]{o.getAncestor().toJAXB(prismContext),
//                    o.getDescendant().toJAXB(prismContext), o.getDepth()});
//        }
//        session.getTransaction().commit();
//        session.close();

        // File file = new File(TEST_DIR + "/query-org-struct.xml")
        QueryType queryType = prismContext.getPrismJaxbProcessor().unmarshalObject(new File(QUERY_ORG_STRUCT_USER_UNBOUNDED), QueryType.class);
//		Document document = DOMUtil.parseFile(new File(QUERY_ORG_STRUCT_USER_UNBOUNDED));
//		Element filter = DOMUtil.listChildElements(document.getDocumentElement()).get(0);
//		QueryType query = QueryUtil.createQuery(filter);

        // List<>

//		ObjectQuery objectQuery = QueryConvertor.createObjectQuery(UserType.class, queryType, prismContext);
        ObjectQuery objectQuery = ObjectQuery.createObjectQuery(OrgFilter.createOrg("00000000-8888-6666-0000-100000000001"));

        List<PrismObject<ObjectType>> resultss = repositoryService.searchObjects(ObjectType.class, objectQuery, parentResult);
        for (PrismObject<ObjectType> u : resultss) {
            LOGGER.info("USER000 ======> {}", ObjectTypeUtil.toShortString(u.asObjectable()));
        }
    }

    @Test
    public void test007searchOrgStructOrgDepth() throws Exception {
        LOGGER.info("===[ SEARCH QUERY ]===");
        OperationResult parentResult = new OperationResult("search objects - org struct");
        Session session = getFactory().openSession();
        session.beginTransaction();

        List<ROrgClosure> results = session.createQuery("from ROrgClosure").list();
        LOGGER.info("==============CLOSURE TABLE==========");
        for (ROrgClosure o : results) {
            LOGGER.info("=> A: {}, D: {}, depth: {}", new Object[]{o.getAncestor().toJAXB(prismContext),
                    o.getDescendant().toJAXB(prismContext), o.getDepth()});
        }
        session.getTransaction().commit();
        session.close();

        // File file = new File(TEST_DIR + "/query-org-struct.xml");
//		Document document = DOMUtil.parseFile(new File(QUERY_ORG_STRUCT_ORG_DEPTH));
//		Element filter = DOMUtil.listChildElements(document.getDocumentElement()).get(0);
//		QueryType query = QueryUtil.createQuery(filter);
//		QueryType queryType = prismContext.getPrismJaxbProcessor().unmarshalObject(new File(QUERY_ORG_STRUCT_ORG_DEPTH), QueryType.class);
        ObjectQuery objectQuery = ObjectQuery.createObjectQuery(OrgFilter.createOrg("00000000-8888-6666-0000-100000000001", null, 1));
//		ObjectQuery objectQuery = QueryConvertor.createObjectQuery(UserType.class, queryType, prismContext);
        // List<>
        List<PrismObject<ObjectType>> resultss = repositoryService.searchObjects(ObjectType.class, objectQuery, parentResult);
        for (PrismObject<ObjectType> u : resultss) {
            LOGGER.info("USER000 ======> {}", ObjectTypeUtil.toShortString(u.asObjectable()));
        }
    }

    @Test
    public void test007searchRootOrg() throws Exception {
        LOGGER.info("===[ SEARCH ROOT QUERY ]===");
        OperationResult parentResult = new OperationResult("search root org struct");
//        Session session = getFactory().openSession();
//        session.beginTransaction();
//
//        List<ROrgClosure> results = session.createQuery("from ROrgClosure").list();
//        LOGGER.info("==============CLOSURE TABLE==========");
//        for (ROrgClosure o : results) {
//            LOGGER.info("=> A: {}, D: {}, depth: {}", new Object[]{o.getAncestor().toJAXB(prismContext),
//                    o.getDescendant().toJAXB(prismContext), o.getDepth()});
//        }
//        
//        Query rootOrgQuery = session.createQuery("select org from ROrg as org where org.oid in (select descendant.oid from ROrgClosure group by descendant.oid having count(descendant.oid)=1)");
//        List<ROrg> results = rootOrgQuery.list();
////      LOGGER.info("==============CLOSURE TABLE==========");
//      for (ROrg o : results) {
//          System.out.println("=> result: " + ObjectTypeUtil.toShortString(o.toJAXB(prismContext)));
//      }
        ObjectQuery q = ObjectQuery.createObjectQuery(OrgFilter.createRootOrg());
        List<PrismObject<OrgType>> rootOrgs = repositoryService.searchObjects(OrgType.class, q, parentResult);
        
        System.out.println("####################query results: "+ rootOrgs.size());
        for (PrismObject<OrgType> obj : rootOrgs){
        	
        		System.out.println("root org: "+ obj.dump());
        	}
        AssertJUnit.assertEquals("Expected two root organization units, but got: " + rootOrgs.size(), 2, rootOrgs.size());
//        session.getTransaction().commit();
//        session.close();

    }
    

    @Test
    public void test009modifyOrgStructRemoveUser() throws Exception {
        LOGGER.info("===[ modify remove org ref from user ]===");
        OperationResult opResult = new OperationResult("===[ modify add user to orgStruct ]===");
        // test modification of org ref in another org type..
//		ObjectModificationType modification = prismContext.getPrismJaxbProcessor().unmarshalObject(new File(MODIFY_ORG_ADD_USER_FILENAME),
//				ObjectModificationType.class);
//		ObjectDelta delta = DeltaConvertor.createObjectDelta(modification, UserType.class, prismContext);

        PrismReferenceValue prv = new PrismReferenceValue("00000000-8888-6666-0000-100000000006");
        prv.setTargetType(OrgType.COMPLEX_TYPE);
        ObjectDelta delta = ObjectDelta.createModificationDeleteReference(UserType.class, ELAINE_OID, UserType.F_PARENT_ORG_REF, prismContext, prv);
        repositoryService.modifyObject(UserType.class, ELAINE_OID, delta.getModifications(), opResult);

//		Session session = getFactory().openSession();
//		LOGGER.info("==>after modify - add user to org<==");
//		List<ROrgClosure> results = session.createQuery("from ROrgClosure").list();
//		AssertJUnit.assertEquals(56, results.size());
//		LOGGER.info("==============CLOSURE TABLE==========");
//		for (ROrgClosure o : results) {
//			LOGGER.info("=> A: {}, D: {}, depth: {}", new Object[] { o.getAncestor().toJAXB(prismContext),
//					o.getDescendant().toJAXB(prismContext), o.getDepth() });
//
//		}
//		session.close();

        UserType userElaine = repositoryService.getObject(UserType.class, ELAINE_OID, opResult).asObjectable();
        LOGGER.trace("elaine's og refs");
        for (ObjectReferenceType ort : userElaine.getParentOrgRef()) {
            LOGGER.trace("{}", ort);
            if (ort.getOid().equals("00000000-8888-6666-0000-100000000006")) {
                AssertJUnit.fail("expected that elain does not have reference on the org with oid: 00000000-8888-6666-0000-100000000006");
            }
        }
    }

    @Test
    public void test010megaSimpleTest() throws Exception {
        LOGGER.info("===[test010megaSimpleTest]===");
        try {
            RUser user = new RUser();
            user.setName(new RPolyString("vilko", "vilko"));
            Set<RObjectReference> refs = new HashSet<RObjectReference>();
            refs.add(createRef(user, "1", null, null));
            refs.add(createRef(user, "1", "namespace", "localpart"));
            refs.add(createRef(user, "6", null, null));
            user.setParentOrgRef(refs);

            Session session = getFactory().openSession();
            session.beginTransaction();
            RContainerId id = (RContainerId) session.save(user);
            session.getTransaction().commit();
            session.close();

            user = getUser(id.getOid(), 3, user);
            //todo asserts

            user = new RUser();
            user.setId(0L);
            user.setOid(id.getOid());
            user.setName(new RPolyString("vilko", "vilko"));
            refs = new HashSet<RObjectReference>();
            refs.add(createRef(user, "1", null, null));
            refs.add(createRef(user, "1", "namespace", "localpart"));
            user.setParentOrgRef(refs);

            session = getFactory().openSession();
            session.beginTransaction();
            session.merge(user);
            session.getTransaction().commit();
            session.close();

            user = getUser(id.getOid(), 2, user);
            //todo asserts
        } catch (Exception ex) {
            LOGGER.error("Exception occurred.", ex);
            throw ex;
        }
    }

    private RUser getUser(String oid, int count, RUser other) {
        Session session = getFactory().openSession();
        session.beginTransaction();
        RUser user = (RUser) session.get(RUser.class, new RContainerId(0L, oid));
        AssertJUnit.assertEquals(count, user.getParentOrgRef().size());

        session.getTransaction().commit();
        session.close();

        return user;
    }

    private RObjectReference createRef(RUser user, String targetOid, String namespace, String localpart) {
        RParentOrgRef ref = new RParentOrgRef();
        ref.setOwner(user);
        ref.setTargetOid(targetOid);
        ref.setType(RContainerType.ORG);
        if (namespace != null && localpart != null) {
            ref.setRelationLocalPart(localpart);
            ref.setRelationNamespace(namespace);
        }

        return ref;
    }
}

package com.evolveum.midpoint.repo.sql;

import java.io.File;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.sql.data.common.ROrgClosure;
import com.evolveum.midpoint.repo.sql.data.common.RUser;
import com.evolveum.midpoint.schema.DeltaConvertor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_2.ObjectModificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.UserType;
import com.evolveum.prism.xml.ns._public.query_2.QueryType;

@ContextConfiguration(locations = { "classpath:application-context-sql-no-server-mode-test.xml",
		"classpath:application-context-repository.xml", "classpath:application-context-repo-cache.xml",
		"classpath:application-context-configuration-sql-test.xml" })
public class OrgStructTest extends AbstractTestNGSpringContextTests {

	private static final File TEST_DIR = new File("src/test/resources/orgstruct");

	private static final String ORG_STRUCT_OBJECTS = TEST_DIR + "/org-monkey-island.xml";
	private static final String ORG_F001_OID = "00000000-8888-6666-0000-100000000001";
	private static final String ORG_PROJECT_ROOT_OID = "00000000-8888-6666-0000-200000000000";

	private static final String MODIFY_ORG_ADD_REF_OID = "00000000-8888-6666-0000-100000000005";
	private static final String MODIFY_ORG_ADD_REF_FILENAME = TEST_DIR + "/modify-orgStruct-add-orgref.xml";
	private static final String MODIFY_DELETE_REF_OID = "00000000-8888-6666-0000-100000000006";
	private static final String MODIFY_DELETE_REF_FILENAME = TEST_DIR + "/modify-orgStruct-replace.xml";
	private static final String MODIFY_ORG_ADD_USER_FILENAME = TEST_DIR + "/modify-orgStruct-add-user.xml";

	private static final String QUERY_ORG_STRUCT_USER_UNBOUNDED = TEST_DIR + "/query-org-struct-user-unbounded.xml";
	private static final String QUERY_ORG_STRUCT_ORG_DEPTH = TEST_DIR + "/query-org-struct-org-depth.xml";

	private static final Trace LOGGER = TraceManager.getTrace(AddGetObjectTest.class);

	String ELAINE_OID;
	private static final String ELAINE_NAME = "elaine";

	@Autowired(required = true)
	RepositoryService repositoryService;
	@Autowired(required = true)
	PrismContext prismContext;
	@Autowired
	SessionFactory factory;

	@Test
	public void test001addOrgStructObjects() throws Exception {

		LOGGER.info("===[ addOrgStruct ]===");
		List<PrismObject<? extends Objectable>> orgStruct = prismContext.getPrismDomProcessor().parseObjects(
				new File(ORG_STRUCT_OBJECTS));

		OperationResult opResult = new OperationResult("===[ addOrgStruct ]===");

		for (PrismObject o : orgStruct) {
			repositoryService.addObject(o, opResult);
		}

		List<PrismObject<OrgType>> orgTypes = repositoryService.searchObjects(OrgType.class, null, null, opResult);
		AssertJUnit.assertNotNull(orgTypes);
		AssertJUnit.assertEquals(9, orgTypes.size());

		OrgType orgF001 = repositoryService.getObject(OrgType.class, ORG_F001_OID, opResult).asObjectable();
		AssertJUnit.assertNotNull(orgF001);
		AssertJUnit.assertEquals("F0001", orgF001.getName());
		AssertJUnit.assertEquals("The office of the most respectful Governor.", orgF001.getDescription());

		PrismAsserts.assertEqualsPolyString("Governor Office", "Governor Office", orgF001.getDisplayName());
		AssertJUnit.assertEquals("0001", orgF001.getIdentifier());
		AssertJUnit.assertEquals(1, orgF001.getOrgType().size());
		AssertJUnit.assertEquals("functional", orgF001.getOrgType().get(0));
		AssertJUnit.assertEquals("CC0", orgF001.getCostCenter());
		PrismAsserts.assertEqualsPolyString("The Governor's Mansion", "The Governor's Mansion", orgF001.getLocality());

		OrgType pRoot = repositoryService.getObject(OrgType.class, ORG_PROJECT_ROOT_OID, opResult).asObjectable();
		AssertJUnit.assertEquals("PRoot", pRoot.getName());
		AssertJUnit.assertEquals("Project organizational structure root", pRoot.getDescription());
		PrismAsserts.assertEqualsPolyString("Projects", "Projects", pRoot.getDisplayName());
		AssertJUnit.assertEquals(1, pRoot.getOrgType().size());
		AssertJUnit.assertEquals("project", pRoot.getOrgType().get(0));

		QueryType query = QueryUtil.createNameQuery(ELAINE_NAME);

		List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, null, opResult);

		AssertJUnit.assertNotNull(users);
		AssertJUnit.assertEquals(1, users.size());
		UserType elaine = users.get(0).asObjectable();
		LOGGER.info("--->elaine<----");
		LOGGER.info(prismContext.silentMarshalObject(elaine));
		AssertJUnit.assertEquals("elaine", elaine.getName());
		AssertJUnit.assertEquals(1, elaine.getOrgRef().size());
		AssertJUnit.assertEquals("00000000-8888-6666-0000-100000000001", elaine.getOrgRef().get(0).getOid());
		AssertJUnit.assertEquals("manager", elaine.getOrgRef().get(0).getRelation().getLocalPart());
		PrismAsserts.assertEqualsPolyString("Elaine Marley", "Elaine Marley", elaine.getFullName());
		PrismAsserts.assertEqualsPolyString("Marley", "Marley", elaine.getFamilyName());
		PrismAsserts.assertEqualsPolyString("Elaine", "Elaine", elaine.getGivenName());
		PrismAsserts.assertEqualsPolyString("Governor", "Governor", elaine.getTitle());
		ELAINE_OID = elaine.getOid();

		LOGGER.info("==>after add<==");
		Session session = factory.openSession();
		List<ROrgClosure> results = session.createQuery("from ROrgClosure").list();
		LOGGER.info("==============CLOSURE TABLE==========");
		for (ROrgClosure o : results) {
			LOGGER.info("=> A: {}, D: {}, depth: {}", new Object[] { o.getAncestor().toJAXB(prismContext),
					o.getDescendant().toJAXB(prismContext), o.getDepth() });
		}

		session.close();
	}

	@Test
	public void test002modifyOrgStructAdd() throws Exception {
		LOGGER.info("===[ modifyOrgStruct ]===");
		OperationResult opResult = new OperationResult("===[ modifyOrgStruct ]===");
		// test modification of org ref in another org type..
		//
		ObjectModificationType modification = PrismTestUtil.unmarshalObject(new File(MODIFY_ORG_ADD_REF_FILENAME),
				ObjectModificationType.class);
		ObjectDelta delta = DeltaConvertor.createObjectDelta(modification, OrgType.class, prismContext);

		repositoryService.modifyObject(OrgType.class, MODIFY_ORG_ADD_REF_OID, delta.getModifications(), opResult);

		Session session = factory.openSession();
		LOGGER.info("==>after modify - add<==");
		List<ROrgClosure> results = session.createQuery("from ROrgClosure").list();
		LOGGER.info("==============CLOSURE TABLE==========");
		for (ROrgClosure o : results) {
			LOGGER.info("=> A: {}, D: {}, depth: {}", new Object[] { o.getAncestor().toJAXB(prismContext),
					o.getDescendant().toJAXB(prismContext), o.getDepth() });

		}
		session.close();
	}

	@Test
	public void test003modifyOrgStructDeleteRef() throws Exception {
		// test modification of org ref - delete org ref
		LOGGER.info("===[ modify delete org ref ]===");
		OperationResult opResult = new OperationResult("===[ modify delete org ref ]===");
		ObjectModificationType modification = PrismTestUtil.unmarshalObject(new File(MODIFY_DELETE_REF_FILENAME),
				ObjectModificationType.class);
		ObjectDelta delta = DeltaConvertor.createObjectDelta(modification, OrgType.class, prismContext);

		repositoryService.modifyObject(OrgType.class, MODIFY_DELETE_REF_OID, delta.getModifications(), opResult);

		Session session = factory.openSession();
		LOGGER.info("==>after modify - delete<==");
		List<ROrgClosure> results = session.createQuery("from ROrgClosure").list();
		LOGGER.info("==============CLOSURE TABLE==========");
		for (ROrgClosure o : results) {
			LOGGER.info("=> A: {}, D: {}, depth: {}", new Object[] { o.getAncestor().toJAXB(prismContext),
					o.getDescendant().toJAXB(prismContext), o.getDepth() });

		}
		session.close();
	}

	@Test
	public void test004modifyOrgStructAddUser() throws Exception {
		LOGGER.info("===[ modify add user to orgStruct ]===");
		OperationResult opResult = new OperationResult("===[ modify add user to orgStruct ]===");
		// test modification of org ref in another org type..
		ObjectModificationType modification = PrismTestUtil.unmarshalObject(new File(MODIFY_ORG_ADD_USER_FILENAME),
				ObjectModificationType.class);
		ObjectDelta delta = DeltaConvertor.createObjectDelta(modification, UserType.class, prismContext);

		repositoryService.modifyObject(UserType.class, ELAINE_OID, delta.getModifications(), opResult);

		Session session = factory.openSession();
		LOGGER.info("==>after modify - add user to org<==");
		List<ROrgClosure> results = session.createQuery("from ROrgClosure").list();
		LOGGER.info("==============CLOSURE TABLE==========");
		for (ROrgClosure o : results) {
			LOGGER.info("=> A: {}, D: {}, depth: {}", new Object[] { o.getAncestor().toJAXB(prismContext),
					o.getDescendant().toJAXB(prismContext), o.getDepth() });

		}
		session.close();
	}

	public void test005deleteOrg() throws Exception {
		LOGGER.info("===[ deleteOrgStruct ]===");
		String orgOidToDelete = "00000000-8888-6666-0000-100000000004";
		OperationResult opResult = new OperationResult("===[ deleteOrgStruct ]===");
		repositoryService.deleteObject(OrgType.class, orgOidToDelete, opResult);

		Session session = factory.openSession();
		LOGGER.info("==>after delete<==");
		List<ROrgClosure> results = session.createQuery("from ROrgClosure").list();
		LOGGER.info("==============CLOSURE TABLE==========");
		for (ROrgClosure o : results) {
			LOGGER.info("=> A: {}, D: {}, depth: {}", new Object[] { o.getAncestor().toJAXB(prismContext),
					o.getDescendant().toJAXB(prismContext), o.getDepth() });

		}
		session.close();
	}

	@Test
	public void test006searchOrgStructUserUnbounded() throws Exception {
		LOGGER.info("===[ SEARCH QUERY ]===");
		OperationResult parentResult = new OperationResult("search objects - org struct");
		Session session = factory.openSession();

		List<ROrgClosure> results = session.createQuery("from ROrgClosure").list();
		LOGGER.info("==============CLOSURE TABLE==========");
		for (ROrgClosure o : results) {
			LOGGER.info("=> A: {}, D: {}, depth: {}", new Object[] { o.getAncestor().toJAXB(prismContext),
					o.getDescendant().toJAXB(prismContext), o.getDepth() });
		}

		// File file = new File(TEST_DIR + "/query-org-struct.xml");
		Document document = DOMUtil.parseFile(new File(QUERY_ORG_STRUCT_USER_UNBOUNDED));
		Element filter = DOMUtil.listChildElements(document.getDocumentElement()).get(0);
		QueryType query = QueryUtil.createQuery(filter);

		// List<>
		List<PrismObject<UserType>> resultss = repositoryService.searchObjects(UserType.class, query, null, parentResult);
		for (PrismObject<UserType> u : resultss) {

			LOGGER.info("USER000 ======> {}", ObjectTypeUtil.toShortString(u.asObjectable()));

		}

	}
	
	@Test
	public void test007searchOrgStructOrgDepth() throws Exception {
		LOGGER.info("===[ SEARCH QUERY ]===");
		OperationResult parentResult = new OperationResult("search objects - org struct");
		Session session = factory.openSession();

		List<ROrgClosure> results = session.createQuery("from ROrgClosure").list();
		LOGGER.info("==============CLOSURE TABLE==========");
		for (ROrgClosure o : results) {
			LOGGER.info("=> A: {}, D: {}, depth: {}", new Object[] { o.getAncestor().toJAXB(prismContext),
					o.getDescendant().toJAXB(prismContext), o.getDepth() });
		}

		// File file = new File(TEST_DIR + "/query-org-struct.xml");
		Document document = DOMUtil.parseFile(new File(QUERY_ORG_STRUCT_ORG_DEPTH));
		Element filter = DOMUtil.listChildElements(document.getDocumentElement()).get(0);
		QueryType query = QueryUtil.createQuery(filter);

		// List<>
		List<PrismObject<OrgType>> resultss = repositoryService.searchObjects(OrgType.class, query, null, parentResult);
		for (PrismObject<OrgType> u : resultss) {

			LOGGER.info("USER000 ======> {}", ObjectTypeUtil.toShortString(u.asObjectable()));

		}

	}

}

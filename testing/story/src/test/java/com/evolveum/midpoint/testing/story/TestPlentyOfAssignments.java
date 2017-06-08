/*
 * Copyright (c) 2017 Evolveum
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
package com.evolveum.midpoint.testing.story;

import static org.testng.AssertJUnit.assertEquals;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyGroup;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.QueryJaxbConvertor;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSearchStrategyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchObjectExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 * MID-3938
 * 
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = {"classpath:ctx-story-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestPlentyOfAssignments extends AbstractStoryTest {
	
	public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "plenty-of-assignments");
	
	protected static final File RESOURCE_DUMMY_FILE = new File(TEST_DIR, "resource-dummy.xml");
	protected static final String RESOURCE_DUMMY_OID = "10000000-0000-0000-0000-000000000004";
	private static final String RESOURCE_DUMMY_NS = MidPointConstants.NS_RI;
	private static final QName RESOURCE_DUMMY_ASSOCIATION_GROUP_QNAME = new QName(RESOURCE_DUMMY_NS, "group");
	
	public static final File USER_CHEESE_FILE = new File(TEST_DIR, "user-cheese.xml");
	public static final String USER_CHEESE_OID = "9e796c76-45e0-11e7-9dfd-1792e56081d0";
	
	protected static final File USER_ALICE_FILE = new File(TEST_DIR, "user-alice.xml");
	protected static final String USER_ALICE_OID = "5e8fdb32-4c4c-11e7-86a8-9706c2f85f86";
	protected static final String USER_ALICE_USERNAME = "alice";
	protected static final String USER_ALICE_FULLNAME = "Alice";
	
	protected static final File USER_BOB_FILE = new File(TEST_DIR, "user-bob.xml");
	protected static final String USER_BOB_OID = "f5ffef5e-4b96-11e7-8e4c-1b0bc353a751";
	protected static final String USER_BOB_USERNAME = "bob";
	protected static final String USER_BOB_FULLNAME = "Bob";
	
	public static final File ROLE_BASIC_FILE = new File(TEST_DIR, "role-basic.xml");
	public static final String ROLE_BASIC_OID = "6909ff20-45e4-11e7-b0a3-0fe76ff4380e";

	private static final int NUMBER_OF_ORDINARY_ROLES = 2; // including superuser role
	private static final int NUMBER_OF_GENERATED_EMPTY_ROLES = 1000;
	private static final String GENERATED_EMPTY_ROLE_OID_FORMAT = "00000000-0000-ffff-2000-e0000000%04d";
	private static final int NUMBER_OF_GENERATED_DUMMY_ROLES = 100;
	private static final String GENERATED_DUMMY_ROLE_OID_FORMAT = "00000000-0000-ffff-2000-d0000000%04d";
	private static final int NUMBER_OF_GENERATED_DUMMY_GROUPS = 100;
	private static final String GENERATED_DUMMY_GROUP_ROLE_OID_FORMAT = "00000000-0000-ffff-2000-g0000000%04d";
	
	private static final int NUMBER_OF_CHEESE_ASSIGNMENTS_APPROVER = 600;
	private static final int NUMBER_OF_CHEESE_ASSIGNMENTS_OWNER = 400;
	private static final int NUMBER_OF_CHEESE_ASSIGNMENTS_ORDINARY = 1;
	private static final int NUMBER_OF_CHEESE_ASSIGNMENTS = NUMBER_OF_CHEESE_ASSIGNMENTS_APPROVER + NUMBER_OF_CHEESE_ASSIGNMENTS_OWNER + NUMBER_OF_CHEESE_ASSIGNMENTS_ORDINARY;

	private static final int NUMBER_OF_BOB_DUMMY_ROLE_ASSIGNMENTS = NUMBER_OF_GENERATED_DUMMY_ROLES;
	
	private static final Trace LOGGER = TraceManager.getTrace(TestPlentyOfAssignments.class);

	
	private RepoReadInspector inspector;
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		
		initDummyResourcePirate(null, RESOURCE_DUMMY_FILE, RESOURCE_DUMMY_OID, initTask, initResult);
		
		importObjectFromFile(ROLE_BASIC_FILE, initResult);
		
		ObjectFactory objectFactory = new ObjectFactory();
		generateRoles(NUMBER_OF_GENERATED_EMPTY_ROLES, "Empty Role %04d", GENERATED_EMPTY_ROLE_OID_FORMAT, null, initResult);
		generateRoles(NUMBER_OF_GENERATED_DUMMY_ROLES, "Dummy Role %04d", GENERATED_DUMMY_ROLE_OID_FORMAT, 
				(role,i) -> {
					ItemPathType attrPath = new ItemPathType(
							new ItemPath(new QName(RESOURCE_DUMMY_NS, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME)));
					JAXBElement<Object> evaluator = objectFactory.createValue(formatRum(i));
					role
						.beginInducement()
							.beginConstruction()
								.resourceRef(RESOURCE_DUMMY_OID, ResourceType.COMPLEX_TYPE)
								.kind(ShadowKindType.ACCOUNT)
								.beginAttribute()
									.ref(attrPath)
									.beginOutbound()
										.beginExpression()
											.expressionEvaluator(evaluator);
//					try {
//						IntegrationTestTools.displayXml("RRRRRRRRR", role.asPrismObject());
//					} catch (SchemaException e) {
//						throw new SystemException(e);
//					}
				}, initResult);
		
		inspector = new RepoReadInspector();
		InternalMonitor.setInspector(inspector);
	}

	private String generateRoleOid(String format, int num) {
		return String.format(format, num);
	}
	
	private String formatRum(int num) {
		return String.format("bottle of rum #%04d", num);
	}
	
	private String formatGroupName(int num) {
		return String.format("G#%04d", num);
	}
	
	@Test
    public void test000Sanity() throws Exception {
		final String TEST_NAME = "test000Sanity";
        displayTestTile(TEST_NAME);

        assertObjects(RoleType.class, NUMBER_OF_GENERATED_EMPTY_ROLES + NUMBER_OF_GENERATED_DUMMY_ROLES + NUMBER_OF_ORDINARY_ROLES);
        
        display("Repo reads", InternalMonitor.getRepositoryReadCount());
        display("Object compares", InternalMonitor.getPrismObjectCompareCount());
	}
	
	@Test
    public void test100AddCheese() throws Exception {
		final String TEST_NAME = "test100AddCheese";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> cheeseBefore = prepareCheese();
        display("Cheese before", assignmentSummary(cheeseBefore));
        
        inspector.reset();
        rememberPrismObjectCompareCount();
        rememberRepositoryReadCount();
        long startMillis = System.currentTimeMillis();
        
        // WHEN
        displayWhen(TEST_NAME);
        
        addObject(cheeseBefore, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        long endMillis = System.currentTimeMillis();
        assertSuccess(result);
        
        display("Added cheese in "+(endMillis - startMillis)+"ms ("+((endMillis - startMillis)/NUMBER_OF_CHEESE_ASSIGNMENTS)+"ms per assignment)");
        
        PrismObject<UserType> cheeseAfter = getUser(USER_CHEESE_OID);
        display("Cheese after", assignmentSummary(cheeseAfter));
        assertCheeseRoleMembershipRef(cheeseAfter);
        
        display("Repo reads", InternalMonitor.getRepositoryReadCount());
        display("Object compares", InternalMonitor.getPrismObjectCompareCount());

        display("Inspector", inspector);
        
//        inspector.assertRead(RoleType.class, NUMBER_OF_CHEESE_ASSIGNMENTS);
//        assertRepositoryReadCount(xxx); // may be influenced by tasks
        
        assertPrismObjectCompareCount(0);
	}
	
	
	@Test
    public void test110RecomputeCheese() throws Exception {
		final String TEST_NAME = "test110RecomputeCheese";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> cheeseBefore = prepareCheese();
        display("Cheese before", assignmentSummary(cheeseBefore));
        
        inspector.reset();
        rememberPrismObjectCompareCount();
        rememberRepositoryReadCount();
        long startMillis = System.currentTimeMillis();
        
        // WHEN
        displayWhen(TEST_NAME);
        
        recomputeUser(USER_CHEESE_OID, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        long endMillis = System.currentTimeMillis();
        assertSuccess(result);
        
        display("Recomputed cheese in "+(endMillis - startMillis)+"ms ("+((endMillis - startMillis)/NUMBER_OF_CHEESE_ASSIGNMENTS)+"ms per assignment)");
        
        PrismObject<UserType> cheeseAfter = getUser(USER_CHEESE_OID);
        display("Cheese after", assignmentSummary(cheeseAfter));
        assertCheeseRoleMembershipRef(cheeseAfter);
        
        display("Repo reads", InternalMonitor.getRepositoryReadCount());
        display("Object compares", InternalMonitor.getPrismObjectCompareCount());
        
        display("Inspector", inspector);

        inspector.assertRead(RoleType.class, 1);
//        assertRepositoryReadCount(4); // may be influenced by tasks
        assertPrismObjectCompareCount(0);
	}
	
	@Test
    public void test120CheesePreviewChanges() throws Exception {
		final String TEST_NAME = "test120CheesePreviewChanges";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> cheeseBefore = prepareCheese();
        display("Cheese before", assignmentSummary(cheeseBefore));
        
        ObjectDelta<UserType> delta = cheeseBefore.createModifyDelta();
        delta.addModificationReplaceProperty(UserType.F_EMPLOYEE_NUMBER, "123");
        
        inspector.reset();
        rememberPrismObjectCompareCount();
        rememberRepositoryReadCount();
        long startMillis = System.currentTimeMillis();
        
        // WHEN
        displayWhen(TEST_NAME);
        
		ModelContext<ObjectType> modelContext = modelInteractionService.previewChanges(MiscSchemaUtil.createCollection(delta), null, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        long endMillis = System.currentTimeMillis();
        assertSuccess(result);
        
        display("Preview cheese in "+(endMillis - startMillis)+"ms ("+((endMillis - startMillis)/NUMBER_OF_CHEESE_ASSIGNMENTS)+"ms per assignment)");
        
        PrismObject<UserType> cheeseAfter = getUser(USER_CHEESE_OID);
        display("Cheese after", assignmentSummary(cheeseAfter));
        assertCheeseRoleMembershipRef(cheeseAfter);
        
        display("Repo reads", InternalMonitor.getRepositoryReadCount());
        display("Object compares", InternalMonitor.getPrismObjectCompareCount());
        
        display("Inspector", inspector);

        inspector.assertRead(RoleType.class, 1);
//        assertRepositoryReadCount(4); // may be influenced by tasks
        assertPrismObjectCompareCount(0);
	}

	private PrismObject<UserType> prepareCheese() throws Exception {
		PrismObject<UserType> cheese = PrismTestUtil.parseObject(USER_CHEESE_FILE);
		addAssignments(cheese, GENERATED_EMPTY_ROLE_OID_FORMAT, SchemaConstants.ORG_APPROVER, 0, NUMBER_OF_CHEESE_ASSIGNMENTS_APPROVER);
		addAssignments(cheese, GENERATED_EMPTY_ROLE_OID_FORMAT, SchemaConstants.ORG_OWNER, NUMBER_OF_CHEESE_ASSIGNMENTS_APPROVER, NUMBER_OF_CHEESE_ASSIGNMENTS_OWNER);
		return cheese;
	}
	
	private void assertCheeseRoleMembershipRef(PrismObject<UserType> cheese) {
		
		assertRoleMembershipRefs(cheese, GENERATED_EMPTY_ROLE_OID_FORMAT, SchemaConstants.ORG_APPROVER, 0, NUMBER_OF_CHEESE_ASSIGNMENTS_APPROVER);
		assertRoleMembershipRefs(cheese, GENERATED_EMPTY_ROLE_OID_FORMAT, SchemaConstants.ORG_OWNER, NUMBER_OF_CHEESE_ASSIGNMENTS_APPROVER, NUMBER_OF_CHEESE_ASSIGNMENTS_OWNER);
		
		assertRoleMembershipRef(cheese, ROLE_BASIC_OID, SchemaConstants.ORG_DEFAULT);
		
		assertRoleMembershipRefs(cheese, NUMBER_OF_CHEESE_ASSIGNMENTS);
	}
	
	
	/**
	 * Create dummy groups that can be used for associationTargetSearch later on.
	 * Create them from midPoint so they have shadows.
	 * 
	 * MID-3938 #8
	 */
	@Test
    public void test200DummyGroups() throws Exception {
		final String TEST_NAME = "test200DummyGroups";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        displayWhen(TEST_NAME);

        PrismObjectDefinition<ShadowType> shadowDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ShadowType.class);
        PrismObjectDefinition<RoleType> roleDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(RoleType.class);
        RefinedResourceSchema rSchema = RefinedResourceSchema.getRefinedSchema(getDummyResourceObject());
        ObjectClassComplexTypeDefinition rOcDef = rSchema.findObjectClassDefinition(getDummyResourceController().getGroupObjectClass());
        
        ObjectFactory objectFactory = new ObjectFactory();
        ItemPath nameAttributePath = new ItemPath(ShadowType.F_ATTRIBUTES, SchemaConstants.ICFS_NAME);
        for (int i=0; i<NUMBER_OF_GENERATED_DUMMY_GROUPS; i++) {
        	PrismObject<ShadowType> shadow = shadowDef.instantiate();
        	ShadowType shadowType = shadow.asObjectable();
        	shadowType
        		.resourceRef(RESOURCE_DUMMY_OID, ResourceType.COMPLEX_TYPE)
        		.objectClass(rOcDef.getTypeName());
        	ResourceAttributeContainer attributesContainer = ShadowUtil.getOrCreateAttributesContainer(shadow, rOcDef);
        	ResourceAttribute<String> nameAttribute = attributesContainer.findOrCreateAttribute(SchemaConstants.ICFS_NAME);
        	String groupName = formatGroupName(i);
        	nameAttribute.setRealValue(groupName);
        	display("Group shadow "+i, shadow);
        	addObject(shadow, task, result);
        	
        	PrismObject<RoleType> role = roleDef.instantiate();
        	RoleType roleType = role.asObjectable();
        	ItemPathType assPath = new ItemPathType(new ItemPath(RESOURCE_DUMMY_ASSOCIATION_GROUP_QNAME));
			SearchObjectExpressionEvaluatorType associationTargetSearchType = new SearchObjectExpressionEvaluatorType();
			EqualFilter<String> filter = EqualFilter.createEqual(nameAttributePath, null, null, prismContext, groupName);
			
			SearchFilterType filterType = QueryJaxbConvertor.createSearchFilterType(filter, prismContext);
			associationTargetSearchType.setFilter(filterType);
			associationTargetSearchType.setSearchStrategy(ObjectSearchStrategyType.IN_REPOSITORY);
			JAXBElement<SearchObjectExpressionEvaluatorType> evaluator = objectFactory.createAssociationTargetSearch(associationTargetSearchType);
        	roleType
        		.oid(generateRoleOid(GENERATED_DUMMY_GROUP_ROLE_OID_FORMAT, i))
        		.name(String.format("Group role %04d", i))
        		.beginInducement()
					.beginConstruction()
					.resourceRef(RESOURCE_DUMMY_OID, ResourceType.COMPLEX_TYPE)
					.kind(ShadowKindType.ACCOUNT)
					.beginAssociation()
						.ref(assPath)
						.beginOutbound()
							.beginExpression()
								.expressionEvaluator(evaluator);
			try {
				IntegrationTestTools.displayXml("RRRRRRRRR group", role);
			} catch (SchemaException e) {
				throw new SystemException(e);
			}
			addObject(role, task, result);
        }
                
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        Collection<DummyGroup> dummyGroups = getDummyResource().listGroups();
        assertEquals("Wrong number of dummy groups", NUMBER_OF_GENERATED_DUMMY_GROUPS, dummyGroups.size());
        assertShadows(NUMBER_OF_GENERATED_DUMMY_GROUPS);
        
        assertObjects(RoleType.class, NUMBER_OF_GENERATED_EMPTY_ROLES + NUMBER_OF_GENERATED_DUMMY_ROLES + NUMBER_OF_ORDINARY_ROLES + NUMBER_OF_GENERATED_DUMMY_GROUPS);
	}
	
	/**
	 * MID-3938 #8
	 */
	@Test
    public void test210AddBob() throws Exception {
		final String TEST_NAME = "test210AddBob";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = PrismTestUtil.parseObject(USER_BOB_FILE);
        addAssignments(userBefore, GENERATED_DUMMY_ROLE_OID_FORMAT, null, 0, NUMBER_OF_BOB_DUMMY_ROLE_ASSIGNMENTS);
        display("User before", assignmentSummary(userBefore));
        
        inspector.reset();
        rememberPrismObjectCompareCount();
        rememberRepositoryReadCount();
        long startMillis = System.currentTimeMillis();
        
        // WHEN
        displayWhen(TEST_NAME);
        
        addObject(userBefore, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        long endMillis = System.currentTimeMillis();
        assertSuccess(result);
        
        display("Added bob in "+(endMillis - startMillis)+"ms ("+((endMillis - startMillis)/NUMBER_OF_BOB_DUMMY_ROLE_ASSIGNMENTS)+"ms per assignment)");
        
        PrismObject<UserType> userAfter = getUser(USER_BOB_OID);
        display("User after", assignmentSummary(userAfter));
        assertBobRoleMembershipRef(userAfter);
        
        display("Repo reads", InternalMonitor.getRepositoryReadCount());
        display("Object compares", InternalMonitor.getPrismObjectCompareCount());

        display("Inspector", inspector);
        
        // TODO: why *3 ???
        inspector.assertRead(RoleType.class, NUMBER_OF_BOB_DUMMY_ROLE_ASSIGNMENTS * 3);
//        assertRepositoryReadCount(xxx); // may be influenced by tasks
        
        assertPrismObjectCompareCount(0);
        
        assertBobDummy(NUMBER_OF_BOB_DUMMY_ROLE_ASSIGNMENTS);
	}
	
	/**
	 * MID-3938 #8
	 */
	@Test
    public void test212RecomputeBob() throws Exception {
		final String TEST_NAME = "test212RecomputeBob";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
                
        inspector.reset();
        rememberPrismObjectCompareCount();
        rememberRepositoryReadCount();
        long startMillis = System.currentTimeMillis();
        
        // WHEN
        displayWhen(TEST_NAME);
        
        recomputeUser(USER_BOB_OID, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        long endMillis = System.currentTimeMillis();
        assertSuccess(result);
        
        display("Recomputed bob in "+(endMillis - startMillis)+"ms ("+((endMillis - startMillis)/NUMBER_OF_BOB_DUMMY_ROLE_ASSIGNMENTS)+"ms per assignment)");
        
        PrismObject<UserType> userAfter = getUser(USER_BOB_OID);
        display("User after", assignmentSummary(userAfter));
        assertBobRoleMembershipRef(userAfter);
        
        display("Repo reads", InternalMonitor.getRepositoryReadCount());
        display("Object compares", InternalMonitor.getPrismObjectCompareCount());

        display("Inspector", inspector);
        
        inspector.assertRead(RoleType.class, NUMBER_OF_BOB_DUMMY_ROLE_ASSIGNMENTS);
//        assertRepositoryReadCount(xxx); // may be influenced by tasks
        
        assertPrismObjectCompareCount(0);
        
        assertBobDummy(NUMBER_OF_BOB_DUMMY_ROLE_ASSIGNMENTS);
	}
	
	/**
	 * MID-3938 #8
	 */
	@Test
    public void test2124ReconcileBob() throws Exception {
		final String TEST_NAME = "test212RecomputeBob";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
                
        inspector.reset();
        rememberPrismObjectCompareCount();
        rememberRepositoryReadCount();
        long startMillis = System.currentTimeMillis();
        
        // WHEN
        displayWhen(TEST_NAME);
        
        reconcileUser(USER_BOB_OID, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        long endMillis = System.currentTimeMillis();
        assertSuccess(result);
        
        display("Reconciled bob in "+(endMillis - startMillis)+"ms ("+((endMillis - startMillis)/NUMBER_OF_BOB_DUMMY_ROLE_ASSIGNMENTS)+"ms per assignment)");
        
        PrismObject<UserType> userAfter = getUser(USER_BOB_OID);
        display("User after", assignmentSummary(userAfter));
        assertBobRoleMembershipRef(userAfter);
        
        display("Repo reads", InternalMonitor.getRepositoryReadCount());
        display("Object compares", InternalMonitor.getPrismObjectCompareCount());

        display("Inspector", inspector);
        
        inspector.assertRead(RoleType.class, NUMBER_OF_BOB_DUMMY_ROLE_ASSIGNMENTS);
//        assertRepositoryReadCount(xxx); // may be influenced by tasks
        
        assertPrismObjectCompareCount(0);
        
        assertBobDummy(NUMBER_OF_BOB_DUMMY_ROLE_ASSIGNMENTS);
	}

	private void assertBobRoleMembershipRef(PrismObject<UserType> user) {
		
		assertRoleMembershipRefs(user, GENERATED_DUMMY_ROLE_OID_FORMAT, null, 0, NUMBER_OF_BOB_DUMMY_ROLE_ASSIGNMENTS);
		assertRoleMembershipRefs(user, NUMBER_OF_BOB_DUMMY_ROLE_ASSIGNMENTS);
	}
	
	private void assertBobDummy(int expectedBottlesOfRum) throws Exception {
        DummyAccount dummyAccount = assertDummyAccount(null, USER_BOB_USERNAME, USER_BOB_FULLNAME, true);
        display("Dummy account", dummyAccount);
        List<Object> expectedDrinks = new ArrayList<>(expectedBottlesOfRum + 1);
        for (int i = 0; i < expectedBottlesOfRum; i++) {
        	expectedDrinks.add(formatRum(i));
        }
        expectedDrinks.add("barrel of rum");
		assertDummyAccountAttribute(null, USER_BOB_USERNAME, DummyResourceContoller.DUMMY_ACCOUNT_ATTRIBUTE_DRINK_NAME, 
        		expectedDrinks.toArray());
	}
	
	/**
	 * MID-3938 #8
	 */
	@Test
    public void test220AddAlice() throws Exception {
		final String TEST_NAME = "test220AddAlice";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> userBefore = PrismTestUtil.parseObject(USER_ALICE_FILE);
        addAssignments(userBefore, GENERATED_DUMMY_GROUP_ROLE_OID_FORMAT, null, 0, NUMBER_OF_GENERATED_DUMMY_GROUPS);
        display("User before", assignmentSummary(userBefore));
        
        inspector.reset();
        rememberPrismObjectCompareCount();
        rememberRepositoryReadCount();
        long startMillis = System.currentTimeMillis();
        
        // WHEN
        displayWhen(TEST_NAME);
        
        addObject(userBefore, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        long endMillis = System.currentTimeMillis();
        assertSuccess(result);
        
        display("Added alice in "+(endMillis - startMillis)+"ms ("+((endMillis - startMillis)/NUMBER_OF_GENERATED_DUMMY_GROUPS)+"ms per assignment)");
        
        PrismObject<UserType> userAfter = getUser(USER_ALICE_OID);
        display("User after", assignmentSummary(userAfter));
        assertAliceRoleMembershipRef(userAfter);
        
        display("Repo reads", InternalMonitor.getRepositoryReadCount());
        display("Object compares", InternalMonitor.getPrismObjectCompareCount());

        display("Inspector", inspector);
        
        // TODO: why *3 ???
        inspector.assertRead(RoleType.class, NUMBER_OF_GENERATED_DUMMY_GROUPS * 3);
//        assertRepositoryReadCount(xxx); // may be influenced by tasks
        
        assertPrismObjectCompareCount(0);
        
        assertAliceDummy(NUMBER_OF_GENERATED_DUMMY_GROUPS);
	}
	
	/**
	 * MID-3938 #8
	 */
	@Test
    public void test222RecomputeAlice() throws Exception {
		final String TEST_NAME = "test222RecomputeAlice";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        inspector.reset();
        rememberPrismObjectCompareCount();
        rememberRepositoryReadCount();
        long startMillis = System.currentTimeMillis();
        
        // WHEN
        displayWhen(TEST_NAME);
        
        recomputeUser(USER_ALICE_OID, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        long endMillis = System.currentTimeMillis();
        assertSuccess(result);
        
        display("Recomputed alice in "+(endMillis - startMillis)+"ms ("+((endMillis - startMillis)/NUMBER_OF_GENERATED_DUMMY_GROUPS)+"ms per assignment)");
        
        PrismObject<UserType> userAfter = getUser(USER_ALICE_OID);
        display("User after", assignmentSummary(userAfter));
        assertAliceRoleMembershipRef(userAfter);
        
        display("Repo reads", InternalMonitor.getRepositoryReadCount());
        display("Object compares", InternalMonitor.getPrismObjectCompareCount());

        display("Inspector", inspector);
        
        inspector.assertRead(RoleType.class, NUMBER_OF_GENERATED_DUMMY_GROUPS);
//        assertRepositoryReadCount(xxx); // may be influenced by tasks
        
        assertPrismObjectCompareCount(0);
        
        assertAliceDummy(NUMBER_OF_GENERATED_DUMMY_GROUPS);
	}
	
	/**
	 * MID-3938 #8
	 */
	@Test
    public void test224ReconcileAlice() throws Exception {
		final String TEST_NAME = "test224ReconcileAlice";
        displayTestTile(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
        inspector.reset();
        rememberPrismObjectCompareCount();
        rememberRepositoryReadCount();
        long startMillis = System.currentTimeMillis();
        
        // WHEN
        displayWhen(TEST_NAME);
        
        reconcileUser(USER_ALICE_OID, task, result);
        
        // THEN
        displayThen(TEST_NAME);
        long endMillis = System.currentTimeMillis();
        assertSuccess(result);
        
        display("Reconciled alice in "+(endMillis - startMillis)+"ms ("+((endMillis - startMillis)/NUMBER_OF_GENERATED_DUMMY_GROUPS)+"ms per assignment)");
        
        PrismObject<UserType> userAfter = getUser(USER_ALICE_OID);
        display("User after", assignmentSummary(userAfter));
        assertAliceRoleMembershipRef(userAfter);
        
        display("Repo reads", InternalMonitor.getRepositoryReadCount());
        display("Object compares", InternalMonitor.getPrismObjectCompareCount());

        display("Inspector", inspector);
        
        inspector.assertRead(RoleType.class, NUMBER_OF_GENERATED_DUMMY_GROUPS);
//        assertRepositoryReadCount(xxx); // may be influenced by tasks
        
        assertPrismObjectCompareCount(0);
        
        assertAliceDummy(NUMBER_OF_GENERATED_DUMMY_GROUPS);
	}
	
	private void assertAliceRoleMembershipRef(PrismObject<UserType> user) {
		
		assertRoleMembershipRefs(user, GENERATED_DUMMY_GROUP_ROLE_OID_FORMAT, null, 0, NUMBER_OF_GENERATED_DUMMY_GROUPS);
		assertRoleMembershipRefs(user, NUMBER_OF_GENERATED_DUMMY_GROUPS);
	}
	
	private void assertAliceDummy(int expectedGroups) throws Exception {
        DummyAccount dummyAccount = assertDummyAccount(null, USER_ALICE_USERNAME, USER_ALICE_FULLNAME, true);
        display("Dummy account", dummyAccount);
        for (int i = 0; i < expectedGroups; i++) {
        	assertDummyGroupMember(null, formatGroupName(i), USER_ALICE_USERNAME);
        }
	}
	
	private void addAssignments(PrismObject<UserType> user, String roleOidFormat, QName relation, int offset, int num) {
		UserType userType = user.asObjectable();
		for (int i = 0; i < num; i++) {
			AssignmentType assignmentType = new AssignmentType();
			String oid = generateRoleOid(roleOidFormat, offset + i);
			assignmentType.targetRef(oid, RoleType.COMPLEX_TYPE, relation);
			userType.getAssignment().add(assignmentType);
		}
	}

	private void assertRoleMembershipRefs(PrismObject<UserType> user, String roleOidFormat, QName relation, int offset, int num) {
		for (int i = 0; i < num; i++) {
			assertRoleMembershipRef(user, roleOidFormat, relation, offset + i);
		}
	}

	private void assertRoleMembershipRef(PrismObject<UserType> user, String roleOidFormat, QName relation, int num) {
		assertRoleMembershipRef(user, generateRoleOid(roleOidFormat, num), relation);
	}
	
	private void assertRoleMembershipRef(PrismObject<UserType> user, String roleOid, QName relation) {
		List<ObjectReferenceType> roleMembershipRefs = user.asObjectable().getRoleMembershipRef();
		for (ObjectReferenceType roleMembershipRef: roleMembershipRefs) {
			if (ObjectTypeUtil.referenceMatches(roleMembershipRef, roleOid, RoleType.COMPLEX_TYPE, relation)) {
				return;
			}
		}
		fail("Cannot find membership of role "+roleOid+" ("+relation.getLocalPart()+") in "+user);
	}

}

/*
 * Copyright (c) 2018 Evolveum
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
import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinitionImpl;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.QueryJaxbConvertor;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
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
import com.evolveum.midpoint.util.DOMUtil;
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
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * @author Radovan Semancik
 */
@ContextConfiguration(locations = {"classpath:ctx-story-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestOperationPerf extends AbstractStoryTest {

	public static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "operation-perf");
	
	protected static final File USER_ALICE_FILE = new File(TEST_DIR, "user-alice.xml");
	protected static final String USER_ALICE_OID = "a077357a-1c5f-11e8-ad16-af1b03cecee9";
	
	protected static final File OBJECT_TEMPLATE_USER_FILE = new File(TEST_DIR, "object-template-user.xml");
	protected static final String OBJECT_TEMPLATE_USER_OID = "995aa1a6-1c5e-11e8-8d2f-6784dbc320a9";

	private static final int NUMBER_OF_ORDINARY_ROLES = 1; // including superuser role
	private static final int NUMBER_OF_GENERATED_EMPTY_ROLES = 1000;
	private static final String GENERATED_EMPTY_ROLE_OID_FORMAT = "00000000-0000-ffff-2000-e0000000%04d";
	private static final String GENERATED_EMPTY_ROLE_NAME_FORMAT = "Empty Role %04d";
	
	private static final int NUMBER_OF_USER_EXTENSION_PROPERTIES = 30;
	private static final String USER_EXTENSION_NS = "http://midpoint.evolveum.com/xml/ns/samples/gen";
	private static final String USER_EXTENSION_PROPERTY_NAME_FORMAT = "prop%04d";

	private static final Trace LOGGER = TraceManager.getTrace(TestOperationPerf.class);

	private CountingInspector inspector;

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);

		generateRoles(NUMBER_OF_GENERATED_EMPTY_ROLES, GENERATED_EMPTY_ROLE_NAME_FORMAT, GENERATED_EMPTY_ROLE_OID_FORMAT, null, initResult);

		repoAddObjectFromFile(OBJECT_TEMPLATE_USER_FILE, initResult);
		setDefaultObjectTemplate(UserType.COMPLEX_TYPE, OBJECT_TEMPLATE_USER_OID, initResult);
		
		inspector = new CountingInspector();
		InternalMonitor.setInspector(inspector);
		
		InternalMonitor.setCloneTimingEnabled(false);
		
		extendUserSchema(NUMBER_OF_USER_EXTENSION_PROPERTIES);
	}

	private void extendUserSchema(int numberOfProperties) {
		PrismObjectDefinition<UserType> userDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
		PrismContainerDefinitionImpl<?> userExtensionDefinition = (PrismContainerDefinitionImpl<?>) userDefinition.getExtensionDefinition();
		
		for (int i=0; i<numberOfProperties; i++) {
			String propName = String.format(USER_EXTENSION_PROPERTY_NAME_FORMAT, i);
			userExtensionDefinition.createPropertyDefinition(
					new QName(USER_EXTENSION_NS, propName), DOMUtil.XSD_STRING);
		}
		
		display("User extension definition", userExtensionDefinition);
	}

	@Test
    public void test000Sanity() throws Exception {
		final String TEST_NAME = "test000Sanity";
        displayTestTitle(TEST_NAME);

        assertObjects(RoleType.class, NUMBER_OF_GENERATED_EMPTY_ROLES + NUMBER_OF_ORDINARY_ROLES);

        display("Repo reads", InternalMonitor.getCount(InternalCounters.REPOSITORY_READ_COUNT));
        display("Object compares", InternalMonitor.getCount(InternalCounters.PRISM_OBJECT_COMPARE_COUNT));
	}

	@Test
    public void test100AddAlice() throws Exception {
		final String TEST_NAME = "test100AddAlice";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = parseObject(USER_ALICE_FILE);
        populateUserExtension(userBefore, NUMBER_OF_USER_EXTENSION_PROPERTIES);
        setRandomOrganizations(userBefore, 1);
        display("User before", userBefore);

        inspector.reset();
        InternalMonitor.reset();
        rememberCounter(InternalCounters.PRISM_OBJECT_COMPARE_COUNT);
        rememberCounter(InternalCounters.REPOSITORY_READ_COUNT);
        rememberCounter(InternalCounters.PRISM_OBJECT_CLONE_COUNT);
        long startMillis = System.currentTimeMillis();

        // WHEN
        displayWhen(TEST_NAME);

        addObject(userBefore, task, result);

        // THEN
        displayThen(TEST_NAME);
        long endMillis = System.currentTimeMillis();
        assertSuccess(result);
        
        display("Added alice in "+(endMillis - startMillis)+"ms");

        display("Inspector", inspector);
        display("Internal counters", InternalMonitor.debugDumpStatic(1));
        
        PrismObject<UserType> userAfter = getUser(USER_ALICE_OID);
        display("User after", userAfter);
        assertAssignments(userAfter, 1);

        display("Repo reads", InternalMonitor.getCount(InternalCounters.REPOSITORY_READ_COUNT));
        display("Object compares", InternalMonitor.getCount(InternalCounters.PRISM_OBJECT_COMPARE_COUNT));

        assertCounterIncrement(InternalCounters.PRISM_OBJECT_COMPARE_COUNT, 0);
	}

	private void populateUserExtension(PrismObject<UserType> user,
			int numberOfProperties) throws SchemaException {
		PrismContainer<?> extension = user.getExtension();
		if (extension == null) {
			extension = user.createExtension();
		}
		for (int i=0; i<numberOfProperties; i++) {
			String propName = String.format(USER_EXTENSION_PROPERTY_NAME_FORMAT, i);
			PrismProperty<String> prop = extension.findOrCreateProperty(new QName(USER_EXTENSION_NS, propName));
			prop.setRealValue("val "+i);
		}
		
	}

	private void setRandomOrganizations(PrismObject<UserType> userBefore, int numberOfOrganizations) {
		List<PolyStringType> organizations = userBefore.asObjectable().getOrganization();
		for (int i=0; i<numberOfOrganizations; i++) {
			organizations.add(
					createPolyStringType(
							String.format(GENERATED_EMPTY_ROLE_NAME_FORMAT, RND.nextInt(NUMBER_OF_GENERATED_EMPTY_ROLES))));
		}
	}


}

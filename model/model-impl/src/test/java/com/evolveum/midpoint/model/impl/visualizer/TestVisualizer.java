/*
 * Copyright (c) 2010-2017 Evolveum
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
package com.evolveum.midpoint.model.impl.visualizer;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.api.context.ModelProjectionContext;
import com.evolveum.midpoint.model.api.visualizer.Scene;
import com.evolveum.midpoint.model.impl.AbstractInternalModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static com.evolveum.midpoint.schema.constants.ObjectTypes.*;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.createObjectRef;
import static org.apache.commons.collections.CollectionUtils.addIgnoreNull;
import static org.testng.AssertJUnit.*;

/**
 * @author mederly
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-test-main.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class TestVisualizer extends AbstractInternalModelIntegrationTest {

	@Autowired
	private Visualizer visualizer;

	@Autowired
	private PrismContext prismContext;

	@Autowired
	private TaskManager taskManager;

	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}
	
	@Test
	public void test100UserBasic() throws Exception {
		final String TEST_NAME = "test100UserBasic";
		Task task = createTask(TEST_NAME);

		PrismObject<UserType> u = prismContext.createObject(UserType.class);
		u.setOid("123");
		u.asObjectable().setName(new PolyStringType("user123"));
		u.asObjectable().setFullName(new PolyStringType("User User123"));

		/// WHEN
		displayWhen(TEST_NAME);
		final Scene scene = visualizer.visualize(u, task, task.getResult());

		// THEN
		displayThen(TEST_NAME);
		display("scene", scene);

		// TODO some asserts
	}

	@Test
	public void test110UserWithContainers() throws Exception {
		final String TEST_NAME = "test101UserWithContainers";
		Task task = createTask(TEST_NAME);

		PrismObject<UserType> u = prismContext.createObject(UserType.class);
		UserType ut = u.asObjectable();
		u.setOid("456");
		ut.setName(new PolyStringType("user456"));
		ut.setFullName(new PolyStringType("User User456"));
		ut.setActivation(new ActivationType(prismContext));
		ut.getActivation().setAdministrativeStatus(ActivationStatusType.ENABLED);
		ut.getActivation().setValidTo(XmlTypeConverter.createXMLGregorianCalendar(2020, 1, 1, 0, 0, 0));
		AssignmentType ass1 = new AssignmentType(prismContext);
		ass1.setActivation(new ActivationType(prismContext));
		ass1.getActivation().setAdministrativeStatus(ActivationStatusType.ENABLED);
		ass1.getActivation().setValidTo(XmlTypeConverter.createXMLGregorianCalendar(2019, 1, 1, 0, 0, 0));
		ass1.setTargetRef(createObjectRef(ROLE_SUPERUSER_OID, ROLE));
		ut.getAssignment().add(ass1);
		AssignmentType ass2 = new AssignmentType(prismContext);
		ass2.setTargetRef(createObjectRef("777", ROLE));
		ut.getAssignment().add(ass2);
		AssignmentType ass3 = new AssignmentType(prismContext);
		ass3.setConstruction(new ConstructionType(prismContext));
		ass3.getConstruction().setResourceRef(createObjectRef(RESOURCE_DUMMY_OID, RESOURCE));
		ut.getAssignment().add(ass3);

		/// WHEN
		displayWhen(TEST_NAME);
		final Scene scene = visualizer.visualize(u, task, task.getResult());

		// THEN
		displayThen(TEST_NAME);
		display("scene", scene);

		// TODO some asserts
	}

	@Test
	public void test200UserDeltaBasic() throws Exception {
		final String TEST_NAME = "test200UserDeltaBasic";
		Task task = createTask(TEST_NAME);

		ObjectDelta<?> delta = DeltaBuilder.deltaFor(UserType.class, prismContext)
				.item(UserType.F_NAME).replace("admin")
				.asObjectDelta(USER_ADMINISTRATOR_OID);

		/// WHEN
		displayWhen(TEST_NAME);
		final Scene scene = visualizer.visualizeDelta((ObjectDelta<? extends ObjectType>) delta, task, task.getResult());

		// THEN
		displayThen(TEST_NAME);
		display("scene", scene);

		// TODO some asserts
	}

	@Test
	public void test210UserDeltaContainers() throws Exception {
		final String TEST_NAME = "test210UserDeltaContainers";
		Task task = createTask(TEST_NAME);

		AssignmentType ass1 = new AssignmentType(prismContext);
		ass1.setActivation(new ActivationType(prismContext));
		ass1.getActivation().setAdministrativeStatus(ActivationStatusType.ENABLED);
		ass1.getActivation().setValidTo(XmlTypeConverter.createXMLGregorianCalendar(2017, 1, 1, 0, 0, 0));
		ass1.setTargetRef(createObjectRef(ROLE_SUPERUSER_OID, ROLE));

		ObjectDelta<?> delta = DeltaBuilder.deltaFor(UserType.class, prismContext)
				.item(UserType.F_NAME).replace("admin")
				.item(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS).replace(ActivationStatusType.ENABLED)
				.item(UserType.F_ASSIGNMENT, 1, AssignmentType.F_TARGET_REF).replace(createObjectRef("123", ROLE).asReferenceValue())
				.item(UserType.F_ASSIGNMENT, 1, AssignmentType.F_DESCRIPTION).add("suspicious")
				.item(UserType.F_ASSIGNMENT).add(ass1)
				.asObjectDelta(USER_ADMINISTRATOR_OID);

		/// WHEN
		displayWhen(TEST_NAME);
		final Scene scene = visualizer.visualizeDelta((ObjectDelta<? extends ObjectType>) delta, task, task.getResult());

		// THEN
		displayThen(TEST_NAME);
		display("scene", scene);

		// TODO some asserts
	}

	@Test
	public void test212UserDeltaContainerSimple() throws Exception {
		final String TEST_NAME = "test212UserDeltaContainerSimple";
		Task task = createTask(TEST_NAME);

		ObjectDelta<?> delta = DeltaBuilder.deltaFor(UserType.class, prismContext)
				.item(UserType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS).replace(ActivationStatusType.ENABLED)
				.item(UserType.F_ACTIVATION, ActivationType.F_ENABLE_TIMESTAMP).replace(XmlTypeConverter.createXMLGregorianCalendar(new Date()))
				.asObjectDelta(USER_ADMINISTRATOR_OID);

		/// WHEN
		displayWhen(TEST_NAME);
		final List<? extends Scene> scenes = visualizer.visualizeDeltas((List) Collections.singletonList(delta), task, task.getResult());

		// THEN
		displayThen(TEST_NAME);
		display("scenes", scenes);

		// TODO some asserts
	}

	@Test
	public void test220UserContainerReplace() throws Exception {
		final String TEST_NAME = "test220UserContainerReplace";
		Task task = createTask(TEST_NAME);

		AssignmentType ass1 = new AssignmentType(prismContext);
		ass1.setActivation(new ActivationType(prismContext));
		ass1.getActivation().setAdministrativeStatus(ActivationStatusType.DISABLED);
		ass1.getActivation().setValidFrom(XmlTypeConverter.createXMLGregorianCalendar(2010, 1, 1, 0, 0, 0));
		ass1.setTargetRef(createObjectRef(ROLE_SUPERUSER_OID, ROLE));

		ActivationType act1 = new ActivationType(prismContext);
		act1.setAdministrativeStatus(ActivationStatusType.DISABLED);

		ObjectDelta<?> delta = DeltaBuilder.deltaFor(UserType.class, prismContext)
				.item(UserType.F_NAME).replace("admin")
				.item(UserType.F_ACTIVATION).replace(act1)
				.item(UserType.F_ASSIGNMENT).replace(ass1)
				.asObjectDelta(USER_ADMINISTRATOR_OID);

		/// WHEN
		displayWhen(TEST_NAME);
		final Scene scene = visualizer.visualizeDelta((ObjectDelta<? extends ObjectType>) delta, task, task.getResult());

		// THEN
		displayThen(TEST_NAME);
		display("scene", scene);

		// TODO some asserts
	}

	@Test
	public void test230UserContainerDelete() throws Exception {
		final String TEST_NAME = "test230UserContainerDelete";
		Task task = createTask(TEST_NAME);

		AssignmentType ass1 = new AssignmentType(prismContext);
		ass1.setId(1L);

		AssignmentType ass2 = new AssignmentType(prismContext);
		ass2.setId(99999L);

		ObjectDelta<?> delta = DeltaBuilder.deltaFor(UserType.class, prismContext)
				.item(UserType.F_NAME).replace("admin")
				.item(UserType.F_ASSIGNMENT).delete(ass1, ass2)
				.asObjectDelta(USER_ADMINISTRATOR_OID);

		/// WHEN
		displayWhen(TEST_NAME);
		final Scene scene = visualizer.visualizeDelta((ObjectDelta<? extends ObjectType>) delta, task, task.getResult());

		// THEN
		displayThen(TEST_NAME);
		display("scene", scene);

		// TODO some asserts
	}

	@Test
	public void test300UserAssignmentPreview() throws Exception {
		final String TEST_NAME = "test300UserAssignmentPreview";
		Task task = createTask(TEST_NAME);

		PrismObject<UserType> jack = getUser(USER_JACK_OID);
		display("jack", jack);

		AssignmentType ass1 = new AssignmentType(prismContext);
		ass1.setConstruction(new ConstructionType(prismContext));
		ass1.getConstruction().setResourceRef(createObjectRef(RESOURCE_DUMMY_OID, RESOURCE));

		ObjectDelta<UserType> delta = (ObjectDelta<UserType>) DeltaBuilder.deltaFor(UserType.class, prismContext)
				.item(UserType.F_ASSIGNMENT).add(ass1)
				.asObjectDelta(USER_JACK_OID);

		delta.applyDefinitionIfPresent(jack.getDefinition(), false);

		/// WHEN
		displayWhen(TEST_NAME);
		ModelContext<UserType> modelContext = modelInteractionService.previewChanges(Collections.<ObjectDelta<? extends ObjectType>>singletonList(delta), null, task, task.getResult());
		List<ObjectDelta<? extends ObjectType>> primaryDeltas = new ArrayList<>();
		List<ObjectDelta<? extends ObjectType>> secondaryDeltas = new ArrayList<>();
		fillDeltas(modelContext, primaryDeltas, secondaryDeltas);

		List<? extends Scene> primaryScenes = modelInteractionService.visualizeDeltas(primaryDeltas, task, task.getResult());
		List<? extends Scene> secondaryScenes = modelInteractionService.visualizeDeltas(secondaryDeltas, task, task.getResult());

		// THEN
		displayThen(TEST_NAME);
		display("primary scenes", primaryScenes);
		display("secondary scenes", secondaryScenes);

		// TODO some asserts
	}

	@Test
	public void test305UserAssignmentAdd() throws Exception {
		final String TEST_NAME = "test305UserAssignmentAdd";
		Task task = createTask(TEST_NAME);

		display("jack", getUser(USER_JACK_OID));

		AssignmentType ass1 = new AssignmentType();
		ass1.setConstruction(new ConstructionType());
		ass1.getConstruction().setResourceRef(createObjectRef(RESOURCE_DUMMY_OID, RESOURCE));

		ObjectDelta<UserType> delta = (ObjectDelta<UserType>) DeltaBuilder.deltaFor(UserType.class, prismContext)
				.item(UserType.F_ASSIGNMENT).add(ass1)
				.asObjectDelta(USER_JACK_OID);

		/// WHEN
		displayWhen(TEST_NAME);
		final Scene scene = visualizer.visualizeDelta(delta, task, task.getResult());

		modelService.executeChanges(Collections.<ObjectDelta<? extends ObjectType>>singletonList(delta), null, task, task.getResult());

		// THEN
		displayThen(TEST_NAME);
		display("scene", scene);
		display("jack with assignment", getUser(USER_JACK_OID));

		// TODO some asserts
	}

	@Test
	public void test307UserDisablePreview() throws Exception {
		final String TEST_NAME = "test307UserDisablePreview";
		Task task = createTask(TEST_NAME);

		ObjectDelta<UserType> delta = (ObjectDelta<UserType>) DeltaBuilder.deltaFor(UserType.class, prismContext)
				.item(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS).replace(ActivationStatusType.DISABLED)
				.asObjectDelta(USER_JACK_OID);

		/// WHEN
		displayWhen(TEST_NAME);
		ModelContext<UserType> modelContext = modelInteractionService.previewChanges(Collections.<ObjectDelta<? extends ObjectType>>singletonList(delta), null, task, task.getResult());
		List<ObjectDelta<? extends ObjectType>> primaryDeltas = new ArrayList<>();
		List<ObjectDelta<? extends ObjectType>> secondaryDeltas = new ArrayList<>();
		fillDeltas(modelContext, primaryDeltas, secondaryDeltas);

		List<? extends Scene> primaryScenes = modelInteractionService.visualizeDeltas(primaryDeltas, task, task.getResult());
		List<? extends Scene> secondaryScenes = modelInteractionService.visualizeDeltas(secondaryDeltas, task, task.getResult());

		// THEN
		displayThen(TEST_NAME);
		display("primary scenes", primaryScenes);
		display("secondary scenes", secondaryScenes);

		// TODO some asserts
	}

	protected void fillDeltas(ModelContext<UserType> modelContext, List<ObjectDelta<? extends ObjectType>> primaryDeltas,
			List<ObjectDelta<? extends ObjectType>> secondaryDeltas) throws SchemaException {
		if (modelContext != null) {
			if (modelContext.getFocusContext() != null) {
				addIgnoreNull(primaryDeltas, modelContext.getFocusContext().getPrimaryDelta());
				addIgnoreNull(secondaryDeltas, modelContext.getFocusContext().getSecondaryDelta());
			}
			for (ModelProjectionContext projCtx : modelContext.getProjectionContexts()) {
				addIgnoreNull(primaryDeltas, projCtx.getPrimaryDelta());
				addIgnoreNull(secondaryDeltas, projCtx.getExecutableDelta());
			}
		}
		display("primary deltas", primaryDeltas);
		display("secondary deltas", secondaryDeltas);
	}

	private String dummyAccountOid;

	@Test
	public void test310UserLinkRefDelete() throws Exception {
		final String TEST_NAME = "test310UserLinkRefDelete";
		Task task = createTask(TEST_NAME);

		UserType jack = getUser(USER_JACK_OID).asObjectable();
		assertEquals("wrong # of linkrefs", 1, jack.getLinkRef().size());
		dummyAccountOid = jack.getLinkRef().get(0).getOid();

		ObjectDelta<UserType> delta = (ObjectDelta<UserType>) DeltaBuilder.deltaFor(UserType.class, prismContext)
				.item(UserType.F_LINK_REF).delete(createObjectRef(dummyAccountOid, SHADOW).asReferenceValue())
				.asObjectDelta(USER_JACK_OID);

		/// WHEN
		displayWhen(TEST_NAME);
		final Scene scene = visualizer.visualizeDelta(delta, task, task.getResult());

		// THEN
		displayThen(TEST_NAME);
		display("scene", scene);

		// TODO some asserts
	}

	@Test
	public void test320UserLinkRefAdd() throws Exception {
		final String TEST_NAME = "test320UserLinkRefAdd";
		Task task = createTask(TEST_NAME);

		ObjectDelta<UserType> delta = (ObjectDelta<UserType>) DeltaBuilder.deltaFor(UserType.class, prismContext)
				.item(UserType.F_LINK_REF).add(createObjectRef(dummyAccountOid, SHADOW).asReferenceValue())
				.asObjectDelta(USER_JACK_OID);

		/// WHEN
		displayWhen(TEST_NAME);
		final Scene scene = visualizer.visualizeDelta(delta, task, task.getResult());

		// THEN
		displayThen(TEST_NAME);
		display("scene", scene);

		// TODO some asserts
	}

	@Test
	public void test330UserLinkRefReplaceNoOp() throws Exception {
		final String TEST_NAME = "test330UserLinkRefReplaceNoOp";
		Task task = createTask(TEST_NAME);

		ObjectDelta<UserType> delta = (ObjectDelta<UserType>) DeltaBuilder.deltaFor(UserType.class, prismContext)
				.item(UserType.F_LINK_REF).replace(createObjectRef(dummyAccountOid, SHADOW).asReferenceValue())
				.asObjectDelta(USER_JACK_OID);

		/// WHEN
		displayWhen(TEST_NAME);
		final Scene scene = visualizer.visualizeDelta(delta, task, task.getResult());

		// THEN
		displayThen(TEST_NAME);
		display("scene", scene);

		// TODO some asserts
	}

	@Test
	public void test340UserLinkRefReplaceOp() throws Exception {
		final String TEST_NAME = "test340UserLinkRefReplaceOp";
		Task task = createTask(TEST_NAME);

		ObjectDelta<UserType> delta = (ObjectDelta<UserType>) DeltaBuilder.deltaFor(UserType.class, prismContext)
				.item(UserType.F_LINK_REF).replace(createObjectRef("777", SHADOW).asReferenceValue())
				.asObjectDelta(USER_JACK_OID);

		/// WHEN
		displayWhen(TEST_NAME);
		final Scene scene = visualizer.visualizeDelta(delta, task, task.getResult());

		// THEN
		displayThen(TEST_NAME);
		display("scene", scene);

		// TODO some asserts
	}

}

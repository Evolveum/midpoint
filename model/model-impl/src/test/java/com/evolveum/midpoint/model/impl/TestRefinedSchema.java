/*
 * Copyright (c) 2016 Evolveum
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
package com.evolveum.midpoint.model.impl;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.common.monitor.InternalMonitor;
import com.evolveum.midpoint.common.refinery.LayerRefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.LayerRefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.impl.AbstractInternalModelIntegrationTest;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.TestProjector;
import com.evolveum.midpoint.model.impl.lens.projector.Projector;
import com.evolveum.midpoint.model.impl.trigger.RecomputeTriggerHandler;
import com.evolveum.midpoint.model.impl.util.Utils;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TriggerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValuePolicyType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestRefinedSchema extends AbstractInternalModelIntegrationTest {
	
	protected static final File TEST_DIR = new File("src/test/resources/refinedschema");
	
	public static final File TASK_RECONCILE_DUMMY_OBJECTCLASS_FILE = new File(TEST_DIR, 
			"task-reconcile-dummy-objectclass.xml");
	public static final String TASK_RECONCILE_DUMMY_OBJECTCLASS_OID = "bed15976-e604-11e5-a181-af0dade5e5a0";

	public static final File TASK_RECONCILE_DUMMY_KIND_INTENT_FILE = new File(TEST_DIR, 
			"task-reconcile-dummy-kind-intent.xml");
	public static final String TASK_RECONCILE_DUMMY_KIND_INTENT_OID = "d4cd18f2-e60c-11e5-a806-3faae6c13aff";

	public static final File TASK_RECONCILE_DUMMY_KIND_INTENT_OBJECTCLASS_FILE = new File(TEST_DIR, 
			"task-reconcile-dummy-kind-intent-objectclass.xml");
	public static final String TASK_RECONCILE_DUMMY_KIND_INTENT_OBJECTCLASS_OID = "3f2a1140-e60e-11e5-adb7-776abfbb2227";

	
	private RefinedResourceSchema refinedSchema;
	private RefinedResourceSchema refinedSchemaModel;
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		InternalMonitor.reset();
//		InternalMonitor.setTraceShadowFetchOperation(true);
	}

	@Test
    public void test000Sanity() throws Exception {
		final String TEST_NAME = "test000Sanity";
        TestUtil.displayTestTile(this, TEST_NAME);

        // WHEN
        refinedSchema = RefinedResourceSchema.getRefinedSchema(resourceDummyType, prismContext);
        
        display("Dummy refined schema", refinedSchema);
        
        // THEN
        dummyResourceCtl.assertRefinedSchemaSanity(refinedSchema);
	}
	
	@Test
    public void test010SanityModel() throws Exception {
		final String TEST_NAME = "test010SanityModel";
        TestUtil.displayTestTile(this, TEST_NAME);

        // WHEN
        refinedSchemaModel = RefinedResourceSchema.getRefinedSchema(resourceDummyType, LayerType.MODEL, prismContext);
        
        display("Dummy refined schema (MODEL)", refinedSchemaModel);
        
        // THEN
        dummyResourceCtl.assertRefinedSchemaSanity(refinedSchemaModel);
        
        assertTrue("Not layer refined schema, it is "+refinedSchemaModel.getClass(), refinedSchemaModel instanceof LayerRefinedResourceSchema);
        assertEquals("Wrong layer", LayerType.MODEL, ((LayerRefinedResourceSchema)refinedSchemaModel).getLayer());
	}
	
	@Test
    public void test110DetermineObjectClassObjectClass() throws Exception {
		final String TEST_NAME = "test110DetermineObjectClassObjectClass";
        TestUtil.displayTestTile(this, TEST_NAME);

        OperationResult result = new OperationResult(TestRefinedSchema.class.getName()  + "." + TEST_NAME);
        
        importObjectFromFile(TASK_RECONCILE_DUMMY_OBJECTCLASS_FILE);
        
        Task task = taskManager.getTask(TASK_RECONCILE_DUMMY_OBJECTCLASS_OID, result);
        display("Task", task);
        
        // WHEN
        ObjectClassComplexTypeDefinition objectClass = Utils.determineObjectClass(refinedSchema, task);
        
        // THEN
        display("Object class", objectClass);
        
        deleteObject(TaskType.class, TASK_RECONCILE_DUMMY_OBJECTCLASS_OID);
        
        assertObjectClass(objectClass, RESOURCE_DUMMY_ACCOUNT_OBJECTCLASS_QNAME);
	}

	@Test
    public void test112DetermineObjectClassKindIntent() throws Exception {
		final String TEST_NAME = "test112DetermineObjectClassKindIntent";
        TestUtil.displayTestTile(this, TEST_NAME);

        OperationResult result = new OperationResult(TestRefinedSchema.class.getName()  + "." + TEST_NAME);
        
        importObjectFromFile(TASK_RECONCILE_DUMMY_KIND_INTENT_FILE);
        
        Task task = taskManager.getTask(TASK_RECONCILE_DUMMY_KIND_INTENT_OID, result);
        display("Task", task);
        
        // WHEN
        ObjectClassComplexTypeDefinition objectClass = Utils.determineObjectClass(refinedSchema, task);
        
        // THEN
        display("Object class", objectClass);
        
        deleteObject(TaskType.class, TASK_RECONCILE_DUMMY_KIND_INTENT_OID);
        
        assertRefinedObjectClass(objectClass, RESOURCE_DUMMY_PRIVILEGE_OBJECTCLASS_QNAME, ShadowKindType.ENTITLEMENT, "privilege");
	}

	@Test
    public void test114DetermineObjectClassKindIntentObjectClass() throws Exception {
		final String TEST_NAME = "test114DetermineObjectClassKindIntentObjectClass";
        TestUtil.displayTestTile(this, TEST_NAME);

        OperationResult result = new OperationResult(TestRefinedSchema.class.getName()  + "." + TEST_NAME);
        
        importObjectFromFile(TASK_RECONCILE_DUMMY_KIND_INTENT_OBJECTCLASS_FILE);
        
        Task task = taskManager.getTask(TASK_RECONCILE_DUMMY_KIND_INTENT_OBJECTCLASS_OID, result);
        display("Task", task);
        
        // WHEN
        ObjectClassComplexTypeDefinition objectClass = Utils.determineObjectClass(refinedSchema, task);
        
        // THEN
        display("Object class", objectClass);
        
        deleteObject(TaskType.class, TASK_RECONCILE_DUMMY_KIND_INTENT_OBJECTCLASS_OID);
        
        assertRefinedObjectClass(objectClass, RESOURCE_DUMMY_PRIVILEGE_OBJECTCLASS_QNAME, ShadowKindType.ENTITLEMENT, "privilege");
	}

	@Test
    public void test120DetermineObjectClassObjectClassModel() throws Exception {
		final String TEST_NAME = "test120DetermineObjectClassObjectClassModel";
        TestUtil.displayTestTile(this, TEST_NAME);

        OperationResult result = new OperationResult(TestRefinedSchema.class.getName()  + "." + TEST_NAME);
        
        importObjectFromFile(TASK_RECONCILE_DUMMY_OBJECTCLASS_FILE);
        
        Task task = taskManager.getTask(TASK_RECONCILE_DUMMY_OBJECTCLASS_OID, result);
        display("Task", task);
        
        // WHEN
        ObjectClassComplexTypeDefinition objectClass = Utils.determineObjectClass(refinedSchemaModel, task);
        
        // THEN
        display("Object class", objectClass);
        display("Object class (toString)", objectClass.toString());
        
        deleteObject(TaskType.class, TASK_RECONCILE_DUMMY_OBJECTCLASS_OID);
        
        assertObjectClass(objectClass, RESOURCE_DUMMY_ACCOUNT_OBJECTCLASS_QNAME);
	}

	@Test
    public void test122DetermineObjectClassKindIntentModel() throws Exception {
		final String TEST_NAME = "test122DetermineObjectClassKindIntentModel";
        TestUtil.displayTestTile(this, TEST_NAME);

        OperationResult result = new OperationResult(TestRefinedSchema.class.getName()  + "." + TEST_NAME);
        
        importObjectFromFile(TASK_RECONCILE_DUMMY_KIND_INTENT_FILE);
        
        Task task = taskManager.getTask(TASK_RECONCILE_DUMMY_KIND_INTENT_OID, result);
        display("Task", task);
        
        // WHEN
        ObjectClassComplexTypeDefinition objectClass = Utils.determineObjectClass(refinedSchemaModel, task);
        
        // THEN
        display("Object class", objectClass);
        display("Object class (toString)", objectClass.toString());
        
        deleteObject(TaskType.class, TASK_RECONCILE_DUMMY_KIND_INTENT_OID);
        
        assertLayerRefinedObjectClass(objectClass, RESOURCE_DUMMY_PRIVILEGE_OBJECTCLASS_QNAME, 
        		ShadowKindType.ENTITLEMENT, "privilege", LayerType.MODEL);
	}

	@Test
    public void test124DetermineObjectClassKindIntentObjectClassModel() throws Exception {
		final String TEST_NAME = "test124DetermineObjectClassKindIntentObjectClassModel";
        TestUtil.displayTestTile(this, TEST_NAME);

        OperationResult result = new OperationResult(TestRefinedSchema.class.getName()  + "." + TEST_NAME);
        
        importObjectFromFile(TASK_RECONCILE_DUMMY_KIND_INTENT_OBJECTCLASS_FILE);
        
        Task task = taskManager.getTask(TASK_RECONCILE_DUMMY_KIND_INTENT_OBJECTCLASS_OID, result);
        display("Task", task);
        
        // WHEN
        ObjectClassComplexTypeDefinition objectClass = Utils.determineObjectClass(refinedSchemaModel, task);
        
        // THEN
        display("Object class", objectClass);
        display("Object class (toString)", objectClass.toString());
        
        deleteObject(TaskType.class, TASK_RECONCILE_DUMMY_KIND_INTENT_OBJECTCLASS_OID);
        
        assertLayerRefinedObjectClass(objectClass, RESOURCE_DUMMY_PRIVILEGE_OBJECTCLASS_QNAME, 
        		ShadowKindType.ENTITLEMENT, "privilege", LayerType.MODEL);
	}

	private void assertObjectClass(ObjectClassComplexTypeDefinition objectClass,
			QName objectClassQName) {
		assertNotNull("No object class", objectClass);
		assertEquals("Wrong object class QName in object class "+objectClass, objectClassQName, objectClass.getTypeName());
	}

	private void assertRefinedObjectClass(ObjectClassComplexTypeDefinition objectClass,
			QName objectClassQName, ShadowKindType kind, String intent) {
		assertNotNull("No object class", objectClass);
		if (!(objectClass instanceof RefinedObjectClassDefinition)) {
			AssertJUnit.fail("Expected refined object class definition, but it was "+objectClass+" ("+objectClass.getClass()+")");
		}
		RefinedObjectClassDefinition rOcDef = (RefinedObjectClassDefinition)objectClass;
		assertEquals("Wrong object class QName in rOcDef "+rOcDef, objectClassQName, rOcDef.getTypeName());
		assertEquals("Wrong kind in rOcDef "+rOcDef, kind, rOcDef.getKind());
		assertEquals("Wrong kind in rOcDef "+rOcDef, intent, rOcDef.getIntent());
	}
	
	private void assertLayerRefinedObjectClass(ObjectClassComplexTypeDefinition objectClass,
			QName objectClassQName, ShadowKindType kind, String intent, LayerType layer) {
		assertRefinedObjectClass(objectClass, objectClassQName, kind, intent);
        assertTrue("Not layer refined definition, it is "+refinedSchemaModel.getClass(), objectClass instanceof LayerRefinedObjectClassDefinition);
        assertEquals("Wrong layer", layer, ((LayerRefinedObjectClassDefinition)objectClass).getLayer());

	}
}

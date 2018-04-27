/*
 * Copyright (c) 2016-2017 Evolveum
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

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.util.Collection;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.refinery.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.impl.util.Utils;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LayerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

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
        TestUtil.displayTestTitle(this, TEST_NAME);

        // WHEN
        refinedSchema = RefinedResourceSchemaImpl.getRefinedSchema(getDummyResourceType(), prismContext);

        display("Dummy refined schema", refinedSchema);

        // THEN
        getDummyResourceController().assertRefinedSchemaSanity(refinedSchema);
	}

	@Test
    public void test010SanityModel() throws Exception {
		final String TEST_NAME = "test010SanityModel";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // WHEN
        refinedSchemaModel = RefinedResourceSchemaImpl.getRefinedSchema(getDummyResourceType(), LayerType.MODEL, prismContext);

        display("Dummy refined schema (MODEL)", refinedSchemaModel);

        // THEN
        getDummyResourceController().assertRefinedSchemaSanity(refinedSchemaModel);

        assertTrue("Not layer refined schema, it is "+refinedSchemaModel.getClass(), refinedSchemaModel instanceof LayerRefinedResourceSchema);
        assertEquals("Wrong layer", LayerType.MODEL, ((LayerRefinedResourceSchema)refinedSchemaModel).getLayer());
	}

	@Test
    public void test100EntitlementRefinedObjectClasses() throws Exception {
		final String TEST_NAME = "test100EntitlementRefinedObjectClasses";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // WHEN
        Collection<? extends RefinedObjectClassDefinition> entitlementROcDefs = refinedSchema.getRefinedDefinitions(ShadowKindType.ENTITLEMENT);

        display("entitlement rOcDefs", entitlementROcDefs);

        // THEN

        for (RefinedObjectClassDefinition entitlementROcDef: entitlementROcDefs) {
        	assertEquals("Wrong kind in "+entitlementROcDef, ShadowKindType.ENTITLEMENT, entitlementROcDef.getKind());
        }

        assertEquals("Wrong number of entitlement rOcDefs", 6, entitlementROcDefs.size());
	}

	@Test
    public void test101EntitlementRefinedObjectClassesModel() throws Exception {
		final String TEST_NAME = "test101EntitlementRefinedObjectClassesModel";
        TestUtil.displayTestTitle(this, TEST_NAME);

        // WHEN
        Collection<? extends RefinedObjectClassDefinition> entitlementROcDefs = refinedSchemaModel.getRefinedDefinitions(ShadowKindType.ENTITLEMENT);

        display("entitlement rOcDefs", entitlementROcDefs);

        // THEN

        for (RefinedObjectClassDefinition entitlementROcDef: entitlementROcDefs) {
        	assertEquals("Wrong kind in "+entitlementROcDef, ShadowKindType.ENTITLEMENT, entitlementROcDef.getKind());
        }

        assertEquals("Wrong number of entitlement rOcDefs", 6, entitlementROcDefs.size());
	}

	@Test
    public void test110DetermineObjectClassObjectClass() throws Exception {
		final String TEST_NAME = "test110DetermineObjectClassObjectClass";
        TestUtil.displayTestTitle(this, TEST_NAME);

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
        TestUtil.displayTestTitle(this, TEST_NAME);

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
        TestUtil.displayTestTitle(this, TEST_NAME);

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
        TestUtil.displayTestTitle(this, TEST_NAME);

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
        TestUtil.displayTestTitle(this, TEST_NAME);

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
        TestUtil.displayTestTitle(this, TEST_NAME);

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

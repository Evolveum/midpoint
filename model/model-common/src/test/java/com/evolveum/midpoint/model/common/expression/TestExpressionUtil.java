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
package com.evolveum.midpoint.model.common.expression;

import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertEquals;

import com.evolveum.midpoint.prism.util.PrismAsserts;

import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

import org.testng.annotations.BeforeSuite;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.repo.common.expression.ItemDeltaItem;
import com.evolveum.midpoint.repo.common.expression.ObjectDeltaObject;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.marshaller.ItemPathHolder;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 *
 */
public class TestExpressionUtil {

	public static final String USER_JACK_OID = "c0c010c0-d34d-b33f-f00d-111111111111";
	public static final File USER_JACK_FILE = new File(MidPointTestConstants.OBJECTS_DIR, USER_JACK_OID + ".xml");

	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}

    @Test
    public void testResolvePathStringProperty() throws Exception {
    	final String TEST_NAME = "testResolvePathStringProperty";
    	System.out.println("\n===[ "+TEST_NAME+" ]===\n");

    	// GIVEN

    	// WHEN
		PrismProperty<String> resolvedProperty = resolvePath("$user/description", TEST_NAME);

    	// THEN
    	assertEquals("Wrong resolved property value", "jack", resolvedProperty.getRealValue());
    }

    @Test
    public void testResolvePathPolyStringProperty() throws Exception {
    	final String TEST_NAME = "testResolvePathPolyStringProperty";
    	System.out.println("\n===[ "+TEST_NAME+" ]===\n");

    	// GIVEN

    	// WHEN
		PrismProperty<PolyString> resolvedProperty = resolvePath("$user/fullName", TEST_NAME);

    	// THEN
    	assertEquals("Wrong resolved property value", PrismTestUtil.createPolyString("Jack Sparrow"),
    			resolvedProperty.getRealValue());
    }

    @Test
    public void testResolvePathPolyStringOrig() throws Exception {
    	final String TEST_NAME = "testResolvePathPolyStringOrig";
    	System.out.println("\n===[ "+TEST_NAME+" ]===\n");

    	// GIVEN

    	// WHEN
		String resolved = resolvePath("$user/fullName/t:orig", TEST_NAME);

    	// THEN
    	assertEquals("Wrong resolved property value", "Jack Sparrow", resolved);
    }

    @Test
    public void testResolvePathPolyStringNorm() throws Exception {
    	final String TEST_NAME = "testResolvePathPolyStringNorm";
    	System.out.println("\n===[ "+TEST_NAME+" ]===\n");

    	// GIVEN

    	// WHEN
		String resolved = resolvePath("$user/fullName/t:norm", TEST_NAME);

    	// THEN
    	assertEquals("Wrong resolved property value", "jack sparrow", resolved);
    }

    @Test
    public void testResolvePathPolyStringOdo() throws Exception {
    	final String TEST_NAME = "testResolvePathPolyStringOdo";
    	System.out.println("\n===[ "+TEST_NAME+" ]===\n");

    	// GIVEN

    	// WHEN
    	ItemDeltaItem<PrismPropertyValue<PolyString>,PrismPropertyDefinition<PolyString>> idi = resolvePathOdo("$user/fullName", TEST_NAME);

    	// THEN
    	assertEquals("Wrong resolved idi old value", PrismTestUtil.createPolyString("Jack Sparrow"),
    			((PrismProperty<PolyString>)idi.getItemOld()).getRealValue());
    	assertEquals("Wrong resolved idi new value", PrismTestUtil.createPolyString("Captain Jack Sparrow"),
    			((PrismProperty<PolyString>)idi.getItemNew()).getRealValue());

    	assertTrue("Wrong residual path: "+idi.getResidualPath(),
    			idi.getResidualPath() == null || idi.getResidualPath().isEmpty());

    }

    @Test
    public void testResolvePathPolyStringOdoOrig() throws Exception {
    	final String TEST_NAME = "testResolvePathPolyStringOdoOrig";
    	System.out.println("\n===[ "+TEST_NAME+" ]===\n");

    	// GIVEN

    	// WHEN
    	ItemDeltaItem<PrismPropertyValue<PolyString>,PrismPropertyDefinition<PolyString>> idi = resolvePathOdo("$user/fullName/t:orig", TEST_NAME);

    	// THEN
    	assertEquals("Wrong resolved idi old value", PrismTestUtil.createPolyString("Jack Sparrow"),
    			((PrismProperty<PolyString>)idi.getItemOld()).getRealValue());
    	assertEquals("Wrong resolved idi new value", PrismTestUtil.createPolyString("Captain Jack Sparrow"),
    			((PrismProperty<PolyString>)idi.getItemNew()).getRealValue());

    	PrismAsserts.assertPathEquivalent("Wrong residual path", new ItemPath(PolyString.F_ORIG), idi.getResidualPath());
    }

    @Test
    public void testResolvePathPolyStringOdoNorm() throws Exception {
    	final String TEST_NAME = "testResolvePathPolyStringOdoNorm";
    	System.out.println("\n===[ "+TEST_NAME+" ]===\n");

    	// GIVEN

    	// WHEN
    	ItemDeltaItem<PrismPropertyValue<PolyString>,PrismPropertyDefinition<PolyString>> idi = resolvePathOdo("$user/fullName/t:norm", TEST_NAME);

    	// THEN
    	assertEquals("Wrong resolved idi old value", PrismTestUtil.createPolyString("Jack Sparrow"),
    			((PrismProperty<PolyString>)idi.getItemOld()).getRealValue());
    	assertEquals("Wrong resolved idi new value", PrismTestUtil.createPolyString("Captain Jack Sparrow"),
    			((PrismProperty<PolyString>)idi.getItemNew()).getRealValue());

        PrismAsserts.assertPathEquivalent("Wrong residual path", new ItemPath(PolyString.F_NORM), idi.getResidualPath());

    }

    private <T> T resolvePath(String path, final String TEST_NAME) throws SchemaException, ObjectNotFoundException, IOException {
    	ExpressionVariables variables = createVariables();
    	return resolvePath(path, variables, TEST_NAME);
    }

    private <T> T resolvePathOdo(String path, final String TEST_NAME) throws SchemaException, ObjectNotFoundException, IOException {
    	ExpressionVariables variables = createVariablesOdo();
    	return resolvePath(path, variables, TEST_NAME);
    }

    private <T> T resolvePath(String path, ExpressionVariables variables, final String TEST_NAME) throws SchemaException, ObjectNotFoundException {
    	OperationResult result = new OperationResult(TestExpressionUtil.class.getName() + "." + TEST_NAME);
		ItemPath itemPath = toItemPath(path);

		// WHEN
    	Object resolved = ExpressionUtil.resolvePath(itemPath, variables, null, null, TEST_NAME, null, result);

    	// THEN
    	System.out.println("Resolved:");
    	System.out.println(resolved);

    	return (T) resolved;
    }

	private ExpressionVariables createVariables() throws SchemaException, IOException {
		ExpressionVariables variables = new ExpressionVariables();
		variables.addVariableDefinition(ExpressionConstants.VAR_USER, createUser());
		return variables;
	}

	private ExpressionVariables createVariablesOdo() throws SchemaException, IOException {
		ExpressionVariables variables = new ExpressionVariables();
		PrismObject<UserType> userOld = createUser();
		ObjectDelta<UserType> delta = ObjectDelta.createModificationReplaceProperty(UserType.class,
				userOld.getOid(), UserType.F_FULL_NAME, PrismTestUtil.getPrismContext(),
				PrismTestUtil.createPolyString("Captain Jack Sparrow"));
		ObjectDeltaObject<UserType> odo = new ObjectDeltaObject<>(userOld, delta, null);
		odo.recompute();
		variables.addVariableDefinition(ExpressionConstants.VAR_USER, odo);
		return variables;
	}

	private PrismObject<UserType> createUser() throws SchemaException, IOException {
		return PrismTestUtil.parseObject(USER_JACK_FILE);
	}

	private ItemPath toItemPath(String stringPath) {
		String xml = "<path " +
				"xmlns='"+SchemaConstants.NS_C+"' " +
				"xmlns:t='"+SchemaConstants.NS_TYPES+"'>" +
				stringPath + "</path>";
		Document doc = DOMUtil.parseDocument(xml);
		Element element = DOMUtil.getFirstChildElement(doc);
		return new ItemPathHolder(element).toItemPath();

	}

}

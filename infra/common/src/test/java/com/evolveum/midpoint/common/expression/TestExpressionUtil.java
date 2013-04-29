/**
 * Copyright (c) 2013 Evolveum
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
 * Portions Copyrighted 2013 [name of copyright owner]
 */
package com.evolveum.midpoint.common.expression;

import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertEquals;
import org.testng.annotations.Test;
import org.testng.AssertJUnit;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.common.CommonTestConstants;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.holder.XPathHolder;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

/**
 * @author semancik
 *
 */
public class TestExpressionUtil {
	
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
    	ItemDeltaItem<PrismPropertyValue<PolyString>> idi = resolvePathOdo("$user/fullName", TEST_NAME);
    	
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
    	ItemDeltaItem<PrismPropertyValue<PolyString>> idi = resolvePathOdo("$user/fullName/t:orig", TEST_NAME);
    	
    	// THEN
    	assertEquals("Wrong resolved idi old value", PrismTestUtil.createPolyString("Jack Sparrow"), 
    			((PrismProperty<PolyString>)idi.getItemOld()).getRealValue());
    	assertEquals("Wrong resolved idi new value", PrismTestUtil.createPolyString("Captain Jack Sparrow"), 
    			((PrismProperty<PolyString>)idi.getItemNew()).getRealValue());
    	
    	assertEquals("Wrong residual path", new ItemPath(PolyString.F_ORIG), idi.getResidualPath());
    	
    }
    
    @Test
    public void testResolvePathPolyStringOdoNorm() throws Exception {
    	final String TEST_NAME = "testResolvePathPolyStringOdoNorm";
    	System.out.println("\n===[ "+TEST_NAME+" ]===\n");
    	
    	// GIVEN

    	// WHEN
    	ItemDeltaItem<PrismPropertyValue<PolyString>> idi = resolvePathOdo("$user/fullName/t:norm", TEST_NAME);
    	
    	// THEN
    	assertEquals("Wrong resolved idi old value", PrismTestUtil.createPolyString("Jack Sparrow"), 
    			((PrismProperty<PolyString>)idi.getItemOld()).getRealValue());
    	assertEquals("Wrong resolved idi new value", PrismTestUtil.createPolyString("Captain Jack Sparrow"), 
    			((PrismProperty<PolyString>)idi.getItemNew()).getRealValue());
    	
    	assertEquals("Wrong residual path", new ItemPath(PolyString.F_NORM), idi.getResidualPath());
    	
    }

    private <T> T resolvePath(String path, final String TEST_NAME) throws SchemaException, ObjectNotFoundException {
    	Map<QName, Object> variables = createVariables();
    	return resolvePath(path, variables, TEST_NAME);
    }
    
    private <T> T resolvePathOdo(String path, final String TEST_NAME) throws SchemaException, ObjectNotFoundException {
    	Map<QName, Object> variables = createVariablesOdo();
    	return resolvePath(path, variables, TEST_NAME);
    }
    
    private <T> T resolvePath(String path, Map<QName, Object> variables, final String TEST_NAME) throws SchemaException, ObjectNotFoundException {
    	OperationResult result = new OperationResult(TestExpressionUtil.class.getName() + "." + TEST_NAME);
		ItemPath itemPath = toItemPath(path);
		
		// WHEN
    	Object resolved = ExpressionUtil.resolvePath(itemPath, variables, null, null, TEST_NAME, result);
    	
    	// THEN
    	System.out.println("Resolved:");
    	System.out.println(resolved);
    	
    	return (T) resolved;
    }

	private Map<QName, Object> createVariables() throws SchemaException {
		Map<QName, Object> variables = new HashMap<QName, Object>();
		variables.put(ExpressionConstants.VAR_USER, createUser());
		return variables;
	}
	
	private Map<QName, Object> createVariablesOdo() throws SchemaException {
		Map<QName, Object> variables = new HashMap<QName, Object>();
		PrismObject<UserType> userOld = createUser();
		ObjectDelta<UserType> delta = ObjectDelta.createModificationReplaceProperty(UserType.class,
				userOld.getOid(), UserType.F_FULL_NAME, PrismTestUtil.getPrismContext(),
				PrismTestUtil.createPolyString("Captain Jack Sparrow"));
		ObjectDeltaObject<UserType> odo = new ObjectDeltaObject<UserType>(userOld, delta, null);
		odo.recompute();
		variables.put(ExpressionConstants.VAR_USER, odo);
		return variables;
	}

	private PrismObject<UserType> createUser() throws SchemaException {
		return PrismTestUtil.parseObject(CommonTestConstants.USER_JACK_FILE);
	}

	private ItemPath toItemPath(String stringPath) {
		String xml = "<path " +
				"xmlns='"+SchemaConstants.NS_C+"' " +
				"xmlns:t='"+SchemaConstants.NS_TYPES+"'>" + 
				stringPath + "</path>";
		Document doc = DOMUtil.parseDocument(xml);
		Element element = DOMUtil.getFirstChildElement(doc);
		return new XPathHolder(element).toItemPath();
		
	}

}

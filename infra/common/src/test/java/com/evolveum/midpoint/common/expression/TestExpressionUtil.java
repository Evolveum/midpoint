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
    
    private <T> T resolvePath(String path, final String TEST_NAME) throws SchemaException, ObjectNotFoundException {
    	OperationResult result = new OperationResult(TestExpressionUtil.class.getName() + "." + TEST_NAME);
		Map<QName, Object> variables = createVariables();
    	
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

	private PrismObject<UserType> createUser() throws SchemaException {
		return PrismTestUtil.parseObject(CommonTestConstants.USER_JACK_FILE);
	}

	private ItemPath toItemPath(String stringPath) {
		String xml = "<path xmlns='"+SchemaConstants.NS_C+"'>"+stringPath+"</path>";
		Document doc = DOMUtil.parseDocument(xml);
		Element element = DOMUtil.getFirstChildElement(doc);
		return new XPathHolder(element).toPropertyPath();
		
	}

}

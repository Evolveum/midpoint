/**
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.prism;

import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertEquals;
import static com.evolveum.midpoint.prism.PrismInternalTestUtil.*;

import java.io.IOException;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.foo.UserType;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 *
 */
public class TestCompare {
	
	@BeforeSuite
	public void setupDebug() {
		DebugUtil.setDefaultNamespacePrefix(DEFAULT_NAMESPACE_PREFIX);
	}
	
	/**
	 * Parse the same files twice, compare the results.
	 */
	@Test
	public void testCompare() throws SchemaException, SAXException, IOException {
		System.out.println("===[ testCompare ]===");
		
		// GIVEN
		PrismContext prismContext = constructInitializedPrismContext();
		
		Document document = DOMUtil.parseFile(USER_JACK_FILE);
		Element userElement = DOMUtil.getFirstChildElement(document);
		
		PrismObject<UserType> user1 = prismContext.parseObject(userElement);
		PrismObject<UserType> user2 = prismContext.parseObject(userElement);
		
		// WHEN, THEN
		
		assertTrue("Users not the same (PrismObject)", user1.equals(user2));
		
		// Following line won't work here. We don't have proper generated classes here. 
		// It is tested in the "schema" project that has a proper code generation
		// assertTrue("Users not the same (Objectable)", user1.asObjectable().equals(user2.asObjectable()));
		
		assertTrue("Users not equivalent", user1.equivalent(user2));
	}

}

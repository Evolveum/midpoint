/**
 * Copyright (c) 2012 Evolveum
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
 * Portions Copyrighted 2012 [name of copyright owner]
 */
package com.evolveum.midpoint.prism;

import static org.testng.AssertJUnit.assertTrue;
import static com.evolveum.midpoint.prism.PrismInternalTestUtil.*;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.foo.AssignmentType;
import com.evolveum.midpoint.prism.foo.UserType;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author Radovan Semancik
 *
 */
public class TestFundamentals {
	
	@BeforeSuite
	public void setupDebug() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(DEFAULT_NAMESPACE_PREFIX);
		PrismTestUtil.resetPrismContext(new PrismInternalTestUtil());
	}
	
	@Test
    public void testPrismValueContainsRealValue() throws Exception {
		System.out.println("\n\n===[ testPrismValueContainsRealValue ]===\n");
		// GIVEN
		PrismPropertyValue<String> valFoo1 = new PrismPropertyValue<String>("foo");
		PrismPropertyValue<String> valBar1 = new PrismPropertyValue<String>("bar");
		valBar1.setOriginType(OriginType.OUTBOUND);
		Collection<PrismValue> collection = new ArrayList<PrismValue>();
		collection.add(valFoo1);
		collection.add(valBar1);
		
		PrismPropertyValue<String> valFoo2 = new PrismPropertyValue<String>("foo");
		PrismPropertyValue<String> valFoo3 = new PrismPropertyValue<String>("foo");
		valFoo3.setOriginType(OriginType.OUTBOUND);
		
		PrismPropertyValue<String> valBar2 = new PrismPropertyValue<String>("bar");
		valBar2.setOriginType(OriginType.OUTBOUND);
		PrismPropertyValue<String> valBar3 = new PrismPropertyValue<String>("bar");
		
		PrismPropertyValue<String> valBaz = new PrismPropertyValue<String>("baz");
		
		// WHEN - THEN
		assert PrismValue.containsRealValue(collection, valFoo1);
		assert PrismValue.containsRealValue(collection, valBar1);
		assert PrismValue.containsRealValue(collection, valFoo2);
		assert PrismValue.containsRealValue(collection, valBar2);
		assert PrismValue.containsRealValue(collection, valFoo3);
		assert PrismValue.containsRealValue(collection, valBar3);
		assert !PrismValue.containsRealValue(collection, valBaz);
    }

}

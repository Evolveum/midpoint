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
import java.util.GregorianCalendar;

import javax.xml.namespace.QName;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.foo.UserType;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.polystring.PolyStringNormalizer;
import com.evolveum.midpoint.prism.polystring.PrismDefaultPolyStringNormalizer;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 *
 */
public class TestPolyString {
	
	@BeforeSuite
	public void setupDebug() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(DEFAULT_NAMESPACE_PREFIX);
		PrismTestUtil.resetPrismContext(new PrismInternalTestUtil());
	}
	
	@Test
	public void testSimpleNormalization() throws SchemaException, SAXException, IOException {
		System.out.println("===[ testSimpleNormalization ]===");
		
		// GIVEN
		String orig = " Gul\u00F4\u010Dka  v jam\u00F4\u010Dke le\u017E\u00ED, Per\u00FAn ju  bleskom usma\u017E\u00ED. Hrom do toho!  ";
		String norm = "gulocka v jamocke lezi perun ju bleskom usmazi hrom do toho";
		
		PolyString polyString = new PolyString(orig);
		
		PolyStringNormalizer normalizer = new PrismDefaultPolyStringNormalizer();
		
		// WHEN
		polyString.recompute(normalizer);
		
		// THEN
		assertEquals("orig have changed", orig, polyString.getOrig());
		assertEquals("wrong norm", norm, polyString.getNorm());
		assertEquals("wrong toString", orig, polyString.toString());
	}
	
	@Test
	public void testRecompute() throws SchemaException, SAXException, IOException {
		System.out.println("===[ testRecompute ]===");
		
		// GIVEN
		PrismContext ctx = constructInitializedPrismContext();
		PrismObjectDefinition<UserType> userDefinition = ctx.getSchemaRegistry().getObjectSchema().findObjectDefinitionByElementName(new QName(NS_FOO,"user"));
		PrismObject<UserType> user = userDefinition.instantiate();

		String orig = "Ľala ho papľuha";
		PolyString polyName = new PolyString(orig);
		
		PrismProperty<Object> polyNameProperty = user.findOrCreateProperty(USER_POLYNAME_QNAME);
		
		// WHEN
		polyNameProperty.setRealValue(polyName);
		
		// THEN
		assertEquals("Changed orig", orig, polyName.getOrig());
		assertEquals("Wrong norm", "lala ho papluha", polyName.getNorm());
		
	}
	
}

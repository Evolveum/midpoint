/*
 * Copyright (c) 2010-2018 Evolveum
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
package com.evolveum.midpoint.prism;

import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertEquals;
import static com.evolveum.midpoint.prism.PrismInternalTestUtil.*;

import java.io.IOException;

import javax.xml.namespace.QName;

import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

import com.evolveum.midpoint.prism.foo.UserType;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.polystring.PolyStringNormalizer;
import com.evolveum.midpoint.prism.polystring.AlphanumericPolyStringNormalizer;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
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
	public void testSimpleNormalization() {
		testNormalization("testSimpleNormalization",
				new AlphanumericPolyStringNormalizer(),
				" Gul\u00F4\u010Dka  v jam\u00F4\u010Dke le\u017E\u00ED, Per\u00FAn ju  bleskom usma\u017E\u00ED. Hrom do toho!  ",
				"gulocka v jamocke lezi perun ju bleskom usmazi hrom do toho");
	}

	@Test
	public void testNormalizationNonLatinAll() {
		testNormalization("testSimpleNormalization",
				new AlphanumericPolyStringNormalizer(),
				"\u0421\u043E\u044E\u0301\u0437 \u0421\u043E\u0432\u0435\u0301\u0442\u0441\u043A\u0438\u0445 \u0421\u043E\u0446\u0438\u0430\u043B\u0438\u0441\u0442\u0438\u0301\u0447\u0435\u0441\u043A\u0438\u0445 \u0420\u0435\u0441\u043F\u0443\u0301\u0431\u043B\u0438\u043A",
				"");
	}

	@Test
	public void testNormalizationNonLatinSome() {
		testNormalization("testSimpleNormalization",
				new AlphanumericPolyStringNormalizer(),
				"In \u0421\u043E\u044E\u0301\u0437 \u0421\u043E\u0432\u0435\u0301\u0442\u0441\u043A\u0438\u0445 \u0421\u043E\u0446\u0438\u0430\u043B\u0438\u0441\u0442\u0438\u0301\u0447\u0435\u0441\u043A\u0438\u0445 \u0420\u0435\u0441\u043F\u0443\u0301\u0431\u043B\u0438\u043A the TV watches YOU!!",
				"in the tv watches you");
	}

	private void testNormalization(final String TEST_NAME, PolyStringNormalizer normalizer, String orig, String expectedNorm) {
		System.out.println("===[ "+TEST_NAME+" ]===");
		PolyString polyString = new PolyString(orig);

		// WHEN
		polyString.recompute(normalizer);

		// THEN
		assertEquals("orig have changed", orig, polyString.getOrig());
		assertEquals("wrong norm", expectedNorm, polyString.getNorm());
		assertEquals("wrong toString", orig, polyString.toString());
	}

	@Test
	public void testRecompute() throws Exception {
		System.out.println("===[ testRecompute ]===");

		// GIVEN
		PrismContext ctx = constructInitializedPrismContext();
		PrismObjectDefinition<UserType> userDefinition = getFooSchema(ctx).findObjectDefinitionByElementName(new QName(NS_FOO,"user"));
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

	@Test
	public void testCompareTo() throws Exception {
		System.out.println("===[ testCompareTo ]===");

		// GIVEN
		String orig = "Ľala ho papľuha";
		PolyString polyName = new PolyString(orig);

		// WHEN, THEN
		assertTrue(polyName.compareTo("Ľala ho papľuha") == 0);
		assertTrue(polyName.compareTo(new PolyString("Ľala ho papľuha")) == 0);
		assertTrue(polyName.compareTo("something different") != 0);
		assertTrue(polyName.compareTo(new PolyString("something different")) != 0);
		assertTrue(polyName.compareTo("") != 0);
		assertTrue(polyName.compareTo(null) != 0);

	}

}

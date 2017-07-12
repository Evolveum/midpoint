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

package com.evolveum.midpoint.schema.parser;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xnode.XNode;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import java.io.File;
import java.io.IOException;
import java.util.Collection;

import static com.evolveum.midpoint.schema.TestConstants.COMMON_DIR_PATH;
import static org.testng.AssertJUnit.*;

/**
 * @author mederly
 */
public abstract class AbstractParserTest<T extends PrismValue> {

	protected String language;
	protected boolean namespaces;

	@BeforeSuite
	public void setup() throws SchemaException, SAXException, IOException {
		PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
		PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
	}

	@BeforeClass
	@Parameters({ "language", "namespaces" })
	public void temp(@Optional String language, @Optional Boolean namespaces) {
		this.language = language != null ? language : "xml";
		this.namespaces = namespaces != null ? namespaces : Boolean.TRUE;
		System.out.println("Testing with language = " + this.language + ", namespaces = " + this.namespaces);
	}

	protected File getFile(String baseName) {
		return new File(COMMON_DIR_PATH + "/" + language + "/" + (namespaces ? "ns":"no-ns"),
				baseName + "." + language);
	}

	protected void displayTestTitle(String testName) {
		PrismTestUtil.displayTestTitle(testName + " (" + language + ", " + (namespaces ? "with" : "no") + " namespaces)");
	}

	protected void assertPropertyDefinition(PrismContainer<?> container, String propName, QName xsdType, int minOccurs,
			int maxOccurs) {
		QName propQName = new QName(SchemaConstantsGenerated.NS_COMMON, propName);
		PrismAsserts.assertPropertyDefinition(container, propQName, xsdType, minOccurs, maxOccurs);
	}

	protected void assertPropertyValue(PrismContainer<?> container, String propName, Object propValue) {
		QName propQName = new QName(SchemaConstantsGenerated.NS_COMMON, propName);
		PrismAsserts.assertPropertyValue(container, propQName, propValue);
	}

	protected <T> void assertPropertyValues(PrismContainer<?> container, String propName, T... expectedValues) {
		QName propQName = new QName(SchemaConstantsGenerated.NS_COMMON, propName);
		PrismAsserts.assertPropertyValue(container, propQName, expectedValues);
	}

	protected void assertContainerDefinition(PrismContainer container, String contName, QName xsdType, int minOccurs,
			int maxOccurs) {
		QName qName = new QName(SchemaConstantsGenerated.NS_COMMON, contName);
		PrismAsserts.assertDefinition(container.getDefinition(), qName, xsdType, minOccurs, maxOccurs);
	}

	// partly covers the same functionality as item.assertDefinitions (TODO clean this)
	protected void assertDefinitions(Visitable value) {
		value.accept(v -> {
			if (v instanceof Item) {
				Item item = (Item) v;
				String label = item.getPath() + ": " + v;
				//System.out.println("Checking " + label);
				if (item.getDefinition() == null) {
					assertTrue("No definition in " + label, isDynamic(item.getPath()));
				} else {
					assertNotNull("No prism context in definition of " + label, item.getDefinition().getPrismContext());
				}
			} else if (v instanceof PrismContainerValue) {
				PrismContainerValue pcv = (PrismContainerValue) v;
				String label = pcv.getPath() + ": " + v;
				//System.out.println("Checking " + label);
				if (pcv.getComplexTypeDefinition() == null) {
					fail("No complex type definition in " + label);
				} else {
					assertNotNull("No prism context in definition of " + label, pcv.getComplexTypeDefinition().getPrismContext());
				}
			}
		});
	}

	protected void assertResolvableRawValues(Visitable value) {
		value.accept(v -> {
			// TODO in RawTypes in beans?
			if (v instanceof PrismPropertyValue) {
				PrismPropertyValue ppv = (PrismPropertyValue) v;
				XNode raw = ppv.getRawElement();
				if (raw != null && raw.getTypeQName() != null) {
					String label = ppv.getPath() + ": " + v;
					fail("Resolvable raw value of " + raw + " in " + label + " (type: " + raw.getTypeQName() + ")");
				}
			}
		});
	}

	protected void assertPrismContext(Visitable value) {
		value.accept(v -> {
			if (v instanceof Item) {
				Item item = (Item) v;
				String label = item.getPath() + ": " + v;
				assertNotNull("No prism context in " + label, item.getPrismContextLocal());
			} else if (v instanceof PrismContainerValue) {
				PrismContainerValue pcv = (PrismContainerValue) v;
				String label = pcv.getPath() + ": " + v;
				assertNotNull("No prism context in " + label, pcv.getPrismContextLocal());
			}
		});
	}

	private boolean isDynamic(ItemPath path) {
		for (ItemPathSegment segment : path.getSegments()) {
			if (segment instanceof NameItemPathSegment) {
				QName name = ((NameItemPathSegment) segment).getName();
				if (QNameUtil.match(name, ShadowType.F_ATTRIBUTES) || QNameUtil.match(name, ObjectType.F_EXTENSION)) {
					return true;
				}
			}
		}
		return false;
	}

	@FunctionalInterface
	interface ParsingFunction<V> {
		V apply(PrismParser prismParser) throws Exception;
	}

	@FunctionalInterface
	interface SerializingFunction<V> {
		String apply(V value) throws Exception;
	}

	protected void process(String desc, ParsingFunction<T> parser, SerializingFunction<T> serializer, String serId) throws Exception {
		PrismContext prismContext = getPrismContext();

		System.out.println("================== Starting test for '" + desc + "' (serializer: " + serId + ") ==================");

		T value = parser.apply(prismContext.parserFor(getFile()));
		assertResolvableRawValues(value);		// should be right here, before any getValue is called (TODO reconsider)

		System.out.println("Parsed value: " + desc);
		System.out.println(value.debugDump());

		assertPrismValue(value);

		if (serializer != null) {

			String serialized = serializer.apply(value);
			System.out.println("Serialized:\n" + serialized);

			T reparsed = parser.apply(prismContext.parserFor(serialized));
			assertResolvableRawValues(reparsed);		// should be right here, before any getValue is called (TODO reconsider)

			System.out.println("Reparsed: " + desc);
			System.out.println(reparsed.debugDump());

			assertPrismValue(reparsed);

			Collection<? extends ItemDelta> deltas = value.diff(reparsed);
			assertTrue("Deltas not empty", deltas.isEmpty());

			assertTrue("Values not equal", value.equals(reparsed));
		}
	}

	protected abstract File getFile();

	protected abstract void assertPrismValue(T value) throws SchemaException;

	protected boolean isContainer() {
		return false;
	}

	protected PrismContext getPrismContext() {
		return PrismTestUtil.getPrismContext();
	}

}

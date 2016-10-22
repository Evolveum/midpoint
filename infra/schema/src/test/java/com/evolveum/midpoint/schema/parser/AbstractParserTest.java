/*
 * Copyright (c) 2010-2016 Evolveum
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
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
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
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

/**
 * @author mederly
 */
public abstract class AbstractParserTest<C extends Containerable> {

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

	protected void assertDefinitions(Visitable value) {
		value.accept(v -> {
			if (v instanceof Item) {
				Item item = (Item) v;
				String label = item.getPath() + ": " + v;
				//System.out.println("Checking " + label);
				assertTrue("No definition in " + label, item.getDefinition() != null || isDynamic(item.getPath()));
			} else if (v instanceof PrismContainerValue) {
				PrismContainerValue pcv = (PrismContainerValue) v;
				String label = pcv.getPath() + ": " + v;
				//System.out.println("Checking " + label);
				assertNotNull("No complex type definition in " + label, pcv.getComplexTypeDefinition());
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

	protected void process(String desc, ParsingFunction<PrismContainerValue<C>> parser, SerializingFunction<PrismContainerValue<C>> serializer, String serId) throws Exception {
		PrismContext prismContext = getPrismContext();

		System.out.println("================== Starting test for '" + desc + "' (serializer: " + serId + ") ==================");

		PrismContainerValue<C> value = parser.apply(prismContext.parserFor(getFile()));

		System.out.println("Parsed value: " + desc);
		System.out.println(value.debugDump());

		assertPrismContainerValue(value);

		if (serializer != null) {

			String serialized = serializer.apply(value);
			System.out.println("Serialized:\n" + serialized);

			PrismContainerValue<C> reparsed = parser.apply(prismContext.parserFor(serialized));

			System.out.println("Reparsed: " + desc);
			System.out.println(reparsed.debugDump());

			assertPrismContainerValue(reparsed);

			Collection<? extends ItemDelta> deltas = ((PrismContainerValue) value).diff((PrismContainerValue) reparsed);
			assertTrue("Deltas not empty", deltas.isEmpty());

			assertTrue("Values not equal", value.equals(reparsed));
		}
	}

	protected abstract File getFile();

	@SuppressWarnings("Convert2MethodRef")
	protected void processParsings(Class<C> clazz, Class<? extends C> specificClass, QName type, QName specificType, SerializingFunction<PrismContainerValue<C>> serializer, String serId) throws Exception {
		process("parseItemValue - no hint", p -> p.parseItemValue(), serializer, serId);

		if (clazz != null) {
			process("parseItemValue - " + clazz.getSimpleName() + ".class",
					p -> p.type(clazz).parseItemValue(),
					serializer, serId);
		}

		if (specificClass != null) {
			process("parseItemValue - " + specificClass.getSimpleName() + ".class",
					p -> p.type(specificClass).parseItemValue(),
					serializer, serId);
		}

		if (type != null) {
			process("parseItemValue - " + type.getLocalPart() + " (QName)",
					p -> p.type(type).parseItemValue(),
					serializer, serId);
		}

		if (specificType != null) {
			process("parseItemValue - " + specificType.getLocalPart() + " (QName)",
					p -> p.type(specificType).parseItemValue(),
					serializer, serId);
		}

		process("parseRealValue - no hint",
				p -> ((C) p.parseRealValue()).asPrismContainerValue(),
				serializer, serId);

		if (clazz != null) {
			process("parseRealValue - " + clazz.getSimpleName() + ".class",
					p -> p.parseRealValue(clazz).asPrismContainerValue(),
					serializer, serId);
		}

		if (specificClass != null) {
			process("parseRealValue - " + specificClass.getSimpleName() + ".class",
					p -> p.parseRealValue(specificClass).asPrismContainerValue(),
					serializer, serId);
		}

		if (type != null) {
			process("parseRealValue - " + type.getLocalPart() + " (QName)",
					p -> ((C) p.type(type).parseRealValue()).asPrismContainerValue(),
					serializer, serId);
		}

		if (specificType != null) {
			process("parseRealValue - " + specificType.getLocalPart() + " (QName)",
					p -> ((C) p.type(specificType).parseRealValue()).asPrismContainerValue(),
					serializer, serId);
		}

		process("parseAnyData",
				p -> ((PrismContainer<C>) p.parseItemOrRealValue()).getValue(0),
				serializer, serId);
	}


	protected abstract void assertPrismContainerValue(PrismContainerValue<C> value) throws SchemaException;

	protected PrismContext getPrismContext() {
		return PrismTestUtil.getPrismContext();
	}

}

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
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.util.exception.SchemaException;

import javax.xml.namespace.QName;

import static org.testng.AssertJUnit.assertTrue;

/**
 * @author mederly
 */
public abstract class AbstractObjectParserTest<O extends Objectable> extends AbstractContainerValueParserTest<O> {

	protected void processObject(String desc, ParsingFunction<PrismObject<O>> parser,
			SerializingFunction<PrismObject<O>> serializer, String serId, boolean checkItemName) throws Exception {
		PrismContext prismContext = getPrismContext();

		System.out.println("================== Starting test for '" + desc + "' (serializer: " + serId + ") ==================");

		PrismObject<O> value = parser.apply(prismContext.parserFor(getFile()));
		assertResolvableRawValues(value);		// should be right here before getValue method is called

		System.out.println("Parsed object: " + desc);
		System.out.println(value.debugDump());

		assertPrismObject(value);

		if (serializer != null) {

			String serialized = serializer.apply(value);
			System.out.println("Serialized:\n" + serialized);

			PrismObject<O> reparsed = parser.apply(prismContext.parserFor(serialized));
			assertResolvableRawValues(reparsed);		// should be right here before getValue method is called

			System.out.println("Reparsed: " + desc);
			System.out.println(reparsed.debugDump());

			assertPrismObject(reparsed);

			ObjectDelta<O> delta = value.diff(reparsed);
			assertTrue("Delta not empty", delta.isEmpty());

			if (checkItemName) {
				assertTrue("Values not equal", value.equals(reparsed));
			}
			assertTrue("Values not equal", value.asObjectable().equals(reparsed.asObjectable()));
		}
	}

	@SuppressWarnings("Convert2MethodRef")
	protected void processObjectParsings(Class<O> clazz, QName type, SerializingFunction<PrismObject<O>> serializer, String serId,
			boolean checkItemName) throws Exception {
		processObject("parse - no hint", p -> p.parse(), serializer, serId, checkItemName);

		if (clazz != null) {
			processObject("parse - " + clazz.getSimpleName() + ".class",
					p -> p.type(clazz).parse(),
					serializer, serId, checkItemName);
		}

		if (type != null) {
			processObject("parse - " + type.getLocalPart() + " (QName)",
					p -> p.type(type).parse(),
					serializer, serId, checkItemName);
		}

		processObject("parseRealValue - no hint",
				p -> ((O) p.parseRealValue()).asPrismObject(),
				serializer, serId, checkItemName);

		if (clazz != null) {
			processObject("parseRealValue - " + clazz.getSimpleName() + ".class",
					p -> p.parseRealValue(clazz).asPrismObject(),
					serializer, serId, checkItemName);
		}

		if (type != null) {
			processObject("parseRealValue - " + type.getLocalPart() + " (QName)",
					p -> ((O) p.type(type).parseRealValue()).asPrismObject(),
					serializer, serId, checkItemName);
		}

		processObject("parseAnyData",
				p -> ((PrismObject<O>) p.parseItemOrRealValue()),
				serializer, serId, checkItemName);
	}

	protected void assertPrismObject(PrismObject<O> object) throws SchemaException {
		assertDefinitions(object);
		object.checkConsistence();
		object.assertDefinitions(true, "");
		assertPrismContext(object);
		assertPrismObjectLocal(object);
	}

	protected abstract void assertPrismObjectLocal(PrismObject<O> object) throws SchemaException;

}

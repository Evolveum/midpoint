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

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

import javax.xml.namespace.QName;

/**
 * @author mederly
 */
public abstract class AbstractContainerValueParserTest<C extends Containerable> extends AbstractParserTest<PrismContainerValue<C>> {

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

		if (isContainer()) {
			process("parseAnyData",
					p -> ((PrismContainer<C>) p.parseItemOrRealValue()).getValue(0),
					serializer, serId);
		}
	}

	protected boolean isContainer() {
		return true;
	}

	@Override
	protected void assertPrismValue(PrismContainerValue<C> value) throws SchemaException {
		assertDefinitions(value);
		assertPrismContext(value);
		assertPrismContainerValueLocal(value);
	}

	protected abstract void assertPrismContainerValueLocal(PrismContainerValue<C> value) throws SchemaException;

	protected PrismContext getPrismContext() {
		return PrismTestUtil.getPrismContext();
	}

}

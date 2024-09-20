/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.parser;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.util.exception.SchemaException;

import javax.xml.namespace.QName;

public abstract class AbstractPropertyValueParserTest<T> extends AbstractPrismValueParserTest<PrismPropertyValue<T>> {

    @SuppressWarnings("Convert2MethodRef")
    protected void processParsings(Class<T> clazz, QName type, PrismPropertyDefinition<T> definition, SerializingFunction<PrismPropertyValue<T>> serializer, String serId) throws Exception {
        process("parseItemValue - no hint", p -> p.parseItemValue(), serializer, serId);

        if (clazz != null) {
            process("parseItemValue - " + clazz.getSimpleName() + ".class",
                    p -> p.type(clazz).parseItemValue(),
                    serializer, serId);
        }

        if (type != null) {
            process("parseItemValue - " + type.getLocalPart() + " (QName)",
                    p -> p.type(type).parseItemValue(),
                    serializer, serId);
        }

        process("parseRealValue - no hint",
                p -> makePPV((T) p.parseRealValue(), definition),
                serializer, serId);

        if (clazz != null) {
            process("parseRealValue - " + clazz.getSimpleName() + ".class",
                    p -> makePPV(p.parseRealValue(clazz), definition),
                    serializer, serId);
        }

        if (type != null) {
            process("parseRealValue - " + type.getLocalPart() + " (QName)",
                    p -> makePPV((T) p.type(type).parseRealValue(), definition),
                    serializer, serId);
        }

//        process("parseAnyData",
//                p -> ((PrismContainer<C>) p.parseItemOrRealValue()).getValue(0),
//                serializer, serId);
    }

    protected PrismPropertyValue<T> makePPV(T realValue, PrismPropertyDefinition<T> definition) {
        PrismProperty<T> property = definition.instantiate();
        property.setRealValue(realValue);
        return property.getAnyValue();
    }

    protected boolean isContainer() {
        return true;
    }

    @Override
    protected void assertPrismValue(PrismPropertyValue<T> value) throws SchemaException {
        assertDefinitions(value);
        assertPrismContext(value);
        assertPrismPropertyValueLocal(value);
    }

    protected abstract void assertPrismPropertyValueLocal(PrismPropertyValue<T> value) throws SchemaException;
}

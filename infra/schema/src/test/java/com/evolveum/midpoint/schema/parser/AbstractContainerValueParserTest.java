/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.parser;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.util.exception.SchemaException;

import javax.xml.namespace.QName;

/**
 * @author mederly
 */
public abstract class AbstractContainerValueParserTest<C extends Containerable>
        extends AbstractPrismValueParserTest<PrismContainerValue<C>> {

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

        //noinspection unchecked
        process("parseRealValue - no hint",
                p -> ((C) p.parseRealValue()).asPrismContainerValue(),
                serializer, serId);

        if (clazz != null) {
            //noinspection unchecked
            process("parseRealValue - " + clazz.getSimpleName() + ".class",
                    p -> p.parseRealValue(clazz).asPrismContainerValue(),
                    serializer, serId);
        }

        if (specificClass != null) {
            //noinspection unchecked
            process("parseRealValue - " + specificClass.getSimpleName() + ".class",
                    p -> p.parseRealValue(specificClass).asPrismContainerValue(),
                    serializer, serId);
        }

        if (type != null) {
            //noinspection unchecked
            process("parseRealValue - " + type.getLocalPart() + " (QName)",
                    p -> ((C) p.type(type).parseRealValue()).asPrismContainerValue(),
                    serializer, serId);
        }

        if (specificType != null) {
            //noinspection unchecked
            process("parseRealValue - " + specificType.getLocalPart() + " (QName)",
                    p -> ((C) p.type(specificType).parseRealValue()).asPrismContainerValue(),
                    serializer, serId);
        }

        if (isContainer()) {
            //noinspection unchecked,deprecation
            process("parseAnyData",
                    p -> ((PrismContainer<C>) p.parseItemOrRealValue()).getAnyValue(),
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

}

/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.parser;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import org.testng.annotations.Test;

import javax.xml.namespace.QName;
import java.io.File;

/**
 * @author mederly
 *
 */
@SuppressWarnings("Convert2MethodRef")
public class TestParseMapping extends AbstractContainerValueParserTest<MappingType> {

    @Override
    protected File getFile() {
        return getFile("mapping");
    }

    @Test
    public void testParseFile() throws Exception {
        processParsings(null, null);
    }

    @Test
    public void testParseRoundTrip() throws Exception{
        processParsings(v -> getPrismContext().serializerFor(language).root(new QName("dummy")).serialize(v), "s1");
        processParsings(v -> getPrismContext().serializerFor(language).root(SchemaConstantsGenerated.C_USER).serialize(v), "s2");        // misleading item name
        processParsings(v -> getPrismContext().serializerFor(language).root(new QName("dummy")).serializeRealValue(v.asContainerable()), "s3");
        processParsings(v -> getPrismContext().serializerFor(language).root(new QName("dummy")).serializeAnyData(v.asContainerable()), "s4");
    }

    private void processParsings(SerializingFunction<PrismContainerValue<MappingType>> serializer, String serId) throws Exception {
        processParsings(MappingType.class, null, MappingType.COMPLEX_TYPE, null, serializer, serId);
    }

    @Override
    protected void assertPrismContainerValueLocal(PrismContainerValue<MappingType> value)
            throws SchemaException {
        // TODO

    }

    @Override
    protected boolean isContainer() {
        return false;
    }

}

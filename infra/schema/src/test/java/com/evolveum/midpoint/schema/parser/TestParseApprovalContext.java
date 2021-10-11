/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.parser;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalContextType;
import org.testng.annotations.Test;

import javax.xml.namespace.QName;
import java.io.File;

import static org.testng.AssertJUnit.assertEquals;

/**
 * TODO finish
 * @author mederly
 *
 */
@SuppressWarnings({ "Convert2MethodRef", "Duplicates" })
public class TestParseApprovalContext extends AbstractContainerValueParserTest<ApprovalContextType> {

    @Override
    protected File getFile() {
        return getFile("approval-context");
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

    private void processParsings(SerializingFunction<PrismContainerValue<ApprovalContextType>> serializer, String serId) throws Exception {
        processParsings(ApprovalContextType.class, null, ApprovalContextType.COMPLEX_TYPE, null, serializer, serId);
    }

    @Override
    public void assertPrismContainerValueLocal(PrismContainerValue<ApprovalContextType> value) throws SchemaException {
        ApprovalContextType wfc = value.asContainerable();
//        assertEquals("Wrong # of events", 1, wfc.getEvent().size());
//        assertEquals("Wrong type of first event", WorkItemCompletionEventType.class, wfc.getEvent().get(0).getClass());

    }

    @Override
    protected boolean isContainer() {
        return false;
    }
}

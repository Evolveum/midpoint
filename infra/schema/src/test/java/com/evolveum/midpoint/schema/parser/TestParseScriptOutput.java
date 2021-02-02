/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.parser;

import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.PipelineDataType;
import org.apache.commons.lang3.StringUtils;
import org.testng.annotations.Test;

import javax.xml.namespace.QName;
import java.io.File;

import static org.testng.AssertJUnit.assertEquals;

/**
 * @author mederly
 *
 */
public class TestParseScriptOutput extends AbstractPropertyValueParserTest<PipelineDataType> {

    @Override
    protected File getFile() {
        return getFile("script-output");
    }

    @Test
    public void testParseToXNode() throws Exception {
        String file = MiscUtil.readFile(getFile());
        System.out.println("Original text:\n" + file);

        RootXNode xnode = getPrismContext().parserFor(file).parseToXNode();
        System.out.println("XNode:\n" + xnode.debugDump());

        System.out.println("source -> XNode -> JSON:\n" + getPrismContext().jsonSerializer().serialize(xnode));
        System.out.println("source -> XNode -> YAML:\n" + getPrismContext().yamlSerializer().serialize(xnode));
        System.out.println("source -> XNode -> XML:\n" + getPrismContext().xmlSerializer().serialize(xnode));
    }

    @Test
    public void testParseFile() throws Exception {
        processParsings(null, null);
    }

    @Test
    public void testParseRoundTrip() throws Exception {
        processParsings(v -> getPrismContext().serializerFor(language).serialize(v), "s0");
        processParsings(v -> getPrismContext().serializerFor(language).root(new QName("dummy")).serialize(v), "s1");
        processParsings(v -> getPrismContext().serializerFor(language).root(SchemaConstantsGenerated.C_USER).serialize(v), "s2");        // misleading item name
        processParsings(v -> getPrismContext().serializerFor(language).serializeRealValue(v.getValue()), "s3");
        processParsings(v -> getPrismContext().serializerFor(language).root(new QName("dummy")).serializeAnyData(v.getValue()), "s4");
    }

    @Test
    public void testNamespaces() throws Exception {
        String file = MiscUtil.readFile(getFile());
        RootXNode xnode = getPrismContext().parserFor(file).parseToXNode();
        System.out.println("XNode:\n" + xnode.debugDump());

        String serialized = getPrismContext().xmlSerializer().serialize(xnode);
        System.out.println("XML: " + serialized);

        final String common_3 = "http://midpoint.evolveum.com/xml/ns/public/common/common-3";

        int count = StringUtils.countMatches(serialized, common_3);
        // temporarily set from 2 to 3 (some XML weirdness in Xerces 2.12/Xalan 2.7.2); see MID-5661
        assertEquals("Wrong # of occurrences of '" + common_3 + "' in serialized form", 2, count);
    }

//    private void assertNamespaceDeclarations(String context, Element element, String... prefixes) {
//        assertEquals("Wrong namespace declarations for " + context, new HashSet<>(Arrays.asList(prefixes)),
//                DOMUtil.getNamespaceDeclarations(element).keySet());
//    }

    private void processParsings(SerializingFunction<PrismPropertyValue<PipelineDataType>> serializer, String serId) throws Exception {
        PrismPropertyDefinition definition = getPrismContext().getSchemaRegistry().findPropertyDefinitionByElementName(SchemaConstants.S_PIPELINE_DATA);
        processParsings(PipelineDataType.class, PipelineDataType.COMPLEX_TYPE, definition, serializer, serId);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void assertPrismPropertyValueLocal(PrismPropertyValue<PipelineDataType> value) throws SchemaException {
        // TODO
    }

    @Override
    protected boolean isContainer() {
        return false;
    }
}

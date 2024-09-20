/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.schema.AbstractSchemaTest;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.DOMUtil;

import com.evolveum.midpoint.xml.ns._public.prism_schema_3.PrismSchemaType;
import com.evolveum.prism.xml.ns._public.types_3.SchemaDefinitionType;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class PrismSchemaTypeUtilTest extends AbstractSchemaTest {

    public static final String SCHEMA_PATH = "src/test/resources/schema/schema-extension.xml";

    @Test
    public void testConvertToPrismSchemaType() throws Exception {
        Document doc = DOMUtil.parseFile(SCHEMA_PATH);
        Element schemaElement = DOMUtil.getFirstChildElement(doc);

        SchemaDefinitionType schemaDef = new SchemaDefinitionType();
        schemaDef.setSchema(schemaElement);

        PrismSchemaType prismSchema = PrismSchemaTypeUtil.convertToPrismSchemaType(schemaDef, SchemaConstants.LIFECYCLE_ACTIVE);

        SchemaDefinitionType convertedSchema = PrismSchemaTypeUtil.convertToSchemaDefinitionType(prismSchema, SchemaConstants.LIFECYCLE_ACTIVE);

        Assert.assertEquals(
                DOMUtil.serializeDOMToString(convertedSchema.getSchema()), DOMUtil.serializeDOMToString(schemaElement));
    }
}

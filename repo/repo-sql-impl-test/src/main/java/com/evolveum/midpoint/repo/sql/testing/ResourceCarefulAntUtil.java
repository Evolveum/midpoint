/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.testing;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.fail;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;

import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismUtil;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceAttributeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaHandlingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.XmlSchemaType;
import com.evolveum.prism.xml.ns._public.types_3.SchemaDefinitionType;

/**
 * @author semancik
 *
 */
public class ResourceCarefulAntUtil {

    private static Random rnd = new Random();

    public static void initAnts(List<CarefulAnt<ResourceType>> ants, final File resourceFile, final PrismContext prismContext) {
        final PrismObjectDefinition<ResourceType> resourceDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ResourceType.class);
        ants.add(new CarefulAnt<ResourceType>() {
            @Override
            public ItemDelta<?,?> createDelta(int iteration) {
                return  prismContext.deltaFactory().property().createModificationReplaceProperty(ResourceType.F_DESCRIPTION,
                        resourceDef, "Blah "+iteration);
            }

            @Override
            public void assertModification(PrismObject<ResourceType> resource, int iteration) {
                assertEquals("Wrong description in iteration "+iteration, "Blah "+iteration, resource.asObjectable().getDescription());
            }
        });

        ants.add(new CarefulAnt<ResourceType>() {
            SchemaHandlingType schemaHandling;
            @Override
            public ItemDelta<?,?> createDelta(int iteration) throws SchemaException {
                schemaHandling = createNewSchemaHandling(resourceFile, iteration);
                return prismContext.deltaFactory().container().createModificationReplace(ResourceType.F_SCHEMA_HANDLING,
                        prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ResourceType.class),
                        schemaHandling.asPrismContainerValue().clone());
            }
            @Override
            public void assertModification(PrismObject<ResourceType> resource, int iteration) {
                if (!schemaHandling.equals(resource.asObjectable().getSchemaHandling())) {
                    System.out.println("Expected: " + PrismUtil.serializeQuietly(prismContext, schemaHandling));
                    System.out.println("Real: " + PrismUtil.serializeQuietly(prismContext, resource.asObjectable().getSchemaHandling()));
                    fail("Wrong schemaHandling in iteration" + iteration);
                }
                //assertEquals("Wrong schemaHandling in iteration "+iteration, schemaHandling, resource.asObjectable().getSchemaHandling());
            }
        });

        ants.add(new CarefulAnt<ResourceType>() {
            SchemaDefinitionType xmlSchemaDef;
            @Override
            public ItemDelta<?,?> createDelta(int iteration) throws SchemaException {
                xmlSchemaDef = createNewXmlSchemaDef(resourceFile, iteration);
                return prismContext.deltaFactory().property().createModificationReplaceProperty(
                        ItemPath.create(ResourceType.F_SCHEMA, XmlSchemaType.F_DEFINITION),
                        resourceDef, xmlSchemaDef);
            }
            @Override
            public void assertModification(PrismObject<ResourceType> resource, int iteration) {
                List<Element> orgigElements = xmlSchemaDef.getAny();
                List<Element> newElements = resource.asObjectable().getSchema().getDefinition().getAny();
                assertEquals("Wrong number of elements in schema definition in iteration "+iteration, orgigElements.size(), newElements.size());
                // TODO look inside elements
            }
        });
    }

    private static SchemaHandlingType createNewSchemaHandling(File resourceFile, int iteration) throws SchemaException {
        PrismObject<ResourceType> resource = parseResource(resourceFile);
        SchemaHandlingType schemaHandling = resource.asObjectable().getSchemaHandling();
        ResourceObjectTypeDefinitionType accountType = schemaHandling.getObjectType().iterator().next();
        List<ResourceAttributeDefinitionType> attrDefs = accountType.getAttribute();
        ResourceAttributeDefinitionType attributeDefinitionType = attrDefs.get(rnd.nextInt(attrDefs.size()));
        attributeDefinitionType.setDescription(Integer.toString(iteration));
        return schemaHandling;
    }

    private static SchemaDefinitionType createNewXmlSchemaDef(File resourceFile, int iteration) throws SchemaException {
        PrismObject<ResourceType> resource = parseResource(resourceFile);
        XmlSchemaType schema = resource.asObjectable().getSchema();
        SchemaDefinitionType def;
        if (schema == null) {
            def = new SchemaDefinitionType();
            def.getAny().add(DOMUtil.createElement(DOMUtil.XSD_SCHEMA_ELEMENT));
        } else {
            def = schema.getDefinition();
            // TODO: modify it somehow
        }
        return def;
    }

    private static PrismObject<ResourceType> parseResource(File resourceFile) throws SchemaException{
        try{
            return PrismContext.get().parseObject(resourceFile);
        } catch (IOException ex){
            throw new SchemaException(ex.getMessage(), ex);
        }
    }

}

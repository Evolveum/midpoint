/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.lang.test;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import org.testng.annotations.Test;

import com.evolveum.axiom.api.AxiomName;
import com.evolveum.axiom.api.schema.AxiomItemDefinition;
import com.evolveum.axiom.api.schema.AxiomSchemaContext;
import com.evolveum.axiom.api.schema.AxiomTypeDefinition;
import com.evolveum.axiom.lang.api.AxiomBuiltIn.Item;
import com.evolveum.axiom.lang.impl.ModelReactorContext;
import com.evolveum.axiom.lang.spi.AxiomSyntaxException;

public class TestAxiomPrism extends AbstractReactorTest {

    private static final AxiomName PERSON = AxiomName.from("https://example.org", "Person");
    private static final AxiomName STORAGE = AxiomName.from("https://example.org/extension", "type");

    private static final AxiomName PERSON_EXTENSION = AxiomName.from("https://schema.org", "SchemaOrgPerson");
    private static final String DIR = "prism/";
    private static final String PRISM = DIR + "prism-model.axiom";
    private static final String COMMON_CORE = DIR + "common-core.axiom";
    private static final String COMMON_CORE_PRISM = DIR + "common-core.prism";
    private static final String METADATA = DIR + "midpoint-metadata-test.axiom";

    private static final AxiomName PRISM_REFERENCE = AxiomName.from("http://midpoint.evolveum.com/xml/ns/public/common/prism", "ReferenceItemDefinition");

    private static final AxiomName MIDPOINT_STORAGE = AxiomName.from("http://midpoint.evolveum.com/xml/ns/public/common/common-3", "storage");

    private ModelReactorContext prismReactor() throws AxiomSyntaxException, IOException {
        ModelReactorContext context = ModelReactorContext.defaultReactor();
        context.loadModelFromSource(source(PRISM));
        AxiomSchemaContext schemaContext = context.computeSchemaContext();


        AxiomTypeDefinition referenceType = schemaContext.getType(PRISM_REFERENCE).get();

        Map<AxiomName, AxiomItemDefinition> itemDefs = referenceType.itemDefinitions();
        AxiomItemDefinition typeRef = referenceType.itemDefinitions().get(Item.TYPE_REFERENCE.name());

        assertTrue(typeRef.constantValue().isPresent());
        ModelReactorContext extendedLanguage = ModelReactorContext.reactor(schemaContext);
        extendedLanguage.loadModelFromSource(source(PRISM));
        return extendedLanguage;
    }

    @Test
    public void testPrismMetadataModel() throws IOException, AxiomSyntaxException {

        ModelReactorContext extendedLanguage = prismReactor();
        extendedLanguage.loadModelFromSource(source(METADATA));
        AxiomSchemaContext schemaContext = extendedLanguage.computeSchemaContext();
        AxiomTypeDefinition metadataType = schemaContext.getType(AxiomName.data("ValueMetadata")).get();

        Optional<AxiomItemDefinition> storageMetadata = metadataType.itemDefinition(MIDPOINT_STORAGE);
        assertTrue(storageMetadata.isPresent());
    }

}

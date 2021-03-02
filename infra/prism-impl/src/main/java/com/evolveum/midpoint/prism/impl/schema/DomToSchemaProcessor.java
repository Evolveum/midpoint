/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.schema;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.impl.PrismContextImpl;
import com.evolveum.midpoint.prism.schema.SchemaDescription;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.sun.xml.xsom.XSSchemaSet;
import com.sun.xml.xsom.parser.XSOMParser;
import com.sun.xml.xsom.util.DomAnnotationParserFactory;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;

/**
 * Parser for DOM-represented XSD, creates midPoint Schema representation.
 *
 * It will parse schema in several passes. DOM -> XSSchemaSet parsing
 * is done by this class. Postprocessing (creation of prism schemas) is delegated
 * to DomToSchemaPostProcessor.
 *
 * @author lazyman
 * @author Radovan Semancik
 */
class DomToSchemaProcessor {

    private static final Trace LOGGER = TraceManager.getTrace(DomToSchemaProcessor.class);

    private static final Object SCHEMA_PARSING = new Object();

    private EntityResolver entityResolver;
    private final PrismContext prismContext;
    private String shortDescription;

    DomToSchemaProcessor(EntityResolver entityResolver, PrismContext prismContext) {
        this.entityResolver = entityResolver;
        this.prismContext = prismContext;
    }

    /**
     * Parses single schema.
     */
    void parseSchema(@NotNull PrismSchemaImpl prismSchema, @NotNull Element xsdSchema, boolean isRuntime,
            boolean allowDelayedItemDefinitions, String shortDescription) throws SchemaException {
        parseSchema(prismSchema,SchemaSource.from(xsdSchema), isRuntime, allowDelayedItemDefinitions, shortDescription);
    }

    void parseSchema(@NotNull PrismSchemaImpl prismSchema, @NotNull SchemaSource xsdSchema, boolean isRuntime,
            boolean allowDelayedItemDefinitions, String shortDescription) throws SchemaException {
        this.shortDescription = shortDescription;
        XSSchemaSet xsSchemaSet = parseSchema(xsdSchema);
        if (xsSchemaSet == null) {
            return;
        }
        DomToSchemaPostProcessor postProcessor = new DomToSchemaPostProcessor(xsSchemaSet, prismContext);
        postProcessor.postprocessSchema(prismSchema, isRuntime, allowDelayedItemDefinitions, shortDescription);
    }

    /**
     * Parses several schemas, referenced by a wrapper schema.
     * Provided to allow circular references (e.g. common-3 -> scripting-3 -> common-3).
     */
    void parseSchemas(List<SchemaDescription> schemaDescriptions, Element wrapper,
            boolean allowDelayedItemDefinitions, String shortDescription) throws SchemaException {
        parseSchemas(schemaDescriptions, SchemaSource.from(wrapper), allowDelayedItemDefinitions, shortDescription);
    }

    void parseSchemas(List<SchemaDescription> schemaDescriptions, SchemaSource wrapper,
            boolean allowDelayedItemDefinitions, String shortDescription) throws SchemaException {
        this.shortDescription = shortDescription;
        XSSchemaSet xsSchemaSet = parseSchema(wrapper);
        if (xsSchemaSet == null) {
            return;
        }
        for (SchemaDescription schemaDescription : schemaDescriptions) {
            DomToSchemaPostProcessor postProcessor = new DomToSchemaPostProcessor(xsSchemaSet, prismContext);
            PrismSchemaImpl prismSchema = (PrismSchemaImpl) schemaDescription.getSchema();
            boolean isRuntime = schemaDescription.getCompileTimeClassesPackage() == null;
            String schemaShortDescription = schemaDescription.getSourceDescription() + " in " + shortDescription;
            postProcessor.postprocessSchema(prismSchema, isRuntime, allowDelayedItemDefinitions, schemaShortDescription);
        }
    }

    private XSSchemaSet parseSchema(SchemaSource schema) throws SchemaException {
        // Synchronization here is a brutal workaround for MID-5648. We need to synchronize on parsing schemas globally, because
        // it looks like there are many fragments (referenced schemas) that get resolved during parsing.
        //
        // Unfortunately, this is not sufficient by itself -- there is a pre-processing that must be synchronized as well.
        synchronized (SCHEMA_PARSING) {
            // Make sure that the schema parser sees all the namespace declarations


            try {
                InputSource inSource = schema.saxInputSource();
                // XXX: hack: it's here to make entity resolver work...
                inSource.setSystemId("SystemId");
                // XXX: end hack


                return parseSchema(inSource);

            } catch (SAXException e) {
                throw new SchemaException("XML error during XSD schema parsing: " + e.getMessage()
                        + "(embedded exception " + e.getException() + ") in " + shortDescription, e);
            } catch (RuntimeException e) {
                // This sometimes happens, e.g. NPEs in Saxon
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.error("Unexpected error {} during parsing of schema:\n{}", e.getMessage(),
                            DOMUtil.serializeDOMToString(schema.element()));
                }
                throw new SchemaException(
                        "XML error during XSD schema parsing: " + e.getMessage() + " in " + shortDescription, e);
            }
        }
    }

    static InputStream inputStreamFrom(Element schema) {
        DOMUtil.fixNamespaceDeclarations(schema);
        // Consider unifying with DOMUtil.printDOM
        try {
            TransformerFactory transfac = DOMUtil.setupTransformerFactory();
            Transformer trans = transfac.newTransformer();
            trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            trans.setOutputProperty(OutputKeys.INDENT, "yes");

            DOMSource source = new DOMSource(schema);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            StreamResult result = new StreamResult(out);

            trans.transform(source, result);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (TransformerException e) {
            throw new SystemException("Error in XML transformation: " + e.getMessage(), e);
        }
    }

    private XSSchemaSet parseSchema(InputSource inSource) throws SAXException {
        XSOMParser parser = createSchemaParser();
        parser.parse(inSource);
        return parser.getResult();
    }

    private XSOMParser createSchemaParser() {
        XSOMParser parser = new XSOMParser();
        if (entityResolver == null) {
            entityResolver = ((PrismContextImpl) prismContext).getEntityResolver();
            if (entityResolver == null) {
                throw new IllegalStateException(
                        "Entity resolver is not set (even tried to pull it from prism context)");
            }
        }
        SchemaHandler errorHandler = new SchemaHandler(entityResolver);
        parser.setErrorHandler(errorHandler);
        parser.setAnnotationParser(new DomAnnotationParserFactory());
        parser.setEntityResolver(errorHandler);

        return parser;
    }

}

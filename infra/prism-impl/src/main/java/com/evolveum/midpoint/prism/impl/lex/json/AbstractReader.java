/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.lex.json;

import com.evolveum.midpoint.prism.ParserSource;
import com.evolveum.midpoint.prism.ParsingContext;
import com.evolveum.midpoint.prism.impl.ParsingContextImpl;
import com.evolveum.midpoint.prism.impl.lex.LexicalProcessor;
import com.evolveum.midpoint.prism.impl.xnode.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.UniformItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Takes care of reading JSON/YAML to XNode.
 */
abstract class AbstractReader {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractReader.class);

    @NotNull protected final SchemaRegistry schemaRegistry;

    AbstractReader(@NotNull SchemaRegistry schemaRegistry) {
        this.schemaRegistry = schemaRegistry;
    }

    @NotNull
    RootXNodeImpl read(@NotNull ParserSource source, @NotNull ParsingContext parsingContext) throws SchemaException, IOException {
        List<RootXNodeImpl> nodes = readInternal(source, parsingContext, false);
        if (nodes.isEmpty()) {
            throw new SchemaException("No data at input");
        } else if (nodes.size() > 1) {
            throw new SchemaException("More than one object found: " + nodes); // should not occur
        } else {
            return nodes.get(0);
        }
    }

    /**
     * Honors multi-document files and multiple objects in a single document (list-as-root mechanisms).
     */
    @NotNull
    List<RootXNodeImpl> readObjects(@NotNull ParserSource source, @NotNull ParsingContext parsingContext) throws SchemaException, IOException {
        return readInternal(source, parsingContext, true);
    }

    @NotNull
    private List<RootXNodeImpl> readInternal(@NotNull ParserSource source, @NotNull ParsingContext parsingContext,
            boolean expectingMultipleObjects) throws SchemaException, IOException {
        InputStream is = source.getInputStream();
        try {
            JsonParser parser = createJacksonParser(is);
            List<RootXNodeImpl> rv = new ArrayList<>();
            readFromStart(parser, parsingContext, rv::add, expectingMultipleObjects);
            return rv;
        } finally {
            if (source.closeStreamAfterParsing()) {
                closeQuietly(is);
            }
        }
    }

    private void closeQuietly(InputStream is) {
        if (is != null) {
            try {
                is.close();
            } catch (IOException e) {
                LoggingUtils.logExceptionAsWarning(LOGGER, "Couldn't close the input stream", e);
            }
        }
    }

    void readObjectsIteratively(@NotNull ParserSource source, @NotNull ParsingContext parsingContext,
            LexicalProcessor.RootXNodeHandler handler) throws SchemaException, IOException {
        InputStream is = source.getInputStream();
        try {
            JsonParser parser = createJacksonParser(is);
            readFromStart(parser, parsingContext, handler, true);
        } finally {
            if (source.closeStreamAfterParsing()) {
                closeQuietly(is);
            }
        }
    }

    protected abstract JsonParser createJacksonParser(InputStream stream) throws SchemaException, IOException;

    @FunctionalInterface
    interface YamlTagResolver {
        QName tagToTypeName(Object tid, JsonReadingContext ctx) throws IOException, SchemaException;
    }

    private void readFromStart(JsonParser unconfiguredParser, ParsingContext parsingContext,
            LexicalProcessor.RootXNodeHandler handler, boolean expectingMultipleObjects) throws SchemaException, IOException {
        JsonParser configuredParser = configureParser(unconfiguredParser);
        JsonReadingContext ctx = new JsonReadingContext(configuredParser, (ParsingContextImpl) parsingContext);
        ReadOperation readOperation = new ReadOperation(configuredParser, ctx, handler, expectingMultipleObjects,
                this::tagToTypeName, schemaRegistry.getPrismContext());
        readOperation.execute();
    }

    private JsonParser configureParser(JsonParser parser) {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule sm = new SimpleModule();
        sm.addDeserializer(QName.class, new QNameDeserializer());
        sm.addDeserializer(UniformItemPath.class, new ItemPathDeserializer());
        sm.addDeserializer(ItemPath.class, new ItemPathDeserializer());
        sm.addDeserializer(PolyString.class, new PolyStringDeserializer());
        sm.addDeserializer(ItemPathType.class, new ItemPathTypeDeserializer());

        mapper.registerModule(sm);
        parser.setCodec(mapper);
        return parser;
    }

    protected abstract QName tagToTypeName(Object tid, JsonReadingContext ctx) throws IOException, SchemaException;

    public abstract boolean canRead(@NotNull File file) throws IOException;
    public abstract boolean canRead(@NotNull String dataString);


    static QName determineNumberType(JsonParser.NumberType numberType) throws SchemaException {
        switch (numberType) {
            case BIG_DECIMAL:
                return DOMUtil.XSD_DECIMAL;
            case BIG_INTEGER:
                return DOMUtil.XSD_INTEGER;
            case LONG:
                return DOMUtil.XSD_LONG;
            case INT:
                return DOMUtil.XSD_INT;
            case FLOAT:
                return DOMUtil.XSD_FLOAT;
            case DOUBLE:
                return DOMUtil.XSD_DOUBLE;
            default:
                throw new SchemaException("Unsupported number type: " + numberType);
        }
    }
}

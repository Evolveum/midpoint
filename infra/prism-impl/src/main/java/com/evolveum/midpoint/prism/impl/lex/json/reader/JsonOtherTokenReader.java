/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.lex.json.reader;

import java.io.IOException;
import java.util.Objects;
import javax.xml.namespace.QName;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismNamespaceContext;
import com.evolveum.midpoint.prism.impl.lex.json.JsonNullValueParser;
import com.evolveum.midpoint.prism.impl.lex.json.JsonValueParser;
import com.evolveum.midpoint.prism.impl.xnode.ListXNodeImpl;
import com.evolveum.midpoint.prism.impl.xnode.PrimitiveXNodeImpl;
import com.evolveum.midpoint.prism.impl.xnode.XNodeImpl;
import com.evolveum.midpoint.prism.xnode.ValueParser;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * TODO
 */
class JsonOtherTokenReader {

    private static final Trace LOGGER = TraceManager.getTrace(JsonOtherTokenReader.class);

    @NotNull private final JsonReadingContext ctx;
    @NotNull private final JsonParser parser;

    private final PrismNamespaceContext parentContext;

    JsonOtherTokenReader(JsonReadingContext ctx, PrismNamespaceContext context) {
        this.ctx = ctx;
        this.parser = ctx.parser;
        this.parentContext = context;
    }

    @NotNull XNodeImpl readValue() throws IOException, SchemaException {
        JsonToken currentToken = Objects.requireNonNull(parser.currentToken(), "currentToken");

        switch (currentToken) {
            case START_OBJECT:
                return new JsonObjectTokenReader(ctx, parentContext).read();
            case START_ARRAY:
                return parseToList();
            case VALUE_STRING:
            case VALUE_TRUE:
            case VALUE_FALSE:
            case VALUE_NUMBER_FLOAT:
            case VALUE_NUMBER_INT:
            case VALUE_EMBEDDED_OBJECT:             // assuming it's a scalar value e.g. !!binary (TODO)
                return parseToPrimitive();
            case VALUE_NULL:
                return parseToEmptyPrimitive();
            default:
                throw new SchemaException("Unexpected current token: " + currentToken + ". At: " + ctx.getPositionSuffix());
        }
    }

    private ListXNodeImpl parseToList() throws SchemaException, IOException {
        Validate.notNull(parser.currentToken());

        ListXNodeImpl list = new ListXNodeImpl();
        Object tid = parser.getTypeId();
        if (tid != null) {
            list.setTypeQName(ctx.yamlTagResolver.tagToTypeName(tid, ctx));
        }
        for (;;) {
            JsonToken token = parser.nextToken();
            if (token == null) {
                ctx.prismParsingContext.warnOrThrow(LOGGER, "Unexpected end of data while parsing a list structure at " + ctx.getPositionSuffix());
                return list;
            } else if (token == JsonToken.END_ARRAY) {
                return list;
            } else {
                list.add(readValue());
            }
        }
    }

    private <T> PrimitiveXNodeImpl<T> parseToPrimitive() throws IOException, SchemaException {
        PrimitiveXNodeImpl<T> primitive = new PrimitiveXNodeImpl<>(this.parentContext);

        Object tid = parser.getTypeId();
        if (tid != null) {
            QName typeName = ctx.yamlTagResolver.tagToTypeName(tid, ctx);
            primitive.setTypeQName(typeName);
            primitive.setExplicitTypeDeclaration(true);
        } else {
            // We don't try to determine XNode type from the implicit JSON/YAML type (integer, number, ...),
            // because XNode type prescribes interpretation in midPoint. E.g. YAML string type would be interpreted
            // as xsd:string, even if the schema would expect e.g. timestamp.
        }

        JsonNode jn = parser.readValueAs(JsonNode.class);
        ValueParser<T> vp = new JsonValueParser<>(parser, jn);
        primitive.setValueParser(vp);

        return primitive;
    }

    private <T> PrimitiveXNodeImpl<T> parseToEmptyPrimitive() {
        PrimitiveXNodeImpl<T> primitive = new PrimitiveXNodeImpl<>();
        primitive.setValueParser(new JsonNullValueParser<>());
        return primitive;
    }

//    @SuppressWarnings("unused") // TODO remove if not needed
//    private QName getCurrentTypeName(JsonReadingContext ctx) throws IOException, SchemaException {
//        switch (parser.currentToken()) {
//            case VALUE_NUMBER_INT:
//            case VALUE_NUMBER_FLOAT:
//                return AbstractReader.determineNumberType(parser.getNumberType());
//            case VALUE_FALSE:
//            case VALUE_TRUE:
//                return DOMUtil.XSD_BOOLEAN;
//            case VALUE_STRING:
//                return DOMUtil.XSD_STRING;
//            case VALUE_NULL:
//                return null;            // TODO?
//            default:
//                throw new SchemaException("Unexpected current token type: " + parser.currentToken() + "/" + parser.getText() + " at " + ctx.getPositionSuffix());
//        }
//    }
}

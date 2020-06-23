/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.lex.json.reader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.xml.namespace.QName;

import com.fasterxml.jackson.core.JsonParser;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.impl.lex.json.yaml.MidpointYAMLFactory;
import com.evolveum.midpoint.prism.impl.lex.json.yaml.MidpointYAMLParser;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;

public class YamlReader extends AbstractReader {

    private static final String YAML = "tag:yaml.org,2002:";
    private static final String TAG_STRING = YAML + "str";
    private static final String TAG_INT = YAML + "int";
    private static final String TAG_BOOL = YAML + "bool";
    private static final String TAG_FLOAT = YAML + "float";
    private static final String TAG_BINARY = YAML + "binary";       // base64-encoded string
    private static final String TAG_NULL = YAML + "null";

    public YamlReader(@NotNull SchemaRegistry schemaRegistry) {
        super(schemaRegistry);
    }

    @Override
    public boolean canRead(@NotNull File file) {
        return file.getName().endsWith(".yaml");
    }

    @Override
    public boolean canRead(@NotNull String dataString) {
        return dataString.startsWith("---");
    }

    @Override
    protected MidpointYAMLParser createJacksonParser(InputStream stream) throws IOException {
        return (MidpointYAMLParser) new MidpointYAMLFactory().createParser(stream);
    }

    @Override
    protected QName tagToTypeName(Object tag, JsonReadingContext ctx) throws IOException, SchemaException {
        if (tag == null) {
            return null;
        } if (TAG_BINARY.equals(tag)) {
            return DOMUtil.XSD_BASE64BINARY;
        } if (TAG_STRING.equals(tag)) {
            return DOMUtil.XSD_STRING;
        } else if (TAG_BOOL.equals(tag)) {
            return DOMUtil.XSD_BOOLEAN;
        } else if (TAG_NULL.equals(tag)) {
            return null;        // ???
        } else if (TAG_INT.equals(tag)) {
            QName type = determineNumberType(ctx.parser.getNumberType());
            if (DOMUtil.XSD_INT.equals(type) || DOMUtil.XSD_INTEGER.equals(type)) {
                return type;
            } else {
                return DOMUtil.XSD_INT;            // suspicious
            }
        } else if (TAG_FLOAT.equals(tag)) {
            QName type = determineNumberType(ctx.parser.getNumberType());
            if (DOMUtil.XSD_FLOAT.equals(type) || DOMUtil.XSD_DOUBLE.equals(type) || DOMUtil.XSD_DECIMAL.equals(type)) {
                return type;
            } else {
                return DOMUtil.XSD_FLOAT;            // suspicious
            }
        } else if (tag instanceof String) {
            return QNameUtil.uriToQName((String) tag, true);
        } else {
            // TODO issue a warning?
            return null;
        }
    }

    private static QName determineNumberType(JsonParser.NumberType numberType) throws SchemaException {
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

    @Override
    boolean supportsMultipleDocuments() {
        return true;
    }
}

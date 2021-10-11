/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.lex.dom;

import com.evolveum.midpoint.prism.marshaller.XNodeProcessorEvaluationMode;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.prism.xnode.ValueParser;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Attr;

import javax.annotation.concurrent.NotThreadSafe;
import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.Map;

/**
 * Parser for attribute values.
 *
 * It can be used only for thread-confined structures because Xerces DOM structures are not thread safe. Even for reading!
 */
@NotThreadSafe
class AttributeValueParser<T> implements ValueParser<T>, Serializable {

    @NotNull private final Attr attr;

    AttributeValueParser(@NotNull Attr attr) {
        this.attr = attr;
    }

    @Override
    public T parse(QName typeName, XNodeProcessorEvaluationMode mode) throws SchemaException {
        try {
            if (DOMUtil.XSD_QNAME.equals(typeName)) {
                //noinspection unchecked
                return (T) DOMUtil.getQNameValue(attr);
            } else {
                Class<T> clazz = XsdTypeMapper.getXsdToJavaMapping(typeName);
                if (clazz != null) {
                    return XmlTypeConverter.toJavaValue(attr.getTextContent(), clazz);
                } else {
                    throw new SchemaException("Cannot convert attribute '"+ attr +"' to "+ typeName);
                }
            }
        } catch (IllegalArgumentException e) {
            return DomLexicalProcessor.processIllegalArgumentException(attr.getTextContent(), typeName, e, mode);        // primitive way of ensuring compatibility mode
        }
    }

    @Override
    public boolean canParseAs(QName typeName) {
        return DOMUtil.XSD_QNAME.equals(typeName) || XsdTypeMapper.getXsdToJavaMapping(typeName) != null;
    }

    @Override
    public boolean isEmpty() {
        return DOMUtil.isEmpty(attr);
    }

    @Override
    public String getStringValue() {
        return attr.getValue();
    }

    @Override
    public String toString() {
        return "ValueParser(DOMa, " + PrettyPrinter.prettyPrint(DOMUtil.getQName(attr)) + ": " + attr.getTextContent() + ")";
    }

    @Override
    public Map<String, String> getPotentiallyRelevantNamespaces() {
        return DOMUtil.getAllVisibleNamespaceDeclarations(attr);
    }

    @Override
    public ValueParser<T> freeze() {
        return new DomLessValueParser<>(attr);
    }
}

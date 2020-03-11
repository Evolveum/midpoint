/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.lex.dom;

import com.evolveum.midpoint.prism.impl.marshaller.ItemPathHolder;
import com.evolveum.midpoint.prism.marshaller.XNodeProcessorEvaluationMode;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.prism.xnode.ValueParser;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Element;

import javax.annotation.concurrent.NotThreadSafe;
import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.Map;

/**
 * Parser for element values.
 *
 * It can be used only for thread-confined structures because Xerces DOM structures are not thread safe. Even for reading!
 */
@NotThreadSafe
class ElementValueParser<T> implements ValueParser<T>, Serializable {

    // element has no children nor application attributes
    @NotNull private final Element element;

    ElementValueParser(@NotNull Element element) {
        this.element = element;
    }

    @Override
    public T parse(QName typeName, XNodeProcessorEvaluationMode mode) throws SchemaException {
        try {
            if (ItemPathType.COMPLEX_TYPE.equals(typeName)) {
                //noinspection unchecked
                return (T) new ItemPathType(ItemPathHolder.parseFromElement(element));
            } else {
                Class<T> clazz = XsdTypeMapper.getXsdToJavaMapping(typeName);
                if (clazz != null) {
                    return (T) XmlTypeConverter.toJavaValue(element, clazz);
                } else if (DOMUtil.XSD_ANYTYPE.equals(typeName)) {
                    //noinspection unchecked
                    return (T) element.getTextContent();                // if parsing primitive as xsd:anyType, we can safely parse it as string
                } else {
                    throw new SchemaException("Cannot convert element '" + element + "' to " + typeName);
                }
            }
        } catch (IllegalArgumentException e) {
            return DomLexicalProcessor.processIllegalArgumentException(element.getTextContent(), typeName, e, mode);        // primitive way of ensuring compatibility mode
        }
    }

    @Override
    public boolean canParseAs(QName typeName) {
        return ItemPathType.COMPLEX_TYPE.equals(typeName) ||
                XsdTypeMapper.getXsdToJavaMapping(typeName) != null ||
                DOMUtil.XSD_QNAME.equals(typeName);
    }

    @Override
    public boolean isEmpty() {
        return DOMUtil.isEmpty(element);
    }

    @Override
    public String getStringValue() {
        return element.getTextContent();
    }

    @Override
    public Map<String, String> getPotentiallyRelevantNamespaces() {
        return DOMUtil.getAllVisibleNamespaceDeclarations(element);
    }

    @Override
    public String toString() {
        return "ValueParser(DOMe, " + PrettyPrinter.prettyPrint(DOMUtil.getQName(element)) + ": " + element.getTextContent()
                + ")";
    }

    @Override
    public ValueParser<T> freeze() {
        return new DomLessValueParser<>(element);
    }
}

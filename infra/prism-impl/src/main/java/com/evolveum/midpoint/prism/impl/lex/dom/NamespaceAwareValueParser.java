/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.lex.dom;

import com.evolveum.midpoint.prism.PrismNamespaceContext;
import com.evolveum.midpoint.prism.impl.marshaller.ItemPathHolder;
import com.evolveum.midpoint.prism.marshaller.XNodeProcessorEvaluationMode;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.prism.xnode.ValueParser;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import org.apache.commons.lang3.StringUtils;
import javax.annotation.concurrent.ThreadSafe;
import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.Map;

/**
 * Thread safe value parser obtained by replacing DOM objects with their static (thread safe) representation.
 *
 */
@ThreadSafe
class NamespaceAwareValueParser<T> implements ValueParser<T>, Serializable {

    private final String textContent;
    private final PrismNamespaceContext visibleNamespaceDeclarations;

    NamespaceAwareValueParser(String textContent, PrismNamespaceContext namespaces) {
        this.textContent = textContent;
        this.visibleNamespaceDeclarations = namespaces;
    }

    @Override
    public T parse(QName typeName, XNodeProcessorEvaluationMode mode) throws SchemaException {
        try {
            if (ItemPathType.COMPLEX_TYPE.equals(typeName)) {
                //noinspection unchecked
                return (T) new ItemPathType(ItemPathHolder.parseFromString(textContent, visibleNamespaceDeclarations.allPrefixes()));
            } else {
                Class<?> javaType = XsdTypeMapper.getXsdToJavaMapping(typeName);
                if (javaType != null) {
                    //noinspection unchecked
                    return (T) XmlTypeConverter.toJavaValue(textContent, visibleNamespaceDeclarations.allPrefixes(), javaType);
                } else if (DOMUtil.XSD_ANYTYPE.equals(typeName)) {
                    //noinspection unchecked
                    return (T) textContent;                // if parsing primitive as xsd:anyType, we can safely parse it as string
                } else {
                    throw new SchemaException("Cannot convert element/attribute '" + textContent + "' to " + typeName);
                }
            }
        } catch (IllegalArgumentException e) {
            return DomLexicalProcessor.processIllegalArgumentException(textContent, typeName, e, mode);        // primitive way of ensuring compatibility mode
        }
    }

    @Override
    public boolean canParseAs(QName typeName) {
        return ItemPathType.COMPLEX_TYPE.equals(typeName) ||
                XmlTypeConverter.canConvert(typeName) ||
                DOMUtil.XSD_ANYTYPE.equals(typeName);
    }

    @Override
    public boolean isEmpty() {
        return StringUtils.isBlank(textContent);
    }

    @Override
    public String getStringValue() {
        return textContent;
    }

    @Override
    public Map<String, String> getPotentiallyRelevantNamespaces() {
        return visibleNamespaceDeclarations.allPrefixes();
    }

    @Override
    public ValueParser<T> freeze() {
        return this;
    }

    @Override
    public String toString() {
        return "ValueParser(DOM-less, " + PrettyPrinter.prettyPrint(textContent) + ", namespace declarations)";
    }
}

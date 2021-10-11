/*
 * Copyright (c) 2014-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.xnode;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.marshaller.XNodeProcessorEvaluationMode;
import com.evolveum.midpoint.util.exception.SchemaException;

import java.util.Map;

/**
 * Objects of type ValueParser should be immutable. (E.g. when cloning PrimitiveXNode, they are copied, not cloned.)
 *
 * @param <T>
 */

public interface ValueParser<T> {

    T parse(QName typeName, XNodeProcessorEvaluationMode mode) throws SchemaException;

    /**
     * Checks if this parser can (by itself) parse the value as given type.
     * (If it cannot, the parsing must be done by higher layers.)
     *
     * This method should be efficient. It may bring some performance penalty (when coupled with following
     * parse() call) but we assume this situation is quite rare and performance effect is negligible.
     */
    boolean canParseAs(QName typeName);

    // This has to work even without the type
    boolean isEmpty();

    /**
     * Returns the value represented as string - in the best format that we can.
     * This has to work even without knowing the exact data type. Therefore
     * there is no guarantee that the returned value will be precise.
     * This method is used as a "last instance" if everything else fails.
     * Invocation of this method will not change the state of the xnode, e.g.
     * it will NOT cause it to be parsed. It can be invoked without any side effects.
     */
    String getStringValue();

    /**
     * Returns namespaces that could be relevant when serializing unparsed string value
     * of this item. Used to preserve xmlns declarations for QNames and ItemPaths in
     * unparsed data. (MID-2196)
     *
     * @return May return null if not supported or no namespace declarations are present.
     */
    Map<String,String> getPotentiallyRelevantNamespaces();

    /**
     * @return Frozen version of this parser.
     */
    ValueParser<T> freeze();
}

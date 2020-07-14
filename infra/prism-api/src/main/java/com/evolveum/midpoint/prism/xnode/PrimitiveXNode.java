/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.xnode;

import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

/**
 *
 */
public interface PrimitiveXNode<T> extends XNode, MetadataAware {

    String getGuessedFormattedValue() throws SchemaException;

    /**
     * Returns the value represented as string - in the best format that we can.
     * There is no guarantee that the returned value will be precise.
     * This method is used as a "last instance" if everything else fails.
     * Invocation of this method will not change the state of this xnode, e.g.
     * it will NOT cause it to be parsed.
     */
    String getStringValue();

    <X> X getParsedValue(@NotNull QName typeName, @Nullable Class<X> expectedClass) throws SchemaException;

    T getValue();

    ValueParser<T> getValueParser();

    boolean isParsed();
}

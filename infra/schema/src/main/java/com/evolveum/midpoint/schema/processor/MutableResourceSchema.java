/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.schema.MutablePrismSchema;

import org.jetbrains.annotations.VisibleForTesting;

/**
 *
 */
public interface MutableResourceSchema extends ResourceSchema, MutablePrismSchema {

    /**
     * Creates a new resource object class definition and adds it to the schema.
     *
     * This is a preferred way how to create definition in the schema.
     *
     * @param typeName
     *            type QName
     * @return new resource object definition
     */
    @VisibleForTesting
    MutableResourceObjectClassDefinition createObjectClassDefinition(QName typeName);
}

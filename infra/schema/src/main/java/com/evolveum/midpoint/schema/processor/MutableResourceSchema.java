/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.schema.MutablePrismSchema;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;

/**
 *
 */
public interface MutableResourceSchema extends ResourceSchema, MutablePrismSchema {

    MutableObjectClassComplexTypeDefinition createObjectClassDefinition(String localTypeName);

    MutableObjectClassComplexTypeDefinition createObjectClassDefinition(QName typeName);

    void parseThis(Element xsdSchema, String shortDesc, PrismContext prismContext) throws SchemaException;
}

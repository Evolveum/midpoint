/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.Collection;

/**
 * @author mederly
 */
public interface ConnectorSchema extends PrismSchema {

    Collection<ObjectClassComplexTypeDefinition> getObjectClassDefinitions();

    default ObjectClassComplexTypeDefinition findObjectClassDefinition(@NotNull ShadowType shadow) {
        return findObjectClassDefinition(shadow.getObjectClass());
    }

    default ObjectClassComplexTypeDefinition findObjectClassDefinition(@NotNull String localName) {
        return findObjectClassDefinition(new QName(getNamespace(), localName));
    }

    ObjectClassComplexTypeDefinition findObjectClassDefinition(QName qName);

    String getUsualNamespacePrefix();
}

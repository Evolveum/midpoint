/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author semancik
 * @author mederly
 */
public interface ResourceSchema extends PrismSchema {

    default Collection<ObjectClassComplexTypeDefinition> getObjectClassDefinitions() {
        return getDefinitions(ObjectClassComplexTypeDefinition.class);
    }

    default ObjectClassComplexTypeDefinition findObjectClassDefinition(ShadowType shadow) {
        return findObjectClassDefinition(shadow.getObjectClass());
    }

    default ObjectClassComplexTypeDefinition findObjectClassDefinition(String localName) {
        return findObjectClassDefinition(new QName(getNamespace(), localName));
    }

    ObjectClassComplexTypeDefinition findObjectClassDefinition(QName qName);

    ObjectClassComplexTypeDefinition findObjectClassDefinition(ShadowKindType kind, String intent);

    ObjectClassComplexTypeDefinition findDefaultObjectClassDefinition(ShadowKindType kind);

    default List<QName> getObjectClassList() {
        List<QName> rv = new ArrayList<>();
        for (ObjectClassComplexTypeDefinition def : getObjectClassDefinitions()) {
            if (!rv.contains(def.getTypeName())) {
                rv.add(def.getTypeName());
            }
        }
        return rv;
    }

    MutableResourceSchema toMutable();
}

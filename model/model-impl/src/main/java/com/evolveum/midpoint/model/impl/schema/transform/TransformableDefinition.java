/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.schema.transform;


import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.Definition;
import com.evolveum.midpoint.prism.deleg.DefinitionDelegator;
import com.evolveum.midpoint.prism.util.CloneUtil;

public abstract class TransformableDefinition implements DefinitionDelegator {

    private enum NullObject {
        VALUE
    }
    private Map<QName, Object> annotations;

    public TransformableDefinition(Definition delegate) {
        if (delegate instanceof TransformableDefinition) {
            // Shallow copy overriden annotations
            Map<QName, Object> maybe = ((TransformableDefinition) delegate).annotationsOverrides();
            if (maybe != null) {
                annotations = new HashMap<>();
                annotations.putAll(maybe);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <A> A getAnnotation(QName qname) {
        if (annotations != null) {
            Object maybe = annotations.get(qname);
            if (maybe != null) {
                // If we NullObject.VALUE it is actually null, we store it to hide original annotation
                // in parent item.
                return maybe != NullObject.VALUE ? (A) maybe : null;
            }
        }
        return DefinitionDelegator.super.getAnnotation(qname);
    }

    @Override
    public <A> void setAnnotation(QName qname, A value) {
        Object newVal = value != null ? value : NullObject.VALUE;
        if (annotations == null) {
            annotations = new HashMap<>();
        }
        annotations.put(qname, newVal);
    }

    protected Map<QName, Object> annotationsOverrides() {
        return annotations;
    }

    @Override
    public Definition clone() {
        throw new UnsupportedOperationException();
    }
}

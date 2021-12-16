/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.CloneStrategy;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.impl.PrismPropertyImpl;
import com.evolveum.midpoint.util.Checks;
import com.evolveum.midpoint.util.exception.SchemaException;

public class ResourceAttributeImpl<T> extends PrismPropertyImpl<T> implements ResourceAttribute<T> {

    private static final long serialVersionUID = -6149194956029296486L;

    ResourceAttributeImpl(QName name, ResourceAttributeDefinition<T> definition) {
        super(name, definition, PrismContext.get());
    }

    @Override
    public ResourceAttributeDefinition<T> getDefinition() {
        return (ResourceAttributeDefinition<T>) super.getDefinition();
    }

    @Override
    public ResourceAttribute<T> clone() {
        return cloneComplex(CloneStrategy.LITERAL);
    }

    @Override
    public ResourceAttribute<T> cloneComplex(CloneStrategy strategy) {
        ResourceAttributeImpl<T> clone = new ResourceAttributeImpl<>(getElementName(), getDefinition());
        copyValues(strategy, clone);
        return clone;
    }

    protected void copyValues(CloneStrategy strategy, ResourceAttributeImpl<T> clone) {
        super.copyValues(strategy, clone);
        // Nothing to copy
    }

    /**
     * Return a human readable name of this class suitable for logs.
     */
    @Override
    protected String getDebugDumpClassName() {
        return "RA";
    }

    @Override
    public void applyDefinition(PrismPropertyDefinition<T> definition, boolean force) throws SchemaException {
        if (definition != null) {
            Checks.checkSchema(definition instanceof ResourceAttributeDefinition, "Definition should be %s not %s" ,
                    ResourceAttributeDefinition.class.getSimpleName(), definition.getClass().getName());
        }
        super.applyDefinition(definition, force);
    }
}

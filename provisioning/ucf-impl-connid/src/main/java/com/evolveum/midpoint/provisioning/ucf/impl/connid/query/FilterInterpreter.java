/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.impl.connid.query;

import org.identityconnectors.framework.common.objects.filter.Filter;

import com.evolveum.midpoint.prism.query.LogicalFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.provisioning.ucf.impl.connid.ConnIdNameMapper;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.jetbrains.annotations.NotNull;

public class FilterInterpreter {

    private final ObjectClassComplexTypeDefinition objectClassDefinition;

    public FilterInterpreter(ObjectClassComplexTypeDefinition objectClassDefinition) {
        this.objectClassDefinition = objectClassDefinition;
    }

    public Filter interpret(ObjectFilter filter, ConnIdNameMapper icfNameMapper) throws SchemaException{
        return createOperation(filter)
                .interpret(filter, icfNameMapper);
    }

    @NotNull
    private Operation createOperation(ObjectFilter filter) {
        if (filter instanceof LogicalFilter) {
            return new LogicalOperation(this);
        } else if (filter instanceof ValueFilter) {
            return new ValueOperation(this);
        } else {
            throw new UnsupportedOperationException("Unsupported filter type: " + filter.getClass().getSimpleName());
        }
    }

    public ObjectClassComplexTypeDefinition getObjectClassDefinition() {
        return objectClassDefinition;
    }

}

/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.provisioning.ucf.impl.connid.query;

import org.identityconnectors.framework.common.objects.filter.Filter;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.provisioning.ucf.impl.connid.ConnIdNameMapper;
import com.evolveum.midpoint.util.exception.SchemaException;

public abstract class Operation {

    private FilterInterpreter interpreter;

    public Operation(FilterInterpreter interpreter){
        this.interpreter = interpreter;
    }

    public abstract <T> Filter interpret(ObjectFilter objectFilter, ConnIdNameMapper icfNameMapper) throws SchemaException;

    public FilterInterpreter getInterpreter() {
        return interpreter;
    }

    public void setInterpreter(FilterInterpreter interpreter) {
        this.interpreter = interpreter;
    }
}

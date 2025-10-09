/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.cases.api.temporary;

import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;

@FunctionalInterface
public interface VariablesProvider {
    VariablesMap get() throws SchemaException, ObjectNotFoundException;
}

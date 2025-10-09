/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.search.wrapper;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.SchemaException;

import java.io.Serializable;

public interface QueryWrapper extends Serializable {

    <T> ObjectQuery createQuery(Class<T> typeClass, PageBase pageBase, VariablesMap variables) throws SchemaException, ExpressionEvaluationException;

     String getAdvancedError();

     void setAdvancedError(String advancedError);
}

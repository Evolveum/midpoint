/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.query;

import com.evolveum.midpoint.prism.ExpressionWrapper;

import java.util.Collection;

/**
 *
 */
public interface FullTextFilter extends ObjectFilter {

    Collection<String> getValues();

    void setValues(Collection<String> values);

    ExpressionWrapper getExpression();

    void setExpression(ExpressionWrapper expression);

    @Override
    FullTextFilter clone();

}

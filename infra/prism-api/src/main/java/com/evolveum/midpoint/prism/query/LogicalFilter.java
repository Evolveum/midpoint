/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.query;

import java.util.List;

/**
 *
 */
public interface LogicalFilter extends ObjectFilter {

    List<ObjectFilter> getConditions();

    void setConditions(List<ObjectFilter> condition);

    void addCondition(ObjectFilter condition);

    boolean contains(ObjectFilter condition);

    LogicalFilter cloneEmpty();

    //List<ObjectFilter> getClonedConditions();

    boolean isEmpty();

    @Override
    void checkConsistence(boolean requireDefinitions);

    @Override
    void accept(Visitor visitor);

    //String getDebugDumpOperationName();
}

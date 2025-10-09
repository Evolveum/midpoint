/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.traces;

import com.evolveum.midpoint.util.annotation.Experimental;

import java.util.HashSet;
import java.util.Set;

@Experimental
public class ViewOptions {

    private final Set<OpType> showOperationTypes = new HashSet<>();

    public Set<OpType> getShowOperationTypes() {
        return showOperationTypes;
    }

    public void show(OpType... types) {
        for (OpType type : types) {
            showOperationTypes.add(type);
        }
    }


}

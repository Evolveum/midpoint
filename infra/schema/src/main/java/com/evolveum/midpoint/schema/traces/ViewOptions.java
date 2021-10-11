/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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

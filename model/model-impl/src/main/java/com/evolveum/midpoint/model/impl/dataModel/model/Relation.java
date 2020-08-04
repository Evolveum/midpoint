/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.dataModel.model;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author mederly
 */
public class Relation {

    @NotNull private final List<DataItem> sources;
    @Nullable private final DataItem target;

    public Relation(@NotNull List<DataItem> sources, @Nullable DataItem target) {
        this.sources = sources;
        this.target = target;
    }

    @NotNull
    public List<DataItem> getSources() {
        return sources;
    }

    @Nullable
    public DataItem getTarget() {
        return target;
    }

    @Override
    public String toString() {
        return "Relation{" +
                "sources=" + sources +
                ", target=" + target +
                '}';
    }

}

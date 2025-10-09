/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.dataModel.model;

import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MappingRelation extends Relation {

    @NotNull private final MappingType mapping;

    public MappingRelation(@NotNull List<DataItem> sources, @Nullable DataItem target, @NotNull MappingType mapping) {
        super(sources, target);
        this.mapping = mapping;
    }

    @NotNull
    public MappingType getMapping() {
        return mapping;
    }

}

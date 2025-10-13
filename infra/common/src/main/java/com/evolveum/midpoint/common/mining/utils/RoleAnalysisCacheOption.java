/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.common.mining.utils;

import java.io.Serializable;
import java.util.List;

import com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RoleAnalysisCacheOption implements Serializable {

    @Nullable List<RoleAnalysisAttributeDef> itemDef;

    public RoleAnalysisCacheOption(@NotNull List<RoleAnalysisAttributeDef> itemDef) {
        this.itemDef = itemDef;
    }

    public @Nullable List<RoleAnalysisAttributeDef> getItemDef() {
        return itemDef;
    }

    public void setItemDef(@NotNull List<RoleAnalysisAttributeDef> itemDef) {
        this.itemDef = itemDef;
    }
}

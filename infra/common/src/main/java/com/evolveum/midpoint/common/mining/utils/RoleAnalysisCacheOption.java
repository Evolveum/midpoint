package com.evolveum.midpoint.common.mining.utils;

import java.io.Serializable;
import java.util.List;

import com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef;

import org.jetbrains.annotations.NotNull;

public class RoleAnalysisCacheOption implements Serializable {

    @NotNull List<RoleAnalysisAttributeDef> itemDef;

    public RoleAnalysisCacheOption(@NotNull List<RoleAnalysisAttributeDef> itemDef) {
        this.itemDef = itemDef;
    }

    public @NotNull List<RoleAnalysisAttributeDef> getItemDef() {
        return itemDef;
    }

    public void setItemDef(@NotNull List<RoleAnalysisAttributeDef> itemDef) {
        this.itemDef = itemDef;
    }
}

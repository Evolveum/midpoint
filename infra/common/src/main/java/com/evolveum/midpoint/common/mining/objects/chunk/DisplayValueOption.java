/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.common.mining.objects.chunk;

import java.io.Serializable;
import java.util.Objects;

import com.evolveum.midpoint.common.mining.objects.analysis.RoleAnalysisAttributeDef;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisChunkAction;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisChunkMode;
import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisSortMode;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

public class DisplayValueOption implements Serializable {
    RoleAnalysisAttributeDef roleAnalysisAttributeDef;
    RoleAnalysisAttributeDef userAnalysisAttributeDef;
    RoleAnalysisSortMode sortMode = RoleAnalysisSortMode.NONE;
    RoleAnalysisProcessModeType processMode;
    RoleAnalysisChunkMode chunkMode = RoleAnalysisChunkMode.COMPRESS;

    RoleAnalysisChunkAction chunkAction = RoleAnalysisChunkAction.SELECTION;
    boolean isPatternToolsPanelMode = true;
    boolean isToolsPanelExpanded = false;
    boolean isFullPage = false;

    public DisplayValueOption() {
    }

    public DisplayValueOption(
            RoleAnalysisAttributeDef roleAnalysisAttributeDef,
            RoleAnalysisAttributeDef userAnalysisAttributeDef,
            RoleAnalysisSortMode sortMode) {
        this.roleAnalysisAttributeDef = roleAnalysisAttributeDef;
        this.userAnalysisAttributeDef = userAnalysisAttributeDef;
        this.sortMode = sortMode;
    }

    public RoleAnalysisAttributeDef getRoleAnalysisRoleDef() {
        return roleAnalysisAttributeDef;
    }

    public RoleAnalysisAttributeDef getNameIfNullAnalysisRoleDef() {
        return Objects.requireNonNullElseGet(roleAnalysisAttributeDef, () -> new RoleAnalysisAttributeDef(
                ObjectType.F_NAME,
                findItemDefinition(RoleType.COMPLEX_TYPE),
                UserType.class));
    }

    public RoleAnalysisAttributeDef getNameIfNullAnalysisUserDef() {
        return Objects.requireNonNullElseGet(userAnalysisAttributeDef, () -> new RoleAnalysisAttributeDef(
                ObjectType.F_NAME,
                findItemDefinition(UserType.COMPLEX_TYPE),
                UserType.class));
    }

    private static ItemDefinition<?> findItemDefinition(@NotNull QName complexType) {
        PrismContainerDefinition<?> objectDefinition = PrismContext.get().getSchemaRegistry().findContainerDefinitionByType(complexType);
        return objectDefinition.findItemDefinition(ObjectType.F_NAME);
    }

    public void setRoleAnalysisRoleDef(RoleAnalysisAttributeDef roleItemValuePath) {
        this.roleAnalysisAttributeDef = roleItemValuePath;
    }

    public RoleAnalysisAttributeDef getUserAnalysisUserDef() {
        return userAnalysisAttributeDef;
    }

    public void setUserAnalysisUserDef(RoleAnalysisAttributeDef userItemValuePath) {
        this.userAnalysisAttributeDef = userItemValuePath;
    }

    public RoleAnalysisSortMode getSortMode() {
        return sortMode;
    }

    public void setSortMode(RoleAnalysisSortMode sortMode) {
        this.sortMode = sortMode;
    }

    public RoleAnalysisProcessModeType getProcessMode() {
        return processMode;
    }

    public void setProcessMode(RoleAnalysisProcessModeType processMode) {
        this.processMode = processMode;
    }

    public RoleAnalysisChunkMode getChunkMode() {
        return chunkMode;
    }

    public void setChunkMode(RoleAnalysisChunkMode chunkMode) {
        this.chunkMode = chunkMode;
    }

    public boolean isFullPage() {
        return isFullPage;
    }

    public void setFullPage(boolean fullPage) {
        isFullPage = fullPage;
    }

    public boolean isPatternToolsPanelMode() {
        return isPatternToolsPanelMode;
    }

    public void setPatternToolsPanelMode(boolean patternToolsPanelMode) {
        isPatternToolsPanelMode = patternToolsPanelMode;
    }

    public boolean isToolsPanelExpanded() {
        return isToolsPanelExpanded;
    }

    public void setToolsPanelExpanded(boolean toolsPanelExpanded) {
        isToolsPanelExpanded = toolsPanelExpanded;
    }

    public RoleAnalysisChunkAction getChunkAction() {
        return chunkAction;
    }

    public void setChunkAction(RoleAnalysisChunkAction chunkAction) {
        this.chunkAction = chunkAction;
    }
}

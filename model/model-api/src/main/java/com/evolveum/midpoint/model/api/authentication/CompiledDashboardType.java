/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.api.authentication;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DashboardType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DashboardWidgetType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserInterfaceElementVisibilityType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import java.io.Serializable;
import java.util.List;

public class CompiledDashboardType implements DebugDumpable, Serializable {

    private DisplayType displayType;
    private UserInterfaceElementVisibilityType visibility;
    private DashboardType originalDashboard;

    public CompiledDashboardType(DashboardType originalDashboard) {
        this.originalDashboard = originalDashboard;
    }

    public void setDisplayType(DisplayType displayType) {
        this.displayType = displayType;
    }

    public DisplayType getDisplay() {
        return displayType;
    }

    public void setVisibility(UserInterfaceElementVisibilityType visibility) {
        this.visibility = visibility;
    }

    public UserInterfaceElementVisibilityType getVisibility() {
        return visibility;
    }

    public List<DashboardWidgetType> getWidget() {
        return originalDashboard.getWidget();
    }

    public PolyStringType getName() {
        return originalDashboard.getName();
    }

    public String getOid() {
        return originalDashboard.getOid();
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(CompiledDashboardType.class, indent);
        DebugUtil.debugDumpWithLabelLn(sb, "name", getName(), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "displayType", displayType, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "visibility", visibility, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "widget", getWidget(), indent + 1);
        return sb.toString();
    }
}

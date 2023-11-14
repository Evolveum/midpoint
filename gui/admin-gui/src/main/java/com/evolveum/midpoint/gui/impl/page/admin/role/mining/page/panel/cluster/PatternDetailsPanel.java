/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster;

import java.util.List;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.RoleAnalysisDetectedPatternTable;
import com.evolveum.midpoint.web.component.dialog.Popupable;

public class PatternDetailsPanel extends BasePanel<String> implements Popupable {

    private static final String ID_PANEL = "panel";

    public PatternDetailsPanel(String id, IModel<String> messageModel, List<DetectedPattern> detection, RoleAnalysisClusterType cluster) {
        super(id, messageModel);

        initLayout(detection, cluster);
    }

    public void initLayout(List<DetectedPattern> detection, RoleAnalysisClusterType cluster) {
        RoleAnalysisDetectedPatternTable components = new RoleAnalysisDetectedPatternTable(ID_PANEL, new LoadableDetachableModel<List<DetectedPattern>>() {
            @Override
            protected List<DetectedPattern> load() {
                return detection;
            }
        }, true, cluster) {
            @Override
            protected void onLoad(AjaxRequestTarget ajaxRequestTarget, IModel<DetectedPattern> rowModel) {
                onLoadPerform(ajaxRequestTarget, rowModel);
            }
        };

        components.setOutputMarkupId(true);
        add(components);
    }

    public void onLoadPerform(AjaxRequestTarget ajaxRequestTarget, IModel<DetectedPattern> rowModel) {

    }

    public void onClose(AjaxRequestTarget ajaxRequestTarget) {
        getPageBase().hideMainPopup(ajaxRequestTarget);
    }

    @Override
    public int getWidth() {
        return 60;
    }

    @Override
    public int getHeight() {
        return 50;
    }

    @Override
    public String getWidthUnit() {
        return "%";
    }

    @Override
    public String getHeightUnit() {
        return "%";
    }

    @Override
    public Component getContent() {
        return this;
    }

    @Override
    public StringResourceModel getTitle() {
        //TODO
        return null;
    }
}

/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster;

import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.panel.RoleAnalysisDetectedPatternTable;
import com.evolveum.midpoint.web.component.dialog.Popupable;

public class DetectedPatternPopupPanel extends BasePanel<String> implements Popupable {

    private static final String ID_PANEL = "panel";
    PageBase pageBase;

    public DetectedPatternPopupPanel(String id, IModel<String> messageModel,
            List<DetectedPattern> detectedPatterns, PageBase pageBase) {
        super(id, messageModel);
        this.pageBase = pageBase;

        initLayout(detectedPatterns);
    }

    public PageBase getPageBase() {
        return pageBase;
    }

    public void initLayout(List<DetectedPattern> detectedPatterns) {

        RoleAnalysisDetectedPatternTable components = new RoleAnalysisDetectedPatternTable(ID_PANEL, DetectedPatternPopupPanel.this.getPageBase(),
                new LoadableDetachableModel<>() {
                    @Override
                    protected List<DetectedPattern> load() {
                        return detectedPatterns;
                    }
                }) {
        };
        components.setOutputMarkupId(true);
        add(components);
    }

    public void onClose(AjaxRequestTarget ajaxRequestTarget) {
        getPageBase().hideMainPopup(ajaxRequestTarget);
    }

    @Override
    public int getWidth() {
        return 80;
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

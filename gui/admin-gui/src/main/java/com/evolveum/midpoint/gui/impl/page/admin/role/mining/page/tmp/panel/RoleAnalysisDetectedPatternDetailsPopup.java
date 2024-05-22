/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel;

import java.io.Serial;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.model.InfoBoxModel;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisAttributeAnalysisResult;

public class RoleAnalysisDetectedPatternDetailsPopup extends BasePanel<DetectedPattern> implements Popupable {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_CONTAINER = "container";

    public RoleAnalysisDetectedPatternDetailsPopup(String id, IModel<DetectedPattern> model) {
        super(id, model);
        initLayout();
    }

    private void initLayout() {
        RoleAnalysisDetectedPatternDetails detailsPanel = new RoleAnalysisDetectedPatternDetails(ID_CONTAINER, getModel());
        detailsPanel.setOutputMarkupId(true);
        add(detailsPanel);
    }



    @Override
    public int getWidth() {
        return 70;
    }

    @Override
    public int getHeight() {
        return 70;
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
    public IModel<String> getTitle() {
        return null;
    }

    @Override
    public Component getContent() {
        return this;
    }
}

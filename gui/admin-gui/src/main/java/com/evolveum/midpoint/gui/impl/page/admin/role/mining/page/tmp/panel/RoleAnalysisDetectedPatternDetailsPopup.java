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
    private static final String ID_HEADER_ITEMS = "header-items";
    private static final String ID_STATISTICS_PANEL = "statistics-panel";

    public RoleAnalysisDetectedPatternDetailsPopup(String id, IModel<DetectedPattern> model) {
        super(id, model);
        initLayout();
    }

    private void initLayout() {
        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.setOutputMarkupId(true);
        add(container);

        RepeatingView headerItems = new RepeatingView(ID_HEADER_ITEMS);
        headerItems.setOutputMarkupId(true);
        container.add(headerItems);

        initHeaderPanel(headerItems);

        initStatisticsPanel(container);
    }

    private void initHeaderPanel(RepeatingView headerItems) {
        DetectedPattern pattern = getModel().getObject();
        if (getModel().getObject() == null) {
            return;
        }
        IModel<String> reduction = Model.of(String.format("%.2f", pattern.getReductionFactorConfidence()) + "%");
        IModel<String> confidence = Model.of(String.format("%.2f", pattern.getItemsConfidence()) + "%");
        IModel<String> roleObjectCount = Model.of(String.valueOf(pattern.getRoles().size()));
        IModel<String> userObjectCount = Model.of(String.valueOf(pattern.getUsers().size()));

        InfoBoxModel infoBoxModelReduction = new InfoBoxModel(GuiStyleConstants.ARROW_LONG_DOWN,
                "Reduction",
                reduction.getObject(),
                pattern.getReductionFactorConfidence(),
                "Reduction factor");

        RoleAnalysisInfoBox reductionLabel = new RoleAnalysisInfoBox(headerItems.newChildId(), Model.of(infoBoxModelReduction)){
            @Override
            protected String getInfoBoxCssClass() {
                return super.getInfoBoxCssClass();
            }
        };
        reductionLabel.add(AttributeModifier.replace("class", "col-md-6"));
        reductionLabel.setOutputMarkupId(true);
        headerItems.add(reductionLabel);



        InfoBoxModel infoBoxModelConfidence = new InfoBoxModel(GuiStyleConstants.THUMBS_UP,
                "Confidence",
                confidence.getObject(),
                pattern.getItemsConfidence(),
                "Confidence of the pattern");

        RoleAnalysisInfoBox confidenceLabel = new RoleAnalysisInfoBox(headerItems.newChildId(), Model.of(infoBoxModelConfidence)){
            @Override
            protected String getInfoBoxCssClass() {
                return super.getInfoBoxCssClass();
            }
        };
        confidenceLabel.add(AttributeModifier.replace("class", "col-md-6"));
        confidenceLabel.setOutputMarkupId(true);
        headerItems.add(confidenceLabel);



        InfoBoxModel infoBoxModelRoles = new InfoBoxModel(GuiStyleConstants.CLASS_OBJECT_ROLE_ICON,
                "Roles",
                roleObjectCount.getObject(),
                100,
                "Number of roles in the pattern");

        RoleAnalysisInfoBox roleObjectCountLabel = new RoleAnalysisInfoBox(headerItems.newChildId(), Model.of(infoBoxModelRoles)){
            @Override
            protected String getInfoBoxCssClass() {
                return super.getInfoBoxCssClass();
            }
        };
        roleObjectCountLabel.add(AttributeModifier.replace("class", "col-md-6"));
        roleObjectCountLabel.setOutputMarkupId(true);
        headerItems.add(roleObjectCountLabel);



        InfoBoxModel infoBoxModelUsers = new InfoBoxModel(GuiStyleConstants.CLASS_OBJECT_USER_ICON,
                "Users",
                userObjectCount.getObject(),
                100,
                "Number of users in the pattern");

        RoleAnalysisInfoBox userObjectCountLabel = new RoleAnalysisInfoBox(headerItems.newChildId(), Model.of(infoBoxModelUsers)){
            @Override
            protected String getInfoBoxCssClass() {
                return super.getInfoBoxCssClass();
            }
        };
        userObjectCountLabel.add(AttributeModifier.replace("class", "col-md-6"));
        userObjectCountLabel.setOutputMarkupId(true);
        headerItems.add(userObjectCountLabel);
    }

    private void initStatisticsPanel(WebMarkupContainer container) {

        RoleAnalysisAttributeAnalysisResult userAttributeAnalysisResult = null;
        RoleAnalysisAttributeAnalysisResult roleAttributeAnalysisResult = null;
        if (getModel().getObject() != null) {
            DetectedPattern pattern = getModel().getObject();
            userAttributeAnalysisResult = pattern.getUserAttributeAnalysisResult();
            roleAttributeAnalysisResult = pattern.getRoleAttributeAnalysisResult();
        }

        if (userAttributeAnalysisResult != null || roleAttributeAnalysisResult != null) {
            RoleAnalysisAttributePanel roleAnalysisAttributePanel = new RoleAnalysisAttributePanel(ID_STATISTICS_PANEL,
                    Model.of("Role analysis attribute panel"), roleAttributeAnalysisResult, userAttributeAnalysisResult);
            roleAnalysisAttributePanel.setOutputMarkupId(true);
            container.add(roleAnalysisAttributePanel);
        } else {
            Label label = new Label(ID_STATISTICS_PANEL, "No data available");
            label.setOutputMarkupId(true);
            container.add(label);
        }
    }

    protected String getIconBoxTextStyle() {
        return "font-size:22px";
    }

    protected String getIconBoxIconStyle() {
        return "font-size:25px";
    }

    protected boolean isBoxVisible() {
        return true;
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

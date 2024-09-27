/*
 * Copyright (C) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.CLASS_CSS;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.STYLE_CSS;
import static com.evolveum.midpoint.gui.impl.util.DetailsPageUtil.dispatchToObjectDetailsPage;

import java.io.Serial;
import java.util.Set;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.detection.BasePattern;
import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.data.column.CompositedIconTextPanel;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisOutlierType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

public class RoleAnalysisTableOpPanelPatternItem extends BasePanel<DetectedPattern> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_CONTAINER = "container";
    private static final String ID_ICON_PANEL = "icon-panel";
    private static final String ID_ICON = "icon";
    private static final String ID_DESCRIPTION_PANEL = "description-panel";
    private static final String ID_DESCRIPTION_TITLE = "description-title";
    private static final String ID_DESCRIPTION_TEXT = "description-text";
    RepeatingView descriptionText;

    public RoleAnalysisTableOpPanelPatternItem(String id, IModel<DetectedPattern> operationPanelModel) {
        super(id, operationPanelModel);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {

        add(AttributeModifier.replace(CLASS_CSS, "d-flex align-items-center rounded"));
        add(new Behavior() {
            @Override
            public void onConfigure(Component component) {
                super.onConfigure(component);
                add(AttributeModifier.replace(STYLE_CSS, new LoadableDetachableModel<String>() {
                    @Override
                    protected String load() {
                        if (getBackgroundColorStyle() == null) {
                            return null;
                        }
                        return getBackgroundColorStyle().getObject();
                    }
                }));
            }
        });

        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.setOutputMarkupId(true);
        add(container);

        container.add(new AjaxEventBehavior("click") {
            @Override
            protected void onEvent(AjaxRequestTarget ajaxRequestTarget) {
                performOnClick(ajaxRequestTarget);
            }
        });

        WebMarkupContainer iconPanel = new WebMarkupContainer(ID_ICON_PANEL);
        iconPanel.setOutputMarkupId(true);
        iconPanel.add(AttributeModifier.append(CLASS_CSS, appendIconPanelCssClass()));
        iconPanel.add(AttributeModifier.append(STYLE_CSS, appendIconPanelStyle()));
        container.add(iconPanel);

        Component icon = generateIconComponent(ID_ICON);
        icon.setOutputMarkupId(true);

        icon.add(new Behavior() {
            @Override
            public void onConfigure(Component component) {
                super.onConfigure(component);
//                icon.add(AttributeAppender.replace("class", replaceIconCssClass()));
                icon.add(AttributeModifier.replace(STYLE_CSS, replaceIconCssStyle()));
            }
        });

//        icon.add(AttributeAppender.append("class", appendIconCssClass()));
        iconPanel.add(icon);

        WebMarkupContainer descriptionPanel = new WebMarkupContainer(ID_DESCRIPTION_PANEL);
        descriptionPanel.setOutputMarkupId(true);
        container.add(descriptionPanel);

        Component descriptionTitle = getDescriptionTitleComponent(ID_DESCRIPTION_TITLE);
        descriptionPanel.add(descriptionTitle);

        descriptionText = new RepeatingView(ID_DESCRIPTION_TEXT);
        descriptionText.setOutputMarkupId(true);
        addDescriptionComponents();
        descriptionPanel.add(descriptionText);

    }

    public Component generateIconComponent(String idIcon) {

        String iconClass = GuiStyleConstants.CLASS_GROUP_ICON; //TODO default?
        switch (getModelObject().getPatternType()) {
            case CANDIDATE -> iconClass = GuiStyleConstants.CLASS_CANDIDATE_ROLE_ICON;
            case PATTERN -> iconClass = GuiStyleConstants.CLASS_DETECTED_PATTERN_ICON;
            case OUTLIER -> iconClass = GuiStyleConstants.CLASS_ICON_OUTLIER;
        }

        DetectedPattern pattern = getModelObject();

        String identifier = pattern.getIdentifier();
        if (identifier == null || identifier.isEmpty()) {
            identifier = "N/A";
        }
        identifier = identifier.replace("(outlier)", "");
        identifier = identifier.trim();
        if (identifier.length() > 2) {
            identifier = String.valueOf(identifier.charAt(0)) + identifier.charAt(identifier.length() - 1);
        }

        return new CompositedIconTextPanel(idIcon,
                "fa-2x " + iconClass + " text-dark",
                identifier,
                "text-secondary bg-white border border-white rounded-circle") {
            @Override
            protected String getBasicIconCssStyle() {
                return "font-size:25px; width:27px; height:27px;";
            }
        };
    }

    protected void addDescriptionComponents() {
        DetectedPattern pattern = getModelObject();
        switch (pattern.getPatternType()) {
            case CANDIDATE -> appendCandidateRolePanel(pattern);
            case OUTLIER -> appendOutlierPanel(pattern);
            default -> appendRoleSuggestionPanel(pattern);
        }
    }

    private void appendCandidateRolePanel(DetectedPattern pattern) {
        Set<String> users = pattern.getUsers();
        Set<String> roles = pattern.getRoles();
        if (users != null && !users.isEmpty() && roles != null && !roles.isEmpty()) {
            appendIcon(GuiStyleConstants.CLASS_OBJECT_USER_ICON_COLORED, null);
            appendText(" " + pattern.getUsers().size(), null);
            appendText("users - ", null);
            appendIcon(GuiStyleConstants.CLASS_OBJECT_ROLE_ICON_COLORED, null);
            appendText(" " + pattern.getRoles().size(), null);
            appendText(" roles", null);
        }
    }

    //TODO localizations
    private void appendRoleSuggestionPanel(DetectedPattern pattern) {
        String formattedReductionFactorConfidence = String.format("%.0f", pattern.getMetric());
        String formattedItemConfidence = String.format("%.1f", pattern.getItemsConfidence());
        appendIcon("fe fe-assignment", "color: red;");
        appendText(" " + formattedReductionFactorConfidence, null);
        appendText("relations - ", null);
        appendIcon("fa fa-leaf", "color: green");
        appendText(" " + formattedItemConfidence + "% ", null);
        appendText(" attribute score", null);
    }

    //TODO localizations
    private void appendOutlierPanel(DetectedPattern pattern) {
        String overallConfidence = String.format("%.0f", pattern.getMetric());
        appendIcon("fa fa-exclamation-circle", "color: red;");
        appendText(" " + overallConfidence, null);
        appendText("confidence ", null);
//        appendIcon("fa fa-leaf", "color: green");
//        appendText(" " + formattedItemConfidence + "% ", null);
//        appendText(" confidence", null);
    }

    public String appendIconPanelCssClass() {
        return "bg-secondary";
    }

    public String appendIconPanelStyle() {
        return switch (getModelObject().getPatternType()) {
            case PATTERN -> "background-color: #DFF2E3;";
            case CANDIDATE -> "background-color: #E2EEF5;";
            case OUTLIER -> "background-color: #F8D7DA;";
        };
    }

    //TODO collapse/expand icon
//    public String replaceIconCssClass() {
//        return "fa-2x fa fa-hashtag";
//    }

    public String replaceIconCssStyle() {
        return null;
    }

    public Component getDescriptionTitleComponent(String id) {
        LoadableDetachableModel<String> model = new LoadableDetachableModel<>() {
            @Override
            protected @NotNull String load() {
                DetectedPattern pattern = getModelObject();
                String identifier = pattern.getIdentifier();
                PageBase pageBase = getPageBase();
                switch (pattern.getPatternType()) {
                    case PATTERN -> {
                        return pageBase.createStringResource("RoleAnalysis.role.suggestion.title", (identifier))
                                .getString();
                    }
                    case CANDIDATE -> {
                        return pageBase.createStringResource("RoleAnalysis.role.candidate.title", (identifier))
                                .getString();
                    }
                    case OUTLIER -> {
                        return identifier;
                    }
                }
                return pageBase.createStringResource("RoleAnalysis.pattern.not.found.title")
                        .getString();
            }
        };

        AjaxLinkPanel linkPanel = new AjaxLinkPanel(id, model) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                DetectedPattern pattern = RoleAnalysisTableOpPanelPatternItem.this.getModelObject();
                if (pattern.getPatternType() == BasePattern.PatternType.OUTLIER) {
                    String outlierOid = pattern.getOutlierRef().getOid();
                    dispatchToObjectDetailsPage(RoleAnalysisOutlierType.class, outlierOid, getPageBase(), true);
                    return;
                }

                if (pattern.getPatternType() == BasePattern.PatternType.CANDIDATE) {
                    String roleOid = pattern.getRoleOid();
                    dispatchToObjectDetailsPage(RoleType.class, roleOid, getPageBase(), true);
                }

                if (pattern.getPatternType() == BasePattern.PatternType.PATTERN) {
                    RoleAnalysisDetectedPatternDetailsPopup component = new RoleAnalysisDetectedPatternDetailsPopup(
                            ((PageBase) getPage()).getMainPopupBodyId(),
                            Model.of(pattern));
                    ((PageBase) getPage()).showMainPopup(component, target);
                }
            }
        };

        linkPanel.setOutputMarkupId(true);
        return linkPanel;

//        WebMarkupContainer descriptionTitle = new WebMarkupContainer(id);
//        descriptionTitle.setOutputMarkupId(true);
//        return descriptionTitle;
    }

    protected void appendText(String text, String additionalCssClass) {
        Label label = new Label(descriptionText.newChildId(), text);
        label.add(AttributeModifier.append(CLASS_CSS, additionalCssClass));
        label.setOutputMarkupId(true);
        descriptionText.add(label);
    }

    protected void appendComponent(Component component) {
        descriptionText.add(component);
    }

    protected void appendIcon(String iconCssClass, String iconStyle) {
        Label label = new Label(descriptionText.newChildId(), "");
        label.add(AttributeModifier.append(CLASS_CSS, "align-self-center "));
        label.add(AttributeModifier.append(CLASS_CSS, iconCssClass));
        label.add(AttributeModifier.replace(STYLE_CSS, iconStyle));
        descriptionText.add(label);
    }

    protected void performOnClick(AjaxRequestTarget ajaxRequestTarget) {
        //override
    }

    public LoadableDetachableModel<String> getBackgroundColorStyle() {
        return new LoadableDetachableModel<>() {
            @Override
            protected String load() {
                String associatedColor = getModelObject().getAssociatedColor();
                if (associatedColor == null) {
                    return null;
                }

                return "background-color: " + getModelObject().getAssociatedColor() + ";";
            }
        };
    }

}

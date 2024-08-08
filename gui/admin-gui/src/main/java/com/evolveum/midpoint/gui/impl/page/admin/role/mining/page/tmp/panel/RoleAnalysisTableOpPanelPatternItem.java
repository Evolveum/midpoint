/*
 * Copyright (C) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel;

import com.evolveum.midpoint.common.mining.objects.detection.BasePattern;
import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.data.column.CompositedIconTextPanel;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.Set;

import static com.evolveum.midpoint.gui.impl.util.DetailsPageUtil.dispatchToObjectDetailsPage;

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

        add(new Behavior() {
            @Override
            public void onConfigure(Component component) {
                super.onConfigure(component);
                add(AttributeAppender.replace("style", new LoadableDetachableModel<String>() {
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
        iconPanel.add(AttributeAppender.append("class", appendIconPanelCssClass()));
        iconPanel.add(AttributeAppender.append("style", appendIconPanelStyle()));
        container.add(iconPanel);

        Component icon = generateIconComponent(ID_ICON);
        icon.setOutputMarkupId(true);

        icon.add(new Behavior() {
            @Override
            public void onConfigure(Component component) {
                super.onConfigure(component);
//                icon.add(AttributeAppender.replace("class", replaceIconCssClass()));
                icon.add(AttributeAppender.replace("style", replaceIconCssStyle()));
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
        return new CompositedIconTextPanel(idIcon,
                "fa-2x " + iconClass + " text-dark",
                0 + 1 + "", //TODO number of pattern
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

    private void appendRoleSuggestionPanel(DetectedPattern pattern) {
        String formattedReductionFactorConfidence = String.format("%.0f", pattern.getMetric());
        String formattedItemConfidence = String.format("%.1f", pattern.getItemsConfidence());
        appendIcon("fe fe-assignment", "color: red;");
        appendText(" " + formattedReductionFactorConfidence, null);
        appendText("relations - ", null);
        appendIcon("fa fa-leaf", "color: green");
        appendText(" " + formattedItemConfidence + "% ", null);
        appendText(" confidence", null);
    }

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
                switch (pattern.getPatternType()) {
                    case PATTERN -> {
                        return "Role suggestion #" + (identifier + 1);
                    }
                    case CANDIDATE -> {
                        return "Candidate role " + (identifier);
                    }
                    case OUTLIER -> {
                        return "Outlier pattern #" + (identifier + 1);
                    }
                }
                return "pattern type not found";
            }
        };

        RepeatingView repeatingView = new RepeatingView(id);
        repeatingView.setOutputMarkupId(true);

        Label label = new Label(repeatingView.newChildId(), model);
        label.setOutputMarkupId(true);

        AjaxIconButton iconButton = new AjaxIconButton(repeatingView.newChildId(),
                Model.of("fa fa-list"), Model.of("")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                DetectedPattern pattern = RoleAnalysisTableOpPanelPatternItem.this.getModelObject();
                if (pattern.getPatternType() == BasePattern.PatternType.CANDIDATE) {
                    String roleOid = pattern.getRoleOid();
                    dispatchToObjectDetailsPage(RoleType.class, roleOid, getPageBase(), true);
                }

                RoleAnalysisDetectedPatternDetailsPopup component = new RoleAnalysisDetectedPatternDetailsPopup(
                        ((PageBase) getPage()).getMainPopupBodyId(),
                        Model.of(pattern));
                ((PageBase) getPage()).showMainPopup(component, target);
            }
        };
        iconButton.setOutputMarkupId(true);
        iconButton.add(AttributeAppender.replace("class", "p-0"));
        repeatingView.add(label);
        repeatingView.add(iconButton);
        return repeatingView;


//        WebMarkupContainer descriptionTitle = new WebMarkupContainer(id);
//        descriptionTitle.setOutputMarkupId(true);
//        return descriptionTitle;
    }

    protected void appendText(String text, String additionalCssClass) {
        Label label = new Label(descriptionText.newChildId(), text);
        label.add(AttributeModifier.append("class", additionalCssClass));
        label.setOutputMarkupId(true);
        descriptionText.add(label);
    }

    protected void appendComponent(Component component) {
        descriptionText.add(component);
    }

    protected void appendIcon(String iconCssClass, String iconStyle) {
        Label label = new Label(descriptionText.newChildId(), "");
        label.add(AttributeModifier.append("class", "align-self-center "));
        label.add(AttributeModifier.append("class", iconCssClass));
        label.add(AttributeModifier.replace("style", iconStyle));
        descriptionText.add(label);
    }

    protected void performOnClick(AjaxRequestTarget ajaxRequestTarget) {

    }

    public LoadableDetachableModel<String> getBackgroundColorStyle() {
        return new LoadableDetachableModel<>() {
            @Override
            protected String load() {
                return "background-color: " + getModelObject().getAssociatedColor() + ";";
            }
        };
    }

}

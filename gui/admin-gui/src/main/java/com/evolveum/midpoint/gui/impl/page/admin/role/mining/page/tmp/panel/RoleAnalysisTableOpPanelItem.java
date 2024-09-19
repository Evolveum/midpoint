/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel;

import java.io.Serial;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.model.OperationPanelModel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.html.WebMarkupContainer;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.CLASS_CSS;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.STYLE_CSS;

public class RoleAnalysisTableOpPanelItem extends BasePanel<OperationPanelModel> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_CONTAINER = "container";
    private static final String ID_ICON_PANEL = "icon-panel";
    private static final String ID_ICON = "icon";
    private static final String ID_DESCRIPTION_PANEL = "description-panel";
    private static final String ID_DESCRIPTION_TITLE = "description-title";
    private static final String ID_DESCRIPTION_TEXT = "description-text";
    RepeatingView descriptionText;

    public RoleAnalysisTableOpPanelItem(String id, IModel<OperationPanelModel> operationPanelModel) {
        super(id, operationPanelModel);
//        initLayout(operationPanelModel);
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
                icon.add(AttributeModifier.replace(CLASS_CSS, replaceIconCssClass()));
                icon.add(AttributeModifier.replace(STYLE_CSS, replaceIconCssStyle()));
            }
        });

//        icon.add(AttributeAppender.append("class", appendIconCssClass()));
        iconPanel.add(icon);

        WebMarkupContainer descriptionPanel = new WebMarkupContainer(ID_DESCRIPTION_PANEL);
        descriptionPanel.setOutputMarkupId(true);
        descriptionPanel.add(new VisibleBehaviour(() -> getModelObject().isPanelExpanded()));
        container.add(descriptionPanel);

        Component descriptionTitle = getDescriptionTitleComponent(ID_DESCRIPTION_TITLE);
        descriptionPanel.add(descriptionTitle);

        descriptionText = new RepeatingView(ID_DESCRIPTION_TEXT);
        descriptionText.setOutputMarkupId(true);
        addDescriptionComponents();
        descriptionPanel.add(descriptionText);

    }

    public Component generateIconComponent(String idIcon) {
        return new WebMarkupContainer(idIcon);
    }

    protected void addDescriptionComponents() {
    //override
    }

    public String appendIconPanelCssClass() {
        return "bg-secondary";
    }

    public String appendIconPanelStyle() {
        return null;
    }

    public String replaceIconCssClass() {
        return "fa-2x fa fa-hashtag";
    }

    public String replaceIconCssStyle() {
        return null;
    }

    public Component getDescriptionTitleComponent(String id) {
        WebMarkupContainer descriptionTitle = new WebMarkupContainer(id);
        descriptionTitle.setOutputMarkupId(true);
        return descriptionTitle;
    }

    protected void appendText(IModel<String> text, String additionalCssClass) {
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
        return null;
    }

}

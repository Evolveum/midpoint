/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel;

import java.io.Serial;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.util.TooltipBehavior;

public class RoleAnalysisInfoItem extends BasePanel<String> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_LINK = "action";
    private static final String ID_DESCRIPTION_CONTAINER = "description-container";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_ICON = "icon";
    private static final String ID_ICON_CONTAINER = "icon-container";
    private static final String ID_BOX = "box";

    RepeatingView description;


    public RoleAnalysisInfoItem(String id, IModel<String> model) {
        super(id, model);
        initLayout();
    }

    private void initLayout() {

        WebMarkupContainer iconContainer = new WebMarkupContainer(ID_ICON_CONTAINER);
        iconContainer.add(AttributeModifier.append("class", getIconContainerCssClass()));
        iconContainer.setOutputMarkupId(true);

        iconContainer.add(new AjaxEventBehavior("click") {
            @Override
            protected void onEvent(AjaxRequestTarget target) {
                onClickIconPerform(target);
            }
        });

        add(iconContainer);

        WebMarkupContainer box = new WebMarkupContainer(ID_BOX);
        box.add(AttributeModifier.replace("class", "info-box-content"));
        box.add(new VisibleBehaviour(this::isBoxVisible));
        box.setOutputMarkupId(true);
        add(box);

        Label iconText = new Label("iconText", getIconBoxText());
        iconText.add(AttributeModifier.replace("style", getIconBoxTextStyle()));
        iconText.add(AttributeModifier.replace("class", getIconBoxTextClass()));
        iconText.setOutputMarkupId(true);
        iconContainer.add(iconText);

        Label icon = new Label(ID_ICON, "");
        icon.add(AttributeModifier.replace("class", getIconClass()));
        icon.add(AttributeModifier.replace("style", getIconBoxIconStyle()));
        icon.setOutputMarkupId(true);
        iconContainer.add(icon);

        WebMarkupContainer descriptionContainer = new WebMarkupContainer(ID_DESCRIPTION_CONTAINER);
        descriptionContainer.setOutputMarkupId(true);
        descriptionContainer.add(AttributeModifier.replace("title", getDescriptionModel()));
        descriptionContainer.add(new TooltipBehavior());
        box.add(descriptionContainer);
        description = new RepeatingView(ID_DESCRIPTION);
        description.add(AttributeModifier.replace("style", getDescriptionStyle()));
        description.add(AttributeModifier.replace("class", getDescriptionCssClass()));
        description.setOutputMarkupId(true);
        descriptionContainer.add(description);

        addDescriptionComponents();

        AjaxLinkPanel link = new AjaxLinkPanel(ID_LINK, getLinkModel()) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                onClickLinkPerform(target);
            }
        };
        link.setOutputMarkupId(true);
        link.add(AttributeModifier.replace("style", getLinkStyle()));
        link.add(AttributeModifier.replace("class", getLinkCssClass()));
        box.add(link);

    }

    protected IModel<String> getDescriptionModel() {
        return Model.of("");
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

    protected RepeatingView getRepeatedView() {
        return description;
    }

    protected void appendText(String text) {
        Label label = new Label(getRepeatedView().newChildId(), text);
        label.add(AttributeModifier.replace("style", getDescriptionStyle()));
        getRepeatedView().add(label);
    }

    protected void appendComponent(Component component) {
        getRepeatedView().add(component);
    }

    protected void appendIcon(String iconCssClass, String iconStyle) {
        Label label = new Label(getRepeatedView().newChildId(), "");
        label.add(AttributeModifier.replace("class", iconCssClass));
        label.add(AttributeModifier.replace("style", iconStyle));
        getRepeatedView().add(label);
    }

    protected void addDescriptionComponents() {
    }

    protected void onClickLinkPerform(AjaxRequestTarget target) {

    }

    protected void onClickIconPerform(AjaxRequestTarget target) {

    }

    protected String getIconClass() {
        return "fa-2x " + GuiStyleConstants.CLASS_DETECTED_PATTERN_ICON;
    }

    protected String getIconBoxTextClass() {
        return null;
    }

    protected String getIconBoxText() {
        return null;
    }

    protected String getIconContainerCssClass() {
        return null;
    }

    protected String getDescriptionCssClass() {
        return "";
    }

    protected String getDescriptionStyle() {
        return "font-size:14px; line-height: 1.1;";
    }

    protected IModel<String> getLinkModel() {
        return Model.of("Explore");
    }

    protected String getLinkStyle() {
        return "";
    }

    protected String getLinkCssClass() {
        return "";
    }

}

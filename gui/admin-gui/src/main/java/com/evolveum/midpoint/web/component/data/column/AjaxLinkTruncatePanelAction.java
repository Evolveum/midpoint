/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data.column;

import java.io.Serial;

import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisOperationMode;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;

public class AjaxLinkTruncatePanelAction extends Panel {
    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_LINK = "link";
    private static final String ID_LINK_LABEL = "link_label";
    private static final String ID_ICON = "icon";

    private static final String ID_LINK_2 = "link_2";
    private static final String ID_IMAGE = "image";

    public AjaxLinkTruncatePanelAction(String id, IModel<?> labelModel, StringResourceModel popupText, DisplayType displayType,
             LoadableDetachableModel<RoleAnalysisOperationMode> status) {
        super(id);

        WebMarkupContainer webMarkupContainer = new WebMarkupContainer("container");
        if(getColor() != null) {
            webMarkupContainer.add(new AttributeAppender("class", getColor()));
        }
        webMarkupContainer.setOutputMarkupId(true);
        webMarkupContainer.setOutputMarkupPlaceholderTag(true);
        webMarkupContainer.add(new AttributeAppender("class",getCssContainer()));
        add(webMarkupContainer);

        AjaxLink<String> link = new AjaxLink<>(ID_LINK) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                AjaxLinkTruncatePanelAction.this.onClick(target);
            }

        };

        Label label = new Label(ID_LINK_LABEL, labelModel);
        label.add(new InfoTooltipBehavior() {
            @Override
            public String getCssClass() {
                return " text-truncate";
            }
        });
        label.add(AttributeModifier.replace("title", popupText));

        link.add(label);
        link.add(new EnableBehaviour(AjaxLinkTruncatePanelAction.this::isEnabled));
        link.add(new AttributeModifier("style", "height:" + getLabelHeight()));

        webMarkupContainer.add(new ImagePanel(ID_ICON, Model.of(displayType)));
        webMarkupContainer.add(link);

        initLayout(status,webMarkupContainer);
    }

    protected String getColor(){
        return null;
    }
    public boolean isEnabled() {
        return true;
    }

    public void onClick(AjaxRequestTarget target) {
    }

    public String getLabelHeight() {
        return "70px";
    }

    public String getCssContainer(){
        return " font-weight-normal";
    }



    public String getModel(LoadableDetachableModel<RoleAnalysisOperationMode> status) {
        return status.getObject().getDisplayString();
    }

    private void initLayout(LoadableDetachableModel<RoleAnalysisOperationMode> status, WebMarkupContainer webMarkupContainer) {
        setOutputMarkupId(true);

        Label image = new Label(ID_IMAGE);
        image.add(AttributeModifier.replace("class", getModel(status)));
        image.setOutputMarkupId(true);
        AjaxLink<Void> link = new AjaxLink<>(ID_LINK_2) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                RoleAnalysisOperationMode roleAnalysisOperationMode = onClickPerformedAction(target, null);

                if (roleAnalysisOperationMode.equals(RoleAnalysisOperationMode.NEUTRAL)) {
                    image.add(AttributeModifier.replace("class", RoleAnalysisOperationMode.ADD.getDisplayString()));
                } else if (roleAnalysisOperationMode.equals(RoleAnalysisOperationMode.ADD)) {
                    image.add(AttributeModifier.replace("class", RoleAnalysisOperationMode.REMOVE.getDisplayString()));
                } else if (roleAnalysisOperationMode.equals(RoleAnalysisOperationMode.REMOVE)) {
                    image.add(AttributeModifier.replace("class", RoleAnalysisOperationMode.NEUTRAL.getDisplayString()));
                }
                target.add(image);
            }
        };
        link.add(image);
        link.setOutputMarkupId(true);

        webMarkupContainer.add(link);

    }

    protected RoleAnalysisOperationMode onClickPerformedAction(AjaxRequestTarget target, RoleAnalysisOperationMode roleAnalysisOperationMode) {
        return roleAnalysisOperationMode;
    }
}

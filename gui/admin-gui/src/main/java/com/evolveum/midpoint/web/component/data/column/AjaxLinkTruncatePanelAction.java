/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data.column;

import java.io.Serial;

import com.evolveum.midpoint.common.mining.utils.values.RoleAnalysisOperationMode;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.data.column.CompositedIconPanel;

import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
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
    private static final String ID_LINK_ICON = "link_icon";
    private static final String ID_IMAGE = "image";
    private static final String ID_CONTAINER = "container";

    public AjaxLinkTruncatePanelAction(String id, IModel<?> labelModel, StringResourceModel popupText,
            DisplayType displayType, LoadableDetachableModel<RoleAnalysisOperationMode> status) {
        super(id);
        initLayout(status, labelModel, popupText, null, displayType);
    }

    public AjaxLinkTruncatePanelAction(String id, IModel<?> labelModel, StringResourceModel popupText,
            CompositedIcon compositedIcon, LoadableDetachableModel<RoleAnalysisOperationMode> status) {
        super(id);
        initLayout(status, labelModel, popupText, compositedIcon, null);
    }

    protected String getColor() {
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

    public String getCssContainer() {
        return " font-weight-normal";
    }

    public String getModel(LoadableDetachableModel<RoleAnalysisOperationMode> status) {
        return status.getObject().getDisplayString();
    }

    private void initLayout(LoadableDetachableModel<RoleAnalysisOperationMode> status,
            IModel<?> labelModel,
            StringResourceModel popupText,
            CompositedIcon compositedIcon,
            DisplayType displayType) {
        WebMarkupContainer webMarkupContainer = new WebMarkupContainer(ID_CONTAINER);
        if (getColor() != null) {
            webMarkupContainer.add(new AttributeAppender("class", getColor()));
        }
        webMarkupContainer.setOutputMarkupId(true);
        webMarkupContainer.setOutputMarkupPlaceholderTag(true);
        webMarkupContainer.add(new AttributeAppender("class", getCssContainer()));
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
        if (compositedIcon != null) {
            webMarkupContainer.add(new CompositedIconPanel(ID_ICON, Model.of(compositedIcon)));
        } else {
            webMarkupContainer.add(new ImagePanel(ID_ICON, Model.of(displayType)));
        }
        webMarkupContainer.add(link);

        setOutputMarkupId(true);

        Label image = new Label(ID_IMAGE);
        image.add(AttributeModifier.replace("class", getModel(status)));
        image.setOutputMarkupId(true);
        AjaxLink<Void> actionLink = new AjaxLink<>(ID_LINK_ICON) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                RoleAnalysisOperationMode roleAnalysisOperationMode = onClickPerformedAction(target, status.getObject());

                if (roleAnalysisOperationMode.equals(RoleAnalysisOperationMode.EXCLUDE)) {
                    getImageComponent().add(AttributeModifier.replace("class", RoleAnalysisOperationMode.INCLUDE.getDisplayString()));
                } else if (roleAnalysisOperationMode.equals(RoleAnalysisOperationMode.INCLUDE)) {
                    getImageComponent().add(AttributeModifier.replace("class", RoleAnalysisOperationMode.EXCLUDE.getDisplayString()));
                }

                target.add(getImageComponent());
            }
        };
        actionLink.add(image);
        actionLink.setOutputMarkupId(true);

        webMarkupContainer.add(actionLink);

    }

    private Component getImageComponent() {
        return get(((PageBase) getPage()).createComponentPath(ID_CONTAINER, ID_LINK_ICON, ID_IMAGE));
    }

    protected RoleAnalysisOperationMode onClickPerformedAction(AjaxRequestTarget target, RoleAnalysisOperationMode roleAnalysisOperationMode) {
        return roleAnalysisOperationMode;
    }
}

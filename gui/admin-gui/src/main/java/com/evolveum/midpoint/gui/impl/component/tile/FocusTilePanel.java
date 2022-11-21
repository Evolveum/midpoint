/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.tile;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.data.column.RoundedImagePanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.IResource;

import java.io.Serializable;

/**
 * @author lskublik
 */
public class FocusTilePanel<F extends Serializable, T extends Tile<F>> extends BasePanel<T> {

    private static final long serialVersionUID = 1L;

    private static final String ID_LOGO = "logo";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_DETAILS = "details";
    private static final String ID_ICON = "icon";
    private static final String ID_TITLE = "title";

    public FocusTilePanel(String id, IModel<T> model) {
        super(id, model);

        initLayout();
    }

    protected void initLayout() {
        setOutputMarkupId(true);

        RoundedImagePanel logo = new RoundedImagePanel(ID_LOGO, () -> createDisplayType(getModel()), createPreferredImage(getModel()));
        add(logo);

        Label description = new Label(ID_DESCRIPTION, () -> getModelObject().getDescription());
        description.add(AttributeAppender.replace("title", () -> getModelObject().getDescription()));
        description.add(new TooltipBehavior());
        add(description);

        WebMarkupContainer icon = new WebMarkupContainer(ID_ICON);
        icon.add(AttributeAppender.append("class", () -> getModelObject().getIcon()));
        add(icon);

        IModel<String> titleModel = () -> {
            String title = getModelObject().getTitle();
            return title != null ? getString(title, null, title) : null;
        };

        Label title = new Label(ID_TITLE, titleModel);
        title.add(AttributeAppender.replace("title", titleModel));
        title.add(new TooltipBehavior());
        add(title);

//        add(new AjaxEventBehavior("click") {
//
//            @Override
//            protected void onEvent(AjaxRequestTarget target) {
//                FocusTilePanel.this.onClick(target);
//            }
//        });

        Component details = createDetailsButton(ID_DETAILS);
        add(details);
    }

    protected Component createDetailsButton(String id) {
        return new AjaxLink<>(id) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                FocusTilePanel.this.onDetails(target);
            }
        };
    }

    protected void onDetails(AjaxRequestTarget target) {

    }

    protected void onClick(AjaxRequestTarget target) {
        getModelObject().toggle();
        target.add(this);
    }

    protected DisplayType createDisplayType(IModel<T> model) {
        return null;
    }

    protected IModel<IResource> createPreferredImage(IModel<T> model) {
        return null;
    }

    protected RoundedImagePanel getLogo() {
        return (RoundedImagePanel) get(ID_LOGO);
    }
}

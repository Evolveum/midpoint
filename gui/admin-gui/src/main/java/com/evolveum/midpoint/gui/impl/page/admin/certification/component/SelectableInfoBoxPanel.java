/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.certification.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;

import java.io.Serial;

public class SelectableInfoBoxPanel<T> extends BasePanel<T> {

    private static final Trace LOGGER = TraceManager.getTrace(SelectableInfoBoxPanel.class);

    private static final String ID_IMAGE = "imageId";
    private static final String ID_LINK = "link";
    private static final String ID_LABEL = "labelId";
    private static final String ID_DESCRIPTION = "descriptionId";

    public SelectableInfoBoxPanel(String id, IModel<T> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initLayout();
    }

    private void initLayout() {

        WebMarkupContainer linkItem = new WebMarkupContainer(ID_LINK);

        AjaxEventBehavior linkClick = new AjaxEventBehavior("click") {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected void onEvent(AjaxRequestTarget target) {
                itemSelectedPerformed(getModelObject(), target);
            }
        };
        linkItem.add(linkClick);
        linkItem.add(AttributeModifier.append("class", getAdditionalLinkStyle()));
        add(linkItem);

        Label icon = new Label(ID_IMAGE);
        icon.add(AttributeModifier.append("class", getIconClassModel()));
        icon.add(AttributeAppender.append("style", "--bs-bg-opacity: .5;"));
        linkItem.add(icon);

        linkItem.add(new Label(ID_LABEL, getLabelModel()));

        Label description = new Label(ID_DESCRIPTION, getDescriptionModel());
        linkItem.add(description);

    }

    protected IModel<String> getIconClassModel() {
        return () -> "";
    }

    protected IModel<String> getIconStyleModel() {
        return () -> "";
    }

    protected IModel<String> getLabelModel() {
        return () -> "";
    }

    protected IModel<String> getDescriptionModel() {
        return () -> "";
    }

    protected void itemSelectedPerformed(T itemObject, AjaxRequestTarget target) {
    }

    protected IModel<String> getAdditionalLinkStyle() {
        return () -> "";
    }
}

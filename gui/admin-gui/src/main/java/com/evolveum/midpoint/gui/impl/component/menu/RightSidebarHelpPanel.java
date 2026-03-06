/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.menu;

import java.io.Serial;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.util.SerializableFunction;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

public class RightSidebarHelpPanel extends BasePanel<Void> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_TITLE = "title";
    private static final String ID_CLOSE = "close";
    private static final String ID_CONTENT = "content";

    private final IModel<Boolean> visible = Model.of(false);

    private IModel<String> titleModel = Model.of();

    public RightSidebarHelpPanel(String id) {
        super(id);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        initLayout();
    }

    private void initLayout() {
        add(AttributeAppender.append("class", "right-sidebar-help"));
        add(new VisibleBehaviour(visible::getObject));

        setOutputMarkupPlaceholderTag(true);

        Label title = new Label(ID_TITLE, () -> titleModel.getObject());
        title.add(new VisibleBehaviour(() -> titleModel.getObject() != null && StringUtils.isNotEmpty(titleModel.getObject())));
        add(title);

        AjaxLink<Void> close = new AjaxLink<>(ID_CLOSE) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                closePerformed(target);
            }
        };
        add(close);

        add(new WebMarkupContainer(ID_CONTENT));
    }

    private void closePerformed(AjaxRequestTarget target) {
        close(target);
    }

    public void open(AjaxRequestTarget target) {
        visible.setObject(true);
        target.add(this);
    }

    public void close(AjaxRequestTarget target) {
        visible.setObject(false);
        target.add(this);
    }

    /**
     * Replace the content of the right sidebar with a new component provided by the given function.
     *
     * @param componentProvider The function will be called with the ID of the new component to create and return.
     */
    public void replaceContent(IModel<String> titleModel, SerializableFunction<String, Component> componentProvider) {
        this.titleModel = titleModel != null ? titleModel : Model.of();

        Component component = componentProvider.apply(ID_CONTENT);
        replace(Objects.requireNonNullElseGet(component, () -> new WebMarkupContainer(ID_CONTENT)));
    }
}

/*
 * Copyright (C) 2020-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.component;

import java.io.Serial;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

public class InlineOperationalButtonsPanel<O extends ObjectType> extends OperationalButtonsPanel<O> {
    @Serial private static final long serialVersionUID = 1L;

    public InlineOperationalButtonsPanel(String id, LoadableModel<PrismObjectWrapper<O>> wrapperModel) {
        super(id, wrapperModel);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
    }

    @Override
    protected void addStateButtons(@NotNull RepeatingView stateButtonsView) {
        stateButtonsView.add(createBackButton());

        Label title = new Label(stateButtonsView.newChildId(), getTitle());
        title.add(AttributeAppender.append("style", getTitleStyle()));
        stateButtonsView.add(title);

        super.addStateButtons(stateButtonsView);
    }

    protected String getTitleStyle() {
        return " font-size:25px;";
    }

    protected AjaxIconButton createBackButton() {
        AjaxIconButton back = new AjaxIconButton("back", Model.of(GuiStyleConstants.ARROW_LEFT),
                getPageBase().createStringResource("pageAdminFocus.button.back")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                backPerformed(ajaxRequestTarget);
            }
        };

        back.showTitleAsLabel(true);
        back.add(AttributeAppender.append("class", "btn btn-default btn-sm"));
        return back;
    }

    protected IModel<String> getTitle() {
        return Model.of("");
    }

    @Override
    protected void buildInitialRepeatingView(RepeatingView repeatingView) {
        createDeleteButton(repeatingView);
        addButtons(repeatingView);
        createSaveButton(repeatingView);
    }
}

/*
 * Copyright (C) 2020-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.component;

import java.io.Serial;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.web.component.AjaxCompositedIconSubmitButton;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public abstract class InlineOperationalButtonsPanel<O extends ObjectType> extends OperationalButtonsPanel<O> {
    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_BUTTONS_CONTAINER = "buttonsContainer";
    private static final String ID_LEFT_BUTTONS = "leftButtons";
    private static final String ID_TITLE = "title";
    private static final String ID_RIGHT_BUTTONS = "rightButtons";
    private static final String ID_DELETE_BUTTON_CONTAINER = "deleteButtonContainer";
    private static final String ID_DELETE_BUTTON = "deleteButton";

    public InlineOperationalButtonsPanel(String id, LoadableModel<PrismObjectWrapper<O>> wrapperModel) {
        super(id, wrapperModel);
    }

    protected void initButtons() {
        WebMarkupContainer buttonsContainer = new WebMarkupContainer(ID_BUTTONS_CONTAINER);
        buttonsContainer.setOutputMarkupId(true);
        add(buttonsContainer);

        RepeatingView leftButtonsView = new RepeatingView(ID_LEFT_BUTTONS);
        buttonsContainer.add(leftButtonsView);
        createBackButton(leftButtonsView);
        addLefButtons(leftButtonsView);
        applyWcagRules(leftButtonsView);

        Label title = new Label(ID_TITLE, getTitle());
        buttonsContainer.add(title);

        WebMarkupContainer deleteButtonContainer = new WebMarkupContainer(ID_DELETE_BUTTON_CONTAINER);
        deleteButtonContainer.setOutputMarkupId(true);
        buttonsContainer.add(deleteButtonContainer);

        RepeatingView deleteButtonView = new RepeatingView(ID_DELETE_BUTTON);
        deleteButtonContainer.add(deleteButtonView);
        createDeleteButton(deleteButtonView);
        applyWcagRules(deleteButtonView);


        RepeatingView rightButtonsView = new RepeatingView(ID_RIGHT_BUTTONS);
        buttonsContainer.add(rightButtonsView);
        buildInitialRepeatingView(rightButtonsView);
        applyWcagRules(rightButtonsView);

       /* if (rightButtonsView.size() > 1) {
            deleteButtonContainer.add(AttributeAppender.append("class", "objectButtons"));
        } */
    }

    @Override
    protected String getDeleteButtonCssClass() {
        return "btn btn-link link-danger";
    }

    @Override
    protected String getBackCssClass() {
        return "btn btn-link";
    }


    private void applyWcagRules(RepeatingView repeatingView) {
        repeatingView.streamChildren()
                .forEach(button -> {
                    String title = null;
                    if (button instanceof AjaxIconButton) {
                        title = ((AjaxIconButton) button).getTitle().getObject();
                    } else if (button instanceof AjaxCompositedIconSubmitButton) {
                        title = ((AjaxCompositedIconSubmitButton) button).getTitle().getObject();
                    }

                    if (StringUtils.isNotEmpty(title)) {
                        button.add(AttributeAppender.append(
                                "aria-label",
                                getPageBase().createStringResource("OperationalButtonsPanel.buttons.main.label", title)));
                    }

                    if (button instanceof AbstractLink) {
                        button.add(new Behavior() {

                            @Serial private static final long serialVersionUID = 1L;

                            @Override
                            public void bind(Component component) {
                                super.bind(component);

                                component.add(AttributeModifier.replace("onkeydown",
                                        Model.of(
                                                "if (event.keyCode == 32 || event.keyCode == 13){"
                                                        + "this.click();"
                                                        + "}"
                                        )));
                            }
                        });
                        button.add(AttributeAppender.append("role", "button"));
                        button.add(AttributeAppender.append("tabindex", "0"));
                    }
                });
    }

    @Override
    protected final void addStateButtons(RepeatingView stateButtonsView) {
        super.addStateButtons(stateButtonsView);
    }

    @Override
    protected final void addButtons(RepeatingView repeatingView) {
        super.addButtons(repeatingView);
    }

    protected void addRightButtons(@NotNull RepeatingView rightButtonsView) {
    }

    protected void addLefButtons(@NotNull RepeatingView leftButtonsView) {
    }

    @Override
    protected void buildInitialRepeatingView(RepeatingView repeatingView) {
        addRightButtons(repeatingView);
        createSaveButton(repeatingView);
    }

    @Override
    protected abstract IModel<String> getDeleteButtonLabelModel(PrismObjectWrapper<O> modelObject);

    @Override
    protected abstract IModel<String> createSubmitButtonLabelModel(PrismObjectWrapper<O> modelObject);

    protected IModel<String> getTitle() {
        return Model.of("");
    }

    @Override
    protected String getSaveButtonAdditionalCssClass() {
        return "btn btn-success";
    }
}

/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.dialog;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.result.MessagePanel;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.util.Describable;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.List;

public class ConfirmationWithOptionsContentPanel<T extends Describable>
        extends BasePanel<ConfirmationWithOptionsDto<T>> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_SUBTITLE = "subtitle";
    private static final String ID_INFO_MESSAGE = "infoMessage";
    private static final String ID_OPTIONS_CONTAINER = "requestContainer";
    private static final String ID_OPTION_LABEL = "requestLabel";
    private static final String ID_LIST_VIEW = "listView";
    private static final String ID_OPTION_CHECK = "check";
    private static final String ID_OPTION_TITLE = "title";
    private static final String ID_OPTION_DESCRIPTION = "description";
    private static final String ID_OPTION_ACTION = "action";

    public ConfirmationWithOptionsContentPanel(
            String id,
            IModel<ConfirmationWithOptionsDto<T>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        Label subtitle = new Label(ID_SUBTITLE, getModelObject().getConfirmationSubtitle());
        subtitle.setOutputMarkupPlaceholderTag(true);
        add(subtitle);

        initInfoMessage();
        initOptions();
    }

    public IModel<List<ConfirmationOption<T>>> getSelectedOptionsModel() {
        return () -> getModelObject().getConfirmationOptions().stream()
                .filter(ConfirmationOption::isSelected)
                .toList();
    }

    private void initInfoMessage() {
        ConfirmationWithOptionsDto<T> config = getModelObject();

        MessagePanel<?> infoMessage = new MessagePanel<>(
                ID_INFO_MESSAGE,
                MessagePanel.MessagePanelType.INFO,
                config.getConfirmationInfoMessage(),
                false) {

            @Override
            protected @NotNull Object getIconTypeCss() {
                return "fa fa-info-circle";
            }

            @Override
            protected @NotNull IModel<String> createHeaderCss() {
                return Model.of("alert-info");
            }
        };

        infoMessage.setOutputMarkupId(true);
        infoMessage.add(new VisibleBehaviour(() -> config.getConfirmationInfoMessage() != null));
        add(infoMessage);
    }

    private void initOptions() {
        Form<?> form = new Form<>("form");
        form.setOutputMarkupId(true);
        add(form);

        WebMarkupContainer container = new WebMarkupContainer(ID_OPTIONS_CONTAINER);
        container.setOutputMarkupId(true);
        container.add(new VisibleBehaviour(() -> numberOfOptions() > 0));
        form.add(container);

        container.add(new Label(ID_OPTION_LABEL, getModelObject().getConfirmationOptionsTitle()));

        ListView<ConfirmationOption<T>> listView = new ListView<>(ID_LIST_VIEW,
                () -> getModelObject().getConfirmationOptions()) {

            @Override
            protected void populateItem(@NotNull ListItem<ConfirmationOption<T>> item) {
                ConfirmationOption<T> option = item.getModelObject();

                item.add(new AjaxCheckBox(ID_OPTION_CHECK, option.selected()) {
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                    }
                });

                item.add(new Label(ID_OPTION_TITLE, option.title()));
                item.add(new Label(ID_OPTION_DESCRIPTION, option.description()));
                item.add(buildAction(option));

                if (item.getIndex() < numberOfOptions() - 1) {
                    item.add(AttributeModifier.append("class", "border-bottom"));
                }
            }
        };

        listView.setOutputMarkupId(true);
        container.add(listView);
    }

    private AjaxIconButton buildAction(ConfirmationOption<T> option) {
        AjaxIconButton action = new AjaxIconButton(
                ID_OPTION_ACTION,
                Model.of("fa fa-info-circle"),
                createStringResource("ConfirmationWithOptionsPanel.confirmationOptions.action.more.info")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                option.onClick().accept(option, target);
            }
        };

        action.showTitleAsLabel(true);
        action.add(new VisibleBehaviour(() -> option.onClick() != null));
        return action;
    }

    private int numberOfOptions() {
        List<ConfirmationOption<T>> options = getModelObject().getConfirmationOptions();
        return options != null ? options.size() : 0;
    }
}

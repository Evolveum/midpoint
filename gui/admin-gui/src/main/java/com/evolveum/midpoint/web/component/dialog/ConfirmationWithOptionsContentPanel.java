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
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.util.List;

public class ConfirmationWithOptionsContentPanel<T extends Describable>
        extends BasePanel<ConfirmationWithOptionsDto<T>> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_SUBTITLE = "subtitle";
    private static final String ID_INFO_MESSAGE = "infoMessage";
    private static final String ID_ERROR_MESSAGE = "errorMessage";
    private static final String ID_ERROR_TEXT = "errorText";
    private static final String ID_WARNING_MESSAGE = "warningMessage";
    private static final String ID_WARNING_TEXT = "warningText";
    private static final String ID_INFO_DETAILS = "infoDetails";
    private static final String ID_OPTIONS_CONTAINER = "requestContainer";
    private static final String ID_OPTION_LABEL = "requestLabel";
    private static final String ID_LIST_VIEW = "listView";
    private static final String ID_OPTION_CHECK = "check";
    private static final String ID_OPTION_TITLE = "title";
    private static final String ID_OPTION_DESCRIPTION = "description";
    private static final String ID_OPTION_ACTION = "action";

    private static final String ID_AI_PROVIDER = "aiProvider";
    private static final String ID_AI_MODEL = "aiModel";
    private static final String ID_AI_STATUS_CONTAINER = "aiStatusContainer";
    private static final String ID_AI_STATUS = "aiStatus";

    public ConfirmationWithOptionsContentPanel(
            String id,
            IModel<ConfirmationWithOptionsDto<T>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        addSubtitle();

        initInfoMessage();
        initErrorMessage();
        initWarningMessage();
        initAiInfo();
        initOptions();
    }

    private void addSubtitle() {
        Label subtitle = new Label(ID_SUBTITLE, getDto().getConfirmationSubtitle());
        subtitle.setOutputMarkupPlaceholderTag(true);
        subtitle.add(new VisibleBehaviour(() -> getDto().isAiServiceAvailable()));
        add(subtitle);
    }

    private void initInfoMessage() {
        MessagePanel<?> infoMessage = new MessagePanel<>(
                ID_INFO_MESSAGE,
                MessagePanel.MessagePanelType.INFO,
                getConfirmationInfoMessage(),
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
        infoMessage.add(new VisibleBehaviour(() -> getConfirmationInfoMessage() != null));
        add(infoMessage);
    }

    private void initErrorMessage() {
        WebMarkupContainer errorContainer = new WebMarkupContainer(ID_ERROR_MESSAGE);
        errorContainer.setOutputMarkupId(true);
        errorContainer.add(new VisibleBehaviour(() -> getErrorMessageText() != null));

        errorContainer.add(new Label(ID_ERROR_TEXT, this::getErrorMessageText));

        add(errorContainer);
    }

    private void initWarningMessage() {
        WebMarkupContainer warningContainer = new WebMarkupContainer(ID_WARNING_MESSAGE);
        warningContainer.setOutputMarkupId(true);
        warningContainer.add(new VisibleBehaviour(() -> getDto().getWarningMessage() != null
                && getDto().getWarningMessage().getObject() != null));
        warningContainer.add(new Label(ID_WARNING_TEXT, getDto().getWarningMessage()));

        add(warningContainer);
    }

    private void initAiInfo() {
        WebMarkupContainer aiInfoContainer = new WebMarkupContainer(ID_INFO_DETAILS);
        aiInfoContainer.setOutputMarkupId(true);
        aiInfoContainer.add(new VisibleBehaviour(() -> getDto().hasAiInfo() && getDto().isAiAvailable()));

        aiInfoContainer.add(new Label(ID_AI_PROVIDER, () -> getDto().getAiProviderText()));
        aiInfoContainer.add(new Label(ID_AI_MODEL, () -> getDto().getAiModelText()));

        WebMarkupContainer aiStatusContainer = new WebMarkupContainer(ID_AI_STATUS_CONTAINER);
        aiStatusContainer.setOutputMarkupId(true);
        aiStatusContainer.add(AttributeModifier.append("class", () -> getDto().getAiStatusCss()));
        aiStatusContainer.add(new Label(ID_AI_STATUS, () -> getDto().getAiStatusText()));

        aiInfoContainer.add(aiStatusContainer);

        add(aiInfoContainer);
    }

    private void initOptions() {
        Form<?> form = new Form<>("form");
        form.setOutputMarkupId(true);
        add(form);

        WebMarkupContainer container = new WebMarkupContainer(ID_OPTIONS_CONTAINER);
        container.setOutputMarkupId(true);
        container.add(new VisibleBehaviour(() -> getDto().isOptionVisible() && numberOfOptions() > 0));
        form.add(container);

        container.add(new Label(ID_OPTION_LABEL, getModelObject().getConfirmationOptionsTitle()));

        ListView<ConfirmationOption<T>> listView =
                new ListView<>(ID_LIST_VIEW, this::getConfirmationOptions) {

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

    private @NotNull AjaxIconButton buildAction(ConfirmationOption<T> option) {
        AjaxIconButton action = new AjaxIconButton(
                ID_OPTION_ACTION,
                Model.of("fa fa-info-circle"),
                createStringResource("ConfirmationWithOptionsPanel.confirmationOptions.action.more.info")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                if (option.onClick() != null) {
                    option.onClick().accept(option, target);
                }
            }
        };

        action.showTitleAsLabel(true);
        action.add(new VisibleBehaviour(() -> option.onClick() != null));
        return action;
    }

    private ConfirmationWithOptionsDto<T> getDto() {
        return getModelObject();
    }

    private @NotNull List<ConfirmationOption<T>> getConfirmationOptions() {
        ConfirmationWithOptionsDto<T> dto = getDto();
        List<ConfirmationOption<T>> options = dto != null ? dto.getConfirmationOptions() : null;
        return options != null ? options : List.of();
    }

    private int numberOfOptions() {
        return getConfirmationOptions().size();
    }

    private @Nullable IModel<String> getConfirmationInfoMessage() {
        ConfirmationWithOptionsDto<T> dto = getDto();
        return dto != null ? dto.getConfirmationInfoMessage() : null;
    }

    private @Nullable String getErrorMessageText() {
        IModel<String> errorModel = getDto().getErrorMessage();
        return errorModel != null ? errorModel.getObject() : null;
    }

}

/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 *    This work is dual-licensed under the Apache License 2.0
 *    and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.dialog;

import java.io.Serial;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.result.MessagePanel;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.util.Describable;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

/**
 * Popup panel showing a confirmation message with optional confirmation options, info note, and "learn more" link.
 *
 * Most of the customizations to this class are made through its configuration model. There are only two methods,
 * which could be overridden. The {@link #confirmationPerformed(AjaxRequestTarget, IModel)} and
 * the {@link #cancelPerformed(AjaxRequestTarget)}. From these the most important is the first one, which allows you
 * to react when the user clicked on the confirmation button.
 *
 * If the panel was configured also with the confirmation options, these will be shown to user as checkboxes. User
 * can select them and upon click on the confirmation button the mentioned
 * {@link #confirmationPerformed(AjaxRequestTarget, IModel)} method is called and the selected options are passed to
 * it as parameter. **Only the selected options are passed**.
 */
public class ConfirmationWithOptionsPanel<T extends Describable> extends BasePanel<ConfirmationWithOptionsDto<T>>
        implements Popupable {
    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_BUTTONS = "buttons";
    protected static final String ID_YES = "yes";
    private static final String ID_NO = "no";

    private static final String ID_SUBTITLE = "subtitle";

    private static final String ID_LEARN_MORE = "learnMore";
    private static final String ID_INFO_MESSAGE = "infoMessage";

    private static final String ID_OPTIONS_CONTAINER = "requestContainer";
    private static final String ID_OPTION_LABEL = "requestLabel";
    private static final String ID_LIST_VIEW = "listView";
    private static final String ID_OPTION_CHECK = "check";
    private static final String ID_OPTION_TITLE = "title";
    private static final String ID_OPTION_DESCRIPTION = "description";
    private static final String ID_OPTION_ACTION = "action";

    Fragment footer;

    public ConfirmationWithOptionsPanel(String id, IModel<ConfirmationWithOptionsDto<T>> confirmationWithOptionsData) {
        super(id, confirmationWithOptionsData);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initFooter();

        Label subtitleLabel = createSubtitle();
        subtitleLabel.setOutputMarkupId(true);
        add(subtitleLabel);

        initInfoMessage();
        initConfirmationOptionsDetailsPart();
    }

    private void initFooter() {
        footer = new Fragment(Popupable.ID_FOOTER, ID_BUTTONS, this);
        createCancelButton(footer);
        createConfirmationButton(footer);
        initExternalLink(footer);
        add(footer);
    }

    @Override
    public @NotNull Component getFooter() {
        return footer;
    }

    private void createCancelButton(@NotNull Fragment footer) {
        AjaxButton cancelButton = new AjaxButton(ID_NO, getModelObject().getCancelButtonLabel()) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                getPageBase().hideMainPopup(target);
                cancelPerformed(target);
            }
        };
        cancelButton.add(AttributeAppender.append("class", getModelObject().getCancelButtonCssClass()));
        footer.add(cancelButton);
    }

    protected void cancelPerformed(AjaxRequestTarget target) {}

    private void createConfirmationButton(@NotNull Fragment footer) {
        final ConfirmationWithOptionsDto<T> panelConfig = getModelObject();
        final IModel<List<ConfirmationOption<T>>> confirmedOptions = () ->
                panelConfig.getConfirmationOptions().stream()
                        .filter(ConfirmationOption::isSelected)
                        .toList();
        AjaxButton confirmButton = new AjaxButton(ID_YES, panelConfig.getConfirmationButtonLabel()) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                getPageBase().hideMainPopup(target);
                confirmationPerformed(target, confirmedOptions);
            }
        };
        confirmButton.add(AttributeAppender.append("class", panelConfig.getConfirmationButtonCssClass()));
        confirmButton.add(new VisibleBehaviour(() -> panelConfig.getConfirmationButtonLabel() != null));
        footer.add(confirmButton);
    }

    protected void confirmationPerformed(AjaxRequestTarget target,
            IModel<List<ConfirmationOption<T>>> confirmedOptions) {}

    private void initInfoMessage() {
        final ConfirmationWithOptionsDto<T> panelConfig = getModelObject();
        MessagePanel<?> infoMessage = new MessagePanel<>(ID_INFO_MESSAGE, MessagePanel.MessagePanelType.INFO,
                panelConfig.getConfirmationInfoMessage(), false) {

            @Contract(pure = true)
            @Override
            protected @NotNull Object getIconTypeCss() {
                return "fa fa-info-circle";
            }

            @Contract(" -> new")
            @Override
            protected @NotNull IModel<String> createHeaderCss() {
                return Model.of("alert-info");
            }
        };
        infoMessage.setOutputMarkupId(true);
        infoMessage.add(new VisibleBehaviour(() -> panelConfig.getConfirmationInfoMessage() != null));
        add(infoMessage);
    }

    private void initExternalLink(@NotNull WebMarkupContainer panel) {
        final ConfirmationWithOptionsDto<T> panelConfig = getModelObject();
        final String externalLinkUrl = panelConfig.getExternalLinkUrl();
        ExternalLink learnMoreButton = new ExternalLink(ID_LEARN_MORE, () -> externalLinkUrl);
        learnMoreButton.add(AttributeModifier.append("target", "_blank"));
        learnMoreButton.setBody(panelConfig.getExternalLinkButtonLabel());
        learnMoreButton.add(new VisibleBehaviour(() -> externalLinkUrl != null && !externalLinkUrl.isBlank()));
        learnMoreButton.setOutputMarkupId(true);

        panel.add(learnMoreButton);
    }

    /**
     * Confirmation options details part contains a list of options with checkboxes and action button.
     */
    private void initConfirmationOptionsDetailsPart() {
        Form<?> form = new Form<>("form");
        form.setOutputMarkupId(true);
        add(form);

        WebMarkupContainer optionsContainer = new WebMarkupContainer(ID_OPTIONS_CONTAINER);
        optionsContainer.setOutputMarkupId(true);
        optionsContainer.add(new VisibleBehaviour(() -> numberOfOptions() > 0));
        form.add(optionsContainer);

        Label confirmationOptionsTitle = new Label(ID_OPTION_LABEL, getModelObject().getConfirmationOptionsTitle());
        confirmationOptionsTitle.setOutputMarkupId(true);
        optionsContainer.add(confirmationOptionsTitle);

        ListView<ConfirmationOption<T>> listView = new ListView<>(ID_LIST_VIEW,
                () -> getModelObject().getConfirmationOptions()) {
            @Override
            protected void populateItem(@NotNull ListItem<ConfirmationOption<T>> item) {
                ConfirmationOption<T> option = item.getModelObject();

                AjaxCheckBox checkBox = new AjaxCheckBox(ID_OPTION_CHECK, option.selected()){
                    @Override
                    protected void onUpdate(AjaxRequestTarget ajaxRequestTarget) {
                        // Nothing to do, checkbox model is updated automatically.
                    }
                };
                checkBox.setOutputMarkupId(true);
                item.add(checkBox);

                item.add(new Label(ID_OPTION_TITLE, option.title()));
                item.add(new Label(ID_OPTION_DESCRIPTION, option.description()));
                item.add(buildActionComponent(option));

                if (item.getIndex() < numberOfOptions() - 1) {
                    item.add(AttributeModifier.append("class", "border-bottom"));
                }
            }

            private @NotNull AjaxIconButton buildActionComponent(ConfirmationOption<T> record) {
                AjaxIconButton action = new AjaxIconButton(ID_OPTION_ACTION,
                        Model.of("fa fa-info-circle"),
                        createStringResource("ConfirmationWithOptionsPanel.confirmationOptions.action.more.info")) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        record.onClick().accept(record, target);
                    }
                };
                action.showTitleAsLabel(true);
                action.setOutputMarkupId(true);
                action.add(new VisibleBehaviour(() -> record.onClick() != null));
                return action;
            }
        };
        listView.setOutputMarkupId(true);
        optionsContainer.add(listView);
    }

    private int numberOfOptions() {
        final List<ConfirmationOption<T>> options = getModelObject().getConfirmationOptions();
        return options != null
                ? options.size()
                : 0;
    }

    private @NotNull Label createSubtitle() {
        Label label = new Label(ConfirmationWithOptionsPanel.ID_SUBTITLE, getModelObject().getConfirmationSubtitle());
        label.setOutputMarkupId(true);
        label.setOutputMarkupPlaceholderTag(true);
        return label;
    }

    @Override
    public IModel<String> getTitle() {
        return getModelObject().getConfirmationTitle();
    }

    @Override
    public IModel<String> getTitleIconClass() {
        return Model.of(GuiStyleConstants.CLASS_INFO_CIRCLE + " fa-xl "+ getModelObject().getTitleIconCssClass());
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
    }

    @Override
    public int getWidth() {
        return 40;
    }

    @Override
    public int getHeight() {
        return 30;
    }

    @Override
    public String getWidthUnit() {
        return "%";
    }

    @Override
    public String getHeightUnit() {
        return "%";
    }

    @Override
    public Component getContent() {
        return this;
    }

}

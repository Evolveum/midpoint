/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 *    This work is dual-licensed under the Apache License 2.0
 *    and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.dialog;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.result.MessagePanel;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

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
import org.apache.wicket.model.StringResourceModel;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.List;

/**
 * Popup panel showing a confirmation message with optional request, info note, and "learn more" link for smart suggestions.
 */
public class RequestDetailsConfirmationPanel extends BasePanel<RequestDetailsRecordDto> implements Popupable {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_BUTTONS = "buttons";
    protected static final String ID_YES = "yes";
    private static final String ID_NO = "no";

    private static final String ID_SUBTITLE = "subtitle";

    private static final String ID_LEARN_MORE = "learnMore";
    private static final String ID_INFO_MESSAGE = "infoMessage";

    private static final String ID_REQUEST_CONTAINER = "requestContainer";
    private static final String ID_REQUEST_LABEL = "requestLabel";
    private static final String ID_LIST_VIEW = "listView";
    private static final String ID_REQUEST_CHECK = "check";
    private static final String ID_REQUEST_TITLE = "title";
    private static final String ID_REQUEST_DESCRIPTION = "description";
    private static final String ID_REQUEST_ACTION = "action";

    Fragment footer;

    public RequestDetailsConfirmationPanel(String id, IModel<RequestDetailsRecordDto> message) {
        super(id, message);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initFooter();

        Label subtitleLabel = createLabelComponent(getSubtitleModel());
        subtitleLabel.setOutputMarkupId(true);
        add(subtitleLabel);

        initInfoMessage();
        initRequestDetailsPart();
    }

    private void initFooter() {
        footer = new Fragment(Popupable.ID_FOOTER, ID_BUTTONS, this);
        createNoButton(footer);
        createYesButton(footer);
        initLearnMoreLink(footer);
        add(footer);
    }

    @Override
    public @NotNull Component getFooter() {
        return footer;
    }

    private void createNoButton(@NotNull Fragment footer) {
        AjaxButton noButton = new AjaxButton(ID_NO, createNoLabel()) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                noPerformed(target);
            }
        };
        noButton.add(AttributeAppender.append("class", getNoButtonCssClass()));
        footer.add(noButton);
    }

    public void noPerformed(AjaxRequestTarget target) {
        getPageBase().hideMainPopup(target);
    }

    protected void createYesButton(@NotNull Fragment footer) {
        AjaxButton yesButton = new AjaxButton(ID_YES, createYesLabel()) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                getPageBase().hideMainPopup(target);
                yesPerformed(target);
            }
        };
        yesButton.add(AttributeAppender.append("class", getYesButtonCssClass()));
        yesButton.add(new VisibleBehaviour(this::isYesButtonVisible));
        footer.add(yesButton);
    }

    public void yesPerformed(AjaxRequestTarget target) {
    }

    protected boolean isYesButtonVisible() {
        return true;
    }

    protected String getYesButtonCssClass() {
        return "btn btn-primary";
    }

    protected String getNoButtonCssClass() {
        return "btn btn-default";
    }

    private void initInfoMessage() {
        MessagePanel<?> infoMessage = new MessagePanel<>(ID_INFO_MESSAGE, MessagePanel.MessagePanelType.INFO,
                getInfoMessageModel(), false) {

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
        infoMessage.add(new VisibleBehaviour(() -> getInfoMessageModel() != null));
        add(infoMessage);
    }

    private void initLearnMoreLink(@NotNull WebMarkupContainer panel) {
        ExternalLink learnMoreButton = new ExternalLink(ID_LEARN_MORE,
                getUrlLink());
        learnMoreButton.add(AttributeModifier.append("target", "_blank"));
        learnMoreButton.setBody(getLearnMoreButtonModel());
        learnMoreButton.add(new VisibleBehaviour(this::isLearnMoreVisible));
        learnMoreButton.setOutputMarkupId(true);

        panel.add(learnMoreButton);
    }

    /**
     * Request details part contains a list of requests with checkboxes and action button.
     */
    private void initRequestDetailsPart() {
        Form<?> form = new Form<>("form");
        form.setOutputMarkupId(true);
        add(form);

        WebMarkupContainer requestContainer = new WebMarkupContainer(ID_REQUEST_CONTAINER);
        requestContainer.setOutputMarkupId(true);
        requestContainer.add(new VisibleBehaviour(this::isRequestPartVisible));
        form.add(requestContainer);

        Label requestLabel = new Label(ID_REQUEST_LABEL, getModelObject().getRequestLabelModel(getPageBase()));
        requestLabel.setOutputMarkupId(true);
        requestContainer.add(requestLabel);

        ListView<RequestDetailsRecordDto.RequestRecord> listView = new ListView<>(ID_LIST_VIEW,
                () -> getModelObject().getRecords()) {
            @Override
            protected void populateItem(@NotNull ListItem<RequestDetailsRecordDto.RequestRecord> item) {
                RequestDetailsRecordDto.RequestRecord record = item.getModelObject();

                AjaxCheckBox checkBox = new AjaxCheckBox(ID_REQUEST_CHECK, record.selected()) {
                    @Override
                    protected void onUpdate(AjaxRequestTarget ajaxRequestTarget) {
                        record.setSelected(getModelObject());
                    }
                };
                checkBox.setOutputMarkupId(true);
                item.add(checkBox);

                item.add(new Label(ID_REQUEST_TITLE, record.title()));
                item.add(new Label(ID_REQUEST_DESCRIPTION, record.description()));
                item.add(buildActionComponent(record));

                if (item.getIndex() < getRequests().size() - 1) {
                    item.add(AttributeModifier.append("class", "border-bottom"));
                }
            }

            private @NotNull AjaxIconButton buildActionComponent(RequestDetailsRecordDto.RequestRecord record) {
                AjaxIconButton action = new AjaxIconButton(ID_REQUEST_ACTION,
                        Model.of("fa fa-info-circle"),
                        createStringResource("SmartSuggestConfirmationPanel.request.record.action.more.info")) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        //TODO
                    }
                };
                action.showTitleAsLabel(true);
                action.setOutputMarkupId(true);
                action.add(new VisibleBehaviour(() -> record.onClick() != null));
                return action;
            }
        };
        listView.setOutputMarkupId(true);
        requestContainer.add(listView);
    }

    protected List<RequestDetailsRecordDto.RequestRecord> getRequests() {
        return getModelObject() != null && getModelObject().getRecords() != null
                ? getModelObject().getRecords()
                : List.of();
    }

    protected IModel<String> getConfirmationMessage() {
        return getModelObject() != null && getModelObject().getConfirmationMessage() != null
                ? getModelObject().getConfirmationMessage()
                : createStringResource("SmartSuggestConfirmationPanel.title");
    }

    protected boolean isRequestPartVisible() {
        List<RequestDetailsRecordDto.RequestRecord> requests = getRequests();
        return requests != null && !requests.isEmpty();
    }

    protected IModel<String> createYesLabel() {
        return getAllowAndContinueModel();
    }

    protected IModel<String> createNoLabel() {
        return getCancelButtonModel();
    }

    /**
     * Creates a label component with common output markup settings.
     *
     * @param title The label model (typically a localized string resource)
     * @return Configured {@link Label} instance
     */
    private @NotNull Label createLabelComponent(StringResourceModel title) {
        Label label = new Label(RequestDetailsConfirmationPanel.ID_SUBTITLE, title);
        label.setOutputMarkupId(true);
        label.setOutputMarkupPlaceholderTag(true);
        return label;
    }

    @Override
    public IModel<String> getTitle() {
        return getConfirmationMessage();
    }

    @Override
    public IModel<String> getTitleIconClass() {
        return Model.of(GuiStyleConstants.CLASS_INFO_CIRCLE + " fa-xl "+ getTitleIconAdditionalCssClass());
    }

    protected String getTitleIconAdditionalCssClass() {
        return "text-info";
    }

    protected StringResourceModel getSubtitleModel() {
        StringResourceModel subtitleModel = getModelObject().getSubtitleModel(getPageBase());
        return subtitleModel != null
                ? subtitleModel
                : createStringResource("");
    }

    protected StringResourceModel getInfoMessageModel() {
        return createStringResource("SmartSuggestConfirmationPanel.infoMessage", this, null);
    }

    private StringResourceModel getLearnMoreButtonModel() {
        return createStringResource("SmartSuggestConfirmationPanel.learnMore", this, null);
    }

    protected boolean isLearnMoreVisible() {
        return getLearnMoreButtonModel() != null;
    }

    private StringResourceModel getCancelButtonModel() {
        return createStringResource("SmartSuggestConfirmationPanel.cancel", this, null);
    }

    private StringResourceModel getAllowAndContinueModel() {
        return createStringResource("SmartSuggestConfirmationPanel.allowAndContinue", this, null);
    }

    protected String getUrlLink() {
        return "https://docs.evolveum.com/";
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

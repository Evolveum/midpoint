/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 *    This work is dual-licensed under the Apache License 2.0
 *    and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.result.MessagePanel;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;

public class SmartSuggestConfirmationPanel extends ConfirmationPanel {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_SUBTITLE = "subtitle";

    private static final String ID_LEARN_MORE = "learnMore";
    private static final String ID_INFO_MESSAGE = "infoMessage";

    public SmartSuggestConfirmationPanel(String id, IModel<String> message) {
        super(id, message);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        Label subtitleLabel = createLabelComponent(getSubtitleModel());
        subtitleLabel.setOutputMarkupId(true);
        add(subtitleLabel);

        initInfoMessage();
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

    @Override
    protected void customInitLayout(WebMarkupContainer panel) {
        initLearnMoreLink(panel);
    }

    private void initLearnMoreLink(@NotNull WebMarkupContainer panel) {
        ExternalLink learnMoreButton = new ExternalLink(ID_LEARN_MORE,
                getUrlLink());
        learnMoreButton.add(AttributeModifier.append("target", "_blank"));
        learnMoreButton.setBody(getLearnMoreButtonModel());
        learnMoreButton.setOutputMarkupId(true);

        panel.add(learnMoreButton);
    }

    @Override
    protected IModel<String> createYesLabel() {
        return getAllowAndContinueModel();
    }

    @Override
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
        Label label = new Label(SmartSuggestConfirmationPanel.ID_SUBTITLE, title);
        label.setOutputMarkupId(true);
        label.setOutputMarkupPlaceholderTag(true);
        return label;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("SmartSuggestConfirmationPanel.title", this, null);
    }

    @Override
    public @Nullable Component getTitleComponent() {
        Label titleComponent = new Label(ID_TITLE, getTitle());
        titleComponent.setOutputMarkupId(true);
        titleComponent.add(AttributeModifier.append("class", "align-self-center"));
        return titleComponent;
    }

    @Override
    public IModel<String> getTitleIconClass() {
        return Model.of(GuiStyleConstants.CLASS_INFO_CIRCLE + " text-info fa-xl");
    }

    private StringResourceModel getSubtitleModel() {
        return createStringResource("SmartSuggestConfirmationPanel.subtitle", this, null);
    }

    private StringResourceModel getInfoMessageModel() {
        return createStringResource("SmartSuggestConfirmationPanel.infoMessage", this, null);
    }

    private StringResourceModel getLearnMoreButtonModel() {
        return createStringResource("SmartSuggestConfirmationPanel.learnMore", this, null);
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

}

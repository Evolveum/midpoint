/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component.dialog;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.util.Describable;

import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.link.ExternalLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;
import java.util.List;

public class ConfirmationWithOptionsPopupPanel<T extends Describable>
        extends BasePanel<ConfirmationWithOptionsDto<T>>
        implements Popupable {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_CONTENT = "content";
    private static final String ID_BUTTONS = "buttons";
    private static final String ID_LEARN_MORE = "learnMore";
    private static final String ID_NO = "no";
    private static final String ID_YES = "yes";

    private Fragment footer;

    public ConfirmationWithOptionsPopupPanel(
            String id,
            IModel<ConfirmationWithOptionsDto<T>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        ConfirmationWithOptionsContentPanel<T> content = new ConfirmationWithOptionsContentPanel<>(ID_CONTENT, getModel());
        add(content);

        initFooter();
    }

    private void initFooter() {
        footer = new Fragment(Popupable.ID_FOOTER, ID_BUTTONS, this);

        initLearnMore(footer);
        initCancel(footer);
        createConfirmationButton(footer);

        add(footer);
    }

    private void initLearnMore(WebMarkupContainer footer) {
        String url = getModelObject().getExternalLinkUrl();

        ExternalLink link = new ExternalLink(ID_LEARN_MORE, () -> url);
        link.add(AttributeModifier.append("target", "_blank"));
        link.setBody(getModelObject().getExternalLinkButtonLabel());
        link.add(new VisibleBehaviour(() -> url != null && !url.isBlank()));

        footer.add(link);
    }

    private void initCancel(WebMarkupContainer footer) {
        AjaxButton cancel = new AjaxButton(ID_NO, getModelObject().getCancelButtonLabel()) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                getPageBase().hideMainPopup(target);
                cancelPerformed(target);
            }
        };

        cancel.add(AttributeAppender.append("class", getModelObject().getCancelButtonCssClass()));
        footer.add(cancel);
    }

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

    protected void cancelPerformed(AjaxRequestTarget target) {
    }

    protected void confirmationPerformed(
            AjaxRequestTarget target,
            IModel<List<ConfirmationOption<T>>> confirmedOptions) {
    }

    @Override
    public IModel<String> getTitle() {
        return getModelObject().getConfirmationTitle();
    }

    @Override
    public IModel<String> getTitleIconClass() {
        return Model.of(GuiStyleConstants.CLASS_INFO_CIRCLE + " fa-xl "
                + getModelObject().getTitleIconCssClass());
    }

    @Override
    public @NotNull Component getFooter() {
        return footer;
    }

    @Override
    public Component getContent() {
        return this;
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

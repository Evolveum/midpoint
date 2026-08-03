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

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
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
    private static final String ID_NO = "no";
    private static final String ID_YES = "yes";
    private static final String ID_REFRESH = "refresh";

    private Fragment footer;

    public ConfirmationWithOptionsPopupPanel(
            String id,
            IModel<ConfirmationWithOptionsDto<T>> model) {
        super(id, model);
        setOutputMarkupId(true);
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
        footer.setOutputMarkupId(true);

        initCancel(footer);
        createConfirmationButton(footer);
        createRefreshButton(footer);

        add(footer);
    }

    private void initCancel(@NotNull WebMarkupContainer footer) {
        AjaxButton cancel = new AjaxButton(ID_NO, () ->
                getPanelConfig().getCancelButtonLabel().getObject()) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                getPageBase().hideMainPopup(target);
                cancelPerformed(target);
            }
        };

        cancel.add(AttributeAppender.append("class", () ->
                getPanelConfig().getCancelButtonCssClass()));

        footer.add(cancel);
    }

    private ConfirmationWithOptionsDto<T> getPanelConfig() {
        return getModelObject();
    }

    private void createConfirmationButton(@NotNull Fragment footer) {
        final IModel<List<ConfirmationOption<T>>> confirmedOptions = () ->
                getModelObject().getConfirmationOptions().stream()
                        .filter(ConfirmationOption::isSelected)
                        .toList();
        AjaxButton confirmButton = new AjaxButton(ID_YES, getPanelConfig().getConfirmationButtonLabel()) {
            @Override
            public void onClick(AjaxRequestTarget target) {

                if (getPanelConfig().hasAiInfo() && !getPanelConfig().isAiServiceAvailable()) {
                    // Clear any previously selected permissions so they are not submitted.
                    confirmedOptions.getObject().forEach(option -> option.setSelected(false));
                }

                getPageBase().hideMainPopup(target);
                confirmationPerformed(target, confirmedOptions);
            }
        };

        confirmButton.add(AttributeAppender.append("class", getPanelConfig().getConfirmationButtonCssClass()));
        confirmButton.add(new VisibleBehaviour(() -> !getPanelConfig().hasError()));
        footer.add(confirmButton);
    }

    private void createRefreshButton(@NotNull Fragment footer) {
        AjaxButton refreshButton = new AjaxButton(ID_REFRESH,
                createStringResource("ConfirmationWithOptionsPopupPanel.refresh")) {
            @Override
            public void onClick(@NotNull AjaxRequestTarget target) {
                getPanelConfig().getAiInfo().detach();
                target.add(footer);
                target.add(ConfirmationWithOptionsPopupPanel.this);
                MainPopupDialog mainPopup = getPageBase().getMainPopup();
                mainPopup.refreshHeader(target);
            }
        };

        refreshButton.add(new VisibleBehaviour(() -> getPanelConfig().hasAiInfo() && !getPanelConfig().isAiServiceAvailable()));
        footer.add(refreshButton);
    }

    protected void cancelPerformed(AjaxRequestTarget target) {
    }

    protected void confirmationPerformed(
            AjaxRequestTarget target,
            IModel<List<ConfirmationOption<T>>> confirmedOptions) {
    }

    @Override
    public IModel<String> getTitle() {
        return () -> {
            ConfirmationWithOptionsDto<T> config = getPanelConfig();

            if (config.hasAiInfo() && !config.isAiServiceAvailable()) {
                return createStringResource("ConfirmationWithOptionsDto.aiServiceUnavailable").getString();
            }

            IModel<String> title = config.getConfirmationTitle();
            return title != null ? title.getObject() : null;
        };
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

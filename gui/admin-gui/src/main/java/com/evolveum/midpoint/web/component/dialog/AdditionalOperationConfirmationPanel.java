/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 *    This work is dual-licensed under the Apache License 2.0
 *    and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.dialog;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.result.MessagePanel;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.jetbrains.annotations.NotNull;

import java.io.Serial;

public abstract class AdditionalOperationConfirmationPanel extends ConfirmationPanel {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_PROCESS = "process";
    private static final String ID_WARNING_MESSAGE = "warningMessage";

    private final IModel<String> warningMessageModel;

    public AdditionalOperationConfirmationPanel(String id) {
        this(id, null);
    }

    public AdditionalOperationConfirmationPanel(String id, IModel<String> title) {
        super(id, title);
        warningMessageModel = createWarningMessageModel();
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        MessagePanel<?> warningMessage = new MessagePanel<>(
                ID_WARNING_MESSAGE,
                MessagePanel.MessagePanelType.WARN,
                warningMessageModel);

        warningMessage.setOutputMarkupId(true);
        warningMessage.add(new VisibleBehaviour(() -> warningMessageModel != null));
        add(warningMessage);
    }

    @Override
    protected void customInitLayout(@NotNull WebMarkupContainer panel) {
        AjaxButton configuredButton = new AjaxButton(ID_PROCESS, getProcessButtonLabel()) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                performOnProcess(target);
            }
        };
        configuredButton.setOutputMarkupId(true);
        configuredButton.add(new VisibleBehaviour((this::isPerformButtonVisible)));
        panel.add(configuredButton);
    }

    protected IModel<String> getProcessButtonLabel() {
        return createStringResource("AdditionalOperationConfirmationPanel.button.process");
    }

    @Override
    protected IModel<String> createYesLabel() {
        return createStringResource("AdditionalOperationConfirmationPanel.button.yes");
    }

    @Override
    protected String getYesButtonCssClass() {
        return null;
    }

    @Override
    protected IModel<String> createNoLabel() {
        return createStringResource("AdditionalOperationConfirmationPanel.button.no");
    }

    @Override
    protected String getNoButtonCssClass() {
        return null;
    }

    protected abstract void performOnProcess(AjaxRequestTarget target);

    protected PrismObject<TaskType> createTask(AjaxRequestTarget target) {
        return null;
    }

    protected IModel<String> createWarningMessageModel() {
        return null;
    }

    public boolean isPerformButtonVisible() {
        return true;
    }
}

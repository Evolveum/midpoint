/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 *    This work is dual-licensed under the Apache License 2.0
 *    and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.dialog;

import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.component.result.MessagePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 * @author lskublik
 */
public class ConfigureTaskConfirmationPanel extends ConfirmationPanel {

    private static final long serialVersionUID = 1L;
    private static final String ID_CONFIGURE = "configure";

    public ConfigureTaskConfirmationPanel(String id) {
        this(id, null);

    }

    private final IModel<String> warningMessageModel;

    public ConfigureTaskConfirmationPanel(String id, IModel<String> message) {
        super(id, message);
        warningMessageModel = createWarningMessageModel();
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        MessagePanel<?> warningMessage = new MessagePanel<>("warnningMessage", MessagePanel.MessagePanelType.WARN, warningMessageModel);
        warningMessage.setOutputMarkupId(true);
        warningMessage.add(new VisibleBehaviour(() -> warningMessageModel != null));
        add(warningMessage);
    }

    @Override
    protected void customInitLayout(WebMarkupContainer panel) {
        AjaxButton configuredButton = new AjaxButton(ID_CONFIGURE,
                new StringResourceModel("ConfigureTaskConfirmationPanel.configure", this, null)) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                ((PageBase) getPage()).hideMainPopup(target);
                DetailsPageUtil.dispatchToObjectDetailsPage(createTask(target), true, ConfigureTaskConfirmationPanel.this);
            }
        };
        configuredButton.setOutputMarkupId(true);
        configuredButton.add(new VisibleBehaviour((this::isConfigurationTaskVisible)));
        panel.add(configuredButton);
    }

    protected PrismObject<TaskType> createTask(AjaxRequestTarget target) {
        return null;
    }

    protected IModel<String> createWarningMessageModel() {
        return null;
    }

    public boolean isConfigurationTaskVisible() {
        return true;
    }
}

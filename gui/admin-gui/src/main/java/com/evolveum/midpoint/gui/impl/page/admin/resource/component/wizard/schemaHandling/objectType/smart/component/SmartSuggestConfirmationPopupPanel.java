/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.component;

import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.smart.api.info.AiInfo;
import com.evolveum.midpoint.web.component.dialog.ConfirmationOption;
import com.evolveum.midpoint.web.component.dialog.ConfirmationWithOptionsContentPanel;
import com.evolveum.midpoint.web.component.dialog.ConfirmationWithOptionsPopupPanel;
import com.evolveum.midpoint.web.component.input.ButtonWithConfirmationOptionsDialog.ButtonHandlers;
import com.evolveum.midpoint.web.component.util.Describable;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

public class SmartSuggestConfirmationPopupPanel<T extends Describable>
        extends ConfirmationWithOptionsPopupPanel<T> {
    private final SmartSuggestConfirmationDto<T> config;

    public SmartSuggestConfirmationPopupPanel(String id, SmartSuggestConfirmationDto<T> config) {
        super(id, Model.of(config.confirmation()));
        this.config = config;
    }

    @Override
    protected void onInitialize() {
        config.detach();
        super.onInitialize();
    }

    @Override
    protected Component createContentPanel(String id) {
        return new ConfirmationWithOptionsContentPanel<>(id, getModel()) {
            @Override
            protected boolean isSubtitleVisible() {
                return config.isAiAvailable() && super.isSubtitleVisible();
            }

            @Override
            protected boolean isOptionsVisible() {
                return config.aiInfo() == null || config.isAiAvailable();
            }

            @Override
            protected Component createAdditionalContent(String contentId) {
                return createAiInfoFragment(contentId);
            }
        };
    }

    private Fragment createAiInfoFragment(String id) {
        Fragment fragment = new Fragment(id, "aiInfo", this);
        fragment.add(new VisibleBehaviour(config::isAiAvailable));
        fragment.add(new Label("aiProvider", () -> {
            AiInfo info = config.aiInfo() != null ? config.aiInfo().getObject() : null;
            return info != null ? info.provider() : "";
        }));
        fragment.add(new Label("aiModel", () -> {
            AiInfo info = config.aiInfo() != null ? config.aiInfo().getObject() : null;
            return info != null ? info.model() : "";
        }));
        WebMarkupContainer status = new WebMarkupContainer("aiStatusContainer");
        status.add(AttributeModifier.append("class", "text-success"));
        status.add(new Label("aiStatus", createStringResource(
                "ConfirmationWithOptionsPanel.confirmationOptions.ai.status.ok")));
        fragment.add(status);
        return fragment;
    }

    @Override
    protected Component createAdditionalFooterControl(String id) {
        AjaxButton refresh = new AjaxButton(id,
                createStringResource("ConfirmationWithOptionsPopupPanel.refresh")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                config.detach();
                target.add(SmartSuggestConfirmationPopupPanel.this);
                target.add(getFooter());
                getPageBase().getMainPopup().refreshHeader(target);
            }
        };
        refresh.add(new VisibleBehaviour(() -> config.aiInfo() != null && !config.isAiAvailable()));
        return refresh;
    }

    @Override
    protected void prepareConfirmation(IModel<List<ConfirmationOption<T>>> confirmedOptions) {
        if (config.aiInfo() != null && !config.isAiAvailable()) {
            confirmedOptions.getObject().forEach(option -> option.setSelected(false));
        }
    }

    @Override
    public IModel<String> getTitle() {
        return () -> config.aiInfo() != null && !config.isAiAvailable()
                ? createStringResource("ConfirmationWithOptionsDto.aiServiceUnavailable").getString()
                : super.getTitle().getObject();
    }

    @Override
    protected void onDetach() {
        config.detach();
        super.onDetach();
    }

    public static <T extends Describable> SmartSuggestConfirmationPopupPanel<T> create(
            String id, SmartSuggestConfirmationDto<T> config, ButtonHandlers<T> handlers) {
        return new SmartSuggestConfirmationPopupPanel<>(id, config) {
            @Override
            protected void confirmationPerformed(AjaxRequestTarget target,
                    IModel<List<ConfirmationOption<T>>> confirmedOptions) {
                handlers.confirmHandler().accept(target, confirmedOptions);
            }

            @Override
            protected void cancelPerformed(AjaxRequestTarget target) {
                handlers.cancelHandler().accept(target);
            }
        };
    }
}

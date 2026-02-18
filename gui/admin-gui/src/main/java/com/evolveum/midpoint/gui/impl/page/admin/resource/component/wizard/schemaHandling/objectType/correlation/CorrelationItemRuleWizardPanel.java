/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.correlation;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;

import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractResourceWizardBasicPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.util.GuiDisplayNameUtil;

/**
 * @author lskublik
 */

@PanelType(name = "rw-correlators")
@PanelInstance(identifier = "rw-correlators",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "CorrelationItemRefsTableWizardPanel.headerLabel", icon = "fa fa-bars-progress"))
public class CorrelationItemRuleWizardPanel extends AbstractResourceWizardBasicPanel<ItemsSubCorrelatorType> {

    private static final String PANEL_TYPE = "rw-correlators";

    private static final String ID_PANEL = "panel";

    IModel<StatusInfo<CorrelationSuggestionsType>> statusInfoModel;
    IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> resourceObjectTypeDefinition;

    public CorrelationItemRuleWizardPanel(
            String id,
            @NotNull IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> resourceObjectTypeDefinition,
            WizardPanelHelper<ItemsSubCorrelatorType, ResourceDetailsModel> superHelper,
            IModel<StatusInfo<CorrelationSuggestionsType>> statusInfoModel) {
        super(id, superHelper);
        this.statusInfoModel = statusInfoModel;
        this.resourceObjectTypeDefinition = resourceObjectTypeDefinition;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        CorrelationItemRulePanel panel =
                new CorrelationItemRulePanel(ID_PANEL, getValueModel(), statusInfoModel,
                        resourceObjectTypeDefinition) {
                    @Override
                    protected boolean isShowEmptyField() {
                        return CorrelationItemRuleWizardPanel.this.isShowEmptyField();
                    }

                    @Override
                    protected ContainerPanelConfigurationType getConfiguration() {
                        return CorrelationItemRuleWizardPanel.this.getConfiguration();
                    }
                };
        panel.setOutputMarkupId(true);
        add(panel);
    }

    @Override
    public boolean isEnabledInHierarchy() {
        return super.isEnabledInHierarchy();
    }

    @Override
    protected void onSubmitPerformed(AjaxRequestTarget target) {
        if (isSuggestionApplied()) {
            acceptSuggestionPerformed(target, getValueModel());
            return;
        }

        onExitPerformed(target);
    }

    protected boolean isShowEmptyField() {
        return false;
    }

    protected void acceptSuggestionPerformed(
            @NotNull AjaxRequestTarget target,
            @NotNull IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> valueModel) {
    }

    @Override
    protected IModel<String> getSubmitLabelModel() {
        return isSuggestionApplied()
                ? getPageBase().createStringResource("CorrelationItemRefsTableWizardPanel.accept")
                : getPageBase().createStringResource("CorrelationItemRefsTableWizardPanel.confirm");
    }

    @Override
    protected @NotNull IModel<String> getBreadcrumbLabel() {
        String name = GuiDisplayNameUtil.getDisplayName(getValueModel().getObject().getRealValue());
        if (StringUtils.isNotBlank(name)) {
            return Model.of(name);
        }
        return getPageBase().createStringResource("CorrelationItemRefsTableWizardPanel.breadcrumb");
    }

    @Override
    protected IModel<String> getTextModel() {
        return getPageBase().createStringResource("CorrelationItemRefsTableWizardPanel.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return getPageBase().createStringResource("CorrelationItemRefsTableWizardPanel.subText");
    }

    @Override
    protected boolean isBackButtonVisible() {
        return isSuggestionApplied();
    }

    @Override
    protected IModel<String> getBackLabel() {
        return getPageBase().createStringResource("CorrelationItemRefsTableWizardPanel.back");
    }

    @Override
    protected boolean isExitButtonVisible() {
        return false;
    }

    protected String getPanelType() {
        return PANEL_TYPE;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    protected StatusInfo<CorrelationSuggestionsType> getStatusInfo() {
        return statusInfoModel.getObject();
    }

    protected boolean isSuggestionApplied() {
        return getStatusInfo() != null;
    }

    @Override
    protected void addCustomButtons(@NotNull RepeatingView buttons) {
        AjaxIconButton discardButton = new AjaxIconButton(
                buttons.newChildId(),
                Model.of("fa fa-trash"),
                Model.of("Discard")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                onDiscardButtonClick(getPageBase(), target, getValueModel(), getStatusInfo());
                onExitPerformed(target);
            }
        };
        discardButton.showTitleAsLabel(true);
        discardButton.add(new VisibleBehaviour(this::isDiscardButtonVisible));
        discardButton.add(AttributeAppender.append("class", "btn-link text-danger"));
        buttons.add(discardButton);
    }

    protected boolean isDiscardButtonVisible() {
        return isSuggestionApplied();
    }

    protected void onDiscardButtonClick(
            @NotNull PageBase pageBase,
            @NotNull AjaxRequestTarget target,
            @NotNull IModel<PrismContainerValueWrapper<ItemsSubCorrelatorType>> valueModel,
            @NotNull StatusInfo<CorrelationSuggestionsType> statusInfo) {
    }

}

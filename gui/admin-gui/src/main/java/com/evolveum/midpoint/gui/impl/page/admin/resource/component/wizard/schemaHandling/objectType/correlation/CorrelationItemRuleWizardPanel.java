/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.correlation;

import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;

import com.evolveum.midpoint.web.component.prism.ValueStatus;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractResourceWizardBasicPanel;
import com.evolveum.midpoint.gui.impl.util.GuiDisplayNameUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.smart.api.info.StatusInfo;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author lskublik
 */

@PanelType(name = "rw-correlators")
@PanelInstance(identifier = "rw-correlators",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "CorrelationItemRefsTableWizardPanel.headerLabel", icon = "fa fa-bars-progress"))
public class CorrelationItemRuleWizardPanel<C extends Containerable> extends AbstractResourceWizardBasicPanel<ItemsSubCorrelatorType> {

    private static final String PANEL_TYPE = "rw-correlators";

    private static final String ID_PANEL = "panel";

    IModel<StatusInfo<CorrelationSuggestionsType>> statusInfoModel;
    IModel<PrismContainerValueWrapper<C>> parentContainerDefWrapper;

    public CorrelationItemRuleWizardPanel(
            String id,
            @NotNull IModel<PrismContainerValueWrapper<C>> parentContainerDefWrapper,
            WizardPanelHelper<ItemsSubCorrelatorType, ResourceDetailsModel> superHelper,
            IModel<StatusInfo<CorrelationSuggestionsType>> statusInfoModel) {
        super(id, superHelper);
        this.statusInfoModel = statusInfoModel;
        this.parentContainerDefWrapper = parentContainerDefWrapper;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        CorrelationItemRulePanel<?> panel =
                new CorrelationItemRulePanel<>(ID_PANEL, getValueModel(), statusInfoModel,
                        parentContainerDefWrapper) {
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
            if (isValid(target)) {
                acceptSuggestionPerformed(target, getValueModel());
            }
            return;
        }

        onExitPerformed(target);
    }

    @Override
    protected boolean isValid(AjaxRequestTarget target) {
        if (!super.isValid(target)) {
            return false;
        }
        boolean valid = ((CorrelationItemRulePanel<?>) get(ID_PANEL)).validateCorrelationItems(target);
        if (!valid) {
            target.add(getFeedback());
        }
        return valid;
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
    protected void onBackPerformed(AjaxRequestTarget target) {
        onExitPerformed(target);
    }

    @Override
    protected void onExitPerformedAfterValidate(AjaxRequestTarget target) {
        WebPrismUtil.removeEmptyAddedValue(getValueModel().getObject());
        removeLastBreadcrumb();
        super.onExitPerformedAfterValidate(target);
    }

    @Override
    protected IModel<String> getBackLabel() {
        return getPageBase().createStringResource("CorrelationItemRefsTableWizardPanel.back");
    }

    @Override
    protected boolean isExitButtonVisible() {
        return false;
    }

    @Override
    protected boolean isBackButtonVisible() {
        if (isSuggestionApplied()) {
            return true;
        }
        return super.isBackButtonVisible();
    }

    @Override
    protected String getButtonContainerAdditionalCssClass() {
        return "";
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
        discardButton.add(AttributeAppender.append("class", "btn btn-outline-danger"));
        buttons.add(discardButton);

        AjaxIconButton deleteButton = createDeleteButton(buttons);
        deleteButton.add(new VisibleBehaviour(() -> !isSuggestionApplied()));
        buttons.add(deleteButton);

    }

    private AjaxIconButton createDeleteButton(RepeatingView buttons) {
        AjaxIconButton deleteButton = new AjaxIconButton(
                buttons.newChildId(),
                Model.of("fa fa-trash"),
                createStringResource("CorrelationItemRuleWizardPanel.delete")) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                ConfirmationPanel confirmation = new ConfirmationPanel(
                        getPageBase().getMainPopupBodyId(),
                        createStringResource("CorrelationItemRuleWizardPanel.deleteConfirmation")) {
                    @Override
                    public void yesPerformed(AjaxRequestTarget target) {
                        deleteRulePerformed(target);
                    }
                };
                getPageBase().showMainPopup(confirmation, target);
            }
        };
        deleteButton.showTitleAsLabel(true);
        deleteButton.add(AttributeAppender.replace("class", "btn btn-outline-danger"));
        return deleteButton;
    }

    private void deleteRulePerformed(AjaxRequestTarget target) {
        PrismContainerValueWrapper<ItemsSubCorrelatorType> value = getValueModel().getObject();
        if (value.getStatus() == ValueStatus.ADDED) {
            PrismContainerWrapper<ItemsSubCorrelatorType> parent = value.getParent();
            parent.getValues().remove(value);
        } else {
            value.setStatus(ValueStatus.DELETED);
        }
        value.setSelected(false);
        onExitPerformedAfterValidate(target);
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

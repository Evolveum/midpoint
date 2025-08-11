/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardBasicPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.SchemaHandlingObjectsPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * @author lskublik
 */
public abstract class SchemaHandlingTypesTableWizardPanel<C extends Containerable> extends AbstractWizardBasicPanel<ResourceDetailsModel> {

    private static final Trace LOGGER = TraceManager.getTrace(SchemaHandlingTypesTableWizardPanel.class);

    private static final String ID_CARD_HEADER = "cardHeader";
    private static final String ID_PANEL = "panel";

    public SchemaHandlingTypesTableWizardPanel(String id, ResourceDetailsModel model) {
        super(id, model);
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        applyShowAsCard();
    }

    private void applyShowAsCard() {
        if (getPanel() != null) {
            getPanel().getTable().setShowAsCard(false);
        }
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        WebMarkupContainer cardHeader = new WebMarkupContainer(ID_CARD_HEADER);
        cardHeader.setOutputMarkupId(true);
        cardHeader.add(new VisibleBehaviour(() -> getPanel() != null && !getPanel().displayNoValuePanel()));
        add(cardHeader);

        initPanel(ID_PANEL);

    }

    protected abstract void initPanel(String panelId);

    protected final void onNewValue(
            PrismContainerValue<C> value,
            IModel<PrismContainerWrapper<C>> containerModel,
            WrapperContext context,
            AjaxRequestTarget target,
            boolean isDeprecate) {
        PageBase pageBase = getPageBase();
        PrismContainerWrapper<C> container = containerModel.getObject();
        PrismContainerValue<C> newValue = value;
        if (newValue == null) {
            newValue = container.getItem().createNewValue();
        }
        PrismContainerValueWrapper newWrapper = null;
        try {
            newWrapper = WebPrismUtil.createNewValueWrapper(
                    container, newValue, pageBase, context);
            container.getValues().add(newWrapper);
        } catch (SchemaException e) {
            LOGGER.error("Couldn't create new value for container " + container, e);
        }
        IModel<PrismContainerValueWrapper<C>> model = Model.of(newWrapper);
        onCreateValue(model, target, isDeprecate);
    }

    @SuppressWarnings("rawtypes")
    public MultivalueContainerListPanel getPanel() {
        return ((SchemaHandlingObjectsPanel) get(ID_PANEL)).getTable();
    }

    protected final ContainerPanelConfigurationType getConfiguration() {
        return WebComponentUtil.getContainerConfiguration(
                getAssignmentHolderDetailsModel().getObjectDetailsPageConfiguration().getObject(),
                getPanelType());
    }

    protected abstract String getPanelType();

    protected abstract void onEditValue(IModel<PrismContainerValueWrapper<C>> value, AjaxRequestTarget target);

    protected abstract void onCreateValue(IModel<PrismContainerValueWrapper<C>> value, AjaxRequestTarget target, boolean isDuplicate);

    @Override
    protected String getCssForWidthOfFeedbackPanel() {
        return "col-8";
    }

    @Override
    protected boolean isSubmitButtonVisible() {
        return true;
    }

    protected boolean isValid(AjaxRequestTarget target) {
        if (getPanel() == null) {
            return true; // no table, no validation
        }
        return getPanel().isValidFormComponents(target);
    }

    @Override
    protected void onExitPerformed(AjaxRequestTarget target) {
        if (isValid(target)) {
            checkDeltasExitPerformed(target);
        }
    }

    @Override
    protected void onSubmitPerformed(AjaxRequestTarget target) {
        if (isValid(target)) {
            OperationResult result = onSaveObjectPerformed(target);
            if (result != null && !result.isError()) {
                onExitPerformedAfterValidate(target);
            } else {
                target.add(getFeedback());
            }
        }
    }

    protected abstract OperationResult onSaveObjectPerformed(AjaxRequestTarget target);

    protected void onExitPerformedAfterValidate(AjaxRequestTarget target) {
        super.onExitPerformed(target);
    }

    private void checkDeltasExitPerformed(AjaxRequestTarget target) {
        getAssignmentHolderDetailsModel().getPageResource().checkDeltasExitPerformed(
                consumerTarget -> {
                    getAssignmentHolderDetailsModel().reloadPrismObjectModel();
                    refreshValueModel();
                    onExitPerformedAfterValidate(consumerTarget);
                },
                target);
    }

    protected abstract void refreshValueModel();
}

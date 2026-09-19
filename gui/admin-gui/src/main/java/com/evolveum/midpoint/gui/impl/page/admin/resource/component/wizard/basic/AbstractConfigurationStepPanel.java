/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.basic;

import java.io.Serial;
import java.util.List;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractFormWizardStepPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettings;
import com.evolveum.midpoint.gui.impl.prism.panel.ItemPanelSettingsBuilder;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormPrismContainerPanel;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormPrismPropertyValuePanel;
import com.evolveum.midpoint.gui.impl.prism.panel.vertical.form.VerticalFormPrismReferenceValuePanel;
import com.evolveum.midpoint.gui.impl.util.ConnectorConfigurationGroupingUtil;
import com.evolveum.midpoint.gui.impl.util.ProvisioningObjectsUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.VirtualContainersSpecificationType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.CapabilityCollectionType;

import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * @author lskublik
 */
public abstract class AbstractConfigurationStepPanel extends AbstractFormWizardStepPanel<ResourceDetailsModel> {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractConfigurationStepPanel.class);

    private static final String OPERATION_RESOURCE_TEST = AbstractConfigurationStepPanel.class.getName() + ".resourceTest";

    /** When there are more groups than this, the sections without mandatory items start collapsed. */
    private static final int MAX_UNCOLLAPSED_GROUPS = 3;

    public AbstractConfigurationStepPanel(ResourceDetailsModel model) {
        super(model);
    }

    @Override
    protected void initLayout() {
        List<ConnectorConfigurationGroupingUtil.Group> groups = getConfigurationGroups();
        PrismContainerWrapper<?> container = getContainerFormModel().getObject();
        if (groups == null || groups.isEmpty() || container == null) {
            super.initLayout();
            return;
        }

        // The grouped sections are backed by the virtual containers hooked up by the
        // configurationProperties wrapper factory, so the wizard renders the very same sections
        // as the details view.
        ContainerPanelConfigurationType config =
                ConnectorConfigurationGroupingUtil.createContainerPanelConfiguration(
                        (PrismContainerDefinition<?>) container, groups, container.getPath());

        RepeatingView form = new RepeatingView(ID_FORM);
        form.setOutputMarkupId(true);
        boolean collapseNonMandatory = groups.size() > MAX_UNCOLLAPSED_GROUPS;
        for (VirtualContainersSpecificationType virtualContainer : config.getContainer()) {
            form.add(createGroupedFormPanel(form.newChildId(), virtualContainer, collapseNonMandatory));
        }
        add(form);
    }

    @Override
    protected void updateFeedbackPanels(AjaxRequestTarget target) {
        MarkupContainer form = (MarkupContainer) get(ID_FORM);
        form.visitChildren(
                VerticalFormPrismPropertyValuePanel.class,
                (component, objectIVisit) -> ((VerticalFormPrismPropertyValuePanel<?>) component).updateFeedbackPanel(target));

        form.visitChildren(
                VerticalFormPrismReferenceValuePanel.class,
                (component, objectIVisit) -> ((VerticalFormPrismReferenceValuePanel<?>) component).updateFeedbackPanel(target));
    }

    protected List<ConnectorConfigurationGroupingUtil.Group> getConfigurationGroups() {
        try {
            PrismContainerWrapper<?> container = getContainerFormModel().getObject();
            return ConnectorConfigurationGroupingUtil.getConfigurationGroups(container);
        } catch (RuntimeException e) {
            LOGGER.error("Cannot determine configuration groups", e);
            return null;
        }
    }

    /**
     * Creates a form panel for a single grouped section, backed by the virtual container with the
     * given identifier. The section shows only the items resolved into the virtual container, so no
     * per-group visibility filtering is needed.
     */
    private MarkupContainer createGroupedFormPanel(String id,
            VirtualContainersSpecificationType virtualContainer, boolean collapseNonMandatory) {
        IModel<PrismContainerWrapper<Containerable>> model = createVirtualContainerModel(virtualContainer);
        ItemPanelSettings settings = new ItemPanelSettingsBuilder()
                .mandatoryHandler(this::checkMandatory)
                .panelConfiguration(getContainerConfiguration())
                .build();
        VerticalFormPrismContainerPanel<Containerable> panel = new VerticalFormPrismContainerPanel<>(id, model, settings) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected IModel<String> getTitleModel() {
                return Model.of(WebComponentUtil.getTranslatedPolyString(virtualContainer.getDisplay().getLabel()));
            }

            @Override
            protected boolean isVisibleSubContainer(PrismContainerWrapper<? extends Containerable> c) {
                return AbstractConfigurationStepPanel.this.isVisibleSubContainer(c);
            }

            @Override
            protected boolean isShowEmptyButtonVisible() {
                return AbstractConfigurationStepPanel.this.isShowEmptyButtonVisible();
            }

            @Override
            protected boolean isExpandedButtonVisible() {
                return AbstractConfigurationStepPanel.this.isExpandedButtonVisible();
            }
        };
        if (collapseNonMandatory && model.getObject() != null && !hasMandatoryItem(model.getObject())) {
            model.getObject().setExpanded(false);
            model.getObject().getValues().forEach(value -> value.setExpanded(false));
        }
        panel.setOutputMarkupId(true);
        return panel;
    }

    /**
     * Creates a model for the virtual container with the given identifier. The virtual containers are
     * materialized on the object value wrapper, so the lookup is resolved against the object wrapper
     * model. This works also for the wizard, where the configuration container itself may not have
     * a value yet.
     */
    @SuppressWarnings("unchecked")
    private IModel<PrismContainerWrapper<Containerable>> createVirtualContainerModel(
            VirtualContainersSpecificationType virtualContainer) {
        IModel<PrismContainerWrapper<Containerable>> objectModel =
                (IModel<PrismContainerWrapper<Containerable>>) (IModel<?>) getDetailsModel().getObjectWrapperModel();
        return PrismContainerWrapperModel.fromContainerWrapper(objectModel, virtualContainer.getIdentifier());
    }

    /**
     * Checks whether the section contains at least one mandatory item.
     */
    private boolean hasMandatoryItem(PrismContainerWrapper<?> section) {
        try {
            PrismContainerValueWrapper<?> valueWrapper = section.getValue();
            if (valueWrapper == null) {
                return false;
            }
            // The items of a virtual container are resolved lazily into the non-container items.
            for (ItemWrapper<?, ?> item : valueWrapper.getNonContainers()) {
                if (item.isMandatory()) {
                    return true;
                }
            }
            return false;
        } catch (SchemaException e) {
            LOGGER.error("Cannot check mandatory items in the configuration section", e);
            return true; // keep the section expanded when in doubt
        }
    }

    @Override
    protected String getIcon() {
        return "fa fa-cog";
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageResource.wizard.step.configuration");
    }

    @Override
    protected IModel<?> getTextModel() {
        return createStringResource("PageResource.wizard.step.configuration.text");
    }

    @Override
    protected IModel<?> getSubTextModel() {
        return createStringResource("PageResource.wizard.step.configuration.subText");
    }

    @Override
    public boolean onNextPerformed(AjaxRequestTarget target) {

        CapabilityCollectionType capabilities
                = ProvisioningObjectsUtil.getNativeCapabilities(getDetailsModel().getObjectType(), getPageBase());

        if (capabilities.getSchema() != null || capabilities.getTestConnection() != null) {
            PageBase pageBase = getPageBase();
            Task task = pageBase.createSimpleTask(OPERATION_RESOURCE_TEST);
            OperationResult result = task.getResult();

            try {
                pageBase.getModelService().testResource(getDetailsModel().getObjectWrapper().getObjectApplyDelta(), task, result);
            } catch (Exception e) {
                LoggingUtils.logUnexpectedException(LOGGER, "Failed to test resource connection", e);
                result.recordFatalError(getString("TestConnectionMessagesPanel.message.testConnection.fatalError"), e);
            }
            result.computeStatus();

            if (result.isSuccess()) {
                return super.onNextPerformed(target);
            }
            pageBase.showResult(result);
            target.add(getFeedback());
        } else {
            super.onNextPerformed(target);
        }

        return false;
    }
}

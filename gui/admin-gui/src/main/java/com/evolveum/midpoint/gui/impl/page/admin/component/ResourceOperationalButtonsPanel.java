/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;

import com.evolveum.midpoint.gui.impl.component.dialog.OnePanelPopupPanel;
import com.evolveum.midpoint.gui.impl.component.input.LifecycleStatePanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.ContainerWithLifecyclePanel;
import com.evolveum.midpoint.gui.impl.util.ProvisioningObjectsUtil;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxIconButton;

import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import com.evolveum.midpoint.web.model.PrismContainerValueWrapperModel;
import com.evolveum.midpoint.web.model.PrismPropertyWrapperModel;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.CapabilityUtil;
import com.evolveum.midpoint.web.page.admin.resources.component.TestConnectionResultPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.SchemaCapabilityType;

public abstract class ResourceOperationalButtonsPanel extends AssignmentHolderOperationalButtonsPanel<ResourceType> {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceOperationalButtonsPanel.class);

    private static final String DOT_CLASS = ResourceOperationalButtonsPanel.class.getName() + ".";
    private static final String OPERATION_REFRESH_SCHEMA = DOT_CLASS + "refreshSchema";
    private static final String OPERATION_SET_MAINTENANCE = DOT_CLASS + "setMaintenance";
    private static final String OPERATION_SET_LIFECYCLE_STATE = DOT_CLASS + "setLifecycleState";

    private static final String ID_RESOURCE_OPERATIONS_CONTAINER="resourceOperationsContainer";
    private static final String ID_RESOURCE_BUTTONS = "resourceButtons";
    private static final String ID_LIFECYCLE_STATE_PANEL = "lifecycleStatePanel";
    private static final String ID_DETAILED_LIFECYCLE_BUTTON = "detailedLifecycle";

    public ResourceOperationalButtonsPanel(String id, LoadableModel<PrismObjectWrapper<ResourceType>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        WebMarkupContainer resourceOperationsContainer = new WebMarkupContainer(ID_RESOURCE_OPERATIONS_CONTAINER){
            @Override
            public boolean isVisible() {
                return !WebComponentUtil.isTemplateCategory(getPrismObject().asObjectable());
            }
        };
        resourceOperationsContainer.setOutputMarkupId(true);
        add(resourceOperationsContainer);

        RepeatingView resourceButtons = new RepeatingView(ID_RESOURCE_BUTTONS);
        initResourceButtons(resourceButtons);
        resourceOperationsContainer.add(resourceButtons);

        initLifecycleStatePanel();
        initDetailedLifecycleButton();
    }

    private void initDetailedLifecycleButton() {
        AjaxIconButton detailedLifecycle = new AjaxIconButton(ID_DETAILED_LIFECYCLE_BUTTON,
                Model.of("fa fa-heart-pulse"),
                createStringResource("SchemaHandlingObjectsPanel.button.showLifecycleStates")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                PrismPropertyWrapperModel<ResourceType, String> lifecycleModel =
                        PrismPropertyWrapperModel.fromContainerWrapper(
                                ResourceOperationalButtonsPanel.this.getModel(), ResourceType.F_LIFECYCLE_STATE);
                OnePanelPopupPanel popupPanel = new OnePanelPopupPanel(
                        getPageBase().getMainPopupBodyId(),
                        1100,
                        600,
                        createStringResource("ContainerWithLifecyclePanel.popup.title")) {
                    @Override
                    protected WebMarkupContainer createPanel(String id) {
                        return new ContainerWithLifecyclePanel<>(
                                id,
                                PrismContainerValueWrapperModel.fromContainerWrapper(
                                        ResourceOperationalButtonsPanel.this.getModel(),
                                        ItemPath.EMPTY_PATH),
                                lifecycleModel);
                    }

                    @Override
                    protected void processHide(AjaxRequestTarget target) {
                        if (isLifecycleChanged(lifecycleModel)) {
                            getPageBase().hideMainPopup(target);
                            showLifecycleConfirmation(target, lifecycleModel, true);
                            return;
                        }
                        super.processHide(target);
                        WebComponentUtil.showToastForRecordedButUnsavedChanges(
                                target,
                                ResourceOperationalButtonsPanel.this.getModelObject().getValue());
                    }
                };

                getPageBase().showMainPopup(popupPanel, target);
            }
        };
        detailedLifecycle.showTitleAsLabel(true);
        add(detailedLifecycle);

    }

    private boolean isLifecycleChanged(PrismPropertyWrapperModel<ResourceType, String> lifecycleModel) {
        try {
            return lifecycleModel.getObject() != null
                    && lifecycleModel.getObject().getValue() != null
                    && ValueStatus.NOT_CHANGED != lifecycleModel.getObject().getValue().getStatus();
        } catch (SchemaException e) {
            LOGGER.error("Couldn't get value of " + lifecycleModel.getObject(), e);
            return false;
        }
    }

    private void initResourceButtons(RepeatingView resourceButtons) {
        AjaxIconButton test = new AjaxIconButton(resourceButtons.newChildId(),
                Model.of(GuiStyleConstants.CLASS_TEST_CONNECTION_MENU_ITEM),
                createStringResource("pageResource.button.test")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                testConnectionPerformed(target);
            }
        };
        test.showTitleAsLabel(true);
        test.add(new VisibleBehaviour(() -> isEditingObject()));
        resourceButtons.add(test);

        AjaxIconButton setMaintenance = new AjaxIconButton(resourceButtons.newChildId(),
                Model.of(GuiStyleConstants.CLASS_ICON_RESOURCE_MAINTENANCE),
                createStringResource("pageResource.button.toggleMaintenance")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                ProvisioningObjectsUtil.toggleResourceMaintenance(getPrismObject(), OPERATION_SET_MAINTENANCE, target, getPageBase());
                refreshStatus(target);
            }
        };
        setMaintenance.showTitleAsLabel(true);
        setMaintenance.add(new VisibleBehaviour(() -> isEditingObject() && canEdit(getObjectType())));
        resourceButtons.add(setMaintenance);

        AjaxIconButton refreshSchema = new AjaxIconButton(resourceButtons.newChildId(),
                Model.of(GuiStyleConstants.CLASS_ICON_RESOURCE_SCHEMA),
                createStringResource("pageResource.button.refreshSchema")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                ProvisioningObjectsUtil.refreshResourceSchema(getPrismObject(), OPERATION_REFRESH_SCHEMA, target, getPageBase());
            }

            @Override
            public boolean isVisible() {
                return !WebComponentUtil.isTemplateCategory(getPrismObject().asObjectable());
            }
        };
        refreshSchema.add(new VisibleBehaviour(() -> isVisibleRefresSchemaButton(getObjectType())));
        refreshSchema.showTitleAsLabel(true);
        resourceButtons.add(refreshSchema);
    }

    private void initLifecycleStatePanel() {
        PrismPropertyWrapperModel<ResourceType, String> model =
                PrismPropertyWrapperModel.fromContainerWrapper(getModel(), ResourceType.F_LIFECYCLE_STATE);
        LifecycleStatePanel lifecycleStatePanel = new LifecycleStatePanel(ID_LIFECYCLE_STATE_PANEL, model) {
            @Override
            protected void onInitialize() {
                super.onInitialize();
                getBaseFormComponent().add(new OnChangeAjaxBehavior() {
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        showLifecycleConfirmation(target, model, false);
                    }
                });
            }
        };
        add(lifecycleStatePanel);

        lifecycleStatePanel.add(new VisibleBehaviour(() -> isEditingObject() && canEdit(getObjectType())));
    }

    private void showLifecycleConfirmation(
            AjaxRequestTarget target,
            PrismPropertyWrapperModel<ResourceType, String> lifecycleModel,
            boolean finishDetailedLifecycleAfterwards) {

        ConfirmationPanel confirm = new ConfirmationPanel(
                getPageBase().getMainPopupBodyId(),
                getPageBase().createStringResource("ResourceOperationalButtonsPanel.confirm.lifecycleState")) {
            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                WebComponentUtil.saveObjectLifeCycle(
                        getPrismObject(), OPERATION_SET_LIFECYCLE_STATE, target, getPageBase());

                if (finishDetailedLifecycleAfterwards) {
                    markLifecycleChangeSaved(lifecycleModel);
                    finishDetailedLifecycleChanges(target);
                } else {
                    refreshStatus(target);
                }
            }

            @Override
            public void noPerformed(AjaxRequestTarget target) {
                super.noPerformed(target);
                try {
                    String realValue = lifecycleModel.getObject().getValue().getOldValue().getRealValue();
                    lifecycleModel.getObject().getValue().setStatus(ValueStatus.NOT_CHANGED);
                    lifecycleModel.getObject().getValue().getNewValue().setValue(realValue);
                    target.add(ResourceOperationalButtonsPanel.this.get(ID_LIFECYCLE_STATE_PANEL));
                    target.add(ResourceOperationalButtonsPanel.this.get(ID_LIFECYCLE_STATE_PANEL).getParent());
                } catch (SchemaException e) {
                    LOGGER.error("Couldn't get value of " + lifecycleModel.getObject());
                }

                if (finishDetailedLifecycleAfterwards) {
                    finishDetailedLifecycleChanges(target);
                }
            }
        };
        getPageBase().showMainPopup(confirm, target);
    }

    /**
     * Clears the changed state of the root lifecycle wrapper after the value was
     * saved immediately by the lifecycle operation.
     */
    private void markLifecycleChangeSaved(PrismPropertyWrapperModel<ResourceType, String> lifecycleModel) {
        try {
            String realValue = lifecycleModel.getObject().getValue().getRealValue();
            lifecycleModel.getObject().getValue().getOldValue().setValue(realValue);
            lifecycleModel.getObject().getValue().setStatus(ValueStatus.NOT_CHANGED);
        } catch (SchemaException e) {
            LOGGER.error("Couldn't get value of " + lifecycleModel.getObject(), e);
        }
    }

    /**
     * Shows the unsaved-changes toast for detailed lifecycle changes that still
     * have to be saved with the resource object.
     */
    private void finishDetailedLifecycleChanges(AjaxRequestTarget target) {
        try {
            var deltas = ResourceOperationalButtonsPanel.this.getModelObject().getValue().getDeltas()
                    .stream()
                    .filter(delta -> !ResourceType.F_LIFECYCLE_STATE.equivalent(delta.getPath().namedSegmentsOnly()))
                    .toList();

            if (!deltas.isEmpty()) {
                WebComponentUtil.showToastForRecordedButUnsavedChanges(target, deltas);
            }
        } catch (SchemaException e) {
            LOGGER.error("Couldn't get detailed lifecycle deltas", e);
        }
        refreshLifecycleViews(target);
    }

    protected void refreshLifecycleViews(AjaxRequestTarget target) {
        target.add(ResourceOperationalButtonsPanel.this.get(ID_LIFECYCLE_STATE_PANEL));
        target.add(ResourceOperationalButtonsPanel.this.get(ID_LIFECYCLE_STATE_PANEL).getParent());
    }

    private void testConnectionPerformed(AjaxRequestTarget target) {
        final PrismObject<ResourceType> dto = getPrismObject();
        if (dto == null || StringUtils.isEmpty(dto.getOid())) {
            error(getString("pageResource.message.oidNotDefined"));
            target.add(getPageBase().getFeedbackPanel());
            return;
        }

        final TestConnectionResultPanel testConnectionPanel =
                new TestConnectionResultPanel(getPageBase().getMainPopupBodyId(), dto.getOid(), getPage());
        testConnectionPanel.setOutputMarkupId(true);

        //TODO fix
//        getMainPopup().setCloseButtonCallback(new ModalWindow.CloseButtonCallback() {
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            public boolean onCloseButtonClicked(AjaxRequestTarget target) {
//                refreshStatus(target);
//                return true;
//            }
//        });

        showMainPopup(testConnectionPanel, target);

    }

    private boolean canEdit(ResourceType resource) {
        if (!resource.getAdditionalConnector().isEmpty()) {
            return false;
        }
        return true;
    }

    private boolean isVisibleRefresSchemaButton(ResourceType resource) {
        if (!isEditingObject()) {
            return false;
        }
        if (!resource.getAdditionalConnector().isEmpty()) { // TODO what is this?
            if (resource.getCapabilities() == null) {
                return false;
            }
            if (resource.getCapabilities().getConfigured() != null) {
                SchemaCapabilityType configuredCapability = CapabilityUtil.getCapability(
                        resource.getCapabilities().getConfigured(), SchemaCapabilityType.class);
                if (configuredCapability == null) {
                    return false;
                }
                return !Boolean.FALSE.equals(configuredCapability.isEnabled());
            }
            return false;
        }
        return true;
    }

    protected abstract void refreshStatus(AjaxRequestTarget target);
}

/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;

import com.evolveum.midpoint.gui.impl.component.dialog.OnePanelPopupPanel;
import com.evolveum.midpoint.gui.impl.component.input.LifecycleStatePanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.ContainerWithLifecyclePanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.ResourceObjectsPanel;
import com.evolveum.midpoint.gui.impl.util.ProvisioningObjectsUtil;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxIconButton;

import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import com.evolveum.midpoint.web.model.PrismContainerValueWrapperModel;
import com.evolveum.midpoint.web.model.PrismPropertyWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaHandlingType;

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
                                        ItemPath.EMPTY_PATH));
                    }

                    @Override
                    protected void processHide(AjaxRequestTarget target) {
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

                        ConfirmationPanel confirm = new ConfirmationPanel(
                                getPageBase().getMainPopupBodyId(),
                                getPageBase().createStringResource("ResourceOperationalButtonsPanel.confirm.lifecycleState")) {
                            @Override
                            public void yesPerformed(AjaxRequestTarget target) {
                                WebComponentUtil.saveObjectLifeCycle(
                                        getPrismObject(), OPERATION_SET_LIFECYCLE_STATE, target, getPageBase());
                                refreshStatus(target);
                            }

                            @Override
                            public void noPerformed(AjaxRequestTarget target) {
                                super.noPerformed(target);
                                try {
                                    String realValue = model.getObject().getValue().getOldValue().getRealValue();
                                    model.getObject().getValue().setStatus(ValueStatus.NOT_CHANGED);
                                    model.getObject().getValue().getNewValue().setValue(realValue);
                                    target.add(ResourceOperationalButtonsPanel.this.get(ID_LIFECYCLE_STATE_PANEL));
                                    target.add(ResourceOperationalButtonsPanel.this.get(ID_LIFECYCLE_STATE_PANEL).getParent());
                                } catch (SchemaException e) {
                                    LOGGER.error("Couldn't get value of " + model.getObject());
                                }
                            }
                        };
                        getPageBase().showMainPopup(confirm, target);
                    }
                });
            }
        };
        add(lifecycleStatePanel);

        lifecycleStatePanel.add(new VisibleBehaviour(() -> isEditingObject() && canEdit(getObjectType())));
    }

    private void testConnectionPerformed(AjaxRequestTarget target) {
        final PrismObject<ResourceType> dto = getPrismObject();
        if (dto == null || StringUtils.isEmpty(dto.getOid())) {
            error(getString("pageResource.message.oidNotDefined"));
            target.add(getPageBase().getFeedbackPanel());
            return;
        }

        final TestConnectionResultPanel testConnectionPanel =
                new TestConnectionResultPanel(getPageBase().getMainPopupBodyId(),
                        dto.getOid(), getPage()) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void okPerformed(AjaxRequestTarget target) {
                        refreshStatus(target);
                    }

                };
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

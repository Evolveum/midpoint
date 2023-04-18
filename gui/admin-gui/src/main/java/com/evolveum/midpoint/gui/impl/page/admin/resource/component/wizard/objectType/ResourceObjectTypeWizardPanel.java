/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType;

import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;

import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;

import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.basic.ResourceObjectTypeBasicWizardPanel;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.activation.ActivationsWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.associations.AssociationsWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.attributeMapping.AttributeMappingWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.capabilities.CapabilitiesWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.correlation.CorrelationWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.credentials.CredentialsWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.synchronization.SynchronizationWizardPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

/**
 * @author lskublik
 */
public class ResourceObjectTypeWizardPanel extends AbstractWizardPanel<ResourceObjectTypeDefinitionType, ResourceDetailsModel> {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceObjectTypeWizardPanel.class);
    private boolean showObjectTypePreview = false;

    public ResourceObjectTypeWizardPanel(
            String id,
            WizardPanelHelper<ResourceObjectTypeDefinitionType, ResourceDetailsModel> helper) {
        super(id, helper);
    }

    protected void initLayout() {
        add(createChoiceFragment(createNewResourceObjectTypeWizard()));
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        if (isShowObjectTypePreview()) {
            addOrReplace(createChoiceFragment(createObjectTypePreview()));
        }
    }

    private boolean isShowObjectTypePreview() {
        return showObjectTypePreview;
    }

    public void setShowObjectTypePreview(boolean showObjectTypePreview) {
        this.showObjectTypePreview = showObjectTypePreview;
    }

    private ResourceObjectTypeBasicWizardPanel createNewResourceObjectTypeWizard() {
        WizardPanelHelper<ResourceObjectTypeDefinitionType, ResourceDetailsModel> helper =
                new WizardPanelHelper<>(getAssignmentHolderModel()) {
            @Override
            public void onExitPerformed(AjaxRequestTarget target) {
                ResourceObjectTypeWizardPanel.this.onExitPerformed(target);
            }

            @Override
            public IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> getValueModel() {
                return ResourceObjectTypeWizardPanel.this.getValueModel();
            }

            @Override
            public OperationResult onSaveObjectPerformed(AjaxRequestTarget target) {
                OperationResult result = ResourceObjectTypeWizardPanel.this.onSavePerformed(target);
                if (result != null && !result.isError()) {
                    refreshValueModel();
                    showObjectTypePreviewFragment(target);
                }
                return result;
            }
        };
        return new ResourceObjectTypeBasicWizardPanel(getIdOfChoicePanel(), helper);
    }

    private void showObjectTypePreviewFragment(AjaxRequestTarget target) {
        showChoiceFragment(target, createObjectTypePreview());
    }

    private ResourceObjectTypeWizardPreviewPanel createObjectTypePreview() {
        return new ResourceObjectTypeWizardPreviewPanel(getIdOfChoicePanel(), createHelper()) {
            @Override
            protected void onTileClickPerformed(ResourceObjectTypePreviewTileType value, AjaxRequestTarget target) {
                switch (value) {
                    case BASIC:
                        showResourceObjectTypeBasic(target);
                        break;
                    case PREVIEW_DATA:
                        showTableForDataOfCurrentlyObjectType(target);
                        break;
                    case ATTRIBUTE_MAPPING:
                        showTableForAttributes(target);
                        break;
                    case SYNCHRONIZATION_CONFIG:
                        showSynchronizationConfigWizard(target);
                        break;
                    case CORRELATION_CONFIG:
                        showCorrelationItemsTable(target);
                        break;
                    case CREDENTIALS:
                        showCredentialsWizardPanel(target);
                        break;
                    case ASSOCIATIONS:
                        showAssociationsWizard(target);
                        break;
                    case ACTIVATION:
                        showActivationsWizard(target);
                        break;
                    case CAPABILITIES_CONFIG:
                        showCapabilitiesConfigWizard(target);
                        break;
                }
            }

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                super.onExitPerformed(target);
                ResourceObjectTypeWizardPanel.this.onExitPerformed(target);
            }
        };
    }

    private void showResourceObjectTypeBasic(AjaxRequestTarget target) {

        WizardPanelHelper<ResourceObjectTypeDefinitionType, ResourceDetailsModel> helper =
                new WizardPanelHelper<>(getAssignmentHolderModel()) {
            @Override
            public void onExitPerformed(AjaxRequestTarget target) {
                showObjectTypePreviewFragment(target);
            }

            @Override
            public IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> getValueModel() {
                return ResourceObjectTypeWizardPanel.this.getValueModel();
            }

            @Override
            public OperationResult onSaveObjectPerformed(AjaxRequestTarget target) {
                OperationResult result = ResourceObjectTypeWizardPanel.this.onSavePerformed(target);
                if (result != null && !result.isError()) {
                    refreshValueModel();
                    onExitPerformed(target);
                }
                return result;
            }
        };

        showChoiceFragment(
                target,
                new ResourceObjectTypeBasicWizardPanel(getIdOfChoicePanel(), helper)
        );
    }

    private void showCredentialsWizardPanel(AjaxRequestTarget target) {
        showChoiceFragment(
                target,
                new CredentialsWizardPanel(getIdOfChoicePanel(), createHelper())
        );
    }

    private void showCorrelationItemsTable(AjaxRequestTarget target) {
        showChoiceFragment(
                target,
                new CorrelationWizardPanel(getIdOfChoicePanel(), createHelper())
        );
    }

    private void showCapabilitiesConfigWizard(AjaxRequestTarget target) {
        showChoiceFragment(
                target,
                new CapabilitiesWizardPanel(getIdOfChoicePanel(), createHelper())
        );
    }

    private void showSynchronizationConfigWizard(AjaxRequestTarget target) {
        showWizardFragment(
                target,
                new SynchronizationWizardPanel(getIdOfWizardPanel(), createHelper())
        );
    }

    private void showActivationsWizard(AjaxRequestTarget target) {
        showWizardFragment(
                target,
                new ActivationsWizardPanel(getIdOfWizardPanel(), createHelper())
        );
    }

    private void showAssociationsWizard(AjaxRequestTarget target) {
        showChoiceFragment(
                target,
                new AssociationsWizardPanel(getIdOfChoicePanel(), createHelper())
        );
    }

    private void showTableForAttributes(AjaxRequestTarget target) {
        showChoiceFragment(
                target,
                new AttributeMappingWizardPanel(getIdOfChoicePanel(), createHelper())
        );
    }

    private WizardPanelHelper<ResourceObjectTypeDefinitionType, ResourceDetailsModel> createHelper() {
        return new WizardPanelHelper<>(getAssignmentHolderModel()) {
            @Override
            public void onExitPerformed(AjaxRequestTarget target) {
                showObjectTypePreviewFragment(target);
            }

            @Override
            public IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> getValueModel() {
                return getHelper().getValueModel();
            }

            @Override
            public OperationResult onSaveObjectPerformed(AjaxRequestTarget target) {
                OperationResult result = ResourceObjectTypeWizardPanel.this.onSavePerformed(target);
                if (result != null && !result.isError()) {
                    refreshValueModel();
                }
                return result;
            }
        };
    }

    private void showTableForDataOfCurrentlyObjectType(AjaxRequestTarget target) {
        showChoiceFragment(target, new PreviewResourceObjectTypeDataWizardPanel(
                getIdOfChoicePanel(),
                getAssignmentHolderModel(),
                ResourceObjectTypeWizardPanel.this.getValueModel()) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                super.onExitPerformed(target);
                showObjectTypePreviewFragment(target);
            }
        });
    }

    private <C extends Containerable> IModel<PrismContainerValueWrapper<C>> refreshValueModel(
            IModel<PrismContainerValueWrapper<C>> valueModel) {
        ItemPath path = valueModel.getObject().getPath();
        valueModel.detach();

        return new LoadableDetachableModel<>() {

            private ItemPath pathWithId;

            @Override
            protected PrismContainerValueWrapper<C> load() {
                ItemPath usedPath = path;
                if (pathWithId == null) {
                    if (!usedPath.isEmpty() && ItemPath.isId(usedPath.last())) {
                        try {
                            PrismContainerValueWrapper<C> newValue = getAssignmentHolderModel().getObjectWrapper().findContainerValue(usedPath);
                            if (newValue != null) {
                                return newValue;
                            }
                            usedPath = path.subPath(0, path.size() - 1);
                        } catch (SchemaException e) {
                            LOGGER.debug("Template was probably used for creating new resource. Cannot find container value wrapper, \nparent: {}, \npath: {}",
                                    getAssignmentHolderModel().getObjectWrapper(), usedPath);
                        }
                    }
                }

                if (pathWithId == null && !usedPath.isEmpty() && ItemPath.isId(usedPath.last())) {
                    pathWithId = usedPath;
                }

                try {
                    if (pathWithId != null) {
                        return getAssignmentHolderModel().getObjectWrapper().findContainerValue(pathWithId);
                    }
                    PrismContainerWrapper<C> container = getAssignmentHolderModel().getObjectWrapper().findContainer(usedPath);
                    PrismContainerValueWrapper<C> ret = null;
                    for (PrismContainerValueWrapper<C> value : container.getValues()) {
                        if (ret == null || ret.getNewValue().getId() == null
                                || (value.getNewValue().getId() != null && ret.getNewValue().getId() < value.getNewValue().getId())) {
                            ret = value;
                        }
                    }
                    if (ret != null && ret.getNewValue().getId() != null) {
                        pathWithId = ret.getPath();
                    }
                    return ret;
                } catch (SchemaException e) {
                    LOGGER.error("Cannot find container value wrapper, \nparent: {}, \npath: {}",
                            getAssignmentHolderModel().getObjectWrapper(), pathWithId);
                }
                return null;
            }
        };
    }

    private void refreshValueModel() {
        getHelper().setValueModel(refreshValueModel(getValueModel()));
    }
}

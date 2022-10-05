/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType;

import com.evolveum.midpoint.gui.api.component.result.Toast;
import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.AbstractResourceWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.attributeMapping.*;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaHandlingType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lskublik
 */
public class ResourceObjectTypeWizardPanel extends AbstractResourceWizardPanel<ResourceObjectTypeDefinitionType> {

    private static final Trace LOGGER = TraceManager.getTrace(ResourceObjectTypeWizardPanel.class);

    private IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel;

    public ResourceObjectTypeWizardPanel(
            String id,
            ResourceDetailsModel model,
            IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel) {
        super(id, model);
        if (valueModel == null) {
            this.valueModel = createModelOfNewValue(ItemPath.create(ResourceType.F_SCHEMA_HANDLING, SchemaHandlingType.F_OBJECT_TYPE));
        } else {
            this.valueModel = valueModel;
        }
    }

    protected void initLayout() {
        add(createWizardFragment(createNewResourceObjectTypeWizard()));
    }

    protected void onExitPerformed(AjaxRequestTarget target) {
    }

    private WizardPanel createNewResourceObjectTypeWizard() {
        return new WizardPanel(getIdOfWizardPanel(), new WizardModel(createNewObjectTypeSteps()));
    }

    private List<WizardStep> createNewObjectTypeSteps() {
        List<WizardStep> steps = new ArrayList<>();

        steps.add(new BasicSettingResourceObjectTypeStepPanel(getResourceModel(), valueModel) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                ResourceObjectTypeWizardPanel.this.onExitPerformed(target);
            }
        });

        steps.add(new DelineationResourceObjectTypeStepPanel(getResourceModel(), valueModel) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                ResourceObjectTypeWizardPanel.this.onExitPerformed(target);
            }
        });

        steps.add(new FocusResourceObjectTypeStepPanel(getResourceModel(), valueModel) {

            @Override
            protected IModel<?> getSubmitLabelModel() {
                if (isSavedAfterDetailsWizard()) {
                    return super.getSubmitLabelModel();
                }
                return getPageBase().createStringResource("WizardPanel.confirm");
            }

            @Override
            protected void onSubmitPerformed(AjaxRequestTarget target) {
                if (isSavedAfterDetailsWizard()) {
                    OperationResult result = onSaveResourcePerformed(target);
                    if (result != null && !result.isError()) {
                        new Toast()
                                .success()
                                .title(getString("ResourceWizardPanel.updateResource"))
                                .icon("fas fa-circle-check")
                                .autohide(true)
                                .delay(5_000)
                                .body(getString("ResourceWizardPanel.updateResource.text")).show(target);

                        valueModel = refreshValueModel(valueModel);
                        showObjectTypePreviewFragment(valueModel, target);
                    } else {
                        target.add(getFeedback());
                    }
                } else {
                    showObjectTypePreviewFragment(valueModel, target);
                }
            }

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                ResourceObjectTypeWizardPanel.this.onExitPerformed(target);
            }
        });

        return steps;
    }

    protected boolean isSavedAfterDetailsWizard() {
        return true;
    }

    private void showObjectTypePreviewFragment(
            IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> model, AjaxRequestTarget target) {
        showChoiceFragment(target, new ResourceObjectTypeWizardPreviewPanel(getIdOfChoicePanel(), getResourceModel(), model) {
            @Override
            protected void onResourceTileClick(ResourceObjectTypePreviewTileType value, AjaxRequestTarget target) {
                switch (value) {
                    case PREVIEW_DATA:
                        showTableForDataOfCurrentlyObjectType(target, getValueModel());
                        break;
                    case ATTRIBUTE_MAPPING:
                        showTableForAttributes(target, getValueModel());
                        break;
                    case SYNCHRONIZATION_CONFIG:
                        showSynchronizationConfigWizard(target, getValueModel());
                        break;
                    case CORRELATION_CONFIG:
                        showCorrelationItemsTable(target, getValueModel());
                        break;
                    case CREDENTIALS:
                        showCredentialsWizardPanel(target, getValueModel());
                        break;
                }
            }

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                super.onExitPerformed(target);
                ResourceObjectTypeWizardPanel.this.onExitPerformed(target);
            }
        });
    }

    private void showCredentialsWizardPanel(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel) {
        showChoiceFragment(target, new CredentialsWizardPanel(getIdOfChoicePanel(), getResourceModel(), valueModel) {

            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                showObjectTypePreviewFragment(getValueModel(), target);
            }

            @Override
            protected OperationResult onSaveResourcePerformed(AjaxRequestTarget target) {
                OperationResult result = ResourceObjectTypeWizardPanel.this.onSaveResourcePerformed(target);
                if (result != null && !result.isError()) {
                    refreshValueModel();
                }
                return result;
            }

            @Override
            protected boolean isSavedAfterWizard() {
                return isSavedAfterDetailsWizard();
            }
        });
    }

    private void showCorrelationItemsTable(
            AjaxRequestTarget target, IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel) {
        showChoiceFragment(target, new CorrelationWizardPanel(getIdOfChoicePanel(), getResourceModel(), valueModel) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                showObjectTypePreviewFragment(getValueModel(), target);
            }

            @Override
            protected OperationResult onSaveResourcePerformed(AjaxRequestTarget target) {
                OperationResult result = ResourceObjectTypeWizardPanel.this.onSaveResourcePerformed(target);
                if (result != null && !result.isError()) {
                    refreshValueModel();
                }
                return result;
            }

            @Override
            protected boolean isSavedAfterWizard() {
                return isSavedAfterDetailsWizard();
            }
        });
    }

    private void showSynchronizationConfigWizard(
            AjaxRequestTarget target, IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel) {
        showWizardFragment(
                target,
                new SynchronizationWizardPanel(getIdOfWizardPanel(), getResourceModel(), valueModel) {
                    protected void onExitPerformed(AjaxRequestTarget target) {
                        showObjectTypePreviewFragment(getValueModel(), target);
                    }

                    @Override
                    protected OperationResult onSaveResourcePerformed(AjaxRequestTarget target) {
                        OperationResult result = ResourceObjectTypeWizardPanel.this.onSaveResourcePerformed(target);
                        if (result != null && !result.isError()) {
                            refreshValueModel();
                        }
                        return result;
                    }

                    @Override
                    protected boolean isSavedAfterWizard() {
                        return isSavedAfterDetailsWizard();
                    }
                });
    }

    private void showCredentialsWizard(
            AjaxRequestTarget target, IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel) {
        showWizardFragment(
                target,
                new WizardPanel(getIdOfWizardPanel(), new WizardModel(createCredentialsSteps(valueModel))));
    }

    private List<WizardStep> createCredentialsSteps(IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel) {
        List<WizardStep> steps = new ArrayList<>();

        return steps;
    }

    private void showTableForAttributes(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> valueModel) {
        showChoiceFragment(target, new AttributeMappingWizardPanel(
                getIdOfChoicePanel(),
                getResourceModel(),
                valueModel) {
            @Override
            protected void onExitPerformed(AjaxRequestTarget target) {
                showObjectTypePreviewFragment(getValueModel(), target);
            }

            @Override
            protected OperationResult onSaveResourcePerformed(AjaxRequestTarget target) {
                OperationResult result = ResourceObjectTypeWizardPanel.this.onSaveResourcePerformed(target);
                if (result != null && !result.isError()) {
                    refreshValueModel();
                }
                return result;
            }

                @Override
                protected boolean isSavedAfterWizard () {
                    return isSavedAfterDetailsWizard();
                }
            });
        }


        private void showTableForDataOfCurrentlyObjectType (
                AjaxRequestTarget target, IModel < PrismContainerValueWrapper < ResourceObjectTypeDefinitionType >> valueModel){
            showChoiceFragment(target, new PreviewResourceObjectTypeDataWizardPanel(
                    getIdOfChoicePanel(),
                    getResourceModel(),
                    valueModel) {
                @Override
                protected void onExitPerformed(AjaxRequestTarget target) {
                    super.onExitPerformed(target);
                    showObjectTypePreviewFragment(getResourceObjectType(), target);
                }
            });
        }

        private <C extends Containerable > IModel < PrismContainerValueWrapper < C >> refreshValueModel(
                IModel < PrismContainerValueWrapper < C >> valueModel) {
            ItemPath path = valueModel.getObject().getPath();
            if (!path.isEmpty() && ItemPath.isId(path.last())) {
                valueModel.detach();
                return new LoadableDetachableModel<>() {
                    @Override
                    protected PrismContainerValueWrapper<C> load() {
                        try {
                            return getResourceModel().getObjectWrapper().findContainerValue(path);
                        } catch (SchemaException e) {
                            LOGGER.error("Cannot find container value wrapper, \nparent: {}, \npath: {}",
                                    getResourceModel().getObjectWrapper(), path);
                        }
                        return null;
                    }
                };
            } else {
                valueModel.detach();
                return getValueWrapperWitLastId(path);
            }
        }

        private <C extends Containerable > IModel < PrismContainerValueWrapper < C >> getValueWrapperWitLastId(ItemPath itemPath)
        {
            return new LoadableDetachableModel<>() {

                private ItemPath pathWithId;

                @Override
                protected PrismContainerValueWrapper<C> load() {
                    if (!itemPath.isEmpty() && ItemPath.isId(itemPath.last())) {
                        pathWithId = itemPath;
                    }

                    try {
                        if (pathWithId != null) {
                            return getResourceModel().getObjectWrapper().findContainerValue(pathWithId);
                        }
                        PrismContainerWrapper<C> container = getResourceModel().getObjectWrapper().findContainer(itemPath);
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
                                getResourceModel().getObjectWrapper(), pathWithId);
                    }
                    return null;
                }
            };
        }

        private void refreshValueModel () {
            valueModel = refreshValueModel(valueModel);
        }
    }

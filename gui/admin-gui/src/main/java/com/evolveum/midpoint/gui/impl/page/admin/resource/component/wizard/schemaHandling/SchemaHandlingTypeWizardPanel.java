/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.PageAssignmentHolderDetails;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.basic.ResourceObjectTypeBasicWizardPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;

import com.evolveum.midpoint.web.model.PrismContainerValueWrapperModel;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

/**
 * @author lskublik
 */
public abstract class SchemaHandlingTypeWizardPanel<C extends Containerable> extends AbstractWizardPanel<C, ResourceDetailsModel> {

    private static final Trace LOGGER = TraceManager.getTrace(SchemaHandlingTypeWizardPanel.class);
    private boolean showTypePreview = false;

    public SchemaHandlingTypeWizardPanel(
            String id,
            WizardPanelHelper<C, ResourceDetailsModel> helper) {
        super(id, helper);
    }

    protected void initLayout() {
        add(createChoiceFragment(createNewTypeWizard()));
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        if (isShowTypePreview()) {
            addOrReplace(createChoiceFragment(createTypePreview()));
        }
    }

    private boolean isShowTypePreview() {
        return showTypePreview;
    }

    public void setShowTypePreview(boolean showTypePreview) {
        this.showTypePreview = showTypePreview;
    }

    private AbstractWizardPanel<C, ResourceDetailsModel> createNewTypeWizard() {
        WizardPanelHelper<C, ResourceDetailsModel> helper =
                new WizardPanelHelper<>(getAssignmentHolderModel()) {
            @Override
            public void onExitPerformed(AjaxRequestTarget target) {
                SchemaHandlingTypeWizardPanel.this.onExitPerformed(target);
            }

            @Override
            public IModel<PrismContainerValueWrapper<C>> getValueModel() {
                return SchemaHandlingTypeWizardPanel.this.getValueModel();
            }

            @Override
            public OperationResult onSaveObjectPerformed(AjaxRequestTarget target) {
                OperationResult result = SchemaHandlingTypeWizardPanel.this.onSavePerformed(target);
                if (result != null && !result.isError()) {
                    refreshValueModel();
                    showTypePreviewFragment(target);
                }
                return result;
            }
        };
        return createNewTypeWizard(getIdOfChoicePanel(), helper);
    }

    protected abstract AbstractWizardPanel<C, ResourceDetailsModel> createNewTypeWizard(
            String id, WizardPanelHelper<C, ResourceDetailsModel> helper);

    protected void showTypePreviewFragment(AjaxRequestTarget target) {
        showChoiceFragment(target, createTypePreview());
    }

    protected abstract Component createTypePreview();

    protected  <V extends Containerable> WizardPanelHelper<V, ResourceDetailsModel> createHelper(ItemPath path, boolean isWizardFlow) {
        return new WizardPanelHelper<>(getAssignmentHolderModel()) {
            @Override
            public void onExitPerformed(AjaxRequestTarget target) {
                checkDeltasExitPerformed(target);
            }

            @Override
            public IModel<PrismContainerValueWrapper<V>> getValueModel() {
                return PrismContainerValueWrapperModel.fromContainerValueWrapper(SchemaHandlingTypeWizardPanel.this.getValueModel(), path);
            }

            @Override
            public OperationResult onSaveObjectPerformed(AjaxRequestTarget target) {
                OperationResult result = SchemaHandlingTypeWizardPanel.this.onSavePerformed(target);
                if (isWizardFlow && result != null && !result.isError()) {
                    refreshValueModel();
                    showTypePreviewFragment(target);
                }
                return result;
            }
        };
    }

    protected WizardPanelHelper<C, ResourceDetailsModel> createHelper(boolean isWizardFlow) {

        return new WizardPanelHelper<>(getAssignmentHolderModel()) {
                    @Override
                    public void onExitPerformed(AjaxRequestTarget target) {
                        checkDeltasExitPerformed(target);
                    }

                    @Override
                    public IModel<PrismContainerValueWrapper<C>> getValueModel() {
                        return SchemaHandlingTypeWizardPanel.this.getValueModel();
                    }

                    @Override
                    public OperationResult onSaveObjectPerformed(AjaxRequestTarget target) {
                        OperationResult result = SchemaHandlingTypeWizardPanel.this.onSavePerformed(target);
                        if (isWizardFlow && result != null && !result.isError()) {
                            refreshValueModel();
                            showTypePreviewFragment(target);
                        }
                        return result;
                    }
                };
    }

    private void checkDeltasExitPerformed(AjaxRequestTarget target) {

        if (!((PageAssignmentHolderDetails)getPageBase()).hasUnsavedChanges(target)) {
            getAssignmentHolderModel().reloadPrismObjectModel();
            refreshValueModel();
            showTypePreviewFragment(target);
            return;
        }
        ConfirmationPanel confirmationPanel = new ConfirmationPanel(getPageBase().getMainPopupBodyId(),
                createStringResource("OperationalButtonsPanel.confirmBack")) {

            private static final long serialVersionUID = 1L;

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                getAssignmentHolderModel().reloadPrismObjectModel();
                refreshValueModel();
                showTypePreviewFragment(target);
            }
        };

        getPageBase().showMainPopup(confirmationPanel, target);
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

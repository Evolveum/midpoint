/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.wizard;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.PageAssignmentHolderDetails;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.SchemaHandlingTypeWizardPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.subject.ResourceAssociationTypeSubjectWizardPanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.model.PrismContainerValueWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.checkerframework.checker.units.qual.A;

/**
 * @author lskublik
 */
public abstract class AbstractWizardPanelWithChoicePanel<C extends Containerable, AHD extends AssignmentHolderDetailsModel> extends AbstractWizardPanel<C, AHD> {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractWizardPanelWithChoicePanel.class);

    private boolean showTypePreview = false;

    public AbstractWizardPanelWithChoicePanel(
            String id,
            WizardPanelHelper<C, AHD> helper) {
        super(id, helper);
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        if (isShowTypePreview()) {
            addOrReplace(createChoiceFragment(createTypePreview()));
        }
    }

    protected abstract Component createTypePreview();

    private boolean isShowTypePreview() {
        return showTypePreview;
    }

    public void setShowTypePreview(boolean showTypePreview) {
        this.showTypePreview = showTypePreview;
    }

    protected  <V extends Containerable> WizardPanelHelper<V, AHD> createHelper(ItemPath path, boolean isWizardFlow) {
        return new WizardPanelHelper<>(getAssignmentHolderModel()) {
            @Override
            public void onExitPerformed(AjaxRequestTarget target) {
                checkDeltasExitPerformed(target);
            }

            @Override
            public IModel<PrismContainerValueWrapper<V>> getValueModel() {
                return PrismContainerValueWrapperModel.fromContainerValueWrapper(AbstractWizardPanelWithChoicePanel.this.getValueModel(), path);
            }

            @Override
            public OperationResult onSaveObjectPerformed(AjaxRequestTarget target) {
                OperationResult result = AbstractWizardPanelWithChoicePanel.this.onSavePerformed(target);
                if (isWizardFlow && result != null && !result.isError()) {
                    refreshValueModel();
                    showTypePreviewFragment(target);
                }
                return result;
            }
        };
    }

    protected WizardPanelHelper<C, AHD> createHelper(boolean isWizardFlow) {

        return new WizardPanelHelper<>(getAssignmentHolderModel()) {
            @Override
            public void onExitPerformed(AjaxRequestTarget target) {
                checkDeltasExitPerformed(target);
            }

            @Override
            public IModel<PrismContainerValueWrapper<C>> getValueModel() {
                return AbstractWizardPanelWithChoicePanel.this.getValueModel();
            }

            @Override
            public OperationResult onSaveObjectPerformed(AjaxRequestTarget target) {
                OperationResult result = AbstractWizardPanelWithChoicePanel.this.onSavePerformed(target);
                if (isWizardFlow && result != null && !result.isError()) {
                    refreshValueModel();
                    showTypePreviewFragment(target);
                }
                return result;
            }
        };
    }

    protected void checkDeltasExitPerformed(AjaxRequestTarget target) {

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

    protected abstract void showTypePreviewFragment(AjaxRequestTarget target);

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

    protected void refreshValueModel() {
        getHelper().setValueModel(refreshValueModel(getValueModel()));
    }
}

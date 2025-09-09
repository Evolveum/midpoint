/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.wizard;

import java.io.Serializable;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.schema.result.OperationResult;

public abstract class WizardPanelHelper<C extends Containerable, ODM extends ObjectDetailsModels> implements Serializable {

    private static final Trace LOGGER = TraceManager.getTrace(WizardPanelHelper.class);

    private IModel<PrismContainerValueWrapper<C>> valueModel;

    private ODM detailsModel;
    private IModel<String> exitLabel;

    public WizardPanelHelper(
            @NotNull ODM resourceModel) {
        this.detailsModel = resourceModel;
    }

    public WizardPanelHelper(
            @NotNull ODM resourceModel,
            IModel<PrismContainerValueWrapper<C>> valueModel) {
        this.detailsModel = resourceModel;
        this.valueModel = valueModel;
    }

    public ODM getDetailsModel() {
        return detailsModel;
    }

    public final void updateDetailsModel(ODM newModel) {
        this.detailsModel = newModel;
    }

    public final IModel<PrismContainerValueWrapper<C>> getValueModel() {
        if (valueModel == null) {
            return getDefaultValueModel();
        }
        return valueModel;
    }

    protected IModel<PrismContainerValueWrapper<C>> getDefaultValueModel() {
        return null;
    }

    public abstract void onExitPerformed(AjaxRequestTarget target);

    public OperationResult onSaveObjectPerformed(AjaxRequestTarget target) {
        return null;
    }

    public void setValueModel(IModel<PrismContainerValueWrapper<C>> newValueModel) {
        valueModel = newValueModel;
    }

    public void onExitPerformedAfterValidate(AjaxRequestTarget target) {
        onExitPerformed(target);
    }

    public final void refreshValueModel() {
        setValueModel(refreshValueModel(getValueModel()));
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
                            PrismContainerValueWrapper<C> newValue = getDetailsModel().getObjectWrapper().findContainerValue(usedPath);
                            if (newValue != null) {
                                return newValue;
                            }
                            usedPath = path.subPath(0, path.size() - 1);
                        } catch (SchemaException e) {
                            LOGGER.debug("Template was probably used for creating new resource. Cannot find container value wrapper, \nparent: {}, \npath: {}",
                                    getDetailsModel().getObjectWrapper(), usedPath);
                        }
                    }
                }

                if (pathWithId == null && !usedPath.isEmpty() && ItemPath.isId(usedPath.last())) {
                    pathWithId = usedPath;
                }

                try {
                    if (pathWithId != null) {
                        return getDetailsModel().getObjectWrapper().findContainerValue(pathWithId);
                    }
                    PrismContainerWrapper<C> container = getDetailsModel().getObjectWrapper().findContainer(usedPath);
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
                            getDetailsModel().getObjectWrapper(), pathWithId);
                }
                return null;
            }
        };
    }

    public void setExitLabel(IModel<String> exitLabel) {
        this.exitLabel = exitLabel;
    }

    public IModel<String> getExitLabel() {
        return exitLabel;
    }
}

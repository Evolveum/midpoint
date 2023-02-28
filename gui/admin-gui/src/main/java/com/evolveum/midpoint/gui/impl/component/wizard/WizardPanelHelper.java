/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.wizard;

import java.io.Serializable;

import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.schema.result.OperationResult;

public abstract class WizardPanelHelper<C extends Containerable, AHD extends AssignmentHolderDetailsModel> implements Serializable {

    private IModel<PrismContainerValueWrapper<C>> valueModel;
    private final AHD detailsModel;

    public WizardPanelHelper(
            @NotNull AHD resourceModel) {
        this.detailsModel = resourceModel;
    }

    public WizardPanelHelper(
            @NotNull AHD resourceModel,
            @NotNull IModel<PrismContainerValueWrapper<C>> valueModel) {
        this.detailsModel = resourceModel;
        this.valueModel = valueModel;
    }

    public AHD getDetailsModel() {
        return detailsModel;
    }

    public IModel<PrismContainerValueWrapper<C>> getValueModel() {
        return valueModel;
    }

    public abstract void onExitPerformed(AjaxRequestTarget target);

    public OperationResult onSaveObjectPerformed(AjaxRequestTarget target) {
        return null;
    }

    public void setValueModel(IModel<PrismContainerValueWrapper<C>> newValueModel) {
        valueModel = newValueModel;
    }
}

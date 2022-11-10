/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard;

import java.io.Serializable;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.schema.result.OperationResult;

public abstract class ResourceWizardPanelHelper<C extends Containerable> implements Serializable {

    private IModel<PrismContainerValueWrapper<C>> valueModel;
    private final ResourceDetailsModel resourceModel;

    public ResourceWizardPanelHelper(
            @NotNull ResourceDetailsModel resourceModel,
            @NotNull IModel<PrismContainerValueWrapper<C>> valueModel) {
        this.resourceModel = resourceModel;
        this.valueModel = valueModel;
    }

    public ResourceDetailsModel getResourceModel() {
        return resourceModel;
    }

    public IModel<PrismContainerValueWrapper<C>> getValueModel() {
        return valueModel;
    }

    public abstract void onExitPerformed(AjaxRequestTarget target);

    public boolean isSavedAfterWizard() {
        return true;
    }

    public OperationResult onSaveResourcePerformed(AjaxRequestTarget target) {
        return null;
    }

    public void setValueModel(IModel<PrismContainerValueWrapper<C>> newValueModel) {
        valueModel = newValueModel;
    }
}

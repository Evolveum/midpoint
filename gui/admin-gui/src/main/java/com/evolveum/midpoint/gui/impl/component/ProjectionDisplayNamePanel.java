/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component;

import java.io.Serial;
import java.util.List;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.DisplayNamePanel;
import com.evolveum.midpoint.gui.api.component.PendingOperationPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.data.column.icon.CompositedIconPanel;
import com.evolveum.midpoint.gui.impl.util.ProvisioningObjectsUtil;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author skublik
 */
public class ProjectionDisplayNamePanel extends DisplayNamePanel<ShadowType> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_PENDING_OPERATION_CONTAINER = "pendingOperationContainer";
    private static final String ID_PENDING_OPERATION = "pendingOperation";

    public ProjectionDisplayNamePanel(String id, IModel<ShadowType> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        WebMarkupContainer pendingOperationContainer = new WebMarkupContainer(ID_PENDING_OPERATION_CONTAINER);
        List<PendingOperationType> pendingOperations = getModelObject().getPendingOperation();
        if (pendingOperations != null
                && !pendingOperations.isEmpty()) {

            pendingOperationContainer.add(new PendingOperationPanel(ID_PENDING_OPERATION,
                    (IModel<List<PendingOperationType>>) () -> pendingOperations));
        } else {
            pendingOperationContainer.add(new WebMarkupContainer(ID_PENDING_OPERATION));
            pendingOperationContainer.add(new VisibleEnableBehaviour() {
                @Override
                public boolean isVisible() {
                    return false;
                }
            });
        }
        add(pendingOperationContainer);
    }

    @Override
    protected IModel<String> getKindIntentLabelModel() {
        return ProvisioningObjectsUtil.getResourceLabelModel(getModelObject(), getPageBase());
    }

    @Override
    protected IModel<List<String>> getDescriptionLabelsModel() {
        return () -> List.of(ProvisioningObjectsUtil.determineDisplayNameForDefinition(getModelObject(), getPageBase()));
    }

    @Override
    protected WebMarkupContainer createTypeImagePanel(String idTypeImage) {
        if (getModelObject() == null) {
            return super.createTypeImagePanel(idTypeImage);
        }
        return new CompositedIconPanel(idTypeImage, Model.of(WebComponentUtil.createAccountIcon(getModelObject(), getPageBase(), false)));
    }
}

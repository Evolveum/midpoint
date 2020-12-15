/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.DisplayNamePanel;
import com.evolveum.midpoint.gui.api.component.PendingOperationPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PendingOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import org.apache.wicket.model.PropertyModel;

/**
 * @author skublik
 */

public class ProjectionDisplayNamePanel extends DisplayNamePanel<ShadowType>{

    private static final long serialVersionUID = 1L;

    private final static String ID_PENDING_OPERATION_CONTAINER = "pendingOperationContainer";
    private final static String ID_PENDING_OPERATION = "pendingOperation";

    public ProjectionDisplayNamePanel(String id, IModel<ShadowType> model) {
        super(id, model);

    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        WebMarkupContainer pendingOperationContainer = new WebMarkupContainer(ID_PENDING_OPERATION_CONTAINER);

        pendingOperationContainer.add(new PendingOperationPanel(ID_PENDING_OPERATION, new PropertyModel<>(getModel(), ShadowType.F_PENDING_OPERATION.getLocalPart())));
        add(pendingOperationContainer);
    }

    @Override
    protected IModel<String> getKindIntentLabelModel() {
        return WebComponentUtil.getResourceLabelModel(getModelObject(), getPageBase());
    }

    @Override
    protected IModel<List<String>> getDescriptionLabelsModel() {
        List<String> descriptionLabels = new ArrayList<String>();
        descriptionLabels.add(WebComponentUtil.getResourceAttributesLabelModel(getModelObject(), getPageBase()).getObject());
        return new IModel<List<String>>() {

            @Override
            public List<String> getObject() {
                return descriptionLabels;
            }

        };
    }

    @Override
    protected String createImageModel() {
        if (getModelObject() == null){
            return "";
        }
        return WebComponentUtil.createShadowIcon(getModelObject().asPrismObject());
    }
}

/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.panel;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValueMetadataType;

/**
 * @author katka
 */
public class ValueMetadataPanel<C extends Containerable, CVW extends PrismContainerValueWrapper<C>>
        extends PrismContainerValuePanel<C, CVW> {

    public ValueMetadataPanel(String id, IModel<CVW> model, ItemPanelSettings settings) {
        super(id, model, settings);
    }

    @Override
    protected void addToHeader(WebMarkupContainer header) {
        IModel<String> headerLabelModel = getLabelModel();
        Label labelComponent = new Label(ID_LABEL, headerLabelModel);
        labelComponent.setRenderBodyOnly(true);
        labelComponent.add(new VisibleBehaviour(this::notEmptyAndNotDirectChildOfValueMetadataType));
        header.add(labelComponent);
    }

    private boolean notEmptyAndNotDirectChildOfValueMetadataType() {
        CVW modelObject = getModelObject();

        if (modelObject == null) {
            return false;
        }

        // TODO probably this doesn't need to be here if the wrapper factory generates wrappers correctly..
        if (modelObject.getOldValue() == null || modelObject.getOldValue().isEmpty()) {
            return false;
        }

        PrismContainerWrapper<C> parent = modelObject.getParent();
        if (parent == null) {
            return false;
        }

        PrismContainerValueWrapper<?> parentContainerValue = parent.getParent();
        if (parentContainerValue == null) {
            return true;
        }

        if (parentContainerValue.getDefinition() == null) {
            return false;
        }

        return !QNameUtil.match(parentContainerValue.getDefinition().getTypeName(), ValueMetadataType.COMPLEX_TYPE);
    }

    @Override
    protected Component createDefaultPanel(String id) {
        MetadataContainerValuePanel<C, CVW> panel = new MetadataContainerValuePanel<>(id, getModel(), getSettings());
        panel.setOutputMarkupId(true);
        return panel;
    }

    @Override
    protected <PV extends PrismValue> PV createNewValue(PrismContainerWrapper<C> itemWrapper) {
        return null;
    }

    @Override
    protected void remove(CVW valueToRemove, AjaxRequestTarget target) {

    }

    @Override
    protected void createMetadataPanel(MidpointForm form) {

    }

    @Override
    protected boolean isRemoveButtonVisible() {
        return false;
    }
}

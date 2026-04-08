/*
 * Copyright (c) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.prism.panel.vertical.form;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.prism.panel.*;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDelineationType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * @author lskublik
 */
public class VerticalFormPrismPropertyPanel<T> extends PrismPropertyPanel<T> {

    private static final long serialVersionUID = 1L;
    private static final Trace LOGGER = TraceManager.getTrace(VerticalFormPrismPropertyPanel.class);

    private static final String ID_HEADER = "header";

    private boolean isRequiredTagVisibleInHeaderPanel = false;

    public VerticalFormPrismPropertyPanel(String id, IModel<PrismPropertyWrapper<T>> model, ItemPanelSettings settings) {
        super(id, model, settings);
    }

    @Override
    protected ItemHeaderPanel createHeaderPanel() {
        VerticalFormPrismPropertyHeaderPanel<T> header = new VerticalFormPrismPropertyHeaderPanel<T>(ID_HEADER, getModel(), getSettings()) {
            @Override
            protected void refreshPanel(AjaxRequestTarget target) {
                target.add(VerticalFormPrismPropertyPanel.this);
            }
        };
        header.setRequiredTagVisibleInHeaderPanel(isRequired());
        return header;
    }

    @Override
    protected Component createValuePanel(ListItem<PrismPropertyValueWrapper<T>> item) {
        return createPrismPropertyValuePanel(item);
    }

    private @NotNull Component createPrismPropertyValuePanel(@NotNull ListItem<PrismPropertyValueWrapper<T>> item) {
        PrismPropertyValuePanel<T> panel = new VerticalFormPrismPropertyValuePanel<T>(ID_VALUE, item.getModel(), getSettings()) {

            @Override
            protected void remove(PrismPropertyValueWrapper<T> valueToRemove, AjaxRequestTarget target) throws SchemaException {
                removeValue(valueToRemove, target);
            }
        };
        item.add(panel);
        return panel;
    }

    protected String getCssClassForValueContainer() {
        PrismPropertyWrapper<T> modelObject = getModelObject();
        ItemPath path = modelObject.getPath();
        //What's if there are other multivalue properties that need special styling? should we check
        // if multivalue then apply/implement something like prism-property-values instead of prism-property-value?
        if (path != null && Objects.equals(path.last(), ResourceObjectTypeDelineationType.F_FILTER)) {
            return "d-flex flex-column gap-2";
        }

        return "";
    }

    public void setRequiredTagVisibleInHeaderPanel(boolean requiredTagVisibleInHeaderPanel) {
        isRequiredTagVisibleInHeaderPanel = requiredTagVisibleInHeaderPanel;
    }

    private boolean isRequired() {
        return isRequiredTagVisibleInHeaderPanel || isMandatory();
    }

    private boolean isMandatory() {
        return getSettings() != null && getSettings().getMandatoryHandler() != null
                && getSettings().getMandatoryHandler().isMandatory(getModelObject());
    }
}

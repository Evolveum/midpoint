/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search.panel;

import com.evolveum.midpoint.gui.impl.component.search.wrapper.ObjectTypeSearchItemWrapper;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.input.QNameObjectTypeChoiceRenderer;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import javax.xml.namespace.QName;
import java.util.List;

public class ObjectTypeSearchItemPanel<T> extends SingleSearchItemPanel<ObjectTypeSearchItemWrapper> {

    public ObjectTypeSearchItemPanel(String id, IModel<ObjectTypeSearchItemWrapper> searchItem) {
        super(id, searchItem);
    }

    @Override
    protected Component initSearchItemField(String id) {
        DropDownChoicePanel<QName> choices = new DropDownChoicePanel<>(id, new PropertyModel(getModel(), ObjectTypeSearchItemWrapper.F_VALUE),
                getSortedAvailableData(),
                new QNameObjectTypeChoiceRenderer(), getModelObject().isAllowAllTypesSearch()) {

            private static final long serialVersionUID = 1L;

            @Override
            protected String getNullValidDisplayValue() {
                return getString("ObjectTypes.all");
            }
        };
        choices.getBaseFormComponent().add(new OnChangeAjaxBehavior() {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                getModelObject().setTypeChanged(true);
                SearchPanel panel = findParent(SearchPanel.class);
//                panel.displayedSearchItemsModelReset();
                panel.searchPerformed(target);
            }
        });
        return choices;
    }

    private IModel<List<QName>> getSortedAvailableData() {
        QNameObjectTypeChoiceRenderer qNameObjectTypeChoiceRenderer = new QNameObjectTypeChoiceRenderer();
        List<QName> values = getModelObject().getAvailableValues();
        values.sort((q1, q2) -> {
            String displayValue1 = qNameObjectTypeChoiceRenderer.getDisplayValue(q1).toString();
            String displayValue2 = qNameObjectTypeChoiceRenderer.getDisplayValue(q2).toString();
            return displayValue1.compareToIgnoreCase(displayValue2);
        });
        return Model.ofList(values);
    }

}

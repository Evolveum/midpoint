/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search.panel;

import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.impl.component.search.wrapper.ObjectTypeSearchItemWrapper;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.input.QNameObjectTypeChoiceRenderer;

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
        return new LoadableDetachableModel<>() {

            @Override
            protected List<QName> load() {
                List<QName> values = getModelObject().getAvailableValues();
                WebComponentUtil.sortObjectTypeList(values);
                return values;
            }
        };
    }
}

/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.search;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.model.IModel;

import java.io.Serializable;
import java.util.List;

/**
 * @author Viliam Repan (lazyman)
 */
abstract class SearchPopupPanel<T extends Serializable> extends BasePanel<DisplayableValue<T>> {

    private static final long serialVersionUID = 1L;
    private static final String ID_REMOVE = "remove";
    private static final String ID_ADD = "add";

    public SearchPopupPanel(String id, IModel<DisplayableValue<T>> model) {
        super(id, model);

        initButtons();
    }

    private void initButtons() {
        AjaxLink<Void> remove = new AjaxLink<Void>(ID_REMOVE) {

           private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                addOrRemovePerformed(target, false);
            }
        };
        add(remove);

        AjaxLink<Void> add = new AjaxLink<Void>(ID_ADD) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                addOrRemovePerformed(target, true);
            }
        };
        add(add);
    }

    private void addOrRemovePerformed(AjaxRequestTarget target, boolean add) {
        SearchItemPanel<T> panel = findParent(SearchItemPanel.class);

//        SearchItemPopoverDto<T> dto = panel.getPopoverModel().getObject();
//        List<DisplayableValue<T>> values = dto.getValues();
//
//        if (add) {
//            values.add(createNewValue(panel.isReferenceDefinition()));
//        } else {
//            DisplayableValue<T> val = getModelObject();
//            values.remove(val);
//
//            if (values.isEmpty()) {
//                values.add(createNewValue(panel.isReferenceDefinition()));
//            }
//        }
//
//        panel.updatePopupBody(target);
    }

    private SearchValue<T> createNewValue(boolean isReference) {
        if (isReference) {
            return (SearchValue<T>) new SearchValue<ObjectReferenceType>(new ObjectReferenceType());
        }

        return new SearchValue<>();
    }

}

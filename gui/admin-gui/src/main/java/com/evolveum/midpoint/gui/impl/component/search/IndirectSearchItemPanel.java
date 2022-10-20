/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchBoxScopeType;

import org.apache.wicket.model.PropertyModel;

public class IndirectSearchItemPanel extends AbstractSearchItemPanel<IndirectSearchItemWrapper> {

    public IndirectSearchItemPanel(String id, IModel<IndirectSearchItemWrapper> searchItemModel) {
        super(id, searchItemModel);
    }

    @Override
    protected Component initSearchItemField() {
        List<Boolean> choices = new ArrayList<>();
        choices.add(Boolean.TRUE);
        choices.add(Boolean.FALSE);
        DropDownChoicePanel inputPanel = new DropDownChoicePanel(ID_SEARCH_ITEM_FIELD,
                new PropertyModel<>(getModel(), RelationSearchItemWrapper.F_SEARCH_CONFIG + "." + SearchConfigurationWrapper.F_INDIRECT),
                Model.ofList(choices),
                new ChoiceRenderer<Boolean>() {
            private static final long serialVersionUID = 1L;

            @Override
            public Object getDisplayValue(Boolean val) {
                if (val) {
                    return getPageBase().createStringResource("Boolean.TRUE").getString();
                }
                return getPageBase().createStringResource("Boolean.FALSE").getString();
            }
        }, false);
        inputPanel.getBaseFormComponent().add(new EnableBehaviour(() -> getModelObject().getSearchConfig().isSearchScope(SearchBoxScopeType.SUBTREE)));
        return inputPanel;
    }

}

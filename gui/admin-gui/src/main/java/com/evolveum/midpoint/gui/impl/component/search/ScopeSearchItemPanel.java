/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchBoxScopeType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import java.util.Arrays;

public class ScopeSearchItemPanel extends AbstractSearchItemPanel<ScopeSearchItemWrapper> {

    public ScopeSearchItemPanel(String id, IModel<ScopeSearchItemWrapper> searchItemModel) {
        super(id, searchItemModel);
    }

    @Override
    protected Component initSearchItemField() {
        DropDownChoicePanel inputPanel = new DropDownChoicePanel(ID_SEARCH_ITEM_FIELD,
                new PropertyModel(getModel(), ScopeSearchItemWrapper.F_SEARCH_CONFIG + "." + SearchConfigurationWrapper.F_SCOPE),
                Model.of(Arrays.asList(SearchBoxScopeType.values())), new EnumChoiceRenderer(), false);
        inputPanel.setOutputMarkupId(true);
        return inputPanel;
    }

}

/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search.panel;

import com.evolveum.midpoint.gui.impl.component.search.wrapper.ScopeSearchItemWrapper;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchBoxScopeType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import java.util.Arrays;

public class ScopeSearchItemPanel extends SingleSearchItemPanel<ScopeSearchItemWrapper> {

    public ScopeSearchItemPanel(String id, IModel<ScopeSearchItemWrapper> searchItemModel) {
        super(id, searchItemModel);
    }

    @Override
    protected Component initSearchItemField(String id) {
        DropDownChoicePanel inputPanel = new DropDownChoicePanel(id,
                new PropertyModel(getModel(), ScopeSearchItemWrapper.F_VALUE),
                Model.of(Arrays.asList(SearchBoxScopeType.values())), new EnumChoiceRenderer(), false);
        inputPanel.getBaseFormComponent().add(new OnChangeAjaxBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget ajaxRequestTarget) {
                onScopeItemChanged(ajaxRequestTarget);
            }
        });
        return inputPanel;
    }

    protected void onScopeItemChanged(AjaxRequestTarget target) {

    }
}

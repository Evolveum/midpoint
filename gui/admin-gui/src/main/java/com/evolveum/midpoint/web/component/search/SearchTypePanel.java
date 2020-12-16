/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search;

import com.evolveum.midpoint.gui.api.component.autocomplete.AutoCompleteTextPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.page.admin.configuration.component.DebugSearchFragment;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.dto.DebugSearchDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;

import java.util.List;

/**
 * @author lskublik
 */
public class SearchTypePanel<C extends Containerable> extends AbstractSearchItemPanel<ContainerTypeSearchItem<C>> {

    private static final long serialVersionUID = 1L;

    private static final String ID_SEARCH_ITEM_FIELD = "searchItemField";

    public SearchTypePanel(String id, IModel<ContainerTypeSearchItem<C>> model) {
        super(id, model);
    }

    protected void initSearchItemField(WebMarkupContainer searchItemContainer) {
        Component searchItemField = new WebMarkupContainer(ID_SEARCH_ITEM_FIELD);
        ContainerTypeSearchItem<C> item = getModelObject();
        if (item != null && item.getAllowedValues(getPageBase()) != null) {
            List<DisplayableValue<Class<? extends C>>> allowedValues = item.getAllowedValues(getPageBase());
            if (allowedValues != null && !allowedValues.isEmpty()) {
                IModel<List<DisplayableValue<?>>> choices = new ListModel(item.getAllowedValues(getPageBase()));
                searchItemField = createDropDownChoices(ID_SEARCH_ITEM_FIELD, new PropertyModel<>(getModel(), ContainerTypeSearchItem.F_TYPE), choices, false);
            }
        }

        if (searchItemField instanceof InputPanel && !(searchItemField instanceof AutoCompleteTextPanel)) {
            ((InputPanel) searchItemField).getBaseFormComponent().add(AttributeAppender.append("style", "width: 175px; max-width: 400px !important;"));
            ((InputPanel) searchItemField).getBaseFormComponent().add(new OnChangeAjaxBehavior() {
                private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    searchPerformed(target);
                }
            });
        }
        searchItemField.setOutputMarkupId(true);
        searchItemContainer.add(searchItemField);
    }

    @Override
    protected boolean canRemoveSearchItem() {
        return false;
    }
}

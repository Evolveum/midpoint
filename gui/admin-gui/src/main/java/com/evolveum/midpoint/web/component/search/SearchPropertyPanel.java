/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search;

import java.io.Serializable;
import java.util.List;

import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchBoxScopeType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;

import com.evolveum.midpoint.gui.api.component.autocomplete.AutoCompleteTextPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;

/**
 * @author Viliam Repan (lazyman)
 * @author lskublik
 */
public class SearchPropertyPanel<T extends Serializable> extends AbstractSearchItemPanel<PropertySearchItem<T>> {

    private static final long serialVersionUID = 1L;

    private static final String ID_SEARCH_ITEM_FIELD = "searchItemField";

    public SearchPropertyPanel(String id, IModel<PropertySearchItem<T>> model) {
        super(id, model);
    }

    @Override
    protected void onConfigure() {
        super.onConfigure();
        PropertySearchItem<T> item = getModelObject();
        if (!item.isEditWhenVisible()) {
            return;
        }
        item.setEditWhenVisible(false);
    }


    protected void initSearchItemField(WebMarkupContainer searchItemContainer) {
        Component searchItemField;
        PropertySearchItem<T> item = getModelObject();
        IModel<List<DisplayableValue<?>>> choices = null;
        switch (item.getSearchItemType()) {
            case REFERENCE:
                searchItemField = new ReferenceValueSearchPanel(ID_SEARCH_ITEM_FIELD,
                        new PropertyModel<>(getModel(), "value.value"),
                        (PrismReferenceDefinition) item.getDefinition().getDef()){
                    @Override
                    protected void referenceValueUpdated(ObjectReferenceType ort, AjaxRequestTarget target) {
                        searchPerformed(target);
                    }

                    @Override
                    public Boolean isItemPanelEnabled() {
                        return item.isEnabled();
                    }

                    @Override
                    protected boolean isAllowedNotFoundObjectRef() {
                        return item.getSearch().getTypeClass().equals(AuditEventRecordType.class);
                    }
                };
                break;
            case BOOLEAN:
                choices = (IModel) createBooleanChoices();
            case ENUM:
                if (choices == null) {
                    choices = new ListModel(item.getAllowedValues(getPageBase()));
                }
                searchItemField = createDropDownChoices(ID_SEARCH_ITEM_FIELD, new PropertyModel<>(getModel(), "value"), choices, true);
                ((InputPanel) searchItemField).getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior() {
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        searchPerformed(target);
                    }
                });
                break;
            case DATE:
                searchItemField = new DateIntervalSearchPanel(ID_SEARCH_ITEM_FIELD,
                        new PropertyModel(getModel(), "fromDate"),
                        new PropertyModel(getModel(), "toDate")){
                    @Override
                    public void searchPerformed(AjaxRequestTarget target) {
                        SearchPropertyPanel.this.searchPerformed(target);
                    }
                };
                break;
            case ITEM_PATH:
                searchItemField = new ItemPathSearchPanel(ID_SEARCH_ITEM_FIELD,
                        new PropertyModel(getModel(), "value.value")){
                    @Override
                    public void searchPerformed(AjaxRequestTarget target) {
                        SearchPropertyPanel.this.searchPerformed(target);
                    }
                };
                break;
            case TEXT:
                PrismObject<LookupTableType> lookupTable = WebComponentUtil.findLookupTable(item.getDefinition().getDef(), getPageBase());
                if (lookupTable != null) {
                    searchItemField = createAutoCompetePanel(ID_SEARCH_ITEM_FIELD, new PropertyModel<>(getModel(), "value.value"),
                            lookupTable.asObjectable());
                } else {
                    searchItemField = new TextPanel<String>(ID_SEARCH_ITEM_FIELD, new PropertyModel<>(getModel(), "value.value"));
                }
                break;
            default:
                searchItemField = new TextPanel<String>(ID_SEARCH_ITEM_FIELD, new PropertyModel<>(getModel(), "value"));
        }
        if (searchItemField instanceof InputPanel && !(searchItemField instanceof AutoCompleteTextPanel)) {
            ((InputPanel) searchItemField).getBaseFormComponent().add(WebComponentUtil.getSubmitOnEnterKeyDownBehavior("searchSimple"));
            ((InputPanel) searchItemField).getBaseFormComponent().add(AttributeAppender.append("style", "width: 140px; max-width: 400px !important;"));
            ((InputPanel) searchItemField).getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
            ((InputPanel) searchItemField).getBaseFormComponent().add(new EnableBehaviour(() -> item.isEnabled()));
        }
        searchItemField.setOutputMarkupId(true);
        searchItemContainer.add(searchItemField);
    }
}

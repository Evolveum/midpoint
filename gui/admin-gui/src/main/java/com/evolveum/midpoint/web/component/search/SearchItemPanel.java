/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.web.util.InfoTooltipBehavior;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.autocomplete.AutoCompleteTextPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.input.CheckPanel;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;

/**
 * @author Viliam Repan (lazyman)
 * @author lskublik
 */
public class SearchItemPanel<T extends Serializable> extends AbstractSearchItemPanel<PropertySearchItem<T>, T> {

    private static final long serialVersionUID = 1L;

    private static final String ID_SEARCH_ITEM_FIELD = "searchItemField";

    public SearchItemPanel(String id, IModel<PropertySearchItem<T>> model) {
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
        IModel<List<DisplayableValue<T>>> choices = null;
        switch (item.getType()) {
            case REFERENCE:
                searchItemField = new ReferenceValueSearchPanel(ID_SEARCH_ITEM_FIELD,
                        new PropertyModel<>(getModel(), "value.value"),
                        (PrismReferenceDefinition) item.getDefinition().getDef()){
                    @Override
                    protected void referenceValueUpdated(ObjectReferenceType ort, AjaxRequestTarget target) {
                        searchPerformed(target);
                    }
                };
                break;
            case BOOLEAN:
                choices = (IModel) createBooleanChoices();
            case ENUM:
                if (choices == null) {
                    choices = new ListModel<>(item.getAllowedValues(getPageBase()));
                }
                searchItemField = new DropDownChoicePanel<>(ID_SEARCH_ITEM_FIELD, new PropertyModel<>(getModel(), "value"),
                        choices, new IChoiceRenderer<DisplayableValue>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public Object getDisplayValue(DisplayableValue val) {
                        return val.getLabel();
                    }

                    @Override
                    public String getIdValue(DisplayableValue val, int index) {
                        return Integer.toString(index);
                    }

                    @Override
                    public DisplayableValue getObject(String id, IModel<? extends List<? extends DisplayableValue>> choices) {
                        return StringUtils.isNotBlank(id) ? choices.getObject().get(Integer.parseInt(id)) : null;
                    }
                }, true);
                break;
            case DATE:
                searchItemField = new DateIntervalSearchPanel(ID_SEARCH_ITEM_FIELD,
                        new PropertyModel(getModel(), "fromDate"),
                        new PropertyModel(getModel(), "toDate"));
                break;
            case ITEM_PATH:
                searchItemField = new ItemPathSearchPanel(ID_SEARCH_ITEM_FIELD,
                        new PropertyModel(getModel(), "value.value"));
                break;
            case TEXT:
                PrismObject<LookupTableType> lookupTable = WebComponentUtil.findLookupTable(item.getDefinition().getDef(), getPageBase());
                if (lookupTable != null) {
                    searchItemField = new AutoCompleteTextPanel<String>(ID_SEARCH_ITEM_FIELD, new PropertyModel<>(getModel(), "value.value"), String.class,
                            true, lookupTable.asObjectable()) {

                        private static final long serialVersionUID = 1L;

                        @Override
                        public Iterator<String> getIterator(String input) {
                            return WebComponentUtil.prepareAutoCompleteList(lookupTable.asObjectable(), input,
                                    ((PageBase) getPage()).getLocalizationService()).iterator();
                        }
                    };

                    ((AutoCompleteTextPanel) searchItemField).getBaseFormComponent().add(new Behavior() {

                        private static final long serialVersionUID = 1L;

                        @Override
                        public void bind(Component component) {
                            super.bind(component);

                            component.add(AttributeModifier.replace("onkeydown",
                                    Model.of(
                                            "if (event.keyCode == 13){"
                                                    + "var autocompletePopup = document.getElementsByClassName(\"wicket-aa-container\");"
                                                    + "if(autocompletePopup != null && autocompletePopup[0].style.display == \"none\"){"
                                                    + "$('[about=\"searchSimple\"]').click();}}"
                                    )));
                        }
                    });
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
        }
        searchItemField.setOutputMarkupId(true);
        searchItemContainer.add(searchItemField);
    }
}

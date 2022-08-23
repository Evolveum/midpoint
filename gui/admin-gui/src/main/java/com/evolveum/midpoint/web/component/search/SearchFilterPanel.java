/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search;

import com.evolveum.midpoint.gui.api.component.autocomplete.AutoCompleteTextPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.MutablePrismReferenceDefinition;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.web.component.input.CheckPanel;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;

import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ParameterType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchFilterParameterType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.List;

/**
 * @author Viliam Repan (lazyman)
 * @author lskublik
 */
public class SearchFilterPanel extends AbstractSearchItemPanel<FilterSearchItem> {

    private static final long serialVersionUID = 1L;

    private static final String ID_SEARCH_ITEM_FIELD = "searchItemField";
    private static final String ID_CHECK_DISABLE_FIELD = "checkDisable";

    public SearchFilterPanel(String id, IModel<FilterSearchItem> model) {
        super(id, model);
    }

    protected void initSearchItemField(WebMarkupContainer searchItemContainer) {
        CheckPanel checkPanel = new CheckPanel(ID_CHECK_DISABLE_FIELD, new PropertyModel<>(getModel(), FilterSearchItem.F_APPLY_FILTER));
        (checkPanel).getBaseFormComponent().add(new OnChangeAjaxBehavior() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget ajaxRequestTarget) {
                searchPerformed(ajaxRequestTarget);
            }
        });
        checkPanel.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        checkPanel.add(new VisibleBehaviour(this::canRemoveSearchItem));

        checkPanel.setOutputMarkupId(true);
        searchItemContainer.add(checkPanel);

        ParameterType functionParameter = getModelObject().getPredefinedFilter().getParameter();
        QName returnType = functionParameter != null ? functionParameter.getType() : null;
        Component inputPanel;
        if (returnType == null) {
            inputPanel = new WebMarkupContainer(ID_SEARCH_ITEM_FIELD);
        } else {
            Class<?> inputClass = getPrismContext().getSchemaRegistry().determineClassForType(returnType);
            Validate.notNull(inputClass, "Couldn't find class for type " + returnType);
            SearchItem.Type inputType = getModelObject().getInputType(inputClass, getPageBase());
            IModel<List<DisplayableValue<?>>> choices = null;
            switch (inputType) {
                case REFERENCE:
                    SearchFilterParameterType parameter = getModelObject().getPredefinedFilter().getParameter();
                    MutablePrismReferenceDefinition def = null;
                    if (parameter != null) {
                        Class<?> clazz = getPrismContext().getSchemaRegistry().determineClassForType(parameter.getType());
                        QName type = getPrismContext().getSchemaRegistry().determineTypeForClass(clazz);
                        def = getPrismContext().definitionFactory().createReferenceDefinition(
                                new QName(parameter.getName()), type);
                        def.setTargetTypeName(parameter.getTargetType());
                    }
                    inputPanel = new ReferenceValueSearchPanel(ID_SEARCH_ITEM_FIELD,
                            new PropertyModel<>(getModel(), FilterSearchItem.F_INPUT_VALUE), def);
                    break;
                case BOOLEAN:
                    choices = (IModel) createBooleanChoices();
                case ENUM:

                    if (choices == null) {
                        choices = CollectionUtils.isEmpty(getModelObject().getAllowedValues(getPageBase())) ?
                                createEnumChoices((Class<? extends Enum>) inputClass) : Model.ofList(getModelObject().getAllowedValues(getPageBase()));
                    }
                    if (choices != null) {
                        inputPanel = WebComponentUtil.createDropDownChoices(
                                ID_SEARCH_ITEM_FIELD, new PropertyModel(getModel(), FilterSearchItem.F_INPUT), (IModel)choices, true, getPageBase());
                        break;
                    }
                case DATE:
                    inputPanel = new DateSearchPanel(ID_SEARCH_ITEM_FIELD,
                            new IModel<XMLGregorianCalendar>() {
                                @Override
                                public XMLGregorianCalendar getObject() {
                                    FilterSearchItem filterSearchItem = getModelObject();
                                    if (filterSearchItem == null || filterSearchItem.getInput() == null) {
                                        return null;
                                    }
                                    return (XMLGregorianCalendar) filterSearchItem.getInput().getValue();
                                }

                                @Override
                                public void setObject(XMLGregorianCalendar val) {
                                    FilterSearchItem filterSearchItem = getModelObject();
                                    if (filterSearchItem == null) {
                                        return;
                                    }
                                    if (val instanceof Serializable) {
                                        filterSearchItem.setInput(new SearchValue<>((Serializable) val));
                                    }
                                }
                            });
                    break;
                case ITEM_PATH:
                    inputPanel = new ItemPathSearchPanel(ID_SEARCH_ITEM_FIELD,
                            new PropertyModel(getModel(), FilterSearchItem.F_INPUT_VALUE));
                    break;
                case TEXT:
                    LookupTableType lookupTable = getModelObject().getLookupTable(getPageBase());
                    if (lookupTable != null) {
                        inputPanel = createAutoCompetePanel(ID_SEARCH_ITEM_FIELD, new PropertyModel<>(getModel(), FilterSearchItem.F_INPUT_VALUE),
                                lookupTable);
                        break;
                    }
                default:
                    inputPanel = new TextPanel<String>(ID_SEARCH_ITEM_FIELD, new PropertyModel<>(getModel(), FilterSearchItem.F_INPUT_VALUE));
            }
            if (inputPanel instanceof InputPanel && !(inputPanel instanceof AutoCompleteTextPanel)) {
                ((InputPanel) inputPanel).getBaseFormComponent().add(WebComponentUtil.getSubmitOnEnterKeyDownBehavior("searchSimple"));
                ((InputPanel) inputPanel).getBaseFormComponent().add(AttributeAppender.append("style", "width: 140px; max-width: 400px !important;"));
                ((InputPanel) inputPanel).getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
            }
            inputPanel.setOutputMarkupId(true);
            searchItemContainer.add(inputPanel);
        }
        searchItemContainer.add(inputPanel);
    }
}

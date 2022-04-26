/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search;

import com.evolveum.midpoint.gui.api.component.autocomplete.AutoCompleteTextPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.MutablePrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.web.component.input.CheckPanel;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.search.*;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
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
import java.util.Collections;
import java.util.List;

public class FilterSearchItemPanel extends AbstractSearchItemPanel<FilterSearchItemWrapper> {
    private static final String ID_CHECK_DISABLE_FIELD = "checkDisable";

    public FilterSearchItemPanel(String id, IModel<FilterSearchItemWrapper> searchItemModel) {
        super(id, searchItemModel);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initFilterSearchItemLayout();
    }

    private void initFilterSearchItemLayout() {
        CheckPanel checkPanel = new CheckPanel(ID_CHECK_DISABLE_FIELD, new PropertyModel<>(getModel(), FilterSearchItem.F_APPLY_FILTER));
        (checkPanel).getBaseFormComponent().add(new OnChangeAjaxBehavior() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget ajaxRequestTarget) {
                searchPerformed(ajaxRequestTarget);
            }
        });
        checkPanel.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        checkPanel.add(new VisibleBehaviour(this::isCheckPanelVisible));

        checkPanel.setOutputMarkupId(true);
        getSearchItemContainer().add(checkPanel);

    }

    private boolean isCheckPanelVisible() {
        return canRemoveSearchItem() && getModelObject().getFilter() != null;
    }

    @Override
    protected Component initSearchItemField() {
        QName returnType = getModelObject().getFilterParameterType();
        Component inputPanel;
        if (returnType == null) {
            inputPanel = new WebMarkupContainer(ID_SEARCH_ITEM_FIELD);
        } else {
            Class<?> inputClass = getPrismContext().getSchemaRegistry().determineClassForType(returnType);
            Validate.notNull(inputClass, "Couldn't find class for type " + returnType);
            SearchItem.Type inputType = getInputType(inputClass, getPageBase());
            IModel<List<DisplayableValue<?>>> choices = null;
            switch (inputType) {
                case REFERENCE:
                    MutablePrismReferenceDefinition def = null;
                    Class<?> clazz = getPrismContext().getSchemaRegistry().determineClassForType(returnType);
                    QName type = getPrismContext().getSchemaRegistry().determineTypeForClass(clazz);
                    def = getPrismContext().definitionFactory().createReferenceDefinition(
                            new QName(getModelObject().getFilterParameterName()), type);
                    def.setTargetTypeName(returnType);
                    inputPanel = new ReferenceValueSearchPanel(ID_SEARCH_ITEM_FIELD,
                            new PropertyModel<>(getModel(), FilterSearchItem.F_INPUT_VALUE), def);
                    break;
                case BOOLEAN:
                    choices = (IModel) createBooleanChoices();
                case ENUM:

                    if (choices == null) {
                        List<DisplayableValue<?>> availableValues = getAllowedValues(getPageBase());
                        choices = CollectionUtils.isEmpty(availableValues) ?
                                createEnumChoices((Class<? extends Enum>) inputClass) : Model.ofList(availableValues);
                    }
                    if (choices != null) {
                        inputPanel = WebComponentUtil.createDropDownChoices(
                                ID_SEARCH_ITEM_FIELD, new PropertyModel(getModel(), FilterSearchItem.F_INPUT), (IModel)choices, true, getPageBase());
                        break;
                    }
                case DATE:
                    inputPanel = new DateSearchPanel(ID_SEARCH_ITEM_FIELD,
                            new PropertyModel(getModel(), FilterSearchItem.F_INPUT_VALUE));
                    break;
                case ITEM_PATH:
                    inputPanel = new ItemPathSearchPanel(ID_SEARCH_ITEM_FIELD,
                            new PropertyModel(getModel(), FilterSearchItem.F_INPUT_VALUE));
                    break;
                case TEXT:
                    LookupTableType lookupTable = getLookupTable(getPageBase());
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
        }
        return inputPanel;
    }

    private SearchItem.Type getInputType(Class clazz, PageBase pageBase) {
        if (CollectionUtils.isNotEmpty(getAllowedValues(pageBase))) {
            return SearchItem.Type.ENUM;
        }
        if (Boolean.class.isAssignableFrom(clazz) || boolean.class.isAssignableFrom(clazz)) {
            return SearchItem.Type.BOOLEAN;
        }
        if (Enum.class.isAssignableFrom(clazz)) {
            return SearchItem.Type.ENUM;
        }
        if (ObjectReferenceType.class.isAssignableFrom(clazz)) {
            return SearchItem.Type.REFERENCE;
        }
        if (ItemPathType.class.isAssignableFrom(clazz)) {
            return SearchItem.Type.ITEM_PATH;
        }
        if (XMLGregorianCalendar.class.isAssignableFrom(clazz)) {
            return SearchItem.Type.DATE;
        }
        return SearchItem.Type.TEXT;
    }

    public List<DisplayableValue<?>> getAllowedValues(PageBase pageBase) {
        if (getModelObject().getFilter() == null) {
            return Collections.EMPTY_LIST;
        }
        List<DisplayableValue<?>> values = WebComponentUtil.getAllowedValues(getModelObject().getAllowedValuesExpression(), pageBase);
        return values;
    }

    private LookupTableType getLookupTable(PageBase pageBase) {
        if (StringUtils.isNotEmpty(getModelObject().getAllowedValuesLookupTableOid())) {
            PrismObject<LookupTableType> lookupTable =
                    WebComponentUtil.findLookupTable((new ObjectReferenceType().oid(getModelObject().getAllowedValuesLookupTableOid())).asReferenceValue(), pageBase);
            if (lookupTable != null) {
                return lookupTable.asObjectable();
            }
        }
        return null;
    }
}

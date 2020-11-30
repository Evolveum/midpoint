/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search;

import com.evolveum.midpoint.gui.api.component.autocomplete.AutoCompleteTextPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.web.component.input.CheckPanel;
import com.evolveum.midpoint.web.component.input.TextPanel;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionParameterType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ParameterType;

import org.apache.commons.lang3.Validate;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Viliam Repan (lazyman)
 * @author lskublik
 */
public class SearchFilterPanel extends AbstractSearchItemPanel<FilterSearchItem, String> {

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

        checkPanel.setOutputMarkupId(true);
        searchItemContainer.add(checkPanel);

        ParameterType functionParameter = getModelObject().getPredefinedFilter().getParameter();
        QName returnType = functionParameter !=null ? functionParameter.getType() : null;
        Component inputPanel;
        if (returnType == null) {
            inputPanel = new WebMarkupContainer(ID_SEARCH_ITEM_FIELD);
        } else {
            Class<?> inputClass = getPrismContext().getSchemaRegistry().determineClassForType(returnType);
            Validate.notNull(inputClass, "Couldn't find class for type " + returnType);
            SearchItem.Type inputType = getModelObject().getInputType(inputClass);
            IModel<List<DisplayableValue>> choices = null;
            switch (inputType) {
                case BOOLEAN:
                    choices = (IModel) createBooleanChoices();
                case ENUM:

                    if (choices == null) {
                        choices = createEnumChoises((Class<? extends Enum>) inputClass);
                    }
                    if (choices != null) {
                        inputPanel = createDropDownChoices(ID_SEARCH_ITEM_FIELD, new PropertyModel<>(getModel(), FilterSearchItem.F_INPUT), choices);
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

    private IModel<List<DisplayableValue>> createEnumChoises(Class<? extends Enum> inputClass) {
        Enum[] enumConstants = inputClass.getEnumConstants();
        List<DisplayableValue> list = new ArrayList<>();
        for(int i = 0; i < enumConstants.length; i++){
            list.add(new SearchValue<>(enumConstants[i], getString(enumConstants[i])));
        }
        return Model.ofList(list);

    }
}

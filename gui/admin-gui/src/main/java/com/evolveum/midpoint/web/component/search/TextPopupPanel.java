/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.search;

import com.evolveum.midpoint.gui.api.component.autocomplete.AutoCompleteTextPanel;
import com.evolveum.midpoint.gui.api.component.autocomplete.LookupTableConverter;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.web.model.LookupPropertyModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableRowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteSettings;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.convert.ConversionException;
import org.apache.wicket.util.convert.IConverter;

import java.io.Serializable;
import java.util.*;

/**
 * @author Viliam Repan (lazyman)
 */
public class TextPopupPanel<T extends Serializable> extends SearchPopupPanel<T> {

   private static final long serialVersionUID = 1L;

    private static final String ID_TEXT_INPUT = "textInput";

    private static final int MAX_ITEMS = 10;

    private PrismObject<LookupTableType> lookup;

    public TextPopupPanel(String id, IModel<DisplayableValue<T>> model, PrismObject<LookupTableType> lookup) {
        super(id, model);
        this.lookup = lookup;

        initLayout();
    }

    private void initLayout() {
        final TextField input = initTextField();

        input.add(new AjaxFormComponentUpdatingBehavior("blur") {

            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                //nothing to do, just update model data
            }
        });
        input.add(new Behavior() {

            private static final long serialVersionUID = 1L;

            @Override
            public void bind(Component component) {
                super.bind(component);

                component.add(AttributeModifier.replace("onkeydown",
                        Model.of("if(event.keyCode == 13) {event.preventDefault();}")));
            }
        });
        input.setOutputMarkupId(true);
        add(input);
    }

    private TextField initTextField() {
//        IModel data = new PropertyModel(getModel(), SearchValue.F_VALUE);

        if (lookup == null) {
            return new TextField(ID_TEXT_INPUT, new PropertyModel(getModel(), SearchValue.F_VALUE));
        }

        //TODO: displayName
//        LookupPropertyModel<String> lookupPropertyModel = new LookupPropertyModel<String>(getModel(), SearchValue.F_VALUE, lookup.asObjectable()) {
//
//            private static final long serialVersionUID = 1L;
//
//            @Override
//            public boolean isSupportsDisplayName() {
//                return true;
//            }
//        };

        AutoCompleteSettings settings = new AutoCompleteSettings();
        settings.setShowListOnEmptyInput(true);


        return new AutoCompleteTextField<String>(ID_TEXT_INPUT, new PropertyModel<>(getModel(), SearchValue.F_VALUE), settings) {

            private static final long serialVersionUID = 1L;
            @Override
            protected Iterator<String> getChoices(String input) {
                return prepareAutoCompleteList(input).iterator();
            }

            @Override
            public <C> IConverter<C> getConverter(Class<C> type) {
                IConverter<C> converter = super.getConverter(type);
                if (lookup == null) {
                    return converter;
                }

                return new LookupTableConverter(converter, lookup.asObjectable(), this, false){
                    @Override
                    public Object convertToObject(String value, Locale locale) throws ConversionException {
                        PropertyModel<Object> label = new PropertyModel<>(TextPopupPanel.this.getModelObject(), SearchValue.F_LABEL);
                        label.setObject(value);
                        return super.convertToObject(value, locale);
                    }
                };

            }

        };
    }


    private List<String> prepareAutoCompleteList(String input) {
        List<String> values = new ArrayList<>();

        if (lookup == null || lookup.asObjectable().getRow() == null) {
            return values;
        }

        List<LookupTableRowType> rows = new ArrayList<>();
        rows.addAll(lookup.asObjectable().getRow());

        Collections.sort(rows, new Comparator<LookupTableRowType>() {

            @Override
            public int compare(LookupTableRowType o1, LookupTableRowType o2) {
                String s1 = WebComponentUtil.getOrigStringFromPoly(o1.getLabel());
                String s2 = WebComponentUtil.getOrigStringFromPoly(o2.getLabel());

                return String.CASE_INSENSITIVE_ORDER.compare(s1, s2);
            }
        });

        for (LookupTableRowType row : rows) {
            String rowLabel = WebComponentUtil.getOrigStringFromPoly(row.getLabel());
            if (StringUtils.isEmpty(input) || rowLabel.toLowerCase().startsWith(input.toLowerCase())) {
                values.add(rowLabel);
            }

            if (values.size() > MAX_ITEMS) {
                break;
            }
        }

        return values;
    }
}

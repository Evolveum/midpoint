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
import java.util.Locale;

import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteSettings;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteTextField;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.convert.ConversionException;
import org.apache.wicket.util.convert.IConverter;

import com.evolveum.midpoint.gui.api.component.autocomplete.LookupTableConverter;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableRowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;

/**
 * @author Viliam Repan (lazyman)
 */
public class TextPopupPanel<T extends Serializable> extends SearchPopupPanel<T> {

    private static final long serialVersionUID = 1L;

    private static final String ID_TEXT_INPUT = "input";

    private static final int MAX_ITEMS = 10;

    private final String lookupOid;

    public TextPopupPanel(String id, IModel<DisplayableValue<T>> model, String lookupOid) {
        super(id, model);
        this.lookupOid = lookupOid;

        initLayout();
    }

    private void initLayout() {
        final TextField input = initTextField();

        input.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        input.setOutputMarkupId(true);
        add(input);
    }

    private TextField initTextField() {
//        IModel data = new PropertyModel(getModelService(), SearchValue.F_VALUE);

        if (lookupOid == null) {
            return new TextField(ID_TEXT_INPUT, new PropertyModel(getModel(), SearchValue.F_VALUE));
        }

        //TODO: displayName
//        LookupPropertyModel<String> lookupPropertyModel = new LookupPropertyModel<String>(getModelService(), SearchValue.F_VALUE, lookup.asObjectable()) {
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

        AutoCompleteTextField<String> textField = new AutoCompleteTextField<String>(ID_TEXT_INPUT, new PropertyModel<>(getModel(), SearchValue.F_VALUE), settings) {

            private static final long serialVersionUID = 1L;

            @Override
            protected Iterator<String> getChoices(String input) {
                return prepareAutoCompleteList(input).iterator();
            }

            @Override
            public <C> IConverter<C> getConverter(Class<C> type) {
                IConverter<C> converter = super.getConverter(type);
                if (lookupOid == null) {
                    return converter;
                }

                return new LookupTableConverter(converter, lookupOid, this, false) {
                    @Override
                    public Object convertToObject(String value, Locale locale) throws ConversionException {
                        PropertyModel<Object> label = new PropertyModel<>(TextPopupPanel.this.getModelObject(), SearchValue.F_LABEL);
                        label.setObject(value);
                        return super.convertToObject(value, locale);
                    }
                };

            }

        };
        return textField;
    }

    public FormComponent getTextField() {
        return (FormComponent) get(ID_TEXT_INPUT);
    }

    private List<String> prepareAutoCompleteList(String input) {

        List<String> values = new ArrayList<>();

        if (lookupOid == null) {
            return values;
        }

        LookupTableType lookup = WebModelServiceUtils.loadLookupTable(lookupOid, getPageBase());
        List<LookupTableRowType> rows = new ArrayList<>(lookup.getRow());
        rows.sort((o1, o2) -> {
            String s1 = WebComponentUtil.getOrigStringFromPoly(o1.getLabel());
            String s2 = WebComponentUtil.getOrigStringFromPoly(o2.getLabel());

            return String.CASE_INSENSITIVE_ORDER.compare(s1, s2);
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

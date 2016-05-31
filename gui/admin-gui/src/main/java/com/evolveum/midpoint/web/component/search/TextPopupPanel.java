/*
 * Copyright (c) 2010-2016 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.component.search;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.DisplayableValue;
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

import java.util.*;

/**
 * @author Viliam Repan (lazyman)
 */
public class TextPopupPanel extends SearchPopupPanel<DisplayableValue> {

    private static final String ID_TEXT_INPUT = "textInput";

    private static final int MAX_ITEMS = 10;

    private PrismObject<LookupTableType> lookup;

    public TextPopupPanel(String id, IModel<DisplayableValue> model, PrismObject<LookupTableType> lookup) {
        super(id, model);
        this.lookup = lookup;

        initLayout();
    }

    private void initLayout() {
        final TextField input = initTextField();

        input.add(new AjaxFormComponentUpdatingBehavior("blur") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                //nothing to do, just update model data
            }
        });
        input.add(new Behavior() {

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
        IModel data = new PropertyModel(getModel(), SearchValue.F_VALUE);

        if (lookup == null) {
            return new TextField(ID_TEXT_INPUT, data);
        }

        AutoCompleteSettings settings = new AutoCompleteSettings();
        settings.setShowListOnEmptyInput(true);

        return new AutoCompleteTextField(ID_TEXT_INPUT, data, settings) {

            @Override
            protected Iterator getChoices(String input) {
                return prepareAutoCompleteList(input).iterator();
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

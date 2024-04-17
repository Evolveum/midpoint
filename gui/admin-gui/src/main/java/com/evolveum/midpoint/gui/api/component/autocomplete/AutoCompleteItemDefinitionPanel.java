/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component.autocomplete;

import java.io.Serial;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteTextField;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.prism.ItemDefinition;

public class AutoCompleteItemDefinitionPanel extends AbstractAutoCompletePanel {

    @Serial private static final long serialVersionUID = 1L;
    private static final String ID_INPUT = "input";

     public AutoCompleteItemDefinitionPanel(String id, final IModel<ItemDefinition<?>> model) {
            super(id);
            initLayout(model);
        }

    private void initLayout(final IModel<ItemDefinition<?>> model) {
        final Model<String> itemDefinitionAsStringModel = new Model<>(null);
        AutoCompleteTextField<String> input = new AutoCompleteTextField<>(
                ID_INPUT, itemDefinitionAsStringModel, String.class, createAutoCompleteSettings()) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected Iterator<String> getChoices(String input) {
                List<String> defsAsString = new ArrayList<>();
                for (ItemDefinition<?> def : listChoices(input).values()) {
                    defsAsString.add(def.getItemName().getLocalPart());
                }
                return defsAsString.iterator();

            }

        };

         input.add(new OnChangeAjaxBehavior() {
                @Serial private static final long serialVersionUID = 1L;

                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    onUpdateAutoComplete(target, itemDefinitionAsStringModel, model);
                }
            });

        add(input);
    }

    protected void onUpdateAutoComplete(AjaxRequestTarget target, final Model<String> itemDefinitionAsStringModel, final IModel<ItemDefinition<?>> model) {
        String newValue = itemDefinitionAsStringModel.getObject();
        if (StringUtils.isNotBlank(newValue)){
            ItemDefinition<?> def = listChoices("").get(newValue);
            if (def != null) {
                model.setObject(def);
            }
        }
    }

    protected Map<String, ItemDefinition<?>> listChoices(String input){
        return new HashMap<>();
    }

    @Override
    public FormComponent<?> getBaseFormComponent() {
        return (FormComponent<?>) get(ID_INPUT);
    }

}

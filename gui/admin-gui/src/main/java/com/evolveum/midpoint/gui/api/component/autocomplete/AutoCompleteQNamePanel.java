/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.api.component.autocomplete;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteSettings;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteTextField;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.namespace.QName;

/**
 * Autocomplete field for QNames.
 *
 * For now it assumes that local part of the QNames will be unique (e.g. for object classes)
 *
 * TODO: prefixes, URL formatting, etc.
 *
 *  @author semancik
 * */
public abstract class AutoCompleteQNamePanel<T extends QName> extends AbstractAutoCompletePanel {
    private static final long serialVersionUID = 1L;

    private static final String ID_INPUT = "input";
    private Map<String, T> choiceMap = null;

    private IModel<T> model;

    public AutoCompleteQNamePanel(String id, final IModel<T> model) {
        super(id);
        this.model = model;
        initLayout(model);
    }

    protected boolean alwaysReload() {
        return false;
    }

    private void initLayout(final IModel<T> model) {
        setOutputMarkupId(true);

        AutoCompleteSettings autoCompleteSettings = createAutoCompleteSettings();
        final IModel<String> stringModel = new Model<String>() {

            @Override
            public void setObject(String object) {
                super.setObject(object);
                model.setObject(convertToQname(object));
            }

            @Override
            public String getObject() {
                return (model.getObject() != null) ? model.getObject().getLocalPart() : null;
            }
        };

        // The inner autocomplete field is always String. Non-string auto-complete fields are problematic
        final AutoCompleteTextField<String> input = new AutoCompleteTextField<String>(ID_INPUT, stringModel, String.class, autoCompleteSettings) {
            private static final long serialVersionUID = 1L;

            @Override
            protected Iterator<String> getChoices(String input) {
                return getIterator(input);
            }


        };
        input.add(new OnChangeAjaxBehavior() {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                String inputString = stringModel.getObject();
                if (StringUtils.isBlank(inputString)) {
                    T modelObject = model.getObject();
                    if (modelObject != null) {
                        model.setObject(null);
                        AutoCompleteQNamePanel.this.onChange(target);
                    }
                } else {
                    T inputQName = convertToQname(stringModel.getObject());
                    if (inputQName == null) {
                        // We have some input, but it does not match any QName. Just do nothing.
                    } else {
                        T modelObject = model.getObject();
                        if (inputQName.equals(modelObject)) {
                            model.setObject(inputQName);
                            AutoCompleteQNamePanel.this.onChange(target);
                        }
                    }
                }
            }
        });
        add(input);
    }



    private Iterator<String> getIterator(String input) {
        Map<String, T> choiceMap = getChoiceMap();
        List<String> selected = new ArrayList<>(choiceMap.size());
        for (Entry<String, T> entry: choiceMap.entrySet()) {
            String key = entry.getKey();
            if (StringUtils.startsWithIgnoreCase(key, input.toLowerCase())) {
                selected.add(key);
            }
        }
        return selected.iterator();
    }

    private Map<String, T> getChoiceMap() {
        if (choiceMap == null || alwaysReload()) {
            Collection<T> choices = loadChoices();
            choiceMap = new HashMap<>();
            for (T choice: choices) {
                // TODO: smarter initialization of the map
                choiceMap.put(choice.getLocalPart(), choice);
            }
        }
        return choiceMap;
    }

    private T convertToQname(String input) {
        Map<String, T> choiceMap = getChoiceMap();
        return choiceMap.get(input);
    }


    public abstract Collection<T> loadChoices();

    protected void onChange(AjaxRequestTarget target) {
        // Nothing to do by default. For use in subclasses
    }

    @Override
    public FormComponent<String> getBaseFormComponent() {
        return (FormComponent<String>) get(ID_INPUT);
    }

}

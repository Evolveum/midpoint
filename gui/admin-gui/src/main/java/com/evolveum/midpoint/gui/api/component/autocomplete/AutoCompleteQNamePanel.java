/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
public abstract class AutoCompleteQNamePanel extends AbstractAutoCompletePanel {
	private static final long serialVersionUID = 1L;

	private static final String ID_INPUT = "input";
	private Map<String, QName> choiceMap = null;

    public AutoCompleteQNamePanel(String id, final IModel<QName> model) {
    	super(id);
    	initLayout(model);
    }

    private void initLayout(final IModel<QName> model) {
    	setOutputMarkupId(true);

        AutoCompleteSettings autoCompleteSettings = createAutoCompleteSettings();
        final IModel<String> stringModel = new Model<String>() {

			@Override
			public void setObject(String object) {
				super.setObject(object);
				model.setObject(convertToQname(object));
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
					QName modelObject = model.getObject();
					if (modelObject != null) {
						model.setObject(null);
						AutoCompleteQNamePanel.this.onChange(target);
					}
				} else {
					QName inputQName = convertToQname(stringModel.getObject());
					if (inputQName == null) {
						// We have some input, but it does not match any QName. Just do nothing.
					} else {
						QName modelObject = model.getObject();
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
    	Map<String, QName> choiceMap = getChoiceMap();
    	List<String> selected = new ArrayList<>(choiceMap.size());
    	for (Entry<String, QName> entry: choiceMap.entrySet()) {
    		String key = entry.getKey();
    		if (StringUtils.startsWithIgnoreCase(key, input.toLowerCase())) {
    			selected.add(key);
    		}
    	}
    	return selected.iterator();
    }

    private Map<String, QName> getChoiceMap() {
    	if (choiceMap == null) {
    		Collection<QName> choices = loadChoices();
    		choiceMap = new HashMap<>();
    		for (QName choice: choices) {
    			// TODO: smarter initialization of the map
    			choiceMap.put(choice.getLocalPart(), choice);
    		}
    	}
    	return choiceMap;
    }

	private QName convertToQname(String input) {
		Map<String, QName> choiceMap = getChoiceMap();
		return choiceMap.get(input);
	}


    public abstract Collection<QName> loadChoices();

    protected void onChange(AjaxRequestTarget target) {
    	// Nothing to do by default. For use in subclasses
    }

    @Override
    public FormComponent<String> getBaseFormComponent() {
        return (FormComponent<String>) get(ID_INPUT);
    }

}

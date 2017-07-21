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

import com.evolveum.midpoint.web.component.prism.PrismValuePanel;
import com.evolveum.midpoint.web.model.LookupPropertyModel;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.attributes.ThrottlingSettings;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteSettings;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteTextField;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.time.Duration;

import java.util.Iterator;

/**
 * Autocomplete field for Strings.
 * 
 * TODO: may need some work to properly support non-string values.
 * 
 *  @author shood
 *  @author semancik
 * */
public abstract class AutoCompleteTextPanel<T> extends AbstractAutoCompletePanel {
	private static final long serialVersionUID = 1L;
	
	private static final String ID_INPUT = "input";

    public AutoCompleteTextPanel(String id, final IModel<T> model, Class<T> type) {
    	super(id);

        AutoCompleteSettings autoCompleteSettings = createAutoCompleteSettings();
        
        // this has to be copied because the  AutoCompleteTextField dies if renderer=null
        final AutoCompleteTextField<T> input = new AutoCompleteTextField<T>(ID_INPUT, model, type, autoCompleteSettings) {
        	private static final long serialVersionUID = 1L;

            @Override
            protected Iterator<T> getChoices(String input) {
                return getIterator(input);
            }

            @Override
            protected void updateAjaxAttributes(AjaxRequestAttributes attributes){
                super.updateAjaxAttributes(attributes);
                attributes.setThrottlingSettings(new ThrottlingSettings(Duration.ONE_SECOND, true));
            }
        };
        
        input.setType(type);
        if (model instanceof LookupPropertyModel) {
            input.add(new OnChangeAjaxBehavior() {
            	private static final long serialVersionUID = 1L;
            	
                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    checkInputValue(input, target, (LookupPropertyModel<T>)model);
                }

                @Override
                protected void updateAjaxAttributes(AjaxRequestAttributes attributes){
                    super.updateAjaxAttributes(attributes);
                    attributes.setThrottlingSettings(new ThrottlingSettings(Duration.ONE_SECOND, true));
                }
            });
        }
        add(input);
    }

    /**
     *  This method takes care of retrieving an iterator over all
     *  options that can be completed. The generation of options can be
     *  affected by using current users input in 'input' variable.
     * */
    public abstract Iterator<T> getIterator(String input);

    @Override
    public FormComponent<T> getBaseFormComponent() {
        return (FormComponent<T>) get(ID_INPUT);
    }

    //by default the method will check if AutoCompleteTextField input is empty
    // and if yes, set empty value to model. This method is necessary because
    // AutoCompleteTextField doesn't set value to model until it is unfocused
    public void checkInputValue(AutoCompleteTextField input, AjaxRequestTarget target, LookupPropertyModel model){
        if (input.getInput() == null || input.getInput().trim().equals("")){
            model.setObject(input.getInput());
        }
        if (getIterator(input.getInput()).hasNext() && getIterator(input.getInput()).next() instanceof String) {
            Iterator<String> lookupTableValuesIterator = (Iterator<String>) getIterator(input.getInput());

            String value = input.getInput();
            boolean isValueExist = false;
            String existingValue = "";
            if (value != null) {
                if (value.trim().equals("")) {
                    isValueExist = true;
                } else {
                    while (lookupTableValuesIterator.hasNext()) {
                        String lookupTableValue = lookupTableValuesIterator.next();
                        if (value.trim().equalsIgnoreCase(lookupTableValue)) {
                            isValueExist = true;
                            existingValue = lookupTableValue;
                            break;
                        }
                    }
                }
            }
            if (isValueExist) {
                input.setModelValue(new String[]{existingValue});
                updateFeedbackPanel(input, false, target);
            } else {
                updateFeedbackPanel(input, true, target);
            }
        }
    }

    protected void updateFeedbackPanel(AutoCompleteTextField input, boolean isError, AjaxRequestTarget target){

    }
}

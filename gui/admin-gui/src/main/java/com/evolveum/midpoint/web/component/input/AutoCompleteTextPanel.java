/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.web.component.input;

import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.component.util.LookupPropertyModel;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteSettings;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteTextField;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;

import java.util.Iterator;

/**
 *  @author shood
 * */
public abstract class AutoCompleteTextPanel<T> extends InputPanel {

    private static final String ID_INPUT = "input";

    public AutoCompleteTextPanel(String id, IModel<T> model) {
        this(id, model, String.class);
    }

    public AutoCompleteTextPanel(String id, final IModel<T> model, Class clazz) {
        super(id);

        AutoCompleteSettings autoCompleteSettings = new AutoCompleteSettings();
        autoCompleteSettings.setShowListOnEmptyInput(true);
        autoCompleteSettings.setShowListOnFocusGain(true);
        autoCompleteSettings.setShowCompleteListOnFocusGain(true);
        final AutoCompleteTextField<T> input = new AutoCompleteTextField<T>(ID_INPUT, model, autoCompleteSettings) {

            @Override
            protected Iterator<T> getChoices(String input) {
                return getIterator(input);
            }
        };
        input.setType(clazz);
        if (model instanceof LookupPropertyModel) {
            input.add(new OnChangeAjaxBehavior() {
                @Override
                protected void onUpdate(AjaxRequestTarget target) {
                    checkInputValue(input, target, (LookupPropertyModel)model);
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
    public FormComponent getBaseFormComponent() {
        return (FormComponent) get(ID_INPUT);
    }

    //by default the method will check if AutoCompleteTextField input is empty
    // and if yes, set empty value to model. This method is necessary because
    // AutoCompleteTextField doesn't set value to model until it is unfocused
    public void checkInputValue(AutoCompleteTextField input, AjaxRequestTarget target, LookupPropertyModel model){
        if (input.getInput() == null || input.getInput().trim().equals("")){
            model.setObject(input.getInput());
        }
    }
}

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


	private static final long serialVersionUID = 1L;
	private static final String ID_INPUT = "input";

	 public AutoCompleteItemDefinitionPanel(String id, final IModel<ItemDefinition<?>> model) {
	    	super(id);
	    	initLayout(model);
	    }

	private void initLayout(final IModel<ItemDefinition<?>> model) {
		final Model<String> itemDefinitionAsStringModel = new Model<>(null);
		AutoCompleteTextField<String> input = new AutoCompleteTextField<String>(
				ID_INPUT, itemDefinitionAsStringModel, String.class, createAutoCompleteSettings()) {

			private static final long serialVersionUID = 1L;

			@Override
			protected Iterator<String> getChoices(String input) {
				List<String> defsAsString = new ArrayList<>();
				for (ItemDefinition<?> def : listChoices(input).values()) {
					defsAsString.add(def.getName().getLocalPart());
				}
				return defsAsString.iterator();

			}

			@Override
					protected void onConfigure() {
						itemDefinitionAsStringModel.setObject(null);
					}


		};

		 input.add(new OnChangeAjaxBehavior() {
				private static final long serialVersionUID = 1L;

				@Override
				protected void onUpdate(AjaxRequestTarget target) {
					String newValue = itemDefinitionAsStringModel.getObject();
					if (StringUtils.isNotBlank(newValue)){
						ItemDefinition<?> def = listChoices("").get(newValue);
						if (def != null) {
							model.setObject(def);
						}
					}
				}
			});

		add(input);
	}

	protected Map<String, ItemDefinition<?>> listChoices(String input){
		return new HashMap<>();
	}

	@Override
	public FormComponent<?> getBaseFormComponent() {
		return (FormComponent<?>) get(ID_INPUT);
	}

}

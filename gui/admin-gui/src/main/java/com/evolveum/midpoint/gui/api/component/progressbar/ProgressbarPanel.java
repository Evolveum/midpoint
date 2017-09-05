/**
 * Copyright (c) 2016 Evolveum
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
package com.evolveum.midpoint.gui.api.component.progressbar;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

/**
 * @author semancik
 */
public class ProgressbarPanel extends Panel{
	private static final long serialVersionUID = 1L;

	private static final String ID_PROGRESS_BAR = "progressBar";

	public ProgressbarPanel(String id, IModel<Integer> model) {
		super(id, model);
		initLayout(model);
	}

	private void initLayout(final IModel<Integer> model){

        WebMarkupContainer progressBar = new WebMarkupContainer(ID_PROGRESS_BAR);
        IModel<String> styleAttributeModel = new Model<String>(){
			private static final long serialVersionUID = 1L;

			@Override
			public String getObject() {
				return "width: " + model.getObject() + "%";
			}
        };
		progressBar.add(new AttributeModifier("style", styleAttributeModel));
        add(progressBar);
	}


}

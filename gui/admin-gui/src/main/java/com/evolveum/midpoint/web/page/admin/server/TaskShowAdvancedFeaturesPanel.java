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

package com.evolveum.midpoint.web.page.admin.server;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.model.IModel;

/**
 * @author mederly
 */
public class TaskShowAdvancedFeaturesPanel extends BasePanel<Boolean> {

    private static final String ID_ADVANCED_FEATURES = "advancedFeatures";

    public TaskShowAdvancedFeaturesPanel(String id, IModel<Boolean> model, VisibleEnableBehaviour checkBoxVisibleEnable) {
        super(id, model);
		initLayout(checkBoxVisibleEnable);
    }

    protected void initLayout(VisibleEnableBehaviour checkBoxVisibleEnable) {
		final AjaxCheckBox checkBox = new AjaxCheckBox(ID_ADVANCED_FEATURES, getModel()) {
			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				onAdvancedFeaturesUpdate(target);
			}
		};
		if (checkBoxVisibleEnable != null) {
			checkBox.add(checkBoxVisibleEnable);
		}
		add(checkBox);
    }

	protected void onAdvancedFeaturesUpdate(AjaxRequestTarget target) {
	}
}

/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
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

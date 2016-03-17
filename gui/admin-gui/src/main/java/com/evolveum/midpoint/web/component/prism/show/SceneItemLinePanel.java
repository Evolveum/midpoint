/*
 * Copyright (c) 2010-2016 Evolveum
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

package com.evolveum.midpoint.web.component.prism.show;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

/**
 * @author lazyman
 */
public class SceneItemLinePanel extends BasePanel<SceneItemLineDto> {

    private static final String ID_NAME_CONTAINER = "nameContainer";
    private static final String ID_NAME = "name";
    private static final String ID_OLD_VALUE = "oldValue";
    private static final String ID_NEW_VALUE = "newValue";

    private static final Trace LOGGER = TraceManager.getTrace(SceneItemLinePanel.class);

    private PageBase pageBase;

    public SceneItemLinePanel(String id, IModel<SceneItemLineDto> model, Form form, PageBase pageBase) {
        super(id, model);
        setOutputMarkupId(true);

        this.pageBase = pageBase;
        initLayout();
    }

	private void initLayout() {
		WebMarkupContainer nameCell = new WebMarkupContainer(ID_NAME_CONTAINER);
		nameCell.add(new AttributeModifier("rowspan", new PropertyModel<Integer>(getModel(), SceneItemLineDto.F_NUMBER_OF_LINES)));
		Label label = new Label("name", new PropertyModel<String>(getModel(), SceneItemLineDto.F_NAME));
		nameCell.add(label);
		nameCell.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return getModelObject().isFirst();
			}
		});
		add(nameCell);
		add(new Label(ID_NEW_VALUE, new PropertyModel<String>(getModel(), SceneItemLineDto.F_NEW_VALUE)));
	}
}

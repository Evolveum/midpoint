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

package com.evolveum.midpoint.web.component.prism.show;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.model.api.visualizer.SceneItemValue;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.column.ImagePanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

/**
 * @author mederly
 */
public class SceneItemLinePanel extends BasePanel<SceneItemLineDto> {

    private static final String ID_NAME_CONTAINER = "nameContainer";
    private static final String ID_NAME = "name";
    private static final String ID_OLD_VALUE_CONTAINER = "oldValueContainer";
    private static final String ID_OLD_VALUE_IMAGE = "oldValueImage";
    private static final String ID_OLD_VALUE = "oldValue";
	private static final String ID_NEW_VALUE_CONTAINER = "newValueContainer";
    private static final String ID_NEW_VALUE_IMAGE = "newValueImage";
    private static final String ID_NEW_VALUE = "newValue";

    private static final Trace LOGGER = TraceManager.getTrace(SceneItemLinePanel.class);

    public SceneItemLinePanel(String id, IModel<SceneItemLineDto> model) {
        super(id, model);

        initLayout();
    }

	private void initLayout() {
		WebMarkupContainer nameCell = new WebMarkupContainer(ID_NAME_CONTAINER);
		nameCell.add(new AttributeModifier("rowspan",
				new PropertyModel<Integer>(getModel(), SceneItemLineDto.F_NUMBER_OF_LINES)));
		Label label = new Label("name", new AbstractReadOnlyModel<String>() {
			@Override
			public String getObject() {
				final SceneItemLineDto lineDto = getModel().getObject();
				if (lineDto == null || lineDto.getName() == null) {
					return null;
				}
				String key = lineDto.getName();
				return getLocalizer().getString(key, SceneItemLinePanel.this, key);
			}
		});
		nameCell.add(label);
		nameCell.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return getModelObject().isFirst();
			}
		});
		add(nameCell);

		WebMarkupContainer oldValueCell = new WebMarkupContainer(ID_OLD_VALUE_CONTAINER);
		oldValueCell.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return getModelObject().isDelta();
			}
		});
		SceneItemValuePanel sivp = new SceneItemValuePanel(ID_OLD_VALUE,
				new PropertyModel<SceneItemValue>(getModel(), SceneItemLineDto.F_OLD_VALUE));
		sivp.setRenderBodyOnly(true);
		oldValueCell.add(sivp);

		ImagePanel oldValueImagePanel = new ImagePanel(ID_OLD_VALUE_IMAGE, Model.of(GuiStyleConstants.CLASS_MINUS_CIRCLE_DANGER),
				createStringResource("SceneItemLinePanel.removedValue"));
		oldValueImagePanel.add(new VisibleEnableBehaviour(){
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible(){
				return getModelObject().getOldValue() != null &&
						getModelObject().getOldValue().getSourceValue() != null;
			}
		});
		oldValueCell.add(oldValueImagePanel);

		add(oldValueCell);

		WebMarkupContainer newValueCell = new WebMarkupContainer(ID_NEW_VALUE_CONTAINER);
		sivp = new SceneItemValuePanel(ID_NEW_VALUE,
				new PropertyModel<SceneItemValue>(getModel(), SceneItemLineDto.F_NEW_VALUE));
		sivp.setRenderBodyOnly(true);
		newValueCell.add(sivp);
		newValueCell.add(new AttributeModifier("colspan", new AbstractReadOnlyModel<Integer>() {
			@Override
			public Integer getObject() {
				return !getModelObject().isDelta() && getModelObject().isDeltaScene() ? 2 : 1;
			}
		}));
		newValueCell.add(new AttributeModifier("align", new AbstractReadOnlyModel<String>() {
			@Override
			public String getObject() {
				return !getModelObject().isDelta() && getModelObject().isDeltaScene() ? "center" : null;
			}
		}));

		ImagePanel newValueImagePanel = new ImagePanel(ID_NEW_VALUE_IMAGE,
				!getModelObject().isDelta() && getModelObject().isDeltaScene() ?
				Model.of(GuiStyleConstants.CLASS_CIRCLE_FULL) :
						Model.of(GuiStyleConstants.CLASS_PLUS_CIRCLE_SUCCESS),
				!getModelObject().isDelta() && getModelObject().isDeltaScene() ?
						createStringResource("SceneItemLinePanel.unchangedValue")
				: createStringResource("SceneItemLinePanel.addedValue"));
		newValueImagePanel.add(new VisibleEnableBehaviour(){
			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible(){
				return getModelObject().getNewValue() != null &&
						getModelObject().getNewValue().getSourceValue() != null;
			}
		});
//		newValueImagePanel.add(new AttributeAppender("style",
//				!getModelObject().isDelta() && getModelObject().isDeltaScene() ?
//						""
//						: "float: left; margin-right: 5px;"));
		newValueCell.add(newValueImagePanel);

		add(newValueCell);
	}

}

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

package com.evolveum.midpoint.web.page.admin.workflow;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.util.LocalizableMessageModel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.InformationPartType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.InformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LocalizableMessageType;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import static com.evolveum.midpoint.schema.util.LocalizationUtil.getLocalizableMessageOrDefault;

/**
 * @author mederly
 */
public class InformationPanel extends BasePanel<InformationType> {

	private static final String ID_TITLE = "title";
	private static final String ID_PARTS = "parts";
	private static final String ID_PART = "part";

	public InformationPanel(String id, IModel<InformationType> model) {
		super(id, model);
		initLayout();
	}

	private void initLayout() {
		Label titleLabel = new Label(ID_TITLE, new LocalizableMessageModel(new AbstractReadOnlyModel<LocalizableMessageType>() {
			@Override
			public LocalizableMessageType getObject() {
				InformationType info = getModelObject();
				return getLocalizableMessageOrDefault(info.getLocalizableTitle(), info.getTitle());
			}
		}, this));
		titleLabel.add(new VisibleBehaviour(() -> getModelObject().getLocalizableTitle() != null && getModelObject().getTitle() != null));
		add(titleLabel);

		ListView<InformationPartType> list = new ListView<InformationPartType>(ID_PARTS,
				new PropertyModel<>(getModel(), InformationType.F_PART.getLocalPart())) {
			@Override
			protected void populateItem(ListItem<InformationPartType> item) {
				InformationPartType part = item.getModelObject();
				Label label = new Label(ID_PART, WebComponentUtil.resolveLocalizableMessage(
						getLocalizableMessageOrDefault(part.getLocalizableText(), part.getText()), InformationPanel.this));
				if (Boolean.TRUE.equals(part.isHasMarkup())) {
					label.setEscapeModelStrings(false);
				}
				item.add(label);
			}
		};
		add(list);
	}
}

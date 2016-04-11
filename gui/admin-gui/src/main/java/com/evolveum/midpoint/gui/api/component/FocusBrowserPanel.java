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
package com.evolveum.midpoint.gui.api.component;

import java.util.List;

import javax.xml.namespace.QName;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.util.ListModel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.input.QNameChoiceRenderer;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class FocusBrowserPanel<T extends FocusType> extends BasePanel<T> {

	private static final String ID_TYPE = "type";
	private static final String ID_TABLE = "table";

	private static final String ID_BUTTON_ADD = "addButton";

	private IModel<QName> typeModel;

	private PageBase parentPage;

	public FocusBrowserPanel(String id, final Class<T> type, boolean multiselect, PageBase parentPage) {
		super(id);
		this.parentPage = parentPage;
		typeModel = new LoadableModel<QName>(true) {

			@Override
			protected QName load() {
				return compileTimeClassToQName(type);
			}

		};

		initLayout(type, multiselect);
	}

	private void initLayout(Class<T> type, final boolean multiselect) {
		DropDownChoice<QName> typeSelect = new DropDownChoice(ID_TYPE, typeModel,
				new ListModel(WebComponentUtil.createFocusTypeList()), new QNameChoiceRenderer());
		typeSelect.add(new OnChangeAjaxBehavior() {

			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				ObjectListPanel<T> listPanel = (ObjectListPanel<T>) get(ID_TABLE);

				listPanel = createObjectListPanel(qnameToCompileTimeClass(typeModel.getObject()),
						multiselect);
				addOrReplace(listPanel);
				target.add(listPanel);
			}
		});
		add(typeSelect);

		ObjectListPanel<T> listPanel = createObjectListPanel(type, multiselect);
		add(listPanel);

		AjaxButton addButton = new AjaxButton(ID_BUTTON_ADD,
				createStringResource("userBrowserDialog.button.addButton")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				List<T> selected = ((PopupObjectListPanel) getParent().get(ID_TABLE)).getSelectedObjects();
				FocusBrowserPanel.this.addPerformed(target, selected);
			}
		};
		
		addButton.add(new VisibleEnableBehaviour() {
			
			@Override
			public boolean isVisible() {
				return multiselect;
			}
		});

		add(addButton);
	}

	protected void onClick(AjaxRequestTarget target, T focus) {
		parentPage.hideMainPopup(target);
	}

	private ObjectListPanel<T> createObjectListPanel(Class<T> type, final boolean multiselect) {
		
		PopupObjectListPanel<T> listPanel = new PopupObjectListPanel<T>(ID_TABLE, type, multiselect, parentPage){
			
		};
		
		
//		ObjectListPanel<T> listPanel = new ObjectListPanel<T>(ID_TABLE, type, parentPage) {
//
//			@Override
//			public void objectDetailsPerformed(AjaxRequestTarget target, T focus) {
//				super.objectDetailsPerformed(target, focus);
//				FocusBrowserPanel.this.onClick(target, focus);
//			}
//
//			@Override
//			public void addPerformed(AjaxRequestTarget target, List<T> selected) {
//				super.addPerformed(target, selected);
//				FocusBrowserPanel.this.addPerformed(target, selected);
//			}
//			
//			@Override
//			public boolean isMultiSelect() {
//				return multiselect;
//			}
//		};
//		listPanel.setMultiSelect(multiselect);
		listPanel.setOutputMarkupId(true);
		return listPanel;
	}

	protected void addPerformed(AjaxRequestTarget target, List<T> selected) {
		parentPage.hideMainPopup(target);
	}

	private Class qnameToCompileTimeClass(QName typeName) {
		return parentPage.getPrismContext().getSchemaRegistry().getCompileTimeClassForObjectType(typeName);
	}

	private QName compileTimeClassToQName(Class<T> type) {
		PrismObjectDefinition def = parentPage.getPrismContext().getSchemaRegistry()
				.findObjectDefinitionByCompileTimeClass(type);
		if (def == null) {
			return UserType.COMPLEX_TYPE;
		}

		return def.getTypeName();
	}

}

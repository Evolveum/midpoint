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
package com.evolveum.midpoint.gui.api.component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;

import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.util.ListModel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

public class ObjectBrowserPanel<O extends ObjectType> extends BasePanel<O> implements Popupable {

	private static final long serialVersionUID = 1L;
	private static final String ID_TYPE = "type";
	private static final String ID_TYPE_PANEL = "typePanel";
	private static final String ID_TABLE = "table";

	private static final String ID_BUTTON_ADD = "addButton";

	private IModel<ObjectTypes> typeModel;

	private PageBase parentPage;
	private ObjectFilter queryFilter;
	private List<O> selectedObjectsList = new ArrayList<O>();

	/**
	 * @param defaultType specifies type of the object that will be selected by default
	 */
	public ObjectBrowserPanel(String id, final Class<? extends O> defaultType, List<QName> supportedTypes, boolean multiselect,
							  PageBase parentPage) {
		this(id, defaultType, supportedTypes, multiselect, parentPage, null);
	}

	/**
	 * @param defaultType specifies type of the object that will be selected by default
	 */
	public ObjectBrowserPanel(String id, final Class<? extends O> defaultType, List<QName> supportedTypes, boolean multiselect,
							  PageBase parentPage, ObjectFilter queryFilter) {
		this(id, defaultType, supportedTypes, multiselect, parentPage, queryFilter, new ArrayList<O>());
	}

	public ObjectBrowserPanel(String id, final Class<? extends O> defaultType, List<QName> supportedTypes, boolean multiselect,
							  PageBase parentPage, ObjectFilter queryFilter, List<O> selectedData) {
		super(id);
		this.parentPage = parentPage;
		this.queryFilter = queryFilter;
		this.selectedObjectsList = selectedData;
		typeModel = new LoadableModel<ObjectTypes>(false) {

			private static final long serialVersionUID = 1L;

			@Override
			protected ObjectTypes load() {
				if (defaultType == null) {
					return null;
				}
				return ObjectTypes.getObjectType(defaultType);
			}

		};

		initLayout(defaultType, supportedTypes, multiselect);
	}

	private void initLayout(Class<? extends O> type, final List<QName> supportedTypes, final boolean multiselect) {
		List<ObjectTypes> supported = new ArrayList<>();
		for (QName qname : supportedTypes) {
			supported.add(ObjectTypes.getObjectTypeFromTypeQName(qname));
		}

		WebMarkupContainer typePanel = new WebMarkupContainer(ID_TYPE_PANEL);
		typePanel.setOutputMarkupId(true);
		typePanel.add(new VisibleEnableBehaviour() {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return supportedTypes.size() != 1;
			}
		});
		add(typePanel);

		DropDownChoice<ObjectTypes> typeSelect = new DropDownChoice<ObjectTypes>(ID_TYPE, typeModel,
				new ListModel<>(supported), new EnumChoiceRenderer<>(this));
		typeSelect.add(new OnChangeAjaxBehavior() {

			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				ObjectListPanel<O> listPanel = (ObjectListPanel<O>) get(ID_TABLE);

				listPanel = createObjectListPanel(typeModel.getObject(), multiselect);
				addOrReplace(listPanel);
				target.add(listPanel);
			}
		});
		typePanel.add(typeSelect);

		ObjectTypes objType = type != null ? ObjectTypes.getObjectType(type) : null;
		ObjectListPanel<O> listPanel = createObjectListPanel(objType, multiselect);
		add(listPanel);

		AjaxButton addButton = new AjaxButton(ID_BUTTON_ADD,
				createStringResource("userBrowserDialog.button.addButton")) {

			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget target) {
				List<O> selected = ((PopupObjectListPanel) getParent().get(ID_TABLE)).getSelectedObjects();
				ObjectTypes type = ObjectBrowserPanel.this.typeModel.getObject();
				QName qname = type != null ? type.getTypeQName() : null;
				ObjectBrowserPanel.this.addPerformed(target, qname, selected);
			}
		};

		addButton.add(new VisibleEnableBehaviour() {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return multiselect;
			}
		});

		add(addButton);
	}

	protected void onClick(AjaxRequestTarget target, O focus) {
		parentPage.hideMainPopup(target);
	}

	protected void onSelectPerformed(AjaxRequestTarget target, O focus) {
		parentPage.hideMainPopup(target);
	}

	private ObjectListPanel<O> createObjectListPanel(ObjectTypes type, final boolean multiselect) {
		Class typeClass = type.getClassDefinition();

		PopupObjectListPanel<O> listPanel = new PopupObjectListPanel<O>(ID_TABLE, typeClass, getOptions(),
				multiselect, parentPage, selectedObjectsList) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void onSelectPerformed(AjaxRequestTarget target, O object) {
				ObjectBrowserPanel.this.onSelectPerformed(target, object);
			}

			@Override
			protected ObjectQuery addFilterToContentQuery(ObjectQuery query) {
				if (queryFilter != null) {
					if (query == null) {
						query = new ObjectQuery();
					}
					query.addFilter(queryFilter);
				}
				return query;
			}
		};
		listPanel.setOutputMarkupId(true);
		return listPanel;
	}

	protected void addPerformed(AjaxRequestTarget target, QName type, List<O> selected) {
		parentPage.hideMainPopup(target);
	}

	private Collection<SelectorOptions<GetOperationOptions>> getOptions() {
		if (ObjectTypes.SHADOW.getTypeQName().equals(typeModel.getObject() != null ? typeModel.getObject().getTypeQName() : null)) {
			return SelectorOptions.createCollection(ItemPath.EMPTY_PATH, GetOperationOptions.createNoFetch());
		}
		return null;

	}

	@Override
	public int getWidth() {
		return 900;
	}

	@Override
	public int getHeight() {
		return 700;
	}

	@Override
	public StringResourceModel getTitle() {
		return parentPage.createStringResource("ObjectBrowserPanel.chooseObject");
	}

	@Override
	public Component getComponent() {
		return this;
	}

}

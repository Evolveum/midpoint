/*
 * Copyright (c) 2010-2018 Evolveum
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
package com.evolveum.midpoint.gui.impl.component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.impl.util.GuiImplUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.data.column.DoubleButtonColumn;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.objectdetails.FocusMainPanel;
import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import com.evolveum.midpoint.web.component.prism.ContainerWrapper;
import com.evolveum.midpoint.web.component.prism.ContainerWrapperFactory;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.util.MultivalueContainerListDataProvider;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage.TableId;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyRuleType;

/**
 * @author skublik
 */

public abstract class MultivalueContainerListPanelWithDetailsPanel<C extends Containerable> extends MultivalueContainerListPanel<C> {

	private static final long serialVersionUID = 1L;

	public static final String ID_ITEMS_DETAILS = "itemsDetails";
	public static final String ID_ITEM_DETAILS = "itemDetails";
	public static final String ID_SEARCH_ITEM_PANEL = "search";

	public static final String ID_DETAILS = "details";

	private final static String ID_DONE_BUTTON = "doneButton";
	private final static String ID_CANCEL_BUTTON = "cancelButton";

	private static final Trace LOGGER = TraceManager.getTrace(MultivalueContainerListPanelWithDetailsPanel.class);

	private List<ContainerValueWrapper<C>> detailsPanelItemsList = new ArrayList<>();
	private boolean itemDetailsVisible;
	
	public MultivalueContainerListPanelWithDetailsPanel(String id, IModel<ContainerWrapper<C>> model, TableId tableId, int itemPerPage, PageStorage pageStorage) {
		super(id, model, tableId, itemPerPage, pageStorage);
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
	}
	
	@Override
	protected void initCustomLayout() {
		
		initDetailsPanel();
		
	}
	
	public void setItemDetailsVisible(boolean itemDetailsVisible) {
		this.itemDetailsVisible = itemDetailsVisible;
	}

	protected abstract MultivalueContainerDetailsPanel<C> getMultivalueContainerDetailsPanel(ListItem<ContainerValueWrapper<C>> item);

	protected void initDetailsPanel() {
		WebMarkupContainer details = new WebMarkupContainer(ID_DETAILS);
		details.setOutputMarkupId(true);
		details.add(new VisibleEnableBehaviour() {

			private static final long serialVersionUID = 1L;

			@Override
			public boolean isVisible() {
				return itemDetailsVisible;
			}
		});

		add(details);
		
		ListView<ContainerValueWrapper<C>> itemDetailsView = new ListView<ContainerValueWrapper<C>>(MultivalueContainerListPanelWithDetailsPanel.ID_ITEMS_DETAILS,
				new AbstractReadOnlyModel<List<ContainerValueWrapper<C>>>() {
					private static final long serialVersionUID = 1L;

					@Override
					public List<ContainerValueWrapper<C>> getObject() {
						return detailsPanelItemsList;
					}
				}) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<ContainerValueWrapper<C>> item) {
				MultivalueContainerDetailsPanel<C> detailsPanel = getMultivalueContainerDetailsPanel(item);
				item.add(detailsPanel);
				detailsPanel.setOutputMarkupId(true);

			}
			

		};

		itemDetailsView.setOutputMarkupId(true);
		details.add(itemDetailsView);

		AjaxButton doneButton = new AjaxButton(ID_DONE_BUTTON,
				createStringResource("MultivalueContainerListPanel.doneButton")) {
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget ajaxRequestTarget) {
				itemDetailsVisible = false;
				refreshTable(ajaxRequestTarget);
				ajaxRequestTarget.add(MultivalueContainerListPanelWithDetailsPanel.this);
			}
		};
		details.add(doneButton);

		AjaxButton cancelButton = new AjaxButton(ID_CANCEL_BUTTON,
				createStringResource("MultivalueContainerListPanel.cancelButton")) {
			private static final long serialVersionUID = 1L;

			@Override
			public void onClick(AjaxRequestTarget ajaxRequestTarget) {
				itemDetailsVisible = false;
				ajaxRequestTarget.add(MultivalueContainerListPanelWithDetailsPanel.this);
			}
		};
		details.add(cancelButton);
	}

	public void itemDetailsPerformed(AjaxRequestTarget target, IModel<ContainerValueWrapper<C>> rowModel) {
		itemPerformedForDefaultAction(target, rowModel, null);
	}

	public void itemDetailsPerformed(AjaxRequestTarget target, List<ContainerValueWrapper<C>> listItems) {
		itemPerformedForDefaultAction(target, null, listItems);
	}
	
	@Override
	protected void itemPerformedForDefaultAction(AjaxRequestTarget target, IModel<ContainerValueWrapper<C>> rowModel,
			List<ContainerValueWrapper<C>> listItems) {
		
		setItemDetailsVisible(true);
    	detailsPanelItemsList.clear();
    	if(rowModel == null) {
    		detailsPanelItemsList.addAll(listItems);
    		listItems.forEach(itemConfigurationTypeContainerValueWrapper -> {
    			itemConfigurationTypeContainerValueWrapper.setSelected(false);
    		});
    	} else {
    		detailsPanelItemsList.add(rowModel.getObject());
    		rowModel.getObject().setSelected(false);
    	}
    	target.add(MultivalueContainerListPanelWithDetailsPanel.this);
	}
	
	@Override
	protected boolean isListPanelVisible() {
		return !itemDetailsVisible;
	}
}

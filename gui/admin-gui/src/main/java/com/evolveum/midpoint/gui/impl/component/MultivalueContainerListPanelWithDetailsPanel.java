/*
 * Copyright (c) 2018 Evolveum
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.prism.ContainerWrapperImpl;
import com.evolveum.midpoint.gui.impl.prism.PrismContainerValueWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage.TableId;

/**
 * @author skublik
 */

public abstract class MultivalueContainerListPanelWithDetailsPanel<C extends Containerable, S extends Serializable>
										extends MultivalueContainerListPanel<C, S> {

	private static final long serialVersionUID = 1L;

	public static final String ID_ITEMS_DETAILS = "itemsDetails";
	public static final String ID_ITEM_DETAILS = "itemDetails";

	public static final String ID_DETAILS = "details";

	private final static String ID_DONE_BUTTON = "doneButton";
	private final static String ID_CANCEL_BUTTON = "cancelButton";

	private static final Trace LOGGER = TraceManager.getTrace(MultivalueContainerListPanelWithDetailsPanel.class);

	private List<PrismContainerValueWrapper<C>> detailsPanelItemsList = new ArrayList<>();
	private boolean itemDetailsVisible;
	
	public MultivalueContainerListPanelWithDetailsPanel(String id, IModel<PrismContainerWrapper<C>> model, TableId tableId, PageStorage pageStorage) {
		super(id, model, tableId, pageStorage);
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		initDetailsPanel();
	}

	public void setItemDetailsVisible(boolean itemDetailsVisible) {
		this.itemDetailsVisible = itemDetailsVisible;
	}

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
		
		ListView<PrismContainerValueWrapper<C>> itemDetailsView = new ListView<PrismContainerValueWrapper<C>>(MultivalueContainerListPanelWithDetailsPanel.ID_ITEMS_DETAILS,
				new IModel<List<PrismContainerValueWrapper<C>>>() {
					private static final long serialVersionUID = 1L;

					@Override
					public List<PrismContainerValueWrapper<C>> getObject() {
						return detailsPanelItemsList;
					}
				}) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void populateItem(ListItem<PrismContainerValueWrapper<C>> item) {
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
				cancelItemDetailsPerformed(ajaxRequestTarget);
				ajaxRequestTarget.add(MultivalueContainerListPanelWithDetailsPanel.this);
			}
		};
		details.add(cancelButton);
	}
	
	protected abstract MultivalueContainerDetailsPanel<C> getMultivalueContainerDetailsPanel(ListItem<PrismContainerValueWrapper<C>> item);

	public void itemDetailsPerformed(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<C>> rowModel) {
		itemPerformedForDefaultAction(target, rowModel, null);
	}

	public void itemDetailsPerformed(AjaxRequestTarget target, List<PrismContainerValueWrapper<C>> listItems) {
		itemPerformedForDefaultAction(target, null, listItems);
	}

	protected void cancelItemDetailsPerformed(AjaxRequestTarget target){
	}
	
	@Override
	protected void itemPerformedForDefaultAction(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<C>> rowModel,
			List<PrismContainerValueWrapper<C>> listItems) {
		
		if((listItems!= null && !listItems.isEmpty()) || rowModel != null) {
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
		} else {
			warn(createStringResource("MultivalueContainerListPanel.message.noItemsSelected").getString());
			target.add(getPageBase().getFeedbackPanel());
		}
	}
	
	@Override
	public boolean isListPanelVisible() {
		return !itemDetailsVisible;
	}
}

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

package com.evolveum.midpoint.web.page.admin.server.handlers;

import com.evolveum.midpoint.web.component.data.column.LinkIconPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.server.PageTaskEdit;
import com.evolveum.midpoint.web.page.admin.server.handlers.dto.LiveSyncHandlerDto;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

/**
 * @author mederly
 */
public class LiveSyncHandlerPanel extends ResourceRelatedHandlerPanel<LiveSyncHandlerDto> {

	private static final long serialVersionUID = 1L;

	private static final String ID_TOKEN_CONTAINER = "tokenContainer";
	private static final String ID_TOKEN = "token";
	private static final String ID_DELETE_TOKEN = "deleteToken";

	public LiveSyncHandlerPanel(String id, IModel<LiveSyncHandlerDto> handlerDtoModel, PageTaskEdit parentPage) {
		super(id, handlerDtoModel, parentPage);
		initLayout(parentPage);
	}

	private void initLayout(final PageTaskEdit parentPage) {
		WebMarkupContainer tokenContainer = new WebMarkupContainer(ID_TOKEN_CONTAINER);
		tokenContainer.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return true;	//TODO
			}
		});
		tokenContainer.add(new Label(ID_TOKEN, new PropertyModel<>(getModel(), LiveSyncHandlerDto.F_TOKEN)));

		LinkIconPanel deleteTokenPanel = new LinkIconPanel(ID_DELETE_TOKEN, new Model("fa fa-fw fa-trash-o fa-lg text-danger"), createStringResource("LiveSyncHandlerPanel.deleteToken")) {
			@Override
			protected void onClickPerformed(AjaxRequestTarget target) {
				parentPage.getController().deleteSyncTokenPerformed(target);
			}
		};
		deleteTokenPanel.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return !parentPage.isEdit() && getModelObject().hasToken();		// TODO ... and security
			}
		});
		deleteTokenPanel.setRenderBodyOnly(true);
		tokenContainer.add(deleteTokenPanel);
		add(tokenContainer);
	}

}

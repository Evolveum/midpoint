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
package com.evolveum.midpoint.web.page.admin.server.handlers;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.data.column.LinkPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.server.PageTaskEdit;
import com.evolveum.midpoint.web.page.admin.server.handlers.dto.HandlerDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

/**
 * @author mederly
 */
public class DefaultHandlerPanel<D extends HandlerDto> extends BasePanel<D> {
	private static final long serialVersionUID = 1L;

	private static final String ID_OBJECT_REF_CONTAINER = "objectRefContainer";
	private static final String ID_OBJECT_REF = "objectRef";

	public DefaultHandlerPanel(String id, IModel<D> model, PageTaskEdit parentPage) {
		super(id, model);
		initLayout(parentPage);
		setOutputMarkupId(true);
	}
	
	private void initLayout(final PageTaskEdit parentPage) {
		WebMarkupContainer objectRefContainer = new WebMarkupContainer(ID_OBJECT_REF_CONTAINER);
		objectRefContainer.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return getModelObject().getTaskDto().getObjectRef() != null;
			}
		});

		final LinkPanel objectRef = new LinkPanel(ID_OBJECT_REF, new PropertyModel<String>(getModel(), HandlerDto.F_OBJECT_REF_NAME)) {
			@Override
			public void onClick(AjaxRequestTarget target) {
				ObjectReferenceType ref = getModelObject().getObjectRef();
				if (ref != null) {
					WebComponentUtil.dispatchToObjectDetailsPage(ref, parentPage, false);
				}
			}
			@Override
			public boolean isEnabled() {
				return WebComponentUtil.hasDetailsPage(getModelObject().getObjectRef());
			}
		};
		objectRefContainer.add(objectRef);
		add(objectRefContainer);
	}

}

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
package com.evolveum.midpoint.gui.impl.page.admin.configuration.component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.PrismPanel;
import com.evolveum.midpoint.web.model.ContainerWrapperListFromObjectWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

import org.apache.wicket.request.mapper.parameter.PageParameters;

/**
 * @author skublik
 */
public class NotificationConfigPanelNew extends BasePanel<ObjectWrapper<SystemConfigurationType>> {

	private static final long serialVersionUID = 1L;

	private static final String ID_NOTIFICATION_CONFIG = "notificationConfiguration";
	public static final ItemPath PATH_NOTIFICATION_CONFIG = new ItemPath(SystemConfigurationType.F_NOTIFICATION_CONFIGURATION);

	public NotificationConfigPanelNew(String id, IModel<ObjectWrapper<SystemConfigurationType>> model, PageParameters parameters) {
		super(id, model);
	}
	
	@Override
	protected void onInitialize() {
		super.onInitialize();
		
		initLayout();
	}

	protected void initLayout() {
		
		Form form = new Form<>("form");
		
		PrismPanel<SystemConfigurationType> panel = new PrismPanel<SystemConfigurationType>(ID_NOTIFICATION_CONFIG, 
				new ContainerWrapperListFromObjectWrapperModel(getModel(), getVisibleContainers()), null, form, itemWrapper -> getActivationVisibileItems(itemWrapper.getPath()), getPageBase());
		add(panel);
		
	}
	
	private ItemVisibility getActivationVisibileItems(ItemPath pathToCheck) {
    	if(pathToCheck.isSubPathOrEquivalent(PATH_NOTIFICATION_CONFIG)){
			return ItemVisibility.AUTO;
		}
		return ItemVisibility.HIDDEN;
    }
	
	private List<ItemPath> getVisibleContainers() {
		List<ItemPath> paths = new ArrayList<>();
		paths.addAll(Arrays.asList(ItemPath.EMPTY_PATH));
		return paths;
	}
}

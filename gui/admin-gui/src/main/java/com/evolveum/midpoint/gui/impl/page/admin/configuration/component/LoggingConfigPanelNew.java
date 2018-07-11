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

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.PrismContainerPanel;
import com.evolveum.midpoint.web.model.ContainerWrapperFromObjectWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

/**
 * @author skublik
 */
public class LoggingConfigPanelNew extends BasePanel<ObjectWrapper<SystemConfigurationType>> {

	private static final long serialVersionUID = 1L;
	
    private static final String ID_LOGGING_CONFIG = "loggingConfiguration";
    private static final ItemPath PATH_LOGGING = new ItemPath(SystemConfigurationType.F_LOGGING);

    public LoggingConfigPanelNew(String id, IModel<ObjectWrapper<SystemConfigurationType>> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
    		super.onInitialize();
    		initLayout();
    }
    
    protected void initLayout() {

    	Form form = new Form<>("form");
    	
    	ContainerWrapperFromObjectWrapperModel<LoggingConfigurationType, SystemConfigurationType> model = new ContainerWrapperFromObjectWrapperModel<>(getModel(), PATH_LOGGING);
		PrismContainerPanel<LoggingConfigurationType> panel = new PrismContainerPanel<>(ID_LOGGING_CONFIG, model, true, form, null, getPageBase());
		add(panel);
		
	}
	
}

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

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

/**
 * @author skublik
 */
public class SystemConfigPanel extends BasePanel<PrismObjectWrapper<SystemConfigurationType>> {
	
    private static final long serialVersionUID = 1L;
    
    private static final Trace LOGGER = TraceManager.getTrace(SystemConfigPanel.class);

    private static final String ID_SYSTEM_CONFIG = "basicSystemConfiguration";

    
    public SystemConfigPanel(String id, IModel<PrismObjectWrapper<SystemConfigurationType>> model) {
        super(id, model);

        setOutputMarkupId(true);
        
    }
    
    @Override
    protected void onInitialize() {
    	super.onInitialize();
    	
    	initLayout();
    }
    
    protected void initLayout() {
    	try {
			Panel panel = getPageBase().initItemPanel(ID_SYSTEM_CONFIG, SystemConfigurationType.COMPLEX_TYPE, getModel(), itemWrapper -> getBasicTabVisibity(itemWrapper));
			add(panel);
		} catch (SchemaException e) {
			LOGGER.error("Cannot create basic panel for system configuration.");
			getSession().error("Cannot create basic panel for system configuration.");
		}
		
    }
    
	private ItemVisibility getBasicTabVisibity(ItemWrapper<?, ?, ?, ?> itemWrapper) {
		if(itemWrapper.getPath().isSubPathOrEquivalent(ItemPath.create(ItemPath.EMPTY_PATH, SystemConfigurationType.F_DESCRIPTION)) || itemWrapper.getPath().isSubPathOrEquivalent(ItemPath.create(
				ItemPath.EMPTY_PATH, SystemConfigurationType.F_GLOBAL_SECURITY_POLICY_REF))) {
			return ItemVisibility.AUTO;
		}

		if(itemWrapper.getPath().isSuperPathOrEquivalent(ItemPath.create(ObjectType.F_EXTENSION))) {
			return ItemVisibility.AUTO;
		}

		return ItemVisibility.HIDDEN;
    }
}

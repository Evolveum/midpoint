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

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.path.ItemName;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.PrismContainerPanel;
import com.evolveum.midpoint.web.model.ContainerWrapperFromObjectWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PcpAspectConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PrimaryChangeProcessorConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WfConfigurationType;

/**
 * @author skublik
 */
public class ContainerOfSystemConfigurationPanel<C extends Containerable> extends BasePanel<ObjectWrapper<SystemConfigurationType>> {

	private static final long serialVersionUID = 1L;
	
	private static final Trace LOGGER = TraceManager.getTrace(ContainerOfSystemConfigurationPanel.class);
	
    private static final String ID_CONTAINER = "container";
    private ItemName qNameContainer;

    public ContainerOfSystemConfigurationPanel(String id, IModel<ObjectWrapper<SystemConfigurationType>> model, QName qNameContainer) {
        super(id, model);
        this.qNameContainer = ItemName.fromQName(qNameContainer);
    }

    @Override
    protected void onInitialize() {
    		super.onInitialize();
    		initLayout();
    }
    
    protected void initLayout() {

    	Form form = new Form<>("form");
    	
    	ContainerWrapperFromObjectWrapperModel<C, SystemConfigurationType> model = new ContainerWrapperFromObjectWrapperModel<>(getModel(), qNameContainer);
		PrismContainerPanel<C> panel = new PrismContainerPanel<>(ID_CONTAINER, model, true, form, itemWrapper -> getVisibity(itemWrapper.getPath()), getPageBase());
		add(panel);
		
	}
    
    protected ItemVisibility getVisibity(ItemPath itemPath) {
    	if (itemPath.firstToName().equals(SystemConfigurationType.F_WORKFLOW_CONFIGURATION)) {
    		if (itemPath.lastName().equals(WfConfigurationType.F_APPROVER_COMMENTS_FORMATTING)) {
    			return ItemVisibility.HIDDEN;
    		}
    		
    		if (itemPath.rest().equivalent(ItemPath.create(WfConfigurationType.F_PRIMARY_CHANGE_PROCESSOR, PrimaryChangeProcessorConfigurationType.F_ADD_ASSOCIATION_ASPECT,
    				PcpAspectConfigurationType.F_APPROVER_REF))) {
    			return ItemVisibility.AUTO;
    		}
    		
    		if (itemPath.rest().startsWithName(WfConfigurationType.F_PRIMARY_CHANGE_PROCESSOR)
    				&& (itemPath.lastName().equals(PcpAspectConfigurationType.F_APPROVER_EXPRESSION)
    						|| itemPath.lastName().equals(PcpAspectConfigurationType.F_APPROVER_REF)
    						|| itemPath.lastName().equals(PcpAspectConfigurationType.F_AUTOMATICALLY_APPROVED)
    						|| itemPath.lastName().equals(PcpAspectConfigurationType.F_APPLICABILITY_CONDITION))) {
    			return ItemVisibility.HIDDEN;
    		}
    	}
    	
    	if (itemPath.equivalent(ItemPath.create(SystemConfigurationType.F_ACCESS_CERTIFICATION, AccessCertificationConfigurationType.F_REVIEWER_COMMENTS_FORMATTING))) {
    		return ItemVisibility.HIDDEN;
    	}
    	
    	return ItemVisibility.AUTO;
    }

	
}

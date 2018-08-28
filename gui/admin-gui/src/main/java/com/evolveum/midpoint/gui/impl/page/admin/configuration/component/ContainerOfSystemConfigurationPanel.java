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

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.prism.ItemWrapper;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.PrismContainerPanel;
import com.evolveum.midpoint.web.model.ContainerWrapperFromObjectWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DeploymentInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
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
    private QName qNameContainer;

    public ContainerOfSystemConfigurationPanel(String id, IModel<ObjectWrapper<SystemConfigurationType>> model, QName qNameContainer) {
        super(id, model);
        this.qNameContainer = qNameContainer;
    }

    @Override
    protected void onInitialize() {
    		super.onInitialize();
    		initLayout();
    }
    
    protected void initLayout() {

    	Form form = new Form<>("form");
    	
    	ContainerWrapperFromObjectWrapperModel<C, SystemConfigurationType> model = new ContainerWrapperFromObjectWrapperModel<>(getModel(), new ItemPath(qNameContainer));
		PrismContainerPanel<C> panel = new PrismContainerPanel<>(ID_CONTAINER, model, true, form, itemWrapper -> getVisibity(itemWrapper.getPath()), getPageBase());
		add(panel);
		
	}
    
    protected ItemVisibility getVisibity(ItemPath itemPath) {
    	if(itemPath.getFirstName().equals(SystemConfigurationType.F_WORKFLOW_CONFIGURATION)) {
    		if(itemPath.lastNamed().getName().equals(WfConfigurationType.F_APPROVER_COMMENTS_FORMATTING)) {
    			return ItemVisibility.HIDDEN;
    		}
    		
    		if(itemPath.tail().equivalent(new ItemPath(WfConfigurationType.F_PRIMARY_CHANGE_PROCESSOR, PrimaryChangeProcessorConfigurationType.F_ADD_ASSOCIATION_ASPECT,
    				PcpAspectConfigurationType.F_APPROVER_REF))) {
    			return ItemVisibility.AUTO;
    		}
    		
    		if(itemPath.tail().startsWithName(WfConfigurationType.F_PRIMARY_CHANGE_PROCESSOR) 
    				&& (itemPath.lastNamed().getName().equals(PcpAspectConfigurationType.F_APPROVER_EXPRESSION)
    						|| itemPath.lastNamed().getName().equals(PcpAspectConfigurationType.F_APPROVER_REF)
    						|| itemPath.lastNamed().getName().equals(PcpAspectConfigurationType.F_AUTOMATICALLY_APPROVED)
    						|| itemPath.lastNamed().getName().equals(PcpAspectConfigurationType.F_APPLICABILITY_CONDITION))) {
    			return ItemVisibility.HIDDEN;
    		}
    	}
    	
    	if(itemPath.equivalent(new ItemPath(SystemConfigurationType.F_ACCESS_CERTIFICATION, AccessCertificationConfigurationType.F_REVIEWER_COMMENTS_FORMATTING))) {
    		return ItemVisibility.HIDDEN;
    	}
    	
    	return ItemVisibility.AUTO;
    }

	
}

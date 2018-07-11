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

import javax.xml.namespace.QName;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.DisplayNamePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.assignment.AbstractAssignmentDetailsPanel;
import com.evolveum.midpoint.web.component.assignment.AssignmentsUtil;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.prism.ContainerValuePanel;
import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import com.evolveum.midpoint.web.component.prism.ContainerWrapper;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.component.prism.ItemWrapper;
import com.evolveum.midpoint.web.component.prism.PrismContainerPanel;
import com.evolveum.midpoint.web.component.prism.PropertyOrReferenceWrapper;
import com.evolveum.midpoint.web.model.ContainerWrapperFromObjectWrapperModel;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectDetails;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyRuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

/**
 * @author skublik
 */

public abstract class MultivalueContainerDetailsPanel<C extends Containerable> extends BasePanel<ContainerValueWrapper<C>> {
	private static final long serialVersionUID = 1L;

    private final static String ID_DISPLAY_NAME = "displayName";
//    private final static String ID_ACTIVATION_PANEL = "activationPanel";
    private final static String ID_BASIC_PANEL = "basicPanel";
    protected final static String ID_SPECIFIC_CONTAINERS_PANEL = "specificContainersPanel";

    public MultivalueContainerDetailsPanel(String id, IModel<ContainerValueWrapper<C>> assignmentModel){
        super(id, assignmentModel);
    }
    
    @Override
    protected void onInitialize() {
    	super.onInitialize();
    	initLayout();
    	setOutputMarkupId(true);
    }
    
    protected abstract IModel<C> createDisplayNameModel();
    
    protected QName getRelationForDisplayNamePanel() {
    	return null;
    }
    
    protected IModel<String> getKindIntentLabelModelForDisplayNamePanel(){
    	return Model.of("");
    }

    protected void initLayout(){
    	
    	final IModel<C> displayNameModel = createDisplayNameModel();

    	DisplayNamePanel<C> displayNamePanel = new DisplayNamePanel<C>(ID_DISPLAY_NAME, displayNameModel) {
    		
	    	@Override
			protected QName getRelation() {
	    		return getRelationForDisplayNamePanel();
			}

			@Override
			protected IModel<String> getKindIntentLabelModel() {
				return getKindIntentLabelModelForDisplayNamePanel();
			}

		};

    	displayNamePanel.setOutputMarkupId(true);
    	add(displayNamePanel);

		add(getBasicContainerValuePanel(ID_BASIC_PANEL));
		
//		WebMarkupContainer specificContainers = new WebMarkupContainer(ID_SPECIFIC_CONTAINERS);
//		specificContainers.setOutputMarkupId(true);
//		add(specificContainers);
		
		add(getSpecificContainers(ID_SPECIFIC_CONTAINERS_PANEL));
    }
    
    protected abstract Fragment getSpecificContainers(String contentAreaId);
    
    protected <O extends ObjectType> ContainerValuePanel<C> getBasicContainerValuePanel(String idPanel){
    	Form form = new Form<>("form");
    	ItemPath itemPath = getModelObject().getPath();
		return new ContainerValuePanel(idPanel, getModel(), true, form,
				itemWrapper -> getBasicTabVisibity(itemWrapper, itemPath), getPageBase());
    }
    
    protected ItemVisibility getBasicTabVisibity(ItemWrapper itemWrapper, ItemPath parentAssignmentPath) {
    	return ItemVisibility.AUTO;
    }

}

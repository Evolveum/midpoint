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

package com.evolveum.midpoint.web.page.admin.services;

import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ServiceType;


/**
 * @author katkav
 * @author lazyman
 */
@PageDescriptor(url = "/admin/users", action = {
        @AuthorizationAction(actionUri = PageAdminServices.AUTH_SERVICES_ALL,
                label = PageAdminServices.AUTH_SERVICES_ALL_LABEL,
                description = PageAdminServices.AUTH_SERVICES_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SERVICES_ALL_URL,
                label = "PageUsers.auth.users.label",
                description = "PageUsers.auth.users.description")})
public class PageServices extends PageAdminServices {
	private static final long serialVersionUID = 1L;

	private static final String ID_MAIN_FORM = "mainForm";
	private static final String ID_TABLE = "table";

	  private IModel<Search> searchModel;

	    public PageServices() {
	        this(true);
	    }

	    public PageServices(boolean clearPagingInSession) {
	        initLayout();
	    }

	    private void initLayout() {
	        Form mainForm = new Form(ID_MAIN_FORM);
	        add(mainForm);
	        
	      
	        MainObjectListPanel<ServiceType> servicePanel = new MainObjectListPanel<ServiceType>(ID_TABLE, ServiceType.class, null, this){
	        	
	        
	        	@Override
	        	public void objectDetailsPerformed(AjaxRequestTarget target, ServiceType service) {
	        		PageServices.this.serviceDetailsPerformed(target, service);
	        	}
	        	
	        	@Override
	        	protected List<IColumn<SelectableBean<ServiceType>, String>> createColumns() {
	        		return ColumnUtils.getDefaultServiceColumns();
	        	}
	        	
	        	@Override
	        	protected List<InlineMenuItem> createInlineMenu() {
	        		// TODO Auto-generated method stub
	        		return null;
	        	}
	        	
	        	@Override
	        	protected void newObjectPerformed(AjaxRequestTarget target) {
	        		setResponsePage(PageService.class);	
	        	}
	        	
	        	@Override
	        	protected String getBoxCssClasses() {
	        		return GuiStyleConstants.CLASS_BOX + " " + GuiStyleConstants.CLASS_OBJECT_SERVICE_BOX_CSS_CLASSES;
	        	}
	        	
	        };
	        mainForm.add(servicePanel);
	  
	    }

	    protected void serviceDetailsPerformed(AjaxRequestTarget target, ServiceType service) {
			 PageParameters parameters = new PageParameters();
		     parameters.add(OnePageParameterEncoder.PARAMETER, service.getOid());
		     setResponsePage(PageService.class, parameters);
			
		}

}
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

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.column.InlineMenuHeaderColumn;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.page.admin.configuration.component.HeaderMenuAction;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.AbstractReadOnlyModel;
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
import com.evolveum.midpoint.web.session.UserProfileStorage.TableId;
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
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_SERVICES_URL,
                label = "PageServices.auth.services.label",
                description = "PageServices.auth.services.description")})
public class PageServices extends PageAdminServices {
	private static final long serialVersionUID = 1L;

	private static final String ID_MAIN_FORM = "mainForm";
	private static final String ID_TABLE = "table";
    private static final String DOT_CLASS = PageServices.class.getName() + ".";
    private static final Trace LOGGER = TraceManager.getTrace(PageServices.class);
    private static final String OPERATION_DELETE_SERVICES = DOT_CLASS + "deleteServices";

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
	        
	      
	        MainObjectListPanel<ServiceType> servicePanel = new MainObjectListPanel<ServiceType>(ID_TABLE, ServiceType.class, TableId.TABLE_SERVICES, null, this){
				private static final long serialVersionUID = 1L;

				@Override
	        	public void objectDetailsPerformed(AjaxRequestTarget target, ServiceType service) {
	        		PageServices.this.serviceDetailsPerformed(target, service);
	        	}
	        	
	        	@Override
	        	protected List<IColumn<SelectableBean<ServiceType>, String>> createColumns() {
                    List<IColumn<SelectableBean<ServiceType>, String>> columns = ColumnUtils.getDefaultServiceColumns();

                    IColumn column = new InlineMenuHeaderColumn(initInlineMenu());
                    columns.add(column);

                    return columns;
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
	        };
	        servicePanel.setAdditionalBoxCssClasses(GuiStyleConstants.CLASS_OBJECT_SERVICE_BOX_CSS_CLASSES);
            servicePanel.setOutputMarkupId(true);
	        mainForm.add(servicePanel);
	  
	    }

	    protected void serviceDetailsPerformed(AjaxRequestTarget target, ServiceType service) {
			 PageParameters parameters = new PageParameters();
		     parameters.add(OnePageParameterEncoder.PARAMETER, service.getOid());
		     setResponsePage(PageService.class, parameters);
			
		}

    private void deletePerformed(AjaxRequestTarget target) {
        List<ServiceType> selected = getSelectedServices();
        if (selected.isEmpty()) {
            warn(getString("PageServices.message.nothingSelected"));
            target.add(getFeedbackPanel());
            return;
        }

        showMainPopup(getDeletePopupContent(), target);
    }

    private List<ServiceType> getSelectedServices() {
        MainObjectListPanel<ServiceType> table = getServicesTable();
        return table.getSelectedObjects();

    }

    private MainObjectListPanel<ServiceType> getServicesTable() {
        return (MainObjectListPanel<ServiceType>) get(createComponentPath(ID_MAIN_FORM, ID_TABLE));
    }

    private Popupable getDeletePopupContent() {
        return new ConfirmationPanel(getMainPopupBodyId(), createDeleteConfirmString()) {
            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                hideMainPopup(target);
                deleteConfirmedPerformed(target);
            }
        };
    }

    private IModel<String> createDeleteConfirmString() {
        return new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                return createStringResource("PageServices.message.deleteServicesConfirm",
                        getSelectedServices().size()).getString();
            }
        };
    }

    private void deleteConfirmedPerformed(AjaxRequestTarget target) {
        List<ServiceType> selected = getSelectedServices();

        OperationResult result = new OperationResult(OPERATION_DELETE_SERVICES);
        for (ServiceType service : selected) {
            try {
                Task task = createSimpleTask(OPERATION_DELETE_SERVICES);

                ObjectDelta delta = ObjectDelta.createDeleteDelta(ServiceType.class, service.getOid(), getPrismContext());
                getModelService().executeChanges(WebComponentUtil.createDeltaCollection(delta), null, task, result);
            } catch (Exception ex) {
                result.recordPartialError("Couldn't delete service.", ex);
                LoggingUtils.logUnexpectedException(LOGGER, "Couldn't delete service", ex);
            }
        }

        if (result.isUnknown()) {
            result.recomputeStatus("Error occurred during service deleting.");
        }

        if (result.isSuccess()) {
            result.recordStatus(OperationResultStatus.SUCCESS, "The service(s) have been successfully deleted.");
        }

        MainObjectListPanel<ServiceType> table = getServicesTable();
        table.clearCache();

        showResult(result);
        target.add(getFeedbackPanel());
        target.add((Component) getServicesTable());
    }

    private List<InlineMenuItem> initInlineMenu() {
        List<InlineMenuItem> headerMenuItems = new ArrayList<InlineMenuItem>();
        headerMenuItems.add(new InlineMenuItem(createStringResource("PageServices.message.buttonDelete"), true,
                new HeaderMenuAction(this) {

                    @Override
                    public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        deletePerformed(target);
                    }
                }));

        return headerMenuItems;
    }

}
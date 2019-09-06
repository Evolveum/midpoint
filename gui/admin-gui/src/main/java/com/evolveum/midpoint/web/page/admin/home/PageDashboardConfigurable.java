/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.home;

import com.evolveum.midpoint.web.application.Url;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordListType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventStageType;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;

import org.apache.commons.collections4.map.HashedMap;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.component.IRequestablePage;
import org.apache.wicket.util.string.StringValue;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.box.SmallInfoBoxPanel;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.box.BasicInfoBoxPanel;
import com.evolveum.midpoint.web.component.box.InfoBoxPanel;
import com.evolveum.midpoint.web.page.admin.configuration.PageDebugList;
import com.evolveum.midpoint.web.page.admin.dto.ObjectViewDto;
import com.evolveum.midpoint.web.page.admin.home.component.DashboardPanel;
import com.evolveum.midpoint.web.page.admin.home.component.PersonalInfoPanel;
import com.evolveum.midpoint.web.page.admin.home.component.SystemInfoPanel;
import com.evolveum.midpoint.web.page.admin.reports.PageAuditLogViewer;
import com.evolveum.midpoint.web.page.admin.reports.dto.AuditSearchDto;
import com.evolveum.midpoint.web.page.admin.resources.PageResources;
import com.evolveum.midpoint.web.page.admin.roles.PageRoles;
import com.evolveum.midpoint.web.page.admin.server.PageTasks;
import com.evolveum.midpoint.web.page.admin.services.PageServices;
import com.evolveum.midpoint.web.page.admin.users.PageOrgTree;
import com.evolveum.midpoint.web.page.admin.users.PageUsers;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;

/**
 * @author skublik
 */
@PageDescriptor(
		urls = {
				@Url(mountUrl = "/admin", matchUrlForSecurity = "/admin"),
				@Url(mountUrl = "/admin/dashboard"),
		},
		action = {
				@AuthorizationAction(actionUri = PageAdminHome.AUTH_HOME_ALL_URI,
						label = PageAdminHome.AUTH_HOME_ALL_LABEL,
						description = PageAdminHome.AUTH_HOME_ALL_DESCRIPTION),
				@AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_DASHBOARD_URL,
						label = "PageDashboard.auth.dashboard.label",
						description = "PageDashboard.auth.dashboard.description")
		})
public class PageDashboardConfigurable extends PageDashboard {
	private static final long serialVersionUID = 1L;

	private static final Trace LOGGER = TraceManager.getTrace(PageDashboardConfigurable.class);
	
	private IModel<DashboardType> dashboardModel;

	private static final String ID_WIDGETS = "widgets";
	private static final String ID_WIDGET = "widget";

    @Override
    protected void onInitialize(){
    	if (dashboardModel == null){
    		dashboardModel = initDashboardObject();
        }
        super.onInitialize();
    }
    
    @Override
    protected IModel<String> createPageTitleModel() {
    	return new IModel<String>() {

			@Override
			public String getObject() {
				
				if(dashboardModel.getObject().getDisplay() != null && dashboardModel.getObject().getDisplay().getLabel() != null) {
		        	return dashboardModel.getObject().getDisplay().getLabel().getOrig();
				} else {
					return dashboardModel.getObject().getName().getOrig();
				}
			}
		};
    }
    
    private IModel<DashboardType> initDashboardObject() {
    	return new IModel<DashboardType>() {

			@Override
			public DashboardType getObject() {
				StringValue dashboardOid = getPageParameters().get(OnePageParameterEncoder.PARAMETER);
                if (dashboardOid == null || StringUtils.isEmpty(dashboardOid.toString())) {
                    getSession().error(getString("PageDashboardConfigurable.message.oidNotDefined"));
                    throw new RestartResponseException(PageDashboardInfo.class);
                }
                Task task = createSimpleTask("Search dashboard");
                return WebModelServiceUtils.loadObject(DashboardType.class, dashboardOid.toString(), PageDashboardConfigurable.this, task, task.getResult()).getRealValue();
			}
    		
    	};
	}

	protected void initLayout() {
    	initInfoBoxes();

    }

    private void initInfoBoxes() {
    	
        add(new ListView<DashboardWidgetType>(ID_WIDGETS, new PropertyModel(dashboardModel, "widget")) {
            @Override
            protected void populateItem(ListItem<DashboardWidgetType> item) {
            	item.add(new SmallInfoBoxPanel(ID_WIDGET, item.getModel(),
            			PageDashboardConfigurable.this));
            }
        });
	}
}

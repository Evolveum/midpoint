package com.evolveum.midpoint.web.page.admin.users.component;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.web.page.admin.roles.RoleMemberPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ServiceType;

public class ServiceMemberPanel extends RoleMemberPanel<ServiceType>{


	private static final long serialVersionUID = 1L;


	public ServiceMemberPanel(String id, IModel<ServiceType> model) {
		super(id, model);
	}

	@Override
	protected boolean isRole() {
		return false;
	}


}

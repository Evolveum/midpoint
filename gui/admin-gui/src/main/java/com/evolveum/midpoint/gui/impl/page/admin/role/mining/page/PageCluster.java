/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page;

import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.PageClusters;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/clusterTable", matchUrlForSecurity = "/admin/clusterTable")
        },
        encoder = OnePageParameterEncoder.class, action = {
        @AuthorizationAction(
                actionUri = AuthorizationConstants.AUTZ_UI_ROLES_ALL_URL,
                label = "PageAdminRoles.auth.roleAll.label",
                description = "PageAdminRoles.auth.roleAll.description"),
        @AuthorizationAction(
                actionUri = AuthorizationConstants.AUTZ_UI_ROLE_URL,
                label = "PageRole.auth.role.label",
                description = "PageRole.auth.role.description") })

public class PageCluster extends PageAdmin {

    private static final String ID_DATATABLE_CLUSTER_DS = "datatable_cluster_ds";

    public static final String PARAMETER_PARENT_OID = "id";
    public static final String PARAMETER_MODE = "mode";

    String getPageParameterParentOid() {
        PageParameters params = getPageParameters();
        return params.get(PARAMETER_PARENT_OID).toString();
    }

    String getPageParameterMode() {
        PageParameters params = getPageParameters();
        return params.get(PARAMETER_MODE).toString();
    }


    public PageCluster() {
        super();
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        System.out.println(getPageParameterParentOid());
        add(new PageClusters(ID_DATATABLE_CLUSTER_DS, getPageParameterParentOid(), getPageParameterMode()).setOutputMarkupId(true));
    }

    public PageBase getPageBase() {
        return ((PageBase) getPage());
    }

    @Override
    protected IModel<String> createPageTitleModel() {
        return createStringResource("RoleMining.page.cluster.title");
    }
}


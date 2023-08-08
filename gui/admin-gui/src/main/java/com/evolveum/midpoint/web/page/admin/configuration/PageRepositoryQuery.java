/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.configuration;

import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.web.component.form.MidpointForm;

import com.evolveum.midpoint.web.page.admin.configuration.dto.QueryDto;

import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.authentication.api.util.AuthConstants;
import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.api.model.NonEmptyModel;
import com.evolveum.midpoint.gui.api.model.NonEmptyWrapperModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.page.admin.configuration.component.QueryConverterPanel;
import com.evolveum.midpoint.web.page.admin.configuration.component.QueryPlaygroundPanel;
import com.evolveum.midpoint.web.page.admin.configuration.dto.RepoQueryDto;

/**
 * @author lazyman
 */
@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/config/repositoryQuery", matchUrlForSecurity = "/admin/config/repositoryQuery")
        },
        action = {
                @AuthorizationAction(actionUri = AuthConstants.AUTH_CONFIGURATION_ALL,
                        label = AuthConstants.AUTH_CONFIGURATION_ALL_LABEL,
                        description = AuthConstants.AUTH_CONFIGURATION_ALL_DESCRIPTION),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CONFIGURATION_REPOSITORY_QUERY_URL,
                        label = "PageRepositoryQuery.auth.query.label",
                        description = "PageRepositoryQuery.auth.query.description")
        })
public class PageRepositoryQuery extends PageAdminConfiguration {

    private static final Trace LOGGER = TraceManager.getTrace(PageRepositoryQuery.class);

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_TAB_PANEL = "tabs";

    private final NonEmptyModel<RepoQueryDto> model = new NonEmptyWrapperModel<>(new Model<>(new RepoQueryDto()));




    @Override
    protected void onInitialize() {
        super.onInitialize();

        initLayout();
    }

    public PageRepositoryQuery() {
        this(null, null);
    }

    public PageRepositoryQuery(QName objectType, String queryText) {
        model.getObject().setObjectType(objectType);
        model.getObject().setMidPointQuery(queryText);
    }

    private void initLayout() {
        MidpointForm<?> mainForm = new MidpointForm<>(ID_MAIN_FORM);
        add(mainForm);

        TabbedPanel<?> tabbedPanel = WebComponentUtil.createTabPanel(ID_TAB_PANEL, this, createTabList(), null);
        mainForm.add(tabbedPanel);

    }

    private List<ITab> createTabList() {
        List<ITab> tabs = new ArrayList<>();

        tabs.add(new PanelTab(createStringResource("Query playground")) {
            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new QueryPlaygroundPanel(panelId, model);
            }
        });

        tabs.add(new PanelTab(createStringResource("Query converter")) {
            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new QueryConverterPanel(panelId, new Model<>(new QueryDto()));
            }
        });
        return tabs;
    }


}

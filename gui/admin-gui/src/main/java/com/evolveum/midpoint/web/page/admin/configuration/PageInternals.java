/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.configuration;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.internals.InternalOperationClasses;
import com.evolveum.midpoint.authentication.api.util.AuthConstants;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.page.admin.configuration.dto.InternalsConfigDto;

import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.spring.injection.annot.SpringBean;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.datatype.XMLGregorianCalendar;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/config/internals", matchUrlForSecurity = "/admin/config/internals")
        },
        action = {
        @AuthorizationAction(actionUri = AuthConstants.AUTH_CONFIGURATION_ALL,
                label = AuthConstants.AUTH_CONFIGURATION_ALL_LABEL, description = AuthConstants.AUTH_CONFIGURATION_ALL_DESCRIPTION),
        @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_CONFIGURATION_INTERNALS_URL,
                label = "PageInternals.auth.configInternals.label", description = "PageInternals.auth.configInternals.description")})
public class PageInternals extends PageAdminConfiguration {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PageInternals.class);

    @SpringBean(name = "clock")
    private Clock clock;

    private static final String ID_TAB_PANEL = "tabPanel";

    private LoadableModel<XMLGregorianCalendar> model;
    private IModel<InternalsConfigDto> internalsModel;
    private Map<String,Boolean> tracesMap;

    public PageInternals() {
        model = new LoadableModel<XMLGregorianCalendar>() {
            private static final long serialVersionUID = 1L;

            @Override
            protected XMLGregorianCalendar load() {
                return clock.currentTimeXMLGregorianCalendar();
            }
        };

        internalsModel = new Model<>(new InternalsConfigDto());
        tracesMap = new HashMap<>();
        for (InternalOperationClasses op: InternalOperationClasses.values()) {
            tracesMap.put(op.getKey(), InternalMonitor.isTrace(op));
        }

        initLayout();
    }

    private void initLayout() {

        List<ITab> tabs = new ArrayList<>();
        tabs.add(new AbstractTab(createStringResource("PageInternals.tab.clock")) {

            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer getPanel(String panelId) {
                return createClockPanel(panelId);
            }
        });

        tabs.add(new AbstractTab(createStringResource("PageInternals.tab.debugUtil")) {

            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer getPanel(String panelId) {
                return initDebugUtilForm(panelId);
            }
        });

        tabs.add(new AbstractTab(createStringResource("PageInternals.tab.internalConfig")) {

            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer getPanel(String panelId) {
                return initInternalsConfigForm(panelId);
            }
        });

        tabs.add(new AbstractTab(createStringResource("PageInternals.tab.traces")) {

            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer getPanel(String panelId) {
                return initTraces(panelId);
            }
        });

        tabs.add(new AbstractTab(createStringResource("PageInternals.tab.counters")) {

            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer getPanel(String panelId) {
                return initCounters(panelId);
            }
        });

        tabs.add(new AbstractTab(createStringResource("PageInternals.tab.cache")) {

            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer getPanel(String panelId) {
                return initCachePanel(panelId);
            }
        });
        // TODO show only if experimental features are enabled?
        tabs.add(new AbstractTab(createStringResource("PageInternals.tab.memory")) {

            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer getPanel(String panelId) {
                return initMemoryPanel(panelId);
            }
        });
        tabs.add(new AbstractTab(createStringResource("PageInternals.tab.threads")) {

            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer getPanel(String panelId) {
                return initThreadsPanel(panelId);
            }
        });
        tabs.add(new AbstractTab(createStringResource("PageInternals.tab.performance")) {

            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer getPanel(String panelId) {
                return initPerformancePanel(panelId);
            }
        });
        tabs.add(new AbstractTab(createStringResource("PageInternals.tab.loggedUsers")) {

            private static final long serialVersionUID = 1L;

            @Override
            public WebMarkupContainer getPanel(String panelId) {
                return initLoggedUsersPanel(panelId);
            }
        });

        add(new TabbedPanel<>(ID_TAB_PANEL, tabs));
    }

    private WebMarkupContainer createClockPanel(String panelId) {
        return new InternalsClockPanel(panelId, model);
    }

    private WebMarkupContainer initDebugUtilForm(String panelId) {
       return new InternalsDebugUtilPanel(panelId, internalsModel);
    }

    private WebMarkupContainer initInternalsConfigForm(String panelId) {
       return new InternalsConfigPanel(panelId, internalsModel);
    }

    private WebMarkupContainer initTraces(String panelId) {
        return new InternalsTracesPanel(panelId, tracesMap);
    }

    private WebMarkupContainer initCounters(String panelId) {
            return new InternalsCountersPanel(panelId);
    }

    private WebMarkupContainer initCachePanel(String panelId) {
        return new InternalsCachePanel(panelId);
    }

    private WebMarkupContainer initThreadsPanel(String panelId) {
        return new InternalsThreadsPanel(panelId);
    }

    private WebMarkupContainer initPerformancePanel(String panelId) {
        return new InternalsPerformancePanel(panelId);
    }

    private WebMarkupContainer initMemoryPanel(String panelId) {
        return new InternalsMemoryPanel(panelId);
    }

    private WebMarkupContainer initLoggedUsersPanel(String panelId) {
        return new InternalsLoggedInUsersPanel(panelId);
    }
}

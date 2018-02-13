/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.web.page.admin.configuration;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.internals.InternalOperationClasses;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
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

@PageDescriptor(url = "/admin/config/internals", action = {
        @AuthorizationAction(actionUri = PageAdminConfiguration.AUTH_CONFIGURATION_ALL,
                label = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_LABEL, description = PageAdminConfiguration.AUTH_CONFIGURATION_ALL_DESCRIPTION),
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
        
        TabbedPanel<ITab> tabPannel = new TabbedPanel<>(ID_TAB_PANEL, tabs);
        add(tabPannel);
       
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


   
    
}

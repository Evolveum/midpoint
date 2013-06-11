/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.web.security;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.common.crypto.Protector;
import com.evolveum.midpoint.common.security.Authorization;
import com.evolveum.midpoint.common.security.AuthorizationConstants;
import com.evolveum.midpoint.common.security.MidPointPrincipal;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.GuiComponents;
import com.evolveum.midpoint.web.page.admin.home.PageDashboard;
import com.evolveum.midpoint.web.page.login.PageLogin;
import com.evolveum.midpoint.web.resource.css.CssResources;
import com.evolveum.midpoint.web.resource.img.ImgResources;
import com.evolveum.midpoint.web.resource.js.JsResources;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.wf.api.WorkflowService;
import org.apache.commons.configuration.Configuration;
import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.authroles.authentication.AbstractAuthenticatedWebSession;
import org.apache.wicket.authroles.authentication.AuthenticatedWebApplication;
import org.apache.wicket.core.request.mapper.MountedMapper;
import org.apache.wicket.markup.head.PriorityFirstComparator;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.resource.SharedResourceReference;
import org.apache.wicket.settings.IResourceSettings;
import org.apache.wicket.spring.injection.annot.SpringComponentInjector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Collection;

/**
 * @author lazyman
 */
@Component("midpointApplication")
public class MidPointApplication extends AuthenticatedWebApplication {

    private static final String WEB_APP_CONFIGURATION = "midpoint.webApplication";
    private static final Trace LOGGER = TraceManager.getTrace(MidPointApplication.class);
    @Autowired
    transient ModelService model;
    @Autowired
    transient ModelInteractionService modelInteractionService;
    @Autowired
    transient PrismContext prismContext;
    @Autowired
    transient TaskManager taskManager;
    @Autowired
    transient private WorkflowService workflowService;
    @Autowired
    transient MidpointConfiguration configuration;
    @Autowired(required = true)
    transient Protector protector;
    private WebApplicationConfiguration webApplicationConfiguration;

    @Override
    protected void onDestroy() {
        GuiComponents.destroy();

        super.onDestroy();
    }

    @Override
    public Class<PageDashboard> getHomePage() {
    	return WebMiscUtil.getHomePage();
    }

    @Override
    public void init() {
        super.init();

        getComponentInstantiationListeners().add(new SpringComponentInjector(this));

        IResourceSettings resourceSettings = getResourceSettings();
        resourceSettings.setHeaderItemComparator(new PriorityFirstComparator(true));

        resourceSettings.setThrowExceptionOnMissingResource(false);
        getMarkupSettings().setStripWicketTags(true);

        if (RuntimeConfigurationType.DEVELOPMENT.equals(getConfigurationType())) {
            getDebugSettings().setAjaxDebugModeEnabled(true);
            getDebugSettings().setDevelopmentUtilitiesEnabled(true);
        }

        //pretty url resources
        mountFiles(CssResources.BASE_PATH, CssResources.class);
        mountFiles(ImgResources.BASE_PATH, ImgResources.class);
        mountFiles(JsResources.BASE_PATH, JsResources.class);

        for (PageUrlMapping m : PageUrlMapping.values()) {
        	
        	//usually m.getPage() will not return null, this is only the case we set the url with wildcard which is then used by spring security
			if (m.getPage() != null) {
				mount(new MountedMapper(m.getUrl(), m.getPage(), m.getEncoder()));
			}
        }

        //todo design error pages...
        //error pages
//        mount(new MountedMapper("/error/401", PageUnauthorized.class, encoder));
//        mount(new MountedMapper("/error/403", PageForbidden.class, encoder));
//        mount(new MountedMapper("/error/404", PageNotFound.class, encoder));
//        mount(new MountedMapper("/error/500", PageServerError.class, encoder));
    }

    private void mountFiles(String path, Class<?> clazz) {
        try {
            String absPath = getServletContext().getRealPath("WEB-INF/classes") + "/"
                    + clazz.getPackage().getName().replace('.', '/');

            File folder = new File(absPath);
            mountFiles(path, clazz, folder);
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't mount files", ex);
        }
    }

    private void mountFiles(String path, Class<?> clazz, File folder) {
        File[] files = folder.listFiles(new ResourceFileFilter());
        for (File file : files) {
            if (!file.exists()) {
                LOGGER.warn("Couldn't mount resource {}.", new Object[]{file.getPath()});
                continue;
            }
            if (file.isDirectory()) {
                mountFiles(path + "/" + file.getName(), clazz, file);
            } else {
                mountResource(path + "/" + file.getName(), new SharedResourceReference(clazz, file.getName()));
            }
        }
    }

    public WebApplicationConfiguration getWebApplicationConfiguration() {
        if (webApplicationConfiguration == null) {
            Configuration config = configuration.getConfiguration(WEB_APP_CONFIGURATION);
            webApplicationConfiguration = new WebApplicationConfiguration(config);
        }
        return webApplicationConfiguration;
    }

    public ModelService getModel() {
        return model;
    }

    public TaskManager getTaskManager() {
        return taskManager;
    }

    public PrismContext getPrismContext() {
        return prismContext;
    }

    public Protector getProtector() {
        return protector;
    }

    @Override
    protected Class<? extends WebPage> getSignInPageClass() {
        return PageLogin.class;
    }

    @Override
    protected Class<? extends AbstractAuthenticatedWebSession> getWebSessionClass() {
        return MidPointAuthWebSession.class;
    }

    public WorkflowService getWorkflowService() {
        return workflowService;
    }

    public ModelInteractionService getModelInteractionService() {
        return modelInteractionService;
    }

    private static class ResourceFileFilter implements FilenameFilter {

        @Override
        public boolean accept(File parent, String name) {
            if (name.endsWith("png") || name.endsWith("gif")) {
                return true;
            }

            return false;
        }
    }
}

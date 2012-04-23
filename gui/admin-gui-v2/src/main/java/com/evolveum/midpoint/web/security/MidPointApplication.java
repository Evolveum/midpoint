/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */

package com.evolveum.midpoint.web.security;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.admin.configuration.*;
import com.evolveum.midpoint.web.page.admin.home.PageHome;
import com.evolveum.midpoint.web.page.admin.resources.PageResources;
import com.evolveum.midpoint.web.page.admin.roles.PageRoles;
import com.evolveum.midpoint.web.page.admin.server.PageTasks;
import com.evolveum.midpoint.web.page.admin.users.PageUser;
import com.evolveum.midpoint.web.page.admin.users.PageUsers;
import com.evolveum.midpoint.web.page.login.PageLogin;
import com.evolveum.midpoint.web.resource.css.CssResources;
import com.evolveum.midpoint.web.resource.img.ImgResources;
import com.evolveum.midpoint.web.resource.js.JsResources;
import com.evolveum.midpoint.web.util.MidPointPageParametersEncoder;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.authroles.authentication.AbstractAuthenticatedWebSession;
import org.apache.wicket.authroles.authentication.AuthenticatedWebApplication;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.mapper.MountedMapper;
import org.apache.wicket.request.resource.SharedResourceReference;
import org.apache.wicket.spring.injection.annot.SpringComponentInjector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FilenameFilter;

/**
 * @author lazyman
 */
@Component("midpointApplication")
public class MidPointApplication extends AuthenticatedWebApplication {

    private static final Trace LOGGER = TraceManager.getTrace(MidPointApplication.class);
    @Autowired
    ModelService model;
    @Autowired
    @Qualifier("repositoryService")
    RepositoryService repository;
    @Autowired
    PrismContext prismContext;
    @Autowired
    TaskManager taskManager;

    @Override
    public Class<PageHome> getHomePage() {
        return PageHome.class;
    }

    @Override
    public void init() {
        super.init();

        getComponentInstantiationListeners().add(new SpringComponentInjector(this));

        getMarkupSettings().setStripWicketTags(true);
        getResourceSettings().setThrowExceptionOnMissingResource(false);


        if (RuntimeConfigurationType.DEVELOPMENT.equals(getConfigurationType())) {
            getDebugSettings().setAjaxDebugModeEnabled(true);
            getDebugSettings().setDevelopmentUtilitiesEnabled(true);
        }

        //pretty url resources
        mountFiles("/css", CssResources.class);
        mountFiles("/img", ImgResources.class);
        mountFiles("/js", JsResources.class);

        //pretty url pages
        MidPointPageParametersEncoder encoder = new MidPointPageParametersEncoder();
        mount(new MountedMapper("/login", PageLogin.class, encoder));
        mount(new MountedMapper("/home", PageHome.class, encoder));
        mount(new MountedMapper("/admin/users", PageUsers.class, encoder));
        mount(new MountedMapper("/admin/tasks", PageTasks.class, encoder));
        mount(new MountedMapper("/admin/roles", PageRoles.class, encoder));
        mount(new MountedMapper("/admin/resources", PageResources.class, encoder));
        mount(new MountedMapper("/admin/config", PageLogging.class, encoder));
        mount(new MountedMapper("/admin/config/logging", PageLogging.class, encoder));
        mount(new MountedMapper("/admin/config/importFile", PageImportFile.class, encoder));
        mount(new MountedMapper("/admin/config/importXml", PageImportXml.class, encoder));
        mount(new MountedMapper("/admin/config/debugs", PageDebugList.class, encoder));

        mount(new MountedMapper("/admin/config/debug", PageDebugView.class,
                new OnePageParameterEncoder(PageDebugView.PARAM_OBJECT_ID)));
        mount(new MountedMapper("/admin/user", PageUser.class, new OnePageParameterEncoder(PageUser.PARAM_USER_ID)));

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
            //todo error handling
            ex.printStackTrace();
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

    public ModelService getModel() {
        return model;
    }

    public RepositoryService getRepository() {
        return repository;
    }

    public TaskManager getTaskManager() {
        return taskManager;
    }

    public PrismContext getPrismContext() {
        return prismContext;
    }

    @Override
    protected Class<? extends WebPage> getSignInPageClass() {
        return PageLogin.class;
    }

    @Override
    protected Class<? extends AbstractAuthenticatedWebSession> getWebSessionClass() {
        return MidPointAuthWebSession.class;
    }

    private static class ResourceFileFilter implements FilenameFilter {

        @Override
        public boolean accept(File parent, String name) {
            if (name.endsWith("class")) {
                return false;
            }

            return true;
        }
    }
}

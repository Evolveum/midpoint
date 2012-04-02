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

import ch.qos.logback.core.spi.ContextAware;
import com.evolveum.midpoint.web.page.admin.configuration.PageLogging;
import com.evolveum.midpoint.web.page.login.PageLogin;
import com.evolveum.midpoint.web.security.MidPointAuthWebSession;
import org.apache.wicket.authroles.authentication.AbstractAuthenticatedWebSession;
import org.apache.wicket.authroles.authentication.AuthenticatedWebApplication;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.mapper.MountedMapper;
import org.apache.wicket.spring.injection.annot.SpringComponentInjector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.web.page.admin.home.PageHome;
import com.evolveum.midpoint.web.page.admin.users.PageUsers;
import com.evolveum.midpoint.web.page.error.PageForbidden;
import com.evolveum.midpoint.web.page.error.PageNotFound;
import com.evolveum.midpoint.web.page.error.PageServerError;
import com.evolveum.midpoint.web.page.error.PageUnauthorized;
import com.evolveum.midpoint.web.util.MidPointPageParametersEncoder;

/**
 * @author lazyman
 */
@Component("midpointApplication")
public class MidPointApplication extends AuthenticatedWebApplication {

    @Autowired
    ModelService model;
    @Autowired
    @Qualifier("repositoryService")
    RepositoryService repository;
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

        //pretty url
        MidPointPageParametersEncoder encoder = new MidPointPageParametersEncoder();
        mount(new MountedMapper("/login", PageLogin.class, encoder));
        mount(new MountedMapper("/home", PageHome.class, encoder));
        mount(new MountedMapper("/admin/users", PageUsers.class, encoder));

        mount(new MountedMapper("/admin/config/logging", PageLogging.class, encoder));

        //error pages
//        mount(new MountedMapper("/error/401", PageUnauthorized.class, encoder));
//        mount(new MountedMapper("/error/403", PageForbidden.class, encoder));
//        mount(new MountedMapper("/error/404", PageNotFound.class, encoder));
//        mount(new MountedMapper("/error/500", PageServerError.class, encoder));
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

    @Override
    protected Class<? extends WebPage> getSignInPageClass() {
        return PageLogin.class;
    }

    @Override
    protected Class<? extends AbstractAuthenticatedWebSession> getWebSessionClass() {
        return MidPointAuthWebSession.class;
    }
}

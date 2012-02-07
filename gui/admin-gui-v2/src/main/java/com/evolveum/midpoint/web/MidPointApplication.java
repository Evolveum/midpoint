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

package com.evolveum.midpoint.web;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.web.page.admin.home.PageHome;
import com.evolveum.midpoint.web.page.admin.users.PageUser;
import com.evolveum.midpoint.web.page.admin.users.PageUsers;
import com.evolveum.midpoint.web.util.DefaultPageParametersEncoder;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.mapper.MountedMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author lazyman
 */
@Component("midpointApplication")
public class MidPointApplication extends WebApplication {

    @Autowired
    ModelService model;
    @Autowired
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

        getMarkupSettings().setStripWicketTags(true);
        getResourceSettings().setThrowExceptionOnMissingResource(false);

        //pretty url
        DefaultPageParametersEncoder encoder = new DefaultPageParametersEncoder();
        mount(new MountedMapper("/home", PageHome.class, encoder));
        mount(new MountedMapper("/users", PageUsers.class, encoder));
        mount(new MountedMapper("/user", PageUser.class, new OnePageParameterEncoder(PageUser.PARAM_USER_ID)));
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
}

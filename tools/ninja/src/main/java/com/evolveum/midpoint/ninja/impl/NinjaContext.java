/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.ninja.impl;

import com.beust.jcommander.JCommander;
import com.evolveum.midpoint.ninja.opts.BaseOptions;
import com.evolveum.midpoint.ninja.opts.ConnectionOptions;
import com.evolveum.midpoint.ninja.util.InitializationBeanPostprocessor;
import com.evolveum.midpoint.ninja.util.NinjaUtils;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.repo.api.RepositoryService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;

import java.nio.charset.Charset;

/**
 * Created by Viliam Repan (lazyman).
 */
public class NinjaContext {

    private static final String MIDPOINT_HOME_OPTION = "midpoint.home";

    private static final String REPOSITORY_SERVICE_BEAN = "repositoryService";

    private static final String CTX_NINJA = "classpath:ctx-ninja.xml";

    private static final String[] CTX_MIDPOINT = new String[]{
            "classpath:ctx-common.xml",
            "classpath:ctx-configuration.xml",
            "classpath:ctx-repository.xml",
            "classpath:ctx-repo-cache.xml",
            "classpath:ctx-audit.xml"
    };

    private JCommander jc;

    private GenericXmlApplicationContext context;

    private RepositoryService repository;

    private RestService restService;

    private PrismContext prismContext;

    public NinjaContext(JCommander jc) {
        this.jc = jc;
    }

    public void init(ConnectionOptions options) {
        boolean initialized = false;
        if (options.isUseWebservice()) {
            restService = setupRestService(options);
            initialized = true;
        }

        if (!initialized && options.getMidpointHome() != null) {
            repository = setupRepositoryViaMidPointHome(options);
            initialized = true;
        }

        if (!initialized) {
            throw new IllegalStateException("One of options must be specified: " + ConnectionOptions.P_MIDPOINT_HOME
                    + ", " + ConnectionOptions.P_WEBSERVICE);
        }
    }

    public void destroy() {
        if (context != null) {
            context.close();
        }
    }

    private RepositoryService setupRepositoryViaMidPointHome(ConnectionOptions options) {
        String midpointHome = options.getMidpointHome();

        String jdbcUrl = options.getUrl();
        String jdbcUsername = options.getUsername();
        String jdbcPassword = getPassword(options);

        System.setProperty(MIDPOINT_HOME_OPTION, midpointHome);

        InitializationBeanPostprocessor postprocessor = new InitializationBeanPostprocessor();
        postprocessor.setJdbcUrl(jdbcUrl);
        postprocessor.setJdbcUsername(jdbcUsername);
        postprocessor.setJdbcPassword(jdbcPassword);

        GenericXmlApplicationContext ctx = new GenericXmlApplicationContext();
        ctx.addBeanFactoryPostProcessor(beanFactory -> beanFactory.addBeanPostProcessor(postprocessor));
        ctx.load(CTX_NINJA);
        ctx.load(CTX_MIDPOINT);
        ctx.refresh();

        context = ctx;

        return context.getBean(REPOSITORY_SERVICE_BEAN, RepositoryService.class);
    }

    public ApplicationContext getApplicationContext() {
        return context;
    }

    private String getPassword(ConnectionOptions options) {
        String password = options.getPassword();
        if (password == null) {
            password = options.getAskPassword();
        }

        return password;
    }

    private RestService setupRestService(ConnectionOptions options) {
        String url = options.getUrl();
        String username = options.getUsername();
        String password = getPassword(options);

        if (url == null) {
            throw new IllegalStateException("Url is not defined");
        }

        return new RestService(url, username, password);
    }

    public JCommander getJc() {
        return jc;
    }

    public RepositoryService getRepository() {
        return repository;
    }

    public RestService getRestService() {
        return restService;
    }

    public boolean isVerbose() {
        BaseOptions base = NinjaUtils.getOptions(jc, BaseOptions.class);
        return base.isVerbose();
    }

    public Charset getCharset() {
        BaseOptions base = NinjaUtils.getOptions(jc, BaseOptions.class);
        String charset = base.getCharset();

        return Charset.forName(charset);
    }

    public PrismContext getPrismContext() {
        if (prismContext != null) {
            return prismContext;
        }

        if (context != null) {
            prismContext = context.getBean(PrismContext.class);
        }

        if (restService != null) {
            prismContext = restService.getPrismContext();
        }

        return prismContext;
    }
}

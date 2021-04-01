/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.impl;

import com.beust.jcommander.JCommander;
import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.ninja.opts.BaseOptions;
import com.evolveum.midpoint.ninja.opts.ConnectionOptions;
import com.evolveum.midpoint.ninja.util.InitializationBeanPostprocessor;
import com.evolveum.midpoint.ninja.util.Log;
import com.evolveum.midpoint.ninja.util.NinjaUtils;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.QueryConverter;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.SchemaService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;

import java.nio.charset.Charset;

/**
 * Created by Viliam Repan (lazyman).
 */
public class NinjaContext {

    private static final String REPOSITORY_SERVICE_BEAN = "repositoryService";

    private static final String CTX_NINJA = "classpath:ctx-ninja.xml";

    private static final String[] CTX_MIDPOINT = new String[] {
            "classpath:ctx-common.xml",
            "classpath:ctx-configuration.xml",
            "classpath*:ctx-repository.xml",
            "classpath:ctx-repo-cache.xml",
            "classpath:ctx-audit.xml"
    };

    private static final String[] CTX_MIDPOINT_NO_REPO = new String[] {
            "classpath:ctx-common.xml",
            "classpath:ctx-configuration-no-repo.xml"
    };

    private final JCommander jc;

    private Log log;

    private GenericXmlApplicationContext context;

    private RepositoryService repository;

    private RestService restService;

    private PrismContext prismContext;

    private SchemaService schemaService;

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

    public void setLog(Log log) {
        this.log = log;
    }

    private RepositoryService setupRepositoryViaMidPointHome(ConnectionOptions options) {
        boolean connectRepo = !options.isOffline();

        log.info("Initializing using midpoint home; {} repository connection", connectRepo ? "with" : "WITHOUT");

        System.setProperty(MidpointConfiguration.MIDPOINT_SILENT_PROPERTY, "true");

        String midpointHome = options.getMidpointHome();

        String jdbcUrl = options.getUrl();
        String jdbcUsername = options.getUsername();
        String jdbcPassword = getPassword(options);

        System.setProperty(MidpointConfiguration.MIDPOINT_HOME_PROPERTY, midpointHome);

        InitializationBeanPostprocessor postprocessor = new InitializationBeanPostprocessor();
        postprocessor.setJdbcUrl(jdbcUrl);
        postprocessor.setJdbcUsername(jdbcUsername);
        postprocessor.setJdbcPassword(jdbcPassword);

        GenericXmlApplicationContext ctx = new GenericXmlApplicationContext();
        ctx.addBeanFactoryPostProcessor(beanFactory -> beanFactory.addBeanPostProcessor(postprocessor));
        ctx.load(CTX_NINJA);
        ctx.load(connectRepo ? CTX_MIDPOINT : CTX_MIDPOINT_NO_REPO);
        ctx.refresh();

        context = ctx;

        return connectRepo ? context.getBean(REPOSITORY_SERVICE_BEAN, RepositoryService.class) : null;
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
        log.info("Initializing rest service");

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

    public SchemaService getSchemaService() {
        if (schemaService != null) {
            return schemaService;
        }

        if (context != null) {
            schemaService = context.getBean(SchemaService.class);
        }

        return schemaService;
    }

    public Log getLog() {
        return log;
    }

    public QueryConverter getQueryConverter() {
        return prismContext.getQueryConverter();
    }
}

/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.impl;

import static com.evolveum.midpoint.common.configuration.api.MidpointConfiguration.REPOSITORY_CONFIGURATION;

import java.nio.charset.Charset;

import com.beust.jcommander.JCommander;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;

import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.ninja.opts.BaseOptions;
import com.evolveum.midpoint.ninja.opts.ConnectionOptions;
import com.evolveum.midpoint.ninja.opts.PolyStringNormalizerOptions;
import com.evolveum.midpoint.ninja.util.Log;
import com.evolveum.midpoint.ninja.util.NinjaUtils;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.QueryConverter;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.sqlbase.JdbcRepositoryConfiguration;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringNormalizerConfigurationType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class NinjaContext {

    private static final String REPOSITORY_SERVICE_BEAN = "repositoryService";
    private static final String AUDIT_SERVICE_BEAN = "auditService";

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

    private ConnectionOptions connectionOptions;

    private GenericXmlApplicationContext context;

    private MidpointConfiguration midpointConfiguration;

    private RepositoryService repository;

    private AuditService auditService;

    private PrismContext prismContext;

    private SchemaService schemaService;

    public NinjaContext(JCommander jc) {
        this.jc = jc;
    }

    public void init(@NotNull ConnectionOptions options) {
        this.connectionOptions = options;
    }

    public void destroy() {
        if (context != null) {
            context.close();
        }
    }

    public void setLog(Log log) {
        this.log = log;
    }

    private void setupRepositoryViaMidPointHome(ConnectionOptions options) {
        boolean connectRepo = !options.isOffline();

        log.info("Initializing using midpoint home; {} repository connection", connectRepo ? "with" : "WITHOUT");

        System.setProperty(MidpointConfiguration.MIDPOINT_SILENT_PROPERTY, "true");

        String midpointHome = options.getMidpointHome();

        System.setProperty(MidpointConfiguration.MIDPOINT_HOME_PROPERTY, midpointHome);
        overrideRepoConfiguration(options);

        GenericXmlApplicationContext ctx = new GenericXmlApplicationContext();
        ctx.load(CTX_NINJA);
        ctx.load(connectRepo ? CTX_MIDPOINT : CTX_MIDPOINT_NO_REPO);
        ctx.refresh();

        context = ctx;

        updatePolyStringNormalizationConfiguration(ctx.getBean(PrismContext.class));
    }

    private void overrideRepoConfiguration(ConnectionOptions options) {
        if (options.getUrl() != null) {
            System.setProperty(REPOSITORY_CONFIGURATION + '.' + JdbcRepositoryConfiguration.PROPERTY_JDBC_URL,
                    options.getUrl());
            System.setProperty(REPOSITORY_CONFIGURATION + '.' + JdbcRepositoryConfiguration.PROPERTY_DATABASE,
                    getDatabase(options.getUrl()));
        }

        if (options.getUsername() != null) {
            System.setProperty(REPOSITORY_CONFIGURATION + '.' + JdbcRepositoryConfiguration.PROPERTY_JDBC_USERNAME,
                    options.getUsername());
        }

        if (options.getPassword() != null) {
            System.setProperty(REPOSITORY_CONFIGURATION + '.' + JdbcRepositoryConfiguration.PROPERTY_JDBC_PASSWORD,
                    options.getPassword());
        }

        // TODO how to override audit repo? the same options?
    }

    private String getDatabase(String url) {
        String postfix = url.replaceFirst("jdbc:", "").toLowerCase();
        if (postfix.startsWith("postgresql")) {
            return "postgresql";
        } else if (postfix.startsWith("sqlserver")) {
            return "sqlserver";
        } else if (postfix.startsWith("oracle")) {
            return "oracle";
        } else if (postfix.startsWith("h2")) {
            return "h2";
        }

        throw new IllegalStateException("Unknown database for url " + url);
    }

    public ApplicationContext getApplicationContext() {
        if (context != null) {
            return context;
        }

        setupRepositoryViaMidPointHome(connectionOptions);

        return context;
    }

    private String getPassword(ConnectionOptions options) {
        String password = options.getPassword();
        if (password == null) {
            password = options.getAskPassword();
        }

        return password;
    }

    public JCommander getJc() {
        return jc;
    }

    public MidpointConfiguration getMidpointConfiguration() {
        if (midpointConfiguration != null) {
            return midpointConfiguration;
        }

        midpointConfiguration = getApplicationContext().getBean(MidpointConfiguration.class);
        return midpointConfiguration;
    }

    public RepositoryService getRepository() {
        if (repository != null) {
            return repository;
        }

        repository = getApplicationContext().getBean(REPOSITORY_SERVICE_BEAN, RepositoryService.class);
        return repository;
    }

    public AuditService getAuditService() {
        if (auditService != null) {
            return auditService;
        }

        auditService = getApplicationContext().getBean(AUDIT_SERVICE_BEAN, AuditService.class);
        return auditService;
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

        prismContext = getApplicationContext().getBean(PrismContext.class);

        return prismContext;
    }

    private void updatePolyStringNormalizationConfiguration(PrismContext ctx) {
        if (!shouldUseCustomPolyStringNormalizer()) {
            return;
        }

        try {
            PolyStringNormalizerConfigurationType psnConfiguration = createPolyStringNormalizerConfiguration();
            ctx.configurePolyStringNormalizer(psnConfiguration);
        } catch (Exception ex) {
            throw new IllegalStateException("Couldn't setup custom PolyString normalizer configuration", ex);
        }
    }

    private PolyStringNormalizerConfigurationType createPolyStringNormalizerConfiguration() {
        BaseOptions base = NinjaUtils.getOptions(jc, BaseOptions.class);
        PolyStringNormalizerOptions opts = base.getPolyStringNormalizerOptions();

        PolyStringNormalizerConfigurationType config = new PolyStringNormalizerConfigurationType();
        config.setClassName(opts.getPsnClassName());
        config.setTrim(opts.isPsnTrim());
        config.setNfkd(opts.isPsnNfkd());
        config.setTrimWhitespace(opts.isPsnTrimWhitespace());
        config.setLowercase(opts.isPsnLowercase());

        return config;
    }

    private boolean shouldUseCustomPolyStringNormalizer() {
        BaseOptions base = NinjaUtils.getOptions(jc, BaseOptions.class);
        PolyStringNormalizerOptions opts = base.getPolyStringNormalizerOptions();

        return StringUtils.isNotEmpty(opts.getPsnClassName()) ||
                opts.isPsnTrim() != null ||
                opts.isPsnNfkd() != null ||
                opts.isPsnTrimWhitespace() != null ||
                opts.isPsnLowercase() != null;
    }

    public SchemaService getSchemaService() {
        if (schemaService != null) {
            return schemaService;
        }

        schemaService = context.getBean(SchemaService.class);

        return schemaService;
    }

    public Log getLog() {
        return log;
    }

    public QueryConverter getQueryConverter() {
        return prismContext.getQueryConverter();
    }

    public <T> T getOptions(Class<T> type) {
        return NinjaUtils.getOptions(jc, type);
    }
}

/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.impl;

import static com.evolveum.midpoint.common.configuration.api.MidpointConfiguration.REPOSITORY_CONFIGURATION;

import java.io.Closeable;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericXmlApplicationContext;

import com.evolveum.midpoint.audit.api.AuditService;
import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;
import com.evolveum.midpoint.ninja.action.BaseOptions;
import com.evolveum.midpoint.ninja.action.ConnectionOptions;
import com.evolveum.midpoint.ninja.action.PolyStringNormalizerOptions;
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
public class NinjaContext implements Closeable {

    private static final String REPOSITORY_SERVICE_BEAN = "repositoryService";
    private static final String AUDIT_SERVICE_BEAN = "auditService";

    private static final String CTX_NINJA = "classpath:ctx-ninja.xml";

    private final List<Object> options;

    private final NinjaApplicationContextLevel applicationContextLevel;

    private Log log;

    private GenericXmlApplicationContext applicationContext;

    private MidpointConfiguration midpointConfiguration;

    private RepositoryService repository;

    private AuditService auditService;

    private PrismContext prismContext;

    private SchemaService schemaService;

    private final Map<String, String> systemPropertiesBackup = new HashMap<>();

    public NinjaContext(@NotNull List<Object> options, @NotNull NinjaApplicationContextLevel applicationContextLevel) {
        this.options = options;
        this.applicationContextLevel = applicationContextLevel;
    }

    @Override
    public void close() {
        if (applicationContext != null) {
            applicationContext.close();
        }

        systemPropertiesBackup.forEach((k, v) -> {
            if (v == null) {
                System.clearProperty(k);
            } else {
                System.setProperty(k, v);
            }
        });
    }

    public void setLog(Log log) {
        this.log = log;
    }

    private void setupRepositoryViaMidPointHome(ConnectionOptions options) {
        if (applicationContextLevel == NinjaApplicationContextLevel.NONE) {
            throw new IllegalStateException("Application context shouldn't be initialized");
        }

        log.info("Initializing using midpoint home ({})", applicationContextLevel);

        backupAndUpdateSystemProperty(MidpointConfiguration.MIDPOINT_SILENT_PROPERTY, "true");

        String midpointHome = options.getMidpointHome();

        backupAndUpdateSystemProperty(MidpointConfiguration.MIDPOINT_HOME_PROPERTY, midpointHome);
        overrideRepoConfiguration(options);

        GenericXmlApplicationContext ctx = new GenericXmlApplicationContext();
        ctx.load(CTX_NINJA);
        ctx.load(applicationContextLevel.contexts);
        ctx.refresh();

        applicationContext = ctx;

        if (applicationContextLevel.containsPrismInitialization()) {
            updatePolyStringNormalizationConfiguration(ctx.getBean(PrismContext.class));
        }
    }

    private void backupAndUpdateSystemProperty(String key, String value) {
        String oldValue = System.getProperty(key);
        systemPropertiesBackup.put(key, oldValue);

        System.setProperty(key, value);
    }

    private void overrideRepoConfiguration(ConnectionOptions options) {
        if (options.getUrl() != null) {
            backupAndUpdateSystemProperty(REPOSITORY_CONFIGURATION + '.' + JdbcRepositoryConfiguration.PROPERTY_JDBC_URL,
                    options.getUrl());
            backupAndUpdateSystemProperty(REPOSITORY_CONFIGURATION + '.' + JdbcRepositoryConfiguration.PROPERTY_DATABASE,
                    getDatabase(options.getUrl()));
        }

        if (options.getUsername() != null) {
            backupAndUpdateSystemProperty(REPOSITORY_CONFIGURATION + '.' + JdbcRepositoryConfiguration.PROPERTY_JDBC_USERNAME,
                    options.getUsername());
        }

        if (options.getPassword() != null) {
            backupAndUpdateSystemProperty(REPOSITORY_CONFIGURATION + '.' + JdbcRepositoryConfiguration.PROPERTY_JDBC_PASSWORD,
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
        if (applicationContext != null) {
            return applicationContext;
        }

        ConnectionOptions opts = getOptions(ConnectionOptions.class);
        if (opts == null) {
            throw new IllegalStateException("Couldn't setup application context, ConnectionOptions is not defined (null)");
        }
        setupRepositoryViaMidPointHome(opts);

        return applicationContext;
    }

    public <T> T getOptions(Class<T> type) {
        return NinjaUtils.getOptions(options, type);
    }

    public List<Object> getAllOptions() {
        return options;
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
        BaseOptions base = getOptions(BaseOptions.class);
        return base.isVerbose();
    }

    public Charset getCharset() {
        BaseOptions base = getOptions(BaseOptions.class);
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
        BaseOptions base = getOptions(BaseOptions.class);
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
        BaseOptions base = getOptions(BaseOptions.class);
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

        schemaService = applicationContext.getBean(SchemaService.class);

        return schemaService;
    }

    public Log getLog() {
        return log;
    }

    public QueryConverter getQueryConverter() {
        return prismContext.getQueryConverter();
    }
}

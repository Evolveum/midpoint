/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.evolveum.midpoint.util.exception.ConfigurationException;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.CacheInvalidationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionProfileCompiler;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.CacheRegistry;
import com.evolveum.midpoint.repo.api.Cache;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.expression.ExpressionProfiles;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import static com.evolveum.midpoint.schema.GetOperationOptions.readOnly;
import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asObjectable;

/**
 * Cache for system object such as SystemConfigurationType. This is a global cache,
 * independent of the request. It will store the system configuration in memory.
 * It will check for system configuration updates in regular interval using the
 * getVersion() method.
 * <p>
 * This supplements the RepositoryCache. RepositoryCache works on per-request
 * (per-operation) basis. The  SystemObjectCache is global. Its goal is to reduce
 * the number of getObject(SystemConfiguration) and the getVersion(SystemConfiguration)
 * calls.
 * <p>
 * In the future: May be used for more objects that are often used and seldom
 * changed, e.g. object templates.
 * <p>
 * TODO: use real repo instead of repo cache
 *
 * @author semancik
 */
@Component
public class SystemObjectCache implements Cache {

    private static final Trace LOGGER = TraceManager.getTrace(SystemObjectCache.class);
    private static final Trace LOGGER_CONTENT = TraceManager.getTrace(SystemObjectCache.class.getName() + ".content");

    private static final String DOT_CLASS = SystemObjectCache.class.getName() + ".";

    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService cacheRepositoryService;

    @Autowired private CacheRegistry cacheRegistry;

    private PrismObject<SystemConfigurationType> systemConfiguration;
    private Long systemConfigurationCheckTimestamp;
    private PrismObject<SecurityPolicyType> securityPolicy;
    private Long securityPolicyCheckTimestamp;

    /** Compiled expression profiles (from system configuration). */
    private volatile ExpressionProfiles expressionProfiles;

    @PostConstruct
    public void register() {
        cacheRegistry.registerCache(this);
    }

    @PreDestroy
    public void unregister() {
        cacheRegistry.unregisterCache(this);
    }

    private long getSystemConfigurationExpirationMillis() {
        return 1000;
    }

    private long getSecurityPolicyExpirationMillis() {
        return 1000;
    }

    public @Nullable SystemConfigurationType getSystemConfigurationBean(OperationResult result) throws SchemaException {
        return asObjectable(
                getSystemConfiguration(result));
    }

    public synchronized @Nullable PrismObject<SystemConfigurationType> getSystemConfiguration(OperationResult result)
            throws SchemaException {
        try {
            if (!hasValidSystemConfiguration(result)) {
                LOGGER.trace("Cache MISS: reading system configuration from the repository: {}, version {}",
                        systemConfiguration, systemConfiguration == null ? null : systemConfiguration.getVersion());
                loadSystemConfiguration(result);
            } else {
                LOGGER.trace("Cache HIT: reusing cached system configuration: {}, version {}",
                        systemConfiguration, systemConfiguration == null ? null : systemConfiguration.getVersion());
            }
        } catch (ObjectNotFoundException e) {
            systemConfiguration = null;
            LOGGER.trace("Cache ERROR: System configuration not found", e);
            result.clearLastSubresultError(); // e.g. because of tests
        }
        return systemConfiguration;
    }

    private boolean hasValidSystemConfiguration(OperationResult result) throws ObjectNotFoundException, SchemaException {
        if (systemConfiguration == null) {
            return false;
        }
        String cachedVersion = systemConfiguration.getVersion();
        if (cachedVersion == null) {
            return false;
        }
        if (systemConfigurationCheckTimestamp == null) {
            return false;
        }
        if (System.currentTimeMillis() < systemConfigurationCheckTimestamp + getSystemConfigurationExpirationMillis()) {
            return true;
        }
        String repoVersion = cacheRepositoryService.getVersion(
                SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(), result);
        if (cachedVersion.equals(repoVersion)) {
            systemConfigurationCheckTimestamp = System.currentTimeMillis();
            return true;
        }
        return false;
    }

    private void loadSystemConfiguration(OperationResult result) throws ObjectNotFoundException, SchemaException {
        var systemConfiguration = cacheRepositoryService.getObject(
                SystemConfigurationType.class,
                SystemObjectsType.SYSTEM_CONFIGURATION.value(),
                GetOperationOptionsBuilder.create()
                        .readOnly()
                        .allowNotFound()
                        .build(),
                result);
        if (systemConfiguration.getVersion() == null) {
            LOGGER.warn("Retrieved system configuration with null version");
        }
        systemConfiguration.freeze();
        expressionProfiles = null;
        this.systemConfiguration = systemConfiguration;
        systemConfigurationCheckTimestamp = System.currentTimeMillis();
    }

    public synchronized PrismObject<SecurityPolicyType> getSecurityPolicy() throws SchemaException {
        OperationResult result = new OperationResult(DOT_CLASS + "getSecurityPolicy");
        PrismObject<SystemConfigurationType> systemConfiguration = getSystemConfiguration(result);
        if (systemConfiguration == null) {
            securityPolicy = null;
            return null;
        } else if (systemConfiguration.asObjectable().getGlobalSecurityPolicyRef() == null
                || systemConfiguration.asObjectable().getGlobalSecurityPolicyRef().getOid() == null) {
            securityPolicy = null;
            String message = "Cache ERROR: System configuration doesn't contains auth policy";
            LOGGER.trace(message);
            result.recordFatalError(message);
            return null;
        }

        String securityPolicyOid = systemConfiguration.asObjectable().getGlobalSecurityPolicyRef().getOid();
        try {
            if (!hasValidSecurityPolicy(result, securityPolicyOid)) {
                LOGGER.trace("Cache MISS: reading security policy from the repository: {}, oid: {}, version {}",
                        securityPolicy, securityPolicyOid, securityPolicy == null ? null : securityPolicy.getVersion());
                loadSecurityPolicy(result, securityPolicyOid);
            } else {
                LOGGER.trace("Cache HIT: reusing cached security policy: {}, oid: {}, version {}",
                        securityPolicy, securityPolicyOid, securityPolicy == null ? null : securityPolicy.getVersion());
            }
        } catch (ObjectNotFoundException e) {
            securityPolicy = null;
            LOGGER.trace("Cache ERROR: Security policy with oid " + securityPolicyOid + " not found", e);
            result.muteLastSubresultError();
        }
        return securityPolicy;
    }

    private boolean hasValidSecurityPolicy(OperationResult result, String oid) throws ObjectNotFoundException, SchemaException {
        if (securityPolicy == null) {
            return false;
        }
        if (securityPolicy.getVersion() == null) {
            return false;
        }
        if (securityPolicyCheckTimestamp == null) {
            return false;
        }

        if (!securityPolicy.getOid().equals(oid)) {
            return false;
        }

        if (System.currentTimeMillis() < securityPolicyCheckTimestamp + getSecurityPolicyExpirationMillis()) {
            return true;
        }
        String repoVersion = cacheRepositoryService.getVersion(SecurityPolicyType.class, oid, result);
        if (securityPolicy.getVersion().equals(repoVersion)) {
            securityPolicyCheckTimestamp = System.currentTimeMillis();
            return true;
        }
        return false;
    }

    private void loadSecurityPolicy(OperationResult result, String oid) throws ObjectNotFoundException, SchemaException {
        securityPolicy =
                cacheRepositoryService
                        .getObject(SecurityPolicyType.class, oid, readOnly(), result)
                        .doFreeze();
        expressionProfiles = null;
        securityPolicyCheckTimestamp = System.currentTimeMillis();
        if (securityPolicy != null && securityPolicy.getVersion() == null) {
            LOGGER.warn("Retrieved auth policy with null version");
        }
    }

    public synchronized void invalidateCaches() {
        systemConfiguration = null;
        expressionProfiles = null;
        securityPolicy = null;
        securityPolicyCheckTimestamp = null;
    }

    @SuppressWarnings("WeakerAccess")
    public PrismObject<ArchetypeType> getArchetype(String oid, OperationResult result) throws ObjectNotFoundException, SchemaException {
        return cacheRepositoryService.getObject(ArchetypeType.class, oid, readOnly(), result);
    }

    public SearchResultList<PrismObject<ArchetypeType>> getAllArchetypes(OperationResult result) throws SchemaException {
        return cacheRepositoryService.searchObjects(ArchetypeType.class, null, readOnly(), result);
    }

    public synchronized @NotNull ExpressionProfile getExpressionProfile(@NotNull String identifier, OperationResult result)
            throws SchemaException, ConfigurationException {
        if (expressionProfiles == null) {
            expressionProfiles = compileExpressionProfiles(result);
        }
        return expressionProfiles.getProfile(identifier);
    }

    private ExpressionProfiles compileExpressionProfiles(OperationResult result) throws SchemaException, ConfigurationException {
        SystemConfigurationType systemConfiguration = getSystemConfigurationBean(result);
        if (systemConfiguration == null) {
            // This should only happen in tests - if ever. Empty expression profiles are just fine.
            return new ExpressionProfiles(List.of());
        }
        SystemConfigurationExpressionsType expressions = systemConfiguration.getExpressions();
        if (expressions == null) {
            return new ExpressionProfiles(List.of());
        } else {
            return new ExpressionProfileCompiler().compile(expressions);
        }
    }

    // We could use SystemConfigurationChangeListener instead but in the future there could be more object types
    // managed by this class.
    @Override
    public synchronized void invalidate(Class<?> type, String oid, CacheInvalidationContext context) {
        // We ignore OID for now
        if (type == null || type.isAssignableFrom(SystemConfigurationType.class)) {
            invalidateCaches();
        }
        expressionProfiles = null;
    }

    @NotNull
    @Override
    public Collection<SingleCacheStateInformationType> getStateInformation() {
        return Collections.singleton(new SingleCacheStateInformationType()
                .name(SystemObjectCache.class.getName())
                .size(getSize())
        );
    }

    private int getSize() {
        ExpressionProfiles cachedProfiles = this.expressionProfiles;

        return (systemConfiguration != null ? 1 : 0) +
                (cachedProfiles != null ? cachedProfiles.size() : 0);
    }

    @Override
    public void dumpContent() {
        if (LOGGER_CONTENT.isInfoEnabled()) {
            PrismObject<SystemConfigurationType> cachedConfiguration = this.systemConfiguration;
            if (cachedConfiguration != null) {
                LOGGER_CONTENT.info("Cached system configuration: {} (version {}); systemConfigurationCheckTimestamp: {}",
                        cachedConfiguration, cachedConfiguration.getVersion(),
                        XmlTypeConverter.createXMLGregorianCalendar(systemConfigurationCheckTimestamp));
            } else {
                LOGGER_CONTENT.info("No cached system configuration");
            }

            ExpressionProfiles cachedProfiles = this.expressionProfiles;
            if (cachedProfiles != null) {
                cachedProfiles.getProfiles().forEach((k, v) -> LOGGER_CONTENT.info("Cached expression profile: {}: {}", k, v));
            }
        }
    }
}

/*
 * Copyright (c) 2016-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common;

import com.evolveum.midpoint.CacheInvalidationContext;
import com.evolveum.midpoint.model.common.expression.ExpressionProfileCompiler;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.cache.CacheRegistry;
import com.evolveum.midpoint.repo.api.Cacheable;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.expression.ExpressionProfiles;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Collection;
import java.util.Collections;

/**
 * Cache for system object such as SystemConfigurationType. This is a global cache,
 * independent of the request. It will store the system configuration in memory.
 * It will check for system configuration updates in regular interval using the
 * getVersion() method.
 *
 * This supplements the RepositoryCache. RepositoryCache works on per-request
 * (per-operation) basis. The  SystemObjectCache is global. Its goal is to reduce
 * the number of getObject(SystemConfiguration) and the getVersion(SystemConfiguration)
 * calls.
 *
 * In the future: May be used for more objects that are often used and seldom
 * changed, e.g. object templates.
 *
 * TODO: use real repo instead of repo cache
 *
 * @author semancik
 */
@Component
public class SystemObjectCache implements Cacheable {

    private static final Trace LOGGER = TraceManager.getTrace(SystemObjectCache.class);

    @Autowired
    @Qualifier("cacheRepositoryService")
    private transient RepositoryService cacheRepositoryService;

    @Autowired private CacheRegistry cacheRegistry;
    @Autowired private PrismContext prismContext;

    private PrismObject<SystemConfigurationType> systemConfiguration;
    private Long systemConfigurationCheckTimestamp;
    private PrismObject<SecurityPolicyType> securityPolicy;
    private Long securityPolicyCheckTimestamp;

    private ExpressionProfiles expressionProfiles;

    @PostConstruct
    public void register() {
        cacheRegistry.registerCacheableService(this);
    }

    @PreDestroy
    public void unregister() {
        cacheRegistry.unregisterCacheableService(this);
    }

    private long getSystemConfigurationExpirationMillis() {
        return 1000;
    }

    private long getSecurityPolicyExpirationMillis() {
        return 1000;
    }

    public synchronized PrismObject<SystemConfigurationType> getSystemConfiguration(OperationResult result) throws SchemaException {
        try {
            if (!hasValidSystemConfiguration(result)) {
                LOGGER.trace("Cache MISS: reading system configuration from the repository: {}, version {}",
                        systemConfiguration, systemConfiguration==null?null:systemConfiguration.getVersion());
                loadSystemConfiguration(result);
            } else {
                LOGGER.trace("Cache HIT: reusing cached system configuration: {}, version {}",
                        systemConfiguration, systemConfiguration==null?null:systemConfiguration.getVersion());
            }
        } catch (ObjectNotFoundException e) {
            systemConfiguration = null;
            LOGGER.trace("Cache ERROR: System configuration not found", e);
            result.muteLastSubresultError();
        }
        return systemConfiguration;
    }


    private boolean hasValidSystemConfiguration(OperationResult result) throws ObjectNotFoundException, SchemaException {
        if (systemConfiguration == null) {
            return false;
        }
        if (systemConfiguration.getVersion() == null) {
            return false;
        }
        if (systemConfigurationCheckTimestamp == null) {
            return false;
        }
        if (System.currentTimeMillis() < systemConfigurationCheckTimestamp + getSystemConfigurationExpirationMillis()) {
            return true;
        }
        String repoVersion = cacheRepositoryService.getVersion(SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(),
                result);
        if (systemConfiguration.getVersion().equals(repoVersion)) {
            systemConfigurationCheckTimestamp = System.currentTimeMillis();
            return true;
        }
        return false;
    }

    private void loadSystemConfiguration(OperationResult result) throws ObjectNotFoundException, SchemaException {
        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createReadOnly());
        systemConfiguration = cacheRepositoryService.getObject(SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(),
                options, result);
        expressionProfiles = null;
        systemConfigurationCheckTimestamp = System.currentTimeMillis();
        if (systemConfiguration != null && systemConfiguration.getVersion() == null) {
            LOGGER.warn("Retrieved system configuration with null version");
        }
    }

    public synchronized PrismObject<SecurityPolicyType> getSecurityPolicy(OperationResult result) throws SchemaException {
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
                        securityPolicy, securityPolicyOid, securityPolicy ==null?null: securityPolicy.getVersion());
                loadSecurityPolicy(result, securityPolicyOid);
            } else {
                LOGGER.trace("Cache HIT: reusing cached security policy: {}, oid: {}, version {}",
                        securityPolicy, securityPolicyOid, securityPolicy ==null?null: securityPolicy.getVersion());
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
        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(GetOperationOptions.createReadOnly());
        securityPolicy = cacheRepositoryService.getObject(SecurityPolicyType.class, oid, options, result);
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

    public PrismObject<ArchetypeType> getArchetype(String oid, OperationResult result) throws ObjectNotFoundException, SchemaException {
        // TODO: make this efficient (use cache)
        return cacheRepositoryService.getObject(ArchetypeType.class, oid, null, result);
    }

    public SearchResultList<PrismObject<ArchetypeType>> getAllArchetypes(OperationResult result) throws SchemaException {
        // TODO: make this efficient (use cache)
        return cacheRepositoryService.searchObjects(ArchetypeType.class, null, null, result);
    }

    public ExpressionProfile getExpressionProfile(String identifier, OperationResult result) throws SchemaException {
        if (identifier == null) {
            return null;
        }
        if (expressionProfiles == null) {
            compileExpressionProfiles(result);
        }
        return expressionProfiles.getProfile(identifier);
    }

    private void compileExpressionProfiles(OperationResult result) throws SchemaException {
        PrismObject<SystemConfigurationType> systemConfiguration = getSystemConfiguration(result);
        if (systemConfiguration == null) {
            // This should only happen in tests - if ever. Empty expression profiles are just fine.
            expressionProfiles = new ExpressionProfiles();
            return;
        }
        SystemConfigurationExpressionsType expressions = systemConfiguration.asObjectable().getExpressions();
        if (expressions == null) {
            // Mark that there is no need to recompile. There are no profiles.
            expressionProfiles = new ExpressionProfiles();
            return;
        }
        ExpressionProfileCompiler compiler = new ExpressionProfileCompiler();
        expressionProfiles = compiler.compile(expressions);
    }

    // We could use SystemConfigurationChangeListener instead but in the future there could be more object types
    // managed by this class.
    @Override
    public void invalidate(Class<?> type, String oid, CacheInvalidationContext context) {
        // We ignore OID for now
        if (type == null || type.isAssignableFrom(SystemConfigurationType.class)) {
            invalidateCaches();
        }
    }

    @NotNull
    @Override
    public Collection<SingleCacheStateInformationType> getStateInformation() {
        return Collections.singleton(new SingleCacheStateInformationType(prismContext)
                .name(SystemObjectCache.class.getName())
                .size(getSize())
        );
    }

    private int getSize() {
        return (systemConfiguration != null ? 1 : 0) +
                (expressionProfiles != null ? expressionProfiles.size() : 0);
    }
}

/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.cache;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.schema.cache.CacheConfiguration.CacheObjectTypeConfiguration;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.CacheStatisticsClassificationType.PER_CACHE;
import static java.util.Collections.emptySet;

/**
 *  TODO consider better place for this component
 */
@Component
public class CacheConfigurationManager {

    private static final Trace LOGGER = TraceManager.getTrace(CacheConfigurationManager.class);

    private static final String DEFAULT_CACHING_PROFILE_RESOURCE_NAME = "/default-caching-profile.xml";
    private static final String ALL_TYPES_MARKER = "__ALL__";

    @Autowired private PrismContext prismContext;

    private CachingProfileType defaultCachingProfile;

    private CachingConfigurationType currentGlobalConfiguration;

    private boolean wrongConfiguration;
    private Map<CacheType, CacheConfiguration> compiledGlobalConfigurations; // not null if !wrongConfiguration

    private final ThreadLocal<ThreadLocalConfiguration> threadLocalConfiguration = new ThreadLocal<>();

    @PostConstruct
    public void initialize() {
        URL resourceUrl = getClass().getResource(DEFAULT_CACHING_PROFILE_RESOURCE_NAME);
        if (resourceUrl == null) {
            throw new IllegalStateException("Default caching profile in resource '" + DEFAULT_CACHING_PROFILE_RESOURCE_NAME + "' was not found");
        }
        try {
            LOGGER.info("Creating initial caching configuration based on default caching profile (system configuration is not known at this moment)");
            InputStream stream = resourceUrl.openStream();
            defaultCachingProfile = prismContext.parserFor(stream).xml().parseRealValue(CachingProfileType.class);
            stream.close();
            compiledGlobalConfigurations = compileConfigurations(null, emptySet());
        } catch (SchemaException | IOException e) {
            throw new SystemException("Couldn't read and parse default caching profile: " + e.getMessage(), e);
        }
    }

    public synchronized void applyCachingConfiguration(SystemConfigurationType systemConfiguration) {
        InternalsConfigurationType internals = systemConfiguration != null ? systemConfiguration.getInternals() : null;
        CachingConfigurationType configurationToApply = internals != null ? internals.getCaching() : null;
        if (PrismUtil.realValueEquals(currentGlobalConfiguration, configurationToApply)) {
            LOGGER.trace("Caching configuration was not changed, skipping the update");
            return;
        }

        currentGlobalConfiguration = configurationToApply;
        try {
            if (validateSystemConfiguration()) {
                LOGGER.info("Applying caching configuration: {} profile(s)",
                        currentGlobalConfiguration != null ? currentGlobalConfiguration.getProfile().size() : 0);
                compiledGlobalConfigurations = compileConfigurations(currentGlobalConfiguration, emptySet());
                wrongConfiguration = false;
            } else {
                compiledGlobalConfigurations = null;
                wrongConfiguration = true;
            }
        } catch (Throwable t) {
            // Couldn't we do the update without setting global config before the application is done?
            // We wouldn't need clearing that information in case of error.
            currentGlobalConfiguration = null;
            wrongConfiguration = true;
            throw t;
        }
    }

    private boolean validateSystemConfiguration() {
        if (currentGlobalConfiguration != null) {
            Set<String> profiles = new HashSet<>();
            for (CachingProfileType profile : currentGlobalConfiguration.getProfile()) {
                if (isEnabled(profile)) {
                    String profileName = profile.getName();
                    if (profileName != null) {
                        if (profiles.contains(profileName)) {
                            LOGGER.error("Caching profile {} is defined multiple times in system configuration", profileName);
                            return false;
                        } else {
                            profiles.add(profileName);
                        }
                    }
                }
            }
        }
        return true;
    }

    private boolean isEnabled(CachingProfileType profile) {
        return !Boolean.FALSE.equals(profile.isEnabled());
    }

    private boolean isGlobal(CachingProfileType profile) {
        return Boolean.TRUE.equals(profile.isGlobal());
    }

    public CacheConfiguration getConfiguration(CacheType type) {
        if (wrongConfiguration) {
            LOGGER.error("Cache configuration is wrong. Please fix this as soon as possible.");
            return null;
        }
        ThreadLocalConfiguration localConfiguration = this.threadLocalConfiguration.get();
        if (localConfiguration != null) {
            return localConfiguration.get(type);
        } else {
            return compiledGlobalConfigurations.get(type);
        }
    }

    class ThreadLocalConfiguration implements DebugDumpable {
        Map<CacheType, CacheConfiguration> preparedConfigurations;
        CachingConfigurationType configurationsPreparedFrom;
        @NotNull final Collection<String> profiles;

        ThreadLocalConfiguration(Collection<String> profiles) {
            this.profiles = profiles != null ? new ArrayList<>(profiles) : Collections.emptyList();
        }

        CacheConfiguration get(CacheType type) {
            if (wrongConfiguration) {
                LOGGER.error("Cache configuration is wrong. Please fix this as soon as possible.");
                return null;
            }
            CachingConfigurationType global = currentGlobalConfiguration;     // copied locally to be sure it is the same
            if (preparedConfigurations == null || configurationsPreparedFrom != global) {
                preparedConfigurations = compileConfigurations(global, profiles);
                configurationsPreparedFrom = global;
            }
            return preparedConfigurations.get(type);
        }

        @Override
        public String debugDump(int indent) {
            StringBuilder sb = new StringBuilder();
            DebugUtil.debugDumpWithLabelLn(sb, "profiles", profiles, indent);
            if (preparedConfigurations != null) {
                preparedConfigurations.forEach((type, config) ->
                        DebugUtil.debugDumpWithLabelLn(sb, "config for " + type, config, indent));
            }
            return sb.toString();
        }
    }

    public void setThreadLocalProfiles(@NotNull Collection<String> profiles) {
        if (wrongConfiguration) {
            throw new IllegalStateException("Couldn't set thread-local caching profiles because system cache configuration is wrong.");
        }
        validateProfiles(profiles);
        threadLocalConfiguration.set(new ThreadLocalConfiguration(profiles));
        LOGGER.trace("Thread local configuration profiles were set to {}", profiles);
    }

    private void validateProfiles(Collection<String> profiles) {
        if (!profiles.isEmpty()) {
            if (currentGlobalConfiguration == null) {
                throw new IllegalArgumentException("Cache profile(s) " + profiles + " couldn't be resolved, because global caching configuration is not defined");
            }
            Set<String> allProfiles = currentGlobalConfiguration.getProfile().stream()
                    .map(p -> p.getName())
                    .collect(Collectors.toSet());
            Set<String> undefinedProfiles = profiles.stream()
                    .filter(p -> !allProfiles.contains(p))
                    .collect(Collectors.toSet());
            if (!undefinedProfiles.isEmpty()) {
                throw new IllegalArgumentException("Cache profile(s) " + undefinedProfiles + " couldn't be resolved, because global caching configuration is not defined");
            }
        }
    }

    public void unsetThreadLocalProfiles() {
        threadLocalConfiguration.remove();
        LOGGER.trace("Thread local configuration profiles were removed");
    }

    @NotNull
    private Map<CacheType, CacheConfiguration> compileConfigurations(@Nullable CachingConfigurationType configuration, Collection<String> profiles) {
        try {
            List<CachingProfileType> relevantProfiles = getRelevantProfiles(configuration, profiles);
            Map<CacheType, CacheConfiguration> rv = new HashMap<>();
            addProfile(rv, defaultCachingProfile);
            for (CachingProfileType profile : relevantProfiles) {
                addProfile(rv, profile);
            }
            if (shouldTrace(configuration)) {
                LOGGER.info("Compiled configurations (profiles = {}):", profiles);
                for (Map.Entry<CacheType, CacheConfiguration> entry : rv.entrySet()) {
                    LOGGER.info("  {}:\n{}", entry.getKey(), entry.getValue().debugDump(2));
                }
            }
            return rv;
        } catch (SchemaException e) {
            // Although technically this is (still) a SchemaException, for upper layers it is something the cannot deal with:
            // we can only report it as unmanageable obstacle. Reporting it as a checked exception causes more trouble than
            // benefit.
            throw new SystemException("Couldn't compile cache configuration: " + e.getMessage(), e);
        }
    }

    private boolean shouldTrace(@Nullable CachingConfigurationType configuration) {
        return configuration != null && Boolean.TRUE.equals(configuration.isTraceConfiguration());
    }

    private void addProfile(Map<CacheType, CacheConfiguration> aggregate, CachingProfileType profile) throws SchemaException {
        if (profile.getLocalRepoCache() != null) {
            if (profile.getLocalRepoObjectCache() != null || profile.getLocalRepoVersionCache() != null || profile.getLocalRepoQueryCache() != null) {
                throw new SchemaException("Both localRepoCache and specific repo caches are specified in profile " + profile.getName());
            }
            addCacheSettings(aggregate, CacheType.LOCAL_REPO_OBJECT_CACHE, profile.getLocalRepoCache());
            addCacheSettings(aggregate, CacheType.LOCAL_REPO_VERSION_CACHE, profile.getLocalRepoCache());
            addCacheSettings(aggregate, CacheType.LOCAL_REPO_QUERY_CACHE, profile.getLocalRepoCache());
        } else {
            addCacheSettings(aggregate, CacheType.LOCAL_REPO_OBJECT_CACHE, profile.getLocalRepoObjectCache());
            addCacheSettings(aggregate, CacheType.LOCAL_REPO_VERSION_CACHE, profile.getLocalRepoVersionCache());
            addCacheSettings(aggregate, CacheType.LOCAL_REPO_QUERY_CACHE, profile.getLocalRepoQueryCache());
        }

        if (profile.getGlobalRepoCache() != null) {
            if (profile.getGlobalRepoObjectCache() != null || profile.getGlobalRepoVersionCache() != null || profile.getGlobalRepoQueryCache() != null) {
                throw new SchemaException("Both globalRepoCache and specific repo caches are specified in profile " + profile.getName());
            }
            addCacheSettings(aggregate, CacheType.GLOBAL_REPO_OBJECT_CACHE, profile.getGlobalRepoCache());
            addCacheSettings(aggregate, CacheType.GLOBAL_REPO_VERSION_CACHE, profile.getGlobalRepoCache());
            addCacheSettings(aggregate, CacheType.GLOBAL_REPO_QUERY_CACHE, profile.getGlobalRepoCache());
        } else {
            addCacheSettings(aggregate, CacheType.GLOBAL_REPO_OBJECT_CACHE, profile.getGlobalRepoObjectCache());
            addCacheSettings(aggregate, CacheType.GLOBAL_REPO_VERSION_CACHE, profile.getGlobalRepoVersionCache());
            addCacheSettings(aggregate, CacheType.GLOBAL_REPO_QUERY_CACHE, profile.getGlobalRepoQueryCache());
        }

        addCacheSettings(aggregate, CacheType.LOCAL_FOCUS_CONSTRAINT_CHECKER_CACHE, profile.getLocalFocusConstraintCheckerCache());
        addCacheSettings(aggregate, CacheType.LOCAL_SHADOW_CONSTRAINT_CHECKER_CACHE, profile.getLocalShadowConstraintCheckerCache());
        addCacheSettings(aggregate, CacheType.LOCAL_ASSOCIATION_TARGET_SEARCH_EVALUATOR_CACHE, profile.getLocalAssociationTargetSearchEvaluatorCache());
        //addCacheSettings(aggregate, CacheType.LOCAL_DEFAULT_SEARCH_EVALUATOR_CACHE, profile.getLocalDefaultSearchEvaluatorCache());
    }

    private void addCacheSettings(Map<CacheType, CacheConfiguration> aggregate, CacheType cacheType, CacheSettingsType settings)
            throws SchemaException {
        if (settings != null) {
            CacheConfiguration matching = aggregate.get(cacheType);
            if (matching == null || Boolean.FALSE.equals(settings.isAppend())) {
                aggregate.put(cacheType, parseSettings(settings));
            } else {
                mergeSettings(matching, settings);
            }
        }
    }

    private CacheConfiguration parseSettings(CacheSettingsType settings) throws SchemaException {
        CacheConfiguration rv = new CacheConfiguration();
        mergeSettings(rv, settings);
        return rv;
    }

    @SuppressWarnings("Duplicates")
    private void mergeSettings(CacheConfiguration configuration, CacheSettingsType increment) throws SchemaException {
        if (increment.getMaxSize() != null) {
            configuration.setMaxSize(increment.getMaxSize());
        }
        if (increment.getTimeToLive() != null) {
            configuration.setTimeToLive(increment.getTimeToLive());
        }
        CacheStatisticsReportingConfigurationType statistics = increment.getStatistics();
        if (statistics != null) {
            configuration.setStatisticsLevel(convertStatisticsLevel(statistics));
        }
        if (increment.isTraceMiss() != null) {
            configuration.setTraceMiss(increment.isTraceMiss());
        }
        if (increment.isTracePass() != null) {
            configuration.setTracePass(increment.isTracePass());
        }
        CacheInvalidationConfigurationType invalidation = increment.getInvalidation();
        if (invalidation != null) {
            if (invalidation.isClusterwide() != null) {
                configuration.setClusterwideInvalidation(invalidation.isClusterwide());
            }
            if (invalidation.getApproximation() != null) {
                configuration.setSafeRemoteInvalidation(invalidation.getApproximation() != CacheInvalidationApproximationType.MINIMAL);
            }
        }
        var objectTypeMap = configuration.getObjectTypes();
        var objectClassMap = configuration.getObjectClasses();
        for (CacheObjectTypeSettingsType typeSpecificSetting : increment.getObjectTypeSettings()) {
            // Processing object types this configuration applies to
            List<QName> specifiedObjectTypes = typeSpecificSetting.getObjectType();
            Collection<Class<?>> applyToTypes = !specifiedObjectTypes.isEmpty() ?
                    resolveClassNames(specifiedObjectTypes) :
                    new ArrayList<>(objectTypeMap.keySet()); // empty object type list means "apply to all types in parent"
            for (Class<?> objectType : applyToTypes) {
                objectTypeMap.put(
                        objectType,
                        mergeTypeSpecificSettings(objectTypeMap.get(objectType), typeSpecificSetting, configuration));
            }
            // Processing object classes this configuration applies to
            List<QName> specifiedObjectClasses = typeSpecificSetting.getShadowObjectClass();
            Collection<QName> applyToClasses = !specifiedObjectClasses.isEmpty() ?
                    specifiedObjectClasses : new ArrayList<>(objectClassMap.keySet()); // empty means "apply to all types in parent"
            for (QName objectClassName : applyToClasses) {
                objectClassMap.put(
                        objectClassName,
                        mergeTypeSpecificSettings(objectClassMap.get(objectClassName), typeSpecificSetting, configuration));
            }
        }
    }

    @SuppressWarnings("Duplicates")
    private CacheObjectTypeConfiguration mergeTypeSpecificSettings(
            CacheObjectTypeConfiguration original, CacheObjectTypeSettingsType increment,
            CacheConfiguration configuration) {
        CacheObjectTypeConfiguration rv = original != null ? original : configuration.new CacheObjectTypeConfiguration();
        if (increment.getTimeToLive() != null) {
            rv.setTimeToLive(increment.getTimeToLive());
        }
        if (increment.getTimeToVersionCheck() != null) {
            rv.setTimeToVersionCheck(increment.getTimeToVersionCheck());
        }
        if (increment.getStatistics() != null) {
            rv.setStatisticsLevel(convertStatisticsLevel(increment.getStatistics()));
        }
        if (increment.isTraceMiss() != null) {
            rv.setTraceMiss(increment.isTraceMiss());
        }
        if (increment.isTracePass() != null) {
            rv.setTracePass(increment.isTracePass());
        }
        CacheInvalidationConfigurationType invalidation = increment.getInvalidation();
        if (invalidation != null) {
            if (invalidation.isClusterwide() != null) {
                rv.setClusterwideInvalidation(invalidation.isClusterwide());
            }
            if (invalidation.getApproximation() != null) {
                rv.setSafeRemoteInvalidation(invalidation.getApproximation() != CacheInvalidationApproximationType.MINIMAL);
            }
        }
        return rv;
    }

    private CacheConfiguration.StatisticsLevel convertStatisticsLevel(CacheStatisticsReportingConfigurationType statistics) {
        if (statistics.getCollection() == CacheStatisticsCollectionStyleType.NONE) {
            return CacheConfiguration.StatisticsLevel.SKIP;
        } else {
            return switch (ObjectUtils.defaultIfNull(statistics.getClassification(), PER_CACHE)) {
                case PER_CACHE -> CacheConfiguration.StatisticsLevel.PER_CACHE;
                case PER_CACHE_AND_OBJECT_TYPE -> CacheConfiguration.StatisticsLevel.PER_OBJECT_TYPE;
            };
        }
    }

    private Collection<Class<?>> resolveClassNames(List<QName> names) throws SchemaException {
        Collection<Class<?>> rv = new HashSet<>();
        for (QName name : names) {
            if (ALL_TYPES_MARKER.equals(name.getLocalPart())) {
                for (ObjectTypes objectType : ObjectTypes.values()) {
                    rv.add(objectType.getClassDefinition());
                }
            } else {
                Class<?> objectClass = prismContext.getSchemaRegistry().determineCompileTimeClass(name);
                if (objectClass == null) {
                    throw new SchemaException("Unknown class: " + name);
                }
                rv.add(objectClass);
            }
        }
        return rv;
    }

    private List<CachingProfileType> getRelevantProfiles(CachingConfigurationType configuration, Collection<String> localProfileNames) {
        List<CachingProfileType> rv = new ArrayList<>();
        Set<String> profileNamesSeen = new HashSet<>();
        if (configuration != null) {
            for (CachingProfileType profile : configuration.getProfile()) {
                profileNamesSeen.add(profile.getName());
                if (isEnabled(profile) && (isGlobal(profile) || localProfileNames.contains(profile.getName()))) {
                    rv.add(profile.clone());
                }
            }
        }
        rv.sort(Comparator.comparing(CachingProfileType::getOrder, Comparator.nullsLast(Integer::compare)));

        // are all localProfileNames known? (Actually this should be already checked; this is only to be really sure.)
        if (!CollectionUtils.isSubCollection(localProfileNames, profileNamesSeen)) {
            throw new IllegalStateException("Some thread-local caching profile names are unknown: local: " + localProfileNames + ", known: " + profileNamesSeen);
        }
        return rv;
    }

    @Nullable
    public String dumpThreadLocalConfiguration(boolean full) {
        ThreadLocalConfiguration localConfig = threadLocalConfiguration.get();
        if (localConfig != null) {
            if (full) {
                // just to compile configurations
                Arrays.stream(CacheType.values()).forEach(localConfig::get);
                return localConfig.debugDump();
            } else {
                return "Profiles: " + localConfig.profiles;
            }
        } else {
            return null;
        }
    }
}

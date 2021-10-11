/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.traces;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;

@Experimental
public enum PerformanceCategory {
    REPOSITORY("Repo", "Repository (all)",
            "com.evolveum.midpoint.repo.api.*",
            "com.evolveum.midpoint.repo.impl.*"),
    REPOSITORY_READ("Repo:R", "Repository (read)",
            "com.evolveum.midpoint.repo.api.RepositoryService.getObject",
            "com.evolveum.midpoint.repo.api.RepositoryService.getVersion",
            "com.evolveum.midpoint.repo.api.RepositoryService.searchObjects",
            "com.evolveum.midpoint.repo.api.RepositoryService.searchObjectsIterative",
            "com.evolveum.midpoint.repo.api.RepositoryService.listAccountShadowOwner",
            "com.evolveum.midpoint.repo.api.RepositoryService.searchContainers",
            "com.evolveum.midpoint.repo.api.RepositoryService.countContainers",
            "com.evolveum.midpoint.repo.api.RepositoryService.listResourceObjectShadows",
            "com.evolveum.midpoint.repo.api.RepositoryService.countObjects",
            "com.evolveum.midpoint.repo.api.RepositoryService.searchShadowOwner"
            // TODO impl
            ),
    REPOSITORY_WRITE("Repo:W", "Repository (write)",
            "com.evolveum.midpoint.repo.api.RepositoryService.addObject",
            "com.evolveum.midpoint.repo.api.RepositoryService.modifyObject",
            "com.evolveum.midpoint.repo.api.RepositoryService.deleteObject",
            "com.evolveum.midpoint.repo.api.RepositoryService.advanceSequence",
            "com.evolveum.midpoint.repo.api.RepositoryService.returnUnusedValuesToSequence",
            "com.evolveum.midpoint.repo.api.RepositoryService.addDiagnosticInformation"),
    REPOSITORY_OTHER("Repo:O", "Repository (other)", Arrays.asList(REPOSITORY), Arrays.asList(REPOSITORY_READ, REPOSITORY_WRITE)),
    REPOSITORY_CACHE("RCache", "Repository cache (all)",
            "com.evolveum.midpoint.repo.cache.RepositoryCache.*"),
    REPOSITORY_CACHE_READ("RCache:R", "Repository cache (read)",
            "com.evolveum.midpoint.repo.cache.RepositoryCache.getObject",
            "com.evolveum.midpoint.repo.cache.RepositoryCache.getVersion",
            "com.evolveum.midpoint.repo.cache.RepositoryCache.searchObjects",
            "com.evolveum.midpoint.repo.cache.RepositoryCache.searchObjectsIterative",
            "com.evolveum.midpoint.repo.cache.RepositoryCache.listAccountShadowOwner",
            "com.evolveum.midpoint.repo.cache.RepositoryCache.searchContainers",
            "com.evolveum.midpoint.repo.cache.RepositoryCache.countContainers",
            "com.evolveum.midpoint.repo.cache.RepositoryCache.listResourceObjectShadows",
            "com.evolveum.midpoint.repo.cache.RepositoryCache.countObjects",
            "com.evolveum.midpoint.repo.cache.RepositoryCache.searchShadowOwner"),
    REPOSITORY_CACHE_WRITE("RCache:R", "Repository cache (write)",
            "com.evolveum.midpoint.repo.cache.RepositoryCache.addObject",
            "com.evolveum.midpoint.repo.cache.RepositoryCache.modifyObject",
            "com.evolveum.midpoint.repo.cache.RepositoryCache.deleteObject",
            "com.evolveum.midpoint.repo.cache.RepositoryCache.advanceSequence",
            "com.evolveum.midpoint.repo.cache.RepositoryCache.returnUnusedValuesToSequence",
            "com.evolveum.midpoint.repo.cache.RepositoryCache.addDiagnosticInformation"),
    REPOSITORY_CACHE_OTHER("RCache:O", "Repository cache (other)", Arrays.asList(REPOSITORY_CACHE), Arrays.asList(REPOSITORY_CACHE_READ, REPOSITORY_CACHE_WRITE)),
    MAPPING_EVALUATION("Map", "Mapping evaluation", "com.evolveum.midpoint.model.common.mapping.MappingImpl.evaluate"),
    ICF("ConnId", "ConnId (all)", "org.identityconnectors.framework.api.ConnectorFacade.*"),
    ICF_READ("ConnId:R", "ConnId (read)",
            "org.identityconnectors.framework.api.ConnectorFacade.getObject",
            "org.identityconnectors.framework.api.ConnectorFacade.sync",
            "org.identityconnectors.framework.api.ConnectorFacade.search"),
    ICF_WRITE("ConnId:W", "ConnId (write)",
            "org.identityconnectors.framework.api.ConnectorFacade.create",
            "org.identityconnectors.framework.api.ConnectorFacade.updateDelta",
            "org.identityconnectors.framework.api.ConnectorFacade.addAttributeValues",
            "org.identityconnectors.framework.api.ConnectorFacade.update",
            "org.identityconnectors.framework.api.ConnectorFacade.removeAttributeValues",
            "org.identityconnectors.framework.api.ConnectorFacade.delete"),
    ICF_SCHEMA("ConnId:S", "ConnId (schema)",
            "org.identityconnectors.framework.api.ConnectorFacade.getSupportedOperations",
            "org.identityconnectors.framework.api.ConnectorFacade.schema"),
    ICF_OTHER("ConnId:O", "ConnId (other)", Arrays.asList(ICF), Arrays.asList(ICF_READ, ICF_WRITE, ICF_SCHEMA));

    private final String shortLabel;
    private final String label;
    private final List<PerformanceCategory> plus, minus;
    private final List<String> patterns;
    private final List<Pattern> compiledPatterns;

    PerformanceCategory(String shortLabel, String label, List<PerformanceCategory> plus, List<PerformanceCategory> minus) {
        this.shortLabel = shortLabel;
        this.label = label;
        this.plus = plus;
        this.minus = minus;
        this.patterns = null;
        this.compiledPatterns = null;
    }

    PerformanceCategory(String shortLabel, String label, String... patterns) {
        this.shortLabel = shortLabel;
        this.label = label;
        this.plus = this.minus = null;
        this.patterns = Arrays.asList(patterns);
        this.compiledPatterns = new ArrayList<>();
        for (String pattern : patterns) {
            String regex = toRegex(pattern);
            compiledPatterns.add(Pattern.compile(regex));
        }
    }

    public String getShortLabel() {
        return shortLabel;
    }

    public String getLabel() {
        return label;
    }

    public boolean isDerived() {
        return plus != null || minus != null;
    }

    public List<PerformanceCategory> getPlus() {
        return plus;
    }

    public List<PerformanceCategory> getMinus() {
        return minus;
    }

    private String toRegex(String pattern) {
        return pattern.replace(".", "\\.").replace("*", ".*");
    }

    public boolean matches(OperationResultType operation) {
        for (Pattern pattern : compiledPatterns) {
            if (pattern.matcher(operation.getOperation()).matches()) {
                return true;
            }
        }
        return false;
    }

}

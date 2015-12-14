package com.evolveum.midpoint.web.page.admin.certification.dto;

import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import javax.xml.namespace.QName;

/**
 * Created by Kate on 13.12.2015.
 */
public class DefinitionScopeDto {

    public static final String F_NAME = "name";
    public static final String F_DESCRIPTION = "description";
    public static final String F_OBJECT_TYPE = "objectType";
    public static final String F_SEARCH_FILTER = "searchFilter";
    public static final String F_INCLUDE_ASSIGNMENTS = "includeAssignments";
    public static final String F_INCLUDE_INDUCEMENTS = "includeInducements";
    public static final String F_INCLUDE_RESOURCES = "includeResources";
    public static final String F_INCLUDE_ROLES = "includeRoles";
    public static final String F_INCLUDE_ORGS = "includeOrgs";
    public static final String F_ENABLED_ITEMS_ONLY = "enabledItemsOnly";

    private String name;
    private String description;
    private QName objectType;
    private SearchFilterType searchFilter;
    private boolean includeAssignments;
    private boolean includeInducements;
    private boolean includeResources;
    private boolean includeRoles;
    private boolean includeOrgs;
    private boolean enabledItemsOnly;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public QName getObjectType() {
        return objectType;
    }

    public void setObjectType(QName objectType) {
        this.objectType = objectType;
    }

    public SearchFilterType getSearchFilter() {
        return searchFilter;
    }

    public void setSearchFilter(SearchFilterType searchFilter) {
        this.searchFilter = searchFilter;
    }

    public boolean isIncludeAssignments() {
        return includeAssignments;
    }

    public void setIncludeAssignments(boolean includeAssignments) {
        this.includeAssignments = includeAssignments;
    }

    public boolean isIncludeInducements() {
        return includeInducements;
    }

    public void setIncludeInducements(boolean includeInducements) {
        this.includeInducements = includeInducements;
    }

    public boolean isIncludeResources() {
        return includeResources;
    }

    public void setIncludeResources(boolean includeResources) {
        this.includeResources = includeResources;
    }

    public boolean isIncludeRoles() {
        return includeRoles;
    }

    public void setIncludeRoles(boolean includeRoles) {
        this.includeRoles = includeRoles;
    }

    public boolean isIncludeOrgs() {
        return includeOrgs;
    }

    public void setIncludeOrgs(boolean includeOrgs) {
        this.includeOrgs = includeOrgs;
    }

    public boolean isEnabledItemsOnly() {
        return enabledItemsOnly;
    }

    public void setEnabledItemsOnly(boolean enabledItemsOnly) {
        this.enabledItemsOnly = enabledItemsOnly;
    }
}

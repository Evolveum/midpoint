/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.certification.dto;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.PrismQuerySerialization;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Kate
 */
public class DefinitionScopeDto implements Serializable {

    public static final String F_NAME = "name";
    public static final String F_DESCRIPTION = "description";
    public static final String F_OBJECT_TYPE = "objectType";
    public static final String F_SEARCH_FILTER_TEXT = "searchFilterText";
    public static final String F_INCLUDE_ASSIGNMENTS = "includeAssignments";
    public static final String F_INCLUDE_INDUCEMENTS = "includeInducements";
    public static final String F_INCLUDE_RESOURCES = "includeResources";
    public static final String F_INCLUDE_ROLES = "includeRoles";
    public static final String F_INCLUDE_ORGS = "includeOrgs";
    public static final String F_INCLUDE_SERVICES = "includeServices";
    public static final String F_INCLUDE_USERS = "includeUsers";
    public static final String F_INCLUDE_POLICIES = "includePolicies";
    public static final String F_RELATION_LIST = "relationList";
    public static final String F_INCLUDE_ENABLED_ITEMS_ONLY = "enabledItemsOnly";

    private String name;
    private String description;
    private DefinitionScopeObjectType objectType;
    private String searchFilterText;
    private boolean includeAssignments;
    private boolean includeInducements;
    private boolean includeResources;
    private boolean includeRoles;
    private boolean includeOrgs;
    private boolean includeServices;
    private boolean includeUsers;
    private boolean includePolicies;
    private boolean enabledItemsOnly;
    private ExpressionType itemSelectionExpression;
    private List<QName> relationList = new ArrayList<>();

    public void loadSearchFilter(SearchFilterType searchFilterType, PrismContext prismContext)  {
        if (searchFilterType == null) {
            return;
        }

        if (searchFilterType.getText() != null) {
            searchFilterText = searchFilterType.getText();
        } else if (searchFilterType.getFilterClauseXNode() != null) {
            try {
                QName objectTypeQname = objectType != null ? new QName(objectType.name()) : FocusType.COMPLEX_TYPE;
                Class<?> objectType = WebComponentUtil.qnameToClass(objectTypeQname);

                ObjectFilter objectFilter = prismContext.getQueryConverter().createObjectFilter(objectType, searchFilterType);
                PrismQuerySerialization serializer = PrismContext.get().querySerializer().serialize(objectFilter);
                searchFilterText = serializer.filterText();
            } catch (Exception e) {
                throw new SystemException("Cannot serialize search filter " + searchFilterType + ": " + e.getMessage(), e);
            }
        }
    }

    public SearchFilterType getParsedSearchFilter(PrismContext context) {
        if (searchFilterText == null || searchFilterText.isEmpty()) {
            return null;
        }

        SearchFilterType rv = new SearchFilterType();
        rv.setText(searchFilterText);
        return rv;
    }

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

    public DefinitionScopeObjectType getObjectType() {
        return objectType;
    }

    public void setObjectType(DefinitionScopeObjectType objectType) {
        this.objectType = objectType;
    }

    public String getSearchFilterText() {
        return searchFilterText;
    }

    public void setSearchFilterText(String searchFilterText) {
        this.searchFilterText = searchFilterText;
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

    public boolean isIncludeServices() {
        return includeServices;
    }

    public void setIncludeServices(boolean includeServices) {
        this.includeServices = includeServices;
    }

    public boolean isIncludeUsers() {
        return includeUsers;
    }

    public void setIncludeUsers(boolean includeUsers) {
        this.includeUsers = includeUsers;
    }

    public boolean isIncludePolicies() {
        return includePolicies;
    }

    public void setIncludePolicies(boolean includePolicies) {
        this.includePolicies = includePolicies;
    }

    public boolean isEnabledItemsOnly() {
        return enabledItemsOnly;
    }

    public void setEnabledItemsOnly(boolean enabledItemsOnly) {
        this.enabledItemsOnly = enabledItemsOnly;
    }

    public ExpressionType getItemSelectionExpression() {
        return itemSelectionExpression;
    }

    public void setItemSelectionExpression(ExpressionType itemSelectionExpression) {
        this.itemSelectionExpression = itemSelectionExpression;
    }

    public List<QName> getRelationList() {
        return relationList;
    }

    public void setRelationList(List<QName> relationList) {
        this.relationList = relationList;
    }
}

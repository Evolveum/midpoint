/*
 * Copyright (c) 2010-2016 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.page.admin.certification.dto;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.xnode.RootXNode;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import javax.xml.namespace.QName;
import java.io.Serializable;
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
    private boolean enabledItemsOnly;
	private ExpressionType itemSelectionExpression;
	private List<QName> relationList;

	public void loadSearchFilter(SearchFilterType searchFilterType, PrismContext prismContext)  {
        if (searchFilterType == null) {
            return;
        }

        try {
            RootXNode clause = searchFilterType.getFilterClauseAsRootXNode();
            searchFilterText = prismContext.xmlSerializer().serialize(clause);
        } catch (SchemaException e) {
            throw new SystemException("Cannot serialize search filter " + searchFilterType + ": " + e.getMessage(), e);
        }
    }

    public SearchFilterType getParsedSearchFilter(PrismContext context) {
        if (searchFilterText == null || searchFilterText.isEmpty()) {
            return null;
        }

        SearchFilterType rv = new SearchFilterType();
        RootXNode filterClauseNode;
        try {
            filterClauseNode = (RootXNode) context.parserFor(searchFilterText).xml().parseToXNode();
        } catch (SchemaException e) {
            throw new SystemException("Cannot parse search filter " + searchFilterText + ": " + e.getMessage(), e);
        }
        rv.setFilterClauseXNode(filterClauseNode);
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

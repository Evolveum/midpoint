package com.evolveum.midpoint.web.component.search.refactored;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.web.page.admin.roles.SearchBoxConfigurationHelper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SearchConfigurationWrapper<C extends Containerable> implements Serializable {

    private SearchBoxConfigurationType config;
    private Class<C> typeClass;
    private List<AbstractSearchItemWrapper> itemsList = new ArrayList<>();
    private SearchBoxModeType searchBoxMode;

    public static final String F_INDIRECT = "config.indirectConfiguration.indirect";
    public static final String F_SCOPE = "config.scopeConfiguration.defaultValue";
    public static final String F_RELATION = "config.relationConfiguration.defaultValue";
    public static final String F_TENANT = "tenantRef";
    public static final String F_PROJECT = "projectRef";

    private ObjectReferenceType projectRef;
    private ObjectReferenceType tenantRef;

    public SearchConfigurationWrapper(Class<C> typeClass, SearchBoxConfigurationType config) {
        this.config = config;
        this.typeClass = typeClass;
        searchBoxMode = config.getDefaultMode();
    }

    public SearchBoxConfigurationType getConfig() {
        return config;
    }

    public void setConfig(SearchBoxConfigurationType config) {
        this.config = config;
    }

    public SearchBoxModeType getSearchBoxMode() {
        return searchBoxMode;
    }

    public void setSearchBoxMode(SearchBoxModeType searchBoxMode) {
        this.searchBoxMode = searchBoxMode;
    }

    public Class<C> getTypeClass() {
        return typeClass;
    }

    public void setTypeClass(Class<C> typeClass) {
        this.typeClass = typeClass;
    }

    public List<AbstractSearchItemWrapper> getItemsList() {
        return itemsList;
    }

    public void addSearchItem(AbstractSearchItemWrapper searchItem) {
        itemsList.add(searchItem);
    }

    public Boolean getIndirect() {
        return config.getIndirectConfiguration() != null ? config.getIndirectConfiguration().isIndirect() : null;
    }

    public void setIndirect(Boolean indirect) {
        if (config.getIndirectConfiguration() == null) {
            config.setIndirectConfiguration(new IndirectSearchItemConfigurationType());
        }
        config.getIndirectConfiguration().setIndirect(indirect);
    }

    public SearchBoxScopeType getScope() {
        return config.getScopeConfiguration() != null ? config.getScopeConfiguration().getDefaultValue() : SearchBoxScopeType.ONE_LEVEL;
    }

    public void setScope(SearchBoxScopeType scope) {
        if (config.getScopeConfiguration() == null) {
            config.setScopeConfiguration(new ScopeSearchItemConfigurationType());
        }
        config.getScopeConfiguration().setDefaultValue(scope);
    }

    public boolean isIndirect() {
        return config.getIndirectConfiguration() != null
                && config.getIndirectConfiguration().isIndirect() != null && config.getIndirectConfiguration().isIndirect().equals(Boolean.TRUE);
    }

    public List<QName> getAvailableRelations() {
        return config.getRelationConfiguration() != null ? config.getRelationConfiguration().getSupportedRelations() : new ArrayList<>();
    }

    public boolean isSearchScope(SearchBoxScopeType scopeType) {
        return config.getScopeConfiguration() != null && config.getScopeConfiguration().getDefaultValue().equals(scopeType);
    }

    public ObjectReferenceType getProjectRef() {
        return projectRef;
    }

    public void setProjectRef(ObjectReferenceType projectRef) {
        this.projectRef = projectRef;
    }

    public ObjectReferenceType getTenantRef() {
        return tenantRef;
    }

    public void setTenantRef(ObjectReferenceType tenantRef) {
        this.tenantRef = tenantRef;
    }

    public boolean isTenantEmpty() {
        return tenantRef == null || tenantRef.getOid() == null || tenantRef.getOid() == null || tenantRef.asReferenceValue().isEmpty();
    }

    public boolean isProjectEmpty() {
        return projectRef == null || projectRef.getOid() == null || projectRef.getOid() == null || projectRef.asReferenceValue().isEmpty();
    }
}

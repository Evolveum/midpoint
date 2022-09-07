package com.evolveum.midpoint.gui.impl.component.search;

import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.RelationTypes;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.collections4.CollectionUtils;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SearchConfigurationWrapper<C extends Containerable> implements Serializable {

    private Class<C> typeClass;

    private List<Class<? extends Containerable>> allowedTypeList = new ArrayList<>();
    private List<AbstractSearchItemWrapper> itemsList = new ArrayList<>();
    private SearchBoxModeType defaultSearchBoxMode;
    private List<SearchBoxModeType> allowedModeList = new ArrayList<>();
    private List<QName> supportedRelations = new ArrayList<>();
    private QName defaultRelation;

    private SearchBoxScopeType defaultScope;

    private boolean indirect;

    private String collectionViewName;

    private String collectionRefOid;
    private boolean allowAllTypeSearch;

    private boolean allowToConfigureSearchItems;
    public static final String F_INDIRECT = "indirect";
    public static final String F_SCOPE = "defaultScope";
    public static final String F_RELATION = "defaultRelation";
    public static final String F_TENANT = "tenantRef";
    public static final String F_PROJECT = "projectRef";

    private ObjectReferenceType projectRef;
    private ObjectReferenceType tenantRef;

    public SearchConfigurationWrapper(Class<C> typeClass, ModelServiceLocator modelServiceLocator) {
        this.typeClass = typeClass;
    }

   public SearchConfigurationWrapper(Class<C> typeClass, SearchBoxConfigurationType searchBoxConfig, ModelServiceLocator modelServiceLocator) {
        this.typeClass = typeClass;
        if (searchBoxConfig.getObjectTypeConfiguration() != null) {
            searchBoxConfig.getObjectTypeConfiguration().getSupportedTypes()
                    .forEach(type -> {
                        allowedTypeList.add((Class<? extends Containerable>) WebComponentUtil.qnameToClass(PrismContext.get(), type));
                    });

        }
        defaultSearchBoxMode = searchBoxConfig.getDefaultMode();
        searchBoxConfig.getAllowedMode().forEach(mode -> allowedModeList.add(mode));
        defaultScope = searchBoxConfig.getScopeConfiguration() != null ? searchBoxConfig.getScopeConfiguration().getDefaultValue()
                : searchBoxConfig.getDefaultScope();
        if (searchBoxConfig.getRelationConfiguration() != null) {
            defaultRelation = searchBoxConfig.getRelationConfiguration().getDefaultValue() != null ?
                    searchBoxConfig.getRelationConfiguration().getDefaultValue() : RelationTypes.MEMBER.getRelation();
            searchBoxConfig.getRelationConfiguration().getSupportedRelations()
                    .forEach(relation -> supportedRelations.add(relation));
        }
        if (searchBoxConfig.getIndirectConfiguration() != null && searchBoxConfig.getIndirectConfiguration().isIndirect() != null) {
            indirect = searchBoxConfig.getIndirectConfiguration().isIndirect();
        }
        if  (searchBoxConfig.getProjectConfiguration() != null) {
            //todo
        }
        if (searchBoxConfig.getTenantConfiguration() != null) {
            //todo
        }
        if (searchBoxConfig.isAllowToConfigureSearchItems() != null) {
            allowToConfigureSearchItems = searchBoxConfig.isAllowToConfigureSearchItems();
        }
        if (searchBoxConfig.getSearchItems() != null && CollectionUtils.isNotEmpty(searchBoxConfig.getSearchItems().getSearchItem())) {
            searchBoxConfig.getSearchItems().getSearchItem().forEach(item -> {
                PropertySearchItemWrapper itemWrapper = SearchFactory.createPropertySearchItemWrapper(typeClass, item, null, null,
                        modelServiceLocator);
                if (itemWrapper != null) {
                    itemsList.add(itemWrapper);
                }
            });
        }
    }

    public SearchConfigurationWrapper(Class<C> typeClass, String collectionViewName) {
        this.typeClass = typeClass;
        this.collectionViewName = collectionViewName;
     }

    public SearchBoxModeType getDefaultSearchBoxMode() {
        return defaultSearchBoxMode;
    }

    public void setDefaultSearchBoxMode(SearchBoxModeType defaultSearchBoxMode) {
        this.defaultSearchBoxMode = defaultSearchBoxMode;
    }

    public List<SearchBoxModeType> getAllowedModeList() {
        return allowedModeList;
    }

    public SearchConfigurationWrapper addAllowedMode(SearchBoxModeType mode) {
        if (!allowedModeList.contains(mode)) {
            allowedModeList.add(mode);
        }
        return this;
    }

    public List<Class<? extends Containerable>> getAllowedTypeList() {
        return allowedTypeList;
    }

    public String getCollectionViewName() {
        return collectionViewName;
    }

    public void setCollectionViewName(String collectionViewName) {
        this.collectionViewName = collectionViewName;
    }

    public String getCollectionRefOid() {
        return collectionRefOid;
    }

    public void setCollectionRefOid(String collectionRefOid) {
        this.collectionRefOid = collectionRefOid;
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

    public SearchBoxScopeType getDefaultScope() {
        return defaultScope;
    }

    public void setDefaultScope(SearchBoxScopeType defaultScope) {
        this.defaultScope = defaultScope;
    }

    public QName getDefaultRelation() {
        return defaultRelation;
    }

    public void setDefaultRelation(QName defaultRelation) {
        this.defaultRelation = defaultRelation;
    }


    public boolean isAllowToConfigureSearchItems() {
        return allowToConfigureSearchItems;
    }

    public void setAllowToConfigureSearchItems(boolean allowToConfigureSearchItems) {
        this.allowToConfigureSearchItems = allowToConfigureSearchItems;
    }

    public boolean isIndirect() {
        //todo fix
        return indirect;
//        return config.getIndirectConfiguration() != null
//                && config.getIndirectConfiguration().isIndirect() != null && config.getIndirectConfiguration().isIndirect().equals(Boolean.TRUE);
    }

    public List<QName> getSupportedRelations() {
        return supportedRelations; //todo fix config.getRelationConfiguration() != null ? config.getRelationConfiguration().getSupportedRelations() : new ArrayList<>();
    }

    public boolean isSearchScope(SearchBoxScopeType scopeType) {
        return getDefaultScope() != null && getDefaultScope().equals(scopeType);
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

    public boolean isAllowAllTypeSearch() {
        return allowAllTypeSearch;
    }

    public void setAllowAllTypeSearch(boolean allowAllTypeSearch) {
        this.allowAllTypeSearch = allowAllTypeSearch;
    }

    public SearchConfigurationWrapper<C> removePropertySearchItem(ItemPath path) {
        if (path == null) {
            return this;
        }
        Iterator<AbstractSearchItemWrapper> it = getItemsList().iterator();
        while (it.hasNext()) {
            AbstractSearchItemWrapper item = it.next();
            if (!(item instanceof PropertySearchItemWrapper)) {
                continue;
            }
            if (path.equivalent(((PropertySearchItemWrapper) item).getPath())) {
                it.remove();
                return this;
            }
        }
        return this;
    }
}

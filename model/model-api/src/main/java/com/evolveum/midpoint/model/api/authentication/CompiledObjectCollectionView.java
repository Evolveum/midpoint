/*
 * Copyright (c) 2018-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api.authentication;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.query_3.PagingType;

import org.apache.commons.lang3.BooleanUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.annotation.Experimental;

import org.jetbrains.annotations.Nullable;

import static com.evolveum.midpoint.prism.Referencable.getOid;

/**
 * Compiled form of either object collection view or an implicit object collection, such as (e.g.) an {@link ArchetypeType}.
 *
 * (TODO is this correct?)
 *
 * @author semancik
 */
@Experimental
public class CompiledObjectCollectionView implements DebugDumpable, Serializable {
    private static final long serialVersionUID = 1L;

    private QName containerType;
    private String viewIdentifier;

    private List<GuiActionType> actions = new ArrayList<>();
    private CollectionRefSpecificationType collection;
    private List<GuiObjectColumnType> columns = new ArrayList<>();
    private DisplayType display;
    private DistinctSearchOptionType distinct;
    private Boolean disableSorting;
    private Boolean disableCounting;
    private SearchBoxConfigurationType searchBoxConfiguration;
    private ObjectFilter filter;
    private ObjectFilter domainFilter;
    private Integer displayOrder;
    private Integer refreshInterval;
    private Collection<SelectorOptions<GetOperationOptions>> options;
    private Collection<SelectorOptions<GetOperationOptions>> domainOptions;
    private PagingType paging;
    private PagingOptionsType pagingOptions;
    private PolyString name;

    private UserInterfaceElementVisibilityType visibility;
    private OperationTypeType applicableForOperation;
    private Boolean includeDefaultColumns;

    private String objectCollectionDescription;

    private boolean defaultView;

    // Only used to construct "default" view definition. May be not needed later on.
    public CompiledObjectCollectionView() {
        containerType = null;
        viewIdentifier = null;
    }

    public CompiledObjectCollectionView(QName objectType, String viewIdentifier) {
        this.containerType = objectType;
        this.viewIdentifier = viewIdentifier;
    }

    public QName getContainerType() {
        return containerType;
    }

    public void setContainerType(QName containerType) {
        this.containerType = containerType;
    }

    public <T> Class<T> getTargetClass() {
        if (containerType == null) {
            return null;
        }
        return SchemaRegistry.get().determineClassForType(containerType);
    }

    public String getViewIdentifier() {
        return viewIdentifier;
    }

    public void setViewIdentifier(String viewIdentifier) {
        this.viewIdentifier = viewIdentifier;
    }

    @NotNull
    public List<GuiActionType> getActions() {
        return actions;
    }

    public CollectionRefSpecificationType getCollection() {
        return collection;
    }

    public void setCollection(CollectionRefSpecificationType collection) {
        this.collection = collection;
    }

    /**
     * Returns column definition list (already ordered).
     * May return empty list if there is no definition. Which means that default columns should be used.
     */
    public List<GuiObjectColumnType> getColumns() {
        return columns;
    }

    public DisplayType getDisplay() {
        return display;
    }

    public void setDisplay(DisplayType display) {
        this.display = display;
    }

    public DistinctSearchOptionType getDistinct() {
        return distinct;
    }

    public void setDistinct(DistinctSearchOptionType distinct) {
        this.distinct = distinct;
    }

    public Boolean isDisableSorting() {
        return disableSorting;
    }

    public Boolean getDisableSorting() {
        return disableSorting;
    }

    public void setDisableSorting(Boolean disableSorting) {
        this.disableSorting = disableSorting;
    }

    public Boolean isDisableCounting() {
        return disableCounting;
    }

    public void setDisableCounting(Boolean disableCounting) {
        this.disableCounting = disableCounting;
    }

    public SearchBoxConfigurationType getSearchBoxConfiguration() {
        return searchBoxConfiguration;
    }

    public void setSearchBoxConfiguration(SearchBoxConfigurationType searchBoxConfiguration) {
        this.searchBoxConfiguration = searchBoxConfiguration;
    }

    public ObjectFilter getFilter() {
        //be careful to use filter with expressions. Expression is not evaluated still
        return filter;
    }

    public void setFilter(ObjectFilter filter) {
        this.filter = filter;
    }

    public ObjectFilter getDomainFilter() {
        return domainFilter;
    }

    public void setDomainFilter(ObjectFilter domainFilter) {
        this.domainFilter = domainFilter;
    }

    public boolean hasDomain() {
        return domainFilter != null;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public boolean match(QName expectedObjectType, String expectedViewIdentifier) {
        if (!QNameUtil.match(containerType, expectedObjectType)) {
            return false;
        }
        if (expectedViewIdentifier == null) {
            return isDefaultView();
        }
        return expectedViewIdentifier.equals(viewIdentifier);
    }

    public boolean match(QName expectedObjectType) {
        return QNameUtil.match(containerType, expectedObjectType);
    }


    private boolean isAllObjectsView() {
        return collection == null;
    }

    public void setRefreshInterval(Integer refreshInterval) {
        this.refreshInterval = refreshInterval;
    }

    public Integer getRefreshInterval() {
        return refreshInterval;
    }

    public void setOptions(Collection<SelectorOptions<GetOperationOptions>> options) {
        this.options = options;
    }

    public Collection<SelectorOptions<GetOperationOptions>> getOptions() {
        return options;
    }

    public void setDomainOptions(Collection<SelectorOptions<GetOperationOptions>> domainOptions) {
        this.domainOptions = domainOptions;
    }

    public Collection<SelectorOptions<GetOperationOptions>> getDomainOptions() {
        return domainOptions;
    }

    public String getObjectCollectionDescription() {
        return objectCollectionDescription;
    }

    public void setObjectCollectionDescription(String objectCollectionDescription) {
        this.objectCollectionDescription = objectCollectionDescription;
    }

    public void setPaging(PagingType paging) {
        this.paging = paging;
    }

    public PagingType getPaging() {
        return paging;
    }

    public void setPagingOptions(PagingOptionsType pagingOptions) {
        this.pagingOptions = pagingOptions;
    }

    public PagingOptionsType getPagingOptions() {
        return pagingOptions;
    }

    public void setVisibility(UserInterfaceElementVisibilityType visibility) {
        this.visibility = visibility;
    }

    public UserInterfaceElementVisibilityType getVisibility() {
        return visibility;
    }

    public void setApplicableForOperation(OperationTypeType applicableForOperation) {
        this.applicableForOperation = applicableForOperation;
    }

    public OperationTypeType getApplicableForOperation() {
        return applicableForOperation;
    }

    public void setIncludeDefaultColumns(Boolean includeDefaultColumns) {
        this.includeDefaultColumns = includeDefaultColumns;
    }

    public Boolean getIncludeDefaultColumns() {
        return includeDefaultColumns;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(CompiledObjectCollectionView.class, indent);
        DebugUtil.debugDumpWithLabelLn(sb, "containerType", containerType, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "viewIdentifier", viewIdentifier, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "actions", actions, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "columns", columns, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "includeDefaultColumns", includeDefaultColumns, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "display", display, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "distinct", distinct, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "disableSorting", disableSorting, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "disableCounting", disableCounting, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "searchBoxConfiguration", searchBoxConfiguration, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "filter", filter, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "domainFilter", domainFilter, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "displayOrder", displayOrder, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "refreshInterval", refreshInterval, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "visibility", visibility, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "applicableForOperation", applicableForOperation, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "objectCollectionDescription", objectCollectionDescription, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "paging", paging, indent + 1);
        return sb.toString();
    }

    public GuiObjectListViewType toGuiObjectListViewType() {
        GuiObjectListViewType viewType = new GuiObjectListViewType();
        viewType.setIdentifier(getViewIdentifier());
        viewType.setType(getContainerType());
        viewType.setDisplay(getDisplay() != null ? getDisplay().clone() : null);
        for (GuiObjectColumnType column : getColumns()) {
            viewType.column(column.clone());
        }
        for (GuiActionType action : getActions()) {
            viewType.action(action.clone());
        }
        viewType.setDistinct(getDistinct());
        viewType.setDisableSorting(isDisableSorting());
        viewType.setDisableCounting(isDisableCounting());
        viewType.setSearchBoxConfiguration(getSearchBoxConfiguration() != null ? getSearchBoxConfiguration().clone() : null);
        viewType.setDisplayOrder(getDisplayOrder());
        viewType.setRefreshInterval(getRefreshInterval());
        viewType.setPaging(getPaging()!= null ? getPaging().clone() : null);
        viewType.setVisibility(getVisibility());
        viewType.setApplicableForOperation(getApplicableForOperation());
        viewType.setIncludeDefaultColumns(getIncludeDefaultColumns());
        return viewType;
    }

    public boolean isApplicableForOperation(OperationTypeType operationTypeType) {
        if (applicableForOperation == null) { //applicable for both add and modify operation
            return true;
        }

        return operationTypeType == applicableForOperation;
    }

    public boolean isIncludeDefaultColumns() {
        return BooleanUtils.isTrue(includeDefaultColumns);
    }

    public boolean isDefaultView() {
        return defaultView;
    }

    public void setDefaultView(boolean defaultView) {
        this.defaultView = defaultView;
    }

    /** Returns a reference to the archetype if this collection view is archetype-based. */
    public @Nullable ObjectReferenceType getArchetypeRef() {
        if (collection == null) {
            return null;
        }
        ObjectReferenceType collectionRef = collection.getCollectionRef();
        if (collectionRef != null && QNameUtil.match(ArchetypeType.COMPLEX_TYPE, collectionRef.getType())) {
            return collectionRef;
        } else {
            return null;
        }
    }

    public @Nullable String getArchetypeOid() {
        return getOid(getArchetypeRef());
    }

    public CompiledObjectCollectionView clone() {
        CompiledObjectCollectionView clone = new CompiledObjectCollectionView(containerType, viewIdentifier);
        clone.actions = CloneUtil.cloneCollectionMembers(actions);
        clone.collection = CloneUtil.clone(collection);
        clone.columns = CloneUtil.cloneCollectionMembers(columns);
        clone.display = CloneUtil.clone(display);
        clone.distinct = distinct;
        clone.disableSorting = disableSorting;
        clone.disableCounting = disableCounting;
        clone.searchBoxConfiguration = CloneUtil.clone(searchBoxConfiguration);
        clone.filter = CloneUtil.clone(filter);
        clone.domainFilter = CloneUtil.clone(domainFilter);
        clone.displayOrder = displayOrder;
        clone.refreshInterval = refreshInterval;
        clone.options = CloneUtil.cloneCollectionMembers(options);
        clone.domainOptions = CloneUtil.cloneCollectionMembers(domainOptions);
        clone.paging = CloneUtil.clone(paging);
        clone.name = CloneUtil.clone(name);
        clone.visibility = visibility;
        clone.applicableForOperation = applicableForOperation;
        clone.includeDefaultColumns = includeDefaultColumns;
        clone.objectCollectionDescription = objectCollectionDescription;
        clone.defaultView = defaultView;
        return clone;
    }
}

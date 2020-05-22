/*
 * Copyright (c) 2018-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api.authentication;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.annotation.Experimental;

/**
 * @author semancik
 *
 */
@Experimental
public class CompiledObjectCollectionView implements DebugDumpable, Serializable {
    private static final long serialVersionUID = 1L;

    private QName objectType;
    private String viewIdentifier;

    private List<GuiActionType> actions = new ArrayList<>();
    private CollectionRefSpecificationType collection;
    private List<GuiObjectColumnType> columns = new ArrayList<>();
    private DisplayType display;
    private GuiObjectListViewAdditionalPanelsType additionalPanels;
    private DistinctSearchOptionType distinct;
    private Boolean disableSorting;
    private Boolean disableCounting;
    private SearchBoxConfigurationType searchBoxConfiguration;
    private ObjectFilter filter;
    private ObjectFilter domainFilter;
    private Integer displayOrder;
    private Integer refreshInterval;

    // Only used to construct "default" view definition. May be not needed later on.
    public CompiledObjectCollectionView() {
        super();
        objectType = null;
        viewIdentifier = null;
    }

    public CompiledObjectCollectionView(QName objectType, String viewIdentifier) {
        super();
        this.objectType = objectType;
        this.viewIdentifier = viewIdentifier;
    }

    public QName getObjectType() {
        return objectType;
    }

    public void setObjectType(QName objectType) {
        this.objectType = objectType;
    }

    public <O extends ObjectType> Class<O> getTargetClass() {
        if (objectType == null) {
            return null;
        }
        return ObjectTypes.getObjectTypeClass(objectType);
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

    public GuiObjectListViewAdditionalPanelsType getAdditionalPanels() {
        return additionalPanels;
    }

    public void setAdditionalPanels(GuiObjectListViewAdditionalPanelsType additionalPanels) {
        this.additionalPanels = additionalPanels;
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
        if (!QNameUtil.match(objectType, expectedObjectType)) {
            return false;
        }
        if (expectedViewIdentifier == null) {
            return isAllObjectsView();
        }
        return expectedViewIdentifier.equals(viewIdentifier);
    }

    public boolean match(QName expectedObjectType) {
        return QNameUtil.match(objectType, expectedObjectType);
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

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(CompiledObjectCollectionView.class, indent);
        DebugUtil.debugDumpWithLabelLn(sb, "objectType", objectType, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "viewIdentifier", viewIdentifier, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "actions", actions, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "columns", columns, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "display", display, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "additionalPanels", additionalPanels, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "distinct", distinct, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "disableSorting", disableSorting, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "disableCounting", disableCounting, indent + 1);
        DebugUtil.debugDumpWithLabelToStringLn(sb, "searchBoxConfiguration", searchBoxConfiguration, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "filter", filter, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "domainFilter", domainFilter, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "displayOrder", displayOrder, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "refreshInterval", refreshInterval, indent + 1);
        return sb.toString();
    }

    public GuiObjectListViewType toGuiObjectListViewType() {
        GuiObjectListViewType viewType = new GuiObjectListViewType();
        viewType.setIdentifier(getViewIdentifier());
        viewType.setType(getObjectType());
        viewType.setAdditionalPanels(getAdditionalPanels());
        viewType.setDisplay(getDisplay());
        for (GuiObjectColumnType column : getColumns()) {
            viewType.column(column.clone());
        }
        for (GuiActionType action : getActions()) {
            viewType.action(action.clone());
        }
        viewType.setDistinct(getDistinct());
        viewType.setDisableSorting(isDisableSorting());
        viewType.setDisableCounting(isDisableCounting());
        viewType.setSearchBoxConfiguration(getSearchBoxConfiguration());
        viewType.setDisplayOrder(getDisplayOrder());
        viewType.setRefreshInterval(getRefreshInterval());
        return viewType;
    }

}

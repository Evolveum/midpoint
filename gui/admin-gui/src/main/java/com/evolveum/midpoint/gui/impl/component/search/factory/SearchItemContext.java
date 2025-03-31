/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.search.factory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.impl.component.search.panel.NamedIntervalPreset;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.*;

import com.evolveum.midpoint.prism.path.PathKeyedMap;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.GuiChannel;
import com.evolveum.midpoint.gui.impl.component.search.SearchContext;
import com.evolveum.midpoint.gui.impl.component.search.SearchValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

public class SearchItemContext implements Serializable {

    private SearchItemType item;

    @Nullable private ItemDefinition<?> itemDef;
    private List<DisplayableValue<?>> availableValues;
    private QName valueTypeName;
    private String lookupTableOid;
    @Nullable private ItemPath path;
    private ModelServiceLocator modelServiceLocator;
    private CompiledObjectCollectionView collectionView;

    private Class<?> containerType;

    private List<DisplayableValue<String>> availableEventMarks;
    private String selectedEventMark;

    private boolean isReportCollectionSearch = false;

    private List<NamedIntervalPreset> intervalPresets;

    private NamedIntervalPreset selectedIntervalPreset;

    public SearchItemContext(
            Class<?> containerType,
            PathKeyedMap<ItemDefinition<?>> availableSearchItems,
            SearchItemType searchItem,
            SearchContext additionalSearchContext,
            CompiledObjectCollectionView collectionView,
            ModelServiceLocator modelServiceLocator) {

        this.item = searchItem;
        this.containerType = containerType;
        if (item.getPath() != null) {
            this.path = item.getPath().getItemPath();
        }
        if (path != null) {
            this.itemDef = availableSearchItems.get(path);
        }
        this.valueTypeName = getSearchItemValueTypeName(item, itemDef);
        if (valueTypeName != null && itemDef == null) {
            this.itemDef = PrismContext.get().getSchemaRegistry().findItemDefinitionByType(valueTypeName);
        }
        this.availableValues = getSearchItemAvailableValues(item, itemDef, modelServiceLocator);
        LookupTableType lookupTable = getSearchItemLookupTable(itemDef, modelServiceLocator);
        this.lookupTableOid = lookupTable == null ? null : lookupTable.getOid();
        this.collectionView = collectionView;
        this.modelServiceLocator = modelServiceLocator;

        if (additionalSearchContext != null) {
            availableEventMarks = additionalSearchContext.getAvailableEventMarks();
            selectedEventMark = additionalSearchContext.getSelectedEventMark();
            isReportCollectionSearch = additionalSearchContext.isReportCollectionSearch();

            intervalPresets = additionalSearchContext.getIntervalPresets(path);
            selectedIntervalPreset = additionalSearchContext.getSelectedIntervalPresets(path);
        }
    }

    private List<DisplayableValue<?>> getSearchItemAvailableValues(SearchItemType searchItem, ItemDefinition<?> def,
            ModelServiceLocator modelServiceLocator) {
        if (def instanceof PrismPropertyDefinition<?>) {
            return CollectionUtils.isNotEmpty(((PrismPropertyDefinition<?>)def).getAllowedValues()) ?
                    (List<DisplayableValue<?>>) ((PrismPropertyDefinition<?>)def).getAllowedValues()
                    : getAllowedValues(ItemPath.create(def.getItemName()));
        }
        if (hasParameter()) {
            SearchFilterParameterType parameter = searchItem.getParameter();
            return WebComponentUtil.getAllowedValues(parameter.getAllowedValuesExpression(), modelServiceLocator);
        }
        return new ArrayList<>();
    }

    private LookupTableType getSearchItemLookupTable(ItemDefinition<?> def,
            ModelServiceLocator modelServiceLocator) {
        if (def != null) {
            PrismObject<LookupTableType> lookupTable = WebComponentUtil.findLookupTable(def, (PageBase) modelServiceLocator);
            return lookupTable != null ? lookupTable.asObjectable() : null;
        }
        if (hasParameter() && hasLookupTableDefined(item)) {
            PrismObject<LookupTableType> lookupTable = WebComponentUtil.findLookupTable(
                    item.getParameter().getAllowedValuesLookupTable().asReferenceValue(), (PageBase) modelServiceLocator);
            return lookupTable != null ? lookupTable.asObjectable() : null;
        }
        return null;
    }

    private boolean hasLookupTableDefined(SearchItemType item) {
        ObjectReferenceType lookupTableRef = item.getParameter().getAllowedValuesLookupTable();
        return  lookupTableRef != null && lookupTableRef.getOid() != null;
    }

    private QName getSearchItemValueTypeName(SearchItemType searchItem, ItemDefinition<?> def) {
        if (def != null) {
            return def.getTypeName();
        }
        if (hasParameter()) {
            return searchItem.getParameter().getType();
        }
        return null;
    }

    private static List<DisplayableValue<?>> getAllowedValues(ItemPath path) {
        if (AuditEventRecordType.F_CHANNEL.equivalent(path)) {
            List<DisplayableValue<?>> list = new ArrayList<>();
            for (GuiChannel channel : GuiChannel.values()) {
                list.add(new SearchValue<>(channel.getUri(), channel.getLocalizationKey()));
            }
            return list;
        }
        return null;
    }

    public boolean hasParameter() {
        return item != null && item.getParameter() != null;
    }

    public List<DisplayableValue<?>> getAvailableValues() {
        return availableValues;
    }

    public String getLookupTableOid() {
        return lookupTableOid;
    }

    @Nullable public ItemDefinition<?> getItemDef() {
        return itemDef;
    }

    @Nullable public ItemPath getPath() {
        return path;
    }

    public QName getValueTypeName() {
        return valueTypeName;
    }

    public PrismReferenceValue getValueEnumerationRef() {
        return itemDef != null ? itemDef.getValueEnumerationRef() : null;
    }

    public boolean isVisible() {
        if (item.isVisibleByDefault() != null) {
            return item.isVisibleByDefault();
        }
        if (hasParameter()) {
            return true;
        }
        if (item.getFilter() != null) {
            return true;
        }
        return false;
    }

    public IModel<String> getDisplayName() {
        return new LoadableDetachableModel<>() {
            @Override
            protected String load() {
                String name = WebComponentUtil.getTranslatedPolyString(GuiDisplayTypeUtil.getLabel(item.getDisplay()));
                if (StringUtils.isNotEmpty(name)) {
                    return name;
                }
                name = WebComponentUtil.getItemDefinitionDisplayNameOrName(itemDef);
                if (StringUtils.isNotEmpty(name)) {
                    return name;
                }
                return hasParameter() ? item.getParameter().getName() : "";
            }
        };
    }

    public IModel<String> getHelp() {
        return new LoadableDetachableModel<>() {
            @Override
            protected String load() {
                String help = GuiDisplayTypeUtil.getHelp(item.getDisplay());
                if (StringUtils.isNotEmpty(help)) {
                    return help;
                }
                if (itemDef != null) {
                    help = WebPrismUtil.getHelpText(itemDef, containerType);
                    if (StringUtils.isNotBlank(help)) {
                        Pattern pattern = Pattern.compile("<.+?>");
                        Matcher m = pattern.matcher(help);
                        help = m.replaceAll("");
                    }
                    if (StringUtils.isNotEmpty(help)) {
                        return help;
                    }
                }
                return hasParameter() ? GuiDisplayTypeUtil.getHelp(item.getParameter().getDisplay()) : "";
            }
        };
    }

    public String getParameterName() {
        return item.getParameter().getName();
    }

    public QName getParameterType() {
        return item.getParameter().getType();
    }

    public boolean hasPredefinedFilter() {
        return getPredefinedFilter() != null;
    }

    public SearchFilterType getPredefinedFilter() {
        return item.getFilter();
    }

    public ExpressionType getFilterExpression() {
        return item.getFilterExpression();
    }

    public Class<?> getContainerClassType() {
        return containerType;
    }

    public QName getParameterTargetType() {
        if (hasParameter()) {
            return item.getParameter().getTargetType();
        }
        return null;
    }

    public ModelServiceLocator getModelServiceLocator() {
        return modelServiceLocator;
    }

    public CompiledObjectCollectionView getCollectionView() {
        return collectionView;
    }

    public List<DisplayableValue<String>> getAvailableEventMarks() {
        if (availableEventMarks == null) {
            availableEventMarks = new ArrayList<>();
        }
        return availableEventMarks;
    }

    public String getSelectedEventMark() {
        return selectedEventMark;
    }

    public boolean isReportCollectionSearch() {
        return isReportCollectionSearch;
    }

    @Nullable
    public List<NamedIntervalPreset> getIntervalPresets() {
        return intervalPresets;
    }

    @Nullable
    public NamedIntervalPreset getSelectedIntervalPreset() {
        return selectedIntervalPreset;
    }
}

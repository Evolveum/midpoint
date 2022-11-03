package com.evolveum.midpoint.gui.impl.component.search;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.GuiChannel;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.*;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.ResourceShadowCoordinates;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.component.search.SearchValue;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchConfigurationWrapperFactory {

    public static  <C extends Containerable> PropertySearchItemWrapper createPropertySearchItemWrapper(Class<C> type,
            SearchItemType item, ResourceShadowCoordinates coordinates, ModelServiceLocator modelServiceLocator) {
//        ItemPath itemPath = null;
        ItemDefinition<?> itemDef = null;
        if (item.getPath() != null) {
            PrismContainerDefinition<C> def;
            if (ObjectType.class.isAssignableFrom(type) && modelServiceLocator != null) {
                def = PredefinedSearchableItems.findObjectDefinition((Class<? extends ObjectType>) type, coordinates, modelServiceLocator);
            } else {
                def = PrismContext.get().getSchemaRegistry().findContainerDefinitionByCompileTimeClass(type);
            }
            itemDef = def.findItemDefinition(item.getPath().getItemPath());
        }

        if (itemDef == null && !hasParameter(item) && item.getFilter() == null) {
            return null;
        }

        PropertySearchItemWrapper<?> searchItemWrapper =
                createPropertySearchItemWrapper(type, item, itemDef, modelServiceLocator); //
        //type, itemDef, itemPath, valueTypeName, availableValues, lookupTable, item.getFilter());

        searchItemWrapper.setVisible(BooleanUtils.isTrue(item.isVisibleByDefault()) || hasParameter(item));
        searchItemWrapper.setValueTypeName(getSearchItemValueTypeName(item, itemDef));

        searchItemWrapper.setName(getSearchItemName(item, itemDef));
        searchItemWrapper.setHelp(getSearchItemHelp(item, itemDef));

        if (hasParameter(item)) {
            searchItemWrapper.setParameterName(item.getParameter().getName());
            if (item.getParameter().getType() != null) {
                searchItemWrapper.setParameterValueType(PrismContext.get().getSchemaRegistry().determineClassForType(item.getParameter().getType()));
            }
            if (searchItemWrapper instanceof DateSearchItemWrapper) {
                ((DateSearchItemWrapper) searchItemWrapper).setInterval(false);
            }
        }

        if (item.isVisibleByDefault() != null) {
            searchItemWrapper.setVisible(item.isVisibleByDefault());
        }
        if (item.getFilter() != null) {
            searchItemWrapper.setPredefinedFilter(item.getFilter());
            searchItemWrapper.setVisible(true);
            searchItemWrapper.setApplyFilter(true);
            searchItemWrapper.setFilterExpression(item.getFilterExpression());
        }
        return searchItemWrapper;
    }

    private static String getSearchItemName(SearchItemType searchItem, ItemDefinition<?> itemDef) {
        String name = null;
        if (searchItem.getDisplayName() != null) {
            name = WebComponentUtil.getTranslatedPolyString(searchItem.getDisplayName());
        }
        if (StringUtils.isNotEmpty(name)) {
            return name;
        }
        name = WebComponentUtil.getTranslatedPolyString(GuiDisplayTypeUtil.getLabel(searchItem.getDisplay()));
        if (StringUtils.isNotEmpty(name)) {
            return name;
        }
        name = WebComponentUtil.getItemDefinitionDisplayNameOrName(itemDef, null);
        if (StringUtils.isNotEmpty(name)) {
            return name;
        }
        return hasParameter(searchItem) ? searchItem.getParameter().getName() : "";
    }

    private static String getSearchItemHelp(SearchItemType searchItem, ItemDefinition<?> itemDef) {
        String help = GuiDisplayTypeUtil.getHelp(searchItem.getDisplay());
        if (StringUtils.isNotEmpty(help)) {
            return help;
        }
        if (itemDef !=null) {
            help = WebPrismUtil.getHelpText(itemDef);
            if (StringUtils.isNotBlank(help)) {
                Pattern pattern = Pattern.compile("<.+?>");
                Matcher m = pattern.matcher(help);
                help = m.replaceAll("");
            }
            if (StringUtils.isNotEmpty(help)) {
                return help;
            }
        }
        return hasParameter(searchItem) ? GuiDisplayTypeUtil.getHelp(searchItem.getParameter().getDisplay()) : "";
    }

    private static boolean hasParameter(SearchItemType searchItem) {
        return searchItem != null && searchItem.getParameter() != null;
    }

    private static List<DisplayableValue<?>> getSearchItemAvailableValues(SearchItemType searchItem, ItemDefinition<?> def,
            ModelServiceLocator modelServiceLocator) {
        if (def instanceof PrismPropertyDefinition<?>) {
            return CollectionUtils.isNotEmpty(((PrismPropertyDefinition<?>)def).getAllowedValues()) ?
                    (List<DisplayableValue<?>>) ((PrismPropertyDefinition<?>)def).getAllowedValues()
                    : getAllowedValues(ItemPath.create(def.getItemName()));
        }
        if (hasParameter(searchItem)) {
            SearchFilterParameterType parameter = searchItem.getParameter();
            return WebComponentUtil.getAllowedValues(parameter.getAllowedValuesExpression(), modelServiceLocator);
        }
        return new ArrayList<>();
    }

    private static LookupTableType getSearchItemLookupTable(SearchItemType searchItem, ItemDefinition<?> def,
            ModelServiceLocator modelServiceLocator) {
        if (def != null) {
            PrismObject<LookupTableType> lookupTable = WebComponentUtil.findLookupTable(def, (PageBase) modelServiceLocator);
            return lookupTable != null ? lookupTable.asObjectable() : null;
        }
        if (hasParameter(searchItem) && searchItem.getParameter().getAllowedValuesLookupTable() != null) {
            PrismObject<LookupTableType> lookupTable = WebComponentUtil.findLookupTable(
                    searchItem.getParameter().getAllowedValuesLookupTable().asReferenceValue(), (PageBase) modelServiceLocator);
            return lookupTable != null ? lookupTable.asObjectable() : null;
        }
        return null;
    }

    private static QName getSearchItemValueTypeName(SearchItemType searchItem, ItemDefinition<?> def) {
        if (def != null) {
            return def.getTypeName();
        }
        if (hasParameter(searchItem)) {
            return searchItem.getParameter().getType();
        }
        return null;
    }

    private static <C extends Containerable> PropertySearchItemWrapper createPropertySearchItemWrapper(
            Class<C> type, SearchItemType item, ItemDefinition<?> itemDef, ModelServiceLocator modelServiceLocator) {
//            Class<C> type,
//            ItemDefinition<?> itemDef, ItemPath path, QName valueTypeName, List<DisplayableValue<?>> availableValues,
//            LookupTableType lookupTable, SearchFilterType predefinedFilter) {

        List<DisplayableValue<?>> availableValues = getSearchItemAvailableValues(item, itemDef, modelServiceLocator);
        QName valueTypeName = getSearchItemValueTypeName(item, itemDef);
        LookupTableType lookupTable = getSearchItemLookupTable(item, itemDef, modelServiceLocator);

        ItemPath path = null;
        if (item.getPath() != null) {
            path = item.getPath().getItemPath();
        }

        if (CollectionUtils.isNotEmpty(availableValues)) {
            return new ChoicesSearchItemWrapper(path, availableValues);
        }
        if (lookupTable != null) {
            return new AutoCompleteSearchItemWrapper(path, lookupTable);
        }
        if (itemDef instanceof PrismReferenceDefinition) {
            ReferenceSearchItemWrapper itemWrapper = new ReferenceSearchItemWrapper((PrismReferenceDefinition)itemDef, type);
            itemWrapper.setName(WebComponentUtil.getItemDefinitionDisplayNameOrName(itemDef, null));
            return itemWrapper;
        }
        if (path != null) {
            if (ShadowType.F_OBJECT_CLASS.equivalent(path)) {
                return new ObjectClassSearchItemWrapper();
            } else if (ShadowType.F_DEAD.equivalent(path)) {
                DeadShadowSearchItemWrapper deadWrapper = new DeadShadowSearchItemWrapper(Arrays.asList(new SearchValue<>(true), new SearchValue<>(false)));
                deadWrapper.setValue(new SearchValue(false));
                return deadWrapper;
            }
        }
        if (valueTypeName != null) {
            if (DOMUtil.XSD_BOOLEAN.equals(valueTypeName)) {
                List<DisplayableValue<Boolean>> list = new ArrayList<>();
                list.add(new SearchValue<>(Boolean.TRUE, "Boolean.TRUE"));
                list.add(new SearchValue<>(Boolean.FALSE, "Boolean.FALSE"));
                return new ChoicesSearchItemWrapper(path, list);
            } else if (QNameUtil.match(ItemPathType.COMPLEX_TYPE, valueTypeName)) {
                return new ItemPathSearchItemWrapper(path);
            } else if (QNameUtil.match(valueTypeName, DOMUtil.XSD_DATETIME)) {
                return new DateSearchItemWrapper(path);
            }
        }
        if (itemDef != null && itemDef.getValueEnumerationRef() != null) {
            return new TextSearchItemWrapper(
                    path,
                    itemDef,
                    itemDef.getValueEnumerationRef().getOid(),
                    itemDef.getValueEnumerationRef().getTargetType());
        }

        if (path != null) {
            return new TextSearchItemWrapper(path, itemDef);
        }
        return new TextSearchItemWrapper();
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

}

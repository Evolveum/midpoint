/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.search.wrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.commons.lang3.StringUtils;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.search.SearchValue;
import com.evolveum.midpoint.gui.impl.component.search.panel.ObjectTypeSearchItemPanel;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTypeSearchItemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchBoxModeType;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;

public class ObjectTypeSearchItemWrapper extends FilterableSearchItemWrapper<QName> {

    private boolean typeChanged;
    private boolean allowAllTypesSearch;

    private List<Class<? extends ObjectType>> supportedTypeList = new ArrayList<>();
    private IModel<String> name = Model.of();
    private IModel<String> help = Model.of();
    private boolean visible = true;

    private QName defaultObjectType;
    private QName valueForNull;

    public ObjectTypeSearchItemWrapper(ObjectTypeSearchItemConfigurationType config) {
        convertSupportedTypeList(config.getSupportedTypes());
        this.defaultObjectType = config.getDefaultValue();
        name = resolveName(config);
        help = resolveHelp(config);
    }

    public ObjectTypeSearchItemWrapper(List<Class<? extends ObjectType>> supportedTypeList, QName defaultObjectType) {
        this.supportedTypeList = supportedTypeList;
        this.defaultObjectType = defaultObjectType;
    }

    private void convertSupportedTypeList(List<QName> supportedTypeList) {
        if (supportedTypeList == null) {
            return;
        }
        this.supportedTypeList = supportedTypeList.stream()
                .map(qName -> (Class<? extends ObjectType>) WebComponentUtil.qnameToClass(qName))
                .collect(Collectors.toList());
    }

    public Class<ObjectTypeSearchItemPanel> getSearchItemPanelClass() {
        return ObjectTypeSearchItemPanel.class;
    }

    public List<QName> getAvailableValues() {
        return supportedTypeList.stream()
                .map(type -> WebComponentUtil.anyClassToQName(PrismContext.get(), type))
                .collect(Collectors.toList());
    }

    public boolean isTypeChanged() {
        return typeChanged;
    }

    public void setTypeChanged(boolean typeChanged) {
        this.typeChanged = typeChanged;
    }

    @Override
    public DisplayableValue<QName> getDefaultValue() {
        return new SearchValue(getDefaultObjectType());
    }

    public List<Class<? extends ObjectType>> getSupportedTypeList() {
        return supportedTypeList;
    }

    public QName getDefaultObjectType() {
        return defaultObjectType;
    }

    public void setDefaultObjectType(QName defaultObjectType) {
        this.defaultObjectType = defaultObjectType;
    }

    @Override
    public IModel<String> getName() {
        return StringUtils.isNotEmpty(name.getObject()) ? name : PageBase.createStringResourceStatic("ContainerTypeSearchItem.name");
    }

    public void setName(IModel<String> name) {
        this.name = name;
    }

    @Override
    public IModel<String> getHelp() {
        return StringUtils.isNotEmpty(help.getObject()) ? help : Model.of("");
    }

    public void setHelp(IModel<String> help) {
        this.help = help;
    }

    @Override
    public boolean canRemoveSearchItem() {
        return false;
    }

    @Override
    public IModel<String> getTitle() {
        return Model.of(""); //todo
    }

    @Override
    public boolean isApplyFilter(SearchBoxModeType searchBoxMode) {
        return !SearchBoxModeType.OID.equals(searchBoxMode);
    }

    @Override
    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public ObjectFilter createFilter(Class type, PageBase pageBase, VariablesMap variables) {
        return PrismContext.get().queryFor(type)
                .buildFilter();
    }

    public QName getValueForNull() {
        return valueForNull;
    }

    public void setValueForNull(QName valueForNull) {
        this.valueForNull = valueForNull;
    }

    public boolean isAllowAllTypesSearch() {
        return allowAllTypesSearch;
    }

    public void setAllowAllTypesSearch(boolean allowAllTypesSearch) {
        this.allowAllTypesSearch = allowAllTypesSearch;
    }

    private IModel<String> resolveName(ObjectTypeSearchItemConfigurationType config) {
        return new LoadableDetachableModel<>() {
            @Override
            protected String load() {
                if (config == null || config.getDisplay() == null) {
                    return null;
                }
                return GuiDisplayTypeUtil.getTranslatedLabel(config.getDisplay());
            }
        };
    }

    private IModel<String> resolveHelp(ObjectTypeSearchItemConfigurationType config) {
        return new LoadableDetachableModel<>() {
            @Override
            protected String load() {
                if (config == null || config.getDisplay() == null) {
                    return null;
                }
                return GuiDisplayTypeUtil.getHelp(config.getDisplay());
            }
        };
    }

    public void resetDefaultTypeIfNull(Class resetToValue) {
        if (defaultObjectType != null) {
            return;
        }
        if (resetToValue != null && supportedTypeList.contains(resetToValue)) {
            QName resetToQname = WebComponentUtil.classToQName(resetToValue);
            defaultObjectType = resetToQname;
            return;
        }
        if (isAllowAllTypesSearch()) {
            return;
        }
        if (supportedTypeList.isEmpty()) {
            return;
        }
        defaultObjectType = WebComponentUtil.classToQName(supportedTypeList.get(0));
    }
}

/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTypeSearchItemConfigurationType;

import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author skublik
 */
public class ContainerTypeSearchItem<C extends Containerable> extends SearchItem {

    private static final long serialVersionUID = 1L;
    private static final Trace LOGGER = TraceManager.getTrace(ContainerTypeSearchItem.class);

    public static final String F_TYPE_VALUE = "type.value";
    public static final String F_TYPE = "type";

    private List<DisplayableValue<Class<C>>> allowedValues = new ArrayList<>();
    private DisplayableValue<Class<C>> type;
    private Class<C> oldType;
    private boolean visible = false;
    private ObjectTypeSearchItemConfigurationType configuration = null;
    private PrismContainerDefinition<C> containerDef;

    public ContainerTypeSearchItem(Class<C> typeClass) {
        this(new SearchValue<>(typeClass, ""), null);
    }

    public ContainerTypeSearchItem(@NotNull DisplayableValue<Class<C>> type) {
        this(type, null);
    }

    public ContainerTypeSearchItem(@NotNull DisplayableValue<Class<C>> type, List<DisplayableValue<Class<C>>> allowedValues) {
        super(null);
        Validate.notNull(type, "Type must not be null.");
        Validate.notNull(type.getValue(), "Type must not be null.");
        this.type = type;
        this.oldType = type.getValue();
        this.allowedValues = allowedValues;
    }

    @Override
    public String getName() {
        if (configuration != null && configuration.getDisplay() != null && configuration.getDisplay().getLabel() != null){
            return WebComponentUtil.getTranslatedPolyString(configuration.getDisplay().getLabel());
        }
        return PageBase.createStringResourceStatic("ContainerTypeSearchItem.name").getString();
    }

    @Override
    public String getHelp(PageBase pageBase) {
        if (configuration != null && configuration.getDisplay() != null && configuration.getDisplay().getHelp() != null){
            return WebComponentUtil.getTranslatedPolyString(configuration.getDisplay().getHelp());
        }
        return "";
    }

    @Override
    public Type getSearchItemType() {
        return Type.TEXT;
    }

    public Class<C> getTypeClass() {
        if (type != null) {
            return type.getValue();
        }
        return null;
    }

    public DisplayableValue<Class<C>> getType() {
        return type;
    }

    public void setType(@NotNull DisplayableValue<Class<C>> type) {
        oldType = this.getTypeClass();
        this.type = type;
    }

    @Override
    public String toString() {
        return "ContainerTypeSearchItem{" +
                "type=" + type +
                "allowedValues=" + allowedValues +
                "visible=" + visible +
                '}';
    }

    public List<DisplayableValue<Class<C>>> getAllowedValues() {
        return allowedValues;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isVisible(){
        return allowedValues != null && !allowedValues.isEmpty() && visible
                && (configuration == null || CompiledGuiProfile.isVisible(configuration.getVisibility(), null));
    }

    public void setTypeClass(Class<C> type) {
        oldType = this.getTypeClass();
        this.type = new SearchValue<Class<C>>(type,"");
    }

    public boolean isTypeChanged(){
        if (oldType == null) {
            if(getTypeClass() == null) {
                return false;
            }
            return true;
        }
        return !oldType.equals(getTypeClass());
    }

    public void setConfiguration(ObjectTypeSearchItemConfigurationType configuration) {
        this.configuration = configuration;
    }

    public PrismContainerDefinition<C> getContainerDefinition() {
        return containerDef;
    }

    public void setContainerDefinition(PrismContainerDefinition<C> containerDef) {
        this.containerDef = containerDef;
    }
}

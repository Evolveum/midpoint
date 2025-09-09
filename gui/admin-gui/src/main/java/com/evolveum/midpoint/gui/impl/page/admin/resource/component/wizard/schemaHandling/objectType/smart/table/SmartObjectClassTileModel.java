/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.table;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectClassSizeEstimationType;
import com.evolveum.midpoint.xml.ns._public.prism_schema_3.ComplexTypeDefinitionType;

import org.apache.cxf.common.util.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

public class SmartObjectClassTileModel<T extends PrismContainerValueWrapper<ComplexTypeDefinitionType>> extends TemplateTile<T> {

    String icon;
    String name;
    String description;
    String count;

    public SmartObjectClassTileModel(T valueWrapper, ObjectClassSizeEstimationType sizeEstimation) {
        super(valueWrapper);

        setValue(valueWrapper);
        this.icon = GuiStyleConstants.CLASS_ICON_OUTLIER;
        this.name = extractName(valueWrapper.getRealValue());
        this.description = "Description for this object class is not ready yet, but it will be available soon."; // TODO

        if (sizeEstimation == null || sizeEstimation.getValue() == null) {
            this.count = "Unknown";
        } else {
            this.count = sizeEstimation.getValue().toString();
        }
    }

    private String extractName(@NotNull ComplexTypeDefinitionType definition) {
        QName rawName = definition.getName();
        return StringUtils.capitalize(rawName.getLocalPart());
    }

    @Override
    public String getIcon() {
        return icon;
    }

    @Override
    public void setIcon(String icon) {
        this.icon = icon;
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

    public String getCount() {
        return count;
    }

    public void setCount(String count) {
        this.count = count;
    }
}

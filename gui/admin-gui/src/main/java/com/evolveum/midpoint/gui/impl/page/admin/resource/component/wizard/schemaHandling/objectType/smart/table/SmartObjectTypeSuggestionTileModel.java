/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.table;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.basic.ObjectClassWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectFocusSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDelineationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

import org.apache.cxf.common.util.StringUtils;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SmartObjectTypeSuggestionTileModel<T extends PrismContainerValueWrapper<ResourceObjectTypeDefinitionType>> extends TemplateTile<T> {

    private String icon;
    private String name;
    private String description;
    private String kind;
    private String intent;
    private final String resourceOid;
    QName focusType;

    public SmartObjectTypeSuggestionTileModel(T valueWrapper, String resourceOid) {
        super(valueWrapper);

        setValue(valueWrapper);
        ResourceObjectTypeDefinitionType suggestion = valueWrapper.getRealValue();

        this.icon = GuiStyleConstants.CLASS_ICON_OUTLIER;
        this.description = suggestion.getDescription();
        this.kind = suggestion.getKind().value();
        this.intent = suggestion.getIntent();
        this.name = suggestion.getDisplayName();
        this.resourceOid = resourceOid;

        ResourceObjectFocusSpecificationType focus = suggestion.getFocus();
        this.focusType = focus != null && focus.getType() != null ? focus.getType() : null;
    }

    protected List<IModel<String>> buildChipsData(PageBase pageBase) {
        ResourceObjectTypeDefinitionType value = getValue().getRealValue();
        if (value == null) {
            return Collections.emptyList();
        }

        var chips = new ArrayList<IModel<String>>();

        ShadowKindType kind = value.getKind();
        if (kind != null) {
            addChip(pageBase, chips, Keys.KIND, kind.value());
        }
        addChip(pageBase, chips, Keys.INTENT, value.getIntent());

        ResourceObjectTypeDelineationType del = value.getDelineation();
        if (del != null && del.getObjectClass() != null) {
            addChip(pageBase, chips, SmartObjectTypeSuggestionTileModel.Keys.OBJECT_CLASS, del.getObjectClass().getLocalPart());
        }

        addChip(pageBase, chips, SmartObjectTypeSuggestionTileModel.Keys.FOCUS_TYPE,
                focusType.getLocalPart() != null
                        ? focusType.getLocalPart()
                        : "-");

        return Collections.unmodifiableList(chips);
    }

    private static void addChip(
            PageBase pageBase,
            List<IModel<String>> chips,
            String key,
            String value) {
        if (value != null && !value.isBlank()) {
            chips.add(pageBase.createStringResource(key, value));
        }
    }

    private static final class Keys {
        static final String KIND = "SmartSuggestObjectTypeTileModel.kind";
        static final String INTENT = "SmartSuggestObjectTypeTileModel.intent";
        static final String OBJECT_CLASS = "SmartSuggestObjectTypeTileModel.delineationClass";
        static final String FOCUS_TYPE = "SmartSuggestObjectTypeTileModel.focusType";

        private Keys() {
        }
    }

    private String extractName(@NotNull SelectableBean<ObjectClassWrapper> wrapper) {
        String rawName = wrapper.getValue().getObjectClassNameAsString();
        return StringUtils.capitalize(rawName);
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

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public ResourceObjectTypeDefinitionType getObjectTypeSuggestion() {
        return getValue().getRealValue();
    }

    public PrismPropertyValueWrapper<Object> getFilterPropertyValueWrapper() {
        try {
            PrismContainerValueWrapper<Containerable> containerValue =
                    getValue().findContainerValue(ResourceObjectTypeDefinitionType.F_DELINEATION);
            if (containerValue == null) {
                return null;
            }

            PrismPropertyWrapper<Object> property =
                    containerValue.findItem(ResourceObjectTypeDelineationType.F_FILTER);
            if (property == null) {
                return null;
            }

            List<PrismPropertyValueWrapper<Object>> values = property.getValues();
            if (values == null || values.isEmpty()) {
                return null;
            }

            return values.get(0);
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
    }

    protected String getResourceOid() {
        return resourceOid;
    }

    protected QName getObjectClass() {
        ResourceObjectTypeDefinitionType suggestion = getValue().getRealValue();
        if (suggestion == null || suggestion.getDelineation() == null) {
            return null;
        }
        return suggestion.getDelineation().getObjectClass();
    }
}

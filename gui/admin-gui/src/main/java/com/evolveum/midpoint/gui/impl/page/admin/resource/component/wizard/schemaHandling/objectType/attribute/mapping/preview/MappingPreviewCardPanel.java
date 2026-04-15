/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.mapping.preview;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * UI panel rendering a card-style preview of a mapping, showing its
 * strength, source and target values, and related icons.
 */
public class MappingPreviewCardPanel extends BasePanel<PrismContainerValueWrapper<MappingType>> {

    private static final String ID_CARD = "card";
    private static final String ID_STRENGTH_ICON = "strengthIcon";
    private static final String ID_STRENGTH_LABEL = "strengthLabel";
    private static final String ID_SOURCE_VALUE = "sourceValue";
    private static final String ID_TARGET_VALUE = "targetValue";
    private static final String ID_AI_ICON = "aiIcon";

    private final boolean isInbound;

    public MappingPreviewCardPanel(String id, IModel<PrismContainerValueWrapper<MappingType>> model, boolean isInbound) {
        super(id, model);
        this.isInbound = isInbound;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        PrismContainerValueWrapper<MappingType> mappingWrapper = getModelObject();
        MappingType mapping = mappingWrapper != null ? mappingWrapper.getRealValue() : null;

        WebMarkupContainer card = new WebMarkupContainer(ID_CARD);
        card.setOutputMarkupId(true);
        card.add(AttributeModifier.append("class", "border rounded p-3 position-relative bg-light"));
        add(card);

        Label strengthIcon = new Label(ID_STRENGTH_ICON, Model.of(""));
        strengthIcon.add(AttributeModifier.append("class", getStrengthIconCss(mapping)));
        card.add(strengthIcon);

        card.add(new Label(ID_STRENGTH_LABEL, Model.of(getStrengthLabel(mapping))));
        card.add(new Label(ID_SOURCE_VALUE, Model.of(getSourceValue(mappingWrapper))));
        card.add(new Label(ID_TARGET_VALUE, Model.of(getTargetValue(mappingWrapper))));

        Label aiIcon = new Label(ID_AI_ICON, Model.of(""));
        card.add(aiIcon);
    }

    private String getSourceValue(@Nullable PrismContainerValueWrapper<MappingType> mappingWrapper) {
        if (mappingWrapper == null) {
            return "";
        }

        if (isInbound) {
            return itemPathToString(getRefPath(mappingWrapper));
        }

        return getTargetPath(mappingWrapper);
    }

    private String getTargetValue(@Nullable PrismContainerValueWrapper<MappingType> mappingWrapper) {
        if (mappingWrapper == null) {
            return "";
        }

        if (isInbound) {
            return getTargetPath(mappingWrapper);
        }

        return itemPathToString(getRefPath(mappingWrapper));
    }

    private String getTargetPath(@NotNull PrismContainerValueWrapper<MappingType> mappingWrapper) {
        MappingType mapping = mappingWrapper.getRealValue();
        if (mapping == null || mapping.getTarget() == null || mapping.getTarget().getPath() == null) {
            return "";
        } else {
            mapping.getTarget();
        }

        return String.valueOf(mapping.getTarget().getPath().getItemPath());
    }

    private String itemPathToString(@Nullable ItemPathType itemPath) {
        if (itemPath == null) {
            return "";
        }
        return String.valueOf(itemPath.getItemPath());
    }

    private String getStrengthLabel(@Nullable MappingType mapping) {
        if (mapping == null || mapping.getStrength() == null) {
            return "Normal";
        }
        return StringUtils.capitalize(mapping.getStrength().value());
    }

    private @Nullable String getStrengthIconCss(@Nullable MappingType mapping) {
        DisplayType display = GuiDisplayTypeUtil.getDisplayTypeForStrengthOfMapping(
                "text-muted", mapping != null ? mapping.getStrength() : null);
        IconType icon = display != null ? display.getIcon() : null;
        return icon != null ? icon.getCssClass() : null;
    }

    private @Nullable ItemPathType getRefPath(@NotNull PrismContainerValueWrapper<MappingType> mappingWrapper) {
        try {
            PrismPropertyWrapper<ItemPathType> refProperty =
                    mappingWrapper.findProperty(AbstractAttributeMappingsDefinitionType.F_REF);

            return refProperty != null && refProperty.getValue() != null
                    ? refProperty.getValue().getRealValue()
                    : null;
        } catch (SchemaException e) {
            throw new RuntimeException("Error retrieving ref property from mapping", e);
        }
    }
}

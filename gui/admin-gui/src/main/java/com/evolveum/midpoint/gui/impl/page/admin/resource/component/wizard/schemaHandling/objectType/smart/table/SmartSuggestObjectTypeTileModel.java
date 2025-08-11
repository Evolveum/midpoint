/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.smart.table;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.tile.Tile;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.basic.ObjectClassWrapper;
import com.evolveum.midpoint.web.component.util.SelectableBean;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import org.apache.cxf.common.util.StringUtils;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class SmartSuggestObjectTypeTileModel<S> extends Tile<ObjectTypeSuggestionType> {

    private String icon;
    private String name;
    private String description;
    private String count;
    private String filter;
    private String kind;
    private String intent;

    public SmartSuggestObjectTypeTileModel(String icon, String title) {
        super(icon, title);
        this.icon = icon;
        this.name = title;
    }

    public SmartSuggestObjectTypeTileModel(@NotNull ObjectTypeSuggestionType suggestion) {
        setValue(suggestion);
        this.icon = GuiStyleConstants.CLASS_ICON_OUTLIER;
        this.description = "TODO: e.g. account for services and applications"; //TODO replace when you have real copy
        this.count = null; // set when available
        ResourceObjectTypeDelineationType delineation = suggestion.getDelineation();
        List<SearchFilterType> filter1 = delineation.getFilter();
        SearchFilterType searchFilterType = filter1.get(0);



//        PropertyModel<PrismPropertyValueWrapper> valueModel = new PropertyModel<>(itemWrapper, "value");

//        new PrismPropertyValuePanel<>()

        this.filter = buildFilterString(suggestion.getDelineation());
        this.kind = suggestion.getIdentification() != null ? suggestion.getIdentification().getKind().value() : null;
        this.intent = suggestion.getIdentification() != null ? suggestion.getIdentification().getIntent() : null;
        this.name = deriveTitle();

    }

    private @NotNull String deriveTitle() {
        String prefix = kind != null ? kind : "N/A";
        String suffix = intent != null ? intent : "N/A";
        return StringUtils.capitalize(prefix) + " " + StringUtils.capitalize(suffix);
    }

    private static String buildFilterString(ResourceObjectTypeDelineationType delineation) {
        if (delineation == null || delineation.getFilter() == null) {
            return "";
        }
        return delineation.getFilter().stream()
                .map(SearchFilterType::getText)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining(" and "));
    }

    protected List<IModel<String>> buildChipsData(PageBase pageBase) {
        ObjectTypeSuggestionType value = getValue();
        if (value == null) {
            return Collections.emptyList();
        }

        var chips = new ArrayList<IModel<String>>();

        ResourceObjectTypeIdentificationType id = value.getIdentification();
        if (id != null) {
            ShadowKindType kind = id.getKind();
            if (kind != null) {
                addChip(pageBase, chips, Keys.KIND, kind.value());
            }
            addChip(pageBase, chips, Keys.INTENT, id.getIntent());
        }

        ResourceObjectTypeDelineationType del = value.getDelineation();
        if (del != null && del.getObjectClass() != null) {
            addChip(pageBase, chips, Keys.OBJECT_CLASS, del.getObjectClass().getLocalPart());
        }

        //TODO until you have a real focusType value to pass.
        addChip(pageBase, chips, Keys.FOCUS_TYPE, "TODO");

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
        static final String OBJECT_CLASS = "SmartSuggestObjectTypeTileModel.focusType";
        static final String FOCUS_TYPE = "SmartSuggestObjectTypeTileModel.focusType";

        private Keys() {
        }
    }

    public String getFilter() {
        return filter;
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

    public String getCount() {
        return count;
    }

    public void setCount(String count) {
        this.count = count;
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }
}

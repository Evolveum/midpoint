/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import org.apache.wicket.model.StringResourceModel;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;

import static com.evolveum.midpoint.web.component.data.column.ColumnUtils.createStringResource;

/**
 * DTO representing a "no objects" state, including title/subtitle text for empty object type panels.
 */
public class NoValuePanelDto implements Serializable {

    private final String defaultTypeSimpleName;

    /**
     * Constructs a NoResourceObjectDto for a specific container path.
     *
     * @param defaultType the default type of the object, used for generating titles.
     */
    @Contract(pure = true)
    public <C extends Serializable> NoValuePanelDto(@NotNull Class<C> defaultType) {
        this.defaultTypeSimpleName = defaultType.getSimpleName();
    }

    /**
     * Returns the localized title message for the  title panel.
     */
    protected StringResourceModel getTitle() {
        return createStringResource("NoResourceObjectDto.noObjectToShow.title", getTypeTitle(false));
    }

    /**
     * Returns the localized subtitle message for the subtitle panel.
     */
    protected StringResourceModel getSubtitle() {
        return createStringResource("NoResourceObjectDto.noObjectToShow.subtitle", getTypeTitle(true));
    }

    /**
     * Builds a type-specific title key based on the path, optionally pluralized.
     */
    protected StringResourceModel getTypeTitle(boolean plural) {
        String suffix = plural ? "s" : "";
        return createStringResource(defaultTypeSimpleName + ".schemaHandlingObjectsPanel.title" + suffix);
    }

    public String getDefaultTypeSimpleName() {
        return defaultTypeSimpleName;
    }

}

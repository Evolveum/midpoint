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

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

import static com.evolveum.midpoint.web.component.data.column.ColumnUtils.createStringResource;

/**
 * DTO representing a "no objects" state, including title/subtitle text
 * for empty object-type panels.
 *
 * <p><strong>Important:</strong> Every identifier used with this DTO must have
 * corresponding localization keys defined in the resource bundles:
 * <ul>
 *   <li>{@code NoValuePanelDto.&lt;identifier&gt;.title}</li>
 *   <li>{@code NoValuePanelDto.&lt;identifier&gt;.title.plural}</li>
 * </ul>
 * Otherwise, the UI will not render proper labels.</p>
 */
public record NoValuePanelDto(@NotNull String identifier) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Validates the identifier.
     */
    @Contract(pure = true)
    public NoValuePanelDto {
        Objects.requireNonNull(identifier, "identifier");
    }

    /**
     * Returns the localized title message for the title panel.
     */
    public StringResourceModel getTitle() {
        return createStringResource(
                "NoValuePanelDto.noObjectToShow.title",
                buildIdentifierTitleModel(false));
    }

    /**
     * Returns the localized subtitle message for the subtitle panel.
     */
    public StringResourceModel getSubtitle() {
        return createStringResource(
                "NoValuePanelDto.noObjectToShow.subtitle",
                buildIdentifierTitleModel(true));
    }

    /**
     * Builds a localized title model for the object type.
     * Avoid naive pluralization; use dedicated resource keys.
     */
    private StringResourceModel buildIdentifierTitleModel(boolean plural) {
        String key = "NoValuePanelDto." + identifier + (plural ? ".title.plural" : ".title");
        return createStringResource(key);
    }
}

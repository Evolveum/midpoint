/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard;

import com.evolveum.midpoint.gui.api.component.Badge;
import com.evolveum.midpoint.web.session.ResourceWizardStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents UI state of tiles in the Schema Handling wizard.
 * Determines recommendation and lock status based on preview
 * visit state and resource configuration.
 */
public enum ResourceGuideSchemaHandlingWizardTileState {

    NORMAL(null),

    RECOMMENDED(new BadgeSpec(
            "badge bg-light text-primary border border-primary",
            "",
            "ResourceWizard.recommended")),

    LOCKED(null);

    private final @Nullable BadgeSpec badgeSpec;

    ResourceGuideSchemaHandlingWizardTileState(@Nullable BadgeSpec badgeSpec) {
        this.badgeSpec = badgeSpec;
    }

    public boolean isLocked() {
        return this == LOCKED;
    }

    public @NotNull IModel<Badge> badgeModel(@NotNull SchemaHandlingWizardChoicePanel panel) {
        if (badgeSpec == null) {
            return Model.of();
        }

        String text = panel.getPageBase()
                .createStringResource(badgeSpec.labelKey())
                .getString();

        return badgeSpec.iconCss() != null
                ? Model.of(new Badge(badgeSpec.cssClass(), badgeSpec.iconCss(), text))
                : Model.of(new Badge(badgeSpec.cssClass(), text));
    }

    public static @NotNull ResourceGuideSchemaHandlingWizardTileState computeState(
            @Nullable SchemaHandlingWizardChoicePanel.PreviewTileType tile,
            @NotNull ResourceType resource,
            @NotNull ResourceWizardStorage storage) {
        if(tile == null) {
            return NORMAL;
        }

        String oid = resource.getOid();
        boolean previewVisited = oid != null && storage.isPreviewDataClicked(oid);

        // There is many cases when association cannot be configured. (This was requested after team discussion).
        boolean hasObjectTypes =
                resource.getSchemaHandling() != null
                        && resource.getSchemaHandling().getObjectType() != null
                        && !resource.getSchemaHandling().getObjectType().isEmpty();

        return switch (tile) {

            case PREVIEW_DATA ->
                    previewVisited ? NORMAL : RECOMMENDED;

            case CONFIGURE_OBJECT_TYPES ->
                    previewVisited ? RECOMMENDED : NORMAL;

            case CONFIGURE_ASSOCIATION_TYPES ->
                    hasObjectTypes ? NORMAL : LOCKED;
        };
    }

    public record BadgeSpec(String cssClass, @Nullable String iconCss, String labelKey) {}

}

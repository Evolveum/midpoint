/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.subject.mappingContainer;

import com.evolveum.midpoint.gui.impl.component.tile.Tile;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TileChoicePopup;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Arrays;
import java.util.List;

public abstract class AssociationMappingTypeChoicePanelPopup
        extends TileChoicePopup<AssociationMappingTypeChoicePanelPopup.AssociationMappingKind> {

    public enum AssociationMappingKind {
        OBJECT_REF,
        ATTRIBUTE
    }

    public AssociationMappingTypeChoicePanelPopup(String id, Component component) {
        super(id, () -> createTiles(component), null);
    }

    public static @NotNull @Unmodifiable List<Tile<AssociationMappingKind>> createTiles(Component component) {
        return Arrays.stream(AssociationMappingKind.values())
                .map(kind -> {
                    String icon = kind == AssociationMappingKind.OBJECT_REF
                            ? "fa-solid fa-link"
                            : "fa-solid fa-tag";

                    Tile<AssociationMappingKind> tile = new Tile<>(
                            icon,
                            component.getString("AssociationMappingKind." + kind.name()));
                    tile.setValue(kind);
                    tile.setDescription(
                            component.getString("AssociationMappingKind." + kind.name() + ".description"));
                    return tile;
                })
                .toList();
    }

    @Override
    protected void performAction(AjaxRequestTarget target, AssociationMappingKind value) {
        onAssociationMappingKindChosen(target, value);
    }

    @Override
    protected IModel<String> getText() {
        return createStringResource("AssociationMappingTypeChoicePanelPopup.text");
    }

    @Override
    protected IModel<String> getSubText() {
        return createStringResource("AssociationMappingTypeChoicePanelPopup.subText");
    }

    @Override
    protected IModel<String> getAcceptButtonLabel() {
        return createStringResource("AssociationMappingTypeChoicePanelPopup.accept");
    }

    protected abstract void onAssociationMappingKindChosen(
            AjaxRequestTarget target,
            AssociationMappingKind value);
}

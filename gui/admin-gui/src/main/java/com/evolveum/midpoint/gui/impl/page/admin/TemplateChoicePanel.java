/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.impl.component.data.column.icon.CompositedIconPanel;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;
import com.evolveum.midpoint.gui.impl.component.tile.Tile;

import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;

import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class TemplateChoicePanel extends AbstractTemplateChoicePanel<CompiledObjectCollectionView> {

    public TemplateChoicePanel(String id) {
        super(id);
    }

    protected abstract Collection<CompiledObjectCollectionView> findAllApplicableArchetypeViews();

    @Override
    protected LoadableModel<List<Tile<CompiledObjectCollectionView>>> loadTilesModel() {
        return new LoadableModel<>(false) {

            @Override
            protected List<Tile<CompiledObjectCollectionView>> load() {
                List<Tile<CompiledObjectCollectionView>> tiles = new ArrayList<>();
                Collection<CompiledObjectCollectionView> compiledObjectCollectionViews = findAllApplicableArchetypeViews();

                compiledObjectCollectionViews.forEach(collection -> {
                    Tile<CompiledObjectCollectionView> tile = new Tile<>(
                            null,
                            LocalizationUtil.translatePolyString(
                                    GuiDisplayTypeUtil.getLabel(collection.getDisplay())));
                    tile.setDescription(GuiDisplayTypeUtil.getHelp(collection.getDisplay()));
                    tile.setValue(collection);
                    tiles.add(tile);
                });
                return tiles;
            }
        };
    }

    @Override
    protected WebMarkupContainer createTemplateIconPanel(IModel<Tile<CompiledObjectCollectionView>> tileModel, String idIcon) {
        IModel<CompositedIcon> iconModel = () -> {
            CompiledObjectCollectionView view = tileModel.getObject().getValue();
            return createCompositedIcon(view);
        };
        return new CompositedIconPanel(idIcon, iconModel);
    }

    private CompositedIcon createCompositedIcon(CompiledObjectCollectionView collectionView) {
        DisplayType additionalButtonDisplayType = GuiDisplayTypeUtil.getNewObjectDisplayTypeFromCollectionView(collectionView);
        CompositedIconBuilder builder = new CompositedIconBuilder();

        builder.setBasicIcon(GuiDisplayTypeUtil.getIconCssClass(additionalButtonDisplayType), IconCssStyle.IN_ROW_STYLE)
                .appendColorHtmlValue(GuiDisplayTypeUtil.getIconColor(additionalButtonDisplayType));

        return builder.build();
    }
}

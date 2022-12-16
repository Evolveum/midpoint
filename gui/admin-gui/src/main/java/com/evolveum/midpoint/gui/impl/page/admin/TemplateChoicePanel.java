/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin;

import com.evolveum.midpoint.gui.impl.component.wizard.WizardChoicePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.data.column.CompositedIconPanel;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;
import com.evolveum.midpoint.gui.impl.component.tile.Tile;
import com.evolveum.midpoint.gui.impl.component.tile.TilePanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;

import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;

import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public abstract class TemplateChoicePanel extends WizardChoicePanel<CompiledObjectCollectionView, AssignmentHolderDetailsModel> {

    public TemplateChoicePanel(String id) {
        super(id, null);
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
                    Tile tile = new Tile(
                            null,
                            WebComponentUtil.getTranslatedPolyString(
                                    GuiDisplayTypeUtil.getLabel(collection.getDisplay())));
                    tile.setValue(collection);
                    tiles.add(tile);
                });
                return tiles;
            }
        };
    }

    @Override
    protected Component createTilePanel(String id, IModel<Tile<CompiledObjectCollectionView>> tileModel) {
        return new TilePanel(id, tileModel) {

            @Override
            protected WebMarkupContainer createIconPanel(String idIcon) {
                IModel<CompositedIcon> iconModel = () -> {
                    CompiledObjectCollectionView view = tileModel.getObject().getValue();
                    return createCompositedIcon(view);
                };
                return new CompositedIconPanel(idIcon, iconModel);
            }

            @Override
            protected void onClick(AjaxRequestTarget target) {
                Tile<CompiledObjectCollectionView> tile = tileModel.getObject();
                onTemplateChosePerformed(tile.getValue(), target);
            }
        };
    }

    private CompositedIcon createCompositedIcon(CompiledObjectCollectionView collectionView) {
        DisplayType additionalButtonDisplayType = GuiDisplayTypeUtil.getNewObjectDisplayTypeFromCollectionView(collectionView, getPageBase());
        CompositedIconBuilder builder = new CompositedIconBuilder();

        builder.setBasicIcon(GuiDisplayTypeUtil.getIconCssClass(additionalButtonDisplayType), IconCssStyle.IN_ROW_STYLE)
                .appendColorHtmlValue(GuiDisplayTypeUtil.getIconColor(additionalButtonDisplayType));

        return builder.build();
    }

    protected abstract void onTemplateChosePerformed(CompiledObjectCollectionView view, AjaxRequestTarget target);

    @Override
    protected @NotNull IModel<String> getBreadcrumbLabel() {
        return () -> "";
    }

    @Override
    protected boolean isExitButtonVisible() {
        return false;
    }

    @Override
    protected boolean isSubmitButtonVisible() {
        return false;
    }

    @Override
    protected IModel<String> getTextModel() {
        return createStringResource(
                "TemplateChoicePanel.text",
                WebComponentUtil.translateMessage(
                        ObjectTypeUtil.createTypeDisplayInformation(getType(), false)));
    }

    protected abstract QName getType();

    @Override
    protected IModel<String> getSubTextModel() {
        return createStringResource(
                "TemplateChoicePanel.subText",
                WebComponentUtil.translateMessage(
                        ObjectTypeUtil.createTypeDisplayInformation(getType(), false)));
    }
}

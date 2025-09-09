/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin;

import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.impl.component.tile.Tile;
import com.evolveum.midpoint.gui.impl.component.tile.TilePanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardChoicePanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.io.Serializable;

/**
 * Abstract class for panels with tiles for choice of object template.
 */
public abstract class AbstractTemplateChoicePanel<T extends Serializable> extends WizardChoicePanel<T, AssignmentHolderDetailsModel<AssignmentHolderType>> {

    public AbstractTemplateChoicePanel(String id) {
        super(id);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        if (isSelectable()) {
            setOutputMarkupId(true);
        }
    }

    @Override
    protected Component createTilePanel(String id, IModel<Tile<T>> tileModel) {
        return new TilePanel<>(id, tileModel) {

            @Override
            protected WebMarkupContainer createIconPanel(String idIcon) {
                return AbstractTemplateChoicePanel.this.createTemplateIconPanel(tileModel, idIcon);
            }

            protected VisibleEnableBehaviour getDescriptionBehaviour() {
                return new VisibleBehaviour(() -> StringUtils.isNotEmpty(tileModel.getObject().getDescription()));
            }

            @Override
            protected void onClick(AjaxRequestTarget target) {
                onClickPerformed(target, tileModel);
            }
        };
    }

    protected void onClickPerformed(AjaxRequestTarget target, IModel<Tile<T>> tileModel) {
        Tile<T> selectedTile = tileModel.getObject();
        if (isSelectable()) {
            getTilesModel().getObject().forEach(tile -> tile.setSelected(false));
            selectedTile.setSelected(true);
            target.add(AbstractTemplateChoicePanel.this);
        }
        onTemplateChosePerformed(selectedTile.getValue(), target);
    }

    protected abstract WebMarkupContainer createTemplateIconPanel(IModel<Tile<T>> tileModel, String idIcon);

    protected boolean isSelectable() {
        return false;
    }

    protected abstract void onTemplateChosePerformed(T view, AjaxRequestTarget target);

    @Override
    protected @NotNull IModel<String> getBreadcrumbLabel() {
        return () -> "";
    }

    @Override
    protected boolean isExitButtonVisible() {
        return false;
    }

    @Override
    protected IModel<String> getTextModel() {
        return createStringResource(
                "TemplateChoicePanel.text",
                LocalizationUtil.translateMessage(
                        ObjectTypeUtil.createTypeDisplayInformation(getType(), false)));
    }

    protected abstract QName getType();

    @Override
    protected IModel<String> getSubTextModel() {
        return createStringResource(
                "TemplateChoicePanel.subText",
                LocalizationUtil.translateMessage(
                        ObjectTypeUtil.createTypeDisplayInformation(getType(), false)));
    }
}

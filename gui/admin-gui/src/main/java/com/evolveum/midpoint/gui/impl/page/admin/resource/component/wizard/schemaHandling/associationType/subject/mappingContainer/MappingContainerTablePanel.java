/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.subject.mappingContainer;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.tile.SingleSelectContainerTileTablePanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.MappingTile;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.MappingTilePanel;
import com.evolveum.midpoint.gui.impl.util.GuiDisplayNameUtil;
import com.evolveum.midpoint.gui.impl.util.IconAndStylesUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.List;

public class MappingContainerTablePanel extends SingleSelectContainerTileTablePanel<MappingType> {

    public MappingContainerTablePanel(String id, UserProfileStorage.TableId tableId, IModel<List<PrismContainerValueWrapper<MappingType>>> model) {
        super(id, tableId, model);
    }

    @Override
    protected Class<? extends Containerable> getType() {
        return MappingType.class;
    }

    @Override
    protected TemplateTile<PrismContainerValueWrapper<MappingType>> createTileObject(PrismContainerValueWrapper<MappingType> object) {
        MappingTile<PrismContainerValueWrapper<MappingType>> tile = new MappingTile<>(object);
        tile.setIcon(IconAndStylesUtil.createMappingIcon(object));
        tile.setTitle(GuiDisplayNameUtil.getDisplayName(object.getRealValue()));
        tile.setDescription(WebPrismUtil.createMappingTypeDescription(object.getRealValue(), false));
        tile.setHelp(WebPrismUtil.createMappingTypeStrengthHelp(object.getRealValue()));
        return tile;
    }

    @Override
    protected Component createTile(String id, IModel<TemplateTile<PrismContainerValueWrapper<MappingType>>> model) {
        MappingTilePanel tile = new MappingTilePanel(id, model) {
            @Override
            protected void onRemovePerformed(PrismContainerValueWrapper value, AjaxRequestTarget target) {
                super.onRemovePerformed(value, target);
                getTilesModel().detach();
                refresh(target);
            }

            @Override
            protected void onConfigureClick(AjaxRequestTarget target, MappingTile modelObject) {
                onTileClick(target, modelObject);
            }

            @Override
            protected boolean isIconVisible() {
                return false;
            }
        };
        tile.add(new VisibleBehaviour(() -> ValueStatus.DELETED != model.getObject().getValue().getStatus()));
        return tile;
    }

    protected void onTileClick(AjaxRequestTarget target, MappingTile modelObject) {
    }

    private AjaxIconButton createAddButton(String buttonId) {
        AjaxIconButton addButton = new AjaxIconButton(
                buttonId,
                Model.of("fa fa-circle-plus text-light"),
                Model.of(LocalizationUtil.translate(getAddButtonLabelKey()))) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                onClickCreateMapping(target);
            }
        };
        addButton.showTitleAsLabel(true);
        addButton.add(AttributeAppender.append("class", "btn btn-primary text-light"));
        return addButton;
    }

    protected void onClickCreateMapping(AjaxRequestTarget target) {
    }

    protected String getAddButtonLabelKey() {
        return "";
    }

    @Override
    protected WebMarkupContainer createTilesButtonToolbar(String id) {
        RepeatingView repView = new RepeatingView(id);
        repView.add(createAddButton(repView.newChildId()));
        return repView;
    }

    @Override
    protected Component createHeader(String id) {
        WebMarkupContainer header = new WebMarkupContainer(id);
        header.add(VisibleBehaviour.ALWAYS_INVISIBLE);
        header.add(AttributeModifier.remove("class"));
        return header;
    }
}

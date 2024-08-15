/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.impl.component.data.provider.ListDataProvider;

import com.evolveum.midpoint.gui.impl.component.tile.Tile;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.basic.AssociationDefinitionWrapper;

import com.evolveum.midpoint.gui.impl.util.GuiDisplayNameUtil;
import com.evolveum.midpoint.schema.processor.CompleteResourceSchema;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.dialog.SimplePopupable;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationTypeDefinitionType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.LoadableDetachableModel;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public abstract class ChoiceAssociationPopupPanel extends SimplePopupable {

    private static final Trace LOGGER = TraceManager.getTrace(ChoiceAssociationPopupPanel.class);

    private static final String ID_TILE = "tile";
    private static final String ID_LIST = "list";
    private static final String ID_BUTTON_CANCEL = "cancelButton";
    private static final String ID_BUTTONS = "buttons";

    private Fragment footer;

    private final ResourceDetailsModel resourceDetailsModel;

    public ChoiceAssociationPopupPanel(
            String id,
            ResourceDetailsModel resourceDetailsModel,
            List<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>> associations) {
        super(id, 940, 600, () -> LocalizationUtil.translate("ChoiceAssociationPopupPanel.title"));
        this.resourceDetailsModel = resourceDetailsModel;
        initLayout(associations);
        initFooter();
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        getContent().add(AttributeAppender.append("class", "card-footer"));
    }

    private void initFooter() {
        Fragment footer = new Fragment(Popupable.ID_FOOTER, ID_BUTTONS, this);
        footer.setOutputMarkupId(true);

        AjaxButton done = new AjaxButton(ID_BUTTON_CANCEL) {

            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                getPageBase().hideMainPopup(target);
            }
        };
        done.setOutputMarkupId(true);
        footer.add(done);
        this.footer = footer;
    }

    @Override
    public @NotNull Component getFooter() {
        return footer;
    }

    private void initLayout(List<PrismContainerValueWrapper<ShadowAssociationTypeDefinitionType>> associations) {
        LoadableDetachableModel<List<Tile<AssociationDefinitionWrapper>>> tilesModel = new LoadableDetachableModel<>() {
            @Override
            protected List<Tile<AssociationDefinitionWrapper>> load() {
                List<Tile<AssociationDefinitionWrapper>> list = new ArrayList<>();
                associations.forEach(associationWrapper -> {
                    try {
                        CompleteResourceSchema resourceSchema = resourceDetailsModel.getRefinedSchema();
                        AssociationDefinitionWrapper defWrapper = new AssociationDefinitionWrapper(associationWrapper, resourceSchema);
                        String title = GuiDisplayNameUtil.getDisplayName(associationWrapper.getRealValue(), true);
                        if (title == null) {
                            title = defWrapper.getAssociationAttribute().getLocalPart();
                        }
                        Tile<AssociationDefinitionWrapper> tile = new Tile<>(null, title);
                        tile.setValue(defWrapper);
                        list.add(tile);
                    } catch (ConfigurationException | SchemaException e) {
                        LOGGER.error("Couldn't collect association definition.", e);
                    }
                });
                return list;
            }
        };

        AssociationsListView list = new AssociationsListView(
                ID_LIST,
                new ListDataProvider<>(ChoiceAssociationPopupPanel.this, tilesModel),
                resourceDetailsModel) {
            @Override
            protected String getTitlePanelId() {
                return ID_TILE;
            }

            @Override
            protected void onTileClickPerformed(AssociationDefinitionWrapper value, AjaxRequestTarget target) {
                getPageBase().hideMainPopup(target);
                ChoiceAssociationPopupPanel.this.onTileClickPerformed(value, target);
            }
        };

        add(list);
    }

    protected abstract void onTileClickPerformed(AssociationDefinitionWrapper value, AjaxRequestTarget target);
}

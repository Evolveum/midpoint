/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.objectType.activation;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.MappingDirection;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.tile.TilePanel;
import com.evolveum.midpoint.gui.impl.component.tile.TileTablePanel;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardBasicPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.gui.impl.util.GuiDisplayNameUtil;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.model.ContainerValueWrapperFromObjectWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

/**
 * @author lskublik
 */

@PanelType(name = "rw-activation")
@PanelInstance(identifier = "rw-activation",
        applicableForType = ResourceType.class,
        applicableForOperation = OperationTypeType.WIZARD,
        display = @PanelDisplay(label = "ActivationMappingWizardPanel.title", icon = "fa fa-toggle-off"))
public class ActivationMappingWizardPanel extends AbstractWizardBasicPanel<ResourceDetailsModel> {

    public static final String PANEL_TYPE = "arw-governance";
    private static final String ID_TABLE = "table";

    private final IModel<PrismContainerWrapper<ResourceActivationDefinitionType>> containerModel;

    public ActivationMappingWizardPanel(
            String id,
            ResourceDetailsModel model,
            IModel<PrismContainerWrapper<ResourceActivationDefinitionType>> containerModel) {
        super(id, model);
        this.containerModel = containerModel;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        TileTablePanel table = new TileTablePanel<MappingTile, PrismContainerValueWrapper>(ID_TABLE) {
            @Override
            protected ISortableDataProvider createProvider() {
                return new SpecificMappingProvider(
                        ActivationMappingWizardPanel.this,
                        new ContainerValueWrapperFromObjectWrapperModel<>(containerModel, ItemPath.EMPTY_PATH),
                        MappingDirection.INBOUND);
            }

            @Override
            protected MappingTile createTileObject(PrismContainerValueWrapper object) {
                MappingTile tile = new MappingTile(object);
                tile.setIcon(WebComponentUtil.createMappingIcon(object));
                tile.setTitle(GuiDisplayNameUtil.getDisplayName(object.getRealValue()));
                return tile;
            }

            @Override
            protected Component createTile(String id, IModel<MappingTile> model) {
                return new TilePanel<>(id, model);
            }
        };

        table.setOutputMarkupId(true);

        add(table);
    }

    @Override
    protected @NotNull IModel<String> getBreadcrumbLabel() {
        return getPageBase().createStringResource("ActivationMappingWizardPanel.title");
    }

    @Override
    protected IModel<String> getTextModel() {
        return getPageBase().createStringResource("ActivationMappingWizardPanel.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return getPageBase().createStringResource("ActivationMappingWizardPanel.subText");
    }

    @Override
    protected String getCssForWidthOfFeedbackPanel() {
        return "col-11";
    }
}

/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.activation;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.MappingDirection;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.dialog.OnePanelPopupPanel;

import com.evolveum.midpoint.gui.impl.component.tile.TileTablePanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.MappingTile;
import com.evolveum.midpoint.gui.impl.util.IconAndStylesUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainerDefinition;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceActivationDefinitionType;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringTranslationType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

public class CreateActivationMappingPopup extends OnePanelPopupPanel {
    private static final Trace LOGGER = TraceManager.getTrace(CreateActivationMappingPopup.class);


    private final MappingDirection mappingDirection;
    private final IModel<PrismContainerValueWrapper<ResourceActivationDefinitionType>> parentModel;
    private final ResourceDetailsModel detailsModel;

    public CreateActivationMappingPopup(
            String id,
            MappingDirection mappingDirection,
            IModel<PrismContainerValueWrapper<ResourceActivationDefinitionType>> parentModel, ResourceDetailsModel detailsModel) {
        super(id, PageBase.createStringResourceStatic("CreateActivationMappingPopup.title"));
        this.mappingDirection = mappingDirection;
        this.parentModel = parentModel;
        this.detailsModel = detailsModel;
        setWidth(660);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        getContent().add(AttributeAppender.append("class", "p-0"));
    }

    @Override
    protected WebMarkupContainer createPanel(String id) {
        return new TileTablePanel<CreateActivationMappingTile, PrismContainerDefinition>(id) {

            @Override
            protected ISortableDataProvider createProvider() {
                return new ActivationContainerProvider(
                        CreateActivationMappingPopup.this, parentModel, mappingDirection);
            }

            @Override
            protected CreateActivationMappingTile createTileObject(PrismContainerDefinition definition) {
                CreateActivationMappingTile tile = new CreateActivationMappingTile(definition);
                tile.setIcon(IconAndStylesUtil.createMappingIcon(definition));
                try {
                    Item item = definition.instantiate();
                    item.setParent(parentModel.getObject().getNewValue());
                    tile.setTitle(WebPrismUtil.getLocalizedDisplayName(item));
                } catch (SchemaException e) {
                    LOGGER.debug("Couldn't instantiate item from definition " + definition);
                }
                tile.setDescription(WebPrismUtil.getHelpText(definition, ResourceActivationDefinitionType.class));
                try {
                    tile.setCanCreateNewValue(
                            MappingTile.MappingDefinitionType.CONFIGURED.equals(tile.getMappingDefinitionType())
                            || defineCanCreateNewValue(definition));
                } catch (SchemaException e) {
                    LOGGER.debug("Couldn't define if we can create new value for " + definition);
                }

                if (!tile.canCreateNewValue()) {
                    PolyStringType label = new PolyStringType("alreadyinUse");
                    label.setTranslation(new PolyStringTranslationType().key("CreateActivationMappingPopup.alreadyinUse"));
                    tile.addTag(new DisplayType().label(label));
                }
                return tile;
            }

            @Override
            protected Component createTile(String id, IModel<CreateActivationMappingTile> model) {
                return new CreateActivationMappingTilePanel(id, model) {
                    @Override
                    protected void onClick(AjaxRequestTarget target) {
                        if (model.getObject().canCreateNewValue()) {
                            createNewValue(getModelObject(), target);
                        }
                    }
                };
            }

            @Override
            protected String getTileCssClasses() {
                return "col-12 py-1 px-2";
            }

            @Override
            protected String getTileCssStyle() {
                return "";
            }

            @Override
            protected boolean showFooter() {
                return false;
            }

            @Override
            protected String getTilesContainerAdditionalClass() {
                return "card-footer";
            }
        };
    }

    private void createNewValue(CreateActivationMappingTile tile, AjaxRequestTarget target) {
        @NotNull ItemPath path = ItemPath.create(tile.getValue().getItemName());
        if (MappingTile.MappingDefinitionType.CONFIGURED.equals(tile.getMappingDefinitionType())) {
            path = path.append(mappingDirection.getContainerName());
        }
        PrismContainerValueWrapper<ResourceActivationDefinitionType> parent = parentModel.getObject();
        try {
            PrismContainerWrapper<Containerable> childContainer = parent.findContainer(path);
            if (childContainer.isSingleValue()) {
                childContainer.getValues().clear();
                childContainer.getItem().clear();
            }
            PrismContainerValue<Containerable> newValue = childContainer.getItem().createNewValue();
            if (MappingTile.MappingDefinitionType.CONFIGURED.equals(tile.getMappingDefinitionType())) {
                MappingType mapping = (MappingType) newValue.asContainerable();
                mapping.beginExpression();
                ExpressionUtil.addAsIsExpressionValue(mapping.getExpression());
            }
            PrismContainerValueWrapper<Containerable> valueWrapper = WebPrismUtil.createNewValueWrapper(
                    childContainer, newValue, getPageBase(), detailsModel.createWrapperContext());
            childContainer.getValues().add(valueWrapper);

            selectMapping(Model.of(valueWrapper), tile.getMappingDefinitionType(), target);
            getPageBase().hideMainPopup(target);
        } catch (SchemaException e) {
            LOGGER.debug("Couldn't find child container by path " + path + " in parent " + parent);
        }
    }

    protected <T extends PrismContainerValueWrapper<? extends Containerable>> void selectMapping(
            IModel<T> valueModel,
            MappingTile.MappingDefinitionType mappingDefinitionType,
            AjaxRequestTarget target) {
    }

    private boolean defineCanCreateNewValue(PrismContainerDefinition definition) throws SchemaException {
        if (definition.isMultiValue()) {
            return true;
        }
        PrismContainerWrapper<Containerable> child = parentModel.getObject().findContainer(definition.getItemName());
        if (child.getValues().isEmpty()){
            return true;
        }
        if (ValueStatus.DELETED.equals(child.getValues().iterator().next().getStatus())) {
            return true;
        }
        return false;
    }
}

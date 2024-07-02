/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.associationType.basic;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.evolveum.midpoint.gui.impl.component.tile.AssociationTilePanel;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardBasicPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;

import com.evolveum.midpoint.schema.processor.CompleteResourceSchema;

import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.component.tile.Tile;

import org.jetbrains.annotations.NotNull;

public abstract class AssociationChoicePanel
        extends AbstractWizardBasicPanel<ResourceDetailsModel> {

    private static final Trace LOGGER = TraceManager.getTrace(AssociationChoicePanel.class);

    private static final String ID_LIST = "list";
    private static final String ID_TILE = "tile";

    private LoadableModel<List<Tile<AssociationDefinitionWrapper>>> tilesModel;

    public AssociationChoicePanel(String id, ResourceDetailsModel resourceModel) {
        super(id, resourceModel);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        tilesModel = loadTilesModel();
        initLayout();
    }

    protected LoadableModel<List<Tile<AssociationDefinitionWrapper>>> loadTilesModel() {
        return new LoadableModel<>(false) {

            @Override
            protected List<Tile<AssociationDefinitionWrapper>> load() {
                List<Tile<AssociationDefinitionWrapper>> list = new ArrayList<>();

                try {
                    CompleteResourceSchema resourceSchema = getAssignmentHolderDetailsModel().getRefinedSchema();

                    resourceSchema.getObjectTypeDefinitions().forEach(objectTypeDef ->
                            objectTypeDef.getReferenceAttributeDefinitions().forEach(
                                    associationDef -> {
                                        if (!associationDef.canRead()) {
                                            return;
                                        }

                                        AssociationDefinitionWrapper wrapper = new AssociationDefinitionWrapper(
                                                objectTypeDef, associationDef, resourceSchema);
                                        Optional<Tile<AssociationDefinitionWrapper>> foundWrapper = list.stream()
                                                .filter(defWrapper -> defWrapper.getValue().equalsSubject(wrapper))
                                                .findFirst();

                                        if (foundWrapper.isPresent()) {
                                            foundWrapper.get().getValue().changeSubjectToObjectClassSelect();
                                        } else {
                                            Tile<AssociationDefinitionWrapper> tile = new Tile<>(null, associationDef.getItemName().getLocalPart());
                                            tile.setValue(wrapper);
                                            list.add(tile);
                                        }
                                    }));

                } catch (SchemaException | ConfigurationException e) {
                    LOGGER.error("Couldn't load complete resource schema.");
                }

                return list;
            }
        };
    }

    private void initLayout() {
        ListView<Tile<AssociationDefinitionWrapper>> list = new ListView<>(ID_LIST, tilesModel) {

            @Override
            protected void populateItem(ListItem<Tile<AssociationDefinitionWrapper>> item) {
                item.add(createTilePanel(ID_TILE, item.getModel()));
            }
        };
        add(list);
    }

    private Component createTilePanel(String id, IModel<Tile<AssociationDefinitionWrapper>> tileModel) {
        return new AssociationTilePanel(id, tileModel, getAssignmentHolderDetailsModel()) {

            @Override
            protected void onClick(AssociationDefinitionWrapper value, AjaxRequestTarget target) {
                onTileClickPerformed(value, target);
            }
        };
    }

    @Override
    protected IModel<String> getTextModel() {
        return createStringResource("AssociationChoicePanel.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return createStringResource("AssociationChoicePanel.subText");
    }

    @Override
    protected @NotNull IModel<String> getBreadcrumbLabel() {
        return getTextModel();
    }

    protected abstract void onTileClickPerformed(AssociationDefinitionWrapper value, AjaxRequestTarget target);

    @Override
    protected void onDetach() {
        super.onDetach();
        tilesModel.detach();
    }
}

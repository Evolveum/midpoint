/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStepPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismReferenceWrapper;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.SearchFactory;
import com.evolveum.midpoint.gui.impl.component.search.SearchPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lskublik
 */
public abstract class ResourceTemplateStepPanel extends WizardStepPanel {

    private static final String ID_TILES_CONTAINER = "tileContainer";
    private static final String ID_TILES = "tiles";
    private static final String ID_TILE = "tile";
//    private static final String ID_SEARCH = "search";
//    private static final String ID_BACK = "back";
    private static final String CREATE_RESOURCE_TEMPLATE_STORAGE_KEY = "resourceTemplateStorage";

    private LoadableDetachableModel<Search<AssignmentHolderType>> searchModel;
    private LoadableModel<List<TemplateTile<ResourceTemplate>>> tilesModel;

    private final ResourceDetailsModel resourceModel;

    public ResourceTemplateStepPanel(ResourceDetailsModel model) {
        super();
        this.resourceModel = model;
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("PageResource.wizard.step.selection");
    }

    @Override
    protected void onInitialize() {
        initTilesModel();
        super.onInitialize();
        initLayout();
    }

    private void initTilesModel() {
        if (tilesModel == null) {
            tilesModel = loadTileDescriptions();
        }
    }

    private void initSearchModel() {
        if (searchModel == null) {
            searchModel = new LoadableDetachableModel<>() {
                @Override
                protected Search<AssignmentHolderType> load() {
                    PageStorage storage = getStorage();
                    if (storage.getSearch() == null) {
                        Search<AssignmentHolderType> search = SearchFactory.createSearch(AssignmentHolderType.class, getPageBase());
                        storage.setSearch(search);
                        return search;
                    }
                    return storage.getSearch();
                }
            };
        }
    }

    @Override
    public Component createHeaderContent(String id) {
        initSearchModel();

        SearchPanel<AssignmentHolderType> searchPanel = new SearchPanel<>(id, searchModel) {

            @Override
            public void searchPerformed(AjaxRequestTarget target) {
                tilesModel.reset();
                target.add(getTilesContainer());
            }

            @Override
            protected void saveSearch(Search search, AjaxRequestTarget target) {
                getStorage().setSearch(search);
            }
        };
        searchPanel.add(AttributeAppender.append("class", () -> "ml-auto"));
        return searchPanel;
    }

    private void initLayout() {
        setOutputMarkupId(true);

        WebMarkupContainer tilesContainer = new WebMarkupContainer(ID_TILES_CONTAINER);
        tilesContainer.setOutputMarkupId(true);
        add(tilesContainer);
        ListView<TemplateTile<ResourceTemplate>> tiles = new ListView<>(ID_TILES, tilesModel) {
            @Override
            protected void populateItem(ListItem<TemplateTile<ResourceTemplate>> listItem) {
                listItem.add(new ResourceTilePanel(ID_TILE, listItem.getModel()) {
                    @Override
                    protected void onClick(AjaxRequestTarget target) {
                        onTemplateChosePerformed(listItem.getModelObject(), target);
                    }
                });
            }
        };
        tilesContainer.add(tiles);
    }

    private WebMarkupContainer getTilesContainer() {
        return (WebMarkupContainer) get(ID_TILES_CONTAINER);
    }

    private PageStorage getStorage() {
        PageStorage storage = getSession().getSessionStorage().getPageStorageMap().get(CREATE_RESOURCE_TEMPLATE_STORAGE_KEY);
        if (storage == null) {
            storage = getSession().getSessionStorage().getObjectListStorage(CREATE_RESOURCE_TEMPLATE_STORAGE_KEY);
        }
        return storage;
    }

    private void onTemplateChosePerformed(TemplateTile<ResourceTemplate> tile, AjaxRequestTarget target) {
        try {
            PrismObjectDefinition<ResourceType> def =
                    PrismContext.get().getSchemaRegistry().findObjectDefinitionByType(ResourceType.COMPLEX_TYPE);
            PrismObject<ResourceType> obj = def.instantiate();

            ResourceTemplate resourceTemplate = tile.getTemplateObject();
            if (resourceTemplate != null){
                if (QNameUtil.match(ConnectorType.COMPLEX_TYPE, resourceTemplate.type)) {
                    PrismReferenceWrapper<Referencable> connectorRef =
                            resourceModel.getObjectWrapper().getValue().findReference(ResourceType.F_CONNECTOR_REF);
                    connectorRef.getValue().setRealValue(new ObjectReferenceType()
                            .oid(resourceTemplate.oid)
                            .type(ConnectorType.COMPLEX_TYPE));
                }
//                else (resourceTemplate.resourceTemplate != null) {
                //TODO Use template for actual new resource
//                }
            }
            getWizard().next();
            target.add(getParent());
        } catch (SchemaException ex) {
            getPageBase().getFeedbackMessages().error(getPageBase(), ex.getUserFriendlyMessage());
            target.add(getPageBase().getFeedbackPanel());
        }
    }

    @Override
    public String appendCssToWizard() {
        return "w-100";
    }

    protected LoadableModel<List<TemplateTile<ResourceTemplate>>> loadTileDescriptions() {
        return new LoadableModel<>(false) {

            @Override
            protected List<TemplateTile<ResourceTemplate>> load() {
                List<TemplateTile<ResourceTemplate>> tiles = new ArrayList<>();

                Task loadConnectorsTask = getPageBase().createSimpleTask("load connectors");

                @NotNull List<PrismObject<ConnectorType>> connectors =
                        WebModelServiceUtils.searchObjects(
                                ConnectorType.class,
                                searchModel.getObject().createObjectQuery(getPageBase()),
                                loadConnectorsTask.getResult(),
                                getPageBase());


                if (CollectionUtils.isNotEmpty(connectors)) {
                    connectors.forEach(connector -> {
                        @NotNull ConnectorType connectorObject = connector.asObjectable();
                        String title;
                        if (connectorObject.getDisplayName() == null || connectorObject.getDisplayName().isEmpty()) {
                            title = connectorObject.getName().getOrig();
                        } else {
                            title = connectorObject.getDisplayName().getOrig();
                        }
                        tiles.add(
                                new TemplateTile(
                                        GuiStyleConstants.CLASS_OBJECT_CONNECTOR_ICON,
                                        title,
                                        new ResourceTemplate(connector.getOid(), ConnectorType.COMPLEX_TYPE))
                                        .description(connectorObject.getDescription()));
                    });
                }

                return tiles;
            }
        };
    }

    @Override
    public VisibleEnableBehaviour getNextBehaviour() {
        return VisibleEnableBehaviour.ALWAYS_INVISIBLE;
    }

    protected class ResourceTemplate implements Serializable {

        private String oid;
        private QName type;

        ResourceTemplate(String oid, QName type){
            this.oid = oid;
            this.type = type;
        }
    }

}

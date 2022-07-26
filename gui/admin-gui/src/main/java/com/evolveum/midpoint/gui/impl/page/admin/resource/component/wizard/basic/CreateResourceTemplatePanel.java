/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.basic;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.SearchFactory;
import com.evolveum.midpoint.gui.impl.component.search.SearchPanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.ResourceTilePanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnChangeAjaxFormUpdatingBehavior;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class CreateResourceTemplatePanel extends BasePanel<PrismObject<ResourceType>> {

    private static final String ID_TILES_CONTAINER = "tileContainer";
    private static final String ID_TILES = "tiles";
    private static final String ID_TILE = "tile";
    private static final String ID_SEARCH = "search";
    private static final String ID_BACK = "back";
    private static final String ID_TYPE_FIELD = "type";
    private static final String CREATE_RESOURCE_TEMPLATE_STORAGE_KEY = "resourceTemplateStorage";

    private enum TemplateType {
        ALL(AssignmentHolderType.class),
        TEMPLATE(ResourceType.class),
        CONNECTOR(ConnectorType.class);

        private final Class<AssignmentHolderType> type;

        private TemplateType(Class<? extends AssignmentHolderType> type) {
            this.type = (Class<AssignmentHolderType>) type;
        }

        public Class<AssignmentHolderType> getType() {
            return type;
        }
    }

    private LoadableDetachableModel<Search<AssignmentHolderType>> searchModel;
    private LoadableDetachableModel<List<TemplateTile<ResourceTemplate>>> tilesModel;

    private Model<TemplateType> templateType = Model.of(TemplateType.ALL);

    public CreateResourceTemplatePanel(String id) {
        super(id);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initSearchModel();
        initTilesModel();
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
                        Search<AssignmentHolderType> search
                                = SearchFactory.createSearch(templateType.getObject().getType(), getPageBase());
                        storage.setSearch(search);
                        return search;
                    }
                    return storage.getSearch();
                }
            };
        }
    }

    private void initLayout() {
        setOutputMarkupId(true);

        DropDownChoicePanel<TemplateType> type
                = WebComponentUtil.createEnumPanel(TemplateType.class, ID_TYPE_FIELD, templateType, this, false);
        type.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                super.onUpdate(target);
                searchModel.detach();
                tilesModel.detach();
                target.add(CreateResourceTemplatePanel.this.get(ID_SEARCH));
                target.add(getTilesContainer());
            }
        });
        add(type);

        AjaxLink back = new AjaxLink<>(ID_BACK) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                getPageBase().redirectBack();
            }
        };
        add(back);

        SearchPanel<AssignmentHolderType> search = initSearch();
        add(search);

        WebMarkupContainer tilesContainer = new WebMarkupContainer(ID_TILES_CONTAINER);
        tilesContainer.setOutputMarkupId(true);
        add(tilesContainer);
        ListView<TemplateTile<ResourceTemplate>> tiles = new ListView<>(ID_TILES, tilesModel) {
            @Override
            protected void populateItem(ListItem<TemplateTile<ResourceTemplate>> listItem) {
                listItem.add(new ResourceTilePanel(ID_TILE, listItem.getModel()) {
                    @Override
                    protected void onClick(AjaxRequestTarget target) {
                        onTemplateSelectionPerformed(listItem.getModelObject(), target);
                    }
                });
            }
        };
        tilesContainer.add(tiles);

    }

    private SearchPanel<AssignmentHolderType> initSearch() {
        return new SearchPanel<>(ID_SEARCH, searchModel) {

            @Override
            public void searchPerformed(AjaxRequestTarget target) {
                tilesModel.detach();
                target.add(getTilesContainer());
            }

            @Override
            protected void saveSearch(Search search, AjaxRequestTarget target) {
                getStorage().setSearch(search);
            }
        };
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

    private void onTemplateSelectionPerformed(TemplateTile<ResourceTemplate> tile, AjaxRequestTarget target) {
        try {
            PrismObjectDefinition<ResourceType> def = PrismContext.get().getSchemaRegistry().findObjectDefinitionByType(getType());
            PrismObject<ResourceType> obj = def.instantiate();

            ResourceTemplate resourceTemplate = tile.getTemplateObject();
            if (resourceTemplate != null) {
                if (QNameUtil.match(ConnectorType.COMPLEX_TYPE, resourceTemplate.type)) {
                    obj.asObjectable().beginConnectorRef()
                            .oid(resourceTemplate.oid)
                            .type(ConnectorType.COMPLEX_TYPE);
                } else if (QNameUtil.match(ResourceType.COMPLEX_TYPE, resourceTemplate.type)) {
                    Task task = getPageBase().createSimpleTask("load resource template");
                    OperationResult result = task.getResult();
                    obj.asObjectable().beginSuper().beginResourceRef()
                            .oid(resourceTemplate.oid)
                            .type(ResourceType.COMPLEX_TYPE);
                    getPageBase().getModelInteractionService().expandConfigurationObject(obj, task, result);

                    result.computeStatus();
                    if (!result.isSuccess()) {
                        getPageBase().showResult(result);
                        target.add(getPageBase().getFeedbackPanel());
                    }
                }
            }
            onTemplateSelectionPerformed(obj, target);
        } catch (SchemaException | ConfigurationException | ObjectNotFoundException ex) {
            getPageBase().getFeedbackMessages().error(getPageBase(), ex.getUserFriendlyMessage());
            target.add(getPageBase().getFeedbackPanel());
        }
    }

    abstract protected void onTemplateSelectionPerformed(PrismObject<ResourceType> newObject, AjaxRequestTarget target);

    protected LoadableDetachableModel<List<TemplateTile<ResourceTemplate>>> loadTileDescriptions() {
        return new LoadableDetachableModel<>() {

            @Override
            protected List<TemplateTile<ResourceTemplate>> load() {
                List<TemplateTile<ResourceTemplate>> tiles = new ArrayList<>();

                Task loadResourceTemplateTask = getPageBase().createSimpleTask("load resource templates");

                if (TemplateType.ALL.equals(templateType.getObject())
                        || TemplateType.CONNECTOR.equals(templateType.getObject())) {
                    @NotNull List<PrismObject<ConnectorType>> connectors =
                            WebModelServiceUtils.searchObjects(
                                    ConnectorType.class,
                                    searchModel.getObject().createObjectQuery(getPageBase()),
                                    loadResourceTemplateTask.getResult(),
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
                                            .description(getDescriptionForConnectorType(connectorObject))
                                            .tag(connectorObject.getConnectorVersion()));
                        });
                    }
                }

                if (TemplateType.ALL.equals(templateType.getObject())
                        || TemplateType.TEMPLATE.equals(templateType.getObject())) {
                    @NotNull List<PrismObject<ResourceType>> resources =
                            WebModelServiceUtils.searchObjects(
                                    ResourceType.class,
                                    searchModel.getObject().createObjectQuery(
//                                        null,
                                            getPageBase() //,
//                                        PrismContext.get()
//                                                .queryFor(ResourceType.class)
//                                                .item(ResourceType.F_TEMPLATE) //TODO uncomment after adding to repo
//                                                .eq(true).build()
                                    ),
                                    loadResourceTemplateTask.getResult(),
                                    getPageBase());

                    resources.removeIf(resource -> !Boolean.TRUE.equals(resource.asObjectable().isTemplate())); //TODO remove after adding to repo

                    if (CollectionUtils.isNotEmpty(resources)) {
                        resources.forEach(resource -> {
                            String title = WebComponentUtil.getDisplayNameOrName(resource);

                            DisplayType display =
                                    GuiDisplayTypeUtil.getDisplayTypeForObject(resource, loadResourceTemplateTask.getResult(), getPageBase());
                            tiles.add(
                                    new TemplateTile(
                                            WebComponentUtil.getIconCssClass(display),
                                            title,
                                            new ResourceTemplate(resource.getOid(), ResourceType.COMPLEX_TYPE))
                                            .description(resource.asObjectable().getDescription())
                                            .tag(getPageBase().createStringResource("CreateResourceTemplatePanel.template").getString()));
                        });
                    }
                }

                Collections.sort(tiles);
                return tiles;
            }
        };
    }

    private String getDescriptionForConnectorType(@NotNull ConnectorType connectorObject) {
        if (connectorObject.getDescription() == null) {
            return connectorObject.getName().getOrig();
        }
        return connectorObject.getDescription();
    }

    protected QName getType() {
        return ResourceType.COMPLEX_TYPE;
    }

    protected class ResourceTemplate implements Serializable {

        private String oid;
        private QName type;

        ResourceTemplate(String oid, QName type) {
            this.oid = oid;
            this.type = type;
        }
    }

}

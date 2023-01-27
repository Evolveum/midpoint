/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.basic;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.SearchBuilder;
import com.evolveum.midpoint.gui.impl.component.search.panel.SearchPanel;
import com.evolveum.midpoint.gui.impl.component.tile.TileTablePanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.basic.ResourceTemplateProvider.TemplateType;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.basic.ResourceTemplateProvider.ResourceTemplate;
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

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;

import javax.xml.namespace.QName;

public abstract class CreateResourceTemplatePanel extends BasePanel<PrismObject<ResourceType>> {

    private static final String ID_SEARCH_FRAGMENT = "searchFragment";
    private static final String ID_TILE_TABLE = "tileTable";
    private static final String ID_SEARCH = "search";
    private static final String ID_BACK = "back";
    private static final String ID_TYPE_FIELD = "type";
    private static final String CREATE_RESOURCE_TEMPLATE_STORAGE_KEY = "resourceTemplateStorage";



    private LoadableDetachableModel<Search> searchModel;

    private final Model<TemplateType> templateType = Model.of(TemplateType.CONNECTOR);

    public CreateResourceTemplatePanel(String id) {
        super(id);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initSearchModel();
        initLayout();
    }

    private void initSearchModel() {
        if (searchModel == null) {
            searchModel = new LoadableDetachableModel<>() {
                @Override
                protected Search load() {
                    PageStorage storage = getStorage();
                    TemplateType template = templateType.getObject();
                    if (storage.getSearch() == null || !storage.getSearch().getTypeClass().equals(template.getType())) {
                        SearchBuilder searchBuilder = new SearchBuilder(template.getType())
                                .modelServiceLocator(getPageBase());

                        Search search = searchBuilder.build();
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
        AjaxLink back = new AjaxLink<>(ID_BACK) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                getPageBase().redirectBack();
            }
        };
        add(back);

        TileTablePanel<TemplateTile<ResourceTemplate>, TemplateTile<ResourceTemplate>> tileTable
                = new TileTablePanel<>(ID_TILE_TABLE) {

            @Override
            protected Component createTile(String id, IModel<TemplateTile<ResourceTemplate>> model) {
                return new ResourceTilePanel(id, model) {
                    @Override
                    protected void onClick(AjaxRequestTarget target) {
                        onTemplateSelectionPerformed(model.getObject(), target);
                    }
                };
            }

            @Override
            protected TemplateTile<ResourceTemplate> createTileObject(TemplateTile<ResourceTemplate> object) {
                return object;
            }

            @Override
            protected Component createHeader(String id) {
                return createSearchFragment(id);
            }

            @Override
            protected ISortableDataProvider<TemplateTile<ResourceTemplate>, String> createProvider() {
                return new ResourceTemplateProvider(this, searchModel, templateType);
            }

            @Override
            protected String getTileCssClasses() {
                return "col-xs-6 col-sm-6 col-md-4 col-lg-3 col-xl-5i col-xxl-2 p-2";
            }
        };
        tileTable.setOutputMarkupId(true);
        add(tileTable);

    }

    private Fragment createSearchFragment(String id) {
        Fragment fragment = new Fragment(id, ID_SEARCH_FRAGMENT, CreateResourceTemplatePanel.this);
        fragment.setOutputMarkupId(true);

        DropDownChoicePanel<TemplateType> type
                = WebComponentUtil.createEnumPanel(TemplateType.class, ID_TYPE_FIELD, templateType, this, false);
        type.getBaseFormComponent().add(new EmptyOnChangeAjaxFormUpdatingBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                super.onUpdate(target);
                target.add(CreateResourceTemplatePanel.this);
            }
        });
        fragment.add(type);

        SearchPanel<AssignmentHolderType> search = initSearch();
        fragment.add(search);

        return fragment;
    }

    private SearchPanel<AssignmentHolderType> initSearch() {
        return new SearchPanel<>(ID_SEARCH, (IModel) searchModel) {

            @Override
            public void searchPerformed(AjaxRequestTarget target) {
                target.add(getTilesTable());
            }

            @Override
            protected void saveSearch(Search search, AjaxRequestTarget target) {
                getStorage().setSearch(search);
            }
        };
    }

    private WebMarkupContainer getTilesTable() {
        return (WebMarkupContainer) get(ID_TILE_TABLE);
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

            ResourceTemplate resourceTemplate = tile.getValue();
            if (resourceTemplate != null) {
                if (QNameUtil.match(ConnectorType.COMPLEX_TYPE, resourceTemplate.getType())) {
                    obj.asObjectable().beginConnectorRef()
                            .oid(resourceTemplate.getOid())
                            .type(ConnectorType.COMPLEX_TYPE);
                } else if (QNameUtil.match(ResourceType.COMPLEX_TYPE, resourceTemplate.getType())) {
                    Task task = getPageBase().createSimpleTask("load resource template");
                    OperationResult result = task.getResult();
                    obj.asObjectable().beginSuper().beginResourceRef()
                            .oid(resourceTemplate.getOid())
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

    protected QName getType() {
        return ResourceType.COMPLEX_TYPE;
    }
}

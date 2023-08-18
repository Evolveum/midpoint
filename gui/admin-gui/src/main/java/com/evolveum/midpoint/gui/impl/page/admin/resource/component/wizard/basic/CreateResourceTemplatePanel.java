/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.basic;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.SearchBuilder;
import com.evolveum.midpoint.gui.impl.component.tile.TileTablePanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.gui.impl.component.data.provider.ResourceTemplateProvider;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.basic.ResourceTemplate.TemplateType;
import com.evolveum.midpoint.prism.CloneStrategy;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

public abstract class CreateResourceTemplatePanel extends BasePanel<PrismObject<ResourceType>> {

    private static final String ID_TILE_TABLE = "tileTable";
    private static final String ID_BACK = "back";
    private static final String CREATE_RESOURCE_TEMPLATE_STORAGE_KEY = "resourceTemplateStorage";



    private LoadableDetachableModel<Search> searchModel;

    private final Model<TemplateType> templateType;

    public CreateResourceTemplatePanel(String id, Model<TemplateType> templateType) {
        super(id);
        this.templateType = templateType;
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
                onBackPerformed(target);
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

    protected void onBackPerformed(AjaxRequestTarget target) {
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
            ResourceTemplate resourceTemplate = tile.getValue();

            if (resourceTemplate != null && TemplateType.COPY_FROM_TEMPLATE.equals(resourceTemplate.getTemplateType())) {
                Task task = getPageBase().createSimpleTask("load resource template");
                OperationResult result = task.getResult();
                @Nullable PrismObject<ResourceType> resource =
                        WebModelServiceUtils.loadObject(ResourceType.class, resourceTemplate.getOid(), getPageBase(), task, result);
                PrismObject<ResourceType> obj = resource.cloneComplex(CloneStrategy.REUSE);
                obj.setOid(null);
                obj.asObjectable().template(null);
                obj.asObjectable().setName(null);
                obj.findOrCreateProperty(ResourceType.F_LIFECYCLE_STATE).addRealValue(SchemaConstants.LIFECYCLE_PROPOSED);
                onTemplateSelectionPerformed(obj, target);
                return;
            }

            PrismObjectDefinition<ResourceType> def = PrismContext.get().getSchemaRegistry().findObjectDefinitionByType(getType());
            PrismObject<ResourceType> obj = def.instantiate();
            obj.asObjectable().lifecycleState(SchemaConstants.LIFECYCLE_PROPOSED);

            if (resourceTemplate != null) {
                if (TemplateType.CONNECTOR.equals(resourceTemplate.getTemplateType())) {
                    obj.asObjectable().beginConnectorRef()
                            .oid(resourceTemplate.getOid())
                            .type(ConnectorType.COMPLEX_TYPE);
                } else if (TemplateType.INHERIT_TEMPLATE.equals(resourceTemplate.getTemplateType())) {
                    Task task = getPageBase().createSimpleTask("expand resource with template");
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

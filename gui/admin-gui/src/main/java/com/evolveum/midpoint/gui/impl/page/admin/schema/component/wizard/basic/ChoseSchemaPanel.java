/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.schema.component.wizard.basic;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.impl.component.data.provider.ObjectTileProvider;
import com.evolveum.midpoint.gui.impl.component.tile.TemplateTilePanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.data.provider.ResourceTemplateProvider;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.SearchBuilder;
import com.evolveum.midpoint.gui.impl.component.tile.TileTablePanel;
import com.evolveum.midpoint.gui.impl.duplication.DuplicationProcessHelper;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.basic.ResourceTemplate.TemplateType;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

public abstract class ChoseSchemaPanel extends BasePanel<PrismObject<ResourceType>> {

    private static final String ID_TILE_TABLE = "tileTable";
    private static final String ID_BACK = "back";
    private static final String CREATE_SCHEMA_STORAGE_KEY = "schemaChoiceStorage";

    private LoadableDetachableModel<Search> searchModel;

    public ChoseSchemaPanel(String id) {
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
                    if (storage.getSearch() == null) {
                        SearchBuilder searchBuilder = new SearchBuilder(SchemaType.class)
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

        TileTablePanel<TemplateTile<String>, TemplateTile<String>> tileTable
                = new TileTablePanel<>(ID_TILE_TABLE) {

            @Override
            protected Component createTile(String id, IModel<TemplateTile<String>> model) {
                return new TemplateTilePanel(id, model) {
                    @Override
                    protected void onClick(AjaxRequestTarget target) {
                        onTemplateSelectionPerformed(model.getObject(), target);
                    }
                };
            }

            @Override
            protected TemplateTile<String> createTileObject(TemplateTile<String> object) {
                if (object != null
                        && object.getValue() != null) {
                    object.setIcon(GuiStyleConstants.CLASS_OBJECT_SCHEMA_TEMPLATE_ICON);
                }
                return object;
            }

            @Override
            protected ISortableDataProvider<TemplateTile<String>, String> createProvider() {
                return new ObjectTileProvider(this, searchModel);
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
        PageStorage storage = getSession().getSessionStorage().getPageStorageMap().get(CREATE_SCHEMA_STORAGE_KEY);
        if (storage == null) {
            storage = getSession().getSessionStorage().getObjectListStorage(CREATE_SCHEMA_STORAGE_KEY);
        }
        return storage;
    }

    private void onTemplateSelectionPerformed(TemplateTile<String> tile, AjaxRequestTarget target) {
        String schemaOid = tile.getValue();

        Task task = getPageBase().createSimpleTask("load schema");
        OperationResult result = task.getResult();
        @Nullable PrismObject<SchemaType> schema =
                WebModelServiceUtils.loadObject(SchemaType.class, schemaOid, getPageBase(), task, result);
        onTemplateSelectionPerformed(schema, target);

    }

    abstract protected void onTemplateSelectionPerformed(PrismObject<SchemaType> existObject, AjaxRequestTarget target);

    protected QName getType() {
        return ResourceType.COMPLEX_TYPE;
    }
}

/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.IResource;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.ContainerableListPanel;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.web.component.data.ISelectableDataProvider;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkColumn;
import com.evolveum.midpoint.web.component.data.column.RoundedIconColumn;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ProcessedObjectsPanel extends ContainerableListPanel<SimulationResultProcessedObjectType, SelectableBean<SimulationResultProcessedObjectType>> {

    private static final long serialVersionUID = 1L;

    public ProcessedObjectsPanel(String id) {
        super(id, SimulationResultProcessedObjectType.class);
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.PAGE_SIMULATION_RESULT_PROCESSED_OBJECTS;
    }

    @Override
    protected IColumn<SelectableBean<SimulationResultProcessedObjectType>, String> createIconColumn() {
        // TODO
        return new RoundedIconColumn<>(null) {

            @Override
            protected DisplayType createDisplayType(IModel<SelectableBean<SimulationResultProcessedObjectType>> model) {
                return null;
            }

            @Override
            protected IModel<IResource> createPreferredImage(IModel<SelectableBean<SimulationResultProcessedObjectType>> model) {
                return () -> null;
            }
        };
    }

    @Override
    protected IColumn<SelectableBean<SimulationResultProcessedObjectType>, String> createNameColumn(
            IModel<String> displayModel, GuiObjectColumnType customColumn, ExpressionType expression) {

        displayModel = displayModel == null ? createStringResource("ProcessedObjectsPanel.nameColumn") : displayModel;

        return new AjaxLinkColumn<>(displayModel, ProcessedObjectsProvider.SORT_BY_NAME, null) {
            private static final long serialVersionUID = 1L;

            @Override
            public IModel<String> createLinkModel(IModel<SelectableBean<SimulationResultProcessedObjectType>> rowModel) {
                return () -> {
                    SelectableBean bean = rowModel.getObject();
                    if (bean == null) {
                        return null;
                    }

                    // todo if bean.getValue == null ||  bean.getResult is not success - show warning/error with some text instead of name
                    SimulationResultProcessedObjectType obj = rowModel.getObject().getValue();
                    if (obj == null) {
                        return "asdf";
                    }

                    return WebComponentUtil.getTranslatedPolyString(obj.getName());
                };
            }

            @Override
            public void onClick(AjaxRequestTarget target, IModel<SelectableBean<SimulationResultProcessedObjectType>> rowModel) {
                onObjectNameClicked(target, rowModel.getObject());
            }
        };
    }

    private void onObjectNameClicked(AjaxRequestTarget target, SelectableBean<SimulationResultProcessedObjectType> bean) {
        SimulationResultProcessedObjectType object = bean.getValue();
        if (object == null) {
            // todo implement case where there was a problem loading object for row
            return;
        }

        PageParameters params = new PageParameters();
        params.set(PageSimulationResultObject.PAGE_PARAMETER_RESULT_OID, getSimulationResultOid());
        String tagOid = getTagOid();
        if (tagOid != null) {
            params.set(PageSimulationResultObject.PAGE_PARAMETER_TAG_OID, tagOid);
        }
        params.set(PageSimulationResultObject.PAGE_PARAMETER_CONTAINER_ID, object.getId());

        setResponsePage(new PageSimulationResultObject(params));
    }

    @Override
    protected ISelectableDataProvider<SelectableBean<SimulationResultProcessedObjectType>> createProvider() {
        return new ProcessedObjectsProvider(this, getSearchModel()) {

            @Override
            protected @NotNull String getSimulationResultOid() {
                return ProcessedObjectsPanel.this.getSimulationResultOid();
            }

            @Override
            protected String getTagOid() {
                return ProcessedObjectsPanel.this.getTagOid();
            }

            // todo remove
            @Override
            public ObjectQuery getQuery() {
                return super.getQuery();
            }
        };
    }

    @NotNull
    protected String getSimulationResultOid() {
        return null;
    }

    protected String getTagOid() {
        return null;
    }

    @Override
    public List<SimulationResultProcessedObjectType> getSelectedRealObjects() {
        return getSelectedObjects().stream().map(o -> o.getValue()).collect(Collectors.toList());
    }

    @Override
    protected PrismContainerDefinition<SimulationResultProcessedObjectType> getContainerDefinitionForColumns() {
        return getPrismContext().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(SimulationResultType.class)
                .findContainerDefinition(SimulationResultType.F_PROCESSED_OBJECT);
    }
}

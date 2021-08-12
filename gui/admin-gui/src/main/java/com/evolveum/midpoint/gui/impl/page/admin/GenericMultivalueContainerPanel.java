/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanelWithDetailsPanel;

import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.session.UserProfileStorage;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.web.application.PanelType;

import java.util.Collections;
import java.util.List;

@PanelType(name = "genericMultiValue", generic = true)
public class GenericMultivalueContainerPanel<C extends Containerable, O extends ObjectType> extends AbstractObjectMainPanel<O> {

    private static final String ID_DETAILS = "details";

    public GenericMultivalueContainerPanel(String id, LoadableModel<PrismObjectWrapper<O>> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {
        MultivalueContainerListPanelWithDetailsPanel<C> panel = new MultivalueContainerListPanelWithDetailsPanel<C>(ID_DETAILS, getTypeClass()) {
            @Override
            protected MultivalueContainerDetailsPanel<C> getMultivalueContainerDetailsPanel(ListItem<PrismContainerValueWrapper<C>> item) {
                return null;
            }

            @Override
            protected boolean isCreateNewObjectVisible() {
                return false;
            }

            @Override
            protected IModel<PrismContainerWrapper<C>> getContainerModel() {
                return createContainerModel();
            }

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return null;
            }

            @Override
            protected List<IColumn<PrismContainerValueWrapper<C>, String>> createDefaultColumns() {
                return Collections.emptyList();
            }

            @Override
            protected boolean isCollectionViewPanel() {
                return getPanelConfiguration().getListView() !=null;
            }

            //TODO generalize
            @Override
            protected CompiledObjectCollectionView getObjectCollectionView() {
                GuiObjectListViewType listView = getPanelConfiguration().getListView();
                if (listView == null) {
                    return null;
                }
                CollectionRefSpecificationType collectionRefSpecificationType = listView.getCollection();
                if (collectionRefSpecificationType == null) {
                    return null;
                }
                Task task = getPageBase().createSimpleTask("Compile collection");
                OperationResult result = task.getResult();
                try {
                    return getPageBase().getModelInteractionService().compileObjectCollectionView(collectionRefSpecificationType, getTypeClass(), task, result);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
//        SingleContainerPanel<C> panel = new SingleContainerPanel<>(ID_DETAILS, createContainerModel(), getType());
        add(panel);

        AssignmentType a;

    }

}

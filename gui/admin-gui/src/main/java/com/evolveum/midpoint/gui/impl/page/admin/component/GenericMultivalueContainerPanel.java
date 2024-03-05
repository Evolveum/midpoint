/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.component;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanelWithDetailsPanel;

import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.web.session.UserProfileStorage;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.web.application.PanelType;

import javax.xml.namespace.QName;
import java.util.Collections;
import java.util.List;

@PanelType(name = "tablePanel")
public class GenericMultivalueContainerPanel<C extends Containerable, O extends ObjectType> extends AbstractObjectMainPanel<O, ObjectDetailsModels<O>> {

    private static final String ID_DETAILS = "details";

    public GenericMultivalueContainerPanel(String id, ObjectDetailsModels<O> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {
        MultivalueContainerListPanelWithDetailsPanel<C> panel = new MultivalueContainerListPanelWithDetailsPanel<C>(ID_DETAILS, getListTypeClass(), getPanelConfiguration()) {
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
        };
        add(panel);
    }

    private <C extends Containerable> Class<C> getListTypeClass() {
        return (Class<C>) WebComponentUtil.qnameToClass(getListType());
    }

    private QName getListType() {
        if (getPanelConfiguration().getListView() == null) {
            throw new IllegalArgumentException("Cannot instantiate panel without proper configuration. List view configuration missing for " + getPanelConfiguration());
        }

        if (getPanelConfiguration().getListView().getType() == null) {
            throw new IllegalArgumentException("Cannot instantiate panel without proper configuration. Type is mandatory for list view configuration in " + getPanelConfiguration());
        }
        return getPanelConfiguration().getListView().getType();
    }

}

/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.dashboard.component;

import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.web.application.PanelType;

import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.ContainerableListPanel;
import com.evolveum.midpoint.gui.impl.component.data.column.CompositedIconColumn;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.IconCssStyle;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.assignment.AssignmentsUtil;
import com.evolveum.midpoint.web.component.data.ISelectableDataProvider;
import com.evolveum.midpoint.web.component.data.SelectableBeanContainerDataProvider;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import javax.xml.namespace.QName;

@PanelType(name = "myAccesses")
//TODO TODO TODO
public class MyAccessesPreviewDataPanel extends ContainerableListPanel<AssignmentType, SelectableBean<AssignmentType>> {

    public MyAccessesPreviewDataPanel(String id) {
        super(id, AssignmentType.class);
    }

    public MyAccessesPreviewDataPanel(String id, Collection<SelectorOptions<GetOperationOptions>> options) {
        super(id, AssignmentType.class, options);
    }

    public MyAccessesPreviewDataPanel(String id, Collection<SelectorOptions<GetOperationOptions>> options, ContainerPanelConfigurationType configurationType) {
        super(id, AssignmentType.class, options, configurationType);
        setDashboard(true);
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return null;
    }

    @Override
    protected AssignmentType getRowRealValue(SelectableBean<AssignmentType> rowModelObject) {
        return rowModelObject.getValue();
    }

    @Override
    protected IColumn<SelectableBean<AssignmentType>, String> createCheckboxColumn() {
        return null;
    }

    @Override
    protected IColumn<SelectableBean<AssignmentType>, String> createIconColumn() {
        return ColumnUtils.createAssignmentIconColumn(getPageBase());
    }


    //TODO default columns. what about other assignment tables? unify somehow
    @Override
    protected List<IColumn<SelectableBean<AssignmentType>, String>> createDefaultColumns() {
        return ColumnUtils.getDefaultAssignmentsColumns(getPageBase());
    }

    @Override
    protected ISelectableDataProvider<AssignmentType, SelectableBean<AssignmentType>> createProvider() {
        SelectableBeanContainerDataProvider<AssignmentType> containerDataProvider = new SelectableBeanContainerDataProvider<>(this, getSearchModel(), null, false) {

            @Override
            public ObjectQuery getQuery() {
                Collection<QName> delegationRelations = getPageBase().getRelationRegistry()
                        .getAllRelationsFor(RelationKindType.DELEGATION);

                return getPrismContext().queryFor(AssignmentType.class)
                        .ownedBy(UserType.class, UserType.F_ASSIGNMENT)
                            .id(SecurityUtil.getPrincipalOidIfAuthenticated())
                        .and()
                            .not()
                                .item(AssignmentType.F_TARGET_REF)
                                .refType(ArchetypeType.COMPLEX_TYPE)
                        .and()
                            .not()
                                .item(AssignmentType.F_TARGET_REF)
                                .refRelation(delegationRelations.toArray(new QName[0]))
                        .build();
            }
        };
//                containerDataProvider.getQuery().getPaging().setMaxSize(5);
        return containerDataProvider;
    }

    @Override
    public List<AssignmentType> getSelectedRealObjects() {
        return null;
    }

    @Override
    protected boolean isPagingVisible() {
        return false;
    }

}

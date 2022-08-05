/*
 * Copyright (C) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.dashboard.component;

import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.component.assignmentType.AbstractAssignmentTypePanel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.application.PanelType;

import com.evolveum.midpoint.web.component.data.column.AjaxLinkColumn;
import com.evolveum.midpoint.web.component.util.RepoAssignmentListProvider;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;

import org.apache.wicket.ajax.AjaxRequestTarget;
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
    protected IColumn<SelectableBean<AssignmentType>, String> createNameColumn(IModel<String> displayModel, GuiObjectColumnType customColumn, ItemPath itemPath, ExpressionType expression) {
        displayModel = displayModel == null ? createStringResource("PolicyRulesPanel.nameColumn") : displayModel;

        return new AjaxLinkColumn<>(displayModel, RepoAssignmentListProvider.TARGET_NAME_STRING, null) {
            private static final long serialVersionUID = 1L;

            @Override
            public IModel<String> createLinkModel(IModel<SelectableBean<AssignmentType>> rowModel) {
                return new LoadableModel<>() {
                    @Override
                    protected String load() {
                        Collection<String> evaluatedValues = loadExportableColumnDataModel(rowModel, customColumn, itemPath, expression);
                        return ColumnUtils.loadValuesForAssignmentNameColumn(rowModel, evaluatedValues,
                                expression != null || itemPath != null, getPageBase());
                    }
                };
            }

            @Override
            public boolean isEnabled(IModel<SelectableBean<AssignmentType>> rowModel) {
                return false;
            }
        };
    }

    @Override
    protected IColumn<SelectableBean<AssignmentType>, String> createCheckboxColumn() {
        return null;
    }

    @Override
    protected IColumn<SelectableBean<AssignmentType>, String> createIconColumn() {
        return ColumnUtils.createAssignmentIconColumn(getPageBase());
    }


    @Override
    protected List<IColumn<SelectableBean<AssignmentType>, String>> createDefaultColumns() {
        return ColumnUtils.getDefaultAssignmentsColumns(null, SelectableBeanImpl.F_VALUE, true, getPageBase());
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

/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.assignment;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.AssignmentValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismReferenceWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn.ColumnType;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismReferenceWrapperColumn;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkColumn;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.search.SearchFactory;
import com.evolveum.midpoint.web.component.search.SearchItemDefinition;
import com.evolveum.midpoint.web.page.admin.PageAdminFocus;
import com.evolveum.midpoint.web.page.admin.resources.PageResourceWizard;
import com.evolveum.midpoint.web.page.admin.roles.PageRole;
import com.evolveum.midpoint.web.page.admin.users.PageOrgUnit;
import com.evolveum.midpoint.web.page.admin.users.component.AssignmentInfoDto;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author lskublik
 */
public class DirectAndIndirectAssignmentPanel extends AssignmentPanel {
    private static final long serialVersionUID = 1L;

    private LoadableModel<List<PrismContainerValueWrapper<AssignmentType>>> allAssignmentModel = null;

    public DirectAndIndirectAssignmentPanel(String id, IModel<PrismContainerWrapper<AssignmentType>> assignmentContainerWrapperModel){
        super(id, assignmentContainerWrapperModel);
    }

    @Override
    protected IModel<List<PrismContainerValueWrapper<AssignmentType>>> loadValuesModel(IModel<List<PrismContainerValueWrapper<AssignmentType>>> originalLoadValuesModel) {
        PageBase pageBase = getPageBase();
        if (pageBase instanceof PageAdminFocus) {
            if (allAssignmentModel == null) {
                allAssignmentModel = new LoadableModel<List<PrismContainerValueWrapper<AssignmentType>>>() {

                    @Override
                    protected List<PrismContainerValueWrapper<AssignmentType>> load() {
                        return (List) ((PageAdminFocus<?>) pageBase).showAllAssignmentsPerformed(getModel());
                    }
                };
            }
            return allAssignmentModel;
        } else {
            return originalLoadValuesModel;
        }
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<AssignmentType>, String>> initBasicColumns() {
        List<IColumn<PrismContainerValueWrapper<AssignmentType>, String>> columns = new ArrayList<>();

        columns.add(new IconColumn<PrismContainerValueWrapper<AssignmentType>>(Model.of("")) {

            private static final long serialVersionUID = 1L;

            @Override
            protected DisplayType getIconDisplayType(IModel<PrismContainerValueWrapper<AssignmentType>> rowModel) {
                AssignmentType assignment = rowModel.getObject().getRealValue();
                if (assignment != null && assignment.getTargetRef() != null && StringUtils.isNotEmpty(assignment.getTargetRef().getOid())) {
                    List<ObjectType> targetObjectList = WebComponentUtil.loadReferencedObjectList(Collections.singletonList(assignment.getTargetRef()), OPERATION_LOAD_ASSIGNMENTS_TARGET_OBJ,
                            DirectAndIndirectAssignmentPanel.this.getPageBase());
                    if (CollectionUtils.isNotEmpty(targetObjectList) && targetObjectList.size() == 1) {
                        ObjectType targetObject = targetObjectList.get(0);
                        DisplayType displayType = WebComponentUtil.getArchetypePolicyDisplayType(targetObject, DirectAndIndirectAssignmentPanel.this.getPageBase());
                        if (displayType != null) {
                            String disabledStyle;
                            if (targetObject instanceof FocusType) {
                                disabledStyle = WebComponentUtil.getIconEnabledDisabled(((FocusType) targetObject).asPrismObject());
                                if (displayType.getIcon() != null && StringUtils.isNotEmpty(displayType.getIcon().getCssClass()) &&
                                        disabledStyle != null) {
                                    displayType.getIcon().setCssClass(displayType.getIcon().getCssClass() + " " + disabledStyle);
                                    displayType.getIcon().setColor("");
                                }
                            }
                            return displayType;
                        }
                    }
                }
                return WebComponentUtil.createDisplayType(WebComponentUtil.createDefaultBlackIcon(
                        AssignmentsUtil.getTargetType(rowModel.getObject().getRealValue())));
            }

        });

        columns.add(new PrismReferenceWrapperColumn<AssignmentType, ObjectReferenceType>(getModel(), AssignmentType.F_TARGET_REF, ColumnType.LINK, getPageBase()){

            @Override
            protected Component createHeader(String componentId, IModel<? extends PrismContainerDefinition<AssignmentType>> mainModel) {
                return new Label(componentId, getPageBase().createStringResource("DirectAndIndirectAssignmentPanel.column.name"));
            }

        });

        columns.add(new AbstractColumn<PrismContainerValueWrapper<AssignmentType>, String>(createStringResource("DirectAndIndirectAssignmentPanel.column.type")) {
            @Override
            public void populateItem(Item<ICellPopulator<PrismContainerValueWrapper<AssignmentType>>> cellItem,
                    String componentId, final IModel<PrismContainerValueWrapper<AssignmentType>> rowModel) {
                AssignmentValueWrapper object = (AssignmentValueWrapper) rowModel.getObject();
                cellItem.add(new Label(componentId, new IModel<String>() {

                    @Override
                    public String getObject() {
                        return object.isDirectAssignment() ?
                                createStringResource("DirectAndIndirectAssignmentPanel.type.direct").getString() :
                                createStringResource("DirectAndIndirectAssignmentPanel.type.indirect").getString();
                    }
                }));
                ObjectType assignmentParent = object.getAssignmentParent();
                if (assignmentParent != null) {
                    cellItem.add(AttributeModifier.replace("title",
                            createStringResource("DirectAndIndirectAssignmentPanel.tooltip.indirect.parent", assignmentParent.getName()).getString()));
                }
            }

            @Override
            public String getCssClass() {
                return "col-md-1";
            }
        });
        columns.add(new PrismPropertyWrapperColumn<AssignmentType, String>(getModel(), AssignmentType.F_DESCRIPTION, ColumnType.STRING, getPageBase()){
            @Override
            protected boolean isHelpTextVisible(boolean helpTextVisible) {
                return false;
            }
        });
        columns.add(new PrismReferenceWrapperColumn<AssignmentType, ObjectReferenceType>(getModel(), AssignmentType.F_TENANT_REF, ColumnType.STRING, getPageBase()));
        columns.add(new PrismReferenceWrapperColumn<AssignmentType, ObjectReferenceType>(getModel(), AssignmentType.F_ORG_REF, ColumnType.STRING, getPageBase()));
        columns.add(new PrismPropertyWrapperColumn<AssignmentType, String>(getModel(), ItemPath.create(AssignmentType.F_CONSTRUCTION, ConstructionType.F_KIND), ColumnType.STRING, getPageBase()));
        columns.add(new PrismPropertyWrapperColumn<AssignmentType, String>(getModel(), ItemPath.create(AssignmentType.F_CONSTRUCTION, ConstructionType.F_INTENT), ColumnType.STRING, getPageBase()));

        columns.add(new AbstractColumn<PrismContainerValueWrapper<AssignmentType>, String>(
                createStringResource("AbstractRoleAssignmentPanel.relationLabel")) {
            @Override
            public void populateItem(Item<ICellPopulator<PrismContainerValueWrapper<AssignmentType>>> item, String componentId, IModel<PrismContainerValueWrapper<AssignmentType>> assignmentModel) {
                item.add(new Label(componentId, WebComponentUtil.getRelationLabelValue(assignmentModel.getObject(), getPageBase())));
            }

            @Override
            public String getCssClass() {
                return "col-md-1";
            }
        });
        return columns;
    }

    private void chooseOperationPerformed(String oid, Class clazz){
        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, oid);

        PageBase page = getPageBase();

        if(clazz.equals(RoleType.class)){
            page.navigateToNext(PageRole.class, parameters);
        } else if(clazz.equals(ResourceType.class)){
            page.navigateToNext(PageResourceWizard.class, parameters);
        } else if(clazz.equals(OrgType.class)){
            page.navigateToNext(PageOrgUnit.class, parameters);
        }
    }

    @Override
    protected List<SearchItemDefinition> createSearchableItems(PrismContainerDefinition<AssignmentType> containerDef) {
        List<SearchItemDefinition> defs = new ArrayList<>();
        SearchFactory.addSearchRefDef(containerDef, AssignmentType.F_TARGET_REF, defs, AreaCategoryType.ADMINISTRATION, getPageBase());
        return defs;
    }

    @Override
    protected void refreshTable(AjaxRequestTarget ajaxRequestTarget) {
        allAssignmentModel.reset();
        super.refreshTable(ajaxRequestTarget);

    }
}

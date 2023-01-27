/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.user.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.ContainerableListPanel;
import com.evolveum.midpoint.gui.impl.component.data.column.ConfigurableExpressionColumn;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.user.UserDetailsModel;
import com.evolveum.midpoint.model.api.context.AssignmentPathSegment;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.data.ISelectableDataProvider;
import com.evolveum.midpoint.web.component.data.SelectableBeanReferenceDataProvider;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@PanelType(name = "userAllAccesses")
@PanelInstance(identifier = "igaAccesses",
        applicableForType = UserType.class,
        display = @PanelDisplay(label = "IGA Accesses", icon = GuiStyleConstants.CLASS_CIRCLE_FULL, order = 25))
public class AllAccessListPanel extends AbstractObjectMainPanel<UserType, UserDetailsModel> {

    private static final String ID_ACCESSES = "accesses";

    public AllAccessListPanel(String id, UserDetailsModel model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {
        var accessesTable = new ContainerableListPanel<ObjectReferenceType, SelectableBean<ObjectReferenceType>>(ID_ACCESSES, ObjectReferenceType.class, null, getPanelConfiguration()) {
            @Override
            protected UserProfileStorage.TableId getTableId() {
                return UserProfileStorage.TableId.PANEL_USER_ACCESSES;
            }

            @Override
            protected IColumn<SelectableBean<ObjectReferenceType>, String> createIconColumn() {
                return null;
            }

            @Override
            protected ISelectableDataProvider<SelectableBean<ObjectReferenceType>> createProvider() {
                return createSearchProvier(getSearchModel());
            }

            @Override
            public List<ObjectReferenceType> getSelectedRealObjects() {
                return null;
            }

            @Override
            protected IColumn<SelectableBean<ObjectReferenceType>, String> createNameColumn(IModel<String> displayModel, GuiObjectColumnType customColumn, ExpressionType expression) {
                return createCustomExportableColumn(displayModel, customColumn, expression);
            }

            @Override
            protected IColumn<SelectableBean<ObjectReferenceType>, String> createCustomExportableColumn(IModel<String> columnDisplayModel, GuiObjectColumnType customColumn, ExpressionType expression) {
                return new ConfigurableExpressionColumn<>(columnDisplayModel, null, customColumn, expression, getPageBase()) {

                    @Override
                    protected void processVariables(VariablesMap variablesMap, ObjectReferenceType rowValue) {
                        super.processVariables(variablesMap,rowValue);
                        variablesMap.put("metadata", getMetadata(rowValue), ProvenanceMetadataType.class);
                        variablesMap.put("activation", getActivation(rowValue), ProvenanceMetadataType.class);
                        variablesMap.put("assignment", getAssignment(rowValue), ProvenanceMetadataType.class);
                        variablesMap.put("owner", getObjectDetailsModels().getObjectType(), UserType.class);
                        variablesMap.put("target", getResolvedTarget(rowValue), WebComponentUtil.qnameToClass(PrismContext.get(), rowValue.getType()));
                    }
                };
            }
        };
        accessesTable.setOutputMarkupId(true);
        add(accessesTable);
    }

    private AssignmentType getAssignment(ObjectReferenceType ref) {
        UserType user = getObjectDetailsModels().getObjectType();
        for (AssignmentType assignmentType : user.getAssignment()) {
            if (assignmentType.getTargetRef().getOid().equals(ref.getOid()) && QNameUtil.match(assignmentType.getTargetRef().getType(), ref.getType())) {
                return assignmentType;
            }
        }
        return null;
    }

    private ActivationType getActivation(ObjectReferenceType ref) {
        UserType user = getObjectDetailsModels().getObjectType();
        for (AssignmentType assignmentType : user.getAssignment()) {
            if (assignmentType.getTargetRef().getOid().equals(ref.getOid()) && QNameUtil.match(assignmentType.getTargetRef().getType(), ref.getType())) {
                return assignmentType.getActivation();
            }
        }
        return new ActivationType().effectiveStatus(ActivationStatusType.ENABLED);
    }

    private ProvenanceMetadataType getMetadata(ObjectReferenceType rowValue) {
//        UserType user = getObjectDetailsModels().getObjectType();
//        Optional<ObjectReferenceType> ref = user.getRoleMembershipRef().stream().filter(r -> r.equals(rowValue)).findFirst();


        PrismContainer<ProvenanceMetadataType> provenance = rowValue.asReferenceValue().getValueMetadataAsContainer().getAnyValue().findContainer(ValueMetadataType.F_PROVENANCE);
        ProvenanceMetadataType provenanceMetadataType = provenance.getRealValue();
        List<AssignmentPathSegmentType> paths = provenanceMetadataType.getAssignmentPath().get(0).getSegment();
        int sergments = paths.size();
        List<String> rolePaths = new ArrayList<>();
        for (int i = 0; i < sergments - 2; i++) {

            String name = WebModelServiceUtils.resolveReferenceName(paths.get(i).getTargetRef(), getPageBase(), true);
            rolePaths.add(name);
        }
        rolePaths.stream().collect(Collectors.joining(" -> "));
        return provenanceMetadataType;
    }
    private <R extends AbstractRoleType> R getResolvedTarget(ObjectReferenceType rowValue) {
        if (rowValue.getObject() != null) {
            return (R) rowValue.getObject().asObjectable();
        }

        if (rowValue.getOid() != null) {
            PrismObject<R> resolvedTarget = WebModelServiceUtils.loadObject(rowValue, getPageBase());
            if (resolvedTarget != null) {
                return resolvedTarget.asObjectable();
            }
        }
        return null;
    }

    private ISelectableDataProvider<SelectableBean<ObjectReferenceType>> createSearchProvier(IModel<Search<ObjectReferenceType>> searchModel) {
        return new SelectableBeanReferenceDataProvider(AllAccessListPanel.this, searchModel, null, false) {

            @Override
            protected ObjectQuery getCustomizeContentQuery() {
                return PrismContext.get().queryForReferenceOwnedBy(UserType.class, UserType.F_ROLE_MEMBERSHIP_REF)
                        .id(getObjectWrapper().getOid())
                        .build();
            }
        };
    }
}

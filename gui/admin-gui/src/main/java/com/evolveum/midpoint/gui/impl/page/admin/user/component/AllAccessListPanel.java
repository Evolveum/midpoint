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
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.Channel;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanReferenceDataProvider;
import com.evolveum.midpoint.web.component.assignment.AssignmentsUtil;
import com.evolveum.midpoint.web.component.data.column.ObjectReferenceColumn;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
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
            protected IColumn<SelectableBean<ObjectReferenceType>, String> createCheckboxColumn() {
                return null;
            }

            @Override
            protected List<IColumn<SelectableBean<ObjectReferenceType>, String>> createDefaultColumns() {
                return createAllAccessesColumns();
            }

            @Override
            protected IColumn<SelectableBean<ObjectReferenceType>, String> createCustomExportableColumn(IModel<String> columnDisplayModel, GuiObjectColumnType customColumn, ExpressionType expression) {
                return new ConfigurableExpressionColumn<>(columnDisplayModel, null, customColumn, expression, getPageBase()) {

                    @Override
                    protected void processVariables(VariablesMap variablesMap, ObjectReferenceType rowValue) {
                        super.processVariables(variablesMap,rowValue);
                        variablesMap.put("metadata", collectProvenanceMetadata(rowValue.asReferenceValue()), ProvenanceMetadataType.class);
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

    private List<IColumn<SelectableBean<ObjectReferenceType>, String>> createAllAccessesColumns() {
        List<IColumn<SelectableBean<ObjectReferenceType>, String>> columns = new ArrayList<>();
        ObjectReferenceColumn<SelectableBean<ObjectReferenceType>> accessColumn = new ObjectReferenceColumn<>(createStringResource("Access"), "value") {
            @Override
            public IModel<List<ObjectReferenceType>> extractDataModel(IModel<SelectableBean<ObjectReferenceType>> rowModel) {
                return () -> Collections.singletonList(getReferenceWithResolvedName(rowModel.getObject().getValue()));
            }
        };
        columns.add(accessColumn);

        ObjectReferenceColumn<SelectableBean<ObjectReferenceType>> sourceColumns = new ObjectReferenceColumn<>(createStringResource("Source"), "value") {
            @Override
            public IModel<List<ObjectReferenceType>> extractDataModel(IModel<SelectableBean<ObjectReferenceType>> rowModel) {
               return () -> {
                        List<ProvenanceMetadataType> metadataValues = collectProvenanceMetadata(rowModel.getObject().getValue().asReferenceValue());
                    if (metadataValues == null) {
                        return null;
                    }
                   List<AssignmentPathType> assignmentPaths = new ArrayList<>();
                    for (ProvenanceMetadataType metadataType : metadataValues) {
                        assignmentPaths.addAll(metadataType.getAssignmentPath());
                    }

                    List<ObjectReferenceType> refs = new ArrayList<>();
                    for (AssignmentPathType assignmentPathType : assignmentPaths) {
                        List<AssignmentPathSegmentType> segments = assignmentPathType.getSegment();
                        if (CollectionUtils.isEmpty(segments) || segments.size() == 1) {
                            continue;
                        }
                        AssignmentPathSegmentType sourceSegment = segments.get(0);
                        refs.add(getReferenceWithResolvedName(sourceSegment.getTargetRef()));
                    }
                    return refs;
                };
            }
        };
        columns.add(sourceColumns);

//        ObjectReferenceColumn<SelectableBean<ObjectReferenceType>> immediateParent = new ObjectReferenceColumn<>(createStringResource("Immediate parent"), "value") {
//            @Override
//            public IModel<ObjectReferenceType> extractDataModel(IModel<SelectableBean<ObjectReferenceType>> rowModel) {
//                ProvenanceMetadataType metadata = collectProvenanceMetadata(rowModel.getObject().getValue());
//                if (metadata == null) {
//                    return null;
//                }
//                List<AssignmentPathType> assignmentPaths = metadata.getAssignmentPath();
//
//                for (AssignmentPathType assignmentPathType : assignmentPaths) {
//                    List<AssignmentPathSegmentType> segments = assignmentPathType.getSegment();
//                    if (CollectionUtils.isEmpty(segments) || segments.size() == 1) {
//                        continue;
//                    }
//                    AssignmentPathSegmentType sourceSegment = segments.get(segments.size() - 1);
//                    return Model.of(getReferenceWithResolvedName(sourceSegment.getSourceRef()));
//                }
//                return null;
//            }
//        };
//        columns.add(immediateParent);

        AbstractExportableColumn<SelectableBean<ObjectReferenceType>, String> assignmentPath = new AbstractExportableColumn<>(createStringResource("Assignment path")) {

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<ObjectReferenceType>>> cellItem, String componentId, IModel<SelectableBean<ObjectReferenceType>> rowModel) {
                RepeatingView repeatingView = new RepeatingView(componentId);
                cellItem.add(repeatingView);

                List<String> paths = resolvedPaths(rowModel.getObject());
                for (String path : paths) {
                    Label pathLabel = new Label(repeatingView.newChildId(), path);
                    repeatingView.add(pathLabel);
                }
            }

            @Override
            public IModel<List<String>> getDataModel(IModel<SelectableBean<ObjectReferenceType>> rowModel) {

                return () -> resolvedPaths(rowModel.getObject());
            }
        };
        columns.add(assignmentPath);

        var why = new AbstractExportableColumn<SelectableBean<ObjectReferenceType>, String>(createStringResource("Why")) {
            @Override
            public IModel<String> getDataModel(IModel<SelectableBean<ObjectReferenceType>> iModel) {
                AssignmentType assignmentType = getAssignment(iModel.getObject().getValue());
                if (assignmentType == null) {
                    return () -> "";
                }
                MetadataType metadataType = assignmentType.getMetadata();
                if (metadataType == null) {
                    return null;
                }
                String chanel = metadataType.getCreateChannel();
                if (chanel == null) {
                    return () -> "channel null, cannot deremine why";
                }

                return () ->  {
                    String creator = null;
                    String approvers = null;
                    String approverComments = null;
                    Channel channel = Channel.findChannel(chanel);
                    switch (channel) {
                        case SELF_SERVICE:
                        case USER:
                            creator = WebModelServiceUtils.resolveReferenceName(metadataType.getCreatorRef(), getPageBase());
                            approvers = metadataType.getCreateApproverRef()
                                    .stream()
                                    .map(approver -> WebModelServiceUtils.resolveReferenceName(approver, getPageBase()))
                                    .collect(Collectors.joining(", "));
                            approverComments = metadataType.getCreateApprovalComment().stream().collect(Collectors.joining(". "));
                            break;
                        case IMPORT:
                        case ASYNC_UPDATE:
                        case DISCOVERY:
                        case LIVE_SYNC:
                        case RECOMPUTATION:
                        case RECONCILIATION:
                            creator = WebModelServiceUtils.resolveReferenceName(metadataType.getCreateTaskRef(), getPageBase());
                    }
                    String whyStatement =  "Created by: " + creator;
                    if (approvers != null && !approvers.isBlank()) {
                        whyStatement += "\n Approved by: " + approvers;
                    }
                    if (approverComments != null && !approverComments.isBlank()) {
                        whyStatement += "\n With a comment: " + approverComments;
                    }
                    return whyStatement;
                };
            }
        };
        columns.add(why);


        var since = new AbstractExportableColumn<SelectableBean<ObjectReferenceType>, String>(createStringResource("Since")) {
            @Override
            public IModel<String> getDataModel(IModel<SelectableBean<ObjectReferenceType>> iModel) {
                AssignmentType assignmentType = getAssignment(iModel.getObject().getValue());
                if (assignmentType == null) {
                    return () -> "";
                }
                MetadataType metadataType = assignmentType.getMetadata();
                if (metadataType == null) {
                    return null;
                }
                return () -> WebComponentUtil.formatDate(metadataType.getCreateTimestamp());

            };

        };
        columns.add(since);

        var activationColumn = new AbstractExportableColumn<SelectableBean<ObjectReferenceType>, String>(createStringResource("Activation")) {
            @Override
            public IModel<?> getDataModel(IModel<SelectableBean<ObjectReferenceType>> iModel) {
                return AssignmentsUtil.createActivationTitleModel(getActivation(iModel.getObject().getValue()), getPageBase());
            }
        };
        columns.add(activationColumn);

        return columns;
    }

    private ObjectReferenceType getReferenceWithResolvedName(ObjectReferenceType referenceType) {
        if (referenceType == null) {
            return null;
        }
        if (referenceType.getObject() != null) {
            return referenceType;
        }

        AbstractRoleType resolvedTarget = getResolvedTarget(referenceType);
        if (resolvedTarget == null) {
            return referenceType;
        }
        referenceType.asReferenceValue().setObject(resolvedTarget.asPrismObject());
        return referenceType;
    }

    private List<String> resolvedPaths(SelectableBean<ObjectReferenceType> ref) {
        List<ProvenanceMetadataType> metadataValues = collectProvenanceMetadata(ref.getValue().asReferenceValue());
        if (metadataValues == null) {
            return null;
        }
        List<AssignmentPathType> assignmentPaths = new ArrayList<>();
        for (ProvenanceMetadataType metadataType : metadataValues) {
            assignmentPaths.addAll(metadataType.getAssignmentPath());
        }


        List<String> resolvedPaths = new ArrayList<>();
        for (int i = 0; i < assignmentPaths.size(); i++) {
            AssignmentPathType assignmentPathType = assignmentPaths.get(i);
            List<AssignmentPathSegmentType> segments = assignmentPathType.getSegment();
            if (CollectionUtils.isEmpty(segments) || segments.size() == 1) {
                continue;
            }
            String path = segments.stream()
                    .map(segment -> WebComponentUtil.getEffectiveName(segment.getTargetRef(), AbstractRoleType.F_DISPLAY_NAME, getPageBase(), "resolveName", true))
                    .collect(Collectors.joining(" -> "));
            resolvedPaths.add(path);
        }
        return resolvedPaths;
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

    private <PV extends PrismValue> List<ProvenanceMetadataType> collectProvenanceMetadata(PV rowValue) {
        PrismContainer<ValueMetadataType> valueMetadataContainer = rowValue.getValueMetadataAsContainer();
        if (valueMetadataContainer == null) {
            return null;
        }
        List<ValueMetadataType> valueMetadataValues = (List<ValueMetadataType>) valueMetadataContainer.getRealValues();
        if (valueMetadataValues == null) {
            return null;
        }

        return valueMetadataValues.stream()
                .map(valueMetadata -> valueMetadata.getProvenance())
                .collect(Collectors.toList());

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

/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.user.component;

import java.util.*;
import java.util.stream.Collectors;

import com.evolveum.midpoint.gui.impl.util.AccessMetadataUtil;

import com.evolveum.midpoint.schema.util.ValueMetadataTypeUtil;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.ContainerableListPanel;
import com.evolveum.midpoint.gui.impl.component.data.column.ConfigurableExpressionColumn;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanReferenceDataProvider;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.SearchContext;
import com.evolveum.midpoint.gui.impl.error.ErrorPanel;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.user.UserDetailsModel;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.Channel;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.data.column.AssignmentPathPanel;
import com.evolveum.midpoint.web.component.data.column.ObjectReferenceColumn;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import javax.xml.datatype.XMLGregorianCalendar;

@PanelType(name = "userAllAccesses")
@PanelInstance(identifier = "igaAccesses",
        applicableForOperation = OperationTypeType.MODIFY,
        applicableForType = UserType.class,
        display = @PanelDisplay(label = "AllAccessListPanel.title", icon = GuiStyleConstants.CLASS_CIRCLE_FULL, order = 25))
public class AllAccessListPanel extends AbstractObjectMainPanel<UserType, UserDetailsModel> {

    private static final String ID_ACCESSES = "accesses";

    public AllAccessListPanel(String id, UserDetailsModel model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {
        if (!getPageBase().isNativeRepo()) {
            add(new ErrorPanel(ID_ACCESSES,
                    () -> getString("AllAccessListPanel.nonNativeRepositoryWarning")));
            return;
        }

        var accessesTable = new ContainerableListPanel<ObjectReferenceType, SelectableBean<ObjectReferenceType>>(
                ID_ACCESSES, ObjectReferenceType.class, getPanelConfiguration()) {
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
                return createSearchProvider(getSearchModel());
            }

            @Override
            public List<ObjectReferenceType> getSelectedRealObjects() {
                return null;
            }

            @Override
            protected IColumn<SelectableBean<ObjectReferenceType>, String> createNameColumn(IModel<String> displayModel, GuiObjectColumnType customColumn, ExpressionType expression) {
                return createAccessNameColumn();
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
            protected SearchContext createAdditionalSearchContext() {
                SearchContext ctx = new SearchContext();
                ctx.setDefinitionOverride(getContainerDefinitionForColumns());
                ctx.setAvailableSearchBoxModes(Arrays.asList(SearchBoxModeType.AXIOM_QUERY));
                return ctx;
            }

            @Override
            protected ItemDefinition<?> getContainerDefinitionForColumns() {
                PrismReferenceDefinition refDef = getPrismContext().getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class)
                        .findReferenceDefinition(UserType.F_ROLE_MEMBERSHIP_REF);
                return getPageBase().getModelInteractionService().refDefinitionWithConcreteTargetRefType(refDef, AbstractRoleType.COMPLEX_TYPE);
            }

            @Override
            protected IColumn<SelectableBean<ObjectReferenceType>, String> createCustomExportableColumn(IModel<String> columnDisplayModel, GuiObjectColumnType customColumn, ExpressionType expression) {
                return new ConfigurableExpressionColumn<>(columnDisplayModel, null, customColumn, expression, getPageBase()) {

                    @Override
                    protected void processVariables(VariablesMap variablesMap, ObjectReferenceType rowValue) {
                        super.processVariables(variablesMap, rowValue);
                        variablesMap.put("metadata", AccessMetadataUtil.collectProvenanceMetadata(rowValue.asReferenceValue()), ProvenanceMetadataType.class);
                        variablesMap.put("activation", getActivation(rowValue), ProvenanceMetadataType.class);
                        variablesMap.put("assignment", getAssignment(rowValue), ProvenanceMetadataType.class);
                        variablesMap.put("owner", getObjectDetailsModels().getObjectType(), UserType.class);
                        variablesMap.put("target", getResolvedTarget(rowValue), WebComponentUtil.qnameToClass(rowValue.getType()));
                    }
                };
            }
        };
        accessesTable.setOutputMarkupId(true);
        add(accessesTable);
    }

    private ObjectReferenceColumn<SelectableBean<ObjectReferenceType>> createAccessNameColumn() {
        ObjectReferenceColumn<SelectableBean<ObjectReferenceType>> accessColumn = new ObjectReferenceColumn<>(createStringResource("AllAccessListPanel.accessColumnTitle"), "value") {
            @Override
            public IModel<List<ObjectReferenceType>> extractDataModel(IModel<SelectableBean<ObjectReferenceType>> rowModel) {
                return () -> Collections.singletonList(getReferenceWithResolvedName(rowModel.getObject().getValue()));
            }
        };
        return accessColumn;
    }

    private List<IColumn<SelectableBean<ObjectReferenceType>, String>> createAllAccessesColumns() {
        List<IColumn<SelectableBean<ObjectReferenceType>, String>> columns = new ArrayList<>();


        AbstractExportableColumn<SelectableBean<ObjectReferenceType>, String> source = new AbstractExportableColumn<>(createStringResource("AllAccessListPanel.sourceColumnTitle")) {

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<ObjectReferenceType>>> cellItem, String componentId, IModel<SelectableBean<ObjectReferenceType>> rowModel) {
                AssignmentPathPanel panel = new AssignmentPathPanel(componentId, Model.ofList(AccessMetadataUtil.computeAssignmentPaths(rowModel.getObject().getValue())));
                cellItem.add(panel);

            }

            @Override
            public IModel<List<String>> getDataModel(IModel<SelectableBean<ObjectReferenceType>> rowModel) {

                return () -> resolvedPaths(rowModel.getObject());
            }
        };
        columns.add(source);

        var why = new AbstractExportableColumn<SelectableBean<ObjectReferenceType>, String>(createStringResource("AllAccessListPanel.whyColumnTitle")) {
            @Override
            public IModel<String> getDataModel(IModel<SelectableBean<ObjectReferenceType>> iModel) {
                AssignmentType assignmentType = getAssignment(iModel.getObject().getValue());
                if (assignmentType == null) {
                    return () -> "";
                }
                var metadataContainer = assignmentType.asPrismContainerValue().<ValueMetadataType>getValueMetadataAsContainer();
                if (metadataContainer.isEmpty()) {
                    return () -> "";
                }

                return () -> {
                    var output = new StringBuilder();
                    for (var metadataType : metadataContainer.getRealValues()) {
                        StorageMetadataType storageMetadataType = metadataType.getStorage();
                        if (storageMetadataType == null) {
                            continue;
                        }

                        String chanel = storageMetadataType.getCreateChannel();
                        if (chanel == null) {
                            continue;
                        }
                        String creator = null;
                        String approvers = null;
                        String approverComments = null;

                        Channel channel = Channel.findChannel(chanel);
                        if (channel == null) {
                            continue;
                        }

                        switch (channel) {
                            case SELF_SERVICE:
                            case USER:
                                creator = WebModelServiceUtils.resolveReferenceName(storageMetadataType.getCreatorRef(), getPageBase());
                                approvers = ValueMetadataTypeUtil.getCreateApproverRefs(assignmentType)
                                        .stream()
                                        .filter(ref -> StringUtils.isNotEmpty(ref.getOid()))
                                        .map(approver -> WebModelServiceUtils.resolveReferenceName(approver, getPageBase()))
                                        .collect(Collectors.joining(", "));
                                approverComments = ValueMetadataTypeUtil.getCreateApprovalComments(assignmentType)
                                        .stream()
                                        .filter(comment -> comment != null && !comment.isBlank())
                                        .collect(Collectors.joining(". "));
                                break;
                            case IMPORT:
                            case ASYNC_UPDATE:
                            case DISCOVERY:
                            case LIVE_SYNC:
                            case RECOMPUTATION:
                            case RECONCILIATION:
                                creator = WebModelServiceUtils.resolveReferenceName(storageMetadataType.getCreateTaskRef(), getPageBase());
                        }
                        String whyStatement = "Created by: " + creator;
                        if (approvers != null && !approvers.isBlank()) {
                            whyStatement += "\n Approved by: " + approvers;
                        }
                        if (approverComments != null && !approverComments.isBlank()) {
                            whyStatement += "\n With a comment: " + approverComments;
                        }
                        if (!output.isEmpty()) {
                            output.append(",\n");
                        }
                        output.append(whyStatement);
                    }
                    if (output.isEmpty()) {
                        output.append("N/A");
                    }
                    return output.toString();
                };
            }
        };
        columns.add(why);

        var since = new AbstractExportableColumn<SelectableBean<ObjectReferenceType>, String>(createStringResource("AllAccessListPanel.sinceColumnTitle")) {
            @Override
            public IModel<String> getDataModel(IModel<SelectableBean<ObjectReferenceType>> iModel) {
                List<StorageMetadataType> storageMetadataTypes = AccessMetadataUtil.collectStorageMetadata(iModel.getObject().getValue().asReferenceValue());
                Optional<XMLGregorianCalendar> since = storageMetadataTypes.stream()
                        .map(m -> m.getCreateTimestamp())
                        .findFirst();
                if (since.isPresent()) {
                    String date = WebComponentUtil.formatDate(since.get());
                    return () -> date;
                }
                return () -> "";
            }

        };
        columns.add(since);

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
        List<AssignmentPathMetadataType> assignmentPaths = AccessMetadataUtil.computeAssignmentPaths(ref.getValue());
        List<String> resolvedPaths = new ArrayList<>();
        for (AssignmentPathMetadataType assignmentPathType : assignmentPaths) {
            List<AssignmentPathSegmentMetadataType> segments = assignmentPathType.getSegment();
            if (CollectionUtils.isEmpty(segments) || segments.size() == 1) {
                return Arrays.asList(getString("DirectAndIndirectAssignmentPanel.type.direct"));
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
            ObjectReferenceType targetRef = assignmentType.getTargetRef();
            if (targetRef != null
                    && targetRef.getOid().equals(ref.getOid())
                    && QNameUtil.match(assignmentType.getTargetRef().getType(), ref.getType())) {
                return assignmentType;
            }
        }
        return null;
    }

    private ActivationType getActivation(ObjectReferenceType ref) {
        AssignmentType assignment = getAssignment(ref);
        return assignment != null
                ? assignment.getActivation()
                : new ActivationType().effectiveStatus(ActivationStatusType.ENABLED);
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

    private ISelectableDataProvider<SelectableBean<ObjectReferenceType>> createSearchProvider(IModel<Search<ObjectReferenceType>> searchModel) {
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

/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.component.assignmentType.assignment;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.AssignmentValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn.ColumnType;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismReferenceWrapperColumn;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.PageAssignmentHolderDetails;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.component.AssignmentHolderAssignmentPanel;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.EvaluatedAssignment;
import com.evolveum.midpoint.model.api.context.EvaluatedAssignmentTarget;
import com.evolveum.midpoint.model.api.context.EvaluatedResourceObjectConstruction;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.assignment.AssignmentsUtil;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkColumn;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.gui.impl.component.search.SearchFactory;
import com.evolveum.midpoint.web.component.search.SearchItemDefinition;
import com.evolveum.midpoint.gui.impl.component.search.AbstractSearchItemWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import javax.xml.namespace.QName;
import java.util.*;

/**
 * @author lskublik
 */
@PanelType(name = "indirectAssignments")
@PanelInstance(identifier = "indirectAssignments",
        applicableForType = AssignmentHolderType.class,
        childOf = AssignmentHolderAssignmentPanel.class,
        display = @PanelDisplay(label = "AssignmentTablePanel.menu.showAllAssignments"))
public class DirectAndIndirectAssignmentPanel<AH extends AssignmentHolderType> extends AbstractAssignmentPanel<AH> {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(DirectAndIndirectAssignmentPanel.class);

    private static final String DOT_CLASS = DirectAndIndirectAssignmentPanel.class.getName() + ".";
    private static final String OPERATION_RECOMPUTE_ASSIGNMENTS = DOT_CLASS + "recomputeAssignments";

    private LoadableModel<List<PrismContainerValueWrapper<AssignmentType>>> allAssignmentModel = null;

    public DirectAndIndirectAssignmentPanel(String id, LoadableModel<PrismObjectWrapper<AH>> objectModel, ContainerPanelConfigurationType config) {
        super(id, objectModel, config);
    }

    @Override
    protected IModel<List<PrismContainerValueWrapper<AssignmentType>>> loadValuesModel() {
            if (allAssignmentModel == null) {
                allAssignmentModel = new LoadableModel<>() {

                    @Override
                    protected List<PrismContainerValueWrapper<AssignmentType>> load() {
                        try {
                            return loadEvaluatedAssignments(getContainerModel());
                        } catch (CommonException e) {
                            LOGGER.error("Couldn't load all assignments", e);
                        }
                        return getContainerModel().getObject().getValues();
                    }
                };
            }
            return allAssignmentModel;
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<AssignmentType>, String>> createDefaultColumns() {
        List<IColumn<PrismContainerValueWrapper<AssignmentType>, String>> columns = new ArrayList<>();


        columns.add(new AbstractColumn<>(createStringResource("DirectAndIndirectAssignmentPanel.column.type")) {
            @Override
            public void populateItem(Item<ICellPopulator<PrismContainerValueWrapper<AssignmentType>>> cellItem,
                    String componentId, final IModel<PrismContainerValueWrapper<AssignmentType>> rowModel) {
                AssignmentValueWrapper object = (AssignmentValueWrapper) rowModel.getObject();
                cellItem.add(new Label(componentId, (IModel<String>) () -> object.isDirectAssignment() ?
                        createStringResource("DirectAndIndirectAssignmentPanel.type.direct").getString() :
                        createStringResource("DirectAndIndirectAssignmentPanel.type.indirect").getString()));
                ObjectType assignmentParent = object.getAssignmentParent();
                if (assignmentParent != null) {
                    cellItem.add(AttributeModifier.replace("title",
                            createStringResource("DirectAndIndirectAssignmentPanel.tooltip.indirect.parent", assignmentParent.getName()).getString()));
                }
            }

            @Override
            public String getCssClass() {
                return "mp-w-md-1";
            }
        });
        columns.add(new PrismPropertyWrapperColumn<AssignmentType, String>(getContainerModel(), AssignmentType.F_DESCRIPTION, ColumnType.STRING, getPageBase()){
            @Override
            protected boolean isHelpTextVisible(boolean helpTextVisible) {
                return false;
            }
        });
        columns.add(new PrismReferenceWrapperColumn<AssignmentType, ObjectReferenceType>(getContainerModel(), AssignmentType.F_TENANT_REF, ColumnType.STRING, getPageBase()));
        columns.add(new PrismReferenceWrapperColumn<AssignmentType, ObjectReferenceType>(getContainerModel(), AssignmentType.F_ORG_REF, ColumnType.STRING, getPageBase()));
        columns.add(new PrismPropertyWrapperColumn<AssignmentType, String>(getContainerModel(), ItemPath.create(AssignmentType.F_CONSTRUCTION, ConstructionType.F_KIND), ColumnType.STRING, getPageBase()));
        columns.add(new PrismPropertyWrapperColumn<AssignmentType, String>(getContainerModel(), ItemPath.create(AssignmentType.F_CONSTRUCTION, ConstructionType.F_INTENT), ColumnType.STRING, getPageBase()));

        columns.add(new AbstractColumn<>(
                createStringResource("AbstractRoleAssignmentPanel.relationLabel")) {
            @Override
            public void populateItem(Item<ICellPopulator<PrismContainerValueWrapper<AssignmentType>>> item, String componentId, IModel<PrismContainerValueWrapper<AssignmentType>> assignmentModel) {
                item.add(new Label(componentId, WebComponentUtil.getRelationLabelValue(assignmentModel.getObject(), getPageBase())));
            }

            @Override
            public String getCssClass() {
                return "mp-w-md-1";
            }
        });
        return columns;
    }

    @Override
    protected IColumn<PrismContainerValueWrapper<AssignmentType>, String> createCheckboxColumn() {
        return null;
    }

    @Override
    protected IColumn<PrismContainerValueWrapper<AssignmentType>, String> createIconColumn() {
        return new IconColumn<>(Model.of("")) {

            private static final long serialVersionUID = 1L;

            @Override
            protected DisplayType getIconDisplayType(IModel<PrismContainerValueWrapper<AssignmentType>> rowModel) {
                AssignmentType assignment = rowModel.getObject().getRealValue();
                if (assignment != null && assignment.getTargetRef() != null && StringUtils.isNotEmpty(assignment.getTargetRef().getOid())) {
                    List<ObjectType> targetObjectList = WebComponentUtil.loadReferencedObjectList(Collections.singletonList(assignment.getTargetRef()), OPERATION_LOAD_ASSIGNMENTS_TARGET_OBJ,
                            DirectAndIndirectAssignmentPanel.this.getPageBase());
                    if (CollectionUtils.isNotEmpty(targetObjectList) && targetObjectList.size() == 1) {
                        ObjectType targetObject = targetObjectList.get(0);
                        DisplayType displayType = GuiDisplayTypeUtil.getArchetypePolicyDisplayType(targetObject.asPrismObject(), DirectAndIndirectAssignmentPanel.this.getPageBase());
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
                return GuiDisplayTypeUtil.createDisplayType(WebComponentUtil.createDefaultBlackIcon(
                        AssignmentsUtil.getTargetType(rowModel.getObject().getRealValue())));
            }

        };
    }

    @Override
    protected IColumn<PrismContainerValueWrapper<AssignmentType>, String> createNameColumn(IModel<String> displayModel, GuiObjectColumnType customColumn, ItemPath itemPath, ExpressionType expression) {
        return new AjaxLinkColumn<>(createStringResource("DirectAndIndirectAssignmentPanel.column.name")) {
            private static final long serialVersionUID = 1L;

            @Override
            protected IModel<String> createLinkModel(IModel<PrismContainerValueWrapper<AssignmentType>> rowModel) {
                String name = AssignmentsUtil.getName(rowModel.getObject(), getPageBase());
                if (StringUtils.isEmpty(name)) {
                    ObjectReferenceType ref;
                    if (rowModel.getObject().getRealValue().getConstruction() != null) {
                        ref = rowModel.getObject().getRealValue().getConstruction().getResourceRef();
                    } else {
                        ref = rowModel.getObject().getRealValue().getTargetRef();
                    }
                    name = WebComponentUtil.getDisplayNameOrName(ref);
                }
                return Model.of(name);
            }

            @Override
            public void onClick(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<AssignmentType>> rowModel) {
                ObjectReferenceType ref;
                if (rowModel.getObject().getRealValue().getConstruction() != null) {
                    ref = rowModel.getObject().getRealValue().getConstruction().getResourceRef();
                } else {
                    ref = rowModel.getObject().getRealValue().getTargetRef();
                }
                if (ref != null) {
                    try {
                        WebComponentUtil.dispatchToObjectDetailsPage(ref, getPageBase(), true);
                    } catch (Exception e) {
                        getPageBase().error("Cannot determine details page for " + ref);
                        target.add(getPageBase().getFeedbackPanel());
                    }
                }
            }
        };
    }

    @Override
    protected List<InlineMenuItem> createInlineMenu() {
        return new ArrayList<>();
    }

    @Override
    protected List<SearchItemDefinition> createSearchableItems(PrismContainerDefinition<AssignmentType> containerDef) {
        List<SearchItemDefinition> defs = new ArrayList<>();
        SearchFactory.addSearchRefDef(containerDef, AssignmentType.F_TARGET_REF, defs, AreaCategoryType.ADMINISTRATION, getPageBase());
        return defs;
    }

    @Override
    protected List<? super AbstractSearchItemWrapper> createSearchableItemWrappers(PrismContainerDefinition<AssignmentType> containerDef) {
        List<? super AbstractSearchItemWrapper> defs = new ArrayList<>();
//        SearchFactory.addSearchRefWrapper(containerDef, AssignmentType.F_TARGET_REF, defs, AreaCategoryType.ADMINISTRATION, getPageBase());
        return defs;
    }

    @Override
    public void refreshTable(AjaxRequestTarget ajaxRequestTarget) {
        if (allAssignmentModel != null) {
            allAssignmentModel.reset();
        }
        super.refreshTable(ajaxRequestTarget);

    }

    private ObjectDelta<AH> getObjectDelta(OperationResult result) throws SchemaException {
        ObjectDetailsModels<AH> model = ((PageAssignmentHolderDetails) getPageBase()).getObjectDetailsModels();
        model.collectDeltas(result);
        return model.getDelta();
    }

    private ModelExecuteOptions createPreviewAssignmentsOptions() {
        ModelExecuteOptions options = getPageBase()
                .executeOptions()
                .evaluateAllAssignmentRelationsOnRecompute();
        options.getOrCreatePartialProcessing().outbound(PartialProcessingTypeType.SKIP);
        return options;
    }

    private List<PrismContainerValueWrapper<AssignmentType>> loadEvaluatedAssignments(IModel<PrismContainerWrapper<AssignmentType>> parent)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException, ConfigurationException, ObjectNotFoundException, PolicyViolationException, ObjectAlreadyExistsException {
        if (!(getPageBase() instanceof PageAssignmentHolderDetails)) {
            return parent.getObject().getValues();
        }
        Task task = getPageBase().createSimpleTask(OPERATION_RECOMPUTE_ASSIGNMENTS);
        OperationResult result = new OperationResult(OPERATION_RECOMPUTE_ASSIGNMENTS);
        Set<AssignmentValueWrapper> assignmentValueWrapperSet = new LinkedHashSet<>();

        ObjectDelta<AH> delta = getObjectDelta(result);
        ModelContext<AH> modelContext = getPageBase().getModelInteractionService().previewChanges(
                Collections.singleton(delta), createPreviewAssignmentsOptions(), task, result);
        Collection<? extends EvaluatedAssignment<?>> evaluatedAssignments = modelContext.getNonNegativeEvaluatedAssignments();

            for (EvaluatedAssignment<?> evaluatedAssignment : evaluatedAssignments) {
                if (!evaluatedAssignment.isValid()) {
                    continue;
                }

                collectRoleAndOrgs(evaluatedAssignment, parent, assignmentValueWrapperSet);
                collectResources(evaluatedAssignment, parent, assignmentValueWrapperSet, task, result);
            }

            return new ArrayList<>(assignmentValueWrapperSet);

    }

    private void collectRoleAndOrgs(EvaluatedAssignment<?> evaluatedAssignment, IModel<PrismContainerWrapper<AssignmentType>> parent, Set<AssignmentValueWrapper> assignmentValueWrapperSet) throws SchemaException {
        DeltaSetTriple<? extends EvaluatedAssignmentTarget> targetsTriple = evaluatedAssignment.getRoles();
        Collection<? extends EvaluatedAssignmentTarget> targets = targetsTriple.getNonNegativeValues();
        for (EvaluatedAssignmentTarget target : targets) {
            target.getTarget();
            if (ArchetypeType.class.equals(target.getTarget().getCompileTimeClass())) {
                continue;
            }

            if (target.appliesToFocusWithAnyRelation(getPageBase().getRelationRegistry())) {
                AssignmentType assignmentType = target.getAssignment().clone();
                assignmentType.setDescription(target.getTarget().asObjectable().getDescription());
                assignmentType.getTargetRef().setOid(target.getTarget().getOid());
                assignmentType.getTargetRef().setTargetName(new PolyStringType(target.getTarget().getName()));
                assignmentType.getTargetRef().setType(target.getTarget().getComplexTypeDefinition().getTypeName());
                ValueStatus status = evaluatedAssignment.getAssignment(true) == null ? ValueStatus.ADDED : ValueStatus.NOT_CHANGED;
                AssignmentValueWrapper assignmentValueWrapper = WebPrismUtil.createNewValueWrapper(parent.getObject(),
                        assignmentType.asPrismContainerValue(), status, getPageBase());
                assignmentValueWrapper.setDirectAssignment(target.isDirectlyAssigned());
                assignmentValueWrapper.setAssignmentParent(target.getAssignmentPath());
                assignmentValueWrapperSet.add(assignmentValueWrapper);
            }
        }
    }

    private void collectResources(EvaluatedAssignment<?> evaluatedAssignment, IModel<PrismContainerWrapper<AssignmentType>> parent, Set<AssignmentValueWrapper> assignmentValueWrapperSet, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException {
        DeltaSetTriple<EvaluatedResourceObjectConstruction> evaluatedConstructionsTriple = evaluatedAssignment
                .getEvaluatedConstructions(task, result);
        Collection<EvaluatedResourceObjectConstruction> evaluatedConstructions = evaluatedConstructionsTriple
                .getNonNegativeValues();
        for (EvaluatedResourceObjectConstruction construction : evaluatedConstructions) {
            if (!construction.isWeak()) {
                PrismContainerDefinition<AssignmentType> assignmentDef = getPrismContext().getSchemaRegistry()
                        .findContainerDefinitionByCompileTimeClass(AssignmentType.class);
                AssignmentType assignmentType = assignmentDef.instantiate().createNewValue().asContainerable();
                ObjectReferenceType targetRef = new ObjectReferenceType();
                targetRef.setOid(construction.getResource().getOid());
                targetRef.setType(ResourceType.COMPLEX_TYPE);
                targetRef.setTargetName(new PolyStringType(construction.getResource().getName()));
                assignmentType.setTargetRef(targetRef);
                ConstructionType constructionType = new ConstructionType();
                constructionType.setResourceRef(targetRef);
                constructionType.setKind(construction.getKind());
                constructionType.setIntent(construction.getIntent());
                assignmentType.setConstruction(constructionType);
                assignmentType.setDescription(construction.getResource().asObjectable().getDescription());
                ValueStatus status = evaluatedAssignment.getAssignment(true) == null ? ValueStatus.ADDED : ValueStatus.NOT_CHANGED;
                AssignmentValueWrapper assignmentValueWrapper = WebPrismUtil.createNewValueWrapper(parent.getObject(),
                        assignmentType.asPrismContainerValue(), status, getPageBase());
                assignmentValueWrapper.setDirectAssignment(construction.isDirectlyAssigned());
                assignmentValueWrapper.setAssignmentParent(construction.getAssignmentPath());
                assignmentValueWrapperSet.add(assignmentValueWrapper);
            }
        }
    }

    @Override
    protected QName getAssignmentType() {
        return null;
    }
}

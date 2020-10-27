/*
 * Copyright (C) 2018-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.assignment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.evolveum.midpoint.web.session.SessionStorage;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn.ColumnType;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismContainerWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.form.multivalue.MultiValueChoosePanel;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.page.admin.PageAdminObjectDetails;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.midpoint.web.util.validation.MidpointFormValidator;
import com.evolveum.midpoint.web.util.validation.MidpointFormValidatorImpl;
import com.evolveum.midpoint.web.util.validation.SimpleValidationError;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 * Created by honchar.
 */
public class InducedEntitlementsPanel extends InducementsPanel {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(InducedEntitlementsPanel.class);

    private static final String DOT_CLASS = InducedEntitlementsPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_SHADOW_OBJECT = DOT_CLASS + "loadReferencedShadowObject";
    private static final String OPERATION_LOAD_RESOURCE_OBJECT = DOT_CLASS + "loadResourceObject";

    private MidpointFormValidator validator;

    public InducedEntitlementsPanel(String id, IModel<PrismContainerWrapper<AssignmentType>> inducementContainerWrapperModel) {
        super(id, inducementContainerWrapperModel);

        createValidator();
    }

    private void createValidator() {
        validator = new MidpointFormValidatorImpl() {

            private static final long serialVersionUID = 1L;

            @Override
            public Collection<SimpleValidationError> validateObject(PrismObject<? extends ObjectType> object, Collection<ObjectDelta<? extends ObjectType>> deltas) {
                List<SimpleValidationError> errors = new ArrayList<>();
                for (ObjectDelta<?> delta : deltas) {
                    if (AbstractRoleType.class.isAssignableFrom(delta.getObjectTypeClass())) {
                        switch (delta.getChangeType()) {
                            case MODIFY:
                                Collection<? extends ItemDelta<?, ?>> itemDeltas = delta.getModifications();
                                for (ItemDelta<?, ?> itemDelta : itemDeltas) {
                                    if (itemDelta.getPath().equivalent(AbstractRoleType.F_INDUCEMENT) && itemDelta.getValuesToAdd() != null) {
                                        for (PrismValue value : itemDelta.getValuesToAdd()) {
                                            errors.addAll(validateInducement(value.getRealValue()));
                                        }
                                    }
                                }
                                break;
                            case ADD:
                                if (delta != null && delta.getObjectToAdd().asObjectable() != null) {
                                    for (AssignmentType assignment : ((AbstractRoleType) object.asObjectable()).getInducement()) {
                                        errors.addAll(validateInducement(assignment));
                                    }
                                }
                                break;
                        }
                    }
                }
                return errors;
            }

            private Collection<SimpleValidationError> validateInducement(AssignmentType assignment) {
                List<SimpleValidationError> errors = new ArrayList<>();
                //TODO impelemnt findContainer(ItemPath)
                com.evolveum.midpoint.prism.Item<PrismContainerValue<ResourceObjectAssociationType>, PrismContainerDefinition<ResourceObjectAssociationType>> association =
                        assignment.asPrismContainerValue().findItem(ItemPath.create(AssignmentType.F_CONSTRUCTION, ConstructionType.F_ASSOCIATION));
                if (association != null && !association.getValues().isEmpty()) {
                    for (PrismContainerValue<ResourceObjectAssociationType> associationValue : association.getValues()) {
                        PrismContainer<MappingType> outbound = associationValue.findContainer(ResourceObjectAssociationType.F_OUTBOUND);
                        if (outbound == null || outbound.getValues().isEmpty()) {
                            SimpleValidationError error = new SimpleValidationError();
                            error.setMessage(getPageBase().createStringResource("InducedEntitlementsPanel.validator.message").getString());
                            ItemPathType path = new ItemPathType();
                            path.setItemPath(ItemPath.create(AbstractRoleType.F_INDUCEMENT, AssignmentType.F_CONSTRUCTION, ConstructionType.F_ASSOCIATION, ResourceObjectAssociationType.F_OUTBOUND));
                            error.setAttribute(path);
                            errors.add(error);
                        }
                    }
                }
                return errors;
            }

            @Override
            public Collection<SimpleValidationError> validateAssignment(AssignmentType assignment) {
                return new ArrayList<>();
            }
        };
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        if (getPageBase() instanceof PageAdminObjectDetails) {
            PageAdminObjectDetails page = (PageAdminObjectDetails) getPageBase();
            if (!page.getFormValidatorRegistry().getValidators().contains(validator)) {
                page.getFormValidatorRegistry().registerValidator(validator);
            }
        }
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<AssignmentType>, String>> initColumns() {
        List<IColumn<PrismContainerValueWrapper<AssignmentType>, String>> columns = new ArrayList<>();

        columns.add(new PrismPropertyWrapperColumn<AssignmentType, String>(getModel(), ItemPath.create(AssignmentType.F_CONSTRUCTION, ConstructionType.F_KIND), ColumnType.STRING, getPageBase()));

        columns.add(new PrismPropertyWrapperColumn<AssignmentType, String>(getModel(), ItemPath.create(AssignmentType.F_CONSTRUCTION, ConstructionType.F_INTENT), ColumnType.STRING, getPageBase()));

        columns.add(new PrismContainerWrapperColumn<>(getModel(), ItemPath.create(AssignmentType.F_CONSTRUCTION, ConstructionType.F_ASSOCIATION), getPageBase()));

        columns.add(new AbstractColumn<PrismContainerValueWrapper<AssignmentType>, String>(createStringResource("InducedEntitlements.value")) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(Item<ICellPopulator<PrismContainerValueWrapper<AssignmentType>>> item, String componentId,
                    final IModel<PrismContainerValueWrapper<AssignmentType>> rowModel) {

                ExpressionType expressionType = getExpressionFromRowModel(rowModel, false);
                List<ShadowType> shadowsList = WebComponentUtil.loadReferencedObjectList(ExpressionUtil.getShadowRefValue(
                        expressionType,
                        InducedEntitlementsPanel.this.getPageBase().getPrismContext()),
                        OPERATION_LOAD_SHADOW_OBJECT, InducedEntitlementsPanel.this.getPageBase());

                MultiValueChoosePanel<ShadowType> valuesPanel = new MultiValueChoosePanel<ShadowType>(componentId,
                        Model.ofList(shadowsList), Collections.singletonList(ShadowType.class), false) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected ObjectFilter getCustomFilter() {
                        ConstructionType construction = rowModel.getObject().getRealValue().getConstruction();
                        return WebComponentUtil.getShadowTypeFilterForAssociation(construction, OPERATION_LOAD_RESOURCE_OBJECT,
                                InducedEntitlementsPanel.this.getPageBase());
                    }

                    @Override
                    protected void removePerformedHook(AjaxRequestTarget target, ShadowType shadow) {
                        if (shadow != null && StringUtils.isNotEmpty(shadow.getOid())) {
                            ExpressionType expression = WebComponentUtil.getAssociationExpression(rowModel.getObject(), getPageBase());
                            ExpressionUtil.removeShadowRefEvaluatorValue(expression, shadow.getOid(), getPageBase().getPrismContext());
                        }
                    }

                    @Override
                    protected void choosePerformedHook(AjaxRequestTarget target, List<ShadowType> selectedList) {
                        ShadowType shadow = selectedList != null && selectedList.size() > 0 ? selectedList.get(0) : null;
                        if (shadow != null && StringUtils.isNotEmpty(shadow.getOid())) {
                            ExpressionType expression = getExpressionFromRowModel(rowModel, true);
                            ExpressionUtil.addShadowRefEvaluatorValue(expression, shadow.getOid(),
                                    InducedEntitlementsPanel.this.getPageBase().getPrismContext());
                        }
                    }

                    @Override
                    protected void selectPerformed(AjaxRequestTarget target, List<ShadowType> chosenValues) {
                        addPerformed(target, chosenValues);
                    }

                };
                valuesPanel.setOutputMarkupId(true);
                item.add(valuesPanel);

            }
        });

        return columns;
    }

    @Override
    protected ObjectQuery createObjectQuery() {
        return getParentPage().getPrismContext().queryFor(AssignmentType.class)
                .exists(AssignmentType.F_CONSTRUCTION)
                .build();
    }

    @Override
    protected Panel getBasicContainerPanel(String idPanel, IModel<PrismContainerValueWrapper<AssignmentType>> model) {
        return getConstructionAssociationPanel(idPanel, model);
    }

    private ConstructionAssociationPanel getConstructionAssociationPanel(String idPanel, IModel<PrismContainerValueWrapper<AssignmentType>> model) {
        IModel<PrismContainerWrapper<ConstructionType>> constructionModel = PrismContainerWrapperModel.fromContainerValueWrapper(model, AssignmentType.F_CONSTRUCTION);
        ConstructionAssociationPanel constructionDetailsPanel = new ConstructionAssociationPanel(idPanel, constructionModel);
        constructionDetailsPanel.setOutputMarkupId(true);
        return constructionDetailsPanel;
    }

    protected List<ObjectTypes> getObjectTypesList() {
        return Collections.singletonList(ObjectTypes.RESOURCE);
    }

    @Override
    protected boolean isEntitlementAssignment() {
        return true;
    }

    @Override
    protected List<PrismContainerValueWrapper<AssignmentType>> customPostSearch(List<PrismContainerValueWrapper<AssignmentType>> assignments) {
        List<PrismContainerValueWrapper<AssignmentType>> filteredAssignments = new ArrayList<>();
        if (assignments == null) {
            return filteredAssignments;
        }
        assignments.forEach(assignmentWrapper -> {
            AssignmentType assignment = assignmentWrapper.getRealValue();
            if (assignment.getConstruction() != null && assignment.getConstruction().getAssociation() != null) {
                List<ResourceObjectAssociationType> associations = assignment.getConstruction().getAssociation();
                if (associations.size() == 0 && ValueStatus.ADDED.equals(assignmentWrapper.getStatus())) {
                    filteredAssignments.add(assignmentWrapper);
                    return;
                }
                associations.forEach(association -> {
                    if (!filteredAssignments.contains(assignmentWrapper)) {
                        if (association.getRef() != null && association.getRef().getItemPath() != null &&
                                !association.getRef().getItemPath().isEmpty()) {
                            filteredAssignments.add(assignmentWrapper);
                        }
                    }
                });
            }
        });
        return filteredAssignments;
    }

    private ExpressionType getExpressionFromRowModel(IModel<PrismContainerValueWrapper<AssignmentType>> rowModel, boolean createIfNotExist) {
        PrismContainerValueWrapper<AssignmentType> assignment = rowModel.getObject();
        try {
            PrismContainerWrapper<ResourceObjectAssociationType> associationWrapper = assignment.findContainer(ItemPath.create(AssignmentType.F_CONSTRUCTION, ConstructionType.F_ASSOCIATION));
            List<PrismContainerValue<ResourceObjectAssociationType>> associationValueList = associationWrapper.getItem().getValues();
            PrismContainerValue<ResourceObjectAssociationType> associationValue;
            if (CollectionUtils.isEmpty(associationValueList)) {
                if (createIfNotExist) {
                    associationValue = associationWrapper.createValue();
                } else {
                    return null;
                }
            } else {
                associationValue = associationValueList.get(0);
            }

            ResourceObjectAssociationType association = associationValue.getRealValue();
            MappingType outbound = association.getOutbound();
            if (outbound == null) {
                if (createIfNotExist) {
                    outbound = association.beginOutbound();
                } else {
                    return null;
                }
            }
            ExpressionType expressionType = outbound.getExpression();
            if (expressionType == null && createIfNotExist) {
                expressionType = outbound.beginExpression();
            }
            return expressionType;
        } catch (SchemaException ex) {
            LOGGER.error("Unable to find association container in the construction: {}", ex.getLocalizedMessage());
        }
        return null;
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.INDUCED_ENTITLEMENTS_TAB_TABLE;
    }

    @Override
    protected String getAssignmentsTabStorageKey() {
        return SessionStorage.KEY_INDUCED_ENTITLEMENTS_TAB;
    }
}

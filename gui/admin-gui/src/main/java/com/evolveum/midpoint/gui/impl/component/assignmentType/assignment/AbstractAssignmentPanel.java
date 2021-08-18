/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.assignmentType.assignment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.impl.component.assignmentType.AbstractAssignmentTypePanel;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.util.logging.Trace;

import com.evolveum.midpoint.util.logging.TraceManager;

import com.evolveum.midpoint.web.component.search.SearchFactory;
import com.evolveum.midpoint.web.component.search.SearchItemDefinition;
import com.evolveum.midpoint.web.session.SessionStorage;

import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.AssignmentPopupDto;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.AssignmentCandidatesSpecification;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;

public abstract class AbstractAssignmentPanel<AH extends AssignmentHolderType> extends AbstractAssignmentTypePanel {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractAssignmentPanel.class);

    public AbstractAssignmentPanel(String id, IModel<PrismObjectWrapper<AH>> model, ContainerPanelConfigurationType config) {
        super(id, PrismContainerWrapperModel.fromContainerWrapper(model, AssignmentHolderType.F_ASSIGNMENT), config);
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<AssignmentType>, String>> initColumns() {
        return null;
    }

    protected IModel<AssignmentPopupDto> createAssignmentPopupModel() {
        return new LoadableModel<>(false) {

            @Override
            protected AssignmentPopupDto load() {
                List<AssignmentObjectRelation> assignmentObjectRelations = getAssignmentObjectRelationList();
                return new AssignmentPopupDto(assignmentObjectRelations);
            }
        };
    }

    private List<AssignmentObjectRelation> getAssignmentObjectRelationList() {
        if (AbstractAssignmentPanel.this.getModelObject() == null) {
            return null;
        }

        List<AssignmentObjectRelation> assignmentRelationsList =
                WebComponentUtil.divideAssignmentRelationsByAllValues(loadAssignmentTargetRelationsList());
        if (assignmentRelationsList == null || assignmentRelationsList.isEmpty()) {
            return assignmentRelationsList;
        }
        QName assignmentType = getAssignmentType();
        if (assignmentType == null) {
            return assignmentRelationsList;
        }
        List<AssignmentObjectRelation> assignmentRelationsListFilteredByType =
                new ArrayList<>();
        assignmentRelationsList.forEach(assignmentRelation -> {
            QName objectType = assignmentRelation.getObjectTypes() != null
                    && !assignmentRelation.getObjectTypes().isEmpty()
                    ? assignmentRelation.getObjectTypes().get(0) : null;
            if (QNameUtil.match(assignmentType, objectType)) {
                assignmentRelationsListFilteredByType.add(assignmentRelation);
            }
        });
        return assignmentRelationsListFilteredByType;
    }

    @Override
    protected String getAssignmentsTabStorageKey() {
        return SessionStorage.KEY_ASSIGNMENTS_TAB;
    }

    @NotNull
    private <AH extends AssignmentHolderType> List<AssignmentObjectRelation> loadAssignmentTargetRelationsList() {
        OperationResult result = new OperationResult(OPERATION_LOAD_ASSIGNMENT_TARGET_RELATIONS);
        List<AssignmentObjectRelation> assignmentTargetRelations = new ArrayList<>();
        PrismObject<AH> obj = getFocusObject();
        try {
            AssignmentCandidatesSpecification spec = getPageBase().getModelInteractionService()
                    .determineAssignmentTargetSpecification(obj, result);
            assignmentTargetRelations = spec != null ? spec.getAssignmentObjectRelations() : new ArrayList<>();
        } catch (SchemaException | ConfigurationException ex) {
            result.recordPartialError(ex.getLocalizedMessage());
            LOGGER.error("Couldn't load assignment target specification for the object {} , {}", obj.getName(), ex.getLocalizedMessage());
        }
        return assignmentTargetRelations;
    }

    @Override
    protected List<ObjectTypes> getObjectTypesList() {
        return Collections.singletonList(ObjectTypes.getObjectTypeFromTypeQName(getAssignmentType()));
    }

    protected RefFilter getTargetTypeFilter() {
        QName targetType = getAssignmentType();
        RefFilter targetRefFilter = null;
        if (targetType != null) {
            ObjectReferenceType ort = new ObjectReferenceType();
            ort.setType(targetType);
            ort.setRelation(new QName(PrismConstants.NS_QUERY, "any"));
            targetRefFilter = (RefFilter) getPageBase().getPrismContext().queryFor(AssignmentType.class)
                    .item(AssignmentType.F_TARGET_REF)
                    .ref(ort.asReferenceValue())
                    .buildFilter();
            targetRefFilter.setOidNullAsAny(true);
        }
        return targetRefFilter;
    }

    @Override
    protected void addSpecificSearchableItems(PrismContainerDefinition<AssignmentType> containerDef, List<SearchItemDefinition> defs) {
        if (getAssignmentType() == null) {
            SearchFactory.addSearchRefDef(containerDef, ItemPath.create(AssignmentType.F_TARGET_REF), defs, AreaCategoryType.ADMINISTRATION, getPageBase());
            SearchFactory.addSearchRefDef(containerDef, ItemPath.create(AssignmentType.F_CONSTRUCTION, ConstructionType.F_RESOURCE_REF), defs, AreaCategoryType.ADMINISTRATION, getPageBase());
            SearchFactory.addSearchPropertyDef(containerDef, ItemPath.create(AssignmentType.F_POLICY_RULE, PolicyRuleType.F_NAME), defs, "AssignmentPanel.search.policyRule.name");
            SearchFactory.addSearchRefDef(containerDef,
                    ItemPath.create(AssignmentType.F_POLICY_RULE, PolicyRuleType.F_POLICY_CONSTRAINTS,
                            PolicyConstraintsType.F_EXCLUSION, ExclusionPolicyConstraintType.F_TARGET_REF), defs, AreaCategoryType.POLICY, getPageBase());
        }
    }

    protected abstract QName getAssignmentType();

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.ASSIGNMENTS_TAB_TABLE;
    }

    @Override
    protected ObjectQuery getCustomizeQuery() {
        Collection<QName> delegationRelations = getPageBase().getRelationRegistry()
                .getAllRelationsFor(RelationKindType.DELEGATION);

        //do not show archetype assignments
        ObjectReferenceType archetypeRef = new ObjectReferenceType();
        archetypeRef.setType(ArchetypeType.COMPLEX_TYPE);
        archetypeRef.setRelation(new QName(PrismConstants.NS_QUERY, "any"));
        RefFilter archetypeFilter = (RefFilter) getPageBase().getPrismContext().queryFor(AssignmentType.class)
                .item(AssignmentType.F_TARGET_REF)
                .ref(archetypeRef.asReferenceValue())
                .buildFilter();
        archetypeFilter.setOidNullAsAny(true);

        ObjectFilter relationFilter = getPageBase().getPrismContext().queryFor(AssignmentType.class)
                .not()
                .item(AssignmentType.F_TARGET_REF)
                .refRelation(delegationRelations.toArray(new QName[0]))
                .buildFilter();

        ObjectQuery query = getPrismContext().queryFactory().createQuery(relationFilter);
        query.addFilter(getPrismContext().queryFactory().createNot(archetypeFilter));

        RefFilter targetRefFilter = getTargetTypeFilter();
        if (targetRefFilter != null) {
            query.addFilter(targetRefFilter);
        }
        return query;
    }
}

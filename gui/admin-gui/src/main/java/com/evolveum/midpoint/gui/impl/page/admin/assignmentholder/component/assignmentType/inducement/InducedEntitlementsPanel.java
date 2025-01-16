/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.component.assignmentType.inducement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.namespace.QName;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractPageObjectDetails;
import com.evolveum.midpoint.gui.impl.page.admin.abstractrole.component.AbstractRoleInducementPanel;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.web.session.SessionStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.validation.MidpointFormValidator;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import java.util.Collection;

@PanelType(name = "inducedEntitlements")
public class InducedEntitlementsPanel<AR extends AbstractRoleType> extends AbstractInducementPanel<AR> {

    private static final Trace LOGGER = TraceManager.getTrace(InducedEntitlementsPanel.class);

    private MidpointFormValidator validator;

    public InducedEntitlementsPanel(String id, IModel<PrismObjectWrapper<AR>> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    private void createValidator() {
        validator = new InducedEntitlementsValidator();
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        if (validator == null) {
            createValidator();
        }

        if (getPageBase() instanceof AbstractPageObjectDetails) {
            AbstractPageObjectDetails page = (AbstractPageObjectDetails) getPageBase();
            if (validatorNotPresentInRegistry(page)) {
                page.getFormValidatorRegistry().registerValidator(validator);
            }
        }
    }

    private boolean validatorNotPresentInRegistry(AbstractPageObjectDetails page) {
        Collection<MidpointFormValidator> validators = page.getFormValidatorRegistry().getValidators();
        return validators != null && validators.stream().noneMatch(v -> v instanceof InducedEntitlementsValidator);
    }

    @Override
    protected String getNameOfAssignment(PrismContainerValueWrapper<AssignmentType> wrapper) {
        return getNameResourceOfConstruction(wrapper);
    }

    @Override
    protected List<IColumn<PrismContainerValueWrapper<AssignmentType>, String>> initColumns() {
        return ColumnUtils.createInducementConstructionColumns(getContainerModel(), getPageBase());
    }

    @Override
    protected ObjectQuery getCustomizeQuery() {
        return getPageBase().getPrismContext().queryFor(AssignmentType.class)
                .exists(AssignmentType.F_CONSTRUCTION)
                .build();
    }

    @Override
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
    protected String getStorageKey() {
        return SessionStorage.KEY_INDUCED_ENTITLEMENTS_TAB;
    }

    @Override
    protected QName getAssignmentType() {
        return ResourceType.COMPLEX_TYPE;
    }
}

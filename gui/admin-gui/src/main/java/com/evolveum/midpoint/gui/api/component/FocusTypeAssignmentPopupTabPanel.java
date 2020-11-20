/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Fragment;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.input.RelationDropDownChoicePanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AreaCategoryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

/**
 * Created by honchar
 */
public class FocusTypeAssignmentPopupTabPanel<F extends FocusType> extends AbstractAssignmentPopupTabPanel<F> {

    private static final long serialVersionUID = 1L;

    private static final String ID_RELATION_CONTAINER = "relationContainer";
    private static final String ID_RELATION = "relation";

    private static final String DOT_CLASS = FocusTypeAssignmentPopupTabPanel.class.getName();
    private static final Trace LOGGER = TraceManager.getTrace(FocusTypeAssignmentPopupTabPanel.class);
    private static final String OPERATION_LOAD_ASSIGNABLE_ROLES = DOT_CLASS + "loadAssignableRoles";

    public FocusTypeAssignmentPopupTabPanel(String id, ObjectTypes type) {
        super(id, type);
    }

    @Override
    protected void initParametersPanel(Fragment parametersPanel) {
        WebMarkupContainer relationContainer = new WebMarkupContainer(ID_RELATION_CONTAINER);
        relationContainer.setOutputMarkupId(true);
        parametersPanel.add(relationContainer);

        List<QName> relationsList = getPredefinedRelation() != null
                ? Collections.singletonList(getPredefinedRelation())
                : getSupportedRelations();
        relationContainer.add(new RelationDropDownChoicePanel(ID_RELATION, getDefaultRelationIfInList(relationsList),
                relationsList, false));
    }

    protected List<QName> getSupportedRelations() {
        return WebComponentUtil.getCategoryRelationChoices(AreaCategoryType.ADMINISTRATION, getPageBase());
    }

    protected QName getPredefinedRelation() {
        return null;
    }

    private QName getDefaultRelationIfInList(List<QName> relationsList) {
        if (CollectionUtils.isNotEmpty(relationsList)) {
            for (QName relation : relationsList) {
                if (QNameUtil.match(relation, SchemaConstants.ORG_DEFAULT)) {
                    return SchemaConstants.ORG_DEFAULT;
                }
            }
        }
        return null;
    }

    @Override
    protected Map<String, AssignmentType> getSelectedAssignmentsMap() {
        Map<String, AssignmentType> assignmentsMap = new HashMap<>();

        List<F> selectedObjects = getObjectType().equals(ObjectTypes.ORG) ? getPreselectedObjects() : getSelectedObjectsList();
        QName relation = getRelationValue();
        selectedObjects.forEach(selectedObject -> assignmentsMap.put(
                selectedObject.getOid(),
                ObjectTypeUtil.createAssignmentTo(selectedObject, relation)));
        return assignmentsMap;
    }

    public QName getRelationValue() {
        RelationDropDownChoicePanel relationPanel = getRelationDropDown();
        return relationPanel.getRelationValue();
    }

    private RelationDropDownChoicePanel getRelationDropDown() {
        return (RelationDropDownChoicePanel) get(ID_PARAMETERS_PANEL).get(ID_RELATION_CONTAINER).get(ID_RELATION);
    }

    @Override
    protected ObjectQuery addFilterToContentQuery(ObjectQuery query) {
        LOGGER.debug("Loading roles which the current user has right to assign");
        Task task = getPageBase().createSimpleTask(OPERATION_LOAD_ASSIGNABLE_ROLES);
        OperationResult result = task.getResult();

        ObjectFilter filter = WebComponentUtil.getAssignableRolesFilter(getTargetedAssignemntObject(), (Class<AbstractRoleType>) getObjectType().getClassDefinition(),
                isInducement() ? WebComponentUtil.AssignmentOrder.INDUCEMENT : WebComponentUtil.AssignmentOrder.ASSIGNMENT, result, task, getPageBase());
        if (query == null) {
            query = getPrismContext().queryFactory().createQuery();
        }
        query.addFilter(filter);
        return query;
    }

    protected boolean isInducement() {
        PrismContainerWrapper<AssignmentType> assignmentWrapper = getAssignmentWrapperModel();
        if (assignmentWrapper != null && assignmentWrapper.getPath() != null && assignmentWrapper.getPath().containsNameExactly(AbstractRoleType.F_INDUCEMENT)) {
            return true;
        }
        return false;
    }

    protected <O extends FocusType> PrismObject<O> getTargetedAssignemntObject() {
        PrismContainerWrapper<AssignmentType> assignmentWrapper = getAssignmentWrapperModel();
        if (assignmentWrapper == null) {
            return null;
        }
        PrismObjectWrapper<O> w = (PrismObjectWrapper<O>) assignmentWrapper.getParent().getParent();
        if (w == null) {
            return null;
        }
        return w.getObject();
    }

    protected PrismContainerWrapper<AssignmentType> getAssignmentWrapperModel() {
        return null;
    }

    @Override
    protected ObjectTypes getObjectType() {
        return ObjectTypes.FOCUS_TYPE;
    }
}

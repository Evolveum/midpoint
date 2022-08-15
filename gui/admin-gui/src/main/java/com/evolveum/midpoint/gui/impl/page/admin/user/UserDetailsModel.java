/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.user;

import static org.apache.commons.collections4.CollectionUtils.addIgnoreNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.LoadableDetachableModel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.FocusDetailsModels;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDto;
import com.evolveum.midpoint.web.page.admin.users.component.AssignmentInfoDto;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class UserDetailsModel extends FocusDetailsModels<UserType> {

    private static final String DOT_CLASS = UserDetailsModel.class.getName() + ".";
    private static final String OPERATION_LOAD_DELEGATED_BY_ME_ASSIGNMENTS = DOT_CLASS + "loadDelegatedByMeAssignments";
    private static final String OPERATION_LOAD_ASSIGNMENT_PEVIEW_DTO_LIST = DOT_CLASS + "createAssignmentPreviewDtoList";

    private LoadableModel<List<AssignmentEditorDto>> delegationsModel;
    private LoadableModel<List<AssignmentInfoDto>> privilegesListModel;
    private LoadableModel<List<AssignmentEditorDto>> delegatedToMeModel;

    public UserDetailsModel(LoadableDetachableModel<PrismObject<UserType>> prismObjectModel, PageBase serviceLocator) {
        this(prismObjectModel,false,serviceLocator);
    }

    public UserDetailsModel(LoadableDetachableModel<PrismObject<UserType>> prismObjectModel,boolean history, PageBase serviceLocator) {
        super(prismObjectModel, history, serviceLocator);

        delegationsModel = new LoadableModel<>(false) {
            private static final long serialVersionUID = 1L;

            @Override
            protected List<AssignmentEditorDto> load() {
                String userOid = prismObjectModel.getObject().getOid();
                if (StringUtils.isNotEmpty(userOid)) {
                    return loadDelegatedByMeAssignments(userOid);
                } else {
                    return new ArrayList<>();
                }
            }
        };

        privilegesListModel = new LoadableModel<>(false) {
            private static final long serialVersionUID = 1L;

            @Override
            protected List<AssignmentInfoDto> load() {
                return getUserPrivilegesList();
            }
        };

        delegatedToMeModel = new LoadableModel<>(false) {

            private static final long serialVersionUID = 1L;

            @Override
            protected List<AssignmentEditorDto> load() {
                return loadDelegatedToMe();
            }
        };

    }

    private List<AssignmentEditorDto> loadDelegatedByMeAssignments(String userOid) {
        OperationResult result = new OperationResult(OPERATION_LOAD_DELEGATED_BY_ME_ASSIGNMENTS);
        List<AssignmentEditorDto> list = new ArrayList<>();
        try{

            Task task = getPageBase().createSimpleTask(OPERATION_LOAD_DELEGATED_BY_ME_ASSIGNMENTS);

            PrismReferenceValue referenceValue = getPrismContext().itemFactory().createReferenceValue(userOid,
                    UserType.COMPLEX_TYPE);
            referenceValue.setRelation(WebComponentUtil.getDefaultRelationOrFail(RelationKindType.DELEGATION));

            ObjectFilter refFilter = getPrismContext().queryFor(UserType.class)
                    .item(UserType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF).ref(referenceValue)
                    .buildFilter();

            ObjectQuery query = getPrismContext().queryFactory().createQuery(refFilter);

            List<PrismObject<UserType>> usersList = getModelServiceLocator().getModelService().searchObjects(UserType.class, query, null, task, result);
            List<String> processedUsersOid = new ArrayList<>();
            if (usersList != null && usersList.size() > 0){
                for (PrismObject<UserType> user : usersList) {
                    if (processedUsersOid.contains(user.getOid())){
                        continue;
                    }
                    List<AssignmentType> assignments = user.asObjectable().getAssignment();
                    for (AssignmentType assignment : assignments) {
                        if (assignment.getTargetRef() != null &&
                                StringUtils.isNotEmpty(assignment.getTargetRef().getOid()) &&
                                assignment.getTargetRef().getOid().equals(userOid)) {
                            AssignmentEditorDto dto = new AssignmentEditorDto(UserDtoStatus.MODIFY, assignment, getPageBase(),
                                    user.asObjectable());
                            dto.setEditable(false);
                            list.add(dto);
                        }
                    }
                    processedUsersOid.add(user.getOid());
                }
            }

        } catch (Exception ex){
            result.recomputeStatus();
            getPageBase().showResult(result);
        }
        Collections.sort(list);
        return list;
    }

    private List<AssignmentInfoDto> getUserPrivilegesList(){
        List<AssignmentInfoDto> list = new ArrayList<>();
        OperationResult result = new OperationResult(OPERATION_LOAD_ASSIGNMENT_PEVIEW_DTO_LIST);
        Task task = getPageBase().createSimpleTask(OPERATION_LOAD_ASSIGNMENT_PEVIEW_DTO_LIST);
        for (AssignmentType assignment : getObjectType().getAssignment()) {
            addIgnoreNull(list, createDelegableAssignmentsPreviewDto(assignment, task, result));
        }
        return list;
    }

    protected <AR extends AbstractRoleType> AssignmentInfoDto createDelegableAssignmentsPreviewDto(AssignmentType assignment, Task task, OperationResult result) {
        if (assignment.getTargetRef() == null) {
            return null;
        }

        if (!isAbstractRoleType(assignment.getTargetRef().getType())) {
            return null;
        }

        PrismObject<AR> targetObject = WebModelServiceUtils.resolveReferenceNoFetch(assignment.getTargetRef(),
                getPageBase(), task, result);
        if (isDelegableTarget(targetObject)) {
            return createDelegableAssignmentsPreviewDto(targetObject,  assignment);
        }

        return null;
    }

    private boolean isAbstractRoleType(QName targetType) {
        return QNameUtil.match(RoleType.COMPLEX_TYPE, targetType)
                || QNameUtil.match(OrgType.COMPLEX_TYPE, targetType)
                || QNameUtil.match(ServiceType.COMPLEX_TYPE, targetType);
    }

    private <AR extends AbstractRoleType> boolean isDelegableTarget(PrismObject<AR> targetObject) {
        Boolean isDelegable = false;
        if (targetObject != null) {
            isDelegable = targetObject.asObjectable().isDelegable();
        }
        return isDelegable != null && isDelegable;
    }

    private AssignmentInfoDto createDelegableAssignmentsPreviewDto(PrismObject<? extends AssignmentHolderType> targetObject, AssignmentType assignment) {
        AssignmentInfoDto dto = new AssignmentInfoDto();
        dto.setTargetOid(targetObject.getOid());
        dto.setTargetName(getNameToDisplay(targetObject));
        dto.setTargetDescription(targetObject.asObjectable().getDescription());
        dto.setTargetClass(targetObject.getCompileTimeClass());
        dto.setTargetType(WebComponentUtil.classToQName(getPrismContext(), targetObject.getCompileTimeClass()));
        dto.setDirect(true);
        if (assignment != null) {
            if (assignment.getTenantRef() != null) {
                dto.setTenantName(WebModelServiceUtils.resolveReferenceName(assignment.getTenantRef(), getPageBase()));
                dto.setTenantRef(assignment.getTenantRef());
            }
            if (assignment.getOrgRef() != null) {
                dto.setOrgRefName(WebModelServiceUtils.resolveReferenceName(assignment.getOrgRef(), getPageBase()));
                dto.setOrgRef(assignment.getOrgRef());
            }
            if (assignment.getTargetRef() != null) {
                dto.setRelation(assignment.getTargetRef().getRelation());
            }
        }
        return dto;
    }

    private String getNameToDisplay(PrismObject<? extends AssignmentHolderType> target) {
        if (target.canRepresent(AbstractRoleType.class)) {
            String n = PolyString.getOrig(((AbstractRoleType) target.asObjectable()).getDisplayName());
            if (StringUtils.isNotBlank(n)) {
                return n;
            }
        }
        return PolyString.getOrig(target.asObjectable().getName());
    }

    private List<AssignmentEditorDto> loadDelegatedToMe() {
        List<AssignmentEditorDto> list = new ArrayList<>();

        List<AssignmentType> assignments = getObjectType().getAssignment();
        for (AssignmentType assignment : assignments) {
            if (assignment.getTargetRef() != null &&
                    UserType.COMPLEX_TYPE.equals(assignment.getTargetRef().getType())) {
                AssignmentEditorDto dto = new AssignmentEditorDto(UserDtoStatus.MODIFY, assignment, getPageBase());
                dto.setSimpleView(true);
                dto.setEditable(false);
                list.add(dto);
            }
        }

        Collections.sort(list);

        return list;
    }

    public LoadableModel<List<AssignmentEditorDto>> getDelegationsModel() {
        return delegationsModel;
    }

    public List<AssignmentEditorDto> getDelegationsModelObject() {
        return delegationsModel.getObject();
    }

    public LoadableModel<List<AssignmentInfoDto>> getPrivilegesListModel() {
        return privilegesListModel;
    }

    public LoadableModel<List<AssignmentEditorDto>> getDelegatedToMeModel() {
        return delegatedToMeModel;
    }
}

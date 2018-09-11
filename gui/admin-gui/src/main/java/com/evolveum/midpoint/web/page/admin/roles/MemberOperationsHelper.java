package com.evolveum.midpoint.web.page.admin.roles;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.wicket.ajax.AjaxRequestTarget;

import com.evolveum.midpoint.gui.api.component.ChooseMemberPopup;
import com.evolveum.midpoint.gui.api.component.ChooseOrgMemberPopup;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.query.InOidFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.TypeFilter;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.query.builder.S_AtomicFilterExit;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskCategory;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.admin.dto.ObjectViewDto;
import com.evolveum.midpoint.web.page.admin.roles.AbstractRoleMemberPanel.MemberOperation;
import com.evolveum.midpoint.web.page.admin.roles.AbstractRoleMemberPanel.QueryScope;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

public class MemberOperationsHelper {

	private static final Trace LOGGER = TraceManager.getTrace(AbstractRoleMemberPanel.class);
	
	public static void recomputeMembersPerformed(PageBase modelServiceLocator, QueryScope scope, ObjectQuery query, Collection<QName> supportedRelations, AjaxRequestTarget target) {
		Task operationalTask = modelServiceLocator.createSimpleTask(getTaskName("Recompute", scope));
		executeMemberOperation(modelServiceLocator, operationalTask, FocusType.COMPLEX_TYPE, query, null,
				TaskCategory.RECOMPUTATION, target);

	}
	
	public static <R extends AbstractRoleType> void unassignMembersPerformed(PageBase pageBase, R targetObject, QueryScope scope, ObjectQuery query, Collection<QName> relation, QName type, AjaxRequestTarget target) {
		Task operationalTask = pageBase.createSimpleTask(getTaskName("Remove", scope));
		ObjectDelta delta = prepareAssignmentDelta(targetObject, type, relation, MemberOperation.REMOVE, pageBase.getPrismContext(), operationalTask.getResult());
		executeMemberOperation(pageBase, operationalTask, type, query, delta,
				TaskCategory.EXECUTE_CHANGES, target);

	}
	
	public static <R extends AbstractRoleType> void unassignOtherOrgMembersPerformed(PageBase pageBase, R targetObject, QueryScope scope, ObjectQuery query, Collection<QName> relations, AjaxRequestTarget target) {
		Task operationalTask = pageBase.createSimpleTask(getTaskName("Remove", scope, false));
		ObjectDelta delta = prepareObjectTypeDelta(targetObject, relations, MemberOperation.REMOVE, operationalTask.getResult(), pageBase.getPrismContext());
		if (delta == null) {
			return;
		}
		executeMemberOperation(pageBase, operationalTask, ObjectType.COMPLEX_TYPE,
				query, delta, TaskCategory.EXECUTE_CHANGES, target);
		
		
	}
	
	public static void deleteMembersPerformed(PageBase pageBase, QueryScope scope, ObjectQuery query, QName type, AjaxRequestTarget target) {
		Task operationalTask = pageBase.createSimpleTask(getTaskName("Delete", scope));
		OperationResult parentResult = operationalTask.getResult();
		try {
			TaskType taskType = WebComponentUtil.createSingleRecurrenceTask(parentResult.getOperation(), type, query, null, null, TaskCategory.UTIL, pageBase);
			taskType.setHandlerUri(ModelPublicConstants.DELETE_TASK_HANDLER_URI);
			
			WebModelServiceUtils.runTask(taskType, operationalTask, operationalTask.getResult(), pageBase);
		} catch (SchemaException e) {
			parentResult.recordFatalError(parentResult.getOperation(), e);
			LoggingUtils.logUnexpectedException(LOGGER,
					"Failed to execute operation " + parentResult.getOperation(), e);
			target.add(pageBase.getFeedbackPanel());
		}
		//FIXME: temporary hack
		
		
	}
	
	public static <O extends ObjectType, R extends AbstractRoleType> void assignMembers(PageBase pageBase, R targetRefObject, AjaxRequestTarget target, List<QName> availableRelationList) {

		ChooseMemberPopup<O, R> browser = new ChooseMemberPopup<O, R>(pageBase.getMainPopupBodyId(), availableRelationList) {
			private static final long serialVersionUID = 1L;

			@Override
			protected R getAssignmentTargetRefObject(){
				return targetRefObject;
			}
		};
		browser.setOutputMarkupId(true);
		pageBase.showMainPopup(browser, target);
	}
	
	public static <O extends ObjectType> void assignOrgMembers(PageBase pageBase, OrgType targetRefObject, AjaxRequestTarget target, List<QName> availableRelationList) {
		ChooseOrgMemberPopup<O> browser = new ChooseOrgMemberPopup<O>(pageBase.getMainPopupBodyId(), availableRelationList) {
			
			private static final long serialVersionUID = 1L;

			@Override
			protected OrgType getAssignmentTargetRefObject(){
				return targetRefObject;
			}
		};
		
		browser.setOutputMarkupId(true);
		pageBase.showMainPopup(browser, target);
	}
	
	public static <O extends ObjectType, R extends AbstractRoleType> void initObjectForAdd(PageBase pageBase, R targetObject, QName type, Collection<QName> relations, AjaxRequestTarget target) throws SchemaException {
		pageBase.hideMainPopup(target);
		PrismContext prismContext = pageBase.getPrismContext();
		PrismObjectDefinition<O> def = prismContext.getSchemaRegistry().findObjectDefinitionByType(type);
		PrismObject<O> obj = def.instantiate();
		List<ObjectReferenceType> newReferences = new ArrayList<>();
		for (QName relation : relations) {
			newReferences.add(createReference(targetObject, relation));
		}
		ObjectType objType = (ObjectType) obj.asObjectable();
		if (FocusType.class.isAssignableFrom(obj.getCompileTimeClass())) {
			newReferences.stream().forEach(ref -> {
				AssignmentType assignment = new AssignmentType();
				assignment.setTargetRef(ref);
				((FocusType) objType).getAssignment().add(assignment);
				
				// Set parentOrgRef in any case. This is not strictly correct.
				// The parentOrgRef should be added by the projector. But
				// this is needed to successfully pass through security
				// TODO: fix MID-3234
				if (ref.getType() != null && OrgType.COMPLEX_TYPE.equals(ref.getType())) {
					objType.getParentOrgRef().add(ref.clone());
				}
			});
			
		}

		WebComponentUtil.dispatchToObjectDetailsPage(obj, true, pageBase);
	}
	
	public static <R extends AbstractRoleType> ObjectQuery createDirectMemberQuery(R targetObject, QName objectType, Collection<QName> relations, ObjectViewDto<OrgType> tenant, ObjectViewDto<OrgType> project, PrismContext prismContext) {
		ObjectQuery query;

		// We assume tenantRef.relation and orgRef.relation are always default ones (see also MID-3581)
		S_AtomicFilterExit q = QueryBuilder.queryFor(FocusType.class, prismContext)
				.item(FocusType.F_ASSIGNMENT, AssignmentType.F_TARGET_REF)
				.ref(createReferenceValuesList(targetObject, relations));
		if (tenant != null && tenant.getObjectType() != null) {
			q = q.and().item(FocusType.F_ASSIGNMENT, AssignmentType.F_TENANT_REF).ref(ObjectTypeUtil.createObjectRef(tenant.getObjectType(),
					prismContext).asReferenceValue());
		}

		if (project != null && project.getObjectType() != null) {
			q = q.and().item(FocusType.F_ASSIGNMENT, AssignmentType.F_ORG_REF).ref(ObjectTypeUtil.createObjectRef(project.getObjectType(),
					prismContext).asReferenceValue());
		}

		query = q.build();
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Searching members of role {} with query:\n{}", targetObject.getOid(), query.debugDump());
		}

		if (objectType == null || FocusType.COMPLEX_TYPE.equals(objectType)) {
			return query;
		} else {
			return ObjectQuery.createObjectQuery(TypeFilter.createType(objectType, query.getFilter()));
		}
	}
	
	public static <R extends AbstractRoleType> List<PrismReferenceValue> createReferenceValuesList(R targetObject, Collection<QName> relations) {
		List<PrismReferenceValue> referenceValuesList = new ArrayList<>();
		relations.forEach(relation -> referenceValuesList.add(createReference(targetObject, relation).asReferenceValue()));
		return referenceValuesList;
	}
	
	public static <O extends ObjectType> ObjectQuery createSelectedObjectsQuery(List<O> selectedObjects) {
		Set<String> oids = getFocusOidToRecompute(selectedObjects);
		ObjectQuery query = ObjectQuery.createObjectQuery(InOidFilter.createInOid(oids));
		return query;
	}

	public static <O extends ObjectType> Set<String> getFocusOidToRecompute(List<O> selectedObjects) {
		Set<String> oids = new HashSet<>();
		selectedObjects.stream().forEach(f -> oids.add(f.getOid()));
		return oids;
	}
	
	private static String getTaskName(String operation, QueryScope scope) {
		return getTaskName(operation, scope, false);
	}
	
	private static String getTaskName(String operation, QueryScope scope, boolean managers) {
		StringBuilder nameBuilder = new StringBuilder(operation);
		nameBuilder.append(".");
		if (scope != null) {
			nameBuilder.append(scope.name());
			nameBuilder.append(".");
		}
		if (managers) {
			nameBuilder.append("managers");
		} else {
			nameBuilder.append("members");
		}
		return nameBuilder.toString();
	}

	private static <R extends AbstractRoleType> ObjectDelta prepareAssignmentDelta(R targetObject, QName type, Collection<QName> relations, MemberOperation operation, PrismContext prismContext, OperationResult result) {
			Class classType = WebComponentUtil.qnameToClass(prismContext, type);
			ObjectDelta delta = null;
			try {
				switch (operation) {
					case ADD:
						delta = getAddAssignmentDelta(targetObject, relations, classType, prismContext);
						break;

					case REMOVE:
						delta = getDeleteAssignmentDelta(targetObject, relations, classType, prismContext);
						break;
				}
			} catch (SchemaException e) {
				LoggingUtils.logUnexpectedException(LOGGER, "Failed to prepare delta for operation " + operation.name(), e);
				result.recordFatalError("Failed to prepare delta for operation " + operation.name(), e);
			}
			return delta;
		
	}
	
	public static <R extends AbstractRoleType> ObjectDelta prepareObjectTypeDelta(R targetObject, Collection<QName> relations, MemberOperation operation, OperationResult result, PrismContext prismContext) {
		ObjectDelta delta = null;
		try {
			switch (operation) {
				case ADD:
					delta = getAddParentOrgDelta(targetObject, relations, prismContext);
					break;
	
				case REMOVE:
					delta = getDeleteParentOrgDelta(targetObject, relations, prismContext);
					break;
				default:
					break;
			}
		} catch (SchemaException e) {
			LoggingUtils.logUnexpectedException(LOGGER, "Failed to prepare delta for operation " + operation.name(), e);
			result.recordFatalError("Failed to prepare delta for operation " + operation.name(), e);
		}
		return delta;

	}
	
	
	
	//TODO: why it is not used??
	private static <R extends AbstractRoleType> AssignmentType createMemberAssignmentToModify(R targetObject, QName relation, ObjectViewDto<OrgType> tenant, ObjectViewDto<OrgType> project, PrismContext prismContext) throws SchemaException {
		AssignmentType assignmentToModify = MemberOperationsHelper.createAssignmentToModify(targetObject, relation, prismContext);

		if (tenant != null && tenant.getObjectType() != null) {
			assignmentToModify.setTenantRef(ObjectTypeUtil.createObjectRef(tenant.getObjectType().getOid(), ObjectTypes.ORG));
		}
		
		if (project != null && project.getObjectType() != null) {
			assignmentToModify.setOrgRef(ObjectTypeUtil.createObjectRef(project.getObjectType().getOid(), ObjectTypes.ORG));
		}

		return assignmentToModify;
	}

	protected static <R extends AbstractRoleType> ObjectDelta getAddAssignmentDelta(R targetObject, Collection<QName> relations, Class classType, PrismContext prismContext) throws SchemaException {
		ObjectDelta delta = ObjectDelta.createEmptyModifyDelta(classType, "fakeOid", prismContext);
		if (relations == null || relations.isEmpty()) {
			delta.addModificationAddContainer(FocusType.F_ASSIGNMENT, createAssignmentToModify(targetObject, null, prismContext));
			return delta;
		} 
	
		for (QName relation : relations) {
				delta.addModificationAddContainer(FocusType.F_ASSIGNMENT, createAssignmentToModify(targetObject, relation, prismContext));
		}
		return delta;
	}
	
	protected static <R extends AbstractRoleType> ObjectDelta getDeleteAssignmentDelta(R targetObject, Collection<QName> relations, Class classType, PrismContext prismContext) throws SchemaException {
		if (relations == null || relations.isEmpty()) {
			return ObjectDelta.createModificationDeleteContainer(classType, "fakeOid",
					FocusType.F_ASSIGNMENT, prismContext, createAssignmentToModify(targetObject, null, prismContext));
		}
		
		ObjectDelta delta =  ObjectDelta.createEmptyModifyDelta(classType, "fakeOid", prismContext);
		
		for (QName relation : relations) {
			delta.addModificationDeleteContainer(FocusType.F_ASSIGNMENT, createAssignmentToModify(targetObject, relation, prismContext));
		}
		
		return delta;  
		
	}
	
	protected static <R extends AbstractRoleType> ObjectDelta getAddParentOrgDelta(R targetObject, Collection<QName> relations, PrismContext prismContext) throws SchemaException {
		ObjectDelta delta = ObjectDelta.createEmptyModifyDelta(ObjectType.class, "fakeOid", prismContext);
		if (relations == null || relations.isEmpty()) {
			delta.addModificationAddReference(ObjectType.F_PARENT_ORG_REF, ObjectTypeUtil.createObjectRef(targetObject,
					prismContext).asReferenceValue());
			return delta;
		} 

		for (QName relation : relations) {
				delta.addModificationAddReference(ObjectType.F_PARENT_ORG_REF, createReference(targetObject, relation).asReferenceValue());
		}
		return delta;
	}
 
	protected static <R extends AbstractRoleType> ObjectDelta getDeleteParentOrgDelta(R targetObject,  Collection<QName> relations, PrismContext prismContext) throws SchemaException {
		if (relations == null || relations.isEmpty()) {
			return ObjectDelta.createModificationDeleteReference(ObjectType.class, "fakeOid",
					ObjectType.F_PARENT_ORG_REF, prismContext, ObjectTypeUtil.createObjectRef(targetObject, prismContext).asReferenceValue());
		}
		
		ObjectDelta delta =  ObjectDelta.createEmptyModifyDelta(ObjectType.class, "fakeOid", prismContext);
		
		for (QName relation : relations) {
			delta.addModificationDeleteReference(ObjectType.F_PARENT_ORG_REF, MemberOperationsHelper.createReference(targetObject, relation).asReferenceValue());
		}
		
		return delta;  
		
	}
	
	public static <R extends AbstractRoleType> AssignmentType createAssignmentToModify(R targetObject, QName relation, PrismContext prismContext) throws SchemaException {
		AssignmentType assignmentToModify = new AssignmentType();
		assignmentToModify.setTargetRef(createReference(targetObject, relation));
		prismContext.adopt(assignmentToModify);
		return assignmentToModify;
	}
	
	public static <R extends AbstractRoleType> ObjectReferenceType createReference(R targetObject, QName relation) {
		ObjectReferenceType ref = ObjectTypeUtil.createObjectRef(targetObject, relation);
		return ref;
	}
	
	protected static void executeMemberOperation(PageBase modelServiceLocator, Task operationalTask, QName type, ObjectQuery memberQuery,
			ObjectDelta delta, String category, AjaxRequestTarget target) {

		OperationResult parentResult = operationalTask.getResult();

		try {
			WebComponentUtil.executeMemberOperation(operationalTask, type, memberQuery, delta, category, parentResult, modelServiceLocator);
		} catch (SchemaException e) {
			parentResult.recordFatalError(parentResult.getOperation(), e);
			LoggingUtils.logUnexpectedException(LOGGER,
					"Failed to execute operation " + parentResult.getOperation(), e);
			target.add(modelServiceLocator.getFeedbackPanel());
		}

		target.add(modelServiceLocator.getFeedbackPanel());
	}

}

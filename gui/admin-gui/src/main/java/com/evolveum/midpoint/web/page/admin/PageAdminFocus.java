/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.web.page.admin;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.EvaluatedAssignment;
import com.evolveum.midpoint.model.api.context.EvaluatedAssignmentTarget;
import com.evolveum.midpoint.model.api.context.EvaluatedConstruction;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDto;
import com.evolveum.midpoint.web.component.assignment.AssignmentsUtil;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.ContainerValueWrapper;
import com.evolveum.midpoint.web.component.prism.ContainerWrapper;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.show.PagePreviewChanges;
import com.evolveum.midpoint.web.component.progress.ProgressReportingAwarePage;
import com.evolveum.midpoint.web.component.util.ObjectWrapperUtil;
import com.evolveum.midpoint.web.page.admin.users.component.AssignmentInfoDto;
import com.evolveum.midpoint.web.page.admin.users.dto.FocusSubwrapperDto;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.web.security.SecurityUtils;
import com.evolveum.midpoint.web.util.validation.MidpointFormValidator;
import com.evolveum.midpoint.web.util.validation.SimpleValidationError;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Session;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.protocol.http.WebSession;

import javax.xml.namespace.QName;
import java.util.*;

public abstract class PageAdminFocus<F extends FocusType> extends PageAdminObjectDetails<F>
		implements ProgressReportingAwarePage {
	private static final long serialVersionUID = 1L;

	public static final String AUTH_USERS_ALL = AuthorizationConstants.AUTZ_UI_USERS_ALL_URL;
	public static final String AUTH_USERS_ALL_LABEL = "PageAdminUsers.auth.usersAll.label";
	public static final String AUTH_USERS_ALL_DESCRIPTION = "PageAdminUsers.auth.usersAll.description";

	public static final String AUTH_ORG_ALL = AuthorizationConstants.AUTZ_UI_ORG_ALL_URL;
	public static final String AUTH_ORG_ALL_LABEL = "PageAdminUsers.auth.orgAll.label";
	public static final String AUTH_ORG_ALL_DESCRIPTION = "PageAdminUsers.auth.orgAll.description";

	private LoadableModel<List<FocusSubwrapperDto<ShadowType>>> projectionModel;
	private LoadableModel<List<AssignmentEditorDto>> delegatedToMeModel;

	private static final String DOT_CLASS = PageAdminFocus.class.getName() + ".";
	private static final String OPERATION_RECOMPUTE_ASSIGNMENTS = DOT_CLASS + "recomputeAssignments";

	private static final String OPERATION_LOAD_SHADOW = DOT_CLASS + "loadShadow";

	private static final Trace LOGGER = TraceManager.getTrace(PageAdminFocus.class);


	@Override
	protected void initializeModel(final PrismObject<F> objectToEdit, boolean isNewObject, boolean isReadonly) {
		super.initializeModel(objectToEdit, isNewObject, isReadonly);

		projectionModel = new LoadableModel<List<FocusSubwrapperDto<ShadowType>>>(false) {
			private static final long serialVersionUID = 1L;

			@Override
			protected List<FocusSubwrapperDto<ShadowType>> load() {
				return loadShadowWrappers();
			}
		};

        delegatedToMeModel= new LoadableModel<List<AssignmentEditorDto>>(false) {

        	private static final long serialVersionUID = 1L;
            @Override
            protected List<AssignmentEditorDto> load() {
                return loadDelegatedToMe();
            }
        };

    }

    public LoadableModel<List<FocusSubwrapperDto<ShadowType>>> getProjectionModel() {
		return projectionModel;
	}

	public LoadableModel<List<AssignmentEditorDto>> getDelegatedToMeModel() {
		return delegatedToMeModel;
	}

	public List<FocusSubwrapperDto<ShadowType>> getFocusShadows() {
		return projectionModel.getObject();
	}

	protected void reviveModels() throws SchemaException {
		super.reviveModels();
		WebComponentUtil.revive(projectionModel, getPrismContext());
	}

	@Override
	public void finishProcessing(AjaxRequestTarget target, OperationResult result, boolean returningFromAsync) {

		if (previewRequested) {
			finishPreviewProcessing(target, result);
			return;
		}
        if (result.isSuccess() && getDelta() != null  && getDelta().getOid().equals(SecurityUtils.getPrincipalUser().getOid())) {
            UserType user = null;
            if (getObjectWrapper().getObject().asObjectable() instanceof UserType){
                user = (UserType) getObjectWrapper().getObject().asObjectable();
            }
            Session.get().setLocale(WebModelServiceUtils.getLocale(user));
            LOGGER.debug("Using {} as locale", getLocale());
            WebSession.get().getClientInfo().getProperties().
                    setTimeZone(WebModelServiceUtils.getTimezone(user));
            LOGGER.debug("Using {} as time zone", WebSession.get().getClientInfo().getProperties().getTimeZone());
        }
		boolean focusAddAttempted = getDelta() != null && getDelta().isAdd();
		boolean focusAddSucceeded = focusAddAttempted && StringUtils.isNotEmpty(getDelta().getOid());

		// we don't want to allow resuming editing if a new focal object was created (on second 'save' there would be a conflict with itself)
		// and also in case of partial errors, like those related to projections (many deltas would be already executed, and this could cause problems on second 'save').
		boolean canContinueEditing = !focusAddSucceeded && result.isFatalError();

		boolean canExitPage;
		if (returningFromAsync) {
			canExitPage = getProgressPanel().isAllSuccess();			// if there's at least a warning in the progress table, we would like to keep the table open
		} else {
			canExitPage = !canContinueEditing;							// no point in staying on page if we cannot continue editing (in synchronous case i.e. no progress table present)
		}

		if (!isKeepDisplayingResults() && canExitPage) {
			showResult(result);
			redirectBack();
		} else {
			if (returningFromAsync) {
				getProgressPanel().showBackButton(target);
				getProgressPanel().hideAbortButton(target);
			}
            showResult(result);
			target.add(getFeedbackPanel());

			if (canContinueEditing) {
				getProgressPanel().hideBackButton(target);
				getProgressPanel().showContinueEditingButton(target);
			}
		}
	}

	private void finishPreviewProcessing(AjaxRequestTarget target, OperationResult result) {
		getMainPanel().setVisible(true);
		getProgressPanel().hide();
		getProgressPanel().hideAbortButton(target);
		getProgressPanel().hideBackButton(target);
		getProgressPanel().hideContinueEditingButton(target);

		showResult(result);
		target.add(getFeedbackPanel());
		navigateToNext(new PagePreviewChanges(getProgressPanel().getPreviewResult(), getModelInteractionService()));
	}

	@Override
	public void continueEditing(AjaxRequestTarget target) {
		getMainPanel().setVisible(true);
		getProgressPanel().hide();
		getProgressPanel().hideAbortButton(target);
		getProgressPanel().hideBackButton(target);
		getProgressPanel().hideContinueEditingButton(target);
		target.add(this);
	}

	private List<FocusSubwrapperDto<ShadowType>> loadShadowWrappers() {
		// Load the projects with noFetch by default. Only load the full projection on-denand.
		// The full projection load happens when loadFullShadow() is explicitly invoked.
		return loadSubwrappers(ShadowType.class, UserType.F_LINK_REF, true);
	}

	public void loadFullShadow(FocusSubwrapperDto<ShadowType> shadowWrapperDto) {
		ObjectWrapper<ShadowType> shadowWrapperOld = shadowWrapperDto.getObject();
		Task task = createSimpleTask(OPERATION_LOAD_SHADOW);
		FocusSubwrapperDto<ShadowType> shadowWrapperDtoNew = loadSubWrapperDto(ShadowType.class, shadowWrapperOld.getObject().getOid(), false, task);
		if (shadowWrapperDtoNew == null) {
			// No access or error. The status is in the last subresult of task result. TODO: pass the result explicitly to loadSubWrapperDto
			OperationResult subresult = task.getResult().getLastSubresult();
			shadowWrapperDto.getObject().setFetchResult(subresult);
			return;
		}
		ObjectWrapper<ShadowType> shadowWrapperNew = shadowWrapperDtoNew.getObject();
		shadowWrapperOld.copyRuntimeStateTo(shadowWrapperNew);
		shadowWrapperDto.setObject(shadowWrapperNew);
	}

	@Override
	protected List<FocusSubwrapperDto<OrgType>> loadOrgWrappers() {
		return loadSubwrappers(OrgType.class, UserType.F_PARENT_ORG_REF, false);
	}

	private <S extends ObjectType> List<FocusSubwrapperDto<S>> loadSubwrappers(Class<S> type,
			QName propertyToLoad, boolean noFetch) {
		List<FocusSubwrapperDto<S>> list = new ArrayList<>();

		ObjectWrapper<F> focusWrapper = getObjectModel().getObject();
		PrismObject<F> focus = focusWrapper.getObject();
		PrismReference prismReference = focus.findReference(new ItemPath(propertyToLoad));
		if (prismReference == null) {
			return new ArrayList<>();
		}
		List<PrismReferenceValue> references = prismReference.getValues();

		Task task = createSimpleTask(OPERATION_LOAD_SHADOW);
		for (PrismReferenceValue reference : references) {
			FocusSubwrapperDto<S> subWrapper = loadSubWrapperDto(type, reference.getOid(), noFetch, task);
			if (subWrapper != null) {
				list.add(subWrapper);
			}
		}

		return list;
	}

	private <S extends ObjectType> FocusSubwrapperDto<S> loadSubWrapperDto(Class<S> type, String oid, boolean noFetch, Task task) {
		if (oid == null) {
			return null;
		}
		OperationResult subResult = task.getResult().createMinorSubresult(OPERATION_LOAD_SHADOW);
		String resourceName = null;
		try {
			Collection<SelectorOptions<GetOperationOptions>> loadOptions;
			if (ShadowType.class.equals(type)) {
				GetOperationOptions resourceOption = GetOperationOptions.createResolve();
				resourceOption.setReadOnly(true);
				loadOptions = SelectorOptions.createCollection(ShadowType.F_RESOURCE, resourceOption);
			} else {
				loadOptions = new ArrayList<>();
			}

			if (noFetch) {
				GetOperationOptions rootOptions = SelectorOptions.findRootOptions(loadOptions);
				if (rootOptions == null) {
					loadOptions.add(new SelectorOptions<>(GetOperationOptions.createNoFetch()));
				} else {
					rootOptions.setNoFetch(true);
				}
			}

			PrismObject<S> projection = WebModelServiceUtils.loadObject(type, oid, loadOptions, this, task, subResult);
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Loaded projection {} ({}):\n{}", oid, loadOptions, projection==null?null:projection.debugDump());
			}
			if (projection == null) {
				// No access or error
				// TODO actually it would be nice to show an error if the shadow repo object does not exist
				return null;
			}
			S projectionType = projection.asObjectable();

			OperationResultType fetchResult = projectionType.getFetchResult();

			StringBuilder description = new StringBuilder();
			if (ShadowType.class.equals(type)) {
				ShadowType shadowType = (ShadowType) projectionType;
				ResourceType resource = shadowType.getResource();
				resourceName = WebComponentUtil.getName(resource);

				if (shadowType.getIntent() != null) {
					description.append(shadowType.getIntent()).append(", ");
				}
			} else if (OrgType.class.equals(type)) {
				OrgType orgType = (OrgType) projectionType;
				resourceName = orgType.getDisplayName() != null
						? WebComponentUtil.getOrigStringFromPoly(orgType.getDisplayName()) : "";
			}
			description.append(WebComponentUtil.getOrigStringFromPoly(projectionType.getName()));

			ObjectWrapper<S> wrapper = ObjectWrapperUtil.createObjectWrapper(resourceName,
					description.toString(), projection, ContainerStatus.MODIFYING, task, this);
			wrapper.setLoadOptions(loadOptions);
			wrapper.setFetchResult(OperationResult.createOperationResult(fetchResult));
			wrapper.setSelectable(true);
			wrapper.setMinimalized(true);

//			wrapper.initializeContainers(this);

			subResult.computeStatus();

			return new FocusSubwrapperDto<>(wrapper, UserDtoStatus.MODIFY);

		} catch (Exception ex) {
			subResult.recordFatalError("Couldn't load account." + ex.getMessage(), ex);
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't load account", ex);
			subResult.computeStatus();
			return new FocusSubwrapperDto<>(false, resourceName, subResult);
		}
	}

    private List<AssignmentEditorDto> loadDelegatedToMe() {
        List<AssignmentEditorDto> list = new ArrayList<>();

        ObjectWrapper<F> focusWrapper = getObjectModel().getObject();
        PrismObject<F> focus = focusWrapper.getObject();
        List<AssignmentType> assignments = focus.asObjectable().getAssignment();
        for (AssignmentType assignment : assignments) {
            if (assignment.getTargetRef() != null &&
                    UserType.COMPLEX_TYPE.equals(assignment.getTargetRef().getType())) {
                AssignmentEditorDto dto = new AssignmentEditorDto(UserDtoStatus.MODIFY, assignment, this);
                dto.setSimpleView(true);
                dto.setEditable(false);
                list.add(dto);
            }
        }

        Collections.sort(list);

        return list;
    }

  protected List<AssignmentType> getPolicyRulesList(List<AssignmentType> assignments, UserDtoStatus status){
		List<AssignmentType> list = new ArrayList<>();
		for (AssignmentType assignment : assignments) {
			if (AssignmentsUtil.isPolicyRuleAssignment(assignment)) {
				//TODO set status
				list.add(assignment);
			}
		}
		return list;
	}

	@Override
	protected void prepareObjectForAdd(PrismObject<F> focus) throws SchemaException {
		super.prepareObjectForAdd(focus);
		F focusType = focus.asObjectable();
		// handle added accounts

		List<ShadowType> shadowsToAdd = prepareSubobject(getFocusShadows());
		if (!shadowsToAdd.isEmpty()) {
			shadowsToAdd.forEach(shadowType -> addDefaultKindAndIntent(shadowType.asPrismObject()));
			focusType.getLink().addAll(shadowsToAdd);
		}

		List<OrgType> orgsToAdd = prepareSubobject(getParentOrgs());
		if (!orgsToAdd.isEmpty()){
			focusType.getParentOrg().addAll(orgsToAdd);
		}

	}

	@Override
	protected void prepareObjectDeltaForModify(ObjectDelta<F> focusDelta) throws SchemaException {
		super.prepareObjectDeltaForModify(focusDelta);
		// handle accounts
		PrismObjectDefinition<F> objectDefinition = getObjectDefinition();
		PrismReferenceDefinition refDef = objectDefinition.findReferenceDefinition(FocusType.F_LINK_REF);
		ReferenceDelta refDelta = prepareUserAccountsDeltaForModify(refDef);
		if (!refDelta.isEmpty()) {
			focusDelta.addModification(refDelta);
		}

		refDef = objectDefinition.findReferenceDefinition(FocusType.F_PARENT_ORG_REF);
		refDelta = prepareUserOrgsDeltaForModify(refDef);
		if (!refDelta.isEmpty()) {
			focusDelta.addModification(refDelta);
		}
	}

	protected PrismObjectDefinition<F> getObjectDefinition() {
		SchemaRegistry registry = getPrismContext().getSchemaRegistry();
		return registry
				.findObjectDefinitionByCompileTimeClass(getCompileTimeClass());
	}

	protected ContainerDelta handleAssignmentDeltas(ObjectDelta<F> focusDelta,
			List<AssignmentEditorDto> assignments, PrismContainerDefinition def,
													boolean isDelegation) throws SchemaException {
		ContainerDelta assDelta = new ContainerDelta(ItemPath.EMPTY_PATH, def.getName(), def, getPrismContext());

		for (AssignmentEditorDto assDto : assignments) {
			PrismContainerValue newValue = assDto.getNewValue(getPrismContext());

			switch (assDto.getStatus()) {
				case ADD:
					newValue.applyDefinition(def, false);
					assDelta.addValueToAdd(newValue.clone());
					break;
				case DELETE:
					PrismContainerValue oldValue = assDto.getOldValue();
					if (isDelegation){
						oldValue.applyDefinition(def, false);
					} else {
						oldValue.applyDefinition(def);
					}
					assDelta.addValueToDelete(oldValue.clone());
					break;
				case MODIFY:
					if (!assDto.isModified(getPrismContext())) {
						LOGGER.trace("Assignment '{}' not modified.", new Object[] { assDto.getName() });
						continue;
					}

					handleModifyAssignmentDelta(assDto, def, newValue, focusDelta);
					break;
				default:
					warn(getString("pageAdminUser.message.illegalAssignmentState", assDto.getStatus()));
			}
		}

		if (!assDelta.isEmpty()) {
			assDelta = focusDelta.addModification(assDelta);
		}

		// todo remove this block [lazyman] after model is updated - it has to
		// remove resource from accountConstruction
		Collection<PrismContainerValue> values = assDelta.getValues(PrismContainerValue.class);
		for (PrismContainerValue value : values) {
			AssignmentType ass = new AssignmentType();
			ass.setupContainerValue(value);
			removeResourceFromAccConstruction(ass);
		}

		return assDelta;
	}

	protected ContainerDelta<AssignmentType> handleAssignmentExperimentalDeltas(ObjectDelta<F> focusDelta,
											List<ContainerValueWrapper<AssignmentType>> assignments, PrismContainerDefinition def,
																				boolean isDelegation) throws SchemaException {
		ContainerDelta<AssignmentType> assDelta = new ContainerDelta(ItemPath.EMPTY_PATH, def.getName(), def, getPrismContext());


		if (!assDelta.isEmpty()) {
			assDelta = focusDelta.addModification(assDelta);
		}

		return assDelta;
	}

	private void handleModifyAssignmentDelta(AssignmentEditorDto assDto,
			PrismContainerDefinition assignmentDef, PrismContainerValue newValue, ObjectDelta<F> focusDelta)
					throws SchemaException {
		LOGGER.debug("Handling modified assignment '{}', computing delta.",
				new Object[] { assDto.getName() });

		PrismValue oldValue = assDto.getOldValue();
		Collection<? extends ItemDelta> deltas = oldValue.diff(newValue);

		for (ItemDelta delta : deltas) {
			ItemPath deltaPath = delta.getPath().rest();
			ItemDefinition deltaDef = assignmentDef.findItemDefinition(deltaPath);

			delta.setParentPath(WebComponentUtil.joinPath(oldValue.getPath(), delta.getPath().allExceptLast()));
			delta.applyDefinition(deltaDef);

			focusDelta.addModification(delta);
		}
	}

	@Override
	protected boolean executeForceDelete(ObjectWrapper userWrapper, Task task, ModelExecuteOptions options,
			OperationResult parentResult) {
		if (isForce()) {
			OperationResult result = parentResult.createSubresult("Force delete operation");

			try {
				ObjectDelta<F> forceDeleteDelta = getForceDeleteDelta(userWrapper);
				forceDeleteDelta.revive(getPrismContext());

				if (forceDeleteDelta != null && !forceDeleteDelta.isEmpty()) {
					getModelService().executeChanges(WebComponentUtil.createDeltaCollection(forceDeleteDelta),
							options, task, result);
				}
			} catch (Exception ex) {
				result.recordFatalError("Failed to execute delete operation with force.");
				LoggingUtils.logUnexpectedException(LOGGER, "Failed to execute delete operation with force", ex);
				return false;
			}

			result.recomputeStatus();
			result.recordSuccessIfUnknown();
			return true;
		}
		return false;
	}

	private ObjectDelta getForceDeleteDelta(ObjectWrapper focusWrapper) throws SchemaException {

		List<FocusSubwrapperDto<ShadowType>> accountDtos = getFocusShadows();
		List<ReferenceDelta> refDeltas = new ArrayList<>();
		ObjectDelta<F> forceDeleteDelta = null;
		for (FocusSubwrapperDto<ShadowType> accDto : accountDtos) {
			if (!accDto.isLoadedOK()) {
				continue;
			}

			if (accDto.getStatus() == UserDtoStatus.DELETE) {
				ObjectWrapper accWrapper = accDto.getObject();
				ReferenceDelta refDelta = ReferenceDelta.createModificationDelete(UserType.F_LINK_REF,
						focusWrapper.getObject().getDefinition(), accWrapper.getObject());
				refDeltas.add(refDelta);
			} else if (accDto.getStatus() == UserDtoStatus.UNLINK) {
				ObjectWrapper accWrapper = accDto.getObject();
				ReferenceDelta refDelta = ReferenceDelta.createModificationDelete(UserType.F_LINK_REF,
						focusWrapper.getObject().getDefinition(), accWrapper.getObject().getOid());
				refDeltas.add(refDelta);
			}
		}
		if (!refDeltas.isEmpty()) {
			forceDeleteDelta = ObjectDelta.createModifyDelta(focusWrapper.getObject().getOid(), refDeltas,
					getCompileTimeClass(), getPrismContext());
		}
		PrismContainerDefinition def = focusWrapper.getObject().findContainer(UserType.F_ASSIGNMENT)
				.getDefinition();
		if (forceDeleteDelta == null) {
			forceDeleteDelta = ObjectDelta.createEmptyModifyDelta(getCompileTimeClass(),
					focusWrapper.getObject().getOid(), getPrismContext());
		}
//perhaps not needed anymore
		ContainerWrapper assignmentContainerWrapper = getObjectWrapper().findContainerWrapper(new ItemPath(FocusType.F_ASSIGNMENT));
		handleAssignmentExperimentalDeltas(forceDeleteDelta, assignmentContainerWrapper.getValues(), def, false);
		return forceDeleteDelta;
	}

	private <P extends ObjectType> List<P> prepareSubobject(List<FocusSubwrapperDto<P>> projections) throws SchemaException{
		List<P> projectionsToAdd = new ArrayList<>();
		for (FocusSubwrapperDto<P> projection : projections) {
			if (!projection.isLoadedOK()) {
				continue;
			}

			if (UserDtoStatus.MODIFY.equals(projection.getStatus())) {
				// this is legal e.g. when child org is being create (one assignment comes pre-created)
				// TODO do we need more specific checks here?
				continue;
			}

			if (!UserDtoStatus.ADD.equals(projection.getStatus())) {
				warn(getString("pageAdminFocus.message.illegalAccountState", projection.getStatus()));
				continue;
			}

			ObjectWrapper<P> projectionWrapper = projection.getObject();
			ObjectDelta<P> delta = projectionWrapper.getObjectDelta();
			PrismObject<P> proj = delta.getObjectToAdd();
			WebComponentUtil.encryptCredentials(proj, true, getMidpointApplication());

			projectionsToAdd.add(proj.asObjectable());
		}
		return projectionsToAdd;
	}


	@Override
	protected List<ObjectDelta<? extends ObjectType>> getAdditionalModifyDeltas(OperationResult result) {
		return getShadowModifyDeltas(result);
	}

	private List<ObjectDelta<? extends ObjectType>> getShadowModifyDeltas(OperationResult result) {
		List<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();

		List<FocusSubwrapperDto<ShadowType>> accounts = getFocusShadows();
		OperationResult subResult = null;
		for (FocusSubwrapperDto<ShadowType> account : accounts) {
			if (!account.isLoadedOK()) {
				continue;
			}

			try {
				ObjectWrapper accountWrapper = account.getObject();
				ObjectDelta delta = accountWrapper.getObjectDelta();
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Account delta computed from {} as:\n{}",
							new Object[] { accountWrapper, delta.debugDump(3) });
				}

				if (!UserDtoStatus.MODIFY.equals(account.getStatus())) {
					continue;
				}

				if (delta.isEmpty()
						&& (accountWrapper.getOldDelta() == null || accountWrapper.getOldDelta().isEmpty())) {
					continue;
				}

				if (accountWrapper.getOldDelta() != null) {
					delta = ObjectDelta.summarize(delta, accountWrapper.getOldDelta());
				}

				// what is this???
				// subResult = result.createSubresult(OPERATION_MODIFY_ACCOUNT);

				WebComponentUtil.encryptCredentials(delta, true, getMidpointApplication());
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Modifying account:\n{}", new Object[] { delta.debugDump(3) });
				}

				deltas.add(delta);
				// subResult.recordSuccess();
			} catch (Exception ex) {
				// if (subResult != null) {
				result.recordFatalError("Couldn't compute account delta.", ex);
				// }
				LoggingUtils.logUnexpectedException(LOGGER, "Couldn't compute account delta", ex);
			}
		}

		return deltas;
	}

	/**
	 * remove this method after model is updated - it has to remove resource
	 * from accountConstruction
	 */
	@Deprecated
	private void removeResourceFromAccConstruction(AssignmentType assignment) {
		ConstructionType accConstruction = assignment.getConstruction();
		if (accConstruction == null || accConstruction.getResource() == null) {
			return;
		}

		ObjectReferenceType ref = new ObjectReferenceType();
		ref.setOid(assignment.getConstruction().getResource().getOid());
		ref.setType(ResourceType.COMPLEX_TYPE);
		assignment.getConstruction().setResourceRef(ref);
		assignment.getConstruction().setResource(null);
	}

	private ReferenceDelta prepareUserAccountsDeltaForModify(PrismReferenceDefinition refDef)
			throws SchemaException {
		ReferenceDelta refDelta = new ReferenceDelta(refDef, getPrismContext());

		List<FocusSubwrapperDto<ShadowType>> accounts = getFocusShadows();
		for (FocusSubwrapperDto<ShadowType> accDto : accounts) {
			if (accDto.isLoadedOK()) {
				ObjectWrapper accountWrapper = accDto.getObject();
				accountWrapper.revive(getPrismContext());
				ObjectDelta delta = accountWrapper.getObjectDelta();
				PrismReferenceValue refValue = new PrismReferenceValue(null, OriginType.USER_ACTION, null);

				PrismObject<ShadowType> account;
				switch (accDto.getStatus()) {
					case ADD:
						account = delta.getObjectToAdd();
						addDefaultKindAndIntent(account);
						WebComponentUtil.encryptCredentials(account, true, getMidpointApplication());
						refValue.setObject(account);
						refDelta.addValueToAdd(refValue);
						break;
					case DELETE:
						account = accountWrapper.getObject();
						refValue.setObject(account);
						refDelta.addValueToDelete(refValue);
						break;
					case MODIFY:
						// nothing to do, account modifications were applied
						// before
						continue;
					case UNLINK:
						refValue.setOid(delta.getOid());
						refValue.setTargetType(ShadowType.COMPLEX_TYPE);
						refDelta.addValueToDelete(refValue);
						break;
					default:
						warn(getString("pageAdminFocus.message.illegalAccountState", accDto.getStatus()));
				}
			}
		}

		return refDelta;
	}

	private void addDefaultKindAndIntent(PrismObject<ShadowType> account) {
		if (account.asObjectable().getKind() == null) {
			account.asObjectable().setKind(ShadowKindType.ACCOUNT);
		}
		if (account.asObjectable().getIntent() == null) {
			account.asObjectable().setIntent(SchemaConstants.INTENT_DEFAULT);
		}
	}

	private ReferenceDelta prepareUserOrgsDeltaForModify(PrismReferenceDefinition refDef)
			throws SchemaException {
		ReferenceDelta refDelta = new ReferenceDelta(refDef, getPrismContext());

		List<FocusSubwrapperDto<OrgType>> orgs = getParentOrgs();
		for (FocusSubwrapperDto<OrgType> orgDto : orgs) {
			if (orgDto.isLoadedOK()) {
				ObjectWrapper<OrgType> orgWrapper = orgDto.getObject();
				orgWrapper.revive(getPrismContext());
				ObjectDelta<OrgType> delta = orgWrapper.getObjectDelta();
				PrismReferenceValue refValue = new PrismReferenceValue(null, OriginType.USER_ACTION, null);

				switch (orgDto.getStatus()) {
					case ADD:
						refValue.setOid(delta.getOid());
						refValue.setTargetType(OrgType.COMPLEX_TYPE);
						refDelta.addValueToAdd(refValue);
						break;
					case DELETE:
						break;
					case MODIFY:
						break;
					case UNLINK:
						refValue.setOid(delta.getOid());
						refValue.setTargetType(OrgType.COMPLEX_TYPE);
						refDelta.addValueToDelete(refValue);
						break;
					default:
						warn(getString("pageAdminFocus.message.illegalAccountState", orgDto.getStatus()));
				}
			}
		}

		return refDelta;
	}

	public List<AssignmentInfoDto> showAllAssignmentsPerformed(AjaxRequestTarget ajaxRequestTarget) {
		LOGGER.debug("Recompute user assignments");
		Task task = createSimpleTask(OPERATION_RECOMPUTE_ASSIGNMENTS);
		OperationResult result = new OperationResult(OPERATION_RECOMPUTE_ASSIGNMENTS);
		ObjectDelta<F> delta;
		Set<AssignmentInfoDto> assignmentInfoDtoSet = new TreeSet<>();

		try {
			reviveModels();

			ObjectWrapper<F> focusWrapper = getObjectWrapper();
			delta = focusWrapper.getObjectDelta();
			if (focusWrapper.getOldDelta() != null) {
				delta = ObjectDelta.summarize(focusWrapper.getOldDelta(), delta);
			}

			switch (focusWrapper.getStatus()) {
				case ADDING:
					PrismObject<F> focus = delta.getObjectToAdd();
					prepareObjectForAdd(focus);
					getPrismContext().adopt(focus, getCompileTimeClass());

					LOGGER.trace("Delta before add focus:\n{}", delta.debugDumpLazily(3));
					if (!delta.isEmpty()) {
						delta.revive(getPrismContext());
					} else {
						result.recordSuccess();
					}
					break;
				case MODIFYING:
					prepareObjectDeltaForModify(delta);
					LOGGER.trace("Delta before modify user:\n{}", delta.debugDumpLazily(3));

					List<ObjectDelta<? extends ObjectType>> accountDeltas = getShadowModifyDeltas(result);
					if (!delta.isEmpty()) {
						delta.revive(getPrismContext());
					}
					for (ObjectDelta accDelta : accountDeltas) {
						if (!accDelta.isEmpty()) {
							accDelta.revive(getPrismContext());
						}
					}
					break;
				default:
					error(getString("pageAdminFocus.message.unsupportedState", focusWrapper.getStatus()));
			}

			ModelContext<UserType> modelContext;
			try {
				ModelExecuteOptions options = ModelExecuteOptions.createEvaluateAllAssignmentRelationsOnRecompute();
				modelContext = getModelInteractionService().previewChanges(Collections.singleton(delta), options, task, result);
			} catch (NoFocusNameSchemaException e) {
				info(getString("pageAdminFocus.message.noUserName"));
				ajaxRequestTarget.add(getFeedbackPanel());
				return null;
			}

			DeltaSetTriple<? extends EvaluatedAssignment<?>> evaluatedAssignmentTriple = modelContext.getEvaluatedAssignmentTriple();
			Collection<? extends EvaluatedAssignment<?>> evaluatedAssignments = evaluatedAssignmentTriple.getNonNegativeValues();

			if (evaluatedAssignments.isEmpty()) {
				info(getString("pageAdminFocus.message.noAssignmentsAvailable"));
				ajaxRequestTarget.add(getFeedbackPanel());
				return null;
			}

			for (EvaluatedAssignment<?> evaluatedAssignment : evaluatedAssignments) {
				if (!evaluatedAssignment.isValid()) {
					continue;
				}
				// roles and orgs
				DeltaSetTriple<? extends EvaluatedAssignmentTarget> targetsTriple = evaluatedAssignment.getRoles();
				Collection<? extends EvaluatedAssignmentTarget> targets = targetsTriple.getNonNegativeValues();
				for (EvaluatedAssignmentTarget target : targets) {
					if (target.appliesToFocusWithAnyRelation()) {
						assignmentInfoDtoSet.add(createAssignmentsPreviewDto(target, task, result));
					}
				}

				// all resources
				DeltaSetTriple<EvaluatedConstruction> evaluatedConstructionsTriple = evaluatedAssignment
						.getEvaluatedConstructions(task, result);
				Collection<EvaluatedConstruction> evaluatedConstructions = evaluatedConstructionsTriple
						.getNonNegativeValues();
				for (EvaluatedConstruction construction : evaluatedConstructions) {
					if (!construction.isWeak()) {
						assignmentInfoDtoSet.add(createAssignmentsPreviewDto(construction));
					}
				}
			}

			return new ArrayList<>(assignmentInfoDtoSet);

		} catch (Exception e) {
			LoggingUtils.logUnexpectedException(LOGGER, "Could not create assignments preview.", e);
			error("Could not create assignments preview. Reason: " + e);
			ajaxRequestTarget.add(getFeedbackPanel());
		}
        return null;
	}

	private AssignmentInfoDto createAssignmentsPreviewDto(EvaluatedAssignmentTarget evaluatedAbstractRole,
			Task task, OperationResult result) {
		return createAssignmentsPreviewDto(evaluatedAbstractRole.getTarget(), evaluatedAbstractRole.isDirectlyAssigned(),
				evaluatedAbstractRole.getAssignment(), task, result);
	}

	protected AssignmentInfoDto createAssignmentsPreviewDto(ObjectReferenceType reference,
																Task task, OperationResult result) {
		PrismObject<? extends FocusType> targetObject = WebModelServiceUtils.resolveReferenceNoFetch(reference,
				PageAdminFocus.this, task, result);

		return createAssignmentsPreviewDto(targetObject, true,
				null, task, result);
	}

    protected AssignmentInfoDto createDelegableAssignmentsPreviewDto(AssignmentType assignment, Task task, OperationResult result) {
        if (assignment.getTargetRef() != null) {
            if (RoleType.COMPLEX_TYPE.equals(assignment.getTargetRef().getType())
                    || OrgType.COMPLEX_TYPE.equals(assignment.getTargetRef().getType())
                    || ServiceType.COMPLEX_TYPE.equals(assignment.getTargetRef().getType())) {
                PrismObject<AbstractRoleType> targetObject = WebModelServiceUtils.resolveReferenceNoFetch(assignment.getTargetRef(),
                        PageAdminFocus.this, task, result);
                Boolean isDelegable = false;
				if (targetObject != null) {
					isDelegable = targetObject.getRealValue().isDelegable();
				}
                if (Boolean.TRUE.equals(isDelegable)) {
                    return createAssignmentsPreviewDto(targetObject, true, assignment, task, result);
                }
            }
        }
        return null;
    }

	private AssignmentInfoDto createAssignmentsPreviewDto(PrismObject<? extends FocusType> targetObject,
			boolean isDirectlyAssigned, AssignmentType assignment,
			Task task, OperationResult result) {
		AssignmentInfoDto dto = new AssignmentInfoDto();
		dto.setTargetOid(targetObject.getOid());
		dto.setTargetName(getNameToDisplay(targetObject));
		dto.setTargetDescription(targetObject.asObjectable().getDescription());
		dto.setTargetClass(targetObject.getCompileTimeClass());
        dto.setTargetType(WebComponentUtil.classToQName(getPrismContext(), targetObject.getCompileTimeClass()));
		dto.setDirect(isDirectlyAssigned);
		if (assignment != null) {
			if (assignment.getTenantRef() != null) {
				dto.setTenantName(nameFromReference(assignment.getTenantRef(),
						task, result));
				dto.setTenantRef(assignment.getTenantRef());
			}
			if (assignment.getOrgRef() != null) {
				dto.setOrgRefName(
						nameFromReference(assignment.getOrgRef(), task, result));
				dto.setOrgRef(assignment.getOrgRef());
			}
			if (assignment.getTargetRef() != null){
				dto.setRelation(assignment.getTargetRef().getRelation());
			}
		}
		return dto;
	}

	private String getNameToDisplay(PrismObject<? extends FocusType> target) {
		if (target.canRepresent(AbstractRoleType.class)) {
			String n = PolyString.getOrig(((AbstractRoleType)target.asObjectable()).getDisplayName());
			if (StringUtils.isNotBlank(n)) {
				return n;
			}
		}
		return PolyString.getOrig(target.asObjectable().getName());
	}

	//TODO: unify with WebComponentUtil getName vs. getEffectiveName (duplicate)
	private String nameFromReference(ObjectReferenceType reference, Task task, OperationResult result) {
		String oid = reference.getOid();
		QName type = reference.getType();
		Class<? extends ObjectType> clazz = getPrismContext().getSchemaRegistry().getCompileTimeClass(type);
		PrismObject<? extends ObjectType> prismObject;
		try {
			prismObject = getModelService().getObject(clazz, oid,
					SelectorOptions.createCollection(GetOperationOptions.createNoFetch()), task, result);
		} catch (ObjectNotFoundException | SchemaException | SecurityViolationException
				| CommunicationException | ConfigurationException | ExpressionEvaluationException
				| RuntimeException | Error e) {
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't retrieve name for {}: {}", e,
					clazz.getSimpleName(), oid);
			return "Couldn't retrieve name for " + oid;
		}
		ObjectType object = prismObject.asObjectable();
		if (object instanceof AbstractRoleType) {
			return getNameToDisplay((PrismObject<? extends FocusType>) object.asPrismObject());
		} else {
			return PolyString.getOrig(object.getName());
		}
	}

	private AssignmentInfoDto createAssignmentsPreviewDto(EvaluatedConstruction evaluatedConstruction) {
		AssignmentInfoDto dto = new AssignmentInfoDto();
		PrismObject<ResourceType> resource = evaluatedConstruction.getResource();
		dto.setTargetOid(resource.getOid());
		dto.setTargetName(PolyString.getOrig(resource.asObjectable().getName()));
		dto.setTargetDescription(resource.asObjectable().getDescription());
		dto.setTargetClass(resource.getCompileTimeClass());
		dto.setDirect(evaluatedConstruction.isDirectlyAssigned());
		dto.setKind(evaluatedConstruction.getKind());
		dto.setIntent(evaluatedConstruction.getIntent());
		return dto;
	}

	@Override
	protected void performAdditionalValidation(PrismObject<F> object,
			Collection<ObjectDelta<? extends ObjectType>> deltas, Collection<SimpleValidationError> errors) throws SchemaException {

		if (object != null && object.asObjectable() != null) {
			for (AssignmentType assignment : object.asObjectable().getAssignment()) {
				for (MidpointFormValidator validator : getFormValidatorRegistry().getValidators()) {
					if (errors == null) {
						errors = validator.validateAssignment(assignment);
					} else {
						errors.addAll(validator.validateAssignment(assignment));
					}
				}
			}
		}

	}

}

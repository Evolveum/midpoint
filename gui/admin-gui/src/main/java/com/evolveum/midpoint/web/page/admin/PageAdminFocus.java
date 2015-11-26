/*
 * Copyright (c) 2010-2015 Evolveum
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

import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.EvaluatedAbstractRole;
import com.evolveum.midpoint.model.api.context.EvaluatedAssignment;
import com.evolveum.midpoint.model.api.context.EvaluatedConstruction;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RetrieveOption;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.FocusSummaryPanel;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDto;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDtoType;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorPanel;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.dialog.ConfirmationDialog;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenu;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.prism.*;
import com.evolveum.midpoint.web.component.progress.ProgressReporter;
import com.evolveum.midpoint.web.component.progress.ProgressReportingAwarePage;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.ObjectWrapperUtil;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.server.PageTasks;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDtoProvider;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDtoProviderOptions;
import com.evolveum.midpoint.web.page.admin.users.component.*;
import com.evolveum.midpoint.web.page.admin.users.dto.FocusProjectionDto;
import com.evolveum.midpoint.web.page.admin.users.dto.SimpleUserResourceProvider;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.web.resource.img.ImgResources;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.web.util.WebModelUtils;
import com.evolveum.midpoint.web.util.validation.MidpointFormValidator;
import com.evolveum.midpoint.web.util.validation.SimpleValidationError;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.string.StringValue;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.*;

public abstract class PageAdminFocus<T extends FocusType> extends PageAdmin
		implements ProgressReportingAwarePage {

	public static final String AUTH_USERS_ALL = AuthorizationConstants.AUTZ_UI_USERS_ALL_URL;
	public static final String AUTH_USERS_ALL_LABEL = "PageAdminUsers.auth.usersAll.label";
	public static final String AUTH_USERS_ALL_DESCRIPTION = "PageAdminUsers.auth.usersAll.description";

	public static final String AUTH_ORG_ALL = AuthorizationConstants.AUTZ_UI_ORG_ALL_URL;
	public static final String AUTH_ORG_ALL_LABEL = "PageAdminUsers.auth.orgAll.label";
	public static final String AUTH_ORG_ALL_DESCRIPTION = "PageAdminUsers.auth.orgAll.description";

	private LoadableModel<ObjectWrapper<T>> focusModel;
	private LoadableModel<List<FocusProjectionDto>> shadowModel;
	private LoadableModel<List<FocusProjectionDto>> orgModel;
	private LoadableModel<List<AssignmentEditorDto>> assignmentsModel;

	private ProgressReporter progressReporter;

	private static final String DOT_CLASS = PageAdminFocus.class.getName() + ".";
	private static final String OPERATION_LOAD_FOCUS = DOT_CLASS + "loadFocus";
	private static final String OPERATION_LOAD_PARENT_ORGS = DOT_CLASS + "loadParentOrgs";
	private static final String OPERATION_LOAD_ASSIGNMENTS = DOT_CLASS + "loadAssignments";
	private static final String OPERATION_LOAD_ASSIGNMENT = DOT_CLASS + "loadAssignment";
	private static final String OPERATION_SAVE = DOT_CLASS + "save";
	private static final String OPERATION_SEND_TO_SUBMIT = DOT_CLASS + "sendToSubmit";
	// private static final String OPERATION_MODIFY_ACCOUNT = DOT_CLASS + "modifyAccount";
	// private static final String OPERATION_PREPARE_ACCOUNTS = DOT_CLASS + "getAccountsForSubmit";
	private static final String OPERATION_RECOMPUTE_ASSIGNMENTS = DOT_CLASS + "recomputeAssignments";

	private static final String OPERATION_LOAD_SHADOW = DOT_CLASS + "loadShadow";

	protected static final String ID_MAIN_FORM = "mainForm";
	private static final String ID_TAB_PANEL = "tabPanel";
	private static final String ID_ASSIGNMENT_EDITOR = "assignmentEditor";
	private static final String ID_ASSIGNMENT_LIST = "assignmentList";
	private static final String ID_TASK_TABLE = "taskTable";
	private static final String ID_EXECUTE_OPTIONS = "executeOptions";
	private static final String ID_SHADOW_LIST = "shadowList";
	private static final String ID_SHADOWS = "shadows";
	private static final String ID_ASSIGNMENTS = "assignments";
	private static final String ID_SHADOW_MENU = "shadowMenu";
	private static final String ID_ASSIGNMENT_MENU = "assignmentMenu";
	private static final String ID_SHADOW_CHECK_ALL = "shadowCheckAll";
	private static final String ID_ASSIGNMENT_CHECK_ALL = "assignmentCheckAll";
	protected static final String ID_SUMMARY_PANEL = "summaryPanel";

	private static final String MODAL_ID_CONFIRM_DELETE_SHADOW = "confirmDeleteShadowPopup";
	private static final String MODAL_ID_CONFIRM_DELETE_ASSIGNMENT = "confirmDeleteAssignmentPopup";

	private static final Trace LOGGER = TraceManager.getTrace(PageAdminFocus.class);

	// used to determine whether to leave this page or stay on it (after
	// operation finishing)
	private ObjectDelta delta;

	private LoadableModel<ExecuteChangeOptionsDto> executeOptionsModel = new LoadableModel<ExecuteChangeOptionsDto>(
			false) {

		@Override
		protected ExecuteChangeOptionsDto load() {
			return new ExecuteChangeOptionsDto();
		}
	};

	@Override
	protected IModel<String> createPageTitleModel() {
		return new LoadableModel<String>() {

			@Override
			protected String load() {
				if (!isEditingFocus()) {
					return createStringResource("pageAdminFocus.title.newFocusType").getObject();
				}

				return createStringResource("pageAdminFocus.title.editFocusType").getObject();
			}
		};
	}

	@Override
	protected IModel<String> createPageSubTitleModel() {
		return new LoadableModel<String>() {

			@Override
			protected String load() {
				if (!isEditingFocus()) {
					return createStringResource(
							"pageAdminFocus.subTitle.new" + getCompileTimeClass().getSimpleName())
									.getObject();
				}

				String name = null;
				if (getFocusWrapper() != null && getFocusWrapper().getObject() != null) {
					name = WebMiscUtil.getName(getFocusWrapper().getObject());
				}

				return createStringResource(
						"pageAdminFocus.subTitle.edit" + getCompileTimeClass().getSimpleName(), name)
								.getObject();
			}
		};
	}

	public LoadableModel<ObjectWrapper<T>> getFocusModel() {
		return focusModel;
	}

	public void initialize(final PrismObject<T> userToEdit) {
		focusModel = new LoadableModel<ObjectWrapper<T>>(false) {

			@Override
			protected ObjectWrapper<T> load() {
				return loadFocusWrapper(userToEdit);
			}
		};
		shadowModel = new LoadableModel<List<FocusProjectionDto>>(false) {

			@Override
			protected List<FocusProjectionDto> load() {
				return loadShadowWrappers();
			}
		};

		orgModel = new LoadableModel<List<FocusProjectionDto>>(false) {

			@Override
			protected List<FocusProjectionDto> load() {
				return loadOrgWrappers();
			}
		};

		assignmentsModel = new LoadableModel<List<AssignmentEditorDto>>(false) {

			@Override
			protected List<AssignmentEditorDto> load() {
				return loadAssignments();
			}
		};

		performCustomInitialization();

		initLayout();
	}

	protected void performCustomInitialization() {

	}

	protected abstract T createNewFocus();

	protected void initCustomLayout(Form mainForm) {
		// Nothing to do here
	}

	protected void initSummaryPanel(Form mainForm) {

		FocusSummaryPanel<T> summaryPanel = createSummaryPanel();

		summaryPanel.setOutputMarkupId(true);

		summaryPanel.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return isEditingFocus();
			}
		});

		mainForm.add(summaryPanel);
	}

	protected abstract FocusSummaryPanel<T> createSummaryPanel();

	protected abstract void initTabs(List<ITab> tabs);

	private void initLayout() {
		final Form mainForm = new Form(ID_MAIN_FORM, true);
		mainForm.setMaxSize(MidPointApplication.FOCUS_PHOTO_MAX_FILE_SIZE);
		mainForm.setMultiPart(true);
		add(mainForm);

		progressReporter = ProgressReporter.create(this, mainForm, "progressPanel");

		// PrismObjectPanel userForm = new PrismObjectPanel(ID_FOCUS_FORM,
		// focusModel, new PackageResourceReference(
		// ImgResources.class, ImgResources.USER_PRISM), mainForm, this) {
		//
		// @Override
		// protected IModel<String> createDescription(IModel<ObjectWrapper>
		// model) {
		// return createStringResource("pageAdminFocus.description");
		// }
		// };
		// mainForm.add(userForm);

		List<ITab> tabs = new ArrayList<>();
		tabs.add(new AbstractTab(createStringResource("pageAdminFocus.basic")) {

			@Override
			public WebMarkupContainer getPanel(String panelId) {
				return new BaseFocusPanel<T>(panelId, mainForm, focusModel, shadowModel, orgModel, assignmentsModel,
						PageAdminFocus.this) {

					@Override
					protected T createNewFocus() {
						return PageAdminFocus.this.createNewFocus();
					}

					@Override
					protected void reviveCustomModels() throws SchemaException {
						PageAdminFocus.this.reviveCustomModels();

					}

					@Override
					protected Class<T> getCompileTimeClass() {
						return PageAdminFocus.this.getCompileTimeClass();
					}

					@Override
					protected Class getRestartResponsePage() {
						return PageAdminFocus.this.getRestartResponsePage();
					}
				};
			}
		});

		initTabs(tabs);

		TabbedPanel focusTabPanel = new TabbedPanel(ID_TAB_PANEL, tabs) {
			@Override
			protected WebMarkupContainer newLink(String linkId, final int index) {
				return new AjaxSubmitLink(linkId) {

					@Override
					protected void onError(AjaxRequestTarget target,
							org.apache.wicket.markup.html.form.Form<?> form) {
						super.onError(target, form);
						target.add(getFeedbackPanel());
					}

					@Override
					protected void onSubmit(AjaxRequestTarget target,
							org.apache.wicket.markup.html.form.Form<?> form) {
						super.onSubmit(target, form);

						setSelectedTab(index);
						if (target != null) {
							target.add(findParent(TabbedPanel.class));
						}
					}

				};
			}
		};
		focusTabPanel.setOutputMarkupId(true);

		mainForm.add(focusTabPanel);

		// WebMarkupContainer shadows = new WebMarkupContainer(ID_SHADOWS);
		// shadows.setOutputMarkupId(true);
		// mainForm.add(shadows);
		// initShadows(shadows);
		//
		// WebMarkupContainer assignments = new
		// WebMarkupContainer(ID_ASSIGNMENTS);
		// assignments.setOutputMarkupId(true);
		// mainForm.add(assignments);
		// initAssignments(assignments);
		//
		// WebMarkupContainer tasks = new WebMarkupContainer(ID_TASKS);
		// tasks.setOutputMarkupId(true);
		// mainForm.add(tasks);
		// initTasks(tasks);
		//
		initButtons(mainForm);
		initOptions(mainForm);
		initSummaryPanel(mainForm);
		initCustomLayout(mainForm);
		//
		// initResourceModal();
		// initAssignableModal();
		// initConfirmationDialogs();
		//
		// initCustomLayout(mainForm);
	}

	public ObjectWrapper getFocusWrapper() {
		return focusModel.getObject();
	}

	public List<FocusProjectionDto> getFocusShadows() {
		return shadowModel.getObject();
	}
	
	public List<FocusProjectionDto> getFocusOrgs() {
		return orgModel.getObject();
	}

	public List<AssignmentEditorDto> getFocusAssignments() {
		return assignmentsModel.getObject();
	}

	protected void reviveCustomModels() throws SchemaException {
		// Nothing to do here;
	}

	protected void reviveModels() throws SchemaException {
		WebMiscUtil.revive(focusModel, getPrismContext());
		WebMiscUtil.revive(shadowModel, getPrismContext());
		WebMiscUtil.revive(assignmentsModel, getPrismContext());
		reviveCustomModels();
	}

	protected abstract Class<T> getCompileTimeClass();

	protected abstract Class getRestartResponsePage();

	protected String getFocusOidParameter() {
		PageParameters parameters = getPageParameters();
		LOGGER.trace("Page parameters: {}", parameters);
		StringValue oidValue = getPageParameters().get(OnePageParameterEncoder.PARAMETER);
		LOGGER.trace("OID parameter: {}", oidValue);
		if (oidValue == null) {
			return null;
		}
		String oid = oidValue.toString();
		if (StringUtils.isBlank(oid)) {
			return null;
		}
		return oid;
	}

	public boolean isEditingFocus() {
		return getFocusOidParameter() != null;
	}

	protected ObjectWrapper<T> loadFocusWrapper(PrismObject<T> userToEdit) {
		Task task = createSimpleTask(OPERATION_LOAD_FOCUS);
		OperationResult result = task.getResult();
		PrismObject<T> focus = null;
		try {
			if (!isEditingFocus()) {
				if (userToEdit == null) {
					LOGGER.trace("Loading focus: New focus (creating)");
					// UserType userType = new UserType();'
					T focusType = createNewFocus();
					getMidpointApplication().getPrismContext().adopt(focusType);
					focus = focusType.asPrismObject();
				} else {
					LOGGER.trace("Loading focus: New focus (supplied): {}", userToEdit);
					focus = userToEdit;
				}
			} else {

				Collection options = SelectorOptions.createCollection(UserType.F_JPEG_PHOTO,
						GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE));

				String focusOid = getFocusOidParameter();
				focus = WebModelUtils.loadObject(getCompileTimeClass(), focusOid, options, this, task,
						result);

				LOGGER.trace("Loading focus: Existing focus (loadled): {} -> {}", focusOid, focus);
			}

			result.recordSuccess();
		} catch (Exception ex) {
			result.recordFatalError("Couldn't get focus.", ex);
			LoggingUtils.logException(LOGGER, "Couldn't load focus", ex);
		}

		if (!result.isSuccess()) {
			showResultInSession(result);
		}

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Loaded focus:\n{}", focus.debugDump());
		}

		if (focus == null) {
			if (isEditingFocus()) {
				getSession().error(getString("pageAdminFocus.message.cantEditFocus"));
			} else {
				getSession().error(getString("pageAdminFocus.message.cantNewFocus"));
			}
			throw new RestartResponseException(getRestartResponsePage());
		}

		ContainerStatus status = isEditingFocus() ? ContainerStatus.MODIFYING : ContainerStatus.ADDING;
		ObjectWrapper<T> wrapper = null;
		try {
			wrapper = ObjectWrapperUtil.createObjectWrapper("pageAdminFocus.focusDetails", null, focus,
					status, this);
		} catch (Exception ex) {
			result.recordFatalError("Couldn't get user.", ex);
			LoggingUtils.logException(LOGGER, "Couldn't load user", ex);
			wrapper = new ObjectWrapper<>("pageAdminFocus.focusDetails", null, focus, null, status, this);
		}
		// ObjectWrapper wrapper = new ObjectWrapper("pageUser.userDetails",
		// null, user, status);
		if (wrapper.getResult() != null && !WebMiscUtil.isSuccessOrHandledError(wrapper.getResult())) {
			showResultInSession(wrapper.getResult());
		}

		loadParentOrgs(wrapper, task, result);

		wrapper.setShowEmpty(!isEditingFocus());

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Loaded focus wrapper:\n{}", wrapper.debugDump());
		}

		return wrapper;
	}

	private void loadParentOrgs(ObjectWrapper<T> wrapper, Task task, OperationResult result) {
		OperationResult subResult = result.createMinorSubresult(OPERATION_LOAD_PARENT_ORGS);
		PrismObject<T> focus = wrapper.getObject();
		// Load parent organizations (full objects). There are used in the
		// summary panel and also in the main form.
		// Do it here explicitly instead of using resolve option to have ability
		// to better handle (ignore) errors.
		for (ObjectReferenceType parentOrgRef : focus.asObjectable().getParentOrgRef()) {

			PrismObject<OrgType> parentOrg = null;
			try {

				parentOrg = getModelService().getObject(OrgType.class, parentOrgRef.getOid(), null, task,
						subResult);
				LOGGER.trace("Loaded parent org with result {}",
						new Object[] { subResult.getLastSubresult() });
			} catch (AuthorizationException e) {
				// This can happen if the user has permission to read parentOrgRef but it does not have
				// the permission to read target org
				// It is OK to just ignore it.
				subResult.muteLastSubresultError();
				LOGGER.debug("User {} does not have permission to read parent org unit {} (ignoring error)", task.getOwner().getName(), parentOrgRef.getOid());
			} catch (Exception ex) {
				subResult.recordWarning("Cannot load parent org " + parentOrgRef.getOid(), ex);
				LOGGER.warn("Cannot load parent org {}: {}", parentOrgRef.getOid(), ex.getMessage(), ex);
			}

			if (parentOrg != null) {
				wrapper.getParentOrgs().add(parentOrg);
			}
		}
		subResult.computeStatus();
	}

	// protected void finishProcessing(ObjectDelta objectDelta,
	// AjaxRequestTarget target, OperationResult result){
	// delta = objectDelta;
	// finishProcessing(target, result);
	// }

	public Object findParam(String param, String oid, OperationResult result) {

		Object object = null;

		for (OperationResult subResult : result.getSubresults()) {
			if (subResult != null && subResult.getParams() != null) {
				if (subResult.getParams().get(param) != null
						&& subResult.getParams().get(OperationResult.PARAM_OID) != null
						&& subResult.getParams().get(OperationResult.PARAM_OID).equals(oid)) {
					return subResult.getParams().get(param);
				}
				object = findParam(param, oid, subResult);

			}
		}
		return object;
	}

	@Override
	public void finishProcessing(AjaxRequestTarget target, OperationResult result) {
		boolean userAdded = delta != null && delta.isAdd() && StringUtils.isNotEmpty(delta.getOid());
		if (!executeOptionsModel.getObject().isKeepDisplayingResults() && progressReporter.isAllSuccess()
				&& (userAdded || !result.isFatalError())) { // TODO
			showResultInSession(result);
			// todo refactor this...what is this for? why it's using some
			// "shadow" param from result???
			PrismObject<T> focus = getFocusWrapper().getObject();
			T focusType = focus.asObjectable();
			for (ObjectReferenceType ref : focusType.getLinkRef()) {
				Object o = findParam("shadow", ref.getOid(), result);
				if (o != null && o instanceof ShadowType) {
					ShadowType accountType = (ShadowType) o;
					OperationResultType fetchResult = accountType.getFetchResult();
					if (fetchResult != null
							&& !OperationResultStatusType.SUCCESS.equals(fetchResult.getStatus())) {
						showResultInSession(OperationResult.createOperationResult(fetchResult));
					}
				}
			}
			setSpecificResponsePage();
		} else {
			showResult(result);
			target.add(getFeedbackPanel());

			// if we only stayed on the page because of displaying results, hide
			// the Save button
			// (the content of the page might not be consistent with reality,
			// e.g. concerning the accounts part...
			// this page was not created with the repeated save possibility in
			// mind)
			if (userAdded || !result.isFatalError()) {
				progressReporter.hideSaveButton(target);
			}
		}
	}

	private List<FocusProjectionDto> loadShadowWrappers() {
		return loadProjectionWrappers(ShadowType.class, UserType.F_LINK_REF);
	}

	private List<FocusProjectionDto> loadOrgWrappers() {
		return loadProjectionWrappers(OrgType.class, UserType.F_PARENT_ORG_REF);
	}

	private <P extends ObjectType> List<FocusProjectionDto> loadProjectionWrappers(Class<P> type,
			QName propertyToLoad) {
		List<FocusProjectionDto> list = new ArrayList<FocusProjectionDto>();

		ObjectWrapper focus = focusModel.getObject();
		PrismObject<T> prismUser = focus.getObject();
		PrismReference prismReference = prismUser.findReference(new ItemPath(propertyToLoad));
		if (prismReference == null) {
			return new ArrayList<>();
		}
		List<PrismReferenceValue> references = prismReference.getValues();

		Task task = createSimpleTask(OPERATION_LOAD_SHADOW);
		for (PrismReferenceValue reference : references) {
			OperationResult subResult = new OperationResult(OPERATION_LOAD_SHADOW);
			String resourceName = null;
			try {
				Collection<SelectorOptions<GetOperationOptions>> options = null;
				if (ShadowType.class.equals(type)) {
					options = SelectorOptions.createCollection(ShadowType.F_RESOURCE,
							GetOperationOptions.createResolve());
				} 
				
				if (reference.getOid() == null) {
					continue;
				}

				PrismObject<P> projection = WebModelUtils.loadObject(type, reference.getOid(), options, this,
						task, subResult);
				if (projection == null) {
					// No access, just skip it
					continue;
				}
				P projectionType = projection.asObjectable();

				OperationResultType fetchResult = projectionType.getFetchResult();

				StringBuilder description = new StringBuilder();
				if (ShadowType.class.equals(type)) {
					ShadowType shadowType = (ShadowType) projectionType;
					ResourceType resource = shadowType.getResource();
					resourceName = WebMiscUtil.getName(resource);

					if (shadowType.getIntent() != null) {
						description.append(shadowType.getIntent()).append(", ");
					}
				} else if (OrgType.class.equals(type)) {
					OrgType orgType = (OrgType) projectionType;
					resourceName = orgType.getDisplayName() != null
							? WebMiscUtil.getOrigStringFromPoly(orgType.getDisplayName()) : "";
				}
				description.append(WebMiscUtil.getOrigStringFromPoly(projectionType.getName()));

				ObjectWrapper wrapper = ObjectWrapperUtil.createObjectWrapper(resourceName,
						description.toString(), projection, ContainerStatus.MODIFYING, true, this);
				wrapper.setFetchResult(OperationResult.createOperationResult(fetchResult));
				wrapper.setSelectable(true);
				wrapper.setMinimalized(true);

				if (ShadowType.class.equals(type)) {

					PrismContainer<ShadowAssociationType> associationContainer = projection
							.findContainer(ShadowType.F_ASSOCIATION);
					if (associationContainer != null && associationContainer.getValues() != null) {
						List<PrismProperty> associations = new ArrayList<>(
								associationContainer.getValues().size());
						for (PrismContainerValue associationVal : associationContainer.getValues()) {
							ShadowAssociationType associationType = (ShadowAssociationType) associationVal
									.asContainerable();
							ObjectReferenceType shadowRef = associationType.getShadowRef();
							// shadowRef can be null in case of "broken"
							// associations we can safely eliminate fetching
							// from resource, because we need only the name
							if (shadowRef != null) {
								PrismObject<ShadowType> association = getModelService()
										.getObject(ShadowType.class, shadowRef.getOid(),
												SelectorOptions.createCollection(
														GetOperationOptions.createNoFetch()),
												task, subResult);
								associations.add(association.findProperty(ShadowType.F_NAME));
							}
						}
						wrapper.setAssociations(associations);
					}

				}
				wrapper.initializeContainers(this);

				list.add(new FocusProjectionDto(wrapper, UserDtoStatus.MODIFY));

				subResult.recomputeStatus();
			} catch (ObjectNotFoundException ex) {
				// this is fix for MID-854, full user/accounts/assignments
				// reload if accountRef reference is broken
				// because consistency already fixed it.
				focusModel.reset();
				shadowModel.reset();
				orgModel.reset();
				assignmentsModel.reset();
			} catch (Exception ex) {
				subResult.recordFatalError("Couldn't load account." + ex.getMessage(), ex);
				LoggingUtils.logException(LOGGER, "Couldn't load account", ex);
				list.add(new FocusProjectionDto(false, resourceName, subResult));
			} finally {
				subResult.computeStatus();
			}
		}

		return list;
	}

	private List<AssignmentEditorDto> loadAssignments() {
		List<AssignmentEditorDto> list = new ArrayList<AssignmentEditorDto>();

		OperationResult result = new OperationResult(OPERATION_LOAD_ASSIGNMENTS);

		ObjectWrapper focusWrapper = focusModel.getObject();
		PrismObject<T> focus = focusWrapper.getObject();
		List<AssignmentType> assignments = focus.asObjectable().getAssignment();
		for (AssignmentType assignment : assignments) {

			list.add(new AssignmentEditorDto(UserDtoStatus.MODIFY, assignment, this));
		}

		Collections.sort(list);

		return list;
	}

	private PrismObject getReference(ObjectReferenceType ref, OperationResult result) {
		OperationResult subResult = result.createSubresult(OPERATION_LOAD_ASSIGNMENT);
		subResult.addParam("targetRef", ref.getOid());
		PrismObject target = null;
		try {
			Task task = createSimpleTask(OPERATION_LOAD_ASSIGNMENT);
			Class type = ObjectType.class;
			if (ref.getType() != null) {
				type = getPrismContext().getSchemaRegistry().determineCompileTimeClass(ref.getType());
			}
			target = getModelService().getObject(type, ref.getOid(), null, task, subResult);
			subResult.recordSuccess();
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't get assignment target ref", ex);
			subResult.recordFatalError("Couldn't get assignment target ref.", ex);
		}

		if (!subResult.isHandledError() && !subResult.isSuccess()) {
			showResult(subResult);
		}

		return target;
	}

	protected void savePerformed(AjaxRequestTarget target) {
		LOGGER.debug("Save user.");
		OperationResult result = new OperationResult(OPERATION_SAVE);
		ObjectWrapper userWrapper = getFocusWrapper();
		// todo: improve, delta variable is quickfix for MID-1006
		// redirecting to user list page everytime user is created in repository
		// during user add in gui,
		// and we're not taking care about account/assignment create errors
		// (error message is still displayed)
		delta = null;

		Task task = createSimpleTask(OPERATION_SEND_TO_SUBMIT);
		ExecuteChangeOptionsDto executeOptions = executeOptionsModel.getObject();
		ModelExecuteOptions options = executeOptions.createOptions();
		LOGGER.debug("Using options {}.", new Object[] { executeOptions });

		try {
			reviveModels();

			delta = userWrapper.getObjectDelta();
			if (userWrapper.getOldDelta() != null) {
				delta = ObjectDelta.summarize(userWrapper.getOldDelta(), delta);
			}
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("User delta computed from form:\n{}", new Object[] { delta.debugDump(3) });
			}
		} catch (Exception ex) {
			result.recordFatalError(getString("pageUser.message.cantCreateUser"), ex);
			LoggingUtils.logException(LOGGER, "Create user failed", ex);
			showResult(result);
			return;
		}

		switch (userWrapper.getStatus()) {
			case ADDING:
				try {
					PrismObject<T> focus = delta.getObjectToAdd();
					WebMiscUtil.encryptCredentials(focus, true, getMidpointApplication());
					prepareFocusForAdd(focus);
					getPrismContext().adopt(focus, getCompileTimeClass());
					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("Delta before add user:\n{}", new Object[] { delta.debugDump(3) });
					}

					if (!delta.isEmpty()) {
						delta.revive(getPrismContext());

						Collection<SimpleValidationError> validationErrors = performCustomValidation(focus,
								WebMiscUtil.createDeltaCollection(delta));
						if (validationErrors != null && !validationErrors.isEmpty()) {
							for (SimpleValidationError error : validationErrors) {
								LOGGER.error("Validation error, attribute: '" + error.printAttribute()
										+ "', message: '" + error.getMessage() + "'.");
								error("Validation error, attribute: '" + error.printAttribute()
										+ "', message: '" + error.getMessage() + "'.");
							}

							target.add(getFeedbackPanel());
							return;
						}

						progressReporter.executeChanges(WebMiscUtil.createDeltaCollection(delta), options,
								task, result, target);
					} else {
						result.recordSuccess();
					}
				} catch (Exception ex) {
					result.recordFatalError(getString("pageFocus.message.cantCreateFocus"), ex);
					LoggingUtils.logException(LOGGER, "Create user failed", ex);
				}
				break;

			case MODIFYING:
				try {
					WebMiscUtil.encryptCredentials(delta, true, getMidpointApplication());
					prepareFocusDeltaForModify(delta);

					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("Delta before modify user:\n{}", new Object[] { delta.debugDump(3) });
					}

					List<ObjectDelta<? extends ObjectType>> accountDeltas = modifyShadows(result);
					Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();

					if (!delta.isEmpty()) {
						delta.revive(getPrismContext());
						deltas.add(delta);
					}
					for (ObjectDelta accDelta : accountDeltas) {
						if (!accDelta.isEmpty()) {
							// accDelta.setPrismContext(getPrismContext());
							accDelta.revive(getPrismContext());
							deltas.add(accDelta);
						}
					}

					if (delta.isEmpty() && ModelExecuteOptions.isReconcile(options)) {
						ObjectDelta emptyDelta = ObjectDelta.createEmptyModifyDelta(getCompileTimeClass(),
								userWrapper.getObject().getOid(), getPrismContext());
						deltas.add(emptyDelta);

						Collection<SimpleValidationError> validationErrors = performCustomValidation(null,
								deltas);
						if (validationErrors != null && !validationErrors.isEmpty()) {
							for (SimpleValidationError error : validationErrors) {
								LOGGER.error("Validation error, attribute: '" + error.printAttribute()
										+ "', message: '" + error.getMessage() + "'.");
								error("Validation error, attribute: '" + error.printAttribute()
										+ "', message: '" + error.getMessage() + "'.");
							}

							target.add(getFeedbackPanel());
							return;
						}

						progressReporter.executeChanges(deltas, options, task, result, target);
					} else if (!deltas.isEmpty()) {
						Collection<SimpleValidationError> validationErrors = performCustomValidation(null,
								deltas);
						if (validationErrors != null && !validationErrors.isEmpty()) {
							for (SimpleValidationError error : validationErrors) {
								LOGGER.error("Validation error, attribute: '" + error.printAttribute()
										+ "', message: '" + error.getMessage() + "'.");
								error("Validation error, attribute: '" + error.printAttribute()
										+ "', message: '" + error.getMessage() + "'.");
							}

							target.add(getFeedbackPanel());
							return;
						}

						progressReporter.executeChanges(deltas, options, task, result, target);
					} else {
						result.recordSuccess();
					}

				} catch (Exception ex) {
					if (!executeForceDelete(userWrapper, task, options, result)) {
						result.recordFatalError(getString("pageUser.message.cantUpdateUser"), ex);
						LoggingUtils.logException(LOGGER, getString("pageUser.message.cantUpdateUser"), ex);
					} else {
						result.recomputeStatus();
					}
				}
				break;
			// support for add/delete containers (e.g. delete credentials)
			default:
				error(getString("pageAdminFocus.message.unsupportedState", userWrapper.getStatus()));
		}

		result.recomputeStatus();

		if (!result.isInProgress()) {
			finishProcessing(target, result);
		}
	}

	private boolean executeForceDelete(ObjectWrapper userWrapper, Task task, ModelExecuteOptions options,
			OperationResult parentResult) {
		if (executeOptionsModel.getObject().isForce()) {
			OperationResult result = parentResult.createSubresult("Force delete operation");
			// List<UserAccountDto> accountDtos = accountsModel.getObject();
			// List<ReferenceDelta> refDeltas = new ArrayList<ReferenceDelta>();
			// ObjectDelta<UserType> forceDeleteDelta = null;
			// for (UserAccountDto accDto : accountDtos) {
			// if (accDto.getStatus() == UserDtoStatus.DELETE) {
			// ObjectWrapper accWrapper = accDto.getObject();
			// ReferenceDelta refDelta =
			// ReferenceDelta.createModificationDelete(UserType.F_ACCOUNT_REF,
			// userWrapper.getObject().getDefinition(), accWrapper.getObject());
			// refDeltas.add(refDelta);
			// }
			// }
			// if (!refDeltas.isEmpty()) {
			// forceDeleteDelta =
			// ObjectDelta.createModifyDelta(userWrapper.getObject().getOid(),
			// refDeltas,
			// UserType.class, getPrismContext());
			// }
			// PrismContainerDefinition def =
			// userWrapper.getObject().findContainer(UserType.F_ASSIGNMENT).getDefinition();
			// if (forceDeleteDelta == null) {
			// forceDeleteDelta =
			// ObjectDelta.createEmptyModifyDelta(UserType.class,
			// userWrapper.getObject().getOid(),
			// getPrismContext());
			// }
			try {
				ObjectDelta<UserType> forceDeleteDelta = getChange(userWrapper);
				forceDeleteDelta.revive(getPrismContext());

				if (forceDeleteDelta != null && !forceDeleteDelta.isEmpty()) {
					getModelService().executeChanges(WebMiscUtil.createDeltaCollection(forceDeleteDelta),
							options, task, result);
				}
			} catch (Exception ex) {
				result.recordFatalError("Failed to execute delete operation with force.");
				LoggingUtils.logException(LOGGER, "Failed to execute delete operation with force", ex);
				return false;
			}

			result.recomputeStatus();
			result.recordSuccessIfUnknown();
			return true;
		}
		return false;
	}

	private ObjectDelta getChange(ObjectWrapper focusWrapper) throws SchemaException {

		List<FocusProjectionDto> accountDtos = getFocusShadows();
		List<ReferenceDelta> refDeltas = new ArrayList<ReferenceDelta>();
		ObjectDelta<T> forceDeleteDelta = null;
		for (FocusProjectionDto accDto : accountDtos) {
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

		handleAssignmentDeltas(forceDeleteDelta, getFocusAssignments(), def);
		return forceDeleteDelta;
	}

	private Collection<SimpleValidationError> performCustomValidation(PrismObject<T> user,
			Collection<ObjectDelta<? extends ObjectType>> deltas) throws SchemaException {
		Collection<SimpleValidationError> errors = null;

		if (user == null) {
			if (getFocusWrapper() != null && getFocusWrapper().getObject() != null) {
				user = getFocusWrapper().getObject();

				for (ObjectDelta delta : deltas) {
					// because among deltas there can be also ShadowType deltas
					if (UserType.class.isAssignableFrom(delta.getObjectTypeClass())) { 
						delta.applyTo(user);
					}
				}
			}
		}

		if (user != null && user.asObjectable() != null) {
			for (AssignmentType assignment : user.asObjectable().getAssignment()) {
				for (MidpointFormValidator validator : getFormValidatorRegistry().getValidators()) {
					if (errors == null) {
						errors = validator.validateAssignment(assignment);
					} else {
						errors.addAll(validator.validateAssignment(assignment));
					}
				}
			}
		}

		for (MidpointFormValidator validator : getFormValidatorRegistry().getValidators()) {
			if (errors == null) {
				errors = validator.validateObject(user, deltas);
			} else {
				errors.addAll(validator.validateObject(user, deltas));
			}
		}

		return errors;
	}
	
	private <P extends ObjectType> List<P> prepareProjection(List<FocusProjectionDto> projections) throws SchemaException{
		List<P> projectionsToAdd = new ArrayList<>();
		for (FocusProjectionDto projection : projections) {
			if (!projection.isLoadedOK()) {
				continue;
			}

			if (!UserDtoStatus.ADD.equals(projection.getStatus())) {
				warn(getString("pageAdminFocus.message.illegalAccountState", projection.getStatus()));
				continue;
			}

			ObjectWrapper projectionWrapper = projection.getObject();
			ObjectDelta delta = projectionWrapper.getObjectDelta();
			PrismObject<P> proj = delta.getObjectToAdd();
			WebMiscUtil.encryptCredentials(proj, true, getMidpointApplication());

			projectionsToAdd.add(proj.asObjectable());
		}
		return projectionsToAdd;
	}

	protected void prepareFocusForAdd(PrismObject<T> focus) throws SchemaException {
		T focusType = focus.asObjectable();
		// handle added accounts
		
		List<ShadowType> shadowsToAdd = prepareProjection(getFocusShadows());
		if (!shadowsToAdd.isEmpty()){
			focusType.getLink().addAll(shadowsToAdd);
		}
		
		
		List<OrgType> orgsToAdd = prepareProjection(getFocusOrgs());
		if (!orgsToAdd.isEmpty()){
			focusType.getParentOrg().addAll(orgsToAdd);
		}

		handleAssignmentForAdd(focus, UserType.F_ASSIGNMENT, assignmentsModel.getObject());

		// PrismObjectDefinition userDef = focus.getDefinition();
		// PrismContainerDefinition assignmentDef =
		// userDef.findContainerDefinition(UserType.F_ASSIGNMENT);
		//
		// // handle added assignments
		// // existing user assignments are not relevant -> delete them
		// focusType.getAssignment().clear();
		// List<AssignmentEditorDto> assignments = getFocusAssignments();
		// for (AssignmentEditorDto assDto : assignments) {
		// if (UserDtoStatus.DELETE.equals(assDto.getStatus())) {
		// continue;
		// }
		//
		// AssignmentType assignment = new AssignmentType();
		// PrismContainerValue value = assDto.getNewValue();
		// assignment.setupContainerValue(value);
		// value.applyDefinition(assignmentDef, false);
		// focusType.getAssignment().add(assignment.clone());
		//
		// // todo remove this block [lazyman] after model is updated - it has
		// // to remove resource from accountConstruction
		// removeResourceFromAccConstruction(assignment);
		// }
	}

	protected void handleAssignmentForAdd(PrismObject<T> focus, QName containerName,
			List<AssignmentEditorDto> assignments) throws SchemaException {
		PrismObjectDefinition userDef = focus.getDefinition();
		PrismContainerDefinition assignmentDef = userDef.findContainerDefinition(containerName);

		// handle added assignments
		// existing user assignments are not relevant -> delete them
		PrismContainer<AssignmentType> assignmentContainer = focus.findOrCreateContainer(containerName);
		if (assignmentContainer != null && !assignmentContainer.isEmpty()){
			assignmentContainer.clear();
		}
		
//		List<AssignmentEditorDto> assignments = getFocusAssignments();
		for (AssignmentEditorDto assDto : assignments) {
			if (UserDtoStatus.DELETE.equals(assDto.getStatus())) {
				continue;
			}

			AssignmentType assignment = new AssignmentType();
			PrismContainerValue value = assDto.getNewValue();
			assignment.setupContainerValue(value);
			value.applyDefinition(assignmentDef, false);
			assignmentContainer.add(assignment.clone().asPrismContainerValue());

			// todo remove this block [lazyman] after model is updated - it has
			// to remove resource from accountConstruction
			removeResourceFromAccConstruction(assignment);
		}
	}

	private List<ObjectDelta<? extends ObjectType>> modifyShadows(OperationResult result) {
		List<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();

		List<FocusProjectionDto> accounts = getFocusShadows();
		OperationResult subResult = null;
		for (FocusProjectionDto account : accounts) {
			if (!account.isLoadedOK())
				continue;

			try {
				ObjectWrapper accountWrapper = account.getObject();
				ObjectDelta delta = accountWrapper.getObjectDelta();
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Account delta computed from form:\n{}",
							new Object[] { delta.debugDump(3) });
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

				WebMiscUtil.encryptCredentials(delta, true, getMidpointApplication());
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Modifying account:\n{}", new Object[] { delta.debugDump(3) });
				}

				deltas.add(delta);
				// subResult.recordSuccess();
			} catch (Exception ex) {
				// if (subResult != null) {
				result.recordFatalError("Couldn't compute account delta.", ex);
				// }
				LoggingUtils.logException(LOGGER, "Couldn't compute account delta", ex);
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

		List<FocusProjectionDto> accounts = getFocusShadows();
		for (FocusProjectionDto accDto : accounts) {
			if (accDto.isLoadedOK()) {
				ObjectWrapper accountWrapper = accDto.getObject();
				ObjectDelta delta = accountWrapper.getObjectDelta();
				PrismReferenceValue refValue = new PrismReferenceValue(null, OriginType.USER_ACTION, null);

				PrismObject<ShadowType> account;
				switch (accDto.getStatus()) {
					case ADD:
						account = delta.getObjectToAdd();
						WebMiscUtil.encryptCredentials(account, true, getMidpointApplication());
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

	
	private ReferenceDelta prepareUserOrgsDeltaForModify(PrismReferenceDefinition refDef)
			throws SchemaException {
		ReferenceDelta refDelta = new ReferenceDelta(refDef, getPrismContext());

		List<FocusProjectionDto> orgs = getFocusOrgs();
		for (FocusProjectionDto orgDto : orgs) {
			if (orgDto.isLoadedOK()) {
				ObjectWrapper orgWrapper = orgDto.getObject();
				ObjectDelta delta = orgWrapper.getObjectDelta();
				PrismReferenceValue refValue = new PrismReferenceValue(null, OriginType.USER_ACTION, null);

				PrismObject<OrgType> account;
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

	
	protected ContainerDelta handleAssignmentDeltas(ObjectDelta<T> focusDelta,
			List<AssignmentEditorDto> assignments, PrismContainerDefinition def) throws SchemaException {
		ContainerDelta assDelta = new ContainerDelta(new ItemPath(), def.getName(), def, getPrismContext());

		for (AssignmentEditorDto assDto : assignments) {
			PrismContainerValue newValue = assDto.getNewValue();

			switch (assDto.getStatus()) {
				case ADD:
					newValue.applyDefinition(def, false);
					assDelta.addValueToAdd(newValue.clone());
					break;
				case DELETE:
					PrismContainerValue oldValue = assDto.getOldValue();
					oldValue.applyDefinition(def);
					assDelta.addValueToDelete(oldValue.clone());
					break;
				case MODIFY:
					if (!assDto.isModified()) {
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

	private void handleModifyAssignmentDelta(AssignmentEditorDto assDto,
			PrismContainerDefinition assignmentDef, PrismContainerValue newValue, ObjectDelta<T> focusDelta)
					throws SchemaException {
		LOGGER.debug("Handling modified assignment '{}', computing delta.",
				new Object[] { assDto.getName() });

		PrismValue oldValue = assDto.getOldValue();
		Collection<? extends ItemDelta> deltas = oldValue.diff(newValue);

		for (ItemDelta delta : deltas) {
			ItemPath deltaPath = delta.getPath().rest();
			ItemDefinition deltaDef = assignmentDef.findItemDefinition(deltaPath);

			delta.setParentPath(joinPath(oldValue.getPath(), delta.getPath().allExceptLast()));
			delta.applyDefinition(deltaDef);

			focusDelta.addModification(delta);
		}
	}

	private ItemPath joinPath(ItemPath path, ItemPath deltaPath) {
		List<ItemPathSegment> newPath = new ArrayList<ItemPathSegment>();

		ItemPathSegment firstDeltaSegment = deltaPath != null ? deltaPath.first() : null;
		if (path != null) {
			for (ItemPathSegment seg : path.getSegments()) {
				if (seg.equivalent(firstDeltaSegment)) {
					break;
				}
				newPath.add(seg);
			}
		}
		if (deltaPath != null) {
			newPath.addAll(deltaPath.getSegments());
		}

		return new ItemPath(newPath);
	}

	protected void prepareFocusDeltaForModify(ObjectDelta<T> focusDelta) throws SchemaException {
		// handle accounts
		SchemaRegistry registry = getPrismContext().getSchemaRegistry();
		PrismObjectDefinition<T> objectDefinition = registry
				.findObjectDefinitionByCompileTimeClass(getCompileTimeClass());
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

		// handle assignments
		PrismContainerDefinition def = objectDefinition.findContainerDefinition(UserType.F_ASSIGNMENT);
		handleAssignmentDeltas(focusDelta, getFocusAssignments(), def);
	}

	protected void recomputeAssignmentsPerformed(AssignmentPreviewDialog dialog, AjaxRequestTarget target) {
		LOGGER.debug("Recompute user assignments");
		Task task = createSimpleTask(OPERATION_RECOMPUTE_ASSIGNMENTS);
		OperationResult result = new OperationResult(OPERATION_RECOMPUTE_ASSIGNMENTS);
		ObjectDelta<T> delta;
		Set<AssignmentsPreviewDto> assignmentDtoSet = new TreeSet<>();

		try {
			reviveModels();

			ObjectWrapper userWrapper = getFocusWrapper();
			delta = userWrapper.getObjectDelta();
			if (userWrapper.getOldDelta() != null) {
				delta = ObjectDelta.summarize(userWrapper.getOldDelta(), delta);
			}

			switch (userWrapper.getStatus()) {
				case ADDING:
					PrismObject<T> focus = delta.getObjectToAdd();
					prepareFocusForAdd(focus);
					getPrismContext().adopt(focus, getCompileTimeClass());

					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("Delta before add user:\n{}", new Object[] { delta.debugDump(3) });
					}

					if (!delta.isEmpty()) {
						delta.revive(getPrismContext());
					} else {
						result.recordSuccess();
					}

					break;
				case MODIFYING:
					prepareFocusDeltaForModify(delta);

					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("Delta before modify user:\n{}", new Object[] { delta.debugDump(3) });
					}

					List<ObjectDelta<? extends ObjectType>> accountDeltas = modifyShadows(result);
					Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();

					if (!delta.isEmpty()) {
						delta.revive(getPrismContext());
						deltas.add(delta);
					}

					for (ObjectDelta accDelta : accountDeltas) {
						if (!accDelta.isEmpty()) {
							accDelta.revive(getPrismContext());
							deltas.add(accDelta);
						}
					}

					break;
				default:
					error(getString("pageAdminFocus.message.unsupportedState", userWrapper.getStatus()));
			}

			ModelContext<UserType> modelContext = null;
			try {
				modelContext = getModelInteractionService()
						.previewChanges(WebMiscUtil.createDeltaCollection(delta), null, task, result);
			} catch (NoFocusNameSchemaException e) {
				info(getString("pageAdminFocus.message.noUserName"));
				target.add(getFeedbackPanel());
				return;
			}

			DeltaSetTriple<? extends EvaluatedAssignment> evaluatedAssignmentTriple = modelContext
					.getEvaluatedAssignmentTriple();
			Collection<? extends EvaluatedAssignment> evaluatedAssignments = evaluatedAssignmentTriple
					.getNonNegativeValues();

			if (evaluatedAssignments.isEmpty()) {
				info(getString("pageAdminFocus.message.noAssignmentsAvailable"));
				target.add(getFeedbackPanel());
				return;
			}

			List<String> directAssignmentsOids = new ArrayList<>();
			for (EvaluatedAssignment<UserType> evaluatedAssignment : evaluatedAssignments) {
				if (!evaluatedAssignment.isValid()) {
					continue;
				}
				// roles and orgs
				DeltaSetTriple<? extends EvaluatedAbstractRole> evaluatedRolesTriple = evaluatedAssignment
						.getRoles();
				Collection<? extends EvaluatedAbstractRole> evaluatedRoles = evaluatedRolesTriple
						.getNonNegativeValues();
				for (EvaluatedAbstractRole role : evaluatedRoles) {
					if (role.isEvaluateConstructions()) {
						assignmentDtoSet.add(createAssignmentsPreviewDto(role, task, result));
					}
				}

				// all resources
				DeltaSetTriple<EvaluatedConstruction> evaluatedConstructionsTriple = evaluatedAssignment
						.getEvaluatedConstructions(task, result);
				Collection<EvaluatedConstruction> evaluatedConstructions = evaluatedConstructionsTriple
						.getNonNegativeValues();
				for (EvaluatedConstruction construction : evaluatedConstructions) {
					assignmentDtoSet.add(createAssignmentsPreviewDto(construction));
				}
			}

			dialog.updateData(target, new ArrayList<>(assignmentDtoSet), directAssignmentsOids);
			dialog.show(target);

		} catch (Exception e) {
			LoggingUtils.logUnexpectedException(LOGGER, "Could not create assignments preview.", e);
			error("Could not create assignments preview. Reason: " + e);
			target.add(getFeedbackPanel());
		}
	}

	private AssignmentsPreviewDto createAssignmentsPreviewDto(EvaluatedAbstractRole evaluatedAbstractRole,
			Task task, OperationResult result) {
		AssignmentsPreviewDto dto = new AssignmentsPreviewDto();
		PrismObject<? extends AbstractRoleType> role = evaluatedAbstractRole.getRole();
		dto.setTargetOid(role.getOid());
		dto.setTargetName(getNameToDisplay(role));
		dto.setTargetDescription(role.asObjectable().getDescription());
		dto.setTargetClass(role.getCompileTimeClass());
		dto.setDirect(evaluatedAbstractRole.isDirectlyAssigned());
		if (evaluatedAbstractRole.getAssignment() != null) {
			if (evaluatedAbstractRole.getAssignment().getTenantRef() != null) {
				dto.setTenantName(nameFromReference(evaluatedAbstractRole.getAssignment().getTenantRef(),
						task, result));
			}
			if (evaluatedAbstractRole.getAssignment().getOrgRef() != null) {
				dto.setOrgRefName(
						nameFromReference(evaluatedAbstractRole.getAssignment().getOrgRef(), task, result));
			}
		}
		return dto;
	}

	private String getNameToDisplay(PrismObject<? extends AbstractRoleType> role) {
		String n = PolyString.getOrig(role.asObjectable().getDisplayName());
		if (StringUtils.isNotBlank(n)) {
			return n;
		}
		return PolyString.getOrig(role.asObjectable().getName());
	}

	private String nameFromReference(ObjectReferenceType reference, Task task, OperationResult result) {
		String oid = reference.getOid();
		QName type = reference.getType();
		Class<? extends ObjectType> clazz = getPrismContext().getSchemaRegistry().getCompileTimeClass(type);
		PrismObject<? extends ObjectType> prismObject;
		try {
			prismObject = getModelService().getObject(clazz, oid,
					SelectorOptions.createCollection(GetOperationOptions.createNoFetch()), task, result);
		} catch (ObjectNotFoundException | SchemaException | SecurityViolationException
				| CommunicationException | ConfigurationException | RuntimeException e) {
			LoggingUtils.logUnexpectedException(LOGGER, "Couldn't retrieve name for {}: {}", e,
					clazz.getSimpleName(), oid);
			return "Couldn't retrieve name for " + oid;
		}
		ObjectType object = prismObject.asObjectable();
		if (object instanceof AbstractRoleType) {
			return getNameToDisplay(object.asPrismObject());
		} else {
			return PolyString.getOrig(object.getName());
		}
	}

	private AssignmentsPreviewDto createAssignmentsPreviewDto(EvaluatedConstruction evaluatedConstruction) {
		AssignmentsPreviewDto dto = new AssignmentsPreviewDto();
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

	private List<FocusProjectionDto> getSelectedAccounts() {
		List<FocusProjectionDto> selected = new ArrayList<FocusProjectionDto>();

		List<FocusProjectionDto> all = shadowModel.getObject();
		for (FocusProjectionDto shadow : all) {
			if (shadow.isLoadedOK() && shadow.getObject().isSelected()) {
				selected.add(shadow);
			}
		}

		return selected;
	}

	private List<AssignmentEditorDto> getSelectedAssignments() {
		List<AssignmentEditorDto> selected = new ArrayList<AssignmentEditorDto>();

		List<AssignmentEditorDto> all = assignmentsModel.getObject();
		for (AssignmentEditorDto wrapper : all) {
			if (wrapper.isSelected()) {
				selected.add(wrapper);
			}
		}

		return selected;
	}

	private void addSelectedResourceAssignPerformed(ResourceType resource) {
		AssignmentType assignment = new AssignmentType();
		ConstructionType construction = new ConstructionType();
		assignment.setConstruction(construction);

		try {
			getPrismContext().adopt(assignment, getCompileTimeClass(), new ItemPath(FocusType.F_ASSIGNMENT));
		} catch (SchemaException e) {
			error(getString("Could not create assignment", resource.getName(), e.getMessage()));
			LoggingUtils.logException(LOGGER, "Couldn't create assignment", e);
			return;
		}

		construction.setResource(resource);

		List<AssignmentEditorDto> assignments = assignmentsModel.getObject();
		AssignmentEditorDto dto = new AssignmentEditorDto(UserDtoStatus.ADD, assignment, this);
		assignments.add(dto);

		dto.setMinimized(false);
		dto.setShowEmpty(true);
	}

	private void addSelectedAssignablePerformed(AjaxRequestTarget target, List<ObjectType> newAssignables,
			String popupId) {
		ModalWindow window = (ModalWindow) get(popupId);
		window.close(target);

		if (newAssignables.isEmpty()) {
			warn(getString("pageAdminFocus.message.noAssignableSelected"));
			target.add(getFeedbackPanel());
			return;
		}

		List<AssignmentEditorDto> assignments = assignmentsModel.getObject();
		for (ObjectType object : newAssignables) {
			try {
				if (object instanceof ResourceType) {
					addSelectedResourceAssignPerformed((ResourceType) object);
					continue;
				}

				AssignmentEditorDtoType aType = AssignmentEditorDtoType.getType(object.getClass());

				ObjectReferenceType targetRef = new ObjectReferenceType();
				targetRef.setOid(object.getOid());
				targetRef.setType(aType.getQname());
				targetRef.setTargetName(object.getName());

				AssignmentType assignment = new AssignmentType();
				assignment.setTargetRef(targetRef);

				AssignmentEditorDto dto = new AssignmentEditorDto(UserDtoStatus.ADD, assignment, this);
				dto.setMinimized(false);
				dto.setShowEmpty(true);

				assignments.add(dto);
			} catch (Exception ex) {
				error(getString("pageAdminFocus.message.couldntAssignObject", object.getName(),
						ex.getMessage()));
				LoggingUtils.logException(LOGGER, "Couldn't assign object", ex);
			}
		}

		target.add(getFeedbackPanel(), get(createComponentPath(ID_MAIN_FORM, ID_ASSIGNMENTS)));
	}

	private void updateShadowActivation(AjaxRequestTarget target, List<FocusProjectionDto> accounts,
			boolean enabled) {
		if (!isAnyAccountSelected(target)) {
			return;
		}

		for (FocusProjectionDto account : accounts) {
			if (!account.isLoadedOK()) {
				continue;
			}

			ObjectWrapper wrapper = account.getObject();
			ContainerWrapper activation = wrapper.findContainerWrapper(new ItemPath(ShadowType.F_ACTIVATION));
			if (activation == null) {
				warn(getString("pageAdminFocus.message.noActivationFound", wrapper.getDisplayName()));
				continue;
			}

			PropertyWrapper enabledProperty = (PropertyWrapper) activation
					.findPropertyWrapper(ActivationType.F_ADMINISTRATIVE_STATUS);
			if (enabledProperty == null || enabledProperty.getValues().size() != 1) {
				warn(getString("pageAdminFocus.message.noEnabledPropertyFound", wrapper.getDisplayName()));
				continue;
			}
			ValueWrapper value = enabledProperty.getValues().get(0);
			ActivationStatusType status = enabled ? ActivationStatusType.ENABLED
					: ActivationStatusType.DISABLED;
			((PrismPropertyValue) value.getValue()).setValue(status);

			wrapper.setSelected(false);
		}

		target.add(getFeedbackPanel(), get(createComponentPath(ID_MAIN_FORM, ID_SHADOWS)));
	}

	private boolean isAnyAccountSelected(AjaxRequestTarget target) {
		List<FocusProjectionDto> selected = getSelectedAccounts();
		if (selected.isEmpty()) {
			warn(getString("pageAdminFocus.message.noAccountSelected"));
			target.add(getFeedbackPanel());
			return false;
		}

		return true;
	}

	private void deleteShadowPerformed(AjaxRequestTarget target) {
		if (!isAnyAccountSelected(target)) {
			return;
		}

		showModalWindow(MODAL_ID_CONFIRM_DELETE_SHADOW, target);
	}

	private void showModalWindow(String id, AjaxRequestTarget target) {
		ModalWindow window = (ModalWindow) get(id);
		window.show(target);
		target.add(getFeedbackPanel());
	}

	private void deleteAccountConfirmedPerformed(AjaxRequestTarget target,
			List<FocusProjectionDto> selected) {
		List<FocusProjectionDto> accounts = shadowModel.getObject();
		for (FocusProjectionDto account : selected) {
			if (UserDtoStatus.ADD.equals(account.getStatus())) {
				accounts.remove(account);
			} else {
				account.setStatus(UserDtoStatus.DELETE);
			}
		}
		target.add(get(createComponentPath(ID_MAIN_FORM, ID_SHADOWS)));
	}

	private void deleteAssignmentConfirmedPerformed(AjaxRequestTarget target,
			List<AssignmentEditorDto> selected) {
		List<AssignmentEditorDto> assignments = assignmentsModel.getObject();
		for (AssignmentEditorDto assignment : selected) {
			if (UserDtoStatus.ADD.equals(assignment.getStatus())) {
				assignments.remove(assignment);
			} else {
				assignment.setStatus(UserDtoStatus.DELETE);
				assignment.setSelected(false);
			}
		}

		target.add(getFeedbackPanel(), get(createComponentPath(ID_MAIN_FORM, ID_ASSIGNMENTS)));
	}

	private void unlinkShadowPerformed(AjaxRequestTarget target, List<FocusProjectionDto> selected) {
		if (!isAnyAccountSelected(target)) {
			return;
		}

		for (FocusProjectionDto account : selected) {
			if (UserDtoStatus.ADD.equals(account.getStatus())) {
				continue;
			}
			account.setStatus(UserDtoStatus.UNLINK);
		}
		target.add(get(createComponentPath(ID_MAIN_FORM, ID_SHADOWS)));
	}

	private void unlockShadowPerformed(AjaxRequestTarget target, List<FocusProjectionDto> selected) {
		if (!isAnyAccountSelected(target)) {
			return;
		}

		for (FocusProjectionDto account : selected) {
			// TODO: implement unlock
		}
	}

	private void deleteAssignmentPerformed(AjaxRequestTarget target) {
		List<AssignmentEditorDto> selected = getSelectedAssignments();
		if (selected.isEmpty()) {
			warn(getString("pageAdminFocus.message.noAssignmentSelected"));
			target.add(getFeedbackPanel());
			return;
		}

		showModalWindow(MODAL_ID_CONFIRM_DELETE_ASSIGNMENT, target);
	}

	private void initButtons(final Form mainForm) {
		AjaxSubmitButton saveButton = new AjaxSubmitButton("save",
				createStringResource("pageAdminFocus.button.save")) {

			@Override
			protected void onSubmit(AjaxRequestTarget target,
					org.apache.wicket.markup.html.form.Form<?> form) {
				progressReporter.onSaveSubmit();
				savePerformed(target);
			}

			@Override
			protected void onError(AjaxRequestTarget target,
					org.apache.wicket.markup.html.form.Form<?> form) {
				target.add(getFeedbackPanel());
			}
		};
		progressReporter.registerSaveButton(saveButton);
		mainForm.setDefaultButton(saveButton);
		mainForm.add(saveButton);

		AjaxSubmitButton abortButton = new AjaxSubmitButton("abort",
				createStringResource("pageAdminFocus.button.abort")) {

			@Override
			protected void onSubmit(AjaxRequestTarget target,
					org.apache.wicket.markup.html.form.Form<?> form) {
				progressReporter.onAbortSubmit(target);
			}

			@Override
			protected void onError(AjaxRequestTarget target,
					org.apache.wicket.markup.html.form.Form<?> form) {
				target.add(getFeedbackPanel());
			}
		};
		progressReporter.registerAbortButton(abortButton);
		mainForm.add(abortButton);

		// AjaxSubmitButton recomputeAssignments = new
		// AjaxSubmitButton(ID_BUTTON_RECOMPUTE_ASSIGNMENTS,
		// createStringResource("pageAdminFocus.button.recompute.assignments"))
		// {
		//
		// @Override
		// protected void onSubmit(AjaxRequestTarget target,
		// org.apache.wicket.markup.html.form.Form<?> form) {
		// recomputeAssignmentsPerformed(target);
		// }
		//
		// @Override
		// protected void onError(AjaxRequestTarget target,
		// org.apache.wicket.markup.html.form.Form<?> form) {
		// target.add(getFeedbackPanel());
		// }
		// };
		// mainForm.add(recomputeAssignments);

		AjaxButton back = new AjaxButton("back", createStringResource("pageAdminFocus.button.back")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				setSpecificResponsePage();
			}
		};
		mainForm.add(back);

	}

	protected ExecuteChangeOptionsPanel initOptions(final Form mainForm) {
		ExecuteChangeOptionsPanel optionsPanel = new ExecuteChangeOptionsPanel(ID_EXECUTE_OPTIONS,
				executeOptionsModel, true, false);
		mainForm.add(optionsPanel);
		return optionsPanel;
	}

	private void initConfirmationDialogs() {
		ConfirmationDialog dialog = new ConfirmationDialog(MODAL_ID_CONFIRM_DELETE_SHADOW,
				createStringResource("pageAdminFocus.title.confirmDelete"),
				new AbstractReadOnlyModel<String>() {

					@Override
					public String getObject() {
						return createStringResource("pageAdminFocus.message.deleteAccountConfirm",
								getSelectedAccounts().size()).getString();
					}
				}) {

			@Override
			public void yesPerformed(AjaxRequestTarget target) {
				close(target);
				deleteAccountConfirmedPerformed(target, getSelectedAccounts());
			}
		};
		add(dialog);

		dialog = new ConfirmationDialog(MODAL_ID_CONFIRM_DELETE_ASSIGNMENT,
				createStringResource("pageAdminFocus.title.confirmDelete"),
				new AbstractReadOnlyModel<String>() {

					@Override
					public String getObject() {
						return createStringResource("pageAdminFocus.message.deleteAssignmentConfirm",
								getSelectedAssignments().size()).getString();
					}
				}) {

			@Override
			public void yesPerformed(AjaxRequestTarget target) {
				close(target);
				deleteAssignmentConfirmedPerformed(target, getSelectedAssignments());
			}
		};
		add(dialog);

		// TODO: uncoment later -> check for unsaved changes
		// dialog = new ConfirmationDialog(MODAL_ID_CONFIRM_CANCEL,
		// createStringResource("pageUser.title.confirmCancel"), new
		// AbstractReadOnlyModel<String>() {
		//
		// @Override
		// public String getObject() {
		// return createStringResource("pageUser.message.cancelConfirm",
		// getSelectedAssignments().size()).getString();
		// }
		// }) {
		//
		// @Override
		// public void yesPerformed(AjaxRequestTarget target) {
		// close(target);
		// setResponsePage(PageUsers.class);
		// // deleteAssignmentConfirmedPerformed(target,
		// getSelectedAssignments());
		// }
		// };
		// add(dialog);
	}

	protected abstract void setSpecificResponsePage();

}

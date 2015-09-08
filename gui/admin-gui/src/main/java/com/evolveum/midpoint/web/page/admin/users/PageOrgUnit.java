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

package com.evolveum.midpoint.web.page.admin.users;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.string.StringValue;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.query.InOidFilter;
import com.evolveum.midpoint.prism.query.NotFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.AuthorizationAction;
import com.evolveum.midpoint.web.application.PageDescriptor;
import com.evolveum.midpoint.web.component.AjaxButton;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.assignment.AssignmentTableDto;
import com.evolveum.midpoint.web.component.assignment.AssignmentTablePanel;
import com.evolveum.midpoint.web.component.form.multivalue.MultiValueChoosePanel;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.prism.CheckTableHeader;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.ContainerWrapper;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.PrismObjectPanel;
import com.evolveum.midpoint.web.component.prism.PropertyWrapper;
import com.evolveum.midpoint.web.component.prism.SimpleErrorPanel;
import com.evolveum.midpoint.web.component.progress.ProgressReporter;
import com.evolveum.midpoint.web.component.progress.ProgressReportingAwarePage;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.ObjectWrapperUtil;
import com.evolveum.midpoint.web.page.PageTemplate;
import com.evolveum.midpoint.web.page.admin.users.component.ExecuteChangeOptionsDto;
import com.evolveum.midpoint.web.page.admin.users.component.ExecuteChangeOptionsPanel;
import com.evolveum.midpoint.web.page.admin.users.dto.FocusShadowDto;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.web.resource.img.ImgResources;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.web.util.WebModelUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author lazyman
 */
@PageDescriptor(url = "/admin/org/unit", encoder = OnePageParameterEncoder.class, action = {
		@AuthorizationAction(actionUri = PageAdminUsers.AUTH_ORG_ALL, label = PageAdminUsers.AUTH_ORG_ALL_LABEL, description = PageAdminUsers.AUTH_ORG_ALL_DESCRIPTION),
		@AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_ORG_UNIT_URL, label = "PageOrgUnit.auth.orgUnit.label", description = "PageOrgUnit.auth.orgUnit.description") })
public class PageOrgUnit extends PageAdminUsers implements ProgressReportingAwarePage {

	private static final Trace LOGGER = TraceManager.getTrace(PageOrgUnit.class);
	private static final String DOT_CLASS = PageOrgUnit.class.getName() + ".";
	private static final String LOAD_UNIT = DOT_CLASS + "loadOrgUnit";
	private static final String SAVE_UNIT = DOT_CLASS + "saveOrgUnit";
	private static final String LOAD_PARENT_UNITS = DOT_CLASS + "loadParentOrgUnits";
	private static final String OPERATION_LOAD_EXTENSION_WRAPPER = "loadExtensionWrapper";

	private static final String ID_LABEL_SIZE = "col-md-4";
	private static final String ID_INPUT_SIZE = "col-md-6";

	private static final String ID_FORM = "form";
	private static final String ID_NAME = "name";
	private static final String ID_DISPLAY_NAME = "displayName";
	private static final String ID_DESCRIPTION = "description";
	private static final String ID_REQUESTABLE = "requestable";
	private static final String ID_TENANT = "tenant";
	private static final String ID_IDENTIFIER = "identifier";
	private static final String ID_COST_CENTER = "costCenter";
	private static final String ID_LOCALITY = "locality";
	private static final String ID_MAIL_DOMAIN = "mailDomain";
	private static final String ID_ADMINISTRATIVE_STATUS = "administrativeStatus";
	private static final String ID_VALID_FROM = "validFrom";
	private static final String ID_VALID_TO = "validTo";
	private static final String ID_PARENT_ORG_UNITS = "parentOrgUnits";
	private static final String ID_ORG_TYPE = "orgType";
	private static final String ID_ORG_FORM = "orgForm";
	private static final String ID_BACK = "back";
	private static final String ID_SAVE = "save";
	private static final String ID_EXECUTE_OPTIONS = "executeOptions";

	private static final String ID_ASSIGNMENTS_TABLE = "assignmentsPanel";
	private static final String ID_INDUCEMENTS_TABLE = "inducementsPanel";
	private static final String ID_EXTENSION_LABEL = "extensionLabel";
	private static final String ID_EXTENSION = "extension";
	private static final String ID_EXTENSION_PROPERTY = "property";

	private static final String ID_ACCOUNT_LIST = "accountList";
	private static final String ID_ACCOUNTS = "accounts";
	private static final String ID_ACCOUNT_MENU = "accountMenu";
	private static final String ID_ACCOUNT_CHECK_ALL = "accountCheckAll";

	// private ContainerStatus status;
	private LoadableModel<ObjectWrapper> orgModel;
//	private IModel<List<OrgType>> parentOrgUnitsModel;  //class="form-group" 
	private IModel<List<PrismPropertyValue>> orgTypeModel;
	private IModel<List<PrismPropertyValue>> orgMailDomainModel;
	private IModel<ContainerWrapper> extensionModel;

	private LoadableModel<List<FocusShadowDto>> shadowsModel;
	private ObjectWrapper orgWrapper;

	private ProgressReporter progressReporter;
	private ObjectDelta delta;

	private LoadableModel<ExecuteChangeOptionsDto> executeOptionsModel = new LoadableModel<ExecuteChangeOptionsDto>(
			false) {

		@Override
		protected ExecuteChangeOptionsDto load() {
			return new ExecuteChangeOptionsDto();
		}
	};

	public PageOrgUnit() {
		initialize(null);
	}

	// todo improve [erik]
	public PageOrgUnit(final PrismObject<OrgType> unitToEdit) {
		initialize(unitToEdit);
	}

	public PageOrgUnit(PageParameters parameters, PageTemplate previousPage) {
		getPageParameters().overwriteWith(parameters);
		setPreviousPage(previousPage);
		initialize(null);
	}

	protected void initialize(final PrismObject<OrgType> unitToEdit) {
		orgModel = new LoadableModel<ObjectWrapper>(false) {

			@Override
			protected ObjectWrapper load() {
				return loadOrgUnitWrapper(unitToEdit);
			}
		};

//		parentOrgUnitsModel = new LoadableModel<List<OrgType>>(false) {
//
//			@Override
//			protected List<OrgType> load() {
//				return loadParentOrgUnits();
//			}
//		};

		shadowsModel = new LoadableModel<List<FocusShadowDto>>(false) {

			@Override
			protected List<FocusShadowDto> load() {
				return loadShadowWrappers();
			}
		};

		initLayout();
	}

	@Override
	protected IModel<String> createPageTitleModel() {
		return new LoadableModel<String>(false) {

			@Override
			protected String load() {
				if (!isEditingOrgUnit()) {
					return PageOrgUnit.super.createPageTitleModel().getObject();
				}

				String name = WebMiscUtil.getName(orgModel.getObject().getObject());
				return new StringResourceModel("page.title.edit", PageOrgUnit.this, null, null, name)
						.getString();
			}
		};
	}

	private List<FocusShadowDto> loadShadowWrappers() {
		List<FocusShadowDto> list = new ArrayList<FocusShadowDto>();

		ObjectWrapper orgWrapper = orgModel.getObject();
		PrismObject<OrgType> prismUser = orgWrapper.getObject();
		List<ObjectReferenceType> references = prismUser.asObjectable().getLinkRef();

		String OPERATION_LOAD_ACCOUNT = "Operation load shadows";
		Task task = createSimpleTask(OPERATION_LOAD_ACCOUNT);
		for (ObjectReferenceType reference : references) {
			OperationResult subResult = new OperationResult(OPERATION_LOAD_ACCOUNT);
			try {
				Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(
						ShadowType.F_RESOURCE, GetOperationOptions.createResolve());

				if (reference.getOid() == null) {
					continue;
				}

				PrismObject<ShadowType> account = getModelService().getObject(ShadowType.class,
						reference.getOid(), options, task, subResult);
				ShadowType accountType = account.asObjectable();

				OperationResultType fetchResult = accountType.getFetchResult();

				ResourceType resource = accountType.getResource();
				String resourceName = WebMiscUtil.getName(resource);

				StringBuilder description = new StringBuilder();
				if (accountType.getIntent() != null) {
					description.append(accountType.getIntent()).append(", ");
				}
				description.append(WebMiscUtil.getOrigStringFromPoly(accountType.getName()));

				ObjectWrapper wrapper = ObjectWrapperUtil.createObjectWrapper(resourceName,
						description.toString(), account, ContainerStatus.MODIFYING, true, this);
				wrapper.setFetchResult(OperationResult.createOperationResult(fetchResult));
				wrapper.setSelectable(true);
				wrapper.setMinimalized(true);

				PrismContainer<ShadowAssociationType> associationContainer = account
						.findContainer(ShadowType.F_ASSOCIATION);
				if (associationContainer != null && associationContainer.getValues() != null) {
					List<PrismProperty> associations = new ArrayList<>(associationContainer.getValues()
							.size());
					for (PrismContainerValue associationVal : associationContainer.getValues()) {
						ShadowAssociationType associationType = (ShadowAssociationType) associationVal
								.asContainerable();
						// we can safely eliminate fetching from resource,
						// because we need only the name
						PrismObject<ShadowType> association = getModelService().getObject(ShadowType.class,
								associationType.getShadowRef().getOid(),
								SelectorOptions.createCollection(GetOperationOptions.createNoFetch()), task,
								subResult);
						associations.add(association.findProperty(ShadowType.F_NAME));
					}

					wrapper.setAssociations(associations);

				}

				wrapper.initializeContainers(this);

				list.add(new FocusShadowDto(wrapper, UserDtoStatus.MODIFY));

				subResult.recomputeStatus();
			} catch (ObjectNotFoundException ex) {
				// this is fix for MID-854, full user/accounts/assignments
				// reload if accountRef reference is broken
				// because consistency already fixed it.

				// orgModel.reset();
				// shadowsModel.reset();
				// assignmentsModel.reset();
			} catch (Exception ex) {
				subResult.recordFatalError("Couldn't load account." + ex.getMessage(), ex);
				LoggingUtils.logException(LOGGER, "Couldn't load account", ex);
				list.add(new FocusShadowDto(false, getResourceName(reference.getOid()), subResult));
			} finally {
				subResult.computeStatus();
			}
		}

		return list;
	}

	private String getResourceName(String oid) {
		String OPERATION_SEARCH_RESOURCE = PageOrgUnit.class.getName() + ".searchAccountResource";
		OperationResult result = new OperationResult(OPERATION_SEARCH_RESOURCE);
		Task task = createSimpleTask(OPERATION_SEARCH_RESOURCE);

		try {
			Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions
					.createCollection(GetOperationOptions.createRaw());

			PrismObject<ShadowType> shadow = getModelService().getObject(ShadowType.class, oid, options,
					task, result);
			PrismObject<ResourceType> resource = getModelService().getObject(ResourceType.class,
					shadow.asObjectable().getResourceRef().getOid(), null, task, result);

			if (resource != null) {
				return WebMiscUtil.getOrigStringFromPoly(resource.asObjectable().getName());
			}
		} catch (Exception e) {
			result.recordFatalError("Account Resource was not found. " + e.getMessage());
			LoggingUtils.logException(LOGGER, "Account Resource was not found.", e);
			showResult(result);
		}

		return "-";
	}

	private void initLayout() {
		final Form form = new Form(ID_FORM);
		add(form);

		progressReporter = ProgressReporter.create(this, form, "progressPanel");

		PrismObjectPanel orgForm = new PrismObjectPanel(ID_ORG_FORM, orgModel, new PackageResourceReference(
				ImgResources.class, ImgResources.USER_PRISM), form, this) {

			@Override
			protected IModel<String> createDescription(IModel<ObjectWrapper> model) {
				return createStringResource("pageUser.description");
			}
		};
		form.add(orgForm);

//		MultiValueChoosePanel parentOrgType = initParentOrgUnit();
//		form.add(parentOrgType);

		AssignmentTablePanel assignments = initAssignments();
		form.add(assignments);

		AssignmentTablePanel inducements = initInducements();
		form.add(inducements);

		WebMarkupContainer accounts = new WebMarkupContainer(ID_ACCOUNTS);
		accounts.setOutputMarkupId(true);
		form.add(accounts);
		initProjections(accounts);
		initButtons(form);
	}

	private AssignmentTablePanel initInducements() {
		AssignmentTablePanel inducements = new AssignmentTablePanel(ID_INDUCEMENTS_TABLE,
				new Model<AssignmentTableDto>(), createStringResource("PageOrgUnit.title.inducements")) {

			@Override
			public List<AssignmentType> getAssignmentTypeList() {
				return ((AbstractRoleType) orgModel.getObject().getObject().asObjectable()).getInducement();
			}

			@Override
			public String getExcludeOid() {
				return orgModel.getObject().getObject().asObjectable().getOid();
			}
		};
		return inducements;
	}

	private AssignmentTablePanel initAssignments() {
		AssignmentTablePanel assignments = new AssignmentTablePanel(ID_ASSIGNMENTS_TABLE,
				new Model<AssignmentTableDto>(), createStringResource("PageOrgUnit.title.assignments")) {

			@Override
			public List<AssignmentType> getAssignmentTypeList() {
				return ((FocusType) orgModel.getObject().getObject().asObjectable()).getAssignment();
			}

			@Override
			public String getExcludeOid() {
				return orgModel.getObject().getObject().asObjectable().getOid();
			}
		};

		return assignments;
	}

//	private MultiValueChoosePanel initParentOrgUnit() {
//		MultiValueChoosePanel parentOrgType = new MultiValueChoosePanel<OrgType>(ID_PARENT_ORG_UNITS,
//				parentOrgUnitsModel, createStringResource("PageOrgUnit.parentOrgRef"), ID_LABEL_SIZE,
//				ID_INPUT_SIZE, false, OrgType.class) {
//
//			@Override
//			protected IModel<String> createTextModel(final IModel model) {
//				return new AbstractReadOnlyModel<String>() {
//
//					@Override
//					public String getObject() {
//						OrgType org = (OrgType) model.getObject();
//
//						return org == null ? null : WebMiscUtil.getOrigStringFromPoly(org.getName());
//					}
//				};
//			}
//
//			@Override
//			protected OrgType createNewEmptyItem() {
//				return new OrgType();
//			}
//
//			@Override
//			protected ObjectQuery createChooseQuery() {
//				ArrayList<String> oidList = new ArrayList<>();
//				ObjectQuery query = new ObjectQuery();
//
//				for (OrgType org : parentOrgUnitsModel.getObject()) {
//					if (org != null) {
//						if (org.getOid() != null && !org.getOid().isEmpty()) {
//							oidList.add(org.getOid());
//						}
//					}
//				}
//
//				if (isEditingOrgUnit()) {
//					oidList.add(orgModel.getObject().getObject().asObjectable().getOid());
//				}
//
//				if (oidList.isEmpty()) {
//					return null;
//				}
//
//				ObjectFilter oidFilter = InOidFilter.createInOid(oidList);
//				query.setFilter(NotFilter.createNot(oidFilter));
//
//				return query;
//			}
//
//			@Override
//			protected void replaceIfEmpty(Object object) {
//
//				boolean added = false;
//
//				List<OrgType> parents = parentOrgUnitsModel.getObject();
//				for (OrgType org : parents) {
//					if (WebMiscUtil.getName(org) == null || WebMiscUtil.getName(org).isEmpty()) {
//						parents.remove(org);
//						parents.add((OrgType) object);
//						added = true;
//						break;
//					}
//				}
//
//				if (!added) {
//					parents.add((OrgType) object);
//				}
//			}
//		};
//
//		return parentOrgType;
//	}

	private IModel<String> createStyleClassModel(final IModel<PropertyWrapper> wrapper) {
		return new AbstractReadOnlyModel<String>() {

			@Override
			public String getObject() {
				PropertyWrapper property = wrapper.getObject();
				return property.isVisible() ? "visible" : null;
			}
		};
	}

	private void initButtons(Form form) {
		AjaxSubmitButton save = new AjaxSubmitButton(ID_SAVE, createStringResource("PageBase.button.save")) {

			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
				progressReporter.onSaveSubmit();
				savePerformed(target);
			}

			@Override
			protected void onError(AjaxRequestTarget target, Form<?> form) {
				target.add(form);
				target.add(getFeedbackPanel());
			}
		};
		progressReporter.registerSaveButton(save);
		form.add(save);

		AjaxSubmitButton abortButton = new AjaxSubmitButton("abort",
				createStringResource("PageBase.button.abort")) {

			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
				progressReporter.onAbortSubmit(target);
			}

			@Override
			protected void onError(AjaxRequestTarget target, Form<?> form) {
				target.add(getFeedbackPanel());
			}
		};
		progressReporter.registerAbortButton(abortButton);
		form.add(abortButton);

		AjaxButton back = new AjaxButton(ID_BACK, createStringResource("PageBase.button.back")) {

			@Override
			public void onClick(AjaxRequestTarget target) {
				setSpecificResponsePage();
			}
		};
		form.add(back);

		form.add(new ExecuteChangeOptionsPanel(ID_EXECUTE_OPTIONS, executeOptionsModel, true, false)); // TODO
																										// add
																										// "show reconcile affected"
																										// when
																										// implemented
																										// for
																										// Orgs
	}

	private boolean isEditingOrgUnit() {
		StringValue oid = getPageParameters().get(OnePageParameterEncoder.PARAMETER);
		return oid != null && StringUtils.isNotEmpty(oid.toString());
	}

	private void setSpecificResponsePage() {
		goBack(PageOrgTree.class);
	}

//	private boolean isRefInParentOrgModel(ObjectReferenceType reference) {
//		for (OrgType parent : parentOrgUnitsModel.getObject()) {
//			if (reference.getOid().equals(parent.getOid())) {
//				return true;
//			}
//		}
//		return false;
//	}

	private boolean isOrgParent(OrgType unit, List<ObjectReferenceType> parentList) {
		for (ObjectReferenceType parentRef : parentList) {
			if (unit.getOid().equals(parentRef.getOid())) {
				return true;
			}
		}

		return false;
	}

	private void savePerformed(AjaxRequestTarget target) {
		LOGGER.debug("Save user.");
		OperationResult result = new OperationResult(SAVE_UNIT);
		ObjectWrapper orgWrapper = orgModel.getObject();
		// todo: improve, delta variable is quickfix for MID-1006
		// redirecting to user list page everytime user is created in repository
		// during user add in gui,
		// and we're not taking care about account/assignment create errors
		// (error message is still displayed)
		delta = null;

		Task task = createSimpleTask(SAVE_UNIT);
		ExecuteChangeOptionsDto executeOptions = executeOptionsModel.getObject();
		ModelExecuteOptions options = executeOptions.createOptions();
		LOGGER.debug("Using options {}.", new Object[] { executeOptions });

		try {
			reviveModels();

			delta = orgWrapper.getObjectDelta();
			if (orgWrapper.getOldDelta() != null) {
				delta = ObjectDelta.summarize(orgWrapper.getOldDelta(), delta);
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

		switch (orgWrapper.getStatus()) {
			case ADDING:
				try {
					PrismObject<OrgType> org = delta.getObjectToAdd();
					// WebMiscUtil.encryptCredentials(org, true,
					// getMidpointApplication());
					prepareOrgForAdd(org);
					getPrismContext().adopt(org, OrgType.class);
					if (LOGGER.isTraceEnabled()) {
						LOGGER.trace("Delta before add user:\n{}", new Object[] { delta.debugDump(3) });
					}

					if (!delta.isEmpty()) {
						delta.revive(getPrismContext());

						progressReporter.executeChanges(WebMiscUtil.createDeltaCollection(delta), options,
								task, result, target);
					} else {
						result.recordSuccess();
					}
				} catch (Exception ex) {
					result.recordFatalError(getString("pageUser.message.cantCreateUser"), ex);
					LoggingUtils.logException(LOGGER, "Create user failed", ex);
				}
				break;

			case MODIFYING:
				try {
					// WebMiscUtil.encryptCredentials(delta, true,
					// getMidpointApplication());
					prepareOrgDeltaForModify(delta,
							((OrgType) orgWrapper.getObject().asObjectable()).getParentOrgRef(), true);

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
						ObjectDelta emptyDelta = ObjectDelta.createEmptyModifyDelta(UserType.class,
								orgWrapper.getObject().getOid(), getPrismContext());
						deltas.add(emptyDelta);

						progressReporter.executeChanges(deltas, options, task, result, target);
					} else if (!deltas.isEmpty()) {

						progressReporter.executeChanges(deltas, options, task, result, target);
					} else {
						result.recordSuccess();
					}

				} catch (Exception ex) {
					if (!executeForceDelete(orgWrapper, task, options, result)) {
						result.recordFatalError(getString("pageUser.message.cantUpdateUser"), ex);
						LoggingUtils.logException(LOGGER, getString("pageUser.message.cantUpdateUser"), ex);
					} else {
						result.recomputeStatus();
					}
				}
				break;
			// support for add/delete containers (e.g. delete credentials)
			default:
				error(getString("pageUser.message.unsupportedState", orgWrapper.getStatus()));
		}

		result.recomputeStatus();

		if (!result.isInProgress()) {
			finishProcessing(target, result);
		}
	}

	private boolean executeForceDelete(ObjectWrapper orgWrapper, Task task, ModelExecuteOptions options,
			OperationResult parentResult) {
		if (executeOptionsModel.getObject().isForce()) {
			OperationResult result = parentResult.createSubresult("Force delete operation");

			try {
				ObjectDelta<UserType> forceDeleteDelta = getChange(orgWrapper);
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

	private ObjectDelta getChange(ObjectWrapper orgWrapper) throws SchemaException {

		List<FocusShadowDto> accountDtos = shadowsModel.getObject();
		List<ReferenceDelta> refDeltas = new ArrayList<ReferenceDelta>();
		ObjectDelta<OrgType> forceDeleteDelta = null;
		for (FocusShadowDto accDto : accountDtos) {
			if (!accDto.isLoadedOK()) {
				continue;
			}

			if (accDto.getStatus() == UserDtoStatus.DELETE) {
				ObjectWrapper accWrapper = accDto.getObject();
				ReferenceDelta refDelta = ReferenceDelta.createModificationDelete(UserType.F_LINK_REF,
						orgWrapper.getObject().getDefinition(), accWrapper.getObject());
				refDeltas.add(refDelta);
			} else if (accDto.getStatus() == UserDtoStatus.UNLINK) {
				ObjectWrapper accWrapper = accDto.getObject();
				ReferenceDelta refDelta = ReferenceDelta.createModificationDelete(UserType.F_LINK_REF,
						orgWrapper.getObject().getDefinition(), accWrapper.getObject().getOid());
				refDeltas.add(refDelta);
			}
		}
		if (!refDeltas.isEmpty()) {
			forceDeleteDelta = ObjectDelta.createModifyDelta(orgWrapper.getObject().getOid(), refDeltas,
					OrgType.class, getPrismContext());
		}
		PrismContainerDefinition def = orgWrapper.getObject().findContainer(UserType.F_ASSIGNMENT)
				.getDefinition();
		if (forceDeleteDelta == null) {
			forceDeleteDelta = ObjectDelta.createEmptyModifyDelta(OrgType.class, orgWrapper.getObject()
					.getOid(), getPrismContext());
		}

		prepareOrgDeltaForModify(forceDeleteDelta, null, false);
		return forceDeleteDelta;
	}

	private List<ObjectDelta<? extends ObjectType>> modifyShadows(OperationResult result) {
		List<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<>();

		List<FocusShadowDto> shadows = shadowsModel.getObject();
		OperationResult subResult = null;
		for (FocusShadowDto account : shadows) {
			if (!account.isLoadedOK())
				continue;

			try {
				ObjectWrapper shadowWrapper = account.getObject();
				ObjectDelta delta = shadowWrapper.getObjectDelta();
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Account delta computed from form:\n{}", new Object[] { delta.debugDump(3) });
				}

				if (!UserDtoStatus.MODIFY.equals(account.getStatus())) {
					continue;
				}

				if (delta.isEmpty()
						&& (shadowWrapper.getOldDelta() == null || shadowWrapper.getOldDelta().isEmpty())) {
					continue;
				}

				if (shadowWrapper.getOldDelta() != null) {
					delta = ObjectDelta.summarize(delta, shadowWrapper.getOldDelta());
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

	private void prepareOrgDeltaForModify(ObjectDelta<OrgType> delta,
			List<ObjectReferenceType> parentOrgList, boolean handleParentOrgs) throws SchemaException {
		// handle assignments
		SchemaRegistry registry = getPrismContext().getSchemaRegistry();
		PrismObjectDefinition objectDefinition = registry
				.findObjectDefinitionByCompileTimeClass(OrgType.class);
		PrismContainerDefinition assignmentDef = objectDefinition
				.findContainerDefinition(OrgType.F_ASSIGNMENT);
		AssignmentTablePanel assignmentPanel = (AssignmentTablePanel) get(createComponentPath(ID_FORM,
				ID_ASSIGNMENTS_TABLE));
		assignmentPanel.handleAssignmentDeltas(delta, assignmentDef, OrgType.F_ASSIGNMENT);

		// handle inducements
		PrismContainerDefinition inducementDef = objectDefinition
				.findContainerDefinition(OrgType.F_INDUCEMENT);
		AssignmentTablePanel inducementPanel = (AssignmentTablePanel) get(createComponentPath(ID_FORM,
				ID_INDUCEMENTS_TABLE));
		inducementPanel.handleAssignmentDeltas(delta, inducementDef, OrgType.F_INDUCEMENT);
		// We are editing OrgUnit
//		if (handleParentOrgs) {
//			if (parentOrgUnitsModel != null && parentOrgUnitsModel.getObject() != null) {
//				for (OrgType parent : parentOrgUnitsModel.getObject()) {
//					if (parent != null && WebMiscUtil.getName(parent) != null
//							&& !WebMiscUtil.getName(parent).isEmpty()) {
//						if (!isOrgParent(parent, parentOrgList)) {
//							ObjectReferenceType ref = new ObjectReferenceType();
//							ref.setOid(parent.getOid());
//							ref.setType(OrgType.COMPLEX_TYPE);
//							ReferenceDelta refDelta = ReferenceDelta.createModificationAdd(
//									OrgType.F_PARENT_ORG_REF, getOrgTypeDefinition(), ref.asReferenceValue());
//							delta.addModification(refDelta);
//							// org.asObjectable().getParentOrgRef().add(ref);
//						}
//					}
//				}
//				// Delete parentOrgUnits from edited OrgUnit
//				for (ObjectReferenceType parent : parentOrgList) {
//					if (!isRefInParentOrgModel(parent)) {
//						ReferenceDelta refDelta = ReferenceDelta.createModificationAdd(
//								OrgType.F_PARENT_ORG_REF, getOrgTypeDefinition(), parent.asReferenceValue());
//						delta.addModification(refDelta);
//						// org.asObjectable().getParentOrgRef().remove(parent);
//					}
//				}
//			}
//		}

	}

	private void prepareOrgForAdd(PrismObject<OrgType> newOrgUnit) throws SchemaException {
		// handle assignments
		PrismObjectDefinition orgDef = newOrgUnit.getDefinition();
		PrismContainerDefinition assignmentDef = orgDef.findContainerDefinition(OrgType.F_ASSIGNMENT);
		AssignmentTablePanel assignmentPanel = (AssignmentTablePanel) get(createComponentPath(ID_FORM,
				ID_ASSIGNMENTS_TABLE));
		assignmentPanel.handleAssignmentsWhenAdd(newOrgUnit, assignmentDef, newOrgUnit.asObjectable()
				.getAssignment());

		// handle inducements
		PrismContainerDefinition inducementDef = orgDef.findContainerDefinition(OrgType.F_INDUCEMENT);
		AssignmentTablePanel inducementPanel = (AssignmentTablePanel) get(createComponentPath(ID_FORM,
				ID_INDUCEMENTS_TABLE));
		inducementPanel.handleAssignmentsWhenAdd(newOrgUnit, inducementDef, newOrgUnit.asObjectable()
				.getInducement());

		// We are creating new OrgUnit
//		if (parentOrgUnitsModel != null && parentOrgUnitsModel.getObject() != null) { // this
//																						// should
//																						// be
//																						// always
//																						// the
//																						// case
//			// parentOrgRef in org is not relevant anymore, so delete it
//			newOrgUnit.asObjectable().getParentOrgRef().clear();
//			for (OrgType parent : parentOrgUnitsModel.getObject()) {
//				if (parent != null && WebMiscUtil.getName(parent) != null
//						&& !WebMiscUtil.getName(parent).isEmpty()) {
//					ObjectReferenceType ref = new ObjectReferenceType();
//					ref.setOid(parent.getOid());
//					ref.setType(OrgType.COMPLEX_TYPE);
//					newOrgUnit.asObjectable().getParentOrgRef().add(ref);
//				}
//			}
//		}

	}

	private PrismObjectDefinition getOrgTypeDefinition() {
		return orgModel.getObject().getObject().getDefinition();
	}

	public void finishProcessing(AjaxRequestTarget target, OperationResult result) {
		if (!executeOptionsModel.getObject().isKeepDisplayingResults() && progressReporter.isAllSuccess()
				&& WebMiscUtil.isSuccessOrHandledErrorOrInProgress(result)) {
			showResultInSession(result);
			setSpecificResponsePage();
		} else {
			showResult(result);
			target.add(getFeedbackPanel());
		}
	}

	private ObjectDelta saveExtension(OperationResult result) {
		ObjectDelta delta = null;

		try {
			WebMiscUtil.revive(orgModel, getPrismContext());
			WebMiscUtil.revive(extensionModel, getPrismContext());

			delta = orgWrapper.getObjectDelta();
			if (orgWrapper.getOldDelta() != null) {
				delta = ObjectDelta.summarize(orgWrapper.getOldDelta(), delta);
			}

			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Org delta computed from extension:\n{}", new Object[] { delta.debugDump(3) });
			}
		} catch (Exception e) {
			result.recordFatalError(getString("PageOrgUnit.message.cantCreateExtensionDelta"), e);
			LoggingUtils.logException(LOGGER, "Can't create delta for org. unit extension.", e);
			showResult(result);

		}

		return delta;
	}

	private ObjectWrapper loadOrgUnitWrapper(PrismObject<OrgType> unitToEdit) {
		OperationResult result = new OperationResult(LOAD_UNIT);

		PrismObject<OrgType> org = null;
		try {
			if (!isEditingOrgUnit()) {
				if (unitToEdit == null) {
					OrgType o = new OrgType();
					ActivationType defaultActivation = new ActivationType();
					defaultActivation.setAdministrativeStatus(ActivationStatusType.ENABLED);
					o.setActivation(defaultActivation);
					getPrismContext().adopt(o);
					org = o.asPrismObject();
				} else {
					org = unitToEdit;
				}
			} else {
				StringValue oid = getPageParameters().get(OnePageParameterEncoder.PARAMETER);
				org = WebModelUtils.loadObject(OrgType.class, oid.toString(), result, this);
			}
		} catch (Exception ex) {
			LoggingUtils.logException(LOGGER, "Couldn't load org. unit", ex);
			result.recordFatalError("Couldn't load org. unit.", ex);
		} finally {
			result.computeStatusIfUnknown();
		}

		if (WebMiscUtil.showResultInPage(result)) {
			showResult(result);
		}

		if (org == null) {
			showResultInSession(result);
			throw new RestartResponseException(PageOrgTree.class);
		}

		ContainerStatus status = isEditingOrgUnit() ? ContainerStatus.MODIFYING : ContainerStatus.ADDING;
		ObjectWrapper wrapper = null;
		try {
			wrapper = ObjectWrapperUtil.createObjectWrapper("PageOrgUnit.title.basic", null, org, status,
					this);
		} catch (Exception ex) {
			result.recordFatalError("Couldn't get user.", ex);
			LoggingUtils.logException(LOGGER, "Couldn't load user", ex);
			wrapper = new ObjectWrapper("pageUser.userDetails", null, org, null, status, this);
		}
		// ObjectWrapper wrapper = new ObjectWrapper("pageUser.userDetails",
		// null, user, status);
		if (wrapper.getResult() != null && !WebMiscUtil.isSuccessOrHandledError(wrapper.getResult())) {
			showResultInSession(wrapper.getResult());
		}

		wrapper.setShowEmpty(!isEditingOrgUnit());
		return wrapper;
	}


	private List<OrgType> loadParentOrgUnits() {
		List<OrgType> parentList = new ArrayList<>();
		List<ObjectReferenceType> refList = new ArrayList<>();
		OrgType orgHelper;
		Task loadTask = createSimpleTask(LOAD_PARENT_UNITS);
		OperationResult result = new OperationResult(LOAD_PARENT_UNITS);

		OrgType actOrg = (OrgType) orgModel.getObject().getObject().asObjectable();

		if (actOrg != null) {
			refList.addAll(actOrg.getParentOrgRef());
		}

		try {
			if (!refList.isEmpty()) {
				// todo improve, use IN OID search, use WebModelUtils
				for (ObjectReferenceType ref : refList) {
					String oid = ref.getOid();
					orgHelper = getModelService().getObject(OrgType.class, oid, null, loadTask, result)
							.asObjectable();
					parentList.add(orgHelper);
				}
			}
		} catch (Exception e) {
			LoggingUtils.logException(LOGGER, "Couldn't load parent org. unit refs.", e);
			result.recordFatalError("Couldn't load parent org. unit refs.", e);
		} finally {
			result.computeStatus();
		}

		if (parentList.isEmpty())
			parentList.add(new OrgType());

		return parentList;
	}

	private void initProjections(final WebMarkupContainer accounts) {
		// InlineMenu accountMenu = new InlineMenu(ID_ACCOUNT_MENU, new
		// Model((Serializable) createAccountsMenu()));
		// accounts.add(accountMenu);

		// TODO: unify - rename UserAccountDto to something else, e.g.
		// FocusShadowDto or FocusProjectionDto or something similar
		final ListView<FocusShadowDto> accountList = new ListView<FocusShadowDto>(ID_ACCOUNT_LIST,
				shadowsModel) {

			@Override
			protected void populateItem(final ListItem<FocusShadowDto> item) {
				PackageResourceReference packageRef;
				final FocusShadowDto dto = item.getModelObject();

				Panel panel;

				if (dto.isLoadedOK()) {
					packageRef = new PackageResourceReference(ImgResources.class, ImgResources.HDD_PRISM);

					panel = new PrismObjectPanel("account", new PropertyModel<ObjectWrapper>(item.getModel(),
							"object"), packageRef, (Form) PageOrgUnit.this.get(ID_FORM), PageOrgUnit.this) {

						@Override
						protected Component createHeader(String id, IModel<ObjectWrapper> model) {
							return new CheckTableHeader(id, model) {

								@Override
								protected List<InlineMenuItem> createMenuItems() {
									return createDefaultMenuItems(getModel());
								}
							};
						}
					};
				} else {
					panel = new SimpleErrorPanel("account", item.getModel()) {

						@Override
						public void onShowMorePerformed(AjaxRequestTarget target) {
							OperationResult fetchResult = dto.getResult();
							if (fetchResult != null) {
								showResult(fetchResult);
								target.add(getPageBase().getFeedbackPanel());
							}
						}
					};
				}

				panel.setOutputMarkupId(true);
				item.add(panel);
			}
		};

		// AjaxCheckBox accountCheckAll = new AjaxCheckBox(ID_ACCOUNT_CHECK_ALL,
		// new Model()) {
		//
		// @Override
		// protected void onUpdate(AjaxRequestTarget target) {
		// for(UserAccountDto dto: accountList.getModelObject()){
		// if(dto.isLoadedOK()){
		// ObjectWrapper accModel = dto.getObject();
		// accModel.setSelected(getModelObject());
		// }
		// }
		//
		// target.add(accounts);
		// }
		// };
		// accounts.add(accountCheckAll);

		accounts.add(accountList);
	}

	private void reviveModels() throws SchemaException {
		WebMiscUtil.revive(orgModel, getPrismContext());
//		WebMiscUtil.revive(parentOrgUnitsModel, getPrismContext());
	}

}

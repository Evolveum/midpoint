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

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDto;
import com.evolveum.midpoint.web.component.assignment.AssignmentTablePanel;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.dialog.ConfirmationDialog;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenu;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.prism.*;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.ObjectWrapperUtil;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.server.PageTasks;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDto;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDtoProvider;
import com.evolveum.midpoint.web.page.admin.server.dto.TaskDtoProviderOptions;
import com.evolveum.midpoint.web.page.admin.users.component.*;
import com.evolveum.midpoint.web.page.admin.users.dto.FocusProjectionDto;
import com.evolveum.midpoint.web.page.admin.users.dto.SimpleUserResourceProvider;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.web.resource.img.ImgResources;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.*;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.string.StringValue;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public abstract class BaseFocusPanel<F extends FocusType> extends Panel {
	private static final long serialVersionUID = 1L;

	public static final String AUTH_USERS_ALL = AuthorizationConstants.AUTZ_UI_USERS_ALL_URL;
	public static final String AUTH_USERS_ALL_LABEL = "PageAdminUsers.auth.usersAll.label";
	public static final String AUTH_USERS_ALL_DESCRIPTION = "PageAdminUsers.auth.usersAll.description";

	public static final String AUTH_ORG_ALL = AuthorizationConstants.AUTZ_UI_ORG_ALL_URL;
	public static final String AUTH_ORG_ALL_LABEL = "PageAdminUsers.auth.orgAll.label";
	public static final String AUTH_ORG_ALL_DESCRIPTION = "PageAdminUsers.auth.orgAll.description";

	private LoadableModel<ObjectWrapper<F>> focusModel;
	private LoadableModel<List<FocusProjectionDto>> shadowModel;
	private LoadableModel<List<FocusProjectionDto>> orgModel;
	private LoadableModel<List<AssignmentEditorDto>> assignmentsModel;

	protected static final String ID_MAIN_FORM = "mainForm";
	private static final String ID_TASK_TABLE = "taskTable";
	private static final String ID_FOCUS_FORM = "focusForm";
	private static final String ID_ASSIGNMENTS = "assignmentsContainer";
	private static final String ID_ASSIGNMENTS_PANEL = "assignmentsPanel";
	private static final String ID_TASKS = "tasks";

	private static final String ID_SHADOW_LIST = "shadowList";
	private static final String ID_SHADOWS = "shadows";
	private static final String ID_SHADOW_MENU = "shadowMenu";
	private static final String ID_SHADOW_CHECK_ALL = "shadowCheckAll";

	private static final String ID_ORG_LIST = "orgList";
	private static final String ID_ORGS = "orgs";
	private static final String ID_ORG_MENU = "orgMenu";
	private static final String ID_ORG_CHECK_ALL = "orgCheckAll";

	private static final String MODAL_ID_RESOURCE = "resourcePopup";
	private static final String MODAL_ID_ADD_ORG = "addOrgPopup";
	private static final String MODAL_ID_CONFIRM_DELETE_SHADOW = "confirmDeleteShadowPopup";
	private static final String MODAL_ID_CONFIRM_DELETE_ORG = "confirmDeleteOrgPopup";
	private static final String MODAL_ID_CONFIRM_DELETE_ASSIGNMENT = "confirmDeleteAssignmentPopup";
	private static final String MODAL_ID_ASSIGNMENTS_PREVIEW = "assignmentsPreviewPopup";

	private static final Trace LOGGER = TraceManager.getTrace(PageAdminFocus.class);

	// used to determine whether to leave this page or stay on it (after
	// operation finishing)
	private ObjectDelta delta;

	private PageBase page;

	private Form mainForm;

	public BaseFocusPanel(String id, Form mainForm, LoadableModel<ObjectWrapper<F>> focusModel,
			LoadableModel<List<FocusProjectionDto>> shadowModel,
			LoadableModel<List<FocusProjectionDto>> orgModel,
			LoadableModel<List<AssignmentEditorDto>> assignmentsModel, PageBase page) {
		super(id);
		this.page = page;
		this.focusModel = focusModel;
		this.shadowModel = shadowModel;
		this.orgModel = orgModel;
		this.assignmentsModel = assignmentsModel;
		this.mainForm = mainForm;
		initLayout();
	}

	public StringResourceModel createStringResource(String resourceKey, Object... objects) {
		return new StringResourceModel(resourceKey, this, null, resourceKey, objects);
	}

	public String getString(String resourceKey, Object... objects) {
		return createStringResource(resourceKey, objects).getString();
	}

	protected String createComponentPath(String... components) {
		return StringUtils.join(components, ":");
	}

	public LoadableModel<ObjectWrapper<F>> getFocusModel() {
		return focusModel;
	}

	protected abstract F createNewFocus();

	protected void initLayout() {

		PrismObjectPanel userForm = new PrismObjectPanel<F>(ID_FOCUS_FORM, focusModel,
				new PackageResourceReference(ImgResources.class, ImgResources.USER_PRISM), mainForm, page) {

			@Override
			protected IModel<String> createDescription(IModel<ObjectWrapper<F>> model) {
				return createStringResource("pageAdminFocus.description");
			}
		};
		add(userForm);

		WebMarkupContainer shadows = new WebMarkupContainer(ID_SHADOWS);
		shadows.setOutputMarkupId(true);
		add(shadows);
		initShadows(shadows);

		WebMarkupContainer orgs = new WebMarkupContainer(ID_ORGS);
		orgs.setOutputMarkupId(true);
		add(orgs);
		initOrgs(orgs);

		WebMarkupContainer assignments = new WebMarkupContainer(ID_ASSIGNMENTS);
		assignments.setOutputMarkupId(true);
		add(assignments);
		initAssignments(assignments);

		WebMarkupContainer tasks = new WebMarkupContainer(ID_TASKS);
		tasks.setOutputMarkupId(true);
		add(tasks);
		initTasks(tasks);

		initResourceModal();
		initAssignableModal();

		initConfirmationDialogs();
	}

	public ObjectWrapper<F> getFocusWrapper() {
		return focusModel.getObject();
	}

	public List<FocusProjectionDto> getFocusShadows() {
		return shadowModel.getObject();
	}

	public List<AssignmentEditorDto> getFocusAssignments() {
		return assignmentsModel.getObject();
	}

	protected abstract void reviveCustomModels() throws SchemaException;

	private void showResultInSession(OperationResult result) {
		page.showResultInSession(result);
	}

	private PrismContext getPrismContext() {
		return page.getPrismContext();
	}

	private PageParameters getPageParameters() {
		return page.getPageParameters();
	}

	private void showResult(OperationResult result) {
		page.showResult(result);
	}

	private WebMarkupContainer getFeedbackPanel() {
		return page.getFeedbackPanel();
	}

	protected ModalWindow createModalWindow(final String id, IModel<String> title, int width, int height) {
		final ModalWindow modal = new ModalWindow(id);
		add(modal);

		modal.setResizable(false);
		modal.setTitle(title);
		modal.setCookieName(PageBase.class.getSimpleName() + ((int) (Math.random() * 100)));

		modal.setInitialWidth(width);
		modal.setWidthUnit("px");
		modal.setInitialHeight(height);
		modal.setHeightUnit("px");

		modal.setCloseButtonCallback(new ModalWindow.CloseButtonCallback() {

			@Override
			public boolean onCloseButtonClicked(AjaxRequestTarget target) {
				return true;
			}
		});

		modal.setWindowClosedCallback(new ModalWindow.WindowClosedCallback() {

			@Override
			public void onClose(AjaxRequestTarget target) {
				modal.close(target);
			}
		});

		modal.add(new AbstractDefaultAjaxBehavior() {

			@Override
			public void renderHead(Component component, IHeaderResponse response) {
				response.render(OnDomReadyHeaderItem.forScript("Wicket.Window.unloadConfirmation = false;"));
				response.render(JavaScriptHeaderItem.forScript(
						"$(document).ready(function() {\n" + "  $(document).bind('keyup', function(evt) {\n"
								+ "    if (evt.keyCode == 27) {\n" + getCallbackScript() + "\n"
								+ "        evt.preventDefault();\n" + "    }\n" + "  });\n" + "});",
						id));
			}

			@Override
			protected void respond(AjaxRequestTarget target) {
				modal.close(target);

			}
		});

		return modal;
	}

	protected void reviveModels() throws SchemaException {
		WebMiscUtil.revive(focusModel, getPrismContext());
		WebMiscUtil.revive(shadowModel, getPrismContext());
		WebMiscUtil.revive(orgModel, getPrismContext());
		WebMiscUtil.revive(assignmentsModel, getPrismContext());
		reviveCustomModels();
		// WebMiscUtil.revive(summaryUser, getPrismContext());
	}

	protected abstract Class<F> getCompileTimeClass();

	protected abstract Class getRestartResponsePage();

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

	private void initShadows(final WebMarkupContainer accounts) {
		InlineMenu accountMenu = new InlineMenu(ID_SHADOW_MENU, new Model((Serializable) createShadowMenu()));
		accounts.add(accountMenu);

		final ListView<FocusProjectionDto> accountList = new ListView<FocusProjectionDto>(ID_SHADOW_LIST,
				shadowModel) {

			@Override
			protected void populateItem(final ListItem<FocusProjectionDto> item) {
				PackageResourceReference packageRef;
				final FocusProjectionDto dto = item.getModelObject();

				Panel panel;

				if (dto.isLoadedOK()) {
					packageRef = new PackageResourceReference(ImgResources.class, ImgResources.HDD_PRISM);

					panel = new PrismObjectPanel<F>("shadow",
							new PropertyModel<ObjectWrapper<F>>(item.getModel(), "object"), packageRef,
							(Form) page.get(ID_MAIN_FORM), page) {

						@Override
						protected Component createHeader(String id, IModel<ObjectWrapper<F>> model) {
							return new CheckTableHeader(id, (IModel) model) {

								@Override
								protected List<InlineMenuItem> createMenuItems() {
									return createDefaultMenuItems(getModel());
								}
							};
						}
					};
				} else {
					panel = new SimpleErrorPanel("shadow", item.getModel()) {

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

		AjaxCheckBox accountCheckAll = new AjaxCheckBox(ID_SHADOW_CHECK_ALL, new Model()) {

			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				for (FocusProjectionDto dto : accountList.getModelObject()) {
					if (dto.isLoadedOK()) {
						ObjectWrapper accModel = dto.getObject();
						accModel.setSelected(getModelObject());
					}
				}

				target.add(accounts);
			}
		};
		accounts.add(accountCheckAll);

		accounts.add(accountList);
	}

	private void initOrgs(final WebMarkupContainer orgs) {
		InlineMenu orgtMenu = new InlineMenu(ID_ORG_MENU, new Model((Serializable) createOrgMenu()));
		orgs.add(orgtMenu);

		final ListView<FocusProjectionDto> orgList = new ListView<FocusProjectionDto>(ID_ORG_LIST, orgModel) {

			@Override
			protected void populateItem(final ListItem<FocusProjectionDto> item) {
				PackageResourceReference packageRef;
				final FocusProjectionDto dto = item.getModelObject();

				Panel panel;

				if (dto.isLoadedOK()) {
					packageRef = new PackageResourceReference(ImgResources.class, ImgResources.HDD_PRISM);

					panel = new PrismObjectPanel<F>("org",
							new PropertyModel<ObjectWrapper<F>>(item.getModel(), "object"), packageRef,
							(Form) page.get(ID_MAIN_FORM), page) {

						@Override
						protected Component createHeader(String id, IModel<ObjectWrapper<F>> model) {
							return new CheckTableHeader(id, (IModel) model) {

								@Override
								protected List<InlineMenuItem> createMenuItems() {
									return createDefaultMenuItems(getModel());
								}
							};
						}
					};
				} else {
					panel = new SimpleErrorPanel("org", item.getModel()) {

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

		AjaxCheckBox orgCheckAll = new AjaxCheckBox(ID_ORG_CHECK_ALL, new Model()) {

			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				for (FocusProjectionDto dto : orgList.getModelObject()) {
					if (dto.isLoadedOK()) {
						ObjectWrapper orgModel = dto.getObject();
						orgModel.setSelected(getModelObject());
					}
				}

				target.add(orgs);
			}
		};
		orgs.add(orgCheckAll);

		orgs.add(orgList);
	}

	private void initAssignments(WebMarkupContainer assignments) {

		AssignmentTablePanel panel = new AssignmentTablePanel(ID_ASSIGNMENTS_PANEL,
				createStringResource("FocusType.assignment"), assignmentsModel) {

			@Override
			protected void showAllAssignments(AjaxRequestTarget target) {
				AssignmentPreviewDialog dialog = (AssignmentPreviewDialog) getParent().getParent()
						.get(createComponentPath(MODAL_ID_ASSIGNMENTS_PREVIEW));
				((PageAdminFocus) page).recomputeAssignmentsPerformed(dialog, target);
			}

		};
		assignments.add(panel);
	}

	private List<InlineMenuItem> createShadowMenu() {
		List<InlineMenuItem> items = new ArrayList<InlineMenuItem>();

        PrismObjectDefinition def = focusModel.getObject().getObject().getDefinition();
        PrismReferenceDefinition ref = def.findReferenceDefinition(UserType.F_LINK_REF);
        InlineMenuItem item ;
        if (ref.canRead() && ref.canAdd()){
            item = new InlineMenuItem(createStringResource("pageAdminFocus.button.addShadow"),
                    new InlineMenuItemAction() {

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            showModalWindow(MODAL_ID_RESOURCE, target);
                        }
                    });
            items.add(item);
            items.add(new InlineMenuItem());
        }
        PrismPropertyDefinition prop = def.findPropertyDefinition(SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
        if (prop.canRead() && prop.canModify()) {
            item = new InlineMenuItem(createStringResource("pageAdminFocus.button.enable"),
                    new InlineMenuItemAction() {

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            updateShadowActivation(target, getSelectedProjections(shadowModel), true);
                        }
                    });
            items.add(item);
            item = new InlineMenuItem(createStringResource("pageAdminFocus.button.disable"),
                    new InlineMenuItemAction() {

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            updateShadowActivation(target, getSelectedProjections(shadowModel), false);
                        }
                    });
            items.add(item);
        }
        if (ref.canRead() && ref.canAdd()) {
            item = new InlineMenuItem(createStringResource("pageAdminFocus.button.unlink"),
                    new InlineMenuItemAction() {

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            unlinkProjectionPerformed(target, shadowModel, getSelectedProjections(shadowModel), ID_SHADOWS);
                        }
                    });
            items.add(item);
        }
        prop = def.findPropertyDefinition(SchemaConstants.PATH_ACTIVATION_LOCKOUT_STATUS);
        if (prop.canRead() && prop.canModify()) {
            item = new InlineMenuItem(createStringResource("pageAdminFocus.button.unlock"),
                    new InlineMenuItemAction() {

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            unlockShadowPerformed(target, shadowModel, getSelectedProjections(shadowModel));
                        }
                    });
            items.add(item);
        }
        prop = def.findPropertyDefinition(SchemaConstants.PATH_ACTIVATION_ADMINISTRATIVE_STATUS);
        if (prop.canRead() && prop.canModify()) {
            items.add(new InlineMenuItem());
            item = new InlineMenuItem(createStringResource("pageAdminFocus.button.delete"),
                    new InlineMenuItemAction() {

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            deleteProjectionPerformed(target, shadowModel);
                        }
                    });
            items.add(item);
        }

		return items;
	}

	private List<InlineMenuItem> createOrgMenu() {
		List<InlineMenuItem> items = new ArrayList<InlineMenuItem>();
//		InlineMenuItem item = new InlineMenuItem(createStringResource("pageAdminFocus.button.addToOrg"),
//				new InlineMenuItemAction() {
//
//					@Override
//					public void onClick(AjaxRequestTarget target) {
//						showModalWindow(MODAL_ID_ADD_ORG, target);
//					}
//				});
//		items.add(item);
//		items.add(new InlineMenuItem());
//		item = new InlineMenuItem(createStringResource("pageAdminFocus.button.unlink"),
//				new InlineMenuItemAction() {
//
//					@Override
//					public void onClick(AjaxRequestTarget target) {
//						unlinkProjectionPerformed(target, orgModel, getSelectedProjections(orgModel), ID_ORGS);
//					}
//				});
//		items.add(item);
//		items.add(new InlineMenuItem());
		

		return items;
	}

	private List<FocusProjectionDto> getSelectedProjections(IModel<List<FocusProjectionDto>> model) {
		List<FocusProjectionDto> selected = new ArrayList<FocusProjectionDto>();

		List<FocusProjectionDto> all = model.getObject();
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

	private void addSelectedAccountPerformed(AjaxRequestTarget target, List<ResourceType> newResources) {
		ModalWindow window = (ModalWindow) get(MODAL_ID_RESOURCE);
		window.close(target);

		if (newResources.isEmpty()) {
			warn(getString("pageUser.message.noResourceSelected"));
			return;
		}

		for (ResourceType resource : newResources) {
			try {
				ShadowType shadow = new ShadowType();
				shadow.setResource(resource);

				RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(
						resource.asPrismObject(), LayerType.PRESENTATION, getPrismContext());
				if (refinedSchema == null) {
					error(getString("pageAdminFocus.message.couldntCreateAccountNoSchema",
							resource.getName()));
					continue;
				}
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Refined schema for {}\n{}", resource, refinedSchema.debugDump());
				}

				RefinedObjectClassDefinition accountDefinition = refinedSchema.getDefaultRefinedDefinition(ShadowKindType.ACCOUNT);
				if (accountDefinition == null) {
					error(getString("pageAdminFocus.message.couldntCreateAccountNoAccountSchema",
							resource.getName()));
					continue;
				}

				QName objectClass = accountDefinition.getObjectClassDefinition().getTypeName();
				shadow.setObjectClass(objectClass);

				getPrismContext().adopt(shadow);

				ObjectWrapper wrapper = ObjectWrapperUtil.createObjectWrapper(
						WebMiscUtil.getOrigStringFromPoly(resource.getName()), null, shadow.asPrismObject(),
						ContainerStatus.ADDING, page);
				if (wrapper.getResult() != null
						&& !WebMiscUtil.isSuccessOrHandledError(wrapper.getResult())) {
					showResultInSession(wrapper.getResult());
				}

				wrapper.setShowEmpty(true);
				wrapper.setMinimalized(false);
				shadowModel.getObject().add(new FocusProjectionDto(wrapper, UserDtoStatus.ADD));
			} catch (Exception ex) {
				error(getString("pageAdminFocus.message.couldntCreateAccount", resource.getName(),
						ex.getMessage()));
				LoggingUtils.logException(LOGGER, "Couldn't create account", ex);
			}
		}
	}

	private void addSelectedOrgPerformed(AjaxRequestTarget target, List<ObjectType> newOrgs) {
		ModalWindow window = (ModalWindow) get(MODAL_ID_ADD_ORG);
		window.close(target);

		if (newOrgs.isEmpty()) {
			warn(getString("pageAdminFocus.message.noOrgSelected"));
			//target.add(getFeedbackPanel());
			return;
		}

		for (ObjectType object : newOrgs) {
			try {
				if (!(object instanceof OrgType)) {
					continue;
				}

				OrgType newOrg = (OrgType) object;

				ObjectWrapper<OrgType> wrapper = ObjectWrapperUtil.createObjectWrapper(
						WebMiscUtil.getOrigStringFromPoly(newOrg.getDisplayName()), null,
						newOrg.asPrismObject(), ContainerStatus.ADDING, page);
				if (wrapper.getResult() != null
						&& !WebMiscUtil.isSuccessOrHandledError(wrapper.getResult())) {
					showResultInSession(wrapper.getResult());
				}

				wrapper.setShowEmpty(false);
				wrapper.setReadonly(true);
				wrapper.setMinimalized(true);
				orgModel.getObject().add(new FocusProjectionDto(wrapper, UserDtoStatus.ADD));
				//setResponsePage(getPage());
			} catch (Exception ex) {
				error(getString("pageAdminFocus.message.couldntAssignObject", object.getName(),
						ex.getMessage()));
				LoggingUtils.logException(LOGGER, "Couldn't assign object", ex);
			}
		}

		//target.add(getFeedbackPanel(), get(createComponentPath(ID_ORGS)));
	}

	private void updateShadowActivation(AjaxRequestTarget target, List<FocusProjectionDto> accounts,
			boolean enabled) {
		if (!isAnyProjectionSelected(target, shadowModel)) {
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

		target.add(getFeedbackPanel(), get(createComponentPath(ID_SHADOWS)));
	}

	private boolean isAnyProjectionSelected(AjaxRequestTarget target,
			IModel<List<FocusProjectionDto>> model) {
		List<FocusProjectionDto> selected = getSelectedProjections(model);
		if (selected.isEmpty()) {
			warn(getString("pageAdminFocus.message.noAccountSelected"));
			target.add(getFeedbackPanel());
			return false;
		}

		return true;
	}

	private void deleteProjectionPerformed(AjaxRequestTarget target, IModel<List<FocusProjectionDto>> model) {
		if (!isAnyProjectionSelected(target, model)) {
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
		target.add(get(createComponentPath(ID_SHADOWS)));
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

		target.add(getFeedbackPanel(), get(createComponentPath(ID_ASSIGNMENTS)));
	}

	private void unlinkProjectionPerformed(AjaxRequestTarget target, IModel<List<FocusProjectionDto>> model,
			List<FocusProjectionDto> selected, String componentPath) {
		if (!isAnyProjectionSelected(target, model)) {
			return;
		}

		for (FocusProjectionDto projection : selected) {
			if (UserDtoStatus.ADD.equals(projection.getStatus())) {
				continue;
			}
			projection.setStatus(UserDtoStatus.UNLINK);
		}
		target.add(get(createComponentPath(componentPath)));
	}

	private void unlockShadowPerformed(AjaxRequestTarget target, IModel<List<FocusProjectionDto>> model,
			List<FocusProjectionDto> selected) {
		if (!isAnyProjectionSelected(target, model)) {
			return;
		}

		for (FocusProjectionDto account : selected) {
			// TODO: implement unlock
		}
	}

	private void initResourceModal() {
		ModalWindow window = new ModalWindow(MODAL_ID_RESOURCE);

		final SimpleUserResourceProvider provider = new SimpleUserResourceProvider(this, shadowModel) {
			@Override
			protected void handlePartialError(OperationResult result) {
				showResult(result);
			}
		};

		ResourcesSelectionPanel.Context context = new ResourcesSelectionPanel.Context(this) {
			@Override
			public BaseFocusPanel getRealParent() {
				return WebMiscUtil.theSameForPage(BaseFocusPanel.this, getCallingPageReference());
			}

			@Override
			public SimpleUserResourceProvider getProvider() {
				return provider;
			}

			@Override
			public void addPerformed(AjaxRequestTarget target, List<ResourceType> newResources) {
				getRealParent().addSelectedAccountPerformed(target, newResources);
			}
		};
		ResourcesSelectionPage.prepareDialog(window, context, this, "pageAdminFocus.title.selectResource", ID_SHADOWS);

		add(window);
	}

	private void initAssignableModal() {

		ModalWindow window = new ModalWindow(MODAL_ID_ADD_ORG);
		AssignableOrgSelectionPanel.Context context = new AssignableOrgSelectionPanel.Context(this) {
			@Override
			public BaseFocusPanel getRealParent() {
				return WebMiscUtil.theSameForPage(BaseFocusPanel.this, getCallingPageReference());
			}

			@Override
			protected void addPerformed(AjaxRequestTarget target, List<ObjectType> selected) {
				getRealParent().addSelectedOrgPerformed(target, selected);
			}

			@Override
			protected void handlePartialError(OperationResult result) {
				getRealParent().showResult(result);
			}

			@Override
			public String getSubmitKey() {
				return "assignablePopupContent.button.add";
			}
		};
		AssignableOrgSelectionPage.prepareDialog(window, context, this, "pageAdminFocus.title.selectAssignable", ID_ORGS);
		context.setType(OrgType.class);
		add(window);

		ModalWindow assignmentPreviewPopup = new AssignmentPreviewDialog(MODAL_ID_ASSIGNMENTS_PREVIEW, null,
				null);
		add(assignmentPreviewPopup);
	}

	private void initTasks(WebMarkupContainer tasks) {
		List<IColumn<TaskDto, String>> taskColumns = initTaskColumns();
		final TaskDtoProvider taskDtoProvider = new TaskDtoProvider(page,
				TaskDtoProviderOptions.minimalOptions());
		taskDtoProvider.setQuery(createTaskQuery(null));
		TablePanel taskTable = new TablePanel<TaskDto>(ID_TASK_TABLE, taskDtoProvider, taskColumns) {

			@Override
			protected void onInitialize() {
				super.onInitialize();
				StringValue oidValue = getPageParameters().get(OnePageParameterEncoder.PARAMETER);

				taskDtoProvider.setQuery(createTaskQuery(oidValue != null ? oidValue.toString() : null));
			}
		};
		tasks.add(taskTable);

		tasks.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isVisible() {
				return taskDtoProvider.size() > 0;
			}
		});
	}

	private ObjectQuery createTaskQuery(String oid) {
		List<ObjectFilter> filters = new ArrayList<ObjectFilter>();

		if (oid == null) {
			oid = "non-existent"; // TODO !!!!!!!!!!!!!!!!!!!!
		}
		try {
			filters.add(RefFilter.createReferenceEqual(TaskType.F_OBJECT_REF, TaskType.class,
					getPrismContext(), oid));
			filters.add(NotFilter.createNot(EqualFilter.createEqual(TaskType.F_EXECUTION_STATUS,
					TaskType.class, getPrismContext(), null, TaskExecutionStatusType.CLOSED)));
			filters.add(EqualFilter.createEqual(TaskType.F_PARENT, TaskType.class, getPrismContext(), null));
		} catch (SchemaException e) {
			throw new SystemException("Unexpected SchemaException when creating task filter", e);
		}

		return new ObjectQuery().createObjectQuery(AndFilter.createAnd(filters));
	}

	private List<IColumn<TaskDto, String>> initTaskColumns() {
		List<IColumn<TaskDto, String>> columns = new ArrayList<IColumn<TaskDto, String>>();

		columns.add(PageTasks.createTaskNameColumn(this, "pageAdminFocus.task.name"));
		columns.add(PageTasks.createTaskCategoryColumn(this, "pageAdminFocus.task.category"));
		columns.add(PageTasks.createTaskExecutionStatusColumn(this, "pageAdminFocus.task.execution"));
		columns.add(PageTasks.createTaskResultStatusColumn(this, "pageAdminFocus.task.status"));
		return columns;
	}

	private void initConfirmationDialogs() {
		ConfirmationDialog dialog = new ConfirmationDialog(MODAL_ID_CONFIRM_DELETE_SHADOW,
				createStringResource("pageAdminFocus.title.confirmDelete"),
				new AbstractReadOnlyModel<String>() {

					@Override
					public String getObject() {
						return createStringResource("pageAdminFocus.message.deleteAccountConfirm",
								getSelectedProjections(shadowModel).size()).getString();
					}
				}) {

			@Override
			public void yesPerformed(AjaxRequestTarget target) {
				close(target);
				deleteAccountConfirmedPerformed(target, getSelectedProjections(shadowModel));
			}
		};
		add(dialog);

		dialog = new ConfirmationDialog(MODAL_ID_CONFIRM_DELETE_ORG,
				createStringResource("pageAdminFocus.title.confirmDelete"),
				new AbstractReadOnlyModel<String>() {

					@Override
					public String getObject() {
						return createStringResource("pageAdminFocus.message.deleteAccountConfirm",
								getSelectedProjections(orgModel).size()).getString();
					}
				}) {

			@Override
			public void yesPerformed(AjaxRequestTarget target) {
				close(target);
				deleteAccountConfirmedPerformed(target, getSelectedProjections(shadowModel));
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

	// private void initButtons(){
	// AjaxSubmitButton recomputeAssignments = new
	// AjaxSubmitButton(ID_BUTTON_RECOMPUTE_ASSIGNMENTS,
	// createStringResource("pageAdminFocus.button.recompute.assignments")) {
	//
	// @Override
	// protected void onSubmit(AjaxRequestTarget target,
	// org.apache.wicket.markup.html.form.Form<?> form) {
	// AssignmentPreviewDialog dialog = (AssignmentPreviewDialog)
	// getParent().get(createComponentPath(MODAL_ID_ASSIGNMENTS_PREVIEW));
	// ((PageAdminFocus)page).recomputeAssignmentsPerformed(dialog, target);
	// }
	//
	// @Override
	// protected void onError(AjaxRequestTarget target,
	// org.apache.wicket.markup.html.form.Form<?> form) {
	// target.add(getFeedbackPanel());
	// }
	// };
	// add(recomputeAssignments);
	// }

}

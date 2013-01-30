/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.page.admin.users;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AbstractAjaxBehavior;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.string.StringValue;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathSegment;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.accordion.Accordion;
import com.evolveum.midpoint.web.component.accordion.AccordionItem;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDto;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorDtoType;
import com.evolveum.midpoint.web.component.assignment.AssignmentEditorPanel;
import com.evolveum.midpoint.web.component.button.AjaxLinkButton;
import com.evolveum.midpoint.web.component.button.AjaxSubmitLinkButton;
import com.evolveum.midpoint.web.component.button.ButtonType;
import com.evolveum.midpoint.web.component.dialog.ConfirmationDialog;
import com.evolveum.midpoint.web.component.prism.ContainerStatus;
import com.evolveum.midpoint.web.component.prism.ContainerWrapper;
import com.evolveum.midpoint.web.component.prism.HeaderStatus;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.PrismObjectPanel;
import com.evolveum.midpoint.web.component.prism.PropertyWrapper;
import com.evolveum.midpoint.web.component.prism.ValueWrapper;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.users.dto.SimpleUserResourceProvider;
import com.evolveum.midpoint.web.page.admin.users.dto.UserAccountDto;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.web.resource.img.ImgResources;
import com.evolveum.midpoint.web.util.WebMiscUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AccountConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceObjectShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;

/**
 * @author lazyman
 */
public class PageUser extends PageAdminUsers {

    public static final String PARAM_USER_ID = "userId";
    private static final String DOT_CLASS = PageUser.class.getName() + ".";
    private static final String OPERATION_LOAD_USER = DOT_CLASS + "loadUser";
    private static final String OPERATION_LOAD_ASSIGNMENTS = DOT_CLASS + "loadAssignments";
    private static final String OPERATION_LOAD_ASSIGNMENT = DOT_CLASS + "loadAssignment";
    private static final String OPERATION_SEND_TO_SUBMIT = DOT_CLASS + "sendToSubmit";
    private static final String OPERATION_MODIFY_ACCOUNT = DOT_CLASS + "modifyAccount";
    private static final String OPERATION_PREPARE_ACCOUNTS = DOT_CLASS + "getAccountsForSubmit";
    private static final String OPERATION_LOAD_ACCOUNTS = DOT_CLASS + "loadAccounts";
    private static final String OPERATION_LOAD_ACCOUNT = DOT_CLASS + "loadAccount";
    private static final String OPERATION_SAVE = DOT_CLASS + "save";

    private static final String MODAL_ID_RESOURCE = "resourcePopup";
    private static final String MODAL_ID_ASSIGNABLE = "assignablePopup";
    private static final String MODAL_ID_CONFIRM_DELETE_ACCOUNT = "confirmDeleteAccountPopup";
    private static final String MODAL_ID_CONFIRM_DELETE_ASSIGNMENT = "confirmDeleteAssignmentPopup";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_ACCOUNT_BUTTONS = "accountsButtons";
    private static final String ID_ACCORDION = "accordion";
    private static final String ID_ASSIGNMENT_EDITOR = "assignmentEditor";
    private static final String ID_ASSIGNMENT_LIST = "assignmentList";
    private static final String ID_ASSIGNMENTS = "assignments";
    private static final String ID_USER_FORM = "userForm";
    private static final String ID_ACCOUNTS_DELTAS = "accountsDeltas";
    private static final String ID_FORCE_CHECK = "forceCheck";

    private static final Trace LOGGER = TraceManager.getTrace(PageUser.class);
    private IModel<ObjectWrapper> userModel;
    private IModel<List<UserAccountDto>> accountsModel;
    private IModel<List<AssignmentEditorDto>> assignmentsModel;

    //used to add force flag to operations if necessary, will be moved to some "page dto"
    private boolean forceAction;
    
	// it should be sent from submit. If the user is on the preview page and
	// than he wants to get back to the edit page, the object delta is set, so
	// we don't loose any changes
    private ObjectDelta objectDelta;
    
    public ObjectDelta getObjectDelta() {
		return objectDelta;
	}

    public PageUser(final Collection<ObjectDelta<? extends ObjectType>> deltas, final String oid){
    	
    	userModel = new LoadableModel<ObjectWrapper>(false) {
    		@Override
    		protected ObjectWrapper load(){
    			return loadUserAfterPreview(deltas, oid);
    		}
		};
		
    	
    	accountsModel = new LoadableModel<List<UserAccountDto>>(false) {
			@Override
			protected List<UserAccountDto> load(){
				return loadAccountsAfterPreview(deltas);
			}
			
		};
    	
    	
		assignmentsModel = new LoadableModel<List<AssignmentEditorDto>>(false) {
			
			@Override
			protected List<AssignmentEditorDto> load(){
				return loadAssignmentsAfterPreview(deltas);
			}

		};
		
		initLayout();
    }
    
    private ObjectWrapper loadUserAfterPreview(Collection<ObjectDelta<? extends ObjectType>> deltas, String oid) {
    	
    	Task task = getTaskManager().createTaskInstance();
    	OperationResult result = new OperationResult("load after preview");
    	PrismObject<UserType> user = null;
  
    	ObjectWrapper wrapper = null;
    	try{
    	
    	
		for (ObjectDelta delta : deltas){
			if (delta.getObjectTypeClass().equals(UserType.class)){
				user = getModelService().getObject(UserType.class, delta.getOid(), null, task, result);
				ReferenceDelta refDelta = delta.findReferenceModification(UserType.F_ACCOUNT_REF);
				ObjectDelta clonedDelta = delta.clone();
				if (refDelta != null && refDelta.isDelete()){
					clonedDelta.removeReferenceModification(UserType.F_ACCOUNT_REF);
				}
				ContainerDelta assigmentDelta = delta.findContainerDelta(UserType.F_ASSIGNMENT);
				if (assigmentDelta != null && assigmentDelta.isDelete()){
					ItemDelta.removeItemDelta(clonedDelta.getModifications(), new ItemPath(UserType.F_ASSIGNMENT), ContainerDelta.class);
				}
				
				PropertyDelta.applyTo(clonedDelta.getModifications(), user);
				wrapper = new ObjectWrapper(null, null, user, ContainerStatus.MODIFYING);
				wrapper.setOldDelta(delta);
		         wrapper.setShowEmpty(false);
			}
		}
		if (wrapper == null && oid != null){
    		user = getModelService().getObject(UserType.class, oid, null, task, result);
    		wrapper = new ObjectWrapper(null, null, user, ContainerStatus.MODIFYING);
    		wrapper.setShowEmpty(false);
    	}
    	} catch (Exception ex){
    		result.recordFatalError("Couldn't get user.", ex);
            LoggingUtils.logException(LOGGER, "Couldn't load user", ex);
    	}
    	
    	result.recomputeStatus();
    	
    	  if (!result.isSuccess()) {
              showResultInSession(result);
          }
    	
		return wrapper;
	}
    
    private List<UserAccountDto> loadAccountsAfterPreview(Collection<ObjectDelta<? extends ObjectType>> deltas){
    	OperationResult result = new OperationResult("Load accounts after preview");
    	List<UserAccountDto> wrappers = loadAccountWrappers();
		for (UserAccountDto acc : wrappers) {
			for (ObjectDelta delta : deltas) {
				ObjectWrapper accountWrapper = acc.getObject();
				if (delta.isModify() && delta.getObjectTypeClass().equals(UserType.class)) {
					ReferenceDelta refDelta = delta.findReferenceModification(UserType.F_ACCOUNT_REF);
					if (refDelta == null){
						continue;
					}
					if (refDelta.getValuesToDelete() != null) {
						for (PrismReferenceValue refValue : refDelta.getValuesToDelete()) {
							if (refValue.getObject() != null && refValue.getObject().getOid() != null && refValue.getObject().getOid().equals(accountWrapper.getObject().getOid())) {
										acc.getObject().setHeaderStatus(HeaderStatus.DELETED);
										acc.setStatus(UserDtoStatus.DELETE);
									
							} else if (refValue.getOid() != null && refValue.getOid().equals(accountWrapper.getObject().getOid())){
								acc.getObject().setHeaderStatus(HeaderStatus.UNLINKED);
								acc.setStatus(UserDtoStatus.UNLINK);
							}
						}
					} 

				}else if (delta.isModify() && ResourceObjectShadowType.class.isAssignableFrom(delta.getObjectTypeClass())){
					try {
						accountWrapper.setOldDelta(delta);
						accountWrapper.setMinimalized(false);
						PropertyDelta.applyTo(delta.getModifications(), accountWrapper.getObject());
					} catch (Exception ex) {
						result.recordFatalError("Couldn't load account after preview." + ex.getMessage(), ex);
		                LoggingUtils.logException(LOGGER, "Couldn't load account after preview", ex);
					}
				}
				
			}
    	}
    	
    	for (ObjectDelta delta : deltas){
    			if (delta.getObjectToAdd() != null && ResourceObjectShadowType.class.isAssignableFrom(delta.getObjectToAdd().getClass())){
    				ObjectWrapper ow = new ObjectWrapper(null , null, delta.getObjectToAdd(), ContainerStatus.ADDING);
    				ow.setShowEmpty(true);
    				wrappers.add(new UserAccountDto(ow, UserDtoStatus.ADD));
    			}
  
    	}
    	
    	return wrappers;
    }
    
    private List<AssignmentEditorDto> loadAssignmentsAfterPreview(
			Collection<ObjectDelta<? extends ObjectType>> deltas) {
    	List<AssignmentEditorDto> wrappers = loadAssignments();
		for (AssignmentEditorDto acc : wrappers) {
			for (ObjectDelta delta : deltas) {
//				ObjectWrapper accountWrapper = acc.getObject();
				if (delta.isModify() && delta.getObjectTypeClass().equals(UserType.class)) {
					ContainerDelta containerDelta = delta.findContainerDelta(UserType.F_ASSIGNMENT);
					if (containerDelta == null){
						continue;
					}
					if (containerDelta.getValuesToDelete() != null) {
						for (Object value : containerDelta.getValuesToDelete()) {
							if (value instanceof PrismContainerValue){
								PrismContainerValue valueToDelete = (PrismContainerValue) value;
								if (valueToDelete.equals(acc.getOldValue())){
									acc.setStatus(UserDtoStatus.DELETE);
									
								}
							}
							
						}
					} 

				}
			}
    	}
    	return wrappers;

	}
    
    public PageUser() {
    	
        userModel = new LoadableModel<ObjectWrapper>(false) {

            @Override
            protected ObjectWrapper load() {
                return loadUserWrapper();
            }
        };
        accountsModel = new LoadableModel<List<UserAccountDto>>(false) {

            @Override
            protected List<UserAccountDto> load() {
                return loadAccountWrappers();
            }
        };
        assignmentsModel = new LoadableModel<List<AssignmentEditorDto>>(false) {

            @Override
            protected List<AssignmentEditorDto> load() {
                return loadAssignments();
            }
        };
        initLayout();
    }

    private ObjectWrapper loadUserWrapper() {
        OperationResult result = new OperationResult(OPERATION_LOAD_USER);
        PrismObject<UserType> user = null;
        try {
        		if (!isEditingUser()) {
                    UserType userType = new UserType();
                    getMidpointApplication().getPrismContext().adopt(userType);
                    user = userType.asPrismObject();
                } else {
                    Task task = createSimpleTask(OPERATION_LOAD_USER);

                    StringValue userOid = getPageParameters().get(PARAM_USER_ID);
                    user = getModelService().getObject(UserType.class, userOid.toString(), null, task, result);
                    
                }

        	result.recordSuccess();
        } catch (Exception ex) {
            result.recordFatalError("Couldn't get user.", ex);
            LoggingUtils.logException(LOGGER, "Couldn't load user", ex);
        }

        if (!result.isSuccess()) {
            showResultInSession(result);
        }

        if (user == null) {
            if (isEditingUser()) {
                getSession().error(getString("pageUser.message.cantEditUser"));
            } else {
                getSession().error(getString("pageUser.message.cantNewUser"));
            }
            throw new RestartResponseException(PageUsers.class);
        }

        ContainerStatus status = isEditingUser() ? ContainerStatus.MODIFYING : ContainerStatus.ADDING;
        ObjectWrapper wrapper = new ObjectWrapper(null, null, user, status);
        wrapper.setShowEmpty(!isEditingUser());
        return wrapper;
    }

    private void initLayout() {
        Form mainForm = new Form(ID_MAIN_FORM);
        add(mainForm);

        PrismObjectPanel userForm = new PrismObjectPanel(ID_USER_FORM, userModel, new PackageResourceReference(
                ImgResources.class, ImgResources.USER_PRISM), mainForm) {

            @Override
            protected IModel<String> createDescription(IModel<ObjectWrapper> model) {
                return createStringResource("pageUser.description");
            }
        };
        mainForm.add(userForm);

        Accordion accordion = new Accordion(ID_ACCORDION);
        accordion.setOutputMarkupId(true);
        accordion.setMultipleSelect(true);
        accordion.setExpanded(true);
        mainForm.add(accordion);

        AccordionItem accounts = new AccordionItem(ID_ACCOUNTS_DELTAS, new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                return getString("pageUser.accounts", getAccountsSize().getObject());
            }
        });
        accounts.setOutputMarkupId(true);
        accordion.getBodyContainer().add(accounts);

        WebMarkupContainer accountsButtonsPanel = new WebMarkupContainer(ID_ACCOUNT_BUTTONS);
        accountsButtonsPanel.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return getAccountsSize().getObject() > 0;
            }
        });
        accounts.getBodyContainer().add(accountsButtonsPanel);

        initAccounts(accounts);

        AccordionItem assignments = new AccordionItem(ID_ASSIGNMENTS, new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                return getString("pageUser.assignments", getAssignmentsSize().getObject());
            }
        });
        assignments.setOutputMarkupId(true);
        accordion.getBodyContainer().add(assignments);

        initAssignments(assignments);
        initButtons(mainForm);

        initResourceModal();
        initAssignableModal();
        initConfirmationDialogs();
    }

    private IModel<Integer> getAccountsSize() {
        return new LoadableModel<Integer>() {

            @Override
            protected Integer load() {
                int accountsSize = 0;
                for (UserAccountDto account : accountsModel.getObject()) {
                    if (!UserDtoStatus.DELETE.equals(account.getStatus())) {
                        accountsSize++;
                    }
                }
                return accountsSize;
            }
        };
    }

    private IModel<Integer> getAssignmentsSize() {
        return new LoadableModel<Integer>() {

            @Override
            protected Integer load() {
                int assignmentsSize = 0;
                for (AssignmentEditorDto assign : assignmentsModel.getObject()) {
                    if (!UserDtoStatus.DELETE.equals(assign.getStatus())) {
                        assignmentsSize++;
                    }
                }
                return assignmentsSize;
            }
        };
    }

    private void initConfirmationDialogs() {
        ConfirmationDialog dialog = new ConfirmationDialog(MODAL_ID_CONFIRM_DELETE_ACCOUNT,
                createStringResource("pageUser.title.confirmDelete"), new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                return createStringResource("pageUser.message.deleteAccountConfirm",
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
                createStringResource("pageUser.title.confirmDelete"), new AbstractReadOnlyModel<String>() {

            @Override
            public String getObject() {
                return createStringResource("pageUser.message.deleteAssignmentConfirm",
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
    }

    private void initAccounts(AccordionItem accounts) {
        ListView<UserAccountDto> accountList = new ListView<UserAccountDto>("accountList", accountsModel) {

            @Override
            protected void populateItem(final ListItem<UserAccountDto> item) {

                PrismObjectPanel account = new PrismObjectPanel("account", new PropertyModel<ObjectWrapper>(
                        item.getModel(), "object"), new PackageResourceReference(ImgResources.class,
                        ImgResources.HDD_PRISM), (Form) PageUser.this.get(ID_MAIN_FORM)) {

                    @Override
                    protected Panel createOperationPanel(String id) {
                        return new AccountOperationButtons(id, new PropertyModel<ObjectWrapper>(
                                item.getModel(), "object")) {

                            @Override
                            public void deletePerformed(AjaxRequestTarget target) {
                                deleteAccountPerformed(target, item.getModel());
                            }

                            @Override
                            public void unlinkPerformed(AjaxRequestTarget target) {
                                unlinkAccountPerformed(target, item.getModel());
                            }
                        };
                    }
                };
                item.add(account);
            }
        };

        accounts.getBodyContainer().add(accountList);
    }

    private AccordionItem getAssignmentAccordionItem() {
        Accordion accordion = (Accordion) get(ID_MAIN_FORM + ":" + ID_ACCORDION);
        return (AccordionItem) accordion.getBodyContainer().get(ID_ASSIGNMENTS);
    }

    private AccordionItem getAccountsAccordionItem() {
        Accordion accordion = (Accordion) get(ID_MAIN_FORM + ":" + ID_ACCORDION);
        return (AccordionItem) accordion.getBodyContainer().get(ID_ACCOUNTS_DELTAS);
    }
    
    
    private Accordion getAccordionsItem() {
    	return (Accordion) get(ID_MAIN_FORM + ":" + ID_ACCORDION);
    }
    

    private List<UserAccountDto> loadAccountWrappers() {
        List<UserAccountDto> list = new ArrayList<UserAccountDto>();

        ObjectWrapper user = userModel.getObject();
        PrismObject<UserType> prismUser = user.getObject();
        List<ObjectReferenceType> references = prismUser.asObjectable().getAccountRef();
        OperationResult result = new OperationResult(OPERATION_LOAD_ACCOUNTS);
        Task task = createSimpleTask(OPERATION_LOAD_ACCOUNT);
        for (ObjectReferenceType reference : references) {
            OperationResult subResult = result.createSubresult(OPERATION_LOAD_ACCOUNT);
            try {
				Collection<SelectorOptions<GetOperationOptions>> options =
						SelectorOptions.createCollection(AccountShadowType.F_RESOURCE, GetOperationOptions.createResolve());

				if (reference.getOid() == null){
					continue;
				}
                PrismObject<AccountShadowType> account = getModelService().getObject(AccountShadowType.class,
                        reference.getOid(), options, task, subResult);
                AccountShadowType accountType = account.asObjectable();

                OperationResultType fetchResult = accountType.getFetchResult();
                if (fetchResult != null && !OperationResultStatusType.SUCCESS.equals(fetchResult.getStatus())) {
                    showResult(OperationResult.createOperationResult(fetchResult));
                }

                ResourceType resource = accountType.getResource();
                String resourceName = WebMiscUtil.getName(resource);

                ObjectWrapper wrapper = new ObjectWrapper(resourceName,
                        WebMiscUtil.getOrigStringFromPoly(accountType.getName()), account, ContainerStatus.MODIFYING);
                wrapper.setSelectable(true);
                wrapper.setMinimalized(true);
                list.add(new UserAccountDto(wrapper, UserDtoStatus.MODIFY));

                subResult.recomputeStatus();
            } catch (Exception ex) {
                subResult.recordFatalError("Couldn't load account." + ex.getMessage(), ex);
                LoggingUtils.logException(LOGGER, "Couldn't load account", ex);
            }
        }
        result.recomputeStatus();
        result.recordSuccessIfUnknown();

        if (!result.isSuccess()) {
            showResult(result);
        }

        return list;
    }

    private List<AssignmentEditorDto> loadAssignments() {
        List<AssignmentEditorDto> list = new ArrayList<AssignmentEditorDto>();

        OperationResult result = new OperationResult(OPERATION_LOAD_ASSIGNMENTS);

        ObjectWrapper user = userModel.getObject();
        PrismObject<UserType> prismUser = user.getObject();
        List<AssignmentType> assignments = prismUser.asObjectable().getAssignment();
        for (AssignmentType assignment : assignments) {
            ObjectType targetObject = null;
            AssignmentEditorDtoType type = AssignmentEditorDtoType.ACCOUNT_CONSTRUCTION;
            if (assignment.getTarget() != null) {
                //object assignment
                targetObject = assignment.getTarget();
                type = AssignmentEditorDtoType.getType(targetObject.getClass());
            } else if (assignment.getTargetRef() != null) {
                //object assignment through reference
                ObjectReferenceType ref = assignment.getTargetRef();
                PrismObject target = getReference(ref, result);

                if (target != null) {
                    targetObject = (ObjectType) target.asObjectable();
                    type = AssignmentEditorDtoType.getType(target.getCompileTimeClass());
                }
            } else if (assignment.getAccountConstruction() != null) {
                //account assignment through account construction
                AccountConstructionType construction = assignment.getAccountConstruction();
                if (construction.getResource() != null) {
                    targetObject = construction.getResource();
                } else if (construction.getResourceRef() != null) {
                    ObjectReferenceType ref = construction.getResourceRef();
                    PrismObject target = getReference(ref, result);
                    if (target != null) {
                        targetObject = (ObjectType) target.asObjectable();
                    }
                }
            }

            list.add(new AssignmentEditorDto(targetObject, type, UserDtoStatus.MODIFY, assignment));
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
            target = getModelService().getObject(ObjectType.class, ref.getOid(), null, task,
                    subResult);
            subResult.recordSuccess();
        } catch (Exception ex) {
            LoggingUtils.logException(LOGGER, "Couldn't get assignment target ref", ex);
            subResult.recordFatalError("Couldn't get assignment target ref.", ex);
        }

        return target;
    }

    private void initAssignments(AccordionItem assignments) {
        ListView<AssignmentEditorDto> assignmentList = new ListView<AssignmentEditorDto>(ID_ASSIGNMENT_LIST, assignmentsModel) {

            @Override
            protected void populateItem(final ListItem<AssignmentEditorDto> item) {
                AssignmentEditorPanel assignmentEditor = new AssignmentEditorPanel(ID_ASSIGNMENT_EDITOR, item.getModel());
                item.add(assignmentEditor);
            }
        };

        assignments.getBodyContainer().add(assignmentList);
    }

    private void initButtons(Form mainForm) {
        AjaxSubmitLinkButton save = new AjaxSubmitLinkButton("save", ButtonType.POSITIVE,
                createStringResource("pageUser.button.save")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                savePerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }
        };
        mainForm.add(save);

        AjaxSubmitLinkButton submit = new AjaxSubmitLinkButton("submit", ButtonType.POSITIVE,
                createStringResource("pageUser.button.submit")) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                submitPerformed(target);
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(getFeedbackPanel());
            }
        };
        mainForm.add(submit);

        AjaxLinkButton back = new AjaxLinkButton("back", createStringResource("pageUser.button.back")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                cancelPerformed(target);
            }
        };
        mainForm.add(back);

        initAssignButtons(mainForm);

        WebMarkupContainer buttons = (WebMarkupContainer) getAccountsAccordionItem().getBodyContainer().get(ID_ACCOUNT_BUTTONS);
        initAccountButtons(buttons);

        initAccountButton(mainForm);

        //todo move model object to some dto and use PropertyModel
        CheckBox forceCheck = new CheckBox(ID_FORCE_CHECK, new IModel<Boolean>() {

            @Override
            public Boolean getObject() {
                return forceAction;
            }

            @Override
            public void setObject(Boolean value) {
                forceAction = value;
            }

            @Override
            public void detach() {
            }
        });
        mainForm.add(forceCheck);
    }

    private void initAccountButton(Form mainForm) {
        AjaxLinkButton addAccount = new AjaxLinkButton("addAccount",
                createStringResource("pageUser.button.addAccount")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                showModalWindow(MODAL_ID_RESOURCE, target);
            }
        };
        mainForm.add(addAccount);
    }

    private void initAssignButtons(Form mainForm) {
        AjaxLinkButton addAccountAssign = new AjaxLinkButton("assignAccount",
                createStringResource("pageUser.button.assignAccount")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                showAssignablePopup(target, ResourceType.class);
            }
        };
        mainForm.add(addAccountAssign);

        AjaxLinkButton addRoleAssign = new AjaxLinkButton("assignRole",
                createStringResource("pageUser.button.assignRole")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                showAssignablePopup(target, RoleType.class);
            }
        };
        mainForm.add(addRoleAssign);

        AjaxLinkButton addOrgUnitAssign = new AjaxLinkButton("assignOrgUnit",
                createStringResource("pageUser.button.assignOrgUnit")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                showAssignablePopup(target, OrgType.class);
            }
        };
        mainForm.add(addOrgUnitAssign);

        AjaxLinkButton unassign = new AjaxLinkButton("unassign", ButtonType.NEGATIVE,
                createStringResource("pageUser.button.unassign")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                deleteAssignmentPerformed(target);
            }
        };
        mainForm.add(unassign);
    }

    private void showAssignablePopup(AjaxRequestTarget target, Class<? extends ObjectType> type) {
        ModalWindow modal = (ModalWindow) get(MODAL_ID_ASSIGNABLE);
        AssignablePopupContent content = (AssignablePopupContent) modal.get(modal.getContentId());
        content.setType(type);
        showModalWindow(MODAL_ID_ASSIGNABLE, target);
    }

    private void initAccountButtons(WebMarkupContainer accountsPanel) {
        AjaxLinkButton enableAccount = new AjaxLinkButton("enableAccount",
                createStringResource("pageUser.button.enable")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                updateAccountActivation(target, getSelectedAccounts(), true);
            }
        };
        accountsPanel.add(enableAccount);

        AjaxLinkButton disableAccount = new AjaxLinkButton("disableAccount",
                createStringResource("pageUser.button.disable")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                updateAccountActivation(target, getSelectedAccounts(), false);
            }
        };
        accountsPanel.add(disableAccount);

        AjaxLinkButton unlinkAccount = new AjaxLinkButton("unlinkAccount",
                createStringResource("pageUser.button.unlink")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                unlinkAccountPerformed(target, getSelectedAccounts());
            }
        };
        accountsPanel.add(unlinkAccount);

        AjaxLinkButton deleteAccount = new AjaxLinkButton("deleteAccount", ButtonType.NEGATIVE,
                createStringResource("pageUser.button.delete")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                deleteAccountPerformed(target);
            }
        };
        accountsPanel.add(deleteAccount);

        AjaxLinkButton unlockAccount = new AjaxLinkButton("unlockAccount",
                createStringResource("pageUser.button.unlock")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                unlockAccountPerformed(target, getSelectedAccounts());
            }
        };
        accountsPanel.add(unlockAccount);
    }

    private ModalWindow createModalWindow(final String id, IModel<String> title) {
        final ModalWindow modal = new ModalWindow(id);
        add(modal);

        modal.setResizable(false);
        modal.setTitle(title);
        modal.setCookieName(PageUser.class.getSimpleName() + ((int) (Math.random() * 100)));

        modal.setInitialWidth(1100);
        modal.setWidthUnit("px");
        modal.setInitialHeight(520);
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
                response.renderOnDomReadyJavaScript("Wicket.Window.unloadConfirmation = false;");
                response.renderJavaScript("$(document).ready(function() {\n" +
                        "  $(document).bind('keyup', function(evt) {\n" +
                        "    if (evt.keyCode == 27) {\n" +
                        getCallbackScript() + "\n" +
                        "        evt.preventDefault();\n" +
                        "    }\n" +
                        "  });\n" +
                        "});", id);

            }

			@Override
			protected void respond(AjaxRequestTarget target) {
				modal.close(target);
				
			}
        });
        
        return modal;
    }

    
    private void initResourceModal() {
        ModalWindow window = createModalWindow(MODAL_ID_RESOURCE,
                createStringResource("pageUser.title.selectResource"));

        SimpleUserResourceProvider provider = new SimpleUserResourceProvider(this, accountsModel);
        window.setContent(new ResourcesPopup(window.getContentId(), provider) {

            @Override
            protected void addPerformed(AjaxRequestTarget target, List<ResourceType> newResources) {
                addSelectedAccountPerformed(target, newResources);
            }
        });
        
        add(window);
    }

    private void initAssignableModal() {
        ModalWindow window = createModalWindow(MODAL_ID_ASSIGNABLE,
                createStringResource("pageUser.title.selectAssignable"));
        window.setContent(new AssignablePopupContent(window.getContentId()) {

            @Override
            protected void addPerformed(AjaxRequestTarget target, List<ObjectType> selected) {
                addSelectedAssignablePerformed(target, selected);
            }
        });
        add(window);
    }

    private boolean isEditingUser() {
        StringValue userOid = getPageParameters().get(PageUser.PARAM_USER_ID);
        return userOid != null && StringUtils.isNotEmpty(userOid.toString());
    }

    private void cancelPerformed(AjaxRequestTarget target) {
        setResponsePage(PageUsers.class);
    }

    private List<ObjectDelta<? extends ObjectType>> modifyAccounts(OperationResult result) {
        List<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();

        List<UserAccountDto> accounts = accountsModel.getObject();
        OperationResult subResult = null;
        for (UserAccountDto account : accounts) {
            try {
                ObjectWrapper accountWrapper = account.getObject();
                ObjectDelta delta = accountWrapper.getObjectDelta();
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Account delta computed from form:\n{}", new Object[]{delta.debugDump(3)});
                }

                if (!UserDtoStatus.MODIFY.equals(account.getStatus())){
                    continue;
                }
                
                if (delta.isEmpty() && (accountWrapper.getOldDelta() == null || accountWrapper.getOldDelta().isEmpty())){
                	continue;
                }
                
                if (accountWrapper.getOldDelta() != null){
                	delta = ObjectDelta.summarize(delta, accountWrapper.getOldDelta());
                }
                
                subResult = result.createSubresult(OPERATION_MODIFY_ACCOUNT);

                WebMiscUtil.encryptCredentials(delta, true, getMidpointApplication());
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Modifying account:\n{}", new Object[]{delta.debugDump(3)});
                }

                deltas.add(delta);
                subResult.recordSuccess();
            } catch (Exception ex) {
                if (subResult != null) {
                    subResult.recordFatalError("Couldn't compute account delta.", ex);
                }
                LoggingUtils.logException(LOGGER, "Couldn't compute account delta", ex);
            }
        }

        return deltas;
    }

    private ArrayList<PrismObject> getAccountsForSubmit(OperationResult result,
                                                        Collection<ObjectDelta<? extends ObjectType>> deltas) {
        List<UserAccountDto> accounts = accountsModel.getObject();
        ArrayList<PrismObject> prismAccounts = new ArrayList<PrismObject>();
        OperationResult subResult = null;
        Task task = createSimpleTask(OPERATION_PREPARE_ACCOUNTS);
        for (UserAccountDto account : accounts) {
            prismAccounts.add(account.getObject().getObject());
            try {
            	ObjectWrapper accountWrapper = account.getObject();
            	ObjectDelta delta = accountWrapper.getObjectDelta();
				if (delta.isEmpty()) {
					if (accountWrapper.getOldDelta() == null || accountWrapper.getOldDelta().isEmpty()) {
						continue;
					} else {
						delta = accountWrapper.getOldDelta();
					}
				} else {
					if (accountWrapper.getOldDelta()!= null && !accountWrapper.getOldDelta().isEmpty()){
						delta = ObjectDelta.summarize(delta, accountWrapper.getOldDelta());
					
					} 
				}
            		 
            	 WebMiscUtil.encryptCredentials(delta, true, getMidpointApplication());
            	 subResult = result.createSubresult(OPERATION_PREPARE_ACCOUNTS);
            	 deltas.add(delta);
            	 subResult.recordSuccess();
            } catch (Exception ex) {
            	if (subResult != null) {
            		subResult.recordFatalError("Preparing account failed.", ex);
            	}
            	LoggingUtils.logException(LOGGER, "Couldn't prepare account for submit", ex);
            }
        }
        return prismAccounts;
    }

    private void prepareUserForAdd(PrismObject<UserType> user) throws SchemaException {
        UserType userType = user.asObjectable();
        // handle added accounts
        List<UserAccountDto> accounts = accountsModel.getObject();
        for (UserAccountDto accDto : accounts) {
            if (!UserDtoStatus.ADD.equals(accDto.getStatus())) {
                warn(getString("pageUser.message.illegalAccountState", accDto.getStatus()));
                continue;
            }

            ObjectWrapper accountWrapper = accDto.getObject();
            ObjectDelta delta = accountWrapper.getObjectDelta();
            PrismObject<AccountShadowType> account = delta.getObjectToAdd();
            WebMiscUtil.encryptCredentials(account, true, getMidpointApplication());

            userType.getAccount().add(account.asObjectable());
        }

        PrismObjectDefinition userDef = user.getDefinition();
        PrismContainerDefinition assignmentDef = userDef.findContainerDefinition(UserType.F_ASSIGNMENT);

        // handle added assignments
        List<AssignmentEditorDto> assignments = assignmentsModel.getObject();
        for (AssignmentEditorDto assDto : assignments) {
            if (!UserDtoStatus.ADD.equals(assDto.getStatus())) {
                warn(getString("pageUser.message.illegalAssignmentState", assDto.getStatus()));
                continue;
            }

            AssignmentType assignment = new AssignmentType();
            PrismContainerValue value = assDto.getNewValue();
            assignment.setupContainerValue(value);
            userType.getAssignment().add(assignment.clone());

            value.applyDefinition(assignmentDef, false);
            //todo remove this block [lazyman] after model is updated - it has to remove resource from accountConstruction
            removeResourceFromAccConstruction(assignment);
        }
    }

    /**
     * remove this method after model is updated - it has to remove resource from accountConstruction
     */
    @Deprecated
    private void removeResourceFromAccConstruction(AssignmentType assignment) {
        AccountConstructionType accConstruction = assignment.getAccountConstruction();
        if (accConstruction == null || accConstruction.getResource() == null) {
            return;
        }

        ObjectReferenceType ref = new ObjectReferenceType();
        ref.setOid(assignment.getAccountConstruction().getResource().getOid());
        ref.setType(ResourceType.COMPLEX_TYPE);
        assignment.getAccountConstruction().setResourceRef(ref);
        assignment.getAccountConstruction().setResource(null);
    }

    private ReferenceDelta prepareUserAccountsDeltaForModify(PrismReferenceDefinition refDef)
            throws SchemaException {
        ReferenceDelta refDelta = new ReferenceDelta(refDef);

        List<UserAccountDto> accounts = accountsModel.getObject();
        for (UserAccountDto accDto : accounts) {
            ObjectWrapper accountWrapper = accDto.getObject();
            ObjectDelta delta = accountWrapper.getObjectDelta();
            PrismReferenceValue refValue = new PrismReferenceValue(null, OriginType.USER_ACTION, null);

            PrismObject<AccountShadowType> account;
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
                    // nothing to do, account modifications were applied before
                    continue;
                case UNLINK:
                    refValue.setOid(delta.getOid());
                    refValue.setTargetType(AccountShadowType.COMPLEX_TYPE);
                    refDelta.addValueToDelete(refValue);
                    break;
                default:
                    warn(getString("pageUser.message.illegalAccountState", accDto.getStatus()));
            }
        }

        return refDelta;
    }

    private ContainerDelta handleAssignmentDeltas(ObjectDelta<UserType> userDelta, PrismContainerDefinition def)
            throws SchemaException {
        ContainerDelta assDelta = new ContainerDelta(new ItemPath(), UserType.F_ASSIGNMENT, def);

        PrismObject<UserType> user = userModel.getObject().getObject();
        PrismObjectDefinition userDef = user.getDefinition();
        PrismContainerDefinition assignmentDef = userDef.findContainerDefinition(UserType.F_ASSIGNMENT);

        List<AssignmentEditorDto> assignments = assignmentsModel.getObject();
        for (AssignmentEditorDto assDto : assignments) {
            PrismContainerValue newValue = assDto.getNewValue();
            switch (assDto.getStatus()) {
                case ADD:
                case DELETE:
                    newValue.applyDefinition(assignmentDef, false);
                    if (UserDtoStatus.ADD.equals(assDto.getStatus())) {
                        assDelta.addValueToAdd(newValue.clone());
                    } else {
                        assDelta.addValueToDelete(newValue.clone());
                    }
                    break;
                case MODIFY:
                    if (!assDto.isModified()) {
                        LOGGER.trace("Assignment '{}' not modified.", new Object[]{assDto.getName()});
                        continue;
                    }

                    handleModifyAssignmentDelta(assDto, assignmentDef, newValue, userDelta);
                    break;
                default:
                    warn(getString("pageUser.message.illegalAssignmentState", assDto.getStatus()));
            }
        }

        if (!assDelta.isEmpty()) {
            userDelta.addModification(assDelta);
        }

        //todo remove this block [lazyman] after model is updated - it has to remove resource from accountConstruction
        Collection<PrismContainerValue> values = assDelta.getValues(PrismContainerValue.class);
        for (PrismContainerValue value : values) {
            AssignmentType ass = new AssignmentType();
            ass.setupContainerValue(value);
            removeResourceFromAccConstruction(ass);
        }

        return assDelta;
    }

    private void handleModifyAssignmentDelta(AssignmentEditorDto assDto, PrismContainerDefinition assignmentDef,
                                             PrismContainerValue newValue, ObjectDelta<UserType> userDelta) throws SchemaException {
        LOGGER.debug("Handling modified assignment '{}', computing delta.", new Object[]{assDto.getName()});

        PrismValue oldValue = assDto.getOldValue();
        Collection<? extends ItemDelta> deltas = oldValue.diff(newValue);
        List<ItemPathSegment> pathSegments = oldValue.getPath(null).getSegments();

        for (ItemDelta delta : deltas) {
            ItemPath deltaPath = delta.getPath();
            ItemDefinition deltaDef = assignmentDef.findItemDefinition(deltaPath);
            //replace relative path - add "assignment" path prefix
            List<ItemPathSegment> newPath = new ArrayList<ItemPathSegment>();
            newPath.addAll(pathSegments);
            newPath.addAll(delta.getParentPath().getSegments());
            //add definition to item delta
            delta.setParentPath(new ItemPath(newPath));
            delta.applyDefinition(deltaDef);

            userDelta.addModification(delta);
        }
    }

    private void prepareUserDeltaForModify(ObjectDelta<UserType> userDelta) throws SchemaException {
        // handle accounts
        SchemaRegistry registry = getPrismContext().getSchemaRegistry();
        PrismObjectDefinition objectDefinition = registry
                .findObjectDefinitionByCompileTimeClass(UserType.class);
        PrismReferenceDefinition refDef = objectDefinition.findReferenceDefinition(UserType.F_ACCOUNT_REF);
        ReferenceDelta refDelta = prepareUserAccountsDeltaForModify(refDef);
        if (!refDelta.isEmpty()) {
            userDelta.addModification(refDelta);
        }

        // handle assignments
        PrismContainerDefinition def = objectDefinition.findContainerDefinition(UserType.F_ASSIGNMENT);
        handleAssignmentDeltas(userDelta, def);
    }

    private void savePerformed(AjaxRequestTarget target) {
        LOGGER.debug("Save user.");

        OperationResult result = new OperationResult(OPERATION_SAVE);
        ObjectWrapper userWrapper = userModel.getObject();
        //todo: improve, delta variable is quickfix for MID-1006
        //redirecting to user list page everytime user is created in repository during user add in gui,
        //and we're not taking care about account/assignment create errors (error message is still displayed)
        ObjectDelta delta = null;
        
        Task task = createSimpleTask(OPERATION_SEND_TO_SUBMIT);
        ModelExecuteOptions options = new ModelExecuteOptions();
        options.setForce(forceAction);
//        try {
        
            try{
        	delta = userWrapper.getObjectDelta();
        	if (userWrapper.getOldDelta() != null){
        		delta = ObjectDelta.summarize(delta, userWrapper.getOldDelta());
        	}
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("User delta computed from form:\n{}", new Object[]{delta.debugDump(3)});
            }
           
            LOGGER.debug("Using force flag: {}.", new Object[]{forceAction});
            }catch (Exception ex){
            	result.recordFatalError(getString("pageUser.message.cantCreateUser"), ex);
 				LoggingUtils.logException(LOGGER, getString("pageUser.message.cantCreateUser"), ex);
 				showResult(result);
 				return;
            }

            switch (userWrapper.getStatus()) {
                case ADDING:
                	try{
                    PrismObject<UserType> user = delta.getObjectToAdd();
                    WebMiscUtil.encryptCredentials(user, true, getMidpointApplication());
                    prepareUserForAdd(user);
                    getPrismContext().adopt(user, UserType.class);
                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("Delta before add user:\n{}", new Object[]{delta.debugDump(3)});
                    }

                    if (!delta.isEmpty()) {
                        getModelService().executeChanges(WebMiscUtil.createDeltaCollection(delta), options, task, result);
                    } else {
                        result.recordSuccess();
                    }
                	}catch (Exception ex){
                		result.recordFatalError(getString("pageUser.message.cantCreateUser"), ex);
         				LoggingUtils.logException(LOGGER, getString("pageUser.message.cantCreateUser"), ex);
                	}
                    break;
                	
                case MODIFYING:
                	try{
                    WebMiscUtil.encryptCredentials(delta, true, getMidpointApplication());
                    prepareUserDeltaForModify(delta);

                    if (LOGGER.isTraceEnabled()) {
                        LOGGER.trace("Delta before modify user:\n{}", new Object[]{delta.debugDump(3)});
                    }

                    List<ObjectDelta<? extends ObjectType>> accountDeltas = modifyAccounts(result);
                    Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
                    
                    if (!delta.isEmpty()) {
                        deltas.add(delta);
                    }
                    for (ObjectDelta accDelta : accountDeltas) {
                        if (!accDelta.isEmpty()) {
                            deltas.add(accDelta);
                        }
                    }

                    if (!deltas.isEmpty()) {
                        getModelService().executeChanges(deltas, options, task, result);
                    } else {
                        result.recordSuccess();
                    }
                  
                	 } catch (Exception ex) {
             			if (!executeForceDelete(userWrapper, task, options, result)) {
             				result.recordFatalError(getString("pageUser.message.cantUpdateUser"), ex);
             				LoggingUtils.logException(LOGGER, getString("pageUser.message.cantUpdateUser"), ex);
             			} else{
             				result.recomputeStatus();
             			}
                     }
                	  break;
                // support for add/delete containers (e.g. delete credentials)
                default:
                    error(getString("pageUser.message.unsupportedState", userWrapper.getStatus()));
            }

            result.recomputeStatus();
//        } catch (Exception ex) {
//			if (!executeForceDelete(userWrapper, task, options, result)) {
//				result.recordFatalError(getString("pageUser.message.cantCreateUser"), ex);
//				LoggingUtils.logException(LOGGER, getString("pageUser.message.cantCreateUser"), ex);
//			} else{
//				result.recomputeStatus();
//			}
//        }

        boolean userAdded = delta != null && delta.isAdd() && StringUtils.isNotEmpty(delta.getOid());
		if (userAdded || result.isSuccess() || result.isHandledError() || result.isInProgress()) {
			showResultInSession(result);
            //todo refactor this...what is this for? why it's using some "shadow" param from result???
			PrismObject<UserType> user = userWrapper.getObject();
			UserType userType = user.asObjectable();
			for (ObjectReferenceType ref : userType.getAccountRef()) {
				Object o = findParam("shadow", ref.getOid(), result);
				if (o != null && o instanceof AccountShadowType) {
					AccountShadowType accountType = (AccountShadowType) o;
					OperationResultType fetchResult = accountType.getFetchResult();
					if (fetchResult != null && !OperationResultStatusType.SUCCESS.equals(fetchResult.getStatus())) {
						showResultInSession(OperationResult.createOperationResult(fetchResult));
					}
				}
			}
			setResponsePage(PageUsers.class);
        } else {
            showResult(result);
            target.add(getFeedbackPanel());
        }
        
    }
    
//    private ModelContext previewForceDelete(ObjectDelta delta, ModelExecuteOptions options, Task task, OperationResult parentResult){
//    		OperationResult result = parentResult.createSubresult("Force preview operation");
//		try {
//			
//			
//			result.recomputeStatus();
//			return context;
//		} catch (Exception ex) {
//			result.recordFatalError("Failed to preview changes: " + ex.getMessage(), ex);
//			LoggingUtils.logException(LOGGER, "Failed to preview changes", ex);
//
//		}
//		return null;
//    }
    
	private boolean executeForceDelete(ObjectWrapper userWrapper, Task task, ModelExecuteOptions options,
			OperationResult parentResult){
		if (forceAction) {
			OperationResult result = parentResult.createSubresult("Force delete operation");
//			List<UserAccountDto> accountDtos = accountsModel.getObject();
//			List<ReferenceDelta> refDeltas = new ArrayList<ReferenceDelta>();
//			ObjectDelta<UserType> forceDeleteDelta = null;
//			for (UserAccountDto accDto : accountDtos) {
//				if (accDto.getStatus() == UserDtoStatus.DELETE) {
//					ObjectWrapper accWrapper = accDto.getObject();
//					ReferenceDelta refDelta = ReferenceDelta.createModificationDelete(UserType.F_ACCOUNT_REF,
//							userWrapper.getObject().getDefinition(), accWrapper.getObject());
//					refDeltas.add(refDelta);
//				}
//			}
//			if (!refDeltas.isEmpty()) {
//				forceDeleteDelta = ObjectDelta.createModifyDelta(userWrapper.getObject().getOid(), refDeltas,
//						UserType.class, getPrismContext());
//			}
//			PrismContainerDefinition def = userWrapper.getObject().findContainer(UserType.F_ASSIGNMENT).getDefinition();
//			if (forceDeleteDelta == null) {
//				forceDeleteDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, userWrapper.getObject().getOid(),
//						getPrismContext());
//			}
			try {
				ObjectDelta<UserType> forceDeleteDelta = getChange(userWrapper);
				if (forceDeleteDelta != null && !forceDeleteDelta.isEmpty()) {
					getModelService().executeChanges(WebMiscUtil.createDeltaCollection(forceDeleteDelta), options,
							task, result);
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
	
	private ObjectDelta getChange(ObjectWrapper userWrapper) throws SchemaException{
		
			List<UserAccountDto> accountDtos = accountsModel.getObject();
			List<ReferenceDelta> refDeltas = new ArrayList<ReferenceDelta>();
			ObjectDelta<UserType> forceDeleteDelta = null;
			for (UserAccountDto accDto : accountDtos) {
				if (accDto.getStatus() == UserDtoStatus.DELETE) {
					ObjectWrapper accWrapper = accDto.getObject();
					ReferenceDelta refDelta = ReferenceDelta.createModificationDelete(UserType.F_ACCOUNT_REF,
							userWrapper.getObject().getDefinition(), accWrapper.getObject());
					refDeltas.add(refDelta);
				}
			}
			if (!refDeltas.isEmpty()) {
				forceDeleteDelta = ObjectDelta.createModifyDelta(userWrapper.getObject().getOid(), refDeltas,
						UserType.class, getPrismContext());
			}
			PrismContainerDefinition def = userWrapper.getObject().findContainer(UserType.F_ASSIGNMENT).getDefinition();
			if (forceDeleteDelta == null) {
				forceDeleteDelta = ObjectDelta.createEmptyModifyDelta(UserType.class, userWrapper.getObject().getOid(),
						getPrismContext());
			}
			
			handleAssignmentDeltas(forceDeleteDelta, def);
			return forceDeleteDelta;
	}
	
    private Object object;

   	public Object findParam(String param, String oid, OperationResult result) {
   		
   		for (OperationResult subResult : result.getSubresults()) {
   			if (subResult != null && subResult.getParams() != null) {
   				if (subResult.getParams().get(param) != null && subResult.getParams().get(OperationResult.PARAM_OID) != null
   						&& subResult.getParams().get(OperationResult.PARAM_OID).equals(oid)) {
   					return subResult.getParams().get(param);
   				}
   				object = findParam(param, oid, subResult);

   			}
   		}
   		return object;
   	}

    private void submitPerformed(AjaxRequestTarget target) {
        LOGGER.debug("Submit user.");

        Task task = createSimpleTask(OPERATION_SEND_TO_SUBMIT);
        OperationResult result = task.getResult();
        Collection<ObjectDelta<? extends ObjectType>> deltas = new ArrayList<ObjectDelta<? extends ObjectType>>();
        ArrayList<PrismObject> accountsBeforeModify = getAccountsForSubmit(result, deltas);

        ObjectWrapper userWrapper = userModel.getObject();
        ObjectDelta delta = null;
        ModelContext changes = null;
        ModelExecuteOptions options = null;
        if (forceAction){
        	options = ModelExecuteOptions.createForce();
        }
        try {
            delta = userWrapper.getObjectDelta();
            if (userWrapper.getOldDelta() != null){
            	delta = ObjectDelta.summarize(delta, userWrapper.getOldDelta());
            }
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("User delta computed from form:\n{}", new Object[]{delta.debugDump(3)});
            }
            
            switch (userWrapper.getStatus()) {
                case ADDING:
                    PrismContainer password = delta.getObjectToAdd().findContainer(
                            SchemaConstants.PATH_PASSWORD);
                    if (password == null) {
                        result.recordFatalError(getString("pageUser.message.noPassword"));
                        break;
                    }
                    PrismObject<UserType> user = delta.getObjectToAdd();
                    WebMiscUtil.encryptCredentials(user, true, getMidpointApplication());
                    prepareUserForAdd(user);
                    getPrismContext().adopt(user, UserType.class);
                    deltas.add(delta);
                    changes = getModelInteractionService().previewChanges(deltas, null, task, result);
                    result.recordSuccess();
                    break;
                case MODIFYING:
                    WebMiscUtil.encryptCredentials(delta, true, getMidpointApplication());
                    prepareUserDeltaForModify(delta);
                    deltas.add(delta);
                    changes = getModelInteractionService().previewChanges(deltas, null, task, result);
                    result.recordSuccess();
                    break;
                // support for add/delete containers (e.g. delete credentials)

                default:
                    error(getString("pageUser.message.unsupportedState", userWrapper.getStatus()));
            }

            result.recomputeStatus();
        } catch (Exception ex) {
			if (forceAction) {
				try{
				ObjectDelta<UserType> forceDelta = getChange(userWrapper);
				deltas = WebMiscUtil.createDeltaCollection(delta);
				changes = getModelInteractionService().previewChanges(deltas
						, options, task, result);
//				changes = previewForceDelete(forceDelta, options, task, result);
				result.recordSuccess();
				} catch(Exception e){
					result.recordFatalError(getString("pageUser.message.cantSubmitUser"), e);
					LoggingUtils.logException(LOGGER, getString("pageUser.message.cantSubmitUser"), e);
				}
			}
			else{
				result.recordFatalError(getString("pageUser.message.cantSubmitUser"), ex);
				LoggingUtils.logException(LOGGER, getString("pageUser.message.cantSubmitUser"), ex);
			}
        }

        if (result.isSuccess() || result.isHandledError()) {
            PageUserPreview pageUserPreview = new PageUserPreview(changes, deltas, delta, accountsBeforeModify, forceAction);
            setResponsePage(pageUserPreview);
        } else {
            showResult(result);
            target.add(getFeedbackPanel());
        }
    }

    private List<UserAccountDto> getSelectedAccounts() {
        List<UserAccountDto> selected = new ArrayList<UserAccountDto>();

        List<UserAccountDto> all = accountsModel.getObject();
        for (UserAccountDto account : all) {
            if (account.getObject().isSelected()) {
                selected.add(account);
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
            target.add(getFeedbackPanel());
            return;
        }

        for (ResourceType resource : newResources) {
            try {
                AccountShadowType shadow = new AccountShadowType();
                shadow.setResource(resource);

                RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(
                        resource.asPrismObject(), getPrismContext());
                if (refinedSchema == null) {
                    error(getString("pageUser.message.couldntCreateAccountNoSchema", resource.getName()));
                    continue;
                }

                QName objectClass = refinedSchema.getDefaultAccountDefinition().getObjectClassDefinition()
                        .getTypeName();
                shadow.setObjectClass(objectClass);

                getPrismContext().adopt(shadow);

                ObjectWrapper wrapper = new ObjectWrapper(WebMiscUtil.getOrigStringFromPoly(resource.getName()), null,
                        shadow.asPrismObject(), ContainerStatus.ADDING);
                wrapper.setShowEmpty(true);
                wrapper.setMinimalized(false);
                accountsModel.getObject().add(new UserAccountDto(wrapper, UserDtoStatus.ADD));
                setResponsePage(getPage());
            } catch (Exception ex) {
                error(getString("pageUser.message.couldntCreateAccount", resource.getName(), ex.getMessage()));
                LoggingUtils.logException(LOGGER, "Couldn't create account", ex);
            }
        }

        target.add(getFeedbackPanel(), getAccordionsItem());
    }

    private void addSelectedResourceAssignPerformed(ResourceType resource) {
        AssignmentType assignment = new AssignmentType();
        AccountConstructionType construction = new AccountConstructionType();
        assignment.setAccountConstruction(construction);
        construction.setResource(resource);

        List<AssignmentEditorDto> assignments = assignmentsModel.getObject();
        AssignmentEditorDto dto = new AssignmentEditorDto(resource, AssignmentEditorDtoType.ACCOUNT_CONSTRUCTION,
                UserDtoStatus.ADD, assignment);
        assignments.add(dto);

        dto.setMinimized(false);
        dto.setShowEmpty(true);
    }

    private void addSelectedAssignablePerformed(AjaxRequestTarget target, List<ObjectType> newAssignables) {
        ModalWindow window = (ModalWindow) get(MODAL_ID_ASSIGNABLE);
        window.close(target);

        if (newAssignables.isEmpty()) {
            warn(getString("pageUser.message.noAssignableSelected"));
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

                AssignmentType assignment = new AssignmentType();
                assignment.setTargetRef(targetRef);

                AssignmentEditorDto dto = new AssignmentEditorDto(object, aType, UserDtoStatus.ADD, assignment);
                dto.setMinimized(false);
                dto.setShowEmpty(true);

                assignments.add(dto);
            } catch (Exception ex) {
                error(getString("pageUser.message.couldntAssignObject", object.getName(), ex.getMessage()));
                LoggingUtils.logException(LOGGER, "Couldn't assign object", ex);
            }
        }

        target.add(getFeedbackPanel(), getAccordionsItem());
    }

    private void updateAccountActivation(AjaxRequestTarget target, List<UserAccountDto> accounts,
                                         boolean enabled) {
        if (!isAnyAccountSelected(target)) {
            return;
        }

        for (UserAccountDto account : accounts) {
            ObjectWrapper wrapper = account.getObject();
            ContainerWrapper activation = wrapper.findContainerWrapper(new ItemPath(
                    ResourceObjectShadowType.F_ACTIVATION));
            if (activation == null) {
                warn(getString("pageUser.message.noActivationFound", wrapper.getDisplayName()));
                continue;
            }

            PropertyWrapper enabledProperty = activation.findPropertyWrapper(ActivationType.F_ENABLED);
            if (enabledProperty.getValues().size() != 1) {
                warn(getString("pageUser.message.noEnabledPropertyFound", wrapper.getDisplayName()));
                continue;
            }
            ValueWrapper value = enabledProperty.getValues().get(0);
            value.getValue().setValue(enabled);

            wrapper.setSelected(false);
        }

        target.add(getFeedbackPanel(), getAccordionsItem());
    }

    private boolean isAnyAccountSelected(AjaxRequestTarget target) {
        List<UserAccountDto> selected = getSelectedAccounts();
        if (selected.isEmpty()) {
            warn(getString("pageUser.message.noAccountSelected"));
            target.add(getFeedbackPanel());
            return false;
        }

        return true;
    }

    private void deleteAccountPerformed(AjaxRequestTarget target) {
        if (!isAnyAccountSelected(target)) {
            return;
        }

        showModalWindow(MODAL_ID_CONFIRM_DELETE_ACCOUNT, target);
    }

    private void showModalWindow(String id, AjaxRequestTarget target) {
        ModalWindow window = (ModalWindow) get(id);
        window.show(target);
    }

    private void deleteAccountConfirmedPerformed(AjaxRequestTarget target, List<UserAccountDto> selected) {
        List<UserAccountDto> accounts = accountsModel.getObject();
        for (UserAccountDto account : selected) {
            if (UserDtoStatus.ADD.equals(account.getStatus())) {
                accounts.remove(account);
            } else {
                account.setStatus(UserDtoStatus.DELETE);
            }
        }
        target.add(getAccordionsItem());
    }

    private void deleteAssignmentConfirmedPerformed(AjaxRequestTarget target, List<AssignmentEditorDto> selected) {
        List<AssignmentEditorDto> assignments = assignmentsModel.getObject();
        for (AssignmentEditorDto assignment : selected) {
            if (UserDtoStatus.ADD.equals(assignment.getStatus())) {
                assignments.remove(assignment);
            } else {
                assignment.setStatus(UserDtoStatus.DELETE);
                assignment.setSelected(false);
            }
        }

        target.add(getFeedbackPanel(), getAccordionsItem());
    }

    private void unlinkAccountPerformed(AjaxRequestTarget target, List<UserAccountDto> selected) {
        if (!isAnyAccountSelected(target)) {
            return;
        }

        for (UserAccountDto account : selected) {
            if (UserDtoStatus.ADD.equals(account.getStatus())) {
                continue;
            }
            account.setStatus(UserDtoStatus.UNLINK);
        }
        target.add(getAccordionsItem());
    }

    private void unlinkAccountPerformed(AjaxRequestTarget target, IModel<UserAccountDto> model) {
        UserAccountDto dto = model.getObject();
        if (UserDtoStatus.ADD.equals(dto.getStatus())) {
            return;
        }
		if (UserDtoStatus.UNLINK == dto.getStatus()) {
			dto.setStatus(UserDtoStatus.MODIFY);
		} else {
			dto.setStatus(UserDtoStatus.UNLINK);
		}
        target.add(getAccordionsItem());
    }

//    private void linkAccountPerformed(AjaxRequestTarget target, IModel<UserAccountDto> model) {
//        UserAccountDto dto = model.getObject();
//        if (UserDtoStatus.ADD.equals(dto.getStatus())) {
//            return;
//        }
//        dto.setStatus(UserDtoStatus.MODIFY);
//        target.add(getAccordionsItem());
//    }

    
    private void deleteAccountPerformed(AjaxRequestTarget target, IModel<UserAccountDto> model) {
        List<UserAccountDto> accounts = accountsModel.getObject();
        UserAccountDto account = model.getObject();

        if (UserDtoStatus.ADD.equals(account.getStatus())) {
            accounts.remove(account);
        } else {
        	if (UserDtoStatus.DELETE == account.getStatus()){
        		account.setStatus(UserDtoStatus.MODIFY);
        	} else{
        		account.setStatus(UserDtoStatus.DELETE);
        	}
        }
//        target.appendJavaScript("window.location.reload()");
        target.add(getAccordionsItem());
    }
    
//    private void undeleteAccountPerformed(AjaxRequestTarget target, IModel<UserAccountDto> model) {
////        List<UserAccountDto> accounts = accountsModel.getObject();
//        UserAccountDto account = model.getObject();
//        account.setStatus(UserDtoStatus.MODIFY);
////        if (UserDtoStatus.ADD.equals(account.getStatus())) {
////            accounts.remove(account);
////        } else {
////            account.setStatus(UserDtoStatus.DELETE);
////        }
////        target.appendJavaScript("window.location.reload()");
//        target.add(getAccordionsItem());
//    }

    private void unlockAccountPerformed(AjaxRequestTarget target, List<UserAccountDto> selected) {
        if (!isAnyAccountSelected(target)) {
            return;
        }

        for (UserAccountDto account : selected) {
            // TODO: implement unlock
        }
    }

    private void deleteAssignmentPerformed(AjaxRequestTarget target) {
        List<AssignmentEditorDto> selected = getSelectedAssignments();
        if (selected.isEmpty()) {
            warn(getString("pageUser.message.noAssignmentSelected"));
            target.add(getFeedbackPanel());
            return;
        }

        showModalWindow(MODAL_ID_CONFIRM_DELETE_ASSIGNMENT, target);
    }
}

package com.evolveum.midpoint.web.page.admin.resources;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RetrieveOption;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.AuthorizationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider2;
import com.evolveum.midpoint.web.component.data.column.CheckBoxColumn;
import com.evolveum.midpoint.web.component.data.column.EnumPropertyColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.dialog.ConfirmationDialog;
import com.evolveum.midpoint.web.component.dialog.UserBrowserDialog;
import com.evolveum.midpoint.web.component.input.AutoCompleteTextPanel;
import com.evolveum.midpoint.web.component.input.TwoStateBooleanPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.resources.content.PageAccount;
import com.evolveum.midpoint.web.page.admin.resources.content.PageContentAccounts;
import com.evolveum.midpoint.web.page.admin.resources.content.dto.AccountContentDto;
import com.evolveum.midpoint.web.page.admin.resources.content.dto.AccountOwnerChangeDto;
import com.evolveum.midpoint.web.page.admin.users.PageUser;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SynchronizationSituationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class ResourceContentTabPanel extends Panel {

	private static final Trace LOGGER = TraceManager.getTrace(PageContentAccounts.class);

	private static final String DOT_CLASS = PageContentAccounts.class.getName() + ".";
	private static final String OPERATION_CHANGE_OWNER = DOT_CLASS + "changeOwner";
	private static final String OPERATION_CREATE_USER_FROM_ACCOUNTS = DOT_CLASS + "createUserFromAccounts";
	private static final String OPERATION_CREATE_USER_FROM_ACCOUNT = DOT_CLASS + "createUserFromAccount";
	private static final String OPERATION_DELETE_ACCOUNT_FROM_RESOURCE = DOT_CLASS
			+ "deleteAccountFromResource";
	private static final String OPERATION_ADJUST_ACCOUNT_STATUS = "changeAccountActivationStatus";
	private static final String OPERATION_LOAD_OWNER = DOT_CLASS + "loadOwner";

	private static final String MODAL_ID_OWNER_CHANGE = "ownerChangePopup";
	private static final String MODAL_ID_CONFIRM_DELETE = "confirmDeletePopup";

	private static final String ID_MAIN_FORM = "mainForm";
	private static final String ID_INTENT = "intent";
	private static final String ID_BASIC_SEARCH = "basicSearch";
	private static final String ID_SEARCH_TYPE = "searchType";
	private static final String ID_NAME_CHECK = "nameCheck";
	private static final String ID_IDENTIFIERS_CHECK = "identifiersCheck";
	private static final String ID_TABLE = "table";
	private static final String ID_OBJECTCLASS_FORM = "objectclassForm";
	private static final String ID_OBJECTCLASS_INPUT = "objectclassInput";

	private PageBase parentPage;
	private ShadowKindType kind;

	private Model<Boolean> searchTypeModel = new Model<Boolean>(true);

	private IModel<String> intentModel;

	// IModel<RefinedObjectClassDefinition> objectClassModel;

	private LoadableModel<AccountOwnerChangeDto> ownerChangeModel;

	// RefinedResourceSchema refinedSchema;

	public ResourceContentTabPanel(String id, ShadowKindType kind,
			final IModel<PrismObject<ResourceType>> model, PageBase parentPage) {
		super(id, model);
		this.parentPage = parentPage;
		this.kind = kind;

		ownerChangeModel = new LoadableModel<AccountOwnerChangeDto>(false) {

			@Override
			protected AccountOwnerChangeDto load() {
				return new AccountOwnerChangeDto();
			}
		};

		intentModel = new Model();
		// objectClassModel = createObjectClassModel(model);

		initLayout(model);
	}

	private IModel<RefinedObjectClassDefinition> createObjectClassModel(
			final IModel<PrismObject<ResourceType>> resourceModel) {
		return new LoadableModel<RefinedObjectClassDefinition>(false) {
			@Override
			protected RefinedObjectClassDefinition load() {
				try {
					return getDefinitionByKind(resourceModel);
				} catch (Exception ex) {
					throw new SystemException(ex.getMessage(), ex);
				}
			}

			@Override
			public void setObject(RefinedObjectClassDefinition object) {
				super.setObject(object);
			}

		};
	}

	private RefinedObjectClassDefinition getDefinitionByKind(IModel<PrismObject<ResourceType>> resourceModel)
			throws SchemaException {
		RefinedResourceSchema refinedSchema = RefinedResourceSchema
				.getRefinedSchema(resourceModel.getObject(), parentPage.getPrismContext());
		return refinedSchema.getRefinedDefinition(kind, intentModel.getObject());

	}

	private void initLayout(final IModel<PrismObject<ResourceType>> model) {

		add(new VisibleEnableBehaviour() {

			@Override
			public boolean isEnabled() {
				try {
					return getDefinitionByKind(model) != null;
				} catch (SchemaException e) {
					return true;
				}
			}

			@Override
			public boolean isVisible() {
				try {
					return getDefinitionByKind(model) != null;
				} catch (SchemaException e) {
					return true;
				}
			}
		});

		AutoCompleteTextPanel<String> intent = new AutoCompleteTextPanel<String>(ID_INTENT, intentModel,
				String.class) {

			@Override
			public Iterator<String> getIterator(String input) {
				RefinedResourceSchema refinedSchema = null;
				try {
					refinedSchema = RefinedResourceSchema.getRefinedSchema(model.getObject(),
							parentPage.getPrismContext());

				} catch (SchemaException e) {
					return new ArrayList().iterator();
				}
				return RefinedResourceSchema.getIntentsForKind(refinedSchema, kind).iterator();

			}
			
			
		};
		intent.getBaseFormComponent().add(new AjaxFormComponentUpdatingBehavior("change") {
		
			
			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				BoxedTablePanel table = (BoxedTablePanel) get(parentPage.createComponentPath(ID_MAIN_FORM, ID_TABLE));
				ObjectDataProvider2 provider = (ObjectDataProvider2) table.getDataTable().getDataProvider();
				try {
					provider.setQuery(createQuery((IModel<PrismObject<ResourceType>>) ResourceContentTabPanel.this.getDefaultModel()));
				} catch (SchemaException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				target.add(table);
				
			}
		});
		add(intent);
		final Form mainForm = new Form(ID_MAIN_FORM);
		mainForm.setOutputMarkupId(true);

		TwoStateBooleanPanel searchType = new TwoStateBooleanPanel(ID_SEARCH_TYPE, searchTypeModel,
				"ResourceContentTabPanel.search.repo", "ResourceContentTabPanel.search.resource", null) {

			@Override
			protected void onStateChanged(AjaxRequestTarget target, Boolean newValue) {
				target.add(ResourceContentTabPanel.this);
			}
		};
		add(searchType);
		// Form searchForm = new Form(ID_SEARCH_FORM);
		// add(searchForm);

		// CheckBox nameCheck = new CheckBox(ID_NAME_CHECK, new
		// PropertyModel(searchModel, AccountContentSearchDto.F_NAME));
		// searchForm.add(nameCheck);
		//
		// CheckBox identifiersCheck = new CheckBox(ID_IDENTIFIERS_CHECK,
		// new PropertyModel(searchModel,
		// AccountContentSearchDto.F_IDENTIFIERS));
		// searchForm.add(identifiersCheck);
		//
		// BasicSearchPanel<AccountContentSearchDto> basicSearch = new
		// BasicSearchPanel<AccountContentSearchDto>(ID_BASIC_SEARCH) {
		//
		// @Override
		// protected IModel<String> createSearchTextModel() {
		// return new PropertyModel<>(searchModel,
		// AccountContentSearchDto.F_SEARCH_TEXT);
		// }
		//
		// @Override
		// protected void searchPerformed(AjaxRequestTarget target) {
		// PageContentAccounts.this.searchPerformed(target);
		// }
		//
		// @Override
		// protected void clearSearchPerformed(AjaxRequestTarget target) {
		// PageContentAccounts.this.clearSearchPerformed(target);
		// }
		// };
		// searchForm.add(basicSearch);

		IModel<RefinedObjectClassDefinition> objectClassModel = createObjectClassModel(model);

		add(mainForm);

		ObjectDataProvider2<AccountContentDto, ShadowType> provider = new ObjectDataProvider2<AccountContentDto, ShadowType>(
				this, ShadowType.class) {

			@Override
			public AccountContentDto createDataObjectWrapper(ShadowType obj) {
				return createAccountContentDto(obj, new OperationResult("Create resource content dto."));
			}
			
			
			// @Override
			// protected void addInlineMenuToDto(AccountContentDto dto) {
			// addRowMenuToTable(dto);
			// }
		};
		try {
			
			provider.setQuery(createQuery(model));
			provider.setEmptyListOnNullQuery(true);
		} catch (SchemaException e) {
			// TODO
		}
		
		Collection<SelectorOptions<GetOperationOptions>> opts =
                SelectorOptions.createCollection(ShadowType.F_ASSOCIATION, GetOperationOptions.createRetrieve(RetrieveOption.EXCLUDE));
		if (searchTypeModel.getObject()){
			opts.add(new SelectorOptions<GetOperationOptions>(GetOperationOptions.createNoFetch()));
		}
		
		provider.setOptions(opts);

		List<IColumn> columns = initColumns();
		final BoxedTablePanel table = new BoxedTablePanel(ID_TABLE, provider, columns,
				UserProfileStorage.TableId.PAGE_RESOURCE_ACCOUNTS_PANEL, 10); // parentPage.getItemsPerPage(UserProfileStorage.TableId.PAGE_RESOURCE_ACCOUNTS_PANEL)
		table.setOutputMarkupId(true);
		mainForm.add(table);

		// Form objectclassForm = new Form(ID_OBJECTCLASS_FORM);
		// add(objectclassForm);
		// RefinedObjectTypeChoicePanel objectclassInput = new
		// RefinedObjectTypeChoicePanel(ID_OBJECTCLASS_INPUT, objectClassModel,
		// model);
		// objectclassInput.getBaseFormComponent().add(new
		// AjaxFormComponentUpdatingBehavior("change") {
		// @Override
		// protected void onUpdate(AjaxRequestTarget target) {
		// target.add(table);
		// }
		// });
		// objectclassForm.add(objectclassInput);

		initDialog();
	}

	private StringResourceModel createStringResource(String key) {
		return parentPage.createStringResource(key);
	}

	private void accountDetailsPerformed(AjaxRequestTarget target, String accountName, String accountOid) {
		if (StringUtils.isEmpty(accountOid)) {
			error(parentPage.getString("pageContentAccounts.message.cantShowAccountDetails", accountName,
					accountOid));
			target.add(parentPage.getFeedbackPanel());
			return;
		}

		PageParameters parameters = new PageParameters();
		parameters.add(OnePageParameterEncoder.PARAMETER, accountOid);
		setResponsePage(PageAccount.class, parameters);
	}

	private List<IColumn> initColumns() {
		List<IColumn> columns = new ArrayList<IColumn>();

		IColumn column = new CheckBoxColumn(new Model<String>(), AccountContentDto.F_SELECTED);
		columns.add(column);

		column = new LinkColumn<AccountContentDto>(createStringResource("pageContentAccounts.name"),
				AccountContentDto.F_ACCOUNT_NAME) {

			@Override
			public void onClick(AjaxRequestTarget target, IModel<AccountContentDto> rowModel) {
				AccountContentDto dto = rowModel.getObject();
				accountDetailsPerformed(target, dto.getAccountName(), dto.getAccountOid());
			}
		};
		columns.add(column);

		column = new AbstractColumn<AccountContentDto, String>(
				createStringResource("pageContentAccounts.identifiers")) {

			@Override
			public void populateItem(Item<ICellPopulator<AccountContentDto>> cellItem, String componentId,
					IModel<AccountContentDto> rowModel) {

				AccountContentDto dto = rowModel.getObject();
				List values = new ArrayList();
				for (ResourceAttribute<?> attr : dto.getIdentifiers()) {
					values.add(attr.getElementName().getLocalPart() + ": " + attr.getRealValue());
				}
				cellItem.add(new Label(componentId, new Model<>(StringUtils.join(values, ", "))));
			}
		};
		columns.add(column);

//		column = new PropertyColumn(createStringResource("pageContentAccounts.kind"),
//				AccountContentDto.F_KIND);
//		columns.add(column);

		column = new PropertyColumn(createStringResource("pageContentAccounts.intent"),
				AccountContentDto.F_INTENT);
		columns.add(column);

//		column = new PropertyColumn(createStringResource("pageContentAccounts.objectClass"),
//				AccountContentDto.F_OBJECT_CLASS);
//		columns.add(column);

		column = new EnumPropertyColumn(createStringResource("pageContentAccounts.situation"),
				AccountContentDto.F_SITUATION) {

			@Override
			protected String translate(Enum en) {
				return parentPage.createStringResource(en).getString();
			}
		};
		columns.add(column);

		column = new LinkColumn<AccountContentDto>(createStringResource("pageContentAccounts.owner")) {

			@Override
			protected IModel<String> createLinkModel(final IModel<AccountContentDto> rowModel) {
				return new AbstractReadOnlyModel<String>() {

					@Override
					public String getObject() {
						AccountContentDto dto = rowModel.getObject();
						if (StringUtils.isNotBlank(dto.getOwnerName())) {
							return dto.getOwnerName();
						}

						return dto.getOwnerOid();
					}
				};
			}

			@Override
			public void onClick(AjaxRequestTarget target, IModel<AccountContentDto> rowModel) {
				AccountContentDto dto = rowModel.getObject();

				ownerDetailsPerformed(target, dto.getOwnerName(), dto.getOwnerOid());
			}
		};
		columns.add(column);

		// column = new InlineMenuHeaderColumn(createHeaderMenuItems());
		// columns.add(column);

		return columns;
	}

	// private List<InlineMenuItem> createHeaderMenuItems() {
	// List<InlineMenuItem> items = new ArrayList<InlineMenuItem>();
	//
	// items.add(new
	// InlineMenuItem(createStringResource("pageContentAccounts.menu.enableAccount"),
	// true,
	// new HeaderMenuAction(this){
	//
	// @Override
	// public void onSubmit(AjaxRequestTarget target, Form<?> form){
	// updateAccountStatusPerformed(target, null, true);
	// }
	// }));
	//
	// items.add(new
	// InlineMenuItem(createStringResource("pageContentAccounts.menu.disableAccount"),
	// true,
	// new HeaderMenuAction(this){
	//
	// @Override
	// public void onSubmit(AjaxRequestTarget target, Form<?> form){
	// updateAccountStatusPerformed(target, null, false);
	// }
	// }));
	//
	// items.add(new
	// InlineMenuItem(createStringResource("pageContentAccounts.menu.deleteAccount"),
	// true,
	// new HeaderMenuAction(this){
	//
	// @Override
	// public void onSubmit(AjaxRequestTarget target, Form<?> form){
	// deleteAccountPerformed(target, null);
	// }
	// }));
	//
	// items.add(new InlineMenuItem());
	//
	// items.add(new
	// InlineMenuItem(createStringResource("pageContentAccounts.menu.importAccount"),
	// true,
	// new HeaderMenuAction(this) {
	//
	// @Override
	// public void onSubmit(AjaxRequestTarget target, Form<?> form) {
	// importAccount(target, null);
	// }
	// }));
	//
	// items.add(new InlineMenuItem());
	//
	// items.add(new
	// InlineMenuItem(createStringResource("pageContentAccounts.menu.removeOwner"),
	// true,
	// new HeaderMenuAction(this) {
	//
	// @Override
	// public void onSubmit(AjaxRequestTarget target, Form<?> form) {
	// removeOwnerPerformed(target, null);
	// }
	// }));
	//
	// return items;
	// }
	//

	private AccountContentDto createAccountContentDto(ShadowType shadow, OperationResult result) {

		AccountContentDto dto = new AccountContentDto();
		dto.setAccountName(WebComponentUtil.getName(shadow));
		dto.setAccountOid(shadow.getOid());
		// ShadowType shadow = object.asObjectable();

		Collection<ResourceAttribute<?>> identifiers = ShadowUtil.getIdentifiers(shadow);
		if (identifiers != null) {
			List<ResourceAttribute<?>> idList = new ArrayList<ResourceAttribute<?>>();
			idList.addAll(identifiers);
			dto.setIdentifiers(idList);
		}

		try {
			PrismObject<? extends FocusType> owner = loadOwner(dto.getAccountOid(), result);
			if (owner != null) {
				dto.setOwnerName(WebComponentUtil.getName(owner));
				dto.setOwnerOid(owner.getOid());
			}
		} catch (AuthorizationException e) {
			// owner was found, but the current user is not authorized to read
			// it
			result.muteLastSubresultError();
			dto.setOwnerName("(unauthorized)");
		} catch (SecurityViolationException | SchemaException | ConfigurationException e) {
			// TODO Auto-generated catch block
			LOGGER.error("Cannot get owner for shadow: " + e.getMessage());
			warn("Could not get owner for shadow: " + e.getMessage());
		}

		dto.setSituation(WebComponentUtil.getValue(shadow.asPrismContainer(),
				ShadowType.F_SYNCHRONIZATION_SITUATION, SynchronizationSituationType.class));

		dto.setKind(shadow.getKind());
		dto.setIntent(shadow.getIntent());
		dto.setObjectClass(shadow.getObjectClass().getLocalPart());

		// addInlineMenuToDto(dto);

		return dto;
	}

	private PrismObject<? extends FocusType> loadOwner(String accountOid, OperationResult result)
			throws SecurityViolationException, SchemaException, ConfigurationException {

		Task task = parentPage.createSimpleTask(OPERATION_LOAD_OWNER);
		try {
			return parentPage.getModelService().searchShadowOwner(accountOid, null, task, result);
		} catch (ObjectNotFoundException ex) {
			// owner was not found, it's possible and it's ok on unlinked
			// accounts
		}
		return null;
	}

	private void initDialog() {
		UserBrowserDialog<UserType> dialog = new UserBrowserDialog<UserType>(MODAL_ID_OWNER_CHANGE,
				UserType.class) {

			@Override
			public void userDetailsPerformed(AjaxRequestTarget target, UserType user) {
				super.userDetailsPerformed(target, user);

				// ownerChangePerformed(target, user);
				// target.add(getTable());
			}
		};
		add(dialog);

		add(new ConfirmationDialog(MODAL_ID_CONFIRM_DELETE,
				parentPage.createStringResource("pageContentAccounts.dialog.title.confirmDelete"),
				createDeleteConfirmString()) {

			@Override
			public void yesPerformed(AjaxRequestTarget target) {
				close(target);
				// deleteConfirmedPerformed(target);
			}
		});
	}

	private IModel<String> createDeleteConfirmString() {
		return new AbstractReadOnlyModel<String>() {
			@Override
			public String getObject() {
				return "asdasd";
				// if(singleDelete == null){
				// return
				// createStringResource("pageContentAccounts.message.deleteConfirmation",
				// getSelectedAccounts(null).size()).getString();
				// } else{
				// return
				// createStringResource("pageContentAccounts.message.deleteConfirmationSingle",
				// singleDelete.getAccountName()).getString();
				// }
			}
		};
	}

	private void ownerDetailsPerformed(AjaxRequestTarget target, String ownerName, String ownerOid) {
		if (StringUtils.isEmpty(ownerOid)) {
			// error(getString("pageContentAccounts.message.cantShowUserDetails",
			// ownerName, ownerOid));
			// target.add(getFeedbackPanel());
			return;
		}

		PageParameters parameters = new PageParameters();
		parameters.add(OnePageParameterEncoder.PARAMETER, ownerOid);
		setResponsePage(PageUser.class, parameters);
	}

	private void changeOwnerPerformed(AjaxRequestTarget target, AccountContentDto dto) {
		reloadOwnerChangeModel(dto.getAccountOid(), dto.getOwnerOid());

		// showModalWindow(MODAL_ID_OWNER_CHANGE, target);
	}

	private void reloadOwnerChangeModel(String accountOid, String ownerOid) {
		ownerChangeModel.reset();

		AccountOwnerChangeDto changeDto = ownerChangeModel.getObject();

		changeDto.setAccountOid(accountOid);
		changeDto.setAccountType(ShadowType.COMPLEX_TYPE);

		changeDto.setOldOwnerOid(ownerOid);
	}

	private ObjectQuery createQuery(IModel<PrismObject<ResourceType>> resourceModel) throws SchemaException {

		ObjectQuery baseQuery = null;

		if (!searchTypeModel.getObject()) {
			IModel<RefinedObjectClassDefinition> objectClassModel = createObjectClassModel(resourceModel);
			RefinedObjectClassDefinition rOcDef = objectClassModel.getObject();
			if (rOcDef != null) {
				if (rOcDef.getKind() != null) {
					baseQuery = ObjectQueryUtil.createResourceAndKindIntent(
							resourceModel.getObject().getOid(), rOcDef.getKind(), rOcDef.getIntent(),
							parentPage.getPrismContext());
				} else {
					baseQuery = ObjectQueryUtil.createResourceAndObjectClassQuery(
							resourceModel.getObject().getOid(), rOcDef.getTypeName(),
							parentPage.getPrismContext());
				}
			}
		} else {
			if (intentModel.getObject() != null) {
				baseQuery = ObjectQueryUtil.createResourceAndKindIntent(resourceModel.getObject().getOid(),
						kind, intentModel.getObject(), parentPage.getPrismContext());
				;
			} else {
				baseQuery = ObjectQueryUtil.createResourceAndKind(resourceModel.getObject().getOid(), kind,
						parentPage.getPrismContext());
			}
		}

	
	// if (rOcDef.getKind() != null) {
	
	// } else {
	// baseQuery =
	// ObjectQueryUtil.createResourceAndObjectClassQuery(resourceModel.getObject().getOid(),
	// rOcDef.getTypeName(), parentPage.getPrismContext());
	// }
	return baseQuery;

	}

	private IModel<Boolean> createUseObjectCountingModel(
			final IModel<PrismObject<ResourceType>> resourceModel) {
		return new LoadableModel<Boolean>(false) {

			@Override
			protected Boolean load() {
				try {
					return isUseObjectCounting(resourceModel);
				} catch (Exception ex) {
					throw new SystemException(ex.getMessage(), ex);
				}
			}
		};
	}

	private boolean isUseObjectCounting(IModel<PrismObject<ResourceType>> resourceModel)
			throws SchemaException {
		MidPointApplication application = (MidPointApplication) getApplication();
		PrismObject<ResourceType> resource = resourceModel.getObject();
		RefinedResourceSchema resourceSchema = RefinedResourceSchema.getRefinedSchema(resource,
				application.getPrismContext());

		// hacking this for now ... in future, we get the type definition (and
		// maybe kind+intent) directly from GUI model
		// TODO here we should deal with the situation that one object class is
		// mentioned in different
		// kind/intent sections -- we would want to avoid mentioning paged
		// search information in all
		// these sections
		ObjectClassComplexTypeDefinition typeDefinition = getDefinitionByKind(resourceModel);
		if (typeDefinition == null) {
			// should not occur
			LOGGER.warn("ObjectClass definition couldn't be found");
			return false;
		}

		RefinedObjectClassDefinition refinedObjectClassDefinition = resourceSchema
				.getRefinedDefinition(typeDefinition.getTypeName());
		if (refinedObjectClassDefinition == null) {
			return false;
		}
		return refinedObjectClassDefinition.isObjectCountingEnabled();
	}

}

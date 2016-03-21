package com.evolveum.midpoint.web.page.admin.resources;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
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
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RetrieveOption;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.ObjectDataProvider2;
import com.evolveum.midpoint.web.component.data.column.CheckBoxColumn;
import com.evolveum.midpoint.web.component.data.column.ColumnTypeDto;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.data.column.InlineMenuHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.LinkColumn;
import com.evolveum.midpoint.web.component.dialog.ConfirmationDialog;
import com.evolveum.midpoint.web.component.dialog.UserBrowserDialog;
import com.evolveum.midpoint.web.component.input.AutoCompleteTextPanel;
import com.evolveum.midpoint.web.component.input.TwoStateBooleanPanel;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.configuration.component.HeaderMenuAction;
import com.evolveum.midpoint.web.page.admin.resources.content.PageAccount;
import com.evolveum.midpoint.web.page.admin.users.PageUser;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public class ResourceContentTabPanel extends Panel {

	private static final Trace LOGGER = TraceManager.getTrace(ResourceContentTabPanel.class);

	enum Operation {
		REMOVE, MODIFY;
	}

	private static final String DOT_CLASS = ResourceContentTabPanel.class.getName() + ".";
	private static final String OPERATION_CHANGE_OWNER = DOT_CLASS + "changeOwner";
	

	private static final String OPERATION_LOAD_SHADOW_OWNER = DOT_CLASS + "loadOwner";
	private static final String ID_MAIN_FORM = "mainForm";
	private static final String ID_INTENT = "intent";

	private static final String ID_SEARCH_TYPE = "searchType";

	private static final String ID_TABLE = "table";


	private PageBase parentPage;
	private ShadowKindType kind;

	private Model<Boolean> searchTypeModel = new Model<Boolean>(false);

	private IModel<String> intentModel;


	public ResourceContentTabPanel(String id, ShadowKindType kind,
			final IModel<PrismObject<ResourceType>> model, PageBase parentPage) {
		super(id, model);
		this.parentPage = parentPage;
		this.kind = kind;

		intentModel = new Model();
	
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
			
		setOutputMarkupId(true);

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
				updateTable(target, model);

			}
		});
		intent.setOutputMarkupId(true);
		add(intent);
		final Form mainForm = new Form(ID_MAIN_FORM);
		mainForm.setOutputMarkupId(true);

		TwoStateBooleanPanel searchType = new TwoStateBooleanPanel(ID_SEARCH_TYPE, searchTypeModel,
				"ResourceContentTabPanel.search.repo", "ResourceContentTabPanel.search.resource", null) {

			@Override
			protected void onStateChanged(AjaxRequestTarget target, Boolean newValue) {
				updateTable(target, model);
			}
		};
		searchType.setOutputMarkupId(true);
		add(searchType);
	

		add(mainForm);

		ObjectDataProvider2<SelectableBean<ShadowType>, ShadowType> provider = new ObjectDataProvider2<SelectableBean<ShadowType>, ShadowType>(
				this, ShadowType.class);
		try {

			provider.setQuery(createQuery(model));
			provider.setEmptyListOnNullQuery(true);
			createSearchOptions(provider, model);
		} catch (SchemaException e) {
			// TODO
		}

		List<IColumn> columns = initColumns();
		final BoxedTablePanel table = new BoxedTablePanel(ID_TABLE, provider, columns,
				UserProfileStorage.TableId.PAGE_RESOURCE_ACCOUNTS_PANEL, 10); // parentPage.getItemsPerPage(UserProfileStorage.TableId.PAGE_RESOURCE_ACCOUNTS_PANEL)
		table.setOutputMarkupId(true);
		mainForm.add(table);

	}

	private void createSearchOptions(ObjectDataProvider2 provider,
			IModel<PrismObject<ResourceType>> resourceModel) throws SchemaException {
		Collection<SelectorOptions<GetOperationOptions>> opts = SelectorOptions.createCollection(
				ShadowType.F_ASSOCIATION, GetOperationOptions.createRetrieve(RetrieveOption.EXCLUDE));
		if (!searchTypeModel.getObject()) {
			opts.add(new SelectorOptions<GetOperationOptions>(GetOperationOptions.createNoFetch()));
			provider.setUseObjectCounting(true);
		} else {

			provider.setUseObjectCounting(isUseObjectCounting(resourceModel));
		}
		provider.setOptions(opts);
	}

	private BoxedTablePanel getTable() {
		return (BoxedTablePanel) get(parentPage.createComponentPath(ID_MAIN_FORM, ID_TABLE));
	}

	private void updateTable(AjaxRequestTarget target, IModel<PrismObject<ResourceType>> model) {
		BoxedTablePanel table = getTable();
		ObjectDataProvider2 provider = (ObjectDataProvider2) table.getDataTable().getDataProvider();
		try {
			provider.setQuery(createQuery(
					(IModel<PrismObject<ResourceType>>) ResourceContentTabPanel.this.getDefaultModel()));
			createSearchOptions(provider, model);
		} catch (SchemaException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		target.add(table);

		Form main = (Form) get(ID_MAIN_FORM);
		main.addOrReplace(table);
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

		List<ColumnTypeDto> columnDefs = Arrays.asList(
				new ColumnTypeDto("ShadowType.synchronizationSituation",
						SelectableBean.F_VALUE + ".synchronizationSituation",
						ShadowType.F_SYNCHRONIZATION_SITUATION.getLocalPart()),
				new ColumnTypeDto<String>("ShadowType.intent", SelectableBean.F_VALUE + ".intent",
						ShadowType.F_INTENT.getLocalPart()));

		List<IColumn> columns = new ArrayList<>();
		IColumn column = new CheckBoxColumn(new Model<String>(), SelectableBean.F_SELECTED);
		columns.add(column);

		column = new LinkColumn<SelectableBean<ShadowType>>(createStringResource("pageContentAccounts.name"),
				SelectableBean.F_VALUE + ".name") {

			@Override
			public void onClick(AjaxRequestTarget target, IModel<SelectableBean<ShadowType>> rowModel) {
				SelectableBean<ShadowType> shadow = rowModel.getObject();
				ShadowType shadowType = shadow.getValue();
				accountDetailsPerformed(target, WebComponentUtil.getName(shadowType), shadowType.getOid());
			}
		};
		columns.add(column);

		column = new AbstractColumn<SelectableBean<ShadowType>, String>(
				createStringResource("pageContentAccounts.identifiers")) {

			@Override
			public void populateItem(Item<ICellPopulator<SelectableBean<ShadowType>>> cellItem,
					String componentId, IModel<SelectableBean<ShadowType>> rowModel) {

				SelectableBean<ShadowType> dto = rowModel.getObject();
				List values = new ArrayList();
				for (ResourceAttribute<?> attr : ShadowUtil.getAllIdentifiers(dto.getValue())) {
					values.add(attr.getElementName().getLocalPart() + ": " + attr.getRealValue());
				}
				cellItem.add(new Label(componentId, new Model<>(StringUtils.join(values, ", "))));
			}
		};
		columns.add(column);

		columns.addAll(ColumnUtils.createColumns(columnDefs));
		column = new LinkColumn<SelectableBean<ShadowType>>(createStringResource("pageContentAccounts.owner"),
				true) {

			@Override
			protected IModel createLinkModel(final IModel<SelectableBean<ShadowType>> rowModel) {

				return new AbstractReadOnlyModel<FocusType>() {

					@Override
					public FocusType getObject() {
						FocusType owner = loadShadowOwner(rowModel);
						if (owner == null) {
							return null;
						}
						return owner;
					
					}

				};
			}

			@Override
			public void onClick(AjaxRequestTarget target, IModel<SelectableBean<ShadowType>> rowModel) {
				SelectableBean<ShadowType> shadow = rowModel.getObject();
				ShadowType shadowType = shadow.getValue();
				ownerDetailsPerformed(target, this.getModelObjectIdentifier());
			}
		};
		columns.add(column);

		column = new InlineMenuHeaderColumn(createHeaderMenuItems());
		columns.add(column);

		return columns;
	}

	private List<InlineMenuItem> createHeaderMenuItems() {
		List<InlineMenuItem> items = new ArrayList<InlineMenuItem>();

		items.add(new InlineMenuItem(createStringResource("pageContentAccounts.menu.enableAccount"), true,
				new HeaderMenuAction(this) {

					@Override
					public void onSubmit(AjaxRequestTarget target, Form<?> form) {
						// updateAccountStatusPerformed(target, null, true);
					}
				}));

		items.add(new InlineMenuItem(createStringResource("pageContentAccounts.menu.disableAccount"), true,
				new HeaderMenuAction(this) {

					@Override
					public void onSubmit(AjaxRequestTarget target, Form<?> form) {
						// updateAccountStatusPerformed(target, null, false);
					}
				}));

		items.add(new InlineMenuItem(createStringResource("pageContentAccounts.menu.deleteAccount"), true,
				new HeaderMenuAction(this) {

					@Override
					public void onSubmit(AjaxRequestTarget target, Form<?> form) {
						// deleteAccountPerformed(target, null);
					}
				}));

		items.add(new InlineMenuItem());

		items.add(new InlineMenuItem(createStringResource("pageContentAccounts.menu.importAccount"), true,
				new HeaderMenuAction(this) {

					@Override
					public void onSubmit(AjaxRequestTarget target, Form<?> form) {
						// importAccount(target, null);
					}
				}));

		items.add(new InlineMenuItem());

		items.add(new InlineMenuItem(createStringResource("pageContentAccounts.menu.removeOwner"), true,
				new HeaderMenuAction(this) {

					@Override
					public void onSubmit(AjaxRequestTarget target, Form<?> form) {
						changeOwner(target, null, Operation.REMOVE);
						// removeOwnerPerformed(target, null);
					}
				}));

		
		items.add(new InlineMenuItem(createStringResource("pageContentAccounts.menu.changeOwner"), true,
				new HeaderMenuAction(this) {

					@Override
					public void onSubmit(AjaxRequestTarget target, Form<?> form) {

						FocusBrowserDialogPanel browser = new FocusBrowserDialogPanel(
								parentPage.getMainPopupBodyId(), UserType.class, parentPage) {

							@Override
							public void userDetailsPerformed(AjaxRequestTarget target, FocusType user) {
								super.userDetailsPerformed(target, user);
								changeOwner(target, user, Operation.MODIFY);
							}

						};
					
						parentPage.getMainPopup().setInitialHeight(400);
						parentPage.getMainPopup().setInitialWidth(600);
						parentPage.showMainPopup(browser, new Model<String>("ChangeOwner"), target);

					}
				}));

		return items;
	}

	private void changeOwner(AjaxRequestTarget target, FocusType ownerToChange, Operation operation) {
		BoxedTablePanel table = getTable();
		List<SelectableBean<ShadowType>> selectedShadow = WebComponentUtil.getSelectedData(table);

		Collection<? extends ItemDelta> modifications = new ArrayList<>();

		ReferenceDelta delta = null;
		switch (operation) {
			
			case REMOVE:
				for (SelectableBean<ShadowType> selected : selectedShadow) {
					modifications = new ArrayList<>();
					FocusType owner = loadShadowOwner(selected.getValue().getOid());
					if (owner != null) {
						delta = ReferenceDelta.createModificationDelete(FocusType.F_LINK_REF,
								getFocusDefinition(),
								ObjectTypeUtil.createObjectRef(selected.getValue()).asReferenceValue());

						((Collection) modifications).add(delta);
						changeOwnerInternal(owner.getOid(), modifications, target);
					}
				}
				break;
			case MODIFY:
				if (!isSatisfyConstraints(selectedShadow)) {
					break;
				}

				ShadowType shadow = selectedShadow.iterator().next().getValue();
				FocusType owner = loadShadowOwner(shadow.getOid());
				if (owner != null) {
					delta = ReferenceDelta.createModificationDelete(FocusType.F_LINK_REF,
							getFocusDefinition(), ObjectTypeUtil.createObjectRef(shadow).asReferenceValue());

					((Collection) modifications).add(delta);
					changeOwnerInternal(owner.getOid(), modifications, target);
				}
				modifications = new ArrayList<>();
				
		delta = ReferenceDelta.createModificationAdd(FocusType.F_LINK_REF, getFocusDefinition(),
						ObjectTypeUtil.createObjectRef(shadow).asReferenceValue());
				((Collection) modifications).add(delta);
				changeOwnerInternal(ownerToChange.getOid(), modifications, target);

				break;
		}

	}

	private boolean isSatisfyConstraints(List selected) {
		if (selected.size() > 1) {
			error("Could not link to more than one owner");
			return false;
		}

		if (selected.isEmpty()) {
			warn("Could not link to more than one owner");
			return false;
		}

		return true;
	}

	private void changeOwnerInternal(String ownerOid, Collection<? extends ItemDelta> modifications, AjaxRequestTarget target) {
		OperationResult result = new OperationResult(OPERATION_CHANGE_OWNER);
		Task task = parentPage.createSimpleTask(OPERATION_CHANGE_OWNER);
		ObjectDelta objectDelta = ObjectDelta.createModifyDelta(ownerOid, modifications, FocusType.class,
				parentPage.getPrismContext());
		Collection deltas = new ArrayList<>();
		deltas.add(objectDelta);
		try {
			if (!deltas.isEmpty()) {
				parentPage.getModelService().executeChanges(deltas, null, task, result);

			}
		} catch (ObjectAlreadyExistsException | ObjectNotFoundException | SchemaException
				| ExpressionEvaluationException | CommunicationException | ConfigurationException
				| PolicyViolationException | SecurityViolationException e) {

		}

		result.computeStatusIfUnknown();

		parentPage.showResult(result);
		target.add(parentPage.getFeedbackPanel());
		target.add(ResourceContentTabPanel.this);
	}

	private PrismObjectDefinition getFocusDefinition() {
		return parentPage.getPrismContext().getSchemaRegistry()
				.findObjectDefinitionByCompileTimeClass(FocusType.class);
	}

	

	private <F extends FocusType> F loadShadowOwner(IModel<SelectableBean<ShadowType>> model) {
		F owner = null;

		ShadowType shadow = model.getObject().getValue();
		String shadowOid;
		if (shadow != null) {
			shadowOid = shadow.getOid();
		} else {
			return null;
		}

		Task task = parentPage.createSimpleTask(OPERATION_LOAD_SHADOW_OWNER);
		OperationResult result = new OperationResult(OPERATION_LOAD_SHADOW_OWNER);

		try {
			PrismObject prismOwner = parentPage.getModelService().searchShadowOwner(shadowOid, null, task,
					result);

			if (prismOwner != null) {
				owner = (F) prismOwner.asObjectable();
			}
		} catch (ObjectNotFoundException exception) {
			// owner was not found, it's possible and it's ok on unlinked
			// accounts
		} catch (Exception ex) {
			result.recordFatalError(parentPage.getString("PageAccounts.message.ownerNotFound", shadowOid),
					ex);
			LoggingUtils.logException(LOGGER, "Could not load owner of account with oid: " + shadowOid, ex);
		} finally {
			result.computeStatusIfUnknown();
		}

		if (WebComponentUtil.showResultInPage(result)) {
			parentPage.showResult(result, false);
		}

		return owner;
	}

	private <F extends FocusType> F loadShadowOwner(String shadowOid) {

		Task task = parentPage.createSimpleTask(OPERATION_LOAD_SHADOW_OWNER);
		OperationResult result = new OperationResult(OPERATION_LOAD_SHADOW_OWNER);

		try {
			PrismObject prismOwner = parentPage.getModelService().searchShadowOwner(shadowOid, null, task,
					result);

			if (prismOwner != null) {
				return (F) prismOwner.asObjectable();
			}
		} catch (ObjectNotFoundException exception) {
			// owner was not found, it's possible and it's ok on unlinked
			// accounts
		} catch (Exception ex) {
			result.recordFatalError(parentPage.getString("PageAccounts.message.ownerNotFound", shadowOid),
					ex);
			LoggingUtils.logException(LOGGER, "Could not load owner of account with oid: " + shadowOid, ex);
		} finally {
			result.computeStatusIfUnknown();
		}

		if (WebComponentUtil.showResultInPage(result)) {
			parentPage.showResult(result, false);
		}

		return null;
	}



	private IModel<String> createDeleteConfirmString() {
		return new AbstractReadOnlyModel<String>() {
			@Override
			public String getObject() {
				return "asdasd";
				
			}
		};
	}

	private void ownerDetailsPerformed(AjaxRequestTarget target, String ownerOid) {
		if (StringUtils.isEmpty(ownerOid)) {
			
			return;
		}

		PageParameters parameters = new PageParameters();
		parameters.add(OnePageParameterEncoder.PARAMETER, ownerOid);
		setResponsePage(PageUser.class, parameters);
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

	
		return baseQuery;

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

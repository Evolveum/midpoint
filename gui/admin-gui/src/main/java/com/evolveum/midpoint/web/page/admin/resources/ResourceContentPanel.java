package com.evolveum.midpoint.web.page.admin.resources;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.component.FocusBrowserPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RetrieveOption;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.result.OperationResult;
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
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.configuration.component.HeaderMenuAction;
import com.evolveum.midpoint.web.page.admin.resources.ResourceContentTabPanel.Operation;
import com.evolveum.midpoint.web.page.admin.resources.content.PageAccount;
import com.evolveum.midpoint.web.page.admin.users.PageUser;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

public abstract class ResourceContentPanel extends Panel {

	private static final Trace LOGGER = TraceManager.getTrace(ResourceContentPanel.class);

	private static final String DOT_CLASS = ResourceContentTabPanel.class.getName() + ".";
	private static final String OPERATION_CHANGE_OWNER = DOT_CLASS + "changeOwner";
	private static final String OPERATION_LOAD_SHADOW_OWNER = DOT_CLASS + "loadOwner";

	private static final String ID_TABLE = "table";

	private PageBase pageBase;
	private ShadowKindType kind;
	private String intent;
	
	IModel<PrismObject<ResourceType>> resourceModel;

	public PageBase getPageBase() {
		return pageBase;
	}

	public ShadowKindType getKind() {
		return kind;
	}

	public String getIntent() {
		return intent;
	}
	
	public IModel<PrismObject<ResourceType>> getResourceModel() {
		return resourceModel;
	}

	public ResourceContentPanel(String id, IModel<PrismObject<ResourceType>> resourceModel,
			ShadowKindType kind, String intent, PageBase pageBase) {
		super(id);
		this.pageBase = pageBase;
		this.kind = kind;
		this.resourceModel = resourceModel;
		this.intent = intent;
		initLayout(resourceModel);
	}

	private void initLayout(IModel<PrismObject<ResourceType>> resourceModel) {
		ObjectDataProvider2<SelectableBean<ShadowType>, ShadowType> provider = new ObjectDataProvider2<SelectableBean<ShadowType>, ShadowType>(
				this, ShadowType.class);

		try {
			provider.setQuery(createQuery(resourceModel));

		} catch (SchemaException e) {
			Label label = new Label(ID_TABLE, "Nothing to show. Select intent to search");
			add(label);
			return;
		}

		provider.setEmptyListOnNullQuery(true);
		createSearchOptions(provider, resourceModel);
		List<IColumn> columns = initColumns();
		final BoxedTablePanel table = new BoxedTablePanel(ID_TABLE, provider, columns,
				UserProfileStorage.TableId.PAGE_RESOURCE_ACCOUNTS_PANEL, 10); // parentPage.getItemsPerPage(UserProfileStorage.TableId.PAGE_RESOURCE_ACCOUNTS_PANEL)
		table.setOutputMarkupId(true);
		add(table);
		
		initCustomLayout(resourceModel);
	}

	protected abstract void initCustomLayout(IModel<PrismObject<ResourceType>> resource);
	
	protected abstract ObjectQuery createQuery(IModel<PrismObject<ResourceType>> resourceModel)
			throws SchemaException;

	private void createSearchOptions(ObjectDataProvider2 provider,
			IModel<PrismObject<ResourceType>> resourceModel) {
		Collection<SelectorOptions<GetOperationOptions>> opts = SelectorOptions.createCollection(
				ShadowType.F_ASSOCIATION, GetOperationOptions.createRetrieve(RetrieveOption.EXCLUDE));

		if (addAdditionalOptions() != null){
			opts.add(addAdditionalOptions()); // new SelectorOptions<GetOperationOptions>(GetOperationOptions.createNoFetch()));
		}
			provider.setUseObjectCounting(isUseObjectCounting(resourceModel));
			provider.setOptions(opts);
	
	}

	private StringResourceModel createStringResource(String key) {
		return pageBase.createStringResource(key);
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
				shadowDetailsPerformed(target, WebComponentUtil.getName(shadowType), shadowType.getOid());
			}
		};
		columns.add(column);

		column = new AbstractColumn<SelectableBean<ShadowType>, String>(
				createStringResource("pageContentAccounts.identifiers")) {

			@Override
			public void populateItem(Item<ICellPopulator<SelectableBean<ShadowType>>> cellItem,
					String componentId, IModel<SelectableBean<ShadowType>> rowModel) {

				SelectableBean<ShadowType> dto = rowModel.getObject();
				RepeatingView repeater = new RepeatingView(componentId);

				for (ResourceAttribute<?> attr : ShadowUtil.getAllIdentifiers(dto.getValue())) {
					repeater.add(new Label(repeater.newChildId(),
							attr.getElementName().getLocalPart() + ": " + attr.getRealValue()));

				}
				cellItem.add(repeater);

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
	
	

	private void ownerDetailsPerformed(AjaxRequestTarget target, String ownerOid) {
		if (StringUtils.isEmpty(ownerOid)) {

			return;
		}

		PageParameters parameters = new PageParameters();
		parameters.add(OnePageParameterEncoder.PARAMETER, ownerOid);
		setResponsePage(PageUser.class, parameters);
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

		Task task = pageBase.createSimpleTask(OPERATION_LOAD_SHADOW_OWNER);
		OperationResult result = new OperationResult(OPERATION_LOAD_SHADOW_OWNER);

		try {
			PrismObject prismOwner = pageBase.getModelService().searchShadowOwner(shadowOid, null, task,
					result);

			if (prismOwner != null) {
				owner = (F) prismOwner.asObjectable();
			}
		} catch (ObjectNotFoundException exception) {
			// owner was not found, it's possible and it's ok on unlinked
			// accounts
		} catch (Exception ex) {
			result.recordFatalError(pageBase.getString("PageAccounts.message.ownerNotFound", shadowOid), ex);
			LoggingUtils.logException(LOGGER, "Could not load owner of account with oid: " + shadowOid, ex);
		} finally {
			result.computeStatusIfUnknown();
		}

		if (WebComponentUtil.showResultInPage(result)) {
			pageBase.showResult(result, false);
		}

		return owner;
	}

	private void shadowDetailsPerformed(AjaxRequestTarget target, String accountName, String accountOid) {
		if (StringUtils.isEmpty(accountOid)) {
			error(pageBase.getString("pageContentAccounts.message.cantShowAccountDetails", accountName,
					accountOid));
			target.add(pageBase.getFeedbackPanel());
			return;
		}

		PageParameters parameters = new PageParameters();
		parameters.add(OnePageParameterEncoder.PARAMETER, accountOid);
		setResponsePage(PageAccount.class, parameters);
	}

	private <F extends FocusType> F loadShadowOwner(String shadowOid) {

		Task task = pageBase.createSimpleTask(OPERATION_LOAD_SHADOW_OWNER);
		OperationResult result = new OperationResult(OPERATION_LOAD_SHADOW_OWNER);

		try {
			PrismObject prismOwner = pageBase.getModelService().searchShadowOwner(shadowOid, null, task,
					result);

			if (prismOwner != null) {
				return (F) prismOwner.asObjectable();
			}
		} catch (ObjectNotFoundException exception) {
			// owner was not found, it's possible and it's ok on unlinked
			// accounts
		} catch (Exception ex) {
			result.recordFatalError(pageBase.getString("PageAccounts.message.ownerNotFound", shadowOid), ex);
			LoggingUtils.logException(LOGGER, "Could not load owner of account with oid: " + shadowOid, ex);
		} finally {
			result.computeStatusIfUnknown();
		}

		if (WebComponentUtil.showResultInPage(result)) {
			pageBase.showResult(result, false);
		}

		return null;
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

						FocusBrowserPanel<UserType> browser = new FocusBrowserPanel<UserType>(
								pageBase.getMainPopupBodyId(), UserType.class, false, pageBase) {
							protected void onClick(AjaxRequestTarget target, UserType focus) {
								changeOwner(target, focus, Operation.MODIFY);
							}
						};

						pageBase.showMainPopup(browser, new Model<String>("ChangeOwner"), target, 900, 500);

					}
				}));

		return items;
	}

	private PrismObjectDefinition getFocusDefinition() {
		return pageBase.getPrismContext().getSchemaRegistry()
				.findObjectDefinitionByCompileTimeClass(FocusType.class);
	}

	private BoxedTablePanel getTable() {
		return (BoxedTablePanel) get(pageBase.createComponentPath(ID_TABLE));
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

	private void changeOwnerInternal(String ownerOid, Collection<? extends ItemDelta> modifications,
			AjaxRequestTarget target) {
		OperationResult result = new OperationResult(OPERATION_CHANGE_OWNER);
		Task task = pageBase.createSimpleTask(OPERATION_CHANGE_OWNER);
		ObjectDelta objectDelta = ObjectDelta.createModifyDelta(ownerOid, modifications, FocusType.class,
				pageBase.getPrismContext());
		Collection deltas = new ArrayList<>();
		deltas.add(objectDelta);
		try {
			if (!deltas.isEmpty()) {
				pageBase.getModelService().executeChanges(deltas, null, task, result);

			}
		} catch (ObjectAlreadyExistsException | ObjectNotFoundException | SchemaException
				| ExpressionEvaluationException | CommunicationException | ConfigurationException
				| PolicyViolationException | SecurityViolationException e) {

		}

		result.computeStatusIfUnknown();

		pageBase.showResult(result);
		target.add(pageBase.getFeedbackPanel());
		target.add(ResourceContentPanel.this);
	}

	protected abstract SelectorOptions<GetOperationOptions> addAdditionalOptions();

	protected abstract boolean isUseObjectCounting(IModel<PrismObject<ResourceType>> resourceModel);

}

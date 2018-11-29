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
package com.evolveum.midpoint.web.page.admin.roles;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.page.admin.configuration.component.HeaderMenuAction;
import com.evolveum.midpoint.web.page.admin.server.PageTasks;
import com.evolveum.midpoint.web.session.MemberPanelStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.NotFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.dialog.ChooseFocusTypeAndRelationDialogPanel;
import com.evolveum.midpoint.web.component.form.CheckFormGroup;
import com.evolveum.midpoint.web.component.form.DropDownFormGroup;
import com.evolveum.midpoint.web.component.input.QNameObjectTypeChoiceRenderer;
import com.evolveum.midpoint.web.component.input.RelationDropDownChoicePanel;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.component.search.SearchFactory;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.configuration.component.ChooseTypePanel;
import com.evolveum.midpoint.web.page.admin.dto.ObjectViewDto;
import com.evolveum.midpoint.web.security.GuiAuthorizationConstants;
import com.evolveum.midpoint.web.session.UserProfileStorage.TableId;
import com.evolveum.midpoint.web.util.StringResourceChoiceRenderer;

import static com.evolveum.midpoint.gui.api.util.WebComponentUtil.isAuthorized;

public abstract class AbstractRoleMemberPanel<R extends AbstractRoleType> extends BasePanel<R> {

	private static final long serialVersionUID = 1L;

	protected enum QueryScope {
		SELECTED, ALL, ALL_DIRECT
	}

	protected enum MemberOperation {
		ADD, REMOVE, RECOMPUTE
	}

	private static final Trace LOGGER = TraceManager.getTrace(AbstractRoleMemberPanel.class);
	private static final String DOT_CLASS = AbstractRoleMemberPanel.class.getName() + ".";
	protected static final String OPERATION_RELATION_DEFINITION_TYPE = DOT_CLASS + "loadRelationDefinitionTypes";

	protected static final String ID_FORM = "form";
	
	protected static final String ID_CONTAINER_MEMBER = "memberContainer";
	protected static final String ID_CHILD_TABLE = "childUnitTable";
	protected static final String ID_MEMBER_TABLE = "memberTable";
	
	private static final String ID_OBJECT_TYPE = "type";
	private static final String ID_TENANT = "tenant";
	private static final String ID_PROJECT = "project";
	private static final String ID_INDIRECT_MEMBERS = "indirectMembers";

	protected static final String ID_SEARCH_SCOPE = "searchScope";
	protected SearchBoxScopeType scopeDefaultValue = null;
	protected QName objectTypeDefaultValue = null;

	protected static final String ID_SEARCH_BY_RELATION = "searchByRelation";

	private static Map<QName, Map<String, String>> authorizations = new HashMap<>();
	private static Map<QName, TableId> tablesId = new HashMap<>();

	static {
		tablesId.put(RoleType.COMPLEX_TYPE, TableId.ROLE_MEMEBER_PANEL);
		tablesId.put(ServiceType.COMPLEX_TYPE, TableId.SERVICE_MEMEBER_PANEL);
		tablesId.put(OrgType.COMPLEX_TYPE, TableId.ORG_MEMEBER_PANEL);
	}
	
	static {
		authorizations.put(RoleType.COMPLEX_TYPE, GuiAuthorizationConstants.ROLE_MEMBERS_AUTHORIZATIONS);
		authorizations.put(ServiceType.COMPLEX_TYPE, GuiAuthorizationConstants.SERVICE_MEMBERS_AUTHORIZATIONS);
		authorizations.put(OrgType.COMPLEX_TYPE, GuiAuthorizationConstants.ORG_MEMBERS_AUTHORIZATIONS);
	}
	
	public AbstractRoleMemberPanel(String id, IModel<R> model) {
		super(id, model);
	}

	@Override
	protected void onInitialize(){
		super.onInitialize();
		initLayout();
	}

	protected void initLayout() {
		Form<?> form = new com.evolveum.midpoint.web.component.form.Form(ID_FORM);
		form.setOutputMarkupId(true);
		add(form);
		initDefaultSearchParameters();
		initSearch(form);
		initMemberTable(form);
		setOutputMarkupId(true);
		
	}

	private void initDefaultSearchParameters(){
		GuiObjectListPanelConfigurationType additionalPanel = getAdditionalPanelConfig();
		if (additionalPanel != null && additionalPanel.getSearchBoxConfiguration() != null) {
			scopeDefaultValue = additionalPanel.getSearchBoxConfiguration().getDefaultScope();
			objectTypeDefaultValue = additionalPanel.getSearchBoxConfiguration().getDefaultObjectType();
		}
		if (scopeDefaultValue == null){
			scopeDefaultValue = SearchBoxScopeType.ONE_LEVEL;
		}
		if (objectTypeDefaultValue == null){
			objectTypeDefaultValue = WebComponentUtil.classToQName(getPrismContext(), getDefaultObjectType());
		}
		if (getMemberPanelStorage() != null){
			if (getMemberPanelStorage().getOrgSearchScope() == null){
				getMemberPanelStorage().setOrgSearchScope(scopeDefaultValue);
			}
			if (getMemberPanelStorage().getType() == null){
				getMemberPanelStorage().setType(ObjectTypes.getObjectType(objectTypeDefaultValue.getLocalPart()));
			}
		}
	}

	protected Form<?> getForm() {
		return (Form) get(ID_FORM);
	}
	
	private void initMemberTable(Form<?> form) {
		WebMarkupContainer memberContainer = new WebMarkupContainer(ID_CONTAINER_MEMBER);
		memberContainer.setOutputMarkupId(true);
		memberContainer.setOutputMarkupPlaceholderTag(true);
		form.add(memberContainer);

		PageBase pageBase =  getPageBase();
		MainObjectListPanel<ObjectType> childrenListPanel = new MainObjectListPanel<ObjectType>(
				ID_MEMBER_TABLE, ObjectType.class, getTableId(getComplexTypeQName()), getSearchOptions(), pageBase) {

			private static final long serialVersionUID = 1L;

			@Override
            protected void objectDetailsPerformed(AjaxRequestTarget target, ObjectType object) {
                detailsPerformed(target, object);
            }

            @Override
            protected boolean isClickable(IModel<SelectableBean<ObjectType>> rowModel) {
                if (rowModel == null || rowModel.getObject() == null
                        || rowModel.getObject().getValue() == null) {
                    return false;
                }
                Class<?> objectClass = rowModel.getObject().getValue().getClass();
                return WebComponentUtil.hasDetailsPage(objectClass);
            }

            @Override
            protected void newObjectPerformed(AjaxRequestTarget target) {
                AbstractRoleMemberPanel.this.createFocusMemberPerformed(target);
            }

			@Override
			protected List<Component> createToolbarButtonsList(String buttonId){
				List<Component> buttonsList = super.createToolbarButtonsList(buttonId);
				AjaxIconButton assignButton = new AjaxIconButton(buttonId, new Model<>(GuiStyleConstants.CLASS_ASSIGN), 	//TODO change icon class
						createStringResource("TreeTablePanel.menu.addMembers")) {

					private static final long serialVersionUID = 1L;

					@Override
					public void onClick(AjaxRequestTarget target) {
						AbstractRoleMemberPanel.this.assignMembers(target, getSupportedRelations());
					}
				};
				assignButton.add(AttributeAppender.append("class", "btn btn-default btn-sm"));
				buttonsList.add(1, assignButton);
				return buttonsList;
			}

			@Override
            protected List<IColumn<SelectableBean<ObjectType>, String>> createColumns() {
                return createMembersColumns();
            }

            @Override
            protected List<InlineMenuItem> createInlineMenu() {
                return createRowActions();
            }

            @Override
            protected Search createSearch() {
                return SearchFactory.createSearch(getDefaultObjectType(), pageBase);
            }

            @Override
            protected ObjectQuery createContentQuery() {
                ObjectQuery q = super.createContentQuery();

                ObjectQuery members = AbstractRoleMemberPanel.this.createContentQuery();

                List<ObjectFilter> filters = new ArrayList<>();

                if (q != null && q.getFilter() != null) {
                    filters.add(q.getFilter());
                }

                if (members != null && members.getFilter() != null) {
                    filters.add(members.getFilter());
                }

                if (filters.size() == 1) {
                    return ObjectQuery.createObjectQuery(filters.iterator().next());
                }

                return ObjectQuery.createObjectQuery(AndFilter.createAnd(filters));
            }

            @Override
			protected GuiObjectListPanelConfigurationType getAdditionalPanelConfig(){
				return AbstractRoleMemberPanel.this.getAdditionalPanelConfig();
			}
        };
        childrenListPanel.setOutputMarkupId(true);
        memberContainer.add(childrenListPanel);
    }
	
	protected TableId getTableId(QName complextType) {
		return tablesId.get(complextType);
	}
	
	protected Map<String, String> getAuthorizations(QName complexType) {
		return authorizations.get(complexType);
	}
	
	protected QName getComplexTypeQName() {
		return getModelObject().asPrismObject().getComplexTypeDefinition().getTypeName();
	}

    private List<InlineMenuItem> createRowActions() {
        List<InlineMenuItem> menu = new ArrayList<>();
        if (isAuthorized(GuiAuthorizationConstants.MEMBER_OPERATION_ASSIGN)) {
            menu.add(new InlineMenuItem(createStringResource("abstractRoleMemberPanel.menu.assign")) {
                private static final long serialVersionUID = 1L;

                @Override
                public InlineMenuItemAction initAction() {
                    return new HeaderMenuAction(AbstractRoleMemberPanel.this) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            assignMembers(target, getSupportedRelations());
                        }
                    };
                }
			});
        }

        if (isAuthorized(GuiAuthorizationConstants.MEMBER_OPERATION_UNASSIGN)) {
            menu.add(new ButtonInlineMenuItem(createStringResource("abstractRoleMemberPanel.menu.unassign")) {
                private static final long serialVersionUID = 1L;

                @Override
                public InlineMenuItemAction initAction() {
                    return new HeaderMenuAction(AbstractRoleMemberPanel.this) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            unassignMembersPerformed(target);
                        }
                    };

                }

				@Override
                public String getButtonIconCssClass() {
                    return GuiStyleConstants.CLASS_UNASSIGN;
                }
            });
        }

        if (isAuthorized(GuiAuthorizationConstants.MEMBER_OPERATION_RECOMPUTE)) {
            menu.add(new ButtonInlineMenuItem(createStringResource("abstractRoleMemberPanel.menu.recompute")) {
                private static final long serialVersionUID = 1L;

                @Override
                public InlineMenuItemAction initAction() {
                    return new HeaderMenuAction(AbstractRoleMemberPanel.this) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            recomputeMembersPerformed(target);
                        }
                    };
                }

				@Override
				public IModel<String> getConfirmationMessageModel() {
					return getMemberTable().getSelectedObjectsCount() > 0 ?
							createStringResource("abstractRoleMemberPanel.recomputeSelectedMembersConfirmationLabel")
							: createStringResource("abstractRoleMemberPanel.recomputeAllMembersConfirmationLabel");
				}

				@Override
				public String getButtonIconCssClass() {
					return GuiStyleConstants.CLASS_RECONCILE_MENU_ITEM;
				}

			});
        }
        if (isAuthorized(GuiAuthorizationConstants.MEMBER_OPERATION_CREATE)) {
            menu.add(new InlineMenuItem(createStringResource("abstractRoleMemberPanel.menu.create")) {
                private static final long serialVersionUID = 1L;

                @Override
                public InlineMenuItemAction initAction() {
                    return new HeaderMenuAction(AbstractRoleMemberPanel.this) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            createFocusMemberPerformed(target);
                        }
                    };
                }
            });
        }
        if (isAuthorized(GuiAuthorizationConstants.MEMBER_OPERATION_DELETE)) {
            menu.add(new InlineMenuItem(createStringResource("abstractRoleMemberPanel.menu.delete")) {
                private static final long serialVersionUID = 1L;

                @Override
                public InlineMenuItemAction initAction() {
                    return new HeaderMenuAction(AbstractRoleMemberPanel.this) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            deleteMembersPerformed(target);
                        }
                    };
                }

            });
        }
        return menu;
	}
	
	protected abstract List<QName> getSupportedRelations();

	protected GuiObjectListPanelConfigurationType getAdditionalPanelConfig(){
		return null;
	}

	private boolean isAuthorized(String action) {
		Map<String, String> memeberAuthz = getAuthorizations(getComplexTypeQName());
		return WebComponentUtil.isAuthorized(memeberAuthz.get(action));
	}
	
	protected <O extends ObjectType> void assignMembers(AjaxRequestTarget target, List<QName> availableRelationList) {
		MemberOperationsHelper.assignMembers(getPageBase(), getModelObject(), target, availableRelationList);
	}

	private void unassignMembersPerformed(AjaxRequestTarget target) {
		QueryScope scope = getQueryScope(false);
		
		ChooseFocusTypeAndRelationDialogPanel chooseTypePopupContent = new ChooseFocusTypeAndRelationDialogPanel(getPageBase().getMainPopupBodyId(),
				createStringResource("abstractRoleMemberPanel.unassignAllMembersConfirmationLabel")) {
			private static final long serialVersionUID = 1L;

			@Override
			protected List<QName> getSupportedObjectTypes() {
				return AbstractRoleMemberPanel.this.getSupportedObjectTypes(true);
			}

			@Override
			protected List<QName> getSupportedRelations() {
				return AbstractRoleMemberPanel.this.getSupportedRelations();
			}
			
			@Override
			protected boolean isFocusTypeSelectorVisible() {
				return !QueryScope.SELECTED.equals(scope);
			}


			protected void okPerformed(QName type, Collection<QName> relations, AjaxRequestTarget target) {
				unassignMembersPerformed(type, scope, relations, target);

			}
		};

		getPageBase().showMainPopup(chooseTypePopupContent, target);
	}
	
	private void deleteMembersPerformed(AjaxRequestTarget target) {
		QueryScope scope = getQueryScope(false);
		ChooseFocusTypeAndRelationDialogPanel chooseTypePopupContent = new ChooseFocusTypeAndRelationDialogPanel(getPageBase().getMainPopupBodyId(),
					createStringResource("abstractRoleMemberPanel.deleteAllMembersConfirmationLabel")) {
			private static final long serialVersionUID = 1L;

			@Override
			protected List<QName> getSupportedObjectTypes() {
				return AbstractRoleMemberPanel.this.getSupportedObjectTypes(true);
			}

			@Override
			protected List<QName> getSupportedRelations() {
				return AbstractRoleMemberPanel.this.getSupportedRelations();
			}

			protected void okPerformed(QName type, Collection<QName> relations, AjaxRequestTarget target) {
				deleteMembersPerformed(type, scope, relations, target);

			}

			@Override
			protected boolean isFocusTypeSelectorVisible() {
				return !QueryScope.SELECTED.equals(scope);
			}

		};

		getPageBase().showMainPopup(chooseTypePopupContent, target);
		
	}
	
	protected void createFocusMemberPerformed(AjaxRequestTarget target) {

		ChooseFocusTypeAndRelationDialogPanel chooseTypePopupContent = new ChooseFocusTypeAndRelationDialogPanel(
				getPageBase().getMainPopupBodyId()) {
			private static final long serialVersionUID = 1L;
			
			@Override
			protected List<QName> getSupportedObjectTypes() {
				return AbstractRoleMemberPanel.this.getSupportedObjectTypes(false);
			}
			
			@Override
			protected List<QName> getSupportedRelations() {
				return AbstractRoleMemberPanel.this.getSupportedRelations();
			}

			protected void okPerformed(QName type, Collection<QName> relations, AjaxRequestTarget target) {
				if (relations == null || relations.isEmpty()) {
					getSession().warn("No relation was selected. Cannot create member");
					target.add(this);
					target.add(getPageBase().getFeedbackPanel());
					return;
				}
				try {
					MemberOperationsHelper.initObjectForAdd(AbstractRoleMemberPanel.this.getPageBase(), AbstractRoleMemberPanel.this.getModelObject(), type, relations, target);
				} catch (SchemaException e) {
					throw new SystemException(e.getMessage(), e);
				}

			};
		};

		getPageBase().showMainPopup(chooseTypePopupContent, target);

	}
	
	protected void deleteMembersPerformed(QName type, QueryScope scope, Collection<QName> relations, AjaxRequestTarget target) {
		if (relations == null || relations.isEmpty()) {
			getSession().warn("No relation was selected. Cannot perform delete members");
			target.add(this);
			target.add(getPageBase().getFeedbackPanel());
			return;
		}
		MemberOperationsHelper.deleteMembersPerformed(getPageBase(), scope, getActionQuery(scope, relations), type, target);
	}

	protected void unassignMembersPerformed(QName type, QueryScope scope, Collection<QName> relations, AjaxRequestTarget target) {
		if (relations == null || relations.isEmpty()) {
			getSession().warn("No relation was selected. Cannot perform unassign members");
			target.add(this);
			target.add(getPageBase().getFeedbackPanel());
			return;
		}
		MemberOperationsHelper.unassignMembersPerformed(getPageBase(), getModelObject(), scope, getActionQuery(scope, relations), relations, type, target);
	}
	
	private ObjectViewDto<OrgType> getParameter(String panelId) {
		ChooseTypePanel<OrgType> tenantChoice = (ChooseTypePanel) get(createComponentPath(ID_FORM, panelId));
		return tenantChoice.getModelObject();
	}
	
	protected ObjectQuery getActionQuery(QueryScope scope, Collection<QName> relations) {
		switch (scope) {
			case ALL:
				return createAllMemberQuery(relations);
			case ALL_DIRECT:
				return MemberOperationsHelper.createDirectMemberQuery(getModelObject(), getSearchType().getTypeQName(), relations, getParameter(ID_TENANT), getParameter(ID_PROJECT), getPrismContext());
			case SELECTED:
				return MemberOperationsHelper.createSelectedObjectsQuery(getMemberTable().getSelectedObjects());
		}

		return null;
	}
	
	protected void initSearch(Form<?> form) {

		List<SearchBoxScopeType> scopeValues = Arrays.asList(SearchBoxScopeType.values());
		DropDownFormGroup<SearchBoxScopeType> searchScrope = createDropDown(ID_SEARCH_SCOPE,
				Model.of(getMemberPanelStorage() != null ? getMemberPanelStorage().getOrgSearchScope() : scopeDefaultValue),
				scopeValues,
				WebComponentUtil.getEnumChoiceRenderer(AbstractRoleMemberPanel.this),
				"abstractRoleMemberPanel.searchScope", "abstractRoleMemberPanel.searchScope.tooltip");
		searchScrope.add(new VisibleBehaviour(() -> getModelObject() instanceof OrgType));
		form.add(searchScrope);
		
		DropDownFormGroup<QName> typeSelect = createDropDown(ID_OBJECT_TYPE,
				Model.of(getMemberPanelStorage() != null ? getMemberPanelStorage().getType().getTypeQName() : WebComponentUtil.classToQName(getPrismContext(), getDefaultObjectType())),
				getSupportedObjectTypes(true), new QNameObjectTypeChoiceRenderer(),
				"abstractRoleMemberPanel.type", "abstractRoleMemberPanel.type.tooltip");
		form.add(typeSelect);

		RelationDropDownChoicePanel relationSelector = new RelationDropDownChoicePanel(ID_SEARCH_BY_RELATION,
				getMemberPanelStorage() != null ? getMemberPanelStorage().getRelation() : null,
				getSupportedRelations(), true){
			private static final long serialVersionUID = 1L;

			@Override
			protected void onValueChanged(AjaxRequestTarget target){
				refreshAll(target);
			}
		};
		form.add(relationSelector);
		
		ChooseTypePanel<OrgType> tenant = createParameterPanel(ID_TENANT, true);
		form.add(tenant);
		tenant.add(new VisibleBehaviour(() -> getModelObject() instanceof RoleType));
		
		ChooseTypePanel<OrgType> project = createParameterPanel(ID_PROJECT, false);
		form.add(project);
		project.add(new VisibleBehaviour(() -> getModelObject() instanceof RoleType));

		CheckFormGroup includeIndirectMembers = new CheckFormGroup(ID_INDIRECT_MEMBERS,
				Model.of(getMemberPanelStorage() != null ? getMemberPanelStorage().getIndirect() : false),
					createStringResource("abstractRoleMemberPanel.indirectMembers"), "abstractRoleMemberPanel.indirectMembers.tooltip", false, "col-md-4", "col-md-2");
		includeIndirectMembers.getCheck().add(new AjaxFormComponentUpdatingBehavior("change") {
			
			private static final long serialVersionUID = 1L;
			
			protected void onUpdate(AjaxRequestTarget target) {
				refreshAll(target);
			}; 
			
		});
		
		includeIndirectMembers.getCheck().add(new EnableBehaviour(() ->
				getSearchScopeValue().equals(SearchBoxScopeType.ONE_LEVEL) || !searchScrope.isVisible()));
		includeIndirectMembers.setOutputMarkupId(true);
		form.add(includeIndirectMembers);

	}
	
	protected List<QName> getSupportedObjectTypes(boolean includeAbstractTypes) {
		return WebComponentUtil.createFocusTypeList(includeAbstractTypes);
	}
	
	private ChooseTypePanel<OrgType> createParameterPanel(String id, boolean isTenant) {

		ChooseTypePanel<OrgType> orgSelector = new ChooseTypePanel<OrgType>(id, Model.of(new ObjectViewDto())) {

			private static final long serialVersionUID = 1L;

			@Override
			protected void executeCustomAction(AjaxRequestTarget target, OrgType object) {
				refreshAll(target);
			}

			@Override
			protected void executeCustomRemoveAction(AjaxRequestTarget target) {
				refreshAll(target);
			}

			@Override
			protected ObjectQuery getChooseQuery() {
				ObjectFilter tenantFilter = QueryBuilder.queryFor(OrgType.class, getPrismContext()).item(OrgType.F_TENANT).eq(true).buildFilter();

				if (isTenant) {
					return ObjectQuery.createObjectQuery(tenantFilter);
				}
				return ObjectQuery.createObjectQuery(NotFilter.createNot(tenantFilter));

			}

			@Override
			protected boolean isSearchEnabled() {
				return true;
			}

			@Override
			public Class<OrgType> getObjectTypeClass() {
				return OrgType.class;
			}

			@Override
			protected AttributeAppender getInputStyleClass(){
				return AttributeAppender.append("class", "col-md-10");
			}

		};
		orgSelector.setOutputMarkupId(true);
		orgSelector.setOutputMarkupPlaceholderTag(true);
		return orgSelector;

	}
		
	private <V> DropDownFormGroup<V> createDropDown(String id, IModel<V> defaultModel, final List<V> values,
			IChoiceRenderer<V> renderer, String labelKey, String tooltipKey) {
		DropDownFormGroup<V> listSelect = new DropDownFormGroup<V>(id, defaultModel, Model.ofList(values), renderer, createStringResource(labelKey), 
				tooltipKey, false, "col-md-4", "col-md-8", true);

		listSelect.getInput().add(new OnChangeAjaxBehavior() {
			private static final long serialVersionUID = 1L;

			@Override
			protected void onUpdate(AjaxRequestTarget target) {
				refreshAll(target);
			}
		});
		listSelect.setOutputMarkupId(true);
		return listSelect;
	}

	protected void refreshAll(AjaxRequestTarget target) {
		updateMembersPanelSessionStorage();

		DropDownFormGroup<QName> typeChoice = (DropDownFormGroup) get(createComponentPath(ID_FORM, ID_OBJECT_TYPE));
		QName type = getMemberPanelStorage() != null ? getMemberPanelStorage().getType().getTypeQName() : typeChoice.getModelObject();
		getMemberTable().clearCache();
		getMemberTable().refreshTable(WebComponentUtil.qnameToClass(getPrismContext(), type, FocusType.class), target);
		target.add(this);
	}

	
	private MainObjectListPanel<FocusType> getMemberTable() {
		return (MainObjectListPanel<FocusType>) get(createComponentPath(ID_FORM, ID_CONTAINER_MEMBER, ID_MEMBER_TABLE));
	}
	
	protected QueryScope getQueryScope(boolean isRecompute) {
		if (CollectionUtils.isNotEmpty(MemberOperationsHelper.getFocusOidToRecompute(getMemberTable().getSelectedObjects()))) {
			return QueryScope.SELECTED;
		}
		
		if (getIndirectmembersPanel().getValue()) {
			return QueryScope.ALL;
		}
		
		return QueryScope.ALL_DIRECT;
	}
	
	private CheckFormGroup getIndirectmembersPanel() {
		return (CheckFormGroup) get(createComponentPath(ID_FORM, ID_INDIRECT_MEMBERS));
	}
	
	protected void recomputeMembersPerformed(AjaxRequestTarget target) {
		MemberOperationsHelper.recomputeMembersPerformed(getPageBase(), getQueryScope(true), getActionQuery(getQueryScope(true), getSupportedRelations()), getSupportedRelations(), target);
		
	}

	protected ObjectQuery createContentQuery() {
		CheckFormGroup isIndirect = getIndirectmembersPanel();
		List<QName> relations = QNameUtil.match(getSelectedRelation(), PrismConstants.Q_ANY) ? getSupportedRelations() : Arrays.asList(getSelectedRelation());  
 		return createMemberQuery(isIndirect != null ? isIndirect.getValue() : false, relations);

	}

	protected QName getSelectedRelation(){
		MemberPanelStorage storage = getMemberPanelStorage();
		if (storage != null){
			return storage.getRelation();
		}
		RelationDropDownChoicePanel relationDropDown = (RelationDropDownChoicePanel) get(createComponentPath(ID_FORM, ID_SEARCH_BY_RELATION));
		return relationDropDown.getRelationValue();
	}

	private SearchBoxScopeType getSearchScopeValue(){
		if (getMemberPanelStorage() != null){
			return getMemberPanelStorage().getOrgSearchScope();
		}
		DropDownFormGroup<SearchBoxScopeType> searchScopeComponent = (DropDownFormGroup<SearchBoxScopeType>)get(createComponentPath(ID_FORM, ID_SEARCH_SCOPE));
		return searchScopeComponent.getModelObject();
	}
	
	protected ObjectTypes getSearchType() {
		DropDownFormGroup<QName> searchByTypeChoice = (DropDownFormGroup<QName>) get(
				createComponentPath(ID_FORM, ID_OBJECT_TYPE));
		QName typeName = searchByTypeChoice.getModelObject();
		return ObjectTypes.getObjectTypeFromTypeQName(typeName);
	}
	
	protected ObjectQuery createMemberQuery(boolean indirect, Collection<QName> relations) {
		if (indirect) {
			return createAllMemberQuery(relations);
		}
		
		return MemberOperationsHelper.createDirectMemberQuery(getModelObject(), getSearchType().getTypeQName(), relations, getParameter(ID_TENANT), getParameter(ID_PROJECT), getPrismContext());
	}


	protected ObjectQuery createAllMemberQuery(Collection<QName> relations) {
		return QueryBuilder.queryFor(FocusType.class, getPrismContext())
				.item(FocusType.F_ROLE_MEMBERSHIP_REF).ref(MemberOperationsHelper.createReferenceValuesList(getModelObject(), relations))
				.build();
	}
	
	
	protected ObjectReferenceType createReference() {
		ObjectReferenceType ref = ObjectTypeUtil.createObjectRef(getModelObject(), getPageBase().getPrismContext());
		return ref;
	}

	protected void detailsPerformed(AjaxRequestTarget target, ObjectType object) {
		if (WebComponentUtil.hasDetailsPage(object.getClass())) {
			WebComponentUtil.dispatchToObjectDetailsPage(object.getClass(), object.getOid(), this, true);
		} else {
			error("Could not find proper response page");
			throw new RestartResponseException(getPageBase());
		}
	}

	protected List<IColumn<SelectableBean<ObjectType>, String>> createMembersColumns() {
		List<IColumn<SelectableBean<ObjectType>, String>> columns = new ArrayList<>();

		IColumn<SelectableBean<ObjectType>, String> column = new AbstractExportableColumn<SelectableBean<ObjectType>, String>(
				createStringResource("TreeTablePanel.fullName.displayName")) {
			private static final long serialVersionUID = 1L;

			@Override
			public void populateItem(Item<ICellPopulator<SelectableBean<ObjectType>>> cellItem,
					String componentId, IModel<SelectableBean<ObjectType>> rowModel) {
				SelectableBean<ObjectType> bean = rowModel.getObject();
				ObjectType object = bean.getValue();
				cellItem.add(new Label(componentId,
							getMemberObjectDisplayName(object)));
			}

			@Override
			public IModel<String> getDataModel(IModel<SelectableBean<ObjectType>> rowModel) {
				return Model.of(getMemberObjectDisplayName(rowModel.getObject().getValue()));
			}

		};
		columns.add(column);

		column = new AbstractExportableColumn<SelectableBean<ObjectType>, String>(
				createStringResource("TreeTablePanel.identifier.description")) {
			private static final long serialVersionUID = 1L;

			@Override
			public void populateItem(Item<ICellPopulator<SelectableBean<ObjectType>>> cellItem,
					String componentId, IModel<SelectableBean<ObjectType>> rowModel) {
				SelectableBean<ObjectType> bean = rowModel.getObject();
				ObjectType object = bean.getValue();
				cellItem.add(new Label(componentId, getMemberObjectIdentifier(object)));
			}

			@Override
			public IModel<String> getDataModel(IModel<SelectableBean<ObjectType>> rowModel) {
				return Model.of(getMemberObjectIdentifier(rowModel.getObject().getValue()));
			}

		};
		columns.add(column);
//		if (isRelationColumnVisible()){
			columns.add(createRelationColumn());
//		}
		return columns;
	}

	protected IColumn<SelectableBean<ObjectType>, String> createRelationColumn() {
		return new AbstractExportableColumn<SelectableBean<ObjectType>, String>(
				createStringResource("roleMemberPanel.relation")) {
			private static final long serialVersionUID = 1L;

			@Override
			public void populateItem(Item<ICellPopulator<SelectableBean<ObjectType>>> cellItem,
									 String componentId, IModel<SelectableBean<ObjectType>> rowModel) {
				cellItem.add(new Label(componentId,
						getRelationValue(rowModel.getObject().getValue())));
			}

			@Override
			public IModel<String> getDataModel(IModel<SelectableBean<ObjectType>> rowModel) {
				return Model.of(getRelationValue(rowModel.getObject().getValue()));
			}

		};
	}

	protected boolean isRelationColumnVisible(){
		return false;
	}


	private String getMemberObjectDisplayName(ObjectType object){
		if (object == null){
			return "";
		}
		if (object instanceof UserType) {
			return WebComponentUtil.getOrigStringFromPoly(((UserType) object).getFullName());
		} else if (object instanceof AbstractRoleType) {
			return WebComponentUtil
					.getOrigStringFromPoly(((AbstractRoleType) object).getDisplayName());
		} else {
			return "";
		}
	}

	private String getMemberObjectIdentifier(ObjectType object){
		if (object == null){
			return "";
		}
		if (object instanceof UserType) {
			return ((UserType) object).getEmailAddress();
		} else if (object instanceof AbstractRoleType) {
			return ((AbstractRoleType) object).getIdentifier();
		} else {
			return object.getDescription();
		}
	}

	private Collection<SelectorOptions<GetOperationOptions>> getSearchOptions(){
			return SelectorOptions
					.createCollection(GetOperationOptions.createDistinct());
	}

	protected <O extends ObjectType> Class<O> getDefaultObjectType(){
		return (Class<O>) FocusType.class;
	}

	protected Form getFormComponent(){
		return (Form) get(ID_FORM);
	}


	private String getRelationValue(ObjectType focusObject){
		String relation = "";
		if (FocusType.class.isAssignableFrom(focusObject.getClass())) {
			// Do NOT take relation from an assignment. Use roleMembershipRef instead. Reasons:
			// 1. Authorizations (MID-4893). User may be authorized just for roleMemberhsipRef and not for assignment
			//    Authorization for roleMembershipRef is enough to display member panel.
			// 2. There may be assignments that are not valid. We do not want to display relation for those.
			for (ObjectReferenceType roleMembershipRef : ((FocusType) focusObject).getRoleMembershipRef()) {
				relation = buildRelation(roleMembershipRef, relation);
			}
			
		} 
		return relation;
				
	}
	
	private String buildRelation(ObjectReferenceType roleMembershipRef, String relation) {
		if (roleMembershipRef.getOid().equals(getModelObject().getOid())) {
			QName assignmentRelation = roleMembershipRef.getRelation();
			if (getSupportedRelations().stream().anyMatch(r -> QNameUtil.match(r, assignmentRelation))) {
				if (!StringUtils.isBlank(relation)) {
					relation += ",";
				}
				relation += assignmentRelation.getLocalPart();
			}
		}
		return relation;
	}

	protected void updateMembersPanelSessionStorage(){
		MemberPanelStorage storage = getMemberPanelStorage();
		if (storage != null){
			storage.setType(getSearchType());

			RelationDropDownChoicePanel relationDropDown = (RelationDropDownChoicePanel) get(createComponentPath(ID_FORM, ID_SEARCH_BY_RELATION));
			storage.setRelation(relationDropDown.getRelationValue());

			CheckFormGroup indirectPanel = getIndirectmembersPanel();
			if (indirectPanel != null){
				storage.setIndirect(indirectPanel.getValue());
			}

			DropDownFormGroup<SearchBoxScopeType> searchScopeComponent =
					(DropDownFormGroup<SearchBoxScopeType>)get(createComponentPath(ID_FORM, ID_SEARCH_SCOPE));
			storage.setOrgSearchScope(searchScopeComponent.getModelObject());
		}
	}

	protected MemberPanelStorage getMemberPanelStorage(){
		return null;
	}
}

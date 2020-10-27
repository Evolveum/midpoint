/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.configuration.component;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.evolveum.midpoint.model.api.AssignmentObjectRelation;
import com.evolveum.midpoint.web.session.SessionStorage;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.DisplayNamePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanelWithDetailsPanel;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn.ColumnType;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismContainerWrapperColumn;
import com.evolveum.midpoint.gui.impl.component.data.column.PrismPropertyWrapperColumn;
import com.evolveum.midpoint.gui.impl.factory.panel.ItemRealValueModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.QueryFactory;
import com.evolveum.midpoint.prism.query.TypeFilter;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.InlineMenuButtonColumn;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.search.SearchFactory;
import com.evolveum.midpoint.web.component.search.SearchItemDefinition;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage.TableId;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AreaCategoryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExclusionPolicyConstraintType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GlobalPolicyRuleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSelectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PolicyConstraintsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;


/**
 * @author skublik
 */
public class GlobalPolicyRuleTabPanel<S extends Serializable> extends BasePanel<PrismContainerWrapper<GlobalPolicyRuleType>> {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(GlobalPolicyRuleTabPanel.class);

    private static final String ID_GLOBAL_POLICY_RULE = "globalPolicyRule";

    public GlobalPolicyRuleTabPanel(String id, IModel<PrismContainerWrapper<GlobalPolicyRuleType>> model) {
        super(id, model);

    }

    @Override
    protected void onInitialize() {
            super.onInitialize();
            initLayout();
    }

    protected void initLayout() {
        MultivalueContainerListPanelWithDetailsPanel<GlobalPolicyRuleType> multivalueContainerListPanel =
                new MultivalueContainerListPanelWithDetailsPanel<GlobalPolicyRuleType>(ID_GLOBAL_POLICY_RULE, GlobalPolicyRuleType.class) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void newItemPerformed(AjaxRequestTarget target, AssignmentObjectRelation relation) {
                newGlobalPolicuRuleClickPerformed(target);
            }

            @Override
            protected boolean isCreateNewObjectVisible() {
                return true;
            }

            @Override
            protected IModel<PrismContainerWrapper<GlobalPolicyRuleType>> getContainerModel() {
                return GlobalPolicyRuleTabPanel.this.getModel();
            }

            @Override
            protected ObjectQuery createQuery() {
                    return GlobalPolicyRuleTabPanel.this.createQuery();
            }

            @Override
            protected String getStorageKey() {
                return SessionStorage.KEY_OBJECT_POLICIES_TAB;
            }

            @Override
            protected TableId getTableId() {
                return UserProfileStorage.TableId.OBJECT_POLICIES_TAB_TABLE;            }

            @Override
            protected List<IColumn<PrismContainerValueWrapper<GlobalPolicyRuleType>, String>> createColumns() {
                return initBasicColumns();
            }

                    @Override
            protected MultivalueContainerDetailsPanel<GlobalPolicyRuleType> getMultivalueContainerDetailsPanel(
                    ListItem<PrismContainerValueWrapper<GlobalPolicyRuleType>> item) {
                return GlobalPolicyRuleTabPanel.this.getMultivalueContainerDetailsPanel(item);
            }

            @Override
            protected List<SearchItemDefinition> initSearchableItems(
                    PrismContainerDefinition<GlobalPolicyRuleType> containerDef) {
                List<SearchItemDefinition> defs = new ArrayList<>();

                SearchFactory.addSearchPropertyDef(containerDef, ItemPath
                        .create(GlobalPolicyRuleType.F_FOCUS_SELECTOR, ObjectSelectorType.F_SUBTYPE), defs);
                SearchFactory.addSearchRefDef(containerDef,
                        ItemPath.create(GlobalPolicyRuleType.F_POLICY_CONSTRAINTS,
                                PolicyConstraintsType.F_EXCLUSION, ExclusionPolicyConstraintType.F_TARGET_REF), defs, AreaCategoryType.POLICY, getPageBase());

                defs.addAll(SearchFactory.createExtensionDefinitionList(containerDef));

                return defs;
            }
        };

        add(multivalueContainerListPanel);

        setOutputMarkupId(true);
    }

    protected void newGlobalPolicuRuleClickPerformed(AjaxRequestTarget target) {
        //TODO maybe change to itemFactory.createContainerValue()???
        PrismContainerValue<GlobalPolicyRuleType> newObjectPolicy = getModelObject().getItem().createNewValue();
        PrismContainerValueWrapper<GlobalPolicyRuleType> newObjectPolicyWrapper = getMultivalueContainerListPanel().createNewItemContainerValueWrapper(newObjectPolicy, getModelObject(), target);
        getMultivalueContainerListPanel().itemDetailsPerformed(target, Arrays.asList(newObjectPolicyWrapper));
    }

    private MultivalueContainerDetailsPanel<GlobalPolicyRuleType> getMultivalueContainerDetailsPanel(
            ListItem<PrismContainerValueWrapper<GlobalPolicyRuleType>> item) {
        MultivalueContainerDetailsPanel<GlobalPolicyRuleType> detailsPanel =
                new  MultivalueContainerDetailsPanel<GlobalPolicyRuleType>(MultivalueContainerListPanelWithDetailsPanel.ID_ITEM_DETAILS, item.getModel()) {

            private static final long serialVersionUID = 1L;

            @Override
            protected DisplayNamePanel<GlobalPolicyRuleType> createDisplayNamePanel(String displayNamePanelId) {
                ItemRealValueModel<GlobalPolicyRuleType> displayNameModel =
                        new ItemRealValueModel<GlobalPolicyRuleType>(item.getModel());
                return new DisplayNamePanel<GlobalPolicyRuleType>(displayNamePanelId, displayNameModel);
            }

        };
        return detailsPanel;
    }

    private MultivalueContainerListPanelWithDetailsPanel<GlobalPolicyRuleType> getMultivalueContainerListPanel(){
        return ((MultivalueContainerListPanelWithDetailsPanel<GlobalPolicyRuleType>)get(ID_GLOBAL_POLICY_RULE));
    }

    private ObjectQuery createQuery() {
        QueryFactory factory = getPrismContext().queryFactory();
        TypeFilter filter = factory.createType(GlobalPolicyRuleType.COMPLEX_TYPE, factory.createAll());
        return factory.createQuery(filter);
    }

    private void initPaging() {
//        getPageBase().getSessionStorage().getGlobalPolicyRulesTabStorage()
//                .setPaging(getPrismContext().queryFactory().createPaging(0, (int) ((PageBase)getPage()).getItemsPerPage(UserProfileStorage.TableId.GLOBAL_POLICY_RULES_TAB_TABLE)));
    }

    private List<IColumn<PrismContainerValueWrapper<GlobalPolicyRuleType>, String>> initBasicColumns() {
        List<IColumn<PrismContainerValueWrapper<GlobalPolicyRuleType>, String>> columns = new ArrayList<>();

        columns.add(new CheckBoxHeaderColumn<>());

        PrismPropertyWrapperColumn<GlobalPolicyRuleType, String> linkColumn = new PrismPropertyWrapperColumn<GlobalPolicyRuleType, String>(getModel(), GlobalPolicyRuleType.F_NAME, ColumnType.LINK, getPageBase()) {

            @Override
            protected void onClick(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<GlobalPolicyRuleType>> rowModel) {
                getMultivalueContainerListPanel().itemDetailsPerformed(target, rowModel);
            }

            protected DisplayType getIconDisplayType(IModel<PrismContainerValueWrapper<GlobalPolicyRuleType>> rowModel) {
                return WebComponentUtil.createDisplayType(WebComponentUtil.createDefaultBlackIcon(SystemConfigurationType.COMPLEX_TYPE));

            }


        };

        columns.add(linkColumn);

        columns.add(new PrismContainerWrapperColumn<>(getModel(), GlobalPolicyRuleType.F_POLICY_CONSTRAINTS, getPageBase()));

        columns.add(new PrismContainerWrapperColumn<>(getModel(),GlobalPolicyRuleType.F_POLICY_ACTIONS, getPageBase()));

        columns.add(new PrismPropertyWrapperColumn<GlobalPolicyRuleType, String>(getModel(), GlobalPolicyRuleType.F_POLICY_SITUATION, ColumnType.STRING, getPageBase()));

        List<InlineMenuItem> menuActionsList = getMultivalueContainerListPanel().getDefaultMenuActions();
        columns.add(new InlineMenuButtonColumn(menuActionsList, getPageBase()) {

            private static final long serialVersionUID = 1L;

            @Override
            public String getCssClass() {
                return " col-md-1 ";
            }

        });

        return columns;
    }
}


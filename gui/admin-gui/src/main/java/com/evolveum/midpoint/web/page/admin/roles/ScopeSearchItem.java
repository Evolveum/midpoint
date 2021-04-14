/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.roles;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.component.search.SearchSpecialItemPanel;
import com.evolveum.midpoint.web.component.search.SpecialSearchItem;
import com.evolveum.midpoint.web.session.MemberPanelStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import java.util.Arrays;
import java.util.function.Consumer;

public class ScopeSearchItem extends SpecialSearchItem {

    private static final Trace LOGGER = TraceManager.getTrace(TenantSearchItem.class);

    private MemberPanelStorage memberStorage;
    private AvailableRelationDto supportedRelations;
    private UserInterfaceFeatureType scopeConfig;

    public ScopeSearchItem(Search search, MemberPanelStorage memberStorage, AvailableRelationDto supportedRelations, ScopeSearchItemConfigurationType scopeConfig) {
        super(search);
        this.memberStorage = memberStorage;
        this.supportedRelations = supportedRelations;
        this.scopeConfig = scopeConfig;
    }

    @Override
    public ObjectFilter createFilter(PageBase pageBase, VariablesMap variables) {
        AbstractRoleType object = getParentVariables(variables);
        if (object == null) {
            return null;
        }
        Class type = getSearch().getTypeClass();
        ObjectReferenceType ref = MemberOperationsHelper.createReference(object, null);
        return pageBase.getPrismContext().queryFor(type).isChildOf(ref.asReferenceValue()).buildFilter();
    }

    @Override
    public boolean isApplyFilter() {
        return SearchBoxScopeType.SUBTREE.equals(getMemberPanelStorage().getOrgSearchScope());
    }

    @Override
    public SearchSpecialItemPanel createSpecialSearchPanel(String id, Consumer<AjaxRequestTarget> searchPerformedConsumer) {
        return new SearchSpecialItemPanel(id, new PropertyModel(getMemberPanelStorage(), MemberPanelStorage.F_ORG_SEARCH_SCOPE)) {
            @Override
            protected WebMarkupContainer initSearchItemField(String id) {
                DropDownChoicePanel inputPanel = new DropDownChoicePanel(id, getModelValue(), Model.of(Arrays.asList(SearchBoxScopeType.values())), new EnumChoiceRenderer(), false);
                inputPanel.getBaseFormComponent().add(WebComponentUtil.getSubmitOnEnterKeyDownBehavior("searchSimple"));
                inputPanel.getBaseFormComponent().add(AttributeAppender.append("style", "width: 88px; max-width: 400px !important;"));
                inputPanel.getBaseFormComponent().add(new OnChangeAjaxBehavior() {
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        searchPerformedConsumer.accept(target);
                    }
                });
                inputPanel.setOutputMarkupId(true);
                return inputPanel;
            }

            @Override
            protected IModel<String> createLabelModel() {
                return Model.of(WebComponentUtil.getTranslatedPolyString(scopeConfig.getDisplay().getLabel()));
            }

            @Override
            protected IModel<String> createHelpModel(){
                return Model.of(WebComponentUtil.getTranslatedPolyString(scopeConfig.getDisplay().getHelp()));
            }
        };
    }

    private <R extends AbstractRoleType> R getParentVariables(VariablesMap variables) {
        if (variables == null) {
            return null;
        }
        try {
            return (R) variables.getValue(ExpressionConstants.VAR_PARENT_OBJECT, AbstractRoleType.class);
        } catch (SchemaException e) {
            LOGGER.error("Couldn't load parent object.");
        }
        return null;
    }

    public AvailableRelationDto getSupportedRelations() {
        return supportedRelations;
    }

    public MemberPanelStorage getMemberPanelStorage() {
        return memberStorage;
    }

}

/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.roles;

import java.util.Arrays;
import java.util.function.Consumer;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.component.search.SearchSpecialItemPanel;
import com.evolveum.midpoint.web.component.search.SpecialSearchItem;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScopeSearchItemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchBoxScopeType;

public class ScopeSearchItem extends SpecialSearchItem {

    private static final Trace LOGGER = TraceManager.getTrace(TenantSearchItem.class);

    private IModel<ScopeSearchItemConfigurationType> scopeConfigModel;

    public ScopeSearchItem(Search search, IModel<ScopeSearchItemConfigurationType> scopeConfigModel) {
        super(search);
        this.scopeConfigModel = scopeConfigModel;
    }

    private ScopeSearchItemConfigurationType getScopeConfig() {
        return scopeConfigModel.getObject();
    }
    @Override
    public ObjectFilter createFilter(PageBase pageBase, VariablesMap variables) {
        throw new UnsupportedOperationException();
//        AbstractRoleType object = getParentVariables(variables);
//        if (object == null) {
//            return null;
//        }
//        Class type = getSearch().getTypeClass();
//        ObjectReferenceType ref = MemberOperationsHelperOld.createReference(object, null);
//        return pageBase.getPrismContext().queryFor(type).isChildOf(ref.asReferenceValue()).buildFilter();
    }

    @Override
    public boolean isApplyFilter() {
        return  getScopeConfig() != null && getScopeConfig().getDefaultValue() == SearchBoxScopeType.SUBTREE;
    }

    @Override
    public SearchSpecialItemPanel createSpecialSearchPanel(String id, Consumer<AjaxRequestTarget> searchPerformedConsumer) {
        return new SearchSpecialItemPanel(id, new PropertyModel(scopeConfigModel, ScopeSearchItemConfigurationType.F_DEFAULT_VALUE.getLocalPart())) {


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
                return Model.of(WebComponentUtil.getTranslatedPolyString(getScopeConfig().getDisplay().getLabel()));
            }

            @Override
            protected IModel<String> createHelpModel(){
                return Model.of(WebComponentUtil.getTranslatedPolyString(getScopeConfig().getDisplay().getHelp()));
            }
        };
    }


}

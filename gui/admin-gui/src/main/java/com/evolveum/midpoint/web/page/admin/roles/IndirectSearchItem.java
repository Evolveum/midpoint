/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.roles;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.component.search.SearchSpecialItemPanel;
import com.evolveum.midpoint.web.component.search.SpecialSearchItem;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.session.MemberPanelStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class IndirectSearchItem extends SpecialSearchItem {

    private static final Trace LOGGER = TraceManager.getTrace(IndirectSearchItem.class);

    private MemberPanelStorage memberStorage;

    public IndirectSearchItem(Search search, MemberPanelStorage memberStorage) {
        super(search);
        this.memberStorage = memberStorage;
    }

    @Override
    public ObjectFilter createFilter(PageBase pageBase, VariablesMap variables) {
        throw new UnsupportedOperationException();
//        AbstractRoleType object = getParentVariables(variables);
//        if (object == null) {
//            return null;
//        }
//        List relations = new ArrayList();
//        if (QNameUtil.match(PrismConstants.Q_ANY, memberStorage.getRelation())) {
//            relations.addAll(supportedRelations.getAvailableRelationList());
//        } else {
//            relations.add(memberStorage.getRelation());
//        }
//
//        ObjectFilter filter;
//        PrismContext prismContext = pageBase.getPrismContext();
//        Class type = getSearch().getTypeClass();
//        if(!Boolean.TRUE.equals(memberStorage.getIndirect())) {
//            filter = prismContext.queryFor(type).exists(AssignmentHolderType.F_ASSIGNMENT)
//                    .block()
//                    .item(AssignmentType.F_TARGET_REF)
//                    .ref(MemberOperationsHelper.createReferenceValuesList(object, relations))
//                    .endBlock().buildFilter();
//        } else {
//            filter = prismContext.queryFor(type)
//                    .item(FocusType.F_ROLE_MEMBERSHIP_REF).ref(MemberOperationsHelper.createReferenceValuesList(object, relations))
//                    .buildFilter();
//        }
//        return filter;
    }


    private IndirectSearchItemConfigurationType getIndirectConfig() {
        return memberStorage.getIndirectSearchItem();
    }

    @Override
    public SearchSpecialItemPanel createSpecialSearchPanel(String id, Consumer<AjaxRequestTarget> searchPerformedConsumer) {
        SearchSpecialItemPanel panel = new SearchSpecialItemPanel(id, new PropertyModel(memberStorage, MemberPanelStorage.F_INDIRECT_ITEM + "." + IndirectSearchItemConfigurationType.F_INDIRECT.getLocalPart())) {
            @Override
            protected WebMarkupContainer initSearchItemField(String id) {
                List<Boolean> choices = new ArrayList<>();
                choices.add(Boolean.TRUE);
                choices.add(Boolean.FALSE);
                DropDownChoicePanel inputPanel = new DropDownChoicePanel(id, getModelValue(), Model.ofList(choices), new ChoiceRenderer<Boolean>() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public Object getDisplayValue(Boolean val) {
                        if (val) {
                            return getPageBase().createStringResource("Boolean.TRUE").getString();
                        }
                        return getPageBase().createStringResource("Boolean.FALSE").getString();
                    }
                }, false);
                inputPanel.getBaseFormComponent().add(WebComponentUtil.getSubmitOnEnterKeyDownBehavior("searchSimple"));
                inputPanel.getBaseFormComponent().add(AttributeAppender.append("style", "width: 68"
                        + "px; max-width: 400px !important;"));
                inputPanel.getBaseFormComponent().add(new OnChangeAjaxBehavior() {
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        searchPerformedConsumer.accept(target);
                    }
                });
                inputPanel.getBaseFormComponent().add(new EnableBehaviour(() -> memberStorage != null && !SearchBoxScopeType.SUBTREE.equals(memberStorage.getOrgSearchScope())));
                inputPanel.setOutputMarkupId(true);
                return inputPanel;
            }

            @Override
            protected IModel<String> createLabelModel() {
                return Model.of(WebComponentUtil.getTranslatedPolyString(getIndirectConfig().getDisplay().getLabel()));
            }

            @Override
            protected IModel<String> createHelpModel(){
                return Model.of(WebComponentUtil.getTranslatedPolyString(getIndirectConfig().getDisplay().getHelp()));
            }
        };
        panel.add(new VisibleBehaviour(() -> isPanelVisible()));
        return panel;
    }

    protected boolean isPanelVisible() {
        return false;
    }
}

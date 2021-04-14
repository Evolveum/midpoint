/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.roles;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.input.QNameIChoiceRenderer;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.builder.S_AtomicFilterExit;
import com.evolveum.midpoint.prism.util.PolyStringUtils;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.input.DropDownChoicePanel;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.component.search.SearchItem;
import com.evolveum.midpoint.web.component.search.SearchSpecialItemPanel;
import com.evolveum.midpoint.web.component.search.SpecialSearchItem;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.web.session.MemberPanelStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.catalina.users.AbstractRole;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class RelationSearchItem extends SpecialSearchItem {

    private static final Trace LOGGER = TraceManager.getTrace(RelationSearchItem.class);

    private MemberPanelStorage memberStorage;
    private AvailableRelationDto supportedRelations;
    private RelationSearchItemConfigurationType relationConfig;

    public RelationSearchItem(Search search, MemberPanelStorage memberStorage, AvailableRelationDto supportedRelations, RelationSearchItemConfigurationType relationConfig) {
        super(search);
        this.memberStorage = memberStorage;
        this.supportedRelations = supportedRelations;
        this.relationConfig = relationConfig;
    }

//    private SearchItem createRelationItem(Search search) {
//        return new SpecialSearchItem(search) {
//            @Override
    public ObjectFilter createFilter(PageBase pageBase, VariablesMap variables) {
        throw new UnsupportedOperationException();
//                AbstractRoleType object = getParentVariables(variables);
//                if (object == null) {
//                    return null;
//                }
//                PrismContext prismContext = pageBase.getPrismContext();
//                List relations;
//                QName relation = memberStorage.getRelation();
//                if (QNameUtil.match(relation, PrismConstants.Q_ANY)){
//                    relations = supportedRelations.getAvailableRelationList();
//                } else {
//                    relations = Collections.singletonList(relation);
//                }
//
//                ObjectFilter filter;
//                Boolean indirect = memberStorage.getIndirect();
//                Class type = getSearch().getTypeClass();
//                if(!Boolean.TRUE.equals(indirect)) {
//                    S_AtomicFilterExit q = prismContext.queryFor(type).exists(AssignmentHolderType.F_ASSIGNMENT)
//                            .block()
//                            .item(AssignmentType.F_TARGET_REF)
//                            .ref(MemberOperationsHelper.createReferenceValuesList(object, relations));
//
//                    if (!memberStorage.isTenantEmpty()) {
//                        q = q.and().item(AssignmentType.F_TENANT_REF).ref(memberStorage.getTenant().getOid());
//                    }
//
//                    if (!memberStorage.isProjectEmpty()) {
//                        q = q.and().item(AssignmentType.F_ORG_REF).ref(memberStorage.getProject().getOid());
//                    }
//                    filter = q.endBlock().buildFilter();
//                } else {
//                    filter = prismContext.queryFor(type)
//                            .item(FocusType.F_ROLE_MEMBERSHIP_REF).ref(MemberOperationsHelper.createReferenceValuesList(object, relations))
//                            .buildFilter();
//                }
//                return filter;
            }

//            @Override
//            public boolean isApplyFilter() {
//                return !CompiledGuiProfile.isVisible(abstractRoleMemberSearchConfiguration.getDefaultSearchScopeVisibility(), null)
//                        || !SearchBoxScopeType.SUBTREE.equals(getMemberPanelStorage().getOrgSearchScope());
//            }

            @Override
            public SearchSpecialItemPanel createSpecialSearchPanel(String id, Consumer<AjaxRequestTarget> searchPerformedConsumer) {
                return new SearchSpecialItemPanel(id, new PropertyModel(memberStorage, MemberPanelStorage.F_RELATION)) {
                    @Override
                    protected WebMarkupContainer initSearchItemField(String id) {

                        List<QName> choices = new ArrayList();
                        List<QName> relations = supportedRelations.getAvailableRelationList();
                        if (relations != null && relations.size() > 1) {
                            choices.add(PrismConstants.Q_ANY);
                        }
                        choices.addAll(relations);

                        DropDownChoicePanel inputPanel = new DropDownChoicePanel(id, getModelValue(), Model.of(choices), new QNameIChoiceRenderer() {
                            private static final long serialVersionUID = 1L;

                            @Override
                            public Object getDisplayValue(QName relation) {
                                RelationDefinitionType relationDef = WebComponentUtil.getRelationDefinition(relation);
                                if (relationDef != null) {
                                    DisplayType display = relationDef.getDisplay();
                                    if (display != null) {
                                        PolyStringType label = display.getLabel();
                                        if (PolyStringUtils.isNotEmpty(label)) {
                                            return WebComponentUtil.getTranslatedPolyString(label);
                                        }
                                    }
                                }
                                if (QNameUtil.match(PrismConstants.Q_ANY, relation)) {
                                    return new ResourceModel("RelationTypes.ANY", relation.getLocalPart()).getObject();
                                }
                                return super.getDisplayValue(relation);
                            }
                        }, false);
                        inputPanel.getBaseFormComponent().add(WebComponentUtil.getSubmitOnEnterKeyDownBehavior("searchSimple"));
                        inputPanel.getBaseFormComponent().add(AttributeAppender.append("style", "width: 100px; max-width: 400px !important;"));
                        inputPanel.getBaseFormComponent().add(new OnChangeAjaxBehavior() {
                            @Override
                            protected void onUpdate(AjaxRequestTarget target) {
                                searchPerformedConsumer.accept(target);
                            }
                        });

                        inputPanel.getBaseFormComponent().add(new EnableBehaviour(() -> supportedRelations.getAvailableRelationList().size() > 1));
                        inputPanel.setOutputMarkupId(true);
                        return inputPanel;
                    }

                    @Override
                    protected IModel<String> createLabelModel() {
                        return Model.of(WebComponentUtil.getTranslatedPolyString(relationConfig.getDisplay().getLabel()));
                    }

                    @Override
                    protected IModel<String> createHelpModel(){
                        return Model.of(WebComponentUtil.getTranslatedPolyString(relationConfig.getDisplay().getHelp()));
                    }
                };
            }
//        };
//    }

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
}

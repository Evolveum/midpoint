/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.wizard.resource.component.schemahandling;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.NonEmptyPropertyModel;
import com.evolveum.midpoint.web.component.input.SearchFilterPanel;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
import com.evolveum.midpoint.web.page.admin.resources.PageResourceWizard;
import com.evolveum.midpoint.web.util.InfoTooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectPatternType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import java.util.ArrayList;
import java.util.List;

/**
 *  @author shood
 * */
public class ResourceProtectedEditor extends BasePanel<List<ResourceObjectPatternType>> {

    private enum ChangeState {
        SKIP, FIRST, LAST
    }

    private static final String ID_CONTAINER = "protectedContainer";
    private static final String ID_REPEATER = "repeater";
    private static final String ID_ACCOUNT_LINK = "accountLink";
    private static final String ID_ACCOUNT_NAME = "accountLinkName";
    private static final String ID_ACCOUNT_BODY = "accountBodyContainer";
    private static final String ID_NAME = "name";
    private static final String ID_UID = "uid";
    private static final String ID_FILTER_EDITOR = "filterClause";
    private static final String ID_BUTTON_ADD = "addButton";
    private static final String ID_BUTTON_DELETE = "deleteAccount";
    private static final String ID_T_NAME = "nameTooltip";
    private static final String ID_T_UID = "uidTooltip";
    private static final String ID_T_FILTER = "filterTooltip";

    private ChangeState changeState = ChangeState.FIRST;

    public ResourceProtectedEditor(String id, IModel<List<ResourceObjectPatternType>> model, PageResourceWizard parentPage) {
        super(id, model);
        initLayout(parentPage);
        if (model.getObject() == null) {        // shouldn't occur, actually
            model.setObject(new ArrayList<>());
        } else {
            for (ResourceObjectPatternType pattern : model.getObject()) {
                if (pattern.getFilter() == null) {
                    pattern.setFilter(new SearchFilterType());            // in order for SearchFilterPanel work correctly; is normalized before saving resource
                }
            }
        }
    }

    protected void initLayout(final PageResourceWizard parentPage) {
        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.setOutputMarkupId(true);
        add(container);

        ListView repeater = new ListView<ResourceObjectPatternType>(ID_REPEATER, getModel()){

            @Override
            protected void populateItem(final ListItem<ResourceObjectPatternType> item){
                WebMarkupContainer linkCont = new WebMarkupContainer(ID_ACCOUNT_LINK);
                linkCont.setOutputMarkupId(true);
                linkCont.add(new AttributeModifier("href", createCollapseItemId(item, true)));
                item.add(linkCont);

                Label accountLabel = new Label(ID_ACCOUNT_NAME, new IModel<String>() {

                    @Override
                    public String getObject() {
                        StringBuilder sb = new StringBuilder();
                        ResourceObjectPatternType account = item.getModelObject();
                        sb.append("#").append(item.getIndex()+1).append(" - ");
                        return sb.toString();
                    }
                });
                linkCont.add(accountLabel);

                AjaxLink<Void> delete = new AjaxLink<Void>(ID_BUTTON_DELETE) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        deleteProtectedAccountPerformed(target, item);
                    }
                };
                parentPage.addEditingVisibleBehavior(delete);
                linkCont.add(delete);

                WebMarkupContainer accountBody = new WebMarkupContainer(ID_ACCOUNT_BODY);
                accountBody.setOutputMarkupId(true);
                accountBody.setMarkupId(createCollapseItemId(item, false).getObject());

                if(changeState != ChangeState.SKIP){
                    accountBody.add(new AttributeModifier("class", new IModel<String>() {

                        @Override
                        public String getObject() {
                            if(changeState == ChangeState.FIRST && item.getIndex() == 0){
                                return "panel-collapse collapse in";
                            } else if(changeState == ChangeState.LAST && item.getIndex() == (getModelObject().size()-1)){
                                return "panel-collapse collapse in";
                            } else {
                                return "panel-collapse collapse";
                            }
                        }
                    }));
                }

                item.add(accountBody);

                //TODO - maybe add some validator and auto-complete functionality?
                TextField name = new TextField<>(ID_NAME, new PropertyModel<String>(item.getModel(), "name"));
                name.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
                parentPage.addEditingEnabledBehavior(name);
                accountBody.add(name);

                //TODO - maybe add some validator and auto-complete functionality?
                TextField uid = new TextField<>(ID_UID, new PropertyModel<String>(item.getModel(), "uid"));
                uid.add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
                parentPage.addEditingEnabledBehavior(uid);
                accountBody.add(uid);

                SearchFilterPanel searchFilterPanel = new SearchFilterPanel<>(ID_FILTER_EDITOR,
                    new NonEmptyPropertyModel<>(item.getModel(), "filter"), parentPage.getReadOnlyModel());
                accountBody.add(searchFilterPanel);

                Label nameTooltip = new Label(ID_T_NAME);
                nameTooltip.add(new InfoTooltipBehavior());
                accountBody.add(nameTooltip);

                Label uidTooltip = new Label(ID_T_UID);
                uidTooltip.add(new InfoTooltipBehavior());
                accountBody.add(uidTooltip);

                Label filterTooltip = new Label(ID_T_FILTER);
                filterTooltip.add(new InfoTooltipBehavior());
                accountBody.add(filterTooltip);
            }
        };
        repeater.setOutputMarkupId(true);
        container.add(repeater);

        AjaxLink<Void> add = new AjaxLink<Void>(ID_BUTTON_ADD) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                addProtectedAccountPerformed(target);
            }
        };
        parentPage.addEditingVisibleBehavior(add);
        add(add);
    }

    private WebMarkupContainer getMainContainer(){
        return (WebMarkupContainer) get(ID_CONTAINER);
    }

    private IModel<String> createCollapseItemId(final ListItem<ResourceObjectPatternType> item,final boolean includeSelector){
        return new IModel<String>() {

            @Override
            public String getObject() {
                StringBuilder sb = new StringBuilder();

                if(includeSelector){
                    sb.append("#");
                }

                sb.append("collapse").append(item.getId());

                return sb.toString();
            }
        };
    }

    private void addProtectedAccountPerformed(AjaxRequestTarget target){
        ResourceObjectPatternType account = new ResourceObjectPatternType();
        account.setFilter(new SearchFilterType());
        changeState = ChangeState.LAST;
        getModel().getObject().add(account);
        target.add(getMainContainer());
    }

    private void deleteProtectedAccountPerformed(AjaxRequestTarget target, ListItem<ResourceObjectPatternType> item){
        changeState = ChangeState.SKIP;
        getModel().getObject().remove(item.getModelObject());
        target.add(getMainContainer());
    }
}

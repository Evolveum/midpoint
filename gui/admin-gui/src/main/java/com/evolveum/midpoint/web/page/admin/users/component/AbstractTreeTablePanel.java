/*
 * Copyright (c) 2010-2015 Evolveum
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

package com.evolveum.midpoint.web.page.admin.users.component;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.tree.ITreeProvider;
import org.apache.wicket.extensions.markup.html.repeater.tree.TableTree;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.match.PolyStringNormMatchingRule;
import com.evolveum.midpoint.prism.polystring.PolyStringNormalizer;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrFilter;
import com.evolveum.midpoint.prism.query.OrgFilter;
import com.evolveum.midpoint.prism.query.SubstringFilter;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.BasicSearchPanel;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.util.LoadableModel;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.web.page.PageBase;
import com.evolveum.midpoint.web.page.admin.users.dto.OrgDto;
import com.evolveum.midpoint.web.page.admin.users.dto.OrgTreeDto;
import com.evolveum.midpoint.web.util.StringResourceChoiceRenderer;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

/**
 * Common superclass for TreeTablePanel and OrgTreeTablePanel
 *
 * @author semancik
 */
public abstract class AbstractTreeTablePanel extends SimplePanel<String> {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractTreeTablePanel.class);

    protected static final int CONFIRM_DELETE = 0;
    protected static final int CONFIRM_DELETE_ROOT = 1;

    protected static final String DOT_CLASS = AbstractTreeTablePanel.class.getName() + ".";
    protected static final String OPERATION_DELETE_OBJECTS = DOT_CLASS + "deleteObjects";
    protected static final String OPERATION_DELETE_OBJECT = DOT_CLASS + "deleteObject";
    protected static final String OPERATION_MOVE_OBJECTS = DOT_CLASS + "moveObjects";
    protected static final String OPERATION_MOVE_OBJECT = DOT_CLASS + "moveObject";
    protected static final String OPERATION_UPDATE_OBJECTS = DOT_CLASS + "updateObjects";
    protected static final String OPERATION_UPDATE_OBJECT = DOT_CLASS + "updateObject";
    protected static final String OPERATION_RECOMPUTE = DOT_CLASS + "recompute";

    protected static final String ID_TREE = "tree";
    protected static final String ID_TREE_CONTAINER = "treeContainer";
    protected static final String ID_CONTAINER_CHILD_ORGS = "childOrgContainer";
    protected static final String ID_CONTAINER_MANAGER = "managerContainer";
    protected static final String ID_CONTAINER_MEMBER = "memberContainer";
    protected static final String ID_CHILD_TABLE = "childUnitTable";
    protected static final String ID_MANAGER_TABLE = "managerTable";
    protected static final String ID_MEMBER_TABLE = "memberTable";
    protected static final String ID_FORM = "form";
    protected static final String ID_CONFIRM_DELETE_POPUP = "confirmDeletePopup";
    protected static final String ID_MOVE_POPUP = "movePopup";
    protected static final String ID_ADD_DELETE_POPUP = "addDeletePopup";
    protected static final String ID_TREE_MENU = "treeMenu";
    protected static final String ID_TREE_HEADER = "treeHeader";
    protected static final String ID_SEARCH_FORM = "searchForm";
    protected static final String ID_BASIC_SEARCH = "basicSearch";
    protected static final String ID_SEARCH_SCOPE = "searchScope";

    protected static final String SEARCH_SCOPE_SUBTREE = "subtree";
    protected static final String SEARCH_SCOPE_ONE = "one";
    protected static final List<String> SEARCH_SCOPE_VALUES = Arrays.asList( SEARCH_SCOPE_SUBTREE, SEARCH_SCOPE_ONE);
    
    protected IModel<OrgTreeDto> selected;
    
    public AbstractTreeTablePanel(String id, IModel<String> rootOid) {
        super(id, rootOid);
    }

    protected void initSearch() {
        Form form = new Form(ID_SEARCH_FORM);
        form.setOutputMarkupId(true);
        add(form);
        
        
        DropDownChoice<String> seachScrope = new DropDownChoice<String>(ID_SEARCH_SCOPE, Model.of(SEARCH_SCOPE_SUBTREE),
        		SEARCH_SCOPE_VALUES, new StringResourceChoiceRenderer("TreeTablePanel.search.scope"));
        form.add(seachScrope);

        BasicSearchPanel basicSearch = new BasicSearchPanel(ID_BASIC_SEARCH, new Model()) {

            @Override
            protected void clearSearchPerformed(AjaxRequestTarget target) {
                clearTableSearchPerformed(target);
            }

            @Override
            protected void searchPerformed(AjaxRequestTarget target) {
                tableSearchPerformed(target);
            }
        };
        form.add(basicSearch);
    }


    protected OrgTreeDto getRootFromProvider() {
        TableTree<OrgTreeDto, String> tree = getTree();
        ITreeProvider<OrgTreeDto> provider = tree.getProvider();
        Iterator<? extends OrgTreeDto> iterator = provider.getRoots();

        return iterator.hasNext() ? iterator.next() : null;
    }


    protected PrismReferenceValue createPrismRefValue(OrgDto dto) {
        PrismReferenceValue value = new PrismReferenceValue();
        value.setOid(dto.getOid());
        value.setRelation(dto.getRelation());
        value.setTargetType(ObjectTypes.getObjectType(dto.getType()).getTypeQName());
        return value;
    }


    protected void refreshTabbedPanel(AjaxRequestTarget target) {
        PageBase page = getPageBase();

        TabbedPanel tabbedPanel = findParent(TabbedPanel.class);
        IModel<List<ITab>> tabs = tabbedPanel.getTabs();

        if (tabs instanceof LoadableModel) {
            ((LoadableModel) tabs).reset();
        }

        tabbedPanel.setSelectedTab(0);

        target.add(tabbedPanel);
        target.add(page.getFeedbackPanel());
    }

    protected TableTree<OrgTreeDto, String> getTree() {
        return (TableTree<OrgTreeDto, String>) get(createComponentPath(ID_TREE_CONTAINER, ID_TREE));
    }

    protected WebMarkupContainer getOrgChildContainer() {
        return (WebMarkupContainer) get(createComponentPath(ID_FORM, ID_CONTAINER_CHILD_ORGS));
    }

    protected TablePanel getOrgChildTable() {
        return (TablePanel) get(createComponentPath(ID_FORM, ID_CONTAINER_CHILD_ORGS, ID_CHILD_TABLE));
    }

    protected ObjectQuery createOrgChildQuery() {
        OrgTreeDto dto = selected.getObject();
        String oid = dto != null ? dto.getOid() : getModel().getObject();

        BasicSearchPanel<String> basicSearch = (BasicSearchPanel) get(createComponentPath(ID_SEARCH_FORM, ID_BASIC_SEARCH));
        String object = basicSearch.getModelObject();

        DropDownChoice<String> searchScopeChoice = (DropDownChoice) get(createComponentPath(ID_SEARCH_FORM, ID_SEARCH_SCOPE));
        String scope = searchScopeChoice.getModelObject();

        if (StringUtils.isBlank(object)) {
        	object = null;
        }
        
        OrgFilter org;
        if (object == null || SEARCH_SCOPE_ONE.equals(scope)) {
        	org = OrgFilter.createOrg(oid, OrgFilter.Scope.ONE_LEVEL);
        } else {
        	org = OrgFilter.createOrg(oid, OrgFilter.Scope.SUBTREE);
        }

        if (object == null) {
            return ObjectQuery.createObjectQuery(org);
        }
        
        PageBase page = getPageBase();
        PrismContext context = page.getPrismContext();

        PolyStringNormalizer normalizer = context.getDefaultPolyStringNormalizer();
        String normalizedString = normalizer.normalize(object);
        if (StringUtils.isEmpty(normalizedString)) {
            return ObjectQuery.createObjectQuery(org);
        }

        SubstringFilter substringName =  SubstringFilter.createSubstring(OrgType.F_NAME, OrgType.class, context,
                PolyStringNormMatchingRule.NAME, normalizedString);
        
        SubstringFilter substringDisplayName =  SubstringFilter.createSubstring(OrgType.F_DISPLAY_NAME, OrgType.class, context,
                PolyStringNormMatchingRule.NAME, normalizedString);
        OrFilter orName = OrFilter.createOr(substringName, substringDisplayName);
        AndFilter and = AndFilter.createAnd(org, orName);
        ObjectQuery query = ObjectQuery.createObjectQuery(and);
        
        if(LOGGER.isTraceEnabled()){
            LOGGER.trace("Searching child orgs of org {} with query:\n{}", oid, query.debugDump());
        }

        return query;
    }

    protected abstract void refreshTable(AjaxRequestTarget target);
    
    protected void clearTableSearchPerformed(AjaxRequestTarget target) {
        BasicSearchPanel basicSearch = (BasicSearchPanel) get(createComponentPath(ID_SEARCH_FORM, ID_BASIC_SEARCH));
        basicSearch.getModel().setObject(null);

        refreshTable(target);
    }

    protected void tableSearchPerformed(AjaxRequestTarget target) {
        refreshTable(target);
    }
}

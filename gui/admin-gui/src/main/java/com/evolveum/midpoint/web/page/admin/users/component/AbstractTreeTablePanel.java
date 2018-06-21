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

import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.query.builder.S_AtomicFilterExit;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.tree.ITreeProvider;
import org.apache.wicket.extensions.markup.html.repeater.tree.TableTree;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.polystring.PolyStringNormalizer;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.BasicSearchPanel;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.component.data.TablePanel;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

/**
 * Common superclass for TreeTablePanel and OrgTreeTablePanel
 *
 * @author semancik
 */
public abstract class AbstractTreeTablePanel extends BasePanel<String> {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractTreeTablePanel.class);

    protected static final int CONFIRM_DELETE = 0;
    protected static final int CONFIRM_DELETE_ROOT = 1;
    protected static final int CONFIRM_DELETE_MANAGER = 2;
    protected static final int CONFIRM_DELETE_MEMBER = 3;

    protected static final String DOT_CLASS = AbstractTreeTablePanel.class.getName() + ".";
    protected static final String OPERATION_DELETE_OBJECTS = DOT_CLASS + "deleteObjects";
    protected static final String OPERATION_DELETE_OBJECT = DOT_CLASS + "deleteObject";
    protected static final String OPERATION_MOVE_OBJECTS = DOT_CLASS + "moveObjects";
    protected static final String OPERATION_MOVE_OBJECT = DOT_CLASS + "moveObject";
    protected static final String OPERATION_UPDATE_OBJECTS = DOT_CLASS + "updateObjects";
    protected static final String OPERATION_UPDATE_OBJECT = DOT_CLASS + "updateObject";
    protected static final String OPERATION_RECOMPUTE = DOT_CLASS + "recompute";
    protected static final String OPERATION_SEARCH_MANAGERS = DOT_CLASS + "searchManagers";

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
    protected static final String ID_TREE_TITLE = "treeTitle";
    protected static final String ID_SEARCH_FORM = "searchForm";
    protected static final String ID_BASIC_SEARCH = "basicSearch";
    protected static final String ID_SEARCH_SCOPE = "searchScope";
    protected static final String ID_SEARCH_BY_TYPE = "searchByType";

    protected static final String SEARCH_SCOPE_SUBTREE = "subtree";
    protected static final String SEARCH_SCOPE_ONE = "one";

    protected static final ObjectTypes OBJECT_TYPES_DEFAULT = ObjectTypes.OBJECT;

    protected static final List<String> SEARCH_SCOPE_VALUES = Arrays.asList( SEARCH_SCOPE_SUBTREE, SEARCH_SCOPE_ONE);

    protected IModel<SelectableBean<OrgType>> selected;

    public AbstractTreeTablePanel(String id, IModel<String> rootOid) {
        super(id, rootOid);
    }


    protected SelectableBean<OrgType> getRootFromProvider() {
        TableTree<SelectableBean<OrgType>, String> tree = getTree();
        ITreeProvider<SelectableBean<OrgType>> provider = tree.getProvider();
        Iterator<? extends SelectableBean<OrgType>> iterator = provider.getRoots();

        return iterator.hasNext() ? iterator.next() : null;
    }

    protected void refreshTabbedPanel(AjaxRequestTarget target) {
        PageBase page = getPageBase();

        TabbedPanel tabbedPanel = findParent(TabbedPanel.class);
        IModel<List<ITab>> tabs = tabbedPanel.getTabs();

        if (tabs instanceof LoadableModel) {
            ((LoadableModel) tabs).reset();
        }

        if (tabs.getObject() != null && tabs.getObject().size() > 0) {
            tabbedPanel.setSelectedTab(0);
        }

        target.add(tabbedPanel);
        target.add(page.getFeedbackPanel());
    }

    protected TableTree<SelectableBean<OrgType>, String> getTree() {
        return (TableTree<SelectableBean<OrgType>, String>) get(createComponentPath(ID_TREE_CONTAINER, ID_TREE));
    }

    protected WebMarkupContainer getOrgChildContainer() {
        return (WebMarkupContainer) get(createComponentPath(ID_FORM, ID_CONTAINER_CHILD_ORGS));
    }

    protected TablePanel getOrgChildTable() {
        return (TablePanel) get(createComponentPath(ID_FORM, ID_CONTAINER_CHILD_ORGS, ID_CHILD_TABLE));
    }

    protected ObjectQuery createOrgChildQuery() {
    	SelectableBean<OrgType> dto = selected.getObject();
        String oid = dto != null && dto.getValue() != null ? dto.getValue().getOid() : getModel().getObject();

        BasicSearchPanel<String> basicSearch = (BasicSearchPanel) get(createComponentPath(ID_SEARCH_FORM, ID_BASIC_SEARCH));
        String object = basicSearch.getModelObject();

        DropDownChoice<String> searchScopeChoice = (DropDownChoice) get(createComponentPath(ID_SEARCH_FORM, ID_SEARCH_SCOPE));
        String scope = searchScopeChoice.getModelObject();

        if (StringUtils.isBlank(object)) {
        	object = null;
        }

        PageBase page = getPageBase();
        PrismContext context = page.getPrismContext();

        S_AtomicFilterExit q;
        if (object == null || SEARCH_SCOPE_ONE.equals(scope)) {
            q = QueryBuilder.queryFor(OrgType.class, context)
                    .isDirectChildOf(oid);
        } else {
            q = QueryBuilder.queryFor(OrgType.class, context)
                    .isChildOf(oid);
        }

        if (object == null) {
            return q.build();
        }

        PolyStringNormalizer normalizer = context.getDefaultPolyStringNormalizer();
        String normalizedString = normalizer.normalize(object);
        if (StringUtils.isEmpty(normalizedString)) {
            return q.build();
        }

        ObjectQuery query = q.and().block()
                .item(OrgType.F_NAME).containsPoly(normalizedString).matchingNorm()
                .or().item(OrgType.F_DISPLAY_NAME).containsPoly(normalizedString).matchingNorm()
                .build();

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Searching child orgs of org {} with query:\n{}", oid, query.debugDump());
        }
        return query;
    }

}

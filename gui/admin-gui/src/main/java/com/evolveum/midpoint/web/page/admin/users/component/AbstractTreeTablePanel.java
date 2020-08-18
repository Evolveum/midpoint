/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.users.component;

import java.util.Iterator;
import java.util.List;

import com.evolveum.midpoint.prism.query.builder.S_AtomicFilterExit;
import com.evolveum.midpoint.web.component.util.TreeSelectableBean;
import com.evolveum.midpoint.web.page.admin.orgs.MidpointNestedTree;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.tree.ITreeProvider;
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
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

/**
 * Common superclass for TreeTablePanel and OrgTreeTablePanel
 *
 * @author semancik
 */
public abstract class AbstractTreeTablePanel extends BasePanel<String> {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractTreeTablePanel.class);

    protected static final String DOT_CLASS = AbstractTreeTablePanel.class.getName() + ".";

    protected static final String ID_TREE = "tree";
    protected static final String ID_TREE_CONTAINER = "treeContainer";
    protected static final String ID_CONTAINER_CHILD_ORGS = "childOrgContainer";
    protected static final String ID_CHILD_TABLE = "childUnitTable";
    protected static final String ID_FORM = "form";
    protected static final String ID_TREE_MENU = "treeMenu";
    protected static final String ID_TREE_HEADER = "treeHeader";
    protected static final String ID_TREE_TITLE = "treeTitle";
    protected static final String ID_SEARCH_FORM = "searchForm";
    protected static final String ID_BASIC_SEARCH = "basicSearch";
    protected static final String ID_SEARCH_SCOPE = "searchScope";

    protected static final String SEARCH_SCOPE_SUBTREE = "subtree";
    protected static final String SEARCH_SCOPE_ONE = "one";

    protected IModel<TreeSelectableBean<OrgType>> selected;

    public AbstractTreeTablePanel(String id, IModel<String> rootOid) {
        super(id, rootOid);
    }


    protected TreeSelectableBean<OrgType> getRootFromProvider() {
        MidpointNestedTree tree = getTree();
        ITreeProvider<TreeSelectableBean<OrgType>> provider = tree.getProvider();
        Iterator<? extends TreeSelectableBean<OrgType>> iterator = provider.getRoots();

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

    protected MidpointNestedTree getTree() {
        return (MidpointNestedTree) get(createComponentPath(ID_TREE_CONTAINER, ID_TREE));
    }

    protected WebMarkupContainer getOrgChildContainer() {
        return (WebMarkupContainer) get(createComponentPath(ID_FORM, ID_CONTAINER_CHILD_ORGS));
    }

    protected TablePanel getOrgChildTable() {
        return (TablePanel) get(createComponentPath(ID_FORM, ID_CONTAINER_CHILD_ORGS, ID_CHILD_TABLE));
    }

    protected ObjectQuery createOrgChildQuery() {
        SelectableBeanImpl<OrgType> dto = selected.getObject();
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
            q = context.queryFor(OrgType.class)
                    .isDirectChildOf(oid);
        } else {
            q = context.queryFor(OrgType.class)
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

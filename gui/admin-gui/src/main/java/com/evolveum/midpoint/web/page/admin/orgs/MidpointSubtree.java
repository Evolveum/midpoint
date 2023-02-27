/*
 * Copyright (c) 2015-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.orgs;

import java.util.Iterator;

import com.evolveum.midpoint.gui.impl.component.data.provider.OrgTreeProvider;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.tree.AbstractTree;
import org.apache.wicket.extensions.markup.html.repeater.tree.nested.BranchItem;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.AbstractPageableView;
import org.apache.wicket.markup.repeater.IItemFactory;
import org.apache.wicket.markup.repeater.IItemReuseStrategy;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.web.component.data.paging.NavigatorPanel;
import com.evolveum.midpoint.web.component.util.TreeSelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

public class MidpointSubtree extends Panel {

    private static final long serialVersionUID = 1L;

    private MidpointNestedTree tree;

    /**
     * Create a subtree for the children of the node contained in the given model or the root nodes
     * if the model contains <code>null</code>.
     *
     * @param id    component id
     * @param tree  the containing tree
     * @param model
     */
    public MidpointSubtree(String id, final MidpointNestedTree tree, final IModel<TreeSelectableBean<OrgType>> model) {
        super(id, model);

        if (tree == null) {
            throw new IllegalArgumentException("argument [tree] cannot be null");
        }
        this.tree = tree;

        AbstractPageableView<TreeSelectableBean<OrgType>> branches = new AbstractPageableView<TreeSelectableBean<OrgType>>("branches") {
            private static final long serialVersionUID = 1L;

            @Override
            protected Iterator<IModel<TreeSelectableBean<OrgType>>> getItemModels(long offset, long size) {
                return new ModelIterator(offset, size);
            }


            @Override
            protected long internalGetItemCount() {
                OrgTreeProvider orgTreeProvider = (OrgTreeProvider) tree.getProvider();
                return orgTreeProvider.size(getModelObject());
            }

            @Override
            protected Item<TreeSelectableBean<OrgType>> newItem(String id, int index, IModel<TreeSelectableBean<OrgType>> model) {
                return newBranchItem(id, index, model);
            }

            @Override
            protected void populateItem(Item<TreeSelectableBean<OrgType>> item) {
                IModel<TreeSelectableBean<OrgType>> model = item.getModel();

                Component node = tree.newNodeComponent("node", model);
                item.add(node);

                item.add(tree.newSubtree("subtree", model));
            }


            @Override
            public long getItemsPerPage() {
                return 20;
            }

            @Override
            public long getViewSize() {
                return 20;
            }
        };
        branches.setItemReuseStrategy(new IItemReuseStrategy() {
            private static final long serialVersionUID = 1L;

            @Override
            public <S> Iterator<Item<S>> getItems(IItemFactory<S> factory,
                                                  Iterator<IModel<S>> newModels, Iterator<Item<S>> existingItems) {
                return tree.getItemReuseStrategy().getItems(factory, newModels, existingItems);
            }
        });
        add(branches);
        branches.setOutputMarkupId(true);

        Panel panel = newPaging("paging", branches);
        panel.setOutputMarkupId(true);
        add(panel);

        setOutputMarkupId(true);
    }

    @SuppressWarnings("unchecked")
    public IModel<TreeSelectableBean<OrgType>> getModel() {
        return (IModel<TreeSelectableBean<OrgType>>) getDefaultModel();
    }

    public TreeSelectableBean<OrgType> getModelObject() {
        return getModel().getObject();
    }

    protected BranchItem<TreeSelectableBean<OrgType>> newBranchItem(String id, int index, IModel<TreeSelectableBean<OrgType>> model) {
        return new BranchItem<>(id, index, model);
    }

    @Override
    public boolean isVisible() {
        TreeSelectableBean<OrgType> t = getModel().getObject();
        if (t == null) {
            // roots always visible
            return true;
        } else {
            return tree.getState(t) == AbstractTree.State.EXPANDED;
        }
    }

    private final class ModelIterator implements Iterator<IModel<TreeSelectableBean<OrgType>>> {
        private Iterator<? extends TreeSelectableBean<OrgType>> children;

        public ModelIterator(long offset, long size) {
            TreeSelectableBean<OrgType> t = getModel().getObject();
            OrgTreeProvider provider = (OrgTreeProvider) tree.getProvider();
            provider.setOffset(offset);
            provider.setCount(size);
            if (t == null) {
                children = tree.getProvider().getRoots();
            } else {
                children = tree.getProvider().getChildren(t);
            }
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean hasNext() {
            return children.hasNext();
        }

        @Override
        public IModel<TreeSelectableBean<OrgType>> next() {
            return tree.getProvider().model(children.next());
        }
    }

    private Panel newPaging(String id, AbstractPageableView view) {
        NavigatorPanel paging = new NavigatorPanel(id, view, true) {

            @Override
            protected void onPageChanged(AjaxRequestTarget target, long page) {
                view.setCurrentPage(page);
                target.add(MidpointSubtree.this);
            }
        };

        paging.add(new VisibleBehaviour(() -> getModelObject() != null && view.getItemCount() > 20));

        return paging;
    }


}



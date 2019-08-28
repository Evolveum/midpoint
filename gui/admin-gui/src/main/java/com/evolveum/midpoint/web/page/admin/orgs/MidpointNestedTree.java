package com.evolveum.midpoint.web.page.admin.orgs;

import com.evolveum.midpoint.web.component.data.BoxedPagingPanel;
import com.evolveum.midpoint.web.component.util.TreeSelectableBean;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import org.antlr.runtime.tree.Tree;
import org.apache.wicket.Component;
import org.apache.wicket.extensions.markup.html.repeater.tree.ITreeProvider;
import org.apache.wicket.extensions.markup.html.repeater.tree.NestedTree;
import org.apache.wicket.markup.html.navigation.paging.IPageable;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import java.util.Set;

public class MidpointNestedTree extends NestedTree<TreeSelectableBean<OrgType>> {


    public MidpointNestedTree(String id, ITreeProvider<TreeSelectableBean<OrgType>> provider) {
        super(id, provider);
    }

    public MidpointNestedTree(String id, ITreeProvider<TreeSelectableBean<OrgType>> provider, IModel<? extends Set<TreeSelectableBean<OrgType>>> state) {
        super(id, provider, state);

    }

    @Override
    protected Component newContentComponent(String id, IModel<TreeSelectableBean<OrgType>> model) {
        return null;
    }

    @Override
    public Component newSubtree(String id, IModel<TreeSelectableBean<OrgType>> model) {
        return new MidpointSubtree(id, this, model);
    }

}

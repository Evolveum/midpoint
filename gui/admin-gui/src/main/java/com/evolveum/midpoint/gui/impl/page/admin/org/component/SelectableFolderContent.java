/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.org.component;

import java.util.Optional;

import com.evolveum.midpoint.web.component.util.TreeSelectableBean;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.tree.AbstractTree;
import org.apache.wicket.extensions.markup.html.repeater.tree.content.Folder;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.util.SelectableBeanImpl;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

/**
 * @author lazyman
 */
public class SelectableFolderContent extends Folder<TreeSelectableBean<OrgType>> {
    private static final long serialVersionUID = 1L;

    private AbstractTree tree;
    private IModel<TreeSelectableBean<OrgType>> selected;

    public SelectableFolderContent(String id, AbstractTree<TreeSelectableBean<OrgType>> tree, IModel<TreeSelectableBean<OrgType>> model,
                                   IModel<TreeSelectableBean<OrgType>> selected) {
        super(id, tree, model);

        this.tree = tree;
        this.selected = selected;
    }

    @Override
    protected IModel<?> newLabelModel(final IModel<TreeSelectableBean<OrgType>> model) {
        return new IModel<String>() {
            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                TreeSelectableBean<OrgType> dto = model.getObject();
                return WebComponentUtil.getEffectiveName(dto.getValue(), OrgType.F_DISPLAY_NAME);

            }
        };
    }

    @Override
    protected void onClick(Optional<AjaxRequestTarget> optionalTarget) {
        if (!optionalTarget.isPresent()) {
            return;
        }

        if (selected.getObject() != null) {
            tree.updateNode(selected.getObject(), optionalTarget.get());
        }

        TreeSelectableBean<OrgType> dto = getModelObject();
        selected.setObject(dto);
        tree.updateNode(dto, optionalTarget.get());
    }

    @Override
    protected boolean isClickable() {
        return true;
    }

    @Override
    protected boolean isSelected() {
        SelectableBeanImpl<OrgType> dto = getModelObject();
        return dto.equals(selected.getObject());
    }

    @Override
    protected String getSelectedStyleClass() {
        return null;
    }
}

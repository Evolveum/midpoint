/*
 * Copyright (c) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.markup.html.repeater.tree.AbstractTree;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;

/**
 * Created by Viliam Repan (lazyman).
 */
public class MyTreeColumnPanel<T> extends BasePanel<T> {

    private static final long serialVersionUID = 1L;

    private static final String ID_LINK = "link";
    private static final String ID_CHEVRON = "chevron";

    private final AbstractTree<T> tree;

    public MyTreeColumnPanel(@NotNull String id, @NotNull AbstractTree<T> tree, @NotNull IModel<T> model) {
        super(id, model);

        this.tree = tree;

        initLayout();
    }

    private void initLayout() {
        AjaxLink<T> link = new AjaxLink<>(ID_LINK) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                onClickPerformed(target);
            }
        };
        add(link);

        WebComponent chevron = new WebComponent(ID_CHEVRON);
        chevron.add(AttributeModifier.append("class", () -> getChevronCss(getModel())));
        link.add(chevron);
    }

    protected String getChevronCss(IModel<T> model) {
        T t = model.getObject();

        String cssClass = "";
        if (tree.getProvider().hasChildren(t)) {
            if (tree.getState(t) == AbstractTree.State.EXPANDED) {
                cssClass = "fa fa-chevron-down";
            } else {
                cssClass = "fa fa-chevron-right";
            }
        } else {
            cssClass = "fa fa-chevron-right invisible";
        }

        if (isSelected()) {
            cssClass += " active";
        }

        return cssClass;
    }

    protected boolean isSelected() {
        return false;
    }

    protected void onClickPerformed(AjaxRequestTarget target) {
        T t = getModelObject();

        if (tree.getState(t) == AbstractTree.State.EXPANDED) {
            tree.collapse(t);
        } else {
            tree.expand(t);
        }
    }
}

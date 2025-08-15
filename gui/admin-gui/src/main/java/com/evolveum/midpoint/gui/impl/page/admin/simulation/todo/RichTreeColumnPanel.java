/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation.todo;

import com.evolveum.midpoint.gui.impl.component.data.column.icon.RoundedImagePanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;

import org.apache.wicket.extensions.markup.html.repeater.tree.AbstractTree;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.IResource;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Viliam Repan (lazyman).
 */
public class RichTreeColumnPanel<T> extends MyTreeColumnPanel<T> {

    private static final long serialVersionUID = 1L;

    private static final String ID_ICON = "icon";
    private static final String ID_LABEL = "label";
    private static final String ID_BADGE = "badge";

    public RichTreeColumnPanel(@NotNull String id, @NotNull AbstractTree<T> tree, @NotNull IModel<T> model) {
        super(id, tree, model);

        initLayout();
    }

    private void initLayout() {
        IModel<DisplayType> iconModel = getIconModel(getModel());
        IModel<IResource> imageModel = getImageResource(getModel());

        RoundedImagePanel icon = new RoundedImagePanel(ID_ICON, iconModel, imageModel);
        icon.add(new VisibleBehaviour(() -> iconModel.getObject() != null || imageModel.isPresent().getObject()));
        add(icon);

        Label label = new Label(ID_LABEL, getModel());
        add(label);

        Label badge = new Label(ID_BADGE, getModel());
        add(badge);
    }

    protected @NotNull IModel<DisplayType> getIconModel(@NotNull IModel<T> model) {
        return Model.of((DisplayType) null);
    }

    protected @NotNull IModel<IResource> getImageResource(@NotNull IModel<T> model) {
        return Model.of((IResource) null);
    }
}

/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.tile;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.IResource;

/**
 * @author lskublik
 */
public class SelectableFocusTilePanel<O extends ObjectType> extends FocusTilePanel<SelectableBean<O>, TemplateTile<SelectableBean<O>>> {

    public SelectableFocusTilePanel(String id, IModel<TemplateTile<SelectableBean<O>>> model) {
        super(id, model);
    }

    @Override
    protected void initLayout() {
        super.initLayout();

        add(new AjaxEventBehavior("click") {
            @Override
            protected void onEvent(AjaxRequestTarget target) {
                onClick(target);
            }
        });

        add(AttributeAppender.append("class", "card catalog-tile-panel d-flex flex-column align-items-center bordered p-3 h-100 mb-0 selectable"));
        add(AttributeAppender.append("class", () -> getModelObject().isSelected() ? "active" : null));
    }

    @Override
    protected Behavior createDetailsBehaviour() {
        return VisibleBehaviour.ALWAYS_INVISIBLE;
    }

    @Override
    protected IModel<IResource> createPreferredImage(IModel<TemplateTile<SelectableBean<O>>> model) {
        return new LoadableModel<>(false) {
            @Override
            protected IResource load() {
                O object = model.getObject().getValue().getValue();
                if (object instanceof FocusType) {
                    return WebComponentUtil.createJpegPhotoResource((FocusType) object);
                }
                return null;
            }
        };
    }
}

/*
 * Copyright (c) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.abstractrole.component;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.tile.MemberTilePanel;
import com.evolveum.midpoint.gui.impl.page.admin.resource.component.TemplateTile;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DisplayType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RelationDefinitionType;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

import javax.xml.namespace.QName;

public class RelationTilePanel extends MemberTilePanel<QName> {

    private static final long serialVersionUID = 1L;

    public RelationTilePanel(String id, QName relation) {
        super(id, createTileModel(relation));
    }

    private static IModel<TemplateTile<QName>> createTileModel(QName relation) {

        return new LoadableDetachableModel<>() {
            @Override
            protected TemplateTile<QName> load() {
                RelationDefinitionType definition = WebComponentUtil.getRelationDefinition(relation);
                QName relation = definition.getRef();

                DisplayType display = definition.getDisplay();

                String icon = GuiDisplayTypeUtil.getIconCssClass(display);
                PolyStringType label = GuiDisplayTypeUtil.getLabel(display);

                String title;
                if (label == null) {
                    title = relation.getLocalPart();
                } else {
                    title = LocalizationUtil.translatePolyString(label);
                }

                TemplateTile<QName> tile = new TemplateTile<>(icon, title, relation)
                        .description(GuiDisplayTypeUtil.getHelp(display))
                        .addTag(display);

                return tile;
            }
        };
    }

    protected void initLayout() {
        super.initLayout();

        getTitle().add(VisibleBehaviour.ALWAYS_INVISIBLE);
        getIcon().add(VisibleBehaviour.ALWAYS_INVISIBLE);
    }

    protected void onUnassign(AjaxRequestTarget target) {
        onChoose(getModelObject().getValue(), target);
    }

    protected void onChoose(QName relation, AjaxRequestTarget target) {
    }

    protected Behavior createDetailsBehaviour() {
        return VisibleBehaviour.ALWAYS_INVISIBLE;
    }

    @Override
    protected DisplayType createDisplayType(IModel<TemplateTile<QName>> model) {
        DisplayType display = model.getObject().getTags().iterator().next().clone();
        if (display.getIcon() != null && StringUtils.isNotEmpty(display.getIcon().getCssClass())) {
            display.getIcon().setCssClass(display.getIcon().getCssClass() + " fa-2x");
        }
        return display;
    }

    protected String getCssForUnassignButton() {
        return "btn btn-outline-primary mt-3 mx-auto";
    }

    @Override
    protected boolean isSelectable() {
        return false;
    }
}

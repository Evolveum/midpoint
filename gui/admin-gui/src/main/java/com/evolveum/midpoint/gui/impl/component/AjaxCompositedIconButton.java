/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component;

import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.parser.XmlTag;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;
import com.evolveum.midpoint.gui.impl.component.icon.LayerIcon;
import com.evolveum.midpoint.web.component.CompositedIconButtonDto;

/**
 * @author Viliam Repan (lazyman)
 * @author skublik
 */
public abstract class AjaxCompositedIconButton extends AjaxLink<String> {

    private static final long serialVersionUID = 1L;

    private IModel<String> title;
    private CompositedIcon icon;

    private IModel<CompositedIconButtonDto> buttonModel;

    private boolean titleAsLabel;

    public AjaxCompositedIconButton(String id, IModel<CompositedIconButtonDto> buttonModel) {
        this(id, null, null, buttonModel);
    }

    public AjaxCompositedIconButton(String id, CompositedIcon icon, IModel<String> title) {
        this(id, icon, title, null);
    }

    public AjaxCompositedIconButton(String id, CompositedIcon icon, IModel<String> title, IModel<CompositedIconButtonDto> buttonModel) {
        super(id);

        this.title = title;
        this.icon = icon;
        this.buttonModel = buttonModel;

        add(AttributeAppender.append("class", () -> "position-relative"));
        add(AttributeAppender.append("class", () -> !AjaxCompositedIconButton.this.isEnabled() ? "disabled" : ""));
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        add(AttributeModifier.replace("title", () -> {
            if (title != null) {
                return title.getObject();
            }

            return isNotEmptyModel() ? GuiDisplayTypeUtil.getDisplayTypeTitle(buttonModel.getObject().getAdditionalButtonDisplayType()) : "";
        }));
    }

    private boolean isNotEmptyModel() {
        if (buttonModel == null) {
            return false;
        }
        return buttonModel.getObject() != null;
    }

    @Override
    public void onComponentTagBody(final MarkupStream markupStream, final ComponentTag openTag) {
        StringBuilder sb = new StringBuilder();

        CompositedIcon icon = this.icon;
        if (icon == null) {
            icon = buttonModel.getObject().getCompositedIcon();
        }
        if (icon.hasBasicIcon()) {
            String margin = titleAsLabel ? "mr-1" : "";
            sb.append("<i class=\"" + margin + " ").append(icon.getBasicIcon()).append("\"");
            if (icon.hasBasicIconHtmlColor()) {
                sb.append(" style=\"color: " + icon.getBasicIconHtmlColor() + ";\"");
            }
            sb.append("></i> ");

            if (titleAsLabel) {
                sb.append(title.getObject());
            }
        }

        if (icon.hasLayerIcons()) {
            for (LayerIcon entry : icon.getLayerIcons()) {
                if (entry == null) {
                    continue;
                }
                if (StringUtils.isNotEmpty(entry.getIconType().getCssClass())) {
                    sb.append("<i class=\"").append(entry.getIconType().getCssClass()).append("\"");
                    if (StringUtils.isNotEmpty(entry.getIconType().getColor())) {
                        sb.append(" style=\"color: ").append(entry.getIconType().getColor()).append(";\"");
                    }
                    sb.append(">").append(entry.hasLabel() ? entry.getLabelModel().getObject() : "").append("</i> ");
                }
            }
        }

        replaceComponentTagBody(markupStream, openTag, sb.toString());
    }

    @Override
    protected void onComponentTag(ComponentTag tag) {
        super.onComponentTag(tag);

        if (tag.isOpenClose()) {
            tag.setType(XmlTag.TagType.OPEN);
        }
    }

    public void titleAsLabel(boolean titleAsLabel) {
        this.titleAsLabel = titleAsLabel;
    }
}

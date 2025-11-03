/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component;

import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;

import com.evolveum.midpoint.xml.ns._public.common.common_3.IconType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.parser.XmlTag;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.impl.component.icon.CompositedIcon;
import com.evolveum.midpoint.gui.impl.component.icon.LayerIcon;

import org.apache.wicket.util.string.Strings;

/**
 * @author skublik
 */
public abstract class AjaxCompositedIconSubmitButton extends AjaxSubmitLink {

    private IModel<String> title;
    private CompositedIcon icon;

    private boolean titleAsLabel;

    public AjaxCompositedIconSubmitButton(String id, CompositedIcon icon, IModel<String> title) {
        super(id);

        this.title = title;
        this.icon = icon;

        add(AttributeAppender.append("class", new IModel<String>() {

            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                return " position-relative ";
            }
        }));

        add(AttributeAppender.append("class", new IModel<String>() {

            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                return !AjaxCompositedIconSubmitButton.this.isEnabled() ? "disabled" : "";
            }
        }));
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        if (getTitle() != null && !titleAsLabel) {
            add(AttributeModifier.replace("title", getTitle()));
        }
    }

    @Override
    public void onComponentTagBody(final MarkupStream markupStream, final ComponentTag openTag) {
        StringBuilder sb = new StringBuilder();

        CompositedIcon icon = getIcon();
        if (icon.hasBasicIcon()) {
            String css = icon.getBasicIcon() != null ? icon.getBasicIcon().trim() : "";

            String margin = titleAsLabel ? "mr-1" : "";
            sb.append("<i class=\"" + margin + " ").append(escapeMarkup(css)).append("\"");
            if (icon.hasBasicIconHtmlColor()) {
                sb.append(" style=\"color: " + escapeMarkup(icon.getBasicIconHtmlColor()) + ";\"");
            }
            sb.append("></i> ");

            if (titleAsLabel) {
                sb.append(getTitle().getObject());
            }
        }

        if (icon.hasLayerIcons()) {
            for (LayerIcon entry : icon.getLayerIcons()) {
                if (entry == null) {
                    continue;
                }
                IconType i = entry.getIconType();

                if (StringUtils.isEmpty(i.getCssClass())) {
                    continue;
                }

                sb.append("<i class=\"").append(escapeMarkup(i.getCssClass())).append("\"");
                if (StringUtils.isNotEmpty(i.getColor())) {
                    sb.append(" style=\"color: ")
                            .append(escapeMarkup(GuiDisplayTypeUtil.removeStringAfterSemicolon(i.getColor())))
                            .append(";\"");
                }
                sb.append(">").append(entry.hasLabel() ? escapeMarkup(entry.getLabelModel().getObject()) : "").append("</i> ");
            }
        }

        replaceComponentTagBody(markupStream, openTag, sb.toString());
    }

    private CharSequence escapeMarkup(String markup) {
        return markup != null ? Strings.escapeMarkup(markup) : null;
    }

    @Override
    protected void onComponentTag(ComponentTag tag) {
        super.onComponentTag(tag);

        if (tag.isOpenClose()) {
            tag.setType(XmlTag.TagType.OPEN);
        }
    }

    public AjaxCompositedIconSubmitButton titleAsLabel(boolean titleAsLabel) {
        this.titleAsLabel = titleAsLabel;
        return this;
    }

    public CompositedIcon getIcon() {
        return this.icon;
    }

    public IModel<String> getTitle() {
        return title;
    }
}

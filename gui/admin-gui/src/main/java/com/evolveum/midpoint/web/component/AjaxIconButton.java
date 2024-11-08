/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.parser.XmlTag;
import org.apache.wicket.model.IModel;

/**
 * @author Viliam Repan (lazyman)
 */
public abstract class AjaxIconButton extends AjaxLink<String> {

    private static final long serialVersionUID = 1L;

    private final IModel<String> title;

    private boolean showTitleAsLabel;

    public AjaxIconButton(String id, IModel<String> icon, IModel<String> title) {
        super(id, icon);

        this.title = title;

        add(AttributeAppender.append("class", new IModel<String>() {

            private static final long serialVersionUID = 1L;

            @Override
            public String getObject() {
                return !AjaxIconButton.this.isEnabled() ? "disabled" : "";
            }
        }));
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();

        if (title != null && !showTitleAsLabel) {
            add(AttributeModifier.replace("title", title));
        }
    }

    @Override
    public void onComponentTagBody(final MarkupStream markupStream, final ComponentTag openTag) {
        StringBuilder sb = new StringBuilder();

        String title = this.title != null ? this.title.getObject() : "";

        String icon = getModelObject();
        if (StringUtils.isNotEmpty(icon)) {
            String margin = showTitleAsLabel ? "mr-1" : "";
            sb.append("<i class=\"" + margin + " ").append(icon).append("\"></i>");
        }

        if (StringUtils.isEmpty(icon)) {
            sb.append(title);
        } else {
            if (showTitleAsLabel) {
                sb.append(" " + title);
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

    public AjaxIconButton showTitleAsLabel(boolean show) {
        showTitleAsLabel = show;

        return this;
    }

    public IModel<String> getTitle() {
        return title;
    }
}

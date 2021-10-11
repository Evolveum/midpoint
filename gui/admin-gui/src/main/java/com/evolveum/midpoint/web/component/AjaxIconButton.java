/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.parser.XmlTag;
import org.apache.wicket.model.IModel;

/**
 * @author Viliam Repan (lazyman)
 */
public abstract class AjaxIconButton extends AjaxLink<String> {

    private static final long serialVersionUID = 1L;

    private IModel<String> title;

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

        if (title != null) {
            add(AttributeModifier.replace("title", title));
        }
    }

    @Override
    public void onComponentTagBody(final MarkupStream markupStream, final ComponentTag openTag) {
        StringBuilder sb = new StringBuilder();

        String title = this.title.getObject();

        String icon = getModelObject();
        if (StringUtils.isNotEmpty(icon)) {
            sb.append("<i class=\"").append(icon).append("\"");
            if (showTitleAsLabel && StringUtils.isNotEmpty(title)) {
                sb.append(" style=\"margin-right: 5px;\"");
            }
            sb.append("></i>");
        }

        if (StringUtils.isEmpty(icon)) {
            sb.append(title);
        } else {
            if (showTitleAsLabel) {
                sb.append(title);
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
}

/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.component;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxChannel;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.parser.XmlTag;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.string.Strings;

/**
 * @author lazyman
 */
public abstract class AjaxButton extends AjaxLink<String> {

    public AjaxButton(String id) {
        super(id);
    }

    public AjaxButton(String id, IModel<String> model) {
        super(id, model);
    }

    @Override
    public void onComponentTagBody(final MarkupStream markupStream, final ComponentTag openTag) {
        String text = getModelObject();
        if (StringUtils.isNotEmpty(text)) {
            if (this.getEscapeModelStrings()) {
                // Escape text iif escapeModel is enabled.
                text = Strings.escapeMarkup(text, false, false).toString();
            }
            replaceComponentTagBody(markupStream, openTag, text);
            return;
        }

        super.onComponentTagBody(markupStream, openTag);
    }

    @Override
    protected void onComponentTag(ComponentTag tag) {
        super.onComponentTag(tag);

        if (tag.isOpenClose()) {
            tag.setType(XmlTag.TagType.OPEN);
        }
    }

    @Override
    protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
        super.updateAjaxAttributes(attributes);
        attributes.setEventPropagation(AjaxRequestAttributes.EventPropagation.BUBBLE);
        attributes.setChannel(new AjaxChannel("blocking", AjaxChannel.Type.ACTIVE));
    }

}

/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.component.button;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.parser.XmlTag;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.PackageResourceReference;

/**
 * @author lazyman
 */
public abstract class AjaxSubmitLinkButton extends AjaxSubmitLink {

    private IModel<String> label;

    public AjaxSubmitLinkButton(String id, IModel<String> label) {
        this(id, ButtonType.SIMPLE, label);
    }

    public AjaxSubmitLinkButton(String id, ButtonType type, IModel<String> label) {
        this(id, type, label, null);
    }

    public AjaxSubmitLinkButton(String id, ButtonType type, IModel<String> label, String image) {
        super(id);
        Validate.notNull(label, "Label model must not be null.");
        Validate.notNull(type, "Button type must not be null.");
        this.label = label;

        add(new AttributeAppender("class", AjaxLinkButton.getTypeModel(type), " "));
        if (StringUtils.isNotEmpty(image)) {
            add(new AttributeAppender("style", AjaxLinkButton.getImgModel(image), " "));
        }
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);

        response.render(CssHeaderItem.forReference(
                new PackageResourceReference(AjaxLinkButton.class, "AjaxLinkButton.css")));
    }

    @Override
    public void onComponentTagBody(final MarkupStream markupStream, final ComponentTag openTag) {
        replaceComponentTagBody(markupStream, openTag, label.getObject());
    }

    @Override
    protected void onComponentTag(ComponentTag tag) {
        super.onComponentTag(tag);

        if (tag.isOpenClose()) {
            tag.setType(XmlTag.TagType.OPEN);
        }
    }
}

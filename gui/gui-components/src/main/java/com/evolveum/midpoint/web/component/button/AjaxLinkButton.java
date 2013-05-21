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
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.parser.XmlTag;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.resource.PackageResourceReference;

/**
 * @author lazyman
 */
public abstract class AjaxLinkButton extends AjaxLink<String> {

    private ButtonType type;

    public AjaxLinkButton(String id, IModel<String> label) {
        this(id, ButtonType.SIMPLE, label);
    }

    public AjaxLinkButton(String id, ButtonType type, IModel<String> label) {
        this(id, type, label, null);
    }

    public AjaxLinkButton(String id, ButtonType type, IModel<String> label, String image) {
        super(id, label);
        Validate.notNull(type, "Button type must not be null.");

        add(new AttributeAppender("class", getTypeModel(type), " "));
        if (StringUtils.isNotEmpty(image)) {
            add(new AttributeAppender("style", getImgModel(image), " "));
        }
    }

    static IModel<String> getImgModel(String image) {
        StringBuilder model = new StringBuilder();
        model.append("padding-left: 30px; background: url('");
        model.append(WebApplication.get().getServletContext().getContextPath());
        if (!image.startsWith("/")) {
            model.append("/");
        }
        model.append(image);
        model.append("') no-repeat 7px 7px #F3F3F3;");

        return new Model<String>(model.toString());
    }

    static IModel<String> getTypeModel(ButtonType type) {
        StringBuilder model = new StringBuilder();
        model.append("button");

        switch (type) {
            case POSITIVE:
                model.append(" positive");
                break;
            case NEGATIVE:
                model.append(" negative");
                break;
        }

        return new Model<String>(model.toString());
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);

        response.render(CssHeaderItem.forReference(
                new PackageResourceReference(AjaxLinkButton.class, "AjaxLinkButton.css")));
    }

    @Override
    public void onComponentTagBody(final MarkupStream markupStream, final ComponentTag openTag) {
        replaceComponentTagBody(markupStream, openTag, getDefaultModelObjectAsString());
    }

    @Override
    protected void onComponentTag(ComponentTag tag) {
        super.onComponentTag(tag);

        if (tag.isOpenClose()) {
            tag.setType(XmlTag.TagType.OPEN);
        }
    }
}

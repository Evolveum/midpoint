/*
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */

package com.evolveum.midpoint.web.component.button;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.parser.XmlTag;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.request.resource.PackageResourceReference;

/**
 * @author lazyman
 */
public abstract class AjaxLinkButton extends AjaxLink<String> {

    public static enum Type {SIMPLE, LEFT, MIDDLE, RIGHT}

    private Type type;

    public AjaxLinkButton(String id, IModel<String> label) {
        this(id, Type.SIMPLE, label);
    }

    public AjaxLinkButton(String id, Type type, IModel<String> label) {
        this(id, type, label, null);
    }

    public AjaxLinkButton(String id, Type type, IModel<String> label, String image) {
        super(id, label);
        Validate.notNull(type, "Button type must not be null.");

        add(new AttributeAppender("class", getTypeModel(type), " "));
        if (StringUtils.isNotEmpty(image)) {
            add(new AttributeAppender("style", getImgModel(image), " "));
        }
    }

    private IModel<String> getImgModel(String image) {
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

    private IModel<String> getTypeModel(Type type) {
        StringBuilder model = new StringBuilder();
        model.append("button");

        switch (type) {
            case LEFT:
                model.append(" left");
                break;
            case MIDDLE:
                model.append(" middle");
                break;
            case RIGHT:
                model.append(" right");
                break;
        }

        return new Model<String>(model.toString());
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);

        response.renderCSSReference(new PackageResourceReference(AjaxLinkButton.class, "AjaxLinkButton.css"));
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

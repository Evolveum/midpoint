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
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.parser.XmlTag;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.PackageResourceReference;

/**
 * @author lazyman
 */
public abstract class AjaxSubmitLinkButton extends AjaxSubmitLink {

    private IModel<String> label;
    private ButtonType type;

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

        response.renderCSSReference(new PackageResourceReference(AjaxLinkButton.class, "AjaxLinkButton.css"));
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

package com.evolveum.midpoint.web.component;

import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.parser.XmlTag;
import org.apache.wicket.model.IModel;

/**
 * @author lazyman
 */
public abstract class AjaxSubmitButton extends AjaxSubmitLink {

    private IModel<String> label;

    public AjaxSubmitButton(String id) {
        super(id, null);
    }

    public AjaxSubmitButton(String id, IModel<String> label) {
        super(id);
        this.label = label;
    }

    @Override
    public void onComponentTagBody(final MarkupStream markupStream, final ComponentTag openTag) {
        if (label != null) {
            String text = label.getObject();
            replaceComponentTagBody(markupStream, openTag, text);
            return;
        }

        super.onComponentTagBody(markupStream, openTag);
    }

    @Override
    protected void onComponentTag(ComponentTag tag) {
        super.onComponentTag(tag);

        if (!isEnabled()) {
            tag.put("disabled", "disabled");
        }

        if (tag.isOpenClose()) {
            tag.setType(XmlTag.TagType.OPEN);
        }
    }
}

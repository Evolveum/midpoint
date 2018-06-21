package com.evolveum.midpoint.web.component.data.paging;

import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.MarkupStream;
import org.apache.wicket.markup.parser.XmlTag;
import org.apache.wicket.model.Model;

/**
 * @author lazyman
 */
public abstract class NavigatorPageLink extends AjaxLink<String> {

    private final long pageNumber;

    public NavigatorPageLink(String id, long pageNumber) {
        super(id, new Model<>(Long.toString(pageNumber + 1)));
        this.pageNumber = pageNumber;
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

    public long getPageNumber() {
        return pageNumber;
    }
}

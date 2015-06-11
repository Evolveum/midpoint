package com.evolveum.midpoint.web.component.menu.top;

import com.evolveum.midpoint.web.component.menu.top.MenuItem;
import com.evolveum.midpoint.web.component.util.BaseSimplePanel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.Model;

/**
 * @author lazyman
 */
public class LabeledBookmarkableLink extends BaseSimplePanel<MenuItem> {

    private static final String ID_LABEL = "label";
    private static final String ID_LINK = "link";

    public LabeledBookmarkableLink(String id, MenuItem menuItem) {
        super(id, new Model<MenuItem>(menuItem));
        setRenderBodyOnly(true);
    }

    @Override
    protected void initLayout() {
        MenuItem item = getModel().getObject();
        BookmarkablePageLink link = new BookmarkablePageLink(ID_LINK, item.getPage(), item.getPageParameters());
        add(link);

        Label label = new Label(ID_LABEL, item.getName());
        label.setRenderBodyOnly(true);
        link.add(label);
    }
}

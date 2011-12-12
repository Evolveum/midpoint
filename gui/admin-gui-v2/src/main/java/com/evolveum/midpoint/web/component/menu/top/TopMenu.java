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

package com.evolveum.midpoint.web.component.menu.top;

import org.apache.wicket.Page;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.list.Loop;
import org.apache.wicket.markup.html.list.LoopItem;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.PackageResourceReference;

import java.util.List;

public class TopMenu extends Panel {

    private List<TopMenuItem> items;

    public TopMenu(String id, List<TopMenuItem> items) {
        super(id);
        if (items == null) {
            throw new IllegalArgumentException("List with top menu items must not be null.");
        }
        this.items = items;

        add(new Loop("list", items.size()) {

            @Override
            protected void populateItem(LoopItem loopItem) {
                final TopMenuItem item = TopMenu.this.items.get(loopItem.getIndex());
                BookmarkablePageLink<String> link = new BookmarkablePageLink<String>("link", item.getPage()); // {
//
//                    @Override
//                    public void onClick(AjaxRequestTarget target) {
//                        setResponsePage(item.getPage());
//                    }
//                };
                link.add(new Label("label", new StringResourceModel(item.getLabel(), TopMenu.this, null)));
                link.add(new Label("description", new StringResourceModel(item.getDescription(), TopMenu.this, null)));

                Page page = TopMenu.this.getPage();
                if (page != null && page.getClass().isAssignableFrom(item.getPage())) {
                    link.add(new AttributeAppender("class", new Model("selected-top"), " "));
                }
                loopItem.add(link);
            }
        });
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);

        response.renderCSSReference(new PackageResourceReference(TopMenu.class, "TopMenu.css"));
    }


}

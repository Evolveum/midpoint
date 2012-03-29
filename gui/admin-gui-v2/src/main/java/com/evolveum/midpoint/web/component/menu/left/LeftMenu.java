/*
 * Copyright (c) 2012 Evolveum
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
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.web.component.menu.left;

import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import org.apache.commons.lang.Validate;
import org.apache.wicket.Page;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.WebComponent;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.list.Loop;
import org.apache.wicket.markup.html.list.LoopItem;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.resource.PackageResourceReference;

import java.util.List;

/**
 * @author lazyman
 */
public class LeftMenu extends Panel {

    private List<LeftMenuItem> items;

    public LeftMenu(String id, List<LeftMenuItem> items) {
        super(id);
        Validate.notNull(items, "List with left menu items must not be null.");
        this.items = items;

        initLayout();
    }

    private void initLayout() {
        Loop loop = new Loop("list", items.size()) {

            @Override
            protected void populateItem(LoopItem loopItem) {
                final LeftMenuItem item = LeftMenu.this.items.get(loopItem.getIndex());
                BookmarkablePageLink<String> link = new BookmarkablePageLink<String>("link", item.getPage());

                link.add(new Label("label", new StringResourceModel(item.getLabel(), LeftMenu.this, null)));
                //link.add(new Label("icon", new Model<String>(item.getIcon())));
                link.add(new StaticImage("icon", new Model<String>(item.getIcon())));

                Page page = LeftMenu.this.getPage();
                if (page != null && page.getClass().isAssignableFrom(item.getPage())) {
                    link.add(new AttributeAppender("class", new Model("selected-left"), " "));
                }
                loopItem.add(link);
            }
        };
        loop.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return items.size() > 0;
            }
        });

        add(loop);
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);

        response.renderCSSReference(new PackageResourceReference(LeftMenu.class, "LeftMenu.css"));
    }
    
    public class StaticImage extends WebComponent {
		private static final long serialVersionUID = 1L;

		public StaticImage(String id, IModel<String> model) {
            super(id, model);
        }

        protected void onComponentTag(ComponentTag tag) {
            super.onComponentTag(tag);
            checkComponentTag(tag, "img");
            tag.put("src", getDefaultModelObjectAsString());
            tag.put("alt", "");
        }

    }
}

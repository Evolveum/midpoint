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

package com.evolveum.midpoint.web.component.menu.top;

import com.evolveum.midpoint.web.component.menu.top.BottomMenuItem;
import com.evolveum.midpoint.web.component.menu.top.TopMenuItem;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.security.PageUrlMapping;
import com.evolveum.midpoint.web.util.WebMiscUtil;

import org.apache.commons.lang.Validate;
import org.apache.wicket.Page;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.list.Loop;
import org.apache.wicket.markup.html.list.LoopItem;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;

import java.util.List;

public class TopMenu extends Panel {

    private List<TopMenuItem> topItems;
    private List<BottomMenuItem> bottomItems;

    public TopMenu(String id, List<TopMenuItem> topItems, List<BottomMenuItem> bottomItems) {
        super(id);
        Validate.notNull(topItems, "List with top menu items must not be null.");
        Validate.notNull(bottomItems, "List with top menu items must not be null.");

        this.topItems = topItems;
        this.bottomItems = bottomItems;

        add(new Loop("topList", topItems.size()) {

            @Override
            protected void populateItem(LoopItem loopItem) {
                final TopMenuItem item = TopMenu.this.topItems.get(loopItem.getIndex());
                BookmarkablePageLink<String> link = new BookmarkablePageLink<String>("topLink", item.getPage());
                if (loopItem.getIndex() == 0) {
                    link.add(new AttributeAppender("class", new Model("first"), " "));
                } else if (loopItem.getIndex() == (TopMenu.this.topItems.size() - 1)) {
                    link.add(new AttributeAppender("class", new Model("last"), " "));
                }

                link.add(new Label("topLabel", new StringResourceModel(item.getLabel(), TopMenu.this, null)));
                link.add(new Label("topDescription", new StringResourceModel(item.getDescription(), TopMenu.this, null)));

                Page page = TopMenu.this.getPage();
                if (page != null && item.getMarker().isAssignableFrom(page.getClass())) {
                    link.add(new AttributeAppender("class", new Model("selected-top"), " "));
                }
                loopItem.add(link);
            }
        });

        Loop bottomLoop = new Loop("bottomList", bottomItems.size()) {

            @Override
            protected void populateItem(LoopItem loopItem) {
                final BottomMenuItem item = TopMenu.this.bottomItems.get(loopItem.getIndex());
                BookmarkablePageLink<String> link = new BookmarkablePageLink<String>("bottomLink", item.getPage());
                link.add(new Label("bottomLabel", item.getLabel()));

                Page page = TopMenu.this.getPage();
                if (page != null && page.getClass().isAssignableFrom(item.getPage())) {
                    link.add(new AttributeAppender("class", new Model("selected-bottom"), " "));
                }
                loopItem.add(link);

                
				loopItem.add(new VisibleEnableBehaviour() {
					
					@Override
					public boolean isVisible() {
						String[] actions = PageUrlMapping.findActions(item.getPage());
						if (item.getVisible() != null){
							return WebMiscUtil.isAuthorized(actions) && item.getVisible().isVisible();
						}
						return WebMiscUtil.isAuthorized(actions);
					}
					
					@Override
					public boolean isEnabled(){
						if (item.getVisible() != null){
							return item.getVisible().isEnabled();
						}
						return true;
					}
				});
                
                
//                if (item.getVisible() != null) {
//                    loopItem.add(item.getVisible());
//                }
            }
        };

        bottomLoop.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return !TopMenu.this.bottomItems.isEmpty();
            }
        });
        add(bottomLoop);
    }
}

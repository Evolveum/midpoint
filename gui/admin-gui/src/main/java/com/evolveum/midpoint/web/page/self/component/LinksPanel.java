/*
 * Copyright (c) 2015 Evolveum
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
package com.evolveum.midpoint.web.page.self.component;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.protocol.http.WebApplication;

import javax.servlet.ServletContext;

import java.util.List;

/**
 * @author Kate Honchar
 */
public class LinksPanel extends SimplePanel<List<RichHyperlinkType>> {
    private static final String DOT_CLASS = LinksPanel.class.getName() + ".";
    private static final String ID_IMAGE = "imageId";
    private static final String ID_LINK = "link";
    private static final String ID_LABEL = "labelId";
    private static final String ID_DESCRIPTION = "descriptionId";
    private static final String ID_LINKS_ROW = "linksRow";
    private static final String ID_LINKS_COLUMN = "linksColumn";

    private static final String ICON_DEFAULT_CSS_CLASS = "fa fa-angle-double-right";

    private static final Trace LOGGER = TraceManager.getTrace(LinksPanel.class);

    IModel<List<RichHyperlinkType>> model;

    public LinksPanel(String id) {
        super(id, null);
    }

    public LinksPanel(String id, IModel<List<RichHyperlinkType>> model) {
        super(id, model);
    }

    @Override
    protected void initLayout() {

        final List<RichHyperlinkType> linksList = getModel().getObject();
        RepeatingView rowView = new RepeatingView(ID_LINKS_ROW);

        int linksListSize = linksList == null ? 0 : linksList.size();
        if (linksListSize > 0) {
            int currentColumn = 0;
            RepeatingView columnView = null;
            WebMarkupContainer row = null;
            boolean isRowAdded = false;
            for (int i = 0; i < linksListSize; i++) {
                final RichHyperlinkType link = linksList.get(i);
                if (WebComponentUtil.isAuthorized(link.getAuthorization())) {
                    if (currentColumn == 0) {
                        row = new WebMarkupContainer(rowView.newChildId());
                        isRowAdded = false;
                        columnView = new RepeatingView(ID_LINKS_COLUMN);
                    }
                    WebMarkupContainer column = new WebMarkupContainer(columnView.newChildId());
                    Link linkItem = new Link(ID_LINK) {
                        @Override
                        public void onClick() {
                        }

                        @Override
                        protected void onComponentTag(final ComponentTag tag) {
                            super.onComponentTag(tag);
                            String rootContext = "";
                            //TODO: what is this for???
                            if (link.getTargetUrl() != null && !link.getTargetUrl().startsWith("http://") &&
                                    !link.getTargetUrl().startsWith("https://") &&
                                    !link.getTargetUrl().startsWith("www://") &&
                                    !link.getTargetUrl().startsWith("//")) {
                                WebApplication webApplication = WebApplication.get();
                                if (webApplication != null) {
                                    ServletContext servletContext = webApplication.getServletContext();
                                    if (servletContext != null) {
                                        rootContext = servletContext.getContextPath();
                                    }
                                }
                            }
                            tag.put("href", rootContext + (link.getTargetUrl() == null ? "#" : link.getTargetUrl()));
                        }
                    };
                    linkItem.add(new Label(ID_IMAGE) {
                        @Override
                        protected void onComponentTag(final ComponentTag tag) {
                            super.onComponentTag(tag);
                            String cssClass = ICON_DEFAULT_CSS_CLASS;
                            if (link.getIcon() != null) {
                            	cssClass = link.getIcon().getCssClass();
                            }
                            tag.put("class", "info-box-icon " + (link.getColor() != null ?
                                    (link.getColor().startsWith("bg-") ? link.getColor() : "bg-" + link.getColor()) : "") + " "
                                    + cssClass);
                        }
                    });

                    linkItem.add(new Label(ID_LABEL, new Model<String>() {
                        public String getObject() {
                            return link.getLabel();
                        }
                    }));
                    Label description = new Label(ID_DESCRIPTION, new Model<String>() {
                        public String getObject() {
                            return link.getDescription();
                        }
                    });
                    description.setEnabled(false);
                    linkItem.add(description);

                    column.add(linkItem);
                    columnView.add(column);
                    if (currentColumn == 1 || (linksList.indexOf(link) == linksListSize - 1)) {
                        row.add(columnView);
                        rowView.add(row);
                        currentColumn = 0;
                        isRowAdded = true;
                    } else {
                        currentColumn++;
                    }
                } else {
                	LOGGER.trace("Link {} not authorized, skipping", link);
                }
            }
            if (row != null && columnView != null && !isRowAdded){
                row.add(columnView);
                rowView.add(row);
            }
        }
        add(rowView);
    }
}

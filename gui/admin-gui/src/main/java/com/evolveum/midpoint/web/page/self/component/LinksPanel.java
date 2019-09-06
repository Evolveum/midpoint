/*
 * Copyright (c) 2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.self.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
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
import org.apache.wicket.protocol.http.WebApplication;

import javax.servlet.ServletContext;

import java.util.List;

/**
 * @author Kate Honchar
 */
public class LinksPanel extends BasePanel<List<RichHyperlinkType>> {

    private static final String DOT_CLASS = LinksPanel.class.getName() + ".";

    private static final String ID_IMAGE = "imageId";
    private static final String ID_LINK = "link";
    private static final String ID_LABEL = "labelId";
    private static final String ID_DESCRIPTION = "descriptionId";
    private static final String ID_LINKS_ROW = "linksRow";
    private static final String ID_LINKS_COLUMN = "linksColumn";

    private static final String ICON_DEFAULT_CSS_CLASS = "fa fa-angle-double-right";

    private static final Trace LOGGER = TraceManager.getTrace(LinksPanel.class);

    public LinksPanel(String id) {
        this(id, null);
    }

    public LinksPanel(String id, IModel<List<RichHyperlinkType>> model) {
        super(id, model);
    }
    
    @Override
    protected void onInitialize() {
    	super.onInitialize();
    	initLayout();
    }

  
    protected void initLayout() {

        final List<RichHyperlinkType> linksList = getModel().getObject();
        RepeatingView rowView = new RepeatingView(ID_LINKS_ROW);
        add(rowView);

        int linksListSize = linksList == null ? 0 : linksList.size();
        if (linksListSize == 0) {
            return;
        }


        int currentColumn = 0;
        RepeatingView columnView = null;
        WebMarkupContainer row = null;
        boolean isRowAdded = false;
        for (int i = 0; i < linksListSize; i++) {
            final RichHyperlinkType link = linksList.get(i);

            if (!WebComponentUtil.isAuthorized(link.getAuthorization())) {
                LOGGER.trace("Link {} not authorized, skipping", link);
                continue;
            }

            if (currentColumn == 0) {
                row = new WebMarkupContainer(rowView.newChildId());
                isRowAdded = false;
                columnView = new RepeatingView(ID_LINKS_COLUMN);
            }

            WebMarkupContainer column = new WebMarkupContainer(columnView.newChildId());
            Link<Void> linkItem = new Link<Void>(ID_LINK) {
            	
            	private static final long serialVersionUID = 1L;

				@Override
				public void onClick() {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				protected void onComponentTag(ComponentTag tag) {
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

            linkItem.add(new Label(ID_LABEL, new IModel<String>() {

                @Override
                public String getObject() {
                    String key = link.getLabel();
                    if (key == null) {
                        return null;
                    }
                    return getString(key, null, key);
                }
            }));

            Label description = new Label(ID_DESCRIPTION, new IModel<String>() {

                @Override
                public String getObject() {
                    String desc = link.getDescription();
                    if (desc == null) {
                        return null;
                    }
                    return getString(desc, null, desc);
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
        }

        if (row != null && columnView != null && !isRowAdded){
            row.add(columnView);
            rowView.add(row);
        }
    }
}

/*
 * Copyright (c) 2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.self.dashboard.component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.ServletContext;

import com.evolveum.midpoint.gui.api.PredefinedDashboardWidgetId;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PreviewContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserInterfaceElementVisibilityType;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.protocol.http.WebApplication;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RichHyperlinkType;

/**
 * @author Kate Honchar
 */
public class LinksPanel extends BasePanel<List<RichHyperlinkType>> {

    private static final String ID_IMAGE = "imageId";
    private static final String ID_LINK = "link";
    private static final String ID_LINKS_PANEL = "linksPanel";
    private static final String ID_LABEL = "labelId";
    private static final String ID_DESCRIPTION = "descriptionId";
//    private static final String ID_LINKS_ROW = "linksRow";
//    private static final String ID_LINKS_COLUMN = "linksColumn";

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
        List<RichHyperlinkType> linksList = getModelObject() != null ?
                getModelObject().stream().filter(link -> WebComponentUtil.isAuthorized(link.getAuthorization())).collect(Collectors.toList())
                : new ArrayList<>();

        ListView<RichHyperlinkType> linksPanel = new ListView<>(ID_LINKS_PANEL, linksList) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void populateItem(ListItem<RichHyperlinkType> item) {
                RichHyperlinkType link = item.getModelObject();
                Link<Void> linkItem = new Link<>(ID_LINK) {

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
                        tag.put("class", "info-box-icon " + " " + cssClass);
                        tag.put("style", link.getColor() != null ? ("background-color: " + link.getColor() + " !important") : "");
                    }
                });

                linkItem.add(new Label(ID_LABEL, () -> {
                    String key = link.getLabel();
                    if (key == null) {
                        return null;
                    }
                    return getString(key, null, key);
                }));

                Label description = new Label(ID_DESCRIPTION, () -> {
                    String desc = link.getDescription();
                    if (desc == null) {
                        return null;
                    }
                    return getString(desc, null, desc);
                });
                description.setEnabled(false);
                linkItem.add(description);
                item.add(linkItem);
            }
        };
        linksPanel.setOutputMarkupId(true);
        linksPanel.add(new VisibleBehaviour(() -> CollectionUtils.isNotEmpty(linksList)));
        add(linksPanel);

    }
}

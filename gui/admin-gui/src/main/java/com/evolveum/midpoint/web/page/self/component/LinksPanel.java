package com.evolveum.midpoint.web.page.self.component;

import com.evolveum.midpoint.web.component.util.SimplePanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.List;

/**
 * Created by Kate on 23.09.2015.
 */
public class LinksPanel extends SimplePanel<List<RichHyperlinkType>> {
    private static final String DOT_CLASS = LinksPanel.class.getName() + ".";
    private static final String ID_IMAGE = "imageId";
    private static final String ID_LINK = "link";
    private static final String ID_LABEL = "labelId";
    private static final String ID_LINKS_ROW = "linksRow";
    private static final String ID_LINKS_COLUMN = "linksColumn";
    private static final String OPERATION_LOAD_LINKS = DOT_CLASS + "loadLinks";

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
            for (int i = 0; i < linksListSize; i++) {
                final RichHyperlinkType link = linksList.get(i);
                if (currentColumn == 0) {
                    row = new WebMarkupContainer(rowView.newChildId());
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
                        tag.put("href", link.getTargetUrl());
                    }
                };
                linkItem.add(new Label(ID_IMAGE) {
                    @Override
                    protected void onComponentTag(final ComponentTag tag) {
                        super.onComponentTag(tag);
                        tag.put("class", "info-box-icon " + (link.getColor() != null ?
                                (link.getColor().startsWith("bg-") ? link.getColor() : "bg-" + link.getColor()) : "") + " "
                                + link.getIcon().getCssClass());
                    }
                });

                linkItem.add(new Label(ID_LABEL, new Model<String>() {
                    public String getObject() {
                        return link.getLabel();
                    }
                }));

                column.add(linkItem);
                columnView.add(column);
                if (currentColumn == 1 || (i == (linksListSize - 1))) {
                    row.add(columnView);
                    rowView.add(row);
                    currentColumn = 0;
                } else {
                    currentColumn++;
                }
            }
        }
        add(rowView);
    }


}

/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

/**
 * Created by Viliam Repan (lazyman).
 */
public class PersonOfInterestPanel extends BasePanel implements WizardPanel {

    private static final long serialVersionUID = 1L;

    public enum PersonOfInterest {

        MYSELF("fas fa-user-circle"),

        GROUP_OTHERS("fas fa-user-friends"),

        TEAM("fas fa-users");

        private String icon;

        PersonOfInterest(String icon) {
            this.icon = icon;
        }

        public String getIcon() {
            return icon;
        }
    }

    private static final String ID_LIST_CONTAINER = "listContainer";
    private static final String ID_LIST = "list";

    private static final String ID_TILE = "tile";
    private static final String ID_BACK = "back";
    private static final String ID_NEXT = "next";
    private static final String ID_NEXT_LABEL = "nextLabel";

    private IModel<List<Tile>> tiles;

    public PersonOfInterestPanel(String id) {
        super(id);

        initModels();
        initLayout();
    }

    @Override
    public VisibleEnableBehaviour getHeaderBehaviour() {
        return new VisibleEnableBehaviour(() -> false);
    }

    @Override
    public IModel<String> getTitle() {
        return () -> getString("PersonOfInterestPanel.title");
    }

    private void initModels() {
        tiles = new LoadableModel<>(false) {
            @Override
            protected List<Tile> load() {
                List<Tile> list = new ArrayList<>();

                for (PersonOfInterest poi : PersonOfInterest.values()) {
                    list.add(new Tile(poi.getIcon(), getString(poi)));
                }

                return list;
            }
        };
    }

    private void initLayout() {
        AjaxLink back = new AjaxLink<>(ID_BACK) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                onBackPerformed(target);
            }
        };
        add(back);

        AjaxLink next = new AjaxLink<>(ID_NEXT) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                onNextPerformed(target);
            }
        };
        next.add(new VisibleBehaviour(() -> tiles.getObject().stream().filter(t -> t.isSelected()).count() > 0));
        next.setOutputMarkupId(true);
        next.setOutputMarkupPlaceholderTag(true);
        add(next);

        Label nextLabel = new Label(ID_NEXT_LABEL);
        next.add(nextLabel);

        WebMarkupContainer listContainer = new WebMarkupContainer(ID_LIST_CONTAINER);
        listContainer.setOutputMarkupId(true);
        add(listContainer);
        ListView<Tile> list = new ListView<>(ID_LIST, tiles) {

            @Override
            protected void populateItem(ListItem<Tile> item) {
                TilePanel tp = new TilePanel(ID_TILE, () -> item.getModelObject()) {

                    @Override
                    protected void onClick(AjaxRequestTarget target) {
                        boolean selected = getModelObject().isSelected();

                        List<Tile> tiles = PersonOfInterestPanel.this.tiles.getObject();
                        tiles.forEach(t -> t.setSelected(false));

                        if (!selected) {
                            getModelObject().setSelected(true);
                        }

                        target.add(listContainer);
                        target.add(next);
                    }
                };
                item.add(tp);
            }
        };
        listContainer.add(list);
    }

    protected void onNextPerformed(AjaxRequestTarget target) {

    }

    protected void onBackPerformed(AjaxRequestTarget target) {

    }
}

/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.web.security.util.SecurityUtils;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.wizard.WizardStep;
import com.evolveum.midpoint.gui.api.component.wizard.WizardStepPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

/**
 * Created by Viliam Repan (lazyman).
 */
public class PersonOfInterestPanel extends WizardStepPanel<RequestAccess> {

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
    private IModel<List<Tile<PersonOfInterest>>> tiles;

    public PersonOfInterestPanel(IModel<RequestAccess> model) {
        super(model);

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
            protected List<Tile<PersonOfInterest>> load() {
                List<Tile<PersonOfInterest>> list = new ArrayList<>();

                for (PersonOfInterest poi : PersonOfInterest.values()) {
                    Tile tile = new Tile(poi.getIcon(), getString(poi));
                    tile.setValue(poi);
                    list.add(tile);
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

        Label nextLabel = new Label(ID_NEXT_LABEL, () -> {
            WizardStep step = getWizard().getNextPanel();
            return step != null ? step.getTitle().getObject() : null;
        });
        next.add(nextLabel);

        WebMarkupContainer listContainer = new WebMarkupContainer(ID_LIST_CONTAINER);
        listContainer.setOutputMarkupId(true);
        add(listContainer);
        ListView<Tile<PersonOfInterest>> list = new ListView<>(ID_LIST, tiles) {

            @Override
            protected void populateItem(ListItem<Tile<PersonOfInterest>> item) {
                TilePanel tp = new TilePanel(ID_TILE, () -> item.getModelObject()) {

                    @Override
                    protected void onClick(AjaxRequestTarget target) {
                        boolean selected = getModelObject().isSelected();

                        List<Tile<PersonOfInterest>> tiles = PersonOfInterestPanel.this.tiles.getObject();
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

    protected void onBackPerformed(AjaxRequestTarget target) {
        new Toast()
                .title("some title")
                .body("example body " + 1000 * Math.random())
                .cssClass("bg-success")
                .autohide(true)
                .show(target);
    }

    private void onNextPerformed(AjaxRequestTarget target) {
        // todo fix
        ObjectReferenceType myself = new ObjectReferenceType()
                .oid(SecurityUtil.getPrincipalOidIfAuthenticated())
                .type(UserType.COMPLEX_TYPE);
        getModelObject().getPersonOfInterest().add(myself);

        getWizard().next();

        target.add(getWizard().getPanel());
    }
}

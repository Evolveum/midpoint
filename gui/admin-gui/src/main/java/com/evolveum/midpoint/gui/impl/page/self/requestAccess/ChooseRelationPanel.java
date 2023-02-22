/*
 * Copyright (c) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.wizard.BasicWizardStepPanel;
import com.evolveum.midpoint.gui.api.component.wizard.WizardModel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.tile.Tile;
import com.evolveum.midpoint.gui.impl.component.tile.TilePanel;
import com.evolveum.midpoint.gui.impl.page.self.PageRequestAccess;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ChooseRelationPanel extends BasePanel<List<QName>> {

    private static final String DEFAULT_RELATION_ICON = "fa-solid fa-user";

    private static final String ID_LIST_CONTAINER = "listContainer";
    private static final String ID_LIST = "list";
    private static final String ID_TILE = "tile";


    private LoadableModel<List<Tile<QName>>> relations;

    public ChooseRelationPanel(String id, IModel<List<QName>> model) {
        super(id, model);

        initModels();
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    public void resetModel() {
        relations.reset();
    }

    private void initModels() {
        relations = new LoadableModel<>(false) {

            @Override
            protected List<Tile<QName>> load() {
                List<Tile<QName>> tiles = new ArrayList<>();

                List<QName> availableRelations = getModelObject();
                for (QName name : availableRelations) {
                    Tile<QName> tile = createTileForRelation(name);
                    tile.setSelected(name.equals(getDefaultRelation()));
                    tile.setValue(name);

                    tiles.add(tile);
                }

                return tiles;
            }
        };
    }

    protected QName getDefaultRelation() {
        return null;
    }

    private void initLayout() {
        WebMarkupContainer listContainer = new WebMarkupContainer(ID_LIST_CONTAINER);
        listContainer.setOutputMarkupId(true);
        add(listContainer);

        ListView<Tile<QName>> list = new ListView<>(ID_LIST, relations) {

            @Override
            protected void populateItem(ListItem<Tile<QName>> item) {
                TilePanel tp = new TilePanel(ID_TILE, item.getModel()) {

                    @Override
                    protected void onClick(AjaxRequestTarget target) {
                        Tile<QName> tile = item.getModelObject();
                        boolean wasSelected = tile.isSelected();

                        relations.getObject().forEach(t -> t.setSelected(false));
                        tile.setSelected(!wasSelected);

                        target.add(ChooseRelationPanel.this.get(ID_LIST_CONTAINER));
                        onTileClick(target);
                    }
                };
                customizeTilePanel(tp);
                item.add(tp);
            }
        };
        listContainer.add(list);
    }

    protected void customizeTilePanel(TilePanel tp) {

    }

    protected void onTileClick(AjaxRequestTarget target) {
    }

    private String getDefaultRelationIcon(QName name) {
        if (SchemaConstants.ORG_DEFAULT.equals(name)) {
            return "fa-solid fa-user";
        } else if (SchemaConstants.ORG_MANAGER.equals(name)) {
            return "fa-solid fa-user-tie";
        } else if (SchemaConstants.ORG_APPROVER.equals(name)) {
            return "fa-solid fa-clipboard-check";
        } else if (SchemaConstants.ORG_OWNER.equals(name)) {
            return "fa-solid fa-crown";
        }

        return DEFAULT_RELATION_ICON;
    }

    private Tile<QName> createTileForRelation(QName name) {
        List<RelationDefinitionType> relations = getSystemRelations();

        String icon = getDefaultRelationIcon(name);
        String label = name.getLocalPart();

        if (relations == null) {
            return createTile(icon, label, name);
        }

        for (RelationDefinitionType rel : relations) {
            if (!name.equals(rel.getRef())) {
                continue;
            }

            DisplayType display = rel.getDisplay();
            if (display == null) {
                break;
            }

            IconType it = display.getIcon();
            if (it != null && it.getCssClass() != null) {
                icon = it.getCssClass();
            }

            label = WebComponentUtil.getTranslatedPolyString(display.getLabel());

            break;
        }

        return createTile(icon, label, name);
    }

    private Tile<QName> createTile(String icon, String label, QName value) {
        Tile<QName> tile = new Tile<>(icon, label);
        tile.setValue(value);

        return tile;
    }

    public QName getSelectedRelation() {
        Tile<QName> selected = relations.getObject().stream().filter(t -> t.isSelected()).findFirst().orElse(null);
        if (selected == null) {
            return null;
        }

        return selected.getValue();
    }

    public boolean isSelectedRelation() {
        return relations.getObject().stream().anyMatch(t -> t.isSelected());
    }

    private List<RelationDefinitionType> getSystemRelations() {
        SystemConfigurationType sys = MidPointApplication.get().getSystemConfigurationIfAvailable();
        if (sys == null) {
            return null;
        }

        RoleManagementConfigurationType roleManagement = sys.getRoleManagement();
        if (roleManagement == null) {
            return null;
        }

        RelationsDefinitionType relations = roleManagement.getRelations();
        return relations != null ? relations.getRelation() : null;
    }

    public List<Tile<QName>> getRelations() {
        return relations.getObject();
    }
}

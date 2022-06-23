/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import com.evolveum.midpoint.gui.api.component.wizard.BasicWizardPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.tile.Tile;
import com.evolveum.midpoint.gui.impl.component.tile.TilePanel;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.SecurityUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Viliam Repan (lazyman).
 */
public class RelationPanel extends BasicWizardPanel<RequestAccess> {

    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = RelationPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_ASSIGNABLE_RELATIONS_LIST = DOT_CLASS + "loadAssignableRelationsList";

    private static final String DEFAULT_RELATION_ICON = "fa-solid fa-user";

    private static final String ID_LIST_CONTAINER = "listContainer";
    private static final String ID_LIST = "list";
    private static final String ID_TILE = "tile";

    private IModel<List<Tile<QName>>> relations;

    public RelationPanel(IModel<RequestAccess> model) {
        super(model);

        initModels();
        initLayout();
    }

    private void initModels() {
        relations = new LoadableModel<>(false) {

            @Override
            protected List<Tile<QName>> load() {
                List<Tile<QName>> tiles = new ArrayList<>();

                List<QName> list = getAvailableRelationsList();
                for (QName name : list) {
                    Tile<QName> tile = createTileForRelation(name);
                    tile.setValue(name);

                    tiles.add(tile);
                }

                return tiles;
            }
        };
    }

    @Override
    public IModel<String> getTitle() {
        return createStringResource("RelationPanel.title");
    }

    @Override
    protected IModel<String> getTextModel() {
        return createStringResource("RelationPanel.text");
    }

    @Override
    protected IModel<String> getSubTextModel() {
        return createStringResource("RelationPanel.subtext");
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

                        target.add(getWizard().getPanel());
                    }
                };
                item.add(tp);
            }
        };
        listContainer.add(list);
    }

    private Tile<QName> createTileForRelation(QName name) {
        RelationSelectionType config = getRelationConfiguration();
        RelationsDefinitionType relations = config != null ? config.getRelations() : new RelationsDefinitionType();

        String icon = DEFAULT_RELATION_ICON;
        String label = name.getLocalPart();

        for (RelationDefinitionType rel : relations.getRelation()) {
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

        Tile tile = new Tile(icon, label);
        tile.setValue(name);

        return tile;
    }

    private List<QName> getAvailableRelationsList() {
        // todo fix focus parameter
        FocusType focus = null;
        try {
            focus = SecurityUtil.getPrincipal().getFocus();
        } catch (SecurityViolationException ex) {
            ex.printStackTrace();
        }
        Task task = getPageBase().createSimpleTask(OPERATION_LOAD_ASSIGNABLE_RELATIONS_LIST);
        OperationResult result = task.getResult();
        List<QName> assignableRelationsList = WebComponentUtil.getAssignableRelationsList(
                focus.asPrismObject(), RoleType.class, WebComponentUtil.AssignmentOrder.ASSIGNMENT, result, task, getPageBase());

        if (CollectionUtils.isEmpty(assignableRelationsList)) {
            return WebComponentUtil.getCategoryRelationChoices(AreaCategoryType.SELF_SERVICE, getPageBase());
        }

        return assignableRelationsList;
    }

    @Override
    public VisibleEnableBehaviour getNextBehaviour() {
        return new EnableBehaviour(() -> relations.getObject().stream().filter(t -> t.isSelected()).count() > 0);
    }

    @Override
    public boolean onNextPerformed(AjaxRequestTarget target) {
        Tile<QName> selected = relations.getObject().stream().filter(t -> t.isSelected()).findFirst().orElse(null);

        getModelObject().setRelation(selected.getValue());

        getWizard().next();
        target.add(getWizard().getPanel());

        return false;
    }

    private RelationSelectionType getRelationConfiguration() {
        CompiledGuiProfile profile = getPageBase().getCompiledGuiProfile();
        if (profile == null) {
            return null;
        }

        AccessRequestType accessRequest = profile.getAccessRequest();
        if (accessRequest == null) {
            return null;
        }

        return accessRequest.getRelationSelection();
    }
}

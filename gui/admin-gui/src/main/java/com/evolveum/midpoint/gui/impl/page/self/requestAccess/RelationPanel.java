/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.wizard.BasicWizardPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.util.EnableBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AreaCategoryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class RelationPanel extends BasicWizardPanel<RequestAccess> {

    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = RelationPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_ASSIGNABLE_RELATIONS_LIST = DOT_CLASS + "loadAssignableRelationsList";

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
        relations = Model.ofList(new ArrayList<>());
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

                    }
                };
                item.add(tp);
            }
        };
        listContainer.add(list);
    }

    private List<QName> getAvailableRelationsList() {
        List<QName> availableRelations = WebComponentUtil.getCategoryRelationChoices(AreaCategoryType.SELF_SERVICE, getPageBase());
        Task task = getPageBase().createSimpleTask(OPERATION_LOAD_ASSIGNABLE_RELATIONS_LIST);
        OperationResult result = task.getResult();
        List<QName> assignableRelationsList = WebComponentUtil.getAssignableRelationsList(
                null,//getTargetUser().asPrismObject(),
                RoleType.class,
                WebComponentUtil.AssignmentOrder.ASSIGNMENT, result, task, getPageBase());
        if (CollectionUtils.isEmpty(assignableRelationsList)) {
            return availableRelations;
        }
        return assignableRelationsList;
    }

    @Override
    public VisibleEnableBehaviour getNextBehaviour() {
        return new EnableBehaviour(() -> relations.getObject().stream().filter(t -> t.isSelected()).count() > 0);
    }

    @Override
    protected void onNextPerformed(AjaxRequestTarget target) {
        // todo save state

    }
}

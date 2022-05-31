/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.self.credentials;

import com.evolveum.midpoint.gui.api.component.form.CheckBoxPanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.ShadowWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.MultivalueContainerListPanel;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.SearchFactory;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.data.ISelectableDataProvider;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.util.ProjectionsListProvider;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.SessionStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import java.util.ArrayList;
import java.util.List;

public class PropagatePasswordPanel<F extends FocusType> extends ChangePasswordPanel<F> {

    private static final String ID_PROPAGATE_PASSWORD_CHECKBOX = "propagatePasswordCheckbox";
    private static final String ID_INDIVIDUAL_SYSTEMS_TABLE = "individualSystemsTable";

    private boolean propagatePassword = false;

    public PropagatePasswordPanel(String id, IModel<F> focusModel) {
        super(id, focusModel);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        CheckBoxPanel propagatePasswordCheckbox = new CheckBoxPanel(ID_PROPAGATE_PASSWORD_CHECKBOX, Model.of(propagatePassword),
                createStringResource("ChangePasswordPanel.changePasswordOnIndividualSystems"));
        propagatePasswordCheckbox.setOutputMarkupId(true);
        add(propagatePasswordCheckbox);

        MultivalueContainerListPanel<ShadowType> multivalueContainerListPanel =
                new MultivalueContainerListPanel<ShadowType>(ID_INDIVIDUAL_SYSTEMS_TABLE, ShadowType.class) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected ISelectableDataProvider<ShadowType, PrismContainerValueWrapper<ShadowType>> createProvider() {
                        return new ProjectionsListProvider(PropagatePasswordPanel.this, getSearchModel(), getShadowModel()) {
                            @Override
                            protected PageStorage getPageStorage() {
                                PageStorage storage = getSession().getSessionStorage().getPageStorageMap().get(SessionStorage.KEY_FOCUS_PROJECTION_TABLE);
                                if (storage == null) {
                                    storage = getSession().getSessionStorage().initPageStorage(SessionStorage.KEY_FOCUS_PROJECTION_TABLE);
                                }
                                return storage;
                            }
                        };
                    }

                    @Override
                    protected void editItemPerformed(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<ShadowType>> rowModel,
                            List<PrismContainerValueWrapper<ShadowType>> listItems) {
                    }

                    @Override
                    protected UserProfileStorage.TableId getTableId() {
                        return UserProfileStorage.TableId.FOCUS_PROJECTION_TABLE; //todo change
                    }

                    @Override
                    protected boolean isCreateNewObjectVisible() {
                        return false;
                    }

                    @Override
                    protected IModel<PrismContainerWrapper<ShadowType>> getContainerModel() {
                        return null;
                    }

                    @Override
                    protected List<IColumn<PrismContainerValueWrapper<ShadowType>, String>> createDefaultColumns() {
                        return new ArrayList<>();
                    }

                    @Override
                    protected IColumn<PrismContainerValueWrapper<ShadowType>, String> createNameColumn(IModel<String> displayModel, GuiObjectColumnType customColumn, ItemPath itemPath, ExpressionType expression) {
                        return new AbstractColumn<>(
                                createStringResource("ChangePasswordPanel.name")) {
                            @Override
                            public void populateItem(Item<ICellPopulator<PrismContainerValueWrapper<ShadowType>>> item, String componentId, IModel<PrismContainerValueWrapper<ShadowType>> shadowModel) {
                                item.add(new Label(componentId, Model.of(WebComponentUtil.getName(shadowModel.getObject().getRealValue()))));
                            }
                        };
                    }

                    @Override
                    protected IColumn<PrismContainerValueWrapper<ShadowType>, String> createCheckboxColumn() {
                        return new CheckBoxHeaderColumn<>();
                    }

                    @Override
                    protected Search createSearch(Class<ShadowType> type) {
                        Search search = SearchFactory.createProjectionsTabSearch(getPageBase());
//                        PropertySearchItem<Boolean> defaultDeadItem = search.findPropertySearchItem(ShadowType.F_DEAD);
//                        if (defaultDeadItem != null) {
//                            defaultDeadItem.setVisible(false);
//                        }
//                        addDeadSearchItem(search);
                        return search;
                    }

                };
        add(multivalueContainerListPanel);
    }

    private IModel<List<PrismContainerValueWrapper<ShadowType>>> getShadowModel() {
        return () -> {
            List<PrismContainerValueWrapper<ShadowType>> items = new ArrayList<>();
            Task task = getPageBase().createSimpleTask("loadAccounts");
            List<ShadowWrapper> shadowWrappers = WebComponentUtil.loadShadowWrapperList(getModelObject().asPrismObject(), null, task,
                    getPageBase());
            for (ShadowWrapper projection : shadowWrappers) {
                items.add(projection.getValue());
            }
            return items;
        };
    }
}

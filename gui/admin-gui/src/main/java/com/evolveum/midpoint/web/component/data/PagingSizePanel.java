/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data;

import java.util.Arrays;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.session.UserProfileStorage;

/**
 * Created by Viliam Repan (lazyman).
 */
public class PagingSizePanel extends BasePanel<Integer> {

    private static final long serialVersionUID = 1L;

    private static final String ID_SIZE = "size";

    private boolean small;

    public PagingSizePanel(String id) {
        super(id, null);

        initLayout();
    }

    @Override
    protected void onComponentTag(ComponentTag tag) {
        checkComponentTag(tag, "div");

        super.onComponentTag(tag);
    }

    private void initLayout() {
        setRenderBodyOnly(false);
        add(AttributeAppender.append("class", "d-flex flex-nowrap align-items-center paging-size"));

        DropDownChoice size = new DropDownChoice(ID_SIZE, createModel(),
                Model.ofList(getPagingSizes()));
        size.add(new AjaxFormComponentUpdatingBehavior("change") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                onPageSizeChangePerformed(target);
            }
        });
        size.add(AttributeAppender.append("class", () -> small ? "form-control-sm" : null));
        add(size);
    }

    protected void onPageSizeChangePerformed(AjaxRequestTarget target) {

    }

    protected List<Integer> getPagingSizes() {
        return Arrays.asList(UserProfileStorage.DEFAULT_PAGING_SIZES);
    }

    @Override
    public IModel<Integer> createModel() {
        return new IModel<>() {

            @Override
            public Integer getObject() {
                Table tablePanel = findParent(Table.class);
                UserProfileStorage.TableId tableId = tablePanel.getTableId();
                if (tableId == null || !tablePanel.enableSavePageSize()) {
                    return tablePanel.getItemsPerPage();
                }

                return getPageBase().getSessionStorage().getUserProfile().getPagingSize(tableId);
            }

            @Override
            public void setObject(Integer o) {
                Table tablePanel = findParent(Table.class);
                UserProfileStorage.TableId tableId = tablePanel.getTableId();
                if (o != null) {
                    if (tableId == null || !tablePanel.enableSavePageSize()) {
                        tablePanel.setItemsPerPage(o);
                        return;
                    }
                }

                getPageBase().getSessionStorage().getUserProfile().setPagingSize(tableId, o);
            }

            @Override
            public void detach() {
            }
        };
    }

    public boolean isSmall() {
        return small;
    }

    public void setSmall(boolean small) {
        this.small = small;
    }
}

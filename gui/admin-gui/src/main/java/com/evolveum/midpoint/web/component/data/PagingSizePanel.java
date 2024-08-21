/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data;

import java.util.ArrayList;
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
                Integer newValue = getModelObject();
                onPageSizeChangePerformed(newValue, target);
            }
        });
        size.add(AttributeAppender.append("class", () -> small ? "form-control-sm" : null));
        add(size);
    }

    protected void onPageSizeChangePerformed(Integer newValue, AjaxRequestTarget target) {

    }

    //TODO this is not entirely correct. If the getPagingSizes is overriden, the getConfiguredPagingSize is not called.
    protected List<Integer> getPagingSizes() {
        List<Integer> predefinedSizes = new ArrayList<>(Arrays.asList(UserProfileStorage.DEFAULT_PAGING_SIZES));
        Integer configuredSize = getConfiguredPagingSize();
        if (configuredSize != null && !predefinedSizes.contains(configuredSize)) {
            predefinedSizes.add(configuredSize);
        }
        return predefinedSizes.stream().sorted().toList();
    }

    protected Integer getConfiguredPagingSize() {
        return null;
    }

    @Override
    public IModel<Integer> createModel() {
        return new IModel<>() {

            @Override
            public Integer getObject() {
                Table tablePanel = findParent(Table.class);
                return tablePanel.getItemsPerPage();
            }

            @Override
            public void setObject(Integer o) {
                Table tablePanel = findParent(Table.class);
                if (o != null) {
                    tablePanel.setItemsPerPage(o);
                }
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

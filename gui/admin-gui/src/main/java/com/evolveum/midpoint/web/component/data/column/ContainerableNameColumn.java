/*
 * Copyright (c) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.data.column;

import java.io.Serial;

import org.apache.wicket.Component;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SelectableRow;
import com.evolveum.midpoint.web.page.admin.server.dto.OperationResultStatusPresentationProperties;
import com.evolveum.midpoint.web.page.error.PageOperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiObjectColumnType;

/**
 * Created by Viliam Repan (lazyman).
 */
public abstract class ContainerableNameColumn<SR extends SelectableRow<C>, C extends Containerable> extends AbstractNameColumn<SR, C> {
    @Serial private static final long serialVersionUID = 1L;

    public ContainerableNameColumn(IModel<String> displayModel, String sortProperty, GuiObjectColumnType customColumn, ExpressionType expression, PageBase pageBase) {
        super(displayModel, sortProperty, customColumn, expression, pageBase);
    }

    @Override
    public void populateItem(final Item<ICellPopulator<SR>> cellItem, String componentId,
            final IModel<SR> rowModel) {

        IModel<String> labelModel = getDataModel(rowModel);

        cellItem.add(createComponent(componentId, labelModel, rowModel));
    }

    protected Component createComponent(String componentId, IModel<String> labelModel, IModel<SR> rowModel) {
        return new AjaxLinkPanel(componentId, labelModel) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                onClickPerformed(target, rowModel);
            }

            @Override
            public boolean isEnabled() {
                return ContainerableNameColumn.this.isClickable(rowModel);
            }
        };
    }

    protected void onClickPerformed(AjaxRequestTarget target, IModel<SR> rowModel) {
        SR selectableBean = rowModel.getObject();
        C value = ColumnUtils.unwrapSelectableRowModel(rowModel);
        if (value == null || resultPresent(selectableBean)) {
            redirectToResultPage(selectableBean);
        } else {
            ContainerableNameColumn.this.onClick(target, rowModel);
        }
    }

    private OperationResult getResult(SR selectableBean) {
        if (!(selectableBean instanceof SelectableBean)) {
            return null;
        }
        return ((SelectableBean<?>) selectableBean).getResult();
    }

    private boolean resultPresent(SR selectableBean) {
        return getResult(selectableBean) != null;
    }

    private void redirectToResultPage(SR selectableBean) {
        throw new RestartResponseException(new PageOperationResult(getResult(selectableBean)));
    }

    private String getResultAsString(SR selectableBean) {
        if (selectableBean instanceof SelectableBean) {
            OperationResult result = ((SelectableBean<?>) selectableBean).getResult();
            getPageBase().showResult(result);
            OperationResultStatusPresentationProperties props = OperationResultStatusPresentationProperties.parseOperationalResultStatus(result.getStatus());
            return getPageBase().getString(props.getStatusLabelKey());
        }
        return "";
    }

    private String getName(SR selectableBean) {
        IModel<String> containerName = getContainerName(selectableBean);

        String result = "";
        if (resultPresent(selectableBean)) {
            result = " (" + getResult(selectableBean).getStatus() + ")";
        }

        return containerName.getObject() + result;
    }

    public boolean isClickable(IModel<SR> rowModel) {
        return true;
    }

    public void onClick(AjaxRequestTarget target, IModel<SR> rowModel) {
    }

    @Override
    protected String handleDefaultValue(SR selectableBean) {
        return getName(selectableBean);
    }

    @Override
    protected String handleNullRowValue(SR selectableBean) {
        return getResultAsString(selectableBean);
    }

    protected abstract IModel<String> getContainerName(SR rowModel);
}

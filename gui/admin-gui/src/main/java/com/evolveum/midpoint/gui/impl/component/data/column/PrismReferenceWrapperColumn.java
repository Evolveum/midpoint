/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.data.column;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;

import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismReferenceHeaderPanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismReferenceWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.model.PrismReferenceWrapperHeaderModel;
import com.evolveum.midpoint.web.model.PrismReferenceWrapperModel;

/**
 * @author skublik
 */
public class PrismReferenceWrapperColumn<C extends Containerable, R extends Referencable> extends AbstractItemWrapperColumn<C, PrismValueWrapper<R>> {

    private static final long serialVersionUID = 1L;

    private static final String ID_HEADER = "header";
    private static final String ID_INPUT = "input";

    private PageBase pageBase;

    public PrismReferenceWrapperColumn(IModel<? extends PrismContainerDefinition<C>> mainModel, ItemPath itemName, ColumnType columnType, PageBase pageBase) {
        super(mainModel, itemName, columnType);
        this.pageBase = pageBase;
    }


    @Override
    public PrismReferenceWrapperModel<C, R> getDataModel(IModel<PrismContainerValueWrapper<C>> rowModel) {
        return PrismReferenceWrapperModel.fromContainerValueWrapper(rowModel, itemName);
    }

    @Override
    protected Component createHeader(String componentId, IModel<? extends PrismContainerDefinition<C>> mainModel) {
        return new PrismReferenceHeaderPanel<R>(componentId, new PrismReferenceWrapperHeaderModel(mainModel, itemName, pageBase)) {
            @Override
            protected boolean isAddButtonVisible() {
                return false;
            }

            @Override
            protected boolean isButtonEnabled() {
                return false;
            }
        };
    }


    @Override
    protected <IW extends ItemWrapper> Component createColumnPanel(String componentId, IModel<IW> rowModel) {
        return new PrismReferenceWrapperColumnPanel<R>(componentId, (IModel<PrismReferenceWrapper<R>>)rowModel, getColumnType()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onClick(AjaxRequestTarget target, PrismContainerValueWrapper<?> rowModel) {
                PrismReferenceWrapperColumn.this.onClick(target, (IModel) Model.of(rowModel));
            }

            @Override
            protected boolean isClickEnabled() {
                return PrismReferenceWrapperColumn.this.isClickEnabled((IModel<PrismReferenceWrapper<R>>)rowModel);
            }
        };
    }

    protected void onClick(AjaxRequestTarget target, IModel<PrismContainerValueWrapper<C>> model) {
        PrismReferenceWrapperModel<C, R> refModel = getDataModel(model);
        PrismReferenceWrapper<R> ref = refModel.getObject();
        if (ref != null) {
            try {
                DetailsPageUtil.dispatchToObjectDetailsPage(ref.getItem().getValue(), pageBase, true);

            } catch (Exception e) {
                pageBase.error("Cannot determine details page for " + ref.getItem().getValue());
                target.add(pageBase.getFeedbackPanel());
            }
        }
    }

    public boolean isClickEnabled(IModel<PrismReferenceWrapper<R>> rowModel) {
        PrismReferenceWrapper<R> ref = rowModel.getObject();
        if (ref != null) {
            Referencable referencable = ref.getItem().getRealValue();
            if (referencable != null) {
                Class targetClass = WebComponentUtil.qnameToClass(pageBase.getPrismContext(), referencable.getType());
                return WebComponentUtil.isAuthorized(targetClass);
            }
        }
        return false;
    }
}



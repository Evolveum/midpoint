/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.input.expression;

import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.impl.component.wizard.collapse.CollapsedItem;
import com.evolveum.midpoint.gui.impl.component.wizard.collapse.DrawerFooterPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.collapse.DrawerModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;

/**
 * Drawer of a script expression.
 */
public abstract class ScriptExpressionDrawerModel extends DrawerModel {

    /**
     * Copy the drawer works with, kept until the user applies it or closes the drawer
     */
    private final IModel<ExpressionType> editedExpression;

    public ScriptExpressionDrawerModel(
            IModel<List<CollapsedItem<DrawerModel>>> collapsedItemsModel, IModel<ExpressionType> editedExpression) {
        super(collapsedItemsModel);
        this.editedExpression = editedExpression;
    }

    @Override
    public Component getFooter(String id, DrawerModel model) {
        return new DrawerFooterPanel(id) {

            @Override
            protected IModel<String> createNoLabel() {
                return createStringResource("ScriptExpressionDrawerModel.close");
            }

            @Override
            protected IModel<String> createYesLabel() {
                return createStringResource("ScriptExpressionDrawerModel.apply");
            }

            @Override
            public void noPerformed(AjaxRequestTarget target) {
                closePerformed(target);
            }

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                storePerformed(editedExpression.getObject(), target);
            }
        };
    }

    /**
     * Writes the edited copy into the expression of the item.
     *
     * @param expression edited copy of the expression, an empty one removes it from the item.
     * @param target target of the ajax request.
     */
    protected abstract void storePerformed(ExpressionType expression, AjaxRequestTarget target);

    /**
     * Closes the drawer and leaves the expression as it was.
     *
     * @param target target of the ajax request.
     */
    protected abstract void closePerformed(AjaxRequestTarget target);
}

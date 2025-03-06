/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.menu.cog;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

/**
 * @author lazyman
 */
public class MenuLinkPanel<I extends InlineMenuItem> extends BasePanel<I> {

    private static final String ID_MENU_ITEM_LINK = "menuItemLink";
    private static final String ID_MENU_ITEM_LABEL = "menuItemLabel";

    public MenuLinkPanel(String id, IModel<I> item) {
        super(id, item);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        I dto = getModelObject();

        AbstractLink a;
        if (dto.isSubmit()) {
            a = new AjaxSubmitLink(ID_MENU_ITEM_LINK) {

                @Override
                protected void onSubmit(AjaxRequestTarget target) {
                    MenuLinkPanel.this.onSubmit(target, getModelObject().getAction(), getModel());
                }

                @Override
                protected void onError(AjaxRequestTarget target) {
                    MenuLinkPanel.this.onError(target, getModelObject().getAction());
                }

                @Override
                protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                    super.updateAjaxAttributes(attributes);
                    attributes.setEventPropagation(AjaxRequestAttributes.EventPropagation.BUBBLE);
                }
            };
        } else {
            a = new AjaxLink<Void>(ID_MENU_ITEM_LINK) {

                @Override
                public void onClick(AjaxRequestTarget target) {
                    MenuLinkPanel.this.onClick(target, MenuLinkPanel.this.getModelObject().getAction(), MenuLinkPanel.this.getModel());
                }

                @Override
                protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                    super.updateAjaxAttributes(attributes);
                    attributes.setEventPropagation(AjaxRequestAttributes.EventPropagation.BUBBLE);
                }
            };
        }
        add(a);

        a.add(new VisibleEnableBehaviour() {

            @Override
            public boolean isVisible() {
                return getModelObject().getAction() != null;
            }
        });

        Label span = new Label(ID_MENU_ITEM_LABEL, dto.getLabel());
        span.setRenderBodyOnly(true);
        a.add(span);
    }

    protected void onSubmit(AjaxRequestTarget target, InlineMenuItemAction action, IModel<I> item) {
        if (action != null) {
            if (item.getObject().showConfirmationDialog() && item.getObject().getConfirmationMessageModel() != null) {
                showConfirmationPopup(item.getObject(), target);
            } else {
                action.onSubmit(target);
            }
        }
    }

    protected void onError(AjaxRequestTarget target, InlineMenuItemAction action) {
        if (action != null) {
            action.onError(target);
        }
    }

    protected void onClick(AjaxRequestTarget target, InlineMenuItemAction action, IModel<I> item) {
        if (action != null) {
            if (item.getObject().showConfirmationDialog() && item.getObject().getConfirmationMessageModel() != null) {
                showConfirmationPopup(item.getObject(), target);
            } else {
                action.onClick(target);
            }
        }
    }

    private void showConfirmationPopup(I menuItem, AjaxRequestTarget target) {
        ConfirmationPanel dialog = new ConfirmationPanel(((PageBase)getPage()).getMainPopupBodyId(),
                menuItem.getConfirmationMessageModel()) {
            private static final long serialVersionUID = 1L;

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
                menuItem.getAction().onClick(target);
            }

            @Override
            public String getCssClassForDialog() {
                return "mt-popup-under-header";
            }
        };
        ((PageBase)getPage()).showMainPopup(dialog, target);
    }

    protected final AbstractLink getLinkContainer() {
        return (AbstractLink) get(ID_MENU_ITEM_LINK);
    }

}

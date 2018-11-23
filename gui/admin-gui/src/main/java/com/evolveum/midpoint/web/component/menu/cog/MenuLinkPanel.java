/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.web.component.menu.cog;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.web.component.dialog.ConfirmationPanel;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.attributes.AjaxRequestAttributes;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import static com.evolveum.midpoint.web.component.data.column.ColumnUtils.createStringResource;

/**
 * @author lazyman
 */
public class MenuLinkPanel extends Panel {

    private static String ID_MENU_ITEM_LINK = "menuItemLink";
    private static String ID_MENU_ITEM_LABEL = "menuItemLabel";

    public MenuLinkPanel(String id, IModel<InlineMenuItem> item) {
        super(id);

        initLayout(item);
    }

    private void initLayout(IModel<InlineMenuItem> item) {
        InlineMenuItem dto = item.getObject();

        AbstractLink a;
        if (dto.isSubmit()) {
            a = new AjaxSubmitLink(ID_MENU_ITEM_LINK) {

                @Override
                protected void onSubmit(AjaxRequestTarget target) {
                    MenuLinkPanel.this.onSubmit(target, dto.getAction(), item);
                }

                @Override
                protected void onError(AjaxRequestTarget target) {
                    MenuLinkPanel.this.onError(target, dto.getAction());
                }

                @Override
                protected void updateAjaxAttributes(AjaxRequestAttributes attributes) {
                    super.updateAjaxAttributes(attributes);
                    attributes.setEventPropagation(AjaxRequestAttributes.EventPropagation.BUBBLE);
                }
            };
        } else {
            a = new AjaxLink(ID_MENU_ITEM_LINK) {

                @Override
                public void onClick(AjaxRequestTarget target) {
                    MenuLinkPanel.this.onClick(target, dto.getAction(), item);
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
                if (dto.getAction() == null) {
                    return false;
                }
                return true;
            }
        });

        Label span = new Label(ID_MENU_ITEM_LABEL, dto.getLabel());
        span.setRenderBodyOnly(true);
        a.add(span);
    }

    protected void onSubmit(AjaxRequestTarget target, InlineMenuItemAction action, IModel<InlineMenuItem> item) {
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

    protected void onClick(AjaxRequestTarget target, InlineMenuItemAction action, IModel<InlineMenuItem> item) {
        if (action != null) {
            if (item.getObject().showConfirmationDialog() && item.getObject().getConfirmationMessageModel() != null) {
                showConfirmationPopup(item.getObject(), target);
            } else {
                action.onClick(target);
            }
        }
    }

    private void showConfirmationPopup(InlineMenuItem menuItem, AjaxRequestTarget target) {
        ConfirmationPanel dialog = new ConfirmationPanel(((PageBase)getPage()).getMainPopupBodyId(),
                menuItem.getConfirmationMessageModel()) {
            private static final long serialVersionUID = 1L;

            @Override
            public StringResourceModel getTitle() {
                return createStringResource("pageUsers.message.confirmActionPopupTitle");
            }

            @Override
            public void yesPerformed(AjaxRequestTarget target) {
            	menuItem.getAction().onClick(target);
            }
        };
        ((PageBase)getPage()).showMainPopup(dialog, target);
    }
}

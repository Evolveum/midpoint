/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard.schemaHandling.objectType.attribute.volatilityMultivalue;

import com.evolveum.midpoint.gui.api.component.wizard.TileEnum;
import com.evolveum.midpoint.gui.impl.page.admin.EnumChoicePanel;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowItemVolatilityType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.web.component.dialog.Popupable;

/**
 * Popup for selecting of direction for created volatility container.
 * Now not use but prepare when incoming and outgoing attributes of attribute's volatility change to multivalue containers.
 */
public class VolatilityCreationPopup extends BasePanel implements Popupable {

    public enum VolatilityDirection implements TileEnum {

        INCOMING("fa fa-arrow-right-to-bracket", ShadowItemVolatilityType.F_INCOMING),

        OUTGOING("fa fa-arrow-right-from-bracket", ShadowItemVolatilityType.F_OUTGOING);

        ItemName path;
        String icon;

        VolatilityDirection(String icon, ItemName path) {
            this.icon = icon;
            this.path = path;
        }

        public ItemName getPath() {
            return path;
        }

        @Override
        public String getIcon() {
            return icon;
        }
    }

    private static final String ID_TEMPLATE_CHOICE_PANEL = "templateChoicePanel";
    private static final String ID_BUTTONS = "buttons";
    private static final String ID_CLOSE = "close";

    private Fragment footer;

    public VolatilityCreationPopup(String id) {
        super(id);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
        initFooter();
    }

    private void initLayout() {
        add(createChoicePanel(ID_TEMPLATE_CHOICE_PANEL));
    }

    private EnumChoicePanel<VolatilityDirection> createChoicePanel(String id){
        return new EnumChoicePanel<>(id, VolatilityDirection.class) {
            @Override
            protected IModel<String> getTextModel() {
                return createStringResource("VolatilityCreationPopup.choicePanel.text");
            }

            @Override
            protected IModel<String> getSubTextModel() {
                return createStringResource("VolatilityCreationPopup.choicePanel.subtext");
            }

            @Override
            protected void onTemplateChosePerformed(VolatilityDirection view, AjaxRequestTarget target) {
                onVolatilityPerformed(view.getPath(), target);
                getPageBase().hideMainPopup(target);
            }
        };
    }

    protected void onVolatilityPerformed(ItemName path, AjaxRequestTarget target) {

    }

    private void initFooter() {
        footer = new Fragment(Popupable.ID_FOOTER, ID_BUTTONS, this);
        footer.add(new AjaxLink<>(ID_CLOSE) {
            @Override
            public void onClick(AjaxRequestTarget target) {
                getPageBase().hideMainPopup(target);
            }
        });

    }

    @Override
    public int getWidth() {
        return 60;
    }

    @Override
    public int getHeight() {
        return 50;
    }

    @Override
    public String getWidthUnit() {
        return "%";
    }

    @Override
    public String getHeightUnit() {
        return "%";
    }

    @Override
    public IModel<String> getTitle() {
        return null;
    }

    @Override
    public Component getContent() {
        return VolatilityCreationPopup.this;
    }

    @Override
    public @NotNull Component getFooter() {
        return footer;
    }
}

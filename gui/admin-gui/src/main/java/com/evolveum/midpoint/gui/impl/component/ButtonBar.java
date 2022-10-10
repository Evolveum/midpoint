/*
 * Copyright (c) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component;

import com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.util.SelectableRow;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.*;

import java.util.*;

/**
 * @param <C> the container of displayed objects in table
 * @param <PO> the type of the object processed by provider
 * @author skublik
 *
 * Abstract class for List panels with table.
 */
public class ButtonBar<C extends Containerable, PO extends SelectableRow> extends Fragment {
    private static final long serialVersionUID = 1L;

//    private PreviewContainerPanelConfigurationType previewConfig;
    private static final String ID_BUTTON_REPEATER = "buttonsRepeater";
    private static final String ID_BUTTON = "button";

    public ButtonBar(String id, String markupId, Panel markupProvider, PreviewContainerPanelConfigurationType previewContainerPanelConfigurationType) {
        super(id, markupId, markupProvider);

        List<Component> buttonsList = createNavigationButtons(ID_BUTTON, previewContainerPanelConfigurationType);
        initLayout(buttonsList);
    }

    public ButtonBar(String id, String markupId, Panel markupProvider, List<Component> buttonsList) {
        super(id, markupId, markupProvider);

        initLayout(buttonsList);
    }

    private void initLayout(final List<Component> buttonsList) {
        ListView<Component> buttonsView = new ListView<>(ID_BUTTON_REPEATER, Model.ofList(buttonsList)) {
            @Override
            protected void populateItem(ListItem<Component> listItem) {
                listItem.add(listItem.getModelObject());
            }
        };
        add(buttonsView);
    }

    private List<Component> createNavigationButtons(String idButton, PreviewContainerPanelConfigurationType previewConfig) {
        List<Component> buttonsList = new ArrayList<>();
        for (GuiActionType action : previewConfig.getAction()) {
            AjaxIconButton button = createViewAllButton(idButton, action);
            buttonsList.add(button);
        }

        return buttonsList;
    }

    private AjaxIconButton createViewAllButton(String buttonId, GuiActionType action) {
        DisplayType displayType = action.getDisplay();
        AjaxIconButton viewAll = new AjaxIconButton(buttonId, new Model<>(GuiDisplayTypeUtil.getIconCssClass(displayType)),
                createButtonLabel(displayType)) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                viewAllActionPerformed(target, action);
            }
        };
        viewAll.add(new VisibleBehaviour(() -> WebComponentUtil.getElementVisibility(action.getVisibility())));
        viewAll.add(AttributeAppender.append("class", "btn btn-info btn-sm mr-2"));
        viewAll.showTitleAsLabel(true);
        return viewAll;
    }

    private IModel<String> createButtonLabel(DisplayType displayType) {
        return () -> {
            if (displayType == null) {
                return "N/A";
            }
            PolyStringType label = GuiDisplayTypeUtil.getLabel(displayType);
            if (label == null) {
                return "N/A";
            }
            return WebComponentUtil.getTranslatedPolyString(label);
        };
    }

    protected void viewAllActionPerformed(AjaxRequestTarget target, GuiActionType action) {
        WebComponentUtil.redirectFromDashboardWidget(action, WebComponentUtil.getPageBase(this), this);
    }
}

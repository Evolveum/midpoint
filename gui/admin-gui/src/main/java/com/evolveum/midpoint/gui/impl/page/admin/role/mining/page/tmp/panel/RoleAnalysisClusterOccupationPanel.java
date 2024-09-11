/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel;

import java.io.Serial;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;

import com.evolveum.midpoint.gui.api.component.BasePanel;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.CLASS_CSS;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.STYLE_CSS;

public class RoleAnalysisClusterOccupationPanel extends BasePanel<String> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_SEPARATOR = "separator";
    private static final String ID_FIRST_PANEL = "firstPanel";
    private static final String ID_SECOND_PANEL = "secondPanel";

    public RoleAnalysisClusterOccupationPanel(String id) {
        super(id);
        initLayout();
    }

    private void initLayout() {
        this.add(AttributeModifier.append(CLASS_CSS, getComponentCssClass()));
        Component firstPanel = createFirstPanel(ID_FIRST_PANEL);
        firstPanel.add(AttributeModifier.append(STYLE_CSS, getStyleForFirstPanel()));
        firstPanel.setOutputMarkupId(true);
        add(firstPanel);

        Component separatorPanel = createSeparatorPanel(ID_SEPARATOR);
        separatorPanel.setOutputMarkupId(true);
        add(separatorPanel);

        Component secondPanel = createSecondPanel(ID_SECOND_PANEL);
        secondPanel.add(AttributeModifier.append(STYLE_CSS, getStyleForSecondPanel()));
        secondPanel.setOutputMarkupId(true);
        add(secondPanel);
    }

    protected String getStyleForFirstPanel() {
        return "";
    }

    protected String getStyleForSecondPanel() {
        return "";
    }

    public Component createFirstPanel(String idFirstPanel) {
        return new WebMarkupContainer(idFirstPanel);
    }

    public Component createSecondPanel(String idFirstPanel) {
        return new WebMarkupContainer(idFirstPanel);
    }

    public Component createSeparatorPanel(String idSeparatorPanel) {
        return new WebMarkupContainer(idSeparatorPanel);
    }

    public String getComponentCssClass() {
        return "";
    }
}

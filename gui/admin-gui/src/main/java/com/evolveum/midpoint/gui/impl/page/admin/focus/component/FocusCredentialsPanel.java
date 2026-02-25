/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.focus.component;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.otp.OtpListPanel;
import com.evolveum.midpoint.gui.api.component.tabs.PanelTab;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.focus.FocusDetailsModels;
import com.evolveum.midpoint.gui.impl.prism.panel.SingleContainerPanel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.TabbedPanel;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@SuppressWarnings("unused")
@PanelType(name = "credentials")
@PanelInstance(
        identifier = "credentials",
        applicableForType = FocusType.class,
        display = @PanelDisplay(
                label = "FocusCredentialsPanel.panel.name",
                icon = GuiStyleConstants.CLASS_PASSWORD_ICON,
                order = 56
        ),
        containerPath = "credentials",
        type = "CredentialsType",
        expanded = true
)
public class FocusCredentialsPanel<F extends FocusType, FDM extends FocusDetailsModels<F>> extends AbstractObjectMainPanel<F, FDM> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_TAB_PANEL = "tabPanel";

    public FocusCredentialsPanel(
            String id,
            FDM objectModel,
            ContainerPanelConfigurationType configurationType) {
        super(id, objectModel, configurationType);
    }

    @Override
    protected void initLayout() {
        TabbedPanel<?> tabPanel = new TabbedPanel<>(ID_TAB_PANEL, createTabs());
        add(tabPanel);
    }

    private List<ITab> createTabs() {
        List<ITab> tabs = new ArrayList<>();

        // todo fix authorization. previously there was panel for password with
        //  custom name (identifier) and details menu and could be hidden via gui panels
        tabs.add(createPasswordTab());
        tabs.add(createOtpsTab());

        return tabs;
    }

    private ITab createPasswordTab() {
        return new PanelTab(createStringResource("FocusCredentialsPanel.tab.password")) {

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return new SingleContainerPanel<>(panelId,
                        PrismContainerWrapperModel.fromContainerWrapper(
                                getObjectWrapperModel(), ItemPath.create(FocusType.F_CREDENTIALS, CredentialsType.F_PASSWORD)),
                        PasswordType.COMPLEX_TYPE);
            }
        };
    }

    private ITab createOtpsTab() {
        return new PanelTab(createStringResource("FocusCredentialsPanel.tab.otp")) {

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                PrismContainerWrapperModel<F, OtpCredentialType> model =
                        PrismContainerWrapperModel.fromContainerWrapper(
                                getObjectWrapperModel(),
                                ItemPath.create(FocusType.F_CREDENTIALS, CredentialsType.F_OTPS, OtpCredentialsType.F_TOTP),
                                () ->
                                        getPageBase());

                return new OtpListPanel(panelId, () -> getObjectDetailsModels().getObjectType(), model, null);
            }
        };
    }
}

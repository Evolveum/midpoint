/*
 * Copyright (c) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.focus.component;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.authentication.api.OtpManager;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;

import com.evolveum.midpoint.web.security.MidPointApplication;

import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;

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
import com.evolveum.midpoint.web.component.util.SerializableFunction;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@SuppressWarnings("unused")
@PanelType(name = "credentials")
//@PanelInstance(
//        identifier = "credentials",
//        applicableForType = FocusType.class,
//        display = @PanelDisplay(
//                label = "FocusCredentialsPanel.panel.name",
//                icon = GuiStyleConstants.CLASS_PASSWORD_ICON,
//                order = 56
//        ),
//        containerPath = "credentials",
//        type = "CredentialsType",
//        expanded = true
//)
public class FocusCredentialsPanel<F extends FocusType, FDM extends FocusDetailsModels<F>> extends AbstractObjectMainPanel<F, FDM> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_CONTENT = "content";

    public FocusCredentialsPanel(
            String id,
            FDM objectModel,
            ContainerPanelConfigurationType configurationType) {
        super(id, objectModel, configurationType);
    }

    @Override
    protected void initLayout() {
        List<ITab> tabs = createTabs();

        if (tabs.size() == 1) {
            WebMarkupContainer panel = tabs.get(0).getPanel(ID_CONTENT);
            add(panel);
        } else {
            add(new TabbedPanel<>(ID_CONTENT, tabs));
        }
    }

    private List<ITab> createTabs() {
        List<ITab> tabs = new ArrayList<>();

        // todo fix authorization. previously there was panel for password with
        //  custom name (identifier) and details menu and could be hidden via gui panels
        tabs.add(
                createTab(
                        createStringResource("FocusCredentialsPanel.tab.password"),
                        new VisibleBehaviour(() -> true),
                        id -> createPasswordPanel(id)));
        tabs.add(
                createTab(
                        createStringResource("FocusCredentialsPanel.tab.otp"),
                        new VisibleBehaviour(() -> isOtpConfigured()),
                        id -> createOtpsPanel(id)));

        return tabs;
    }

    private boolean isOtpConfigured() {
        FocusType focus = getObjectDetailsModels().getObjectType();

        Task task =  getPageBase().createSimpleTask("checkOtpConfiguration");
        OperationResult result = task.getResult();

        OtpManager manager = MidPointApplication.get().getOtpManager();
        return manager.isOtpAvailable(focus.asPrismObject(), task, result);
    }

    private ITab createTab(IModel<String> title, VisibleBehaviour visible, SerializableFunction<String, WebMarkupContainer> panelSupplier) {
        return new PanelTab(title, visible) {

            @Override
            public WebMarkupContainer createPanel(String panelId) {
                return panelSupplier.apply(panelId);
            }
        };
    }

    private WebMarkupContainer createPasswordPanel(String panelId) {
        return new SingleContainerPanel<>(panelId,
                PrismContainerWrapperModel.fromContainerWrapper(
                        getObjectWrapperModel(), ItemPath.create(FocusType.F_CREDENTIALS, CredentialsType.F_PASSWORD)),
                PasswordType.COMPLEX_TYPE);
    }

    private WebMarkupContainer createOtpsPanel(String panelId) {
        PrismContainerWrapperModel<F, OtpCredentialType> model =
                PrismContainerWrapperModel.fromContainerWrapper(
                        getObjectWrapperModel(),
                        ItemPath.create(FocusType.F_CREDENTIALS, CredentialsType.F_OTPS, OtpCredentialsType.F_TOTP),
                        () -> getPageBase());

        return new OtpListPanel(panelId, () -> getObjectDetailsModels().getObjectType(), model, null);
    }
}

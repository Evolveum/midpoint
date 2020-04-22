/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.objectdetails;

import java.util.Collection;
import java.util.Collections;

import com.evolveum.midpoint.gui.impl.prism.panel.ItemHeaderPanel;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.panel.Panel;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.page.admin.server.RefreshableTabPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author semancik
 */
public class AssignmentHolderTypeDetailsTabPanel<AHT extends AssignmentHolderType> extends AbstractObjectTabPanel<AHT> implements RefreshableTabPanel {
    private static final long serialVersionUID = 1L;

    private static final String ID_MAIN_PANEL = "main";
    private static final String ID_ACTIVATION_PANEL = "activation";
    private static final String ID_PASSWORD_PANEL = "password";

    private static final Trace LOGGER = TraceManager.getTrace(AssignmentHolderTypeDetailsTabPanel.class);

    public AssignmentHolderTypeDetailsTabPanel(String id, Form<PrismObjectWrapper<AHT>> mainForm,
            LoadableModel<PrismObjectWrapper<AHT>> focusWrapperModel) {
        super(id, mainForm, focusWrapperModel);

    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {

        try {

            ItemHeaderPanel.ItemPanelSettingsBuilder builder = new ItemHeaderPanel.ItemPanelSettingsBuilder().visibilityHandler(w -> ItemVisibility.AUTO).showOnTopLevel(true);
            builder.headerVisibility(false);

            Panel main = getPageBase().initItemPanel(ID_MAIN_PANEL, getObjectWrapper().getTypeName(),
                    PrismContainerWrapperModel.fromContainerWrapper(getObjectWrapperModel(), ItemPath.EMPTY_PATH), builder.build());
            add(main);
            Panel activation = getPageBase().initItemPanel(ID_ACTIVATION_PANEL, ActivationType.COMPLEX_TYPE,
                    PrismContainerWrapperModel.fromContainerWrapper(getObjectWrapperModel(), FocusType.F_ACTIVATION), builder.build());
            add(activation);
            Panel password = getPageBase().initItemPanel(ID_PASSWORD_PANEL, PasswordType.COMPLEX_TYPE,
                    PrismContainerWrapperModel.fromContainerWrapper(getObjectWrapperModel(), ItemPath.create(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD)), builder.build());
            add(password);
        } catch (SchemaException e) {
            LOGGER.error("Could not create focus details panel. Reason: {}", e.getMessage(), e);
        }
    }

    @Override
    public Collection<Component> getComponentsToUpdate() {
        getObjectWrapperModel().reset();
        return Collections.singleton(this);
    }
}

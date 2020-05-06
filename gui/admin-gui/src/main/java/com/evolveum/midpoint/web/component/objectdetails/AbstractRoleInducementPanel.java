/*
 * Copyright (c) 2010-2017 Evolveum and contributors

 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.objectdetails;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.web.component.assignment.InducementsPanel;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;

/**
 * Created by honchar
 */
public class AbstractRoleInducementPanel<R extends AbstractRoleType> extends AbstractObjectTabPanel<R> {

    private static final String ID_INDUCEMENT_PANEL = "inducementPanel";

    public AbstractRoleInducementPanel(String id, Form mainForm, LoadableModel<PrismObjectWrapper<R>> focusWrapperModel,
                                    PageBase page) {
        super(id, mainForm, focusWrapperModel);
        initLayout();
    }

    private void initLayout() {
        InducementsPanel inducementsPanel = new InducementsPanel(ID_INDUCEMENT_PANEL,
                PrismContainerWrapperModel.fromContainerWrapper(getObjectWrapperModel(), AbstractRoleType.F_INDUCEMENT));
//                new ContainerWrapperFromObjectWrapperModel<>(getObjectWrapperModel(), AbstractRoleType.F_INDUCEMENT));

        add(inducementsPanel);
    }
}

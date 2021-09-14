/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.abstractrole.component;

import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.midpoint.web.application.*;

//@PanelType(name = "inducements", defaultContainerPath = "inducement")
@PanelInstance(identifier = "inducements", applicableForType = AbstractRoleType.class,
        display = @PanelDisplay(label = "FocusType.inducement", order = 70))
@Counter(provider = InducementCounter.class)
public class AbstractRoleInducementPanel<AR extends AbstractRoleType> extends AbstractObjectMainPanel<AR, ObjectDetailsModels<AR>> {

    public AbstractRoleInducementPanel(String id, ObjectDetailsModels<AR> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {

    }


}

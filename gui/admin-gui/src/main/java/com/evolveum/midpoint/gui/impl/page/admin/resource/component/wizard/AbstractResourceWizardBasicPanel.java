/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.resource.component.wizard;

import com.evolveum.midpoint.gui.impl.component.wizard.AbstractVerifiableWizardBasicPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.AbstractWizardBasicPanel;
import com.evolveum.midpoint.gui.impl.component.wizard.WizardPanelHelper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.resource.ResourceDetailsModel;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.schema.result.OperationResult;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.IModel;

/**
 * @author lskublik
 */
public abstract class AbstractResourceWizardBasicPanel<C extends Containerable> extends AbstractVerifiableWizardBasicPanel<C, ResourceDetailsModel> {

    public AbstractResourceWizardBasicPanel(
            String id,
            WizardPanelHelper<C, ResourceDetailsModel> superHelper) {
        super(id, superHelper);
    }
}

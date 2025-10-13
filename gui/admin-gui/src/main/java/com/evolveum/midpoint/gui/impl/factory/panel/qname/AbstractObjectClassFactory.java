/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.factory.panel.qname;

import com.evolveum.midpoint.gui.api.component.autocomplete.AutoCompleteQNamePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.factory.panel.AbstractInputGuiComponentFactory;
import com.evolveum.midpoint.gui.impl.factory.panel.PrismPropertyPanelContext;
import com.evolveum.midpoint.gui.impl.util.ProvisioningObjectsUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;

//FIXME serializable
public abstract class AbstractObjectClassFactory extends AbstractInputGuiComponentFactory<QName> implements Serializable {

    @Override
    protected InputPanel getPanel(PrismPropertyPanelContext<QName> panelCtx) {
        AutoCompleteQNamePanel<QName> panel = new AutoCompleteQNamePanel<>(panelCtx.getComponentId(), panelCtx.getRealValueModel()) {

            @Override
            public Collection<QName> loadChoices() {
                PrismPropertyWrapper<QName> itemWrapper = panelCtx.unwrapWrapperModel();
                PrismObject<ResourceType> resourceType = WebComponentUtil.findResource(itemWrapper, panelCtx);

                if (resourceType == null) {
                    return Collections.emptyList();
                }
                return ProvisioningObjectsUtil.loadResourceObjectClassValues(resourceType.asObjectable(), panelCtx.getPageBase());
            }

            @Override
            protected boolean alwaysReload() {
                return true;
            }
        };
        return panel;
    }

    @Override
    public Integer getOrder() {
        return 10000 - 10;
    }
}

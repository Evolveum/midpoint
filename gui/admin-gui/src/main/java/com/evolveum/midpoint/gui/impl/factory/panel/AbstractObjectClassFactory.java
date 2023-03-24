/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import com.evolveum.midpoint.gui.api.component.autocomplete.AutoCompleteQNamePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.prism.InputPanel;
import com.evolveum.midpoint.web.page.admin.configuration.component.EmptyOnBlurAjaxFormUpdatingBehaviour;
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
                return WebComponentUtil.loadResourceObjectClassValues(resourceType.asObjectable(), panelCtx.getPageBase());
            }

            @Override
            protected boolean alwaysReload() {
                return true;
            }
        };
        panel.getBaseFormComponent().add(new EmptyOnBlurAjaxFormUpdatingBehaviour());
        return panel;
    }

    @Override
    public Integer getOrder() {
        return 10000 - 10;
    }
}

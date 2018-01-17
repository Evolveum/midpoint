/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.util;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ItemWrapper;
import com.evolveum.midpoint.web.component.prism.PrismHeaderPanel;
import com.evolveum.midpoint.web.component.prism.PrismPropertyPanel;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.reflect.FieldUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.application.IComponentInitializationListener;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.Response;

import javax.xml.namespace.QName;

/**
 * Created by Viliam Repan (lazyman).
 */
public class SchrodingerComponentInitListener implements IComponentInitializationListener {

    private static final Trace LOGGER = TraceManager.getTrace(SchrodingerComponentInitListener.class);

    private static final String ATTR_DATA_PREFIX = "data-s-";

    private static final String ATTR_ID = "id";
    private static final String ATTR_RESOURCE_KEY = "resource-key";
    private static final String ATTR_QNAME = "qname";

    @Override
    public void onInitialize(Component component) {
        try {
            handleId(component);
            handleLocalization(component);
        } catch (Exception ex) {
            LOGGER.error("Schrodinger component initializer failed", ex);
        }
    }

    private void handleId(Component component) {
        writeDataAttribute(component, ATTR_ID, component.getId());
    }

    private void writeDataAttribute(Component component, String key, String value) {
        if (!component.getRenderBodyOnly()) {
            component.add(AttributeModifier.append(ATTR_DATA_PREFIX + key, value));
            return;
        }

        if ("title".equals(component.getId()) && component.getParent() instanceof Page) {
            // we don't want to alter <title> element
            return;
        }

        component.add(new Behavior() {

            @Override
            public void afterRender(Component component) {
                Response resp = component.getResponse();
                resp.write("<schrodinger " + ATTR_DATA_PREFIX + key + "=\"" + value + "\"></schrodinger>");
            }
        });
    }

    private void handleLocalization(Component component) {
        if (component instanceof PrismPropertyPanel) {
            PrismPropertyPanel ppp = (PrismPropertyPanel) component;
            ItemWrapper iw = (ItemWrapper) ppp.getModel().getObject();
            String key = iw.getDisplayName();

            QName qname = iw.getName();

            writeDataAttribute(component, ATTR_RESOURCE_KEY, key);
            writeDataAttribute(component, ATTR_QNAME, qnameToString(qname));
            return;
        }

        if (component instanceof PrismHeaderPanel) {
            PrismHeaderPanel php = (PrismHeaderPanel) component;
            String key = php.getLabel();

            writeDataAttribute(component, ATTR_RESOURCE_KEY, key);
            return;
        }

        StringResourceModel model = null;
        if (component.getDefaultModel() instanceof StringResourceModel) {
            model = (StringResourceModel) component.getDefaultModel();
        } else if (component.getInnermostModel() instanceof StringResourceModel) {
            model = (StringResourceModel) component.getInnermostModel();
        }

        if (model == null) {
            return;
        }

        try {
            String key = (String) FieldUtils.readField(model, "resourceKey", true);
            if (key != null) {
                writeDataAttribute(component, ATTR_RESOURCE_KEY, key);
            }
        } catch (Exception ex) {
            // we don't care, should be all right, unless selenium tests starts failing
        }
    }

    private String qnameToString(QName qname) {
        if (qname == null) {
            return null;
        }

        return StringUtils.join(new Object[]{qname.getNamespaceURI(), qname.getLocalPart()}, "#");
    }
}

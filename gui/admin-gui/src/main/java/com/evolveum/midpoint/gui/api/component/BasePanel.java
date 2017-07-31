/*
 * Copyright (c) 2010-2017 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.gui.api.component;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.security.MidPointApplication;
import com.evolveum.midpoint.web.security.MidPointAuthWebSession;
import com.evolveum.midpoint.web.security.WebApplicationConfiguration;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

/**
 * Base class for most midPoint GUI panels. It has a constructor and
 * utility methods for convenient handling of the model. It also has
 * other utility methods often used in reusable components.
 * 
 * Almost all reusable components should extend this class.
 * 
 * @author lazyman
 * @author semancik
 */
public class BasePanel<T> extends Panel {
	private static final long serialVersionUID = 1L;

	private IModel<T> model;

    public BasePanel(String id) {
        this(id, null);
    }

    public BasePanel(String id, IModel<T> model) {
        super(id);
        this.model = model == null ? createModel() : model;
    }

    public IModel<T> createModel() {
        return null;
    }

    public IModel<T> getModel() {
        return model;
    }

    public T getModelObject() {
        return model != null ? model.getObject() : null;
    }

    public String getString(String resourceKey, Object... objects) {
        return createStringResource(resourceKey, objects).getString();
    }

    public StringResourceModel createStringResource(String resourceKey, Object... objects) {
    	return new StringResourceModel(resourceKey, this).setModel(null)
    			.setDefaultValue(resourceKey)
    			.setParameters(objects);
//    	return StringResourceModelMigration.of(resourceKey, this, null, resourceKey, objects);
    }

    public StringResourceModel createStringResource(Enum e) {
        return createStringResource(e, null);
    }

    public StringResourceModel createStringResource(Enum e, String prefix) {
        return createStringResource(e, prefix, null);
    }

    public StringResourceModel createStringResource(Enum e, String prefix, String nullKey) {
        StringBuilder sb = new StringBuilder();
        if (StringUtils.isNotEmpty(prefix)) {
            sb.append(prefix).append('.');
        }

        if (e == null) {
            if (StringUtils.isNotEmpty(nullKey)) {
                sb.append(nullKey);
            } else {
                sb = new StringBuilder();
            }
        } else {
            sb.append(e.getDeclaringClass().getSimpleName()).append('.');
            sb.append(e.name());
        }

        return createStringResource(sb.toString());
    }
    
    public PageBase getPageBase() {
        return WebComponentUtil.getPageBase(this);
    }

    protected String createComponentPath(String... components) {
        return StringUtils.join(components, ":");
    }

    public WebApplicationConfiguration getWebApplicationConfiguration() {
        MidPointApplication application = (MidPointApplication) MidPointApplication.get();
        return application.getWebApplicationConfiguration();
    }

    @Override
    public MidPointAuthWebSession getSession() {
        return (MidPointAuthWebSession) super.getSession();
    }
}

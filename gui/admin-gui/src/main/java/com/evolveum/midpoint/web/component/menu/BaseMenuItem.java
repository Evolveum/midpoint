/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.web.component.menu;

import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import java.io.Serializable;
import java.util.Arrays;

/**
 * @author Viliam Repan (lazyman)
 */
public class BaseMenuItem implements Serializable {
	private static final long serialVersionUID = 1L;

	private IModel<String> nameModel;
    private Class<? extends WebPage> pageClass;
    private PageParameters params;
    private VisibleEnableBehaviour visibleEnable;
    private Class<? extends WebPage>[] aliases;

    public BaseMenuItem(IModel<String> name, Class<? extends WebPage> page) {
        this(name, page, null, null);
    }

    public BaseMenuItem(IModel<String> nameModel, Class<? extends WebPage> pageClass,
                        PageParameters params, VisibleEnableBehaviour visibleEnable,
                        Class<? extends WebPage>... aliases) {
        this.aliases = aliases;
        this.nameModel = nameModel;
        this.pageClass = pageClass;
        this.params = params;
        this.visibleEnable = visibleEnable;
    }

    /**
     * @return Returns array of {@link WebPage} classes where this menu should be marked as <b>active</b>.
     */
    public Class<? extends WebPage>[] getAliases() {
        return aliases;
    }

    public IModel<String> getNameModel() {
        return nameModel;
    }

    public Class<? extends WebPage> getPageClass() {
        return pageClass;
    }

    public PageParameters getParams() {
        return params;
    }

    public VisibleEnableBehaviour getVisibleEnable() {
        return visibleEnable;
    }

    public boolean isMenuActive(WebPage page) {
        if (page == null) {
            return false;
        }

        Class pageClass = page.getClass();

        if (this.pageClass == null) {
            return false;
        }

        boolean isMenuActive = isMenuActive();

        if (pageClass.equals(this.pageClass)) {
            return isMenuActive;
        }

        if (aliases == null) {
            return false;
        }

        for (Class c : aliases) {
            if (pageClass.equals(c)) {
                return isMenuActive;
            }
        }

        return false;
    }

    protected boolean isMenuActive() {
        return true;
    }

	@Override
	public String toString() {
		return "BaseMenuItem(nameModel=" + nameModel + ", pageClass=" + pageClass + ", params=" + params
				+ ", visibleEnable=" + visibleEnable + ", aliases=" + Arrays.toString(aliases) + ")";
	}


}

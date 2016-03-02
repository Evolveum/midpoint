/*
 * Copyright (c) 2010-2016 Evolveum
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

package com.evolveum.midpoint.web.component.breadcrumbs;

import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;

import java.io.Serializable;
import java.util.Arrays;

/**
 * @author Viliam Repan (lazyman)
 */
public class Breadcrumb implements Serializable {

    private IModel<String> label;
    private IModel<String> icon;

    public Breadcrumb() {
    }

    public Breadcrumb(IModel<String> label) {
        this(label, null);
    }

    public Breadcrumb(IModel<String> label, IModel<String> icon) {
        this.icon = icon;
        this.label = label;
    }

    public IModel<String> getLabel() {
        return label;
    }

    public void setLabel(IModel<String> label) {
        this.label = label;
    }

    public IModel<String> getIcon() {
        return icon;
    }

    public void setIcon(IModel<String> icon) {
        this.icon = icon;
    }

    public boolean isLink() {
        return false;
    }

    public void redirect(Component component) {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        //we don't compare label/icon models, we would need to compare models values
        return true;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{label, icon});
    }
}

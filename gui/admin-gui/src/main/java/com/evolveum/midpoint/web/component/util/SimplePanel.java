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

package com.evolveum.midpoint.web.component.util;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;

/**
 * @author lazyman
 */
@Deprecated
public abstract class SimplePanel<T> extends BasePanel<T> {

    public SimplePanel(String id) {
        this(id, null);
    }

    public SimplePanel(String id, IModel<T> model) {
        super(id, model);
        // TODO: initLayout should NOT be called here. Calling it here
        // makes problem with proper class initialization
        // (superclass constructor must be called first, no place where to
        //  initialize subclass fields).
        // But this is maybe OK for "simple" panel. Anyway, leaving it here for
        // now because a lot of code relies on this.
        initLayout();
    }


    public PageBase getPageBase() {
        return WebComponentUtil.getPageBase(this);
    }

    public void setModelObject(T obj){
    	setDefaultModelObject(obj);
    }

    protected abstract void initLayout();

//    public PrismContext getPrismContext(){
//    	return getPageBase().getPrismContext();
//    }

//    public WebMarkupContainer getFeedbackPanel(){
//    	return getPageBase().getFeedbackPanel();
//    }
}

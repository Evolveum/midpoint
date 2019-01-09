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

package com.evolveum.midpoint.gui.impl.session;

import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.web.component.search.Search;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_4.RoleType;

import javax.xml.namespace.QName;

/**
 * @author skublik
 */
public class ObjectTabStorage implements PageStorage{

	private static final long serialVersionUID = 1L;

	private QName type = null;
	private ObjectPaging objectPaging;


    @Override
    public Search getSearch() {
        return null;
    }

    @Override
    public void setSearch(Search search) {
    }

    @Override
    public ObjectPaging getPaging() {
        return objectPaging;
    }

    @Override
    public void setPaging(ObjectPaging objectPaging) {
        this.objectPaging = objectPaging;
    }

    public QName getType() {
        return type;
    }

    public void setType(QName type) {
        this.type = type;
    }

    @Override
    public String debugDump() {
        return debugDump(0);
    }

    @Override
    public String debugDump(int indent) {
        return "";
    }

}

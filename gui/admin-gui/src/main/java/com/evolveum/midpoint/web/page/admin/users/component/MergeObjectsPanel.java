/**
 * Copyright (c) 2010-2016 Evolveum
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
package com.evolveum.midpoint.web.page.admin.users.component;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;

/**
 * Created by honchar.
 */
public class MergeObjectsPanel<F extends FocusType> extends BasePanel{
    private static final String ID_MERGE_OBJECT_DETAILS_PANEL = "mergeObjectDetailsPanel";
    private static final String ID_MERGE_WITH_OBJECT_DETAILS_PANEL = "mergeWithObjectDetailsPanel";
    private static final String ID_MERGE_RESULT_OBJECT_DETAILS_PANEL = "mergeResultObjectDetailsPanel";
    private static final String ID_PLUS_ICON = "mergeObjectDetailsPanel";
    private static final String ID_EQUAL_ICON = "mergeObjectDetailsPanel";

    private F mergeObject;
    private F mergeWithObject;
    private Class<F> type;

    public MergeObjectsPanel(String id){
        super(id);
    }

    public MergeObjectsPanel(String id, F mergeObject, F mergeWithObject, Class<F> type){
        super(id);
        this.mergeObject = mergeObject;
        this.mergeWithObject = mergeWithObject;
        this.type = type;
        initLayout();
    }

    private void initLayout(){
        MergeObjectDetailsPanel mergeObjectPanel = new MergeObjectDetailsPanel(ID_MERGE_OBJECT_DETAILS_PANEL,
                mergeObject, type);
        mergeObjectPanel.setOutputMarkupId(true);
        add(mergeObjectPanel);

        MergeObjectDetailsPanel mergeWithObjectPanel = new MergeObjectDetailsPanel(ID_MERGE_WITH_OBJECT_DETAILS_PANEL,
                mergeWithObject, type);
        mergeWithObjectPanel.setOutputMarkupId(true);
        add(mergeWithObjectPanel);



    }
}

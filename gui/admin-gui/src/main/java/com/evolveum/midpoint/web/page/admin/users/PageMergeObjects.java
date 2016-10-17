/*
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
package com.evolveum.midpoint.web.page.admin.users;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.users.component.MergeObjectsPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.markup.html.basic.Label;

/**
 * Created by honchar.
 */
public class PageMergeObjects<F extends FocusType> extends PageBase {
    private static final String ID_MERGE_PANEL = "mergePanel";
    private F mergeObject;
    private F mergeWithObject;
    private Class<F> type;
    public PageMergeObjects(){
    }

    public PageMergeObjects(F mergeObject, F mergeWithObject, Class<F> type){
        this.mergeObject = mergeObject;
        this.mergeWithObject = mergeWithObject;
        this.type = type;
        initLayout();
    }

    private void initLayout(){
        if (mergeObject == null || StringUtils.isEmpty(mergeObject.getOid())
                || mergeWithObject == null || StringUtils.isEmpty(mergeWithObject.getOid())) {
            Label warningMessage = new Label(ID_MERGE_PANEL, createStringResource("PageMergeObjects.warningMessage"));
            warningMessage.setOutputMarkupId(true);
            add(warningMessage);
        } else {
            MergeObjectsPanel mergePanel = new MergeObjectsPanel(ID_MERGE_PANEL, mergeObject, mergeWithObject, type);
            mergePanel.setOutputMarkupId(true);
            add(mergePanel);
        }
    }
}

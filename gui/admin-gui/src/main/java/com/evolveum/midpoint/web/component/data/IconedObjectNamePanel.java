/*
 * Copyright (c) 2010-2019 Evolveum
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

package com.evolveum.midpoint.web.component.data;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.data.column.ImagePanel;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;


/**
 * Created by honchar
 */
public class IconedObjectNamePanel<AHT extends AssignmentHolderType> extends BasePanel<AHT> {
    private static final long serialVersionUID = 1L;

    private static final String ID_ICON = "icon";
    private static final String ID_NAME = "name";

    private static final String DOT_CLASS = IconedObjectNamePanel.class.getName() + ".";
    private static final String OPERATION_LOAD_REFERENCED_OBJECT = DOT_CLASS + "loadReferencedObject";

    ObjectReferenceType objectReference;

    public IconedObjectNamePanel(String id, ObjectReferenceType objectReference){
        super(id);
        this.objectReference = objectReference;
    }

    public IconedObjectNamePanel(String id, IModel<AHT> assignmentHolder){
        super(id, assignmentHolder);
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        initLayout();
    }

    @Override
    public IModel<AHT> createModel() {
        return new LoadableModel<AHT>() {
            @Override
            protected AHT load() {
                PageBase pageBase = IconedObjectNamePanel.this.getPageBase();
                OperationResult result = new OperationResult(OPERATION_LOAD_REFERENCED_OBJECT);
                if (objectReference != null) {
                    PrismObject<AHT> assignmentHolder = WebModelServiceUtils.resolveReferenceNoFetch(objectReference, pageBase,
                            pageBase.createSimpleTask(OPERATION_LOAD_REFERENCED_OBJECT), result);
                    return assignmentHolder != null ? assignmentHolder.asObjectable() : null;
                } else {
                    return null;
                }
            }
        };
    }

    private void initLayout(){
        setOutputMarkupId(true);

        DisplayType displayType = WebComponentUtil.getArchetypePolicyDisplayType(IconedObjectNamePanel.this.getModelObject(), IconedObjectNamePanel.this.getPageBase());
        ImagePanel imagePanel = new ImagePanel(ID_ICON, displayType);
        imagePanel.setOutputMarkupId(true);
        imagePanel.add(new VisibleBehaviour(() -> displayType != null && displayType.getIcon() != null && StringUtils.isNotEmpty(displayType.getIcon().getCssClass())));

        Label nameLabel = new Label(ID_NAME, Model.of(WebComponentUtil.getEffectiveName(getModelObject(), AbstractRoleType.F_DISPLAY_NAME)));
        nameLabel.setOutputMarkupId(true);
        add(nameLabel);

        //todo navigate to object icon or name label as link ?
    }

}

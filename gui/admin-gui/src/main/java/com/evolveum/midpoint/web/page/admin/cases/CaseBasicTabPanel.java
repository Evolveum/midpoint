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
package com.evolveum.midpoint.web.page.admin.cases;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectTabPanel;
import com.evolveum.midpoint.web.component.prism.ObjectWrapper;
import com.evolveum.midpoint.web.component.prism.PrismPanel;
import com.evolveum.midpoint.web.model.ContainerWrapperListFromObjectWrapperModel;
import com.evolveum.midpoint.web.resource.img.ImgResources;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import org.apache.wicket.request.resource.PackageResourceReference;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by honchar
 */
public class CaseBasicTabPanel extends AbstractObjectTabPanel<CaseType> {
    private static final long serialVersionUID = 1L;

    protected static final String ID_CASE_FORM = "caseDetails";

    public CaseBasicTabPanel(String id, Form<ObjectWrapper<CaseType>> mainForm, LoadableModel<ObjectWrapper<CaseType>> objectWrapperModel, PageBase pageBase){
        super(id, mainForm, objectWrapperModel, pageBase);
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        PrismPanel<CaseType> caseForm = new PrismPanel<CaseType>(ID_CASE_FORM,
                new ContainerWrapperListFromObjectWrapperModel(getObjectWrapperModel(), getVisibleContainers()),
        		new PackageResourceReference(ImgResources.class, ImgResources.HDD_PRISM),
                getMainForm(), null, getPageBase());
        add(caseForm);
    }

    private List<ItemPath> getVisibleContainers() {
        List<ItemPath> paths = new ArrayList<>();
        paths.add(ItemPath.EMPTY_PATH);
        return paths;
    }
}

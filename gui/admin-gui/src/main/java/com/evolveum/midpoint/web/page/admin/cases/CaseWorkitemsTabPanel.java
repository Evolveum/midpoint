/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.cases;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.component.objectdetails.AbstractObjectTabPanel;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Created by honchar
 */
public class CaseWorkitemsTabPanel extends AbstractObjectTabPanel<CaseType> {
    private static final long serialVersionUID = 1L;

    private static final String ID_WORKITEMS_PANEL = "workitemsPanel";

    public CaseWorkitemsTabPanel(String id, Form<PrismObjectWrapper<CaseType>> mainForm, LoadableModel<PrismObjectWrapper<CaseType>> objectWrapperModel, PageBase pageBase) {
        super(id, mainForm, objectWrapperModel);
    }

    @Override
    protected void onInitialize(){
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        setOutputMarkupId(true);

        PrismContainerWrapperModel<CaseType, CaseWorkItemType> workitemsModel = PrismContainerWrapperModel.fromContainerWrapper(getObjectWrapperModel(), CaseType.F_WORK_ITEM);

        add(new CaseWorkItemsTableWithDetailsPanel(ID_WORKITEMS_PANEL, workitemsModel){
            private static final long serialVersionUID = 1L;

            @Override
            protected UserProfileStorage.TableId getTableId(){
                return UserProfileStorage.TableId.PAGE_CASE_WORKITEMS_TAB;
            }

            @Override
            protected ObjectQuery createQuery(){
                return null;
            }
        });
    }
}

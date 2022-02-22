/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.users.component;

import com.evolveum.midpoint.gui.api.page.PageBase;
import org.apache.wicket.model.StringResourceModel;

import java.util.List;

public class DelegationTargetLimitationDialog extends AssignmentsInfoDialog {

    public DelegationTargetLimitationDialog(String id, List<AssignmentInfoDto> data, PageBase pageBase) {
        super(id, data, pageBase);
    }

    @Override
    protected boolean enableMultiSelect() {
        return true;
    }

    @Override
    protected boolean showDirectIndirectColumn() {
        return false;
    }

    @Override
    protected boolean showKindAndIntentColumns() {
        return false;
    }

    @Override
    protected boolean showRelationColumn() {
        return false;
    }

    @Override
    protected boolean showCancelButton(){
        return true;
    }

    @Override
    public StringResourceModel getTitle() {
        return new StringResourceModel("AssignmentPreviewDialog.delegationPreviewLabel");
    }
}

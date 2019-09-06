/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0 
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.users.component;

import com.evolveum.midpoint.gui.api.page.PageBase;

import java.util.List;

/**
 * @author mederly
 */
public class AllAssignmentsPreviewDialog extends AssignmentsInfoDialog {

	public AllAssignmentsPreviewDialog(String id, List<AssignmentInfoDto> data, PageBase pageBase) {
		super(id, data, pageBase);
	}

	@Override
	protected boolean enableMultiSelect() {
		return false;
	}

	@Override
	protected boolean showDirectIndirectColumn() {
		return true;
	}

	@Override
	protected boolean showKindAndIntentColumns() {
		return true;
	}

	@Override
	protected boolean showRelationColumn() {
		return true;
	}
}

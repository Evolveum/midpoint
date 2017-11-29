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

package com.evolveum.midpoint.web.page.admin.users.component;

import com.evolveum.midpoint.gui.api.page.PageBase;
import org.apache.wicket.model.StringResourceModel;

import java.util.List;

/**
 * @author mederly
 */
public class DelegationTargetLimitationDialog extends AssignmentsInfoDialog {

	public DelegationTargetLimitationDialog(String id, List<AssignmentInfoDto> data, PageBase pageBase) {
		super(id, data, pageBase);
	}

	@Override
	public StringResourceModel getTitle() {
		return new StringResourceModel("AssignmentPreviewDialog.delegationPreviewLabel");
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
}

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

package com.evolveum.midpoint.web.page.admin.resources;

import com.evolveum.midpoint.gui.api.model.NonEmptyLoadableModel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.validator.ResourceValidator;
import com.evolveum.midpoint.model.api.validator.Scope;
import com.evolveum.midpoint.model.api.validator.ValidationResult;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.web.component.wizard.resource.dto.WizardIssuesDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import org.jetbrains.annotations.NotNull;

/**
 * @author mederly
 */
public class ResourceWizardIssuesModel extends NonEmptyLoadableModel<WizardIssuesDto> {

	@NotNull private final NonEmptyLoadableModel<PrismObject<ResourceType>> resourceModel;
	@NotNull private final PageResourceWizard wizardPage;

	ResourceWizardIssuesModel(@NotNull NonEmptyLoadableModel<PrismObject<ResourceType>> resourceModel, @NotNull PageResourceWizard wizardPage) {
		super(false);
		this.resourceModel = resourceModel;
		this.wizardPage = wizardPage;
	}

	@NotNull
	@Override
	protected WizardIssuesDto load() {
		final WizardIssuesDto issuesDto = new WizardIssuesDto();
		if (!resourceModel.isLoaded()) {
			return issuesDto;		// e.g. in first two wizard steps (IT PROBABLY DOES NOT WORK AS EXPECTED)
		}
		ResourceValidator validator = wizardPage.getResourceValidator();
		ValidationResult validationResult = validator
				.validate(resourceModel.getObject(), Scope.QUICK, WebComponentUtil.getCurrentLocale(), wizardPage.createSimpleTask("validate"), new OperationResult("validate"));

		issuesDto.fillFrom(validationResult);
		issuesDto.sortIssues();
		return issuesDto;
	}

}

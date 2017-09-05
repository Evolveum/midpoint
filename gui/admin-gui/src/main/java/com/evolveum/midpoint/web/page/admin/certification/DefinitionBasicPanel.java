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

package com.evolveum.midpoint.web.page.admin.certification;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.certification.dto.CertDefinitionDto;
import com.evolveum.midpoint.web.page.admin.configuration.component.ChooseTypePanel;
import com.evolveum.midpoint.web.page.admin.dto.ObjectViewDto;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseOutcomeStrategyType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationRemediationStyleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationResponseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;

import javax.xml.namespace.QName;
import java.util.List;

/**
 * @author mederly
 */

public class DefinitionBasicPanel extends BasePanel<CertDefinitionDto> {

	private static final String ID_NAME = "name";
	private static final String ID_DESCRIPTION = "description";
	private static final String ID_OWNER = "owner";
	private static final String ID_REVIEW_STAGE_CAMPAIGNS = "campaignsInReviewStage";
	private static final String ID_CAMPAIGNS_TOTAL = "campaignsTotal";
	private static final String ID_LAST_STARTED = "campaignLastStarted";
	private static final String ID_LAST_STARTED_HELP = "campaignLastStartedHelp";
	private static final String ID_LAST_CLOSED = "campaignLastClosed";
	private static final String ID_LAST_CLOSED_HELP = "campaignLastClosedHelp";
	//	private static final String ID_OWNER_VALUE_CONTAINER = "ownerValueContainer";
	//	private static final String ID_OWNER_INPUT = "ownerInput";
	private static final String ID_OWNER_REF_CHOOSER = "ownerRefChooser";
	private static final String ID_REMEDIATION = "remediation";
	private static final String ID_OUTCOME_STRATEGY = "outcomeStrategy";
	private static final String ID_OUTCOME_STRATEGY_HELP = "outcomeStrategyHelp";
	private static final String ID_STOP_REVIEW_ON = "stopReviewOn";


	public DefinitionBasicPanel(String id, IModel<CertDefinitionDto> model) {
		super(id, model);
		initBasicInfoLayout();
	}

	private void initBasicInfoLayout() {

		final TextField nameField = new TextField(ID_NAME, new PropertyModel<>(getModel(), CertDefinitionDto.F_NAME));
		nameField.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isEnabled() {
				return true;
			}
		});
		add(nameField);

		final TextArea descriptionField = new TextArea(ID_DESCRIPTION, new PropertyModel<>(getModel(), CertDefinitionDto.F_DESCRIPTION));
		descriptionField.add(new VisibleEnableBehaviour() {
			@Override
			public boolean isEnabled() {
				return true;
			}
		});
		add(descriptionField);

		final WebMarkupContainer ownerRefChooser = createOwnerRefChooser(ID_OWNER_REF_CHOOSER);
		ownerRefChooser.setOutputMarkupId(true);
		add(ownerRefChooser);

		DropDownChoice remediation = new DropDownChoice<>(ID_REMEDIATION, new Model<AccessCertificationRemediationStyleType>() {

			@Override
			public AccessCertificationRemediationStyleType getObject() {
				return getModel().getObject().getRemediationStyle();
			}

			@Override
			public void setObject(AccessCertificationRemediationStyleType object) {
				getModel().getObject().setRemediationStyle(object);
			}
		}, WebComponentUtil.createReadonlyModelFromEnum(AccessCertificationRemediationStyleType.class),
				new EnumChoiceRenderer<>(this));
		add(remediation);

		DropDownChoice outcomeStrategy =
				new DropDownChoice<>(ID_OUTCOME_STRATEGY,
						new PropertyModel<>(getModel(), CertDefinitionDto.F_OUTCOME_STRATEGY),
						WebComponentUtil.createReadonlyModelFromEnum(AccessCertificationCaseOutcomeStrategyType.class),
						new EnumChoiceRenderer<>(this));
		add(outcomeStrategy);

		add(WebComponentUtil.createHelp(ID_OUTCOME_STRATEGY_HELP));

		Label stopReviewOn = new Label(ID_STOP_REVIEW_ON, new AbstractReadOnlyModel<String>() {
			@Override
			public String getObject() {
				List<AccessCertificationResponseType> stopOn = getModel().getObject().getStopReviewOn();
				return CertMiscUtil.getStopReviewOnText(stopOn, getPageBase());
			}
		});
		add(stopReviewOn);

		//        add(new Label(ID_REVIEW_STAGE_CAMPAIGNS, new PropertyModel<>(getModel(), CertDefinitionDto.F_NUMBER_OF_STAGES)));
		//        add(new Label(ID_CAMPAIGNS_TOTAL, new PropertyModel<>(getModel(), CertDefinitionDto.F_NUMBER_OF_STAGES)));
		add(new Label(ID_LAST_STARTED, new PropertyModel<>(getModel(), CertDefinitionDto.F_LAST_STARTED)));
		add(new Label(ID_LAST_CLOSED, new PropertyModel<>(getModel(), CertDefinitionDto.F_LAST_CLOSED)));
		add(WebComponentUtil.createHelp(ID_LAST_STARTED_HELP));
		add(WebComponentUtil.createHelp(ID_LAST_CLOSED_HELP));
	}

	private WebMarkupContainer createOwnerRefChooser(String id) {
		ChooseTypePanel tenantRef = new ChooseTypePanel(id,
				new PropertyModel<ObjectViewDto>(getModel(), CertDefinitionDto.F_OWNER)) {

			@Override
			protected boolean isSearchEnabled() {
				return true;
			}

			@Override
			protected QName getSearchProperty() {
				return UserType.F_NAME;
			}
		};

		return tenantRef;
	}

}

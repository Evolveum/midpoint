package com.evolveum.midpoint.web.page.admin.configuration;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxSubmitButton;
import com.evolveum.midpoint.web.component.form.CheckFormGroup;
import com.evolveum.midpoint.web.page.admin.configuration.dto.InternalsConfigDto;

public class InternalsConfigPanel extends BasePanel<InternalsConfigDto> {

	private static final long serialVersionUID = 1L;
	
	private static final Trace LOGGER = TraceManager.getTrace(InternalsConfigPanel.class);
	
	private static final String ID_FORM = "form";
	private static final String ID_INTERNALS_CONFIG_FORM = "internalsConfigForm";
	private static final String ID_UPDATE_INTERNALS_CONFIG = "updateInternalsConfig";
	private static final String ID_CONSISTENCY_CHECKS = "consistencyChecks";
	private static final String ID_ENCRYPTION_CHECKS = "encryptionChecks";
	private static final String ID_MODEL_PROFILING = "modelProfiling";
	private static final String ID_READ_ENCRYPTION_CHECKS = "readEncryptionChecks";
	private static final String ID_TOLERATE_UNDECLARED_PREFIXES = "tolerateUndeclaredPrefixes";
	private static final String ID_DETAILED_DEBUG_DUMP = "detailedDebugDump";
	
	private static final String LABEL_SIZE = "col-md-4";
    private static final String INPUT_SIZE = "col-md-8";

	public InternalsConfigPanel(String id, IModel<InternalsConfigDto> model) {
		super(id, model);
	}

	@Override
	protected void onInitialize() {
		super.onInitialize();

		setOutputMarkupId(true);
		
		Form form = new com.evolveum.midpoint.web.component.form.Form<>(ID_FORM);
		form.setOutputMarkupId(true);
		add(form);

		form.add(createCheckbox(ID_CONSISTENCY_CHECKS, InternalsConfigDto.F_CONSISTENCY_CHECKS));
		form.add(createCheckbox(ID_ENCRYPTION_CHECKS, InternalsConfigDto.F_ENCRYPTION_CHECKS));
		form.add(createCheckbox(ID_READ_ENCRYPTION_CHECKS, InternalsConfigDto.F_READ_ENCRYPTION_CHECKS));
		form.add(createCheckbox(ID_MODEL_PROFILING, InternalsConfigDto.F_MODEL_PROFILING));
		form.add(createCheckbox(ID_TOLERATE_UNDECLARED_PREFIXES, InternalsConfigDto.F_TOLERATE_UNDECLARED_PREFIXES));

		AjaxSubmitButton update = new AjaxSubmitButton(ID_UPDATE_INTERNALS_CONFIG,
				createStringResource("PageBase.button.update")) {
			private static final long serialVersionUID = 1L;

			@Override
			protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
				updateInternalConfig(target);
			}

			@Override
			protected void onError(AjaxRequestTarget target, Form<?> form) {
				target.add(getPageBase().getFeedbackPanel());
			}
		};
		form.add(update);
	}

	private void updateInternalConfig(AjaxRequestTarget target) {
		getModelObject().saveInternalsConfig();

		LOGGER.trace(
				"Updated internals config, consistencyChecks={},encryptionChecks={},readEncryptionChecks={}, QNameUtil.tolerateUndeclaredPrefixes={}",
				new Object[] { InternalsConfig.consistencyChecks, InternalsConfig.encryptionChecks,
						InternalsConfig.readEncryptionChecks, QNameUtil.isTolerateUndeclaredPrefixes() });
		success(getString("PageInternals.message.internalsConfigUpdate", InternalsConfig.consistencyChecks,
				InternalsConfig.encryptionChecks, InternalsConfig.readEncryptionChecks,
				QNameUtil.isTolerateUndeclaredPrefixes()));
		target.add(getPageBase().getFeedbackPanel(), getForm());
	}

	private Form getForm() {
		return (Form) get(ID_FORM);
	}

	private CheckFormGroup createCheckbox(String id, String propName) {
		return new CheckFormGroup(id, new PropertyModel<Boolean>(getModel(), propName),
				createStringResource("PageInternals." + propName), LABEL_SIZE, INPUT_SIZE);
	}
}

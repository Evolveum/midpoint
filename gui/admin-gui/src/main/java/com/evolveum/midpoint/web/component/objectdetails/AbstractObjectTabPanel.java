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
package com.evolveum.midpoint.web.component.objectdetails;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismPropertyPanel;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.form.Form;
import com.evolveum.midpoint.web.model.PrismPropertyWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author semancik
 */
public abstract class AbstractObjectTabPanel<O extends ObjectType> extends Panel {
	private static final long serialVersionUID = 1L;

	protected static final String ID_MAIN_FORM = "mainForm";

	private static final Trace LOGGER = TraceManager.getTrace(AbstractObjectTabPanel.class);

	private LoadableModel<PrismObjectWrapper<O>> objectWrapperModel;
//	protected PageBase pageBase;
	private Form<PrismObjectWrapper<O>> mainForm;

	public AbstractObjectTabPanel(String id, Form<PrismObjectWrapper<O>> mainForm, LoadableModel<PrismObjectWrapper<O>> objectWrapperModel) {
		super(id);
		this.objectWrapperModel = objectWrapperModel;
		this.mainForm = mainForm;
//		this.pageBase = pageBase;
	}

	public LoadableModel<PrismObjectWrapper<O>> getObjectWrapperModel() {
		return objectWrapperModel;
	}

	public PrismObjectWrapper<O> getObjectWrapper() {
		return objectWrapperModel.getObject();
	}

	protected PrismContext getPrismContext() {
		return getPageBase().getPrismContext();
	}

	protected PageParameters getPageParameters() {
		return getPageBase().getPageParameters();
	}

	public PageBase getPageBase() {
		return (PageBase) getPage();
	}

	public Form<PrismObjectWrapper<O>> getMainForm() {
		return mainForm;
	}

	public StringResourceModel createStringResource(String resourceKey, Object... objects) {
		return PageBase.createStringResourceStatic(this, resourceKey, objects);
//		return new StringResourceModel(resourceKey, this, null, resourceKey, objects);
	}

	public String getString(String resourceKey, Object... objects) {
		return createStringResource(resourceKey, objects).getString();
	}

	protected String createComponentPath(String... components) {
		return StringUtils.join(components, ":");
	}

	protected void showResult(OperationResult result) {
		getPageBase().showResult(result);
	}

	protected void showResult(OperationResult result, boolean showSuccess) {
		getPageBase().showResult(result, false);
	}


	protected WebMarkupContainer getFeedbackPanel() {
		return getPageBase().getFeedbackPanel();
	}

	public Object findParam(String param, String oid, OperationResult result) {

		Object object = null;

		for (OperationResult subResult : result.getSubresults()) {
			if (subResult != null && subResult.getParams() != null) {
				if (subResult.getParams().get(param) != null
						&& subResult.getParams().get(OperationResult.PARAM_OID) != null
						&& subResult.getParams().get(OperationResult.PARAM_OID).equals(oid)) {
					return subResult.getParams().get(param);
				}
				object = findParam(param, oid, subResult);

			}
		}
		return object;
	}

	protected void showModalWindow(Popupable popupable, AjaxRequestTarget target) {
        getPageBase().showMainPopup(popupable, target);
		target.add(getFeedbackPanel());
	}

	protected <T> PrismPropertyPanel<T> addPrismPropertyPanel(MarkupContainer parentComponent, String id, ItemPath propertyPath) {
		PrismPropertyPanel<T> panel = new PrismPropertyPanel<>(id, new PrismPropertyWrapperModel<O, T>(getObjectWrapperModel(), propertyPath));
				//mainForm, wrapper -> ItemVisibility.VISIBLE);
		parentComponent.add(panel);
		return panel;
	}
}

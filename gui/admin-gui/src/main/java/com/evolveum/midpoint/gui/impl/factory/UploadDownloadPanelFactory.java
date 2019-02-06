/*
 * Copyright (c) 2010-2018 Evolveum
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
package com.evolveum.midpoint.gui.impl.factory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.annotation.PostConstruct;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.factory.AbstractGuiComponentFactory;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemWrapperOld;
import com.evolveum.midpoint.gui.api.registry.GuiComponentRegistry;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.web.component.input.UploadDownloadPanel;

/**
 * @author katkav
 *
 */
@Component
public class UploadDownloadPanelFactory extends AbstractGuiComponentFactory {
	
	@Autowired GuiComponentRegistry registry;
	
	@PostConstruct
	public void register() {
		registry.addToRegistry(this);
	}

	@Override
	public <T> boolean match(ItemWrapperOld itemWrapper) {
		return DOMUtil.XSD_BASE64BINARY.equals(itemWrapper.getItemDefinition().getTypeName());
	}

	@Override
	public <T> Panel getPanel(PanelContext<T> panelCtx) {
		return new UploadDownloadPanel(panelCtx.getComponentId(), false) { //getModel().getObject().isReadonly()

			@Override
			public InputStream getStream() {
				T object = panelCtx.getRealValueModel().getObject();
				return object != null ? new ByteArrayInputStream((byte[]) object) : new ByteArrayInputStream(new byte[0]);
			}

			@Override
			public void updateValue(byte[] file) {
				panelCtx.getRealValueModel().setObject((T) file);
			}

			@Override
			public void uploadFileFailed(AjaxRequestTarget target) {
				super.uploadFileFailed(target);
				target.add(((PageBase) getPage()).getFeedbackPanel());
			}
		};
	}

}

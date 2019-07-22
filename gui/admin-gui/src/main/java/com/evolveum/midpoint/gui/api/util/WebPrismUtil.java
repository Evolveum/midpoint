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
package com.evolveum.midpoint.gui.api.util;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.factory.WrapperContext;
import com.evolveum.midpoint.gui.impl.prism.PrismValueWrapper;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ValueStatus;

/**
 * @author katka
 *
 */
public class WebPrismUtil {
	
	private static final transient Trace LOGGER = TraceManager.getTrace(WebPrismUtil.class);
	
	private static final String DOT_CLASS = WebPrismUtil.class.getName() + ".";
	private static final String OPERATION_CREATE_NEW_VALUE = DOT_CLASS + "createNewValue";
	
	public static <ID extends ItemDefinition<I>, I extends Item<V, ID>, V extends PrismValue> String getHelpText(ID def) {
		String doc = def.getHelp();
        if (StringUtils.isEmpty(doc)) {
        	doc = def.getDocumentation();
        	if (StringUtils.isEmpty(doc)) {
            	return null;
            }
        }
        
        return doc.replaceAll("\\s{2,}", " ").trim();
	}
	
	
	public static <IW extends ItemWrapper, PV extends PrismValue, VW extends PrismValueWrapper> VW createNewValueWrapper(IW itemWrapper, PV newValue, PageBase pageBase, AjaxRequestTarget target) {
		LOGGER.debug("Adding value to {}", itemWrapper);

		OperationResult result = new OperationResult(OPERATION_CREATE_NEW_VALUE);
		
		VW newValueWrapper = null;
		try {

			if (!(itemWrapper instanceof PrismContainerWrapper)) {
				itemWrapper.getItem().add(newValue);
			}

			Task task = pageBase.createSimpleTask(OPERATION_CREATE_NEW_VALUE);

			WrapperContext context = new WrapperContext(task, result);
			context.setObjectStatus(itemWrapper.findObjectStatus());
			context.setShowEmpty(true);
			context.setCreateIfEmpty(true);

			newValueWrapper = pageBase.createValueWrapper(itemWrapper, newValue, ValueStatus.ADDED, context);
			itemWrapper.getValues().add(newValueWrapper);
			result.recordSuccess();

		} catch (SchemaException e) {
			LOGGER.error("Cannot create new value for {}", itemWrapper, e);
			result.recordFatalError("Cannot create value wrapper for " + newValue + ". Reason: " + e.getMessage(), e);
			target.add(pageBase.getFeedbackPanel());
		}

		return newValueWrapper;
	}

}

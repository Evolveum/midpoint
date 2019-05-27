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
package com.evolveum.midpoint.gui.impl.prism;

import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ValueStatus;

/**
 * @author katka
 *
 */
public class PrismReferenceValueWrapperImpl<T extends Referencable> extends PrismValueWrapperImpl<T, PrismReferenceValue> {
	
	private static final long serialVersionUID = 1L;

	private static final transient Trace LOGGER = TraceManager.getTrace(PrismReferenceValueWrapperImpl.class);

	public PrismReferenceValueWrapperImpl(ItemWrapper<?, ?, ?, ?> parent, PrismReferenceValue value, ValueStatus status) {
		super(parent, value, status);
	}
	
	private boolean editEnabled = true;

	@Override
	public void setRealValue(T realValue) {
		LOGGER.info("#####$$$$$Nothing to do");
	}
	
	public boolean isEditEnabled() {
		return editEnabled;
	}
	
	public void setEditEnabled(boolean editEnabled) {
		this.editEnabled = editEnabled;
	}

}

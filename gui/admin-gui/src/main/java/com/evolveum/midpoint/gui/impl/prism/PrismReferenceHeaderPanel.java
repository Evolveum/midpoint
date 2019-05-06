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

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LambdaModel;

import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.Referencable;

/**
 * @author katka
 *
 */
public class PrismReferenceHeaderPanel<R extends Referencable> extends ItemHeaderPanel<PrismReferenceValue, PrismReference, PrismReferenceDefinition, PrismReferenceWrapper<R>> {

	private static final long serialVersionUID = 1L;
	
	public PrismReferenceHeaderPanel(String id, IModel<PrismReferenceWrapper<R>> model) {
		super(id, model);
	}


	@Override
	protected void initButtons() {
		// TODO Auto-generated method stub
		
	}


	@Override
	protected Component createTitle(IModel<String> label) {
		Label displayName = new Label(ID_LABEL, label);
        
        return displayName;
	}

}

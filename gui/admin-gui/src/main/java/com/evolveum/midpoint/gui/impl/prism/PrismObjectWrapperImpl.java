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

import java.util.List;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * @author katka
 *
 */
public class PrismObjectWrapperImpl<O extends ObjectType> extends PrismContainerWrapperImpl<O> implements PrismObjectWrapper<O> {

	private static final long serialVersionUID = 1L;
	
	private PrismObject<O> prismObjectNew;
	private PrismObject<O> prismObjectOld;
	
	public PrismObjectWrapperImpl(PrismObject<O> item, ItemStatus status) {
		super(null, item, status);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public PrismObject<O> getObject() {
		return prismObjectNew;
	}
	
	@Override
	public ObjectDelta<O> getObjectDelta() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public String getOid() {
		return prismObjectOld.getOid();
	}

	/* (non-Javadoc)
	 * @see com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper#getContainers()
	 */
	@Override
	public List<PrismContainerWrapper<?>> getContainers() {
		// TODO Auto-generated method stub
		return null;
	}
	

}

/*
 * Copyright (c) 2010-2019 Evolveum
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

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.ShadowWrapper;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.page.admin.users.dto.UserDtoStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author skublik
 *
 */
public class ShadowWrapperImpl<S extends ShadowType> extends PrismObjectWrapperImpl<S> implements ShadowWrapper<S> {

	private static final long serialVersionUID = 1L;
	
	UserDtoStatus status;
	boolean noFetch = false;

	public ShadowWrapperImpl(PrismObject<S> item, ItemStatus status) {
		super(item, status);
	}

	@Override
	public UserDtoStatus getProjectionStatus() {
		return status;
	}

	@Override
	public void setProjectionStatus(UserDtoStatus status) {
		this.status = status;
	}

	@Override
	public boolean isLoadWithNoFetch() {
		return noFetch;
	}

	@Override
	public void setLoadWithNoFetch(boolean noFetch) {
		this.noFetch = noFetch;
	}
}

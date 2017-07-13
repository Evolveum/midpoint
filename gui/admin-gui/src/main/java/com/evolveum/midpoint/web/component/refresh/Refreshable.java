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

package com.evolveum.midpoint.web.component.refresh;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;

/**
 * Refreshable page (or component).
 *
 * @author mederly
 */
public interface Refreshable {

	/**
	 * Called on manually requested refresh action.
	 * @param target The request target.
	 */
	void refresh(AjaxRequestTarget target);

	/**
	 * Component to which the refreshing timer should be attached.
	 */
	Component getRefreshingBehaviorParent();

	/**
	 * Current refreshing interval (may depend on page content).
	 */
	int getRefreshInterval();
}

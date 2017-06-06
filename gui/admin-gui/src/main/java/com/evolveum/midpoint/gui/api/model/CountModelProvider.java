/**
 * Copyright (c) 2016 Evolveum
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
package com.evolveum.midpoint.gui.api.model;

import org.apache.wicket.model.IModel;

/**
 * Interface for objects that provide Wicket model which represents
 * object count or similar tag. The count in usually displayed as a
 * small "bubble" in the tab, next to the menu item, etc.
 * 
 * @author semancik
 */
public interface CountModelProvider {
	
	/**
	 * Return count model. May return null. If null is
	 * returned then no count should be displayed.
	 */
	IModel<String> getCountModel();

}

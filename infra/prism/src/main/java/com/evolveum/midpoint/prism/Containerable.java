/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.prism;

import java.io.Serializable;

/**
 * @author semancik
 *
 */
public interface Containerable extends Serializable {


	PrismContainerValue asPrismContainerValue();

	/**
	 * Setup value to the containerable representation. This is used to after (empty) containerable is created to
	 * initialize it with a correct prism container value.
	 * Note: This method DOES NOT change the container value parent.
	 */
	void setupContainerValue(PrismContainerValue container);

}

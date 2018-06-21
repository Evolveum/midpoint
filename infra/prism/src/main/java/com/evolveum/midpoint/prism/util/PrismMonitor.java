/**
 * Copyright (c) 2016-2017 Evolveum
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
package com.evolveum.midpoint.prism.util;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;

/**
 * Interface to plug in a monitoring code to prism. Implementation of this
 * interface are called when selected important (usually expensive) operations
 * take place in prism. This can be used for gathering stats, making assertions
 * in the test code, etc.
 *
 * @author semancik
 */
public interface PrismMonitor {

	<O extends Objectable> void recordPrismObjectCompareCount(PrismObject<O> thisObject, Object thatObject);

	<O extends Objectable> void beforeObjectClone(PrismObject<O> orig);

	<O extends Objectable> void afterObjectClone(PrismObject<O> orig, PrismObject<O> clone);

}

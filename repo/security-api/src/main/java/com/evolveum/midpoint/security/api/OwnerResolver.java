/**
 * Copyright (c) 2014-2017 Evolveum
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
package com.evolveum.midpoint.security.api;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * This would be more appropriate in the security-impl. But we need it as low as this.
 * Otherwise there is a dependency cycle (task->security->repo-common->task)
 * Moving this to task yields better cohesion. So, it may in fact belong here.
 * 
 * @author semancik
 */
public interface OwnerResolver {

	<F extends FocusType, O extends ObjectType> PrismObject<F> resolveOwner(PrismObject<O> object);

}

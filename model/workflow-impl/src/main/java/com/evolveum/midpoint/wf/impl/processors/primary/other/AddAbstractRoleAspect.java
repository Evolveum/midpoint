/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.wf.impl.processors.primary.other;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import org.apache.commons.lang3.Validate;
import org.springframework.stereotype.Component;

/**
 * Change aspect that manages abstract role (i.e. Role or Org) addition.
 *
 * @author mederly
 */
@Component
public class AddAbstractRoleAspect extends AddObjectAspect<AbstractRoleType> {

    @Override
    protected Class<AbstractRoleType> getObjectClass() {
        return AbstractRoleType.class;
    }

    @Override
    protected String getObjectLabel(AbstractRoleType object) {
        Validate.notNull(object);
        if (object instanceof RoleType) {
            return "role " + object.getName();
        } else if (object instanceof OrgType) {
            return "org " + object.getName();
        } else {
            // should not occur
            return object.getClass().getSimpleName() + " " + object.getName();
        }
    }
}

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
package com.evolveum.midpoint.gui.impl.factory;

import com.evolveum.midpoint.gui.api.component.password.PasswordPropertyPanel;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.impl.prism.*;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Created by honchar
 */
@Component
public class ProtectedStringWrapperFactory extends PrismPropertyWrapperFactoryImpl<ProtectedStringType>{

    @Override
    public boolean match(ItemDefinition<?> def) {
        return QNameUtil.match(ProtectedStringType.COMPLEX_TYPE, def.getTypeName()) ;
    }

    @PostConstruct
    @Override
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    public int getOrder() {
        return 1010;
    }

    @Override
    protected PrismPropertyWrapper<ProtectedStringType> createWrapper(PrismContainerValueWrapper<?> parent, PrismProperty<ProtectedStringType> item,
                                                                      ItemStatus status) {
        ProtectedStringTypeWrapperImpl propertyWrapper = new ProtectedStringTypeWrapperImpl(parent, item, status);
        getRegistry().registerWrapperPanel(item.getDefinition().getTypeName(), PasswordPropertyPanel.class);
        return propertyWrapper;
    }

}

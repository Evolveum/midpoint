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

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.impl.prism.*;
import com.evolveum.midpoint.gui.impl.prism.component.ExpressionPropertyPanel;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.gui.impl.prism.ExpressionWrapper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import net.sf.jasperreports.olap.mapping.Mapping;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Created by honchar
 */
@Component
public class ExpressionWrapperFactory  extends PrismPropertyWrapperFactoryImpl<ExpressionType> {

    @Override
    public boolean match(ItemDefinition<?> def) {
        return QNameUtil.match(ExpressionType.COMPLEX_TYPE, def.getTypeName()) ;
    }

    @PostConstruct
    @Override
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    public int getOrder() {
        return 100;
    }

    @Override
    protected PrismPropertyWrapper<ExpressionType> createWrapper(PrismContainerValueWrapper<?> parent, PrismProperty<ExpressionType> item,
                                                                 ItemStatus status) {
        getRegistry().registerWrapperPanel(item.getDefinition().getTypeName(), ExpressionPropertyPanel.class);
        ExpressionWrapper propertyWrapper = new ExpressionWrapper(parent, item, status);
            return propertyWrapper;
    }

}

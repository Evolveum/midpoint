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

package com.evolveum.midpoint.web.util;

import org.apache.wicket.core.request.handler.ListenerInterfaceRequestHandler;
import org.apache.wicket.core.request.mapper.MountedMapper;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.component.IRequestablePage;
import org.apache.wicket.request.mapper.info.PageComponentInfo;
import org.apache.wicket.request.mapper.parameter.IPageParametersEncoder;

/**
 * @author lazyman
 */
public class MountedMapperWithoutPageComponentInfo extends MountedMapper {

    public MountedMapperWithoutPageComponentInfo(String mountPath,
                                                 Class<? extends IRequestablePage> pageClass,
                                                 IPageParametersEncoder encoder) {

        super(mountPath, pageClass, encoder);
    }

    @Override
    protected void encodePageComponentInfo(Url url, PageComponentInfo info) {
        //do nothing, we don't want to render page version in url
    }

    @Override
    public Url mapHandler(IRequestHandler requestHandler) {
        if (requestHandler instanceof ListenerInterfaceRequestHandler) {
            return null;
        }

        return super.mapHandler(requestHandler);
    }
}

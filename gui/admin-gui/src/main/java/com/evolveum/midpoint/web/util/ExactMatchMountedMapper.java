/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.web.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.core.request.mapper.MountedMapper;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.component.IRequestablePage;
import org.apache.wicket.request.mapper.parameter.IPageParametersEncoder;

/**
 * Created by lazyman on 09/03/2017.
 */
public class ExactMatchMountedMapper extends MountedMapper {

    public ExactMatchMountedMapper(String mountPath,
                                   Class<? extends IRequestablePage> pageClass,
                                   IPageParametersEncoder pageParametersEncoder) {
        super(mountPath, pageClass, pageParametersEncoder);
    }

    /**
     * We want to fully match url path. Parent class is OK with partial match and then
     * marking other path element as page parameters.
     *
     * @param url
     * @return
     */
    @Override
    protected boolean urlStartsWithMountedSegments(Url url) {
        if (url == null) {
            return false;
        }

        String mountUrl = StringUtils.join(mountSegments, "/");
        return url.getPath().equals(mountUrl);
    }
}

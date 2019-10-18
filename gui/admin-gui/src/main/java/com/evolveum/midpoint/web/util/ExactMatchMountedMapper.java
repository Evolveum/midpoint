/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.util;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.core.request.mapper.MountedMapper;
import org.apache.wicket.request.Url;
import org.apache.wicket.request.component.IRequestablePage;
import org.apache.wicket.request.mapper.parameter.IPageParametersEncoder;
import org.apache.wicket.request.mapper.parameter.PageParametersEncoder;

/**
 * Created by lazyman on 09/03/2017.
 */
public class ExactMatchMountedMapper extends MountedMapper {

    private static final Trace LOG = TraceManager.getTrace(ExactMatchMountedMapper.class);

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

        if (!(pageParametersEncoder instanceof PageParametersEncoder)) {
            LOG.trace("Matching using standard mounted mapper for '{}'", url);
            return super.urlStartsWithMountedSegments(url);
        }

        String mountUrl = StringUtils.join(mountSegments, "/");
        boolean matched = url.getPath().equals(mountUrl);

        LOG.trace("Matched: {} for '{}' with mount url '{}'", matched, url, mountUrl);
        return matched;
    }
}

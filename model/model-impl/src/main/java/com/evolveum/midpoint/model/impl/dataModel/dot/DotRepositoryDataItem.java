/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.dataModel.dot;

import com.evolveum.midpoint.model.impl.dataModel.model.RepositoryDataItem;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.apache.commons.lang.StringUtils;

/**
 * @author mederly
 */
public class DotRepositoryDataItem implements DotDataItem {

    private static final String COLOR_USER = "darkred";
    private static final String COLOR_ROLE = "darkgreen";
    private static final String COLOR_ORG = "darkorange";
    private static final String COLOR_DEFAULT = "black";
    private static final String COLOR_FILL = "grey92";

    private RepositoryDataItem dataItem;

    public DotRepositoryDataItem(RepositoryDataItem dataItem) {
        this.dataItem = dataItem;
    }

    @Override
    public String getNodeName() {
        return "\"" + dataItem.getTypeName().getLocalPart() + "." + dataItem.getItemPath() + "\"";
    }

    @Override
    public String getNodeLabel() {
        String entity = StringUtils.removeEnd(dataItem.getTypeName().getLocalPart(), "Type");
        String pathString = dataItem.getItemPath().toString();
        final String ext = "extension/";
        if (pathString.startsWith(ext)) {
            entity += " extension";
            pathString = pathString.substring(ext.length());
        }
        return entity + "&#10;" + pathString;
    }

    @Override
    public String getNodeStyleAttributes() {
        return "style=filled, fillcolor=" + COLOR_FILL + ", color=" + getBorderColor();
    }

    private String getBorderColor() {
        if (QNameUtil.match(UserType.COMPLEX_TYPE, dataItem.getTypeName())) {
            return COLOR_USER;
        } else if (QNameUtil.match(RoleType.COMPLEX_TYPE, dataItem.getTypeName())) {
            return COLOR_ROLE;
        } else if (QNameUtil.match(OrgType.COMPLEX_TYPE, dataItem.getTypeName())) {
            return COLOR_ORG;
        } else {
            return COLOR_DEFAULT;
        }
    }

}

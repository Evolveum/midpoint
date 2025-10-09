/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.dataModel.dot;

import com.evolveum.midpoint.model.impl.dataModel.model.ResourceDataItem;
import com.evolveum.midpoint.prism.polystring.PolyString;
import org.jetbrains.annotations.NotNull;

public class DotResourceDataItem implements DotDataItem {

    private final ResourceDataItem dataItem;
    private final DotModel dotModel;

    DotResourceDataItem(ResourceDataItem dataItem, DotModel dotModel) {
        this.dataItem = dataItem;
        this.dotModel = dotModel;
    }

    @Override
    public String getNodeName() {
        return "\"" + getResourceName() + ":" +
                dotModel.getObjectTypeName(dataItem.getObjectDefinition(), false) + ":" + dataItem.getItemPath() + "\"";
    }

    @Override
    public String getNodeLabel() {
        return dataItem.getLastItemName().getLocalPart();
    }

    @Override
    public String getNodeStyleAttributes() {
        return "";
    }

    @NotNull
    public String getResourceName() {
        PolyString name = dotModel.getDataModel().getResource(dataItem.getResourceOid()).getName();
        return name != null ? name.getOrig() : dataItem.getResourceOid();
    }

}

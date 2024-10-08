package com.evolveum.midpoint.gui.impl.page.admin.role.mining.model;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyValueWrapper;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RangeType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisDetectionOptionType;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import java.io.Serializable;

public class RangeDto implements Serializable {

    IModel<PrismPropertyValueWrapper<RangeType>> model;
    double max;
    boolean doubleType;
    ItemName itemName;

    public RangeDto(IModel<PrismPropertyValueWrapper<RangeType>> model, ItemName itemName) {
        this.model = model;
        this.itemName = itemName;
        if (RoleAnalysisDetectionOptionType.F_FREQUENCY_RANGE.equals(itemName)) {
            this.doubleType = true;
            this.max = 100.0;
        } else {
            this.max = 1000000.0;
            this.doubleType = false;
        }
    }

    public RangeDto(IModel<PrismPropertyValueWrapper<RangeType>> model, double max, boolean doubleType, ItemName itemName) {
        this.model = model;
        this.max = max;
        this.doubleType = doubleType;
        this.itemName = itemName;
    }

    public IModel<PrismPropertyValueWrapper<RangeType>> getModel() {
        return model;
    }

    public double getMax() {
        return max;
    }

    public boolean isDoubleType() {
        return doubleType;
    }

    public ItemName getItemName() {
        return itemName;
    }

    public StringResourceModel getMinTitle(PageBase pageBase) {
        boolean stdField = RoleAnalysisDetectionOptionType.F_STANDARD_DEVIATION.equals(itemName);
        return stdField
                ? pageBase.createStringResource("RangeSimplePanel.negativeConfidence")
                : pageBase.createStringResource("RangeType.min");
    }

    public StringResourceModel getMaxTitle(PageBase pageBase) {
        boolean stdField = RoleAnalysisDetectionOptionType.F_STANDARD_DEVIATION.equals(itemName);
        return stdField
                ? pageBase.createStringResource("RangeSimplePanel.positiveConfidence")
                : pageBase.createStringResource("RangeType.max");
    }
}

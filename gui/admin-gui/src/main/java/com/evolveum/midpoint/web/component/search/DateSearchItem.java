/*
 * Copyright (c) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DisplayableValue;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
/**
 * @author honchar
 */
public class DateSearchItem<T extends Serializable> extends PropertySearchItem<T>{

    private static final long serialVersionUID = 1L;

    private IModel<XMLGregorianCalendar> fromDateModel;
    private IModel<XMLGregorianCalendar> toDateModel;

    public DateSearchItem(Search search, ItemPath path, ItemDefinition definition, IModel<XMLGregorianCalendar> dateFromModel,
            IModel<XMLGregorianCalendar> dateToModel) {
        super(search, path, definition, null);
        this.fromDateModel = fromDateModel;
        this.toDateModel = toDateModel;
    }

    public IModel<XMLGregorianCalendar> getFromDateModel() {
        return fromDateModel;
    }

    public IModel<XMLGregorianCalendar> getToDateModel() {
        return toDateModel;
    }

    @Override
    public Type getType(){
        return Type.DATE;
    }

}

/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.model;

import com.evolveum.midpoint.util.MiscUtil;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Date;

/**
 * @author lazyman
 */
public class XmlGregorianCalendarModel extends Model<Date> {

    private IModel<XMLGregorianCalendar> model;

    public XmlGregorianCalendarModel(IModel<XMLGregorianCalendar> model) {
        this.model = model;
    }

    @Override
    public Date getObject() {
        XMLGregorianCalendar calendar = model.getObject();
        if (calendar == null) {
            return null;
        }
        return MiscUtil.asDate(calendar);
    }

    @Override
    public void setObject(Date object) {
        if (object == null) {
            model.setObject(null);
        } else {
            model.setObject(MiscUtil.asXMLGregorianCalendar(object));
        }
    }
}

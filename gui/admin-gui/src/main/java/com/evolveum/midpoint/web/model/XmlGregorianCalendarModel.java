/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.model;

import com.evolveum.midpoint.util.MiscUtil;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

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
            return;
        }

        XMLGregorianCalendar newValue = MiscUtil.asXMLGregorianCalendar(object);

        Long currentMinuteMillis = getNormalizedMinuteEpochMillis(model.getObject());
        if (currentMinuteMillis != null) {
            // this check is done on UI side to prevent stripping of seconds and milliseconds when date was not changed
            // This happens because of the way how date picker works - it doesn't have seconds/miliseconds field therefore
            // those fields submitted via html form are always zeroed.
            // See MID-9733 for more info.
            Long newMinuteMillis = getNormalizedMinuteEpochMillis(newValue);
            if (currentMinuteMillis.equals(newMinuteMillis)) {
                return;
            }
        }

        model.setObject(newValue);
    }

    private Long getNormalizedMinuteEpochMillis(XMLGregorianCalendar cal) {
        if (cal == null) {
            return null;
        }
        GregorianCalendar c = cal.toGregorianCalendar();
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        return c.getTimeInMillis();
    }
}

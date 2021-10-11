/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.test.asserter;

import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TriggerType;

import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author semancik
 */
public class TriggerAsserter<R> extends AbstractAsserter<R> {

    final private TriggerType trigger;

    public TriggerAsserter(TriggerType trigger) {
        super();
        this.trigger = trigger;
    }

    public TriggerAsserter(TriggerType trigger, String detail) {
        super(detail);
        this.trigger = trigger;
    }

    public TriggerAsserter(TriggerType trigger, R returnAsserter, String detail) {
        super(returnAsserter, detail);
        this.trigger = trigger;
    }

    protected TriggerType getTrigger() {
        return trigger;
    }

    public TriggerAsserter<R> assertHandlerUri() {
        assertNotNull("No handler URI in "+desc(), trigger.getHandlerUri());
        return this;
    }

    public TriggerAsserter<R> assertHandlerUri(String expected) {
        assertEquals("Wrong handler URI in "+desc(), expected, trigger.getHandlerUri());
        return this;
    }

    public TriggerAsserter<R> assertTimestamp() {
        assertNotNull("No timestamp in "+desc(), trigger.getTimestamp());
        return this;
    }

    public TriggerAsserter<R> assertTimestamp(XMLGregorianCalendar expected) {
        assertEquals("Wrong timestamp in "+desc(), expected, trigger.getTimestamp());
        return this;
    }

    public TriggerAsserter<R> assertTimestampBetween(XMLGregorianCalendar start, XMLGregorianCalendar end) {
        TestUtil.assertBetween("Wrong timestamp in "+desc(), start, end, trigger.getTimestamp());
        return this;
    }

    public TriggerAsserter<R> assertTimestampFutureBetween(XMLGregorianCalendar start, XMLGregorianCalendar end, String durationOffset) {
        TestUtil.assertBetween("Wrong timestamp in "+desc(),
                XmlTypeConverter.addDuration(start, durationOffset),
                XmlTypeConverter.addDuration(end, durationOffset),
                trigger.getTimestamp());
        return this;
    }

    public TriggerAsserter<R> assertTimestampFuture(String durationOffset, long tolerance) {
        assertTimestampFuture(getClock().currentTimeXMLGregorianCalendar(), durationOffset, tolerance);
        return this;
    }

    public TriggerAsserter<R> assertTimestampFuture(XMLGregorianCalendar now, String durationOffset, long tolerance) {
        Duration offsetDuration = XmlTypeConverter.createDuration(durationOffset);
        XMLGregorianCalendar mid = XmlTypeConverter.addDuration(now, offsetDuration);
        XMLGregorianCalendar start = XmlTypeConverter.addMillis(mid, -tolerance);
        XMLGregorianCalendar end = XmlTypeConverter.addMillis(mid, tolerance);
        TestUtil.assertBetween("Wrong timestamp in "+desc(), start, end, trigger.getTimestamp());
        return this;
    }

    protected String desc() {
        // TODO: better desc
        return descWithDetails(trigger);
    }

    public TriggerAsserter<R> display() {
        display(desc());
        return this;
    }

    public TriggerAsserter<R> display(String message) {
        IntegrationTestTools.display(message, trigger);
        return this;
    }
}

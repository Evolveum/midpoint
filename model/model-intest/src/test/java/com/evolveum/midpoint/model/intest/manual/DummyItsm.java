/*
 * Copyright (c) 2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.manual;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.CommonException;

/**
 * Dummy ITSM system for testing.
 *
 * @author semancik
 */
public class DummyItsm implements DebugDumpable {

    private static DummyItsm instance = new DummyItsm();
    private List<DummyItsmTicket> tickets = new ArrayList<>();
    private int nextId = 1;
    private Class<? extends CommonException> failureClass;

    public static DummyItsm getInstance() {
        return instance;
    }

    public Class<? extends CommonException> getFailureClass() {
        return failureClass;
    }

    public void setFailureClass(Class<? extends CommonException> failureClass) {
        this.failureClass = failureClass;
    }

    public void clearFailureClass() {
        this.failureClass = null;
    }

    public List<DummyItsmTicket> getTickets() {
        return tickets;
    }

    private synchronized String nextIdentifier() {
        String identifier = String.format("%04d", nextId);
        nextId++;
        return identifier;
    }

    public String createTicket(String body) throws CommonException {
        tryToFail();
        DummyItsmTicket ticket = new DummyItsmTicket(nextIdentifier());
        ticket.setBody(body);
        tickets.add(ticket);
        return ticket.getIdentifier();
    }

    public DummyItsmTicket findTicket(String identifier) throws CommonException {
        tryToFail();
        for (DummyItsmTicket ticket: tickets) {
            if (ticket.getIdentifier().equals(identifier)) {
                return ticket;
            }
        }
        return null;
    }

    private void tryToFail() throws CommonException {
        if (failureClass == null) {
            return;
        }
        CommonException exception;
        try {
            Constructor<? extends CommonException> constructor = failureClass.getConstructor(String.class);
            exception = constructor.newInstance("Simulated error: "+failureClass.getSimpleName());
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
            throw new RuntimeException("Failed to fail: "+e.getMessage(), e);
        }
        throw exception;
    }

    public void test() throws CommonException {
        tryToFail();
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(DummyItsm.class, indent);
        DebugUtil.debugDumpWithLabelLn(sb, "tickets", tickets, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "failureClass", failureClass, indent + 1);
        return sb.toString();
    }

}

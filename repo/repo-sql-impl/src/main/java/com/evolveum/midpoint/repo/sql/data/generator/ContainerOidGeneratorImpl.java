package com.evolveum.midpoint.repo.sql.data.generator;

import java.util.EnumSet;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.generator.BeforeExecutionGenerator;
import org.hibernate.generator.EventType;
import org.hibernate.generator.EventTypeSets;
import org.hibernate.id.IdentifierGenerationException;
import org.hibernate.id.IdentifierGenerator;

import com.evolveum.midpoint.repo.sql.data.common.container.Container;

public class ContainerOidGeneratorImpl implements BeforeExecutionGenerator, IdentifierGenerator {

    @Override
    public Object generate(SharedSessionContractImplementor session, Object object) {
        return generate(object);
    }

    @Override
    public Object generate(SharedSessionContractImplementor session, Object owner, Object currentValue, EventType eventType) {
        return generate(owner);
    }

    private Object generate(Object owner) {
        if (!(owner instanceof Container<?> container)) {
            throw new IdentifierGenerationException("Can't generate container OID for owner that is not a container: " + owner);
        }

        if (container.getOwnerOid() != null) {
            return container.getOwnerOid();
        }

        if (container.getOwner() == null) {
            throw new IdentifierGenerationException("Can't generate container OID for container without owner: " + owner);
        }

        String oid = container.getOwner().getOid();
        if (oid != null) {
            return oid;
        }

        throw new IdentifierGenerationException("Can't generate container OID for container without owner OID: " + owner);
    }

    @Override
    public EnumSet<EventType> getEventTypes() {
        return EventTypeSets.INSERT_ONLY;
    }
}

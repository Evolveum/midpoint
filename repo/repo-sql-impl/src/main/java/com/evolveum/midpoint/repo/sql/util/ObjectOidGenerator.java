package com.evolveum.midpoint.repo.sql.util;

import com.evolveum.midpoint.repo.sql.data.common.RObject;
import org.apache.commons.lang.StringUtils;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;

import java.io.Serializable;
import java.util.UUID;

/**
 * @author lazyman
 */
public class ObjectOidGenerator implements IdentifierGenerator {

    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object object) throws HibernateException {
        RObject rObject = (RObject) object;
        if (StringUtils.isNotEmpty(rObject.getOid())) {
            return rObject.getOid();
        }

        return UUID.randomUUID().toString();
    }
}

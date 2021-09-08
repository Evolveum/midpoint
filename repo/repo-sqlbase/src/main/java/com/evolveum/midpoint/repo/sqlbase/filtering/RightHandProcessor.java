package com.evolveum.midpoint.repo.sqlbase.filtering;

import com.evolveum.midpoint.prism.query.ValueFilter;
import com.evolveum.midpoint.repo.sqlbase.RepositoryException;
import com.querydsl.core.types.Expression;

public interface RightHandProcessor {

    public Expression<?> rightHand(ValueFilter<?, ?> filter) throws RepositoryException;

}

package com.evolveum.midpoint.repo.sql.data.generator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.annotations.IdGeneratorType;
import org.hibernate.annotations.ValueGenerationType;

/**
 * FIXME this new way of creating generators @ContainerOidGenerator doesn't replace existing default
 *  ForeignGenerator instance when used in conjunction with @MapsId probably related to
 *  https://hibernate.atlassian.net/browse/HHH-18124
 */
@IdGeneratorType(ContainerOidGeneratorImpl.class)
@ValueGenerationType(generatedBy = ContainerOidGeneratorImpl.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
public @interface ContainerOidGenerator {
}

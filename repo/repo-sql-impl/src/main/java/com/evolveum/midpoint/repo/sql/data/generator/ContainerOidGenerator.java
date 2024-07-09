package com.evolveum.midpoint.repo.sql.data.generator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.hibernate.annotations.IdGeneratorType;
import org.hibernate.annotations.ValueGenerationType;

@IdGeneratorType(ContainerOidGeneratorImpl.class)
@ValueGenerationType(generatedBy = ContainerOidGeneratorImpl.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
public @interface ContainerOidGenerator {
}

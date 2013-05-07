package com.evolveum.midpoint.repo.sql.data.common.type;

import com.evolveum.midpoint.repo.sql.data.common.RObjectReference;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * @author lazyman
 */
@Entity
@DiscriminatorValue(RIncludeRef.DISCRIMINATOR)
public class RIncludeRef extends RObjectReference {

    public static final String DISCRIMINATOR = "7";
}

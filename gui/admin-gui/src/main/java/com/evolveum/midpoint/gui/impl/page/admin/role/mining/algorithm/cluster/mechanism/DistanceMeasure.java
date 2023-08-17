
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.cluster.mechanism;

import java.io.Serializable;
import java.util.Set;

import org.apache.commons.math3.exception.DimensionMismatchException;

public interface DistanceMeasure extends Serializable {
    double compute(Set<String> valueA, Set<String> valueB) throws DimensionMismatchException;
}

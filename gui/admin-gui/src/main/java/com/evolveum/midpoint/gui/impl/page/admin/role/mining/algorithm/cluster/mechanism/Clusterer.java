
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.cluster.mechanism;

import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.utils.Handler;

import org.apache.commons.math3.exception.ConvergenceException;
import org.apache.commons.math3.exception.MathIllegalArgumentException;

public abstract class Clusterer<T extends Clusterable> {
    private final DistanceMeasure measure;

    protected Clusterer(DistanceMeasure measure) {
        this.measure = measure;
    }

    public abstract List<? extends Cluster<T>> cluster(Collection<T> var1, Handler handler) throws MathIllegalArgumentException, ConvergenceException;

    protected double distance(Clusterable p1, Clusterable p2) {
        return this.measure.compute(p1.getPoint(), p2.getPoint());
    }
}

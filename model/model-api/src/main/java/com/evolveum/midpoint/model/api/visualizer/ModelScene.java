/*
 * Copyright (c) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.visualizer;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

/**
 * TODO Better name probably (maybe RootScene, ParentScene, ModelContextScene)
 *
 * Created by Viliam Repan (lazyman).
 */
public class ModelScene {

    private List<Scene> primaryScenes;

    private List<Scene> secondaryScenes;

    public ModelScene() {
        this(null, null);
    }

    public ModelScene(List<Scene> primaryScenes, List<Scene> secondaryScenes) {
        this.primaryScenes = primaryScenes;
        this.secondaryScenes = secondaryScenes;
    }

    @NotNull
    public List<Scene> getPrimaryScenes() {
        if (primaryScenes == null) {
            primaryScenes = new ArrayList<>();
        }
        return primaryScenes;
    }

    @NotNull
    public List<Scene> getSecondaryScenes() {
        if (secondaryScenes == null) {
            secondaryScenes = new ArrayList<>();
        }
        return secondaryScenes;
    }
}

/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.model;

import java.io.Serializable;
import java.util.*;

import com.evolveum.midpoint.common.mining.objects.chunk.DisplayValueOption;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;

public class OperationPanelModel implements Serializable {

    @NotNull List<DetectedPattern> selectedPatterns = new ArrayList<>();
    Map<String, String> palletColors = new HashMap<>();
    String patternIconClass = GuiStyleConstants.CLASS_DETECTED_PATTERN_ICON + " fa-2x text-primary";
    String candidateRoleIconClass = GuiStyleConstants.CLASS_CANDIDATE_ROLE_ICON + " fa-2x text-success";
    String bgIconClass;
    List<DetectedPattern> patterns = new ArrayList<>();
    String selectedButtonColor = "#627383";
    List<DetectedPattern> candidatesRoles = new ArrayList<>();
    boolean isCompareMode = false;
    boolean isCandidateRoleView = false;
    DisplayValueOption displayValueOption;

    public OperationPanelModel() {
    }

    public void removeSelectedPattern(DetectedPattern pattern) {
        this.selectedPatterns.remove(pattern);
    }

    public void clearSelectedPatterns() {
        this.selectedPatterns.clear();
    }

    public void createDetectedPatternModel(List<DetectedPattern> patterns) {
        this.patterns = patterns;
        this.bgIconClass = "bg-secondary";
    }

    public void createCandidatesRolesRoleModel(List<DetectedPattern> candidatesRoles) {
        this.candidatesRoles = candidatesRoles;
        this.bgIconClass = "bg-light";
    }

    public String getBgIconClass() {
        return bgIconClass;
    }

    public void setBgIconClass(String bgIconClass) {
        this.bgIconClass = bgIconClass;
    }

    public List<DetectedPattern> getPatterns() {
        return patterns;
    }

    public void setPatterns(List<DetectedPattern> patterns) {
        this.patterns = patterns;
    }

    public @NotNull List<DetectedPattern> getSelectedPatterns() {
        return selectedPatterns;
    }

    public void setSelectedPatterns(@NotNull List<DetectedPattern> selectedPatterns) {
        this.selectedPatterns = selectedPatterns;
    }

    public String getSelectedButtonColor() {
        return selectedButtonColor;
    }

    public void setSelectedButtonColor(String selectedButtonColor) {
        this.selectedButtonColor = selectedButtonColor;
    }

    public void addSelectedPattern(DetectedPattern pattern) {
        if (pattern == null) {
            return;
        }

        for (DetectedPattern selectedPattern : this.selectedPatterns) {
            String identifier = selectedPattern.getIdentifier();
            if (identifier.equals(pattern.getIdentifier())) {
                removeSelectedPattern(selectedPattern);
                return;
            }
        }

        this.selectedPatterns.add(pattern);

        this.palletColors = generateObjectColors(this.selectedPatterns);
    }

    public void addSelectedPatternSingleAllowed(DetectedPattern pattern) {
        if (pattern == null) {
            return;
        }

        for (DetectedPattern selectedPattern : this.selectedPatterns) {
            String identifier = selectedPattern.getIdentifier();
            if (identifier == null || identifier.isEmpty() || identifier.equals(pattern.getIdentifier())) {
                removeSelectedPattern(selectedPattern);
                return;
            }
        }
        if (!this.selectedPatterns.isEmpty()) {
            this.selectedPatterns.clear();
        }

        this.selectedPatterns.add(pattern);

        this.palletColors = generateObjectColors(this.selectedPatterns);
    }

    public void addSelectedPattern(List<DetectedPattern> patterns) {
        if (patterns == null) {
            return;
        }

        for (DetectedPattern pattern : patterns) {
            if (this.selectedPatterns.contains(pattern)) {
                removeSelectedPattern(pattern);
            }
            this.selectedPatterns.add(pattern);
        }

        this.palletColors = generateObjectColors(this.selectedPatterns);
    }

    public boolean isCompareMode() {
        return isCompareMode;
    }

    public void setCompareMode(boolean compareMode) {
        isCompareMode = compareMode;
    }

    public boolean isCandidateRoleView() {
        return isCandidateRoleView;
    }

    public void setCandidateRoleView(boolean candidateRoleView) {
        isCandidateRoleView = candidateRoleView;
    }

    public List<DetectedPattern> getCandidatesRoles() {
        return candidatesRoles;
    }

    public void setCandidatesRoles(List<DetectedPattern> candidatesRoles) {
        this.candidatesRoles = candidatesRoles;
    }

    public @NotNull String getPatternIconClass() {
        return patternIconClass;
    }

    public void setPatternIconClass(String patternIconClass) {
        this.patternIconClass = patternIconClass;
    }

    public @NotNull String getCandidateRoleIconClass() {
        return candidateRoleIconClass;
    }

    public void setCandidateRoleIconClass(String candidateRoleIconClass) {
        this.candidateRoleIconClass = candidateRoleIconClass;
    }

    public Map<String, String> getPalletColors() {
        return palletColors;
    }

    public void setPalletColors(Map<String, String> palletColors) {
        this.palletColors = palletColors;
    }

    protected static @NotNull Map<String, String> generateObjectColors(List<DetectedPattern> patterns) {
        if (patterns == null || patterns.isEmpty()) {
            return Collections.emptyMap();
        }

        int numberOfObjects = patterns.size();
        Map<String, String> objectColorMap = new HashMap<>();

        int baseGreen = 0x00A65A;
        patterns.get(0).setAssociatedColor("#00A65A");
        objectColorMap.put(patterns.get(0).getIdentifier(), "#00A65A");

        if (numberOfObjects == 1) {
            return objectColorMap;
        }

        int brightnessStep = 255 / numberOfObjects;

        if (numberOfObjects < 3) {
            brightnessStep = 30;
        } else if (numberOfObjects < 5) {
            brightnessStep = 40;
        }

        for (int i = 1; i < numberOfObjects; i++) {
            int brightness = 255 - (i * brightnessStep);
            int greenValue = (baseGreen & 0xFF0000) | (brightness << 8) | (baseGreen & 0x0000FF);
            String hexColor = String.format("#%06X", greenValue);
            patterns.get(i).setAssociatedColor(hexColor);
            objectColorMap.put(patterns.get(i).getIdentifier(), hexColor);
        }

        return objectColorMap;
    }

    public DisplayValueOption getDisplayValueOption() {
        return displayValueOption;
    }

    public void setDisplayValueOption(DisplayValueOption displayValueOption) {
        this.displayValueOption = displayValueOption;
    }
}

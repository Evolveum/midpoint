/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.model;

import java.io.Serializable;
import java.util.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;

public class OperationPanelModel implements Serializable {

    public static final String F_PALLET_COLORS = "palletColors";

    private Map<String, String> palletColors = new HashMap<>();
    private String patternIconClass = GuiStyleConstants.CLASS_DETECTED_PATTERN_ICON + " fa-2x text-primary";
    private String candidateRoleIconClass = GuiStyleConstants.CLASS_CANDIDATE_ROLE_ICON + " fa-2x text-success";
    private String bgIconClass;
    private String selectedButtonColor = "#627383";

    private List<DetectedPattern> patterns = new ArrayList<>();
    private List<DetectedPattern> candidatesRoles = new ArrayList<>();
    private List<DetectedPattern> outlierPatterns = new ArrayList<>();

    private boolean isCompareMode = false;
    private boolean isCandidateRoleView = false;

    List<DetectedPattern> allPatterns = new ArrayList<>();


    private boolean isPanelExpanded = false;
    private boolean showAsExpandCard = false;

    public OperationPanelModel() {
    }

    public void createDetectedPatternModel(List<DetectedPattern> patterns) {
        this.patterns = patterns;
        this.bgIconClass = "bg-secondary";
        this.allPatterns.addAll(patterns);
    }

    public void createCandidatesRolesRoleModel(List<DetectedPattern> candidatesRoles) {
        this.candidatesRoles = candidatesRoles;
        this.bgIconClass = "bg-light";
        this.allPatterns.addAll(candidatesRoles);
    }

    public void createOutlierPatternModel(List<DetectedPattern> outlierPatterns) {
        this.outlierPatterns = outlierPatterns;
        this.bgIconClass = "bg-danger";
        this.allPatterns.addAll(outlierPatterns);
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
        List<DetectedPattern> selectedPatterns = allPatterns.stream().filter(pattern -> pattern.isPatternSelected())
                .toList();
        this.palletColors = generateObjectColors(selectedPatterns); //TODO generate properly
        return selectedPatterns;
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

    public boolean isOutlierView() {
        return !outlierPatterns.isEmpty();
    } //TODO fix this properly

    public List<DetectedPattern> getOutlierPatterns() {
        return outlierPatterns;
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

    public boolean isPanelExpanded() {
        return isPanelExpanded;
    }

    public void setPanelExpanded(boolean panelExpanded) {
        isPanelExpanded = panelExpanded;
    }

    public boolean isShowAsExpandCard() {
        return showAsExpandCard;
    }

    public void setShowAsExpandCard(boolean showAsExpandCard) {
        this.showAsExpandCard = showAsExpandCard;
    }

    public void clearSelectedPatterns() {
        for (DetectedPattern pattern : allPatterns) {
            pattern.setPatternSelected(false);
            pattern.setAssociatedColor(null);
        }
        palletColors = new HashMap<>();
    }

    public void removeFromPalette(DetectedPattern detectedPattern) {
        detectedPattern.setAssociatedColor(null);
        palletColors.remove(detectedPattern.getIdentifier());
    }
}

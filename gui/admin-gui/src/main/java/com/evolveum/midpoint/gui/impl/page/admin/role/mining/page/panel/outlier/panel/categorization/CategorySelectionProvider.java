/*
 * Copyright (C) 2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.panel.categorization;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisIdentifiedCharacteristicsItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisObjectCategorizationType;

import org.apache.wicket.Component;
import org.jetbrains.annotations.NotNull;
import org.wicketstuff.select2.ChoiceProvider;
import org.wicketstuff.select2.Response;

import java.io.Serial;
import java.util.*;
import java.util.stream.Collectors;

import static com.evolveum.midpoint.gui.api.util.LocalizationUtil.translate;

public class CategorySelectionProvider extends ChoiceProvider<RoleAnalysisObjectCategorizationType> {
    @Serial private static final long serialVersionUID = 1L;

    boolean advanced;
    LoadableModel<Boolean> isRoleSelected;

    public CategorySelectionProvider(boolean advanced, LoadableModel<Boolean> isRoleSelected) {
        this.advanced = advanced;
        this.isRoleSelected = isRoleSelected;
    }

    @Override
    public String getDisplayValue(RoleAnalysisObjectCategorizationType value) {
        return getCategoryValueDisplayString(value, isRoleSelected.getObject());
    }

    @Override
    public String getIdValue(RoleAnalysisObjectCategorizationType value) {
        return value.toString();
    }

    @Override
    public void query(String text, int page, Response<RoleAnalysisObjectCategorizationType> response) {
        List<RoleAnalysisObjectCategorizationType> allowedValues = allowedValues(advanced, isRoleSelected);
        if (text == null) {
            response.addAll(allowedValues);
            return;
        }
        for (RoleAnalysisObjectCategorizationType value : allowedValues) {
            if (value.toString().toLowerCase().contains(text)) {
                response.add(value);
            }
        }
    }

    @Override
    public Collection<RoleAnalysisObjectCategorizationType> toChoices(@NotNull Collection<String> values) {
        List<RoleAnalysisObjectCategorizationType> choices = new ArrayList<>();
        values.stream().map(RoleAnalysisObjectCategorizationType::valueOf).collect(Collectors.toCollection(() -> choices));
        return choices;
    }

    public static @NotNull List<RoleAnalysisObjectCategorizationType> allowedValues(
            boolean advanced,
            @NotNull LoadableModel<Boolean> isRoleSelected) {

        List<RoleAnalysisObjectCategorizationType> allowedValues = new ArrayList<>();

        if (Boolean.TRUE.equals(isRoleSelected.getObject())) {
            addRoleAllowedValues(advanced, allowedValues);
        } else if (Boolean.FALSE.equals(isRoleSelected.getObject())) {
            addUserAllowedValues(advanced, allowedValues);
        }

        return allowedValues;
    }

    private static void addRoleAllowedValues(
            boolean advanced,
            @NotNull List<RoleAnalysisObjectCategorizationType> allowedValues) {

        allowedValues.add(RoleAnalysisObjectCategorizationType.UN_POPULAR);
        allowedValues.add(RoleAnalysisObjectCategorizationType.NOISE_EXCLUSIVE);

        if (advanced) {
            allowedValues.add(RoleAnalysisObjectCategorizationType.OVERALL_ANOMALY);
            allowedValues.add(RoleAnalysisObjectCategorizationType.ABOVE_POPULAR);
            allowedValues.add(RoleAnalysisObjectCategorizationType.NOISE_EXCLUSIVE_UNPOPULAR);
            allowedValues.add(RoleAnalysisObjectCategorizationType.NOISE);
            allowedValues.add(RoleAnalysisObjectCategorizationType.ANOMALY);
            allowedValues.add(RoleAnalysisObjectCategorizationType.EXCLUDED);
        }
    }

    public static String getCategoryValueDisplayString(@NotNull RoleAnalysisObjectCategorizationType value, boolean isRoleSelected) {
        if (isRoleSelected) {
            return getRoleDisplayValues(value);
        } else {
            return getUserDisplayValues(value);
        }

    }

    public static String getRoleDisplayValues(@NotNull RoleAnalysisObjectCategorizationType value) {
        if (value.equals(RoleAnalysisObjectCategorizationType.UN_POPULAR)) {
            return translate("RoleAnalysisObjectCategorizationType.un_popular.role");
        } else if (value.equals(RoleAnalysisObjectCategorizationType.NOISE_EXCLUSIVE)) {
            return translate("RoleAnalysisObjectCategorizationType.noise_exclusive.role");
        } else if (value.equals(RoleAnalysisObjectCategorizationType.OVERALL_ANOMALY)) {
            return translate("RoleAnalysisObjectCategorizationType.overall.anomaly");
        }else if (value.equals(RoleAnalysisObjectCategorizationType.NOISE_EXCLUSIVE_UNPOPULAR)) {
            return translate("RoleAnalysisObjectCategorizationType.noise_exclusive.and.un_popular.role");
        }else if (value.equals(RoleAnalysisObjectCategorizationType.ABOVE_POPULAR)) {
            return translate("RoleAnalysisObjectCategorizationType.above_popular");
        }else if (value.equals(RoleAnalysisObjectCategorizationType.NOISE)) {
            return translate("RoleAnalysisObjectCategorizationType.noise");
        }else if (value.equals(RoleAnalysisObjectCategorizationType.ANOMALY)) {
            return translate("RoleAnalysisObjectCategorizationType.anomaly");
        }else if (value.equals(RoleAnalysisObjectCategorizationType.EXCLUDED)) {
            return translate("RoleAnalysisObjectCategorizationType.excluded");
        }
        return value.toString();
    }

    public static String getUserDisplayValues(@NotNull RoleAnalysisObjectCategorizationType value) {
        if (value.equals(RoleAnalysisObjectCategorizationType.INSUFFICIENT)) {
            return translate("RoleAnalysisObjectCategorizationType.insufficient.peer.similarity");
        } else if (value.equals(RoleAnalysisObjectCategorizationType.NOISE_EXCLUSIVE_UNPOPULAR)) {
            return translate("RoleAnalysisObjectCategorizationType.noise_exclusive.and.un_popular.user");
        } else if (value.equals(RoleAnalysisObjectCategorizationType.UN_POPULAR)) {
            return translate("RoleAnalysisObjectCategorizationType.un_popular.user");
        } else if (value.equals(RoleAnalysisObjectCategorizationType.ABOVE_POPULAR)) {
            return translate("RoleAnalysisObjectCategorizationType.above_popular");
        } else if (value.equals(RoleAnalysisObjectCategorizationType.NOISE)) {
            return translate("RoleAnalysisObjectCategorizationType.noise");
        } else if (value.equals(RoleAnalysisObjectCategorizationType.NOISE_EXCLUSIVE)) {
            return translate("RoleAnalysisObjectCategorizationType.noise_exclusive.user");
        } else if (value.equals(RoleAnalysisObjectCategorizationType.OUTLIER)) {
            return translate("RoleAnalysisObjectCategorizationType.outlier");
        } else if (value.equals(RoleAnalysisObjectCategorizationType.EXCLUDED)) {
            return translate("RoleAnalysisObjectCategorizationType.excluded");
        }
        return value.toString();
    }

    private static void addUserAllowedValues(
            boolean advanced,
            @NotNull List<RoleAnalysisObjectCategorizationType> allowedValues) {

        allowedValues.add(RoleAnalysisObjectCategorizationType.NOISE_EXCLUSIVE_UNPOPULAR);
        allowedValues.add(RoleAnalysisObjectCategorizationType.INSUFFICIENT);

        if (advanced) {
            allowedValues.add(RoleAnalysisObjectCategorizationType.UN_POPULAR);
            allowedValues.add(RoleAnalysisObjectCategorizationType.ABOVE_POPULAR);
            allowedValues.add(RoleAnalysisObjectCategorizationType.NOISE);
            allowedValues.add(RoleAnalysisObjectCategorizationType.NOISE_EXCLUSIVE);
            allowedValues.add(RoleAnalysisObjectCategorizationType.OUTLIER);
            allowedValues.add(RoleAnalysisObjectCategorizationType.EXCLUDED);
        }
    }

    public static @NotNull SelectableBeanObjectDataProvider<FocusType> createTableProvider(
            Component component,
            LoadableModel<List<RoleAnalysisObjectCategorizationType>> selectionModel,
            boolean isAdvanced,
            List<RoleAnalysisIdentifiedCharacteristicsItemType> items,
            Map<String, List<RoleAnalysisObjectCategorizationType>> params,
            LoadableModel<Boolean> isRoleSelectedModel) {

        List<RoleAnalysisObjectCategorizationType> allowedValues = CategorySelectionProvider.allowedValues(
                isAdvanced, isRoleSelectedModel);

        return new SelectableBeanObjectDataProvider<>(
                component, Set.of()) {

            @SuppressWarnings({ "unchecked", "rawtypes" })
            @Override
            protected List<?> searchObjects(Class type, ObjectQuery query, Collection collection, Task task, OperationResult result) {

                Integer offset = query.getPaging().getOffset();
                Integer maxSize = query.getPaging().getMaxSize();
                Integer end = offset + maxSize;

                List<FocusType> objects = new ArrayList<>();
                int counter = 0;
                RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();
                for (RoleAnalysisIdentifiedCharacteristicsItemType item : items) {
                    List<RoleAnalysisObjectCategorizationType> category = item.getCategory();

                    if (selectionModel.getObject() != null
                            && !selectionModel.getObject().isEmpty()
                            && (category == null || !new HashSet<>(category).containsAll(selectionModel.getObject()))) {
                        continue;
                    }

                    boolean existSuitableCategory = category.stream().anyMatch(allowedValues::contains);

                    if (!existSuitableCategory) {
                        continue;
                    }

                    counter++;

                    params.put(item.getObjectRef().getOid(), item.getCategory());

                    if (counter >= offset) {
                        PrismObject<FocusType> focusTypeObject = roleAnalysisService.getFocusTypeObject(
                                item.getObjectRef().getOid(), task, result);
                        if (focusTypeObject != null) {
                            objects.add(focusTypeObject.asObjectable());
                        } else {
                            counter--;
                        }
                    }

                    if (counter >= end) {
                        break;
                    }
                }

                return objects;
            }

            @Override
            protected Integer countObjects(Class<FocusType> type, ObjectQuery query,
                    Collection<SelectorOptions<GetOperationOptions>> currentOptions, Task task, OperationResult result) {
                int count = 0;
                for (RoleAnalysisIdentifiedCharacteristicsItemType item : items) {
                    List<RoleAnalysisObjectCategorizationType> category = item.getCategory();
                    if (selectionModel.getObject() != null
                            && !selectionModel.getObject().isEmpty()
                            && (category == null || !new HashSet<>(category).containsAll(selectionModel.getObject()))) {
                        continue;
                    }

                    boolean existSuitableCategory = category.stream().anyMatch(allowedValues::contains);

                    if (!existSuitableCategory) {
                        continue;
                    }

                    count++;
                }

                return count;
            }
        };
    }
}

package com.evolveum.midpoint.web.component.search;

import java.io.Serializable;

public class SearchConfigDto implements Serializable {

    public static final String F_PROPERTY = "property";
    public static final String F_FILTER_TYPE = "filterType";
    public static final String F_MATCHING_RULE = "matchingRule";
    public static final String F_NEGATION = "negation";
    public static final String F_VALUE = "value";

    public enum FilterType {
        SUBSTRING,
        EQUALS
    }

    public enum MatchingRule {
        POLYSTRING_NORM,
        EMAIL_EXCHANGE
    }

    public enum Operator {
        AND,
        OR
    }

    private Property property;
    private FilterType filterType;
    private MatchingRule matchingRule;
    private Object value;
    private boolean negation;

    public SearchConfigDto(){
    }

    public Property getProperty() {
        return property;
    }

    public void setProperty(Property property) {
        this.property = property;
    }

    public FilterType getFilterType() {
        return filterType;
    }

    public void setFilterType(FilterType filterType) {
        this.filterType = filterType;
    }

    public MatchingRule getMatchingRule() {
        return matchingRule;
    }

    public void setMatchingRule(MatchingRule matchingRule) {
        this.matchingRule = matchingRule;
    }

    public boolean isNegation() {
        return negation;
    }

    public void setNegation(boolean negation) {
        this.negation = negation;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public static SearchConfigDto createSearchConfigDto(SearchItem searchItem){
        SearchConfigDto searchConfigDto = new SearchConfigDto();
        searchConfigDto.setProperty(new Property(searchItem.getDefinition()));
        return searchConfigDto;
    }
}

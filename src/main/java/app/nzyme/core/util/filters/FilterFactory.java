package app.nzyme.core.util.filters;

import app.nzyme.core.rest.parameters.FiltersParameter;

public class FilterFactory {

    /*
     * This logic mostly just maps the String filter operator names to their corresponding Enum value and
     * casts the filter value to what the operator works with. For example, numeric value filters cast the
     * value to Long, while others keep it as String.
     *
     * This fails hard in case of unexpected filter configurations because the UI takes care of a lot of
     * filter validation. No invalid filters should ever arrive here.
     */

    public static Filter fromRestQuery(FiltersParameter parameter) {
        switch (parameter.operator()) {
            case "equals":
                return Filter.create(
                        parameter.field(), FilterOperator.EQUALS, parameter.value()
                );
            case "not_equals":
                return Filter.create(
                        parameter.field(), FilterOperator.NOT_EQUALS, parameter.value()
                );
            case "equals_numeric":
                return Filter.create(
                        parameter.field(), FilterOperator.EQUALS_NUMERIC, Long.valueOf(parameter.value())
                );
            case "not_equals_numeric":
                return Filter.create(
                        parameter.field(), FilterOperator.NOT_EQUALS_NUMERIC, Long.valueOf(parameter.value())
                );
            case "regex_match":
                return Filter.create(
                        parameter.field(), FilterOperator.REGEX_MATCH, parameter.value()
                );
            case "not_regex_match":
                return Filter.create(
                        parameter.field(), FilterOperator.NOT_REGEX_MATCH, parameter.value()
                );
            case "greater_than":
                return Filter.create(
                        parameter.field(), FilterOperator.GREATER_THAN, Long.valueOf(parameter.value())
                );
            case "smaller_than":
                return Filter.create(
                        parameter.field(), FilterOperator.SMALLER_THAN, Long.valueOf(parameter.value())
                );
            case "in_cidr":
                return Filter.create(
                        parameter.field(), FilterOperator.IN_CIDR, parameter.value()
                );
            case "not_in_cidr":
                return Filter.create(
                        parameter.field(), FilterOperator.NOT_IN_CIDR, parameter.value()
                );
            case "is_private":
                return Filter.create(
                        parameter.field(), FilterOperator.IS_PRIVATE, parameter.value()
                );
            case "is_not_private":
                return Filter.create(
                        parameter.field(), FilterOperator.IS_NOT_PRIVATE, parameter.value()
                );
            default:
                throw new RuntimeException("Unknown filter operator: [" + parameter.operator() + "]");
        }
    }

}

package com.rentals.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.time.YearMonth;

/**
 * Convierte YearMonth ↔ CHAR(7) "YYYY-MM" para la columna year_month
 * de la tabla paid_month.
 *
 * autoApply = true: se aplica automáticamente a todos los atributos YearMonth.
 */
@Converter(autoApply = true)
public class YearMonthConverter implements AttributeConverter<YearMonth, String> {

    @Override
    public String convertToDatabaseColumn(YearMonth attribute) {
        return attribute == null ? null : attribute.toString();
    }

    @Override
    public YearMonth convertToEntityAttribute(String dbData) {
        return dbData == null ? null : YearMonth.parse(dbData);
    }
}

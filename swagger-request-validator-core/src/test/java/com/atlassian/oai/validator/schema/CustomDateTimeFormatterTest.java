package com.atlassian.oai.validator.schema;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class CustomDateTimeFormatterTest {

    private final DateTimeFormatter dateTimeFormatter = CustomDateTimeFormatter.getRFC3339Formatter();

    @Test
    public void parse_withNanoSecondFraction_shouldPass() {
        try {
            dateTimeFormatter.parse("2017-05-31T20:00:01.123456789+10:00");
        }  catch (final DateTimeParseException ex) {
            fail("Nano second fraction should pass");
        }
    }
    
    @Test
    public void parse_withoutSecondFraction_shouldPass() {
        try {
            dateTimeFormatter.parse("2017-05-31T20:00:01+10:00");
        } catch (final DateTimeParseException ex) {
            fail("No second fraction should pass");
        }        
    }
    
    @Test
    public void parse_withZeroOffset_shouldPass() {
        try {
            dateTimeFormatter.parse("2017-05-31T20:00:01.123Z");
        } catch (final DateTimeParseException ex) {
            fail("Zero date-time offset should pass");
        }        
    }
    
    @Test
    public void parse_withoutSecondFractionZeroOffset_shouldPass() {
        try {
            dateTimeFormatter.parse("2017-05-31T23:45:01Z");
        } catch (final DateTimeParseException ex) {
            fail("No second fraction should pass");
        }        
    }
    
    @Test
    public void parse_withNegativeOffset_shouldPass() {
        try {
            dateTimeFormatter.parse("2017-05-31T20:00:01.123456789-05:30");
        } catch (final DateTimeParseException ex) {
            fail("Negative date time offset should pass");
        }
    }
    
    @Test
    public void parse_shouldReturnSameInstantAsInput() {
        final String datetime="2017-05-31T20:00:01.123456789Z";
        final Instant instant = Instant.parse(datetime);
        try {
            final TemporalAccessor temporalAccessor = dateTimeFormatter.parse(datetime);
            assertTrue(Instant.from(temporalAccessor).compareTo(instant) == 0);
        } catch (final DateTimeParseException ex) {
            fail("Correct format should be parsedd successfully");
        }
        
    }
    
    @Test
    public void parse_withoutSecond_shouldFail() {
        assertThrows(DateTimeParseException.class, () -> dateTimeFormatter.parse("2017-05-31T23:45Z"));
    }

    @Test
    public void parse_withoutOffset_shouldFail() {
        assertThrows(DateTimeParseException.class, () -> dateTimeFormatter.parse("2017-05-31T23:45:20.12345"));
    }

    @Test
    public void parse_wrongDateTime_shouldFail() {
        assertThrows(DateTimeParseException.class, () -> dateTimeFormatter.parse("2017:05:31T23:45:20.12345Z"));
    }

    @Test
    public void parse_withTimeZone_shouldFail() {
        assertThrows(DateTimeParseException.class, () -> dateTimeFormatter.parse("2017-05-31T23:45:20.123Z[Europe/Paris]"));
    }
}

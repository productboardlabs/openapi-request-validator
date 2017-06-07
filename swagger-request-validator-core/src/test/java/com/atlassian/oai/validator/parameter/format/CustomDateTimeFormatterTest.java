package com.atlassian.oai.validator.parameter.format;

import static org.junit.Assert.fail;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.junit.Test;

public class CustomDateTimeFormatterTest {

    private DateTimeFormatter dateTimeFormatter = CustomDateTimeFormatter.getInstance(); 

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
    public void parse_withoutZeroOffset_shouldPass() {
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
            fail("Nano second fraction should pass");
        }
    }
    
    @Test(expected=DateTimeParseException.class)
    public void parse_withoutSecond_shouldFail() {
        dateTimeFormatter.parse("2017-05-31T23:45Z");
    }
    
    @Test(expected=DateTimeParseException.class)
    public void parse_withoutOffset_shouldFail() {
        dateTimeFormatter.parse("2017-05-31T23:45:20.12345");
    }
    
    @Test(expected=DateTimeParseException.class)
    public void parse_wrongDateTime_shouldFail() {
        dateTimeFormatter.parse("2017:05:31T23:45:20.12345Z");
    }
}

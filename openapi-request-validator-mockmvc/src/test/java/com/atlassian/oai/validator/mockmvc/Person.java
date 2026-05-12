package com.atlassian.oai.validator.mockmvc;

public class Person {
    private Integer id;
    private String name;

    public Person() {
    }

    public Person(final Integer id, final String name) {
        this.id = id;
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public void setId(final Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }
}
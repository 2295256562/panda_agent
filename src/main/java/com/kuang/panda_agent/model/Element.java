package com.kuang.panda_agent.model;

import lombok.Data;

import java.util.List;

@Data
public class Element {
    private String name;
    // ["@AndroidFindBy", "uiAutomator"]
    private List<String> findBy;
    private String value;
    private String description;
}

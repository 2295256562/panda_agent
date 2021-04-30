package com.kuang.panda_agent.model;

import lombok.Data;
import org.openqa.selenium.By;

import java.util.List;


@Data
public class Page {

    public static final int TYPE_ANDROID_NATIVE = 1;
    public static final int TYPE_IOS_NATIVE = 2;
    public static final int TYPE_WEB = 3;

    private Integer id;
    private String name;
    private Integer type;
    private List<Element> elements;
    private List<By> bys;
}

package com.dacubeking.AutoBuilder.robot.serialization.reflection;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;

public class ReflectionClassDataList {
    @JsonProperty
    public ArrayList<ReflectionClassData> reflectionClassData;

    @JsonCreator
    public ReflectionClassDataList(ArrayList<ReflectionClassData> reflectionClassData) {
        this.reflectionClassData = reflectionClassData;
    }
}

package com.dacubeking.AutoBuilder.robot.reflection;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;

public class ReflectionClassDataList {
    @JsonProperty
    public ArrayList<ReflectionClassData> reflectionClassData = new ArrayList<>();

    @JsonProperty
    public ArrayList<String> instanceLocations = new ArrayList<>();

    @JsonCreator
    public ReflectionClassDataList() {
    }
}

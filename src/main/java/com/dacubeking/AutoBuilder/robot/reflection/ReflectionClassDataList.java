package com.dacubeking.AutoBuilder.robot.reflection;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;

class ReflectionClassDataList {
    @JsonProperty
    ArrayList<ReflectionClassData> reflectionClassData = new ArrayList<>();

    @JsonProperty
    ArrayList<String> instanceLocations = new ArrayList<>();


    protected ReflectionClassDataList() {
    }

    @JsonCreator
    ReflectionClassDataList(ArrayList<ReflectionClassData> reflectionClassData,
                            ArrayList<String> instanceLocations) {
        this.reflectionClassData = reflectionClassData;
        this.instanceLocations = instanceLocations;
    }
}

package com.dacubeking.AutoBuilder.robot.reflection;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Method;
import java.util.Arrays;

final class ReflectionClassData {
    @JsonProperty
    @NotNull
    private final String fullName;
    @JsonProperty private final String @NotNull [] fieldNames;
    @JsonProperty private final String @NotNull [] fieldTypes;
    @JsonProperty private final ReflectionMethodData @NotNull [] methods;
    @JsonProperty private final int modifiers;
    @JsonProperty private final boolean isEnum;


    ReflectionClassData(@NotNull Class<?> clazz) {
        this.fullName = clazz.getName();
        Method[] methods = clazz.getMethods();
        this.methods = new ReflectionMethodData[methods.length];
        for (int i = 0; i < methods.length; i++) {
            this.methods[i] = new ReflectionMethodData(methods[i]);
        }

        this.fieldNames = new String[clazz.getDeclaredFields().length];
        this.fieldTypes = new String[clazz.getDeclaredFields().length];
        for (int i = 0; i < clazz.getDeclaredFields().length; i++) {
            this.fieldNames[i] = clazz.getDeclaredFields()[i].getName();
            this.fieldTypes[i] = clazz.getDeclaredFields()[i].getType().getName();
        }

        this.isEnum = clazz.isEnum();

        modifiers = clazz.getModifiers();
    }

    @Override
    public @NotNull
    String toString() {
        return "ReflectionClassData{" +
                "fullName='" + fullName + '\'' +
                ", fieldNames=" + Arrays.toString(fieldNames) +
                ", fieldTypes=" + Arrays.toString(fieldTypes) +
                ", methods=" + Arrays.toString(methods) +
                '}';
    }
}

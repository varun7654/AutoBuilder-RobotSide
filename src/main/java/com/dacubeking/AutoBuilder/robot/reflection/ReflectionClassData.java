package com.dacubeking.AutoBuilder.robot.reflection;

import com.fasterxml.jackson.annotation.JsonProperty;
import edu.wpi.first.wpilibj2.command.Command;
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

    @JsonProperty
    private final @NotNull String superClass;

    @JsonProperty private final String @NotNull [] interfaces;
    @JsonProperty private final int modifiers;
    @JsonProperty private final boolean isEnum;
    @JsonProperty private final boolean isCommand;

    ReflectionClassData(@NotNull Class<?> clazz) {
        this.fullName = clazz.getName();
        Method[] methods = clazz.getDeclaredMethods();
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

        this.superClass = clazz.getSuperclass().getName();
        interfaces = Arrays.stream(clazz.getInterfaces()).map(Class::getName).toArray(String[]::new);

        modifiers = clazz.getModifiers();
        this.isEnum = clazz.isEnum();

        isCommand = isCommand(clazz);
    }

    private boolean isCommand(Class<?> clazz) {
        if (clazz == null) {
            return false;
        }

        if (clazz.getName().equals(Command.class.getName())) {
            return true;
        }

        for (Class<?> anInterface : clazz.getInterfaces()) {
            if (isCommand(anInterface)) {
                return true;
            }
        }

        return isCommand(clazz.getSuperclass());
    }

    @Override
    public String toString() {
        return "ReflectionClassData{" +
                "fullName='" + fullName + '\'' +
                ", fieldNames=" + Arrays.toString(fieldNames) +
                ", fieldTypes=" + Arrays.toString(fieldTypes) +
                ", methods=" + Arrays.toString(methods) +
                ", superClass='" + superClass + '\'' +
                ", interfaces=" + Arrays.toString(interfaces) +
                ", modifiers=" + modifiers +
                ", isEnum=" + isEnum +
                ", isCommand=" + isCommand +
                '}';
    }
}

package com.dacubeking.AutoBuilder.robot.serialization.reflection;

import org.jetbrains.annotations.NotNull;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public final class ReflectionUtils {

    /**
     * Recursive method used to find all classes in a given directory and subdirs.
     *
     * @param packageName The package name for classes found inside the base directory
     * @return The classes
     */
    public static @NotNull
    List<Class<?>> findClasses(@NotNull String packageName)
            throws ClassNotFoundException {
//        Reflections reflections = new Reflections(new ConfigurationBuilder()
//                .setScanners(Scanners.MethodsAnnotated, Scanners.FieldsAnnotated, Scanners.ConstructorsAnnotated));


        Reflections reflections = new Reflections(packageName, Scanners.SubTypes.filterResultsBy(s -> true));
        return new ArrayList<>(reflections.getSubTypesOf(Object.class));
    }


    public static String @NotNull [] getParameterTypes(@NotNull Method method) {
        String[] parameterTypes = new String[method.getParameterCount()];
        for (int i = 0; i < method.getParameterCount(); i++) {
            parameterTypes[i] = method.getParameterTypes()[i].getName();
        }
        return parameterTypes;
    }
}

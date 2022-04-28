package com.dacubeking.AutoBuilder.robot.reflection;

import com.dacubeking.AutoBuilder.robot.serialization.Serializer;
import org.jetbrains.annotations.Nullable;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public final class ClassInformationSender {

    public static void updateReflectionInformation(@Nullable File file) {
        try {

            Reflections reflections = new Reflections("", Scanners.SubTypes.filterResultsBy(s -> true));
            ArrayList<Class<?>> classes = new ArrayList<>(reflections.getSubTypesOf(Object.class));
            ReflectionClassDataList reflectionClassDataList = new ReflectionClassDataList();
            for (Class<?> aClass : classes) {
                reflectionClassDataList.reflectionClassData.add(new ReflectionClassData(aClass));
            }
            //System.out.println(Serializer.serializeToString(reflectionClassData));
            System.out.println("Successfully updated reflection information");


            if (file != null) {
                file.getParentFile().mkdir();
                Serializer.serializeToFile(reflectionClassDataList, file);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

package com.dacubeking.AutoBuilder.robot.reflection;

import com.dacubeking.AutoBuilder.robot.robotinterface.AutonomousContainer;
import com.dacubeking.AutoBuilder.robot.serialization.Serializer;
import com.dacubeking.AutoBuilder.robot.utility.OsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public final class ClassInformationSender {

    public static void updateReflectionInformation(@NotNull String packageName) {
        updateReflectionInformation(new File(OsUtil.getUserConfigDirectory("AutoBuilder") + "/robotCodeData.json"), packageName);
    }

    public static void updateReflectionInformation(@Nullable File file, @NotNull String packageName) {
        try {
            AutonomousContainer.getInstance().isInitialized();
            Reflections reflections = new Reflections(packageName, Scanners.SubTypes.filterResultsBy(s -> true));
            ArrayList<Class<?>> classes = new ArrayList<>(reflections.getSubTypesOf(Object.class));

            ReflectionClassDataList reflectionClassDataList = new ReflectionClassDataList();
            for (Class<?> aClass : classes) {
                reflectionClassDataList.reflectionClassData.add(new ReflectionClassData(aClass));
            }

            reflectionClassDataList.instanceLocations.addAll(AutonomousContainer.getInstance().getAccessibleInstances().keySet());

            System.out.println(reflectionClassDataList.instanceLocations);


            if (file != null) {
                file.getParentFile().mkdir();
                Serializer.serializeToFile(reflectionClassDataList, file);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class ClassInformationSender {

    public static void updateReflectionInformation(@NotNull String packageName) {
        updateReflectionInformation(new File(OsUtil.getUserConfigDirectory("AutoBuilder") + "/robotCodeData.json"), packageName);
    }

    public static void updateReflectionInformation(@Nullable File file, @NotNull String packageName) {
        try {
            AutonomousContainer.getInstance().isInitialized();
            Reflections reflections = new Reflections(packageName, Scanners.SubTypes.filterResultsBy(s -> true));
            Set<Class<?>> classes = new HashSet<>();


            HashMap<String, Map<String, Set<String>>> store = reflections.getStore();
            for (Map<String, Set<String>> value : store.values()) {
                for (Set<String> discoveredClasses : value.values()) {
                    for (String discoveredClass : discoveredClasses) {
                        if (discoveredClass.contains(packageName)) {
                            try {
                                classes.add(Class.forName(discoveredClass));
                            } catch (ClassNotFoundException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }

            System.out.println("Store Map: " + reflections.getStore());
            System.out.println("Classes: " + classes);

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

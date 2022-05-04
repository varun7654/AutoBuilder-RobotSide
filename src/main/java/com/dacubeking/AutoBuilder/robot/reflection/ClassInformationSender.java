package com.dacubeking.AutoBuilder.robot.reflection;

import com.dacubeking.AutoBuilder.robot.robotinterface.AutonomousContainer;
import com.dacubeking.AutoBuilder.robot.serialization.Serializer;
import com.dacubeking.AutoBuilder.robot.utility.OsUtil;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class ClassInformationSender {

    /**
     * Gets information about your robot code for the AutoBuilder Gui to use. This includes the names of all the classes, their
     * methods, their parameters and more. Also saves this data in a json file for the AutoBuilder Gui to use.
     *
     * @param packageName The package name of your robot code. (it will recursively search the packages for all classes)
     */
    public static void updateReflectionInformation(@NotNull String packageName) {
        updateReflectionInformation(new File(OsUtil.getUserConfigDirectory("AutoBuilder") + "/robotCodeData.json"), packageName);
    }

    public static void updateReflectionInformation(@Nullable File file, @NotNull String packageName) {
        try {
            AutonomousContainer.getInstance().isInitialized();

            Set<ClassInfo> classInfos = ClassPath.from(Thread.currentThread().getContextClassLoader()).getTopLevelClasses();

            Set<Class<?>> classes = new HashSet<>();
            for (ClassInfo classInfo : classInfos) {
                if (classInfo.getPackageName().startsWith(packageName)) {
                    Class<?> clazz = classInfo.load();
                    classes.addAll(Arrays.asList(clazz.getDeclaredClasses()));
                    classes.add(clazz);
                }
            }

            ReflectionClassDataList reflectionClassDataList = new ReflectionClassDataList();
            for (Class<?> aClass : classes) {
                reflectionClassDataList.reflectionClassData.add(new ReflectionClassData(aClass));
            }

            reflectionClassDataList.instanceLocations.addAll(AutonomousContainer.getInstance().getAccessibleInstances().keySet());

            System.out.println("Found " + reflectionClassDataList.reflectionClassData.size() + " classes with "
                    + reflectionClassDataList.instanceLocations.size() + " defined instances found.");

            if (file != null) {
                file.getParentFile().mkdir();
                Serializer.serializeToFile(reflectionClassDataList, file);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

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

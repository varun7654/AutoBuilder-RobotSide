package com.dacubeking.AutoBuilder.robot.serialization.reflection;

import com.dacubeking.AutoBuilder.robot.serialization.Serializer;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class ClassInformationSender {
    public ClassInformationSender() {

    }

    public static void updateReflectionInformation(@Nullable File file) {
        try {
            List<Class<?>> classes = ReflectionUtils.findClasses("");
            ArrayList<ReflectionClassData> reflectionClassData = new ArrayList<>();
            for (Class<?> aClass : classes) {
                reflectionClassData.add(new ReflectionClassData(aClass));
            }
            //System.out.println(Serializer.serializeToString(reflectionClassData));
            System.out.println("Successfully updated reflection information");

            if (file != null) {
                file.getParentFile().mkdir();
                Serializer.serializeToFile(reflectionClassData, file);
            }
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }
    }
}

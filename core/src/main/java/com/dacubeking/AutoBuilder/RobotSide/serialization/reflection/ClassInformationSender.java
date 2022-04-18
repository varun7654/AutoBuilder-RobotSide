package com.dacubeking.AutoBuilder.RobotSide.serialization.reflection;

import com.dacubeking.AutoBuilder.RobotSide.serialization.Serializer;
import edu.wpi.first.wpilibj.Filesystem;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.dacubeking.AutoBuilder.RobotSide.serialization.reflection.ReflectionUtils.findClasses;
public final class ClassInformationSender {
    public ClassInformationSender() {

    }

    public static void updateReflectionInformation(@Nullable File file) {
        try {
            List<Class<?>> classes = findClasses(new File(Filesystem.getLaunchDirectory() + "/build/classes/java/main"), "");
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

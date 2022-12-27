package com.dacubeking.AutoBuilder.robot.serialization.command;

import com.dacubeking.AutoBuilder.robot.robotinterface.AutonomousContainer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.util.concurrent.UncheckedExecutionException;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj2.command.Command;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import static com.dacubeking.AutoBuilder.robot.robotinterface.AutonomousContainer.getCommandTranslator;

@JsonIgnoreProperties(ignoreUnknown = true)
class SendableCommand {

    public static final double LOOPING_PERIOD_SECONDS = 0.02; // 50 Hz/20 ms
    @JsonProperty("methodName")
    @NotNull
    protected final String methodName;

    @JsonProperty("args") public final String @NotNull [] args;

    @JsonProperty("argTypes") public final String[] argTypes;

    @JsonProperty("reflection") public final boolean reflection;

    @JsonProperty("command") private final boolean command;

    private static final @NotNull Map<String, Function<String, Object>> INFERABLE_TYPES_PARSER;

    static {
        INFERABLE_TYPES_PARSER = new HashMap<>();
        INFERABLE_TYPES_PARSER.put(int.class.getName(), Integer::parseInt);
        INFERABLE_TYPES_PARSER.put(double.class.getName(), Double::parseDouble);
        INFERABLE_TYPES_PARSER.put(float.class.getName(), Float::parseFloat);
        INFERABLE_TYPES_PARSER.put(long.class.getName(), Long::parseLong);
        INFERABLE_TYPES_PARSER.put(short.class.getName(), Short::parseShort);
        INFERABLE_TYPES_PARSER.put(byte.class.getName(), Byte::parseByte);
        INFERABLE_TYPES_PARSER.put(char.class.getName(), s -> s.charAt(0));
        INFERABLE_TYPES_PARSER.put(boolean.class.getName(), Boolean::parseBoolean);
        INFERABLE_TYPES_PARSER.put(String.class.getName(), s -> s);
        INFERABLE_TYPES_PARSER.put(Integer.class.getName(), Integer::valueOf);
        INFERABLE_TYPES_PARSER.put(Double.class.getName(), Double::valueOf);
        INFERABLE_TYPES_PARSER.put(Float.class.getName(), Float::valueOf);
        INFERABLE_TYPES_PARSER.put(Long.class.getName(), Long::valueOf);
        INFERABLE_TYPES_PARSER.put(Short.class.getName(), Short::valueOf);
        INFERABLE_TYPES_PARSER.put(Byte.class.getName(), Byte::valueOf);
        INFERABLE_TYPES_PARSER.put(Character.class.getName(), s -> Character.valueOf(s.charAt(0)));
        INFERABLE_TYPES_PARSER.put(Boolean.class.getName(), Boolean::valueOf);
    }

    private static final List<String> primitiveTypes = Arrays.asList(
            int.class.getName(),
            double.class.getName(),
            float.class.getName(),
            long.class.getName(),
            short.class.getName(),
            byte.class.getName(),
            char.class.getName(),
            boolean.class.getName()
    );

    @JsonCreator
    protected SendableCommand(@JsonProperty("methodName") @NotNull String methodName,
                              @JsonProperty("args") String @NotNull [] args,
                              @JsonProperty("argTypes") String[] argTypes,
                              @JsonProperty("reflection") boolean reflection,
                              @JsonProperty("command") boolean command) {
        Method methodToCall = null;
        Object instance = null;

        this.methodName = methodName;
        this.args = args;
        this.argTypes = argTypes;
        this.reflection = reflection;
        this.command = command;

        objArgs = new Object[args.length];

        if (command) {
            // If we're a command, the command name is the method name
            if (AutonomousContainer.getInstance().getAccessibleInstances().containsKey(methodName)) {
                instance = AutonomousContainer.getInstance().getAccessibleInstances().get(methodName);
            } else {
                throwIllegalArgumentException("Command " + methodName + " not found. Make sure it's annotated with @AutoBuilderAccessible", null);
            }
            try {
                Command castedCommand = (Command) instance; // Check that it's actually a command to prevent errors from occurring when we try to run it
            } catch (ClassCastException e) {
                throwIllegalArgumentException("Command " + methodName + " is not a Command. Make sure it's annotated with @AutoBuilderAccessible" +
                        "Try rebuilding your robotCodeData.json by rerunning your robot code in simulation.", e);
            }
        } else {
            for (int i = 0; i < args.length; i++) {
                try {
                    // Parse the arguments into the correct types
                    if (INFERABLE_TYPES_PARSER.containsKey(argTypes[i])) {
                        // Convert the string to the correct type
                        objArgs[i] = INFERABLE_TYPES_PARSER.get(argTypes[i]).apply(args[i]);
                    } else {
                        // Convert the string to the correct enum if it's not a primitive type
                        objArgs[i] = Enum.valueOf(Class.forName(argTypes[i]).asSubclass(Enum.class), args[i]);
                    }
                } catch (ClassNotFoundException e) {
                    throwIllegalArgumentException("We couldn't find the class " + argTypes[i] +
                            ". Try rebuilding your robotCodeData.json by rerunning your robot code in simulation.", e);
                } catch (ClassCastException e) {
                    throwIllegalArgumentException("We couldn't cast the argument " + args[i] + " to the type " + argTypes[i] +
                            ". Try rebuilding your robotCodeData.json by rerunning your robot code in simulation.", e);
                } catch (NumberFormatException e) {
                    throwIllegalArgumentException("We couldn't parse number with the type " + args[i] + " to the type " + argTypes[i] +
                            ". Try rebuilding your robotCodeData.json by rerunning your robot code in simulation.", e);
                }
            }

            if (reflection) {
                String[] splitMethod = methodName.split("\\.");

                String[] classNameArray = new String[splitMethod.length - 1];
                System.arraycopy(splitMethod, 0, classNameArray, 0, classNameArray.length);
                String className = String.join(".", classNameArray); // Get the class name that the method is in
                try {
                    Class<?> cls = Class.forName(className); // Get the class that the method is in

                    // Get an array of the class types to find the correct method
                    Class<?>[] typeArray = new Class[argTypes.length];
                    for (int i = 0; i < objArgs.length; i++) {
                        if (primitiveTypes.contains(argTypes[i])) {
                            typeArray[i] = getPrimitiveClass(objArgs[i].getClass());
                        } else {
                            typeArray[i] = objArgs[i].getClass();
                        }
                    }

                    if (typeArray.length == 0) {
                        // If there are no arguments, just get the method with no arguments
                        methodToCall = cls.getDeclaredMethod(splitMethod[splitMethod.length - 1]);
                    } else {
                        // Get the method with the correct arguments
                        methodToCall = cls.getDeclaredMethod(splitMethod[splitMethod.length - 1], typeArray);
                    }
                    methodToCall.setAccessible(true); // Make the method accessible so that we can call it if it's private

                    if (AutonomousContainer.getInstance().getAccessibleInstances().containsKey(className)) {
                        // The user has specified an instance to use for this class, so use it
                        instance = AutonomousContainer.getInstance().getAccessibleInstances().get(className);
                    } else {
                        if (!Modifier.isStatic(methodToCall.getModifiers())) {
                            // If the method isn't static, we need to get an instance of the class
                            Method getInstance = cls.getDeclaredMethod("getInstance"); // Get the getInstance method
                            getInstance.setAccessible(true);
                            instance = getInstance.invoke(null); // Invoke the getInstance method to get an instance of the class
                        }
                    }
                } catch (ClassNotFoundException e) {
                    throwIllegalArgumentException("Class not found: " + className + ". " + e.getMessage()
                            + ". Try rebuilding your robotCodeData.json by rerunning your robot code in simulation.", e);
                } catch (NoSuchMethodException e) {
                    throwIllegalArgumentException("Could not find method : " + splitMethod[splitMethod.length - 1] + " in class "
                            + className + ". " + e.getMessage()
                            + ". Try rebuilding your robotCodeData.json by rerunning your robot code in simulation.", e);
                } catch (InvocationTargetException | IllegalAccessException e) {
                    throwIllegalArgumentException("Could not get singleton reference in class " + className + " for method: " +
                            splitMethod[splitMethod.length - 1] + ". " + e.getMessage(), e);
                }
            }
        }

        this.methodToCall = methodToCall;
        this.instance = instance;
    }

    private static void throwIllegalArgumentException(@NotNull String errorMessage, @Nullable Exception e) {
        DriverStation.reportError(errorMessage, false);
        throw new IllegalArgumentException(errorMessage, e);
    }

    @JsonIgnoreProperties
    @Nullable
    private final Object instance;

    @JsonIgnoreProperties
    @Nullable
    private final Method methodToCall;

    @JsonIgnoreProperties private final Object @NotNull [] objArgs;

    private static Class<?> getPrimitiveClass(Class<?> clazz) {
        if (clazz.equals(Integer.class)) {
            return double.class;
        } else if (clazz.equals(Double.class)) {
            return double.class;
        } else if (clazz.equals(Boolean.class)) {
            return boolean.class;
        } else if (clazz.equals(char.class)) {
            return char.class;
        } else if (clazz.equals(Byte.class)) {
            return byte.class;
        } else if (clazz.equals(Short.class)) {
            return short.class;
        } else if (clazz.equals(Long.class)) {
            return long.class;
        } else if (clazz.equals(Float.class)) {
            return float.class;
        } else if (clazz.equals(String.class)) {
            return String.class;
        } else {
            return clazz;
        }
    }


    /**
     * @throws InterruptedException            If the command fails do to an interrupt
     * @throws CommandExecutionFailedException If the command fails to execute for any other reason
     * @throws ExecutionException              Should never happen (for some reason the future was cancelled or threw and exception)
     */
    protected void execute() throws InterruptedException, CommandExecutionFailedException, ExecutionException {
        if (!command && methodToCall == null && reflection) {
            throw new CommandExecutionFailedException("Method to call is null");
        }

        if (command && instance == null) {
            throw new CommandExecutionFailedException("Instance is null when calling a command");
        }

        boolean firstRun = true;

        if (getCommandTranslator().runOnMainThread && reflection) {
            try {
                while (true) {
                    double startTime = Timer.getFPGATimestamp(); // Time the command to keep the period constant

                    CompletableFuture<Object> future = new CompletableFuture<>();
                    final boolean finalFirstRun = firstRun;

                    getCommandTranslator().runOnMainThread(() -> {
                        try {
                            future.complete(invokeMethod(finalFirstRun));
                        } catch (CommandExecutionFailedException | InterruptedException e) {
                            future.complete(new UncheckedExecutionException(e));
                        }
                    }); // Schedule the command to run on the main thread

                    Object result = future.get(); // Wait for the command to finish

                    if (result instanceof UncheckedExecutionException) { // If the command failed rethrow the exception
                        Throwable exception = ((UncheckedExecutionException) result).getCause();
                        if (exception instanceof InterruptedException) {
                            throw (InterruptedException) exception;
                        } else if (exception instanceof CommandExecutionFailedException) {
                            throw (CommandExecutionFailedException) exception;
                        } else {
                            throw new CommandExecutionFailedException(
                                    "Unexpected Exception. Please report this: " + exception.getMessage(), exception);
                        }
                    }

                    if (!(result instanceof Boolean) || result.equals(true)) break; // If the command returns true or is not a boolean, stop the command

                    firstRun = false;
                    //Keep executing the method if it returns false
                    //noinspection BusyWait
                    Thread.sleep((long) Math.max((LOOPING_PERIOD_SECONDS - (Timer.getFPGATimestamp() - startTime)) * 1000, 0));
                }
            } catch (InterruptedException e) {
                if (command) {
                    // If a command is interrupted, we want to end the command with the interrupted flag as true
                    CompletableFuture<Object> future = new CompletableFuture<>();
                    getCommandTranslator().runOnMainThread(() -> {
                        try {
                            Command command = (Command) instance;
                            command.end(true);
                            future.complete(null);
                        } catch (Exception ex) {
                            future.complete(ex); // If the command fails to end, we want to throw the exception
                        }
                    });
                    @Nullable Object result = future.get(); // Wait for the command to finish
                    if (result != null) {
                        // If the command failed rethrow the exception
                        Exception ex = (Exception) result;
                        throw new CommandExecutionFailedException("Failed to interrupt command " + methodName + ": " + ex.getMessage(), ex);
                    }
                }
                throw e; // rethrow the interrupt
            }
        } else {
            try {
                while (true) {
                    double startTime = Timer.getFPGATimestamp();
                    Object result = invokeMethod(firstRun);
                    if (!(result instanceof Boolean) || result.equals(true)) break;
                    firstRun = false;

                    //Keep executing the method if it returns false
                    //noinspection BusyWait
                    Thread.sleep((long) Math.max((LOOPING_PERIOD_SECONDS - (Timer.getFPGATimestamp() - startTime)) * 1000, 0));
                }
            } catch (InterruptedException e) {
                if (command) {
                    // If a command is interrupted, we want to end the command with the interrupted flag as true
                    try {
                        ((Command) instance).end(true);
                    } catch (Exception ex) {
                        throw new CommandExecutionFailedException("Failed to interrupt command " + methodName + ": " + ex.getMessage(), ex);
                    }
                }

                throw e; // rethrow the interrupt
            }
        }
    }

    private @Nullable Object invokeMethod(boolean firstRun) throws InterruptedException, CommandExecutionFailedException {
        try {
            if (reflection) {
                if (command) {
                    assert instance != null;
                    Command command = (Command) instance;
                    if (firstRun) {
                        command.initialize();
                    }
                    command.execute();
                    if (command.isFinished()) {
                        command.end(false);
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    assert methodToCall != null;
                    return methodToCall.invoke(instance, objArgs); // If the method returns true & is a boolean, stop executing it
                }
            } else {
                switch (methodName) {
                    case "print":
                        System.out.println(objArgs[0]);
                        break;
                    case "sleep":
                        Thread.sleep((long) objArgs[0]);
                        break;
                }
                return null;
            }
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            throw new CommandExecutionFailedException("Could not invoke method " + methodName + " due to: " + e.getMessage(), e);
        }
    }
}

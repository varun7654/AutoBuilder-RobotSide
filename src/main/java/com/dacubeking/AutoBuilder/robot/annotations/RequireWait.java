package com.dacubeking.AutoBuilder.robot.annotations;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * This annotation should be used on methods/commands that the AutoBuilder should wait for to finish before continuing.
 * <p>
 * When using this annotation, the AutoBuilder will wait for the command/method to finish before continuing.
 * <p>
 * - For Commands: This means the command will be submitted to the scheduler, and the AutoBuilder will wait until the command finishes before continuing. The AutoBuilder will wait until
 * {@code Command#isScheduled} returns false for the command.
 * <p>
 * - For other methods: This means the AutoBuilder will wait until the method returns true before continuing.
 * <p>
 * If a method is <STRONG>not</STRONG> annotated with this annotation, the AutoBuilder will not check if it's finished before continuing.
 * <p>
 * - For Commands: This means the command will be submitted to the scheduler, but the AutoBuilder will not wait for it to finish. The CommandScheduler will run the command in the background.
 * <p>
 * - For other methods: This means the AutoBuilder will not check if the method returns true before continuing.
 * <p>
 * <STRONG>NOTE:</STRONG> Only annotate classes that are commands with this annotation. If the class is not a command, the annotation will be ignored.
 * Methods can be annotated with this annotation regardless of whether or not they are commands.
 * <p>
 * Example Use Case:
 * <p>
 * You have a shoot command/method, and you want to wait until the shooter shoots before continuing. You can annotate the shoot command with this annotation, and the AutoBuilder will wait until the
 * shooter is done shooting before continuing.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface RequireWait {

}

package com.dacubeking.AutoBuilder.RobotSide.serialization;

import com.dacubeking.AutoBuilder.RobotSide.serialization.command.CommandExecutionFailedException;
import com.dacubeking.AutoBuilder.RobotSide.serialization.command.SendableScript;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;

import java.util.List;


@JsonIgnoreProperties(ignoreUnknown = true)
public class ScriptAutonomousStep extends AbstractAutonomousStep {

    private final SendableScript sendableScript;

    @JsonCreator
    public ScriptAutonomousStep(@JsonProperty(required = true, value = "sendableScript") SendableScript sendableScript) {
        this.sendableScript = sendableScript;
    }

    @Override
    public @NotNull String toString() {
        return "ScriptAutonomousStep{" + "sendableScript='" + sendableScript + '\'' + '}';
    }

    @JsonProperty
    public SendableScript getSendableScript() {
        return sendableScript;
    }

    /**
     * Runs the script
     */
    @Override
    public void execute(@NotNull List<SendableScript> scriptsToExecuteByTime,
                        @NotNull List<SendableScript> scriptsToExecuteByPercent)
            throws InterruptedException, CommandExecutionFailedException {

        if (sendableScript.getDelayType() == SendableScript.DelayType.TIME) {
            scriptsToExecuteByTime.add(sendableScript);
            return;
        }

        if (sendableScript.getDelayType() == SendableScript.DelayType.PERCENT) {
            scriptsToExecuteByPercent.add(sendableScript);
            return;
        }


        sendableScript.execute();
    }
}
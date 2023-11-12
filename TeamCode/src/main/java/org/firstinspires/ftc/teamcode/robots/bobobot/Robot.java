package org.firstinspires.ftc.teamcode.robots.bobobot;

import static org.firstinspires.ftc.teamcode.robots.taubot.util.Constants.LOW_BATTERY_VOLTAGE;

import com.acmerobotics.dashboard.telemetry.TelemetryPacket;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.internal.system.Misc;

import java.util.LinkedHashMap;
import java.util.Map;


public class Robot {
        Telemetry telemetry;
        DriveTrain driveTrain;
        Drone droneLaunch;
        IntakeClaw claw;
        private double averageVoltage;
        public Robot(Telemetry telemetry, HardwareMap hardwareMap)
        {
            this.telemetry=telemetry;
            driveTrain = new DriveTrain(telemetry, hardwareMap);
            droneLaunch = new Drone(telemetry, hardwareMap);
            claw = new IntakeClaw(telemetry, hardwareMap);
            motorInit();
            droneInit();
            intakeClawInit();

        }
        public void motorInit()
        {
            driveTrain.motorInit();

        }
        public void droneInit()
        {
            droneLaunch.droneInit();
        }
        public void intakeClawInit()
        {
            claw.intakeClawInit();
        }

    private void handleTelemetry(Map<String, Object> telemetryMap, String telemetryName, TelemetryPacket packet) {
        telemetry.addLine(telemetryName);
        packet.addLine(telemetryName);

        if (averageVoltage <= LOW_BATTERY_VOLTAGE) {
            telemetryMap = new LinkedHashMap<>();
            for (int i = 0; i < 20; i++) {
                telemetryMap.put(i + (System.currentTimeMillis() / 500 % 2 == 0 ? "**BATTERY VOLTAGE LOW**" : "  BATTERY VOLTAGE LOW  "), (System.currentTimeMillis() / 500 % 2 == 0 ? "**CHANGE BATTERY ASAP!!**" : "  CHANGE BATTERY ASAP!!  "));
            }
        }
        for (Map.Entry<String, Object> entry : telemetryMap.entrySet()) {
            String line = Misc.formatInvariant("%s: %s", entry.getKey(), entry.getValue());
            packet.addLine(line);
            telemetry.addLine(line);
        }
    }
}

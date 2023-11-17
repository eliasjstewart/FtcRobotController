package org.firstinspires.ftc.teamcode.robots.bobobot;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.config.Config;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;


@Autonomous (name = "BoboAuto")
public class BoboAuto extends OpMode {

    Autobot autobobo;
    FtcDashboard dashboard;
    @Override
    public void init() {
        autobobo = new Autobot(telemetry, hardwareMap);
        dashboard = FtcDashboard.getInstance();
        dashboard.setTelemetryTransmissionInterval(25);
        telemetry.setMsTransmissionInterval(25);
    }

    @Override
    public void loop() {
        autobobo.drive.tile(1);
        autobobo.drive.driveTelemetry();
    }

}

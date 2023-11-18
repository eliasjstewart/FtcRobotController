package org.firstinspires.ftc.teamcode.robots.bobobot;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.config.Config;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;


@Autonomous (name = "BoboAuto")
public class BoboAuto extends OpMode {

    Autobot autobobo;
//    boolean ranOnce;
    FtcDashboard dashboard;
    @Override
    public void init() {
        autobobo = new Autobot(telemetry, hardwareMap);
//      ranOnce = false;
        dashboard = FtcDashboard.getInstance();
        dashboard.setTelemetryTransmissionInterval(25);
        telemetry.setMsTransmissionInterval(25);
    }

    boolean didDriveTile = false;
    @Override
    public void loop() {
        autobobo.grip.autoWrist();
        if(didDriveTile == false) {
              if(autobobo.drive.tile(1)) {
                  didDriveTile = true;
              }
            }
//        autobobo.drive.right();
//        autobobo.drive.tile(1);
          autobobo.drive.driveTelemetry();

    }

}

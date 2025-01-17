package org.firstinspires.ftc.teamcode.robots.bobobot;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.robots.r2v2.util.Utils;

public class AutoClaw {
    private Servo clawSpan = null;
    public static double OPENCLAW = 0.1;
    public static double CLOSECLAW = 0.35;
    private Telemetry telemetry;
    private HardwareMap hardwareMap;
    private Servo clawWrist = null;
    public AutoClaw(Telemetry telemetry, HardwareMap hardwareMap) {
        this.telemetry = telemetry;
        this.hardwareMap = hardwareMap;
    }
    public void intakeTelemetry() {
        telemetry.addData("Claw Position \t", Utils.servoDenormalize(clawSpan.getPosition()));
        //telemetry.addData("Claw Arm Position \t", clawArm.getCurrentPosition());
        telemetry.addData("Claw Wrist Position \t", Utils.servoDenormalize(clawWrist.getPosition()));
    }
    public void openAuto() {
        clawSpan.setPosition(OPENCLAW);
    }

    public void closeAuto() {
        clawSpan.setPosition(CLOSECLAW);

    }

    public void gripInit() {
        //clawArm = this.hardwareMap.get(DcMotorEx.class, "clawArm");
        clawSpan = this.hardwareMap.get(Servo.class, "clawSpan");
        clawWrist = this.hardwareMap.get(Servo.class, "clawWrist");
        closeAuto();
        //clawArm.setDirection(DcMotor.Direction.REVERSE);
        //clawArm.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        //clawArm.setPower(1);
        //clawArm.setTargetPosition(0);
        //clawArm.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        //clawWrist.setPosition(0);

    }
    public void autoWrist(){
        clawWrist.setPosition(0);
    }
}

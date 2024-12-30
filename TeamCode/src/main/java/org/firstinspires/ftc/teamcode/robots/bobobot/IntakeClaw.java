package org.firstinspires.ftc.teamcode.robots.bobobot;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;
import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.teamcode.robots.r2v2.util.Utils;

public class IntakeClaw {
    private DcMotorEx clawArm = null;
    private Servo clawSpan = null;
    private Servo clawWrist = null;
    public static double OPENCLAW = 0.1;
    public static double CLOSECLAW = 0.35;
    public static double armliftSpeed = 0.2;
    public static double armSpeed = 2.0;
    private static int armPosition = 0;
    private static int maxArmTicks = 100;
    private static int armInitPosition = -129;
    private double powerArm = 0;
    private Telemetry telemetry;
    private HardwareMap hardwareMap;

    public IntakeClaw(Telemetry telemetry, HardwareMap hardwareMap) {
        this.telemetry = telemetry;
        this.hardwareMap = hardwareMap;
    }

    public void telemetryOutput() {
        telemetry.addData("Claw Position \t", Utils.servoDenormalize(clawSpan.getPosition()));
        telemetry.addData("Claw Arm Position \t", clawArm.getCurrentPosition());
        telemetry.addData("Claw Wrist Position \t", Utils.servoDenormalize(clawWrist.getPosition()));
    }

    public void intakeClawInit() {
        clawArm = this.hardwareMap.get(DcMotorEx.class, "clawArm");
        clawSpan = this.hardwareMap.get(Servo.class, "clawSpan");
        clawWrist = this.hardwareMap.get(Servo.class, "clawWrist");
        //clawArm.setDirection(DcMotor.Direction.REVERSE);
        clawArm.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        clawArm.setPower(1);
        clawArm.setTargetPosition(0);
        clawArm.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        clawWrist.setPosition(0);
        clawSpan.setPosition(CLOSECLAW);


    }

    public void openClaw(boolean press) {
        if (press == true)
            clawSpan.setPosition(OPENCLAW);
    }

    public void closeClaw(boolean press) {
        if (press == true)
            clawSpan.setPosition(CLOSECLAW);

    }

    public void clawArmLift(boolean press) {
        //int temptarget = clawArm.getCurrentPosition() + (int) (press * armSpeed);
        //if (temptarget > maxArmTicks)
        //    temptarget = maxArmTicks;
        if (press == true) {
            clawArm.setTargetPosition(240);
        }


    }

    public void armPositionTest() {
        clawArm.setTargetPosition(90);
    }

    public void clawArmLower(boolean press) {
        //int targettemp = clawArm.getCurrentPosition()-(int)(press*armSpeed);
        //if (targettemp <= 0)
        //    targettemp = 0;
        if (press == true) {
            clawArm.setTargetPosition(0);
        }


    }

    public void armWristDown(boolean press) {
        if (press == true && clawArm.getCurrentPosition() < 200) {
            clawWrist.setPosition(0.45);
        }
        else if (press == true && clawArm.getCurrentPosition() > 200) {
            clawWrist.setPosition(0.3);
        }
    }

    public void armWristUp(boolean press) {
        if (press == true) {
            clawWrist.setPosition(0);
        }
    }



    //public double getClawPosition () {return clawSpan.getPosition();



}


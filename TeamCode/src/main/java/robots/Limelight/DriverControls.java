package robots.Limelight;

import com.qualcomm.robotcore.hardware.Gamepad;

public class DriverControls {

    static Robot robot;
    Gamepad gamepad1, gamepad2;
    public int pipelineIndex = robot.result.getPipelineIndex();
    public DriverControls(Gamepad pad){
        gamepad1 = new Gamepad();

    }

    public void changePipeline(){
//        if(gamepad1.a){
//            switch(){
//                case 0:
//
//                    break;
//            }
//        }
    }
}

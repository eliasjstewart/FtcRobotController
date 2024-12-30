package robots.Limelight;

import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.LLResultTypes;
import com.qualcomm.hardware.limelightvision.LLStatus;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.robotcore.hardware.HardwareMap;


public class Robot {

    public LLResult result;
    Limelight3A limelight;

    HardwareMap hardwareMap;

    Robot(HardwareMap hardwareMap) {
        limelight = hardwareMap.get(Limelight3A.class, "Limelight");
        limelight.setPollRateHz(100); // This sets how often we ask Limelight for data (100 times per second)
        limelight.start(); // This tells Limelight to start looking!
        limelight.pipelineSwitch(0);

        LLResult result = limelight.getLatestResult();
//        if (result != null && result.isValid()) {
//            double tx = result.getTx(); // How far left or right the target is (degrees)
//            double ty = result.getTy(); // How far up or down the target is (degrees)
//            double ta = result.getTa(); // How big the target looks (0%-100% of the image)
//
//            telemetry.addData("Target X", tx);
//            telemetry.addData("Target Y", ty);
//            telemetry.addData("Target Area", ta);
//        } else {
//            telemetry.addData("Limelight", "No Targets");
    }







}

package org.firstinspires.ftc.teamcode.robots.reach.vision.dummy;

import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.robots.reach.vision.StackHeight;
import org.firstinspires.ftc.teamcode.robots.reach.vision.VisionProvider;
import org.firstinspires.ftc.teamcode.vision.Viewpoint;

public abstract class AbstractDummyVisionIntegration implements VisionProvider {
    @Override
    public void initializeVision(HardwareMap hardwareMap, Viewpoint viewpoint, boolean enableDashboard) {

    }

    @Override
    public abstract StackHeight detect();

    @Override
    public void shutdownVision() {}

    @Override
    public void reset() {}

}

package org.firstinspires.ftc.teamcode.robots.ri2d.vision.dummy;

import org.firstinspires.ftc.teamcode.robots.ri2d.vision.StackHeight;

public class FourDummyVisionIntegration extends AbstractDummyVisionIntegration {

    @Override
    public StackHeight detect() {
        return StackHeight.FOUR;
    }

}

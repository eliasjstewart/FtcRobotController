package org.firstinspires.ftc.teamcode.robots.reachRefactor.vision.provider.dummy;

import org.firstinspires.ftc.teamcode.robots.reachRefactor.vision.Position;

public class MiddleDummyProvider extends AbstractDummyProvider {

    private static final String TELEMETRY_NAME = "Middle Dummy Vision Provider";

    @Override
    public Position getPosition() {
        return Position.MIDDLE;
    }
    @Override
    public String getTelemetryName() {
        return TELEMETRY_NAME;
    }
}

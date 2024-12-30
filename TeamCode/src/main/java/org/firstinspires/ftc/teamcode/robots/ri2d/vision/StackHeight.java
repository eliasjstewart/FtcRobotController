package org.firstinspires.ftc.teamcode.robots.ri2d.vision;

public enum StackHeight {
    ZERO('A'), ONE('B'), FOUR('C'), HOLD_STATE, NONE_FOUND;

    char targetZone;

    private StackHeight() {}

    private StackHeight(char targetZone) {
        this.targetZone = targetZone;
    }
}

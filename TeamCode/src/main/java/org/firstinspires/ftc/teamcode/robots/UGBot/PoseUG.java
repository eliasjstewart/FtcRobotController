package org.firstinspires.ftc.teamcode.robots.UGBot;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.canvas.Canvas;
import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.dashboard.telemetry.TelemetryPacket;
import com.qualcomm.hardware.bosch.BNO055IMU;
import com.qualcomm.hardware.rev.RevBlinkinLedDriver;
import com.qualcomm.hardware.rev.RevColorSensorV3;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DistanceSensor;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.hardware.VoltageSensor;

import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;
import org.firstinspires.ftc.teamcode.robots.UGBot.utils.CanvasUtils;
import org.firstinspires.ftc.teamcode.robots.UGBot.utils.CanvasUtils.Point;
import org.firstinspires.ftc.teamcode.robots.UGBot.utils.Constants;
import org.firstinspires.ftc.teamcode.robots.UGBot.utils.KinematicModel;
import org.firstinspires.ftc.teamcode.robots.UGBot.utils.TrajectoryCalculator;
import org.firstinspires.ftc.teamcode.robots.UGBot.utils.TrajectorySolution;
import org.firstinspires.ftc.teamcode.robots.UGBot.vision.StackHeight;
import org.firstinspires.ftc.teamcode.util.PIDController;
import org.firstinspires.ftc.teamcode.vision.Viewpoint;

import java.util.Arrays;

import static org.firstinspires.ftc.teamcode.robots.UGBot.utils.Constants.INTAKE_MINIJOG_NOW;
import static org.firstinspires.ftc.teamcode.robots.UGBot.utils.Constants.INTAKE_MINIJOG_POWER;
import static org.firstinspires.ftc.teamcode.robots.UGBot.utils.Constants.INTAKE_MINIJOG_TIME;
import static org.firstinspires.ftc.teamcode.robots.UGBot.utils.Constants.INTAKE_ROLLING_JOG_BW_NOW;
import static org.firstinspires.ftc.teamcode.robots.UGBot.utils.Constants.INTAKE_ROLLING_JOG_BW_POWER;
import static org.firstinspires.ftc.teamcode.robots.UGBot.utils.Constants.INTAKE_ROLLING_JOG_FW_NOW;
import static org.firstinspires.ftc.teamcode.robots.UGBot.utils.Constants.INTAKE_ROLLING_JOG_FW_POWER;
import static org.firstinspires.ftc.teamcode.robots.UGBot.utils.Constants.INTAKE_ROLLING_JOG_FW_TIME;
import static org.firstinspires.ftc.teamcode.robots.UGBot.utils.Constants.INTAKE_ROLLING_RING_DELAY;
import static org.firstinspires.ftc.teamcode.robots.UGBot.utils.Constants.INTAKE_ROLLING_RING_FAR;
import static org.firstinspires.ftc.teamcode.robots.UGBot.utils.Constants.INTAKE_ROLLING_RING_NEAR;
import static org.firstinspires.ftc.teamcode.robots.UGBot.utils.Constants.INTAKE_ROLLING_RING_TOO_FAR;
import static org.firstinspires.ftc.teamcode.robots.UGBot.utils.Constants.INTAKE_TO_TURRET_XFER_ANGLE;
import static org.firstinspires.ftc.teamcode.robots.UGBot.utils.Constants.INTAKE_TO_TURRET_XFER_ELEVATION;
import static org.firstinspires.ftc.teamcode.util.utilMethods.between;
import static org.firstinspires.ftc.teamcode.util.utilMethods.futureTime;
import static org.firstinspires.ftc.teamcode.util.utilMethods.wrap360;
import static org.firstinspires.ftc.teamcode.util.utilMethods.wrapAngle;
import static org.firstinspires.ftc.teamcode.util.utilMethods.wrapAngleMinus;
import static org.firstinspires.ftc.teamcode.vision.Config.ALIGN_D;
import static org.firstinspires.ftc.teamcode.vision.Config.ALIGN_I;
import static org.firstinspires.ftc.teamcode.vision.Config.ALIGN_P;

/**
 * The Pose class stores the current real world position/orientation:
 * <b>position</b>, <b>heading</b>, and <b>speed</b> of the robot.
 *
 * This class should be a point of reference for any navigation classes that
 * want to know current orientation and location of the robot. The update method
 * must be called regularly, it monitors and integrates data from the
 * orientation (IMU) and odometry (motor encoder) sensors.
 * 
 * @author plethora of ironreign programmers
 * @version 17564.70_b
 * @since 2018-11-02
 */

@Config
public class PoseUG {

    // setup
    HardwareMap hwMap;
    PIDController turnPID = new PIDController(0, 0, 0);
    PIDController distPID = new PIDController(0, 0, 0);
    PIDController alignPID = new PIDController(ALIGN_P, ALIGN_I, ALIGN_D);
    private int autoAlignStage = 0;
    FtcDashboard dashboard;
    public static double turnP = 0.0055; // proportional constant applied to error in degrees /.0055 seems very low
    public static double driveP = .01;
    public static double turnI = 0.0; // integral constant
    public static double turnD = .13; // derivative constant
    public static double distP = 0.5; // proportional constant applied to error in meters
    public static double distI = 0.0; // integral constant
    public static double distD = .19; // derivative constant
    public static double cutout = 1.0;

    public double headingP = 0.007;
    public double headingD = 0;

    public double balanceP = .35;
    public double balanceD = 3.1444;

    public long[] visionTimes = new long[] {};

    // All Actuators
    private DcMotor motorFrontRight = null;
    private DcMotor motorBackLeft = null;
    private DcMotor motorFrontLeft = null;
    private DcMotor motorBackRight = null;
    private DcMotor elbow = null;
    private DcMotorEx intakeMotor = null;
    private Servo tiltServo = null;
    private Servo outServo = null;
    private DcMotorEx flywheelMotor = null;
    private DcMotor gripperExtendABob = null;
    private DcMotor turretMotor = null;
    private Servo triggerServo = null;
    private Servo gripperServo = null;
    private Servo servoWiper = null;
    RevBlinkinLedDriver blinkin = null;

    // All Subsystems
    public Launcher launcher = null;
    public Turret turret = null;
    public Intake intake = null;

    private KinematicModel model;

    // All sensors
    BNO055IMU imu; // Inertial Measurement Unit: Accelerometer and Gyroscope combination sensor
    BNO055IMU turretIMU;
    DistanceSensor distForward;
    DistanceSensor distLeft;
    DistanceSensor distRight;
    RevColorSensorV3 bottomColorSensor;
    // DigitalChannel magSensor;

    private Constants.Target target = Constants.Target.NONE;
    public TrajectoryCalculator trajCalc = new TrajectoryCalculator(poseX, poseY, target);
    public TrajectorySolution trajSol = null;

    // drive train power values
    private double powerLeft = 0;
    private double powerRight = 0;
    // mecanum types
    private double powerFrontLeft = 0;
    private double powerFrontRight = 0;
    private double powerBackLeft = 0;
    private double powerBackRight = 0;

    // PID values
    public int forwardTPM = 1304;// todo- use drive IMU to get this perfect
    int rightTPM = 1304; // todo - these need to be tuned for each robot
    int leftTPM = 1304; // todo - these need to be tuned for each robot
    private int strafeTPM = 1909; // todo - fix value high priority this current value is based on Kraken -

    // minimech will be different
    public static double poseX;
    public static double poseY;
    private static double poseHeading; // current heading in degrees. Might be rotated by 90 degrees from imu's heading when strafing
    private double poseHeadingRad; // current heading converted to radians
    private double poseSpeed;
    private double velocityX, velocityY;
    private double posePitch;
    private double poseRoll;
    private long timeStamp; // timestamp of last update
    private boolean initialized = false; //todo: this should never be static in competition
    public double offsetHeading; //todo- maybe static
    private double offsetPitch;
    private double offsetRoll;
    private double goalHeading;

    public static double displacement;
    private double displacementPrev;
    private double odometer;

    private double cachedXAcceleration;
    private double lastXAcceleration;

    private double lastUpdateTimestamp = 0;
    private double loopTime = 0;

    private long turnTimer = 0;
    private boolean turnTimerInit = false;
    private double minTurnError = 2.0;
    public boolean maintainHeadingInit = false;

    private double poseSavedHeading = 0.0;

    public boolean isBlue = false;

    public int servoTesterPos = 1600;
    public double autonomousIMUOffset = 0;

    private int craneArticulation = 0;

    public boolean rangeIsHot = true;

    public enum MoveMode {
        forward, backward, left, right, rotate, still;
    }

    protected MoveMode moveMode;

    public enum Articulation { // serves as a desired robot articulation which may include related complex movements of the elbow, lift and supermanLeft
        inprogress, // currently in progress to a final articulation
        manual, // target positions are all being manually overridden
        toggleTrigger,
        cardinalBaseRight,
        cardinalBaseLeft,
        returnHome,
        testShot,
        intakeDisk,
        dumpWobbleGoal,
        enterWobbleGoalMode,
        exitWobbleGoalMode,
        deployWobbleGoalAuton,
        releaseWobbleGoalAuton,
        ohBabyATriple
    }

    public enum RobotType {
        BigWheel, Icarus, Minimech, TomBot, UGBot;
    }

    public RobotType currentBot;

    public Articulation getArticulation() {
        return articulation;
    }

    protected Articulation articulation = Articulation.manual;
    double articulationTimer = 0;

    Orientation imuAngles; // pitch, roll and yaw from the IMU
    // roll is in x, pitch is in y, yaw is in z

    public boolean isAutonSingleStep() {
        return autonSingleStep;
    }

    public void setAutonSingleStep(boolean autonSingleStep) {
        this.autonSingleStep = autonSingleStep;
    }

    boolean autonSingleStep = false; // single step through auton deploying stages to facilitate testing and demos

    public void setIsBlue(boolean blue) {
        isBlue = blue;
    }
    //////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////
    //// ////
    //// Constructors ////
    //// ////
    //////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Create a Pose instance that stores all real world position/orientation
     * elements: <var>x</var>, <var>y</var>, <var>heading</var>, and
     * <var>speed</var>.
     *
     * @param x       The position relative to the x axis of the field
     * @param y       The position relative to the y axis of the field
     * @param heading The heading of the robot
     * @param speed   The speed of the robot
     */
    public PoseUG(double x, double y, double heading, double speed) {

        poseX = x;
        poseY = y;
        poseHeading = heading;
        poseSpeed = speed;
        posePitch = 0;
        poseRoll = 0;
    }

    /**
     * Creates a Pose instance with _0 speed, to prevent muscle fatigue by excess
     * typing demand on the software team members.
     *
     * @param x     The position relative to the x axis of the field
     * @param y     The position relative to the y axis of the field
     * @param angle The vuAngle of the robot
     */
    public PoseUG(double x, double y, double angle) {

        poseX = x;
        poseY = y;
        poseHeading = angle;
        poseSpeed = 0;

    }

    /**
     * Creates a base Pose instance at the origin, (_0,_0), with _0 speed and _0
     * vuAngle. Useful for determining the Pose of the robot relative to the origin.
     */



    public PoseUG(RobotType name) {

        poseX = Constants.startingXOffset;
        poseY = Constants.startingYOffset;
        poseHeading = 0;
        poseSpeed = 0;
        posePitch = 0;
        poseRoll = 0;

        currentBot = name;

    }

    //////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////
    ////                                                                                  ////
    ////                                   Init/Update                                    ////
    ////                                                                                  ////
    //////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Initializes motors, servos, lights and sensors from a given hardware map
     *
     * @param ahwMap Given hardware map
     */
    public void init(HardwareMap ahwMap) {
        hwMap = ahwMap;
        /*
         * eg: Initialize the hardware variables. Note that the strings used here as
         * parameters to 'get' must correspond to the names assigned during the robot
         * configuration step (using the FTC Robot Controller app on the phone).
         */

        // create hwmap with config values
        // this.driveLeft = this.hwMap.dcMotor.get("driveLeft");
        // this.driveRight = this.hwMap.dcMotor.get("driveRight");
        this.elbow = this.hwMap.dcMotor.get("elbow");

        this.flywheelMotor = (DcMotorEx) this.hwMap.dcMotor.get("flywheelMotor");
        this.gripperExtendABob = (DcMotor) this.hwMap.dcMotor.get("gripperExtendABob");
        this.triggerServo = this.hwMap.servo.get("triggerServo");
        this.gripperServo = this.hwMap.servo.get("gripperServo");
        this.servoWiper = this.hwMap.servo.get("servoWiper");

        this.intakeMotor = (DcMotorEx) this.hwMap.dcMotor.get("intakeMotor");
        this.tiltServo = this.hwMap.servo.get("tiltServo");
        this.outServo = this.hwMap.servo.get("outServo");

        this.blinkin = hwMap.get(RevBlinkinLedDriver.class, "blinkin");
        this.distForward = this.hwMap.get(DistanceSensor.class, "distForward");
        this.distRight = this.hwMap.get(DistanceSensor.class, "distRight");
        this.distLeft = this.hwMap.get(DistanceSensor.class, "distLeft");
        this.bottomColorSensor = this.hwMap.get(RevColorSensorV3.class, "bottomColorSensor");
        // this.magSensor = this.hwMap.get(DigitalChannel.class, "magSensor");
        // motorFrontLeft = hwMap.get(DcMotor.class, "motorFrontLeft");
        motorBackLeft = hwMap.get(DcMotor.class, "motorBackLeft");
        // motorFrontRight = hwMap.get(DcMotor.class, "motorFrontRight");
        motorBackRight = hwMap.get(DcMotor.class, "motorBackRight");
        turretMotor = hwMap.get(DcMotor.class, "turret");
        // elbow.setDirection(DcMotor.Direction.REVERSE);

        // motorFrontLeft.setDirection(DcMotor.Direction.FORWARD);
        motorBackLeft.setDirection(DcMotor.Direction.REVERSE);
        // motorFrontRight.setDirection(DcMotor.Direction.REVERSE);
        motorBackRight.setDirection(DcMotor.Direction.FORWARD);
        // motorFrontRight.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        motorBackLeft.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        // motorFrontLeft.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        motorBackRight.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        // turretMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        // magSensor.setMode(DigitalChannel.Mode.INPUT);

        //flywheelMotor.setDirection(DcMotor.Direction.REVERSE);

        // behaviors of motors
        /*
         * driveLeft.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
         * driveRight.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE); if
         * (this.currentBot == RobotType.BigWheel) {
         * driveLeft.setDirection(DcMotorSimple.Direction.FORWARD);
         * driveRight.setDirection(DcMotorSimple.Direction.REVERSE); } else {
         * driveLeft.setDirection(DcMotorSimple.Direction.REVERSE);
         * driveRight.setDirection(DcMotorSimple.Direction.FORWARD); }
         */
        // setup subsystems
        launcher = new Launcher(elbow, flywheelMotor, gripperExtendABob, triggerServo, gripperServo, servoWiper);
        turretIMU = hwMap.get(BNO055IMU.class, "imuTest");
        turret = new Turret(turretMotor, turretIMU);
        intake = new Intake(intakeMotor, tiltServo, outServo);

        moveMode = MoveMode.still;

        // setup both IMU's (Assuming 2 rev hubs
        BNO055IMU.Parameters parametersIMU = new BNO055IMU.Parameters();
        parametersIMU.angleUnit = BNO055IMU.AngleUnit.DEGREES;
        parametersIMU.accelUnit = BNO055IMU.AccelUnit.METERS_PERSEC_PERSEC;
        parametersIMU.loggingEnabled = true;
        parametersIMU.loggingTag = "baseIMU";

        imu = hwMap.get(BNO055IMU.class, "baseIMU");
        imu.initialize(parametersIMU);

        Constants.ALLIANCE_INT_MOD = Math.abs(Constants.ALLIANCE_INT_MOD);

        // initialize vision

//        VuforiaLocalizer vuforia;
//        VuforiaLocalizer.Parameters parameters = new VuforiaLocalizer.Parameters();
//        parameters.vuforiaLicenseKey = RC.VUFORIA_LICENSE_KEY;
//        parameters.cameraName = hwMap.get(WebcamName.class, "Webcam 1");
//        vuforia = ClassFactory.getInstance().createVuforia(parameters);
//        Vuforia.setFrameFormat(PIXEL_FORMAT.RGB565, true);
//        vuforia.setFrameQueueCapacity(1);
//        towerHeightPipeline = new TowerHeightPipeline(hwMap, vuforia);

        // dashboard
        dashboard = FtcDashboard.getInstance();

        model = new KinematicModel();
    }

    public void setVisionTimes(long[] visionTimes) {
        this.visionTimes = visionTimes;
    }

    private void initVuforia(HardwareMap hardwareMap, Viewpoint viewpoint) {

    }

    public void resetIMU() {
        BNO055IMU.Parameters parametersIMU = new BNO055IMU.Parameters();
        parametersIMU.angleUnit = BNO055IMU.AngleUnit.DEGREES;
        parametersIMU.accelUnit = BNO055IMU.AccelUnit.METERS_PERSEC_PERSEC;
        parametersIMU.loggingEnabled = true;
        parametersIMU.loggingTag = "IMU";

        imu.initialize(parametersIMU);
    }

    public void resetEncoders() {
        launcher.resetEncoders();
    }

    private StackHeight detection = StackHeight.HOLD_STATE;

    public void setDetection(StackHeight detection) {this.detection = detection;}
    public StackHeight getDetection() {return detection;}

    private int frameCount;
    public void setFrameCount(int frameCount) {
        this.frameCount = frameCount;
    }

    private float visionFPS;
    public void setVisionFPS(float visionFPS) {
        this.visionFPS = visionFPS;
    }

    private int totalFrameTimeMs;
    public void setTotalFrameTimeMs(int totalFrameTimeMs) {
        this.totalFrameTimeMs = totalFrameTimeMs;
    }

    private int pipelineTimeMs;
    public void setPipelineTimeMs(int pipelineTimeMs) {
        this.pipelineTimeMs = pipelineTimeMs;
    }

    private int overheadTimeMs;
    public void setOverheadTimeMs(int overheadTimeMs) {
        this.overheadTimeMs = overheadTimeMs;
    }

    private int currentPipelineMaxFps;
    public void setCurrentPipelineMaxFps(int currentPipelineMaxFps) {
        this.currentPipelineMaxFps = currentPipelineMaxFps;
    }

    private double aspectRatio;
    public void setAspectRatio(double aspectRatio) {
        this.aspectRatio = aspectRatio;
    }

    public void sendTelemetry() {
        TelemetryPacket packet = new TelemetryPacket();
        Canvas fieldOverlay = packet.fieldOverlay();

        Point posePoint = new Point(getPoseX(), getPoseY());

        // goal location
        Point goalCanvasPoint = CanvasUtils.toCanvasPoint(new Point(Constants.Target.HIGH_GOAL.getX(), Constants.goalY));
        fieldOverlay.strokeCircle(goalCanvasPoint.getX(), goalCanvasPoint.getY(), Constants.GOAL_RADIUS);

        // robot location
        Point robotCanvasPoint = CanvasUtils.toCanvasPoint(posePoint);
        fieldOverlay.strokeCircle(robotCanvasPoint.getX(), robotCanvasPoint.getY(), Constants.ROBOT_RADIUS_INCHES);

        // turret center in field coords
        Point turretCenter = new Point(model.getTurretX(), model.getTurretY());
        //Point turretCenter = new Point(getPoseX()  * Math.sin(poseHeadingRad), (getPoseY() - Constants.TURRET_AXIS_OFFSET) * Math.cos(poseHeadingRad));

        Point turretCanvasCenter = CanvasUtils.toCanvasPoint(turretCenter);
        fieldOverlay.strokeCircle(turretCanvasCenter.getX(), turretCanvasCenter.getY(), Constants.TURRET_RADIUS * Constants.INCHES_PER_METER);

        //muzzle center
        //muzzle angle in field orientation

        Point muzzleCenterField = new Point(model.getMuzzleX(), model.getMuzzleY());
        Point muzzleCanvasCenter = CanvasUtils.toCanvasPoint(muzzleCenterField);
        fieldOverlay.strokeCircle(muzzleCanvasCenter.getX(), muzzleCanvasCenter.getY(), 2.5);

        // power shots
        Point firstPowerShot = CanvasUtils.toCanvasPoint(new Point(Constants.Target.FIRST_POWER_SHOT.getX(), Constants.Target.FIRST_POWER_SHOT.y));
        Point secondPowerShot = CanvasUtils.toCanvasPoint(new Point(Constants.Target.SECOND_POWER_SHOT.getX(), Constants.Target.SECOND_POWER_SHOT.y));
        Point thirdPowerShot = CanvasUtils.toCanvasPoint(new Point(Constants.Target.THIRD_POWER_SHOT.getX(), Constants.Target.THIRD_POWER_SHOT.y));

        fieldOverlay.strokeCircle(firstPowerShot.getX(), firstPowerShot.getY(), Constants.POWER_SHOT_RADIUS);
        fieldOverlay.strokeCircle(secondPowerShot.getX(), secondPowerShot.getY(), Constants.POWER_SHOT_RADIUS);
        fieldOverlay.strokeCircle(thirdPowerShot.getX(), thirdPowerShot.getY(), Constants.POWER_SHOT_RADIUS);

        if(!target.equals(Constants.Target.NONE)) {
            // bearing to offset (speed corrected) target (dark green)
            Point offsetTargetPoint = CanvasUtils.toCanvasPoint(new Point(trajSol.getxOffset(), getTarget().y));
            fieldOverlay.setStroke("#4D934D");
            fieldOverlay.strokeLine(muzzleCanvasCenter.getX(), muzzleCanvasCenter.getY(), offsetTargetPoint.getX(), offsetTargetPoint.getY());

            // bearing to target (neon green)
            Point targetPoint = CanvasUtils.toCanvasPoint(new Point(getTarget().getX(), getTarget().y));
            fieldOverlay.setStroke("#39FF14");
            fieldOverlay.strokeLine(muzzleCanvasCenter.getX(), muzzleCanvasCenter.getY(), targetPoint.getX(), targetPoint.getY());

            // actual heading (purple)
            CanvasUtils.drawVector(fieldOverlay, turretCenter, 1000, turret.getHeading(), "#6A0DAD");
        }

        // robot heading (black)
        CanvasUtils.drawVector(fieldOverlay, posePoint, 2 * Constants.ROBOT_RADIUS_INCHES, poseHeading, "#000000");

        // turret heading (red)
        CanvasUtils.drawVector(fieldOverlay, turretCenter, 3 * Constants.ROBOT_RADIUS_INCHES, turret.getHeading(), "#FF0000");

        packet.put("trajectory calculator bearing to target", trajSol.getBearing());
        packet.put("trajectory calculator flywheel speed output", trajSol.getVelocity());
        packet.put("trajectory calculator launcher elevation output", trajSol.getElevation());

        packet.put("TurretMotorPos", turret.getTicksPerDegree());
        packet.put("TurretTicksPerDegree", turret.getTicksPerDegree());
        packet.put("virtual IMU heading", turret.getVirtualHeading());
        packet.put("virtual heading variance", turret.getHeadingVariance());

        // vision
        packet.put("Right Laser", getDistRightDist());
        packet.put("Frame Count", frameCount);
        packet.put("FPS", String.format("%.2f", visionFPS));
        packet.put("Total frame time ms", totalFrameTimeMs);
        packet.put("Pipeline time ms", pipelineTimeMs);
        packet.put("Overhead time ms", overheadTimeMs);
        packet.put("Theoretical max FPS", currentPipelineMaxFps);
        packet.put("Aspect ratio", aspectRatio);

        packet.put("detection", detection);
        packet.put("vision times", Arrays.toString(visionTimes));
        long totalTime = 0;
        for(long time: visionTimes)
            totalTime += time;
        packet.put("total time", totalTime);
        packet.put("current flywheel velocity", launcher.getFlywheelTPS());
        packet.put("target flywheel velocity", launcher.getFlywheelTargetTPS());
        packet.put("flywheel motor power", launcher.flywheelMotor.getPower() * 200);
        packet.put("pose y",getPoseY());
        packet.put("pose x",getPoseX());
        packet.put("velocity x", velocityX);
        packet.put("velocity y", velocityY);
        packet.put("target angle for the thing", goalHeading);
        packet.put("avg ticks",getAverageTicks());
        packet.put("voltage", getBatteryVoltage());
        packet.put("emaAmperage", intake.EMAofIntakeAmps);
//        packet.put("x offset", trajSol.getxOffset());
//        packet.put("disk speed", trajSol.getVelocity());
//        packet.put("pose speed", poseSpeed);
//        packet.put("rotVelBase", rotVelBase);
        packet.put("zero indicator", 0);
        packet.put("turret error", turret.turretPID.getError());
        packet.put("turret integrated error", turret.turretPID.getTotalError());
        packet.put("turret derivative error", turret.turretPID.getDeltaError());//ben was here
        packet.put("base error", turnPID.getError());
        packet.put("base integrated error", turnPID.getTotalError());
        packet.put("base derivative error", turnPID.getDeltaError());
        packet.put("turret angle offset", Constants.MUZZLE_ANGLE_OFFSET_IN_TELE_OP);
        packet.put("staring height offset", Constants.STARTING_HEIGHT_OFFSET);
        packet.put("IntakeThing", intake.intakeMotor.getCurrent(CurrentUnit.AMPS));
        packet.put("EMAofIntakeAmps", intake.EMAofIntakeAmps);
        packet.put("EMAofIntakeAmpsLowerLimit", Constants.INTAKE_AUTO_PICKUP_AMPS_LIM);
        packet.put("2Bottom color sensor R", bottomColorSensor.red());
        packet.put("3Bottom color sensor G", bottomColorSensor.green());
        packet.put("1Bottom color sensor B", bottomColorSensor.blue());
//        packet.put("4is on line", (bottomColorSensor.red() + bottomColorSensor.green() + bottomColorSensor.blue() > 900 * Constants.LINE_DETECTION_THRESHHOLD) &&  bottomColorSensor.green() > bottomColorSensor.red() && bottomColorSensor.green() > bottomColorSensor.blue() ? 100 : 0);
        packet.put("extendabobtarget", launcher.gripperExtendABobTargetPos);
        packet.put("loop time", loopTime);

//        packet.put("exit point x", turretCenter.getX() + Constants.LAUNCHER_Y_OFFSET * Math.sin(Math.toRadians(turret.getHeading())));
//        packet.put("exit point y",  turretCenter.getY() + Constants.LAUNCHER_X_OFFSET * Math.cos(Math.toRadians(turret.getHeading())));

        dashboard.sendTelemetryPacket(packet);
    }

    private Double oldValue;

    public double exponentialMovingAverage(double value) {
        if (oldValue == null) {
            oldValue = value;
            return value;
        }
        double newValue = oldValue + Constants.K_AMP_ALPHA * (value - oldValue);
        oldValue = newValue;
        return newValue;
    }

    public double getGoalHeading() {
        return goalHeading;
    }

    double getBatteryVoltage() {
        double result = Double.POSITIVE_INFINITY;
        for (VoltageSensor sensor : hwMap.voltageSensor) {
            double voltage = sensor.getVoltage();
            if (voltage > 0) {
                result = Math.min(result, voltage);
            }
        }
        return result;
    }

    /**
     * update the current location of the robot. This implementation gets heading
     * and orientation from the Bosch BNO055 IMU and assumes a simple differential
     * steer robot with left and right motor encoders. also updates the positions of
     * robot subsystems; make sure to add each subsystem's update class as more are
     * implemented.
     * <p>
     * <p>
     * The current naive implementation assumes an unobstructed robot - it cannot
     * account for running into objects and assumes no slippage in the wheel
     * encoders. Debris on the field and the mountain ramps will cause problems for
     * this implementation. Further work could be done to compare odometry against
     * IMU integrated displacement calculations to detect stalls and slips
     * <p>
     * This method should be called regularly - about every 20 - 30 milliseconds or
     * so.
     *
     * @param imu
     * @param ticksLeft
     * @param ticksRight
     */
    double rotVelBase = 0.0;
    double prevHeading = getHeading();
    boolean flywheelIsActive = false;
    int numLoops = 0;
    int laggyCounter = 0;
    boolean autoLaunchActive = false;
    double autoLaunchTimer = 0.0;
    boolean ringChambered = false;

    public void update(BNO055IMU imu, long ticksLeft, long ticksRight, boolean isActive) {
        long currentTime = System.nanoTime();

        imuAngles = imu.getAngularOrientation().toAxesReference(AxesReference.INTRINSIC).toAxesOrder(AxesOrder.ZYX);
        if (!initialized) {
            // first time in - we assume that the robot has not started moving and that
            // orientation values are set to the current absolute orientation
            // so first set of imu readings are effectively offsets

            offsetHeading = wrapAngleMinus((double) (360 - imuAngles.firstAngle), poseHeading);
            offsetRoll = wrapAngleMinus(imuAngles.secondAngle, poseRoll);
            offsetPitch = wrapAngleMinus(imuAngles.thirdAngle, posePitch);
            initialized = true;
        }
        poseHeading = wrapAngle(360 - imuAngles.firstAngle, offsetHeading);
        posePitch = wrapAngle(imuAngles.thirdAngle, offsetPitch);
        poseRoll = wrapAngle(imuAngles.secondAngle, offsetRoll);

        //monitor for tent and motors moving to start ringevator rolling
        //placing this monitor early since we want a good chance of it being overridden by subsequent intake behaviors
        TentRingevatorMonitor();

        /*
         * double jerkX = (cachedXAcceleration - lastXAcceleration) / loopTime; boolean
         * correct = false;
         * 
         * if (Math.abs(jerkX) > 0.1) { driveMixerTank(1, 0); correct = true; } int
         * correctionswitch = 0; double correctionTime = 0; if(correct){ switch
         * (correctionswitch){ case 0: correctionTime = futureTime(2);
         * correctionswitch++; break; case 1: driveMixerTank(1,0);
         * if(System.nanoTime()>correctionTime){ correctionswitch++; } break; default:
         * correctionswitch = 0; correct= false;
         * 
         * } }
         */
        /*
         * if(posePitch<300 && posePitch >10 imu.getAcceleration().xAccel > ){
         * driveMixerTank(-1,0); }
         */

        articulate(articulation); // call the most recently requested articulation

        // we haven't worked out the trig of calculating displacement from any
        // driveMixer combination, so
        // for now we are just restricting ourselves to cardinal relative directions of
        // pure forward, backward, left and right
        // so no diagonals or rotations - if we do those then our absolute positioning
        // fails
//
//        switch (moveMode) {
//            case forward:
//                displacement = (getAverageTicks() - displacementPrev) * forwardTPM;
//                odometer += Math.abs(displacement);
//                poseHeadingRad = Math.toRadians(poseHeading);
//                break;
//            case backward:
//                displacement = (getAverageTicks() - displacementPrev) * forwardTPM;
//                odometer += Math.abs(displacement);
//                poseHeadingRad = Math.toRadians(poseHeading);
//                break;
//            default:
//                displacement = 0; // when rotating or in an undefined moveMode, ignore/reset displacement
//                displacementPrev = 0;
//                break;
//        }
        displacement = (getAverageTicks() - displacementPrev) / forwardTPM;
        odometer += Math.abs(displacement);
        poseHeadingRad = Math.toRadians(poseHeading);

        odometer += Math.abs(displacement);
        poseSpeed = displacement / ((currentTime - this.timeStamp) / 1e9); // meters per second when ticks
                                                                                      // per meter is calibrated
        velocityX = poseSpeed * Math.sin(poseHeadingRad);
        velocityY = poseSpeed * Math.cos(poseHeadingRad);

        timeStamp = currentTime;
        displacementPrev = getAverageTicks();

        rotVelBase = (getHeading() - prevHeading) / (loopTime / 1E9);
        prevHeading = getHeading();

        poseX += displacement * Math.sin(poseHeadingRad);
        poseY += displacement * Math.cos(poseHeadingRad);

//        if((bottomColorSensor.red() + bottomColorSensor.green() + bottomColorSensor.blue() > 900 * Constants.LINE_DETECTION_THRESHHOLD)
//                &&  bottomColorSensor.green() > bottomColorSensor.red()
//                && bottomColorSensor.green() > bottomColorSensor.blue())
//            poseY = Constants.MIDFIELD_COLOR_RESET_POSITION;

        lastXAcceleration = cachedXAcceleration;
        cachedXAcceleration = imu.getLinearAcceleration().xAccel;

        loopTime = System.nanoTime() - lastUpdateTimestamp;

        if(loopTime / 1E9 > .10){
            laggyCounter++;
        }
        numLoops++;

        lastUpdateTimestamp = System.nanoTime();

        model.update(
          getPoseX(), getPoseY(), poseHeadingRad, turret.getHeading(), launcher.getElbowAngle() ,turret.getTurretTargetHeading(), intake.getTiltTargetPosition(), powerLeft, powerRight
        );

        //monitor for extended gripper to set turret into Danger Mode
        if(launcher.IsGripperExtended()) turret.setDangerModeActive(true);
        else turret.setDangerModeActive(false);

        powerLeft = model.getLeftPower();
        powerRight = model.getRightPower();
        motorBackLeft.setPower(clampMotor(powerBackLeft));
        motorBackRight.setPower(clampMotor(powerBackRight));

        //turret.setTurntableAngle(model.getTurretHeading());

        trajCalc.updatePos(model.getMuzzleX(), model.getMuzzleY());
        trajCalc.updateVel(velocityX, velocityY);
        trajCalc.setTarget(target);
        trajSol = trajCalc.getTrajectorySolution();

        if(setTargetInProgress){
            setTarget(target);
        }

//        if(isRampedUp()){
//            blinkin.setPattern(RevBlinkinLedDriver.BlinkinPattern.GREEN);
//
//        }
//        else{
//            blinkin.setPattern(RevBlinkinLedDriver.BlinkinPattern.COLOR_WAVES_RAINBOW_PALETTE);
//        }

        maintainTarget();

        //auto intake when we are tented and ring crosses the right distance sensor
        TentTake();

        MiniJog(); //service any request for jogs
        RollingJogForward();
        RollingJogBackward();

//        if(bottomColorSensor.red() + bottomColorSensor.green() + bottomColorSensor.blue() > 900 * Constants.LINE_DETECTION_THRESHHOLD
//                &&  bottomColorSensor.green() > bottomColorSensor.red() && bottomColorSensor.green() > bottomColorSensor.blue()){
//            blinkin.setPattern(RevBlinkinLedDriver.BlinkinPattern.WHITE);
//        }


        //subsystem updates should be the very last movement methods called in this update cycle
        launcher.update();
        turret.update(getHeading());
        intake.update(); //watermelon
        sendTelemetry();
    }

    public void setAutoLaunchActive(boolean autoLaunchActive){
        this.autoLaunchActive = autoLaunchActive;
    }



    boolean setTargetInProgress = false;
    public void setTarget(Constants.Target target) {
        this.target = target;

        if(Constants.IN_WOBBLE_MODE){
            setTargetInProgress = true;
            exitWobbleGoalMode();
        }
        else{
            setTargetInProgress = false;
        }

        //when we come out of targeting, go to chassis relative mode and set up for catching rings
        if (target == Constants.Target.NONE){
            SetRingCatch();
        }
        else {
            turret.setCurrentMode(Turret.TurretMode.fieldRelative);}
    }

    public void SetRingCatch(){
        turret.setCurrentMode(Turret.TurretMode.chassisRelative);
        turret.setTurretAngle(INTAKE_TO_TURRET_XFER_ANGLE);
        launcher.setElbowTargetAngle(INTAKE_TO_TURRET_XFER_ELEVATION);
    }

    public Constants.Target getTarget() {
        return target; }

        private boolean noneInit = false;
    public void maintainTarget() {
        switch(target) {
            case NONE:
                if(noneInit == false) {
                    flywheelIsActive = false;
                    launcher.setFlywheelActivePID(false);
                    noneInit = true;
                }

                break;
            default:
                    noneInit = false;
                    goalHeading = getBearingTo(trajSol.getxOffset(), target.y);
                    turret.setTurretAngle(goalHeading + Constants.MUZZLE_ANGLE_OFFSET_IN_TELE_OP);

                    launcher.setElbowTargetAngle(trajSol.getElevation() * Constants.HEIGHT_MULTIPLIER);
                    flywheelIsActive = true;
                    ringChambered = true; //todo- when the color sensor is implemented, this is where it will be updated
                    //we might not need this when we get the color sensor implemented

                    if(autoLaunchActive &&
                            ringChambered &&
                            System.nanoTime() - autoLaunchTimer > Constants.autoLaunchTime * 1E9 &&
                            poseY < 66/Constants.INCHES_PER_METER &&
                            target != Constants.Target.NONE){
                        flywheelIsActive = true;
                        if(toggleTriggerArticulation()){
                            autoLaunchTimer = System.nanoTime();
                        }
                    }

                    if (flywheelIsActive) {
                        launcher.setFlywheelActivePID(true);
                        launcher.setFlywheelTargetTPS(trajSol.getAngularVelocity() * Constants.RPS_MULTIPLIER);
                    } else {
                        launcher.setFlywheelActivePID(false);
                    }
                break;
        }
    }

    public void updateSensors(boolean isActive) {
        update(imu, 0, 0, isActive);
    }

    public double getDistForwardDist() {
        return distForward.getDistance(DistanceUnit.METER);
    }

    public double getDistLeftDist() {
        return distLeft.getDistance(DistanceUnit.METER);
    }

    public double getDistRightDist() {
        return distRight.getDistance(DistanceUnit.METER);
    }


    /**
     * Drive forwards for a set power while maintaining an IMU heading using PID
     *
     * @param Kp          proportional multiplier for PID
     * @param Ki          integral multiplier for PID
     * @param Kd          derivative proportional for PID
     * @param pwr         set the forward power
     * @param targetAngle the heading the robot will try to maintain while driving
     */
    public void driveIMU(double Kp, double Ki, double Kd, double pwr, double targetAngle) {
        movePID(Kp, Ki, Kd, pwr, poseHeading, targetAngle);
    }

    public boolean driveIMUDistanceWithReset(double pwr, double targetAngle, boolean forward, double targetMeters) {
        if (!driveIMUDistanceInitialzed) {
            resetMotors(false);
        }
        return driveIMUDistance(pwr,  targetAngle,  forward,  targetMeters);
    }

    public boolean driveIMUUntilDistanceWithReset(double pwr, double targetAngle, boolean forward, double targetMeters) {
        if (!driveIMUDistanceInitialzed) {
            resetMotors(false);
        }
        return driveIMUUntilDistance(pwr,  targetAngle,  forward,  targetMeters);
    }

    /**
     * Drive with a set power for a set distance while maintaining an IMU heading
     * using PID This is a relative version
     *
     * @param pwr          set the forward power
     * @param targetAngle  the heading the robot will try to maintain while driving
     * @param forward      is the robot driving in the forwards/left (positive)
     *                     directions or backwards/right (negative) directions
     * @param targetMeters the target distance (in meters)
     */
    boolean driveIMUDistanceInitialzed = false;
    long driveIMUDistanceTarget = 0;

    public boolean driveIMUDistance(double pwr, double targetAngle, boolean forward, double targetMeters) {

        if (!driveIMUDistanceInitialzed) {
            // set what direction the robot is supposed to be moving in for the purpose of
            // the field position calculator

            // calculate the target position of the drive motors
            driveIMUDistanceTarget = (long) Math.abs((targetMeters * forwardTPM)) + Math.abs(getAverageTicks());
            driveIMUDistanceInitialzed = true;
        }

        if (!forward) {
            moveMode = moveMode.backward;
            targetMeters = -targetMeters;
            pwr = -pwr;
        } else
            moveMode = moveMode.forward;

        // if this statement is true, then the robot has not achieved its target
        // position
        if (Math.abs(driveIMUDistanceTarget) > Math.abs(getAverageTicks())) {
            // driveIMU(Kp, kiDrive, kdDrive, pwr, targetAngle);
            driveIMU(turnP, turnI, turnD, pwr, targetAngle, false);
            return false;
        } // destination achieved
        else {
            //stopAll();
            driveMixerDiffSteer(0, 0);
            driveIMUDistanceInitialzed = false;
            return true;
        }
    }


    public boolean driveAbsoluteDistance(double pwr, double targetAngle, boolean forward, double targetMeters, double closeEnoughDist) {
        //driveAbsoluteDistance(MaxPower, heading, forward, distance,.1)
        if (!forward) {
            moveMode = moveMode.backward;
            //targetMeters = -targetMeters;
            //pwr = -pwr;
        } else
            moveMode = moveMode.forward;

        targetAngle= wrap360(targetAngle);  //this was probably already done but repeated as a safety

        // if this statement is true, then the robot has not achieved its target
        // position
        if (Math.abs(targetMeters) > Math.abs(closeEnoughDist)) {
            //driveIMU(Kp, kiDrive, kdDrive, pwr, targetAngle);
            //driveIMU(turnP, turnI, turnD, pwr, wrap360(targetAngle), false);
            movePIDMixer(pwr, forward, targetMeters,0,getHeading(),targetAngle);
            return false;
        } // destination achieved
        else {
            stopChassis(); //todo: maybe this should be optional when you are stringing moves together
            return true;
        }
    }

    public boolean driveUntilXNotSoJankPossibly(double pwr, boolean forward, double targetX, double closeEnoughDist) {
        return driveToFieldPosition(getNewPosAway(targetX)[0], getNewPosAway(targetX)[1], forward, pwr, closeEnoughDist);
    }

    public boolean driveUntilXJank(double pwr, boolean forward, double targetX, double closeEnoughDist) {
        forward = !forward;
        //driveAbsoluteDistance(MaxPower, heading, forward, distance,.1)
        if (!forward) {
            moveMode = moveMode.backward;
            //targetMeters = -targetMeters;
            //pwr = -pwr;
        } else
            moveMode = moveMode.forward;



        // if this statement is true, then the robot has not achieved its target
        // position
        if (Math.abs(getX() - targetX) > Math.abs(closeEnoughDist)) {
            //driveIMU(Kp, kiDrive, kdDrive, pwr, targetAngle);
            //driveIMU(turnP, turnI, turnD, pwr, wrap360(targetAngle), false);

            movePIDMixer(pwr, forward, getX() - targetX,0,getHeading(),getHeading());
            return false;
        } // destination achieved
        else {
            stopChassis(); //todo: maybe this should be optional when you are stringing moves together
            return true;
        }
    }

    double WallVal = 0.0;

    int countOutliers = 0;
    double driveWallDistanceTarget = 0;

    public boolean alignmentRun(double pwr, double targetVal, double currentVal, boolean forward, double targetMeters) {

        if (!driveIMUDistanceInitialzed) {
            // set what direction the robot is supposed to be moving in for the purpose of
            // the field position calculator

            // calculate the target position of the drive motors
            driveWallDistanceTarget = (long) Math.abs((targetMeters * forwardTPM)) + Math.abs(getAverageTicks());
            driveIMUDistanceInitialzed = true;
        }



        if (!forward) {
            moveMode = moveMode.backward;
            targetMeters = -targetMeters;
            pwr = -pwr;
        } else
            moveMode = moveMode.forward;

//        if(Math.abs(getAverageTicks() / Math.abs(driveWallDistanceTarget)) > .90){
//            if(rotVelBase < 5){
//                shiftOdometer(getHeading());
//                initialized = false;
//            }
//        }

        // if this statement is true, then the robot has not achieved its target
        // position
        if (Math.abs(driveWallDistanceTarget) > Math.abs(getAverageTicks())) {
            // driveIMU(Kp, kiDrive, kdDrive, pwr, targetAngle);
            movegenericPIDMixer(turnP, turnI, turnD,pwr,0,currentVal,targetVal);
            return false;
        } // destination achieved\
        else {
            //stopAll();
            driveMixerDiffSteer(0, 0);
            driveIMUDistanceInitialzed = false;

            setPoseHeading(0);

            return true;
        }
    }

    public boolean driveIMUUntilDistance(double pwr, double targetAngle, boolean forward, double targetMetersAway) {

        if (!forward) {
            moveMode = moveMode.backward;
            pwr = -pwr;
        } else
            moveMode = moveMode.forward;

        // if this statement is true, then the robot has not achieved its target
        // position
        if (Math.abs(targetMetersAway) > Math.abs(getDistForwardDist())) {
            // driveIMU(Kp, kiDrive, kdDrive, pwr, targetAngle);
            driveIMU(turnP, turnI, turnD, pwr, targetAngle, false);
            return false;
        } // destination achieved
        else {
            // stopAll();
            driveMixerDiffSteer(0, 0);
            driveIMUDistanceInitialzed = false;
            return true;
        }
        // long targetPos = (long)(targetMeters * forwardTPM);
        // if(Math.abs(targetPos) > Math.abs(getAverageTicks())){//we've not arrived yet
        // driveMixerDiffSteer(power,0);
        // return false;
        // }
        // else { //destination achieved
        // driveMixerDiffSteer(0,0);
        // return true;
        // }

    }

    int fieldPosState = 0;
    public boolean driveToFieldPosition(double targetX, double targetY, boolean forward, double maxPower, double closeEnoughDist){

        double heading = getBearingToWrapped(targetX, targetY);

        if(!forward){ //need to reverse heading when driving backwards
            heading = wrapAngle(heading,180);
        }

        switch (fieldPosState){
            case 0: //initially rotate to align with target location

                if(true) { //rotateIMU(heading,1)) {
                    fieldPosState++;
                }
                break;
            case 1:
                double distance = getDistanceTo(targetX, targetY);

                //this seems to be a way to slow down when close to a target location
                //todo: better PID pased position closing
                //double power =  distance < 0.1524 ? 0 : 0.5;
                //double maxPower = .5; // (as a magnituded)

                if(driveAbsoluteDistance(maxPower, heading, forward, distance, closeEnoughDist)) {
                    fieldPosState = 0;
                    return true;
                }
                break;
        }
        return false;
    }

    int fieldPosStateToo = 0;


    // drive with a final heading
        public boolean driveToFieldPosition(double targetX, double targetY, boolean forward, double maxPower, double closeEnoughDist, double targetFinalHeading){
        switch (fieldPosStateToo){
            case 0:
                if(driveToFieldPosition(targetX, targetY, forward, maxPower, closeEnoughDist)) {
                    fieldPosStateToo++;
                }
                break;
            case 1:
                if(rotateIMU(targetFinalHeading, 1)){
                    fieldPosStateToo = 0;
                    return true;
                }
        }
        return false;
    }

        int getFieldPosStateThree = 0;
        double launchMoveDist = 0.0; // the distance at which launcher movements should start
        //drive to a fully defined Position
    public boolean driveToFieldPosition(Constants.Position targetPose, boolean forward, double maxPower, double closeEnoughDist){
        switch (getFieldPosStateThree){
            case 0:
                //calc the distance remaining when the launcher movements should kick-in
                launchMoveDist = getDistanceTo(targetPose.x, targetPose.y);
                launchMoveDist-= launchMoveDist * targetPose.launchStart;

                getFieldPosStateThree++;

                break;
            case 1: //driving to target location
                if(getDistanceTo((Constants.ALLIANCE_INT_MOD) * targetPose.x, targetPose.y) <= launchMoveDist){
                    //kick in launcher adjustments if requested
                    if(targetPose.launchElevation > -.01) { //set elevation{
                        launcher.setElbowTargetAngle(targetPose.launchElevation);
                    }
                    if(targetPose.launchHeading > -.01) {
                        turret.setTurretAngle(targetPose.launchHeading);
                    }
                }

                if(targetPose.baseHeading > -.01) {
                    if (driveToFieldPosition((Constants.ALLIANCE_INT_MOD) * targetPose.x, targetPose.y, forward, maxPower, closeEnoughDist, targetPose.baseHeading)) {
                        getFieldPosStateThree++;
                    }
                }
                else{
                    if (driveToFieldPosition((Constants.ALLIANCE_INT_MOD) * targetPose.x, targetPose.y, forward, maxPower, closeEnoughDist)) {
                        getFieldPosStateThree++;
                    }
                }
                break;
            case 2: //end of travel
                // if we haven't kicked in launcher adjustments yet, now is the time to do it
                if (targetPose.launchElevation > -.01 && targetPose.launchStart > .99) { //set elevation{
                        launcher.setElbowTargetAngle(targetPose.launchElevation);
                    }

                if(targetPose.launchHeading > -.01 && targetPose.launchStart > .99) {
                    turret.setTurretAngle(targetPose.launchHeading);
                }
                    getFieldPosStateThree = 0;
                    return true;
                }
        return false;
    }

    private double getDistanceTo(double targetX, double targetY) {
        return Math.sqrt(Math.pow((targetX-getPoseX()),2) + Math.pow((targetY-getPoseY()),2));
    }

    private double[] getNewPosAway(double x) {
        double h = x / Math.asin(getHeading());
        double y = Math.sqrt(Math.pow(h,2) - Math.pow(x,2));
        double[] j = new double[2];
        j[0] = x;
        j[1] = y;
        return j;
    }

    public double getBearingTo(double targetX, double targetY) {
        return Math.toDegrees(Math.atan2((targetX-getPoseX()), (targetY-getPoseY())));
    }

    private double getBearingFromTo(double fromX, double fromY, double targetX, double targetY) {
        return Math.toDegrees(Math.atan2((targetX-fromX), (targetY-fromY)));
    }

    private double getBearingToWrapped(double targetX, double targetY) {
        return wrap360(Math.toDegrees(Math.atan2((targetX-getPoseX()), (targetY-getPoseY()))));
    }

    /**
     * a method written to test servos by plugging them into a designated servo
     * tester port on the REV module designed to work best with debounced gamepad
     * buttons
     *
     * @param largeUp   if true, increase PWM being sent to the servo tester by a
     *                  large amount
     * @param smallUp   if true, increase PWM being sent to the servo tester by a
     *                  small amount
     * @param smallDown if true, decrease PWM being sent to the servo tester by a
     *                  small amount
     * @param largeDown if true, decrease PWM being sent to the servo tester by a
     *                  large amount
     */
    public void servoTester(boolean largeUp, boolean smallUp, boolean smallDown, boolean largeDown) {
        // check to see if the PWM value being sent to the servo should be altered
        if (largeUp) {
            servoTesterPos += 100;
        }
        if (smallUp) {
            servoTesterPos += 25;
        }
        if (smallDown) {
            servoTesterPos -= 25;
        }
        if (largeDown) {
            servoTesterPos -= 100;
        }

        // send the PWM value to the servo regardless of if it is altered or not
    }

    // todo - All Articulations need to be rebuilt - most of these are from icarus
    // and will be removed
    //////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////
    //// ////
    //// Articulations ////
    //// ////
    //////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////

    double miniTimer;
    int miniState = 0;

    public boolean articulate(Articulation target, boolean setAndForget) {
        articulate(target);
        return true;
    }

    public boolean isRampedUp(){
        if(target == Constants.Target.NONE){
            return false;
        }

        if ((Math.abs(launcher.flywheelTargetTPS - launcher.flywheelTPS) / launcher.flywheelTargetTPS < Constants.rampedUpPercent)
                || (getTarget()== Constants.Target.NONE&& launcher.flywheelTPS<50)){ //you are also allowed to trigger (for settling rings) if you have no active target and your TPS is very low
            return true;
        }
        return false;
    }

    public int toggleTriggerState = 0;
    public long lastTriggerTime;

    public boolean toggleTriggerArticulation() {

        if (getTarget()== Constants.Target.NONE){

        }


//        if(isRampedUp()){
            switch (toggleTriggerState) {
                case 0:
                    if(target == Constants.Target.NONE) {
                        launcher.setTriggerTargetPos(Constants.LAUNCHER_TRIGGER_FLIP);
                    }
                    else{
                        launcher.setTriggerTargetPos(Constants.LAUNCHER_TRIGGER_SHOOT);
                    }
                    lastTriggerTime = System.currentTimeMillis();
                    toggleTriggerState++;
                    break;
                case 1:
                    if (System.currentTimeMillis() - lastTriggerTime > 500) {
                        launcher.setTriggerTargetPos(Constants.LAUNCHER_TRIGGER_BACK);
                        toggleTriggerState = 0;
                        return true;
                    }
                    break;
            }
//        }
        return false;
    }

    int ohBabyAStage = 0;
    double ohBabyATimer = 0;
    public boolean ohBabyATriple(){
        switch (ohBabyAStage){
            case 0:
                setTarget(Constants.Target.HIGH_GOAL);
                ohBabyATimer = System.nanoTime();
                ohBabyAStage++;
                break;
            case 1:
                if(turret.isTurretNearTarget()) {
                    if(shootRingAuton(Constants.Target.HIGH_GOAL, 3))
                        ohBabyAStage++;
                }
                break;
            case 2:
                setTarget(Constants.Target.NONE);
                ohBabyAStage = 0;
                return true;
        }
        return false;
    }

    int shootRingStage = 0;
    int ringsShot = 0;
    double shootTime = 0;

    public boolean shootRingAuton(Constants.Target newTarget, int numShots){
        switch(shootRingStage){
            case 0:
                setTarget(newTarget);
                flywheelIsActive = true;
                //wait until we are on-target
                if (turret.isTurretNearTarget()) {
                    shootRingStage++;
                    shootTime = System.nanoTime();
                }
                break;
            case 1:
                if(toggleTriggerArticulation()){
                    ringsShot++;
                    shootRingStage++;
                    shootTime = System.nanoTime();
                }
                break;
            case 2:
                if(ringsShot == numShots){
                    shootRingStage++;
                }
                else if(System.nanoTime() - shootTime > 0.3 * 1E9){
                    shootRingStage--;//ben was here
                }
                break;
            case 3:
                shootRingStage = 0;
                setTarget(Constants.Target.NONE);
                flywheelIsActive = false;
                ringsShot = 0;
                return true;
        }
        return false;
    }

    boolean isNavigating = false;
    boolean autonTurnInitialized = false;
    double autonTurnTarget = 90;

    public boolean cardinalBaseTurn(boolean isRightTurn) {
//        if (!autonTurnInitialized) {
//            autonTurnTarget = nextCardinal(getHeading(), isRightTurn, 10);
//            autonTurnInitialized = true;
//            isNavigating = true;
//        }
//
//        if (isNavigating == false)
//            return true; // abort if drivers override

        if (rotateIMU(autonTurnTarget, 5.0) ) {
//            isNavigating = false;
//            autonTurnInitialized = false;
            autonTurnTarget = wrap360(isRightTurn ? autonTurnTarget + 90 : autonTurnTarget - 90);
            return true;
        }
        return false;
    }

    int returnHomeState = 0;
    public boolean returnHome(){
        switch(returnHomeState){
            case 0:
                if(driveToFieldPosition(Constants.Position.HOME,false,  .3,.08)){
                    returnHomeState++;
                }
                break;
            case 1:
                returnHomeState = 0;
                return true;
        }
        return false;
    }

    int dumpWobbleGoalState = 0;
    double wobbleGoalDumpTimer = 0.0;
    public boolean dumpWobbleGoal(){

        switch (dumpWobbleGoalState) {
            case 0:
                launcher.setElbowTargetAngle(45);
                launcher.wobbleGrip();
                wobbleGoalDumpTimer = System.nanoTime();
                dumpWobbleGoalState++;
                break;
            case 1:
                if(System.nanoTime() - wobbleGoalDumpTimer > 1 * 1E9) {
                    if (driveToFieldPosition(Constants.Position.WOBBLE_GOAL_DUMP, false, .8, .1)) {
                        launcher.wobbleRelease();
                        wobbleGoalDumpTimer = System.nanoTime();
                        dumpWobbleGoalState++;
                    }
                }
                break;
            case 2:
                if (System.nanoTime() - wobbleGoalDumpTimer > .7 * 1E9) {
                    dumpWobbleGoalState = 0;
                    exitWobbleGoalMode();
                    return true;
                }
        }
        return false;
    }

    boolean gripperModeIsInReverse = false;
    int enterWobbleGoalModeState = 0;
    double enterWobbleGoalModeTimer = 0.0;
    public boolean enterWobbleGoalMode() {
        switch (enterWobbleGoalModeState) {
            case 0:
                gripperModeIsInReverse = true;
                setTarget(Constants.Target.NONE);
                turret.setCurrentMode(Turret.TurretMode.chassisRelative);
                launcher.setElbowTargetAngle(25);
                turret.setTurretAngle(180 + Constants.GRIPPER_HEADING_OFFSET);
                enterWobbleGoalModeTimer = System.nanoTime();
                enterWobbleGoalModeState++;
                break;
            case 1:
                if(System.nanoTime() - enterWobbleGoalModeTimer > 2 * 1E9) {
                    turret.setDangerModeActive(true);
                    launcher.gripperExtend();
                    launcher.gripperOpenWide();
                    Constants.IN_WOBBLE_MODE = true;
                    enterWobbleGoalModeTimer = System.nanoTime();
                    enterWobbleGoalModeState++;
                }
                break;

            case 2:
                if(System.nanoTime() - enterWobbleGoalModeTimer > .5 * 1E9) {
                    launcher.setElbowTargetAngle(0);
                    enterWobbleGoalModeTimer = System.nanoTime();
                    enterWobbleGoalModeState++;
                }
                break;
            case 3:
                if(System.nanoTime() - enterWobbleGoalModeTimer > 1 * 1E9) {
                    enterWobbleGoalModeState = 0;
                    return true;
                }
                break;
        }
        return false;
    }

    int exitWobbleGoalState = 0;
    double exitWobbleGoalTimer = 0.0;
    public boolean exitWobbleGoalMode(){
        switch (exitWobbleGoalState) {
            case 0:
                launcher.wobbleGrip();
                launcher.gripperRetract();
                exitWobbleGoalTimer = System.nanoTime();
                exitWobbleGoalState++;
                break;
            case 1:
                if(System.nanoTime() - enterWobbleGoalModeTimer > 1.3 * 1E9){
                    gripperModeIsInReverse = false;
                    turret.setDangerModeActive(false);  //todo this is dangerous and not needed
                    Constants.IN_WOBBLE_MODE = false;
                    exitWobbleGoalState = 0;
                    return true;
                }
        }
        return false;
    }

    int deployWobbleGoalAutonState = 0;
    double deployWobbleGoalAutonTimer = 0.0;
    public boolean deployWobbleGoalGripperAuton(){
        switch (deployWobbleGoalAutonState){
            case 0:
                turret.setDangerModeActive(true);
                launcher.setElbowTargetAngle(0);
                launcher.wobbleRelease();
                deployWobbleGoalAutonTimer = System.nanoTime();
                deployWobbleGoalAutonState++;
                break;
            case 1:
                if(System.nanoTime() - deployWobbleGoalAutonTimer > .5 * 1E9) {
                    launcher.gripperExtend();
                    deployWobbleGoalAutonState++;
                    deployWobbleGoalAutonTimer = System.nanoTime();
                }
                break;
            case 2:
                if(System.nanoTime() - deployWobbleGoalAutonTimer > .5 * 1E9) {
                    deployWobbleGoalAutonState = 0;
                    return true;
                }
                break;
        }
        return false;
    }

    int releaseWobbleGoalAutonState = 0;
    double releaseWobbleGoalAutonTimer = 0.0;
    public boolean releaseWobbleGoalAuton(boolean withRetract){
        switch (releaseWobbleGoalAutonState){
            case 0: //release and set down
                launcher.wobbleRelease();
                launcher.setElbowTargetAngle(0);
                releaseWobbleGoalAutonTimer = System.nanoTime();
                releaseWobbleGoalAutonState++;

            case 1: //for safety, raise for retract
                if(System.nanoTime() - releaseWobbleGoalAutonTimer > .5 * 1E9) {
                    launcher.setElbowTargetAngle(30);
                    releaseWobbleGoalAutonTimer = System.nanoTime();
                    releaseWobbleGoalAutonState++;
                    }
                break;

            case 2://close gripper prior to retract
                if(System.nanoTime() - releaseWobbleGoalAutonTimer > .5 * 1E9) {
                    if (withRetract) launcher.wobbleGrip();
                    releaseWobbleGoalAutonTimer = System.nanoTime();
                    releaseWobbleGoalAutonState++;
                }
                break;

            case 3: //retract gripper or raise elbow
                if(System.nanoTime() - releaseWobbleGoalAutonTimer > .5 * 1E9) {
                    if (withRetract)
                        launcher.gripperRetract();
                    else {
                        releaseWobbleGoalAutonState = 0;
                        return true;
                    }

                    releaseWobbleGoalAutonTimer = System.nanoTime();
                    releaseWobbleGoalAutonState++;
                }
                break;

            case 4: //give it time to complete raising or retracting
                if(System.nanoTime() - releaseWobbleGoalAutonTimer > .5 * 1E9) {
                    releaseWobbleGoalAutonState = 0;
                    return true;
                }
                break;
        }
        return false;
    }

    public Articulation articulate(Articulation target) {
        articulation = target; // store the most recent explict articulation request as our target, allows us
                               // to keep calling incomplete multi-step transitions
        if (target == Articulation.manual) {
            miniState = 0; // reset ministate - it should only be used in the context of a multi-step
                           // transition, so safe to reset it here
        }

        //articulatuion backbone
        switch (articulation) {
            case manual:
                break; // do nothing here - likely we are directly overriding articulations in game
            case toggleTrigger:
                if(toggleTriggerArticulation())
                        articulation = Articulation.manual;
                break;
            case cardinalBaseLeft:
                if (cardinalBaseTurn(false)) {
                    articulation = PoseUG.Articulation.manual;
                }
                break;
            case cardinalBaseRight:
                if (cardinalBaseTurn(true)) {
                    articulation = PoseUG.Articulation.manual;
                }
                break;
            case returnHome:
                if (returnHome()) {
                    articulation = PoseUG.Articulation.manual;
                }
                break;
            case dumpWobbleGoal:
                if(dumpWobbleGoal()){
                    articulation = PoseUG.Articulation.manual;
                }
                break;
            case enterWobbleGoalMode:
                if(enterWobbleGoalMode()){
                    articulation = PoseUG.Articulation.manual;
                }
                break;
            case exitWobbleGoalMode:
                if(exitWobbleGoalMode()){
                    articulation = PoseUG.Articulation.manual;
                }
                break;
            case deployWobbleGoalAuton:
                if(exitWobbleGoalMode()){
                    articulation = PoseUG.Articulation.manual;
                }
                break;
            case releaseWobbleGoalAuton:
                if(exitWobbleGoalMode()){
                    articulation = PoseUG.Articulation.manual;
                }
                break;
            case ohBabyATriple:
                if(ohBabyATriple()){
                    articulation = PoseUG.Articulation.manual;
                }
                break;
            default:
                return target;

        }
        return target;
    }

    //////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////
    ////                                                                                  ////
    ////                                Platform Movements                                ////
    ////                                                                                  ////
    //////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////

    /**
     *
     * @param forward
     * @param targetMeters
     * @param power
     * @return
     */
    public boolean driveForward(boolean forward, double targetMeters, double power) {
        if (!forward) {
            moveMode = moveMode.backward;
            targetMeters = -targetMeters;
            power = -power;
        } else
            moveMode = moveMode.forward;

        long targetPos = (long) (targetMeters * forwardTPM);
        if (Math.abs(targetPos) > Math.abs(getAverageTicks())) {// we've not arrived yet
            driveMixerDiffSteer(power, 0);
            return false;
        } else { // destination achieved
            driveMixerDiffSteer(0, 0);
            return true;
        }
    }

    /**
     * Stops all motors on the robot
     */
    public void stopAll() {
        launcher.stopAll();
        turret.stopAll();
        driveMixerMec(0, 0, 0);
    }

    public void stopChassis() {
        driveMixerDiffSteer(0, 0);
    }

    //////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////
    ////                                                                                  ////
    ////                           Drive Platform Mixing Methods                          ////
    ////                                                                                  ////
    //////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////

    /**
     * drive method for a mecanum drive
     * 
     * @param forward sets how much power will be provided in the forwards direction
     * @param strafe  sets how much power will be provided in the left strafe
     *                direction
     * @param rotate  sets how much power will be provided to clockwise rotation
     */
    public void driveMixerMec(double forward, double strafe, double rotate) {

        // reset the power of all motors
        powerBackRight = 0;
        powerFrontRight = 0;
        powerBackLeft = 0;
        powerFrontLeft = 0;

        // set power in the forward direction
        powerFrontLeft = forward;
        powerBackLeft = forward;
        powerFrontRight = forward;
        powerBackRight = forward;

        // set power in the left strafe direction
        powerFrontLeft += -strafe;
        powerFrontRight += strafe;
        powerBackLeft += strafe;
        powerBackRight += -strafe;

        // set power in the clockwise rotational direction
        powerFrontLeft += rotate;
        powerBackLeft += rotate;
        powerFrontRight += -rotate;
        powerBackRight += -rotate;
    }

    public void driveMixerTank(double forward, double rotate) {

        // reset the power of all motors
        powerRight = 0;
        powerLeft = 0;

        // set power in the forward direction
        powerLeft = forward;
        powerRight = forward;

        // set power in the clockwise rotational direction
        powerLeft += rotate;
        powerRight += -rotate;
        // provide power to the motors
        motorBackLeft.setPower(clampMotor(powerBackLeft));
        motorBackRight.setPower(clampMotor(powerBackRight));

    }

    public static void normalize(double[] motorspeeds) {
        double max = Math.abs(motorspeeds[0]);
        for (int i = 0; i < motorspeeds.length; i++) {
            double temp = Math.abs(motorspeeds[i]);
            if (max < temp) {
                max = temp;
            }
        }
        if (max > 1) {
            for (int i = 0; i < motorspeeds.length; i++) {
                motorspeeds[i] = motorspeeds[i] / max;
            }
        }
    }

    /**
     * drive method for a four motor differential drive based on speed and steering
     * input
     * 
     * @param forward sets how much power will be provided in the forwards direction
     * @param rotate  sets how much power will be provided to clockwise rotation
     */
    public void driveMixerDiffSteer(double forward, double rotate) {

        driveMixerMec(forward, 0, rotate);
    }

    /**
     * Reset the encoder readings on all drive motors
     * 
     * @param enableEncoders if true, the motors will continue to have encoders
     *                       active after reset
     */
    public void resetMotors(boolean enableEncoders) {
        // motorFrontLeft.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        motorBackLeft.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        // motorFrontRight.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        motorBackRight.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        if (enableEncoders) {
            // motorFrontLeft.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            motorBackLeft.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            // motorFrontRight.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            motorBackRight.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        } else {
            // motorFrontLeft.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            motorBackLeft.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            // motorFrontRight.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            motorBackRight.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        }

    }

    //////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////
    ////                                                                                  ////
    ////                   Drive Platform Differential Mixing Methods                     ////
    ////                                                                                  ////
    //////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////

    public double clampMotor(double power) {
        return clampDouble(-1, 1, power);
    }

    /**
     * clamp a double value to put it within a given range
     * 
     * @param min   lower bound of the range
     * @param max   upper bound of the range
     * @param value double being clamped to the given range
     */
    public double clampDouble(double min, double max, double value) {
        double result = value;
        if (value > max)
            result = max;
        if (value < min)
            result = min;
        return result;
    }

    // todo - these getAverageTicks are suspicious - it's not the best way to
    // approximate travel - prefer to use a separate position sensor than the wheels
    // - particularly with mecanums

    /**
     * retrieve the average value of ticks on all motors - differential
     */
    public long getAverageTicks() {
        long averageTicks = (motorBackLeft.getCurrentPosition() + motorBackRight.getCurrentPosition()) / 2;
        return averageTicks;
    }

    /**
     * retrieve the average of the absolute value of ticks on all motors - mecanum
     */
    public long getAverageAbsTicks() {
        long averageTicks = (Math.abs(motorFrontLeft.getCurrentPosition())
                + Math.abs(motorBackLeft.getCurrentPosition()) + Math.abs(motorFrontRight.getCurrentPosition())
                + Math.abs(motorBackRight.getCurrentPosition())) / 4;
        return averageTicks;
    }

    public int getLeftMotorTicks() {
        return motorBackLeft.getCurrentPosition();
    }

    public int getRightMotorTicks() {
        return motorBackRight.getCurrentPosition();
    }

    /**
     * Moves the tank platform under PID control applied to the rotation of the
     * robot. This version can either drive forwards/backwards or strafe.
     *
     * @param Kp           proportional constant multiplier
     * @param Ki           integral constant multiplier
     * @param Kd           derivative constant multiplier
     * @param pwr          base motor power before correction is applied
     * @param currentAngle current angle of the robot in the coordinate system of
     *                     the sensor that provides it- should be updated every
     *                     cycle
     * @param targetAngle  the target angle of the robot in the coordinate system of
     *                     the sensor that provides the current angle
     */
    // this version is for differential steer robots
    public void movePID(double Kp, double Ki, double Kd, double pwr, double currentAngle, double targetAngle) {

        movePIDMixer(Kp, Ki, Kd, pwr, 0, currentAngle, targetAngle);
    }

    /**
     * Moves the mecanum platform under PID control applied to the rotation of the
     * robot. This version can either drive forwards/backwards or strafe.
     *
     * @param Kp           proportional constant multiplier
     * @param Ki           integral constant multiplier
     * @param Kd           derivative constant multiplier
     * @param pwr          base motor power before correction is applied
     * @param currentAngle current angle of the robot in the coordinate system of
     *                     the sensor that provides it- should be updated every
     *                     cycle
     * @param targetAngle  the target angle of the robot in the coordinate system of
     *                     the sensor that provides the current angle
     * @param strafe       if true, the robot will drive left/right. if false, the
     *                     robot will drive forwards/backwards.
     */
    public void movePID(double Kp, double Ki, double Kd, double pwr, double currentAngle, double targetAngle,
            boolean strafe) {

        if (strafe)
            movePIDMixer(Kp, Ki, Kd, 0, pwr, currentAngle, targetAngle);
        else
            movePIDMixer(Kp, Ki, Kd, pwr, 0, currentAngle, targetAngle);

    }

    /**
     *
     * Moves the chassis under PID control applied to the
     * heading and speed of the robot with support for mecanum drives.
     * This version can drive forwards/backwards and strafe
     * simultaneously. If called with strafe set to zero, this can be used for normal
     * 4 motor or 2 motor differential drive robots
     *
     * @param maxPwrFwd       base forwards/backwards motor power before correction is
     *                     applied
     * @param dist         the distance in meters still to go - this is effectively the distance error
     * @param pwrStf       base left/right motor power before correction is applied
     * @param currentAngle current angle of the robot in the coordinate system of
     *                     the sensor that provides it- should be updated every
     *                     cycle
     * @param targetAngle  the target angle of the robot in the coordinate system of
     *                     the sensor that provides the current angle
     */
    public void movePIDMixer(double maxPwrFwd,  boolean forward, double dist, double pwrStf, double currentAngle,
                             double targetAngle) {

        // setup turnPID
        turnPID.setOutputRange(-.5, .5);
        turnPID.setIntegralCutIn(cutout);
        turnPID.setPID(driveP, turnI, turnD);
        turnPID.setSetpoint(targetAngle);
        turnPID.setInputRange(0, 360);
        turnPID.setContinuous();
        turnPID.setInput(currentAngle);
        turnPID.enable();

        // setup distPID
        distPID.setOutputRange(-maxPwrFwd, maxPwrFwd);
        distPID.setIntegralCutIn(cutout); //todo - cutout was for turning and probably needs to be changed for distance if integral is needed
        distPID.setPID(distP, distI, distD);
        distPID.setSetpoint(dist); //trying to get to a zero distance
        distPID.setInput(0);
        distPID.enable();

        // calculate the angular correction to apply
        double turnCorrection = turnPID.performPID();
        // calculate chassis power
        double basePwr = distPID.performPID();
        if (!forward) basePwr *=-1;

        // performs the drive with the correction applied
        driveMixerMec(basePwr, pwrStf, turnCorrection);
    }


    /**
     * OLD METHOD - only applies  PID to turns, not base power
     * Moves the omnidirectional (mecanum) platform under PID control applied to the
     * rotation of the robot. This version can drive forwards/backwards and strafe
     * simultaneously. If called with strafe set to zero, this will work for normal
     * 4 motor differential robots
     *
     * @param Kp           proportional constant multiplier
     * @param Ki           integral constant multiplier
     * @param Kd           derivative constant multiplier
     * @param pwrFwd       base forwards/backwards motor power before correction is
     *                     applied
     * @param pwrStf       base left/right motor power before correction is applied
     * @param currentAngle current angle of the robot in the coordinate system of
     *                     the sensor that provides it- should be updated every
     *                     cycle
     * @param targetAngle  the target angle of the robot in the coordinate system of
     *                     the sensor that provides the current angle
     */

    double turnError = 0;

    public void movePIDMixer(double Kp, double Ki, double Kd, double pwrFwd, double pwrStf, double currentAngle,
            double targetAngle) {
        // if (pwr>0) PID.setOutputRange(pwr-(1-pwr),1-pwr);
        // else PID.setOutputRange(pwr - (-1 - pwr),-1-pwr);

        // initialization of the PID calculator's output range, target value and
        // multipliers
        turnPID.setOutputRange(-.3, .3);
        turnPID.setIntegralCutIn(cutout);
        turnPID.setPID(Kp, Ki, Kd);
        turnPID.setSetpoint(targetAngle);
        turnPID.enable();

        // initialization of the PID calculator's input range and current value
        turnPID.setInputRange(0, 360);
        turnPID.setContinuous();
        turnPID.setInput(currentAngle);


        // calculates the angular correction to apply
        double turnCorrection = turnPID.performPID();
        turnError = wrapAngleMinus(targetAngle, currentAngle);

        // performs the drive with the correction applied
        driveMixerMec(pwrFwd, pwrStf, turnCorrection);

        // logging section that can be reactivated for debugging
        /*
         * ArrayList toUpdate = new ArrayList(); toUpdate.add((PID.m_deltaTime));
         * toUpdate.add(Double.valueOf(PID.m_error)); toUpdate.add(new
         * Double(PID.m_totalError)); toUpdate.add(new Double(PID.pwrP));
         * toUpdate.add(new Double(PID.pwrI)); toUpdate.add(new Double(PID.pwrD));
         */
        /*
         * logger.UpdateLog(Long.toString(System.nanoTime()) + "," +
         * Double.toString(PID.m_deltaTime) + "," + Double.toString(pose.getOdometer())
         * + "," + Double.toString(PID.m_error) + "," +
         * Double.toString(PID.m_totalError) + "," + Double.toString(PID.pwrP) + "," +
         * Double.toString(PID.pwrI) + "," + Double.toString(PID.pwrD) + "," +
         * Double.toString(correction)); motorLeft.setPower(pwr + correction);
         * motorRight.setPower(pwr - correction);
         */
    }

    public void movegenericPIDMixer(double Kp, double Ki, double Kd, double pwrFwd, double pwrStf, double currentVal, double targetVal) {
        // if (pwr>0) PID.setOutputRange(pwr-(1-pwr),1-pwr);
        // else PID.setOutputRange(pwr - (-1 - pwr),-1-pwr);

        // initialization of the PID calculator's output range, target value and
        // multipliers
        turnPID.setOutputRange(-.5, .5);
        turnPID.setIntegralCutIn(cutout);
        turnPID.setPID(Kp, Ki, Kd);
        turnPID.setSetpoint(targetVal);
        turnPID.enable();

        // initialization of the PID calculator's input range and current value
        turnPID.setContinuous();
        turnPID.setInput(currentVal);

        // calculates the angular correction to apply
        double correction = turnPID.performPID();

        // performs the drive with the correction applied
        driveMixerMec(pwrFwd, pwrStf, correction);

        // logging section that can be reactivated for debugging
        /*
         * ArrayList toUpdate = new ArrayList(); toUpdate.add((PID.m_deltaTime));
         * toUpdate.add(Double.valueOf(PID.m_error)); toUpdate.add(new
         * Double(PID.m_totalError)); toUpdate.add(new Double(PID.pwrP));
         * toUpdate.add(new Double(PID.pwrI)); toUpdate.add(new Double(PID.pwrD));
         */
        /*
         * logger.UpdateLog(Long.toString(System.nanoTime()) + "," +
         * Double.toString(PID.m_deltaTime) + "," + Double.toString(pose.getOdometer())
         * + "," + Double.toString(PID.m_error) + "," +
         * Double.toString(PID.m_totalError) + "," + Double.toString(PID.pwrP) + "," +
         * Double.toString(PID.pwrI) + "," + Double.toString(PID.pwrD) + "," +
         * Double.toString(correction)); motorLeft.setPower(pwr + correction);
         * motorRight.setPower(pwr - correction);
         */
    }

    public void driveDriftCorrect(double driftance, double origialDist, double pwr, boolean forward) {
        double correctionAngle;
        if (forward)
            correctionAngle = Math.acos(origialDist / driftance);
        else
            correctionAngle = 360.0 - Math.acos(origialDist / driftance);
        driveIMUDistance(pwr, correctionAngle, forward, Math.sqrt(Math.pow(origialDist, 2) + Math.pow(driftance, 2)));
    }

    /**
     * Drive forwards for a set power while maintaining an IMU heading using PID
     * 
     * @param Kp          proportional multiplier for PID
     * @param Ki          integral multiplier for PID
     * @param Kd          derivative proportional for PID
     * @param pwr         set the forward power
     * @param targetAngle the heading the robot will try to maintain while driving
     */
    // this version is for omnidirectional robots
    public void driveIMU(double Kp, double Ki, double Kd, double pwr, double targetAngle, boolean strafe) {
        movePID(Kp, Ki, Kd, pwr, poseHeading, targetAngle, strafe);
    }

    /**
     * Drive with a set power for a set distance while maintaining an IMU heading
     * using PID
     * 
     * @param Kp            proportional multiplier for PID
     * @param pwr           set the forward power
     * @param targetAngle   the heading the robot will try to maintain while driving
     * @param forwardOrLeft is the robot driving in the forwards/left (positive)
     *                      directions or backwards/right (negative) directions
     * @param targetMeters  the target distance (in meters)
     * @param strafe        tells if the robot driving forwards/backwards or
     *                      left/right
     */
    public boolean driveIMUDistance(double Kp, double pwr, double targetAngle, boolean forwardOrLeft,
            double targetMeters, boolean strafe) {

        // set what direction the robot is supposed to be moving in for the purpose of
        // the field position calculator
        if (!forwardOrLeft) {
            moveMode = moveMode.backward;
            targetMeters = -targetMeters;
            pwr = -pwr;
        } else
            moveMode = moveMode.forward;

        // calculates the target position of the drive motors
        long targetPos;
        if (strafe)
            targetPos = (long) targetMeters * strafeTPM;
        else
            targetPos = (long) (targetMeters * forwardTPM);

        // if this statement is true, then the robot has not achieved its target
        // position
        if (Math.abs(targetPos) < Math.abs(getAverageAbsTicks())) {
            driveIMU(Kp, turnI, turnD, pwr, targetAngle, strafe);
            return false;
        }

        // destination achieved
        else {
            driveMixerMec(0, 0, 0);
            return true;
        }
    }

    /**
     * Rotate to a specific heading with a time cutoff in case the robot gets stuck
     * and cant complete the turn otherwise
     * 
     * @param targetAngle the heading the robot will attempt to turn to
     * @param maxTime     the maximum amount of time allowed to pass before the
     *                    sequence ends
     */
    public boolean rotateIMU(double targetAngle, double maxTime) {
        if (!turnTimerInit) { // intiate the timer that the robot will use to cut of the sequence if it takes
                              // too long; only happens on the first cycle
            turnTimer = System.nanoTime() + (long) (maxTime * (long) 1e9);
            turnTimerInit = true;
        }
        driveIMU(turnP, turnI, turnD, 0, targetAngle, false); // check to see if the robot turns within a
                                                                    // threshold of the target
         if(Math.abs(getHeading() - targetAngle) < minTurnError && Math.abs(rotVelBase) < 20) {
         turnTimerInit = false;
         driveMixerMec(0,0,0);
         return true;
         }

        if (turnTimer < System.nanoTime()) { // check to see if the robot takes too long to turn within a threshold of
                                             // the target (e.g. it gets stuck)
            turnTimerInit = false;
            driveMixerMec(0, 0, 0);
            return true;
        }
        return false;
    }


    public static double getPoseX() { //todo-get rid of once done
        return poseX;
    }

    public static double getPoseY() {
        return poseY;
    }

    public static double getDisplacement() {
        return displacement;
    }


    /**
     * the maintain heading function used in demo: holds the heading read on initial
     * button press
     * 
     * @param buttonState the active state of the button; if true, hold the current
     *                    position. if false, do nothing
     */
    public void maintainHeading(boolean buttonState) {

        // if the button is currently down, maintain the set heading
        if (buttonState) {
            // if this is the first time the button has been down, then save the heading
            // that the robot will hold at and set a variable to tell that the heading has
            // been saved
            if (!maintainHeadingInit) {
                poseSavedHeading = poseHeading;
                maintainHeadingInit = true;
            }
            // hold the saved heading with PID
            driveIMU(turnP, turnI, turnD, 0, poseSavedHeading, false);
        }

        // if the button is not down, set to make sure the correct heading will be saved
        // on the next button press
        if (!buttonState) {
            maintainHeadingInit = false;
        }
    }

    boolean intakeStartedByMonitor = false;
    void TentRingevatorMonitor(){
    if (intake.isTented()){
            if(Math.abs(motorBackLeft.getPower())>.1 || Math.abs(motorBackRight.getPower())>.1) {
                intake.setIntakeSpeed(1);
                intakeStartedByMonitor=true; //yup, this is a very weak way to do it
            }
            else{
                    intake.setIntakeSpeed(0);
                    intakeStartedByMonitor=false;
                }
        }
    }


    //auto intake when we are tented and ring crosses the right distance sensor
    long tentTakeTime = 0;
    boolean tentTakeInit = false;
    public void TentTake() {
        //try to adjust foward or backward for edge case rings
        //todo again, rushed - really need to set the calculated distance to move forward or back
        if (intake.isTented() && intake.isRollingRingMode()) {
            if(getDistRightDist()<INTAKE_ROLLING_RING_NEAR)
                //INTAKE_ROLLING_JOG_BW_NOW=true;
            if(between(getDistRightDist(),INTAKE_ROLLING_RING_FAR,INTAKE_ROLLING_RING_TOO_FAR))
            {
                //INTAKE_ROLLING_JOG_FW_NOW = true;
                //intake.setIntakeSpeed(1); //todo: this should really be implemented as a monitor to turn on the intake motor if the robot is moving and tented is true
            }

            if (between(getDistRightDist(), INTAKE_ROLLING_RING_NEAR, INTAKE_ROLLING_RING_FAR)) {
                if (!tentTakeInit) {
                    tentTakeInit = true;
                    tentTakeTime = futureTime(INTAKE_ROLLING_RING_DELAY);
                }
                //don't interrupt other articulations
                //if (getArticulation() == Articulation.manual || getArticulation() == Articulation.toggleTrigger)
                if (System.nanoTime() > tentTakeTime) {
                    intake.Do(Intake.Behavior.INTAKE);
                    tentTakeInit = false;
                }
            }
        }
    }

    //jog the robot backward briefly
    boolean miniJogInit = false;
    double miniJogTime = 0;
    public void MiniJog(){
        if (INTAKE_MINIJOG_NOW)
        {
            if (!miniJogInit) {
                miniJogTime = futureTime(INTAKE_MINIJOG_TIME);
                miniJogInit = true;
            }

            if (miniJogTime > System.nanoTime()){
                //set power on drive motors backward
                motorBackLeft.setPower(-INTAKE_MINIJOG_POWER);
                motorBackRight.setPower(-INTAKE_MINIJOG_POWER);

            }
            else {
                miniJogInit = false;
                motorBackLeft.setPower(0);
                motorBackRight.setPower(0);
                INTAKE_MINIJOG_NOW=false;
            }

        }

    }

    //jog the robot forward briefly - horrid way to duplicate code, but in a hurry
    boolean rollingJogFwInit = false;
    double rollingJogFwTime = 0;
    public void RollingJogForward(){
        if (INTAKE_ROLLING_JOG_FW_NOW)
        {
            if (!rollingJogFwInit) {
                rollingJogFwTime = futureTime(INTAKE_ROLLING_JOG_FW_TIME);
                rollingJogFwInit = true;
            }

            if (rollingJogFwTime > System.nanoTime()){
                //set power on drive motors backward
                motorBackLeft.setPower(INTAKE_ROLLING_JOG_FW_POWER);
                motorBackRight.setPower(INTAKE_ROLLING_JOG_FW_POWER);

            }
            else {
                rollingJogFwInit = false;
                motorBackLeft.setPower(0);
                motorBackRight.setPower(0);
                INTAKE_ROLLING_JOG_FW_NOW=false;
            }

        }

    }
    //jog the robot forward briefly - screaming horrid way to duplicate code, but in a hurry
    boolean rollingJogBwInit = false;
    double rollingJogBwTime = 0;
    public void RollingJogBackward(){
        if (INTAKE_ROLLING_JOG_BW_NOW)
        {
            if (!rollingJogBwInit) {
                rollingJogBwTime = futureTime(INTAKE_ROLLING_JOG_FW_TIME);
                rollingJogBwInit = true;
            }

            if (rollingJogBwTime > System.nanoTime()){
                //set power on drive motors backward
                motorBackLeft.setPower(INTAKE_ROLLING_JOG_BW_POWER);
                motorBackRight.setPower(INTAKE_ROLLING_JOG_BW_POWER);

            }
            else {
                rollingJogBwInit = false;
                motorBackLeft.setPower(0);
                motorBackRight.setPower(0);
                INTAKE_ROLLING_JOG_BW_NOW=false;
            }

        }

    }


    /**
     * pivotTurn is a simple low level method to turn the robot from an arbitrary
     * point on the virtual axle of a diffsteer platform call this method until it
     * returns true this must only be called serially until completion - if aborted
     * early it will be in a falsely initialized state
     * 
     * @param speed     maximum normalized (-1 to +1) speed to achieve on outermost
     *                  wheel. negative speed means anticlockwise turn
     * @param angle     degrees of turn to achieve - this is relative to the initial
     *                  heading as measured during the init stage. negative means
     *                  measured anticlockwise - this does not set the direction of
     *                  motion, but only when it ends
     * @param offset    center of turn as offset in meters from center of wheelbase.
     *                  negative offset means to the left of the normal centerpoint
     * @param timelimit - maximum time alloted to achieve the turn - if hit, the
     *                  turn probably did not complete
     * @return - false until it has completed the turn
     */

    boolean pivotTurnInitialized = false;

    public boolean pivotTurnIMU(double speed, double angle, double offset, float timelimit) {

        final double wheelbase = 0.335; // tombot meters separation between wheels
        double startangle, finalangle = 0;
        double radiusright, radiusleft;
        double arcleft, arcright = 0;
        double speedleft, speedright;

        if (!pivotTurnInitialized) {
            startangle = getHeading();
            timelimit = futureTime(timelimit);
            radiusleft = offset + wheelbase / 2;
            radiusright = offset - wheelbase / 2;
            arcleft = 2 * Math.PI * radiusleft * angle / 360;
            arcright = 2 * Math.PI * radiusright * angle / 360;

            // the longest arc will be set to the speed and the shorter arc will be the
            // relative fraction of that speed
            // the goal is to get both motors to arrive at their destination position at the
            // same time with constant speed
            if (Math.abs(arcleft) >= Math.abs(arcright)) {
                speedleft = speed;
                speedright = speed * arcright / arcleft;
            } else {
                speedright = speed;
                speedleft = speed * arcleft / arcright;
            }

            // set drive motors to run to position mode
            motorBackRight.setMode(DcMotor.RunMode.RUN_TO_POSITION);
            motorBackLeft.setMode(DcMotor.RunMode.RUN_TO_POSITION);

            motorBackRight.setTargetPosition((int) arcright * rightTPM);
            motorBackLeft.setTargetPosition((int) arcleft * leftTPM);

            // start moving
            motorBackRight.setPower(speedleft);
            motorBackLeft.setPower((speedright));

            pivotTurnInitialized = true;

        }

        if ((timelimit < futureTime(0)) && ((motorBackLeft.isBusy() || motorBackRight.isBusy())))
            return false;
        else {
            resetMotors(true);
            return true;
        }
    }

    /**
     * a method written to test servos by plugging them into a designated servo
     * tester port on the REV module designed to work best with debounced gamepad
     * buttons
     * 
     * @param largeUp   if true, increase PWM being sent to the servo tester by a
     *                  large amount
     * @param smallUp   if true, increase PWM being sent to the servo tester by a
     *                  small amount
     * @param smallDown if true, decrease PWM being sent to the servo tester by a
     *                  small amount
     * @param largeDown if true, decrease PWM being sent to the servo tester by a
     *                  large amount
     */

    /**
     * clamp a double to match the power range of a motor
     * 
     * @param power the double value being clamped to motor power range
     */

    /**
     * assign the current heading of the robot to zero
     */
    public void setZeroHeading() {
        setHeading(0);
        turret.setHeading(0);
    }

    /**
     * assign the current heading of the robot to alliance setup values
     */
    public void setHeadingAlliance() {
        if(isBlue){
            setHeading(90);
            turret.setHeading(90);
        }
        else
        {
            setHeading(270);
            turret.setHeading(270);
        }
    }

    public void setHeadingBase(double offset) {
        setHeading(360 - offset);
    }

    /**
     * assign the current heading of the robot to 45 (robot on field perimeter wall)
     */
    public void setWallHeading() {
        setHeading(45);
    }

    /**
     * assign the current heading of the robot to a specific angle
     * 
     * @param angle the value that the current heading will be assigned to
     */
    public void setHeading(double angle) {
        poseHeading = angle;
        initialized = false; // triggers recalc of heading offset at next IMU update cycle
    }

    public void resetTPM() {
        forwardTPM = 2493;
    }

    /**
     * sets autonomous imu offset for turns
     */
    public void setAutonomousIMUOffset(double offset) {
        autonomousIMUOffset = offset;
    }

    /**
     * Set the current position of the robot in the X direction on the field
     * 
     * @param poseX
     */
    public void setPoseX(double poseX) {
        this.poseX = poseX;
    }

    /**
     * Set the current position of the robot in the Y direction on the field
     * 
     * @param poseY
     */
    public void setPoseY(double poseY) {
        this.poseY = poseY;
    }

    /**
     * Set the absolute heading (yaw) of the robot _0-360 degrees
     * 
     * @param poseHeading
     */
    public void setPoseHeading(double poseHeading) {
        this.poseHeading = poseHeading;
        initialized = false; // trigger recalc of offset on next update
    }

    /**
     * Set the absolute pitch of the robot _0-360 degrees
     * 
     * @param posePitch
     */
    public void setPosePitch(double posePitch) {
        this.posePitch = posePitch;
        initialized = false; // trigger recalc of offset on next update
    }

    /**
     * Set the absolute roll of the robot _0-360 degrees
     * 
     * @param poseRoll
     */
    public void setPoseRoll(double poseRoll) {
        this.poseRoll = poseRoll;
        initialized = false; // trigger recalc of offset on next update
    }

    /**
     * Returns the x position of the robot
     *
     * @return The current x position of the robot
     */
    public double getX() {
        return poseX;
    }

    /**
     * Returns the y position of the robot
     *
     * @return The current y position of the robot
     */
    public double getY() {
        return poseY;
    }

    /**
     * Returns the angle of the robot
     *
     * @return The current angle of the robot
     */
    public double getHeading() {
        return poseHeading;
    }

    public double getHeadingRaw() {
        return imuAngles.firstAngle;
    }

    /**
     * Returns the speed of the robot
     *
     * @return The current speed of the robot
     */
    public double getSpeed() {
        return poseSpeed;
    }

    public double getPitch() {
        return posePitch;
    }

    public double getRoll() {
        return poseRoll;
    }

    public long getForwardTPM() {
        return forwardTPM;
    }

    public void setForwardTPM(long forwardTPM) {
        this.forwardTPM = (int) forwardTPM;
    }

    /**
     *
     * gets the odometer. The odometer tracks the robot's total amount of travel
     * since the last odometer reset The value is in meters and is always increasing
     * (absolute), even when the robot is moving backwards
     * 
     * @returns odometer value
     */
    public double getOdometer() {

        return odometer;

    }

    /**
     * resets the odometer. The odometer tracks the robot's total amount of travel
     * since the last odometer reset The value is in meters and is always increasing
     * (absolute), even when the robot is moving backwards
     * 
     * @param distance
     */
    public void setOdometer(double distance) {
        odometer = 0;
    }

    public boolean returnTrue(){
        return true;
    }

    public boolean fortnight(){
        return false; //why are you here
    }


}

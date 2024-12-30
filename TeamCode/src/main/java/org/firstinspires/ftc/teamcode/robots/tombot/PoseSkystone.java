package org.firstinspires.ftc.teamcode.robots.tombot;

import android.graphics.Bitmap;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.dashboard.telemetry.TelemetryPacket;
import com.qualcomm.hardware.bosch.BNO055IMU;
import com.qualcomm.robotcore.hardware.AnalogInput;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DistanceSensor;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;
import org.firstinspires.ftc.teamcode.util.PIDController;
import org.firstinspires.ftc.teamcode.vision.Viewpoint;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import static org.firstinspires.ftc.teamcode.util.utilMethods.futureTime;
import static org.firstinspires.ftc.teamcode.util.utilMethods.nextCardinal;
import static org.firstinspires.ftc.teamcode.util.utilMethods.wrapAngle;
import static org.firstinspires.ftc.teamcode.util.utilMethods.wrapAngleMinus;
import static org.firstinspires.ftc.teamcode.vision.Config.*;

/**
 * The Pose class stores the current real world position/orientation:
 * <b>position</b>, <b>heading</b>, and <b>speed</b> of the robot.
 *
 * This class should be a point of reference for any navigation classes that
 * want to know current orientation and location of the robot. The update method
 * must be called regularly, it monitors and integrates data from the
 * orientation (IMU) and odometry (motor encoder) sensors.
 * 
 * @author Abhijit Bhattaru
 * @version 3.0
 * @since 2018-11-02
 */

@Config
public class PoseSkystone {

    // setup
    HardwareMap hwMap;
    PIDController drivePID = new PIDController(0, 0, 0);
    PIDController alignPID = new PIDController(ALIGN_P, ALIGN_I, ALIGN_D);
    private int autoAlignStage = 0;
    FtcDashboard dashboard;

    public static double kpDrive = 0.02; // proportional constant multiplier
    public static double kiDrive = 0.01; // integral constant multiplier
    public static double kdDrive = 0.68; // derivative constant multiplier //increase
    public static double cutout = 1.0;

    public double headingP = 0.007;
    public double headingD = 0;

    public double balanceP = .35;
    public double balanceD = 3.1444;

    public double stoneLengthMeters = 8 * 25.4 / 1000;
    public long stoneLengthTicks = (long) stoneLengthMeters * forwardTPM;
    public double foundationToNearestStoneMeters = 1.75; // tune depending on final arm position.

    // All Actuators
    private DcMotor motorFrontRight = null;
    private DcMotor motorBackLeft = null;
    private DcMotor motorFrontLeft = null;
    private DcMotor motorBackRight = null;
    private DcMotor elbow = null;
    private DcMotor extender = null;
    private DcMotor turretMotor = null;
    private Servo intakeServoFront = null;
    private Servo intakeServoBack = null;
    private Servo gripperSwivel = null;
    private Servo hook = null;
    Servo blinkin = null;

    // All Subsystems
    public Crane crane = null;
    public LEDSystem ledSystem = null;
    public Turret turret = null;

    // All sensors
    BNO055IMU imu; // Inertial Measurement Unit: Accelerometer and Gyroscope combination sensor
    BNO055IMU turretIMU;
    DistanceSensor distForward;
    DistanceSensor distLeft;
    DistanceSensor distRight;
    AnalogInput gripperLeft;
    AnalogInput gripperRight;
    // DigitalChannel magSensor;

    // drive train power values
    private double powerLeft = 0;
    private double powerRight = 0;
    // mecanum types
    private double powerFrontLeft = 0;
    private double powerFrontRight = 0;
    private double powerBackLeft = 0;
    private double powerBackRight = 0;

    // PID values
    public static int forwardTPM = 1304;// todo- use drive IMU to get this perfect
    int rightTPM = 1304; // todo - these need to be tuned for each robot
    int leftTPM = 1304; // todo - these need to be tuned for each robot
    private int strafeTPM = 1909; // todo - fix value high priority this current value is based on Kraken -
                                  // minimech will be different
    private double poseX;
    private double poseY;
    private static double poseHeading; // current heading in degrees. Might be rotated by 90 degrees from imu's heading when strafing
    private double poseHeadingRad; // current heading converted to radians
    private double poseSpeed;
    private double posePitch;
    private double poseRoll;
    private long timeStamp; // timestamp of last update
    private static boolean initialized = false;
    public  double offsetHeading;
    private double offsetPitch;
    private double offsetRoll;

    private double displacement;
    private double displacementPrev;
    private double odometer;

    private double cachedXAcceleration;
    private double lastXAcceleration;

    private double lastUpdateTimestamp = 0;
    private double loopTime = 0;

    private long turnTimer = 0;
    private boolean turnTimerInit = false;
    private double minTurnError = 1.0;
    public boolean maintainHeadingInit = false;

    private double poseSavedHeading = 0.0;

    public boolean isBlue = false;

    public int servoTesterPos = 1600;
    public double autonomousIMUOffset = 0;

    private int craneArticulation = 0;

    // vision related stuff broke in sdk 9.0 when vuforia was removed

    /*
    public SkystoneGripPipeline pipeline;
    public TowerHeightPipeline towerHeightPipeline;
 */
    public enum MoveMode {
        forward, backward, left, right, rotate, still;
    }



    protected MoveMode moveMode;

    public enum Articulation { // serves as a desired robot articulation which may include related complex movements of the elbow, lift and supermanLeft
        calibratePartOne,
        calibratePartTwo,
        calibrateBasic,
        inprogress, // currently in progress to a final articulation
        manual, // target positions are all being manually overridden
        yoinkStone,
        bridgeTransit,
        extendToTowerHeightArticulation,
        autoExtendToTowerHeightArticulation,
        autoAlignArticulation,
        autoRotateToFaceStone,
        retractFromTower,
        retrieveStone,
        cardinalBaseRight,
        cardinalBaseLeft,
        shootOut,
        shootOutII,
        recockGripper;
    }

    public enum RobotType {
        BigWheel, Icarus, Minimech, TomBot;
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
    public PoseSkystone(double x, double y, double heading, double speed) {

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
    public PoseSkystone(double x, double y, double angle) {

        poseX = x;
        poseY = y;
        poseHeading = angle;
        poseSpeed = 0;

    }

    /**
     * Creates a base Pose instance at the origin, (_0,_0), with _0 speed and _0
     * vuAngle. Useful for determining the Pose of the robot relative to the origin.
     */
    public PoseSkystone(RobotType name) {

        poseX = 0;
        poseY = 0;
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

        this.extender = this.hwMap.dcMotor.get("extender");

        this.intakeServoFront = this.hwMap.servo.get("servoGripper");
        this.intakeServoBack = this.hwMap.servo.get("intakeServoBack");
        this.gripperSwivel = this.hwMap.servo.get("gripperSwivel");

        this.hook = this.hwMap.servo.get("hook");

        this.blinkin = this.hwMap.servo.get("blinkin");
        this.distForward = this.hwMap.get(DistanceSensor.class, "distForward");
        this.distRight = this.hwMap.get(DistanceSensor.class, "distRight");
        this.distLeft = this.hwMap.get(DistanceSensor.class, "distLeft");
        // this.magSensor = this.hwMap.get(DigitalChannel.class, "magSensor");
        this.gripperLeft = this.hwMap.get(AnalogInput.class, "gripperLeft"); // Use generic form of device mapping
        this.gripperRight = this.hwMap.get(AnalogInput.class, "gripperRight"); // Use generic form of device mapping

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

        extender.setDirection(DcMotor.Direction.REVERSE);

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
        crane = new Crane(elbow, extender, hook, intakeServoFront, intakeServoBack, gripperSwivel,gripperLeft,gripperRight);
        turretIMU = hwMap.get(BNO055IMU.class, "turretIMU");
        turret = new Turret(turretMotor, turretIMU);
        ledSystem = new LEDSystem(blinkin);

        moveMode = MoveMode.still;

        // setup both IMU's (Assuming 2 rev hubs
        BNO055IMU.Parameters parametersIMU = new BNO055IMU.Parameters();
        parametersIMU.angleUnit = BNO055IMU.AngleUnit.DEGREES;
        parametersIMU.accelUnit = BNO055IMU.AccelUnit.METERS_PERSEC_PERSEC;
        parametersIMU.loggingEnabled = true;
        parametersIMU.loggingTag = "baseIMU";

        imu = hwMap.get(BNO055IMU.class, "baseIMU");
        imu.initialize(parametersIMU);

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
        crane.resetEncoders();
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
        crane.update();
        turret.update(isActive);

        // we haven't worked out the trig of calculating displacement from any
        // driveMixer combination, so
        // for now we are just restricting ourselves to cardinal relative directions of
        // pure forward, backward, left and right
        // so no diagonals or rotations - if we do those then our absolute positioning
        // fails

        switch (moveMode) {
            case forward:
            case backward:
                displacement = (getAverageTicks() - displacementPrev) * forwardTPM;
                odometer += Math.abs(displacement);
                poseHeadingRad = Math.toRadians(poseHeading);
                break;
            default:
                displacement = 0; // when rotating or in an undefined moveMode, ignore/reset displacement
                displacementPrev = 0;
                break;
        }

        odometer += Math.abs(displacement);
        poseSpeed = displacement / (double) (currentTime - this.timeStamp) * 1000000; // meters per second when ticks
                                                                                      // per meter is calibrated

        timeStamp = currentTime;
        displacementPrev = displacement;

        poseX += displacement * Math.cos(poseHeadingRad);
        poseY += displacement * Math.sin(poseHeadingRad);

        lastXAcceleration = cachedXAcceleration;
        cachedXAcceleration = imu.getLinearAcceleration().xAccel;

        loopTime = System.currentTimeMillis() - lastUpdateTimestamp;
        lastUpdateTimestamp = System.currentTimeMillis();

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
     * Stops all motors on the robot
     */
    // public void stopAll(){
    // driveMixerTank(0,0);
    // }

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
            driveIMU(kpDrive, kiDrive, kdDrive, pwr, targetAngle, false);
            return false;
        } // destination achieved
        else {
            // stopAll();
            driveMixerDiffSteer(0, 0);
            driveIMUDistanceInitialzed = false;
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
            driveIMU(kpDrive, kiDrive, kdDrive, pwr, targetAngle, false);
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

    double depositDriveDistance;

    public boolean articulate(Articulation target, boolean setAndForget) {
        articulate(target);
        return true;
    }

    public Articulation articulate(Articulation target) {
        articulation = target; // store the most recent explict articulation request as our target, allows us
                               // to keep calling incomplete multi-step transitions
        if (target == Articulation.manual) {
            miniState = 0; // reset ministate - it should only be used in the context of a multi-step
                           // transition, so safe to reset it here
        }

        switch (articulation) {
            case calibratePartOne:
                if (calibratePartOne())
                    articulation = Articulation.manual;
                break;
            case calibratePartTwo:
                if (calibratePartTwo())
                    articulation = Articulation.manual;
                break;
            case calibrateBasic:
                if (calibrateBasic())
                    articulation = Articulation.manual;
                break;
            case manual:
                break; // do nothing here - likely we are directly overriding articulations in game
            case retractFromTower:
                if (retractFromTower()) {
                    articulation = Articulation.manual;
                }
                break;
            case retrieveStone:
                if (retrieveStone(true)) {
                    articulation = Articulation.manual;
                }
                break;
            case yoinkStone: // todo: fixup comments for deploy actions - moved stuff around
                // auton initial hang at the beginning of a match
                if (YoinkStone()) {
                    articulation = Articulation.manual;
                }
                break;
            case extendToTowerHeightArticulation:
                //if (extendToTowerHeightArticulation()) {
                    articulation = Articulation.manual;
                //}
                break;
            case autoExtendToTowerHeightArticulation:
                //if (autoExtendToTowerHeightArticulation()) {
                    articulation = Articulation.manual;
                //}
                break;
            case autoAlignArticulation:
                //if(autoAlignArticulation()) {
                    articulation = Articulation.manual;
                //}
                break;
            case autoRotateToFaceStone:
                if(RotateToFaceStone()) {
                    articulation = Articulation.manual;
                }
                break;
            case shootOut:
                if (shootOut()) {
                    articulation = Articulation.manual;
                }
                break;
            case shootOutII:
                if (shootOutPartII()) {
                    articulation = Articulation.manual;
                }
                break;
            case recockGripper:
                if (recockStoneGrabber()) {
                    articulation = Articulation.manual;
                }
                break;
            case cardinalBaseLeft:
                if (cardinalBaseTurn(false)) {
                    articulation = Articulation.manual;
                }
                break;
            case cardinalBaseRight:
                if (cardinalBaseTurn(true)) {
                    articulation = Articulation.manual;
                }
                break;

            default:
                return target;

        }
        return target;
    }

    //////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////
    //// ////
    //// Superman/Elbow control functions ////
    //// ////
    //////////////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////////////////////////////////////////////////////////////////

    private int calibrateStage = 0;// todo- finish

    public boolean calibratePartOne() {
        switch (calibrateStage) {
            case 0:
                // calibrate the elbow and arm
                if (crane.calibrate()) {
                    calibrateStage++;
                    miniTimer = futureTime(1);
                }
                break;
            case 1:
                calibrateStage = 0;
                return true;//no break needed -- it would be unreachable
        }
        return false;
    }

    int calibrateOtherStage = 0;

    public boolean calibratePartTwo() {
        switch (calibrateOtherStage) {
            case 0:
                setZeroHeading();
                miniTimer = futureTime(0.2f);
                calibrateOtherStage++;
                break;
            case 1:
                if (System.nanoTime() >= miniTimer) {
                    if (!isBlue) {
                        if (turret.rotateIMUTurret(270.0, 2))
                            calibrateOtherStage++;
                    } else {
                        if (turret.rotateIMUTurret(90.0, 2))
                            calibrateOtherStage++;
                    }
                }
                break;
            case 2:
                if(!isBlue) {
                    if (rotateIMU(270, 2.0)) {
                        if(crane.setElbowTargetPos(460, 1.0)) {
                            calibrateOtherStage++;
                        }
                    }
                }
                else{
                    if (rotateIMU(90, 2.0)) {
                        if(crane.setElbowTargetPos(460, 1.0)) {
                            calibrateOtherStage++;
                        }
                    }
                }
                break;
            case 3:
                ///todo: DANGER - we are temporarily overriding extendMin so the robat can fully retract to a start-legal position Onece the opmode goes active it is very important that extendMin gets reset to 320
                if(crane.extendToPositionUnsafe(0, 1)) {
                    crane.toggleGripper();
                    calibrateOtherStage = 0;
                    return true;
                }
        }
        return false;
    }

    public boolean StoneToFoundation(int stoneNumber) {

        // drive North (forward)
        double dist = stoneNumber * stoneLengthMeters + foundationToNearestStoneMeters;
        if (driveIMUDistance(.6, 0.0, true, dist + .2)) {
            return true;
        }

        return false;

    }

    public boolean FoundationToStone(int stoneNumber) {

        // drive South (reverse)
        double dist = stoneNumber * stoneLengthMeters + foundationToNearestStoneMeters;
        if (driveIMUDistance(.6, 0.0, false, dist)) {
            return true;
        }

        return false;

    }


    public boolean calibrateBasic() {
        setZeroHeading();
        return true;
    }

    int grabState = 0;
    double grabTimer;

    public boolean recockStoneGrabber() {
        switch (grabState) {
            case 0:
                crane.servoGripper.setPosition(2200);
                grabTimer = futureTime(3);
                grabState++;
            case 1:
                if (System.nanoTime() >= grabTimer) {
                    crane.servoGripper.setPosition(1500);
                    grabState = 0;
                    return true;
                }
                break;
        }
        return false;
    }


    // todo these need to be tested - those that are used in articulate() have
    // probably been fixed up by now
    double retrieveTimer;

    public boolean retrieveStone(boolean endsAtNorth) {
        switch (craneArticulation) {
            case 0:
                if (crane.getElbowCurrentPos() < crane.elbowMid)
                    crane.setElbowTargetPos(crane.elbowMid, 1); //make sure were not dragging the stone across the floor
                crane.setGripperSwivelRotation(crane.swivel_Front);
                crane.extendToPosition(crane.extensionBridgeTransit, 1.0); //gets it in very close so we don't strain the arm
                retrieveTimer = futureTime(1);
                craneArticulation++;
                break;
            case 1:
                if (System.nanoTime() >= retrieveTimer) {
                    turret.setPower(.2); //this is so that the turret doesn't yeet the block while turning
                    if(endsAtNorth)
                        turret.setTurntableAngle(0.0); //faces the north
                    else
                        turret.setTurntableAngle(180.0); //faces the south
                    craneArticulation++;
                    retrieveTimer = futureTime(1);
                    turret.setPower(1); //sets the turret power back

                }
                break;
            case 2:
                if (System.nanoTime() >= retrieveTimer) {
                    crane.setElbowTargetPos(crane.elbowBrigeTransit, 1); //sets it down for transition to base
                    craneArticulation = 0;
                    return true;
                }
        }
        return false;
    }

    private int yoinkStage = 0;
    private int ticksTheElbowShouldGoBasedOnVoltageReading = 146; // todo- make this an actual value, based on the
                                                                  // distance sensor reading

    public boolean YoinkStone() { // this goes down and grabs the block note how it assumes it is over the block
                                  // already
        switch (yoinkStage) {
            case 0:
                if (crane.setElbowTargetPos(ticksTheElbowShouldGoBasedOnVoltageReading, .5)) {
                    yoinkStage++;
                }

                break;
            case 1:
                if (crane.setElbowTargetPos(0, 1)) {
                    yoinkStage++;
                }
                break;
            case 2:
                yoinkStage = 0;
                return true;
        }
        return false;
    }

    public boolean extendToTowerHeightArticulation() {
        crane.extendToTowerHeight();
        return true;
    }

    /*
    public boolean autoExtendToTowerHeightArticulation() {
        Mat mat = towerHeightPipeline.process();
        if(mat != null) {
            Bitmap bm = Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.RGB_565);
            Utils.matToBitmap(mat, bm);
            dashboard.sendImage(bm);

            TelemetryPacket packet = new TelemetryPacket();
            packet.put("stack height", towerHeightPipeline.blocks);
            packet.put("aspect ratio", towerHeightPipeline.aspectRatio);
            dashboard.sendTelemetryPacket(packet);
        }
        crane.extendToTowerHeight(getDistForwardDist(), towerHeightPipeline.blocks);
        return true;
    }



    public Mat towerHeightPipelineProcess() {
        Mat mat = towerHeightPipeline.process();
        int error = 0;
        if(mat != null) {
            error = towerHeightPipeline.x - mat.width() / 2;
            Bitmap bm = Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.RGB_565);
            Utils.matToBitmap(mat, bm);
            dashboard.sendImage(bm);

            TelemetryPacket packet = new TelemetryPacket();
            packet.put("x", towerHeightPipeline.x);
            packet.put("error", error);
            dashboard.sendTelemetryPacket(packet);
        }
        return mat;
    }




    public boolean autoAlignArticulation() {
        Mat mat = towerHeightPipelineProcess();
        if(mat == null)
            return false;

        switch(autoAlignStage) {
            case 0:
                alignPID.setSetpoint(mat.width() / 2.0);
                alignPID.setOutputRange(-0.5, 0.5);
                alignPID.setTolerance(0.05);
                alignPID.enable();
                autoAlignStage++;
                break;
            case 1:
                if(!alignPID.onTarget()) {
                    alignPID.setInput(towerHeightPipeline.x);
                    driveMixerDiffSteer(-alignPID.performPID(), 0);
                    break;
                } else {
                    driveMixerDiffSteer(0, 0);
                    return true;
                }
        }
        return false;
    }
     */
    int RotateToFaceStoneStage = 0;

    public boolean RotateToFaceStone(){
        switch(RotateToFaceStoneStage) {
            case 0:
                if (isBlue)
                    crane.setGripperSwivelRotation(crane.swivel_left_Block);
                else
                    crane.setGripperSwivelRotation(crane.swivel_Right_Block);
                crane.setElbowTargetPos(270,.5);
                crane.extendToPosition(1100,.5);
                RotateToFaceStoneStage++;
                break;
            case 1:
                if (turret.rotateUntil(!isBlue, crane.alignGripperForwardFacing())) {
                    miniTimer = futureTime(2);
                    RotateToFaceStoneStage++;
                }
                break;
            case 2:
                crane.setElbowTargetPos(50, .5);
                if (System.nanoTime() >= miniTimer) {
                    crane.setElbowTargetPos(300,.5);
                    RotateToFaceStoneStage = 0;
                    return true;
                }
                break;
        }
        return false;
    }

    int shootStage = 0;

    public boolean shootOut() {
        switch (shootStage) {
            case 0:
                if (crane.setElbowTargetAngle(30)) {
                    miniTimer = futureTime(1);
                    shootStage++;
                }
                break;
            case 1:
                if (System.nanoTime() >= miniTimer) {
                    crane.setExtendABobLengthMeters(3.28);
                    shootStage++;
                }
                break;
            case 2:
                shootStage = 0;
                return true;

        }
        return false;
    }

    public boolean shootOutPartII() {
        switch (shootStage) {
            case 0:
                if (crane.setElbowTargetAngle(30)) {
                    miniTimer = futureTime(1);
                    shootStage++;
                }
                break;
            case 1:
                if (System.nanoTime() >= miniTimer) {
                    crane.setExtendABobLengthMeters(6.56);
                    shootStage++;
                }
                break;
            case 2:
                shootStage = 0;
                return true;

        }
        return false;
    }

    int miniStateRetTow = 0;
    double retractTimer;

    public boolean retractFromTower() {
        switch (miniStateRetTow) {
            case (0):
                crane.toggleGripper();
                retractTimer = futureTime(.1f);
                miniStateRetTow++;
                break;
            case (1):

                if (System.nanoTime() >= retractTimer) {
                    retractTimer = futureTime(.5f);
                    crane.setElbowTargetAngle(crane.getCurrentAngle() + 15);
                    miniStateRetTow++;
                }

                break;
            case (2):

                if (System.nanoTime() >= retractTimer) {
                    if(retrieveStone(false)) {
                        miniStateRetTow++;
                    }
                }

                break;
            case (3):
                //articulate(Articulation.cardinalBaseLeft);
                miniStateRetTow = 0;
                return true;
        }
        return false;
    }

    boolean isNavigating = false;
    boolean autonTurnInitialized = false;
    double autonTurnTarget = 0.0;

    // this is driver interruptible if they set isNavigating back to false
    public boolean cardinalBaseTurn(boolean isRightTurn) {
        if (!autonTurnInitialized) {
            autonTurnTarget = nextCardinal(getHeading(), isRightTurn, 10);
            autonTurnInitialized = true;
            isNavigating = true;
        }

        if (isNavigating == false)
            return true; // abort if drivers override

        if (rotateIMU(autonTurnTarget, 3.0)) {
            isNavigating = false;
            autonTurnInitialized = false;
            return true;
        }
        return false;
    }

    // start pos for this is going to be turret 90 degrees left, where the arm is
    // facing the left side of the board
    boolean atLeft;
    int auxTowerHeight;
    int restackStage = 0;
    double restackTimer;
    boolean restacktoggle = false;

    public boolean restaccDemo(boolean buttonvalue) {
        if (buttonvalue) {
            restacktoggle = !restacktoggle;
        }
        if (restacktoggle) {
            if (crane.getCurrentTowerHeight() > 0) {
                switch (restackStage) {
                    case 0:
                        extendToTowerHeightArticulation();
                        crane.changeTowerHeight(-1);
                        extendToTowerHeightArticulation();
                        restackTimer = futureTime(1);
                        restackStage++;
                        break;
                    case 1:
                        if (System.nanoTime() >= restackTimer) {

                            if (retractFromTower()) {
                                restackTimer = futureTime(1);
                                restackStage++;
                                resetMotors(true);
                            }
                        }
                        break;
                    case 2:
                        if (System.nanoTime() >= restackTimer) {
                            if (driveForward(atLeft, .1, .3)) {
                                crane.changeTowerHeight(auxTowerHeight);
                                restackTimer = futureTime(1);
                                restackStage++;
                            }
                        }
                        break;
                    case 3:
                        if (System.nanoTime() >= restackTimer) {
                            if (retractFromTower()) {
                                auxTowerHeight++;
                                restackTimer = futureTime(1);
                                restackStage++;
                                resetMotors(true);
                            }
                        }
                        break;
                    case 4:
                        if (System.nanoTime() >= restackTimer) {
                            if (driveForward(!atLeft, .1, .3))
                                restackStage = 0;
                        }
                        break;
                }

            } else {
                resetMotors(true);
                if (driveForward(atLeft, .1, .3)) {
                    atLeft = !atLeft;
                    crane.changeTowerHeight(auxTowerHeight);
                    auxTowerHeight = 0;
                }
                return false;
            }
        }
        return false;
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
        crane.stopAll();
        turret.stopAll();
        driveMixerMec(0, 0, 0);
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

        // provide power to the motors
        // motorFrontLeft.setPower(clampMotor(powerFrontLeft));
        motorBackLeft.setPower(clampMotor(powerBackLeft));
        // motorFrontRight.setPower(clampMotor(powerFrontRight));
        motorBackRight.setPower(clampMotor(powerBackRight));

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
    public void movePIDMixer(double Kp, double Ki, double Kd, double pwrFwd, double pwrStf, double currentAngle,
            double targetAngle) {
        // if (pwr>0) PID.setOutputRange(pwr-(1-pwr),1-pwr);
        // else PID.setOutputRange(pwr - (-1 - pwr),-1-pwr);

        // initialization of the PID calculator's output range, target value and
        // multipliers
        drivePID.setOutputRange(-.5, .5);
        drivePID.setIntegralCutIn(cutout);
        drivePID.setPID(Kp, Ki, Kd);
        drivePID.setSetpoint(targetAngle);
        drivePID.enable();

        // initialization of the PID calculator's input range and current value
        drivePID.setInputRange(0, 360);
        drivePID.setContinuous();
        drivePID.setInput(currentAngle);

        // calculates the angular correction to apply
        double correction = drivePID.performPID();

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
            driveIMU(Kp, kiDrive, kdDrive, pwr, targetAngle, strafe);
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
        driveIMU(kpDrive, kiDrive, kdDrive, 0, targetAngle, false); // check to see if the robot turns within a
                                                                    // threshold of the target
        // if(Math.abs(poseHeading - targetAngle) < minTurnError) {
        // turnTimerInit = false;
        // driveMixerMec(0,0,0);
        // return true;
        // }

        if (turnTimer < System.nanoTime()) { // check to see if the robot takes too long to turn within a threshold of
                                             // the target (e.g. it gets stuck)
            turnTimerInit = false;
            driveMixerMec(0, 0, 0);
            return true;
        }
        return false;
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
            driveIMU(kpDrive, kiDrive, kdDrive, 0, poseSavedHeading, false);
        }

        // if the button is not down, set to make sure the correct heading will be saved
        // on the next button press
        if (!buttonState) {
            maintainHeadingInit = false;
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

}

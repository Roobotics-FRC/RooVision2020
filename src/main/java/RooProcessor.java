import edu.wpi.cscore.VideoSource;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.vision.VisionThread;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

public class RooProcessor {
    private static final double TARGET_WIDTH_INCHES = 39.25;
    private static final double TARGET_HEIGHT_INCHES = 17;
    private static final String FOCAL_LENGTH_CONFIG_PATH = "/home/pi/focal_length.txt";
    private static final String NT_CALIB_DIST_FIELD = "fl_calibration_distance";
    private static final String NT_CALIB_ENABLE_FIELD = "fl_calibration_enable";

    private double focalLength = -1;

    private VideoSource camera;
    private NetworkTable visionTable;

    public RooProcessor(VideoSource camera, NetworkTable visionTable) {
        this.camera = camera;
        this.visionTable = visionTable;
        readFocalLength();
        initNetworkTablesFields();
    }

    public void process() {
        VisionThread visionThread = new VisionThread(camera,
                new RooPipeline(), pipeline -> {
            if (!pipeline.filterContoursOutput().isEmpty()) {
                Rect contourRect = Imgproc.boundingRect(pipeline.filterContoursOutput().get(0));
                double centerX = contourRect.x + (contourRect.width / 2d);
                double imgWidth = pipeline.blurOutput().width();
                double pixelOffset = imgWidth / 2d - centerX;

                double degreeOffset = (pixelOffset / imgWidth) * 60d;
                visionTable.getEntry("degree_offset").setDouble(degreeOffset);

                double pixelWidth = contourRect.width;
                visionTable.getEntry("pixel_width").setDouble(pixelWidth);
                double pixelToInchesRatioWidth = TARGET_WIDTH_INCHES / pixelWidth;
                double inchOffset = pixelOffset * pixelToInchesRatioWidth;
                visionTable.getEntry("inch_offset").setDouble(inchOffset);

                double pixelHeight = contourRect.height;
                visionTable.getEntry("pixel_height").setDouble(pixelHeight);
                double pixelToInchesRatioHeight = TARGET_HEIGHT_INCHES / pixelHeight;
//                double currentDistance = focalLength * pixelToInchesRatioHeight;
//                visionTable.getEntry("current_distance").setDouble(currentDistance);

                double pixelYDist = contourRect.y;
                visionTable.getEntry("pixel_y_dist").setDouble(pixelYDist);
                double currentHeightDistance = (pixelHeight - 66.1) / -0.145;
                double currentWidthDistance = (Math.pow(pixelWidth, 2) * 0.00191) - (1.41 * pixelWidth) + 231;
                visionTable.getEntry("height_distance").setDouble(currentHeightDistance);
                visionTable.getEntry("width_distance").setDouble(currentWidthDistance);
                double averageDistance = (currentHeightDistance + currentWidthDistance) / 2;
                visionTable.getEntry("average_distance").setDouble(averageDistance);
                visionTable.getEntry("current_distance").setDouble(averageDistance);

                if (visionTable.getEntry(NT_CALIB_ENABLE_FIELD).getBoolean(false)) {
                    visionTable.getEntry(NT_CALIB_ENABLE_FIELD).setBoolean(false);
                    computeFocalLength(pixelHeight);
                }
            }
        });
        visionThread.start();
    }

    /**
     * Computes the focal length based on the perceived contour height at a known distance specified
     * in NetworkTables in the <code>fl_calibration_distance</code> field. Saves in
     * {@link #focalLength} and writes to disk.
     *
     * @param perceivedHeightPx the height of the contour at the calibration distance, in px.
     */
    private void computeFocalLength(double perceivedHeightPx) {
        double knownDist = visionTable.getEntry(NT_CALIB_DIST_FIELD).getDouble(-1);
        if (knownDist <= 0) {
            System.out.println("Invalid or missing " + NT_CALIB_DIST_FIELD + ". Computation aborted.");
            return;
        }
        System.out.println("Recomputing focal length with parameters: " +
                "known_height = " + TARGET_HEIGHT_INCHES + " in; " +
                "perceived_height = " + perceivedHeightPx + " px;" +
                "known_dist = " + knownDist);
        this.focalLength = knownDist * perceivedHeightPx / TARGET_HEIGHT_INCHES;
        writeFocalLength();
    }

    /**
     * Reads the saved focal length from disk and stores in the {@link #focalLength} variable.
     */
    private void readFocalLength() {
        try {
            String rawSave = Files.readString(Paths.get(FOCAL_LENGTH_CONFIG_PATH));
            // We should be able to use Double.parseDouble(), but that doesn't work for some reason
            DecimalFormat df = new DecimalFormat();
            DecimalFormatSymbols symbols = new DecimalFormatSymbols();
            symbols.setDecimalSeparator('.');
            this.focalLength = df.parse(rawSave).doubleValue();
        } catch (IOException e) {
            System.out.println("Warning: No saved focal length found." +
                    " This must be computed for distance measurement to work.");
        } catch (Exception e) {
            System.err.println("Illegal data found when trying to read saved focal length");
            e.printStackTrace();
        }
    }

    /**
     * Writes the currently computed focal length to disk.
     */
    private void writeFocalLength() {
        final double valueToSave = this.focalLength;
        Runnable runnable = () -> {
            try {
                Runtime.getRuntime().exec(new String[]{"/usr/bin/sudo", "/bin/sh", "-c",
                        "/bin/mount -o remount,rw / && /bin/mount -o remount,rw /boot"});
                Thread.sleep(1000);
            } catch (IOException | InterruptedException e) {
                System.err.println("Failed to make system writable.");
                e.printStackTrace();
            }
            try {
                Files.writeString(Paths.get(FOCAL_LENGTH_CONFIG_PATH), Double.toString(valueToSave));
            } catch (IOException e) {
                System.err.println("Failed to write focal length to disk.");
                e.printStackTrace();
            }
            try {
                Runtime.getRuntime().exec(new String[]{"/usr/bin/sudo", "/bin/sh", "-c",
                        "/bin/mount -o remount,ro / && /bin/mount -o remount,ro /boot"});
            } catch (IOException e) {
                System.err.println("Failed to make system readonly. WARNING: this could render the " +
                        "filesystem corrupt and should be manually corrected immediately.");
                e.printStackTrace();
            }
        };
        new Thread(runnable).start();
    }

    /**
     * Initializes fields on the vision table to prepare for later input in Shuffleboard.
     */
    private void initNetworkTablesFields() {
        if (!visionTable.getEntry(NT_CALIB_DIST_FIELD).exists()) {
            visionTable.getEntry(NT_CALIB_DIST_FIELD).setDouble(-1);
        }
        if (!visionTable.getEntry(NT_CALIB_ENABLE_FIELD).exists()) {
            visionTable.getEntry(NT_CALIB_ENABLE_FIELD).setBoolean(false);
        }

    }
}
import edu.wpi.cscore.VideoSource;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.vision.VisionThread;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class RooProcessor {
    private static final double TARGET_WIDTH_INCHES = 39.25;
    private static final String FOCAL_LENGTH_CONFIG_PATH = "/home/pi/focal_length.txt";

    private double focalLength = -1;

    private VideoSource camera;
    private NetworkTable visionTable;

    public RooProcessor(VideoSource camera, NetworkTable visionTable) {
        this.camera = camera;
        this.visionTable = visionTable;
        readFocalLength();
    }

    public void process() {
        VisionThread visionThread = new VisionThread(camera,
                new GripPipeline(), pipeline -> {
            if (!pipeline.filterContoursOutput().isEmpty()) {
                Rect contourRect = Imgproc.boundingRect(pipeline.filterContoursOutput().get(0));
                double centerX = contourRect.x + (contourRect.width / 2d);
                double imgWidth = pipeline.blurOutput().width();
                double pixelOffset = imgWidth / 2d - centerX;

                double degreeOffset = (pixelOffset / imgWidth) * 60d;
                visionTable.getEntry("degree_offset").setDouble(degreeOffset);

                double pixelWidth = contourRect.width;
                double pixelToInchesRatio = TARGET_WIDTH_INCHES / pixelWidth;
                double inchOffset = pixelOffset * pixelToInchesRatio;
                visionTable.getEntry("inch_offset").setDouble(inchOffset);

                double currentDistance = focalLength * pixelToInchesRatio;
                visionTable.getEntry("current_distance").setDouble(currentDistance);

                if (visionTable.getEntry("fl_calibration_enable").getBoolean(false)) {
                    visionTable.getEntry("fl_calibration_enable").setBoolean(false);
                    computeFocalLength(pixelWidth);
                }
            }
        });
        visionThread.start();
    }

    /**
     * Reads the saved focal length from disk and stores in the {@link #focalLength} variable.
     */
    private void readFocalLength() {
        try {
            String rawSave = Files.readString(Paths.get(FOCAL_LENGTH_CONFIG_PATH));
            this.focalLength = Double.parseDouble(rawSave);
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
        try {
            Runtime.getRuntime().exec(new String[]{"/usr/bin/sudo", "/bin/sh", "-c",
                    "/bin/mount -o remount,rw / && /bin/mount -o remount,rw /boot"});
        } catch (IOException e) {
            System.err.println("Failed to make system writable.");
            e.printStackTrace();
        }
        try {
            Files.writeString(Paths.get(FOCAL_LENGTH_CONFIG_PATH), Double.toString(this.focalLength));
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
    }

    /**
     * Computes the focal length based on the perceived contour width at a known distance specified
     * in NetworkTables in the <code>fl_calibration_distance</code> field. Saves in
     * {@link #focalLength} and writes to disk.
     *
     * @param perceivedWidthPx the width of the contour at the calibration distance, in px.
     */
    private void computeFocalLength(double perceivedWidthPx) {
        double knownDist = visionTable.getEntry("fl_calibration_distance").getDouble(-1);
        if (knownDist <= 0) {
            System.out.println("Invalid or missing fl_calibration_distance. Computation aborted.");
            return;
        }
        this.focalLength = knownDist * perceivedWidthPx / TARGET_WIDTH_INCHES;
        writeFocalLength();
    }
}
import edu.wpi.cscore.VideoSource;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.vision.VisionThread;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

public class RooProcessor {
    private static final double TARGET_WIDTH_INCHES = 39.25;

    private VideoSource camera;
    private NetworkTable visionTable;

    public RooProcessor(VideoSource camera, NetworkTable visionTable) {
        this.camera = camera;
        this.visionTable = visionTable;
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

                double pixelToInchesRatio = TARGET_WIDTH_INCHES / contourRect.width;
                double inchOffset = pixelOffset * pixelToInchesRatio;
                visionTable.getEntry("inch_offset").setDouble(inchOffset);
            }
        });
        visionThread.start();
    }
}
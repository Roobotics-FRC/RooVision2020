import edu.wpi.cscore.VideoSource;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.vision.VisionThread;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

public class RooProcessor {
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
                Rect r = Imgproc.boundingRect(pipeline.filterContoursOutput().get(0));
                double centerX = r.x + (r.width / 2d);
                double imgWidth = pipeline.blurOutput().width();
                double pixelOffset = imgWidth / 2d - centerX;
                double degreeOffset = (pixelOffset / imgWidth) * 60d;
                visionTable.getEntry("center_x").setDouble(centerX);
                visionTable.getEntry("degree_offset").setDouble(degreeOffset);
            }
        });
        visionThread.start();
    }
}
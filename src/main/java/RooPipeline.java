import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

public class RooPipeline extends GripPipeline {
    @Override
    public void process(Mat source0) {
        // Mat destMat = new Mat();
        // Mat cmat = new Mat(3, 3, CvType.CV_64FC1);
        // // cmat.put(0, 0, 3.9880059069460083e+02, 0., 3.1950000000000000e+02, 0., 3.9880059069460083e+02, 2.3950000000000000e+02, 0., 0., 1.);
        // cmat.put(0, 0, 4.4591267180257097e+02, 0., 3.1950000000000000e+02, 0., 4.4591267180257097e+02, 2.3950000000000000e+02, 0., 0., 1.);
        // Mat dmat = new Mat(5, 1, CvType.CV_64FC1);
        // // dmat.put(0, 0,-2.8391855665804844e-01, 1.4110956317092877e-02, 0., 0., 5.5756067125119907e-02);
        // dmat.put(0, 0,-4.2827077098878126e-02, -1.8441706717563970e+00, 0., 0., 6.4991402298444072e+00);
        // Imgproc.undistort(source0, destMat, cmat, dmat);
        //
        // super.process(destMat);
        super.process(source0);
    }
}

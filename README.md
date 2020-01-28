# RooVision2020

This is Team 4373 RooBotics' vision code for the 2020 FIRST Robotics Infinite Recharge competition. It is designed to run on a Raspberry Pi coprocessor using the FRCVision image.

## Exporting from GRIP
* Open the appropriate pipeline file from `pipelines/` in GRIP and generate Java code
* Change `import edu.wpi.first.wpilibj.vision.VisionPipeline` to `import edu.wpi.first.vision.VisionPipeline` in the generated file
* Place the Java file in the correct location (in the `java` directory)

## Building on Desktop
* Ensure Java 11 is properly installed and configured (check your `JAVA_HOME` environment variable)
* Run `./gradlew build` (this generates a jar file in `build/libs`)

### Deploying
* Open the Pi web dashboard at [http://frcvision.local](http://frcvision.local)
* Go to the Application tab, select "Uploaded Java jar," and upload the `RooVision2020-all.jar` file in `build/libs`
* Set the filesystem to "Writable"
* Click Save
* Set the filesystem back to "Read-Only"

To monitor output, go to the Vision Status tab on the web server.

## Building on Pi
* Run `./gradlew build`
* Run `./install.sh` (replaces `/home/pi/runCamera`)
* Run `./runInteractive` in `/home/pi` or `sudo svc -t /service/camera` to restart the service

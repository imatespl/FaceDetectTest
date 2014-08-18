FaceDetectTest
==============

Get pic from video,and save it in redis

Compile And Package
==============

<code>sbt/sbt assembly</code>

SBT Run Test
==============

<code>sbt/sbt "run-main com.opzoon.face.FaceDetectTest /path/to/video  fileExtendName redisServer redisPort"</code>

Run Everywhere
==============

Copy assembly jar to anywhere
<code>cp ./target/scala-2.10/FaceDetectTest-assembly-1.0.1.jar /home/face;cd /home/face</code>
<code>java -cp FaceDetectTest-assembly-1.0.1.jar com.opzoon.face.FaceDetecTest ./faceTest avi 10.0.0.23 6379</code>

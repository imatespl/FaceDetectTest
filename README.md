FaceDetectTest
==============

Get pic from video,and save it in redis

Compile And Package
==============

<pre><code>sbt/sbt assembly</code></pre>

SBT Run Test
==============

<pre><code>sbt/sbt "run-main com.opzoon.face.FaceDetectTest /path/to/video  fileExtendName redisServer redisPort"</code></pre>

Run Everywhere
==============

Copy assembly jar to anywhere<br/>
<pre><code>cp ./target/scala-2.10/FaceDetectTest-assembly-1.0.1.jar /home/face;cd /home/face</code></pre><br/>
<pre><code>java -cp FaceDetectTest-assembly-1.0.1.jar com.opzoon.face.FaceDetecTest ./faceTest avi 10.0.0.23 6379</code></pre>

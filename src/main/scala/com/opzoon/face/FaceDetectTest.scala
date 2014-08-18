package com.opzoon.face


import java.io.{ByteArrayOutputStream, File}
import javax.imageio.ImageIO
import scala.util.matching.Regex

import scredis._
import scala.util.{Success, Failure}

import org.bytedeco.javacv.FFmpegFrameGrabber
import scala.collection.mutable.Set
import java.util.concurrent.atomic.AtomicInteger
import java.lang.Exception
import com.typesafe.scalalogging.slf4j.LazyLogging

/**
 * Created by liudw on 8/7/14.
 */

/**
 * Recursive list file in directory, And filter file
 * use it extends name
 */
class RecursiveListFile{
  def getFiles(f: File, r: Regex): Array[File]= {

    val these = f.listFiles()
    if (these == null) return Array()
    val good = these.filter(f => r.findFirstIn(f.getName).isDefined)
    good ++ these.filter(_.isDirectory).flatMap(getFiles(_, r))
  }
}

/**
 * Parse video to jpg and send all pic to redis
 *
 * @param listFile instance of RecursiveListFile
 * @param dir which dir to scan
 * @param extName the video extends name to filter
 * @param host redis server ip to connect to
 * @param port redis server port to connect to
 */
class ParseVideo(
                 listFile: RecursiveListFile,
                 dir: String,
                 extName: String,
                 host: String,
                 port: Int = 6379 ) extends LazyLogging {

  //Get all files need to parse
  private val files = listFile.getFiles(new File(dir), (""".*\.""" + extName + """$""").r)

  //Static all pic has send and per video pic has send
  private val allVideototalPic = new AtomicInteger(0)
  private val perVideoTotalPic =  Set[(String, Int)]()

  //scredis redis client init
  private val developmentRedis = Redis(host, port.toInt)
  import developmentRedis.dispatcher

  if (parallelFiles(files)) {
    staticTotalPic
  }

  closeRedisConnection

  /**
   * Get all files to par List,Scala can parallel compute List
   * @param files Array of video file which need to get pic from it
   */
  private def parallelFiles(files: Array[File]): Boolean = {

    files.toList.par.foreach( f = file => {

      //Use students ID in directory as Redis key
      // The directory must like file:///rootDirectory/studentsId/students.avi
      val dirSplit: Array[String] = file.getAbsolutePath.split("/")
      val key = dirSplit(dirSplit.length - 2)

      //Use JAVACV FFmpeg get jpg form video
      val grab = new FFmpegFrameGrabber(file)
      grab.start()
      // Get pic and send to redis
      try {
        genImageAndSendToRedis(grab, key)
      } catch {
        case e: Exception => return false
      }
      grab.stop()
    }
    )
    true
  }

  private def genImageAndSendToRedis(
                              grab: FFmpegFrameGrabber,
                              key: String): Boolean = {

    //Get  video all pic to iterator
    val iter = Iterator.continually(grab.grab()).takeWhile(_ != null)

    //Static per Video Pic
    val i = new AtomicInteger(0)

    // get per pic send it
    iter.foreach(
      grabbedImage => {
        //encode bufferedImage to jpg, write it to ByteArrayOutputStream
        val picTimeStamp = System.currentTimeMillis()
        val byteArrayOutputStream = new ByteArrayOutputStream()
        ImageIO.write(grabbedImage.getBufferedImage(), "jpg",
          byteArrayOutputStream)

        //change pic to ByteArray
        byteArrayOutputStream.flush()
        val imageInByte: Array[Byte] = byteArrayOutputStream.toByteArray
        byteArrayOutputStream.close()
        i.getAndIncrement

        //Connect redis and save a pic
        developmentRedis.set(key + "_" + picTimeStamp, imageInByte).onComplete {
          case Success(_) => {
             logger.debug("Save Key name: " + key + "_" + picTimeStamp +
               ", 1 jpg pic in redis")
            developmentRedis.rPush("FaceImageKey", key + "_" + picTimeStamp).onComplete {
              case Success(_) => {
                logger.debug(s"Add <FaceImageKey> value use rPush key: "
                    + key + "_" + picTimeStamp)
              }
              case Failure(e) =>
                throw e
                return false
            }
          }
          case Failure(e) =>
            throw e
            return false
        }
      }
    )
    perVideoTotalPic += (key -> i.get())
    allVideototalPic.getAndAdd(i.get())
    true
  }

  private def closeRedisConnection = developmentRedis.quit()

  def staticTotalPic = {
    perVideoTotalPic map {
      p => logger.info("Send key name: " + p._1 + " pic count: " + p._2.toString)
    }
    logger.info(s"Send total pic is: " + allVideototalPic.get().toString)
  }

}

object ParseVideo{
  def apply(
             listFile: RecursiveListFile,
             dir: String,
             extName: String,
             host: String,
             port: Int = 6379) = new ParseVideo(listFile, dir, extName, host, port)
}

object FaceDetectTest {
  def main(args: Array[String]) {
    if (args.length == 3) {
      val Seq(dir, extName, host) = args.toSeq

      // Get All files in directory
      val listFile = new RecursiveListFile()

      ParseVideo(listFile, dir, extName, host)

    } else if (args.length == 4) {

      val Seq(dir, extName, host, port) = args.toSeq

      // Get All files in directory
      val listFile = new RecursiveListFile()

      ParseVideo(listFile, dir, extName, host, port.toInt)
    } else {
      System.err.println("Usage: FaceDetectTest <directory> <extendName> <redisHost> [<redisPort>]")
      System.exit(1)
    }
  }

}
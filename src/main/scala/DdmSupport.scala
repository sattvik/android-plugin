import sbt._

import com.android.ddmlib.AndroidDebugBridge
import com.android.ddmlib.IDevice
import com.android.ddmlib.Log
import com.android.ddmlib.RawImage

import java.io.{File, OutputStream, IOException}
import java.awt.image.{BufferedImage, RenderedImage}
import javax.imageio.ImageIO

trait DdmSupport extends BaseAndroidProject {

  lazy val bridge = {
    AndroidDebugBridge.init(false)
    Runtime.getRuntime().addShutdownHook(new Thread() { override def run() { AndroidDebugBridge.terminate() }})
    AndroidDebugBridge.createBridge(adbPath.absolutePath, false)
  }

  lazy val screenshotEmulator = screenshotEmulatorAction
  def screenshotEmulatorAction = task {
    screenshot(true, false).getOrElse(error("could not get screenshot")).toFile("png", "emulator.png")
    None
  } describedAs("take a screenshot from the emulator")

  lazy val screenshotDevice = screenshotDeviceAction
  def screenshotDeviceAction = task {
    screenshot(false, false).getOrElse(error("could not get screenshot")).toFile("png", "device.png")
    None
  } describedAs("take a screenshot from the device")


  // ported from http://dustingram.com/wiki/Device_Screenshots_with_the_Android_Debug_Bridge
  private[this] def withDevice[F](emulator: Boolean)(action: IDevice => F):Option[F] = {
    var count = 0
    while (!bridge.hasInitialDeviceList() && count < 50) {
      Thread.sleep(100)
      count += 1
    }
    if (!bridge.hasInitialDeviceList()) {
      System.err.println("Timeout getting device list")
      None
    } else {
      val (emus, devices) = bridge.getDevices.partition(_.isEmulator)
      (if (emulator) emus else devices).firstOption.map(action)
    }
  }

  private[this] def screenshot(emulator: Boolean, landscape: Boolean):Option[Screenshot] = {
    withDevice(emulator) { device =>
      val raw = device.getScreenshot()
      val (width2, height2) = if (landscape) (raw.height, raw.width) else (raw.width, raw.height)
      val image = new BufferedImage(width2, height2, BufferedImage.TYPE_INT_RGB)
      var index = 0
      val indexInc = raw.bpp >> 3
      for (y <- 0 until raw.height; x <- 0 until raw.width) {
        val value = raw.getARGB(index)
        if (landscape)
          image.setRGB(y, raw.width - x - 1, value)
        else
          image.setRGB(x, y, value)
        index += indexInc
      }
      new Screenshot(image)
    }
  }

  class Screenshot(val r: RenderedImage) {
    def toFile(format: String, f: File):Boolean = ImageIO.write(r, format, f)
    def toFile(format: String, s: String):Boolean = toFile(format, new File(s))
    def toOutputStream(format: String, o: OutputStream) = ImageIO.write(r, format, o)
  }
}

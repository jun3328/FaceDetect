package io.github.jesterz91.opcvapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_detect.*
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.debug
import org.jetbrains.anko.error
import org.opencv.android.BaseLoaderCallback
import org.opencv.android.CameraBridgeViewBase
import org.opencv.android.LoaderCallbackInterface
import org.opencv.android.OpenCVLoader
import org.opencv.core.Core
import org.opencv.core.Mat
import java.io.File
import java.io.FileOutputStream


class DetectActivity : AppCompatActivity(), CameraBridgeViewBase.CvCameraViewListener2, AnkoLogger {

    private var cascadeClassifier_face = 0L
    private var cascadeClassifier_eye = 0L

    companion object {
        init {
            System.loadLibrary("opencv_java4")
            System.loadLibrary("native-lib")
        }
    }

    external fun loadCascade(cascadeFileName: String): Long

    external fun detect(cascadeClassifier_face: Long, cascadeClassifier_eye: Long, matAddrInput: Long, matAddrResult: Long)

    val mLoaderCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            super.onManagerConnected(status)
            when (status) {
                LoaderCallbackInterface.SUCCESS -> javaCameraView.enableView()
                else -> super.onManagerConnected(status)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.apply {
            setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
            setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        setContentView(R.layout.activity_detect)

        if (!allPermissionGranted()) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        } else {
            read_cascade_file()
        }

        javaCameraView.apply {
            visibility = View.VISIBLE
            setCameraIndex(0)
            setCvCameraViewListener(this@DetectActivity)
        }

        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
    }

    override fun onPause() {
        super.onPause()
        javaCameraView?.disableView()
    }

    override fun onResume() {
        super.onResume()
        if (!OpenCVLoader.initDebug()) {
            debug { "onResume :: Internal OpenCV library not found." }
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_2_0, this, mLoaderCallback)
        } else {
            debug { "onResum :: OpenCV library found inside package. Using it!" }
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        javaCameraView?.disableView()
    }

    override fun onCameraViewStarted(width: Int, height: Int) {
    }

    override fun onCameraViewStopped() {
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame?): Mat {
        val matInput = inputFrame?.rgba()
        val matResult = Mat(matInput!!.rows(), matInput.cols(), matInput.type())

        Core.flip(matInput, matInput, 1)
        detect(cascadeClassifier_face, cascadeClassifier_eye, matInput.nativeObjAddr, matResult.nativeObjAddr)

        return matResult
    }

    private fun read_cascade_file() {
        copyFile("haarcascade_frontalface_alt.xml")
        copyFile("haarcascade_eye_tree_eyeglasses.xml")

        debug { "read_cascade_file:" }
        cascadeClassifier_face = loadCascade("haarcascade_frontalface_alt.xml")

        debug { "read_cascade_file:" }
        cascadeClassifier_eye = loadCascade("haarcascade_eye_tree_eyeglasses.xml")
    }

    fun copyFile(filename: String) {
        val baseDir = Environment.getExternalStorageDirectory().path
        val pathDir = baseDir + File.separator + filename

        val assetManager = this.assets

        try {
            debug { "copyFile :: 다음 경로로 파일복사 $pathDir" }
            val inputStream = assetManager.open(filename)
            val outputStream = FileOutputStream(pathDir)
            val buffer = ByteArray(1024)

            var read = inputStream.read(buffer)

            while (read != -1) {
                outputStream.write(buffer, 0, read)
                read = inputStream.read(buffer)
            }

            inputStream.close()
            outputStream.flush()
            outputStream.close()
            
        } catch (e: Exception) {
            error { "copyFile :: 파일 복사 중 예외 발생 ${e.message}" }
        }

    }

    // 퍼미션 관련 메소드

    private val REQUEST_CODE_PERMISSIONS = 1000

    private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)

    private fun allPermissionGranted(): Boolean {
        REQUIRED_PERMISSIONS.forEach {
            if (ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS && grantResults.size > 0) {
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "권한을 설정하세요", Toast.LENGTH_SHORT).show()
                    return
                }
            }
            read_cascade_file()
        }
    }
}

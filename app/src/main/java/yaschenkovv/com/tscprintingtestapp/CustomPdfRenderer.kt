package yaschenkovv.com.tscprintingtestapp

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.graphics.pdf.PdfRenderer.Page
import android.os.ParcelFileDescriptor
import android.support.annotation.RequiresApi
import android.util.Log
import android.widget.ImageView
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.toObservable
import io.reactivex.schedulers.Schedulers
import yaschenkovv.com.tscprintingtestapp.bitmapconverter.BitmapRescaler
import yaschenkovv.com.tscprintingtestapp.bitmapconverter.BmpConverter
import yaschenkovv.com.tscprintingtestapp.bitmapconverter.OneBitBmpConverter
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object BmpSizeConstant {
    const val HEIGHT = 210
    const val WIDTH = 344
}

class CustomPdfRenderer {

    private val TAG = "PdfConverter"
    private lateinit var _executor: ExecutorService
    private lateinit var _scheduler: Scheduler
    private val root = "${android.os.Environment.getExternalStorageDirectory()}/Download/Converted"
    private val convertSaver = BmpConverter()
    private val rescaler = BitmapRescaler()


    @RequiresApi(21)
    fun convertPdfToBitmap(file: File, increaseFactor: Double = 1.0): Observable<Int> {
        initExecutorService()
        Log.d(TAG, "Начало конвертации")
        return renderPdfDoc(file)
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap { it.toObservable() }
                .flatMap {
                    Observable.just(it)
                            .subscribeOn(_scheduler)
                            .map {
                                // todo тут зменять реализацию, после всех опытов удалить
                                convertSaver.save(it.bitmap, "$root/convertedFile.bmp")
                            }
                }.map {
                    Log.d(TAG, "file was saved")
                }.doFinally { _executor.shutdown() }
    }

    fun getByteArrayFromPdf(file: File, imageView: ImageView): Observable<ByteArray> {
        initExecutorService()
        return renderPdfDoc(file)
                .flatMap { it.toObservable() }
                .flatMap {
                    Observable.just(it)
                            .subscribeOn(_scheduler)
                            .map {
                                //Log.d(TAG, "Thread is ${Thread.currentThread()}")
                                val image = rescaler.rescaleDown(2, OneBitBmpConverter().convert(it.bitmap, 0), BmpSizeConstant.HEIGHT * 2)
                                //saveImage(it.fileName, image, BmpSizeConstant.WIDTH, BmpSizeConstant.HEIGHT)
                                image
                            }
                }
    }

    private fun saveImage(fileName: String, rawBitmapData: ByteArray, width: Int, height: Int): String {
        val fileOutputStream: FileOutputStream
        val bmpFile = BMPFile()
        val file = File(root, fileName + ".bmp")
        file.createNewFile()
        fileOutputStream = FileOutputStream(file)

        bmpFile.saveBitmap(fileOutputStream, rawBitmapData, width, height)
        return "Success"
    }

    private fun renderPdfDoc(file: File): Observable<ArrayList<CheckList>> =
            Observable.create<ArrayList<CheckList>>{ source ->
                Log.d(TAG, "filePath is ${file.absolutePath}")
                val pdfRenderer = PdfRenderer(getFileDescriptor(file))
                pdfRenderer.use {
                    val checkList = ArrayList<CheckList>(280)
                    for (i in 0..81) {
                        checkList.add(createCheckList(it.openPage(i), "${i}_${file.absolutePath.toString().substringAfterLast("/")}"))
                    }
                    source.onNext(checkList)
                }
            }.subscribeOn(Schedulers.io())

    private fun createCheckList(page: Page, fileName: String) = CheckList(renderPdfPage(page), fileName)

    private fun renderPdfPage(page: Page): Bitmap {
        val bitmap = Bitmap.createBitmap(BmpSizeConstant.WIDTH * 2, BmpSizeConstant.HEIGHT * 2, Bitmap.Config.ARGB_8888)
        Log.d(TAG, "Bitmap size is ${bitmap.byteCount}")
        page.render(bitmap, null, null, Page.RENDER_MODE_FOR_PRINT)
        page.close()
        return bitmap
    }

    private fun initExecutorService() {
        val threadCt = Runtime.getRuntime().availableProcessors() / 2 + 1
        Log.d(TAG, "available processor count = " + threadCt)
        _executor = Executors.newFixedThreadPool(threadCt)
        _scheduler = Schedulers.from(_executor)
    }

    private fun getFileDescriptor(file: File) =
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
}
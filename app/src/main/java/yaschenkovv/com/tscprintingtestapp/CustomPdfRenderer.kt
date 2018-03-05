package yaschenkovv.com.tscprintingtestapp

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.graphics.pdf.PdfRenderer.Page
import android.os.ParcelFileDescriptor
import android.util.Log
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.schedulers.Schedulers
import yaschenkovv.com.tscprintingtestapp.bitmapconverter.BitmapResizer
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
    private val converter = OneBitBmpConverter()
    private val resizer = BitmapResizer()
    private val root = "${android.os.Environment.getExternalStorageDirectory()}/Download/Converted"

    //TODO Не забудь закрыть executor!!!
    fun getByteArrayFromPdf(file: File): Observable<ByteArray> {
        initExecutorService()
        val startTime = System.currentTimeMillis()
        return renderPdfDoc(file)
                .flatMap {
                    prepareImageByteArray(it)
                }
                .doFinally {
                    Log.d(TAG, "Время выполнения = ${System.currentTimeMillis() - startTime}")
                    _executor.shutdown()
                }
    }

    fun getArrayOfPrintableImage(file: File): Observable<ArrayList<ByteArray>> {
        initExecutorService()
        val startTime = System.currentTimeMillis()
        return renderPdfDocList(file)
                .flatMap { prepareImageNullableByteArray(it)}
                .doFinally {
                    Log.d(TAG, "Время выполнения = ${System.currentTimeMillis() - startTime}")
                    _executor.shutdown()
                }
    }

    private fun prepareImageByteArray(label: Bitmap): Observable<ByteArray> {
        return Observable.create<ByteArray> {
            val byteArray = resizer.rescaleDown(2, converter.convert(label, 0), BmpSizeConstant.HEIGHT * 2)
                    .apply {
                        //saveImage("fileName", this, BmpSizeConstant.WIDTH, BmpSizeConstant.HEIGHT)
                    }
//            Log.d(TAG, "count is $count")
//            if (count in 800..830)
                it.onNext(byteArray)
                it.onComplete()
        }.subscribeOn(_scheduler)
//                .subscribeOn(_scheduler)
//                .map {
//                    count++
//                    Log.d(TAG, "count is $count")
//                    val byteArray = resizer.rescaleDown(2, converter.convert(label, 0), BmpSizeConstant.HEIGHT * 2)
//                            .apply {
//                                //saveImage("fileName", this, BmpSizeConstant.WIDTH, BmpSizeConstant.HEIGHT)
//                            }
//
//                    else
//                        return@map null
//                }
    }

    //TODO на данный момент запись в массив - потоконебезопасная операция
    private fun prepareImageNullableByteArray(labels: Array<Bitmap?>): Observable<ArrayList<ByteArray>> {
        val byteArrayList = ArrayList<ByteArray>(25)
        return Observable.just(labels)
                .subscribeOn(_scheduler)
                .flatMap {
                    Observable.create<ArrayList<ByteArray>> {
                        Log.d(TAG, "Current thread is ${Thread.currentThread()}")
                        for (label in labels) {
                            if (label != null) byteArrayList.add(resizer.rescaleDown(
                                    2, converter.convert(label, 0), BmpSizeConstant.HEIGHT * 2)
                            )
                        }
                        it.onNext(byteArrayList)
                        it.onComplete()
                    }
                }
    }

    private fun saveImage(fileName: String, rawBitmapData: ByteArray, width: Int, height: Int) {
        val fileOutputStream: FileOutputStream
        val bmpFile = BMPFile()
        val file = File(root, fileName + ".bmp")
        file.createNewFile()
        fileOutputStream = FileOutputStream(file)
        bmpFile.saveBitmap(fileOutputStream, rawBitmapData, width, height)
    }

    //В один момент времени можно открыть (it.openPage) только одну страницу. Не надо пытаться
    //распаралелить рендеринг pdf страниц на несколько потоков
    private fun renderPdfDoc(file: File) = Observable.create<Bitmap> { source ->
        val pdfRenderer = PdfRenderer(getFileDescriptor(file))
        //var bitmapList: Array<Bitmap?> = arrayOfNulls(10)
        pdfRenderer.use {
            for (j in 0..2){
                for (i in 0 until 200) {
                    val page = it.openPage(i)
                    source.onNext(renderPdfPage(page))
                }
            }
        }
        Log.d(TAG, "task is complete")
        source.onComplete()
    }.subscribeOn(Schedulers.io())

    private fun renderPdfDocList(file: File) = Observable.create<Array<Bitmap?>> { source ->
        val pdfRenderer = PdfRenderer(getFileDescriptor(file))
        var bitmapList: Array<Bitmap?> = arrayOfNulls(20)
        pdfRenderer.use {
            for (j in 0..0){
                for (i in 0 until 30) {
                    val page = it.openPage(i)
                    bitmapList[i] = renderPdfPage(page)
                }
                source.onNext(bitmapList)
                bitmapList = arrayOfNulls(10)
//                System.gc()
            }
        }
        Log.d(TAG, "task is complete")
        source.onComplete()
    }.subscribeOn(Schedulers.io())

    private fun renderPdfPage(page: Page): Bitmap {
        val bitmap = Bitmap.createBitmap(BmpSizeConstant.WIDTH * 2, BmpSizeConstant.HEIGHT * 2, Bitmap.Config.ARGB_8888)
        page.use { it.render(bitmap, null, null, Page.RENDER_MODE_FOR_PRINT) }
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
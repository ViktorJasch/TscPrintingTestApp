package yaschenkovv.com.tscprintingtestapp

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.Menu
import android.widget.Toast
import com.example.tscdll.TSCUSBActivity
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File
import java.io.FileInputStream

object TscCommands {
    val POSTFIX = "\r\n"
}

class MainActivity : AppCompatActivity() {


    internal var TscUSB = TSCUSBActivity()

    private val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"
    private val TAG = "TSC"
    private var mUsbManager: UsbManager? = null
    private var mPermissionIntent: PendingIntent? = null
    private var hasPermissionToCommunicate = false
    private val root = "${android.os.Environment.getExternalStorageDirectory()}/Download"
    private val pdfRoot = "$root/checks.pdf"
    private val renderer = CustomPdfRenderer()
    private lateinit var usbConnection: UsbDeviceConnection
    private lateinit var usbEndpoint: UsbEndpoint

    private var device: UsbDevice? = null

    private val mUsbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            Log.d(TAG, "onReceive")
            if (ACTION_USB_PERMISSION == action) {
                synchronized(this) {
                    Log.d(TAG, "device attached")
                    val device = intent.getParcelableExtra<Parcelable>(UsbManager.EXTRA_DEVICE) as UsbDevice
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (device != null) {
                            hasPermissionToCommunicate = true
                        }
                    }
                }
            }
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mUsbManager = getSystemService(Context.USB_SERVICE) as UsbManager
        mPermissionIntent = PendingIntent.getBroadcast(this, 0, Intent(ACTION_USB_PERMISSION), 0)
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        registerReceiver(mUsbReceiver, filter)

        val deviceList = mUsbManager!!.deviceList
        Log.d(TAG, deviceList.size.toString() + " USB device(s) found")
        val deviceIterator = deviceList.values.iterator()
        while (deviceIterator.hasNext()) {
            device = deviceIterator.next()
            if (device!!.vendorId == 4611) {
                Log.d(TAG, "device name = ${device.toString()}")
                Toast.makeText(this, device.toString(), Toast.LENGTH_SHORT).show()
                break
            }
        }

        //-----------start-----------
        val mPermissionIntent = PendingIntent.getBroadcast(this@MainActivity, 0,
                Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_ONE_SHOT)
        //mUsbManager!!.requestPermission(device, mPermissionIntent)

        convertButton!!.setOnClickListener { convertPdf(pdfRoot) }

        printBitmapBtn!!.setOnClickListener {//if (mUsbManager!!.hasPermission(device))
            printByteArrayBitmap(pdfRoot)
        }

        downloadBitmapBtn!!.setOnClickListener { if (mUsbManager!!.hasPermission(device))
            downloadBmp(editText.text.toString())
                .observeOn(AndroidSchedulers.mainThread()).subscribe()}

        deleteFab!!.setOnClickListener { if (mUsbManager!!.hasPermission(device))
            deleteImage(editText.text.toString())
                    .subscribe() }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.activity_main, menu)
        return true
    }

    private fun printBitmap(filename: String) = Completable.create {
        preparePrinter()
        tscPrintBmpLabel(filename)
        TscUSB.closeport()
        it.onComplete()
    }.subscribeOn(Schedulers.io())

    private fun printPcx(filename: String) = Completable.create {
        preparePrinter()
        tscPrintPcxLabel(filename)
        TscUSB.closeport()
        it.onComplete()
    }.subscribeOn(Schedulers.io())

    private fun printByteArrayBitmap(filePath: String) {
        if (!filePath.endsWith(".pdf")){
            throw IllegalArgumentException("Only pdf files can be used")
        }
        var file = File(filePath)
        renderer.getByteArrayFromPdf(file, checkIv)
                .flatMapCompletable { tscBitmapPrinting(it) }
                .subscribe { Log.d(TAG, "end printing") }
    }

    private fun printBmp(filePath: String) {
        if (!filePath.endsWith(".pdf")){
            throw IllegalArgumentException("Only pdf files can be used")
        }
        var file = File(filePath)
        renderer.getByteArrayFromPdf(file, checkIv)
                .flatMapCompletable { tscBitmapPrinting(it) }
                .subscribe { Log.d(TAG, "end printing") }
    }

    private fun tscBitmapPrinting(byteArray: ByteArray) = Completable.create {
        Log.d(TAG, "printing the byteArray with size: " + byteArray.size)
//        preparePrinter()
//        bitmapCommand(byteArray)
//        TscUSB.closeport()
        it.onComplete()
    }

    private fun preparePrinter() {
        openPort()
        sendMessage("SIZE 43 mm,25 mm\n\r".toByteArray())
        sendMessage("GAP 5 mm,0 mm\n\r".toByteArray())
        sendMessage("SPEED 4\n\r".toByteArray())
    }

    private fun bitmapCommand(byteArray: ByteArray) {
        val string = " 00 " +
                "00 00 00 00 00 07 FF 03 FF 11 " +
                "FF 18 FF 1C 7F 1E 3F 1F 1F 1F " +
                "8F 1F C7 1F E3 1F E7 1F FF 1F " +
                "FF"
        val bytes= string.hexStringToByteArray()
        sendMessage("CLS\n\r".toByteArray())
        sendMessage("BITMAP 0,0,44,210,0,".toByteArray())
        sendMessage(byteArray)
        sendMessage(TscCommands.POSTFIX.toByteArray())
//        sendMessage("ERASE 0,125,360,50\n\r".toByteArray())
//        sendMessage("BARCODE 15,125,\"128\",45,0,0,2,2,\"108416190017999_\"\n\r".toByteArray())
        sendMessage("PRINT 1,1\n\r".toByteArray())
    }

    private fun convertPdf(filePath: String) {
        if (!filePath.endsWith(".pdf")){
            throw IllegalArgumentException("Only pdf files can be used")
        }
        var file = File(filePath)
        renderer.convertPdfToBitmap(file)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { Toast.makeText(this, "Все сконвертнулось", Toast.LENGTH_SHORT).show() }
    }

    private fun tscPrintBmpLabel(file: String){
        sendMessage("CLS\n".toByteArray())
        sendMessage("PUTBMP 0,0,\"$file\"\n".toByteArray())
        sendMessage("PRINT 1,1\n".toByteArray())
    }

    private fun tscPrintPcxLabel(file: String){
        sendMessage("CLS\n".toByteArray())
        sendMessage("PUTPCX 0,0,\"$file\"\n".toByteArray())
        sendMessage("PRINT 1,1\n".toByteArray())
    }

    private fun downloadBmp(filename: String) = Completable.create {
            FileInputStream("$root/Converted/$filename.bmp").use {
                var data = ByteArray(it.available())
                val test = "DOWNLOAD F,\"$filename\",${data.size},"
                data = it.readBytes()
                Log.d(TAG, "Size is ${data.size}")
                openPort()
                Log.d(TAG, "send command:")
                sendMessage(test.toByteArray())
                Log.d(TAG, "send data:")
                sendMessage(data)
                Thread.sleep(1000)
                sendMessage(data)
                TscUSB.sendcommand(TscCommands.POSTFIX.toByteArray())
                Log.d(TAG, "close port")
                TscUSB.closeport()
            }
            it.onComplete()
        }.subscribeOn(Schedulers.computation())

    private fun deleteImage(filename: String) = Single.create<String> {
        TscUSB.openport(mUsbManager!!, device!!)
        TscUSB.sendcommand("KILL F, \"$filename\"\r\n")
        TscUSB.closeport()
    }.subscribeOn(Schedulers.io())

    private fun sendMessage(byteArray: ByteArray) {
        usbConnection.bulkTransfer(usbEndpoint, byteArray, byteArray.size, 4000)
    }

    private fun openPort() {
        val usbIntf = device!!.getInterface(0)
        usbEndpoint = usbIntf.getEndpoint(0)
        usbConnection = mUsbManager!!.openDevice(device)
        val result = usbConnection.claimInterface(usbIntf, true)
        Log.d(TAG, "result of usb connection = " + result)
    }

    fun String.hexStringToByteArray(): ByteArray {
        val hexStr = this.replace(" ", "")

        var result = ByteArray(hexStr.length / 2, {0})

        for(i in 0 until hexStr.length step 2) {
            val hex = hexStr.substring(i, i + 2)
            val byte: Byte = Integer.valueOf(hex, 16).toByte()
            result[ i / 2] = byte
        }

        return result
    }

    override fun onDestroy() {
        unregisterReceiver(mUsbReceiver)
        super.onDestroy()
    }
}

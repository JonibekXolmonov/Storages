package com.example.storages

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import java.io.*
import java.nio.charset.Charset
import java.util.*


class MainActivity : AppCompatActivity() {

    private val APP_PERMISSION_CODE: Int = 1001
    private lateinit var btnSaveInternal: MaterialButton
    private lateinit var btnReadInternal: MaterialButton
    private lateinit var btnSaveExternal: MaterialButton
    private lateinit var btnReadExternal: MaterialButton
    private lateinit var btnPhotoOperation: MaterialButton
    private lateinit var btnAppSettings: MaterialButton

    private var isPersistent = true
    var isInternal = false
    private var readPermissionGranted = false
    private var writePermissionGranted = true

    private val fileName = "android.txt"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkStoragePath()
        createInternalFile()
        initViews()

        //requestPermissions()
    }

    private fun requestPermissions() {
        val hasReadPermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

        val hasWritePermission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

        val permissionToRequest = mutableListOf<String>()

        if (!readPermissionGranted) {
            permissionToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        if (!writePermissionGranted) {
            permissionToRequest.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        if (permissionToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionToRequest.toTypedArray())
        }
    }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            readPermissionGranted =
                permissions[Manifest.permission.READ_EXTERNAL_STORAGE] ?: readPermissionGranted

            writePermissionGranted =
                permissions[Manifest.permission.WRITE_EXTERNAL_STORAGE] ?: writePermissionGranted

            if (readPermissionGranted)
                Toast.makeText(this, "READ_EXTERNAL_STORAGE", Toast.LENGTH_SHORT).show()

            if (writePermissionGranted)
                Toast.makeText(this, "WRITE_EXTERNAL_STORAGE", Toast.LENGTH_SHORT).show()
        }

    private fun initViews() {
        btnSaveInternal = findViewById(R.id.btnSaveInternal)
        btnReadInternal = findViewById(R.id.btnReadInternal)
        btnSaveExternal = findViewById(R.id.btnSaveExternal)
        btnReadExternal = findViewById(R.id.btnReadExternal)
        btnPhotoOperation = findViewById(R.id.btnPhotoOperation)
        btnAppSettings = findViewById(R.id.btnAppSettings)

        btnSaveInternal.setOnClickListener {
            saveInternalFile("Jonibek Xolmonov")
        }

        btnReadInternal.setOnClickListener {
            readInternal()
        }

        btnSaveExternal.setOnClickListener {
            saveExternalFile("I think the message has been saved externally!")
        }

        btnReadExternal.setOnClickListener {
            readExternalFile()
        }

        btnPhotoOperation.setOnClickListener {
            takePhoto.launch()
        }

        btnAppSettings.setOnClickListener {
            openAppPermission()
        }
    }

    private fun openAppPermission() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri: Uri = Uri.fromParts("package", packageName, null)
        intent.data = uri

        startActivityForResult(intent, APP_PERMISSION_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == APP_PERMISSION_CODE) {
            // Here we check if the user granted the permission or not using
            //Manifest and PackageManager as usual
            checkPermissionIsGranted()
        }

    }

    private fun checkPermissionIsGranted() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PERMISSION_GRANTED
        )
            Toast.makeText(this, "Granted", Toast.LENGTH_SHORT).show()
        else
            Toast.makeText(this, "Not granted", Toast.LENGTH_SHORT).show()
    }

    private val takePhoto =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
            val filename = UUID.randomUUID().toString()

            val isPhotoSaved = if (isInternal) {
                savePhotoToInternalStorage(filename, bitmap)
            } else {
                if (writePermissionGranted) {
                    savePhotoToExternalStorage(filename, bitmap)
                } else {
                    false
                }
            }

            if (isPhotoSaved) {
                Toast.makeText(this, "Photo saved successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Failed to save photo", Toast.LENGTH_SHORT).show()
            }
        }

    private fun savePhotoToExternalStorage(filename: String, bitmap: Bitmap?): Boolean {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$filename.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpg")
            put(MediaStore.Images.Media.WIDTH, bitmap!!.width)
            put(MediaStore.Images.Media.HEIGHT, bitmap.height)
        }

        return try {
            contentResolver.insert(collection, contentValues)?.also { uri ->
                contentResolver.openOutputStream(uri).use { outputStream ->
                    if (!bitmap!!.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)) {
                        throw IOException("Couldn't save bitmap")
                    }
                }
            } ?: throw IOException("Couldn't create MediaStore entry")
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun savePhotoToInternalStorage(filename: String, bitmap: Bitmap?): Boolean {
        return try {
            openFileOutput("$filename.jpg", MODE_PRIVATE).use { stream ->
                if (!bitmap!!.compress(Bitmap.CompressFormat.JPEG, 95, stream)) {
                    throw IOException("Couldn't save bitmap")
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun checkStoragePath() {
        val internal_m1 = getDir("custom", 0)
        val internal_m2 = filesDir

        val external_m1 = getExternalFilesDir(null)
        val external_m2 = externalCacheDir
        val external_m3 = getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        Log.d("TAG", "checkStoragePath: ${internal_m1.absolutePath}")
        Log.d("TAG", "checkStoragePath: ${internal_m2.absolutePath}")
        Log.d("TAG", "checkStoragePath: ${external_m1!!.absolutePath}")
        Log.d("TAG", "checkStoragePath: ${external_m2!!.absolutePath}")
        Log.d("TAG", "checkStoragePath: ${external_m3!!.absolutePath}")
    }

    private fun createInternalFile() {

        val file: File = if (isPersistent) {
            File(filesDir, fileName)
        } else {
            File(cacheDir, fileName)
        }

        if (!file.exists()) {
            try {
                file.createNewFile()
                Log.d(
                    "TAG",
                    "createInternalFile: ${String.format("File %s has been created", fileName)}"
                )
            } catch (e: Exception) {
                Log.d(
                    "TAG",
                    "createInternalFile: ${String.format("File %s creation failed", fileName)}"
                )
            }
        } else {
            Log.d(
                "TAG",
                "createInternalFile: ${String.format("File %s already exists", fileName)}"
            )
        }
    }

    private fun saveInternalFile(data: String) {

        try {

            val fileOutputStream: FileOutputStream = if (isPersistent) {
                openFileOutput(fileName, MODE_PRIVATE)
            } else {
                val file = File(cacheDir, fileName)
                FileOutputStream(file)
            }

            fileOutputStream.write(data.toByteArray(Charset.forName("UTF-8")))
            Log.d(
                "TAG",
                "saveInternalFile: ${String.format("Write to file %s succeed", fileName)}"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d(
                "TAG",
                "saveInternalFile: ${String.format("Write to file %s failed", fileName)}"
            )
        }
    }

    private fun readInternal() {
        try {
            val fileInputStream: FileInputStream = if (isPersistent) {
                openFileInput(fileName)
            } else {
                val file = File(cacheDir, fileName)
                FileInputStream(file)
            }

            val inputStreamReader = InputStreamReader(fileInputStream, Charset.forName("UTf-8"))

            val lines: MutableList<String?> = ArrayList()
            val reader = BufferedReader(inputStreamReader)
            var line = reader.readLine()

            while (line != null) {
                lines.add(line)
                line = reader.readLine()
            }

            val readText = TextUtils.join("\n", lines)
            Log.d("TAG", "readInternal: $readText")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d("TAG", "readInternal: ${String.format("Read from file % failed", fileName)}")
        }
    }

    private fun saveExternalFile(data: String) {
        val file = if (isPersistent) {
            File(getExternalFilesDir(null), fileName)
        } else {
            File(externalCacheDir, fileName)
        }

        try {
            val fileOutputStream = FileOutputStream(file)
            fileOutputStream.write(data.toByteArray(Charset.forName("UTF-8")))
            Log.d("TAG", "saveExternalFile: ${String.format("Write to %s succeed", fileName)}")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d("TAG", "saveExternalFile: ${String.format("Write to file %s failed", file)}")
        }
    }

    private fun readExternalFile() {
        val file = if (isPersistent) {
            File(getExternalFilesDir(null), fileName)
        } else {
            File(externalCacheDir, fileName)
        }

        try {
            val fileInputStream = FileInputStream(file)
            val inputStreamReader = InputStreamReader(fileInputStream, Charset.forName("UTF-8"))

            val lines: MutableList<String?> = ArrayList()
            val reader = BufferedReader(inputStreamReader)
            var line = reader.readLine()

            while (line != null) {
                lines.add(line)
                line = reader.readLine()
            }

            val readText = TextUtils.join("\n", lines)
            Log.d("TAG", "readExternalFile: $readText")

        } catch (e: Exception) {
            e.printStackTrace()
            Log.d(
                "TAG",
                "readExternalFile: ${String().format("Read from file %s failed", fileName)}"
            )
        }
    }
}
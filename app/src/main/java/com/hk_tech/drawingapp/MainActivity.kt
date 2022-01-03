package com.hk_tech.drawingapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import com.hk_tech.drawingapp.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var brushSizeOpen = false
    private var creatorDialogOpen = false
    private var toolbarOpen = false
    private var colorPattelOpen = false
    private lateinit var mImageButtonCurrentPaint: ImageButton
    private lateinit var progressBar: Dialog

    private val openGalleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                binding.ivBackground.setImageURI(result.data?.data)
            }
        }

    // Permission
    private val requestPermission: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                val permissionName = it.key
                val isGranted = it.value

                if (isGranted) {
                    Toast.makeText(
                        this, "Permission Granted", Toast.LENGTH_LONG).show()

                    val pickIntent =
                        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    openGalleryLauncher.launch(pickIntent)

                } else {
                    if (permissionName == Manifest.permission.READ_EXTERNAL_STORAGE) {
                        Toast.makeText(
                            this, "You denied the permission", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }

    // Animations -Start-
    private val rotateOpen: Animation by lazy {
        AnimationUtils.loadAnimation(this,
            R.anim.open_rotate_anim)
    }
    private val rotateClose: Animation by lazy {
        AnimationUtils.loadAnimation(this,
            R.anim.close_rotate_anim)
    }
    private val horizontalToolsOpen: Animation by lazy {
        AnimationUtils.loadAnimation(this,
            R.anim.horizontal_tools_open)
    }
    private val horizontalToolsClose: Animation by lazy {
        AnimationUtils.loadAnimation(this,
            R.anim.horizontal_tools_close)
    }
    private val creatorClose: Animation by lazy {
        AnimationUtils.loadAnimation(this,
            R.anim.creator_close_anime)
    }
    private val creatorOpen: Animation by lazy {
        AnimationUtils.loadAnimation(this,
            R.anim.creator_open_anim)
    }
    private val verticalToolsOpen: Animation by lazy {
        AnimationUtils.loadAnimation(this,
            R.anim.vertical_tools_open)
    }
    private val verticalToolsClose: Animation by lazy {
        AnimationUtils.loadAnimation(this,
            R.anim.vertical_tools_close)
    }
    // Animation -Close-

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val linearLayoutPaintColors = binding.llColorPetal
        mImageButtonCurrentPaint = linearLayoutPaintColors[1] as ImageButton
        mImageButtonCurrentPaint.setImageDrawable(
            ContextCompat.getDrawable(this, R.drawable.pallet_selected)
        )

        binding.drawingView.setSizeForBrush(20.toFloat())

        binding.fbBack.setOnClickListener {
            binding.drawingView.undo()
        }
        binding.fbClear.setOnClickListener {
            binding.drawingView.clear()
        }

        binding.fbShowTools.setOnClickListener {
            if (creatorDialogOpen) {
                binding.creatorDialog.creatorDialogD.startAnimation(creatorClose)
                binding.creatorDialog.creatorDialogD.visibility = View.GONE
                creatorDialogOpen = false
            }
            if (!toolbarOpen) {
                binding.fbShowTools.startAnimation(rotateOpen)
                binding.llHorizontalToolBar.startAnimation(horizontalToolsOpen)
                binding.llHorizontalToolBar.visibility = View.VISIBLE
                binding.llVerticalToolBar.startAnimation(verticalToolsOpen)
                binding.llVerticalToolBar.visibility = View.VISIBLE
                toolbarOpen = true
            } else {
                binding.fbShowTools.startAnimation(rotateClose)
                binding.llHorizontalToolBar.startAnimation(horizontalToolsClose)
                binding.llHorizontalToolBar.visibility = View.GONE
                binding.llVerticalToolBar.startAnimation(verticalToolsClose)
                binding.llVerticalToolBar.visibility = View.GONE
                toolbarOpen = false
                binding.llColorPetal.visibility = View.GONE
                colorPattelOpen = false
                binding.brushSize.lBrushSize.visibility = View.GONE
                brushSizeOpen = false
            }

        }

        binding.fbBrush.setOnClickListener {
            if (!brushSizeOpen) {
                binding.brushSize.lBrushSize.visibility = View.VISIBLE
                brushSizeOpen = true
                binding.llColorPetal.visibility = View.GONE
                colorPattelOpen = false
            } else {
                binding.brushSize.lBrushSize.visibility = View.GONE
                brushSizeOpen = false
            }
            showBrushSizeDialog()
        }

        binding.fbColor.setOnClickListener {
            if (!colorPattelOpen) {
                binding.llColorPetal.visibility = View.VISIBLE
                colorPattelOpen = true
                binding.brushSize.lBrushSize.visibility = View.GONE
                brushSizeOpen = false
            } else {
                binding.llColorPetal.visibility = View.GONE
                colorPattelOpen = false
            }

        }

        binding.fbCreator.setOnClickListener {
            if (!creatorDialogOpen) {
                showCreatorDialog()
                creatorDialogOpen = true

            } else {
                creatorDialogOpen = false
                binding.creatorDialog.creatorDialogD.startAnimation(creatorClose)
                binding.creatorDialog.creatorDialogD.visibility = View.GONE
            }
        }

        binding.fbGallery.setOnClickListener {
            requestStoragePermission()
        }

        binding.fbSAVE.setOnClickListener {
            if (isReadStorageAllowed()) {
                showProgressBar()
                lifecycleScope.launch {
                    saveBitmapFile(getBitMapFromView(binding.flDrawingViewContainer))
                }
            }
        }
    }

    private fun requestStoragePermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.READ_EXTERNAL_STORAGE)
        ) {
            showRationaleDialog("Let's Draw", "Let's Draw App " +
                    "needs to Access Your External Storage")
        } else {
            requestPermission.launch(arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ))
        }
    }

    private fun isReadStorageAllowed(): Boolean {
        val result =
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun showRationaleDialog(title: String, message: String) {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
        builder.create().show()

    }

    private fun showCreatorDialog() {
        binding.creatorDialog.creatorDialogD.startAnimation(creatorOpen)
        binding.creatorDialog.creatorDialogD.visibility = View.VISIBLE
        binding.creatorDialog.ivClose.setOnClickListener {
            binding.creatorDialog.creatorDialogD.startAnimation(creatorClose)
            binding.creatorDialog.creatorDialogD.visibility = View.GONE
            creatorDialogOpen = false
        }
        connectToSocialMedia()
    }

    @SuppressLint("QueryPermissionsNeeded")
    private fun connectToSocialMedia() {
        binding.creatorDialog.ivWhatsApp.setOnClickListener {
            // initialise mobile number with country code
            val mNum = "+917999863081"
            val packageManager = this.packageManager
            // initialise uri
            val uri: Uri = Uri.parse("http://api.whatsapp.com/sent?phone=${mNum}"
                    + "&text=Hi Hamid")
            // initialise intent
            val intent = Intent(Intent.ACTION_VIEW)
            // set data
            intent.data = uri
            // set package
            intent.`package` = "com.whatsapp"
            //set flag
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            // start activity
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
            } else {
                Toast.makeText(this, "kismat hi kharab he tere to!", Toast.LENGTH_SHORT).show()
            }
        }

        binding.creatorDialog.ivLinkedin.setOnClickListener {
            // initialise link and package
            val sPackage = "com.linkedin.android"
            val sWebLink = "http://www.linkedin.com/in/abdul-hamid-khatri-4599581a0/"
            openLink(sWebLink, sPackage, sWebLink)
        }

        binding.creatorDialog.ivInstagram.setOnClickListener {
            // initialise link and package
            val sAppLink = "http://www.instagram.com/__deadline_gamer__"
            val sPackage = "com.instagram.android"
            openLink(sAppLink, sPackage, sAppLink)
        }
    }

    private fun openLink(sAppLink: String, sPackage: String, sWebLink: String) {
        try {
            // when app is installed
            // initializing uri
            val uri = Uri.parse(sAppLink)
            // initialise intent
            val intent = Intent(Intent.ACTION_VIEW)
            // set data
            intent.data = uri
            // set package
            intent.`package` = sPackage
            // set flag
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            // start activity
            startActivity(intent)
        } catch (activityNotFoundException: ActivityNotFoundException) {
            // open link in browser
            val uri = Uri.parse(sWebLink)
            // initialize intent
            val intent = Intent(Intent.ACTION_VIEW)
            // sent data
            intent.data = uri
            // set flag
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

            startActivity(intent)
        }
    }

    private fun showBrushSizeDialog() {
        binding.brushSize.ibSmallBrush.setOnClickListener {
            binding.drawingView.setSizeForBrush(5.toFloat())
            binding.brushSize.lBrushSize.visibility = View.GONE
            brushSizeOpen = false
        }
        binding.brushSize.ibMediumBrush.setOnClickListener {
            binding.drawingView.setSizeForBrush(10.toFloat())
            binding.brushSize.lBrushSize.visibility = View.GONE
            brushSizeOpen = false
        }
        binding.brushSize.ibLargeBrush.setOnClickListener {
            binding.drawingView.setSizeForBrush(20.toFloat())
            binding.brushSize.lBrushSize.visibility = View.GONE
            brushSizeOpen = false
        }
    }

    fun paintClicked(view: View) {
        if (mImageButtonCurrentPaint != view) {
            val imageButton = view as ImageButton
            val colorTag = imageButton.tag.toString()

            binding.drawingView.setColor(colorTag)
            imageButton.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_selected)
            )
            mImageButtonCurrentPaint.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.pallet_normal)
            )

            mImageButtonCurrentPaint = view
        }
    }

    private fun getBitMapFromView(view: View): Bitmap {
        val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(returnedBitmap)
        val bgDrawable = view.background
        if (bgDrawable != null) {
            bgDrawable.draw(canvas)
        } else {
            canvas.drawColor(Color.WHITE)
        }
        view.draw(canvas)
        return returnedBitmap
    }

    private suspend fun saveBitmapFile(mBitmap: Bitmap?): String {
        var result = ""
        withContext(Dispatchers.IO) {
            if (mBitmap != null) {
                try {
                    val bytes = ByteArrayOutputStream()
                    mBitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)
                    val f = File(externalCacheDir?.absoluteFile.toString()
                            + File.separator + "Let's Draw_" + System.currentTimeMillis() / 1000 + ".png")
                    val fo = FileOutputStream(f)
                    fo.write(bytes.toByteArray())
                    fo.close()

                    result = f.absolutePath

                    runOnUiThread {
                        hideProgressBar()
                        if (result.isNotEmpty()) {
                            Toast.makeText(applicationContext,
                                "File Saved Successfully: $result",
                                Toast.LENGTH_LONG).show()
                            shareImage(result)
                        } else {
                            Toast.makeText(applicationContext,
                                "Something went wrong wile saving the file",
                                Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    result = ""
                    e.printStackTrace()
                }
            }
        }
        return result
    }

    private fun showProgressBar() {
        progressBar = Dialog(this)
        progressBar.setContentView(R.layout.progressbar)
        progressBar.show()
    }

    private fun hideProgressBar() {
        progressBar.dismiss()
    }

    private fun shareImage(result: String) {
        MediaScannerConnection.scanFile(this, arrayOf(result), null) { path, uri ->
            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
            shareIntent.type = "image/png"
            startActivity(Intent.createChooser(shareIntent, "Share"))
        }
    }

    companion object {
    }

}
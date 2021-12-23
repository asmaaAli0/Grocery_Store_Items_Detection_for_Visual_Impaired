package com.example.imageclassification

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.example.imageclassification.ml.General
import com.example.imageclassification.ml.MobilenetV110224Quant
import com.example.imageclassification.ml.MobilenetVeges
import com.example.imageclassification.ml.VegesVgg
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.util.*
import java.util.jar.Manifest

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    lateinit var select_image_button : Button
    lateinit var make_prediction : Button
    lateinit var img_view : ImageView // to view the image
    lateinit var text_view : TextView // to view text of predicted class
    lateinit var bitmap: Bitmap // to store the image that user selected
    lateinit var camerabtn : Button
    lateinit var tts: TextToSpeech

    // function to camera permission
    public fun checkandGetpermissions(){
        if(checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED){
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), 100)
        }
        else{
            Toast.makeText(this, "Camera permission granted", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == 100){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(this, "Camera permission granted", Toast.LENGTH_SHORT).show()
            }
            else{
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        select_image_button = findViewById(R.id.button)
        make_prediction = findViewById(R.id.button2)
        img_view = findViewById(R.id.imageView2)
        text_view = findViewById(R.id.textView)
        camerabtn = findViewById<Button>(R.id.camerabtn)
        tts = TextToSpeech(this, this)

        // handling permissions
        checkandGetpermissions()

        // save class names in list of strings
        val labels = application.assets.open("Veges_Labels.txt").bufferedReader().use { it.readText() }.split("\n")

        // when user clicks select button
        select_image_button.setOnClickListener(View.OnClickListener {
            Log.d("mssg", "button pressed")

            // intent of type image
            var intent : Intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"

            startActivityForResult(intent, 250)
        })

        // when user clicks on predict button
        make_prediction.setOnClickListener(View.OnClickListener {
            // resize the bitmap to model input shape (check .tflite file)
            var resized = Bitmap.createScaledBitmap(bitmap, 224, 224, true)

            // the following code is copied from tflite file
            //val model = MobilenetVeges.newInstance(this)
            //val model = General.newInstance(this)
            val model = VegesVgg.newInstance(this)

            // create byte buffer from the resized bitmap
            // var tbuffer = TensorImage.fromBitmap(resized)
            // var byteBuffer = tbuffer.

            var tensorImage = TensorImage(DataType.FLOAT32)
            tensorImage.load(resized)
            var byteBuffer = tensorImage.buffer

// Creates inputs for reference.
            val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.FLOAT32)
            inputFeature0.loadBuffer(byteBuffer)

// Runs model inference and gets result.
            val outputs = model.process(inputFeature0)
            // predicted output
            val outputFeature0 = outputs.outputFeature0AsTensorBuffer

            // get the index of the highest value from output array
            var max = getMax(outputFeature0.floatArray)

            //Log.d("fruit", outputFeature0.floatArray[0].toString())
            //Log.d("package", outputFeature0.floatArray[1].toString())
            //Log.d("veges", outputFeature0.floatArray[2].toString())

            // get class name of the index from labels.txt
            // and replace text view with prediction
             text_view.setText(labels[max])
            //text_view.setText(outputFeature0.floatArray)

// Releases model resources if no longer used.
            model.close()
            speakOut()
        })

        // when users clicks on camera button
        camerabtn.setOnClickListener(View.OnClickListener {
            var camera : Intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(camera, 200)
        })


    }

    // when user selects an image, change the img view to this image
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // if user selected image
        if(requestCode == 250){
            // show the selected image in the img view
            img_view.setImageURI(data?.data)

            // save the image in bitmap to use in predicting
            var uri : Uri ?= data?.data
            bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
        }
        // if user captured image
        else if(requestCode == 200 && resultCode == Activity.RESULT_OK){
            // save the captured image in bitmap to use in predicting
            bitmap = data?.extras?.get("data") as Bitmap
            // show the captured image in the img view
            img_view.setImageBitmap(bitmap)
        }

    }

    fun getMax(arr:FloatArray) : Int{
        var ind = 0;
        var min = 0.0f;

        // iterate over the 1000 label (bec we have 1000 class) and get the index with max value
        // TODO : change number of classes

        for(i in 0..21)
        {
            Log.d("loop", i.toString())
            if(arr[i] > min)
            {
                Log.d("hi","in if")
                Log.d("index", i.toString())
                min = arr[i]
                ind = i;
                Log.d("min", min.toString())
            }
        }
        Log.d("out", ind.toString())
        return ind
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS){
            val result = tts.setLanguage(Locale.US)
        }
    }

    private fun speakOut(){
        val text = text_view.text.toString()
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "")
    }
}
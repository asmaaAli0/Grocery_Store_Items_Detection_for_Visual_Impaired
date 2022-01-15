package com.example.imageclassification

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.*
import com.example.imageclassification.ml.*
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.IOException
import java.util.*
import android.content.ActivityNotFoundException
import android.text.Editable
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    lateinit var select_image_button : Button
    lateinit var img_view : ImageView // to view the image
    lateinit var text_view : TextView // to view text of predicted class
    lateinit var bitmap: Bitmap // to store the image that user selected
    lateinit var cameraimgbtn : ImageButton
    lateinit var tts: TextToSpeech
    lateinit var tts1: TextToSpeech
    lateinit var tts2: TextToSpeech
    lateinit var tts0: TextToSpeech
    lateinit var activityResultLauncher: ActivityResultLauncher<Intent>

    public var desc: String =""

    var mediaPlayer1 : MediaPlayer? = null
    lateinit var speechRecognizer : SpeechRecognizer

    // function to camera permission
    public fun checkandGetpermissions(){
        if(checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED){
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), 100)
        }
        else{
            Toast.makeText(this, "Camera permission granted", Toast.LENGTH_SHORT).show()
        }

        if(checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED){
            requestPermissions(arrayOf(android.Manifest.permission.RECORD_AUDIO), 200)
        }
        else{
            Toast.makeText(this, "Record permission granted", Toast.LENGTH_SHORT).show()
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

        if(requestCode == 200){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED)
            {
                Toast.makeText(this, "Record permission granted", Toast.LENGTH_SHORT).show()
            }
            else{
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        welcomeAudio()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        select_image_button = findViewById(R.id.button)
        img_view = findViewById(R.id.imageView2)
        text_view = findViewById(R.id.textView)
        cameraimgbtn = findViewById<ImageButton>(R.id.imageButton)
        tts = TextToSpeech(this, this)
        tts1 = TextToSpeech(this, this)
        tts2 = TextToSpeech(this, this)
        tts0 = TextToSpeech(this, this)

        //tts0.speak("Welcome to SuperEye. Press anywhere to open the camera.", TextToSpeech.QUEUE_FLUSH, null, "")

        // handling permissions
        checkandGetpermissions()

        // when user clicks select button
        select_image_button.setOnClickListener(View.OnClickListener {
            Log.d("mssg", "button pressed")

            // intent of type image
            var intent : Intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"

            startActivityForResult(intent, 250)
        })

        // when users clicks on camera button
        cameraimgbtn.setOnClickListener(View.OnClickListener {
            var camera : Intent = Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(camera, 200)
        })

        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result: ActivityResult? ->
            if (result!!.resultCode == RESULT_OK && result!!.data != null){
                val speechtext = result!!.data!!.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS) as ArrayList<Editable>
                Log.d("speec:", speechtext[0].toString())
                if(speechtext[0].toString() == "yes"){
                    Log.d("yaay", desc)
                    tts2.speak(desc, TextToSpeech.QUEUE_FLUSH, null, "")
                }
            }
        }
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
            predict()
        }
        // if user captured image
        else if(requestCode == 200 && resultCode == Activity.RESULT_OK){
            // save the captured image in bitmap to use in predicting
            bitmap = data?.extras?.get("data") as Bitmap
            // show the captured image in the img view
            img_view.setImageBitmap(bitmap)
            predict()
        }


    }

    private fun predict() {
        // save classes names in list of strings
        val Primary_labels = application.assets.open("Primary_Labels.txt").bufferedReader().use { it.readText() }.split("\n")
        val Veg_labels = application.assets.open("Veges_Labels.txt").bufferedReader().use { it.readText() }.split("\n")
        val Fruit_labels = application.assets.open("Fruit_Labels.txt").bufferedReader().use { it.readText() }.split("\n")
        val Package_labels = application.assets.open("Package_Labels.txt").bufferedReader().use { it.readText() }.split("\n")

        // save classes description in list of strings
        val Veg_descriptions = application.assets.open("Veges_Description.txt").bufferedReader().use { it.readText() }.split("\n")
        val Fruit_descriptions  = application.assets.open("Fruit_Description.txt").bufferedReader().use { it.readText() }.split("\n")
        val Package_descriptions  = application.assets.open("Package_Description.txt").bufferedReader().use { it.readText() }.split("\n")


        var resized = Bitmap.createScaledBitmap(bitmap, 224, 224, true)

        // the following code is copied from tflite file
        val prim_model = BaseModelVgg.newInstance(this)

        // create byte buffer from the resized bitmap
        var prim_tensorImage = TensorImage(DataType.FLOAT32)
        prim_tensorImage.load(resized)
        var prim_byteBuffer = prim_tensorImage.buffer

        // Creates inputs for reference.
        val prim_inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 224, 224, 3), DataType.FLOAT32)
        prim_inputFeature0.loadBuffer(prim_byteBuffer)

        // Runs model inference and gets result.
        val prim_outputs = prim_model.process(prim_inputFeature0)
        // predicted output
        val prim_outputFeature0 = prim_outputs.outputFeature0AsTensorBuffer

        // get the index of the highest value from output array
        var prim_max = getMax(prim_outputFeature0.floatArray,3)
        //text_view.setText(Primary_labels[prim_max])

        // Releases model resources if no longer used.
        prim_model.close()

        // Fruits class
        if(prim_max == 0){
            val model = FruitsVgg.newInstance(this)

            // create byte buffer from the resized bitmap
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
            var max = getMax(outputFeature0.floatArray,28)

            // get class name of the index from labels.txt
            // and replace text view with prediction
            text_view.setText(Fruit_labels[max])

            // Releases model resources if no longer used.
            model.close()
            speakOut()
            //Thread.sleep(1000)
            tts1.speak("Do you want to hear the full product description?", TextToSpeech.QUEUE_FLUSH, null, "")
            //Thread.sleep(3500)
            desc = Fruit_descriptions[max]
            Describe()
        }
        // Packages class
        else if(prim_max == 1){
            val model = PackagesVgg16.newInstance(this)

            // create byte buffer from the resized bitmap
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
            var max = getMax(outputFeature0.floatArray,31)

            // get class name of the index from labels.txt
            // and replace text view with prediction
            text_view.setText(Package_labels[max])

            // Releases model resources if no longer used.
            model.close()
            speakOut()
            //Thread.sleep(1000)
            tts1.speak("Do you want to hear the full product description?", TextToSpeech.QUEUE_FLUSH, null, "")
            //Thread.sleep(3500)
            desc = Package_descriptions[max]
            Describe()
        }
        // Vegetables class
        else if (prim_max == 2){
            val model = VegesVgg.newInstance(this)

            // create byte buffer from the resized bitmap
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
            var max = getMax(outputFeature0.floatArray,22)

            // get class name of the index from labels.txt
            // and replace text view with prediction
            text_view.setText(Veg_labels[max])

            // Releases model resources if no longer used.
            model.close()
            speakOut()
            //Thread.sleep(1000)
            tts1.speak("Do you want to hear the full product description?", TextToSpeech.QUEUE_FLUSH, null, "")
            //Thread.sleep(3500)
            desc = Veg_descriptions[max]
            Describe()

        }
    }

    fun getMax(arr:FloatArray,label_num:Int) : Int{
        var ind = 0;
        var min = 0.0f;

        // iterate over the classes labels and get the index with max value
        for(i in 0..(label_num-1))
        {
            Log.d("loop", i.toString())
            if(arr[i] > min)
            {
                min = arr[i]
                ind = i;
            }
        }
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
        Thread.sleep(1000)
    }

    private fun Describe() {
        Thread.sleep(3500)
        val rec_intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        rec_intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        rec_intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE,Locale.getDefault())
        rec_intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "recording..")

        try {
            activityResultLauncher.launch(rec_intent)
        }catch (exp:ActivityNotFoundException){
            Toast.makeText(applicationContext, "Device does not support recording", Toast.LENGTH_SHORT).show()
        }

    }


    private fun welcomeAudio() {
        //tts0.speak("Welcome to SuperEye. Press anywhere to open the camera.", TextToSpeech.QUEUE_FLUSH, null, "")

        try {
            mediaPlayer1 = MediaPlayer()
            mediaPlayer1!!.setAudioStreamType(AudioManager.STREAM_MUSIC)
            val audio = getAssets().openFd("welcome_camera.wav")
            mediaPlayer1!!.setDataSource(audio.getFileDescriptor(),audio.getStartOffset(),audio.getLength())
            mediaPlayer1!!.prepare()
            mediaPlayer1!!.start()
            Log.d("audio","playing audio")
            Toast.makeText(this, "playing welcome audio", Toast.LENGTH_LONG).show()
        }catch (e: IOException){
            e.printStackTrace() }


    }

}

package com.example.SuperEye

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
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.*

import java.io.File
import java.io.IOException
import android.media.MediaRecorder
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.lang.IllegalStateException
import android.widget.*
import java.io.ByteArrayOutputStream


class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    lateinit var select_image_button : Button
    lateinit var img_view : ImageView // to view the image
    lateinit var text_view : TextView // to view text of predicted class
    lateinit var bitmap: Bitmap // to store the image that user selected
    lateinit var cameraimgbtn : ImageButton
    lateinit var tts: TextToSpeech
    lateinit var imageUri : Uri
    lateinit var storage: FirebaseStorage
    lateinit var storageRefrence : StorageReference
    var mediaPlayer : MediaPlayer? = null
    var mediaPlayer1 : MediaPlayer? = null
    protected var recorder: MediaRecorder? = null
    private var recFileName: String = ""
    private var player: MediaPlayer? = null
    lateinit var audioUri : Uri


    override fun onCreate(savedInstanceState: Bundle?) {
        welcomeAudio()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        select_image_button = findViewById(R.id.button)
        img_view = findViewById(R.id.imageView2)
        text_view = findViewById(R.id.textView)
        tts = TextToSpeech(this, this)
        cameraimgbtn = findViewById<ImageButton>(R.id.imageButton)
        // Connecting to firebase
        storage = FirebaseStorage.getInstance()
        storageRefrence = storage.getReference()

        recFileName = "${externalCacheDir?.absolutePath}/response.mp3"

        // handling permissions

        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ) {
            val permissions = arrayOf(android.Manifest.permission.RECORD_AUDIO, android.Manifest.permission.CAMERA,  android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE)
            ActivityCompat.requestPermissions(this, permissions,0)
        }




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


    }


    // when user selects an image, change the img view to this image
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // if user selected image
        if(requestCode == 250 ){
            // show the selected image in the img view
            img_view.setImageURI(data?.data)

            // save the image in bitmap to use in predicting
            //var uri : Uri ?= data?.data
            if (data != null) {

                imageUri = data.getData()!!
                bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, imageUri)
                uploadPicture()
                predict()

            }

        }
        // if user captured image
        else if(requestCode == 200 && resultCode == Activity.RESULT_OK){
            /*
                Log.d("mm","gowa el if")
                // save the captured image in bitmap to use in predicting
                bitmap = data?.extras?.get("data") as Bitmap
                // show the captured image in the img view
                img_view.setImageBitmap(bitmap)

                //predict()

             */
            bitmap = data?.extras?.get("data") as Bitmap
            img_view.setImageBitmap(bitmap)
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            val byteimg = stream.toByteArray()
            uploadCapturedImg(byteimg)
            predict()
        }

    }

    private fun uploadCapturedImg(byteimg: ByteArray) {
        val riversRef: StorageReference = storageRefrence.child("images/" +  "captured.jpeg")

        riversRef.putBytes(byteimg)
    }


    private fun predict() {
        //Thread.sleep(8_000)
        downloadAudio_className(classesaudio)
        //hearDescription()
        //startRecording()
        //uploadAudio()
        //Thread.sleep(8_000)
        //downloadAudio_description(descriptionaudio)


    }

    val descriptionaudio= "description_audios/product_decription.wav"
    val classesaudio= "classes_audios/class_name.wav"

    private fun downloadAudio_className(path:String) {
        Thread.sleep(10_000)
        val storageRef = FirebaseStorage.getInstance().reference.child(path)
        val localfile = File.createTempFile("tempAudio","wav")

        storageRef.getFile(localfile).addOnSuccessListener {
            mediaPlayer = MediaPlayer()
            mediaPlayer!!.setAudioStreamType(AudioManager.STREAM_MUSIC)
            try {
                mediaPlayer!!.setDataSource(localfile.path)
                mediaPlayer!!.prepare()
                mediaPlayer!!.start()
                Thread.sleep(2_000)
                mediaPlayer1 = MediaPlayer()
                mediaPlayer1!!.setAudioStreamType(AudioManager.STREAM_MUSIC)
                val audio = getAssets().openFd("download.wav")
                mediaPlayer1!!.setDataSource(audio.getFileDescriptor(),audio.getStartOffset(),audio.getLength())
                mediaPlayer1!!.prepare()
                mediaPlayer1!!.start()
                Thread.sleep(6_000)
                startRecording()
            }catch (e: IOException){
                e.printStackTrace() }
            //Toast.makeText(this, "Playing Audio", Toast.LENGTH_LONG).show()
        }
            .addOnFailureListener{
                Toast.makeText(this, "failed to retrieve audio", Toast.LENGTH_LONG).show()
            }
    }

    private fun downloadAudio_description(path:String) {
        Thread.sleep(30_000)

        val storageRef = FirebaseStorage.getInstance().reference.child(path)
        val localfile = File.createTempFile("tempAudio","wav")

        storageRef.getFile(localfile).addOnSuccessListener {
            mediaPlayer = MediaPlayer()
            mediaPlayer!!.setAudioStreamType(AudioManager.STREAM_MUSIC)
            try {
                mediaPlayer!!.setDataSource(localfile.path)
                mediaPlayer!!.prepare()
                mediaPlayer!!.start()
            }catch (e: IOException){
                e.printStackTrace() }
            //Toast.makeText(this, "Playing Audio", Toast.LENGTH_LONG).show()
        }
            .addOnFailureListener{
                Toast.makeText(this, "failed to retrieve description", Toast.LENGTH_LONG).show()
            }
    }

    private fun welcomeAudio() {
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


    private fun hearDescription(){
        Thread.sleep(2_000)
        mediaPlayer1 = MediaPlayer()
        mediaPlayer1!!.setAudioStreamType(AudioManager.STREAM_MUSIC)
        val audio = getAssets().openFd("download.wav")
        mediaPlayer1!!.setDataSource(audio.getFileDescriptor(),audio.getStartOffset(),audio.getLength())
        mediaPlayer1!!.prepare()
        mediaPlayer1!!.start()
        //startRecording(recFileName)
        //uploadAudio()
    }


    private fun uploadPicture() {
        //val randomKey: String = UUID.randomUUID().toString()
        val riversRef: StorageReference = storageRefrence.child("images/" +  "image.jpeg")
        riversRef.putFile(imageUri)
    }



    private fun uploadAudio() {

        //val randomKey: String = UUID.randomUUID().toString()
        val riversRef: StorageReference = storageRefrence.child("user_response/" +  "response.mp3")
        audioUri = Uri.fromFile(File(recFileName))

        riversRef.putFile(audioUri)
        Thread.sleep(8_000)
        downloadAudio_description(descriptionaudio)
    }


    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS){
            val result = tts.setLanguage(Locale.US)
        }
    }


    private fun startRecording() {
        // initialize and configure MediaRecorder
        recorder = MediaRecorder()
        recorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)
        recorder!!.setOutputFile(recFileName)
        recorder!!.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        recorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        try {
            recorder!!.prepare()
        } catch (e: IOException) {
            // handle error
        } catch (e: IllegalStateException) {
            // handle error
        }
        recorder!!.start()
        Toast.makeText(this, "recording", Toast.LENGTH_SHORT).show()
        Thread.sleep(3_000)
        stopRecording()
    }

    private fun stopRecording() {
        // stop recording and free up resources
        recorder!!.stop()
        recorder!!.release()
        recorder = null
        Toast.makeText(this, "record stopped", Toast.LENGTH_SHORT).show()
        //startPlay()
        uploadAudio()
    }
/*
    private fun startPlay() {
         player = MediaPlayer().apply {
             try{
                 setDataSource(recFileName)
                 prepare()
                 start()

             }catch (e: IOException){
                 Log.e("err", "Prepare audio failed")
             }

         }
         Toast.makeText(this, "playing record", Toast.LENGTH_SHORT).show()

     }
*/

}



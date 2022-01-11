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
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import com.example.imageclassification.ml.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.util.*

import java.util.jar.Manifest
import com.google.android.gms.tasks.OnFailureListener

import com.google.firebase.storage.UploadTask

import com.google.android.gms.tasks.OnSuccessListener
import java.io.File
import java.io.IOException
import java.text.BreakIterator
import android.media.MediaRecorder
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.lang.IllegalStateException
import android.Manifest.permission.RECORD_AUDIO
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.os.Environment
import android.widget.*


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

        recFileName = "${externalCacheDir?.absolutePath}/response.wav"

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
                predict()

            }

        }
        // if user captured image
        else if(requestCode == 200 && resultCode == Activity.RESULT_OK){
            img_view.setImageURI(data?.data)
            if (data != null) {
                Log.d("mm","gowa el if")
                // save the captured image in bitmap to use in predicting
                //bitmap = data?.extras?.get("data") as Bitmap
                // show the captured image in the img view
                imageUri = data.getExtras()?.get("data") as Uri
                //imageUri = data.getData()!!
                bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, imageUri)
                //img_view.setImageBitmap(bitmap)

                predict()
            }
        }

    }
    private fun predict() {
        uploadPicture()
        Thread.sleep(8_000)
        downloadAudio()
        startRecording()
        uploadAudio()
    }

    private fun downloadAudio() {

            val storageRef = FirebaseStorage.getInstance().reference.child("classes_audios/class_name.wav")
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

                            //startRecording(recFileName)
                            //uploadAudio()

                        }catch (e: IOException){
                            e.printStackTrace()
                        }
                        //Toast.makeText(this, "Playing Audio", Toast.LENGTH_LONG).show()

                }

                    .addOnFailureListener{
                        Toast.makeText(this, "failed to retrieve audio", Toast.LENGTH_LONG).show()

                    }


    }


    private fun uploadPicture() {

        //val randomKey: String = UUID.randomUUID().toString()
        val riversRef: StorageReference = storageRefrence.child("images/" +  "image.jpeg")

        riversRef.putFile(imageUri)
    }



    private fun uploadAudio() {

        //val randomKey: String = UUID.randomUUID().toString()
        val riversRef: StorageReference = storageRefrence.child("user_response/" +  "response.wav")
        audioUri = Uri.fromFile(File(recFileName))

        riversRef.putFile(audioUri)
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
        recorder!!.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
        recorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
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
    }

   /* private fun startPlay() {
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

    } */


}





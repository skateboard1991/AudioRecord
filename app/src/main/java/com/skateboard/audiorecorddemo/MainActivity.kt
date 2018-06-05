package com.skateboard.audiorecorddemo

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import kotlinx.android.synthetic.main.activity_main.*
import java.io.File

class MainActivity : AppCompatActivity()
{

    private var isRecording = false

    private val audioRecorder = AudioRecorder()

    private val audioPlayer=AudioPlayer()

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        audioPlayer.prepare()

        audioRecorder.prepare(null)

        recordBtn.setOnClickListener {

            isRecording = if (isRecording)
            {
                audioRecorder.stopRecord()
                false
            } else
            {
                audioRecorder.startRecord()
                true
            }

        }

        playBtn.setOnClickListener {

            audioPlayer.play(File(Environment.getExternalStorageDirectory().absolutePath+File.separator+"recordDemo","audio.pcm"))
        }

    }

}

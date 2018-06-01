package com.skateboard.audiorecorddemo

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity()
{

    private var isRecording = false

    private val audioRecorder = AudioRecorder()

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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

    }


    override fun onDestroy()
    {
        super.onDestroy()
        audioRecorder.release()
    }
}

package com.skateboard.audiorecorddemo

import android.media.MediaMuxer
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

    private val DIR = "recordDemo"

    private val FILE_NAME = "audio.mp4"

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        audioPlayer.prepare()

        val mediaMuxer=MediaMuxer(generateFilePath(),MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

        val audioEncoderCore=AudioEncoderCore(mediaMuxer)

        audioEncoderCore.prepare(128000)

        recordBtn.setOnClickListener {

            isRecording = if (isRecording)
            {
                audioEncoderCore.release()
                recordBtn.text="record"
                false
            } else
            {
                audioEncoderCore.startRecord()
                recordBtn.text="stop"
                true
            }

        }

//        playBtn.setOnClickListener {
//
//            audioPlayer.play(File(Environment.getExternalStorageDirectory().absolutePath+File.separator+"recordDemo","audio.pcm"))
//        }

    }

    private fun generateFilePath():String
    {
        val dir = File(Environment.getExternalStorageDirectory().absolutePath, DIR)
        if (!dir.exists())
        {
            dir.mkdir()
        }
        return File(dir, FILE_NAME).absolutePath
    }

    override fun onDestroy()
    {
        super.onDestroy()
        audioRecorder.destroy()
    }
}

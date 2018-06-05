package com.skateboard.audiorecorddemo

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.File
import java.io.FileInputStream

class AudioPlayer
{
    private lateinit var audioTrack: AudioTrack

    private var minBufferSize = 1024

    fun prepare()
    {

        minBufferSize = AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT)

        audioTrack = AudioTrack(AudioManager.STREAM_MUSIC, 44100, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT, minBufferSize, AudioTrack.MODE_STREAM)

    }


    fun play(file: File)
    {
        val inputStream = DataInputStream(BufferedInputStream(FileInputStream(file)))
        val data = ByteArray(file.length().toInt())

        var count = 0
        while (inputStream.available() > 0 && count < data.size)
        {
            data[count] = inputStream.readByte()
            count++
        }


        audioTrack.play()
        audioTrack.write(data, 0, data.size)
        audioTrack.stop()
        audioTrack.release()
    }

}
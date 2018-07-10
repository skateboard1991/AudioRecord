package com.skateboard.audiorecorddemo

import android.media.*
import android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM
import android.os.Build
import android.util.Log
import java.nio.ByteBuffer
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.Executors

class AudioEncoderCore(private val mediaMuxer: MediaMuxer)
{
    private lateinit var audioCodec: MediaCodec

    private lateinit var bufferInfo: MediaCodec.BufferInfo

    private var trackIndex = -1

    private var isEncoding = false

    private var lastPreTime = -1L

    private var audioRecord: AudioRecord? = null

    private var minBufferSize = 9000

    private val sampleRateSize = 44100

    private val executors = Executors.newFixedThreadPool(2)

    private val TAG = "AudioEncoderCore"

    private var prevOutputPTSUs=0L

    private val blockingQueue=ArrayBlockingQueue<ByteArray>(100)

    fun prepare(bitrate: Int)
    {
        prepareAudioRecord()
        prepareAudioCodec(bitrate, sampleRateSize)
    }

    private fun prepareAudioRecord()
    {
        minBufferSize = AudioRecord.getMinBufferSize(sampleRateSize, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRateSize, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBufferSize)
    }


    private fun prepareAudioCodec(bitrate: Int, sampleRate: Int)
    {
        bufferInfo = MediaCodec.BufferInfo()
        val mediaFormat = MediaFormat()
        mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
        mediaFormat.setString(MediaFormat.KEY_MIME, getMinType())
        mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_MASK, AudioFormat.CHANNEL_IN_MONO)
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
        mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 2)
        mediaFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, sampleRate)
        mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 9000)
        audioCodec = MediaCodec.createEncoderByType(getMinType())
        audioCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        audioCodec.start()
    }

    private fun getMinType(): String
    {
        return if (Build.VERSION.SDK_INT >= 21)
        {
            MediaFormat.MIMETYPE_AUDIO_AAC
        }
        else
        {
            "audio/mp4a-latm"
        }
    }

    private val encodeRunnable= Runnable {

        while(isEncoding)
        {
            val data=blockingQueue.poll()
            if(data!=null)
            {
                drainEncoder(data)
            }
        }
        drainEncoder(null)
        audioCodec.stop()
        audioCodec.release()
        mediaMuxer.stop()
        mediaMuxer.release()
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }


    private val recordRunnable = Runnable {
        val data = ByteArray(minBufferSize)
        audioRecord?.startRecording()
        while (isEncoding)
        {
            val size=audioRecord?.read(data, 0, data.size)?:0
            if(size>0)
            {
                blockingQueue.offer(data)
            }

        }

    }

    protected fun getPTSUs(): Long
    {
        var result = System.nanoTime() / 1000L
        // presentationTimeUs should be monotonic
        // otherwise muxer fail to write
        if (result < prevOutputPTSUs)
            result = prevOutputPTSUs - result + result
        return result
    }

    fun startRecord()
    {
        if (!isEncoding)
        {
            isEncoding = true
            executors.execute(recordRunnable)
            executors.execute(encodeRunnable)
            //            executors.shutdown()
        }
    }

    fun drainEncoder(data: ByteArray?)
    {

        var isEOS = false
        while (true)
        {
            if (!isEOS)
            {
                val inIndex = audioCodec.dequeueInputBuffer(-1)
                if (inIndex >= 0)
                {
                    if (data == null)
                    {
                        isEOS = true
                        audioCodec.queueInputBuffer(inIndex, 0, 0, System.nanoTime()/1000, BUFFER_FLAG_END_OF_STREAM)

                    } else
                    {
                        val inBuffer = getInBuffer(inIndex)
                        inBuffer.clear()
                        inBuffer.put(data)
                        audioCodec.queueInputBuffer(inIndex, 0, data.size, System.nanoTime() / 1000, 0)
                    }

                } else if (inIndex == MediaCodec.INFO_TRY_AGAIN_LATER)
                {


                }
            }


            var outIndex = audioCodec.dequeueOutputBuffer(bufferInfo, 0)
            while (outIndex >= 0)
            {

                if (lastPreTime == -1L)
                {
                    lastPreTime = bufferInfo.presentationTimeUs
                } else if (lastPreTime < bufferInfo.presentationTimeUs)
                {
                    lastPreTime = bufferInfo.presentationTimeUs
                }
                if (bufferInfo.size != 0 && lastPreTime <= bufferInfo.presentationTimeUs)
                {
                    val outBuffer = getOutBuffer(outIndex)
                    outBuffer.position(bufferInfo.offset)
                    outBuffer.limit(bufferInfo.offset + bufferInfo.size)
//                    if(mediaMuxer.isStarting())
//                    {
                        mediaMuxer.writeSampleData(trackIndex, outBuffer, bufferInfo)
//                    }
//                    else
//                    {
//                        Log.d(TAG,"lost frame")
//                    }
                }
                audioCodec.releaseOutputBuffer(outIndex, false)
                outIndex = audioCodec.dequeueOutputBuffer(bufferInfo, 0)
            }

            if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED)
            {
                trackIndex = mediaMuxer.addTrack(audioCodec.outputFormat)
                mediaMuxer.start()
            }
            if (outIndex == MediaCodec.INFO_TRY_AGAIN_LATER)
            {
                if (!isEOS)
                {
                    break
                }
            }

            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0)
            {

                isEncoding = false

                break
            }


        }

    }

    fun release()
    {
        isEncoding = false
    }


    private fun getInBuffer(index: Int): ByteBuffer
    {
        return if (Build.VERSION.SDK_INT >= 21)
        {
            audioCodec.getInputBuffer(index)
        }
        else
        {
            audioCodec.inputBuffers[index]
        }
    }

    private fun getOutBuffer(index: Int): ByteBuffer
    {
        return if (Build.VERSION.SDK_INT >= 21)
        {
            audioCodec.getOutputBuffer(index)
        }
        else
        {
            audioCodec.outputBuffers[index]
        }
    }
}
package com.google.firebase.example.friendlymeals.ui.recipe.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import android.util.Log

class PcmAudioPlayer {
    private var audioTrack: AudioTrack? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isPaused = false

    fun play(pcmData: ByteArray, onCompletion: () -> Unit) {
        stop()

        if (pcmData.isEmpty()) {
            onCompletion()
            return
        }

        try {
            val sampleRate = 24000
            val channelConfig = AudioFormat.CHANNEL_OUT_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            val frameSize = 2 // 16-bit mono = 2 bytes per frame
            val frameCount = pcmData.size / frameSize

            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(audioFormat)
                        .setSampleRate(sampleRate)
                        .setChannelMask(channelConfig)
                        .build()
                )
                .setBufferSizeInBytes(pcmData.size)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onMarkerReached(track: AudioTrack?) {
                    mainHandler.post {
                        onCompletion()
                    }
                }

                override fun onPeriodicNotification(track: AudioTrack?) {}
            }, mainHandler)

            track.notificationMarkerPosition = frameCount

            track.write(pcmData, 0, pcmData.size)
            track.play()
            audioTrack = track
            isPaused = false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize or play AudioTrack", e)
            onCompletion()
        }
    }

    fun pause() {
        try {
            if (audioTrack?.playState == AudioTrack.PLAYSTATE_PLAYING) {
                audioTrack?.pause()
                isPaused = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing AudioTrack", e)
        }
    }

    fun resume() {
        try {
            if (audioTrack?.playState == AudioTrack.PLAYSTATE_PAUSED) {
                audioTrack?.play()
                isPaused = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming AudioTrack", e)
        }
    }

    fun stop() {
        try {
            audioTrack?.let {
                if (it.playState != AudioTrack.PLAYSTATE_STOPPED) {
                    it.stop()
                }
                it.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AudioTrack", e)
        } finally {
            audioTrack = null
            isPaused = false
        }
    }

    fun isPlaying(): Boolean {
        return audioTrack?.playState == AudioTrack.PLAYSTATE_PLAYING
    }

    fun isPaused(): Boolean {
        return isPaused
    }

    fun release() {
        stop()
    }

    companion object {
        private const val TAG = "PcmAudioPlayer"
    }
}

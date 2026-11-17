package com.example.ajustadordevolumen.ui.main

import com.example.ajustadordevolumen.audio.AudioStreamInfo
import com.example.ajustadordevolumen.audio.StreamCategory
import junit.framework.TestCase.assertEquals
import org.junit.Test

class AudioStreamModelTest {
  @Test
  fun percentage_calculatesCorrectly() {
    val stream = AudioStreamInfo(
      streamType = 3,
      category = StreamCategory.MEDIA,
      title = "Multimedia",
      subtitle = "Apps",
      currentVolume = 5,
      minVolume = 0,
      maxVolume = 10,
      isMuted = false
    )
    assertEquals(50, stream.percentage)
  }
}

package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ml.AnomalyDetectionEngine
import com.example.ml.MultilingualNLP
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("BudgetMate", appName)
  }

  @Test
  fun `test MultilingualNLP parsing for mixed Tamil and English`() {
    val result = MultilingualNLP.parseInput("Inniku food-ku 250 rupees spend panninen")
    assertEquals("EXPENSE", result.type)
    assertEquals(250.0, result.amount, 0.01)
    assertEquals("Food", result.category)
  }

  @Test
  fun `test MultilingualNLP parsing for Salary income`() {
    val result = MultilingualNLP.parseInput("Salary 25000 vandhudhu")
    assertEquals("INCOME", result.type)
    assertEquals(25000.0, result.amount, 0.01)
    assertEquals("Salary", result.category)
  }
}


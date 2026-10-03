package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.ml.AITrainedModelManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AITrainedModelTest {

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        AITrainedModelManager.initialize(context)
    }

    @Test
    fun testModelLoadsFromAssets() {
        assertTrue("Model should be loaded from packaged assets", AITrainedModelManager.isModelReady())
        assertEquals("2.1.0", AITrainedModelManager.getModelVersion())
        assertNotNull(AITrainedModelManager.getModelName())
    }

    @Test
    fun testExpensePredictionFood() {
        val pred = AITrainedModelManager.predict("Bought special meals and dosa at Saravana Bhavan")
        assertEquals("Food", pred.category)
        assertTrue(pred.confidence >= 0.70f)
    }

    @Test
    fun testExpensePredictionGroceries() {
        val pred = AITrainedModelManager.predict("DMart monthly kirana atta rice oil provisions")
        assertEquals("Groceries", pred.category)
        assertTrue(pred.confidence >= 0.70f)
    }

    @Test
    fun testExpensePredictionPetrol() {
        val pred = AITrainedModelManager.predict("HP Auto petrol bunk fuel refill")
        assertEquals("Petrol", pred.category)
        assertTrue(pred.confidence >= 0.70f)
    }

    @Test
    fun testExpensePredictionMultilingualHindi() {
        val pred = AITrainedModelManager.predict("doodh aur sabji kharida")
        assertEquals("Groceries", pred.category)
        assertTrue(pred.confidence >= 0.85f)
    }
}

package com.appVanta.i230017_i230507

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MessagingTest {

    private var scenario: ActivityScenario<ChatActivity>? = null

    @Before
    fun setUp() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
        val context = ApplicationProvider.getApplicationContext<Context>()
        RecentLoginStore.save(context, "Test User", "testuser@kinnect.app", "test_auth_token_123")
    }

    @After
    fun tearDown() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
        scenario?.close()
    }

    @Test
    fun testCriticalMessagingWorkflow() {
        // Step 1: Open a conversation
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(context, ChatActivity::class.java).apply {
            putExtra("uid", "test_user_789")
        }
        scenario = ActivityScenario.launch(intent)

        // Step 2: Enter a message
        val testMsgText = "Espresso test message ${System.currentTimeMillis()}"
        onView(withId(R.id.chatInput)).perform(typeText(testMsgText), closeSoftKeyboard())

        // Step 3: Send the message
        onView(withId(R.id.btnSend)).perform(click())

        // Step 4: Verify the message appears in the conversation
        onView(withText(testMsgText)).check(matches(isDisplayed()))
    }
}

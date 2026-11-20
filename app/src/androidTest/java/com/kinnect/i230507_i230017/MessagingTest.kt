package com.kinnect.i230507_i230017
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
@RunWith(AndroidJUnit4::class)
class MessagingTest {
    @Test fun queuedMessageSurvivesScreenRecreationWithoutDuplication() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        synchronized(DataRepository){DataRepository.db.clear()}
        // Fixture credentials intentionally cannot authenticate with a real backend.
        RecentLoginStore.save(context,"Espresso User","espresso@example.invalid","invalid-test-token","999999",System.currentTimeMillis()+3600000,true)
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
        try {
            ActivityScenario.launch<ChatActivity>(Intent(context,ChatActivity::class.java).putExtra("uid","888888")).use {scenario ->
                val text="Durable queued message"
                onView(withId(R.id.chatInput)).perform(replaceText(text),closeSoftKeyboard())
                onView(withId(R.id.btnSend)).perform(click())
                onView(withText(text)).check(matches(isDisplayed()))
                val before=DataRepository.db.actions().filter{it.endpoint=="message_send"}
                assertEquals(1,before.size)
                val id=before.single().uuid
                scenario.recreate()
                onView(withText(text)).check(matches(isDisplayed()))
                val after=DataRepository.db.actions().filter{it.endpoint=="message_send"}
                assertEquals(1,after.size);assertEquals(id,after.single().uuid)
            }
        }finally{IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource);RecentLoginStore.clear(context)}
    }
}

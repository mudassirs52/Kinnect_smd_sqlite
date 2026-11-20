package com.kinnect.i230507_i230017
import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
@RunWith(AndroidJUnit4::class)
class AuthenticationTest {
    @Test fun rejectedLoginNeverCreatesAnOfflineSession() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        RecentLoginStore.clear(context)
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
        try {
            ActivityScenario.launch(LoginActivity::class.java).use {
                onView(withId(R.id.etEmail)).perform(replaceText("unregistered-espresso@example.invalid"))
                onView(withId(R.id.etPassword)).perform(replaceText("WrongPassword123!"),closeSoftKeyboard())
                onView(withId(R.id.btnLogin)).perform(click())
                onView(withId(R.id.btnLogin)).check(matches(isDisplayed()))
                assertNull(RecentLoginStore.getToken(context))
            }
        }finally{IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)}
    }
}

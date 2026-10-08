package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AccountType
import com.example.data.model.UserRole
import com.example.data.repository.ChatRepository
import com.example.data.storage.DataStorageManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("MVA Business Chat", appName)
  }

  @Test
  fun `verify production authentication and local persistence`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val testDispatcher = UnconfinedTestDispatcher()
    val testScope = TestScope(testDispatcher)

    val storage = DataStorageManager(context)
    storage.clearSession()

    // Fresh production repository starts unauthenticated
    val repo1 = ChatRepository(testScope, context)
    assertFalse("Initial production state should be unauthenticated", repo1.isGoogleSignedIn.value)

    // User signs in with real Google identity
    repo1.signInWithGoogle(
      name = "Austin Cooper",
      email = "austin.cooper@enterprise.com",
      username = "austin_c",
      accountType = AccountType.BUSINESS,
      company = "Cooper Enterprise Solutions",
      title = "Managing Director"
    )

    assertTrue("User should be signed in after Google authentication", repo1.isGoogleSignedIn.value)
    assertEquals("Austin Cooper", repo1.currentUser.value.name)
    assertEquals("austin_c", repo1.currentUser.value.username)
    assertEquals("austin.cooper@enterprise.com", repo1.currentUser.value.email)
    assertEquals(UserRole.ADMIN, repo1.currentUser.value.role)

    // Verify persistence across app restart (new repository instance with same context)
    val repo2 = ChatRepository(testScope, context)
    assertTrue("Persisted session should restore signed-in state on launch", repo2.isGoogleSignedIn.value)
    assertEquals("Austin Cooper", repo2.currentUser.value.name)
    assertEquals("austin_c", repo2.currentUser.value.username)

    // Test sign out
    repo2.signOutGoogle()
    assertFalse("Signed out state should persist", repo2.isGoogleSignedIn.value)

    val repo3 = ChatRepository(testScope, context)
    assertFalse("New instance after signout should be unauthenticated", repo3.isGoogleSignedIn.value)
  }
}

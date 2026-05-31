package com.coreclean.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * JUnit rule that replaces Dispatchers.Main with a [StandardTestDispatcher].
 *
 * The exposed [testScheduler] should be passed to [kotlinx.coroutines.test.runTest]
 * so both the ViewModel's viewModelScope and the test coroutines share the same
 * virtual clock, enabling [kotlinx.coroutines.test.advanceUntilIdle] to work correctly.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testScheduler: TestCoroutineScheduler = TestCoroutineScheduler()
) : TestWatcher() {
    val dispatcher: TestDispatcher = StandardTestDispatcher(testScheduler)

    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}

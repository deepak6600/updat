package com.safe.setting.app.ui.activities.login

import android.content.Context
import dagger.hilt.android.qualifiers.ActivityContext
import androidx.fragment.app.FragmentManager
import com.safe.setting.app.R
import com.safe.setting.app.data.rxFirebase.InterfaceFirebase
import com.safe.setting.app.ui.activities.base.BaseInteractor
// Migrate thread switching to coroutines; keep Rx facade temporarily
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.rx3.await
import javax.inject.Inject

class InteractorLogin<V : InterfaceViewLogin> @Inject constructor(
    supportFragment: FragmentManager,
    @ActivityContext context: Context,
    firebase: InterfaceFirebase
) : BaseInteractor<V>(supportFragment, context, firebase), InterfaceInteractorLogin<V> {

    override fun signInDisposable(email: String, pass: String) {
        val scope = CoroutineScope(kotlinx.coroutines.Dispatchers.Main)
        val job: Job = scope.launch {
            getView()?.showProgressDialog(null, getContext().getString(R.string.logging_in))
            flow {
                val result = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    firebase().signIn(email, pass).blockingGet()
                }
                emit(result?.user != null)
            }
                .retryWhen { cause, attempt ->
                    val baseDelayMs = 500L
                    val multiplier = 2.0
                    val maxDelayMs = 10_000L
                    val jitterPct = 0.2
                    if (attempt < 3) {
                        val computed = (baseDelayMs * Math.pow(multiplier, (attempt).toDouble())).toLong()
                        val delay = kotlin.math.min(maxDelayMs, computed)
                        val jitter = (delay * jitterPct).toLong()
                        val jittered = delay + (-jitter..jitter).random()
                        kotlinx.coroutines.delay(kotlin.math.max(0L, jittered))
                        true
                    } else false
                }
                .catch { e -> if (isNullView()) getView()?.failedResult(e) }
                .collect { ok -> if (isNullView()) getView()?.successResult(ok) }
        }
        // Disposable bridge removed; coroutines managed by scope
    }
}

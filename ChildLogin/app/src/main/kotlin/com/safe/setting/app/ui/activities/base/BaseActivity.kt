package com.safe.setting.app.ui.activities.base

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.viewbinding.ViewBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
// Removed RxBinding; using callbackFlow for text changes
import android.text.TextWatcher
import android.text.Editable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.rx3.asFlow
import kotlinx.coroutines.rx3.asObservable
import kotlinx.coroutines.FlowPreview
import com.safe.setting.app.R
import com.safe.setting.app.ui.fragments.base.BaseFragment
import com.safe.setting.app.ui.widget.toolbar.CustomToolbar
import com.safe.setting.app.utils.ConstFun.adjustFontScale
import com.safe.setting.app.utils.Consts.TEXT
import io.reactivex.rxjava3.core.Observable

@OptIn(FlowPreview::class)
abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity(),
    InterfaceView, BaseFragment.Callback {

    private var currentDialog: AlertDialog? = null
    // CompositeDisposable removed; using lifecycleScope
    private lateinit var snackbar: Snackbar

    private lateinit var emailObservable: Observable<Boolean>
    private lateinit var passObservable: Observable<Boolean>
    private lateinit var signInEnabled: Observable<Boolean>
    protected lateinit var binding: VB

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = instanceViewBinding()
        setContentView(binding.root)
        // CompositeDisposable initialization removed
        adjustFontScale()
    }

    abstract fun instanceViewBinding(): VB

    override fun showProgressDialog(title: String?, message: String) {
        hideDialog()
        currentDialog = MaterialAlertDialogBuilder(this).apply {
            setTitle(title)
            setMessage(message)
            setCancelable(false)
        }.show()
    }

    override fun showDialog(
        title: String,
        message: String,
        positiveButtonText: String?,
        positiveAction: (() -> Unit)?,
        negativeButtonText: String?,
        isCancelable: Boolean
    ) {
        hideDialog()
        currentDialog = MaterialAlertDialogBuilder(this).apply {
            setTitle(title)
            setMessage(message)
            setCancelable(isCancelable)
            positiveButtonText?.let {
                setPositiveButton(it) { dialog, _ ->
                    positiveAction?.invoke()
                    dialog.dismiss()
                }
            }
            negativeButtonText?.let {
                setNegativeButton(it) { dialog, _ -> dialog.dismiss() }
            }
        }.show()
    }

    override fun hideDialog() {
        currentDialog?.dismiss()
        currentDialog = null
    }

    override fun showError(message: String) {
        showDialog(getString(R.string.ops), message, getString(android.R.string.ok))
    }

    override fun showMessage(message: Int) {
        showMessage(getString(message))
    }

    override fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun showSnackbar(message: Int, v: View) {
        snackbar = Snackbar.make(v, message, Snackbar.LENGTH_LONG)
            .setAction(android.R.string.ok) { snackbar.dismiss() }
        snackbar.show()
    }

    override fun showSnackbar(message: String, v: View) {
        snackbar = Snackbar.make(v, message, Snackbar.LENGTH_INDEFINITE)
            .setAction(android.R.string.ok) { snackbar.dismiss() }
        snackbar.show()
    }

    // Disposable management removed from BaseActivity

    fun newChildValidationObservable(newChild: EditText): io.reactivex.rxjava3.disposables.Disposable {
        // Maintain signature by returning a no-op Disposable; internally use Flow
        val job = callbackFlow<CharSequence> {
            val watcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { trySend(s ?: "") }
                override fun afterTextChanged(s: Editable?) {}
            }
            newChild.addTextChangedListener(watcher)
            awaitClose { newChild.removeTextChangedListener(watcher) }
        }
            .debounce(300)
            .map { textNewChild -> TEXT.matcher(textNewChild).matches() }
            .distinctUntilChanged()
            .map { b -> if (b) R.drawable.ic_child_care else R.drawable.ic_child_care_black_24dp }
            .onEach { id -> newChild.setCompoundDrawablesWithIntrinsicBounds(id, 0, 0, 0) }
            .launchIn(lifecycleScope)

        return object : io.reactivex.rxjava3.disposables.Disposable {
            override fun dispose() { job.cancel() }
            override fun isDisposed(): Boolean = job.isCancelled
        }
    }

    private fun textChangesFlow(editText: EditText): Flow<CharSequence> = callbackFlow {
        val watcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { trySend(s ?: "") }
            override fun afterTextChanged(s: Editable?) {}
        }
        editText.addTextChangedListener(watcher)
        awaitClose { editText.removeTextChangedListener(watcher) }
    }

    fun emailValidationObservable(edtEmail: EditText) {
        val emailFlow = textChangesFlow(edtEmail)
            .debounce(300)
            .map { textEmail -> Patterns.EMAIL_ADDRESS.matcher(textEmail).matches() }
        emailObservable = emailFlow.asObservable()
        emailObservable(edtEmail)
    }

    private fun emailObservable(edtEmail: EditText): io.reactivex.rxjava3.disposables.Disposable {
        val job = emailObservable.distinctUntilChanged()
            .map { b -> if (b) R.drawable.ic_user else R.drawable.ic_user_alert }
            .asFlow()
            .onEach { id -> edtEmail.setCompoundDrawablesWithIntrinsicBounds(id, 0, 0, 0) }
            .launchIn(lifecycleScope)
        return object : io.reactivex.rxjava3.disposables.Disposable {
            override fun dispose() { job.cancel() }
            override fun isDisposed(): Boolean = job.isCancelled
        }
    }

    fun passValidationObservable(edtPass: EditText) {
        val passFlow = textChangesFlow(edtPass)
            .debounce(300)
            .map { textPass -> textPass.length > 5 }
        passObservable = passFlow.asObservable()
        passObservable(edtPass)
    }

    private fun passObservable(edtPass: EditText): io.reactivex.rxjava3.disposables.Disposable {
        val job = passObservable.distinctUntilChanged()
            .map { b -> if (b) R.drawable.ic_lock_open else R.drawable.ic_lock }
            .asFlow()
            .onEach { id -> edtPass.setCompoundDrawablesWithIntrinsicBounds(id, 0, 0, 0) }
            .launchIn(lifecycleScope)
        return object : io.reactivex.rxjava3.disposables.Disposable {
            override fun dispose() { job.cancel() }
            override fun isDisposed(): Boolean = job.isCancelled
        }
    }

    fun signInValidationObservable(btnSignIn: Button) {
        // Bridge Observable.combineLatest to Flow.combine for internal processing
        val emailFlow: Flow<Boolean> = emailObservable.asFlow()
        val passFlow: Flow<Boolean> = passObservable.asFlow()
        val job = combine(emailFlow, passFlow) { email, pass -> email && pass }
            .distinctUntilChanged()
            .onEach { enabled ->
                btnSignIn.isEnabled = enabled
                val colorResId = if (enabled) R.color.dark_accent else R.color.colorTextDisabled
                val color = ContextCompat.getColorStateList(this, colorResId)
                btnSignIn.backgroundTintList = color
            }
            .launchIn(lifecycleScope)
        signInEnabled = Observable.combineLatest(emailObservable, passObservable) { e, p -> e && p }
        // Maintain return/disposable behavior
    }

    private fun signInEnableObservable(btnSignIn: Button): io.reactivex.rxjava3.disposables.Disposable {
        val job = signInEnabled.distinctUntilChanged()
            .doOnNext { enabled ->
                btnSignIn.isEnabled = enabled
                val colorResId = if (enabled) R.color.dark_accent else R.color.colorTextDisabled
                val color = ContextCompat.getColorStateList(this, colorResId)
                btnSignIn.backgroundTintList = color
            }
            .subscribe()
        return job
    }

    override fun setActionToolbar(action: Boolean) {}
    override fun successResult(result: Boolean, filter: Boolean) {}
    override fun failedResult(throwable: Throwable) {}
    override fun onItemClick(key: String?, child: String, file: String, position: Int) {}
    override fun onItemLongClick(key: String?, child: String, file: String, position: Int) {}
    override fun setDrawerLock() {}
    override fun setDrawerUnLock() {}
    override fun openDrawer() {}
    override fun setMenu(menu: PopupMenu?) {}
    override fun changeChild(fragmentTag: String) {}
    override fun setToolbar(toolbar: CustomToolbar, showSearch: Boolean, title: Int, showItemMenu: Int, hint: Int) {}
}

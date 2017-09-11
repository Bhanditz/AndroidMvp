package com.idapgroup.android.mvp.impl

import android.os.Bundle
import android.support.v4.app.DialogFragment
import com.idapgroup.android.mvp.MvpPresenter

abstract class BasePresenterDialogFragment<V, out P : MvpPresenter<V>> : DialogFragment() {
    val KEY_FRAGMENT_ID = "fragment_id"

    private lateinit var presenterDelegate: PresenterDelegate<V, P>

    /**
     * Creates a Presenter when needed.
     * This instance should not contain explicit or implicit reference for [android.app.Activity] context
     * since it will be keep on rotations.
     */
    abstract fun onCreatePresenter(): P

    val presenter: P
        get() = presenterDelegate.presenter

    /** Override in case of activity not implementing Presenter<View> interface <View> */
    @Suppress("UNCHECKED_CAST")
    open val presenterView: V
        get() = this as V

    /** Indicates to retain or not presenter when activity configuration changing */
    open val retainPresenter = false

    /** Usefully for flexible view attach and detach handle */
    open var manualViewAttach = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val fragmentId = savedInstanceState?.getString(KEY_FRAGMENT_ID)
        if(fragmentId != null && retainedPresenterDelegates.isNotEmpty()) {
            // Restore previously preserved presenter when configuration change
            @Suppress("UNCHECKED_CAST")
            presenterDelegate = retainedPresenterDelegates[fragmentId] as PresenterDelegate<V, P>
            retainedPresenterDelegates.remove(fragmentId)
        } else {
            presenterDelegate = PresenterDelegate(onCreatePresenter())
        }
        savedInstanceState?.let { presenterDelegate.onRestoreState(it) }
        presenterDelegate.onCreate()
    }

    override fun onSaveInstanceState(savedState: Bundle) {
        super.onSaveInstanceState(savedState)
        presenterDelegate.onSaveState(savedState)
        if(retainPresenter) {
            // Tmp preserve presenter when configuration change
            val fragmentId = javaClass.name + getFragmentId()
            savedState.putString(KEY_FRAGMENT_ID, fragmentId)
            retainedPresenterDelegates.put(fragmentId, presenterDelegate)
        }
    }

    override fun onResume() {
        super.onResume()
        if(!manualViewAttach) {
            presenterDelegate.attachView(presenterView)
        }
    }

    override fun onPause() {
        if(!manualViewAttach) {
            presenterDelegate.detachView()
        }
        super.onPause()
    }

    protected fun attachView() {
        if(!manualViewAttach) {
            throw IllegalStateException("Use only with manualViewAttach set to true")
        }
        presenterDelegate.attachView(presenterView)
    }

    protected fun detachView() {
        if(!manualViewAttach) {
            throw IllegalStateException("Use only with manualViewAttach set to true")
        }
        presenterDelegate.detachView()
    }

    /**
     * Override if at the same time may exist many examples of the same class.
     * May be unique only for this class of fragment not required global uniqueness
     * */
    protected open fun getFragmentId() : String = hashCode().toString()
}

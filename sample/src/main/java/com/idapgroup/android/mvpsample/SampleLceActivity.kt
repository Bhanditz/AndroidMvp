package com.idapgroup.android.mvpsample

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.idapgroup.android.mvp.impl.LcePresenterActivity

class SampleLceActivity : SampleLceMvp.View, LcePresenterActivity<SampleLceMvp.View, SampleLceMvp.Presenter>() {

    override fun showError(error: Throwable, retry: (() -> Unit)?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onCreatePresenter(): SampleLceMvp.Presenter {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onCreateContentView(inflater: LayoutInflater, container: ViewGroup): View {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
//
//    override var retainPresenter = true
//
//    var loadDialog: ProgressDialog? = null
//
//    override fun onCreatePresenter() = SampleLcePresenter()
//
//    override fun onCreateContentView(inflater: LayoutInflater, container: ViewGroup): View {
//        return inflater.inflate(R.layout.screen_sample, container, false)
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        findViewById(R.id.ask).setOnClickListener {
//            val question = (findViewById(R.id.question) as TextView).text
//            presenter.onAsk(question.toString())
//        }
//        findViewById(R.id.confirm).setOnClickListener { presenter.onConfirm() }
//
//        if (savedInstanceState != null) {
//            if(savedInstanceState.getBoolean("load_dialog_shown", false)) {
//                showLoad()
//            }
//        }
//    }
//
//    override fun onSaveInstanceState(outState: Bundle) {
//        super.onSaveInstanceState(outState)
//        outState.putBoolean("load_dialog_shown", loadDialog != null)
//    }
//
//    override fun goToMain() {
//        finish()
//    }
//
//    override fun showMessage(message: String) {
//        (findViewById(R.id.result) as TextView).text = message
//    }
//
//    override fun showLceLoad() {
//        // Because was override by SampleMvp.View interface
//        super.showLoad()
//    }
//
//    override fun showLoad() {
//        val loadDialog = ProgressDialog(this)
//        loadDialog.setMessage("Processing...")
//        loadDialog.isIndeterminate = true
//        loadDialog.show()
//        this.loadDialog = loadDialog
//    }
//
//    override fun hideLoad() {
//        loadDialog!!.hide()
//        loadDialog = null
//    }
//
//    override fun showError(error: Throwable, retry: (() -> Unit)?) {
//
//    }
}
package com.idapgroup.android.mvpsample

import com.idapgroup.android.rx_mvp.RxBasePresenter
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.IOException
import java.util.*

class SampleLcePresenter : SampleLceMvp.Presenter, RxBasePresenter<SampleLceMvp.View>() {

    init {
        setResetTaskStateAction("task_start", { initContent() })
    }

    override fun onCreate() {
        execute { initContent() }
    }

    fun initContent() { Observable.create<String> { it.setCancellable {  } }
        (view as SampleLceMvp.View).showLoad()
        Observable
                .fromCallable {
                    if(Random().nextBoolean()) {
                        Thread.sleep(1000)
                        throw IOException("OH MY GOD. We have a big problem!!!")
                    } else {
                        Thread.sleep(4000)
                    }
                }
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .taskTracker("task_start")
                .safeSubscribe({
                    (view as SampleLceMvp.View).showContent()
                }, {
                    view!!.showError(it, { initContent() })
                })
    }
}
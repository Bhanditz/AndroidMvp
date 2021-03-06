package com.idapgroup.android.rx_mvp

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.idapgroup.android.mvp.impl.Action
import com.idapgroup.android.mvp.impl.BasePresenter
import io.reactivex.*
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.internal.functions.Functions
import io.reactivex.subjects.CompletableSubject
import java.util.*

open class RxBasePresenter<V> : BasePresenter<V>() {

    // Optimization for safe subscribe
    private val ERROR_CONSUMER: (Throwable) -> Unit = { Functions.ERROR_CONSUMER.accept(it) }
    private val EMPTY_ACTION: Action = {}

    private val mainHandler = Handler(Looper.getMainLooper())
    private val activeTasks = LinkedHashMap<String, Task>()
    private val resetTaskStateActionMap = LinkedHashMap<String, Action>()
    private var isSavedState = false
    private val onDetachViewActionList = mutableListOf<Action>()
    private val onSaveStateActionList = mutableListOf<Action>()

    inner class Task(val key: String) {
        private val subTaskList = ArrayList<Disposable>()
        private var subTaskCount = 0
        private var cancelled = false
        private var completable = CompletableSubject.create()

        fun safeAddSubTask(subTask: Disposable) {
            runOnUiThread { addSubTask(subTask) }
        }

        private fun addSubTask(subTask: Disposable) {
            checkMainThread()
            subTaskList.add(subTask)
            ++subTaskCount

            if(cancelled) subTask.dispose()
        }

        fun removeSubTask() {
            checkMainThread()
            --subTaskCount

            if(subTaskCount == 0) {
                activeTasks.remove(key)
                completable.onComplete()
            }
        }

        fun cancel(): Completable {
            checkMainThread()
            val activeSubTaskList = subTaskList.filter { !it.isDisposed }
            val awaitState = await(activeSubTaskList)
            activeSubTaskList.forEach { it.dispose() }
            cancelled = true
            return awaitState
        }

        fun await(): Completable {
            val activeSubTaskList = subTaskList.filter { !it.isDisposed }
            return await(activeSubTaskList)
        }

        fun await(activeSubTaskList: List<Disposable>): Completable {
            return if(activeSubTaskList.isEmpty()) Completable.complete() else completable
        }
    }

    override fun onSaveState(savedState: Bundle) {
        super.onSaveState(savedState)
        onSaveStateActionList.forEach { it() }
        onSaveStateActionList.clear()

        savedState.putStringArrayList("task_keys", ArrayList(activeTasks.keys))
        isSavedState = true
    }

    override fun onRestoreState(savedState: Bundle) {
        super.onRestoreState(savedState)
        // Reset tasks only if presenter was destroyed
        if(!isSavedState) {
            val taskKeyList = savedState.getStringArrayList("task_keys")
            taskKeyList.forEach { resetTaskState(it) }
        }
        isSavedState = false
    }

    override fun onDetachedView() {
        super.onDetachedView()
        onDetachViewActionList.forEach { it() }
        onDetachViewActionList.clear()
    }

    /** Preserves link for task by key while it's running  */
    protected fun <T> taskTracker(taskKey: String): ObservableTransformer<T, T> {
        return ObservableTransformer { it.taskTracker(taskKey) }
    }

    /** Preserves link for task by key while it's running  */
    protected fun <T> Observable<T>.taskTracker(taskKey: String): Observable<T> {
        val task = addTask(taskKey)
        return doFinally { task.removeSubTask() }
                .doOnSubscribe { disposable -> task.safeAddSubTask(disposable) }
    }

    /** Preserves link for task by key while it's running  */
    protected fun <T> singleTaskTracker(taskKey: String): SingleTransformer<T, T> {
        return SingleTransformer { it.taskTracker(taskKey) }
    }

    /** Preserves link for task by key while it's running  */
    protected fun <T> Single<T>.taskTracker(taskKey: String): Single<T> {
        val task = addTask(taskKey)
        return doFinally { task.removeSubTask() }
                .doOnSubscribe { disposable -> task.safeAddSubTask(disposable) }
    }

    /** Preserves link for task by key while it's running  */
    protected fun completableTaskTracker(taskKey: String): CompletableTransformer {
        return CompletableTransformer { it.taskTracker(taskKey) }
    }

    /** Preserves link for task by key while it's running  */
    protected fun Completable.taskTracker(taskKey: String): Completable {
        val task = addTask(taskKey)
        return doFinally { task.removeSubTask() }
                .doOnSubscribe { disposable -> task.safeAddSubTask(disposable) }
    }

    /** Preserves link for task by key while it's running  */
    protected fun <T> maybeTaskTracker(taskKey: String): MaybeTransformer<T, T> {
        return MaybeTransformer { it.taskTracker(taskKey) }
    }

    /** Preserves link for task by key while it's running  */
    protected fun <T> Maybe<T>.taskTracker(taskKey: String): Maybe<T> {
        val task = addTask(taskKey)
        return doFinally { task.removeSubTask() }
                .doOnSubscribe { disposable -> task.safeAddSubTask(disposable) }
    }

    private fun addTask(taskKey: String): Task {
        checkMainThread()
        if(activeTasks.containsKey(taskKey)) {
            throw IllegalStateException("'$taskKey' is already tracked")
        }
        val task = Task(taskKey)
        activeTasks[taskKey] = task
        return task
    }

    protected fun setResetTaskStateAction(key: String, resetAction: Action) {
        resetTaskStateActionMap.put(key, resetAction)
    }

    protected fun cancelTask(taskKey: String): Completable {
        checkMainThread()
        val task = activeTasks[taskKey] ?: return Completable.complete()
        activeTasks.remove(taskKey)
        val completable = task.cancel()
        resetTaskState(taskKey)
        return completable
    }

    protected fun awaitTask(taskKey: String): Completable {
        return activeTasks[taskKey]?.await() ?: return Completable.complete()
    }

    protected fun isTaskActive(taskKey: String): Boolean {
        checkMainThread()
        return activeTasks[taskKey] != null
    }

    /** Calls preliminarily set a reset task state action   */
    private fun resetTaskState(taskKey: String) {
        val resetTaskAction = resetTaskStateActionMap[taskKey]
        if (resetTaskAction == null) {
            Log.w(javaClass.simpleName, "Reset task action is not set for task key: " + taskKey)
        } else {
            execute(resetTaskAction)
        }
    }

    fun <T> Observable<T>.safeSubscribe(
            onNext: (T) -> Unit,
            onError: (Throwable) -> Unit = ERROR_CONSUMER,
            onComplete: Action = EMPTY_ACTION
    ): Disposable {
        return subscribe(safeOnItem(onNext), safeOnError(onError), safeOnComplete(onComplete))
    }

    fun <T> Flowable<T>.safeSubscribe(
            onNext: (T) -> Unit,
            onError: (Throwable) -> Unit = ERROR_CONSUMER,
            onComplete: Action = EMPTY_ACTION
    ): Disposable {
        return subscribe(safeOnItem(onNext), safeOnError(onError), safeOnComplete(onComplete))
    }

    fun <T> Single<T>.safeSubscribe(
            onSuccess: (T) -> Unit,
            onError: (Throwable) -> Unit = ERROR_CONSUMER
    ): Disposable {
        return subscribe(safeOnItem(onSuccess), safeOnError(onError))
    }

    fun Completable.safeSubscribe(
            onComplete: Action,
            onError: (Throwable) -> Unit = ERROR_CONSUMER
    ): Disposable {
        return subscribe({ execute(onComplete) }, safeOnError(onError))
    }

    fun <T> Maybe<T>.safeSubscribe(
            onSuccess: (T) -> Unit,
            onError: (Throwable) -> Unit = ERROR_CONSUMER,
            onComplete: Action = EMPTY_ACTION
    ): Disposable {
        return subscribe(safeOnItem(onSuccess), safeOnError(onError), safeOnComplete(onComplete))
    }

    fun <T> safeOnItem(onItem: (T) -> Unit): (T) -> Unit {
        return { item -> execute { onItem(item) } }
    }

    fun safeOnComplete(onComplete: Action): () -> Unit {
        if(onComplete == EMPTY_ACTION) {
            return EMPTY_ACTION
        } else {
            return { execute(onComplete) }
        }
    }

    fun safeOnError(onError: (Throwable) -> Unit): (Throwable) -> Unit {
        if(onError == ERROR_CONSUMER) {
            return ERROR_CONSUMER
        } else {
            return { error: Throwable -> execute { onError(error) } }
        }
    }

    fun <T> Observable<T>.cancelOnDetachView(onSaveState: Boolean = false): Observable<T> {
        return doOnSubscribe { cancelOnDetachView(it, onSaveState) }
    }

    fun <T> Flowable<T>.cancelOnDetachView(onSaveState: Boolean = false): Flowable<T> {
        return doOnSubscribe {
            cancelOnDetachView(object : Disposable {
                override fun isDisposed() = throw RuntimeException("Unsupported")
                override fun dispose() = it.cancel()
            }, onSaveState)
        }
    }

    fun <T> Single<T>.cancelOnDetachView(onSaveState: Boolean = false): Single<T> {
        return doOnSubscribe { cancelOnDetachView(it, onSaveState) }
    }

    fun <T> Maybe<T>.cancelOnDetachView(onSaveState: Boolean = false): Maybe<T> {
        return doOnSubscribe { cancelOnDetachView(it, onSaveState) }
    }

    fun Completable.cancelOnDetachView(onSaveState: Boolean = false): Completable {
        return doOnSubscribe { cancelOnDetachView(it, onSaveState) }
    }

    private fun cancelOnDetachView(disposable: Disposable, onSaveState: Boolean) {
        runOnUiThread {
            if(view == null || (onSaveState && isSavedState)) {
                disposable.dispose()
            } else {
                if(onSaveState) {
                    onSaveStateActionList.add({
                        disposable.dispose()
                    })
                }
                onDetachViewActionList.add({
                    disposable.dispose()
                })
            }
        }
    }

    private fun checkMainThread(message: String = "Must be call after observeOn(AndroidSchedulers.mainThread())") {
        if(!isMainThread()) {
            throw IllegalStateException(message)
        }
    }

    private fun runOnUiThread(action: Action) {
        if(isMainThread()) {
            action()
        } else {
            mainHandler.post(action)
        }
    }

    private fun isMainThread() = Looper.myLooper() == Looper.getMainLooper()
}

package com.fphoenixcorneae.core.multistatus

import android.app.Activity
import android.app.Fragment
import android.content.Context
import android.util.SparseArray
import android.util.SparseIntArray
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.annotation.Keep
import androidx.annotation.LayoutRes
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.fphoenixcorneae.core.R
import com.fphoenixcorneae.ext.loggerD

/**
 * @desc：多状态布局管理器：利用[MultiStatusLayoutManager.Builder.register]获取[MultiStatusLayoutManager]对象
 * @date：2021/1/15 19:23
 */
class MultiStatusLayoutManager(private val builder: Builder) : LifecycleObserver {
    private val mTarget = builder.target
    private var mStatusViewMap = builder.statusViewMap
    private var mClickListeners = builder.clickListeners

    /**
     * contentLayout 在 parentLayout 中的位置
     */
    private var mViewIndex = 0
    private var mCurrentStatus = 0
    private var mInflater: LayoutInflater? = null
    private var mContentLayout: View? = null
    private var mParentLayout: ViewGroup? = null
    private var mStatusViews: SparseArray<View?>? = null
    private var mLayoutParams: ViewGroup.LayoutParams? = null

    /**
     * 获取 contentLayout 的参数信息 LayoutParams、Parent
     */
    private fun initContentLayoutParams() {
        if (mTarget == null) {
            throw NullPointerException("Status Layout target must not be null.")
        }
        val context: Context?
        when (mTarget) {
            is Activity -> {
                // 认为 contentLayout 是 activity 的跟布局
                // 所以它的父控件就是 android.R.id.content
                context = mTarget
                mParentLayout = mTarget.findViewById(android.R.id.content)
                mContentLayout = if (mParentLayout != null) {
                    mParentLayout?.getChildAt(0)
                } else {
                    null
                }
            }
            is View -> {
                // 有直接的父控件
                mContentLayout = mTarget
                context = mTarget.context
                mParentLayout = mContentLayout?.parent as ViewGroup
            }
            is Fragment -> {
                // 有直接的父控件
                mContentLayout = mTarget.view
                context = mTarget.activity
                mParentLayout = mContentLayout?.parent as ViewGroup
            }
            is androidx.fragment.app.Fragment -> {
                // 有直接的父控件
                mContentLayout = mTarget.view
                context = mTarget.activity
                mParentLayout = mContentLayout?.parent as ViewGroup
            }
            else -> {
                throw IllegalArgumentException("Status Layout target must be of Type View, Fragment, or Activity.")
            }
        }
        mInflater =
            context?.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as? LayoutInflater
        mLayoutParams = mContentLayout?.layoutParams
    }

    /**
     * 初始化预设的状态布局
     */
    private fun initStatusViews() {
        if (mStatusViews == null) {
            mStatusViews = SparseArray()
        }
        if (mStatusViewMap == null) {
            mStatusViewMap = SparseIntArray()
        }
        if (mStatusViewMap?.get(StatusType.EMPTY_TYPE) == 0) {
            mStatusViewMap?.put(
                StatusType.EMPTY_TYPE,
                DEFAULT_LAYOUT_ID_EMPTY
            )
        }
        if (mStatusViewMap?.get(StatusType.ERROR_TYPE) == 0) {
            mStatusViewMap?.put(
                StatusType.ERROR_TYPE,
                DEFAULT_LAYOUT_ID_ERROR
            )
        }
        if (mStatusViewMap?.get(StatusType.NO_NETWORK_TYPE) == 0) {
            mStatusViewMap?.put(
                StatusType.NO_NETWORK_TYPE,
                DEFAULT_LAYOUT_ID_NO_NETWORK
            )
        }
        if (mStatusViewMap?.get(StatusType.LOADING_TYPE) == 0) {
            mStatusViewMap?.put(
                StatusType.LOADING_TYPE,
                DEFAULT_LAYOUT_ID_LOADING
            )
        }
        mStatusViewMap?.apply {
            for (i in 0 until size()) {
                val key = keyAt(i)
                val value = valueAt(i)
                val view = mInflater?.inflate(value, null)
                if (view != null) {
                    mStatusViews?.put(key, view)
                }
            }
        }
        mStatusViews?.put(StatusType.CONTENT_TYPE, mContentLayout)
    }

    private fun initStatusViewClickListeners() {
        if (mClickListeners == null) {
            mClickListeners = SparseArray()
        }
    }

    /**
     * 展示加载动画
     */
    fun showLoadingView() {
        showStatusView(StatusType.LOADING_TYPE)
    }

    /**
     * 展示加载动画
     *
     * @param loadingText 加载中文案
     */
    fun showLoadingView(loadingText: String?) {
        showStatusView(StatusType.LOADING_TYPE, loadingText)
    }

    /**
     * 展示出错页面
     */
    fun showErrorView() {
        showStatusView(StatusType.ERROR_TYPE)
    }

    /**
     * 展示出错页面
     *
     * @param errorText 错误文案
     */
    fun showErrorView(errorText: String?) {
        showStatusView(StatusType.ERROR_TYPE, errorText)
    }

    /**
     * 展示空页面
     */
    fun showEmptyView() {
        showStatusView(StatusType.EMPTY_TYPE)
    }

    /**
     * 展示空页面
     *
     * @param emptyText 空数据文案
     */
    fun showEmptyView(emptyText: String?) {
        showStatusView(StatusType.EMPTY_TYPE, emptyText)
    }

    /**
     * 展示网络错误页面
     */
    fun showNoNetWorkView() {
        showStatusView(StatusType.NO_NETWORK_TYPE)
    }

    /**
     * 展示网络错误页面
     *
     * @param noNetworkText 网络错误文案
     */
    fun showNoNetWorkView(noNetworkText: String?) {
        showStatusView(StatusType.NO_NETWORK_TYPE, noNetworkText)
    }

    /**
     * 显示原有布局
     */
    fun showContentView() {
        showStatusView(StatusType.CONTENT_TYPE)
    }

    /**
     * 展示各种状态的页面
     *
     * @param statusType 状态类型
     */
    private fun showStatusView(
        @StatusType statusType: Int,
        text: String? = null
    ) {
        checkStatusViewExist(statusType)
        if (mCurrentStatus == statusType) {
            return
        }
        val view = mStatusViews?.get(statusType)
        if (view != null) {
            // 去除 view 的 父 view，才能添加到别的 ViewGroup 中
            mParentLayout?.removeViewAt(mViewIndex)
            if (mClickListeners != null) {
                val clickListener = mClickListeners?.get(statusType)
                if (clickListener != null) {
                    view.setOnClickListener(clickListener)
                }
            }
            mParentLayout?.addView(view, mViewIndex, mLayoutParams)
            mCurrentStatus = statusType
        } else {
            throw IllegalArgumentException(
                String.format(
                    "the status (%d) layout is not exist",
                    statusType
                )
            )
        }
    }

    /**
     * 检测状态视图是否存在
     */
    private fun checkStatusViewExist(statusType: Int) {
        if (mStatusViews == null || mStatusViews?.get(statusType) == null) {
            // 如果没有主动设置某个状态的布局的话，添加默认布局
            initStatusViews()
        }
    }

    /**
     * 清理缓存
     */
    private fun release() {
        mClickListeners?.clear()
        mStatusViewMap?.clear()
        mStatusViews?.clear()
        builder.removeObserver(mTarget, this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    fun onCreate() {
        loggerD("MultiStatus:OnLifecycleEvent::${mTarget?.javaClass?.name} onCreate")
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onStart() {
        loggerD("MultiStatus:OnLifecycleEvent::${mTarget?.javaClass?.name} onStart")
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResume() {
        loggerD("MultiStatus:OnLifecycleEvent::${mTarget?.javaClass?.name} onResume")
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun onPause() {
        loggerD("MultiStatus:OnLifecycleEvent::${mTarget?.javaClass?.name} onPause")
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onStop() {
        loggerD("MultiStatus:OnLifecycleEvent::${mTarget?.javaClass?.name} onStop")
        showContentView()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun onDestroy() {
        loggerD("MultiStatus:OnLifecycleEvent::${mTarget?.javaClass?.name} onDestroy")
        // 清理缓存
        release()
    }

    /**
     * @desc：状态类型 参考：https://www.jianshu.com/p/1fb27f46622c
     * @date：2021/1/16 17:04
     */
    @Keep
    @kotlin.annotation.Retention(AnnotationRetention.SOURCE)
    annotation class StatusType {
        companion object {
            const val CONTENT_TYPE = 0
            const val LOADING_TYPE = 1
            const val ERROR_TYPE = 2
            const val EMPTY_TYPE = 3
            const val NO_NETWORK_TYPE = 4
        }
    }

    /**
     * @desc：多状态布局管理器的建造者
     * @date：2021/1/16 17:24
     */
    @Keep
    class Builder {
        var target: Any? = null
        var statusViewMap: SparseIntArray? = null
        var clickListeners: SparseArray<View.OnClickListener?>? = null

        fun setErrorViewId(@LayoutRes layoutId: Int): Builder {
            return addStatusView(StatusType.ERROR_TYPE, layoutId)
        }

        fun setLoadingViewId(@LayoutRes layoutId: Int): Builder {
            return addStatusView(StatusType.LOADING_TYPE, layoutId)
        }

        fun setNoNetWorkViewId(@LayoutRes layoutId: Int): Builder {
            return addStatusView(StatusType.NO_NETWORK_TYPE, layoutId)
        }

        fun setEmptyViewId(@LayoutRes layoutId: Int): Builder {
            return addStatusView(StatusType.EMPTY_TYPE, layoutId)
        }

        /**
         * 添加状态布局
         */
        private fun addStatusView(@StatusType status: Int, layoutId: Int): Builder {
            if (statusViewMap == null) {
                statusViewMap = SparseIntArray()
            }
            statusViewMap?.put(status, layoutId)
            return this
        }

        /**
         * 添加无网络点击事件
         */
        fun addNoNetWorkClickListener(clickListener: View.OnClickListener?): Builder {
            return addOnClickListener(StatusType.NO_NETWORK_TYPE, clickListener)
        }

        /**
         * 添加错误布局点击事件
         */
        fun addErrorClickListener(statusClickListener: View.OnClickListener?): Builder {
            return addOnClickListener(StatusType.ERROR_TYPE, statusClickListener)
        }

        /**
         * 添加无数据点击事件
         */
        fun addEmptyClickListener(statusClickListener: View.OnClickListener?): Builder {
            return addOnClickListener(StatusType.EMPTY_TYPE, statusClickListener)
        }

        /**
         * 添加其他状态布局点击事件，以方便自定义设置
         */
        private fun addOnClickListener(
            @StatusType status: Int,
            statusClickListener: View.OnClickListener?
        ): Builder {
            if (clickListeners == null) {
                clickListeners = SparseArray()
            }
            clickListeners?.put(status, statusClickListener)
            return this
        }

        /**
         * 用[register]方法注册[Builder.target]，并获取[MultiStatusLayoutManager]对象
         */
        fun register(target: Any?): MultiStatusLayoutManager {
            this.target = target
            return MultiStatusLayoutManager(this).apply {
                addObserver(target, this)
            }
        }

        private fun addObserver(
            target: Any?,
            multiStatusLayoutManager: MultiStatusLayoutManager
        ) {
            when (target) {
                is ComponentActivity -> {
                    target.lifecycle.addObserver(multiStatusLayoutManager)
                }
                is androidx.fragment.app.Fragment -> {
                    target.lifecycle.addObserver(multiStatusLayoutManager)
                }
            }
        }

        fun removeObserver(
            target: Any?,
            multiStatusLayoutManager: MultiStatusLayoutManager
        ) {
            when (target) {
                is ComponentActivity -> {
                    target.lifecycle.removeObserver(multiStatusLayoutManager)
                }
                is androidx.fragment.app.Fragment -> {
                    target.lifecycle.removeObserver(multiStatusLayoutManager)
                }
            }
        }
    }

    companion object {
        /**
         * 四种默认布局 ID
         */
        private val DEFAULT_LAYOUT_ID_LOADING = R.layout.core_layout_multiple_status_loading
        private val DEFAULT_LAYOUT_ID_EMPTY = R.layout.core_layout_multiple_status_empty
        private val DEFAULT_LAYOUT_ID_ERROR = R.layout.core_layout_multiple_status_error
        private val DEFAULT_LAYOUT_ID_NO_NETWORK = R.layout.core_layout_multiple_status_no_network
    }

    init {
        initContentLayoutParams()
        initStatusViews()
        initStatusViewClickListeners()
        val count = mParentLayout?.childCount ?: 0
        for (index in 0 until count) {
            if (mContentLayout === mParentLayout?.getChildAt(index)) {
                // 获取 contentLayout 在 mParentLayout 中的位置
                mViewIndex = index
                break
            }
        }
    }
}
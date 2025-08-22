package com.hyphenate.callkit.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isNotEmpty
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hyphenate.callkit.databinding.CallkitFragmentBaseListBinding
import com.hyphenate.callkit.interfaces.OnItemClickListener
import com.scwang.smart.refresh.header.MaterialHeader
import com.scwang.smart.refresh.layout.SmartRefreshLayout
import kotlinx.coroutines.launch

/**
 * \~chinese
 * 通话基类列表Fragment
 *
 * \~english
 * Base call list fragment
 */
abstract class BaseListFragment<T>:BaseFragment<CallkitFragmentBaseListBinding>(),
    OnItemClickListener {
    lateinit var srlContactRefresh:SmartRefreshLayout
    var mRecyclerView: RecyclerView? = null
    lateinit var mListAdapter: BaseAdapter<T>
    protected lateinit var concatAdapter: ConcatAdapter

    companion object{
        const val KEY_USER = "user"
        const val KEY_SELECT_USER = "select_user"
    }

    override fun initView(savedInstanceState: Bundle?) {
        super.initView(savedInstanceState)
        binding?.let {
            srlContactRefresh = it.srlContactRefresh
            val refreshHeader = it.srlContactRefresh.refreshHeader
            if (refreshHeader == null) {
                it.srlContactRefresh.setRefreshHeader(MaterialHeader(context))
            }
            mRecyclerView = if (initRecyclerView()?.isNotEmpty() == true){
                initRecyclerView()
            }else{
                it.rvList
            }
        }
        mRecyclerView?.layoutManager = getLayoutManager()
        concatAdapter = ConcatAdapter()
        addHeader(concatAdapter)
        mListAdapter = initAdapter()
        concatAdapter.addAdapter(mListAdapter)
        mRecyclerView?.adapter = concatAdapter
    }

    override fun getViewBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): CallkitFragmentBaseListBinding {
        return CallkitFragmentBaseListBinding.inflate(inflater)
    }

    override fun initListener() {
        super.initListener()
        mListAdapter.setOnItemClickListener(this)
        srlContactRefresh.setOnRefreshListener {
            refreshData()
        }
    }

    override fun initData() {
        super.initData()
        mRecyclerView?.adapter = concatAdapter
    }

    /**
     * Can add header adapters
     * @param adapter
     */
    open fun addHeader(adapter: ConcatAdapter) {
        // Add header adapter by adapter
    }

    /**
     * Can change the RecyclerView's orientation
     * @return
     */
    protected open fun getLayoutManager(): RecyclerView.LayoutManager {
        return LinearLayoutManager(mContext)
    }

    /**
     * Must initialize the RecyclerView
     * @return
     */
    protected abstract fun initRecyclerView(): RecyclerView?

    /**
     * Must provide the list adapter
     * @return
     */
    protected abstract fun initAdapter(): BaseAdapter<T>


    protected abstract fun refreshData()


    override fun onItemClick(view: View?, position: Int) {

    }

    fun finishRefresh() {
        lifecycleScope.launch {
            if (srlContactRefresh.isNotEmpty()) {
                srlContactRefresh.finishRefresh()
            }
        }
    }
}
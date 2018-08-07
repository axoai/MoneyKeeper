/*
 * Copyright 2018 Bakumon. https://github.com/Bakumon
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package me.bakumon.moneykeeper.ui.typesort

import android.os.Bundle
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import me.bakumon.moneykeeper.R
import me.bakumon.moneykeeper.Router
import me.bakumon.moneykeeper.database.entity.RecordType
import me.bakumon.moneykeeper.datasource.BackupFailException
import me.bakumon.moneykeeper.ui.common.AbsListActivity
import me.bakumon.moneykeeper.utill.ToastUtils
import me.drakeet.multitype.Items
import me.drakeet.multitype.MultiTypeAdapter
import me.drakeet.multitype.register

/**
 * 类型排序
 *
 * @author bakumon https://bakumon.me
 */
class TypeSortActivity : AbsListActivity() {

    private lateinit var mViewModel: TypeSortViewModel
    private var mType: Int = RecordType.TYPE_OUTLAY

    override fun onSetupTitle(tvTitle: TextView) {
        tvTitle.text = getString(R.string.text_drag_sort)
    }

    override fun onAdapterCreated(adapter: MultiTypeAdapter) {
        adapter.register(RecordType::class, TypeSortViewBinder())
    }

    override fun onItemsCreated(items: Items) {

    }

    override fun onParentInitDone(recyclerView: RecyclerView, savedInstanceState: Bundle?) {

        recyclerView.layoutManager = GridLayoutManager(this, 4)


        val callback = SortDragCallback(mAdapter)
        val itemTouchHelper = ItemTouchHelper(callback)
        itemTouchHelper.attachToRecyclerView(recyclerView)

        mType = intent.getIntExtra(Router.ExtraKey.KEY_TYPE, RecordType.TYPE_OUTLAY)
        mViewModel = getViewModel()
        initData()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_sort, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(menuItem: MenuItem?): Boolean {
        when (menuItem?.itemId) {
            R.id.action_done -> sortRecordTypes()
            android.R.id.home -> finish()
        }
        return true
    }

    private fun sortRecordTypes() {
        mDisposable.add(mViewModel.sortRecordTypes(mAdapter.items as List<RecordType>).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ this.finish() }) { throwable ->
                    if (throwable is BackupFailException) {
                        ToastUtils.show(throwable.message)
                        Log.e(TAG, "备份失败（类型排序失败的时候）", throwable)
                        finish()
                    } else {
                        ToastUtils.show(R.string.toast_sort_fail)
                        Log.e(TAG, "类型排序失败", throwable)
                    }
                })
    }

    private fun initData() {
        mDisposable.add(mViewModel.getRecordTypes(mType).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ recordTypes ->
                    setItems(recordTypes)
                }
                ) { throwable ->
                    ToastUtils.show(R.string.toast_get_types_fail)
                    Log.e(TAG, "获取类型数据失败", throwable)
                })
    }

    private fun setItems(data: List<RecordType>) {
        val items = Items()
        items.addAll(data)
        mAdapter.items = items
        mAdapter.notifyDataSetChanged()
    }

    companion object {
        private val TAG = TypeSortActivity::class.java.simpleName
    }
}

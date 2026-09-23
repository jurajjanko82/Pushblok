package com.pushblok.app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pushblok.app.databinding.ItemExerciseHeaderBinding
import com.pushblok.app.databinding.ItemExerciseRowBinding

private sealed class ListItem {
    data class Header(val category: ExerciseCategory) : ListItem()
    data class Row(val type: ExerciseType) : ListItem()
}

class ExerciseListAdapter(
    private val onStart: (ExerciseType) -> Unit,
    private val onCreditsChanged: (ExerciseType, Int) -> Unit,
    private val getCredits: (ExerciseType) -> Int
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items: List<ListItem> = buildList {
        for (cat in ExerciseCategory.values()) {
            add(ListItem.Header(cat))
            ExerciseType.byCategory(cat).forEach { add(ListItem.Row(it)) }
        }
    }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_ROW = 1
    }

    override fun getItemViewType(position: Int): Int =
        if (items[position] is ListItem.Header) TYPE_HEADER else TYPE_ROW

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_HEADER) {
            HeaderVH(ItemExerciseHeaderBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        } else {
            RowVH(ItemExerciseRowBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ListItem.Header -> (holder as HeaderVH).bind(item.category)
            is ListItem.Row -> (holder as RowVH).bind(item.type)
        }
    }

    override fun getItemCount(): Int = items.size

    inner class HeaderVH(val binding: ItemExerciseHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(category: ExerciseCategory) {
            binding.tvHeader.text = category.label
        }
    }

    inner class RowVH(val binding: ItemExerciseRowBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(type: ExerciseType) {
            val unitLabel = if (type.unit == ExerciseUnit.SECONDS)
                "kr. / ${ExerciseSettings.SECONDS_PER_CREDIT_UNIT}s" else "kr. / opak."
            binding.exerciseLabel.text = type.label
            binding.unitLabel.text = unitLabel
            binding.etCredits.setText(getCredits(type).toString())
            binding.etCredits.tag = type

            binding.etCredits.addTextChangedListener(object : android.text.TextWatcher {
                override fun afterTextChanged(s: android.text.Editable?) {
                    if (binding.etCredits.tag != type) return
                    val v = s.toString().toIntOrNull() ?: return
                    onCreditsChanged(type, v)
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })

            binding.btnStart.setOnClickListener { onStart(type) }
        }
    }
}

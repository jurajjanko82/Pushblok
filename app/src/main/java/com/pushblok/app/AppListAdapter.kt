package com.pushblok.app

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.pushblok.app.databinding.ItemAppBinding

class AppListAdapter(
    private val apps: List<AppEntry>,
    private val blockedSet: MutableSet<String>,
    private val onToggle: (packageName: String, blocked: Boolean) -> Unit
) : RecyclerView.Adapter<AppListAdapter.VH>() {

    inner class VH(val binding: ItemAppBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val app = apps[position]
        holder.binding.appIcon.setImageDrawable(app.icon)
        holder.binding.appLabel.text = app.label
        holder.binding.appSwitch.setOnCheckedChangeListener(null)
        holder.binding.appSwitch.isChecked = blockedSet.contains(app.packageName)
        holder.binding.appSwitch.setOnCheckedChangeListener { _, checked ->
            if (checked) blockedSet.add(app.packageName) else blockedSet.remove(app.packageName)
            onToggle(app.packageName, checked)
        }
    }

    override fun getItemCount(): Int = apps.size
}

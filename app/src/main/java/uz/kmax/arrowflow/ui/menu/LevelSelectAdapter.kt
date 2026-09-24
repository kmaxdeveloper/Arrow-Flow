package uz.kmax.arrowflow.ui.menu

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import uz.kmax.arrowflow.databinding.ItemLevelBinding
import uz.kmax.arrowflow.model.Level

data class LevelSelectItem(
    val level: Level,
    val bestStars: Int,
    val isUnlocked: Boolean
)

class LevelSelectAdapter(
    private val items: List<LevelSelectItem>,
    private val onLevelClick: (Level) -> Unit
) : RecyclerView.Adapter<LevelSelectAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemLevelBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLevelBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val binding = holder.binding

        binding.tvLevelNumber.text = "LEVEL ${item.level.id}"
        binding.tvDifficulty.text = item.level.difficulty.name
        
        val stars = StringBuilder()
        for (i in 0 until 3) {
            if (i < item.bestStars) stars.append("⭐")
            else stars.append("☆")
        }
        binding.tvStars.text = stars.toString()

        if (item.isUnlocked) {
            binding.ivLock.visibility = View.GONE
            binding.root.alpha = 1.0f
            binding.root.setOnClickListener { onLevelClick(item.level) }
            binding.tvStars.visibility = View.VISIBLE
        } else {
            binding.ivLock.visibility = View.VISIBLE
            binding.root.alpha = 0.6f
            binding.root.setOnClickListener(null)
            binding.tvStars.visibility = View.GONE
        }
    }

    override fun getItemCount() = items.size
}

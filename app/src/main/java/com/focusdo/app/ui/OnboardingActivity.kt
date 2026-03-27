package com.focusdo.app.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.focusdo.app.R
import com.focusdo.app.databinding.ActivityOnboardingBinding
import com.focusdo.app.databinding.ItemOnboardPageBinding

class OnboardingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOnboardingBinding

    // ── Page data ────────────────────────────────────────────────────────────

    data class OnboardPage(val emoji: String, val title: String, val desc: String)

    private val pages = listOf(
        OnboardPage(
            emoji = "✅",
            title = "할 일을 한눈에",
            desc = "날짜별로 할 일을 관리하고\n완료 여부를 간편하게 체크하세요."
        ),
        OnboardPage(
            emoji = "🍅",
            title = "뽀모도로로 집중",
            desc = "타이머를 켜면 화면을 꺼도\n백그라운드에서 계속 집중을 측정해요."
        ),
        OnboardPage(
            emoji = "📊",
            title = "집중 기록 확인",
            desc = "오늘·이번 주·이번 달의\n집중 시간과 토마토 개수를 확인하세요."
        )
    )

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewPager()
        setupDots()
        setupButtons()
    }

    // ── Setup ─────────────────────────────────────────────────────────────────

    private fun setupViewPager() {
        binding.viewPager.adapter = OnboardAdapter()
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateDots(position)
                updateButton(position)
            }
        })
    }

    private fun setupDots() {
        repeat(pages.size) { i ->
            val dot = ImageView(this).apply {
                setImageResource(if (i == 0) R.drawable.bg_dot_active else R.drawable.bg_dot_inactive)
                val params = ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(6, 0, 6, 0)
                layoutParams = params
            }
            binding.dotsContainer.addView(dot)
        }
    }

    private fun updateDots(selectedPos: Int) {
        for (i in 0 until binding.dotsContainer.childCount) {
            val dot = binding.dotsContainer.getChildAt(i) as ImageView
            dot.setImageResource(
                if (i == selectedPos) R.drawable.bg_dot_active else R.drawable.bg_dot_inactive
            )
        }
    }

    private fun updateButton(position: Int) {
        binding.btnNext.text = if (position == pages.lastIndex) "시작하기 🎉" else "다음"
        binding.tvSkip.visibility = if (position == pages.lastIndex) View.INVISIBLE else View.VISIBLE
    }

    private fun setupButtons() {
        binding.btnNext.setOnClickListener {
            val current = binding.viewPager.currentItem
            if (current < pages.lastIndex) {
                binding.viewPager.currentItem = current + 1
            } else {
                finishOnboarding()
            }
        }
        binding.tvSkip.setOnClickListener { finishOnboarding() }
    }

    private fun finishOnboarding() {
        getSharedPreferences("focusdo_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("onboarding_done", true).apply()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    // ── Adapter ───────────────────────────────────────────────────────────────

    inner class OnboardAdapter : RecyclerView.Adapter<OnboardAdapter.VH>() {

        inner class VH(val binding: ItemOnboardPageBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            return VH(ItemOnboardPageBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

        override fun getItemCount() = pages.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val page = pages[position]
            holder.binding.tvEmoji.text = page.emoji
            holder.binding.tvTitle.text = page.title
            holder.binding.tvDesc.text = page.desc
            // BuddyView shows on all pages
            holder.binding.buddyView.visibility = View.VISIBLE
        }
    }
}

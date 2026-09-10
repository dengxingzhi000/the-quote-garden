package com.quotegarden.core.designsystem

import androidx.annotation.DrawableRes
import com.quotegarden.R

@DrawableRes
fun categoryDrawable(category: String): Int {
    val key = category.lowercase()
    return when {
        // Exact-match routes for DB slugs that have a dedicated asset.
        // NB: "age" is an exact match and is listed before any "courage" rule
        // so "age" cannot misroute to a future courage substring match.
        key == "age" -> R.drawable.age
        key == "adventure" -> R.drawable.adventure
        key == "adversity" -> R.drawable.adversity
        key == "advice" -> R.drawable.advice
        key == "afternoon" -> R.drawable.afternoon
        key == "astrology" -> R.drawable.astrology
        key == "autumn" -> R.drawable.autumn
        key == "birds" -> R.drawable.birds
        key == "blog-hawthorne-story-ideas" -> R.drawable.blog_hawthorne_story_ideas
        key == "create-your-own-quote-contests" -> R.drawable.create_your_own_quote_contests
        key == "safari-unhand-your-pearls" -> R.drawable.safari_unhand_your_pearls
        // Generic concept keyword tiles.
        "love" in key || category.contains("爱") -> R.drawable.category_love
        "wisdom" in key || category.contains("智") -> R.drawable.category_wisdom
        "courage" in key || category.contains("勇") -> R.drawable.category_courage
        "humor" in key || category.contains("幽") || category.contains("谐") -> R.drawable.category_humor
        "life" in key || category.contains("生") -> R.drawable.category_life
        "solitude" in key || "alone" in key || category.contains("独") || category.contains("孤") -> R.drawable.category_solitude
        "time" in key || category.contains("时") -> R.drawable.category_time
        "change" in key || category.contains("改") || category.contains("变") -> R.drawable.category_change
        else -> R.drawable.ic_brand_mark
    }
}

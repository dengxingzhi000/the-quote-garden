package com.quotegarden.core.designsystem

import com.quotegarden.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CategoryIconTest {

    @Test fun `DB slugs with a dedicated asset map to that asset`() {
        assertEquals(R.drawable.adventure, categoryDrawable("adventure"))
        assertEquals(R.drawable.adversity, categoryDrawable("adversity"))
        assertEquals(R.drawable.advice, categoryDrawable("advice"))
        assertEquals(R.drawable.afternoon, categoryDrawable("afternoon"))
        assertEquals(R.drawable.age, categoryDrawable("age"))
        assertEquals(R.drawable.astrology, categoryDrawable("astrology"))
        assertEquals(R.drawable.autumn, categoryDrawable("autumn"))
        assertEquals(R.drawable.birds, categoryDrawable("birds"))
        // Slug keys carry hyphens (matching the DB), but Android resource names
        // forbid hyphens, so the underlying files use underscores.
        assertEquals(R.drawable.blog_hawthorne_story_ideas, categoryDrawable("blog-hawthorne-story-ideas"))
        assertEquals(R.drawable.create_your_own_quote_contests, categoryDrawable("create-your-own-quote-contests"))
        assertEquals(R.drawable.safari_unhand_your_pearls, categoryDrawable("safari-unhand-your-pearls"))
    }

    @Test fun `DB slug match is case insensitive`() {
        assertEquals(R.drawable.adventure, categoryDrawable("Adventure"))
        assertEquals(R.drawable.astrology, categoryDrawable("ASTROLOGY"))
        assertEquals(R.drawable.age, categoryDrawable("Age"))
        assertEquals(R.drawable.safari_unhand_your_pearls, categoryDrawable("Safari-Unhand-Your-Pearls"))
    }

    @Test fun `action has no dedicated asset yet and falls back to brand mark`() {
        // action.png is not on disk yet (highest-count DB category, 69 rows).
        // When the asset lands, this test should be moved into the dedicated-asset
        // case above and a route added to categoryDrawable.
        assertEquals(R.drawable.ic_brand_mark, categoryDrawable("action"))
    }

    @Test fun `generic concept keywords still map to their concept tiles`() {
        assertEquals(R.drawable.category_love, categoryDrawable("love"))
        assertEquals(R.drawable.category_wisdom, categoryDrawable("wisdom"))
        assertEquals(R.drawable.category_courage, categoryDrawable("courage"))
        assertEquals(R.drawable.category_humor, categoryDrawable("humor"))
        assertEquals(R.drawable.category_life, categoryDrawable("life"))
        assertEquals(R.drawable.category_solitude, categoryDrawable("solitude"))
        assertEquals(R.drawable.category_time, categoryDrawable("time"))
        assertEquals(R.drawable.category_change, categoryDrawable("change"))
    }

    @Test fun `age slug does not collide with courage which contains the age substring`() {
        // "courage" contains "age" as a substring. If the new "age" rule used a
        // substring match and was ordered before "courage", "courage" would misroute
        // to the age drawable. The age rule must be an exact match.
        assertEquals(R.drawable.category_courage, categoryDrawable("courage"))
        assertEquals(R.drawable.age, categoryDrawable("age"))
        assertNotEquals(R.drawable.age, categoryDrawable("courage"))
    }

    @Test fun `unknown or unrenderable categories fall back to brand mark`() {
        assertEquals(R.drawable.ic_brand_mark, categoryDrawable(""))
        assertEquals(R.drawable.ic_brand_mark, categoryDrawable("not-a-real-category"))
    }
}

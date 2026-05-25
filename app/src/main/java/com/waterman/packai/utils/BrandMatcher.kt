package com.waterman.packai.utils

object BrandMatcher {

    fun isMatch(brand: String, vararg texts: String): Boolean {
        val cleanBrand = brand.trim().lowercase()
        return texts.all { text ->
            text.trim().lowercase().contains(cleanBrand)
        }
    }
}
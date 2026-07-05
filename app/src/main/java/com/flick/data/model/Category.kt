package com.flick.data.model

data class Category(
    val id: Long = 0,
    val name: String,
    val sortOrder: Int,
    val colorOrIconRes: Int? = null
)

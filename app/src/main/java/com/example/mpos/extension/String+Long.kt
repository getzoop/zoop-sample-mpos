package com.example.mpos.extension

fun String.convertToLong(): Long {
    return if(this.isNotEmpty() || this.isNotBlank()) this.replace("\\D".toRegex(), "0").toLong()
    else 0
}
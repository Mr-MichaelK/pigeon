package com.example.pigeon.domain.model

data class TrustScore(
    val percentage: Int,
    val totalConfirms: Int,
    val totalContradicts: Int,
    val isVerified: Boolean
) {
    companion object {
        val EMPTY = TrustScore(
            percentage = 0,
            totalConfirms = 0,
            totalContradicts = 0,
            isVerified = false
        )
    }
}

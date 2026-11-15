package com.wamad.petaljong.domain

/** A local Android connectivity signal, independent of advertising eligibility. */
object InternetPolicy {
    fun validated(internet: Boolean, validated: Boolean) = internet && validated
}

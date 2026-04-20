package com.egr.squashgo.core.common.di

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class Dispatcher(val dispatcher: SquashGoDispatchers)

enum class SquashGoDispatchers {
    IO,
    DEFAULT,
}

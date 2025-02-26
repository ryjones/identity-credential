package org.multipaz.ui

/**
 * Thrown if no [UiView] has registered with [UiModel].
 */
class UiViewNotAvailableException(message: String): Exception(message)

package org.multipaz.appsupport.ui.presentment

/**
 * Thrown when timing out waiting for the reader to connect.
 *
 * @property message message to display.
 */
class PresentmentTimeout(message: String): Exception(message)
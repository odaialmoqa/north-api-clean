package com.north.mobile.data.plaid

import android.content.Context

actual class PlaidLinkHandlerFactory(
    private val context: Context
) {
    actual fun create(): PlaidLinkHandler {
        return AndroidPlaidLinkHandler(context)
    }
}
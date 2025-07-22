package com.north.mobile.data.plaid

actual class PlaidLinkHandlerFactory {
    actual fun create(): PlaidLinkHandler {
        return IOSPlaidLinkHandler()
    }
}
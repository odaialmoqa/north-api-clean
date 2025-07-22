import SwiftUI
import ComposeApp

@main
struct NorthApp: App {
    @StateObject private var biometricAuthManager = BiometricAuthManager()
    @StateObject private var siriShortcutsManager = SiriShortcutsManager()
    
    init() {
        // Initialize Kotlin Multiplatform shared module
        KoinInitKt.doInitKoin()
    }
    
    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(biometricAuthManager)
                .environmentObject(siriShortcutsManager)
                .onAppear {
                    setupSiriShortcuts()
                }
        }
    }
    
    private func setupSiriShortcuts() {
        siriShortcutsManager.setupDefaultShortcuts()
    }
}
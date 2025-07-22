import Foundation
import Intents
import IntentsUI
import SwiftUI

@MainActor
class SiriShortcutsManager: ObservableObject {
    @Published var availableShortcuts: [INShortcut] = []
    @Published var donatedShortcuts: [INShortcut] = []
    
    init() {
        setupAvailableShortcuts()
    }
    
    func setupDefaultShortcuts() {
        // Donate common shortcuts that users might want to use
        donateCheckBalanceShortcut()
        donateViewGoalsShortcut()
        donateAddExpenseShortcut()
        donateViewInsightsShortcut()
    }
    
    private func setupAvailableShortcuts() {
        availableShortcuts = [
            createCheckBalanceShortcut(),
            createViewGoalsShortcut(),
            createAddExpenseShortcut(),
            createViewInsightsShortcut(),
            createCheckStreaksShortcut(),
            createViewRecommendationsShortcut()
        ]
    }
    
    // MARK: - Check Balance Shortcut
    private func createCheckBalanceShortcut() -> INShortcut {
        let intent = CheckBalanceIntent()
        intent.suggestedInvocationPhrase = "Check my balance in North"
        
        return INShortcut(intent: intent)!
    }
    
    private func donateCheckBalanceShortcut() {
        let intent = CheckBalanceIntent()
        intent.suggestedInvocationPhrase = "Check my balance in North"
        
        let interaction = INInteraction(intent: intent, response: nil)
        interaction.donate { error in
            if let error = error {
                print("Failed to donate check balance shortcut: \(error)")
            }
        }
    }
    
    // MARK: - View Goals Shortcut
    private func createViewGoalsShortcut() -> INShortcut {
        let intent = ViewGoalsIntent()
        intent.suggestedInvocationPhrase = "Show my financial goals"
        
        return INShortcut(intent: intent)!
    }
    
    private func donateViewGoalsShortcut() {
        let intent = ViewGoalsIntent()
        intent.suggestedInvocationPhrase = "Show my financial goals"
        
        let interaction = INInteraction(intent: intent, response: nil)
        interaction.donate { error in
            if let error = error {
                print("Failed to donate view goals shortcut: \(error)")
            }
        }
    }
    
    // MARK: - Add Expense Shortcut
    private func createAddExpenseShortcut() -> INShortcut {
        let intent = AddExpenseIntent()
        intent.suggestedInvocationPhrase = "Add expense to North"
        
        return INShortcut(intent: intent)!
    }
    
    private func donateAddExpenseShortcut() {
        let intent = AddExpenseIntent()
        intent.suggestedInvocationPhrase = "Add expense to North"
        
        let interaction = INInteraction(intent: intent, response: nil)
        interaction.donate { error in
            if let error = error {
                print("Failed to donate add expense shortcut: \(error)")
            }
        }
    }
    
    // MARK: - View Insights Shortcut
    private func createViewInsightsShortcut() -> INShortcut {
        let intent = ViewInsightsIntent()
        intent.suggestedInvocationPhrase = "Show my spending insights"
        
        return INShortcut(intent: intent)!
    }
    
    private func donateViewInsightsShortcut() {
        let intent = ViewInsightsIntent()
        intent.suggestedInvocationPhrase = "Show my spending insights"
        
        let interaction = INInteraction(intent: intent, response: nil)
        interaction.donate { error in
            if let error = error {
                print("Failed to donate view insights shortcut: \(error)")
            }
        }
    }
    
    // MARK: - Check Streaks Shortcut
    private func createCheckStreaksShortcut() -> INShortcut {
        let intent = CheckStreaksIntent()
        intent.suggestedInvocationPhrase = "Check my financial streaks"
        
        return INShortcut(intent: intent)!
    }
    
    // MARK: - View Recommendations Shortcut
    private func createViewRecommendationsShortcut() -> INShortcut {
        let intent = ViewRecommendationsIntent()
        intent.suggestedInvocationPhrase = "Show my financial recommendations"
        
        return INShortcut(intent: intent)!
    }
    
    // MARK: - Shortcut Management
    func addShortcutToSiri(_ shortcut: INShortcut, completion: @escaping (Bool) -> Void) {
        let viewController = INUIAddVoiceShortcutViewController(shortcut: shortcut)
        viewController.delegate = SiriShortcutDelegate { success in
            completion(success)
        }
        
        // Present the view controller
        if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
           let window = windowScene.windows.first {
            window.rootViewController?.present(viewController, animated: true)
        }
    }
    
    func removeShortcut(_ shortcut: INShortcut) {
        // Remove from donated shortcuts
        if let voiceShortcut = shortcut.intent as? INIntent {
            INVoiceShortcutCenter.shared.getAllVoiceShortcuts { voiceShortcuts, error in
                guard let voiceShortcuts = voiceShortcuts else { return }
                
                for voiceShortcut in voiceShortcuts {
                    if voiceShortcut.shortcut.intent?.intentDescription == voiceShortcut.intentDescription {
                        INVoiceShortcutCenter.shared.removeVoiceShortcut(voiceShortcut) { error in
                            if let error = error {
                                print("Failed to remove shortcut: \(error)")
                            }
                        }
                    }
                }
            }
        }
    }
}

// MARK: - Intent Definitions
class CheckBalanceIntent: INIntent {
    override var intentDescription: String {
        return "Check account balance in North app"
    }
}

class ViewGoalsIntent: INIntent {
    override var intentDescription: String {
        return "View financial goals in North app"
    }
}

class AddExpenseIntent: INIntent {
    override var intentDescription: String {
        return "Add expense in North app"
    }
}

class ViewInsightsIntent: INIntent {
    override var intentDescription: String {
        return "View spending insights in North app"
    }
}

class CheckStreaksIntent: INIntent {
    override var intentDescription: String {
        return "Check financial streaks in North app"
    }
}

class ViewRecommendationsIntent: INIntent {
    override var intentDescription: String {
        return "View financial recommendations in North app"
    }
}

// MARK: - Siri Shortcut Delegate
class SiriShortcutDelegate: NSObject, INUIAddVoiceShortcutViewControllerDelegate {
    private let completion: (Bool) -> Void
    
    init(completion: @escaping (Bool) -> Void) {
        self.completion = completion
    }
    
    func addVoiceShortcutViewController(_ controller: INUIAddVoiceShortcutViewController, didFinishWith voiceShortcut: INVoiceShortcut?, error: Error?) {
        controller.dismiss(animated: true)
        completion(voiceShortcut != nil)
    }
    
    func addVoiceShortcutViewControllerDidCancel(_ controller: INUIAddVoiceShortcutViewController) {
        controller.dismiss(animated: true)
        completion(false)
    }
}

// MARK: - SwiftUI Integration
struct SiriShortcutsView: View {
    @StateObject private var shortcutsManager = SiriShortcutsManager()
    
    var body: some View {
        NavigationView {
            List {
                Section("Available Shortcuts") {
                    ForEach(shortcutsManager.availableShortcuts, id: \.intent?.intentDescription) { shortcut in
                        HStack {
                            VStack(alignment: .leading) {
                                Text(shortcut.intent?.intentDescription ?? "Unknown")
                                    .font(.headline)
                                if let phrase = (shortcut.intent as? CheckBalanceIntent)?.suggestedInvocationPhrase {
                                    Text("Say: \"\(phrase)\"")
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                }
                            }
                            
                            Spacer()
                            
                            Button("Add to Siri") {
                                shortcutsManager.addShortcutToSiri(shortcut) { success in
                                    // Handle success/failure
                                }
                            }
                            .buttonStyle(.bordered)
                        }
                    }
                }
            }
            .navigationTitle("Siri Shortcuts")
        }
    }
}
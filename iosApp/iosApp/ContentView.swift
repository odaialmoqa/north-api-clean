import SwiftUI
import ComposeApp

struct ContentView: View {
    @EnvironmentObject private var biometricAuthManager: BiometricAuthManager
    @EnvironmentObject private var siriShortcutsManager: SiriShortcutsManager
    @State private var isAuthenticated = false
    @State private var showingAuthenticationError = false
    @State private var authenticationErrorMessage = ""
    
    var body: some View {
        Group {
            if isAuthenticated {
                // Main app content using Compose Multiplatform
                ComposeView()
                    .ignoresSafeArea(.all, edges: .bottom)
            } else {
                // iOS-specific authentication screen following HIG
                AuthenticationView()
            }
        }
        .onAppear {
            checkAuthenticationStatus()
        }
        .alert("Authentication Error", isPresented: $showingAuthenticationError) {
            Button("Try Again") {
                authenticateUser()
            }
            Button("Cancel", role: .cancel) { }
        } message: {
            Text(authenticationErrorMessage)
        }
    }
    
    private func checkAuthenticationStatus() {
        if biometricAuthManager.isAuthenticationRequired {
            authenticateUser()
        } else {
            isAuthenticated = true
        }
    }
    
    private func authenticateUser() {
        Task {
            do {
                let success = try await biometricAuthManager.authenticateUser()
                await MainActor.run {
                    isAuthenticated = success
                }
            } catch {
                await MainActor.run {
                    authenticationErrorMessage = error.localizedDescription
                    showingAuthenticationError = true
                }
            }
        }
    }
}

struct AuthenticationView: View {
    @EnvironmentObject private var biometricAuthManager: BiometricAuthManager
    @State private var showingPINEntry = false
    
    var body: some View {
        VStack(spacing: 32) {
            Spacer()
            
            // App logo and branding
            VStack(spacing: 16) {
                Image(systemName: "dollarsign.circle.fill")
                    .font(.system(size: 80))
                    .foregroundColor(.blue)
                
                Text("North")
                    .font(.largeTitle)
                    .fontWeight(.bold)
                    .foregroundColor(.primary)
                
                Text("Your Intelligent Finance Partner")
                    .font(.headline)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
            }
            
            Spacer()
            
            // Authentication options
            VStack(spacing: 16) {
                if biometricAuthManager.biometricType != .none {
                    Button(action: {
                        Task {
                            try await biometricAuthManager.authenticateUser()
                        }
                    }) {
                        HStack {
                            Image(systemName: biometricAuthManager.biometricType == .faceID ? "faceid" : "touchid")
                                .font(.title2)
                            Text("Unlock with \(biometricAuthManager.biometricType == .faceID ? "Face ID" : "Touch ID")")
                                .fontWeight(.medium)
                        }
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(Color.blue)
                        .foregroundColor(.white)
                        .cornerRadius(12)
                    }
                }
                
                Button(action: {
                    showingPINEntry = true
                }) {
                    HStack {
                        Image(systemName: "lock.fill")
                            .font(.title2)
                        Text("Enter PIN")
                            .fontWeight(.medium)
                    }
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(Color.secondary.opacity(0.1))
                    .foregroundColor(.primary)
                    .cornerRadius(12)
                }
            }
            .padding(.horizontal, 32)
            
            Spacer()
            
            // Security message
            Text("Your financial data is protected with bank-level security")
                .font(.caption)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)
        }
        .sheet(isPresented: $showingPINEntry) {
            PINEntryView()
        }
    }
}

struct PINEntryView: View {
    @Environment(\.dismiss) private var dismiss
    @EnvironmentObject private var biometricAuthManager: BiometricAuthManager
    @State private var pin = ""
    @State private var showingError = false
    @State private var errorMessage = ""
    
    var body: some View {
        NavigationView {
            VStack(spacing: 32) {
                Text("Enter your PIN")
                    .font(.title2)
                    .fontWeight(.semibold)
                
                // PIN dots display
                HStack(spacing: 16) {
                    ForEach(0..<6, id: \.self) { index in
                        Circle()
                            .fill(index < pin.count ? Color.blue : Color.gray.opacity(0.3))
                            .frame(width: 16, height: 16)
                    }
                }
                
                // Number pad
                LazyVGrid(columns: Array(repeating: GridItem(.flexible()), count: 3), spacing: 16) {
                    ForEach(1...9, id: \.self) { number in
                        Button(action: {
                            if pin.count < 6 {
                                pin += "\(number)"
                                if pin.count == 6 {
                                    validatePIN()
                                }
                            }
                        }) {
                            Text("\(number)")
                                .font(.title)
                                .fontWeight(.medium)
                                .frame(width: 80, height: 80)
                                .background(Color.gray.opacity(0.1))
                                .cornerRadius(40)
                        }
                        .buttonStyle(PlainButtonStyle())
                    }
                    
                    // Empty space
                    Color.clear
                        .frame(width: 80, height: 80)
                    
                    // Zero button
                    Button(action: {
                        if pin.count < 6 {
                            pin += "0"
                            if pin.count == 6 {
                                validatePIN()
                            }
                        }
                    }) {
                        Text("0")
                            .font(.title)
                            .fontWeight(.medium)
                            .frame(width: 80, height: 80)
                            .background(Color.gray.opacity(0.1))
                            .cornerRadius(40)
                    }
                    .buttonStyle(PlainButtonStyle())
                    
                    // Delete button
                    Button(action: {
                        if !pin.isEmpty {
                            pin.removeLast()
                        }
                    }) {
                        Image(systemName: "delete.left")
                            .font(.title2)
                            .frame(width: 80, height: 80)
                            .background(Color.gray.opacity(0.1))
                            .cornerRadius(40)
                    }
                    .buttonStyle(PlainButtonStyle())
                }
                
                Spacer()
            }
            .padding()
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") {
                        dismiss()
                    }
                }
            }
            .alert("Invalid PIN", isPresented: $showingError) {
                Button("Try Again") {
                    pin = ""
                }
            } message: {
                Text(errorMessage)
            }
        }
    }
    
    private func validatePIN() {
        Task {
            do {
                let success = try await biometricAuthManager.validatePIN(pin)
                await MainActor.run {
                    if success {
                        dismiss()
                    } else {
                        errorMessage = "Incorrect PIN. Please try again."
                        showingError = true
                        pin = ""
                    }
                }
            } catch {
                await MainActor.run {
                    errorMessage = error.localizedDescription
                    showingError = true
                    pin = ""
                }
            }
        }
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        return MainViewControllerKt.MainViewController()
    }
    
    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {
        // Updates can be handled here if needed
    }
}

#Preview {
    ContentView()
        .environmentObject(BiometricAuthManager())
        .environmentObject(SiriShortcutsManager())
}
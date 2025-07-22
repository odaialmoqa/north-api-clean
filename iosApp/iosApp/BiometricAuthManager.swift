import Foundation
import LocalAuthentication
import SwiftUI

enum BiometricType {
    case none
    case touchID
    case faceID
}

enum AuthenticationError: LocalizedError {
    case biometricNotAvailable
    case biometricNotEnrolled
    case authenticationFailed
    case userCancel
    case userFallback
    case systemCancel
    case passcodeNotSet
    case biometricLockout
    case evaluationFailed
    case invalidCredentials
    
    var errorDescription: String? {
        switch self {
        case .biometricNotAvailable:
            return "Biometric authentication is not available on this device."
        case .biometricNotEnrolled:
            return "Biometric authentication is not set up. Please set up Face ID or Touch ID in Settings."
        case .authenticationFailed:
            return "Authentication failed. Please try again."
        case .userCancel:
            return "Authentication was cancelled."
        case .userFallback:
            return "User chose to enter password."
        case .systemCancel:
            return "Authentication was cancelled by the system."
        case .passcodeNotSet:
            return "Passcode is not set on the device."
        case .biometricLockout:
            return "Biometric authentication is locked. Please try again later."
        case .evaluationFailed:
            return "Authentication evaluation failed."
        case .invalidCredentials:
            return "Invalid credentials provided."
        }
    }
}

@MainActor
class BiometricAuthManager: ObservableObject {
    @Published var isAuthenticated = false
    @Published var biometricType: BiometricType = .none
    @Published var isAuthenticationRequired = true
    
    private let context = LAContext()
    private let keychain = KeychainManager()
    
    init() {
        checkBiometricSupport()
        checkAuthenticationRequirement()
    }
    
    private func checkBiometricSupport() {
        var error: NSError?
        
        guard context.canEvaluatePolicy(.deviceOwnerAuthenticationWithBiometrics, error: &error) else {
            biometricType = .none
            return
        }
        
        switch context.biometryType {
        case .faceID:
            biometricType = .faceID
        case .touchID:
            biometricType = .touchID
        default:
            biometricType = .none
        }
    }
    
    private func checkAuthenticationRequirement() {
        // Check if user has previously authenticated and session is still valid
        if let lastAuthTime = keychain.getLastAuthenticationTime(),
           Date().timeIntervalSince(lastAuthTime) < 300 { // 5 minutes session timeout
            isAuthenticationRequired = false
            isAuthenticated = true
        }
    }
    
    func authenticateUser() async throws -> Bool {
        let context = LAContext()
        context.localizedCancelTitle = "Enter PIN"
        context.localizedFallbackTitle = "Use PIN"
        
        do {
            let success = try await context.evaluatePolicy(
                .deviceOwnerAuthenticationWithBiometrics,
                localizedReason: "Authenticate to access your financial data"
            )
            
            if success {
                await MainActor.run {
                    self.isAuthenticated = true
                    self.isAuthenticationRequired = false
                }
                keychain.saveLastAuthenticationTime(Date())
                return true
            }
            return false
        } catch let error as LAError {
            throw mapLAError(error)
        } catch {
            throw AuthenticationError.evaluationFailed
        }
    }
    
    func validatePIN(_ pin: String) async throws -> Bool {
        // In a real app, this would validate against a securely stored PIN
        // For demo purposes, we'll use a simple validation
        guard let storedPIN = keychain.getPIN() else {
            // If no PIN is stored, set this as the new PIN (first time setup)
            keychain.savePIN(pin)
            await MainActor.run {
                self.isAuthenticated = true
                self.isAuthenticationRequired = false
            }
            keychain.saveLastAuthenticationTime(Date())
            return true
        }
        
        if pin == storedPIN {
            await MainActor.run {
                self.isAuthenticated = true
                self.isAuthenticationRequired = false
            }
            keychain.saveLastAuthenticationTime(Date())
            return true
        } else {
            throw AuthenticationError.invalidCredentials
        }
    }
    
    func logout() {
        isAuthenticated = false
        isAuthenticationRequired = true
        keychain.clearLastAuthenticationTime()
    }
    
    func setupBiometricAuthentication() async throws -> Bool {
        guard biometricType != .none else {
            throw AuthenticationError.biometricNotAvailable
        }
        
        return try await authenticateUser()
    }
    
    private func mapLAError(_ error: LAError) -> AuthenticationError {
        switch error.code {
        case .biometryNotAvailable:
            return .biometricNotAvailable
        case .biometryNotEnrolled:
            return .biometricNotEnrolled
        case .authenticationFailed:
            return .authenticationFailed
        case .userCancel:
            return .userCancel
        case .userFallback:
            return .userFallback
        case .systemCancel:
            return .systemCancel
        case .passcodeNotSet:
            return .passcodeNotSet
        case .biometryLockout:
            return .biometricLockout
        default:
            return .evaluationFailed
        }
    }
}

// MARK: - Keychain Manager
class KeychainManager {
    private let service = "com.north.mobile.keychain"
    private let pinKey = "user_pin"
    private let lastAuthKey = "last_auth_time"
    
    func savePIN(_ pin: String) {
        let data = pin.data(using: .utf8)!
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: pinKey,
            kSecValueData as String: data
        ]
        
        SecItemDelete(query as CFDictionary)
        SecItemAdd(query as CFDictionary, nil)
    }
    
    func getPIN() -> String? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: pinKey,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]
        
        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        
        guard status == errSecSuccess,
              let data = result as? Data,
              let pin = String(data: data, encoding: .utf8) else {
            return nil
        }
        
        return pin
    }
    
    func saveLastAuthenticationTime(_ date: Date) {
        let data = try! JSONEncoder().encode(date)
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: lastAuthKey,
            kSecValueData as String: data
        ]
        
        SecItemDelete(query as CFDictionary)
        SecItemAdd(query as CFDictionary, nil)
    }
    
    func getLastAuthenticationTime() -> Date? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: lastAuthKey,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]
        
        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        
        guard status == errSecSuccess,
              let data = result as? Data,
              let date = try? JSONDecoder().decode(Date.self, from: data) else {
            return nil
        }
        
        return date
    }
    
    func clearLastAuthenticationTime() {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: lastAuthKey
        ]
        
        SecItemDelete(query as CFDictionary)
    }
}
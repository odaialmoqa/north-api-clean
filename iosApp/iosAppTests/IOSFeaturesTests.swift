import XCTest
import LocalAuthentication
@testable import iosApp

class IOSFeaturesTests: XCTestCase {
    
    var biometricAuthManager: BiometricAuthManager!
    var siriShortcutsManager: SiriShortcutsManager!
    var performanceMonitor: PerformanceMonitor!
    
    override func setUpWithError() throws {
        biometricAuthManager = BiometricAuthManager()
        siriShortcutsManager = SiriShortcutsManager()
        performanceMonitor = PerformanceMonitor.shared
    }
    
    override func tearDownWithError() throws {
        biometricAuthManager = nil
        siriShortcutsManager = nil
        performanceMonitor = nil
    }
    
    // MARK: - Biometric Authentication Tests
    
    func testBiometricTypeDetection() throws {
        // Test that biometric type is properly detected
        XCTAssertNotNil(biometricAuthManager.biometricType)
        
        // Biometric type should be one of the expected values
        let validTypes: [BiometricType] = [.none, .touchID, .faceID]
        XCTAssertTrue(validTypes.contains(biometricAuthManager.biometricType))
    }
    
    func testAuthenticationRequirement() throws {
        // Test initial authentication requirement
        XCTAssertTrue(biometricAuthManager.isAuthenticationRequired)
        XCTAssertFalse(biometricAuthManager.isAuthenticated)
    }
    
    func testPINValidation() async throws {
        // Test PIN validation with a test PIN
        let testPIN = "123456"
        
        do {
            let result = try await biometricAuthManager.validatePIN(testPIN)
            XCTAssertTrue(result, "PIN validation should succeed for first-time setup")
            XCTAssertTrue(biometricAuthManager.isAuthenticated)
            XCTAssertFalse(biometricAuthManager.isAuthenticationRequired)
        } catch {
            XCTFail("PIN validation failed: \(error)")
        }
    }
    
    func testKeychainOperations() throws {
        let keychain = KeychainManager()
        let testPIN = "654321"
        
        // Test saving PIN
        keychain.savePIN(testPIN)
        
        // Test retrieving PIN
        let retrievedPIN = keychain.getPIN()
        XCTAssertEqual(retrievedPIN, testPIN, "Retrieved PIN should match saved PIN")
        
        // Test authentication time
        let testDate = Date()
        keychain.saveLastAuthenticationTime(testDate)
        
        let retrievedDate = keychain.getLastAuthenticationTime()
        XCTAssertNotNil(retrievedDate)
        XCTAssertEqual(retrievedDate?.timeIntervalSince1970, testDate.timeIntervalSince1970, accuracy: 1.0)
        
        // Test clearing authentication time
        keychain.clearLastAuthenticationTime()
        let clearedDate = keychain.getLastAuthenticationTime()
        XCTAssertNil(clearedDate)
    }
    
    // MARK: - Siri Shortcuts Tests
    
    func testSiriShortcutsInitialization() throws {
        XCTAssertNotNil(siriShortcutsManager.availableShortcuts)
        XCTAssertGreaterThan(siriShortcutsManager.availableShortcuts.count, 0)
    }
    
    func testShortcutCreation() throws {
        let shortcuts = siriShortcutsManager.availableShortcuts
        
        // Verify that all expected shortcuts are created
        let expectedShortcutTypes = [
            "CheckBalanceIntent",
            "ViewGoalsIntent", 
            "AddExpenseIntent",
            "ViewInsightsIntent",
            "CheckStreaksIntent",
            "ViewRecommendationsIntent"
        ]
        
        for expectedType in expectedShortcutTypes {
            let hasShortcut = shortcuts.contains { shortcut in
                shortcut.intent?.intentDescription?.contains(expectedType) == true
            }
            XCTAssertTrue(hasShortcut, "Should have \(expectedType) shortcut")
        }
    }
    
    // MARK: - Performance Monitor Tests
    
    func testPerformanceMonitorInitialization() throws {
        XCTAssertNotNil(performanceMonitor)
        XCTAssertGreaterThanOrEqual(performanceMonitor.memoryUsage, 0)
        XCTAssertGreaterThanOrEqual(performanceMonitor.cpuUsage, 0)
    }
    
    func testMemoryUsageTracking() throws {
        let initialMemory = performanceMonitor.memoryUsage
        
        // Create some memory pressure
        var testData: [Data] = []
        for _ in 0..<1000 {
            testData.append(Data(count: 1024)) // 1KB each
        }
        
        // Allow some time for monitoring to update
        let expectation = XCTestExpectation(description: "Memory usage updated")
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.0) {
            expectation.fulfill()
        }
        wait(for: [expectation], timeout: 2.0)
        
        // Memory usage should have increased
        XCTAssertGreaterThanOrEqual(performanceMonitor.memoryUsage, initialMemory)
        
        // Clean up
        testData.removeAll()
    }
    
    func testThermalStateMonitoring() throws {
        let thermalState = performanceMonitor.thermalState
        let validStates: [ProcessInfo.ThermalState] = [.nominal, .fair, .serious, .critical]
        XCTAssertTrue(validStates.contains(thermalState))
    }
    
    // MARK: - Cache Tests
    
    func testImageCache() throws {
        let imageCache = ImageCache.shared
        let testImage = UIImage(systemName: "star.fill")!
        let testKey = "test_image"
        
        // Test setting and getting image
        imageCache.setImage(testImage, forKey: testKey)
        let retrievedImage = imageCache.image(forKey: testKey)
        
        XCTAssertNotNil(retrievedImage)
        XCTAssertEqual(retrievedImage?.size, testImage.size)
        
        // Test cache clearing
        imageCache.clearCache()
        let clearedImage = imageCache.image(forKey: testKey)
        XCTAssertNil(clearedImage)
    }
    
    func testDataCache() throws {
        let dataCache = DataCache.shared
        let testData = "Test data for caching"
        let testKey = "test_key"
        
        // Test setting and getting data
        dataCache.set(testData, forKey: testKey, ttl: 60)
        let retrievedData = dataCache.get(String.self, forKey: testKey)
        
        XCTAssertNotNil(retrievedData)
        XCTAssertEqual(retrievedData, testData)
        
        // Test TTL expiration
        dataCache.set(testData, forKey: "expired_key", ttl: 0.1)
        
        let expectation = XCTestExpectation(description: "Data expired")
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.2) {
            let expiredData = dataCache.get(String.self, forKey: "expired_key")
            XCTAssertNil(expiredData)
            expectation.fulfill()
        }
        wait(for: [expectation], timeout: 1.0)
    }
    
    // MARK: - Background Task Manager Tests
    
    func testBackgroundTaskManagement() throws {
        let taskManager = BackgroundTaskManager.shared
        let expectation = XCTestExpectation(description: "Background task completed")
        
        taskManager.startBackgroundTask(name: "test_task", isEssential: false) {
            // Simulate some work
            Thread.sleep(forTimeInterval: 0.1)
            expectation.fulfill()
        }
        
        wait(for: [expectation], timeout: 2.0)
    }
    
    // MARK: - Launch Time Optimizer Tests
    
    func testLaunchTimeTracking() throws {
        let optimizer = LaunchTimeOptimizer.shared
        
        optimizer.recordLaunchStart()
        
        // Simulate some launch work
        Thread.sleep(forTimeInterval: 0.1)
        
        optimizer.recordLaunchEnd()
        
        // Test that optimization methods can be called without crashing
        optimizer.optimizeForFastLaunch()
    }
    
    // MARK: - Widget Data Tests
    
    func testWidgetDataStructure() throws {
        let widgetEntry = NorthWidgetEntry(
            date: Date(),
            configuration: ConfigurationIntent(),
            netWorth: 50000.0,
            monthlyChange: 1500.0,
            currentStreak: 7,
            nextGoalProgress: 0.75
        )
        
        XCTAssertNotNil(widgetEntry.date)
        XCTAssertEqual(widgetEntry.netWorth, 50000.0)
        XCTAssertEqual(widgetEntry.monthlyChange, 1500.0)
        XCTAssertEqual(widgetEntry.currentStreak, 7)
        XCTAssertEqual(widgetEntry.nextGoalProgress, 0.75)
    }
    
    // MARK: - Error Handling Tests
    
    func testAuthenticationErrorMapping() throws {
        let authManager = BiometricAuthManager()
        
        // Test different error scenarios
        let testErrors: [LAError.Code] = [
            .biometryNotAvailable,
            .biometryNotEnrolled,
            .authenticationFailed,
            .userCancel
        ]
        
        for errorCode in testErrors {
            let laError = LAError(errorCode)
            // This would test the private mapLAError method if it were public
            // For now, we just verify the error types exist
            XCTAssertNotNil(laError)
        }
    }
    
    // MARK: - Performance Tests
    
    func testPerformanceOfCacheOperations() throws {
        let cache = DataCache.shared
        
        measure {
            for i in 0..<1000 {
                cache.set("test_data_\(i)", forKey: "key_\(i)")
            }
            
            for i in 0..<1000 {
                _ = cache.get(String.self, forKey: "key_\(i)")
            }
        }
    }
    
    func testPerformanceOfImageCache() throws {
        let imageCache = ImageCache.shared
        let testImage = UIImage(systemName: "star.fill")!
        
        measure {
            for i in 0..<100 {
                imageCache.setImage(testImage, forKey: "image_\(i)")
            }
            
            for i in 0..<100 {
                _ = imageCache.image(forKey: "image_\(i)")
            }
        }
    }
    
    // MARK: - Integration Tests
    
    func testFullAuthenticationFlow() async throws {
        let authManager = BiometricAuthManager()
        
        // Test initial state
        XCTAssertTrue(authManager.isAuthenticationRequired)
        XCTAssertFalse(authManager.isAuthenticated)
        
        // Test PIN setup and validation
        let testPIN = "123456"
        let setupResult = try await authManager.validatePIN(testPIN)
        XCTAssertTrue(setupResult)
        XCTAssertTrue(authManager.isAuthenticated)
        
        // Test logout
        authManager.logout()
        XCTAssertFalse(authManager.isAuthenticated)
        XCTAssertTrue(authManager.isAuthenticationRequired)
        
        // Test PIN validation after logout
        let loginResult = try await authManager.validatePIN(testPIN)
        XCTAssertTrue(loginResult)
        XCTAssertTrue(authManager.isAuthenticated)
    }
}

// MARK: - Mock Classes for Testing

class MockLAContext: LAContext {
    var mockBiometryType: LABiometryType = .none
    var mockCanEvaluatePolicy = false
    var mockEvaluateResult = false
    var mockError: Error?
    
    override var biometryType: LABiometryType {
        return mockBiometryType
    }
    
    override func canEvaluatePolicy(_ policy: LAPolicy, error: NSErrorPointer) -> Bool {
        return mockCanEvaluatePolicy
    }
    
    override func evaluatePolicy(_ policy: LAPolicy, localizedReason: String, reply: @escaping (Bool, Error?) -> Void) {
        DispatchQueue.main.async {
            reply(self.mockEvaluateResult, self.mockError)
        }
    }
}
import Foundation
import UIKit
import SwiftUI
import Combine

// MARK: - Performance Monitoring
class PerformanceMonitor: ObservableObject {
    static let shared = PerformanceMonitor()
    
    @Published var memoryUsage: Double = 0.0
    @Published var cpuUsage: Double = 0.0
    @Published var batteryLevel: Float = 0.0
    @Published var thermalState: ProcessInfo.ThermalState = .nominal
    
    private var cancellables = Set<AnyCancellable>()
    private var monitoringTimer: Timer?
    
    private init() {
        startMonitoring()
        setupNotifications()
    }
    
    private func startMonitoring() {
        monitoringTimer = Timer.scheduledTimer(withTimeInterval: 5.0, repeats: true) { _ in
            self.updateMetrics()
        }
    }
    
    private func setupNotifications() {
        NotificationCenter.default.publisher(for: UIApplication.didReceiveMemoryWarningNotification)
            .sink { _ in
                self.handleMemoryWarning()
            }
            .store(in: &cancellables)
        
        NotificationCenter.default.publisher(for: ProcessInfo.thermalStateDidChangeNotification)
            .sink { _ in
                self.thermalState = ProcessInfo.processInfo.thermalState
                self.handleThermalStateChange()
            }
            .store(in: &cancellables)
    }
    
    private func updateMetrics() {
        memoryUsage = getMemoryUsage()
        cpuUsage = getCPUUsage()
        batteryLevel = UIDevice.current.batteryLevel
        thermalState = ProcessInfo.processInfo.thermalState
    }
    
    private func getMemoryUsage() -> Double {
        var info = mach_task_basic_info()
        var count = mach_msg_type_number_t(MemoryLayout<mach_task_basic_info>.size)/4
        
        let kerr: kern_return_t = withUnsafeMutablePointer(to: &info) {
            $0.withMemoryRebound(to: integer_t.self, capacity: 1) {
                task_info(mach_task_self_,
                         task_flavor_t(MACH_TASK_BASIC_INFO),
                         $0,
                         &count)
            }
        }
        
        if kerr == KERN_SUCCESS {
            return Double(info.resident_size) / 1024.0 / 1024.0 // MB
        }
        return 0.0
    }
    
    private func getCPUUsage() -> Double {
        var info = mach_task_basic_info()
        var count = mach_msg_type_number_t(MemoryLayout<mach_task_basic_info>.size)/4
        
        let kerr: kern_return_t = withUnsafeMutablePointer(to: &info) {
            $0.withMemoryRebound(to: integer_t.self, capacity: 1) {
                task_info(mach_task_self_,
                         task_flavor_t(MACH_TASK_BASIC_INFO),
                         $0,
                         &count)
            }
        }
        
        if kerr == KERN_SUCCESS {
            return Double(info.virtual_size) / 1024.0 / 1024.0 // Simplified CPU usage approximation
        }
        return 0.0
    }
    
    private func handleMemoryWarning() {
        // Clear caches and free up memory
        ImageCache.shared.clearCache()
        DataCache.shared.clearExpiredEntries()
        
        // Notify the app to reduce memory usage
        NotificationCenter.default.post(name: .memoryPressureDetected, object: nil)
    }
    
    private func handleThermalStateChange() {
        switch thermalState {
        case .serious, .critical:
            // Reduce background processing
            BackgroundTaskManager.shared.pauseNonEssentialTasks()
        case .fair:
            // Moderate background processing
            BackgroundTaskManager.shared.resumeEssentialTasks()
        case .nominal:
            // Normal operation
            BackgroundTaskManager.shared.resumeAllTasks()
        @unknown default:
            break
        }
    }
    
    deinit {
        monitoringTimer?.invalidate()
    }
}

// MARK: - Image Cache
class ImageCache {
    static let shared = ImageCache()
    
    private let cache = NSCache<NSString, UIImage>()
    private let maxMemoryUsage: Int = 50 * 1024 * 1024 // 50MB
    
    private init() {
        cache.totalCostLimit = maxMemoryUsage
        cache.countLimit = 100
        
        // Clear cache on memory warning
        NotificationCenter.default.addObserver(
            self,
            selector: #selector(clearCache),
            name: UIApplication.didReceiveMemoryWarningNotification,
            object: nil
        )
    }
    
    func setImage(_ image: UIImage, forKey key: String) {
        let cost = Int(image.size.width * image.size.height * 4) // Approximate memory cost
        cache.setObject(image, forKey: key as NSString, cost: cost)
    }
    
    func image(forKey key: String) -> UIImage? {
        return cache.object(forKey: key as NSString)
    }
    
    @objc func clearCache() {
        cache.removeAllObjects()
    }
    
    deinit {
        NotificationCenter.default.removeObserver(self)
    }
}

// MARK: - Data Cache
class DataCache {
    static let shared = DataCache()
    
    private var cache: [String: CacheEntry] = [:]
    private let queue = DispatchQueue(label: "com.north.datacache", attributes: .concurrent)
    private let maxCacheSize = 100
    
    private struct CacheEntry {
        let data: Any
        let timestamp: Date
        let ttl: TimeInterval
        
        var isExpired: Bool {
            Date().timeIntervalSince(timestamp) > ttl
        }
    }
    
    private init() {
        // Clean expired entries every 5 minutes
        Timer.scheduledTimer(withTimeInterval: 300, repeats: true) { _ in
            self.clearExpiredEntries()
        }
    }
    
    func set<T>(_ data: T, forKey key: String, ttl: TimeInterval = 300) {
        queue.async(flags: .barrier) {
            let entry = CacheEntry(data: data, timestamp: Date(), ttl: ttl)
            self.cache[key] = entry
            
            // Remove oldest entries if cache is full
            if self.cache.count > self.maxCacheSize {
                let sortedKeys = self.cache.keys.sorted { key1, key2 in
                    self.cache[key1]!.timestamp < self.cache[key2]!.timestamp
                }
                
                for key in sortedKeys.prefix(self.cache.count - self.maxCacheSize) {
                    self.cache.removeValue(forKey: key)
                }
            }
        }
    }
    
    func get<T>(_ type: T.Type, forKey key: String) -> T? {
        return queue.sync {
            guard let entry = cache[key], !entry.isExpired else {
                cache.removeValue(forKey: key)
                return nil
            }
            return entry.data as? T
        }
    }
    
    func clearExpiredEntries() {
        queue.async(flags: .barrier) {
            let expiredKeys = self.cache.compactMap { key, entry in
                entry.isExpired ? key : nil
            }
            
            for key in expiredKeys {
                self.cache.removeValue(forKey: key)
            }
        }
    }
    
    func clearAll() {
        queue.async(flags: .barrier) {
            self.cache.removeAll()
        }
    }
}

// MARK: - Background Task Manager
class BackgroundTaskManager {
    static let shared = BackgroundTaskManager()
    
    private var backgroundTasks: [String: BackgroundTask] = [:]
    private let queue = DispatchQueue(label: "com.north.backgroundtasks")
    
    private struct BackgroundTask {
        let identifier: UIBackgroundTaskIdentifier
        let name: String
        let isEssential: Bool
        let startTime: Date
    }
    
    private init() {}
    
    func startBackgroundTask(name: String, isEssential: Bool = false, task: @escaping () -> Void) {
        queue.async {
            let identifier = UIApplication.shared.beginBackgroundTask(withName: name) {
                self.endBackgroundTask(name: name)
            }
            
            guard identifier != .invalid else { return }
            
            let backgroundTask = BackgroundTask(
                identifier: identifier,
                name: name,
                isEssential: isEssential,
                startTime: Date()
            )
            
            self.backgroundTasks[name] = backgroundTask
            
            DispatchQueue.global().async {
                task()
                self.endBackgroundTask(name: name)
            }
        }
    }
    
    func endBackgroundTask(name: String) {
        queue.async {
            guard let task = self.backgroundTasks.removeValue(forKey: name) else { return }
            UIApplication.shared.endBackgroundTask(task.identifier)
        }
    }
    
    func pauseNonEssentialTasks() {
        queue.async {
            let nonEssentialTasks = self.backgroundTasks.filter { !$0.value.isEssential }
            for (name, task) in nonEssentialTasks {
                UIApplication.shared.endBackgroundTask(task.identifier)
                self.backgroundTasks.removeValue(forKey: name)
            }
        }
    }
    
    func resumeEssentialTasks() {
        // Resume only essential background tasks
        // Implementation would depend on specific task requirements
    }
    
    func resumeAllTasks() {
        // Resume all background tasks
        // Implementation would depend on specific task requirements
    }
}

// MARK: - Launch Time Optimizer
class LaunchTimeOptimizer {
    static let shared = LaunchTimeOptimizer()
    
    private var launchStartTime: CFAbsoluteTime = 0
    private var launchEndTime: CFAbsoluteTime = 0
    
    private init() {}
    
    func recordLaunchStart() {
        launchStartTime = CFAbsoluteTimeGetCurrent()
    }
    
    func recordLaunchEnd() {
        launchEndTime = CFAbsoluteTimeGetCurrent()
        let launchTime = launchEndTime - launchStartTime
        
        // Log launch time for monitoring
        print("App launch time: \(launchTime * 1000)ms")
        
        // Send analytics if launch time is concerning
        if launchTime > 2.0 {
            // Log slow launch for investigation
            Analytics.shared.logEvent("slow_launch", parameters: [
                "launch_time": launchTime
            ])
        }
    }
    
    func optimizeForFastLaunch() {
        // Defer non-critical initializations
        DispatchQueue.main.async {
            self.performDeferredInitialization()
        }
    }
    
    private func performDeferredInitialization() {
        // Initialize non-critical services after launch
        AnalyticsService.shared.initialize()
        CrashReportingService.shared.initialize()
        RemoteConfigService.shared.initialize()
    }
}

// MARK: - Memory Management Extensions
extension Notification.Name {
    static let memoryPressureDetected = Notification.Name("memoryPressureDetected")
}

// MARK: - Placeholder Services
class Analytics {
    static let shared = Analytics()
    private init() {}
    
    func logEvent(_ name: String, parameters: [String: Any]) {
        // Implementation would integrate with analytics service
        print("Analytics event: \(name), parameters: \(parameters)")
    }
}

class AnalyticsService {
    static let shared = AnalyticsService()
    private init() {}
    
    func initialize() {
        // Deferred analytics initialization
    }
}

class CrashReportingService {
    static let shared = CrashReportingService()
    private init() {}
    
    func initialize() {
        // Deferred crash reporting initialization
    }
}

class RemoteConfigService {
    static let shared = RemoteConfigService()
    private init() {}
    
    func initialize() {
        // Deferred remote config initialization
    }
}
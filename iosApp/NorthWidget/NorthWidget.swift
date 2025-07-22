import WidgetKit
import SwiftUI
import Intents

// MARK: - Widget Timeline Provider
struct NorthWidgetProvider: IntentTimelineProvider {
    func placeholder(in context: Context) -> NorthWidgetEntry {
        NorthWidgetEntry(
            date: Date(),
            configuration: ConfigurationIntent(),
            netWorth: 47250.00,
            monthlyChange: 1200.00,
            currentStreak: 5,
            nextGoalProgress: 0.68
        )
    }
    
    func getSnapshot(for configuration: ConfigurationIntent, in context: Context, completion: @escaping (NorthWidgetEntry) -> ()) {
        let entry = NorthWidgetEntry(
            date: Date(),
            configuration: configuration,
            netWorth: 47250.00,
            monthlyChange: 1200.00,
            currentStreak: 5,
            nextGoalProgress: 0.68
        )
        completion(entry)
    }
    
    func getTimeline(for configuration: ConfigurationIntent, in context: Context, completion: @escaping (Timeline<NorthWidgetEntry>) -> ()) {
        // In a real app, this would fetch data from the shared Kotlin module
        let currentDate = Date()
        let entry = NorthWidgetEntry(
            date: currentDate,
            configuration: configuration,
            netWorth: 47250.00,
            monthlyChange: 1200.00,
            currentStreak: 5,
            nextGoalProgress: 0.68
        )
        
        // Update every 15 minutes
        let nextUpdate = Calendar.current.date(byAdding: .minute, value: 15, to: currentDate)!
        let timeline = Timeline(entries: [entry], policy: .after(nextUpdate))
        completion(timeline)
    }
}

// MARK: - Widget Entry
struct NorthWidgetEntry: TimelineEntry {
    let date: Date
    let configuration: ConfigurationIntent
    let netWorth: Double
    let monthlyChange: Double
    let currentStreak: Int
    let nextGoalProgress: Double
}

// MARK: - Widget Views
struct NorthWidgetSmallView: View {
    var entry: NorthWidgetEntry
    
    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Image(systemName: "dollarsign.circle.fill")
                    .foregroundColor(.blue)
                    .font(.title2)
                Spacer()
                Text("North")
                    .font(.caption)
                    .fontWeight(.medium)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
            
            VStack(alignment: .leading, spacing: 2) {
                Text("Net Worth")
                    .font(.caption2)
                    .foregroundColor(.secondary)
                
                Text("$\(entry.netWorth, specifier: "%.0f")")
                    .font(.title3)
                    .fontWeight(.bold)
                    .foregroundColor(.primary)
                
                HStack {
                    Image(systemName: entry.monthlyChange >= 0 ? "arrow.up" : "arrow.down")
                        .font(.caption2)
                        .foregroundColor(entry.monthlyChange >= 0 ? .green : .red)
                    
                    Text("$\(abs(entry.monthlyChange), specifier: "%.0f")")
                        .font(.caption2)
                        .foregroundColor(entry.monthlyChange >= 0 ? .green : .red)
                }
            }
            
            Spacer()
            
            HStack {
                Image(systemName: "flame.fill")
                    .foregroundColor(.orange)
                    .font(.caption)
                
                Text("\(entry.currentStreak) day streak")
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }
        }
        .padding()
        .background(Color(.systemBackground))
    }
}

struct NorthWidgetMediumView: View {
    var entry: NorthWidgetEntry
    
    var body: some View {
        HStack(spacing: 16) {
            // Left side - Net Worth
            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    Image(systemName: "dollarsign.circle.fill")
                        .foregroundColor(.blue)
                        .font(.title2)
                    
                    Text("North")
                        .font(.headline)
                        .fontWeight(.semibold)
                }
                
                VStack(alignment: .leading, spacing: 4) {
                    Text("Net Worth")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    
                    Text("$\(entry.netWorth, specifier: "%.0f")")
                        .font(.title2)
                        .fontWeight(.bold)
                        .foregroundColor(.primary)
                    
                    HStack {
                        Image(systemName: entry.monthlyChange >= 0 ? "arrow.up" : "arrow.down")
                            .font(.caption)
                            .foregroundColor(entry.monthlyChange >= 0 ? .green : .red)
                        
                        Text("$\(abs(entry.monthlyChange), specifier: "%.0f") this month")
                            .font(.caption)
                            .foregroundColor(entry.monthlyChange >= 0 ? .green : .red)
                    }
                }
                
                Spacer()
            }
            
            Divider()
            
            // Right side - Goals & Streaks
            VStack(alignment: .leading, spacing: 12) {
                VStack(alignment: .leading, spacing: 4) {
                    Text("Emergency Fund")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    
                    ProgressView(value: entry.nextGoalProgress)
                        .progressViewStyle(LinearProgressViewStyle(tint: .blue))
                    
                    Text("\(Int(entry.nextGoalProgress * 100))% complete")
                        .font(.caption2)
                        .foregroundColor(.secondary)
                }
                
                HStack {
                    Image(systemName: "flame.fill")
                        .foregroundColor(.orange)
                        .font(.caption)
                    
                    Text("\(entry.currentStreak) day streak")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    
                    Spacer()
                    
                    Image(systemName: "star.fill")
                        .foregroundColor(.yellow)
                        .font(.caption)
                    
                    Text("Level 7")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                
                Spacer()
            }
        }
        .padding()
        .background(Color(.systemBackground))
    }
}

struct NorthWidgetLargeView: View {
    var entry: NorthWidgetEntry
    
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            // Header
            HStack {
                Image(systemName: "dollarsign.circle.fill")
                    .foregroundColor(.blue)
                    .font(.title)
                
                VStack(alignment: .leading) {
                    Text("North")
                        .font(.title2)
                        .fontWeight(.bold)
                    
                    Text("Financial Overview")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                
                Spacer()
                
                Text(entry.date, style: .time)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            
            // Net Worth Section
            VStack(alignment: .leading, spacing: 8) {
                Text("Net Worth")
                    .font(.headline)
                    .foregroundColor(.primary)
                
                HStack(alignment: .bottom) {
                    Text("$\(entry.netWorth, specifier: "%.0f")")
                        .font(.largeTitle)
                        .fontWeight(.bold)
                        .foregroundColor(.primary)
                    
                    Spacer()
                    
                    VStack(alignment: .trailing) {
                        HStack {
                            Image(systemName: entry.monthlyChange >= 0 ? "arrow.up" : "arrow.down")
                                .font(.caption)
                                .foregroundColor(entry.monthlyChange >= 0 ? .green : .red)
                            
                            Text("$\(abs(entry.monthlyChange), specifier: "%.0f")")
                                .font(.subheadline)
                                .fontWeight(.medium)
                                .foregroundColor(entry.monthlyChange >= 0 ? .green : .red)
                        }
                        
                        Text("This month")
                            .font(.caption2)
                            .foregroundColor(.secondary)
                    }
                }
            }
            
            Divider()
            
            // Goals and Gamification
            HStack(spacing: 20) {
                VStack(alignment: .leading, spacing: 8) {
                    Text("Emergency Fund")
                        .font(.subheadline)
                        .fontWeight(.medium)
                    
                    ProgressView(value: entry.nextGoalProgress)
                        .progressViewStyle(LinearProgressViewStyle(tint: .blue))
                    
                    Text("$8,500 / $10,000")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                
                VStack(alignment: .leading, spacing: 8) {
                    HStack {
                        Image(systemName: "flame.fill")
                            .foregroundColor(.orange)
                        
                        Text("Streak")
                            .font(.subheadline)
                            .fontWeight(.medium)
                    }
                    
                    Text("\(entry.currentStreak) days")
                        .font(.title3)
                        .fontWeight(.bold)
                        .foregroundColor(.orange)
                    
                    Text("Keep it up!")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
            
            Spacer()
        }
        .padding()
        .background(Color(.systemBackground))
    }
}

// MARK: - Main Widget View
struct NorthWidgetEntryView: View {
    var entry: NorthWidgetProvider.Entry
    @Environment(\.widgetFamily) var family
    
    var body: some View {
        switch family {
        case .systemSmall:
            NorthWidgetSmallView(entry: entry)
        case .systemMedium:
            NorthWidgetMediumView(entry: entry)
        case .systemLarge:
            NorthWidgetLargeView(entry: entry)
        default:
            NorthWidgetSmallView(entry: entry)
        }
    }
}

// MARK: - Widget Configuration
struct NorthWidget: Widget {
    let kind: String = "NorthWidget"
    
    var body: some WidgetConfiguration {
        IntentConfiguration(kind: kind, intent: ConfigurationIntent.self, provider: NorthWidgetProvider()) { entry in
            NorthWidgetEntryView(entry: entry)
        }
        .configurationDisplayName("North Financial Overview")
        .description("Keep track of your net worth, goals, and streaks at a glance.")
        .supportedFamilies([.systemSmall, .systemMedium, .systemLarge])
    }
}

// MARK: - Widget Bundle
@main
struct NorthWidgetBundle: WidgetBundle {
    var body: some Widget {
        NorthWidget()
    }
}

// MARK: - Preview
struct NorthWidget_Previews: PreviewProvider {
    static var previews: some View {
        Group {
            NorthWidgetEntryView(entry: NorthWidgetEntry(
                date: Date(),
                configuration: ConfigurationIntent(),
                netWorth: 47250.00,
                monthlyChange: 1200.00,
                currentStreak: 5,
                nextGoalProgress: 0.68
            ))
            .previewContext(WidgetPreviewContext(family: .systemSmall))
            
            NorthWidgetEntryView(entry: NorthWidgetEntry(
                date: Date(),
                configuration: ConfigurationIntent(),
                netWorth: 47250.00,
                monthlyChange: 1200.00,
                currentStreak: 5,
                nextGoalProgress: 0.68
            ))
            .previewContext(WidgetPreviewContext(family: .systemMedium))
            
            NorthWidgetEntryView(entry: NorthWidgetEntry(
                date: Date(),
                configuration: ConfigurationIntent(),
                netWorth: 47250.00,
                monthlyChange: 1200.00,
                currentStreak: 5,
                nextGoalProgress: 0.68
            ))
            .previewContext(WidgetPreviewContext(family: .systemLarge))
        }
    }
}
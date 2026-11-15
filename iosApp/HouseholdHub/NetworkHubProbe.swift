import Foundation
import Network
import HouseholdHubKit

/// A test connection to the hub, for reading iOS's local network permission (#5).
///
/// iOS has no API to read that permission or show its question (Apple TN3179). A denial only shows
/// as a connection in `.waiting` whose path's `unsatisfiedReason` is `.localNetworkDenied`, so this
/// connects to the hub, reports the first thing that happens, and cancels. Over a VPN the traffic
/// is not local, so it comes up ready. Kotlin's `IosLocalNetworkAccess` maps the report to a status.
final class NetworkHubProbe: NSObject, HubConnectionProbe {

    private let queue = DispatchQueue(label: "com.homelab.household.hub-probe")

    func probe(host: String, port: Int32, timeoutSeconds: Double, report: ProbeReport) {
        guard let nwPort = NWEndpoint.Port(rawValue: UInt16(clamping: port)) else {
            report.failed()
            return
        }
        let connection = NWConnection(host: NWEndpoint.Host(host), port: nwPort, using: .tcp)
        let once = Once()

        connection.stateUpdateHandler = { state in
            switch state {
            case .ready:
                once.run(connection) { report.ready() }
            case .waiting:
                let denied = connection.currentPath?.unsatisfiedReason == .localNetworkDenied
                once.run(connection) { report.waiting(localNetworkDenied: denied) }
            case .failed:
                once.run(connection) { report.failed() }
            default:
                break
            }
        }
        connection.start(queue: queue)
        queue.asyncAfter(deadline: .now() + timeoutSeconds) {
            once.run(connection) { report.timedOut() }
        }
    }
}

/// The first outcome wins; every later one is dropped. Only touched on the probe's serial queue.
private final class Once {
    private var done = false

    func run(_ connection: NWConnection, _ send: () -> Void) {
        guard !done else { return }
        done = true
        connection.cancel()
        send()
    }
}

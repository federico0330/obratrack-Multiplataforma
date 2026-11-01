import SwiftUI
import ComposeApp

@main
struct ObraTrackIOSApp: App {
    var body: some Scene {
        WindowGroup {
            ComposeHost().ignoresSafeArea()
        }
    }
}

private struct ComposeHost: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        let uiTesting = ProcessInfo.processInfo.arguments.contains("-ui-testing")
        return MainViewControllerKt.MainViewController(uiTesting: uiTesting)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

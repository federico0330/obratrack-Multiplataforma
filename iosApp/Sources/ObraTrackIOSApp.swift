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
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

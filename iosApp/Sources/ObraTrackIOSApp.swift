import SwiftUI
import UIKit
import ComposeApp
import GoogleSignIn

@main
struct ObraTrackIOSApp: App {
    var body: some Scene {
        WindowGroup {
            ComposeHost()
                .ignoresSafeArea()
                .onOpenURL { url in _ = GIDSignIn.sharedInstance.handle(url) }
        }
    }
}

private struct ComposeHost: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        let uiTesting = ProcessInfo.processInfo.arguments.contains("-ui-testing")
        return MainViewControllerKt.MainViewController(uiTesting: uiTesting, selectorGoogle: SelectorGoogleIOS())
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

/// Implementa el puerto de Kotlin con Google Sign-In. Los errores viajan como dominio y código del
/// NSError; la clasificación (cancelado, sin conexión, otro) se hace en Kotlin.
private final class SelectorGoogleIOS: NSObject, SelectorCuentaGoogle {
    func pedirIdToken(respuesta: RespuestaSelectorGoogle) {
        guard let clientID = Self.clientID(), let presentador = Self.presentador() else {
            respuesta.sinToken()
            return
        }
        GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: clientID)
        GIDSignIn.sharedInstance.signIn(withPresenting: presentador) { resultado, error in
            if let error = error as NSError? {
                respuesta.error(dominio: error.domain, codigo: Int64(error.code))
            } else if let idToken = resultado?.user.idToken?.tokenString {
                respuesta.exito(idToken: idToken)
            } else {
                respuesta.sinToken()
            }
        }
    }

    /// CLIENT_ID sale del mismo GoogleService-Info.plist (no versionado) que usa Firebase.
    private static func clientID() -> String? {
        guard let path = Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist"),
              let valores = NSDictionary(contentsOfFile: path) else { return nil }
        return valores["CLIENT_ID"] as? String
    }

    private static func presentador() -> UIViewController? {
        var actual = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
            .first(where: \.isKeyWindow)?
            .rootViewController
        while let siguiente = actual?.presentedViewController { actual = siguiente }
        return actual
    }
}

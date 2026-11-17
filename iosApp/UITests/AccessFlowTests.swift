import XCTest

final class AccessFlowTests: XCTestCase {
    func testLoginAndRegistrationNavigation() {
        let app = XCUIApplication()
        app.launchArguments += ["-ui-testing", "-AppleLanguages", "(es)", "-AppleLocale", "es_AR"]
        app.launch()

        XCTAssertTrue(app.staticTexts["Ingresá para continuar"].waitForExistence(timeout: 15))
        tapButton("Crear cuenta", in: app)

        XCTAssertTrue(app.staticTexts["Creá tu cuenta para empezar"].waitForExistence(timeout: 5))
        tapButton("Ya tengo cuenta", in: app)
        XCTAssertTrue(app.staticTexts["Ingresá para continuar"].waitForExistence(timeout: 5))
    }

    private func tapButton(
        _ title: String,
        in app: XCUIApplication,
        file: StaticString = #filePath,
        line: UInt = #line
    ) {
        let button = app.buttons[title]
        for _ in 0..<3 {
            if button.exists && button.isHittable { break }
            app.swipeUp()
        }

        guard button.exists, button.isHittable else {
            XCTFail("No se pudo encontrar y tocar el botón «\(title)» después de desplazar la pantalla.", file: file, line: line)
            return
        }
        button.tap()
    }
}

import XCTest

final class AccessFlowTests: XCTestCase {
    func testLoginAndRegistrationNavigation() {
        let app = XCUIApplication()
        app.launchArguments += ["-ui-testing", "-AppleLanguages", "(es)", "-AppleLocale", "es_AR"]
        app.launch()

        XCTAssertTrue(app.staticTexts["Ingresá para continuar"].waitForExistence(timeout: 15))
        app.buttons["Crear cuenta"].tap()

        XCTAssertTrue(app.staticTexts["Creá tu cuenta para empezar"].waitForExistence(timeout: 5))
        XCTAssertTrue(app.buttons["Ya tengo cuenta"].exists)

        app.buttons["Ya tengo cuenta"].tap()
        XCTAssertTrue(app.staticTexts["Ingresá para continuar"].waitForExistence(timeout: 5))
    }
}

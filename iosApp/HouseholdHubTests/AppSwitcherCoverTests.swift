import SwiftUI
import XCTest
@testable import HouseholdHub

/// The app switcher keeps a picture of the app as it leaves the screen. While secret chats are
/// unlocked, that picture shows a blur instead (#30); the rest of the time it shows the app as is.
final class AppSwitcherCoverTests: XCTestCase {

    func testLeavingWhileSecretChatsAreUnlockedShowsTheCover() {
        XCTAssertTrue(AppSwitcherCover.isShown(phase: .inactive, hides: true))
    }

    func testInTheBackgroundWhileSecretChatsAreUnlockedShowsTheCover() {
        XCTAssertTrue(AppSwitcherCover.isShown(phase: .background, hides: true))
    }

    func testTheAppInUseNeverShowsTheCover() {
        XCTAssertFalse(AppSwitcherCover.isShown(phase: .active, hides: true))
    }

    func testLeavingWithSecretChatsLockedShowsTheAppAsIs() {
        XCTAssertFalse(AppSwitcherCover.isShown(phase: .inactive, hides: false))
    }
}

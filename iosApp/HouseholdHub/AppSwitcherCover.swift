import SwiftUI
import UIKit

/// What the app switcher sees while secret chats are unlocked (#30).
///
/// iOS has no switch to turn its app-switcher picture off, so the window is covered as the scene
/// leaves `.active`: resigning active comes before the picture, entering the background may not.
/// Whether to hide is Kotlin's rule (`AppSwitcherPrivacy`, read through `hidesFromAppSwitcher()`).
enum AppSwitcherCover {

    /// The cover is up whenever the app is not in use and secret chats need hiding.
    static func isShown(phase: ScenePhase, hides: Bool) -> Bool {
        hides && phase != .active
    }
}

/// The cover itself: a blur over the whole window.
struct AppSwitcherBlur: UIViewRepresentable {

    /// The thinnest material, so the screen's layout and colours still show through. The blur
    /// itself is what makes text unreadable; a thicker material only adds tint, which turned the
    /// picture into a flat sheet.
    static let style: UIBlurEffect.Style = .systemUltraThinMaterial

    func makeUIView(context: Context) -> UIVisualEffectView {
        UIVisualEffectView(effect: UIBlurEffect(style: Self.style))
    }

    func updateUIView(_ uiView: UIVisualEffectView, context: Context) {}
}

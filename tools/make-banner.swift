// Draws the Android TV launcher banner (320x180 px, xhdpi) and the Play Store
// TV banner (1280x720 px). Run on a Mac: swift tools/make-banner.swift
import AppKit

func render(width: Int, height: Int, to path: String) {
    let s = CGFloat(width) / 320.0
    let rep = NSBitmapImageRep(
        bitmapDataPlanes: nil, pixelsWide: width, pixelsHigh: height, bitsPerSample: 8,
        samplesPerPixel: 4, hasAlpha: true, isPlanar: false, colorSpaceName: .deviceRGB,
        bytesPerRow: 0, bitsPerPixel: 0)!
    NSGraphicsContext.saveGraphicsState()
    NSGraphicsContext.current = NSGraphicsContext(bitmapImageRep: rep)
    let ctx = NSGraphicsContext.current!.cgContext

    let bg = NSGradient(
        starting: NSColor(red: 0.04, green: 0.06, blue: 0.09, alpha: 1),
        ending: NSColor(red: 0.06, green: 0.09, blue: 0.16, alpha: 1))!
    bg.draw(in: NSRect(x: 0, y: 0, width: width, height: height), angle: -30)

    // Gauge: 240 degree arc open at the bottom, with a needle.
    let cx = 78 * s, cy = 92 * s, r = 40 * s
    ctx.setLineCap(.round)
    ctx.setLineWidth(11 * s)
    ctx.setStrokeColor(NSColor(red: 0.18, green: 0.83, blue: 0.75, alpha: 1).cgColor)
    ctx.addArc(center: CGPoint(x: cx, y: cy), radius: r,
               startAngle: 210 * .pi / 180, endAngle: -30 * .pi / 180, clockwise: true)
    ctx.strokePath()
    ctx.setStrokeColor(NSColor.white.cgColor)
    ctx.setLineWidth(9 * s)
    let a = 60 * CGFloat.pi / 180
    ctx.move(to: CGPoint(x: cx, y: cy))
    ctx.addLine(to: CGPoint(x: cx + cos(a) * r * 0.78, y: cy + sin(a) * r * 0.78))
    ctx.strokePath()
    ctx.setFillColor(NSColor.white.cgColor)
    ctx.fillEllipse(in: CGRect(x: cx - 7 * s, y: cy - 7 * s, width: 14 * s, height: 14 * s))

    let font = NSFont.systemFont(ofSize: 44 * s, weight: .bold)
    let attrs: [NSAttributedString.Key: Any] = [.font: font, .foregroundColor: NSColor.white]
    NSAttributedString(string: "Speedyt", attributes: attrs)
        .draw(at: NSPoint(x: 128 * s, y: 82 * s))
    let sub: [NSAttributedString.Key: Any] = [
        .font: NSFont.systemFont(ofSize: 15 * s, weight: .medium),
        .foregroundColor: NSColor(red: 0.66, green: 0.71, blue: 0.75, alpha: 1)]
    NSAttributedString(string: "Internet speed test", attributes: sub)
        .draw(at: NSPoint(x: 130 * s, y: 58 * s))

    NSGraphicsContext.restoreGraphicsState()
    try! rep.representation(using: .png, properties: [:])!.write(to: URL(fileURLWithPath: path))
}

render(width: 320, height: 180, to: "app/src/main/res/drawable-xhdpi/banner.png")
render(width: 1280, height: 720, to: "docs/play/tv-banner-1280x720.png")

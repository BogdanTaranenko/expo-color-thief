import ExpoModulesCore
import UIKit

public class ExpoColorThiefModule: Module {
    public func definition() -> ModuleDefinition {
        Name("ExpoColorThief")

        AsyncFunction("getColor") { (imageUri: String, quality: Int, ignoreWhite: Bool, promise: Promise) in
            self.loadImage(from: imageUri) { image in
                guard let image = image else {
                    promise.resolve(nil)
                    return
                }

                guard let color = ColorThief.getColor(from: image, quality: quality, ignoreWhite: ignoreWhite) else {
                    promise.resolve(nil)
                    return
                }

                promise.resolve(self.colorToDict(color))
            }
        }

        AsyncFunction("getPalette") { (imageUri: String, colorCount: Int, quality: Int, ignoreWhite: Bool, promise: Promise) in
            self.loadImage(from: imageUri) { image in
                guard let image = image else {
                    promise.resolve(nil)
                    return
                }

                guard let palette = ColorThief.getPalette(from: image, colorCount: colorCount, quality: quality, ignoreWhite: ignoreWhite) else {
                    promise.resolve(nil)
                    return
                }

                let colors = palette.map { self.colorToDict($0) }
                promise.resolve(colors)
            }
        }
    }

    private func colorToDict(_ color: MMCQ.Color) -> [String: Any] {
        let hex = String(format: "#%02x%02x%02x", color.r, color.g, color.b)
        return [
            "rgb": [
                "r": Int(color.r),
                "g": Int(color.g),
                "b": Int(color.b)
            ],
            "hex": hex
        ]
    }

    private func loadImage(from uri: String, completion: @escaping (UIImage?) -> Void) {
        // Handle different URI schemes
        if uri.hasPrefix("file://") {
            let path = String(uri.dropFirst(7))
            let image = UIImage(contentsOfFile: path)
            completion(image)
            return
        }

        if uri.hasPrefix("http://") || uri.hasPrefix("https://") {
            guard let url = URL(string: uri) else {
                completion(nil)
                return
            }

            URLSession.shared.dataTask(with: url) { data, _, error in
                DispatchQueue.main.async {
                    if let data = data, error == nil {
                        completion(UIImage(data: data))
                    } else {
                        completion(nil)
                    }
                }
            }.resume()
            return
        }

        // Handle asset URIs (e.g., from require() or asset library)
        if uri.hasPrefix("asset://") || uri.hasPrefix("assets-library://") {
            // For asset library URIs, we need to use Photos framework
            // This is a simplified implementation
            completion(nil)
            return
        }

        // Handle base64 data URIs
        if uri.hasPrefix("data:image") {
            if let commaIndex = uri.firstIndex(of: ",") {
                let base64String = String(uri[uri.index(after: commaIndex)...])
                if let data = Data(base64Encoded: base64String) {
                    completion(UIImage(data: data))
                    return
                }
            }
            completion(nil)
            return
        }

        // Try as local file path
        if FileManager.default.fileExists(atPath: uri) {
            let image = UIImage(contentsOfFile: uri)
            completion(image)
            return
        }

        // Try as bundle resource
        if let image = UIImage(named: uri) {
            completion(image)
            return
        }

        completion(nil)
    }
}
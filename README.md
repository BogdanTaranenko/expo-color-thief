# expo-color-thief

An Expo module for extracting dominant colors from images using the ColorThief algorithm (MMCQ - Modified Median Cut Quantization).

Based on [ColorThiefSwift](https://github.com/yamoridon/ColorThiefSwift) by Kazuki Ohara and the original [Color Thief](http://lokeshdhakar.com/projects/color-thief/) by Lokesh Dhakar.

## Installation

```bash
npx expo install expo-color-thief
```

Or with npm/yarn:

```bash
npm install expo-color-thief
# or
yarn add expo-color-thief
```

Then rebuild your native app:

```bash
npx expo prebuild
npx expo run:ios
# or
npx expo run:android
```

## Usage

### Get Dominant Color

```typescript
import { getColor } from 'expo-color-thief';

// From a URL
const color = await getColor('https://example.com/image.jpg');
console.log(color?.hex);  // '#ff5733'
console.log(color?.rgb);  // { r: 255, g: 87, b: 51 }

// From a local file
const color = await getColor('file:///path/to/image.jpg');

// With options
const color = await getColor(imageUri, {
  quality: 5,        // 1-10, lower = better quality but slower
  ignoreWhite: true  // ignore white pixels
});
```

### Get Color Palette

```typescript
import { getPalette } from 'expo-color-thief';

// Get 5 colors (default)
const palette = await getPalette('https://example.com/image.jpg');

// Get 8 colors with custom quality
const palette = await getPalette(imageUri, {
  colorCount: 8,
  quality: 5,
  ignoreWhite: true
});

// Use the palette
palette?.forEach(color => {
  console.log(color.hex);
  // '#ff5733', '#3498db', '#2ecc71', ...
});
```

### Utility Functions

```typescript
import { rgbToHex, hexToRgb } from 'expo-color-thief';

// Convert RGB to Hex
const hex = rgbToHex({ r: 255, g: 87, b: 51 });
// '#ff5733'

// Convert Hex to RGB
const rgb = hexToRgb('#ff5733');
// { r: 255, g: 87, b: 51 }
```

## API Reference

### `getColor(imageUri, options?)`

Extract the dominant color from an image.

**Parameters:**
- `imageUri` (string): URI of the image (http://, https://, file://, or data:)
- `options` (optional):
  - `quality` (number): 1-10, lower is better quality but slower. Default: 10
  - `ignoreWhite` (boolean): Skip white pixels. Default: true

**Returns:** `Promise<ColorResult | null>`

### `getPalette(imageUri, options?)`

Extract a color palette from an image.

**Parameters:**
- `imageUri` (string): URI of the image
- `options` (optional):
  - `colorCount` (number): Number of colors to extract. Default: 5
  - `quality` (number): 1-10, lower is better quality but slower. Default: 10
  - `ignoreWhite` (boolean): Skip white pixels. Default: true

**Returns:** `Promise<ColorResult[] | null>`

### Types

```typescript
interface RGB {
  r: number;
  g: number;
  b: number;
}

interface ColorResult {
  rgb: RGB;
  hex: string;
}
```

## Supported Image Sources

- **Remote URLs**: `https://example.com/image.jpg`
- **Local files**: `file:///path/to/image.jpg`
- **Base64 data URIs**: `data:image/png;base64,...`
- **Bundle resources** (iOS): Image name from asset catalog

## Example App

```typescript
import React, { useState, useEffect } from 'react';
import { View, Image, Text, StyleSheet } from 'react-native';
import { getColor, getPalette, ColorResult } from 'expo-color-thief';

const IMAGE_URL = 'https://picsum.photos/400/300';

export default function App() {
  const [dominantColor, setDominantColor] = useState<ColorResult | null>(null);
  const [palette, setPalette] = useState<ColorResult[] | null>(null);

  useEffect(() => {
    async function extractColors() {
      const color = await getColor(IMAGE_URL);
      setDominantColor(color);

      const colors = await getPalette(IMAGE_URL, { colorCount: 6 });
      setPalette(colors);
    }
    extractColors();
  }, []);

  return (
    <View style={styles.container}>
      <Image source={{ uri: IMAGE_URL }} style={styles.image} />

      {dominantColor && (
        <View style={styles.section}>
          <Text style={styles.title}>Dominant Color</Text>
          <View
            style={[styles.colorBox, { backgroundColor: dominantColor.hex }]}
          />
          <Text>{dominantColor.hex}</Text>
        </View>
      )}

      {palette && (
        <View style={styles.section}>
          <Text style={styles.title}>Palette</Text>
          <View style={styles.paletteContainer}>
            {palette.map((color, index) => (
              <View key={index} style={styles.paletteItem}>
                <View
                  style={[styles.colorBox, { backgroundColor: color.hex }]}
                />
                <Text style={styles.hexText}>{color.hex}</Text>
              </View>
            ))}
          </View>
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 20,
    backgroundColor: '#fff',
  },
  image: {
    width: '100%',
    height: 200,
    borderRadius: 8,
  },
  section: {
    marginTop: 20,
  },
  title: {
    fontSize: 18,
    fontWeight: 'bold',
    marginBottom: 10,
  },
  colorBox: {
    width: 50,
    height: 50,
    borderRadius: 4,
    borderWidth: 1,
    borderColor: '#ddd',
  },
  paletteContainer: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 10,
  },
  paletteItem: {
    alignItems: 'center',
  },
  hexText: {
    fontSize: 10,
    marginTop: 4,
  },
});
```

## Performance Tips

1. **Quality setting**: Use higher values (5-10) for large images to improve speed
2. **Image size**: Consider resizing large images before extraction
3. **Caching**: Cache results if analyzing the same image multiple times
4. **Color count**: Request only as many colors as you need

## Credits

- [ColorThiefSwift](https://github.com/yamoridon/ColorThiefSwift) by Kazuki Ohara
- [Color Thief](http://lokeshdhakar.com/projects/color-thief/) by Lokesh Dhakar
- [color-thief-java](https://github.com/SvenWoltmann/color-thief-java) by Sven Woltmann
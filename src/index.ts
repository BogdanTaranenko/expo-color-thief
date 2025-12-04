import ExpoColorThiefModule from "./ExpoColorThiefModule";

export interface RGB {
  r: number;
  g: number;
  b: number;
}

export interface ColorResult {
  rgb: RGB;
  hex: string;
}

export interface GetColorOptions {
  /**
   * Quality setting for color extraction.
   * 1 is highest quality, 10 is default.
   * Higher values are faster but may miss the true dominant color.
   * @default 10
   */
  quality?: number;

  /**
   * Whether to ignore white pixels in the image.
   * @default true
   */
  ignoreWhite?: boolean;
}

export interface GetPaletteOptions extends GetColorOptions {
  /**
   * Number of colors to extract for the palette.
   * @default 5
   */
  colorCount?: number;
}

/**
 * Extract the dominant color from an image.
 *
 * @param imageUri - URI of the image (file://, http://, https://, or asset)
 * @param options - Optional settings for color extraction
 * @returns Promise resolving to the dominant color, or null if extraction fails
 *
 * @example
 * ```typescript
 * const color = await getColor('https://example.com/image.jpg');
 * console.log(color?.hex); // '#ff5733'
 * console.log(color?.rgb); // { r: 255, g: 87, b: 51 }
 * ```
 */
export async function getColor(
  imageUri: string,
  options?: GetColorOptions
): Promise<ColorResult | null> {
  const quality = options?.quality ?? 10;
  const ignoreWhite = options?.ignoreWhite ?? true;

  return ExpoColorThiefModule.getColor(imageUri, quality, ignoreWhite);
}

/**
 * Extract a color palette from an image.
 *
 * @param imageUri - URI of the image (file://, http://, https://, or asset)
 * @param options - Optional settings for palette extraction
 * @returns Promise resolving to an array of colors, or null if extraction fails
 *
 * @example
 * ```typescript
 * const palette = await getPalette('https://example.com/image.jpg', { colorCount: 5 });
 * palette?.forEach(color => {
 *   console.log(color.hex);
 * });
 * ```
 */
export async function getPalette(
  imageUri: string,
  options?: GetPaletteOptions
): Promise<ColorResult[] | null> {
  const colorCount = options?.colorCount ?? 5;
  const quality = options?.quality ?? 10;
  const ignoreWhite = options?.ignoreWhite ?? true;

  return ExpoColorThiefModule.getPalette(
    imageUri,
    colorCount,
    quality,
    ignoreWhite
  );
}

/**
 * Convert RGB values to a hex color string.
 *
 * @param rgb - RGB color object
 * @returns Hex color string (e.g., '#ff5733')
 */
export function rgbToHex(rgb: RGB): string {
  const toHex = (n: number) => {
    const hex = Math.max(0, Math.min(255, Math.round(n))).toString(16);
    return hex.length === 1 ? "0" + hex : hex;
  };
  return `#${toHex(rgb.r)}${toHex(rgb.g)}${toHex(rgb.b)}`;
}

/**
 * Parse a hex color string to RGB values.
 *
 * @param hex - Hex color string (e.g., '#ff5733' or 'ff5733')
 * @returns RGB color object, or null if parsing fails
 */
export function hexToRgb(hex: string): RGB | null {
  const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
  return result
    ? {
        r: parseInt(result[1], 16),
        g: parseInt(result[2], 16),
        b: parseInt(result[3], 16),
      }
    : null;
}
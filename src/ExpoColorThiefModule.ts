import { requireNativeModule } from "expo-modules-core";

import type { ColorResult } from "./index";

interface ExpoColorThiefModuleInterface {
  getColor(
    imageUri: string,
    quality: number,
    ignoreWhite: boolean
  ): Promise<ColorResult | null>;

  getPalette(
    imageUri: string,
    colorCount: number,
    quality: number,
    ignoreWhite: boolean
  ): Promise<ColorResult[] | null>;
}

export default requireNativeModule<ExpoColorThiefModuleInterface>(
  "ExpoColorThief"
);
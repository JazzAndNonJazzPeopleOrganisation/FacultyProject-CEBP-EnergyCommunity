import { Pink40, Pink80, Purple40, Purple80, PurpleGrey40, PurpleGrey80 } from './Color';

export interface ColorScheme {
  primary: string;
  onPrimary: string;
  secondary: string;
  tertiary: string;
  background: string;
  surface: string;
  surfaceVariant: string;
  onBackground: string;
  onSurface: string;
  outline: string;
  error: string;
}

export const LightColorScheme: ColorScheme = {
  primary: Purple40,
  onPrimary: '#FFFFFF',
  secondary: PurpleGrey40,
  tertiary: Pink40,
  background: '#FFFBFE',
  surface: '#FFFFFF',
  surfaceVariant: '#F3EDF7',
  onBackground: '#1C1B1F',
  onSurface: '#1C1B1F',
  outline: '#79747E',
  error: '#B3261E',
};

export const DarkColorScheme: ColorScheme = {
  primary: Purple80,
  onPrimary: '#381E72',
  secondary: PurpleGrey80,
  tertiary: Pink80,
  background: '#1C1B1F',
  surface: '#2B2930',
  surfaceVariant: '#49454F',
  onBackground: '#E6E1E5',
  onSurface: '#E6E1E5',
  outline: '#938F99',
  error: '#F2B8B5',
};

import { TextStyle } from 'react-native';

export const Typography: Record<string, TextStyle> = {
  headlineMedium: {
    fontSize: 28,
    lineHeight: 36,
    fontWeight: '600',
  },
  titleLarge: {
    fontSize: 22,
    lineHeight: 28,
    fontWeight: '600',
  },
  titleMedium: {
    fontSize: 16,
    lineHeight: 24,
    fontWeight: '600',
    letterSpacing: 0.15,
  },
  bodyLarge: {
    fontSize: 16,
    lineHeight: 24,
    fontWeight: '400',
    letterSpacing: 0.5,
  },
};

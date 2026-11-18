import { StatusBar } from 'expo-status-bar';
import React, { useMemo } from 'react';
import { SafeAreaView, StyleSheet, useColorScheme } from 'react-native';
import { FakeAppContainer } from './src/data/fake/FakeAppContainer';
import { RealAppContainer } from './src/di/RealAppContainer';
import { AppRoot } from './src/ui/AppRoot';
import { DarkColorScheme, LightColorScheme } from './src/ui/theme/Theme';

/**
 * Set USE_MOCK = false to connect to the Spring Boot backend (CoreEnergyApplication).
 * Set USE_MOCK = true for offline fake data.
 */
const USE_MOCK = false;

export default function App() {
  const container = useMemo(
    () => (USE_MOCK ? new FakeAppContainer() : new RealAppContainer()),
    [],
  );
  const colorScheme = useColorScheme() === 'dark' ? DarkColorScheme : LightColorScheme;

  return (
    <SafeAreaView style={[styles.safeArea, { backgroundColor: colorScheme.background }]}>
      <StatusBar style="auto" />
      <AppRoot container={container} />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
  },
});

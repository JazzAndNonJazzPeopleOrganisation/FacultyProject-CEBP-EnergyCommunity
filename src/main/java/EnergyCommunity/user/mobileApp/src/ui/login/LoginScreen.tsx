import React, { useState } from 'react';
import {
  ActivityIndicator,
  StyleSheet,
  Text,
  TextInput,
  TouchableOpacity,
  View,
  useColorScheme,
} from 'react-native';
import { DarkColorScheme, LightColorScheme } from '../theme/Theme';
import { Typography } from '../theme/Type';
import { LoginUiState } from './LoginViewModel';

export interface LoginScreenProps {
  state: LoginUiState;
  onLogin: (email: string, password: string) => void;
}

export const LoginScreen: React.FC<LoginScreenProps> = ({ state, onLogin }) => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const colorScheme = useColorScheme() === 'dark' ? DarkColorScheme : LightColorScheme;

  return (
    <View style={[styles.container, { backgroundColor: colorScheme.background }]}>
      <Text style={[Typography.headlineMedium, { color: colorScheme.onBackground }]}>
        Energy Community
      </Text>
      <View style={styles.spacer16} />
      <TextInput
        value={email}
        onChangeText={setEmail}
        placeholder="Email"
        placeholderTextColor={colorScheme.outline}
        autoCapitalize="none"
        keyboardType="email-address"
        style={[
          styles.input,
          {
            borderColor: colorScheme.outline,
            color: colorScheme.onSurface,
            backgroundColor: colorScheme.surface,
          },
        ]}
      />
      <View style={styles.spacer8} />
      <TextInput
        value={password}
        onChangeText={setPassword}
        placeholder="Parola"
        placeholderTextColor={colorScheme.outline}
        secureTextEntry
        style={[
          styles.input,
          {
            borderColor: colorScheme.outline,
            color: colorScheme.onSurface,
            backgroundColor: colorScheme.surface,
          },
        ]}
      />
      {state.error ? (
        <Text style={[styles.errorText, { color: colorScheme.error }]}>{state.error}</Text>
      ) : null}
      <View style={styles.spacer16} />
      <TouchableOpacity
        onPress={() => onLogin(email, password)}
        disabled={state.loading}
        style={[
          styles.button,
          {
            backgroundColor: colorScheme.primary,
            opacity: state.loading ? 0.6 : 1,
          },
        ]}
      >
        {state.loading ? (
          <ActivityIndicator size="small" color={colorScheme.onPrimary} />
        ) : (
          <Text style={[styles.buttonText, { color: colorScheme.onPrimary }]}>Login</Text>
        )}
      </TouchableOpacity>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 24,
    justifyContent: 'center',
  },
  spacer8: {
    height: 8,
  },
  spacer16: {
    height: 16,
  },
  input: {
    width: '100%',
    borderWidth: 1,
    borderRadius: 8,
    paddingHorizontal: 14,
    paddingVertical: 12,
    fontSize: 16,
  },
  errorText: {
    marginTop: 8,
    fontSize: 14,
  },
  button: {
    width: '100%',
    borderRadius: 24,
    paddingVertical: 12,
    alignItems: 'center',
    justifyContent: 'center',
  },
  buttonText: {
    fontSize: 16,
    fontWeight: '600',
  },
});

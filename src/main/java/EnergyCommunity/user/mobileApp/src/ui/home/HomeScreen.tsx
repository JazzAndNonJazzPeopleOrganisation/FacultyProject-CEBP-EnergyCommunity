import React from 'react';
import {
  ActivityIndicator,
  FlatList,
  StyleSheet,
  Text,
  TouchableOpacity,
  View,
  useColorScheme,
} from 'react-native';
import { DarkColorScheme, LightColorScheme } from '../theme/Theme';
import { Typography } from '../theme/Type';
import { HomeUiState } from './HomeViewModel';

export interface HomeScreenProps {
  state: HomeUiState;
  onAccept: (proposalId: string) => void;
  onReject: (proposalId: string) => void;
  onLogout: () => void;
}

export const HomeScreen: React.FC<HomeScreenProps> = ({
  state,
  onAccept,
  onReject,
  onLogout,
}) => {
  const colorScheme = useColorScheme() === 'dark' ? DarkColorScheme : LightColorScheme;

  return (
    <View style={[styles.container, { backgroundColor: colorScheme.background }]}>
      {state.loading ? <ActivityIndicator size="large" color={colorScheme.primary} /> : null}
      {state.error ? (
        <Text style={[styles.errorText, { color: colorScheme.error }]}>{state.error}</Text>
      ) : null}

      {state.status ? (
        <View style={[styles.card, { backgroundColor: colorScheme.surfaceVariant }]}>
          <Text style={[Typography.titleLarge, { color: colorScheme.onSurface }]}>
            Baterie: {state.status.batteryLevelPercent}%
          </Text>
          <Text style={[Typography.bodyLarge, { color: colorScheme.onSurface }]}>
            Consum: {state.status.currentConsumptionKw} kW
          </Text>
          <Text style={[Typography.bodyLarge, { color: colorScheme.onSurface }]}>
            Productie solara: {state.status.currentSolarProductionKw} kW
          </Text>
        </View>
      ) : null}

      <View style={styles.spacer16} />
      <Text style={[Typography.titleMedium, { color: colorScheme.onBackground }]}>
        Propuneri
      </Text>

      <FlatList
        style={styles.list}
        data={state.proposals}
        keyExtractor={(item) => item.id}
        renderItem={({ item }) => (
          <View style={[styles.proposalCard, { backgroundColor: colorScheme.surfaceVariant }]}>
            <Text style={[Typography.bodyLarge, { color: colorScheme.onSurface }]}>
              {item.description}
            </Text>
            {item.estimatedProfit != null ? (
              <Text style={[Typography.bodyLarge, { color: colorScheme.onSurface }]}>
                Profit estimat: {item.estimatedProfit}
              </Text>
            ) : null}
            <View style={styles.actionsRow}>
              <TouchableOpacity
                onPress={() => onAccept(item.id)}
                style={[styles.primaryButton, { backgroundColor: colorScheme.primary }]}
              >
                <Text style={{ color: colorScheme.onPrimary, fontWeight: '600' }}>Accepta</Text>
              </TouchableOpacity>
              <TouchableOpacity
                onPress={() => onReject(item.id)}
                style={[styles.outlinedButton, { borderColor: colorScheme.outline }]}
              >
                <Text style={{ color: colorScheme.primary, fontWeight: '600' }}>Respinge</Text>
              </TouchableOpacity>
            </View>
          </View>
        )}
      />

      <TouchableOpacity onPress={onLogout} style={styles.logoutButton}>
        <Text style={{ color: colorScheme.primary, fontWeight: '600' }}>Logout</Text>
      </TouchableOpacity>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    padding: 16,
  },
  errorText: {
    marginBottom: 8,
    fontSize: 14,
  },
  card: {
    width: '100%',
    padding: 16,
    borderRadius: 12,
    gap: 4,
  },
  spacer16: {
    height: 16,
  },
  list: {
    flex: 1,
    marginTop: 8,
  },
  proposalCard: {
    width: '100%',
    padding: 12,
    borderRadius: 12,
    marginVertical: 4,
    gap: 6,
  },
  actionsRow: {
    flexDirection: 'row',
    gap: 8,
    marginTop: 4,
  },
  primaryButton: {
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 20,
  },
  outlinedButton: {
    paddingHorizontal: 16,
    paddingVertical: 8,
    borderRadius: 20,
    borderWidth: 1,
  },
  logoutButton: {
    paddingVertical: 12,
    alignSelf: 'flex-start',
  },
});

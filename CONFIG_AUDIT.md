# Audit de Configuration EngineLab

## ✅ Options UTILISÉES (à conserver)

### tournament.*
- ✅ **name** - Utilisé dans `printSummary()`
- ✅ **mode** - Utilisé dans `ConfigMain` pour déterminer le type de tournoi
- ✅ **engines** - Utilisé pour créer `MatchRunner`
- ✅ **concurrency** - Utilisé dans `MatchRunner` pour le pool de threads
- ✅ **pairsPerMatch** - Utilisé dans `ConfigMain.runPairs()`
- ✅ **timeControls** - Utilisé dans `MatchRunner` et `ConfigMain`
- ✅ **openings.enabled** - Utilisé dans `ConfigMain`
- ✅ **openings.file** - Utilisé dans `Config.getStartingPositions()`
- ✅ **openings.mode** - Utilisé dans `ConfigMain` et `runPairs()`

### server.*
- ✅ **webSocket.enabled** - Utilisé dans `ConfigMain` (wsPort = 0 si désactivé)
- ✅ **webSocket.port** - Utilisé dans `MatchRunner` pour démarrer WebSocket
- ✅ **ssl.*** - Utilisé dans `MatchRunner` pour SSL/TLS (si activé)

### paths.*
- ✅ **engineDir** - Utilisé dans `Config.getEnginePaths()`
- ✅ **resourcesDir** - Utilisé dans `WebSocketServer` pour servir les fichiers statiques

### logging.*
- ✅ **level** - Validation seulement (DEBUG/INFO/WARN/ERROR)
- ✅ **engineCommunication** - Utilisé dans `ConfigMain` → `Engine.setLogCommunication()`

### stats.*
- ✅ **persistenceEnabled** - Utilisé dans `ConfigMain` pour créer `StatsManager`
- ✅ **statsDirectory** - Utilisé dans `ConfigMain` pour `StatsManager`

---

## ❌ Options NON UTILISÉES (à supprimer)

### server.*
- ❌ **webSocket.host** - JAMAIS utilisé (bind toujours sur toutes les interfaces)
- ❌ **http.*** - Toute la section HTTP inutile (WebSocket gère déjà HTTP)
- ❌ **shutdown.gracefulTimeoutSeconds** - JAMAIS utilisé dans le code

### paths.*
- ❌ **outputDir** - Crée le dossier mais jamais utilisé ensuite
- ❌ **logDir** - Crée le dossier mais jamais utilisé ensuite

### performance.*
- ❌ **engineStartupTimeoutSeconds** - Validation seulement, jamais utilisé
- ❌ **engineResponseTimeoutSeconds** - Validation seulement, jamais utilisé
- ❌ **maxThreadPoolSize** - Existe dans Config mais JAMAIS utilisé
- ❌ **recommendedHeapSizeMB** - Existe dans Config mais JAMAIS utilisé

### deployment.*
- ❌ **TOUTE LA SECTION** - Juste un avertissement à la validation, aucune utilisation réelle
  - environment
  - healthCheck.enabled
  - healthCheck.endpoint
  - healthCheck.interval

---

## 🎯 Recommandation : Configuration épurée

```yaml
# EngineLab Configuration

tournament:
  name: "EngineLab Tournament"
  mode: "pairs"
  engines:
    - "Aspira_3"
    - "stockfish"
  concurrency: 1
  pairsPerMatch: 100
  timeControls:
    - baseTimeMs: 5000
      incrementMs: 100
  openings:
    enabled: true
    file: "8moves.epd"
    mode: "random"

server:
  webSocket:
    enabled: true
    port: 8080
  # SSL optionnel (non documenté dans config mais utilisé)
  # ssl:
  #   enabled: true
  #   port: 8443
  #   keyStorePath: "./keystore.jks"
  #   keyStorePassword: "password"
  #   keyStoreType: "JKS"

paths:
  engineDir: "./engines"
  resourcesDir: "./src/main/resources"

logging:
  level: "WARN"
  engineCommunication: false

stats:
  persistenceEnabled: true
  statsDirectory: "./stats"
```

---

## 📉 Réduction

**Avant** : ~45 lignes avec toutes les options  
**Après** : ~30 lignes (gain de 33%)  

**Options supprimées** : 15+ options inutilisées

---

## ⚠️ Notes

1. **server.webSocket.host** pourrait être utile pour bind sur une IP spécifique, mais actuellement non implémenté
2. **performance.engineStartupTimeoutSeconds** pourrait être utile mais faudrait l'implémenter dans Engine.java
3. **deployment** section complètement inutile pour une app standalone

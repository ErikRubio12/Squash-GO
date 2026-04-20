# Rating System

## Elo Formula

```
Expected score:  E_a = 1 / (1 + 10^((R_b - R_a) / 400))
New rating:      R_a' = R_a + K * (S - E_a)
```
Where S = 1.0 (win) or 0.0 (loss). No draws in squash.

## K-Factor

| Scenario | K | Rationale |
|----------|---|-----------|
| Both provisional (<5 ranked matches) | 60 | Fast calibration |
| One provisional | 40 | Faster adjustment for new player |
| Both established | 32 | Standard competitive |

## Tier / Division Mapping

Each tier = 200 Elo. Each division = 40 Elo within a tier.

| Tier | Elo Range |
|------|-----------|
| Bronze | 800-999 |
| Silver | 1000-1199 |
| Gold | 1200-1399 |
| Platinum | 1400-1599 |
| Diamond | 1600-1799 |
| Master | 1800+ |

Division 5 = lowest 40 Elo of tier, Division 1 = highest. Master has no divisions.

## Key Rules

- **Starting Elo**: 1200 (Gold 5)
- **Provisional**: First 5 ranked matches. Tier shown as "Calibrating".
- **Floor**: 400 (hard minimum)
- **Decay**: None in MVP
- **Casual matches**: No rating impact
- **Update timing**: Immediately on match confirmation (server-side Edge Function)
- **Visibility**: Numeric Elo hidden. Players see tier + division only.

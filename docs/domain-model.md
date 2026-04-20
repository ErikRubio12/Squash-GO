# Domain Model

## Entities

- **Player**: User profile (displayName, avatarUrl, availabilityNote)
- **Court**: Squash court with location (name, address, lat/lng)
- **PlayerCourt**: Many-to-many association between players and their preferred courts
- **Rating**: Player's Elo score, provisional status, ranked match count
- **Challenge**: Intent to play (challenger, challenged, court, matchType, status)
- **Match**: Actual game record (players, score, result, confirmation status)
- **Dispute**: Disagreement on match result (reason, resolution)
- **RatingHistory**: Audit trail of rating changes per match

## Challenge Lifecycle

```
[created] -> PENDING
PENDING -> ACCEPTED    (rival accepts)
PENDING -> DECLINED    (rival declines)
PENDING -> CANCELLED   (sender withdraws)
PENDING -> EXPIRED     (7 days, no response)
ACCEPTED -> COMPLETED  (match result confirmed)
ACCEPTED -> CANCELLED  (either cancels before result)
```

## Match Lifecycle

```
[challenge accepted] -> IN_PROGRESS
IN_PROGRESS -> RESULT_SUBMITTED  (player submits score)
IN_PROGRESS -> CANCELLED         (mutual cancel)
RESULT_SUBMITTED -> CONFIRMED    (other confirms OR 72h auto-accept)
RESULT_SUBMITTED -> DISPUTED     (other disagrees)
DISPUTED -> CONFIRMED            (admin resolves)
DISPUTED -> CANCELLED            (admin cancels)
```

## Result Confirmation

1. Player A submits game-by-game scores
2. Validation: winner has 3+ games, each game valid (11+, win by 2)
3. autoAcceptAt = now + 72h
4. Player B: confirm / dispute / no action (auto-accept at 72h)
5. On CONFIRMED + RANKED: Edge Function calculates Elo update

## Match Types

- **Casual**: Recorded in history, no rating impact
- **Ranked**: Affects Elo rating and tier progression

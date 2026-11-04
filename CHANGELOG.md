# Changelog

## 1.1.1
- Fix: clicking items in menus (SkyBlock menu, bazaar, auctions, chests...) no longer counts as a pickup. The cursor stack is no longer counted, and changes made while a menu is open must stay stable for ~0.4 s, so client-side predicted changes the server reverts are ignored

## 1.1.0
- Speech bubbles: hungry reminders (escalating), reactions to eating/throwing up, big-stack and too-fast comments, greeting and random SkyBlock chatter
- New config options: `speech`, `hungryAfterSeconds`, `chatterSeconds`

## 1.0.0
- First release: hanging cat that eats picked-up items and throws up dropped ones
- Fading pickup log, SkyBlock-id based item tracking, JSON config

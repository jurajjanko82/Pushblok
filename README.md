# PushBlok – projekt pre Android Studio

Kompletný Kotlin projekt appky podobnej PushScroll. Funguje **100 % offline**
(žiadne API volania, všetko v SharedPreferences na zariadení).

## Ako spustiť
1. Stiahni si **Android Studio** (Hedgehog alebo novšie).
2. `File → Open` → vyber priečinok `PushBlok`.
3. Nechaj Gradle sync doběhnúť.
4. Pripoj telefón (USB debugging) alebo spusti emulátor a klikni Run ▶.

## Ako to funguje
- **Kredity**: tlačidlo "+1 kredit" v appke je len ukážka. Reálne pripisovanie
  za kroky rieši `StepCounterService.kt` (100 krokov = 1 kredit, dá sa zmeniť
  konštantou `STEPS_PER_CREDIT`). Za kliky/cvičenia stačí na tvojej obrazovke
  s cvičením zavolať `CreditManager.addCredits(context, počet)`.
- **1 kredit = 1 minúta** odblokovania appky/telefónu – rieši `BlockManager.kt`.
- **Polnočný reset**: `MidnightResetReceiver` + `CreditManager.checkMidnightReset()`
  vynulujú kredity o 0:00, **okrem** toho, čo je vo vkladnej knižke.
- **Vkladná knižka**: `CreditManager.kt` – odomknutie za 200 kr., limit
  30 kr./deň na vklad aj výber, poplatok 5 kr. za každú operáciu.
- **Blokovanie appiek**: `BlockerAccessibilityService.kt` sleduje, ktorá appka
  je v popredí, a ak je na zozname blokovaných (nastaví sa v
  `AppSelectionActivity`), spustí `BlockOverlayActivity` cez ktorú sa appka
  odomkne kreditmi.
- **Blokovanie celého telefónu**: prepínač na hlavnej obrazovke +
  `PhoneAdminReceiver` (Device Admin) → `dpm.lockNow()`.

## Povolenia, ktoré treba v appke odklikať
- **Accessibility** (Nastavenia → Zjednodušenie ovládania) – nutné pre
  blokovanie appiek.
- **Device Admin** – nutné pre zamknutie celého telefónu.
- Krokomer (ACTIVITY_RECOGNITION) a notifikácie sa pýtajú automaticky.

## Dôležité obmedzenia, o ktorých treba vedieť
- **Google Play**: appky, ktoré zneužívajú Accessibility Service na
  blokovanie iných appiek, majú prísne pravidlá Google Play (musíš presne
  deklarovať účel v Play Console, inak appku zamietnu/odstránia). Na osobné
  použitie / sideload (APK mimo Play) žiadny problém nie je.
- **Device Admin API** je na novších Androidoch (12+) čiastočne obmedzené a
  Google ho postupne nahrádza inými mechanizmami (Kiosk mode / DPC pre firemné
  zariadenia). Pre bežného používateľa `lockNow()` stále funguje, ale
  "neobísiteľné" zamknutie na 100 % nie je možné bez toho, aby appka bola
  nastavená ako **Device Owner** (vyžaduje NFC provisioning alebo ADB príkaz
  pri prvom nastavení telefónu – dá sa dorobiť, ale presahuje rozsah tohto
  základu).
- Šikovný používateľ sa vie blokovaniu appiek vyhnúť (napr. vypnutím
  Accessibility service v nastaveniach, alebo odinštalovaním appky). Ak chceš
  toto sťažiť, treba pridať `Device Owner` mód + "uninstall protection" cez
  `setUninstallBlocked()`.

## 21 cvikov + kontrola techniky + hlasová spätná väzba

- **21 cvikov v 4 kategóriách** (Nohy, Core/Brucho, Ruky/hruď, Kardio) — zoznam
  a nastavenia kreditov v `ExerciseModels.kt`, detekcia v `ExerciseEngine.kt`.
- Tri "režimy" detekcie:
  - **Opakovania podľa uhla kĺbu** (drepy, kliky, výpady, brušáky...) — sleduje
    sa uhol v kĺbe (napr. bok-koleno-členok) a počíta prechody dole→hore.
  - **Striedavé cviky** (jumping jacks, mountain climbers, high knees, shoulder
    taps, bicyklové brušáky) — sleduje sa striedanie strán.
  - **Výdrže** (plank, bočný plank, wall sit, krúženie rukami) — namiesto
    opakovaní sa počíta čas strávený v správnej polohe; kredity sa dávajú za
    každých 5 sekúnd (dá sa zmeniť cez `ExerciseSettings.SECONDS_PER_CREDIT_UNIT`).
- **Kontrola techniky**: pri drepoch a klikoch appka sleduje aj uhol chrbta/tela
  a keď je zlý (napr. prehnutý chrbát pri drepe, prehnuté boky pri kliku),
  zobrazí a **povie nahlas** upozornenie ("Narovnaj chrbát", "Drž telo v rovnej
  línii"). Každých 5 správnych opakovaní appka pochváli ("Super forma!").
- **Hlas beží cez vstavaný Android TextToSpeech** — teda úplne offline, žiadne
  volanie na server. Ak telefón nemá stiahnutý slovenský hlasový balík, TTS
  potichu použije predvolený jazyk zariadenia (appka nespadne).
- Presnosť detekcie je pri jednoduchých stojacich cvikoch (drepy, výpady,
  jumping jacks) dobrá. Pri cvikoch v ľahu/planku (brušáky, mountain climbers,
  shoulder taps...) závisí veľmi od uhla kamery — v appke je pri každom cviku
  tip, ako telefón položiť (`ExerciseType.placementTip`), over si ho v UI.

## Čo môžeš ľahko doladiť
- `STEPS_PER_CREDIT` v `StepCounterService.kt` – koľko krokov = 1 kredit.
- Pridať body za "kliky" alebo cvičenia: v ľubovoľnej Activity zavolaj
  `CreditManager.addCredits(this, N)`.
- Farby/dizajn: `res/values/colors.xml`, `themes.xml`, layouty v `res/layout`.

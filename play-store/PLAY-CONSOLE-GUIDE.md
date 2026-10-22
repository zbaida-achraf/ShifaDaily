# Publishing ShifaDaily on Google Play — step by step

Prepared 22 Oct 2026. Field names are given in English with the **French Play Console label** in brackets.
Requirements were checked against Google's help pages on that date; the Console can change, so follow what it
shows if it differs.

---

## 0. Before you start — important

1. **Testing rule for new personal accounts.** If your developer account is a *personal* account created after
   13 Nov 2023, Google requires a **closed test with at least 12 testers opted in for 14 consecutive days**
   before you can apply for production. Plan for this (friends/family with Android phones and a Gmail account).
   Organisation accounts are reported to be exempt.
2. **Public developer email.** Play shows a contact email on the store page. Use a dedicated address
   (e.g. a new Gmail for ShifaDaily) — **not** a work address.
3. **Which file to upload:** `release/ShifaDaily-1.0.1.aab` (version 1.0.1, versionCode 2, signed with your
   ShifaDaily key). Every later upload needs a higher `versionCode`.
4. **Same signing key as the website (recommended).** The APK on the website is signed with your key
   (certificate SHA-256 `6d5bdc86…3278fd`). If Google generates a *new* app-signing key, people who installed
   from the website cannot update from Play (and vice-versa) without uninstalling. To avoid that, choose to
   **use your existing key** when Play App Signing asks (see §5).

---

## 1. Create the app [Créer une application]

| Field | Value |
|---|---|
| App name [Nom de l'application] | `ShifaDaily – Rappels & repas` |
| Default language [Langue par défaut] | Français (France) – fr-FR |
| App or game [Application ou jeu] | App [Application] |
| Free or paid [Sans frais ou payante] | Free [Sans frais] |
| Declarations | Accept the Developer Programme Policies and US export laws (you read and accept them yourself) |

## 2. Store listing [Fiche Play Store principale]

Texts: copy from `LISTING.md` (FR default, then add EN and AR translations).

| Field | File |
|---|---|
| App icon [Icône de l'application] 512×512 | `graphics/icon-512.png` |
| Feature graphic [Image de présentation] 1024×500 | `graphics/feature-graphic-fr.png` (per language: `-en`, `-ar`) |
| Phone screenshots [Captures d'écran pour téléphone] | `screenshots/phone/fr/01…08.png` (EN/AR folders for translations) |
| 7-inch tablet [Tablette 7 pouces] | `screenshots/tablet-7/01…05.png` |
| 10-inch tablet [Tablette 10 pouces] | `screenshots/tablet-10/01…05.png` |

**Store settings [Paramètres de la fiche Play Store]**

| Field | Value |
|---|---|
| Category [Catégorie] | **Medical [Médecine]** (alternative: Health & Fitness [Santé et remise en forme]) |
| Tags | Medication reminder, Diet / nutrition, Health |
| Email [Adresse e-mail] | your dedicated public address |
| Website [Site Web] | `https://zbaida-achraf.github.io/ShifaDaily/` |
| Phone | optional — leave empty |

## 3. App content [Contenu de l'application] — answers

### Privacy policy [Règles de confidentialité]
```
https://zbaida-achraf.github.io/ShifaDaily/fr/confidentialite/
```
(English: `…/en/privacy/` · Arabic: `…/ar/privacy/`)

### App access [Accès aux applications]
**All functionality is available without special access** [Toutes les fonctionnalités sont disponibles sans
accès spécial] — there is no login.

### Ads [Annonces]
**No, my app does not contain ads** [Non].

### Content rating [Classification du contenu] (IARC questionnaire)
- Category: **All other app types** [Toutes les autres applications] / Utility.
- Violence, sexuality, language, controlled substances, gambling, user interaction/chat, sharing location,
  purchases: **No** to all.
- Note: the app *mentions medication names entered by the user* but does not promote drugs or alcohol → answer
  **No** to "references to drugs/alcohol/tobacco" unless the questionnaire explicitly asks about medicine
  reminders; if it does, describe it as "user-entered prescription reminders, no promotion".
- Expected result: PEGI 3 / Everyone.

### Target audience [Public cible et contenu]
Age groups: **18 and over** only. Not designed for children; no appeal to children.

### News app [Application d'actualités]: **No**.
### COVID-19 contact tracing/status: **No**.
### Government app [Application gouvernementale]: **No**.
### Financial features [Fonctionnalités financières]: **None**.

### Data safety [Sécurité des données]
| Question | Answer |
|---|---|
| Does your app collect or share any of the required user data types? | **No** — the app has no internet permission; all data stays on the device and is never transmitted to you or third parties. (Google defines "collect" as transmitting off the device.) |
| Is all user data encrypted in transit? | Not applicable (nothing is transmitted). If the form still requires an answer after "No collection", it will not ask. |
| Do you provide a way for users to request that their data be deleted? | Users can delete everything in-app (Settings › Erase all data) and by uninstalling. |
| Independent security review | No |

### Health apps declaration [Déclaration relative aux applications de santé]
Required for every app. Declare that the app has health features, and tick the options that correspond to:
- **Medication / treatment management or reminders**
- **Nutrition / diet** (meal ideas)

Then confirm: the app is **not** a medical device, provides **no** diagnosis, treatment or dosing, and is
**not** used for clinical decisions. The disclaimer is shown in-app (onboarding and Settings) and in the listing.
Do not claim any regulatory clearance.

### Advertising ID [Identifiant publicitaire]
**No** — the app does not use the advertising ID (no AD_ID permission).

### Sensitive permissions
- `SCHEDULE_EXACT_ALARM` — user-granted "Alarms & reminders" access. Google's restricted declaration applies to
  `USE_EXACT_ALARM`, which ShifaDaily does **not** use. If the Console nevertheless asks for a justification:
  > ShifaDaily is a medication reminder. The user schedules reminders at the exact times written on their
  > prescription; a delayed reminder defeats the app's core purpose. The user grants "Alarms & reminders"
  > access in system settings and can revoke it; the app then falls back to inexact reminders and warns the user.
- No location, contacts, storage, camera, microphone or foreground-service permissions.

## 4. Testing [Tests]

1. **Internal testing [Tests internes]** (optional, instant): create a release with the AAB, add your own email,
   install from the opt-in link, check reminders and both languages.
2. **Closed testing [Tests fermés]** (required for new personal accounts): create a track, upload the same AAB,
   add ≥12 testers (an email list or Google Group), share the opt-in link, keep them opted in **14 days**.
   Ask testers to really use it (add a medication, receive a reminder, try the meal assistant).
3. **Apply for production [Demander l'accès à la production]**: answer the questionnaire about the test
   (what was tested, feedback, changes).

## 5. Releasing [Créer une release]

1. Release notes: from `LISTING.md` (FR/EN/AR).
2. **Play App Signing** (first upload): choose **"Use existing app signing key"** / **export and upload a key
   from a Java keystore** [Utiliser une clé de signature existante]. Play gives you a small tool (`pepk.jar`) and
   an encryption key; run (on this PC):
   ```
   java -jar pepk.jar --keystore="C:\Users\Dell Precision\ShifaDaily-signing\shifadaily-release.jks" --alias=shifadaily --output=shifadaily-signing-key.zip --include-cert --rsa-aes-encryption --encryption-key-path=encryption_public_key.pem
   ```
   enter the password from `keystore.properties` when asked, and upload the generated zip. The same key can be
   used as your **upload key**. Never upload the `.jks` or the password anywhere else.
3. Upload `release/ShifaDaily-1.0.1.aab`, review warnings, roll out to the chosen track.
4. After rollout, Play's **pre-launch report** runs the app on test devices; read it (crashes, accessibility).

## 6. Checklist

- [ ] Personal vs organisation account; closed-test plan (12 testers × 14 days) if personal
- [ ] Dedicated public contact email
- [ ] App created (FR default), EN + AR translations added
- [ ] Icon, feature graphics, 8 phone + 5 + 5 tablet screenshots uploaded
- [ ] Privacy policy URL, App access, Ads, Content rating, Target audience 18+, Data safety (no collection),
      Health apps declaration, Advertising ID (no), News/COVID/Government/Financial (no)
- [ ] Play App Signing with your existing key
- [ ] AAB 1.0.1 uploaded to internal/closed testing
- [ ] 14-day closed test completed → production access requested
- [ ] Back up `C:\Users\Dell Precision\ShifaDaily-signing\` (two safe places)

Sources checked: Google Play help — store listing assets (answer 9866151), closed-testing requirement for new
personal accounts (answer 14151465), Health apps declaration (answer 14738291); Android docs — exact alarms.

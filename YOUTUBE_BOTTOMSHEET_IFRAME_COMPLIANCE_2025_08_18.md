# Aktualizacja: YouTube w Bottom Sheet – zgodność z polityką (2025-08-18)

## Cel
Zastąpienie wycofanego YouTube Android Player API (`YouTubePlayerView`) oficjalnym osadzeniem YouTube przez iframe w `WebView` w dolnym arkuszu (BottomSheet), z zachowaniem zgodności z zasadami YouTube i Google Play.

## Kluczowe zmiany
- Plik: `toplistadiscopolo/src/main/java/com/grandline/toplistadiscopolo/YouTubeBottomSheetController.java`
  - Wdrożono odtwarzanie filmu YouTube przez oficjalny iframe (`https://www.youtube.com/embed/{videoId}`) w `WebView`.
  - Ustawienia `WebView` przyjazne zgodności i bezpieczeństwu:
    - `JavaScript` + `DOM storage` włączone.
    - `mediaPlaybackRequiresUserGesture = true` (wymaga interakcji użytkownika, zgodnie z polityką).
    - Brak dostępu do plików (`allowFileAccess = false`, `allowContentAccess = false`).
    - Brak przestarzałych i niezalecanych API (np. `PluginState`).
  - Załadowanie HTML przez `loadDataWithBaseURL("http://localhost", ...)` – stabilne renderowanie iframe w WebView.
  - Obsługa pełnego ekranu przez `WebChromeClient` (wejście/wyjście z fullscreen, tryb immersyjny, przywracanie UI).
  - Poprawiony lifecycle i czyszczenie zasobów (`onPause`, `onResume`, `onDestroy`).
  - Blokada niepożądanej nawigacji poza domeny YouTube w `WebViewClient`.
- Layout `youtube_bottom_sheet_layout.xml` już zawiera `WebView` i strukturę UI bottom sheet (bez zmian).
- `AndroidManifest.xml` – `INTERNET` już obecny (bez zmian wymaganych).

## Zgodność z polityką YouTube i Google Play
- Używany wyłącznie oficjalny odtwarzacz (iframe) i/lub IFrame Player API osadzone w `WebView`.
- Brak pobierania strumieni, modyfikowania odtwarzacza, ukrywania reklam ani omijania ograniczeń.
- Wymagany gest użytkownika do rozpoczęcia odtwarzania multimediów w WebView.
- Żadnych prywatnych endpointów/nieoficjalnych SDK.

## Efekty dla UX
- Nowoczesny mini-player w dolnym arkuszu: stan zwinięty (podgląd) i rozwinięty (pełny widok), tryb pełnoekranowy.
- Oglądanie bez opuszczania głównego ekranu; lepsze zaangażowanie.
- Responsywne wideo 16:9 (`playsinline`), spójne działanie na szerokiej gamie urządzeń.

## Testy / weryfikacja
- Przegląd błędów kompilacji – brak.
- Ręczna weryfikacja:
  - Otwieranie bottom sheet, ładowanie wideo, przejście do fullscreen i z powrotem.
  - Pauza/wznowienie po zmianie aktywności.
  - Brak niepożądanej nawigacji poza YouTube.

## Wskazówki utrzymaniowe
- Regularnie sprawdzać aktualizacje zasad YouTube i wytyczne Google Play dot. osadzania treści.
- Aktualizować WebView/Chromium na urządzeniach testowych; kompatybilność zależy od implementacji systemowej.

## Kontekst
- `YouTube Android Player API` zostało wycofane. Rekomendowanym rozwiązaniem jest iframe / IFrame Player API.
# Sonorus für Android TV

Filme und Serien von [Sonorus](https://github.com/flopsyan/sonorus), dem selbst
gehosteten Mediaplayer, auf dem Fernseher. Die App braucht einen laufenden
Sonorus-Server und läuft ab Android TV 8.0.

## Funktionen

- **Filme und Serien** des Servers mit Übersicht, Filmreihen und Suche.
- **Weiterschauen** an der Stelle, an der man im Browser, am Handy oder am
  Fernseher aufgehört hat.
- **Spielt ab, was der Fernseher kann**: die Datei direkt, sonst wandelt der
  Server nur um, was nötig ist.
- **Ton und Untertitel** wählbar, die Sprachwahl gilt fürs ganze Konto.
- **Nächste Folge** startet am Ende von selbst, abschaltbar.
- **Bild füllen** zoomt Filme mit eingebrannten schwarzen Balken auf die volle
  Breite, ohne Bild abzuschneiden.
- Komplett mit der **Fernbedienung** bedienbar.

Musik, Hörbücher, Podcasts, E-Books und Downloads gibt es in der
[Android-App](https://github.com/flopsyan/sonorus-android) und im Browser.

## Bauen und installieren

Das APK wird aus dem Quelltext gebaut. Voraussetzung sind JDK 21 und das
Android-SDK.

Android installiert nur signierte APKs. Dafür gehört eine `keystore.properties`
ins Projektverzeichnis (steht in `.gitignore`):

```properties
storeFile=/pfad/zu/sonorus-release.keystore
storePassword=<passwort>
keyAlias=sonorus
keyPassword=<passwort>
```

Einen neuen Schlüssel erzeugt
`keytool -genkeypair -v -keystore sonorus-release.keystore -alias sonorus -keyalg RSA -keysize 2048 -validity 10000`.
Geht er verloren, lässt sich die App nicht mehr aktualisieren, nur neu
installieren.

```bash
export JAVA_HOME=/pfad/zu/jdk21
export ANDROID_HOME=/pfad/zum/android-sdk
./gradlew assembleRelease
adb connect <ip-des-fernsehers>
adb install -r app/build/outputs/apk/release/app-release.apk
```

Für `adb` müssen am Fernseher die Entwickleroptionen und das Debugging über das
Netzwerk eingeschaltet sein. Ohne `adb` lässt sich das APK per USB-Stick und
einem Dateimanager auf dem Fernseher installieren.

Beim ersten Start fragt die App nach Serveradresse, Benutzername und Passwort.
Die Adresse muss mit HTTPS erreichbar sein.

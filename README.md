# masi-STP
MASI - Short-Term Pelicans

## Uruchomienie projektu springowego
### java sdk
Na początku należy upewnić się, że kompilator javy jest zainstalowany i ustawiona jest ścieżka do java home. Patrz
https://confluence.atlassian.com/doc/setting-the-java_home-variable-in-windows-8895.html
### zbudowanie projektu
Po pobraniu projektu z repozytorium należy w konsoli wejść do katalogu `masi-STP-backend` i wywołać komendę
```bash
./gradlew build (linux/osx)
```
na windowsie należy wywołać plik `gradlew.bat` z parametrem `build`
### uruchomienie projektu
Po wywołaniu powyższej komendy projekt uruchomić można komendą
```bash
./gradlew bootRun (linux/osx)
```
na windowsie należy wywołać plik `gradlew.bat` z parametrem `bootRun`

## Uruchomienie aplikacji Angular
* Instalacja Node.js.
* Po pobraniu projektu z repozytorium należy w konsoli wejść do katalogu `masi-STP-interface`.
* Należy wywołać komendę:
```bash
npm install
```
* Po zainstalowaniu wszystkich pakietów można uruchomić projekt za pomocą polecenia:
```bash
npm start
```
* Projekt uruchamia się pod adresem: localhost:4200

## MongoDB
### Instalacja
1. Wchodzimy na stronę: https://www.mongodb.com/ i ściągamy z niej instalator MongoDB w wersji Community Server.
2. Instalujemy pobrane oprogramowanie (radzę przy instalacji zaznaczyć opcję instalacji dodatkowego narzędzia MongoDB Compass Community - udostępnia ono graficzny interfejs do zarządzania bazą danych).
3. Uruchamiamy terminal (`Windows + S`, a następnie wpisujemy `cmd`).
4. W otwartym terminalu wpisujemy `mongod`. W przypadku, gdy ukaże się komunikat `'mongod'is not recognized as an internal or external command, operable program or batch file.` należy dodać katalog `<installation_path>\MongoDB\Server\3.6\bin` do zmiennej środowiskowej `PATH`.
5. Ponawiamy krok 3. W tym momencie uruchamia się baza danych Mongo, jeśli widnieje komunikat `I NETWORK  [initandlisten] waiting for connections on port 27017` wszystko przebiegło pomyślnie i baza danych została poprawnie skonfigurowana.

### Przywracanie stanu bazy danych
1. Kopiujemy katalog `masi-STP-database` do tego samego katalogu, w którym znajduje się folder `data` bazy Mongo (został on utworzony podczas instalacji).
2. Uruchamiamy bazę poleceniem `mongod`.
3. Uruchamiamy kolejny terminal i w nim wpisujemy `mongorestore masi-STP-database`.
4. Stan bazy danych został przywrócny.

#### Listing poprawnie wykonanego procesu przywracania stanu bazy danych
```
C:\>mongorestore masi-STP-database
2018-04-17T16:48:26.058+0200    preparing collections to restore from
2018-04-17T16:48:26.066+0200    reading metadata for stp.logs from masi-STP-database\stp\logs.metadata.json
2018-04-17T16:48:26.082+0200    restoring stp.logs from masi-STP-database\stp\logs.bson
2018-04-17T16:48:26.085+0200    no indexes to restore
2018-04-17T16:48:26.085+0200    finished restoring stp.logs (2 documents)
2018-04-17T16:48:26.085+0200    done
```

#### Sprawdzenie poprawności przywrócenia stanu bazy danych
##### Sposób 1
1. Podczas gdy baza danych Mongo jest włączona w kolejnym terminalu wykonujemy komendę `mongo`. Otworzyła się konsola bazy danych.
2. Wpisujemy komendę `show dbs`. W wylistowanych bazach danych powinna znajdować się nazwa `stp`.
3. Wpisujemy odpowiednio komendy `use stp` a następnie `db.logs.find()`. Powinny się wyświetlić wszystkie obiekty w kolekcji `logs` bazy danych o nazwie `stp`.

##### Sposób 2
1. Uruchamiamy zainstalowane narzędzie MongoDB Compass Community.
2. Wciskamy klawisz Connect. Na panelu z lewej strony mamy do wyboru dostępne bazy danych.
3. Sprawdzamy czy na liście znajduje się baza danych o nazwie `stp`.
4. Rozwijamy listę `stp`. Powinna tam znajdować się jedna kolekcja `logs`. Klikamy w nią.
5. Sprawdzamy czy na liście znajdują się odpowiednie obiekty.


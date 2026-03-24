# Orbital

**Renārs Ozoliņš** <br>
12. klase <br>
2026. gads <br>

---

# Satura rādītājs
1. [Problēmas izpēte un analīze](#problēmas-izpēte-un-analīze)
2. [Programmatūras prasību specifikācija](#programmatūras-prasību-specifikācija)
3. [Programmatūras projektējums](#programmatūras-projektējums)
4. [Programmatūras izstrādes plāns](#programmatūras-izstrādes-plāns)
5. [Atkļūdošanas un akcepttestēšanas pārskats](#atkļūdošanas-un-akcepttestēšanas-pārskats)
6. [Lietotāja ceļvedis](#lietotāja-ceļvedis)
7. [Papildus API Informācija](#papildus-api-informācija)

---

# Problēmas izpēte un analīze

Šī projekta atrisinātā problēma ir diezgan vienkārša: kalendāra/plānošanas lietotnes komandām ir vai nu dārgas, vai arī tās 
nenodrošina funkcijas, kuras es vēlos. Tāpēc, kad strādāju ar komandu, plānošanai parasti izmantojam tikai WhatsApp vai Discord, kas 
laika gaitā var kļūt apgrūtinoši.

## Mērķauditorija

- Dažāda skaita cilvēku komandas
- Studentu projektu grupas
- Individuāli lietotāji ar plānošanas vajadzībām

## Lietotāju vajadzību izpēte

Lai saprastu lietotāju vajadzības, es izmantoju šīs metodes:

- **Intervijas** ar projektu vadītājiem
- **Diskusijas** ar potenciālajiem lietotājiem

### Galvenie secinājumi
Lai es būtu apmierināts ar šo projektu, man bija nepieciešamas tikai 3 lietas:
- Vienkārša komandas izveide.
- Vienkārša uzdevumu pārvaldība/uzdevumu piešķiršana.
- Minimālistisks kalendāra dizains ar papildu iestatījumiem, ja nepieciešams.

---

# Programmatūras prasību specifikācija
## Funkcionālās prasības

- Lietotājs var reģistrēties un autorizēties
- Lietotājs var izveidot komandu un workspace
- Lietotājs var pievienot un pārvaldīt uzdevumus
- Lietotājs var piešķirt uzdevumus citiem lietotājiem
- Lietotājs var apskatīt kalendāru

## Nefunkcionālās prasības

- Sistēmai jābūt ātrai un atsaucīgai
- Datiem jābūt droši uzglabātiem
- Lietotāja interfrace jābūt vienkāršam
- Aplikācijai jābūt pieejama no dažādām ierīcēm

---

# Programmatūras projektējums
## Izmantotās tehnoloģijas

- **Backend:** Spring Boot (Java)
- **Frontend:** HeroUI (TypeScript)
- **Datubāze:** DynamoDB (NoSQL)

### Izvēles pamatojums

Es izvēlējos Spring Boot un HeroUI, jo man ir daudz pieredze ar tām, un ir ļoti ērti strādāt ar viņām.
DynamoDB izmantoju, jo man patīk NoSQL datubāzes shēmas, un arī man ir pieredze ar to.
Sākotnēji es gribēju izmantot Rust backend, bet vairāku iemeslu dēļ nolēmu to nedarīt.

## Sistēmas arhitektūra

Lietotne izmanto client-server arhitektūru:

- Frontend sazinās ar backend API
- Backend apstrādā pieprasījumus un strādā ar datubāzi

## ER Modelis
<img src="./ermodel.png" alt="ER Modelis" width="600"> <br>

## Galvenās funkcijas

- Workspace pārvaldība
- Uzdevumu (task) pārvaldība
- Lietotāju pārvaldība

(Pilns API punktu skaidrojums ir beigās!)

---

# Programmatūras izstrādes plāns
## Izvēlētais modelis

Šī projekta plānošanai es galvenokārt izmantoju savu atmiņu un intuīciju. Man bija vispārējs plāns, kas pierakstīts dokumentā, bet viss pēc tam 
tika plānots uz vietas. Kas lielākam projektam nebūtu optimāli. Tā kā lielākiem projektiem ir nepieciešama daudz lielāka savienojamība starp 
sistēmām, to plānot uz vietas ir grūtāk un laikietilpīgāk. Tāpēc es nolēmu plānot vispārēju izkārtojumu, bet ne sarežģījumus. 
Piemēram, darba vietas sistēma un tās saglabāšanas veids sākotnēji bija paredzēts citā datubāzes tabulā, bet es sapratu, ka, ja es vienkārši 
saglabātu JSON failu vecākobjektā, tas samazinātu ielādes slodzi.

## Izstrādes plāns
- Idejas definēšana
- Pamatfunkcionalitātes izstrāde
- API izveide
- Frontend izstrāde
- Testēšana un uzlabojumi

---
 
# Atkļūdošanas un akcepttestēšanas pārskats
Šī projekta atkļūdošana bija plašs process, jo katra jaunā funkcija tika pamatīgi pārbaudīta, lai pārliecinātos, ka nav kļūdu. Dažos gadījumos 
man bija jāizveido testa gadījumi serverī (tie vēlāk tika noņemti). 

## Izmantotie rīki
- IntelliJ debugger, testēšanai galvenokārt izmantoju iebūvēto debugger, jo tas ļauj man 
pārbaudīt atmiņu un iestatīt pārtraukumpunktus.
- Manuāla testēšana

## Testēšanas process
- Funkciju individuāla pārbaude
- Kļūdu identificēšana un labošana
- Try/catch izmantošana kļūdu apstrādei

## Akcepttestēšana
Programma tika testēta no lietotāja perspektīvas, lai pārliecinātos, ka tā atbilst prasībām.

---

# Lietotāja ceļvedis
## Uzstādīšana
Izveido `.env` failu ar šādiem parametriem:

```
AWS_ACCESS_KEY=tavs_dynamodb_key
AWS_SECRET_KEY=tavs_dynamodb_secret
JWT_SECRET=tavs_secret (vienkārši liels random strings)
JWT_EXPIRATION=3600000
JWT_REFRESH_EXPIRATION=86400000
```

## Projekta palaišana

```
.\gradlew build
```

vai

```
.\gradlew bootRun
```

## Lietošana
- Reģistrējies vai ielogojies sistēmā
- Izveido workspace
- Pievieno uzdevumus

---

# Papildus API informācija

Lai vieglāk saprastu nākamo sekciju, šie ir shortcuts, ko izmantošu:
- Api ("/apiVārds/?"), kur ? ir nākamas sekcijas "klausītājs"
  - "/?" - ģenerāla descripcija
    - post (ko sūta uz serveri) -> ko serveris atbild
    vai
    - get (ko serveris atbild)

Visiem publiskiem API, izņemot tiem, kas ir annotēti ar "*"
Ir jābut valid User statusam, kas tiek validēts ar accessToken

Publiskie API:
- User API ("/user/?"):
  - "/register"* - Atļauj veikt jauna User reģistrāciju 
    - post (username, displayName, password) -> statuss
  - "/login"* - Atļauj lietotājam iejiet mājaslapā
    - post (username, password) -> statuss
  - "/logout"* - Informē serveri, ka lietotājs ir izrakstijies + atjauno accessToken
    - get (accessToken)
  - "/me" - Atbild ar svarīgu informāciju par lietotāju
    - get (id, username, displayName)
  - "/teams" - Atbild ar komandu sarkastu, kurā ir lietotājs
    - get (map<id, (team) name>)
  - "/teamInvites" - Atbild ar sarakstu, kuras komandas ir ielūguši lietotāju
    - get (map<id, (team) name>)
  - "/workspaces" - Atbild ar visu workspace sarakstu
    - get (map<id, (workspace) name>)
  - "/workspace/{id}" - Atbild ar konkrētu lietotāja workspace info
    - get (id, (workspace) name)
  - "/refresh-token" - Atbild ar jaunu accessToken
    - post (refreshToken) -> accessToken
    
- Team API ("/team/?")
  - "/register" - Atļauj lietotājam reģistrēt jaunu komandu
    - post (name) -> statuss
  - "/{teamID}" - Atbild ar komandas informāciju
    - get (id, name, list<(User displayName) string>)
  - "/{teamID}/join" - Atļauj User pievienoties komandai (ja ir ielūgums)
    - get (statuss)
  - "/{teamID}/invite" - Atļauj User taisīt ielūgumu citam
    - post (username) -> statuss

- Workspace API ("/workspace/?")
  - "/create" - Atļauj taisīt jaunu workspace komandai, vai lietotājam
    - post (name, teamID) -> statuss
  - "/{id}/createEvent" - Uztaisa jaunu tukšu kalendāra event 
    - get (statuss)
  - "/{id}/updateEvent" - Pārmaina event informāciju
    - post (id, title, description, setDate, dueDate, list<attendees>) -> statuss
  - "/{id}/events" - Atbild ar visiem workspace events
    - get (list<id, event>)

Ir plāni taisīt "private" api, kur admin users var rediģēt users, teams, uttl. bez iešanas uz datubāzes
Bet tie ir nākotnes plāni!

Paldies par uzmanību!! 
--Renars

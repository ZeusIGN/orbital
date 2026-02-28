Ģenerāla informācija: <br>
Orbital ir kalendārs kadāi komandai vai organizācijai <br>
Katrs lietotājs var pievienoties komandai, un arī taisīt kopējus workspaces <br>
Workspace ir kalendārs, kas atļauj taisīt plānus jebkurai dienai <br>
Projekts izmanto SpringBoot Java 25 <br>
Izmantoju arī Bcrypt, kas shifrē paroli <br>
Datubāze ir DynamoDB (nosql), shēmas tiek izskaidrotas nākamajā sekcijā <br>
Kaut vai SpringBoot piedāvā daudzus rīkus jau uztaisītus, es izvēlējos pārsvarā tos taisīt no jauna <br>
Lai būtu maksimāla kontrolē šajam projektam (un man patīk taisīt jaunus tools (kaut vai tie jau eksistē)) <br>
Frontend ir taisīts NextJs / HeroUI, kur izmantoju TypeScript <br>
Nākamā informācija ir tehniska :D <br>

Datubāses (table) shēmas:
- Team (komanda)
    - id: num - komandas id
    - name: string - nosaukums
    - members: list<num> - komandas biedri, kur num ir User ID
    - invitedUsers: list<num> - cilvēki kas ir tikuši uzaicināti komandai

- User (lietotājs)
    - id: num - lietotāja id
    - username: string - unikāls vārds
    - displayName: string - publiskais vārds
    - email: string - e-pasts (nav pagaidām izmantots!)
    - signUpStamp: num - laiks, kad lietotājs taisīja kontu (no epoch)
    - teams: list<num> - lietotāja komandas, kur num ir Team ID (šo tehniski varētu izņemt ārā, un ļaut Team lādēt visus Users)
    - notifications: map<num, obj> - num ir id un obj ir abstracts datu tips (DataHolder)

Abām datubāses shēmām arī piemīt 
    - workspace: map<num, obj> - kur num ir id un obj ir abstracts datu tips (DataHolder)
    
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

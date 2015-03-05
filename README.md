# hackathon
hackathon

To link to the bbox for launch the app : 
WIN + R -> cmd
cd AppData/Local/Android/sdk/platform-tools
adb (IP de la box) ---> 192.168.8.2 for the moment
If port need use 5555

To start the bbox api :
adb.exe shell am startservice - a "fr.bouyguestelecom.bboxapi.StartService" --user 0
Check in the parma, and process app.

To use an AVD :
Do not use intel x86 processsor, only ARM.
There is an issue with the haxwell processor boost.

Git RULES:

If you're not sure about what u're doing push in an other branch please.
Leave master clean.
[git checkout -b "issue_branch_name"]
[git add --all] [git commit -m "what u have done"] [git push origin issue_branch_name]

Please follow the rules or go die.

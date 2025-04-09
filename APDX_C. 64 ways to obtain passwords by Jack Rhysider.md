https://www.youtube.com/watch?v=Qm7k1CPFkIc&ab_channel=JackRhysider

1) Steal the computer/phone.
2) Get physical access to the computer. (Evil maid attack)
3) Sometimes there is no password.
4) Ask for the password
5) Maybe breached before. Go to breach forums and buy a password DB. cracked.io.
6) Brute force Burp Suit
7) Brute force Hydra
8) Hash - e.g. by grabbing windows32 config sam - tools John the Ripper, Hash cat.
9) Get to a higher level account, like root, and reset password for any user on that machine, or see the private keys.
10) ActiveDirectory Admin access, or Helpdesk access - reset any password.
11) Get in as the website admin
12) Get into the database directly - use SQL
13) Sometimes passwords are stored in plain text in the database
14) Get to DB via network / vulnerability
15) shodan.io - gives you open mongodb databases
16) SQL injection on a website. Entire databases have been dumped through SQL injection.
17) Comb through any code found on the website or app, creds sometimes are hardcoded. Also look for open AWS instances that expose the codebase, and dive there for db creds.
18) Look through github repos searching for creds. A lot of passwords and private keys are discovered that way.  
    An API Key is often as good as a password.
19) Inspect the app itself using `strings` command, looking through plist files, may reveal a plaintext password.
20) View source and look through the code on a website to find a vulnerability, password or API key.
21) Trick API to send you more data than you're entitled for. Major breaches can follow from data from insecure API.
22) Physically get into a DC and steal a db server and bring it home, you might get into it eventually. It could be as simple as pulling out a hard drive and plugging it into your computer.
23) On the same windows computer run mimikatz to extract other users password out of memory.
24) If you're on the same local subnet, you could run Responder, which will act like a shared drive on the network. Other computers will see it and try to connect, and authenticate, obtaining password hashes.
25) Sometimes passing a hash is good enough to get to something, without knowing an actual password.
26) Run Responder, get a password of another user, and see if that user has some extra privileges like Admin.
27) If you have a domain admin password, that will give you access to other accounts that might be of interest.
28) Passwords tend to be reused
29) If you want a wifi password and you're near the device, you can run "wifi pineapple", which will act as the wifi network and ask for the wifi password
30) Wifi - if you're in range, you can use tools like Aircrack-ng to wathc the traffic and try to crack the password on some networks.
31) Insiders. (Inny) Like someone at Facebook who can reset a password for a fee.
32) Nation State Actors: seeding operation - recruit somebody who is about to get hired, help the guy get a job, use the insider access.
33) Nation State Actors: Surveillance systems, microphones, cameras, etc. See NSA Ant catalog.
34) Turn mic on your phone, record somebody typing and decode a password out of that
35) Use thermal camera to watch what keys got warmer when somebody is typing
36) Phishing / Social Engineering / Scamming. Trick a user to give you the password. E.g. install a keylogger on your computer and have user use your computer to log in into something of interest.
37) Shoulder surfing - watch the fingers while they type. Practice required.
38) Fake lookalike website, and give user a link.
39) Call and trick into giving you the password, like calling from customer support.
40) Call the place you're trying to access and impersonate the target. Ask the company to reset "your" password.
41) Look on the desk for stick-it notes or other paper with passwords written down.
42) Look through the files - local storage, network storage, Dropbox, google drive, etc.
43) Have him install a keylogger on the machine.
44) Usb keyloggers - walk by someone's machine and plug it in.
45) USB rubber sucky, omg cable - looks like an ordinary cable, but when plugged in it injects keystrokes to the computer. Can potentially grab a dump of the memory or hashtable, which can be analyzed for a password.
46) Attack the device over the network, identifying a vulnerability, then install a keylogger or sift through the files
47) Password manager - get the master password, and you got all the passwords.
48) Email - if you have access to the email, a password can be reset using an email link; or one-time login link can be used.
49) Attacking an email is very effective, some people attack it first when they need to get into someone's account. They'd call Google or Microsoft and trick them to reset the gmail password.
50) Guessable password, like dogs' names, relatives' names. Pop relevant wordlist with the hash into John the Ripper and within minutes you can get the password.
51)  You don't have to social engineer to build the wordlist. It can be built solely from the online publications.  
     A wordlist like:
```  
Charlie, dog  
Sandra, wife  
Sky Insurance, work  
Hilton Resort, fav vacation  
The Lumineers, fav band  
Blue, fav color  
Nobu, fav eats  
Buffman Gym  
etc.  
```  
52) Security researchers found that it helps to throw a bunch of cultural relevant words into a wordlist, like local school names, local sports teams, local street names, local restaurants, city names, etc.  
    Or things that are related to the company, like the name of the company, or its mascot, or address.  
    Unbelievable, how many employees use the company name as the password.
53) Top 200 most common passwords. People will often use the most common password they can. https://nordpass.com/most-common-passwords-list/
54) Sometimes websites have a weak password reset policy, e.g. website chooses a 4-character password for the user. Then if the password is reset, it's easily brutable.
55) If you know where the person works, call the helpdesk and pretend to be that person, and ask for a password reset. you can even trick him into changing it for you to whatever you choose.
56) Sometimes a password is not needed, all you need is a session key, which will result in logging in without providing a password, and 2FA won't help here either.
57) Send fake links, like fake Dyno links on Discord, which look like you're authenticating into discord dyno.
58) Website vulnerability - switch to a different account by simply telling the website that they are a different user, and the website would switch user without re-authenticating. That's bad really.
59) Private keys are better than passwords, and they're too long to memorize. They're usually stored somewhere, find them.
60) Right-click on a browser, do "inspect element" to see the password in clear text, even though it's shown in asterisks/stars.  
    61.1) Look through cached data to see if a password is saved somewhere.  
    61.b) Lot of times a password is left as default.
62) On the same network you might act as a proxy, and inspect all the traffic that they're sending and receiving, or intercept with lantap or wifi sniffer.
63) Attack a third party - e.g. through apple account get into the phone, and through the phone get to the account you want.
64) Extortion, including physical measures.
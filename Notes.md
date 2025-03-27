## Serial ports on Linux

1. Verify User Group Membership

Make sure your user is indeed part of the dialout group. You can check your group memberships with the following command:
```
groups
```
Look for `dialout` in the output. If it's not there, you need to add your user to the group:

```
sudo usermod -a -G dialout $USER
reboot
```

After running this command, log out and log back in for the changes to take effect.

2. Check Permissions on the Serial Port

Run the following command to check the permissions of the serial port:
```
ls -l /dev/ttyACM0
```
You should see something like this:

```
crw-rw---- 1 root dialout 166, 0 date time /dev/ttyACM0
```
This means that the owner (root) has read and write permissions, and the group (dialout) also has read and write permissions. If your user is in the dialout group, you should have access.


3. Test Access with a Serial Tool

Before running your Java program, test access to the serial port using a terminal program like screen or minicom:

```
screen /dev/ttyACM0 9600
```

4. Test Serial Communication with a Terminal Program

Before running your Java program, test the serial communication using a terminal program like screen or minicom:

Using screen:
```
screen /dev/ttyACM0 9600
```
Using minicom:
```
sudo apt install minicom
minicom -D /dev/ttyACM0 -b 9600
```

5. Using Fidd to secure backup DB

Even though Phraser DB by itself is encrypted, we don't want to risk keeping its backups as is.
Ideally, backup files should be encrypted using some private key, e.g. with 
Hybrid Asymmetric-Symmetric Encryption Method.  
With tools like Cryptor non-exportable certificates from PKCS#11 tokens can be utilized for that.
Fidd can be used to further improve the reliability allowing dual certificate encryption, so that
a single PKCS#11 token failure won't be fatal, and another token can be used to access the backups.

# Database tools for the Phraser Project

This project provides a UI management tool for custom database format used in the Phraser device.

## A Bit About the Database:

The database consists of 4096 bytes blocks (the size of a flash sector of the target microcontroller RP2040). 
By default, we occupy 1 MB of built-in flash, which gives us 256 such blocks. The structure and types of data 
stored in the blocks are described using FlatBuf, and their structure can be examined in more detail in 
`resources/Schema.fbs`.

### 1. External Structure
Data is stored in an encrypted form. All blocks consist of an encrypted data segment and an unencrypted footer.  
In the footer, we have 16 bytes - IV.

### 2. Encrypted Segment
AES-256 CBC is used for encryption, thus the size of the encrypted data is 4096-16 = 4080 bytes, 
which is convenient since AES works with a block size of 16 bytes.  
In the header of the encrypted segment, we have: 1 (BlockType) + 2 (DataLength) = 3 bytes.  
In the footer of the encrypted segment, we have: 4 (Adler32) = 4 bytes.  
Thus, the maximum size of the data is 4080 - (4+3) = 4073 bytes.

![DB Block structure.png](DB%20Block%20structure.png)

Data is padded with random bytes to the maximum size and reversed before encryption so that 
what an attacker trying to decrypt sees first is random data, not structured data. Triple AES is 
theoretically possible, but the effort required to implement a non-standard mode on the microcontroller 
does not allow it to be one of the practical goals of the first version.

Also, for security, when initializing the database, it makes sense to place blocks in random locations (shuffle)
so that it is harder for an attacker to deduce that, for example, the block with the key should be 
at a certain position by default.

### 3. KeyBlock
A special KeyBlock containing the decryption key for all other blocks is encrypted with a user key 
generated from the user password using PBKDF2 algorithm. This block also contains an IV mask for all 
other blocks, which should be XORed with the IV from the footer of the block, the result is used as 
an actual IV for encryption/decryption.


### Optimizations:

To optimize the life of flash memory, new and modified blocks are written in append-only mode, using 
unoccupied space as a circular buffer (Copy On Write). To avoid bit rot, each time we write a new/updated block, 
we move another one; the idea here is to make sure we overwrite blocks that have not been updated in a while. 
(More details in [1. Phraser DB - Optimizing Flash Wear and Bit Rot.md](1.%20Phraser%20DB%20-%20Optimizing%20Flash%20Wear%20and%20Bit%20Rot.md))

It's possible to go even further and move one block at each power-up, as there may be situations where data changes 
too infrequently; but this approach might be too aggressive and is likely to reduce the lifespan of the flash 
due to excessive writes. Alternatively, a special user-invokable function could be created for "refreshing" 
the storage by moving blocks around; or all data could be stored in 2 copies; overall, there is a room for creativity here.

However, making a built-in flash reliable in the "true" sense of the word seems very labor-intensive, 
if not impossible; therefore, the best and primary way to protect the device's data from loss or corruption 
is to periodically export the database data and recover from data loss using those external backups.
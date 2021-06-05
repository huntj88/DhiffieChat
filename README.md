## DhiffieChat: End-To-End Encrypted Private Chat
### How your data is protected
Every user provided piece of data is encrypted before it leaves the 
device. The encryption process uses a shared key derived from a 
Diffie-Hellman key exchange, so the server is never aware of the 
credentials required to decrypt the data.

To add a friend you must exchange QR codes which contains the user's 
`alias` as well as a hash of their public key, which is used to 
identity themselves. This is done because it would be impractical to 
MiTM attack two people physically in the same location exchanging QR
codes, although they could be exchanged through another method. The 
`alias` is also never shared with the server.

On Android the Local database is encrypted with SQLCipher. The DB 
password is randomly generated on first launch and is encrypted within 
Android's `Trusted Execution Environment` where the private key for 
the DB password is held

The App also does a handshake with the server so that the server can 
verify that the client actually has the private key that goes with the
public key they claim to have.

Any uploaded media is stored as encrypted binary data in s3, where 
after 14 days the data is automatically deleted. 

The user provided encrypted text is also automatically deleted from 
DynamoDB after 14 days


### Coming soon
Ephemeral keys: Right now a user has one main private/public 
Diffie-Hellman key pair which is used for encrypting the data. In the
future, each message should be encrypted using a different set of
credentials, and no key pair should be used for more than one 
message

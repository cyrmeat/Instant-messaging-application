# Instant-messaging-application
an easy command line communication program

The server can accept the following three arguments:
• server_port: this is the port number which the server will use to communicate with the clients. Recall that a TCP socket is NOT uniquely identified by the server port number. So it is possible for multiple TCP connections to use the same server-side port number.
• block_duration: this is the duration in seconds for which a user should be blocked after three unsuccessful authentication attempts.
• timeout: this is the duration in seconds of inactivity after which a user is logged off by the server.

The server should be executed before any of the clients. It should be initiated as follows:
If you use Java:
java Server server_port block_duration timeout

The client can accept the following two arguments:
• server_IP: this is the IP address of the machine on which the server is running.
• server_port: this is the port number being used by the server. This argument should be the same as the first argument of the server.
Note that, you do not have to specify the port to be used by the client. You should allow the OS to pick a random available port. Each client should be initiated in a separate terminal as follows:
If you use Java:
java Client server_IP server_port

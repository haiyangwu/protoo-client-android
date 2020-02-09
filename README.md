# protoo-client-android

![Bintray][bintray-shield-protoo-client-android]
[![Build Status][travis-ci-shield-protoo-client-android]][travis-ci-protoo-client-android] 
[![Codacy Badge][codacy-grade-shield-protoo-client-android]][codacy-grade-protoo-client-android]

Minimalist and extensible Android Client signaling framework for multi-party Real-Time applications

## Getting Started
### Setting up the dependency
Include `protoo-client-android` into your project, for example, as a Gradle compile dependency:

```groovy
implementation 'org.protoojs.droid:protoo-client:4.0.3'
```
### Example

* implement your own `WebSocketTransport`
```java
public class WebSocketTransport extends AbsWebSocketTransport {
    // ...
}
```
> `protoo-client-android` just define a base class [`AbsWebSocketTransport`][code-AbsWebSocketTransport] 
> which offer opportunity to implement your own `WebSocketTransport` 

* creates a WebSocket connection

```java
// class WebSocketTransport extends AbsWebSocketTransport
WebSocketTransport transport = new WebSocketTransport("wss://example.org");
```

* create a participant in a remote room

```java
private Peer.Listener peerListener =
      new Peer.Listener() {
        // ...
      };
mPeer = new Peer(transport, peerListener);
```

* send request or notify

Once connected to remote server `Peer.Listener#onOpen` will be called, then you can call 
`Peer#request` or `Peer#notify` to send message to server.

```java
mPeer.request("dummy", ...);
mPeer.notify("dummy", ...);
```

## Author
Haiyang Wu([@haiyangwu](https://github.com/haiyangwu/) at Github)

## License
[MIT](./LICENSE)




[bintray-shield-protoo-client-android]:https://img.shields.io/bintray/v/haiyangwu/maven/protoo-client
[travis-ci-shield-protoo-client-android]:https://travis-ci.org/haiyangwu/protoo-client-android.svg?branch=master
[travis-ci-protoo-client-android]:https://travis-ci.org/haiyangwu/protoo-client-android
[codacy-grade-shield-protoo-client-android]:https://api.codacy.com/project/badge/Grade/bc233c4d62de4fe9aee1ec9e7c406ef4
[codacy-grade-protoo-client-android]:https://app.codacy.com/manual/haiyangwu/protoo-client-android?utm_source=github.com&utm_medium=referral&utm_content=haiyangwu/protoo-client-android&utm_campaign=Badge_Grade_Dashboard
[code-AbsWebSocketTransport]:./protoo-client/src/main/java/org/protoojs/droid/transports/AbsWebSocketTransport.java

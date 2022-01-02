# Eclipse Paho MQTT client PoC *Public Repo*

The commit history in this repo shows various "tests" demonstrating how MQTT delivery modes work.

## dev setup

```shell
brew install mosqitto
brew services start mosqitto

git clone <repo>
./gradlew build
./gradlew run
```

## Summary

1. exactly once is really slow, and probably is still only "at most once"
2. "at most once" is probably what we want to do, and we'll need message sequence numbers, timeouts, and retries
3. MQTT 5 is a lot better than MQTT 3 - Mosquitto does both just fine
4. MQTT 5 can (sometimes) tell us if there are subscribers or if the message will be discarded
5. We probably want `retained mode` = false (better never than late)
6. MQTT 5 allows message timeouts (which we shouldn't need with retained mode, but is nice)
7. MQTT 5 gives us "clean start" which makes sure there are no stale messages

Summary of summary: one can disable queueing in MQTT, making sure we don't have stale data, but it can't ensure exactly once delivery

MQTT 5 also gives us [Request/Resopnse](https://stackoverflow.com/questions/59888811/request-response-pattern-with-mosca-mqtt/59916330#59916330), however:

```text
Even with MQTT v5 you would need to implement the idle timeout bit yourself.
```

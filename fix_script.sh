#!/bin/bash

# Fix lambda capture issues in schedule trigger
cd /storage/emulated/0/AndroidPEProjects/TgFlowBot

# Make finalMillis effectively final
# The fix is already applied at line 935

# Make intervalSec effectively final in http poll trigger
# Need to declare finalIntervalSec above and use it in the lambda

sed -i 's/final long finalMillis = final long finalMillis =/final long finalMillis =/' app/src/main/java/com/tgflowbot/MainActivity.java
sed -i 's/final long finalMillis = final long finalMillis =/final long finalMillis =/' app/src/main/java/com/tgflowbot/MainActivity.java


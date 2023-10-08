Updating
========

This is a guide for porting CoordinateOffset to a new Minecraft version.

Step 0: Wait
------------
Wait for the dependencies to be updated. That typically means ProtocolLib and CommandAPI. Dev builds for ProtocolLib
have worked well for testing, get them [here](https://ci.dmulloy2.net/job/ProtocolLib/).

When they are fully updated, bump any dependency versions in `build.gradle.kts` at the root of the project.

Step 1: New Translator
----------------------
This plugin is heavily dependent on the Minecraft protocol and its details, so the majority of breakages happen when
Mojang makes even a small change to the protocol. CoordinateOffset is written to easily handle multiple Minecraft
versions with its "Translator" interface. These translators aggregate all functionality that can be described as
"offsetting coordinates within every packet that contains coordinates".

Translators are located at `com.jtprince.coordinateoffset.translator`. The first step for supporting a new version is to
create a new one.
1. Copy the existing translator directory with the closest version to the one you're trying to support.
2. Rename all references in that translator directory to the new version.
3. Navigate to `com.jtprince.coordinateoffset.translator.Translator` and locate the translator version registry, named
   `VERSIONS`. Add your new translator version to the list. Keep it sorted with the most recent Minecraft version first.
4. If the new version is more recent than the currently supported version, update the `LATEST_SUPPORTED` variable in the
   Translator file.
5. Try compiling and running the plugin on a test server. It should start up and load, indicating the new Translator
   version in the console with no errors.

Step 2: Fix Translator Errors
-----------------------------
Now that you have a translator for the new Minecraft version, you will need to debug it. Log into the server and watch
the console. If you see errors, you will have to start debugging them.

The easiest way to debug an error from ProtocolLib is to use a debugger in IntelliJ or similar. There are plenty of
guides online for how to attach a debugger to the running Minecraft server and place breakpoints within plugin code.

My general gameplay loop for debugging is:
* Locate a packet error being thrown from ProtocolLib
* Put a breakpoint on the function that is throwing it, most likely in the `PacketContainerUtils` file.
* Use [wiki.vg protocol documentation](https://wiki.vg/Protocol) to determine what changed in the packet.
* Write a new packet container util for the new packet format and point the translator to use it. Don't modify the
  existing one, or other versions might break!

Step 3: Add New Packets
-----------------------
New protocol versions likely added some new packet types. It is critical that every packet containing an absolute
coordinate is registered in the Translator so that the plugin can obfuscate that coordinate. The easiest way to do this
is to look at the Minecraft changelog, and try out all of the new features in that version to make sure that none of
them are broken. Do this while confirming that your coordinates displayed in F3 are not the world's real coordinates.

Step 3: Translator Cleanup
--------------------------
Check for Deprecated packet types in the translator. ProtocolLib helpfully marks packet types that are no longer part
of the Minecraft protocol as Deprecated in Java. These can safely be removed from any translator for the latest
protocol.

Step 4: Testing
---------------
Now it's time to test everything. I summarized everything that tends to exhibit problems in [Testing.md](Testing.md).

Step 5: Done!
-------------

